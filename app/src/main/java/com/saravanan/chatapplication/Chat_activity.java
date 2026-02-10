package com.saravanan.chatapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.HashMap;
import java.util.Map;

import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;

public class Chat_activity extends AppCompatActivity {

    // ---------------- UI ----------------
    TextView txtChatUser, txtStatus;
    EditText chatbox;
    Button sendbutton;
    ImageButton btnAudioCall, btnEndCall;
    LinearLayout chatContainer;
    ScrollView scrollView;
    MenuItem blockItem, unblockItem;

    // ---------------- Firebase ----------------
    FirebaseAuth auth;
    FirebaseFirestore db;

    String myUid, otherUserId, receiverUsername, chatId;
    boolean isBlocked = false;

    // ---------------- Agora ----------------
    private static final String APP_ID = "3fbec1ca02c34c1692f1606e1da37c6f";
    private static final int MIC_REQ = 101;
    private RtcEngine rtcEngine;
    private boolean isInCall = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "push_channel",
                    "Chat Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Chat message alerts");

            NotificationManager manager =
                    getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

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

        // UI bind
        txtChatUser = findViewById(R.id.txtChatUser);
        txtStatus = findViewById(R.id.txtStatus);
        chatbox = findViewById(R.id.chatbox);
        sendbutton = findViewById(R.id.sendbutton);
        btnAudioCall = findViewById(R.id.audiocall);
        btnEndCall = findViewById(R.id.video_call);
        chatContainer = findViewById(R.id.chatContainer);
        scrollView = findViewById(R.id.scroll);

        txtChatUser.setText(receiverUsername);

        sendbutton.setOnClickListener(v -> sendMessage());

        checkBlockStatus();
        listenMessages();

        btnAudioCall.setOnClickListener(v -> {
            Map<String, Object> call = new HashMap<>();
            call.put("callerId", myUid);
            call.put("receiverId", otherUserId);
            call.put("status", "ringing");

            db.collection("calls")
                    .document(chatId)
                    .set(call);

            Intent intent = new Intent(Chat_activity.this, AudiocallActivity.class);
            intent.putExtra("receiverUsername", receiverUsername);
            intent.putExtra("receiverUid", otherUserId);
            startActivity(intent);
        });
    }

    // ---------------- SEND MESSAGE ----------------
    private void sendMessage() {
        if (isBlocked) return;

        String msg = chatbox.getText().toString().trim();
        if (msg.isEmpty()) return;

        Map<String, Object> map = new HashMap<>();
        map.put("senderUid", myUid);
        map.put("receiverUid", otherUserId);
        map.put("senderName", "Saravanan");
        map.put("text", msg);
        map.put("time", System.currentTimeMillis());
        map.put("deleted", false);   // 🔥 DELETE FEATURE ADD

        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(map);

        chatbox.setText("");
    }

    // ---------------- LISTEN + SHOW MESSAGES ----------------
    private void listenMessages() {

        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("time", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {

                    if (error != null || value == null) return;

                    chatContainer.removeAllViews();

                    for (DocumentSnapshot doc : value.getDocuments()) {

                        String messageId = doc.getId();
                        String text = doc.getString("text");
                        String senderUid = doc.getString("senderUid");
                        Boolean deleted = doc.getBoolean("deleted");

                        TextView tv = new TextView(this);

                        if (deleted != null && deleted) {
                            tv.setText("This message was deleted");
                            tv.setTextColor(0xFF888888);
                        } else {
                            tv.setText(text);
                        }

                        tv.setTextSize(16);
                        tv.setPadding(20, 10, 20, 10);

                        if (senderUid.equals(myUid)) {
                            tv.setBackgroundColor(0xFFD1FFC6);
                            tv.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
                        } else {
                            tv.setBackgroundColor(0xFFFFFFFF);
                            tv.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                        }

                        // 🔥 LONG PRESS DELETE
                        tv.setOnLongClickListener(v -> {

                            if (!senderUid.equals(myUid)) {
                                Toast.makeText(Chat_activity.this,
                                        "You can delete only your message",
                                        Toast.LENGTH_SHORT).show();
                                return true;
                            }

                            new AlertDialog.Builder(Chat_activity.this)
                                    .setTitle("Delete message")
                                    .setMessage("Delete this message for everyone?")
                                    .setPositiveButton("Delete", (d, w) -> {
                                        deleteMessageForEveryone(messageId);
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();

                            return true;
                        });

                        chatContainer.addView(tv);
                    }

                    scrollView.post(() ->
                            scrollView.fullScroll(View.FOCUS_DOWN));
                });
    }

    // ---------------- DELETE MESSAGE ----------------
    private void deleteMessageForEveryone(String messageId) {

        Map<String, Object> update = new HashMap<>();
        update.put("text", "This message was deleted");
        update.put("deleted", true);

        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .update(update);
    }

    // ---------------- BLOCK LOGIC (UNCHANGED) ----------------
    private void checkBlockStatus() {
        db.collection("blocks")
                .whereEqualTo("blockerId", myUid)
                .whereEqualTo("blockedId", otherUserId)
                .get()
                .addOnSuccessListener(qs -> {
                    isBlocked = !qs.isEmpty();
                    updateUI();
                });
    }

    private void updateUI() {
        if (blockItem == null || unblockItem == null) return;

        if (isBlocked) {
            blockItem.setVisible(false);
            unblockItem.setVisible(true);
            chatbox.setEnabled(false);
            sendbutton.setEnabled(false);
            btnAudioCall.setEnabled(false);
            chatbox.setHint("You blocked this user");
        } else {
            blockItem.setVisible(true);
            unblockItem.setVisible(false);
            chatbox.setEnabled(true);
            sendbutton.setEnabled(true);
            btnAudioCall.setEnabled(true);
            chatbox.setHint("Type a message");
        }
    }
}
