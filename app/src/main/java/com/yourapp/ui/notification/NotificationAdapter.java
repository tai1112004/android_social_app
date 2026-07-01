package com.yourapp.ui.notification;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.yourapp.R;
import com.yourapp.model.StoredNotification;
import com.yourapp.util.AvatarUtil;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    public interface OnNotificationClickListener {
        void onNotificationClick(StoredNotification notification);
    }

    private final OnNotificationClickListener listener;
    private final List<StoredNotification> items = new ArrayList<>();

    public NotificationAdapter(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<StoredNotification> notifications) {
        items.clear();
        if (notifications != null) {
            items.addAll(notifications);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StoredNotification notification = items.get(position);
        holder.bind(notification);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notification);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivAvatar;
        private final TextView tvAvatar;
        private final TextView tvTitle;
        private final TextView tvMessage;
        private final TextView tvTime;
        private final View unreadDot;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_notification_avatar);
            tvAvatar = itemView.findViewById(R.id.tv_notification_avatar);
            tvTitle = itemView.findViewById(R.id.tv_notification_title);
            tvMessage = itemView.findViewById(R.id.tv_notification_message);
            tvTime = itemView.findViewById(R.id.tv_notification_time);
            unreadDot = itemView.findViewById(R.id.v_notification_unread);
        }

        void bind(StoredNotification notification) {
            String name = notification.getActorDisplayName();
            if (name == null || name.trim().isEmpty()) {
                name = notification.getTitle() != null && !notification.getTitle().trim().isEmpty()
                        ? notification.getTitle()
                        : "Thông báo";
            }
            AvatarUtil.loadAvatar(notification.getActorAvatarUrl(), name, notification.getActorUsername(), ivAvatar, tvAvatar);

            boolean unread = !notification.isRead();
            tvTitle.setText(notification.getTitle() != null ? notification.getTitle() : "Thông báo");
            tvMessage.setText(notification.getMessage() != null ? notification.getMessage() : "");
            tvTime.setText(formatTime(notification));

            tvTitle.setTypeface(Typeface.DEFAULT, unread ? Typeface.BOLD : Typeface.NORMAL);
            tvMessage.setTextColor(Color.parseColor(unread ? "#1C1E21" : "#6B7280"));
            tvTime.setTextColor(Color.parseColor(unread ? "#2563EB" : "#9CA3AF"));
            unreadDot.setVisibility(unread ? View.VISIBLE : View.GONE);
        }

        private String formatTime(StoredNotification notification) {
            long millis = notification.getReceivedAtMillis();
            if (millis <= 0 && notification.getCreatedAt() != null) {
                try {
                    millis = Instant.parse(notification.getCreatedAt()).toEpochMilli();
                } catch (Exception ignored) {
                }
            }
            if (millis <= 0) {
                return "";
            }
            LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
            LocalDateTime now = LocalDateTime.now();
            if (time.toLocalDate().equals(now.toLocalDate())) {
                return time.format(DateTimeFormatter.ofPattern("HH:mm"));
            }
            if (time.isAfter(now.minusDays(7))) {
                int day = time.getDayOfWeek().getValue();
                return "T" + (day == 7 ? "CN" : String.valueOf(day + 1));
            }
            return time.format(DateTimeFormatter.ofPattern("dd/MM"));
        }
    }
}
