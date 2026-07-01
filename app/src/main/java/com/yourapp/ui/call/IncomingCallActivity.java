package com.yourapp.ui.call;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.yourapp.R;
import com.yourapp.call.CallForegroundService;
import com.yourapp.call.CallRuntime;
import com.yourapp.data.remote.CallApiService;
import com.yourapp.model.ApiResponse;
import com.yourapp.model.CallSessionSnapshot;
import com.yourapp.model.CallSignalMessage;
import com.yourapp.network.RetrofitClient;
import com.yourapp.realtime.SignalingService;
import com.yourapp.ui.auth.LoginActivity;
import com.yourapp.util.Constants;
import com.yourapp.util.TokenManager;
import com.yourapp.webrtc.WebRtcManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IncomingCallActivity extends AppCompatActivity {
    private static final String TAG = "CallFlow";
    private static final int REQ_RECORD_AUDIO_ANSWER = 7102;

    public static final String EXTRA_SESSION_ID = "callSessionId";
    public static final String EXTRA_CALLER_ID = "callerId";
    public static final String EXTRA_CALLER_NAME = "callerName";
    public static final String EXTRA_CALLER_AVATAR = "callerAvatar";
    public static final String EXTRA_OFFER_SDP = "offerSdp";

    private TokenManager tokenManager;
    private CallApiService callApiService;
    private String sessionId;
    private Long callerId;
    private String callerName;
    private String callerAvatar;
    private String offerSdp;
    private boolean speakerOn = true;
    private boolean accepted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        setContentView(R.layout.activity_incoming_call);

        tokenManager = new TokenManager(this);
        if (!tokenManager.hasValidTokens()) {
            redirectToLogin();
            return;
        }

        sessionId = getIntent().getStringExtra(EXTRA_SESSION_ID);
        callerId = getIntent().hasExtra(EXTRA_CALLER_ID) ? getIntent().getLongExtra(EXTRA_CALLER_ID, -1L) : null;
        callerName = getIntent().getStringExtra(EXTRA_CALLER_NAME);
        callerAvatar = getIntent().getStringExtra(EXTRA_CALLER_AVATAR);
        offerSdp = getIntent().getStringExtra(EXTRA_OFFER_SDP);

        TextView tvTitle = findViewById(R.id.tv_call_title);
        TextView tvStatus = findViewById(R.id.tv_call_status);
        tvTitle.setText(callerName != null && !callerName.isEmpty() ? callerName : "Cuoc goi den");
        tvStatus.setText("Ban co 1 cuoc goi den");

        findViewById(R.id.btn_call_reject).setOnClickListener(v -> rejectCall());
        findViewById(R.id.btn_call_answer).setOnClickListener(v -> answerCall());

        callApiService = RetrofitClient.getInstance(tokenManager).create(CallApiService.class);
        if (offerSdp == null || offerSdp.isEmpty()) {
            fetchSession();
        }
    }

    private void fetchSession() {
        if (sessionId == null || sessionId.isEmpty()) {
            finish();
            return;
        }
        callApiService.getCallSession(sessionId).enqueue(new Callback<ApiResponse<CallSessionSnapshot>>() {
            @Override
            public void onResponse(Call<ApiResponse<CallSessionSnapshot>> call, Response<ApiResponse<CallSessionSnapshot>> response) {
                if (!response.isSuccessful() || response.body() == null || !Boolean.TRUE.equals(response.body().isSuccess())) {
                    finish();
                    return;
                }
                CallSessionSnapshot snapshot = response.body().getData();
                if (snapshot != null) {
                    callerId = snapshot.getCallerId();
                    callerName = snapshot.getCallerName();
                    callerAvatar = snapshot.getCallerAvatarUrl();
                    offerSdp = snapshot.getOfferSdp();
                    ((TextView) findViewById(R.id.tv_call_title)).setText(callerName != null && !callerName.isEmpty() ? callerName : "Cuoc goi den");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<CallSessionSnapshot>> call, Throwable t) {
                Toast.makeText(IncomingCallActivity.this, safeMessage(t), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void answerCall() {
        if (accepted) {
            return;
        }
        if (!ensureMicPermissionForAnswer()) {
            return;
        }
        accepted = true;
        if (sessionId == null || sessionId.isEmpty()) {
            finish();
            return;
        }
        if (offerSdp == null || offerSdp.isEmpty()) {
            fetchSession();
        }
        if (offerSdp == null || offerSdp.isEmpty()) {
            Toast.makeText(this, "Chua co du lieu cuoc goi", Toast.LENGTH_SHORT).show();
            accepted = false;
            return;
        }

        WebRtcManager webRtcManager = new WebRtcManager();
        webRtcManager.init(this, new WebRtcManager.Listener() {
            @Override
            public void onLocalIceCandidate(org.webrtc.IceCandidate candidate) {
                sendIce(candidate);
            }

            @Override
            public void onConnectionEstablished() {
                runOnUiThread(() -> ((TextView) findViewById(R.id.tv_call_status)).setText("Da ket noi"));
            }

            @Override
            public void onConnectionClosed() { }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(IncomingCallActivity.this, message, Toast.LENGTH_SHORT).show());
            }
        });

        if (!webRtcManager.isLocalAudioReady()) {
            Toast.makeText(this, "Mic chua san sang, vui long kiem tra quyen micro", Toast.LENGTH_LONG).show();
            openAppPermissionSettings();
            accepted = false;
            webRtcManager.release();
            return;
        }

        SignalingService signalingService = new SignalingService(
                Constants.getWsUrl(),
                tokenManager.getAccessToken(),
                tokenManager.getUserId(),
                new SignalingService.Listener() {
                    @Override public void onConnected() { createAnswer(webRtcManager); }
                    @Override public void onIncomingCall(CallSignalMessage message) { }
                    @Override public void onCallAnswered(CallSignalMessage message) { }
                    @Override public void onIceCandidate(CallSignalMessage message) {
                        if (message != null) {
                            Log.d(TAG, "Received ICE session=" + message.getSessionId() + ", mid=" + message.getSdpMid());
                    webRtcManager.addIceCandidate(message.getCandidate(), message.getSdpMid(), message.getSdpMLineIndex() != null ? message.getSdpMLineIndex() : 0);
                        }
                    }
                    @Override public void onCallRejected(CallSignalMessage message) { finishCall(); }
                    @Override public void onCallEnded(CallSignalMessage message) { finishCall(); }
                    @Override public void onDisconnected() { }
                    @Override public void onError(String message) { runOnUiThread(() -> Toast.makeText(IncomingCallActivity.this, message, Toast.LENGTH_SHORT).show()); }
                });

        CallRuntime runtime = CallRuntime.get();
        runtime.clear();
        runtime.setWebRtcManager(webRtcManager);
        runtime.setSignalingService(signalingService);
        runtime.setPeerUserId(callerId);
        runtime.setPeerName(callerName);
        runtime.setPeerAvatarUrl(callerAvatar);
        runtime.setSessionId(sessionId);
        runtime.setCaller(false);
        signalingService.connect();
        CallForegroundService.start(this, callerName != null ? callerName : "Cuoc goi den", "Dang tra loi");
    }

    private void createAnswer(WebRtcManager webRtcManager) {
        webRtcManager.setRemoteDescription(offerSdp, true, new WebRtcManager.SdpCallback() {
            @Override
            public void onSuccess(String sdp) {
                webRtcManager.createAnswer(new WebRtcManager.SdpCallback() {
                    @Override
                    public void onSuccess(String answerSdp) {
                        SignalingService signalingService = CallRuntime.get().getSignalingService();
                        if (signalingService == null) {
                            finishCall();
                            return;
                        }
                        CallSignalMessage message = new CallSignalMessage();
                        message.setSessionId(sessionId);
                        message.setSdp(answerSdp);
                        message.setCallerId(callerId);
                        message.setCalleeId(tokenManager.getUserId());
                        sendAnswerByRest(message, signalingService);
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> Toast.makeText(IncomingCallActivity.this, error, Toast.LENGTH_SHORT).show());
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(IncomingCallActivity.this, error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void sendAnswerByRest(CallSignalMessage message, SignalingService fallbackSignalingService) {
        callApiService.answerCall(message).enqueue(new Callback<ApiResponse<CallSignalMessage>>() {
            @Override
            public void onResponse(Call<ApiResponse<CallSignalMessage>> call, Response<ApiResponse<CallSignalMessage>> response) {
                if (!response.isSuccessful() || response.body() == null || !Boolean.TRUE.equals(response.body().isSuccess())) {
                    fallbackSignalingService.sendAnswer(message);
                }
                openOngoingCall();
            }

            @Override
            public void onFailure(Call<ApiResponse<CallSignalMessage>> call, Throwable t) {
                fallbackSignalingService.sendAnswer(message);
                openOngoingCall();
            }
        });
    }

    private void openOngoingCall() {
        runOnUiThread(() -> {
            startActivity(new Intent(IncomingCallActivity.this, OngoingCallActivity.class));
            finish();
        });
    }

    private void sendIce(org.webrtc.IceCandidate candidate) {
        SignalingService signalingService = CallRuntime.get().getSignalingService();
        if (signalingService == null || candidate == null) {
            return;
        }
        CallSignalMessage message = new CallSignalMessage();
        message.setSessionId(sessionId);
        message.setCallerId(callerId);
        message.setCalleeId(tokenManager.getUserId());
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

    private void rejectCall() {
        if (sessionId == null || sessionId.isEmpty()) {
            finish();
            return;
        }
        SignalingService signalingService = CallRuntime.get().getSignalingService();
        if (signalingService == null) {
            signalingService = new SignalingService(
                    Constants.getWsUrl(),
                    tokenManager.getAccessToken(),
                    tokenManager.getUserId(),
                    new SignalingService.Listener() {
                        @Override public void onConnected() { }
                        @Override public void onIncomingCall(CallSignalMessage message) { }
                        @Override public void onCallAnswered(CallSignalMessage message) { }
                        @Override public void onIceCandidate(CallSignalMessage message) { }
                        @Override public void onCallRejected(CallSignalMessage message) { }
                        @Override public void onCallEnded(CallSignalMessage message) { }
                        @Override public void onDisconnected() { }
                        @Override public void onError(String message) { }
                    });
            CallRuntime.get().setSignalingService(signalingService);
        }
        CallSignalMessage message = new CallSignalMessage();
        message.setSessionId(sessionId);
        message.setCallerId(callerId);
        message.setCalleeId(tokenManager.getUserId());
        SignalingService finalSignalingService = signalingService;
        finalSignalingService.connect();
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            finalSignalingService.sendReject(message);
            finishCall();
        }, 300L);
    }

    private void finishCall() {
        CallRuntime.get().clear();
        CallForegroundService.stop(this);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_RECORD_AUDIO_ANSWER) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                answerCall();
            } else {
                Toast.makeText(this, "Can quyen micro de tra loi cuoc goi", Toast.LENGTH_LONG).show();
                openAppPermissionSettings();
                accepted = false;
            }
        }
    }

    private boolean ensureMicPermissionForAnswer() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO_ANSWER);
        return false;
    }

    private void openAppPermissionSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private String safeMessage(Throwable t) {
        return t != null && t.getMessage() != null ? t.getMessage() : "Loi ket noi";
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

