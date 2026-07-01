package com.yourapp.conversation;

import com.yourapp.conversation.dto.ConversationDto;
import com.yourapp.conversation.dto.ConversationInfoDto;
import com.yourapp.conversation.dto.CreateConversationRequest;
import com.yourapp.user.entity.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for conversation endpoints.
 * All endpoints require a valid JWT (enforced by SecurityConfig — any request not under
 * /api/auth/** must be authenticated).
 */
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    /**
     * GET /api/conversations
     * Returns all conversations the authenticated user belongs to.
     * Response format: { success, message, data: [...], error }
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getConversations() {
        // Retrieve authenticated user from SecurityContext (set by JwtAuthenticationFilter)
        User currentUser = (User) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        Long userId = currentUser.getId();
        List<ConversationDto> conversations = conversationService.getConversationsForUser(userId);

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("message", "OK");
        body.put("data", conversations);
        body.put("error", null);

        return ResponseEntity.ok(body);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createConversation(
            @RequestBody CreateConversationRequest request) {
        User currentUser = (User) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        ConversationDto conversation = conversationService.createConversation(currentUser.getId(), request);

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("message", "Conversation created");
        body.put("data", conversation);
        body.put("error", null);

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/{conversationId}/read")
    public ResponseEntity<Map<String, Object>> markConversationRead(@PathVariable Long conversationId) {
        User currentUser = (User) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        conversationService.markConversationRead(currentUser.getId(), conversationId);

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("message", "Conversation marked as read");
        body.put("data", null);
        body.put("error", null);

        return ResponseEntity.ok(body);
    }
    @GetMapping("/{conversationId}/info")
    public ResponseEntity<Map<String, Object>> getConversationInfo(@PathVariable Long conversationId) {
        User currentUser = (User) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        ConversationInfoDto info = conversationService.getConversationInfo(currentUser.getId(), conversationId);

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("message", "OK");
        body.put("data", info);
        body.put("error", null);

        return ResponseEntity.ok(body);
    }
}
