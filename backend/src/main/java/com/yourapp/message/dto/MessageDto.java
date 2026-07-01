package com.yourapp.message.dto;

import java.time.LocalDateTime;

public class MessageDto {

    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderUsername;
    private String content;
    private String type;
    private LocalDateTime sentAt;
    private Long replyToMessageId;
    private String replyPreview;
    private String replyContextType;
    private Long replyContextId;
    private Long replyContextAuthorId;
    private String replyContextAuthorUsername;
    private String replyContextAuthorDisplayName;
    private String replyContextText;
    private String replyContextMediaUrl;
    private String reaction;
    private String mediaUrl;

    public MessageDto() {
    }

    public MessageDto(Long id, Long conversationId, Long senderId, String senderUsername,
                      String content, String type, LocalDateTime sentAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.content = content;
        this.type = type;
        this.sentAt = sentAt;
    }

    public MessageDto(Long id, Long conversationId, Long senderId, String senderUsername,
                      String content, String type, LocalDateTime sentAt, Long replyToMessageId,
                      String replyPreview, String replyContextType, Long replyContextId,
                      Long replyContextAuthorId, String replyContextAuthorUsername,
                      String replyContextAuthorDisplayName, String replyContextText,
                      String replyContextMediaUrl, String reaction, String mediaUrl) {
        this(id, conversationId, senderId, senderUsername, content, type, sentAt);
        this.replyToMessageId = replyToMessageId;
        this.replyPreview = replyPreview;
        this.replyContextType = replyContextType;
        this.replyContextId = replyContextId;
        this.replyContextAuthorId = replyContextAuthorId;
        this.replyContextAuthorUsername = replyContextAuthorUsername;
        this.replyContextAuthorDisplayName = replyContextAuthorDisplayName;
        this.replyContextText = replyContextText;
        this.replyContextMediaUrl = replyContextMediaUrl;
        this.reaction = reaction;
        this.mediaUrl = mediaUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public Long getReplyToMessageId() {
        return replyToMessageId;
    }

    public void setReplyToMessageId(Long replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
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
}