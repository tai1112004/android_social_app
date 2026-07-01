package com.yourapp.message.dto;

public class SendMessageRequest {

    private String content;
    private String type;
    private Long replyToMessageId;
    private Long replyToStoryId;
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

    public SendMessageRequest() {
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

    public Long getReplyToMessageId() {
        return replyToMessageId;
    }

    public void setReplyToMessageId(Long replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }

    public Long getReplyToStoryId() {
        return replyToStoryId;
    }

    public void setReplyToStoryId(Long replyToStoryId) {
        this.replyToStoryId = replyToStoryId;
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