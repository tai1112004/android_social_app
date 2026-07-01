package com.yourapp.story;

import com.yourapp.friendship.repository.FriendshipRepository;
import com.yourapp.notification.NotificationService;
import com.yourapp.story.dto.StoryDto;
import com.yourapp.story.dto.StoryGroupDto;
import com.yourapp.story.dto.StoryViewerDto;
import com.yourapp.story.entity.Story;
import com.yourapp.story.entity.StoryView;
import com.yourapp.story.repository.StoryRepository;
import com.yourapp.story.repository.StoryViewRepository;
import com.yourapp.user.entity.User;
import com.yourapp.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StoryService {
    private final StoryRepository storyRepository;
    private final StoryViewRepository storyViewRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final NotificationService notificationService;

    public StoryService(StoryRepository storyRepository,
                        StoryViewRepository storyViewRepository,
                        UserRepository userRepository,
                        FriendshipRepository friendshipRepository,
                        NotificationService notificationService) {
        this.storyRepository = storyRepository;
        this.storyViewRepository = storyViewRepository;
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public StoryDto createStory(Long currentUserId, Map<String, String> body) {
        User current = currentUser(currentUserId);
        String mediaUrl = body != null ? body.get("mediaUrl") : null;
        String content = body != null ? body.get("content") : null;
        if ((mediaUrl == null || mediaUrl.isEmpty()) && (content == null || content.trim().isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Media or content is required");
        }

        Story story = new Story();
        story.setUser(current);
        story.setMediaUrl(mediaUrl);
        story.setContent(content);
        story.setMusicTitle(clean(body != null ? body.get("musicTitle") : null));
        story.setMusicArtist(clean(body != null ? body.get("musicArtist") : null));
        story.setMusicPreviewUrl(clean(body != null ? body.get("musicPreviewUrl") : null));
        story.setMusicAlbumCover(clean(body != null ? body.get("musicAlbumCover") : null));
        story.setMusicDeezerTrackId(clean(body != null ? body.get("musicDeezerTrackId") : null));
        Story saved = storyRepository.save(story);

        List<Long> friendIds = friendshipRepository.findAcceptedFriendUserIds(current.getId());
        notificationService.sendToUsers(friendIds, "NEW_STORY",
                displayName(current) + " vua dang mot tin moi",
                "Bam de xem tin cua " + displayName(current),
                current,
                notificationService.data("storyId", saved.getId(), "mediaUrl", saved.getMediaUrl()));

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<StoryGroupDto> getFeedGrouped(Long currentUserId) {
        List<Long> friendIds = friendshipRepository.findAcceptedFriendUserIds(currentUserId);
        List<Long> authorIds = new ArrayList<>(friendIds);
        authorIds.add(currentUserId);

        List<Story> stories = storyRepository.findActiveByUserIds(authorIds, LocalDateTime.now());
        Map<Long, List<Story>> grouped = stories.stream()
                .collect(Collectors.groupingBy(s -> s.getUser().getId(), LinkedHashMap::new, Collectors.toList()));

        List<StoryGroupDto> result = new ArrayList<>();
        for (Map.Entry<Long, List<Story>> entry : grouped.entrySet()) {
            List<Story> groupStories = new ArrayList<>(entry.getValue());
            groupStories.sort(Comparator.comparing(Story::getCreatedAt));
            Story first = groupStories.get(0);
            User author = first.getUser();

            boolean hasUnviewed = groupStories.stream()
                    .anyMatch(story -> !storyViewRepository.existsByStoryIdAndViewerId(story.getId(), currentUserId));

            result.add(new StoryGroupDto(
                    author.getId(),
                    displayName(author),
                    author.getAvatarUrl(),
                    hasUnviewed,
                    groupStories.stream().map(this::toDto).collect(Collectors.toList()),
                    groupStories.get(groupStories.size() - 1).getCreatedAt()
            ));
        }

        result.sort((a, b) -> {
            if (a.isHasUnviewed() != b.isHasUnviewed()) {
                return a.isHasUnviewed() ? -1 : 1;
            }
            LocalDateTime at = a.getLatestStoryTime();
            LocalDateTime bt = b.getLatestStoryTime();
            if (at == null && bt == null) return 0;
            if (at == null) return 1;
            if (bt == null) return -1;
            return bt.compareTo(at);
        });

        return result;
    }

    @Transactional
    public void markViewed(Long currentUserId, Long storyId) {
        User viewer = currentUser(currentUserId);
        Story story = storyRepository.findByIdAndExpiresAtAfter(storyId, LocalDateTime.now())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Story not found or expired"));

        if (story.getUser().getId().equals(currentUserId)) {
            return;
        }

        StoryView view = storyViewRepository.findByStoryIdAndViewerId(storyId, currentUserId)
                .orElseGet(StoryView::new);
        view.setStory(story);
        view.setViewer(viewer);
        view.setViewedAt(LocalDateTime.now());
        storyViewRepository.save(view);
    }

    @Transactional(readOnly = true)
    public List<StoryViewerDto> getViewers(Long currentUserId, Long storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Story not found"));
        if (!story.getUser().getId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot view viewers of this story");
        }
        return storyViewRepository.findByStoryIdOrderByViewedAtDesc(storyId).stream()
                .map(view -> {
                    User viewer = view.getViewer();
                    return new StoryViewerDto(
                            viewer.getId(),
                            viewer.getUsername(),
                            displayName(viewer),
                            viewer.getAvatarUrl(),
                            view.getViewedAt()
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteStory(Long currentUserId, Long storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Story not found"));
        if (!story.getUser().getId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot delete this story");
        }
        storyRepository.delete(story);
    }

    private StoryDto toDto(Story s) {
        User author = s.getUser();
        return new StoryDto(
                s.getId(),
                author.getId(),
                author.getUsername(),
                author.getDisplayName(),
                author.getAvatarUrl(),
                s.getMediaUrl(),
                s.getContent(),
                s.getMusicTitle(),
                s.getMusicArtist(),
                s.getMusicPreviewUrl(),
                s.getMusicAlbumCover(),
                s.getMusicDeezerTrackId(),
                s.getCreatedAt(),
                s.getExpiresAt()
        );
    }

    private String clean(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private User currentUser(Long currentUserId) {
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
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
