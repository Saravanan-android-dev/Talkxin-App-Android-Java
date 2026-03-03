package com.saravanan.chatapplication;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import okhttp3.*;

public class MainActivity2 extends AppCompatActivity {

    TextView txtChatUser;
    EditText chatbox;
    ImageButton sendbutton, attachBtn;
    LinearLayout chatContainer;
    ScrollView scrollView;
    ProgressBar progressBar;

    FirebaseAuth auth;
    FirebaseFirestore db;

    String myUid, otherUserId, receiverUsername, chatId;

    static final int GALLERY_REQ = 201;
    static final int CAMERA_REQ = 202;
    static final int CAMERA_PERMISSION_CODE = 101;

    Uri cameraImageUri;

    private static final String CLOUD_NAME = "dlfrm7myp";
    private static final String UPLOAD_PRESET = "chatapplication";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Toolbar toolbar = findViewById(R.id.userbar);
        setSupportActionBar(toolbar);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        myUid = auth.getCurrentUser().getUid();
        otherUserId = getIntent().getStringExtra("receiverUid");
        receiverUsername = getIntent().getStringExtra("receiverUsername");

        chatId = myUid.compareTo(otherUserId) < 0
                ? myUid + "_" + otherUserId
                : otherUserId + "_" + myUid;

        txtChatUser = findViewById(R.id.txtChatUser);
        chatbox = findViewById(R.id.chatbox);
        sendbutton = findViewById(R.id.sendbutton);
        attachBtn = findViewById(R.id.attachBtn);
        chatContainer = findViewById(R.id.chatContainer);
        scrollView = findViewById(R.id.scroll);

        txtChatUser.setText(receiverUsername);

        sendbutton.setOnClickListener(v -> sendMessage());
        attachBtn.setOnClickListener(v -> showImagePickerDialog());

        listenMessages();
    }

    // ================= SEND TEXT =================
    private void sendMessage() {

        String msg = chatbox.getText().toString().trim();
        if (msg.isEmpty()) return;

        Map<String, Object> map = new HashMap<>();
        map.put("senderUid", myUid);
        map.put("receiverUid", otherUserId);
        map.put("text", msg);
        map.put("imageUrl", "");
        map.put("time", System.currentTimeMillis());

        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(map);

        chatbox.setText("");
    }

    // ================= SEND IMAGE =================
    private void sendImageMessage(String imageUrl) {

        String msg = chatbox.getText().toString().trim();

        Map<String, Object> map = new HashMap<>();
        map.put("senderUid", myUid);
        map.put("receiverUid", otherUserId);
        map.put("text", msg.isEmpty() ? "" : msg);
        map.put("imageUrl", imageUrl);
        map.put("time", System.currentTimeMillis());

        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(map);

        chatbox.setText("");
    }

    // ================= LISTEN MESSAGES =================
    private void listenMessages() {

        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("time", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {

                    if (error != null || value == null) return;

                    chatContainer.removeAllViews();

                    for (DocumentSnapshot doc : value.getDocuments()) {

                        String text = doc.getString("text");
                        String imageUrl = doc.getString("imageUrl");
                        String senderUid = doc.getString("senderUid");

                        View messageView = getLayoutInflater().inflate(
                                senderUid.equals(myUid)
                                        ? R.layout.item_sender
                                        : R.layout.item_receiver,
                                chatContainer,
                                false
                        );

                        TextView txtMessage =
                                messageView.findViewById(R.id.txtMessage);
                        ImageView imageMessage =
                                messageView.findViewById(R.id.imageMessage);
                        TextView txtTime =
                                messageView.findViewById(R.id.txtTime);

                        if (txtMessage != null) {
                            if (text != null && !text.isEmpty()) {
                                txtMessage.setVisibility(View.VISIBLE);
                                txtMessage.setText(text);
                            } else {
                                txtMessage.setVisibility(View.GONE);
                            }
                        }

                        if (imageMessage != null) {
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                imageMessage.setVisibility(View.VISIBLE);
                                Glide.with(this)
                                        .load(imageUrl)
                                        .into(imageMessage);
                            } else {
                                imageMessage.setVisibility(View.GONE);
                            }
                        }

                        if (txtTime != null) {
                            Long timeMillis = doc.getLong("time");
                            if (timeMillis != null) {
                                String formattedTime =
                                        android.text.format.DateFormat
                                                .format("hh:mm a", timeMillis)
                                                .toString();
                                txtTime.setText(formattedTime);
                            }
                        }

                        chatContainer.addView(messageView);
                    }

                    chatContainer.postDelayed(() -> {
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }, 200);
                });
    }

    // ================= IMAGE PICKER =================
    private void showImagePickerDialog() {

        String[] options = {"Camera", "Gallery"};

        new AlertDialog.Builder(this)
                .setTitle("Select Image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) openCamera();
                    else openGallery();
                })
                .show();
    }

    // ================= OPEN GALLERY =================
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, GALLERY_REQ);
    }

    // ================= OPEN CAMERA =================
    private void openCamera() {

        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE);
            return;
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Chat Image");

        cameraImageUri = getContentResolver()
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
        startActivityForResult(intent, CAMERA_REQ);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            openCamera();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) return;

        if (requestCode == GALLERY_REQ && data != null) {
            uploadToCloudinary(data.getData());
        }

        if (requestCode == CAMERA_REQ && cameraImageUri != null) {
            uploadToCloudinary(cameraImageUri);
        }
    }

    // ================= UPLOAD TO CLOUDINARY =================
    private void uploadToCloudinary(Uri imageUri) {

        if (progressBar != null)
            progressBar.setVisibility(View.VISIBLE);

        OkHttpClient client = new OkHttpClient();

        try {
            InputStream inputStream =
                    getContentResolver().openInputStream(imageUri);
            byte[] bytes = readBytes(inputStream);

            RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "image.jpg",
                            RequestBody.create(bytes,
                                    MediaType.parse("image/*")))
                    .addFormDataPart("upload_preset", UPLOAD_PRESET)
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/" +
                            CLOUD_NAME + "/image/upload")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(Call call, java.io.IOException e) {
                    runOnUiThread(() -> {
                        if (progressBar != null)
                            progressBar.setVisibility(View.GONE);
                    });
                }

                @Override
                public void onResponse(Call call, Response response)
                        throws java.io.IOException {

                    try {
                        JSONObject json =
                                new JSONObject(response.body().string());
                        String imageUrl =
                                json.getString("secure_url");

                        runOnUiThread(() -> {
                            if (progressBar != null)
                                progressBar.setVisibility(View.GONE);
                            sendImageMessage(imageUrl);
                        });

                    } catch (Exception ignored) {}
                }
            });

        } catch (Exception e) {
            if (progressBar != null)
                progressBar.setVisibility(View.GONE);
        }
    }

    private byte[] readBytes(InputStream is) throws Exception {
        ByteArrayOutputStream buffer =
                new ByteArrayOutputStream();
        byte[] data = new byte[16384];
        int n;
        while ((n = is.read(data)) != -1)
            buffer.write(data, 0, n);
        return buffer.toByteArray();
    }
}