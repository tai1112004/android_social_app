package com.yourapp.message;

import com.yourapp.conversation.entity.Conversation;
import com.yourapp.conversation.entity.ConversationMember;
import com.yourapp.conversation.repository.ConversationMemberRepository;
import com.yourapp.conversation.repository.ConversationRepository;
import com.yourapp.message.dto.MessageDto;
import com.yourapp.message.dto.ReactionRequest;
import com.yourapp.message.dto.SendMessageRequest;
import com.yourapp.message.entity.Message;
import com.yourapp.message.repository.MessageRepository;
import com.yourapp.story.entity.Story;
import com.yourapp.story.repository.StoryRepository;
import com.yourapp.user.entity.User;
import com.yourapp.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.yourapp.notification.NotificationService;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final UserRepository userRepository;
    private final StoryRepository storyRepository;
    private final NotificationService notificationService;

    public MessageService(MessageRepository messageRepository,
                          ConversationRepository conversationRepository,
                          ConversationMemberRepository conversationMemberRepository,
                          UserRepository userRepository,
                          StoryRepository storyRepository,
                          NotificationService notificationService) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.userRepository = userRepository;
        this.storyRepository = storyRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public List<MessageDto> getMessages(Long currentUserId, Long conversationId) {
        ensureMember(currentUserId, conversationId);
        return messageRepository.findByConversationIdAndDeletedFalseOrderBySentAtAsc(conversationId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public MessageDto sendMessage(Long currentUserId, Long conversationId, SendMessageRequest request) {
        ensureMember(currentUserId, conversationId);

        String content = request != null ? request.getContent() : null;
        if (content == null || content.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content is required");
        }

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
        User sender = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content.trim());
        message.setType(resolveType(request.getType()));
        message.setReaction(normalizeOptional(request.getReaction()));
        message.setMediaUrl(normalizeOptional(request.getMediaUrl()));
        message.setReplyPreview(normalizeOptional(request.getReplyPreview()));
        message.setReplyContextType(normalizeOptional(request.getReplyContextType()));
        message.setReplyContextId(request.getReplyContextId());
        message.setReplyContextAuthorId(request.getReplyContextAuthorId());
        message.setReplyContextAuthorUsername(normalizeOptional(request.getReplyContextAuthorUsername()));
        message.setReplyContextAuthorDisplayName(normalizeOptional(request.getReplyContextAuthorDisplayName()));
        message.setReplyContextText(normalizeOptional(request.getReplyContextText()));
        message.setReplyContextMediaUrl(normalizeOptional(request.getReplyContextMediaUrl()));

        if (request.getReplyToStoryId() != null) {
            Story story = storyRepository.findByIdAndExpiresAtAfter(request.getReplyToStoryId(), LocalDateTime.now())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Story not found or expired"));
            applyStoryReplyContext(message, story);
        }

        if (request.getReplyToMessageId() != null) {
            Message replyTo = messageRepository.findById(request.getReplyToMessageId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reply message not found"));
            if (!replyTo.getConversation().getId().equals(conversationId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reply message is outside this conversation");
            }
            message.setReplyToMessage(replyTo);
            applyMessageReplyContext(message, replyTo);
        }

        normalizeReplyPreview(message);

        Message saved = messageRepository.save(message);
        markConversationRead(currentUserId, conversationId);

        // Gui thong bao real-time cho cac thanh vien khac trong cuoc tro chuyen
        List<ConversationMember> members = conversationMemberRepository.findByConversationId(conversationId);
        List<Long> otherMemberIds = members.stream()
                .map(m -> m.getUser().getId())
                .filter(id -> !id.equals(currentUserId))
                .collect(java.util.stream.Collectors.toList());
        String preview = saved.getContent() != null && !saved.getContent().isBlank()
                ? saved.getContent() : "[media]";
        notificationService.sendToUsers(otherMemberIds, "NEW_MESSAGE",
                safeDisplayName(sender),
                preview,
                sender,
                notificationService.data("conversationId", conversationId,
                        "messageId", saved.getId(),
                        "preview", preview));

        return toDto(saved);
    }

    @Transactional
    public void deleteMessage(Long currentUserId, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        ensureMember(currentUserId, message.getConversation().getId());
        boolean canDelete = message.getSender().getId().equals(currentUserId);
        if (!canDelete) {
            Conversation conversation = message.getConversation();
            if (conversation.getCreatedBy() != null && conversation.getCreatedBy().getId().equals(currentUserId)) {
                canDelete = true;
            }
        }
        if (!canDelete) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to delete this message");
        }
        message.setDeleted(true);
        messageRepository.save(message);
    }

    @Transactional
    public MessageDto reactToMessage(Long currentUserId, Long messageId, ReactionRequest request) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        ensureMember(currentUserId, message.getConversation().getId());

        String reaction = request != null ? normalizeOptional(request.getReaction()) : null;
        if (reaction == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reaction is required");
        }
        message.setReaction(reaction);
        return toDto(messageRepository.save(message));
    }

    private void applyStoryReplyContext(Message message, Story story) {
        User author = story.getUser();
        message.setReplyContextType("STORY");
        message.setReplyContextId(story.getId());
        message.setReplyContextAuthorId(author.getId());
        message.setReplyContextAuthorUsername(author.getUsername());
        message.setReplyContextAuthorDisplayName(safeDisplayName(author));
        message.setReplyContextText(normalizeOptional(story.getContent()));
        message.setReplyContextMediaUrl(normalizeOptional(story.getMediaUrl()));
        if (message.getReplyPreview() == null || message.getReplyPreview().isBlank()) {
            message.setReplyPreview(buildStoryPreview(author, story));
        }
    }

    private void applyMessageReplyContext(Message message, Message replyTo) {
        User author = replyTo.getSender();
        message.setReplyContextType("MESSAGE");
        message.setReplyContextId(replyTo.getId());
        message.setReplyContextAuthorId(author.getId());
        message.setReplyContextAuthorUsername(author.getUsername());
        message.setReplyContextAuthorDisplayName(safeDisplayName(author));
        message.setReplyContextText(normalizeOptional(replyTo.getContent()));
        message.setReplyContextMediaUrl(normalizeOptional(replyTo.getMediaUrl()));
        if (message.getReplyPreview() == null || message.getReplyPreview().isBlank()) {
            message.setReplyPreview(buildMessagePreview(replyTo));
        }
    }

    private void normalizeReplyPreview(Message message) {
        if (message.getReplyPreview() != null && !message.getReplyPreview().isBlank()) {
            return;
        }
        if (message.getReplyContextText() != null && !message.getReplyContextText().isBlank()) {
            message.setReplyPreview(message.getReplyContextText());
            return;
        }
        if (message.getReplyToMessage() != null) {
            message.setReplyPreview(buildMessagePreview(message.getReplyToMessage()));
            return;
        }
        if (message.getReplyContextType() != null && "STORY".equalsIgnoreCase(message.getReplyContextType())
                && message.getReplyContextAuthorDisplayName() != null) {
            message.setReplyPreview(buildStoryPreviewText(message.getReplyContextAuthorDisplayName(), message.getReplyContextText()));
        }
    }

    private String buildStoryPreview(User author, Story story) {
        return buildStoryPreviewText(safeDisplayName(author), story.getContent());
    }

    private String buildStoryPreviewText(String authorName, String caption) {
        StringBuilder builder = new StringBuilder();
        builder.append("Story: ");
        builder.append(authorName != null && !authorName.isBlank() ? authorName : "Story");
        builder.append("\n");
        if (caption != null && !caption.isBlank()) {
            builder.append(trimToLength(caption.trim(), 120));
        } else {
            builder.append("(no caption)");
        }
        return builder.toString();
    }

    private String buildMessagePreview(Message replyTo) {
        StringBuilder builder = new StringBuilder();
        builder.append("Reply: ");
        builder.append(safeDisplayName(replyTo.getSender()));
        builder.append("\n");
        if (replyTo.getContent() != null && !replyTo.getContent().isBlank()) {
            builder.append(trimToLength(replyTo.getContent().trim(), 120));
        } else if (replyTo.getMediaUrl() != null && !replyTo.getMediaUrl().isBlank()) {
            builder.append("[media]");
        } else {
            builder.append("(empty)");
        }
        return builder.toString();
    }

    private String safeDisplayName(User user) {
        if (user == null) {
            return "User";
        }
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName();
        }
        return user.getUsername();
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 1) + "...";
    }

    public void markConversationRead(Long currentUserId, Long conversationId) {
        ConversationMember member = conversationMemberRepository.findByConversationIdAndUserId(conversationId, currentUserId)
                .orElse(null);
        if (member != null) {
            member.setLastReadAt(LocalDateTime.now());
            conversationMemberRepository.save(member);
        }
    }

    private void ensureMember(Long userId, Long conversationId) {
        boolean exists = conversationRepository.existsById(conversationId);
        if (!exists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found");
        }
        boolean member = conversationMemberRepository
                .existsByConversationIdAndUserId(conversationId, userId);
        if (!member) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a conversation member");
        }
    }

    private Message.Type resolveType(String type) {
        if (type == null || type.isBlank()) {
            return Message.Type.TEXT;
        }
        try {
            return Message.Type.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid message type");
        }
    }

    private String normalizeOptional(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private MessageDto toDto(Message message) {
        Message replyTo = message.getReplyToMessage();
        String replyPreview = message.getReplyPreview();
        if ((replyPreview == null || replyPreview.isBlank()) && replyTo != null) {
            replyPreview = buildMessagePreview(replyTo);
        }
        return new MessageDto(
                message.getId(),
                message.getConversation().getId(),
                message.getSender().getId(),
                message.getSender().getUsername(),
                message.getContent(),
                message.getType().name(),
                message.getSentAt(),
                replyTo != null ? replyTo.getId() : null,
                replyPreview,
                message.getReplyContextType(),
                message.getReplyContextId(),
                message.getReplyContextAuthorId(),
                message.getReplyContextAuthorUsername(),
                message.getReplyContextAuthorDisplayName(),
                message.getReplyContextText(),
                message.getReplyContextMediaUrl(),
                message.getReaction(),
                message.getMediaUrl()
        );
    }
}

