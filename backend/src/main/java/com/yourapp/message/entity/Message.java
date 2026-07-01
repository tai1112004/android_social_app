package com.yourapp.message.entity;

import com.yourapp.conversation.entity.Conversation;
import com.yourapp.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
public class Message {

    public enum Type {
        TEXT, IMAGE, VIDEO, FILE, EMOJI, STICKER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 10)
    private Type type;

    @Column(name = "sent_at", updatable = false)
    private LocalDateTime sentAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_message_id")
    private Message replyToMessage;

    @Column(name = "reply_preview", length = 1000)
    private String replyPreview;

    @Column(name = "reply_context_type", length = 20)
    private String replyContextType;

    @Column(name = "reply_context_id")
    private Long replyContextId;

    @Column(name = "reply_context_author_id")
    private Long replyContextAuthorId;

    @Column(name = "reply_context_author_username", length = 50)
    private String replyContextAuthorUsername;

    @Column(name = "reply_context_author_display_name", length = 100)
    private String replyContextAuthorDisplayName;

    @Column(name = "reply_context_text", length = 2000)
    private String replyContextText;

    @Column(name = "reply_context_media_url", length = 1000)
    private String replyContextMediaUrl;

    @Column(name = "reaction", length = 32)
    private String reaction;

    @Column(name = "media_url", length = 1000)
    private String mediaUrl;

    @Column(name = "is_deleted")
    private Boolean deleted;

    @PrePersist
    void onCreate() {
        if (sentAt == null) {
            sentAt = LocalDateTime.now();
        }
        if (type == null) {
            type = Type.TEXT;
        }
        if (deleted == null) {
            deleted = false;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public Message getReplyToMessage() {
        return replyToMessage;
    }

    public void setReplyToMessage(Message replyToMessage) {
        this.replyToMessage = replyToMessage;
    }

    public String getReplyPreview() {
        return replyPreview;
    }

    public void setReplyPreview(String replyPreview) {
        this.replyPreview = replyPreview;
    }

    public String getReplyContextType() {
        return replyContextType;
    }

    public void setReplyContextType(String replyContextType) {
        this.replyContextType = replyContextType;
    }

    public Long getReplyContextId() {
        return replyContextId;
    }

    public void setReplyContextId(Long replyContextId) {
        this.replyContextId = replyContextId;
    }

    public Long getReplyContextAuthorId() {
        return replyContextAuthorId;
    }

    public void setReplyContextAuthorId(Long replyContextAuthorId) {
        this.replyContextAuthorId = replyContextAuthorId;
    }

    public String getReplyContextAuthorUsername() {
        return replyContextAuthorUsername;
    }

    public void setReplyContextAuthorUsername(String replyContextAuthorUsername) {
        this.replyContextAuthorUsername = replyContextAuthorUsername;
    }

    public String getReplyContextAuthorDisplayName() {
        return replyContextAuthorDisplayName;
    }

    public void setReplyContextAuthorDisplayName(String replyContextAuthorDisplayName) {
        this.replyContextAuthorDisplayName = replyContextAuthorDisplayName;
    }

    public String getReplyContextText() {
        return replyContextText;
    }

    public void setReplyContextText(String replyContextText) {
        this.replyContextText = replyContextText;
    }

    public String getReplyContextMediaUrl() {
        return replyContextMediaUrl;
    }

    public void setReplyContextMediaUrl(String replyContextMediaUrl) {
        this.replyContextMediaUrl = replyContextMediaUrl;
    }

    public String getReaction() {
        return reaction;
    }

    public void setReaction(String reaction) {
        this.reaction = reaction;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}