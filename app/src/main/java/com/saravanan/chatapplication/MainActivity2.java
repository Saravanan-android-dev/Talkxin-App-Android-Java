package com.saravanan.chatapplication;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
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
    static final int VIDEO_GALLERY_REQ = 203;
    static final int VIDEO_CAMERA_REQ = 204;
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
        attachBtn.setOnClickListener(v -> showPickerDialog());

        listenMessages();
    }

    // ================= TEXT =================
    private void sendMessage() {

        String msg = chatbox.getText().toString().trim();
        if (msg.isEmpty()) return;

        Map<String, Object> map = new HashMap<>();
        map.put("senderUid", myUid);
        map.put("receiverUid", otherUserId);
        map.put("text", msg);
        map.put("imageUrl", "");
        map.put("videoUrl", "");
        map.put("time", System.currentTimeMillis());

        db.collection("chats").document(chatId)
                .collection("messages").add(map);

        chatbox.setText("");
    }

    // ================= IMAGE =================
    private void sendImageMessage(String imageUrl) {

        Map<String, Object> map = new HashMap<>();
        map.put("senderUid", myUid);
        map.put("receiverUid", otherUserId);
        map.put("text", "");
        map.put("imageUrl", imageUrl);
        map.put("videoUrl", "");
        map.put("time", System.currentTimeMillis());

        db.collection("chats").document(chatId)
                .collection("messages").add(map);
    }

    // ================= VIDEO =================
    private void sendVideoMessage(String videoUrl) {

        Map<String, Object> map = new HashMap<>();
        map.put("senderUid", myUid);
        map.put("receiverUid", otherUserId);
        map.put("text", "");
        map.put("imageUrl", "");
        map.put("videoUrl", videoUrl);
        map.put("time", System.currentTimeMillis());

        db.collection("chats").document(chatId)
                .collection("messages").add(map);
    }

    // ================= LISTEN =================
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
                        String videoUrl = doc.getString("videoUrl");
                        String senderUid = doc.getString("senderUid");

                        View messageView = getLayoutInflater().inflate(
                                senderUid.equals(myUid)
                                        ? R.layout.item_sender
                                        : R.layout.item_receiver,
                                chatContainer,
                                false
                        );

                        TextView txtMessage = messageView.findViewById(R.id.txtMessage);
                        ImageView imageMessage = messageView.findViewById(R.id.imageMessage);
                        VideoView videoView = messageView.findViewById(R.id.videoMessage);
                        TextView txtTime = messageView.findViewById(R.id.txtTime);

                        if (text != null && !text.isEmpty()) {
                            txtMessage.setVisibility(View.VISIBLE);
                            txtMessage.setText(text);
                        } else {
                            txtMessage.setVisibility(View.GONE);
                        }

                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            imageMessage.setVisibility(View.VISIBLE);
                            Glide.with(this).load(imageUrl).into(imageMessage);
                        } else {
                            imageMessage.setVisibility(View.GONE);
                        }

                        if (videoUrl != null && !videoUrl.isEmpty()) {

                            videoView.setVisibility(View.VISIBLE);

                            MediaController mediaController =
                                    new MediaController(this);
                            mediaController.setAnchorView(videoView);
                            videoView.setMediaController(mediaController);

                            videoView.setVideoURI(Uri.parse(videoUrl));

                            videoView.setOnPreparedListener(mp -> {
                                mp.setLooping(false);
                                videoView.start();
                            });

                        } else {
                            videoView.setVisibility(View.GONE);
                        }

                        Long timeMillis = doc.getLong("time");
                        if (timeMillis != null) {
                            txtTime.setText(android.text.format.DateFormat
                                    .format("hh:mm a", timeMillis));
                        }

                        // DELETE
                        messageView.setOnLongClickListener(v -> {

                            if (senderUid != null && senderUid.equals(myUid)) {

                                new AlertDialog.Builder(MainActivity2.this)
                                        .setTitle("Delete Message")
                                        .setMessage("Delete this message?")
                                        .setPositiveButton("Delete",
                                                (d, w) -> doc.getReference().delete())
                                        .setNegativeButton("Cancel", null)
                                        .show();
                            }
                            return true;
                        });

                        chatContainer.addView(messageView);
                    }

                    scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                });
    }

    // ================= PICKER =================
    private void showPickerDialog() {

        String[] options = {
                "Photo Camera",
                "Gallery Image",
                "Video Camera",
                "Video Gallery"
        };

        new AlertDialog.Builder(this)
                .setTitle("Select")
                .setItems(options, (dialog, which) -> {

                    if (which == 0) openCamera();
                    else if (which == 1) openGallery();
                    else if (which == 2) openVideoCamera();
                    else openVideoGallery();
                })
                .show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, GALLERY_REQ);
    }

    private void openVideoGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("video/*");
        startActivityForResult(intent, VIDEO_GALLERY_REQ);
    }

    private void openVideoCamera() {

        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE);
            return;
        }

        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 30);
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);

        startActivityForResult(intent, VIDEO_CAMERA_REQ);
    }
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK) return;

        if (requestCode == GALLERY_REQ && data != null) {
            uploadToCloudinary(data.getData(), "image");
        }

        else if (requestCode == VIDEO_GALLERY_REQ && data != null) {
            uploadToCloudinary(data.getData(), "video");
        }

        else if (requestCode == VIDEO_CAMERA_REQ) {

            Uri videoUri = null;

            if (data != null && data.getData() != null) {
                videoUri = data.getData();
            }

            if (videoUri != null) {
                uploadToCloudinary(videoUri, "video");
            } else {
                Toast.makeText(this, "Video capture failed", Toast.LENGTH_SHORT).show();
            }
        }

        else if (requestCode == CAMERA_REQ && cameraImageUri != null) {
            uploadToCloudinary(cameraImageUri, "image");
        }
    }
    // ================= UPLOAD =================
    private void uploadToCloudinary(Uri uri, String type) {

        OkHttpClient client = new OkHttpClient();

        try {
            InputStream inputStream =
                    getContentResolver().openInputStream(uri);
            byte[] bytes = readBytes(inputStream);

            RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file",
                            type.equals("video") ? "video.mp4" : "image.jpg",
                            RequestBody.create(bytes,
                                    MediaType.parse(type + "/*")))
                    .addFormDataPart("upload_preset", UPLOAD_PRESET)
                    .build();

            String uploadUrl = type.equals("video")
                    ? "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/video/upload"
                    : "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";

            Request request = new Request.Builder()
                    .url(uploadUrl)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(Call call, java.io.IOException e) {}

                @Override
                public void onResponse(Call call, Response response)
                        throws java.io.IOException {

                    try {
                        JSONObject json =
                                new JSONObject(response.body().string());
                        String url = json.getString("secure_url");

                        runOnUiThread(() -> {
                            if (type.equals("video"))
                                sendVideoMessage(url);
                            else
                                sendImageMessage(url);
                        });

                    } catch (Exception ignored) {}
                }
            });

        } catch (Exception ignored) {}
    }

    private byte[] readBytes(InputStream is) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[16384];
        int n;
        while ((n = is.read(data)) != -1)
            buffer.write(data, 0, n);
        return buffer.toByteArray();
    }
}