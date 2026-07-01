package com.yourapp.util;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yourapp.model.StoredNotification;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class NotificationStore {
    private static final String PREFS_NAME = "app_notifications";
    private static final String KEY_NOTIFICATIONS = "notifications_json";
    private static final String KEY_NOTIFICATIONS_PREFIX = "notifications_json_";

    private final SharedPreferences preferences;
    private final Gson gson = new Gson();
    private final Type listType = new TypeToken<List<StoredNotification>>() {}.getType();
    private String storageKey;

    public NotificationStore(Context context) {
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.storageKey = KEY_NOTIFICATIONS;
    }

    public NotificationStore(Context context, long userId) {
        this(context);
        setUserId(userId);
    }

    public NotificationStore(Context context, String ownerKey) {
        this(context);
        setOwnerKey(ownerKey);
    }

    public synchronized void setUserId(long userId) {
        if (userId > 0) {
            storageKey = KEY_NOTIFICATIONS_PREFIX + "user_" + userId;
        }
    }

    public synchronized void setOwnerKey(String ownerKey) {
        if (ownerKey != null && !ownerKey.trim().isEmpty()) {
            storageKey = KEY_NOTIFICATIONS_PREFIX + ownerKey.trim();
        }
    }

    public synchronized List<StoredNotification> getAll() {
        return orderedCopy(readAll());
    }

    public synchronized List<StoredNotification> getUnread() {
        List<StoredNotification> items = new ArrayList<>();
        for (StoredNotification notification : readAll()) {
            if (!notification.isRead()) {
                items.add(notification);
            }
        }
        return orderedCopy(items);
    }

    public synchronized List<StoredNotification> getRead() {
        List<StoredNotification> items = new ArrayList<>();
        for (StoredNotification notification : readAll()) {
            if (notification.isRead()) {
                items.add(notification);
            }
        }
        return orderedCopy(items);
    }

    public synchronized int getUnreadCount() {
        int count = 0;
        for (StoredNotification notification : readAll()) {
            if (!notification.isRead()) {
                count++;
            }
        }
        return count;
    }

    public synchronized void add(StoredNotification notification) {
        if (notification == null || notification.getId() == null || notification.getId().isEmpty()) {
            return;
        }
        List<StoredNotification> items = readAll();
        items.removeIf(existing -> notification.getId().equals(existing.getId()));
        items.add(0, notification);
        save(items);
    }

    public synchronized void markAsRead(String notificationId) {
        if (notificationId == null || notificationId.isEmpty()) {
            return;
        }
        List<StoredNotification> items = readAll();
        boolean changed = false;
        for (StoredNotification notification : items) {
            if (notificationId.equals(notification.getId()) && !notification.isRead()) {
                notification.setRead(true);
                changed = true;
                break;
            }
        }
        if (changed) {
            save(items);
        }
    }

    public synchronized void markAllAsRead() {
        List<StoredNotification> items = readAll();
        boolean changed = false;
        for (StoredNotification notification : items) {
            if (!notification.isRead()) {
                notification.setRead(true);
                changed = true;
            }
        }
        if (changed) {
            save(items);
        }
    }

    public synchronized void clear() {
        preferences.edit().remove(storageKey).apply();
    }

    private List<StoredNotification> readAll() {
        String json = preferences.getString(storageKey, null);
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<StoredNotification> items = gson.fromJson(json, listType);
        return items != null ? new ArrayList<>(items) : new ArrayList<>();
    }

    private void save(List<StoredNotification> items) {
        preferences.edit().putString(storageKey, gson.toJson(items)).apply();
    }

    private List<StoredNotification> orderedCopy(List<StoredNotification> items) {
        List<StoredNotification> copy = new ArrayList<>(items);
        copy.sort(Comparator.comparingLong(StoredNotification::getReceivedAtMillis).reversed());
        return copy;
    }
}