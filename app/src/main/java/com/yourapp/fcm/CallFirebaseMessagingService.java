package com.yourapp.fcm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.yourapp.R;
import com.yourapp.call.CallForegroundService;
import com.yourapp.data.remote.UserApiService;
import com.yourapp.model.ApiResponse;
import com.yourapp.network.RetrofitClient;
import com.yourapp.ui.call.IncomingCallActivity;
import com.yourapp.util.TokenManager;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CallFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "incoming_call_channel";
    private static final int NOTIFICATION_ID = 3411;

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        TokenManager tokenManager = new TokenManager(this);
        if (!tokenManager.hasValidTokens()) {
            return;
        }
        UserApiService userApiService = RetrofitClient.getInstance(tokenManager).create(UserApiService.class);
        Map<String, String> body = new HashMap<>();
        body.put("token", token);
        userApiService.updateFcmToken(body).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) { }
            @Override public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) { }
        });
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);
        Map<String, String> data = message.getData();
        if (data == null || data.isEmpty()) {
            return;
        }
        String type = data.get("type");
        if ("INCOMING_CALL".equals(type)) {
            showIncomingCall(data);
        } else if ("CALL_ENDED".equals(type)) {
            NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
            CallForegroundService.stop(this);
        }
    }

    private void showIncomingCall(Map<String, String> data) {
        ensureChannel();

        Intent intent = new Intent(this, IncomingCallActivity.class);
        intent.putExtra(IncomingCallActivity.EXTRA_SESSION_ID, data.get("callSessionId"));
        if (data.containsKey("callerId")) {
            try {
                intent.putExtra(IncomingCallActivity.EXTRA_CALLER_ID, Long.parseLong(data.get("callerId")));
            } catch (NumberFormatException ignored) { }
        }
        intent.putExtra(IncomingCallActivity.EXTRA_CALLER_NAME, data.get("callerName"));
        intent.putExtra(IncomingCallActivity.EXTRA_CALLER_AVATAR, data.get("callerAvatar"));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent openPendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(data.getOrDefault("callerName", "Cuoc goi den"))
                .setContentText("Ban co mot cuoc goi moi")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(openPendingIntent, true)
                .setContentIntent(openPendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build());
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null && manager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Incoming calls",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Thong bao cuoc goi den");
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
            manager.createNotificationChannel(channel);
        }
    }
}
