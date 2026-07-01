package com.yourapp.model;

public class Comment {
    private Long id;
    private Long authorId;
    private String authorUsername;
    private String authorDisplayName;
    private String authorAvatarUrl;
    private String content;
    private String createdAt;
    private Long replyToCommentId;
    private String replyToAuthorDisplayName;
    private String replyToContent;
    private Long replyCount;
    private String myReaction;
    private Long reactionCount;

    public Comment() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }
    public String getAuthorDisplayName() { return authorDisplayName; }
    public void setAuthorDisplayName(String authorDisplayName) { this.authorDisplayName = authorDisplayName; }
    public String getAuthorAvatarUrl() { return authorAvatarUrl; }
    public void setAuthorAvatarUrl(String authorAvatarUrl) { this.authorAvatarUrl = authorAvatarUrl; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public Long getReplyToCommentId() { return replyToCommentId; }
    public void setReplyToCommentId(Long replyToCommentId) { this.replyToCommentId = replyToCommentId; }
    public String getReplyToAuthorDisplayName() { return replyToAuthorDisplayName; }
    public void setReplyToAuthorDisplayName(String replyToAuthorDisplayName) { this.replyToAuthorDisplayName = replyToAuthorDisplayName; }
    public String getReplyToContent() { return replyToContent; }
    public void setReplyToContent(String replyToContent) { this.replyToContent = replyToContent; }
    public Long getReplyCount() { return replyCount; }
    public void setReplyCount(Long replyCount) { this.replyCount = replyCount; }
    public String getMyReaction() { return myReaction; }
    public void setMyReaction(String myReaction) { this.myReaction = myReaction; }
    public Long getReactionCount() { return reactionCount; }
    public void setReactionCount(Long reactionCount) { this.reactionCount = reactionCount; }
}