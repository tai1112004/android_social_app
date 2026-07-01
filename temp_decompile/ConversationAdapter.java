package com.yourapp.ui.home;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.yourapp.R.id;
import com.yourapp.R.layout;
import com.yourapp.model.Conversation;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ViewHolder> {
   private List<Conversation> conversations = new ArrayList();
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
      this.conversations = (List<Conversation>)(list != null ? list : new ArrayList());
      this.notifyDataSetChanged();
   }

   @NonNull
   public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View view = LayoutInflater.from(parent.getContext()).inflate(layout.item_conversation, parent, false);
      return new ViewHolder(view);
   }

   public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
      Conversation conversation = (Conversation)this.conversations.get(position);
      holder.bind(conversation);
      holder.itemView.setOnClickListener((v) -> {
         if (this.listener != null) {
            this.listener.onConversationClick(conversation);
         }

      });
   }

   public int getItemCount() {
      return this.conversations.size();
   }

   static class ViewHolder extends RecyclerView.ViewHolder {
      private final TextView tvConvName;
      private final TextView tvConvType;
      private final TextView tvMemberCount;
      private final TextView tvConvAvatar;
      private final TextView tvUnread;
      private final View vOnlineDot;

      ViewHolder(@NonNull View itemView) {
         super(itemView);
         this.tvConvAvatar = (TextView)itemView.findViewById(id.tv_conv_avatar);
         this.tvConvName = (TextView)itemView.findViewById(id.tv_conv_name);
         this.tvConvType = (TextView)itemView.findViewById(id.tv_conv_type);
         this.tvMemberCount = (TextView)itemView.findViewById(id.tv_member_count);
         this.tvUnread = (TextView)itemView.findViewById(id.tv_conv_unread);
         this.vOnlineDot = itemView.findViewById(id.v_conv_online_dot);
      }

      void bind(Conversation conversation) {
         String name = conversation.getName() != null && !conversation.getName().isEmpty() ? conversation.getName() : "Conversation";
         this.tvConvAvatar.setText(name.substring(0, 1).toUpperCase());
         this.tvConvName.setText(name);
         this.tvConvType.setText(conversation.getType());
         boolean online = Boolean.TRUE.equals(conversation.getOnline());
         this.vOnlineDot.setVisibility(online ? 0 : 8);
         long unreadCount = conversation.getUnreadCount();
         if (unreadCount > 0L) {
            this.tvUnread.setVisibility(0);
            this.tvUnread.setText(unreadCount > 99L ? "99+" : String.valueOf(unreadCount));
         } else {
            this.tvUnread.setVisibility(8);
         }

         String preview = conversation.getLastMessagePreview();
         if (preview != null && !preview.isEmpty()) {
            this.tvMemberCount.setText(preview);
            this.tvMemberCount.setTextColor(Color.parseColor("#475569"));
         } else {
            if ("PRIVATE".equals(conversation.getType())) {
               if (online) {
                  this.tvMemberCount.setText("Đang hoạt động");
                  this.tvMemberCount.setTextColor(Color.parseColor("#22C55E"));
               } else {
                  String relativeTime = this.getRelativeTimeSpanString(conversation.getLastSeenAt());
                  this.tvMemberCount.setText(relativeTime);
                  this.tvMemberCount.setTextColor(Color.parseColor("#6B7280"));
               }
            } else {
               this.tvMemberCount.setText(conversation.getMemberCount() + " thành viên - Chạm để mở");
               this.tvMemberCount.setTextColor(Color.parseColor("#6B7280"));
            }

         }
      }

      private String getRelativeTimeSpanString(String lastSeenAtStr) {
         if (lastSeenAtStr != null && !lastSeenAtStr.isEmpty()) {
            try {
               if (lastSeenAtStr.contains("T")) {
                  String sanitized = lastSeenAtStr;
                  if (lastSeenAtStr.endsWith("Z")) {
                     sanitized = lastSeenAtStr.substring(0, lastSeenAtStr.length() - 1);
                  }

                  if (sanitized.contains("+")) {
                     sanitized = sanitized.split("\\+")[0];
                  }

                  LocalDateTime lastSeen = LocalDateTime.parse(sanitized);
                  LocalDateTime now = LocalDateTime.now();
                  Duration duration = Duration.between(lastSeen, now);
                  long seconds = duration.getSeconds();
                  if (seconds < 60L) {
                     return "Vừa hoạt động";
                  } else {
                     long minutes = seconds / 60L;
                     if (minutes < 60L) {
                        return "Hoạt động " + minutes + " phút trước";
                     } else {
                        long hours = minutes / 60L;
                        if (hours < 24L) {
                           return "Hoạt động " + hours + " giờ trước";
                        } else {
                           long days = hours / 24L;
                           return days < 30L ? "Hoạt động " + days + " ngày trước" : "Hoạt động lâu trước";
                        }
                     }
                  }
               } else {
                  return "Hoạt động gần đây";
               }
            } catch (Exception e) {
               e.printStackTrace();
               return "Hoạt động gần đây";
            }
         } else {
            return "Hoạt động gần đây";
         }
      }
   }

   public interface OnConversationClickListener {
      void onConversationClick(Conversation conversation);
   }
}
