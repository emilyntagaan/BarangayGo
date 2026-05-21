package com.example.barangaygo.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.barangaygo.R;
import com.example.barangaygo.activities.MainActivity;

public class NotificationHelper {

    public static final String CHANNEL_QUEUE    = "channel_queue";
    public static final String CHANNEL_ANNOUNCE = "channel_announce";

    private static int nextId = 2000;

    /** Call once at app startup (e.g. in MainActivity / AdminActivity onCreate). */
    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = context.getSystemService(NotificationManager.class);

            nm.createNotificationChannel(new NotificationChannel(
                CHANNEL_QUEUE,
                "Queue Updates",
                NotificationManager.IMPORTANCE_HIGH));

            nm.createNotificationChannel(new NotificationChannel(
                CHANNEL_ANNOUNCE,
                "Announcements",
                NotificationManager.IMPORTANCE_DEFAULT));
        }
    }

    public static void showQueueNotification(Context context, String title, String body) {
        show(context, CHANNEL_QUEUE, title, body);
    }

    public static void showAnnouncementNotification(Context context, String title, String body) {
        show(context, CHANNEL_ANNOUNCE, title, body);
    }

    private static void show(Context context, String channel, String title, String body) {
        NotificationManager nm =
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_barangay_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH);

        nm.notify(nextId++, builder.build());
    }
}