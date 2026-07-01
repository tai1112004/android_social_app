package com.yourapp.ui.group;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.yourapp.R;
import com.yourapp.model.Friend;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GroupFriendAdapter extends RecyclerView.Adapter<GroupFriendAdapter.ViewHolder> {

    private List<Friend> friends = new ArrayList<>();
    private final Set<Long> selectedUserIds = new HashSet<>();

    public void setFriends(List<Friend> friends) {
        this.friends = friends != null ? friends : new ArrayList<>();
        selectedUserIds.clear();
        notifyDataSetChanged();
    }

    public List<Long> getSelectedUserIds() {
        return new ArrayList<>(selectedUserIds);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group_friend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Friend friend = friends.get(position);
        holder.bind(friend, selectedUserIds.contains(friend.getUserId()));
        holder.itemView.setOnClickListener(v -> toggle(friend));
        holder.checkBox.setOnClickListener(v -> toggle(friend));
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    private void toggle(Friend friend) {
        if (friend.getUserId() == null) {
            return;
        }
        if (selectedUserIds.contains(friend.getUserId())) {
            selectedUserIds.remove(friend.getUserId());
        } else {
            selectedUserIds.add(friend.getUserId());
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView avatar;
        private final TextView name;
        private final TextView status;
        private final CheckBox checkBox;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.tv_group_friend_avatar);
            name = itemView.findViewById(R.id.tv_group_friend_name);
            status = itemView.findViewById(R.id.tv_group_friend_status);
            checkBox = itemView.findViewById(R.id.cb_group_friend);
        }

        void bind(Friend friend, boolean checked) {
            String display = friend.getDisplayName() != null && !friend.getDisplayName().isEmpty()
                    ? friend.getDisplayName()
                    : friend.getUsername();
            if (display == null || display.isEmpty()) {
                display = "User";
            }
            avatar.setText(display.substring(0, 1).toUpperCase());
            name.setText(display);
            status.setText(friend.isOnline() ? "Đang online" : "Ngoại tuyến");
            checkBox.setChecked(checked);
        }
    }
}
