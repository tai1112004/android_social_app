package com.yourapp.user;

import com.yourapp.friendship.repository.FriendshipRepository;
import com.yourapp.post.repository.PostRepository;
import com.yourapp.story.repository.StoryRepository;
import com.yourapp.user.dto.PublicProfileDto;
import com.yourapp.user.entity.User;
import com.yourapp.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * User search and profile endpoints.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final PostRepository postRepository;
    private final StoryRepository storyRepository;

    public UserController(UserRepository userRepository,
                          FriendshipRepository friendshipRepository,
                          PostRepository postRepository,
                          StoryRepository storyRepository) {
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.postRepository = postRepository;
        this.storyRepository = storyRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        User current = currentUser();
        return ResponseEntity.ok(ok("OK", toPrivateProfileMap(current)));
    }

    @GetMapping("/me/profile")
    public ResponseEntity<Map<String, Object>> meProfile() {
        return me();
    }

    @PutMapping("/me")
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody Map<String, String> body) {
        User current = currentUser();
        applyProfileUpdates(current, body);
        current.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(current);
        return ResponseEntity.ok(ok("Profile updated successfully", toPrivateProfileMap(saved)));
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(@RequestParam String q) {
        if (q == null || q.trim().length() < 2) {
            return ResponseEntity.badRequest().body(error("Query too short"));
        }
        Long currentId = currentUser().getId();
        List<Map<String, Object>> results = userRepository
                .findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(q.trim(), q.trim())
                .stream()
                .filter(u -> !u.getId().equals(currentId))
                .map(u -> toPublicProfileMap(u, currentId))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ok("OK", results));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> publicProfile(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Long currentId = currentUser().getId();
        PublicProfileDto profile = buildPublicProfileDto(user, currentId);
        return ResponseEntity.ok(ok("OK", profile));
    }

    private void applyProfileUpdates(User current, Map<String, String> body) {
        String displayName = body.get("displayName");
        String avatarUrl = body.get("avatarUrl");
        String coverUrl = body.get("coverUrl");
        String bio = body.get("bio");
        String location = body.get("location");
        String website = body.get("website");
        String dateOfBirth = body.get("dateOfBirth");
        String gender = body.get("gender");
        String phone = body.get("phone");
        String email = body.get("email");

        if (displayName != null && !displayName.trim().isEmpty()) {
            current.setDisplayName(displayName.trim());
        }
        if (avatarUrl != null) {
            current.setAvatarUrl(normalize(avatarUrl));
        }
        if (coverUrl != null) {
            current.setCoverUrl(normalize(coverUrl));
        }
        if (bio != null) {
            current.setBio(normalize(bio));
        }
        if (location != null) {
            current.setLocation(normalize(location));
        }
        if (website != null) {
            current.setWebsite(normalize(website));
        }
    }

    private Map<String, Object> toPrivateProfileMap(User user) {
        Map<String, Object> data = toPublicProfileMap(user, user.getId());
        data.put("email", user.getEmail());
        data.put("updatedAt", user.getUpdatedAt());
        data.put("createdAt", user.getCreatedAt());
        return data;
    }

    private Map<String, Object> toPublicProfileMap(User user, Long viewerId) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("displayName", user.getDisplayName());
        data.put("avatarUrl", user.getAvatarUrl());
        data.put("coverUrl", user.getCoverUrl());
        data.put("bio", user.getBio());
        data.put("location", user.getLocation());
        data.put("website", user.getWebsite());
        data.put("dateOfBirth", user.getDateOfBirth());
        data.put("gender", user.getGender());
        data.put("phone", user.getPhone());
        data.put("lastSeenAt", user.getLastSeenAt());
        data.put("friendshipStatus", friendshipStatus(viewerId, user.getId()));
        data.put("mutualFriendCount", mutualFriendCount(viewerId, user.getId()));
        data.put("friendCount", friendCount(user.getId()));
        data.put("postCount", postRepository.countByUserId(user.getId()));
        data.put("storyCount", storyRepository.countByUserIdAndExpiresAtAfter(user.getId(), LocalDateTime.now()));
        data.put("owner", user.getId().equals(viewerId));
        return data;
    }

    private PublicProfileDto buildPublicProfileDto(User user, Long viewerId) {
        String status = friendshipStatus(viewerId, user.getId());
        return new PublicProfileDto(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getCoverUrl(),
                user.getBio(),
                user.getLocation(),
                user.getWebsite(),
                user.getDateOfBirth(),
                user.getGender(),
                user.getPhone(),
                user.getLastSeenAt(),
                status,
                mutualFriendCount(viewerId, user.getId()),
                friendCount(user.getId()),
                (int) postRepository.countByUserId(user.getId()),
                (int) storyRepository.countByUserIdAndExpiresAtAfter(user.getId(), LocalDateTime.now()),
                user.getId().equals(viewerId)
        );
    }

    private String friendshipStatus(Long viewerId, Long targetId) {
        if (viewerId != null && viewerId.equals(targetId)) {
            return "ME";
        }
        return friendshipRepository.findBetween(viewerId, targetId)
                .map(f -> f.getStatus().name())
                .orElse(null);
    }

    private int friendCount(Long userId) {
        return friendshipRepository.findAcceptedFriendUserIds(userId).size();
    }

    private int mutualFriendCount(Long viewerId, Long targetId) {
        HashSet<Long> viewerFriends = new HashSet<>(friendshipRepository.findAcceptedFriendUserIds(viewerId));
        HashSet<Long> targetFriends = new HashSet<>(friendshipRepository.findAcceptedFriendUserIds(targetId));
        viewerFriends.retainAll(targetFriends);
        return viewerFriends.size();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<String, Object> ok(String msg, Object data) {
        Map<String, Object> r = new HashMap<>();
        r.put("success", true);
        r.put("message", msg);
        r.put("data", data);
        r.put("error", null);
        return r;
    }

    private Map<String, Object> error(String msg) {
        Map<String, Object> r = new HashMap<>();
        r.put("success", false);
        r.put("message", msg);
        r.put("data", null);
        r.put("error", msg);
        return r;
    }

    private User currentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}