package com.yourapp.websocket.dto;

import com.yourapp.message.dto.SendMessageRequest;

/**
 * STOMP payload sent from the Android client to /app/chat.send.
 * The conversation id is included so the server can route the message
 * to the correct topic after persistence.
 */
public class ChatMessageRequest {

    private Long conversationId;
    private String content;
    private String type;
    private Long replyToMessageId;
    private String reaction;
    private String mediaUrl;

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
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

    public SendMessageRequest toSendMessageRequest() {
        SendMessageRequest request = new SendMessageRequest();
        request.setContent(content);
        request.setType(type);
        request.setReplyToMessageId(replyToMessageId);
        request.setReaction(reaction);
        request.setMediaUrl(mediaUrl);
        return request;
    }
}
