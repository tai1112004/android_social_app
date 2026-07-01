package com.yourapp.conversation;

import com.yourapp.conversation.dto.CreateConversationRequest;
import com.yourapp.conversation.dto.ConversationDto;
import com.yourapp.conversation.dto.ConversationInfoDto;
import com.yourapp.conversation.dto.ConversationMemberDto;
import com.yourapp.conversation.entity.Conversation;
import com.yourapp.conversation.entity.ConversationMember;
import com.yourapp.conversation.repository.ConversationMemberRepository;
import com.yourapp.conversation.repository.ConversationRepository;
import com.yourapp.message.entity.Message;
import com.yourapp.message.repository.MessageRepository;
import com.yourapp.user.entity.User;
import com.yourapp.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public ConversationService(ConversationRepository conversationRepository,
                               ConversationMemberRepository conversationMemberRepository,
                               MessageRepository messageRepository,
                               UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ConversationDto> getConversationsForUser(Long userId) {
        List<Conversation> conversations = conversationRepository.findByUserId(userId);

        return conversations.stream()
                .map(c -> {
                    int memberCount = conversationMemberRepository.countByConversationId(c.getId());
                    String displayName = displayNameFor(c, userId);

                    Boolean online = null;
                    LocalDateTime lastSeenAt = null;
                    long unreadCount = 0L;
                    if (c.getType() == Conversation.Type.PRIVATE) {
                        List<ConversationMember> members = conversationMemberRepository.findByConversationIdOrderByRoleAscJoinedAtAsc(c.getId());
                        for (ConversationMember m : members) {
                            if (!m.getUser().getId().equals(userId)) {
                                lastSeenAt = m.getUser().getLastSeenAt();
                                online = lastSeenAt != null && lastSeenAt.isAfter(LocalDateTime.now().minusMinutes(5));
                                break;
                            }
                        }
                    }

                    ConversationMember self = conversationMemberRepository.findByConversationIdAndUserId(c.getId(), userId).orElse(null);
                    if (self != null) {
                        LocalDateTime lastReadAt = self.getLastReadAt();
                        LocalDateTime lowerBound = lastReadAt != null ? lastReadAt : LocalDateTime.of(1970, 1, 1, 0, 0);
                        unreadCount = messageRepository.countByConversationIdAndDeletedFalseAndSentAtAfterAndSenderIdNot(
                                c.getId(), lowerBound, userId);
                    }

                    return new ConversationDto(
                            c.getId(),
                            c.getType().name(),
                            displayName,
                            memberCount,
                            c.getCreatedAt(),
                            online,
                            lastSeenAt,
                            unreadCount,
                            lastMessagePreviewFor(c.getId(), userId)
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public ConversationDto createConversation(Long currentUserId, CreateConversationRequest request) {
        if (request == null || request.getMemberIds() == null || request.getMemberIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conversation members are required");
        }

        Conversation.Type type = resolveType(request.getType());
        Set<Long> memberIds = new LinkedHashSet<>(request.getMemberIds());
        memberIds.add(currentUserId);

        if (type == Conversation.Type.PRIVATE && memberIds.size() != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Private conversation requires one other member");
        }
        if (type == Conversation.Type.GROUP && memberIds.size() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group conversation requires at least two other members");
        }

        if (type == Conversation.Type.PRIVATE) {
            Long otherMemberId = request.getMemberIds().get(0);
            List<Conversation> existing = conversationRepository.findPrivateConversationBetweenUsers(currentUserId, otherMemberId);
            if (!existing.isEmpty()) {
                Conversation existingConv = existing.get(0);
                int memberCount = conversationMemberRepository.countByConversationId(existingConv.getId());
                return toDto(existingConv, memberCount, currentUserId);
            }
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Conversation conversation = new Conversation();
        conversation.setType(type);
        conversation.setCreatedBy(currentUser);
        conversation.setName(type == Conversation.Type.GROUP ? normalizeGroupName(request.getName()) : null);
        Conversation saved = conversationRepository.save(conversation);

        for (Long memberId : memberIds) {
            User member = userRepository.findById(memberId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
            ConversationMember cm = new ConversationMember();
            cm.setConversation(saved);
            cm.setUser(member);
            cm.setRole(memberId.equals(currentUserId)
                    ? ConversationMember.Role.ADMIN
                    : ConversationMember.Role.MEMBER);
            conversationMemberRepository.save(cm);
        }

        return toDto(saved, memberIds.size(), currentUserId);
    }

    @Transactional(readOnly = true)
    public ConversationInfoDto getConversationInfo(Long currentUserId, Long conversationId) {
        ensureMember(currentUserId, conversationId);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
        List<ConversationMemberDto> members = conversationMemberRepository
                .findByConversationIdOrderByRoleAscJoinedAtAsc(conversationId)
                .stream()
                .map(this::toMemberDto)
                .collect(Collectors.toList());
        return new ConversationInfoDto(
                conversation.getId(),
                conversation.getType().name(),
                displayNameFor(conversation, currentUserId),
                members.size(),
                conversation.getCreatedAt(),
                members
        );
    }

    @Transactional
    public void markConversationRead(Long currentUserId, Long conversationId) {
        ensureMember(currentUserId, conversationId);
        ConversationMember member = conversationMemberRepository.findByConversationIdAndUserId(conversationId, currentUserId)
                .orElse(null);
        if (member != null) {
            member.setLastReadAt(LocalDateTime.now());
            conversationMemberRepository.save(member);
        }
    }
    private Conversation.Type resolveType(String type) {
        if (type == null || type.isBlank()) {
            return Conversation.Type.PRIVATE;
        }
        try {
            return Conversation.Type.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid conversation type");
        }
    }

    private String normalizeGroupName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group name is required");
        }
        return name.trim();
    }

    private ConversationDto toDto(Conversation c, int memberCount, Long currentUserId) {
        String displayName = displayNameFor(c, currentUserId);

        Boolean online = null;
        LocalDateTime lastSeenAt = null;
        if (c.getType() == Conversation.Type.PRIVATE && currentUserId != null) {
            List<ConversationMember> members = conversationMemberRepository.findByConversationIdOrderByRoleAscJoinedAtAsc(c.getId());
            for (ConversationMember m : members) {
                if (!m.getUser().getId().equals(currentUserId)) {
                    lastSeenAt = m.getUser().getLastSeenAt();
                    online = lastSeenAt != null && lastSeenAt.isAfter(LocalDateTime.now().minusMinutes(5));
                    break;
                }
            }
        }

        long unreadCount = 0L;
        ConversationMember self = conversationMemberRepository.findByConversationIdAndUserId(c.getId(), currentUserId).orElse(null);
        if (self != null) {
            LocalDateTime lastReadAt = self.getLastReadAt();
            LocalDateTime lowerBound = lastReadAt != null ? lastReadAt : LocalDateTime.of(1970, 1, 1, 0, 0);
            unreadCount = messageRepository.countByConversationIdAndDeletedFalseAndSentAtAfterAndSenderIdNot(
                    c.getId(), lowerBound, currentUserId);
        }

        return new ConversationDto(
                c.getId(),
                c.getType().name(),
                displayName,
                memberCount,
                c.getCreatedAt(),
                online,
                lastSeenAt,
                unreadCount,
                lastMessagePreviewFor(c.getId(), currentUserId)
        );
    }

    private String lastMessagePreviewFor(Long conversationId, Long currentUserId) {
        Message lastMessage = messageRepository.findTopByConversationIdAndDeletedFalseOrderBySentAtDesc(conversationId);
        if (lastMessage == null) {
            return null;
        }
        String preview = lastMessage.getContent();
        if (preview == null || preview.isBlank()) {
            preview = lastMessage.getType() != null ? lastMessage.getType().name() : null;
        }
        if (preview == null) {
            return null;
        }
        preview = preview.trim().replaceAll("\\s+", " ");
        return preview.length() > 48 ? preview.substring(0, 45) + "..." : preview;
    }

    private void ensureMember(Long userId, Long conversationId) {
        if (!conversationRepository.existsById(conversationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found");
        }
        if (!conversationMemberRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a conversation member");
        }
    }

    private String displayNameFor(Conversation c, Long currentUserId) {
        if (c.getName() != null && !c.getName().isBlank()) {
            return c.getName();
        }
        if (c.getType() == Conversation.Type.PRIVATE && currentUserId != null) {
            return conversationMemberRepository.findByConversationIdOrderByRoleAscJoinedAtAsc(c.getId())
                    .stream()
                    .map(ConversationMember::getUser)
                    .filter(user -> !user.getId().equals(currentUserId))
                    .findFirst()
                    .map(user -> user.getDisplayName() != null && !user.getDisplayName().isBlank()
                            ? user.getDisplayName()
                            : user.getUsername())
                    .orElse("Chat riêng");
        }
        return c.getType() == Conversation.Type.PRIVATE ? "Chat riêng" : "Nhóm chat";
    }

    private ConversationMemberDto toMemberDto(ConversationMember member) {
        User user = member.getUser();
        boolean online = user.getLastSeenAt() != null
                && user.getLastSeenAt().isAfter(LocalDateTime.now().minusMinutes(5));
        return new ConversationMemberDto(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                member.getRole().name(),
                member.getJoinedAt(),
                online
        );
    }
}