package com.yourapp.util;

public class Constants {

    public static String getBaseUrl() {
        return BackendConfig.getBaseUrl();
    }

    public static String getWsUrl() {
        return BackendConfig.getWsUrl();
    }

    public static final String TOKEN_KEY = "access_token";
    public static final String REFRESH_TOKEN_KEY = "refresh_token";
    public static final String USER_ID_KEY = "user_id";
    public static final String USERNAME_KEY = "username";

    public static final String ENDPOINT_LOGIN = "auth/login";
    public static final String ENDPOINT_REGISTER = "auth/register";
    public static final String ENDPOINT_REFRESH = "auth/refresh";
    public static final String ENDPOINT_USERS = "users";
    public static final String ENDPOINT_MESSAGES = "messages";
    public static final String ENDPOINT_CONVERSATIONS = "conversations";
    public static final String ENDPOINT_CALLS = "calls";
    public static final String ENDPOINT_ME_FCM_TOKEN = "users/me/fcm-token";

    public static final String WS_CONNECT_ENDPOINT = "/app/connect";
    public static final String WS_CALL_TOPIC = "/topic/call";
    public static final String WS_CALL_INITIATE = "/app/call.initiate";
    public static final String WS_CALL_ANSWER = "/app/call.answer";
    public static final String WS_CALL_ICE = "/app/call.ice-candidate";
    public static final String WS_CALL_REJECT = "/app/call.reject";
    public static final String WS_CALL_END = "/app/call.end";
    public static final String WS_CHAT_SEND = "/app/chat.send";
    public static final String WS_CHAT_TOPIC = "/topic/messages";

    public static final long CONNECT_TIMEOUT = 30000;
    public static final long READ_TIMEOUT = 30000;
    public static final long WRITE_TIMEOUT = 30000;

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String DB_NAME = "social_app.db";

    private Constants() {
        // Prevent instantiation
    }
}
