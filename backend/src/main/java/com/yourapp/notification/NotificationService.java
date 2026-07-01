package com.yourapp.notification;

import com.yourapp.notification.dto.NotificationDto;
import com.yourapp.user.entity.User;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendToUser(Long userId, String type, String title, String message, User actor, Map<String, Object> data) {
        if (userId == null) return;
        messagingTemplate.convertAndSend("/topic/users/" + userId + "/notifications",
                build(type, title, message, actor, data));
    }

    public void sendToUsers(Collection<Long> userIds, String type, String title, String message, User actor, Map<String, Object> data) {
        if (userIds == null) return;
        for (Long userId : userIds) {
            if (actor != null && actor.getId() != null && actor.getId().equals(userId)) {
                continue;
            }
            sendToUser(userId, type, title, message, actor, data);
        }
    }

    public Map<String, Object> data(Object... pairs) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            if (pairs[i] != null) {
                map.put(String.valueOf(pairs[i]), pairs[i + 1]);
            }
        }
        return map;
    }

    private NotificationDto build(String type, String title, String message, User actor, Map<String, Object> data) {
        return new NotificationDto(
                UUID.randomUUID().toString(),
                type,
                title,
                message,
                actor != null ? actor.getId() : null,
                actor != null ? actor.getUsername() : null,
                actor != null ? actor.getDisplayName() : null,
                actor != null ? actor.getAvatarUrl() : null,
                LocalDateTime.now(),
                data
        );
    }
}