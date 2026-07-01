package com.yourapp.friendship;

import com.yourapp.friendship.dto.FriendDto;
import com.yourapp.friendship.dto.FriendSuggestionDto;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/friends")
public class FriendshipController {

    private final FriendshipService friendshipService;

    public FriendshipController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> sendRequest(@RequestParam Long userId) {
        Long currentUserId = getCurrentUserId();
        FriendDto dto = friendshipService.sendRequest(currentUserId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ok("Friend request sent", dto));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listFriends() {
        List<FriendDto> friends = friendshipService.listFriends(getCurrentUserId());
        return ResponseEntity.ok(ok("OK", friends));
    }

    @GetMapping("/requests/incoming")
    public ResponseEntity<Map<String, Object>> incomingRequests() {
        List<FriendDto> list = friendshipService.listPendingIncoming(getCurrentUserId());
        return ResponseEntity.ok(ok("OK", list));
    }

    @GetMapping("/requests/sent")
    public ResponseEntity<Map<String, Object>> sentRequests() {
        List<FriendDto> list = friendshipService.listPendingSent(getCurrentUserId());
        return ResponseEntity.ok(ok("OK", list));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<Map<String, Object>> suggestions() {
        List<FriendSuggestionDto> list = friendshipService.suggestFriends(getCurrentUserId());
        return ResponseEntity.ok(ok("OK", list));
    }

    @PutMapping("/{friendshipId}/accept")
    public ResponseEntity<Map<String, Object>> accept(@PathVariable Long friendshipId) {
        FriendDto dto = friendshipService.accept(getCurrentUserId(), friendshipId);
        return ResponseEntity.ok(ok("Friend request accepted", dto));
    }

    @PutMapping("/{friendshipId}/block")
    public ResponseEntity<Map<String, Object>> block(@PathVariable Long friendshipId) {
        friendshipService.block(getCurrentUserId(), friendshipId);
        return ResponseEntity.ok(ok("Blocked", null));
    }

    @DeleteMapping("/{friendshipId}")
    public ResponseEntity<Map<String, Object>> reject(@PathVariable Long friendshipId) {
        friendshipService.reject(getCurrentUserId(), friendshipId);
        return ResponseEntity.ok(ok("Removed", null));
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
