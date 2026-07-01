package com.yourapp.call;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.yourapp.R;

public class CallForegroundService extends Service {

    public static final String ACTION_START = "com.yourapp.call.ACTION_START";
    public static final String ACTION_STOP = "com.yourapp.call.ACTION_STOP";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_STATUS = "extra_status";
    public static final int NOTIFICATION_ID = 2401;
    private static final String CHANNEL_ID = "call_foreground_channel";

    public static void start(Context context, String title, String status) {
        Intent intent = new Intent(context, CallForegroundService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_STATUS, status);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, CallForegroundService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        ensureChannel();
        String title = intent != null ? intent.getStringExtra(EXTRA_TITLE) : "Dang trong cuoc goi";
        String status = intent != null ? intent.getStringExtra(EXTRA_STATUS) : "Dang ket noi";

        Intent stopIntent = new Intent(this, CallForegroundService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                0,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.sym_call_outgoing)
                .setContentTitle(title)
                .setContentText(status)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addAction(new NotificationCompat.Action(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        "Ket thuc",
                        stopPendingIntent));

        startForeground(NOTIFICATION_ID, builder.build());
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null && manager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Call foreground",
                    NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }
    }
}
