package com.saravanan.chatapplication;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Profilesection extends AppCompatActivity {

    ImageView imgDp;
    ProgressBar progressLoader;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    static final int GALLERY_REQ = 1;
    static final int CAMERA_REQ = 2;
    static final int CAMERA_PERMISSION_CODE = 101;

    Uri cameraImageUri;

    private static final String CLOUD_NAME = "dlfrm7myp";
    private static final String UPLOAD_PRESET = "chatapplication";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profilesection);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        imgDp = findViewById(R.id.imgDp);
        progressLoader = findViewById(R.id.progressLoader);

        loadProfileImage();

        imgDp.setOnClickListener(v -> showImagePickerDialog());
    }

    // ================= IMAGE PICK DIALOG =================
    void showImagePickerDialog() {
        String[] options = {"Camera", "Gallery"};

        new AlertDialog.Builder(this)
                .setTitle("Select Image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else {
                        openGallery();
                    }
                })
                .show();
    }

    // ================= OPEN GALLERY =================
    void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, GALLERY_REQ);
    }

    // ================= OPEN CAMERA (WITH PERMISSION) =================
    void openCamera() {

        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE
            );
            return;
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Profile Image");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Camera Image");

        cameraImageUri = getContentResolver()
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
        startActivityForResult(intent, CAMERA_REQ);
    }

    // ================= PERMISSION RESULT =================
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this,
                        "Camera permission denied",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ================= RESULT =================
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) return;

        if (requestCode == GALLERY_REQ && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                imgDp.setImageURI(imageUri);
                uploadToCloudinary(imageUri);
            }
        }

        if (requestCode == CAMERA_REQ) {
            if (cameraImageUri != null) {
                imgDp.setImageURI(cameraImageUri);
                uploadToCloudinary(cameraImageUri);
            }
        }
    }

    // ================= CLOUDINARY UPLOAD =================
    private void uploadToCloudinary(Uri imageUri) {

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        progressLoader.setVisibility(ProgressBar.VISIBLE);

        String url = "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";
        OkHttpClient client = new OkHttpClient();

        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            byte[] bytes = readBytes(inputStream);

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                            "file",
                            "image.jpg",
                            RequestBody.create(bytes)
                    )
                    .addFormDataPart("upload_preset", UPLOAD_PRESET)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        progressLoader.setVisibility(ProgressBar.GONE);
                        Toast.makeText(Profilesection.this,
                                "Upload failed", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        String imageUrl = json.getString("secure_url");
                        runOnUiThread(() -> saveImageUrl(imageUrl));
                    } catch (Exception e) {
                        runOnUiThread(() ->
                                progressLoader.setVisibility(ProgressBar.GONE));
                    }
                }
            });

        } catch (Exception e) {
            progressLoader.setVisibility(ProgressBar.GONE);
        }
    }

    // ================= READ BYTES =================
    private byte[] readBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[16384];
        int nRead;
        while ((nRead = inputStream.read(data)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    // ================= SAVE URL =================
    private void saveImageUrl(String imageUrl) {

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("Users")
                .document(user.getUid())
                .update("profileImage", imageUrl)
                .addOnSuccessListener(unused -> {
                    progressLoader.setVisibility(ProgressBar.GONE);
                    Glide.with(this).load(imageUrl).into(imgDp);
                });
    }

    // ================= LOAD IMAGE =================
    private void loadProfileImage() {

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("Users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.getString("profileImage") != null) {
                        Glide.with(this)
                                .load(doc.getString("profileImage"))
                                .into(imgDp);
                    }
                });
    }
}
