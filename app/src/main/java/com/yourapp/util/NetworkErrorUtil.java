package com.yourapp.util;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public final class NetworkErrorUtil {
    private NetworkErrorUtil() {
    }

    public static String friendlyMessage(Throwable t, String fallbackMessage) {
        String rawMessage = t != null && t.getMessage() != null ? t.getMessage().trim() : "";
        if (isUsbLoopbackMode() && isConnectionProblem(t, rawMessage)) {
            return "Cannot connect to backend via localhost. If you just unplugged USB, choose LAN IP or run adb reverse tcp:8082 tcp:8082 again.";
        }
        if (!rawMessage.isEmpty()) {
            return rawMessage;
        }
        return fallbackMessage != null && !fallbackMessage.trim().isEmpty()
                ? fallbackMessage
                : "Network connection error";
    }

    private static boolean isUsbLoopbackMode() {
        String host = BackendConfig.getHost();
        return BackendConfig.isLoopbackHost(host);
    }

    private static boolean isConnectionProblem(Throwable t, String rawMessage) {
        if (t == null) {
            return false;
        }
        if (t instanceof ConnectException || t instanceof SocketTimeoutException || t instanceof UnknownHostException) {
            return true;
        }
        String className = t.getClass().getName();
        String message = rawMessage.toLowerCase();
        return className.contains("ConnectException")
                || className.contains("SocketTimeoutException")
                || className.contains("UnknownHostException")
                || message.contains("failed to connect")
                || message.contains("unexpected end of stream")
                || message.contains("connection refused")
                || message.contains("timeout");
    }
}





