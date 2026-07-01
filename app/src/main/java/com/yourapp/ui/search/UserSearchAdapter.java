package com.yourapp.ui.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.yourapp.R;
import com.yourapp.model.User;
import java.util.ArrayList;
import java.util.List;

public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.ViewHolder> {

    public interface OnUserAddListener {
        void onAdd(User user);
    }

    public interface OnUserMessageListener {
        void onMessage(User user);
    }

    private final OnUserAddListener addListener;
    private final OnUserMessageListener messageListener;
    private List<User> users = new ArrayList<>();

    public UserSearchAdapter(OnUserAddListener addListener, OnUserMessageListener messageListener) {
        this.addListener = addListener;
        this.messageListener = messageListener;
    }

    public void setUsers(List<User> users) {
        this.users = users != null ? users : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_search, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        holder.bind(user);
        holder.btnAdd.setOnClickListener(v -> {
            if (addListener != null) {
                addListener.onAdd(user);
            }
        });
        holder.btnMessage.setOnClickListener(v -> {
            if (messageListener != null) {
                messageListener.onMessage(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvAvatar;
        private final TextView tvName;
        private final TextView tvStatus;
        private final Button btnAdd;
        private final Button btnMessage;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_user_avatar);
            tvName = itemView.findViewById(R.id.tv_user_name);
            tvStatus = itemView.findViewById(R.id.tv_user_status);
            btnAdd = itemView.findViewById(R.id.btn_user_add);
            btnMessage = itemView.findViewById(R.id.btn_user_message);
        }

        void bind(User user) {
            String name = user.getDisplayName() != null && !user.getDisplayName().isEmpty()
                    ? user.getDisplayName()
                    : user.getUsername();
            if (name == null || name.isEmpty()) {
                name = "User";
            }

            tvAvatar.setText(name.substring(0, 1).toUpperCase());
            tvName.setText(name);
            tvStatus.setText(user.getFriendshipStatus() != null
                    ? user.getFriendshipStatus()
                    : "Chưa kết bạn");
            btnAdd.setEnabled(user.getFriendshipStatus() == null);
            btnAdd.setText(user.getFriendshipStatus() == null ? "Thêm" : "Đã thêm");
        }
    }
}
