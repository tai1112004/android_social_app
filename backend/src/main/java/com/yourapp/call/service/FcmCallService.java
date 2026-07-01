package com.yourapp.call.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.yourapp.user.entity.User;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FcmCallService {

    private static final Logger log = LoggerFactory.getLogger(FcmCallService.class);

    public void sendIncomingCallNotification(User callee, User caller, String sessionId) {
        sendDataMessage(callee, buildIncomingPayload(caller, sessionId));
    }

    public void sendCallEndedNotification(User callee, String sessionId) {
        Map<String, String> data = new HashMap<>();
        data.put("type", "CALL_ENDED");
        data.put("callSessionId", sessionId);
        sendDataMessage(callee, data);
    }

    private void sendDataMessage(User target, Map<String, String> data) {
        if (target == null || target.getFcmToken() == null || target.getFcmToken().isBlank()) {
            return;
        }
        if (FirebaseApp.getApps().isEmpty()) {
            log.info("Firebase chua duoc cau hinh, bo qua FCM cho user {}", target.getId());
            return;
        }
        try {
            Message message = Message.builder()
                    .setToken(target.getFcmToken())
                    .putAllData(data)
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setTtl(30_000L)
                            .build())
                    .build();
            FirebaseMessaging.getInstance().sendAsync(message);
        } catch (Exception ex) {
            log.warn("Khong the gui FCM cuoc goi", ex);
        }
    }

    private Map<String, String> buildIncomingPayload(User caller, String sessionId) {
        Map<String, String> data = new HashMap<>();
        data.put("type", "INCOMING_CALL");
        data.put("callerId", caller != null && caller.getId() != null ? String.valueOf(caller.getId()) : "");
        data.put("callerName", displayName(caller));
        data.put("callerAvatar", caller != null && caller.getAvatarUrl() != null ? caller.getAvatarUrl() : "");
        data.put("callSessionId", sessionId);
        return data;
    }

    private String displayName(User user) {
        if (user == null) {
            return "Ai do";
        }
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName();
        }
        return user.getUsername() != null ? user.getUsername() : "Ai do";
    }
}
