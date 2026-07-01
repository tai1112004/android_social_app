package com.yourapp.call;

import com.yourapp.call.entity.CallHistory;
import com.yourapp.call.entity.CallSessionSnapshot;
import com.yourapp.call.repository.CallHistoryRepository;
import com.yourapp.call.service.CallSessionService;
import com.yourapp.user.entity.User;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/calls")
public class CallHistoryController {

    private final CallHistoryRepository callHistoryRepository;
    private final CallSessionService callSessionService;

    public CallHistoryController(CallHistoryRepository callHistoryRepository, CallSessionService callSessionService) {
        this.callHistoryRepository = callHistoryRepository;
        this.callSessionService = callSessionService;
    }

    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> history(@RequestParam(required = false) Long userId,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size,
                                                       Principal principal) {
        User current = extractUser(principal);
        Long targetId = userId != null ? userId : current.getId();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<CallHistory> historyPage = callHistoryRepository
                .findByCallerIdOrCalleeIdOrderByStartTimeDesc(targetId, targetId, pageable);

        Map<String, Object> payload = new HashMap<>();
        payload.put("items", historyPage.getContent());
        payload.put("page", historyPage.getNumber());
        payload.put("size", historyPage.getSize());
        payload.put("totalElements", historyPage.getTotalElements());
        payload.put("totalPages", historyPage.getTotalPages());
        return ResponseEntity.ok(ok("OK", payload));
    }

    @GetMapping("/incoming")
    public ResponseEntity<Map<String, Object>> incoming(Principal principal) {
        User current = extractUser(principal);
        CallSessionSnapshot snapshot = callSessionService.findIncomingRinging(current.getId()).orElse(null);
        return ResponseEntity.ok(ok("OK", snapshot));
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<Map<String, Object>> session(@PathVariable String sessionId, Principal principal) {
        extractUser(principal);
        CallSessionSnapshot snapshot = callSessionService.findSnapshot(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Call session not found"));
        return ResponseEntity.ok(ok("OK", snapshot));
    }

    private User extractUser(Principal principal) {
        if (principal instanceof User user) {
            return user;
        }
        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof User user) {
            return user;
        }
        throw new AccessDeniedException("Unauthenticated request");
    }

    private Map<String, Object> ok(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        response.put("error", null);
        return response;
    }
}
