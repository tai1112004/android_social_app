package com.yourapp.realtime;

import android.os.Handler;
import android.os.Looper;
import com.google.gson.Gson;
import com.yourapp.model.Message;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class ChatSocketClient {

    public interface Listener {
        void onMessage(Message message);
    }

    private static final String STOMP_NULL = Character.toString((char) 0);

    private final String wsUrl;
    private final String accessToken;
    private final Long conversationId;
    private final Listener listener;
    private final Gson gson = new Gson();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final OkHttpClient client;

    private WebSocket webSocket;
    private boolean connected;
    private boolean subscribed;

    public ChatSocketClient(String wsUrl, String accessToken, Long conversationId, Listener listener) {
        this.wsUrl = wsUrl;
        this.accessToken = accessToken;
        this.conversationId = conversationId;
        this.listener = listener;
        this.client = new OkHttpClient.Builder()
                .pingInterval(20, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();
    }

    public void connect() {
        if (conversationId == null || wsUrl == null || wsUrl.isEmpty() || webSocket != null) return;
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
            }

            @Override
            public void onFailure(WebSocket socket, Throwable t, Response response) {
                connected = false;
                subscribed = false;
                webSocket = null;
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
                + "id:chat-" + conversationId + "\n"
                + "destination:/topic/messages/" + conversationId + "\n"
                + "\n" + STOMP_NULL;
        socket.send(frame);
        subscribed = true;
    }

    private void handleFrame(WebSocket socket, String frame) {
        if (frame == null) return;
        if (frame.startsWith("CONNECTED")) {
            connected = true;
            subscribe(socket);
            return;
        }
        if (frame.startsWith("MESSAGE")) {
            String body = extractBody(frame);
            if (body == null || body.isEmpty()) return;
            try {
                Message message = gson.fromJson(body, Message.class);
                if (listener != null && message != null) {
                    mainHandler.post(() -> listener.onMessage(message));
                }
            } catch (Exception ignored) {
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
}
