package com.yourapp.story;

import com.yourapp.story.dto.StoryDto;
import com.yourapp.story.dto.StoryGroupDto;
import com.yourapp.story.dto.StoryViewerDto;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stories")
public class StoryController {

    private final StoryService storyService;

    public StoryController(StoryService storyService) {
        this.storyService = storyService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createStory(@RequestBody Map<String, String> body) {
        User current = currentUser();
        StoryDto story = storyService.createStory(current.getId(), body);
        return ResponseEntity.status(HttpStatus.CREATED).body(ok("Story created", story));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getStories() {
        User current = currentUser();
        List<StoryGroupDto> groups = storyService.getFeedGrouped(current.getId());
        return ResponseEntity.ok(ok("OK", groups));
    }

    @PostMapping("/{storyId}/view")
    public ResponseEntity<Map<String, Object>> markViewed(@PathVariable Long storyId) {
        User current = currentUser();
        storyService.markViewed(current.getId(), storyId);
        return ResponseEntity.ok(ok("Story viewed", null));
    }

    @GetMapping("/{storyId}/viewers")
    public ResponseEntity<Map<String, Object>> getViewers(@PathVariable Long storyId) {
        User current = currentUser();
        List<StoryViewerDto> viewers = storyService.getViewers(current.getId(), storyId);
        return ResponseEntity.ok(ok("OK", viewers));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteStory(@PathVariable Long id) {
        User current = currentUser();
        storyService.deleteStory(current.getId(), id);
        return ResponseEntity.ok(ok("Story deleted", null));
    }

    private User currentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private Map<String, Object> ok(String msg, Object data) {
        Map<String, Object> r = new HashMap<>();
        r.put("success", true);
        r.put("message", msg);
        r.put("data", data);
        r.put("error", null);
        return r;
    }
}
