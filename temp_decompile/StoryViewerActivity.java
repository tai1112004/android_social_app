package com.yourapp.ui.home;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.yourapp.R.id;
import com.yourapp.R.layout;
import com.yourapp.data.remote.ConversationApiService;
import com.yourapp.data.remote.MessageApiService;
import com.yourapp.model.ApiResponse;
import com.yourapp.model.Conversation;
import com.yourapp.model.CreateConversationRequest;
import com.yourapp.model.Message;
import com.yourapp.model.SendMessageRequest;
import com.yourapp.network.RetrofitClient;
import com.yourapp.ui.chat.ChatActivity;
import com.yourapp.util.AvatarUtil;
import com.yourapp.util.TokenManager;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StoryViewerActivity extends AppCompatActivity {
   private static final long STORY_DURATION_MS = 5000L;
   private ImageView ivStoryContent;
   private ProgressBar pbStory;
   private ImageView ivViewerAvatar;
   private TextView tvViewerAvatar;
   private TextView tvViewerAuthor;
   private TextView tvViewerTime;
   private TextView tvViewerCaption;
   private ImageButton btnViewerClose;
   private TextView btnStoryReply;
   private TextView btnStoryHeart;
   private TextView btnStoryLaugh;
   private TextView btnStoryWow;
   private ValueAnimator progressAnimator;
   private boolean dialogVisible;
   private Long storyAuthorId;
   private String storyAuthorName;
   private String storyAuthorUsername;
   private String storyCaption;
   private TokenManager tokenManager;
   private ConversationApiService conversationApiService;
   private MessageApiService messageApiService;

   public StoryViewerActivity() {
   }

   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      this.getWindow().getDecorView().setSystemUiVisibility(5380);
      this.setContentView(layout.activity_story_viewer);
      this.getWindow().addFlags(128);
      this.ivStoryContent = (ImageView)this.findViewById(id.iv_story_content);
      this.pbStory = (ProgressBar)this.findViewById(id.pb_story);
      this.ivViewerAvatar = (ImageView)this.findViewById(id.iv_viewer_avatar);
      this.tvViewerAvatar = (TextView)this.findViewById(id.tv_viewer_avatar);
      this.tvViewerAuthor = (TextView)this.findViewById(id.tv_viewer_author);
      this.tvViewerTime = (TextView)this.findViewById(id.tv_viewer_time);
      this.tvViewerCaption = (TextView)this.findViewById(id.tv_viewer_caption);
      this.btnViewerClose = (ImageButton)this.findViewById(id.btn_viewer_close);
      this.btnStoryReply = (TextView)this.findViewById(id.btn_story_reply);
      this.btnStoryHeart = (TextView)this.findViewById(id.btn_story_heart);
      this.btnStoryLaugh = (TextView)this.findViewById(id.btn_story_laugh);
      this.btnStoryWow = (TextView)this.findViewById(id.btn_story_wow);
      this.tokenManager = new TokenManager(this);
      this.conversationApiService = (ConversationApiService)RetrofitClient.getInstance(this.tokenManager).create(ConversationApiService.class);
      this.messageApiService = (MessageApiService)RetrofitClient.getInstance(this.tokenManager).create(MessageApiService.class);
      String mediaUrl = this.getIntent().getStringExtra("mediaUrl");
      this.storyCaption = this.getIntent().getStringExtra("content");
      this.storyAuthorId = this.getIntent().getLongExtra("authorId", -1L);
      this.storyAuthorName = this.getIntent().getStringExtra("authorName");
      this.storyAuthorUsername = this.getIntent().getStringExtra("authorUsername");
      String authorAvatarUrl = this.getIntent().getStringExtra("authorAvatarUrl");
      String timeStr = this.getIntent().getStringExtra("time");
      this.tvViewerAuthor.setText(this.safeDisplayName());
      this.tvViewerTime.setText(TextUtils.isEmpty(timeStr) ? "Vừa xong" : this.formatTime(timeStr));
      AvatarUtil.loadAvatar(authorAvatarUrl, this.storyAuthorName, this.storyAuthorUsername, this.ivViewerAvatar, this.tvViewerAvatar);
      if (!TextUtils.isEmpty(this.storyCaption)) {
         this.tvViewerCaption.setVisibility(0);
         this.tvViewerCaption.setText(this.storyCaption);
      } else {
         this.tvViewerCaption.setVisibility(8);
      }

      if (!TextUtils.isEmpty(mediaUrl)) {
         ((RequestBuilder)Glide.with(this).load(mediaUrl).centerCrop()).into(this.ivStoryContent);
      }

      this.btnViewerClose.setOnClickListener((v) -> this.finish());
      this.btnStoryReply.setOnClickListener((v) -> this.showStoryReplyDialog());
      this.btnStoryHeart.setOnClickListener((v) -> this.sendQuickReaction("❤️"));
      this.btnStoryLaugh.setOnClickListener((v) -> this.sendQuickReaction("\ud83d\ude02"));
      this.btnStoryWow.setOnClickListener((v) -> this.sendQuickReaction("\ud83d\ude2e"));
      this.startProgress();
   }

   private String safeDisplayName() {
      if (!TextUtils.isEmpty(this.storyAuthorName)) {
         return this.storyAuthorName;
      } else {
         return !TextUtils.isEmpty(this.storyAuthorUsername) ? this.storyAuthorUsername : "Story";
      }
   }

   private void startProgress() {
      this.progressAnimator = ValueAnimator.ofInt(new int[]{0, 100});
      this.progressAnimator.setDuration(5000L);
      this.progressAnimator.setInterpolator(new LinearInterpolator());
      this.progressAnimator.addUpdateListener((animation) -> {
         int progress = (Integer)animation.getAnimatedValue();
         this.pbStory.setProgress(progress);
         if (progress >= 100) {
            this.finish();
         }

      });
      this.progressAnimator.start();
   }

   private void pauseStoryTimer() {
      if (this.progressAnimator != null && this.progressAnimator.isStarted() && !this.progressAnimator.isPaused()) {
         this.progressAnimator.pause();
      }

   }

   private void resumeStoryTimer() {
      if (this.progressAnimator != null && this.progressAnimator.isStarted() && this.progressAnimator.isPaused()) {
         this.progressAnimator.resume();
      }

   }

   private String formatTime(String createdAt) {
      if (createdAt == null) {
         return "";
      } else {
         try {
            LocalDateTime time = LocalDateTime.parse(createdAt.substring(0, 19));
            LocalDateTime now = LocalDateTime.now();
            long minutes = Duration.between(time, now).toMinutes();
            if (minutes < 1L) {
               return "Vừa xong";
            } else if (minutes < 60L) {
               return minutes + " phút trước";
            } else {
               long hours = minutes / 60L;
               return hours < 24L ? hours + " giờ trước" : hours / 24L + " ngày trước";
            }
         } catch (Exception var8) {
            return createdAt.length() > 16 ? createdAt.substring(0, 16).replace("T", " ") : createdAt;
         }
      }
   }

   private boolean isValidStoryAuthor() {
      return this.storyAuthorId != null && this.storyAuthorId > 0L;
   }

   private void showStoryReplyDialog() {
      if (!this.isValidStoryAuthor()) {
         Toast.makeText(this, "Không tìm thấy chủ story", 0).show();
      } else {
         this.pauseStoryTimer();
         this.dialogVisible = true;
         View dialogView = this.getLayoutInflater().inflate(layout.dialog_story_reply, (ViewGroup)null);
         EditText input = (EditText)dialogView.findViewById(id.et_story_reply_input);
         TextView tvPreview = (TextView)dialogView.findViewById(id.tv_story_reply_preview);
         TextView btnCancel = (TextView)dialogView.findViewById(id.btn_story_reply_cancel);
         TextView btnSend = (TextView)dialogView.findViewById(id.btn_story_reply_send);
         tvPreview.setText(this.buildStoryQuotePreview());
         if (!TextUtils.isEmpty(this.storyCaption)) {
            input.setText("Về story của " + this.safeDisplayName() + ": ");
            input.setSelection(input.getText().length());
         }

         AlertDialog dialog = (new AlertDialog.Builder(this)).setView(dialogView).create();
         btnCancel.setOnClickListener((v) -> dialog.dismiss());
         btnSend.setOnClickListener((v) -> {
            String text = input.getText().toString().trim();
            if (TextUtils.isEmpty(text)) {
               Toast.makeText(this, "Nội dung không được để trống", 0).show();
            } else {
               this.sendStoryMessage(text, "TEXT", true, dialog);
            }
         });
         dialog.setOnDismissListener((d) -> {
            this.dialogVisible = false;
            this.resumeStoryTimer();
         });
         dialog.show();
      }
   }

   private void sendQuickReaction(String reaction) {
      if (!this.isValidStoryAuthor()) {
         Toast.makeText(this, "Không tìm thấy chủ story", 0).show();
      } else {
         this.sendStoryMessage(reaction, "EMOJI", false, (AlertDialog)null);
      }
   }

   private void sendStoryMessage(final String content, final String type, final boolean openChatAfterSend, final AlertDialog dialog) {
      if (this.isValidStoryAuthor()) {
         CreateConversationRequest request = new CreateConversationRequest("PRIVATE", (String)null, Collections.singletonList(this.storyAuthorId));
         this.conversationApiService.createConversation(request).enqueue(new Callback<ApiResponse<Conversation>>() {
            public void onResponse(Call<ApiResponse<Conversation>> call, Response<ApiResponse<Conversation>> response) {
               if (response.isSuccessful() && response.body() != null && ((ApiResponse)response.body()).isSuccess() && ((ApiResponse)response.body()).getData() != null) {
                  final Conversation conversation = (Conversation)((ApiResponse)response.body()).getData();
                  SendMessageRequest messageRequest = new SendMessageRequest(content, type);
                  messageRequest.setReplyPreview("Story: " + StoryViewerActivity.this.buildStoryQuotePreview());
                  StoryViewerActivity.this.messageApiService.sendMessage(conversation.getId(), messageRequest).enqueue(new Callback<ApiResponse<Message>>() {
                     public void onResponse(Call<ApiResponse<Message>> call, Response<ApiResponse<Message>> response) {
                        if (response.isSuccessful() && response.body() != null && ((ApiResponse)response.body()).isSuccess()) {
                           Toast.makeText(StoryViewerActivity.this, "Đã gửi vào chat", 0).show();
                           if (dialog != null) {
                              dialog.dismiss();
                           }

                           if (openChatAfterSend) {
                              StoryViewerActivity.this.openChat(conversation.getId(), conversation.getName());
                           }
                        } else {
                           Toast.makeText(StoryViewerActivity.this, "Không gửi được tin nhắn", 0).show();
                        }

                     }

                     public void onFailure(Call<ApiResponse<Message>> call, Throwable t) {
                        Toast.makeText(StoryViewerActivity.this, "Lỗi kết nối", 0).show();
                     }
                  });
               } else {
                  Toast.makeText(StoryViewerActivity.this, "Không mở được chat", 0).show();
                  if (dialog != null) {
                     dialog.dismiss();
                  }

               }
            }

            public void onFailure(Call<ApiResponse<Conversation>> call, Throwable t) {
               Toast.makeText(StoryViewerActivity.this, "Lỗi tạo chat", 0).show();
               if (dialog != null) {
                  dialog.dismiss();
               }

            }
         });
      }
   }

   private String buildStoryQuotePreview() {
      StringBuilder builder = new StringBuilder();
      builder.append(this.safeDisplayName());
      builder.append("\n");
      if (!TextUtils.isEmpty(this.storyCaption)) {
         builder.append(this.trimToLength(this.storyCaption.trim(), 120));
      } else {
         builder.append("(không có caption)");
      }

      return builder.toString();
   }

   private String trimToLength(String value, int maxLength) {
      return value != null && value.length() > maxLength ? value.substring(0, maxLength - 1) + "…" : value;
   }

   private void openChat(Long conversationId, String conversationName) {
      Intent intent = new Intent(this, ChatActivity.class);
      intent.putExtra("conversation_id", conversationId);
      intent.putExtra("conversation_name", conversationName != null && !conversationName.isEmpty() ? conversationName : this.safeDisplayName());
      this.startActivity(intent);
   }

   protected void onPause() {
      super.onPause();
      if (!this.dialogVisible) {
         this.pauseStoryTimer();
      }

   }

   protected void onResume() {
      super.onResume();
      if (!this.dialogVisible) {
         this.resumeStoryTimer();
      }

   }

   protected void onDestroy() {
      super.onDestroy();
      if (this.progressAnimator != null) {
         this.progressAnimator.cancel();
      }

   }
}
