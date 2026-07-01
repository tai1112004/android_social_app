package com.yourapp.ui.feed;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.yourapp.R;
import com.yourapp.model.Comment;
import com.yourapp.util.AvatarUtil;
import java.util.ArrayList;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    public interface OnCommentActionListener {
        void onReply(Comment comment);
        void onReact(Comment comment);
    }

    private List<Comment> comments = new ArrayList<>();
    private OnCommentActionListener listener;

    public CommentAdapter() {
    }

    public CommentAdapter(OnCommentActionListener listener) {
        this.listener = listener;
    }

    public void setListener(OnCommentActionListener listener) {
        this.listener = listener;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
        notifyDataSetChanged();
    }

    public void addComment(Comment comment) {
        comments.add(comment);
        notifyItemInserted(comments.size() - 1);
    }

    public void updateComment(Comment updated) {
        if (updated == null || updated.getId() == null) {
            return;
        }
        for (int i = 0; i < comments.size(); i++) {
            Comment current = comments.get(i);
            if (current.getId() != null && current.getId().equals(updated.getId())) {
                comments.set(i, updated);
                notifyItemChanged(i);
                return;
            }
        }
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        String name = comment.getAuthorDisplayName() != null && !comment.getAuthorDisplayName().isEmpty()
                ? comment.getAuthorDisplayName() : comment.getAuthorUsername();
        holder.tvAuthor.setText(name);
        holder.tvContent.setText(comment.getContent());

        boolean isReply = comment.getReplyToCommentId() != null;
        holder.replyContainer.setVisibility(isReply ? View.VISIBLE : View.GONE);
        if (isReply) {
            String repliedName = comment.getReplyToAuthorDisplayName();
            String repliedContent = comment.getReplyToContent();
            if (repliedName == null || repliedName.isEmpty()) {
                repliedName = "Người được trả lời";
            }
            if (repliedContent == null || repliedContent.isEmpty()) {
                repliedContent = "Bình luận gốc";
            }
            holder.tvReplyTag.setText("@" + repliedName);
            holder.tvReplyText.setText(repliedContent);
        }

        long replyCount = comment.getReplyCount() != null ? comment.getReplyCount() : 0L;
        long reactionCount = comment.getReactionCount() != null ? comment.getReactionCount() : 0L;
        String myReaction = comment.getMyReaction();

        if (replyCount > 0) {
            holder.tvReplyCount.setVisibility(View.VISIBLE);
            holder.tvReplyCount.setText(replyCount + " phản hồi");
        } else {
            holder.tvReplyCount.setVisibility(View.GONE);
        }

        if (reactionCount > 0) {
            holder.tvReactionSummary.setVisibility(View.VISIBLE);
            holder.tvReactionSummary.setText(reactionCount + " biểu cảm");
        } else {
            holder.tvReactionSummary.setVisibility(View.GONE);
        }

        holder.btnReply.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReply(comment);
            }
        });

        holder.btnReact.setText(myReaction != null && !myReaction.isEmpty() ? myReaction : "😊");
        holder.btnReact.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReact(comment);
            }
        });

        AvatarUtil.loadAvatar(comment.getAuthorAvatarUrl(), comment.getAuthorDisplayName(), comment.getAuthorUsername(), holder.ivAvatar, holder.tvAvatar);

        int indent = isReply ? holder.replyIndent : 0;
        int top = isReply ? holder.replyTopGap : holder.normalTopGap;
        MarginLayoutParams params = (MarginLayoutParams) holder.contentCard.getLayoutParams();
        params.setMarginStart(indent);
        holder.contentCard.setLayoutParams(params);
        holder.contentCard.setPadding(
                holder.basePaddingLeft,
                top,
                holder.basePaddingRight,
                holder.basePaddingBottom);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvAvatar, tvAuthor, tvContent, tvReplyCount, tvReactionSummary, btnReply, btnReact, tvReplyTag, tvReplyText;
        View contentCard;
        View replyContainer;
        int replyIndent;
        int normalTopGap;
        int replyTopGap;
        int basePaddingLeft;
        int basePaddingRight;
        int basePaddingBottom;

        CommentViewHolder(View v) {
            super(v);
            ivAvatar = v.findViewById(R.id.iv_comment_avatar);
            tvAvatar = v.findViewById(R.id.tv_comment_avatar);
            tvAuthor = v.findViewById(R.id.tv_comment_author);
            tvContent = v.findViewById(R.id.tv_comment_content);
            tvReplyCount = v.findViewById(R.id.tv_comment_reply_count);
            tvReactionSummary = v.findViewById(R.id.tv_comment_reaction_summary);
            btnReply = v.findViewById(R.id.btn_comment_reply);
            btnReact = v.findViewById(R.id.btn_comment_react);
            replyContainer = v.findViewById(R.id.layout_comment_reply);
            tvReplyTag = v.findViewById(R.id.tv_comment_reply_tag);
            tvReplyText = v.findViewById(R.id.tv_comment_reply_text);
            contentCard = v.findViewById(R.id.layout_comment_card);
            replyIndent = v.getResources().getDimensionPixelSize(R.dimen.comment_card_indent_reply);
            normalTopGap = v.getResources().getDimensionPixelSize(R.dimen.comment_card_top_gap_normal);
            replyTopGap = v.getResources().getDimensionPixelSize(R.dimen.comment_card_top_gap_reply);
            basePaddingLeft = contentCard.getPaddingLeft();
            basePaddingRight = contentCard.getPaddingRight();
            basePaddingBottom = contentCard.getPaddingBottom();
        }
    }
}