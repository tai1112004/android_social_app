package com.yourapp.websocket;

import com.yourapp.user.entity.User;
import com.yourapp.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketPresenceListener {

    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketPresenceListener(UserRepository userRepository,
                                     SimpMessagingTemplate messagingTemplate) {
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        publishPresence(event, true);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        publishPresence(event, false);
    }

    private void publishPresence(AbstractSubProtocolEvent event, boolean online) {
        User user = resolveUser(event.getUser());
        if (user == null) {
            return;
        }

        user.setLastSeenAt(LocalDateTime.now());
        User saved = userRepository.save(user);

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", saved.getId());
        payload.put("online", online);
        payload.put("lastSeenAt", saved.getLastSeenAt());

        messagingTemplate.convertAndSend("/topic/presence/" + saved.getId(), payload);
    }

    private User resolveUser(java.security.Principal principal) {
        if (principal instanceof User user) {
            return user;
        }
        if (principal instanceof UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }
}
