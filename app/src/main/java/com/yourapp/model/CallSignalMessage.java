package com.yourapp.model;

public class CallSignalMessage {
    private String type;
    private Long callerId;
    private Long calleeId;
    private String sessionId;
    private String sdp;
    private String candidate;
    private String sdpMid;
    private Integer sdpMLineIndex;
    private Long timestamp;
    private String callerName;
    private String callerAvatarUrl;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getCallerId() { return callerId; }
    public void setCallerId(Long callerId) { this.callerId = callerId; }
    public Long getCalleeId() { return calleeId; }
    public void setCalleeId(Long calleeId) { this.calleeId = calleeId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getSdp() { return sdp; }
    public void setSdp(String sdp) { this.sdp = sdp; }
    public String getCandidate() { return candidate; }
    public void setCandidate(String candidate) { this.candidate = candidate; }
    public String getSdpMid() { return sdpMid; }
    public void setSdpMid(String sdpMid) { this.sdpMid = sdpMid; }
    public Integer getSdpMLineIndex() { return sdpMLineIndex; }
    public void setSdpMLineIndex(Integer sdpMLineIndex) { this.sdpMLineIndex = sdpMLineIndex; }
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    public String getCallerName() { return callerName; }
    public void setCallerName(String callerName) { this.callerName = callerName; }
    public String getCallerAvatarUrl() { return callerAvatarUrl; }
    public void setCallerAvatarUrl(String callerAvatarUrl) { this.callerAvatarUrl = callerAvatarUrl; }
}
