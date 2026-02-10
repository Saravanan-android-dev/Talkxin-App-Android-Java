package com.saravanan.chatapplication;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
public class message_servic extends FirebaseMessagingService {

        @Override
        public void onMessageReceived(@NonNull RemoteMessage message) {

            String title = message.getNotification() != null
                    ? message.getNotification().getTitle()
                    : "New Message";

            String body = message.getNotification() != null
                    ? message.getNotification().getBody()
                    : "";

            Intent intent = new Intent(this, Chat_activity.class);
            intent.putExtra("chatId", message.getData().get("chatId"));
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE
            );

            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(this, "push_channel")
                            .setSmallIcon(R.drawable.baseline_notifications_active_24)
                            .setContentTitle(title)
                            .setContentText(body)
                            .setAutoCancel(true)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setContentIntent(pendingIntent);

            NotificationManager manager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    @Override
    public void onNewToken(@NonNull String token) {
        Log.d("FCM_TOKEN", token);
    }

}


