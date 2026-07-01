package com.yourapp.ui.call;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.yourapp.R;
import com.yourapp.call.CallForegroundService;
import com.yourapp.call.CallRuntime;
import com.yourapp.data.remote.CallApiService;
import com.yourapp.data.remote.ConversationApiService;
import com.yourapp.model.ApiResponse;
import com.yourapp.model.CallSessionSnapshot;
import com.yourapp.model.CallSignalMessage;
import com.yourapp.model.ConversationInfo;
import com.yourapp.model.ConversationMember;
import com.yourapp.network.RetrofitClient;
import com.yourapp.realtime.SignalingService;
import com.yourapp.ui.auth.LoginActivity;
import com.yourapp.util.Constants;
import com.yourapp.util.TokenManager;
import com.yourapp.webrtc.WebRtcManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OutgoingCallActivity extends AppCompatActivity {
    private static final String TAG = "CallFlow";
    private static final int REQ_RECORD_AUDIO_OUTGOING = 7103;

    public static final String EXTRA_CONVERSATION_ID = "conversation_id";
    public static final String EXTRA_CONVERSATION_NAME = "conversation_name";

    private TokenManager tokenManager;
    private ConversationApiService conversationApiService;
    private CallApiService callApiService;
    private Long peerUserId;
    private String peerName;
    private String peerAvatarUrl;
    private String sessionId;
    private boolean muted;
    private boolean speakerOn = true;
    private boolean callStarted;
    private boolean signalingReady;
    private final List<org.webrtc.IceCandidate> queuedLocalIceCandidates = new ArrayList<>();
    private boolean answerHandled;
    private final Handler answerPollHandler = new Handler(Looper.getMainLooper());

    private View btnMute;
    private View btnSpeaker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outgoing_call);

        tokenManager = new TokenManager(this);
        if (!tokenManager.hasValidTokens()) {
            redirectToLogin();
            return;
        }

        long conversationId = getIntent().getLongExtra(EXTRA_CONVERSATION_ID, -1L);
        String conversationName = getIntent().getStringExtra(EXTRA_CONVERSATION_NAME);
        if (conversationId == -1L) {
            finish();
            return;
        }

        ((android.widget.TextView) findViewById(R.id.tv_call_title)).setText(
                conversationName != null && !conversationName.isEmpty() ? conversationName : "Dang goi...");
        ((android.widget.TextView) findViewById(R.id.tv_call_status)).setText("Dang chuan bi cuoc goi");

        btnMute = findViewById(R.id.btn_call_mute);
        btnSpeaker = findViewById(R.id.btn_call_speaker);
        findViewById(R.id.btn_call_end).setOnClickListener(v -> finishCall());
        btnMute.setOnClickListener(v -> toggleMute());
        btnSpeaker.setOnClickListener(v -> toggleSpeaker());

        conversationApiService = RetrofitClient.getInstance(tokenManager).create(ConversationApiService.class);
        callApiService = RetrofitClient.getInstance(tokenManager).create(CallApiService.class);
        loadPeerAndStart(conversationId);
    }

    private void loadPeerAndStart(long conversationId) {
        conversationApiService.getConversationInfo(conversationId).enqueue(new Callback<ApiResponse<ConversationInfo>>() {
            @Override
            public void onResponse(Call<ApiResponse<ConversationInfo>> call, Response<ApiResponse<ConversationInfo>> response) {
                if (!response.isSuccessful() || response.body() == null || !Boolean.TRUE.equals(response.body().isSuccess())) {
                    finish();
                    return;
                }
                ConversationInfo info = response.body().getData();
                if (info == null || info.getMembers() == null || info.getMembers().size() != 2) {
                    toastAndFinish("Chi ho tro goi trong cuoc tro chuyen 1-1");
                    return;
                }
                resolvePeer(info.getMembers());
            }

            @Override
            public void onFailure(Call<ApiResponse<ConversationInfo>> call, Throwable t) {
                toastAndFinish(safeMessage(t));
            }
        });
    }

    private void resolvePeer(List<ConversationMember> members) {
        Long currentUserId = tokenManager.getUserId();
        for (ConversationMember member : members) {
            if (member != null && member.getUserId() != null && !member.getUserId().equals(currentUserId)) {
                peerUserId = member.getUserId();
                peerName = member.getDisplayName() != null && !member.getDisplayName().isEmpty()
                        ? member.getDisplayName()
                        : member.getUsername();
                peerAvatarUrl = member.getAvatarUrl();
                break;
            }
        }
        if (peerUserId == null) {
            toastAndFinish("Khong tim thay nguoi nhan cuoc goi");
            return;
        }
        ensureMicPermissionAndStart();
    }

    private void ensureMicPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startCall();
            return;
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO_OUTGOING);
    }

    private void startCall() {
        if (callStarted) {
            return;
        }
        callStarted = true;
        sessionId = UUID.randomUUID().toString();
        ((android.widget.TextView) findViewById(R.id.tv_call_status)).setText("Dang goi " + peerName);
        CallForegroundService.start(this, "Dang goi " + peerName, "Dang ket noi");

        WebRtcManager webRtcManager = new WebRtcManager();
        webRtcManager.init(this, new WebRtcManager.Listener() {
            @Override
            public void onLocalIceCandidate(org.webrtc.IceCandidate candidate) {
                sendIce(candidate);
            }

            @Override
            public void onConnectionEstablished() {
                runOnUiThread(() -> ((android.widget.TextView) findViewById(R.id.tv_call_status)).setText("Da ket noi"));
            }

            @Override
            public void onConnectionClosed() {
                runOnUiThread(() -> ((android.widget.TextView) findViewById(R.id.tv_call_status)).setText("Cuoc goi da ket thuc"));
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> toastAndFinish(message));
            }
        });

        SignalingService signalingService = new SignalingService(
                Constants.getWsUrl(),
                tokenManager.getAccessToken(),
                tokenManager.getUserId(),
                new SignalingService.Listener() {
                    @Override
                    public void onConnected() {
                        createOffer(webRtcManager);
                    }

                    @Override public void onIncomingCall(CallSignalMessage message) { }

                    @Override
                    public void onCallAnswered(CallSignalMessage message) {
                        if (message != null && message.getSdp() != null) {
                            handleAnswerSdp(webRtcManager, message.getSdp());
                        }
                    }

                    @Override
                    public void onIceCandidate(CallSignalMessage message) {
                        if (message != null) {
                            Log.d(TAG, "Received ICE session=" + message.getSessionId() + ", mid=" + message.getSdpMid());
                    webRtcManager.addIceCandidate(message.getCandidate(), message.getSdpMid(), message.getSdpMLineIndex() != null ? message.getSdpMLineIndex() : 0);
                        }
                    }

                    @Override public void onCallRejected(CallSignalMessage message) { toastAndFinish("Cuoc goi bi tu choi"); }

                    @Override public void onCallEnded(CallSignalMessage message) { toastAndFinish("Cuoc goi da ket thuc"); }

                    @Override public void onDisconnected() { }

                    @Override public void onError(String message) { runOnUiThread(() -> toastAndFinish(message)); }
                });

        CallRuntime runtime = CallRuntime.get();
        runtime.clear();
        runtime.setWebRtcManager(webRtcManager);
        runtime.setSignalingService(signalingService);
        runtime.setPeerUserId(peerUserId);
        runtime.setPeerName(peerName);
        runtime.setPeerAvatarUrl(peerAvatarUrl);
        runtime.setSessionId(sessionId);
        runtime.setCaller(true);
        signalingService.connect();
    }

    private void createOffer(WebRtcManager webRtcManager) {
        CallSignalMessage message = new CallSignalMessage();
        message.setType("INCOMING_CALL");
        message.setCallerId(tokenManager.getUserId());
        message.setCalleeId(peerUserId);
        message.setSessionId(sessionId);
        message.setCallerName(tokenManager.getUsername() != null ? tokenManager.getUsername() : "Ban");
        message.setTimestamp(System.currentTimeMillis());
        webRtcManager.createOffer(new WebRtcManager.SdpCallback() {
            @Override
            public void onSuccess(String sdp) {
                message.setSdp(sdp);
                sendInitiateByRest(message);
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> toastAndFinish(error));
            }
        });
    }

    private void sendInitiateByRest(CallSignalMessage message) {
        if (callApiService == null) {
            sendInitiateBySocket(message);
            return;
        }
        callApiService.initiateCall(message).enqueue(new Callback<ApiResponse<CallSignalMessage>>() {
            @Override
            public void onResponse(Call<ApiResponse<CallSignalMessage>> call, Response<ApiResponse<CallSignalMessage>> response) {
                if (!response.isSuccessful() || response.body() == null || !Boolean.TRUE.equals(response.body().isSuccess())) {
                    sendInitiateBySocket(message);
                    return;
                }
                markSignalingReady();
                runOnUiThread(() -> ((android.widget.TextView) findViewById(R.id.tv_call_status)).setText("Da goi, dang cho tra loi"));
                startAnswerPolling(CallRuntime.get().getWebRtcManager());
            }

            @Override
            public void onFailure(Call<ApiResponse<CallSignalMessage>> call, Throwable t) {
                sendInitiateBySocket(message);
            }
        });
    }

    private void sendInitiateBySocket(CallSignalMessage message) {
        SignalingService signalingService = CallRuntime.get().getSignalingService();
        if (signalingService != null) {
            signalingService.sendInitiate(message);
        }
        markSignalingReady();
        runOnUiThread(() -> ((android.widget.TextView) findViewById(R.id.tv_call_status)).setText("Da goi, dang cho tra loi"));
        startAnswerPolling(CallRuntime.get().getWebRtcManager());
    }

    private void startAnswerPolling(WebRtcManager webRtcManager) {
        if (webRtcManager == null || callApiService == null || sessionId == null) {
            return;
        }
        answerPollHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (answerHandled || isFinishing()) {
                    return;
                }
                callApiService.getCallSession(sessionId).enqueue(new Callback<ApiResponse<CallSessionSnapshot>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<CallSessionSnapshot>> call, Response<ApiResponse<CallSessionSnapshot>> response) {
                        if (answerHandled || !response.isSuccessful() || response.body() == null || !Boolean.TRUE.equals(response.body().isSuccess())) {
                            scheduleNext();
                            return;
                        }
                        CallSessionSnapshot snapshot = response.body().getData();
                        if (snapshot != null && "ACTIVE".equals(snapshot.getStatus()) && snapshot.getAnswerSdp() != null && !snapshot.getAnswerSdp().isEmpty()) {
                            handleAnswerSdp(webRtcManager, snapshot.getAnswerSdp());
                            return;
                        }
                        scheduleNext();
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<CallSessionSnapshot>> call, Throwable t) {
                        scheduleNext();
                    }

                    private void scheduleNext() {
                        if (!answerHandled && !isFinishing()) {
                            answerPollHandler.postDelayed(() -> startAnswerPolling(webRtcManager), 1000L);
                        }
                    }
                });
            }
        }, 1000L);
    }

    private void handleAnswerSdp(WebRtcManager webRtcManager, String answerSdp) {
        if (answerHandled || webRtcManager == null || answerSdp == null || answerSdp.isEmpty()) {
            return;
        }
        answerHandled = true;
        answerPollHandler.removeCallbacksAndMessages(null);
        webRtcManager.setRemoteDescription(answerSdp, false, new WebRtcManager.SdpCallback() {
            @Override
            public void onSuccess(String sdp) {
                runOnUiThread(() -> openOngoingCall());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> toastAndFinish(error));
            }
        });
    }

    private void openOngoingCall() {
        startActivity(new Intent(OutgoingCallActivity.this, OngoingCallActivity.class));
        finish();
    }

    private void markSignalingReady() {
        signalingReady = true;
        if (queuedLocalIceCandidates.isEmpty()) {
            return;
        }
        List<org.webrtc.IceCandidate> pending = new ArrayList<>(queuedLocalIceCandidates);
        queuedLocalIceCandidates.clear();
        for (org.webrtc.IceCandidate candidate : pending) {
            sendIce(candidate);
        }
    }

    private void sendIce(org.webrtc.IceCandidate candidate) {
        if (candidate == null) {
            return;
        }
        if (!signalingReady) {
            queuedLocalIceCandidates.add(candidate);
            return;
        }
        SignalingService signalingService = CallRuntime.get().getSignalingService();
        if (signalingService == null) {
            return;
        }
        CallSignalMessage message = new CallSignalMessage();
        message.setSessionId(sessionId);
        message.setCallerId(tokenManager.getUserId());
        message.setCalleeId(peerUserId);
        message.setCandidate(candidate.sdp);
        message.setSdpMid(candidate.sdpMid);
        message.setSdpMLineIndex(candidate.sdpMLineIndex);
        Log.d(TAG, "Sending ICE session=" + message.getSessionId() + ", mid=" + message.getSdpMid());
        sendIceByRestOrSocket(message, signalingService);
    }

    private void sendIceByRestOrSocket(CallSignalMessage message, SignalingService signalingService) {
        if (callApiService == null) {
            signalingService.sendIceCandidate(message);
            return;
        }
        callApiService.sendIceCandidate(message).enqueue(new Callback<ApiResponse<CallSignalMessage>>() {
            @Override public void onResponse(Call<ApiResponse<CallSignalMessage>> call, Response<ApiResponse<CallSignalMessage>> response) {
                if ((response == null || !response.isSuccessful()) && signalingService != null) {
                    signalingService.sendIceCandidate(message);
                }
            }
            @Override public void onFailure(Call<ApiResponse<CallSignalMessage>> call, Throwable t) {
                if (signalingService != null) {
                    signalingService.sendIceCandidate(message);
                }
            }
        });
    }

    private void toggleMute() {
        WebRtcManager webRtcManager = CallRuntime.get().getWebRtcManager();
        if (webRtcManager == null) {
            return;
        }
        muted = !muted;
        webRtcManager.toggleMute(muted);
        Toast.makeText(this, muted ? "Da tat mic" : "Da bat mic", Toast.LENGTH_SHORT).show();
    }

    private void toggleSpeaker() {
        WebRtcManager webRtcManager = CallRuntime.get().getWebRtcManager();
        if (webRtcManager == null) {
            return;
        }
        speakerOn = !speakerOn;
        webRtcManager.toggleSpeaker(speakerOn, this);
        Toast.makeText(this, speakerOn ? "Da bat loa" : "Da tat loa", Toast.LENGTH_SHORT).show();
    }

    private void finishCall() {
        SignalingService signalingService = CallRuntime.get().getSignalingService();
        if (signalingService != null && sessionId != null) {
            CallSignalMessage message = new CallSignalMessage();
            message.setSessionId(sessionId);
            message.setCallerId(tokenManager.getUserId());
            message.setCalleeId(peerUserId);
            signalingService.sendEnd(message);
        }
        CallRuntime.get().clear();
        CallForegroundService.stop(this);
        finish();
    }

    private void toastAndFinish(String message) {
        if (message != null && !message.isEmpty()) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
        finishCall();
    }

    private String safeMessage(Throwable t) {
        return t != null && t.getMessage() != null ? t.getMessage() : "Loi ket noi";
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_RECORD_AUDIO_OUTGOING) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCall();
            } else {
                Toast.makeText(this, "Can quyen micro de goi", Toast.LENGTH_LONG).show();
                openAppPermissionSettings();
                finish();
            }
        }
    }

    private void openAppPermissionSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
