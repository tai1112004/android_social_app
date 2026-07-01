package com.yourapp.realtime;

import android.os.Handler;
import android.os.Looper;
import com.google.gson.Gson;
import com.yourapp.model.CallSignalMessage;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class SignalingService {

    public interface Listener {
        void onConnected();
        void onIncomingCall(CallSignalMessage message);
        void onCallAnswered(CallSignalMessage message);
        void onIceCandidate(CallSignalMessage message);
        void onCallRejected(CallSignalMessage message);
        void onCallEnded(CallSignalMessage message);
        void onDisconnected();
        void onError(String message);
    }

    private static final String STOMP_NULL = Character.toString((char) 0);

    private final String wsUrl;
    private final String accessToken;
    private final Long userId;
    private Listener listener;
    private final Gson gson = new Gson();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final OkHttpClient client;

    private WebSocket webSocket;
    private boolean connected;
    private boolean subscribed;

    public SignalingService(String wsUrl, String accessToken, Long userId, Listener listener) {
        this.wsUrl = wsUrl;
        this.accessToken = accessToken;
        this.userId = userId;
        this.listener = listener;
        this.client = new OkHttpClient.Builder()
                .pingInterval(20, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();
    }

    public void connect() {
        if (userId == null || wsUrl == null || wsUrl.isEmpty() || webSocket != null) return;
        Request request = new Request.Builder().url(wsUrl).build();
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket socket, Response response) {
                sendConnect(socket);
            }

            @Override
            public void onMessage(WebSocket socket, String text) {
                handleFrame(socket, text);
            }

            @Override
            public void onClosed(WebSocket socket, int code, String reason) {
                connected = false;
                subscribed = false;
                webSocket = null;
                notifyDisconnected();
            }

            @Override
            public void onFailure(WebSocket socket, Throwable t, Response response) {
                connected = false;
                subscribed = false;
                webSocket = null;
                notifyError(t != null ? t.getMessage() : "Loi WebSocket");
                notifyDisconnected();
            }
        });
    }

    public void disconnect() {
        connected = false;
        subscribed = false;
        if (webSocket != null) {
            webSocket.close(1000, "bye");
            webSocket = null;
        }
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void sendInitiate(CallSignalMessage message) { send("/app/call.initiate", message); }
    public void sendAnswer(CallSignalMessage message) { send("/app/call.answer", message); }
    public void sendIceCandidate(CallSignalMessage message) { send("/app/call.ice-candidate", message); }
    public void sendReject(CallSignalMessage message) { send("/app/call.reject", message); }
    public void sendEnd(CallSignalMessage message) { send("/app/call.end", message); }

    private void send(String destination, CallSignalMessage message) {
        if (webSocket == null || message == null) return;
        String json = gson.toJson(message);
        StringBuilder frame = new StringBuilder();
        frame.append("SEND\n");
        frame.append("destination:").append(destination).append("\n");
        frame.append("content-type:application/json\n");
        frame.append("content-length:").append(json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length).append("\n");
        frame.append("\n").append(json).append(STOMP_NULL);
        webSocket.send(frame.toString());
    }

    private void sendConnect(WebSocket socket) {
        StringBuilder frame = new StringBuilder();
        frame.append("CONNECT\n");
        frame.append("accept-version:1.2\n");
        frame.append("heart-beat:10000,10000\n");
        if (accessToken != null && !accessToken.isEmpty()) {
            frame.append("Authorization:Bearer ").append(accessToken).append("\n");
        }
        frame.append("\n").append(STOMP_NULL);
        socket.send(frame.toString());
    }

    private void subscribe(WebSocket socket) {
        if (subscribed) return;
        String frame = "SUBSCRIBE\n"
                + "id:call-" + userId + "\n"
                + "destination:/topic/call/" + userId + "\n"
                + "\n" + STOMP_NULL;
        socket.send(frame);
        subscribed = true;
    }

    private void handleFrame(WebSocket socket, String frame) {
        if (frame == null) return;
        if (frame.startsWith("CONNECTED")) {
            connected = true;
            subscribe(socket);
            notifyConnected();
            return;
        }
        if (frame.startsWith("MESSAGE")) {
            String body = extractBody(frame);
            if (body == null || body.isEmpty()) return;
            try {
                CallSignalMessage message = gson.fromJson(body, CallSignalMessage.class);
                if (message == null || message.getType() == null) return;
                switch (message.getType()) {
                    case "INCOMING_CALL":
                        post(() -> listener.onIncomingCall(message));
                        break;
                    case "CALL_ANSWERED":
                        post(() -> listener.onCallAnswered(message));
                        break;
                    case "ICE_CANDIDATE":
                        post(() -> listener.onIceCandidate(message));
                        break;
                    case "CALL_REJECTED":
                        post(() -> listener.onCallRejected(message));
                        break;
                    case "CALL_ENDED":
                        post(() -> listener.onCallEnded(message));
                        break;
                    default:
                        break;
                }
            } catch (Exception ex) {
                notifyError("Khong doc duoc thong diep signal");
            }
        }
    }

    private String extractBody(String frame) {
        int split = frame.indexOf("\n\n");
        if (split < 0) return null;
        String body = frame.substring(split + 2);
        int nullIndex = body.indexOf((char) 0);
        return nullIndex >= 0 ? body.substring(0, nullIndex) : body;
    }

    private void post(Runnable runnable) {
        mainHandler.post(runnable);
    }

    private void notifyConnected() {
        if (listener != null) post(listener::onConnected);
    }

    private void notifyDisconnected() {
        if (listener != null) post(listener::onDisconnected);
    }

    private void notifyError(String message) {
        if (listener != null) post(() -> listener.onError(message));
    }
}
