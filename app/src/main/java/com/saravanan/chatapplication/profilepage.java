package com.saravanan.chatapplication;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class profilepage extends AppCompatActivity {

    ImageView imgProfile;
    ProgressBar progressLoader;
    EditText username, age, gender, email, phonenumber;
    Button save, logout, deletaccount;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    static final int GALLERY_REQ = 201;
    static final int CAMERA_REQ = 202;
    static final int CAMERA_PERMISSION_CODE = 101;

    Uri cameraImageUri;

    private static final String CLOUD_NAME = "dlfrm7myp";
    private static final String UPLOAD_PRESET = "chatapplication";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profilepage);

        // Toolbar for three dots
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        imgProfile = findViewById(R.id.imgProfile);
        progressLoader = findViewById(R.id.progressLoader);
        username = findViewById(R.id.edtUsername);
        age = findViewById(R.id.edtAge);
        email = findViewById(R.id.edtEmail);
        phonenumber = findViewById(R.id.edtPhone);
        gender = findViewById(R.id.edtGender);
        save = findViewById(R.id.btnSaveProfile);

        loadUserProfile();
        loadProfileImage();

        save.setOnClickListener(v -> saveProfile());
        imgProfile.setOnClickListener(v -> showImagePickerDialog());
    }

    // ================= LOAD USER DATA =================
    private void loadUserProfile() {

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("Users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {

                    if (!doc.exists()) return;

                    if (doc.getString("username") != null)
                        username.setText(doc.getString("username"));

                    if (doc.getString("email") != null) {
                        email.setText(doc.getString("email"));
                        email.setEnabled(false);
                    }

                    if (doc.getString("phonenumber") != null)
                        phonenumber.setText(doc.getString("phonenumber"));

                    if (doc.getString("gender") != null)
                        gender.setText(doc.getString("gender"));

                    if (doc.getString("age") != null)
                        age.setText(doc.getString("age"));
                });
    }

    // ================= LOGOUT =================
    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout Account")
                .setMessage("Are you sure you want to logout?")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", (d, w) -> {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(this, Login.class));
                    finish();
                })
                .show();
    }

    // ================= DELETE CONFIRM =================
    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("This will permanently delete your account")
                .setPositiveButton("Yes", (d, w) -> showPasswordDialog())
                .setNegativeButton("No", null)
                .show();
    }

    // ================= PASSWORD DIALOG =================
    private void showPasswordDialog() {

        EditText passwordInput = new EditText(this);
        passwordInput.setHint("Enter your password");
        passwordInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        new AlertDialog.Builder(this)
                .setTitle("Confirm Password")
                .setView(passwordInput)
                .setPositiveButton("Delete", (d, w) -> {
                    String password = passwordInput.getText().toString().trim();
                    if (!password.isEmpty()) {
                        deleteAccount(password);
                    } else {
                        Toast.makeText(this,
                                "Password required",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ================= DELETE ACCOUNT =================
    private void deleteAccount(String password) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        AuthCredential credential =
                EmailAuthProvider.getCredential(user.getEmail(), password);

        user.reauthenticate(credential)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        String uid = user.getUid();

                        FirebaseFirestore.getInstance()
                                .collection("Users")
                                .document(uid)
                                .delete()
                                .addOnSuccessListener(unused -> {

                                    user.delete()
                                            .addOnCompleteListener(del -> {

                                                SharedPreferences prefs =
                                                        getSharedPreferences(
                                                                "chatapplication",
                                                                MODE_PRIVATE);
                                                prefs.edit().clear().apply();

                                                Intent i =
                                                        new Intent(this, Login.class);
                                                i.setFlags(
                                                        Intent.FLAG_ACTIVITY_NEW_TASK |
                                                                Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(i);
                                                finish();

                                                Toast.makeText(this,
                                                        "Account deleted permanently",
                                                        Toast.LENGTH_LONG).show();
                                            });
                                });
                    } else {
                        Toast.makeText(this,
                                "Wrong password",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ================= SAVE PROFILE =================
    private void saveProfile() {

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        Map<String, Object> data = new HashMap<>();

        if (!username.getText().toString().isEmpty())
            data.put("username", username.getText().toString());

        if (!phonenumber.getText().toString().isEmpty())
            data.put("phonenumber", phonenumber.getText().toString());

        if (!gender.getText().toString().isEmpty())
            data.put("gender", gender.getText().toString());

        if (!age.getText().toString().isEmpty())
            data.put("age", age.getText().toString());

        if (data.isEmpty()) {
            Toast.makeText(this, "Nothing to update", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("Users")
                .document(user.getUid())
                .update(data)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this,
                                "Profile updated successfully",
                                Toast.LENGTH_SHORT).show());
    }

    // ================= IMAGE PICK =================
    void showImagePickerDialog() {
        String[] options = {"Camera", "Gallery"};

        new AlertDialog.Builder(this)
                .setTitle("Select Image")
                .setItems(options, (d, i) -> {
                    if (i == 0) openCamera();
                    else openGallery();
                })
                .show();
    }

    void openGallery() {
        startActivityForResult(
                new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
                GALLERY_REQ);
    }

    void openCamera() {

        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE);
            return;
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Profile Image");

        cameraImageUri = getContentResolver()
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
        startActivityForResult(intent, CAMERA_REQ);
    }

    @Override
    public void onRequestPermissionsResult(int code,
                                           @NonNull String[] p,
                                           @NonNull int[] r) {
        if (code == CAMERA_PERMISSION_CODE &&
                r.length > 0 &&
                r[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        }
    }

    @Override
    protected void onActivityResult(int code, int result,
                                    @Nullable Intent data) {
        super.onActivityResult(code, result, data);

        if (result != RESULT_OK) return;

        if (code == GALLERY_REQ && data != null)
            uploadToCloudinary(data.getData());

        if (code == CAMERA_REQ && cameraImageUri != null)
            uploadToCloudinary(cameraImageUri);
    }

    // ================= CLOUDINARY UPLOAD =================
    private void uploadToCloudinary(Uri imageUri) {

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        progressLoader.setVisibility(View.VISIBLE);

        OkHttpClient client = new OkHttpClient();

        try {
            InputStream inputStream =
                    getContentResolver().openInputStream(imageUri);
            byte[] bytes = readBytes(inputStream);

            RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "image.jpg",
                            RequestBody.create(bytes))
                    .addFormDataPart("upload_preset", UPLOAD_PRESET)
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/" +
                            CLOUD_NAME + "/image/upload")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call c, IOException e) {
                    runOnUiThread(() ->
                            progressLoader.setVisibility(View.GONE));
                }

                @Override public void onResponse(Call c, Response r)
                        throws IOException {
                    try {
                        JSONObject json =
                                new JSONObject(r.body().string());
                        String imageUrl =
                                json.getString("secure_url");
                        runOnUiThread(() -> saveImageUrl(imageUrl));
                    } catch (Exception ignored) {}
                }
            });

        } catch (Exception e) {
            progressLoader.setVisibility(View.GONE);
        }
    }

    private byte[] readBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[16384];
        int n;
        while ((n = is.read(data)) != -1)
            buffer.write(data, 0, n);
        return buffer.toByteArray();
    }

    private void saveImageUrl(String url) {

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("Users")
                .document(user.getUid())
                .update("profileImage", url)
                .addOnSuccessListener(v -> {
                    progressLoader.setVisibility(View.GONE);
                    Glide.with(this).load(url).into(imgProfile);
                });
    }

    private void loadProfileImage() {

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("Users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()
                            && doc.getString("profileImage") != null) {
                        Glide.with(this)
                                .load(doc.getString("profileImage"))
                                .into(imgProfile);
                    }
                });
    }

    // ================= OPTIONS MENU =================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.profilemenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.logout) {
            logout();
            return true;
        }
        else if (item.getItemId() == R.id.deleteaccount) {
            showDeleteConfirmDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, Homepage.class));
        finish();
    }
}
