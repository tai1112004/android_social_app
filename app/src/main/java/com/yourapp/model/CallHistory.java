package com.yourapp.model;

public class CallHistory {
    private Long id;
    private String sessionId;
    private Long callerId;
    private Long calleeId;
    private String callerName;
    private String calleeName;
    private String startTime;
    private String endTime;
    private int durationSeconds;
    private String status;
    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public Long getCallerId() { return callerId; }
    public void setCallerId(Long callerId) { this.callerId = callerId; }
    public Long getCalleeId() { return calleeId; }
    public void setCalleeId(Long calleeId) { this.calleeId = calleeId; }
    public String getCallerName() { return callerName; }
    public void setCallerName(String callerName) { this.callerName = callerName; }
    public String getCalleeName() { return calleeName; }
    public void setCalleeName(String calleeName) { this.calleeName = calleeName; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
