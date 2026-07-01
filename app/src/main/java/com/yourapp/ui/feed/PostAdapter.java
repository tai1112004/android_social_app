package com.yourapp.ui.feed;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.yourapp.R;
import com.yourapp.model.Post;
import com.yourapp.util.AvatarUtil;
import java.util.ArrayList;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    public interface OnPostActionListener {
        void onLike(Post post, int position);
        void onComment(Post post);
    }

    private List<Post> posts = new ArrayList<>();
    private OnPostActionListener listener;

    public PostAdapter(OnPostActionListener listener) {
        this.listener = listener;
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts;
        notifyDataSetChanged();
    }

    public void updatePost(int position, Post updated) {
        posts.set(position, updated);
        notifyItemChanged(position);
    }

    public void addPost(Post post) {
        posts.add(0, post);
        notifyItemInserted(0);
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = posts.get(position);

        // Avatar loading
        String name = post.getAuthorDisplayName() != null && !post.getAuthorDisplayName().isEmpty()
                ? post.getAuthorDisplayName() : post.getAuthorUsername();
        holder.tvAuthor.setText(name);
        AvatarUtil.loadAvatar(post.getAuthorAvatarUrl(), post.getAuthorDisplayName(), post.getAuthorUsername(), holder.ivAvatar, holder.tvAvatar);

        // Time
        holder.tvTime.setText(formatTime(post.getCreatedAt()));

        // Content
        if (post.getContent() != null && !post.getContent().isEmpty()) {
            holder.tvContent.setVisibility(View.VISIBLE);
            holder.tvContent.setText(post.getContent());
        } else {
            holder.tvContent.setVisibility(View.GONE);
        }

        // Media image loading
        if (post.getMediaUrl() != null && !post.getMediaUrl().isEmpty()) {
            holder.ivMedia.setVisibility(View.VISIBLE);
            Glide.with(holder.ivMedia.getContext())
                    .load(post.getMediaUrl())
                    .centerCrop()
                    .into(holder.ivMedia);
        } else {
            holder.ivMedia.setVisibility(View.GONE);
        }

        // Like button
        boolean liked = post.isLikedByMe();
        holder.tvLikeIcon.setText(liked ? "❤️" : "👍");
        int likeCount = post.getLikeCount();
        holder.tvLikeCount.setText(likeCount > 0 ? String.valueOf(likeCount) + " Thích" : "Thích");
        holder.tvLikeCount.setTextColor(liked ? 0xFF2563EB : 0xFF6B7280);

        // Comment count
        int commentCount = post.getCommentCount();
        holder.tvCommentCount.setText(commentCount > 0 ? commentCount + " Bình luận" : "Bình luận");

        holder.btnLike.setOnClickListener(v -> {
            if (listener != null) listener.onLike(post, holder.getAdapterPosition());
        });
        holder.btnComment.setOnClickListener(v -> {
            if (listener != null) listener.onComment(post);
        });
    }

    @Override
    public int getItemCount() { return posts.size(); }

    private String formatTime(String createdAt) {
        if (createdAt == null) return "";
        try {
            // Simple formatting - show relative time or truncated date
            java.time.LocalDateTime time = java.time.LocalDateTime.parse(createdAt.substring(0, 19));
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            long minutes = java.time.Duration.between(time, now).toMinutes();
            if (minutes < 1) return "Vừa xong";
            if (minutes < 60) return minutes + " phút trước";
            long hours = minutes / 60;
            if (hours < 24) return hours + " giờ trước";
            return (hours / 24) + " ngày trước";
        } catch (Exception e) {
            return createdAt.length() > 16 ? createdAt.substring(0, 16).replace("T", " ") : createdAt;
        }
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar, ivMedia;
        TextView tvAvatar, tvAuthor, tvTime, tvContent;
        TextView tvLikeIcon, tvLikeCount, tvCommentCount;
        LinearLayout btnLike, btnComment;

        PostViewHolder(View v) {
            super(v);
            ivAvatar = v.findViewById(R.id.iv_post_avatar);
            ivMedia = v.findViewById(R.id.iv_post_media);
            tvAvatar = v.findViewById(R.id.tv_post_avatar);
            tvAuthor = v.findViewById(R.id.tv_post_author);
            tvTime = v.findViewById(R.id.tv_post_time);
            tvContent = v.findViewById(R.id.tv_post_content);
            tvLikeIcon = v.findViewById(R.id.tv_like_icon);
            tvLikeCount = v.findViewById(R.id.tv_like_count);
            tvCommentCount = v.findViewById(R.id.tv_comment_count);
            btnLike = v.findViewById(R.id.btn_post_like);
            btnComment = v.findViewById(R.id.btn_post_comment);
        }
    }
}
