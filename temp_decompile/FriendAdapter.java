package com.yourapp.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.yourapp.R.drawable;
import com.yourapp.R.id;
import com.yourapp.R.layout;
import com.yourapp.model.Friend;
import java.util.ArrayList;
import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<ViewHolder> {
   private final OnFriendActionListener actionListener;
   private final OnFriendClickListener clickListener;
   private List<Friend> friends = new ArrayList();
   private String mode = "friends";

   public FriendAdapter(OnFriendActionListener actionListener, OnFriendClickListener clickListener) {
      this.actionListener = actionListener;
      this.clickListener = clickListener;
   }

   public void setMode(String mode) {
      this.mode = mode;
   }

   public void setFriends(List<Friend> friends) {
      this.friends = (List<Friend>)(friends != null ? friends : new ArrayList());
      this.notifyDataSetChanged();
   }

   @NonNull
   public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View view = LayoutInflater.from(parent.getContext()).inflate(layout.item_friend, parent, false);
      return new ViewHolder(view);
   }

   public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
      Friend friend = (Friend)this.friends.get(position);
      holder.bind(friend, this.mode);
      holder.itemView.setOnClickListener((v) -> {
         if (this.clickListener != null) {
            this.clickListener.onClick(friend);
         }

      });
      holder.btnAction.setOnClickListener((v) -> {
         if (this.actionListener != null) {
            this.actionListener.onAction(friend);
         }

      });
   }

   public int getItemCount() {
      return this.friends.size();
   }

   static class ViewHolder extends RecyclerView.ViewHolder {
      private final TextView tvAvatar;
      private final View onlineDot;
      private final TextView tvName;
      private final TextView tvStatus;
      private final Button btnAction;

      ViewHolder(@NonNull View itemView) {
         super(itemView);
         this.tvAvatar = (TextView)itemView.findViewById(id.tv_friend_avatar);
         this.onlineDot = itemView.findViewById(id.view_online_dot);
         this.tvName = (TextView)itemView.findViewById(id.tv_friend_name);
         this.tvStatus = (TextView)itemView.findViewById(id.tv_friend_status);
         this.btnAction = (Button)itemView.findViewById(id.btn_friend_action);
      }

      void bind(Friend friend, String mode) {
         String name = friend.getDisplayName() != null && !friend.getDisplayName().isEmpty() ? friend.getDisplayName() : friend.getUsername();
         if (name == null || name.isEmpty()) {
            name = "User";
         }

         this.tvAvatar.setText(name.substring(0, 1).toUpperCase());
         this.tvName.setText(name);
         this.onlineDot.setBackgroundResource(friend.isOnline() ? drawable.bg_online_dot : drawable.bg_offline_dot);
         if ("incoming".equals(mode)) {
            this.tvStatus.setText("Muốn kết bạn với bạn");
            this.btnAction.setText("Chấp nhận");
            this.btnAction.setVisibility(0);
         } else if ("sent".equals(mode)) {
            this.tvStatus.setText("Đã gửi lời mời");
            this.btnAction.setText("Hủy");
            this.btnAction.setVisibility(0);
         } else if ("suggestions".equals(mode)) {
            String reason = friend.getReason() != null && !friend.getReason().isEmpty() ? friend.getReason() : friend.getMutualFriendCount() + " bạn chung";
            this.tvStatus.setText(reason);
            this.btnAction.setText("Thêm bạn");
            this.btnAction.setVisibility(0);
         } else {
            this.tvStatus.setText(friend.isOnline() ? "Đang online" : "Ngoại tuyến");
            this.btnAction.setText("Nhắn tin");
            this.btnAction.setVisibility(0);
         }

      }
   }

   public interface OnFriendActionListener {
      void onAction(Friend friend);
   }

   public interface OnFriendClickListener {
      void onClick(Friend friend);
   }
}
