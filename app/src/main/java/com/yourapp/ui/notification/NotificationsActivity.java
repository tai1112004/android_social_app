package com.yourapp.ui.notification;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.yourapp.R;
import com.yourapp.model.StoredNotification;
import com.yourapp.ui.auth.LoginActivity;
import com.yourapp.ui.chat.ChatActivity;
import com.yourapp.ui.profile.PublicProfileActivity;
import com.yourapp.util.NotificationStore;
import com.yourapp.util.TokenManager;
import java.util.List;
import java.util.Map;

public class NotificationsActivity extends AppCompatActivity {
    private static final String MODE_ALL = "all";
    private static final String MODE_UNREAD = "unread";
    private static final String MODE_READ = "read";

    private NotificationStore notificationStore;
    private NotificationAdapter adapter;
    private View emptyState;
    private TextView btnAll;
    private TextView btnUnread;
    private TextView btnRead;

    private String currentMode = MODE_ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        TokenManager tokenManager = new TokenManager(this);
        if (!tokenManager.hasValidTokens()) {
            redirectToLogin();
            return;
        }

        long currentUserId = tokenManager.getUserId();
        if (currentUserId > 0) {
            notificationStore = new NotificationStore(this, currentUserId);
        } else {
            notificationStore = new NotificationStore(this, "username_" + tokenManager.getUsername());
        }
        setupToolbar();
        setupViews();
        render();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_notifications);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Thong bao");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupViews() {
        emptyState = findViewById(R.id.notifications_empty_state);
        btnAll = findViewById(R.id.btn_notifications_all);
        btnUnread = findViewById(R.id.btn_notifications_unread);
        btnRead = findViewById(R.id.btn_notifications_read);

        Button btnMarkAllRead = findViewById(R.id.btn_notifications_mark_all_read);
        RecyclerView rvNotifications = findViewById(R.id.rv_notifications);

        adapter = new NotificationAdapter(this::onNotificationClicked);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        btnAll.setOnClickListener(v -> {
            currentMode = MODE_ALL;
            render();
        });
        btnUnread.setOnClickListener(v -> {
            currentMode = MODE_UNREAD;
            render();
        });
        btnRead.setOnClickListener(v -> {
            currentMode = MODE_READ;
            render();
        });
        btnMarkAllRead.setOnClickListener(v -> {
            notificationStore.markAllAsRead();
            render();
            Toast.makeText(this, "Da danh dau tat ca la da doc", Toast.LENGTH_SHORT).show();
        });
    }

    private void render() {
        List<StoredNotification> items;
        if (MODE_UNREAD.equals(currentMode)) {
            items = notificationStore.getUnread();
        } else if (MODE_READ.equals(currentMode)) {
            items = notificationStore.getRead();
        } else {
            items = notificationStore.getAll();
        }

        adapter.setItems(items);
        updateTabs();
        boolean empty = items == null || items.isEmpty();
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void updateTabs() {
        int unreadCount = notificationStore.getUnreadCount();
        int totalCount = notificationStore.getAll().size();
        int readCount = Math.max(0, totalCount - unreadCount);
        btnAll.setText("Tat ca (" + totalCount + ")");
        btnUnread.setText("Chua doc (" + unreadCount + ")");
        btnRead.setText("Da doc (" + readCount + ")");

        applyTabState(btnAll, MODE_ALL.equals(currentMode));
        applyTabState(btnUnread, MODE_UNREAD.equals(currentMode));
        applyTabState(btnRead, MODE_READ.equals(currentMode));
    }

    private void applyTabState(TextView tab, boolean active) {
        tab.setSelected(active);
        tab.setAlpha(active ? 1f : 0.72f);
        tab.setTypeface(tab.getTypeface(), active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void onNotificationClicked(StoredNotification notification) {
        if (notification == null) {
            return;
        }
        notificationStore.markAsRead(notification.getId());
        routeNotification(notification);
        render();
    }

    private void routeNotification(StoredNotification notification) {
        String type = notification.getType();
        Map<String, Object> data = notification.getData();
        if (type == null) {
            Toast.makeText(this, "Da mo thong bao", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("NEW_MESSAGE".equals(type) || "MESSAGE_REPLY".equals(type)) {
            Long conversationId = asLong(data != null ? data.get("conversationId") : null);
            if (conversationId != null) {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversationId);
                intent.putExtra(ChatActivity.EXTRA_CONVERSATION_NAME,
                        notification.getActorDisplayName() != null ? notification.getActorDisplayName() : "Tin nhan");
                startActivity(intent);
                return;
            }
        }

        if ("FRIEND_REQUEST".equals(type) || "FRIEND_ACCEPTED".equals(type)) {
            Long userId = asLong(data != null ? data.get("requesterId") : null);
            if (userId == null) {
                userId = asLong(data != null ? data.get("friendId") : null);
            }
            if (userId == null) {
                userId = notification.getActorId();
            }
            if (userId != null) {
                Intent intent = new Intent(this, PublicProfileActivity.class);
                intent.putExtra(PublicProfileActivity.EXTRA_USER_ID, userId);
                startActivity(intent);
                return;
            }
        }

        Toast.makeText(this, "Da mo thong bao", Toast.LENGTH_SHORT).show();
    }

    private Long asLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
