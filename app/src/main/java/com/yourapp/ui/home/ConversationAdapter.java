package com.yourapp.ui.home;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.yourapp.R;
import com.yourapp.model.Conversation;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }

    private List<Conversation> conversations = new ArrayList<>();
    private OnConversationClickListener listener;

    public ConversationAdapter() {
    }

    public ConversationAdapter(OnConversationClickListener listener) {
        this.listener = listener;
    }

    public void setOnConversationClickListener(OnConversationClickListener listener) {
        this.listener = listener;
    }

    public void setConversations(List<Conversation> list) {
        this.conversations = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Conversation conversation = conversations.get(position);
        holder.bind(conversation);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onConversationClick(conversation);
        });
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvConvName;
        private final TextView tvPreview;
        private final TextView tvConvAvatar;
        private final TextView tvUnread;
        private final TextView tvTime;
        private final View vOnlineDot;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvConvAvatar = itemView.findViewById(R.id.tv_conv_avatar);
            tvConvName = itemView.findViewById(R.id.tv_conv_name);
            tvPreview = itemView.findViewById(R.id.tv_member_count);
            tvUnread = itemView.findViewById(R.id.tv_conv_unread);
            tvTime = itemView.findViewById(R.id.tv_conv_time);
            vOnlineDot = itemView.findViewById(R.id.v_conv_online_dot);
        }

        void bind(Conversation conversation) {
            String name = conversation.getName() != null && !conversation.getName().isEmpty()
                    ? conversation.getName() : "Conversation";
            boolean unread = conversation.getUnreadCount() > 0;
            boolean online = Boolean.TRUE.equals(conversation.getOnline());

            tvConvAvatar.setText(name.substring(0, 1).toUpperCase(Locale.US));
            tvConvName.setText(name);
            tvConvName.setTypeface(Typeface.DEFAULT, unread ? Typeface.BOLD : Typeface.NORMAL);
            tvConvName.setTextColor(Color.parseColor("#1C1E21"));
            vOnlineDot.setVisibility(online ? View.VISIBLE : View.GONE);

            String preview = conversation.getLastMessagePreview();
            if (preview == null || preview.isEmpty()) {
                preview = online ? "Dang hoat dong" : "Cham de mo cuoc tro chuyen";
            }
            tvPreview.setText(preview);
            tvPreview.setTypeface(Typeface.DEFAULT, unread ? Typeface.BOLD : Typeface.NORMAL);
            tvPreview.setTextColor(Color.parseColor(unread ? "#1C1E21" : "#8E8E8E"));

            tvTime.setText(formatTimestamp(conversation));
            tvTime.setTypeface(Typeface.DEFAULT, unread ? Typeface.BOLD : Typeface.NORMAL);
            tvTime.setTextColor(Color.parseColor(unread ? "#0084FF" : "#8E8E8E"));

            if (unread) {
                tvUnread.setVisibility(View.VISIBLE);
                long count = conversation.getUnreadCount();
                tvUnread.setText(count > 99 ? "99+" : String.valueOf(count));
            } else {
                tvUnread.setVisibility(View.GONE);
            }
        }

        // Format gon kieu Messenger: hom nay hien gio, trong tuan hien thu, cu hon hien ngay/thang.
        private String formatTimestamp(Conversation conversation) {
            LocalDateTime time = parseDate(firstNonEmpty(conversation.getCreatedAt(), conversation.getLastSeenAt()));
            if (time == null) return "";
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

        private LocalDateTime parseDate(String value) {
            if (value == null || value.isEmpty()) return null;
            try {
                String sanitized = value.replace("Z", "");
                if (sanitized.contains("+")) sanitized = sanitized.split("\\+")[0];
                return LocalDateTime.parse(sanitized);
            } catch (Exception ignored) {
                return null;
            }
        }

        private String firstNonEmpty(String first, String second) {
            return first != null && !first.isEmpty() ? first : second;
        }
    }
}