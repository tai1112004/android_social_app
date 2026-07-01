package com.yourapp.ui.home;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.yourapp.R;
import com.yourapp.model.Story;
import com.yourapp.model.StoryGroup;
import com.yourapp.model.User;
import com.yourapp.util.AvatarUtil;
import java.util.ArrayList;
import java.util.List;

public class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.StoryViewHolder> {

    public interface OnStoryActionListener {
        void onAddStory();
        void onViewStory(List<StoryGroup> groups, int startIndex);
    }

    private static final int TYPE_ADD_STORY = 0;
    private static final int TYPE_STORY = 1;

    private final List<StoryGroup> storyGroups = new ArrayList<>();
    private User currentUser;
    private final OnStoryActionListener listener;

    public StoryAdapter(OnStoryActionListener listener) {
        this.listener = listener;
    }

    public void setStoryGroups(List<StoryGroup> groups, User currentUser) {
        this.storyGroups.clear();
        if (groups != null) {
            this.storyGroups.addAll(groups);
        }
        this.currentUser = currentUser;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_ADD_STORY : TYPE_STORY;
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_story, parent, false);
        return new StoryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_ADD_STORY) {
            holder.tvName.setText("Tin cua toi");
            holder.ivAvatar.setVisibility(View.GONE);
            holder.tvAvatar.setVisibility(View.VISIBLE);
            holder.tvAvatar.setText("+");
            holder.tvAvatar.setTextColor(0xFF2563EB);
            holder.tvAvatar.setBackgroundResource(R.drawable.bg_empty_icon);
            holder.frameRing.setBackgroundResource(R.drawable.bg_empty_icon);
            holder.addBadge.setVisibility(View.VISIBLE);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddStory();
                }
            });
            return;
        }

        int groupIndex = position - 1;
        StoryGroup group = storyGroups.get(groupIndex);
        String name = group.getUserName() != null && !group.getUserName().isEmpty()
                ? group.getUserName() : "Story";
        holder.tvName.setText(name);
        holder.addBadge.setVisibility(View.GONE);

        applyRing(holder.frameRing, group.isHasUnviewed());
        holder.tvAvatar.setVisibility(View.GONE);
        holder.ivAvatar.setVisibility(View.VISIBLE);
        AvatarUtil.loadAvatar(group.getUserAvatarUrl(), group.getUserName(), group.getUserName(), holder.ivAvatar, holder.tvAvatar);
        if (holder.ivAvatar.getDrawable() == null) {
            holder.tvAvatar.setVisibility(View.VISIBLE);
            holder.ivAvatar.setVisibility(View.GONE);
            holder.tvAvatar.setText(name.substring(0, 1).toUpperCase());
            holder.tvAvatar.setBackgroundResource(R.drawable.bg_story_ring);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewStory(new ArrayList<>(storyGroups), groupIndex);
            }
        });
    }

    @Override
    public int getItemCount() {
        return storyGroups.size() + 1;
    }

    private void applyRing(FrameLayout ring, boolean hasUnviewed) {
        if (hasUnviewed) {
            GradientDrawable gradient = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{Color.parseColor("#833AB4"), Color.parseColor("#FD1D1D"), Color.parseColor("#FCAF45")});
            gradient.setShape(GradientDrawable.OVAL);
            gradient.setGradientType(GradientDrawable.SWEEP_GRADIENT);
            ring.setBackground(gradient);
        } else {
            GradientDrawable gray = new GradientDrawable();
            gray.setShape(GradientDrawable.OVAL);
            gray.setColor(Color.parseColor("#CCCCCC"));
            ring.setBackground(gray);
        }
    }

    static class StoryViewHolder extends RecyclerView.ViewHolder {
        FrameLayout frameRing;
        ImageView ivAvatar;
        TextView tvAvatar, tvName, addBadge;

        StoryViewHolder(View v) {
            super(v);
            frameRing = v.findViewById(R.id.frame_story_ring);
            ivAvatar = v.findViewById(R.id.iv_story_avatar);
            tvAvatar = v.findViewById(R.id.tv_story_avatar);
            tvName = v.findViewById(R.id.tv_story_name);
            addBadge = v.findViewById(R.id.tv_story_add_badge);
        }
    }
}
