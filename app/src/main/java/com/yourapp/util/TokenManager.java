package com.yourapp.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import org.jetbrains.annotations.Nullable;

/**
 * TokenManager wraps EncryptedSharedPreferences for secure token storage.
 * Handles saving/retrieving access and refresh tokens.
 */
public class TokenManager {
    private static final String PREFS_NAME = "auth_tokens";
    private final SharedPreferences encryptedPrefs;

    public TokenManager(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

            this.encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize EncryptedSharedPreferences", e);
        }
    }

    /**
     * Save access token
     */
    public void saveAccessToken(String token) {
        if (token != null) {
            encryptedPrefs.edit().putString(Constants.TOKEN_KEY, token).apply();
        }
    }

    /**
     * Save refresh token
     */
    public void saveRefreshToken(String token) {
        if (token != null) {
            encryptedPrefs.edit().putString(Constants.REFRESH_TOKEN_KEY, token).apply();
        }
    }

    /**
     * Get access token
     */
    @Nullable
    public String getAccessToken() {
        return encryptedPrefs.getString(Constants.TOKEN_KEY, null);
    }

    /**
     * Get refresh token
     */
    @Nullable
    public String getRefreshToken() {
        return encryptedPrefs.getString(Constants.REFRESH_TOKEN_KEY, null);
    }

    /**
     * Check if both tokens exist and are not empty
     */
    public boolean hasValidTokens() {
        String accessToken = getAccessToken();
        String refreshToken = getRefreshToken();
        return accessToken != null && !accessToken.isEmpty() &&
               refreshToken != null && !refreshToken.isEmpty();
    }

    /**
     * Clear both tokens
     */
    public void clearTokens() {
        encryptedPrefs.edit()
            .remove(Constants.TOKEN_KEY)
            .remove(Constants.REFRESH_TOKEN_KEY)
            .apply();
    }

    /**
     * Save user ID
     */
    public void saveUserId(Long userId) {
        if (userId != null) {
            encryptedPrefs.edit().putLong(Constants.USER_ID_KEY, userId).apply();
        }
    }

    /**
     * Get user ID
     */
    public long getUserId() {
        return encryptedPrefs.getLong(Constants.USER_ID_KEY, -1L);
    }

    /**
     * Save username
     */
    public void saveUsername(String username) {
        if (username != null) {
            encryptedPrefs.edit().putString(Constants.USERNAME_KEY, username).apply();
        }
    }

    /**
     * Get username
     */
    @Nullable
    public String getUsername() {
        return encryptedPrefs.getString(Constants.USERNAME_KEY, null);
    }

    /**
     * Clear all authentication data
     */
    public void clearAll() {
        encryptedPrefs.edit().clear().apply();
    }
}
