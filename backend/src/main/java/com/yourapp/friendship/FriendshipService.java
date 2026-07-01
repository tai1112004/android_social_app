package com.yourapp.friendship;

import com.yourapp.friendship.dto.FriendDto;
import com.yourapp.friendship.dto.FriendSuggestionDto;
import com.yourapp.friendship.entity.Friendship;
import com.yourapp.friendship.entity.Friendship.Status;
import com.yourapp.friendship.repository.FriendshipRepository;
import com.yourapp.notification.NotificationService;
import com.yourapp.user.entity.User;
import com.yourapp.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public FriendshipService(FriendshipRepository friendshipRepository,
                             UserRepository userRepository,
                             NotificationService notificationService) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public FriendDto sendRequest(Long requesterId, Long addresseeId) {
        if (requesterId.equals(addresseeId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot befriend yourself");
        }
        friendshipRepository.findBetween(requesterId, addresseeId).ifPresent(f -> {
            if (f.getStatus() == Status.BLOCKED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Friendship blocked");
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Friendship already exists");
        });

        User requester = userRepository.findById(requesterId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        User addressee = userRepository.findById(addresseeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Friendship f = new Friendship();
        f.setRequester(requester);
        f.setAddressee(addressee);
        f.setStatus(Status.PENDING);
        Friendship saved = friendshipRepository.save(f);

        notificationService.sendToUser(addressee.getId(), "FRIEND_REQUEST", "Loi moi ket ban",
                displayName(requester) + " da gui loi moi ket ban", requester,
                notificationService.data("friendshipId", saved.getId(), "requesterId", requester.getId()));

        return toDto(saved, requesterId);
    }

    @Transactional
    public FriendDto accept(Long currentUserId, Long friendshipId) {
        Friendship f = friendshipRepository.findById(friendshipId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        if (!f.getAddressee().getId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized");
        }
        if (f.getStatus() != Status.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request not pending");
        }
        f.setStatus(Status.ACCEPTED);
        Friendship saved = friendshipRepository.save(f);

        notificationService.sendToUser(saved.getRequester().getId(), "FRIEND_ACCEPTED", "Da tro thanh ban be",
                displayName(saved.getAddressee()) + " da chap nhan loi moi ket ban", saved.getAddressee(),
                notificationService.data("friendshipId", saved.getId(), "friendId", saved.getAddressee().getId()));

        return toDto(saved, currentUserId);
    }

    @Transactional
    public void block(Long currentUserId, Long friendshipId) {
        Friendship f = friendshipRepository.findById(friendshipId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friendship not found"));
        boolean isParty = f.getAddressee().getId().equals(currentUserId)
                       || f.getRequester().getId().equals(currentUserId);
        if (!isParty) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized");
        }
        f.setStatus(Status.BLOCKED);
        friendshipRepository.save(f);
    }

    @Transactional
    public void reject(Long currentUserId, Long friendshipId) {
        Friendship f = friendshipRepository.findById(friendshipId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found"));
        boolean isParty = f.getAddressee().getId().equals(currentUserId)
                       || f.getRequester().getId().equals(currentUserId);
        if (!isParty) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized");
        }
        friendshipRepository.delete(f);
    }

    @Transactional(readOnly = true)
    public List<FriendDto> listFriends(Long userId) {
        return friendshipRepository.findAcceptedFriends(userId).stream()
            .map(f -> toDto(f, userId))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FriendDto> listPendingIncoming(Long userId) {
        return friendshipRepository.findPendingForUser(userId).stream()
            .map(f -> toDto(f, userId))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FriendDto> listPendingSent(Long userId) {
        return friendshipRepository.findSentByUser(userId).stream()
            .map(f -> toDto(f, userId))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FriendSuggestionDto> suggestFriends(Long userId) {
        Set<Long> directFriendIds = new HashSet<>(friendshipRepository.findAcceptedFriendUserIds(userId));
        Set<Long> excludedIds = new HashSet<>(friendshipRepository.findRelatedUserIds(userId));
        excludedIds.add(userId);

        Map<Long, Integer> mutualCounts = new HashMap<>();
        for (Long friendId : directFriendIds) {
            for (Long candidateId : friendshipRepository.findAcceptedFriendUserIds(friendId)) {
                if (!excludedIds.contains(candidateId)) {
                    mutualCounts.merge(candidateId, 1, Integer::sum);
                }
            }
        }

        List<FriendSuggestionDto> suggestions = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : mutualCounts.entrySet()) {
            userRepository.findById(entry.getKey()).ifPresent(user -> suggestions.add(toSuggestionDto(user, entry.getValue())));
        }

        return suggestions.stream()
            .sorted(Comparator.comparingInt(FriendSuggestionDto::getMutualFriendCount).reversed())
            .limit(20)
            .collect(Collectors.toList());
    }

    private FriendDto toDto(Friendship f, Long currentUserId) {
        User other = f.getRequester().getId().equals(currentUserId)
                     ? f.getAddressee()
                     : f.getRequester();

        String direction = f.getRequester().getId().equals(currentUserId) ? "SENT" : "RECEIVED";
        boolean online = other.getLastSeenAt() != null
                      && other.getLastSeenAt().isAfter(LocalDateTime.now().minusMinutes(5));

        return new FriendDto(
            f.getId(),
            other.getId(),
            other.getUsername(),
            other.getDisplayName(),
            other.getAvatarUrl(),
            f.getStatus().name(),
            online,
            direction
        );
    }

    private String displayName(User user) {
        if (user == null) return "Ai do";
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) return user.getDisplayName();
        return user.getUsername() != null ? user.getUsername() : "Ai do";
    }

    private FriendSuggestionDto toSuggestionDto(User user, int mutualCount) {
        boolean online = user.getLastSeenAt() != null
            && user.getLastSeenAt().isAfter(LocalDateTime.now().minusMinutes(5));
        String reason = mutualCount > 0
            ? mutualCount + " ban chung"
            : "Co the ban biet nguoi nay";
        return new FriendSuggestionDto(
            user.getId(),
            user.getUsername(),
            user.getDisplayName(),
            user.getAvatarUrl(),
            online,
            mutualCount,
            reason
        );
    }
}
