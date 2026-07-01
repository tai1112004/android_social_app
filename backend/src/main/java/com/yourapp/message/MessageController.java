package com.yourapp.message;

import com.yourapp.message.dto.MessageDto;
import com.yourapp.message.dto.ReactionRequest;
import com.yourapp.message.dto.SendMessageRequest;
import com.yourapp.user.entity.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping({
            "/api/conversations/{conversationId}/messages",
            "/api/messages/{conversationId}"
    })
    public ResponseEntity<Map<String, Object>> getMessages(@PathVariable Long conversationId) {
        List<MessageDto> messages = messageService.getMessages(getCurrentUserId(), conversationId);
        return ResponseEntity.ok(ok("OK", messages));
    }

    @PostMapping("/api/conversations/{conversationId}/messages")
    public ResponseEntity<Map<String, Object>> sendMessage(@PathVariable Long conversationId,
                                                           @RequestBody SendMessageRequest request) {
        MessageDto message = messageService.sendMessage(getCurrentUserId(), conversationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ok("Message sent", message));
    }

    @PutMapping("/api/messages/{messageId}/reaction")
    public ResponseEntity<Map<String, Object>> reactToMessage(@PathVariable Long messageId,
                                                              @RequestBody ReactionRequest request) {
        MessageDto message = messageService.reactToMessage(getCurrentUserId(), messageId, request);
        return ResponseEntity.ok(ok("Reaction updated", message));
    }

    @DeleteMapping("/api/messages/{messageId}")
    public ResponseEntity<Map<String, Object>> deleteMessage(@PathVariable Long messageId) {
        messageService.deleteMessage(getCurrentUserId(), messageId);
        return ResponseEntity.ok(ok("Message deleted", null));
    }

    private Long getCurrentUserId() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return user.getId();
    }

    private Map<String, Object> ok(String message, Object data) {
        Map<String, Object> r = new HashMap<>();
        r.put("success", true);
        r.put("message", message);
        r.put("data", data);
        r.put("error", null);
        return r;
    }
}
