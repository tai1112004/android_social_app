package com.yourapp.call;

import com.yourapp.model.CallSignalMessage;
import com.yourapp.realtime.SignalingService;
import com.yourapp.webrtc.WebRtcManager;

public final class CallRuntime {

    private static final CallRuntime INSTANCE = new CallRuntime();

    private WebRtcManager webRtcManager;
    private SignalingService signalingService;
    private Long peerUserId;
    private String peerName;
    private String peerAvatarUrl;
    private String sessionId;
    private boolean caller;
    private CallSignalMessage pendingOffer;

    private CallRuntime() {
    }

    public static CallRuntime get() {
        return INSTANCE;
    }

    public WebRtcManager getWebRtcManager() {
        return webRtcManager;
    }

    public void setWebRtcManager(WebRtcManager webRtcManager) {
        this.webRtcManager = webRtcManager;
    }

    public SignalingService getSignalingService() {
        return signalingService;
    }

    public void setSignalingService(SignalingService signalingService) {
        this.signalingService = signalingService;
    }

    public Long getPeerUserId() {
        return peerUserId;
    }

    public void setPeerUserId(Long peerUserId) {
        this.peerUserId = peerUserId;
    }

    public String getPeerName() {
        return peerName;
    }

    public void setPeerName(String peerName) {
        this.peerName = peerName;
    }

    public String getPeerAvatarUrl() {
        return peerAvatarUrl;
    }

    public void setPeerAvatarUrl(String peerAvatarUrl) {
        this.peerAvatarUrl = peerAvatarUrl;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isCaller() {
        return caller;
    }

    public void setCaller(boolean caller) {
        this.caller = caller;
    }

    public CallSignalMessage getPendingOffer() {
        return pendingOffer;
    }

    public void setPendingOffer(CallSignalMessage pendingOffer) {
        this.pendingOffer = pendingOffer;
    }

    public void clear() {
        if (webRtcManager != null) {
            webRtcManager.release();
        }
        if (signalingService != null) {
            signalingService.disconnect();
        }
        webRtcManager = null;
        signalingService = null;
        peerUserId = null;
        peerName = null;
        peerAvatarUrl = null;
        sessionId = null;
        caller = false;
        pendingOffer = null;
    }
}
