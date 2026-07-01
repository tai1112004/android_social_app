package com.yourapp.ui.chat;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.yourapp.R;
import com.yourapp.model.Message;
import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private final String currentUsername;
    private final OnMessageActionListener actionListener;
    private List<Message> messages = new ArrayList<>();

    public interface OnMessageActionListener {
        void onLongPress(Message message);
        void onMediaClick(Message message);
        void onDelete(Message message);
    }

    public MessageAdapter(String currentUsername) {
        this(currentUsername, null);
    }

    public MessageAdapter(String currentUsername, OnMessageActionListener actionListener) {
        this.currentUsername = currentUsername;
        this.actionListener = actionListener;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages != null ? messages : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.bind(message, currentUsername, actionListener);
        holder.itemView.setOnLongClickListener(v -> {
            if (actionListener != null) {
                actionListener.onLongPress(message);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public Message getMessageAt(int position) {
        if (position < 0 || position >= messages.size()) {
            return null;
        }
        return messages.get(position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final LinearLayout messageRow;
        private final FrameLayout messageStack;
        private final LinearLayout messageBubble;
        private final TextView tvAvatar;
        private final TextView tvSender;
        private final LinearLayout replyBlock;
        private final TextView tvReplyLabel;
        private final ImageView ivReplyMedia;
        private final FrameLayout storyPreviewMediaFrame;
        private final TextView tvReplyAuthor;
        private final TextView tvReply;
        private final TextView tvContent;
        private final FrameLayout mediaFrame;
        private final ImageView ivMedia;
        private final TextView tvVideoBadge;
        private final TextView tvReaction;
        private final TextView tvTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            messageRow = itemView.findViewById(R.id.message_row);
            messageStack = itemView.findViewById(R.id.message_stack);
            messageBubble = itemView.findViewById(R.id.message_bubble);
            tvAvatar = itemView.findViewById(R.id.tv_message_avatar);
            tvSender = itemView.findViewById(R.id.tv_message_sender);
            replyBlock = itemView.findViewById(R.id.layout_message_reply);
            tvReplyLabel = itemView.findViewById(R.id.tv_message_reply_label);
            ivReplyMedia = itemView.findViewById(R.id.iv_message_reply_media);
            storyPreviewMediaFrame = itemView.findViewById(R.id.frame_story_preview_media);
            tvReplyAuthor = itemView.findViewById(R.id.tv_message_reply_author);
            tvReply = itemView.findViewById(R.id.tv_message_reply);
            tvContent = itemView.findViewById(R.id.tv_message_content);
            mediaFrame = itemView.findViewById(R.id.media_frame);
            ivMedia = itemView.findViewById(R.id.iv_message_media);
            tvVideoBadge = itemView.findViewById(R.id.tv_video_badge);
            tvReaction = itemView.findViewById(R.id.tv_message_reaction);
            tvTime = itemView.findViewById(R.id.tv_message_time);
        }

        void bind(Message message, String currentUsername, OnMessageActionListener actionListener) {
            boolean mine = currentUsername != null && currentUsername.equals(message.getSenderUsername());
            boolean mediaOnly = hasMedia(message)
                    && (message.getContent() == null || message.getContent().isEmpty()
                    || "Anh".equals(message.getContent()) || "Video".equals(message.getContent()));

            messageRow.setGravity(mine ? Gravity.END : Gravity.START);
            tvAvatar.setVisibility(mine ? View.GONE : View.VISIBLE);
            tvAvatar.setText(initial(message.getSenderUsername()));
            LinearLayout.LayoutParams stackParams = (LinearLayout.LayoutParams) messageStack.getLayoutParams();
            stackParams.leftMargin = mine ? dp(64) : 0;
            stackParams.rightMargin = mine ? dp(10) : dp(56);
            messageStack.setLayoutParams(stackParams);
            FrameLayout.LayoutParams reactionParams = (FrameLayout.LayoutParams) tvReaction.getLayoutParams();
            reactionParams.gravity = mine ? Gravity.BOTTOM | Gravity.END : Gravity.BOTTOM | Gravity.START;
            reactionParams.leftMargin = mine ? 0 : dp(12);
            reactionParams.rightMargin = mine ? dp(12) : 0;
            tvReaction.setLayoutParams(reactionParams);

            boolean storyReply = isStoryReply(message);
            if (storyReply) {
                messageBubble.setBackground(new ColorDrawable(Color.TRANSPARENT));
                messageBubble.setPadding(0, 0, 0, 0);
                messageStack.setPadding(0, 0, 0, hasReaction(message) ? dp(18) : dp(6));
            } else if (mediaOnly) {
                messageBubble.setBackground(new ColorDrawable(Color.TRANSPARENT));
                messageBubble.setPadding(0, 0, 0, 0);
                messageStack.setPadding(0, 0, 0, hasReaction(message) ? dp(18) : dp(4));
            } else {
                messageBubble.setBackgroundResource(mine ? R.drawable.bg_message_mine : R.drawable.bg_message_other);
                messageBubble.setPadding(dp(12), dp(8), dp(12), dp(7));
                messageStack.setPadding(0, 0, 0, hasReaction(message) ? dp(16) : dp(4));
            }

            int maxWidth = (int) (itemView.getResources().getDisplayMetrics().widthPixels * 0.72f);
            tvContent.setMaxWidth(maxWidth);
            tvReply.setMaxWidth(maxWidth);
            tvReplyAuthor.setMaxWidth(maxWidth);

            tvSender.setVisibility((mine || storyReply) ? View.GONE : View.VISIBLE);
            tvSender.setText(message.getSenderUsername());
            tvContent.setTextColor(mine ? Color.WHITE : Color.rgb(26, 26, 26));
            tvTime.setTextColor(storyReply ? Color.parseColor("#8E8E8E") : (mine ? 0xCCFFFFFF : Color.rgb(142, 142, 142)));
            tvTime.setGravity(mine ? Gravity.END : Gravity.START);

            bindReply(message);

            String formattedContent = formatContent(message);
            tvContent.setText(formattedContent);
            tvContent.setVisibility(formattedContent == null || formattedContent.isEmpty() ? View.GONE : View.VISIBLE);

            renderMedia(message);

            if (hasReaction(message)) {
                tvReaction.setVisibility(View.VISIBLE);
                tvReaction.setText(message.getReaction());
            } else {
                tvReaction.setVisibility(View.GONE);
            }
            tvTime.setText(formatTime(message.getSentAt()));

            mediaFrame.setOnClickListener(v -> {
                if (actionListener != null && hasMedia(message) && "IMAGE".equals(message.getType())) {
                    actionListener.onMediaClick(message);
                }
            });
        }

        private void bindReply(Message message) {
            String replyText = firstNonEmpty(message.getReplyContextText(), message.getReplyPreview());
            String replyType = message.getReplyContextType();
            String replyAuthor = firstNonEmpty(message.getReplyContextAuthorDisplayName(), message.getReplyContextAuthorUsername());
            String replyMediaUrl = message.getReplyContextMediaUrl();

            if ((replyText == null || replyText.isEmpty()) && (replyMediaUrl == null || replyMediaUrl.isEmpty())) {
                replyBlock.setVisibility(View.GONE);
                return;
            }

            replyBlock.setVisibility(View.VISIBLE);
            boolean storyReply = "STORY".equalsIgnoreCase(replyType) || looksLikeStoryPreview(message.getReplyPreview());
            tvReplyLabel.setText(storyReply ? "Da chia se mot tin" : "Dang tra loi");
            tvReplyLabel.setTextColor(storyReply ? Color.parseColor("#DB2777") : Color.parseColor("#2563EB"));
            tvReplyAuthor.setText(replyAuthor != null ? replyAuthor : "Story");
            tvReplyAuthor.setVisibility(replyAuthor == null || replyAuthor.isEmpty() ? View.GONE : View.VISIBLE);
            tvReply.setText(replyText != null && !replyText.isEmpty() ? replyText : (storyReply ? "(Khong co chu thich)" : ""));
            tvReply.setVisibility(replyText == null || replyText.isEmpty() ? View.GONE : View.VISIBLE);

            if (replyMediaUrl != null && !replyMediaUrl.isEmpty()) {
                storyPreviewMediaFrame.setVisibility(View.VISIBLE);
                ivReplyMedia.setVisibility(View.VISIBLE);
                Glide.with(itemView).load(replyMediaUrl).centerCrop().into(ivReplyMedia);
            } else {
                storyPreviewMediaFrame.setVisibility(View.GONE);
                ivReplyMedia.setVisibility(View.GONE);
                Glide.with(itemView).clear(ivReplyMedia);
            }
        }

        private boolean isStoryReply(Message message) {
            return "STORY".equalsIgnoreCase(message.getReplyContextType()) || looksLikeStoryPreview(message.getReplyPreview());
        }

        private String initial(String value) {
            if (value == null || value.isEmpty()) return "U";
            return value.substring(0, 1).toUpperCase();
        }

        private boolean looksLikeStoryPreview(String replyPreview) {
            return replyPreview != null && replyPreview.startsWith("Story: ");
        }

        private int dp(int value) {
            return (int) (value * itemView.getResources().getDisplayMetrics().density + 0.5f);
        }

        private String formatContent(Message message) {
            String type = message.getType() != null ? message.getType() : "TEXT";
            if ("IMAGE".equals(type) || "VIDEO".equals(type)) {
                return hasMedia(message) ? "" : message.getContent();
            }
            return message.getContent();
        }

        private void renderMedia(Message message) {
            String type = message.getType() != null ? message.getType() : "TEXT";
            String mediaUrl = message.getMediaUrl();
            boolean hasMedia = hasMedia(message);
            mediaFrame.setVisibility(hasMedia ? View.VISIBLE : View.GONE);
            tvVideoBadge.setVisibility("VIDEO".equals(type) && hasMedia ? View.VISIBLE : View.GONE);
            if (hasMedia) {
                Glide.with(itemView).load(mediaUrl).centerCrop().into(ivMedia);
            } else {
                Glide.with(itemView).clear(ivMedia);
            }
        }

        private boolean hasMedia(Message message) {
            String type = message.getType() != null ? message.getType() : "TEXT";
            String mediaUrl = message.getMediaUrl();
            return mediaUrl != null && !mediaUrl.isEmpty() && ("IMAGE".equals(type) || "VIDEO".equals(type));
        }

        private boolean hasReaction(Message message) {
            return message.getReaction() != null && !message.getReaction().isEmpty();
        }

        private String formatTime(String sentAt) {
            if (sentAt == null || sentAt.length() < 16) {
                return "";
            }
            return sentAt.substring(11, 16);
        }

        private String firstNonEmpty(String first, String second) {
            if (first != null && !first.isEmpty()) {
                return first;
            }
            return second != null && !second.isEmpty() ? second : null;
        }
    }
}