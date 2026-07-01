package com.yourapp.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.yourapp.R;
import com.yourapp.model.Friend;
import java.util.ArrayList;
import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {

    public interface OnFriendActionListener {
        void onAction(Friend friend);
    }

    public interface OnFriendClickListener {
        void onClick(Friend friend);
    }

    private final OnFriendActionListener actionListener;
    private final OnFriendClickListener clickListener;
    private List<Friend> friends = new ArrayList<>();
    private String mode = "friends";

    public FriendAdapter(OnFriendActionListener actionListener, OnFriendClickListener clickListener) {
        this.actionListener = actionListener;
        this.clickListener = clickListener;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setFriends(List<Friend> friends) {
        this.friends = friends != null ? friends : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Friend friend = friends.get(position);
        holder.bind(friend, mode);
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onClick(friend);
            }
        });
        holder.btnAction.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onAction(friend);
            }
        });
        holder.btnMessage.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onClick(friend);
            }
        });
        holder.btnUnfriend.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onAction(friend);
            }
        });
        holder.btnBlock.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onAction(friend);
            }
        });
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvAvatar;
        private final View onlineDot;
        private final TextView tvName;
        private final TextView tvStatus;
        private final Button btnAction;
        private final LinearLayout layoutActions;
        private final Button btnMessage;
        private final Button btnUnfriend;
        private final Button btnBlock;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_friend_avatar);
            onlineDot = itemView.findViewById(R.id.view_online_dot);
            tvName = itemView.findViewById(R.id.tv_friend_name);
            tvStatus = itemView.findViewById(R.id.tv_friend_status);
            btnAction = itemView.findViewById(R.id.btn_friend_action);
            layoutActions = itemView.findViewById(R.id.layout_friend_actions);
            btnMessage = itemView.findViewById(R.id.btn_friend_message);
            btnUnfriend = itemView.findViewById(R.id.btn_friend_unfriend);
            btnBlock = itemView.findViewById(R.id.btn_friend_block);
        }

        void bind(Friend friend, String mode) {
            String name = friend.getDisplayName() != null && !friend.getDisplayName().isEmpty()
                    ? friend.getDisplayName()
                    : friend.getUsername();
            if (name == null || name.isEmpty()) {
                name = "User";
            }

            tvAvatar.setText(name.substring(0, 1).toUpperCase());
            tvName.setText(name);
            onlineDot.setBackgroundResource(friend.isOnline()
                    ? R.drawable.bg_online_dot
                    : R.drawable.bg_offline_dot);

            if ("incoming".equals(mode)) {
                layoutActions.setVisibility(View.GONE);
                btnAction.setVisibility(View.VISIBLE);
                tvStatus.setText("Mu\u1ED1n k\u1EBFt b\u1EA1n v\u1EDBi b\u1EA1n");
                btnAction.setText("Ch\u1EA5p nh\u1EADn");
            } else if ("sent".equals(mode)) {
                layoutActions.setVisibility(View.GONE);
                btnAction.setVisibility(View.VISIBLE);
                tvStatus.setText("\u0110\u00E3 g\u1EEDi l\u1EDDi m\u1EDDi");
                btnAction.setText("H\u1EE7y");
            } else if ("suggestions".equals(mode)) {
                layoutActions.setVisibility(View.GONE);
                btnAction.setVisibility(View.VISIBLE);
                String reason = friend.getReason() != null && !friend.getReason().isEmpty()
                        ? friend.getReason()
                        : friend.getMutualFriendCount() + " ban chung";
                tvStatus.setText(reason);
                btnAction.setText("Th\u00EAm b\u1EA1n");
            } else {
                btnAction.setVisibility(View.GONE);
                layoutActions.setVisibility(View.VISIBLE);
                tvStatus.setText(friend.isOnline() ? "\u0110ang online" : "Ngo\u1EA1i tuy\u1EBFn");
            }
        }
    }
}

