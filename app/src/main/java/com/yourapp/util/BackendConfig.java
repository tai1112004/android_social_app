package com.yourapp.util;

import android.content.Context;
import android.content.SharedPreferences;

public final class BackendConfig {
    private static final String PREFS_NAME = "backend_config";
    private static final String KEY_HOST = "backend_host";
    private static final String KEY_HOST_PROMPT_SHOWN = "host_prompt_shown";
    private static final String EMULATOR_HOST = "10.0.2.2";
    private static final String DEVICE_HOST = "127.0.0.1";

    private static volatile SharedPreferences prefs;

    private BackendConfig() {
    }

    public static void init(Context context) {
        if (prefs == null) {
            synchronized (BackendConfig.class) {
                if (prefs == null) {
                    prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                }
            }
        }
    }

    public static String getHost() {
        SharedPreferences localPrefs = prefs;
        if (localPrefs != null) {
            String override = localPrefs.getString(KEY_HOST, null);
            if (override != null && !override.trim().isEmpty()) {
                return override.trim();
            }
        }
        return getDefaultHost();
    }

    public static void setHost(String host) {
        if (prefs == null) {
            return;
        }
        if (host == null || host.trim().isEmpty()) {
            prefs.edit().remove(KEY_HOST).apply();
        } else {
            prefs.edit().putString(KEY_HOST, host.trim()).apply();
        }
    }

    public static void clearOverride() {
        if (prefs != null) {
            prefs.edit().remove(KEY_HOST).apply();
        }
    }

    public static String getDefaultHost() {
        return isEmulator() ? EMULATOR_HOST : DEVICE_HOST;
    }

    public static boolean shouldShowHostPrompt() {
        SharedPreferences localPrefs = prefs;
        if (localPrefs == null) {
            return true;
        }
        String currentHost = localPrefs.getString(KEY_HOST, null);
        boolean promptShown = localPrefs.getBoolean(KEY_HOST_PROMPT_SHOWN, false);
        if (currentHost == null || currentHost.trim().isEmpty()) {
            return true;
        }
        if (!isEmulator() && isLoopbackHost(currentHost.trim())) {
            return true;
        }
        return !promptShown;
    }

    public static void markHostPromptShown() {
        if (prefs != null) {
            prefs.edit().putBoolean(KEY_HOST_PROMPT_SHOWN, true).apply();
        }
    }

    public static void resetHostPrompt() {
        if (prefs != null) {
            prefs.edit().remove(KEY_HOST_PROMPT_SHOWN).apply();
        }
    }

    private static boolean isEmulator() {
        String fingerprint = android.os.Build.FINGERPRINT == null ? "" : android.os.Build.FINGERPRINT.toLowerCase();
        String model = android.os.Build.MODEL == null ? "" : android.os.Build.MODEL.toLowerCase();
        String product = android.os.Build.PRODUCT == null ? "" : android.os.Build.PRODUCT.toLowerCase();
        String hardware = android.os.Build.HARDWARE == null ? "" : android.os.Build.HARDWARE.toLowerCase();
        String manufacturer = android.os.Build.MANUFACTURER == null ? "" : android.os.Build.MANUFACTURER.toLowerCase();
        String brand = android.os.Build.BRAND == null ? "" : android.os.Build.BRAND.toLowerCase();
        String device = android.os.Build.DEVICE == null ? "" : android.os.Build.DEVICE.toLowerCase();

        return fingerprint.contains("generic")
                || fingerprint.contains("emulator")
                || fingerprint.contains("sdk_gphone")
                || product.contains("sdk")
                || product.contains("emulator")
                || product.contains("google_sdk")
                || product.contains("sdk_x86")
                || hardware.contains("goldfish")
                || hardware.contains("ranchu")
                || hardware.contains("emulator")
                || model.contains("android sdk built for x86")
                || model.contains("android sdk built for arm")
                || model.contains("sdk_gphone")
                || manufacturer.contains("genymotion")
                || brand.startsWith("generic")
                || device.startsWith("generic");
    }

    public static boolean isLoopbackHost(String host) {
        if (host == null) {
            return false;
        }
        String trimmed = host.trim();
        return "127.0.0.1".equals(trimmed) || "10.0.2.2".equals(trimmed);
    }
}




