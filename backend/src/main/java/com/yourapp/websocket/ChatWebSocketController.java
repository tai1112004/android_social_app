package com.yourapp.websocket;

import com.yourapp.message.MessageService;
import com.yourapp.message.dto.MessageDto;
import com.yourapp.user.entity.User;
import com.yourapp.websocket.dto.ChatMessageRequest;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class ChatWebSocketController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(MessageService messageService,
                                   SimpMessagingTemplate messagingTemplate) {
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.send")
    public void send(ChatMessageRequest request, Principal principal) {
        User user = extractUser(principal);
        if (request == null || request.getConversationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conversationId is required");
        }
        if (!StringUtils.hasText(request.getContent()) && !StringUtils.hasText(request.getMediaUrl())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content or mediaUrl is required");
        }

        MessageDto saved = messageService.sendMessage(
                user.getId(),
                request.getConversationId(),
                request.toSendMessageRequest());
        messagingTemplate.convertAndSend("/topic/messages/" + request.getConversationId(), saved);
    }

    private User extractUser(Principal principal) {
        if (principal instanceof User user) {
            return user;
        }
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof User user) {
            return user;
        }
        throw new AccessDeniedException("Unauthenticated websocket connection");
    }
}
