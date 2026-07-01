package com.yourapp.model;

import java.util.List;

public class CallSessionSnapshot {
    private String sessionId;
    private Long callerId;
    private Long calleeId;
    private String callerName;
    private String callerAvatarUrl;
    private String calleeName;
    private String offerSdp;
    private String answerSdp;
    private String status;
    private String startedAt;
    private String answeredAt;
    private String endedAt;
    private long timeoutSeconds;
    private List<CallSignalCandidate> pendingCandidates;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public Long getCallerId() { return callerId; }
    public void setCallerId(Long callerId) { this.callerId = callerId; }
    public Long getCalleeId() { return calleeId; }
    public void setCalleeId(Long calleeId) { this.calleeId = calleeId; }
    public String getCallerName() { return callerName; }
    public void setCallerName(String callerName) { this.callerName = callerName; }
    public String getCallerAvatarUrl() { return callerAvatarUrl; }
    public void setCallerAvatarUrl(String callerAvatarUrl) { this.callerAvatarUrl = callerAvatarUrl; }
    public String getCalleeName() { return calleeName; }
    public void setCalleeName(String calleeName) { this.calleeName = calleeName; }
    public String getOfferSdp() { return offerSdp; }
    public void setOfferSdp(String offerSdp) { this.offerSdp = offerSdp; }
    public String getAnswerSdp() { return answerSdp; }
    public void setAnswerSdp(String answerSdp) { this.answerSdp = answerSdp; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStartedAt() { return startedAt; }
    public void setStartedAt(String startedAt) { this.startedAt = startedAt; }
    public String getAnsweredAt() { return answeredAt; }
    public void setAnsweredAt(String answeredAt) { this.answeredAt = answeredAt; }
    public String getEndedAt() { return endedAt; }
    public void setEndedAt(String endedAt) { this.endedAt = endedAt; }
    public long getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(long timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public List<CallSignalCandidate> getPendingCandidates() { return pendingCandidates; }
    public void setPendingCandidates(List<CallSignalCandidate> pendingCandidates) { this.pendingCandidates = pendingCandidates; }

    public static class CallSignalCandidate {
        private String candidate;
        private String sdpMid;
        private Integer sdpMLineIndex;
        private Long senderId;
        public String getCandidate() { return candidate; }
        public void setCandidate(String candidate) { this.candidate = candidate; }
        public String getSdpMid() { return sdpMid; }
        public void setSdpMid(String sdpMid) { this.sdpMid = sdpMid; }
        public Integer getSdpMLineIndex() { return sdpMLineIndex; }
        public void setSdpMLineIndex(Integer sdpMLineIndex) { this.sdpMLineIndex = sdpMLineIndex; }
        public Long getSenderId() { return senderId; }
        public void setSenderId(Long senderId) { this.senderId = senderId; }
    }
}
