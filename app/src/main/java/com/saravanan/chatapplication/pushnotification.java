package com.saravanan.chatapplication;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;

public class pushnotification extends FirebaseMessagingService {

        @Override
        public void onNewToken(@NonNull String token) {
            super.onNewToken(token);

            String uid = FirebaseAuth.getInstance().getUid();
            if (uid != null) {
                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .update("fcmToken", token);
            }
        }
    }


