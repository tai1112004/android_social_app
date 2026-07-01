package com.yourapp.ui.call;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
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
import com.yourapp.ui.home.HomeActivity;
import com.yourapp.util.TokenManager;
import com.yourapp.webrtc.WebRtcManager;
import java.util.HashSet;
import java.util.Set;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OngoingCallActivity extends AppCompatActivity {
    private static final String TAG = "CallFlow";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            updateTimer();
            handler.postDelayed(this, 1000L);
        }
    };

    private long startedAt = System.currentTimeMillis();
    private boolean muted;
    private boolean speakerOn = true;
    private TextView tvStatus;
    private TextView tvTimer;
    private CallApiService callApiService;
    private boolean finishingCall;
    private Long currentUserId;
    private final Set<String> appliedRemoteCandidates = new HashSet<>();
    private final Runnable endPollRunnable = new Runnable() {
        @Override public void run() {
            pollCallStillActive();
            handler.postDelayed(this, 1500L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ongoing_call);

        TokenManager tokenManager = new TokenManager(this);
        if (!tokenManager.hasValidTokens()) {
            redirectToLogin();
            return;
        }

        callApiService = RetrofitClient.getInstance(tokenManager).create(CallApiService.class);
        currentUserId = tokenManager.getUserId();

        CallRuntime runtime = CallRuntime.get();
        if (runtime.getWebRtcManager() == null || runtime.getSignalingService() == null) {
            Toast.makeText(this, "Khong co cuoc goi dang hoat dong", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvStatus = findViewById(R.id.tv_call_status);
        tvTimer = findViewById(R.id.tv_call_timer);
        TextView tvTitle = findViewById(R.id.tv_call_title);
        tvTitle.setText(runtime.getPeerName() != null ? runtime.getPeerName() : "Dang trong cuoc goi");
        tvStatus.setText("Dang ket noi");
        attachRuntimeWebRtcListener(runtime);
        attachRuntimeSignalingListener(runtime);

        findViewById(R.id.btn_call_mute).setOnClickListener(v -> toggleMute());
        findViewById(R.id.btn_call_speaker).setOnClickListener(v -> toggleSpeaker());
        findViewById(R.id.btn_call_end).setOnClickListener(v -> endCall());

        handler.post(timerRunnable);
        handler.postDelayed(endPollRunnable, 1500L);
        CallForegroundService.start(this, "Dang goi", "Dang trong cuoc goi");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(timerRunnable);
        handler.removeCallbacks(endPollRunnable);
    }

    private void updateTimer() {
        long elapsed = Math.max(0L, System.currentTimeMillis() - startedAt);
        long totalSeconds = elapsed / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }

    private void attachRuntimeSignalingListener(CallRuntime runtime) {
        SignalingService signalingService = runtime.getSignalingService();
        WebRtcManager webRtcManager = runtime.getWebRtcManager();
        if (signalingService == null) {
            return;
        }
        signalingService.setListener(new SignalingService.Listener() {
            @Override public void onConnected() { }
            @Override public void onIncomingCall(CallSignalMessage message) { }
            @Override public void onCallAnswered(CallSignalMessage message) { }
            @Override public void onIceCandidate(CallSignalMessage message) {
                if (message != null && webRtcManager != null && !finishingCall) {
                    Log.d(TAG, "Received ICE session=" + message.getSessionId() + ", mid=" + message.getSdpMid());
                    webRtcManager.addIceCandidate(message.getCandidate(), message.getSdpMid(), message.getSdpMLineIndex() != null ? message.getSdpMLineIndex() : 0);
                }
            }
            @Override public void onCallRejected(CallSignalMessage message) { remoteEnded(); }
            @Override public void onCallEnded(CallSignalMessage message) { remoteEnded(); }
            @Override public void onDisconnected() { }
            @Override public void onError(String message) {
                if (!finishingCall) {
                    runOnUiThread(() -> Toast.makeText(OngoingCallActivity.this, message, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void attachRuntimeWebRtcListener(CallRuntime runtime) {
        WebRtcManager webRtcManager = runtime.getWebRtcManager();
        if (webRtcManager == null) {
            return;
        }
        webRtcManager.setListener(new WebRtcManager.Listener() {
            @Override public void onLocalIceCandidate(org.webrtc.IceCandidate candidate) {
                sendLocalIceCandidate(candidate);
            }

            @Override public void onConnectionEstablished() {
                runOnUiThread(() -> tvStatus.setText("Da ket noi am thanh"));
            }

            @Override public void onConnectionClosed() {
                if (!finishingCall) {
                    runOnUiThread(() -> tvStatus.setText("Dang mat ket noi am thanh"));
                }
            }

            @Override public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(OngoingCallActivity.this, message, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void sendLocalIceCandidate(org.webrtc.IceCandidate candidate) {
        SignalingService signalingService = CallRuntime.get().getSignalingService();
        if (candidate == null || signalingService == null || CallRuntime.get().getSessionId() == null) {
            return;
        }
        CallSignalMessage message = new CallSignalMessage();
        message.setSessionId(CallRuntime.get().getSessionId());
        if (CallRuntime.get().isCaller()) {
            message.setCallerId(currentUserId);
            message.setCalleeId(CallRuntime.get().getPeerUserId());
        } else {
            message.setCallerId(CallRuntime.get().getPeerUserId());
            message.setCalleeId(currentUserId);
        }
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

    private void endCall() {
        if (finishingCall) {
            return;
        }
        finishingCall = true;
        CallSignalMessage message = buildEndMessage();
        SignalingService signalingService = CallRuntime.get().getSignalingService();
        if (callApiService != null) {
            callApiService.endCall(message).enqueue(new Callback<ApiResponse<CallSignalMessage>>() {
                @Override public void onResponse(Call<ApiResponse<CallSignalMessage>> call, Response<ApiResponse<CallSignalMessage>> response) {
                    if ((response == null || !response.isSuccessful()) && signalingService != null) {
                        signalingService.sendEnd(message);
                    }
                    closeLocalCall();
                }

                @Override public void onFailure(Call<ApiResponse<CallSignalMessage>> call, Throwable t) {
                    if (signalingService != null) {
                        signalingService.sendEnd(message);
                    }
                    closeLocalCall();
                }
            });
            return;
        }
        if (signalingService != null) {
            signalingService.sendEnd(message);
        }
        closeLocalCall();
    }

    private CallSignalMessage buildEndMessage() {
        CallSignalMessage message = new CallSignalMessage();
        message.setSessionId(CallRuntime.get().getSessionId());
        if (CallRuntime.get().isCaller()) {
            message.setCalleeId(CallRuntime.get().getPeerUserId());
        } else {
            message.setCallerId(CallRuntime.get().getPeerUserId());
        }
        return message;
    }

    private void pollCallStillActive() {
        if (finishingCall || callApiService == null || CallRuntime.get().getSessionId() == null) {
            return;
        }
        callApiService.getCallSession(CallRuntime.get().getSessionId()).enqueue(new Callback<ApiResponse<CallSessionSnapshot>>() {
            @Override public void onResponse(Call<ApiResponse<CallSessionSnapshot>> call, Response<ApiResponse<CallSessionSnapshot>> response) {
                if (finishingCall) return;
                if (!response.isSuccessful() || response.body() == null || !Boolean.TRUE.equals(response.body().isSuccess())) {
                    remoteEnded();
                    return;
                }
                CallSessionSnapshot snapshot = response.body().getData();
                if (snapshot == null || "COMPLETED".equals(snapshot.getStatus()) || "NO_ANSWER".equals(snapshot.getStatus()) || "REJECTED".equals(snapshot.getStatus())) {
                    remoteEnded();
                    return;
                }
                applyPendingCandidates(snapshot);
            }

            @Override public void onFailure(Call<ApiResponse<CallSessionSnapshot>> call, Throwable t) { }
        });
    }

    private void applyPendingCandidates(CallSessionSnapshot snapshot) {
        WebRtcManager webRtcManager = CallRuntime.get().getWebRtcManager();
        if (webRtcManager == null || snapshot.getPendingCandidates() == null) {
            return;
        }
        for (CallSessionSnapshot.CallSignalCandidate candidate : snapshot.getPendingCandidates()) {
            if (candidate == null || candidate.getCandidate() == null) {
                continue;
            }
            if (currentUserId != null && currentUserId.equals(candidate.getSenderId())) {
                continue;
            }
            String key = candidate.getSenderId() + ":" + candidate.getSdpMid() + ":" + candidate.getSdpMLineIndex() + ":" + candidate.getCandidate();
            if (appliedRemoteCandidates.add(key)) {
                webRtcManager.addIceCandidate(candidate.getCandidate(), candidate.getSdpMid(), candidate.getSdpMLineIndex() != null ? candidate.getSdpMLineIndex() : 0);
            }
        }
    }

    private void remoteEnded() {
        if (finishingCall) return;
        finishingCall = true;
        runOnUiThread(() -> {
            Toast.makeText(this, "Cuoc goi da ket thuc", Toast.LENGTH_SHORT).show();
            closeLocalCall();
        });
    }

    private void closeLocalCall() {
        handler.removeCallbacks(timerRunnable);
        handler.removeCallbacks(endPollRunnable);
        CallRuntime.get().clear();
        CallForegroundService.stop(this);
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
