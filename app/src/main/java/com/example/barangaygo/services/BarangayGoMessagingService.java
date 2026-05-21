package com.example.barangaygo.services;

import com.example.barangaygo.utils.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles FCM token refresh and incoming push messages.
 * Token is saved to Firestore under users/{uid}.fcmToken so the
 * admin (or a Cloud Function) can target specific devices.
 */
public class BarangayGoMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Map<String, Object> update = new HashMap<>();
            update.put("fcmToken", token);
            FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .update(update);
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);
        NotificationHelper.createChannels(this);

        RemoteMessage.Notification notif = message.getNotification();
        if (notif != null) {
            String title = notif.getTitle() != null ? notif.getTitle() : "BarangayGo";
            String body  = notif.getBody()  != null ? notif.getBody()  : "";

            // Route to appropriate channel based on data payload
            String type = message.getData().get("type");
            if ("announcement".equals(type)) {
                NotificationHelper.showAnnouncementNotification(this, title, body);
            } else {
                NotificationHelper.showQueueNotification(this, title, body);
            }
        }
    }
}