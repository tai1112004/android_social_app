package com.yourapp.ui.home;

import android.util.Log;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yourapp.R;
import com.yourapp.data.remote.StoryApiService;
import com.yourapp.data.remote.ConversationApiService;
import com.yourapp.data.remote.MessageApiService;
import com.yourapp.model.ApiResponse;
import com.yourapp.model.Story;
import com.yourapp.model.StoryGroup;
import com.yourapp.model.Conversation;
import com.yourapp.model.CreateConversationRequest;
import com.yourapp.model.Message;
import com.yourapp.model.SendMessageRequest;
import com.yourapp.network.RetrofitClient;
import com.yourapp.ui.chat.ChatActivity;
import com.yourapp.util.AvatarUtil;
import com.yourapp.util.TokenManager;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StoryViewerActivity extends AppCompatActivity {

    private static final long STORY_DURATION_MS = 5000;

    private ImageView ivStoryContent;
    private ProgressBar pbStory;
    private ImageView ivViewerAvatar;
    private TextView tvViewerAvatar;
    private TextView tvViewerAuthor;
    private TextView tvViewerTime;
    private TextView tvViewerCaption;
    private ImageButton btnViewerClose;
    private ImageButton btnViewerVolume;
    private boolean isMuted = false;
    private TextView btnStoryReply;
    private TextView btnStoryHeart;
    private TextView btnStoryLaugh;
    private TextView btnStoryWow;
    private View musicBarOverlay;
    private ImageView ivOverlayMusicCover;
    private TextView tvOverlayMusicTitle;
    private TextView tvOverlayMusicArtist;
    private View musicWave1;
    private View musicWave2;
    private View musicWave3;

    private ValueAnimator progressAnimator;
    private MediaPlayer storyMusicPlayer;
    private ObjectAnimator albumRotateAnimator;
    private AnimatorSet waveAnimatorSet;
    private boolean dialogVisible;
    private final List<StoryGroup> storyGroups = new ArrayList<>();
    private int currentGroupIndex = 0;

    private Long storyAuthorId;
    private String storyAuthorName;
    private String storyAuthorUsername;
    private String storyAuthorAvatarUrl;
    private String storyCaption;
    private String storyMediaUrl;
    private String musicTitle;
    private String musicArtist;
    private String musicPreviewUrl;
    private String musicAlbumCover;
    private String storyTime;
    private Long storyId;
    private ImageButton btnViewerDelete;
    private TokenManager tokenManager;
    private ConversationApiService conversationApiService;
    private MessageApiService messageApiService;
    private StoryApiService storyApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.activity_story_viewer);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

bindViews();
        setupViewerButtons();
        tokenManager = new TokenManager(this);
        conversationApiService = RetrofitClient.getInstance(tokenManager).create(ConversationApiService.class);
        messageApiService = RetrofitClient.getInstance(tokenManager).create(MessageApiService.class);
        storyApiService = RetrofitClient.getInstance(tokenManager).create(StoryApiService.class);

        loadStoryStateFromIntent();
        renderCurrentStory();
    }

    private void setupViewerButtons() {
        btnViewerClose.setOnClickListener(v -> finish());
        btnViewerDelete.setOnClickListener(v -> deleteStory());
        btnViewerVolume.setOnClickListener(v -> toggleVolume());

        btnViewerClose.bringToFront();
        btnViewerDelete.bringToFront();
        btnViewerVolume.bringToFront();
        btnViewerClose.setElevation(24f);
        btnViewerDelete.setElevation(24f);
        btnViewerVolume.setElevation(24f);
    }
    private void bindViews() {
        ivStoryContent = findViewById(R.id.iv_story_content);
        pbStory = findViewById(R.id.pb_story);
        ivViewerAvatar = findViewById(R.id.iv_viewer_avatar);
        tvViewerAvatar = findViewById(R.id.tv_viewer_avatar);
        tvViewerAuthor = findViewById(R.id.tv_viewer_author);
        tvViewerTime = findViewById(R.id.tv_viewer_time);
        tvViewerCaption = findViewById(R.id.tv_viewer_caption);
        btnViewerClose = findViewById(R.id.btn_viewer_close);
        btnViewerVolume = findViewById(R.id.btn_viewer_volume);
        btnViewerDelete = findViewById(R.id.btn_viewer_delete);
        btnStoryReply = findViewById(R.id.btn_story_reply);
        btnStoryHeart = findViewById(R.id.btn_story_heart);
        btnStoryLaugh = findViewById(R.id.btn_story_laugh);
        btnStoryWow = findViewById(R.id.btn_story_wow);
        musicBarOverlay = findViewById(R.id.music_bar_overlay);
        ivOverlayMusicCover = findViewById(R.id.iv_overlay_music_cover);
        tvOverlayMusicTitle = findViewById(R.id.tv_overlay_music_title);
        tvOverlayMusicArtist = findViewById(R.id.tv_overlay_music_artist);
        musicWave1 = findViewById(R.id.music_wave_1);
        musicWave2 = findViewById(R.id.music_wave_2);
        musicWave3 = findViewById(R.id.music_wave_3);
    }

    private void loadStoryStateFromIntent() {
        String groupsJson = getIntent().getStringExtra("storyGroupsJson");
        storyGroups.clear();
        if (!TextUtils.isEmpty(groupsJson)) {
            Type type = new TypeToken<List<StoryGroup>>() {}.getType();
            List<StoryGroup> parsed = new Gson().fromJson(groupsJson, type);
            if (parsed != null) {
                for (StoryGroup group : parsed) {
                    if (group != null && group.getStories() != null && !group.getStories().isEmpty()) {
                        storyGroups.add(group);
                    }
                }
            }
            currentGroupIndex = getIntent().getIntExtra("startGroupIndex", 0);
            if (currentGroupIndex < 0 || currentGroupIndex >= storyGroups.size()) {
                currentGroupIndex = 0;
            }
        }
        if (storyGroups.isEmpty()) {
            storyGroups.add(buildFallbackGroupFromIntent());
            currentGroupIndex = 0;
        }
        for (StoryGroup group : storyGroups) {
            if (group.getCurrentIndex() < 0 || group.getCurrentIndex() >= group.getStories().size()) {
                group.setCurrentIndex(0);
            }
        }
        syncCurrentStoryFields();
    }

    private StoryGroup buildFallbackGroupFromIntent() {
        Story story = new Story();
        story.setId(getIntent().getLongExtra("storyId", -1L));
        story.setAuthorId(getIntent().getLongExtra("authorId", -1L));
        story.setAuthorUsername(getIntent().getStringExtra("authorUsername"));
        story.setAuthorDisplayName(getIntent().getStringExtra("authorName"));
        story.setAuthorAvatarUrl(getIntent().getStringExtra("authorAvatarUrl"));
        story.setMediaUrl(getIntent().getStringExtra("mediaUrl"));
        story.setContent(getIntent().getStringExtra("content"));
        story.setMusicTitle(getIntent().getStringExtra("musicTitle"));
        story.setMusicArtist(getIntent().getStringExtra("musicArtist"));
        story.setMusicPreviewUrl(getIntent().getStringExtra("musicPreviewUrl"));
        story.setMusicAlbumCover(getIntent().getStringExtra("musicAlbumCover"));
        story.setCreatedAt(getIntent().getStringExtra("time"));

        StoryGroup group = new StoryGroup();
        group.setUserId(story.getAuthorId());
        group.setUserName(story.getAuthorDisplayName());
        group.setUserAvatarUrl(story.getAuthorAvatarUrl());
        group.setStories(Collections.singletonList(story));
        group.setHasUnviewed(true);
        group.setLatestStoryTime(story.getCreatedAt());
        group.setCurrentIndex(0);
        return group;
    }

    private StoryGroup currentGroup() {
        if (storyGroups.isEmpty()) {
            return null;
        }
        if (currentGroupIndex < 0 || currentGroupIndex >= storyGroups.size()) {
            currentGroupIndex = 0;
        }
        return storyGroups.get(currentGroupIndex);
    }

    private Story currentStory() {
        StoryGroup group = currentGroup();
        if (group == null || group.getStories() == null || group.getStories().isEmpty()) {
            return null;
        }
        if (group.getCurrentIndex() < 0 || group.getCurrentIndex() >= group.getStories().size()) {
            group.setCurrentIndex(0);
        }
        return group.getStories().get(group.getCurrentIndex());
    }

    private void syncCurrentStoryFields() {
        Story story = currentStory();
        if (story == null) {
            storyId = -1L;
            storyAuthorId = -1L;
            storyAuthorName = null;
            storyAuthorUsername = null;
            storyAuthorAvatarUrl = null;
            storyCaption = null;
            storyMediaUrl = null;
            musicTitle = null;
            musicArtist = null;
            musicPreviewUrl = null;
            musicAlbumCover = null;
            storyTime = null;
            return;
        }
        storyId = story.getId();
        storyAuthorId = story.getAuthorId();
        storyAuthorName = story.getAuthorDisplayName();
        storyAuthorUsername = story.getAuthorUsername();
        storyAuthorAvatarUrl = story.getAuthorAvatarUrl();
        storyCaption = story.getContent();
        storyMediaUrl = story.getMediaUrl();
        musicTitle = story.getMusicTitle();
        musicArtist = story.getMusicArtist();
        musicPreviewUrl = story.getMusicPreviewUrl();
        musicAlbumCover = story.getMusicAlbumCover();
        storyTime = story.getCreatedAt();
    }

    private void renderCurrentStory() {
        syncCurrentStoryFields();
        tvViewerAuthor.setText(safeDisplayName());
        tvViewerTime.setText(TextUtils.isEmpty(storyTime) ? "Vua xong" : formatTime(storyTime));
        AvatarUtil.loadAvatar(storyAuthorAvatarUrl, storyAuthorName, storyAuthorUsername, ivViewerAvatar, tvViewerAvatar);

        if (!TextUtils.isEmpty(storyCaption)) {
            tvViewerCaption.setVisibility(View.VISIBLE);
            tvViewerCaption.setText(storyCaption);
        } else {
            tvViewerCaption.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(storyMediaUrl)) {
            Glide.with(this).load(storyMediaUrl).centerCrop().into(ivStoryContent);
        } else {
            ivStoryContent.setImageDrawable(null);
        }

        if (tokenManager.getUserId() == storyAuthorId && storyAuthorId != -1L) {
            btnViewerDelete.setVisibility(View.VISIBLE);
        } else {
            btnViewerDelete.setVisibility(View.GONE);
        }

        setupMusicOverlay();
        startStoryMusic();
        startProgress();
        markCurrentStoryViewed();
    }

    private void navigateNext() {
        StoryGroup group = currentGroup();
        if (group == null) {
            finish();
            return;
        }
        if (group.getCurrentIndex() < group.getStories().size() - 1) {
            group.setCurrentIndex(group.getCurrentIndex() + 1);
            releaseStoryMusic();
            renderCurrentStory();
            return;
        }
        if (currentGroupIndex < storyGroups.size() - 1) {
            currentGroupIndex++;
            storyGroups.get(currentGroupIndex).setCurrentIndex(0);
            releaseStoryMusic();
            renderCurrentStory();
            return;
        }
        finish();
    }

    private void navigatePrevious() {
        StoryGroup group = currentGroup();
        if (group == null) {
            finish();
            return;
        }
        if (group.getCurrentIndex() > 0) {
            group.setCurrentIndex(group.getCurrentIndex() - 1);
            releaseStoryMusic();
            renderCurrentStory();
            return;
        }
        if (currentGroupIndex > 0) {
            currentGroupIndex--;
            StoryGroup prev = currentGroup();
            if (prev != null) {
                prev.setCurrentIndex(Math.max(0, prev.getStories().size() - 1));
            }
            releaseStoryMusic();
            renderCurrentStory();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event != null && event.getAction() == MotionEvent.ACTION_UP && !dialogVisible) {
            float width = getWindow().getDecorView().getWidth();
            if (event.getX() < width / 2f) {
                navigatePrevious();
            } else {
                navigateNext();
            }
            return true;
        }
        return super.onTouchEvent(event);
    }
    private void markCurrentStoryViewed() {
        if (storyApiService == null || storyId == null || storyId <= 0 || isValidStoryAuthor()) {
            return;
        }
        storyApiService.markStoryViewed(storyId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                Log.w("StoryViewer", "Failed to mark story viewed", t);
            }
        });
    }
    private void setupMusicOverlay() {
        if (TextUtils.isEmpty(musicPreviewUrl)) {
            musicBarOverlay.setVisibility(View.GONE);
            btnViewerVolume.setVisibility(View.GONE);
            return;
        }
        musicBarOverlay.setVisibility(View.VISIBLE);
        btnViewerVolume.setVisibility(View.VISIBLE);
        btnViewerVolume.setImageResource(isMuted ? R.drawable.ic_volume_off : R.drawable.ic_volume_up);
        tvOverlayMusicTitle.setText(TextUtils.isEmpty(musicTitle) ? "Story music" : musicTitle);
        tvOverlayMusicTitle.setSelected(true);
        tvOverlayMusicArtist.setText(TextUtils.isEmpty(musicArtist) ? "Deezer preview" : musicArtist);
        if (!TextUtils.isEmpty(musicAlbumCover)) {
            Glide.with(this).load(musicAlbumCover).centerCrop().into(ivOverlayMusicCover);
        }
        startMusicAnimations();
    }

    private void toggleVolume() {
        isMuted = !isMuted;
        if (storyMusicPlayer != null) {
            try {
                if (isMuted) {
                    storyMusicPlayer.setVolume(0f, 0f);
                    btnViewerVolume.setImageResource(R.drawable.ic_volume_off);
                } else {
                    storyMusicPlayer.setVolume(1f, 1f);
                    btnViewerVolume.setImageResource(R.drawable.ic_volume_up);
                }
            } catch (Exception ignored) {}
        }
    }

    private void startStoryMusic() {
        Log.d("StoryViewer", "startStoryMusic called. URL: " + musicPreviewUrl);
        if (TextUtils.isEmpty(musicPreviewUrl)) return;
        try {
            storyMusicPlayer = new MediaPlayer();
            storyMusicPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            storyMusicPlayer.setDataSource(musicPreviewUrl);
            storyMusicPlayer.setLooping(true);
            storyMusicPlayer.setOnPreparedListener(mp -> {
                Log.d("StoryViewer", "MediaPlayer prepared successfully. Playing...");
                if (isMuted) {
                    mp.setVolume(0f, 0f);
                } else {
                    mp.setVolume(1f, 1f);
                }
                mp.start();
            });
            storyMusicPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e("StoryViewer", "MediaPlayer error: what=" + what + ", extra=" + extra);
                musicBarOverlay.setVisibility(View.GONE);
                btnViewerVolume.setVisibility(View.GONE);
                stopMusicAnimations();
                return true;
            });
            storyMusicPlayer.prepareAsync();
        } catch (Exception e) {
            Log.e("StoryViewer", "MediaPlayer setup exception: ", e);
            musicBarOverlay.setVisibility(View.GONE);
            btnViewerVolume.setVisibility(View.GONE);
            stopMusicAnimations();
        }
    }

    private void startMusicAnimations() {
        albumRotateAnimator = ObjectAnimator.ofFloat(ivOverlayMusicCover, "rotation", 0f, 360f);
        albumRotateAnimator.setDuration(8000);
        albumRotateAnimator.setRepeatCount(ValueAnimator.INFINITE);
        albumRotateAnimator.setInterpolator(new LinearInterpolator());
        albumRotateAnimator.start();

        ObjectAnimator bar1 = waveAnimator(musicWave1, 0);
        ObjectAnimator bar2 = waveAnimator(musicWave2, 200);
        ObjectAnimator bar3 = waveAnimator(musicWave3, 400);
        waveAnimatorSet = new AnimatorSet();
        waveAnimatorSet.playTogether(bar1, bar2, bar3);
        waveAnimatorSet.start();
    }

    private ObjectAnimator waveAnimator(View view, long delay) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "scaleY", 0.35f, 1f, 0.35f);
        animator.setDuration(600);
        animator.setStartDelay(delay);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        return animator;
    }

    private void pauseStoryMusic() {
        if (storyMusicPlayer != null && storyMusicPlayer.isPlaying()) storyMusicPlayer.pause();
        if (albumRotateAnimator != null && albumRotateAnimator.isStarted()) albumRotateAnimator.pause();
        if (waveAnimatorSet != null && waveAnimatorSet.isStarted()) waveAnimatorSet.pause();
    }

    private void resumeStoryMusic() {
        if (storyMusicPlayer != null) {
            try { storyMusicPlayer.start(); } catch (Exception ignored) {}
        }
        if (albumRotateAnimator != null && albumRotateAnimator.isPaused()) albumRotateAnimator.resume();
        if (waveAnimatorSet != null && waveAnimatorSet.isPaused()) waveAnimatorSet.resume();
    }

    private void releaseStoryMusic() {
        if (storyMusicPlayer != null) {
            try { storyMusicPlayer.stop(); } catch (Exception ignored) {}
            storyMusicPlayer.release();
            storyMusicPlayer = null;
        }
        stopMusicAnimations();
    }

    private void stopMusicAnimations() {
        if (albumRotateAnimator != null) albumRotateAnimator.cancel();
        if (waveAnimatorSet != null) waveAnimatorSet.cancel();
    }

    private String safeDisplayName() {
        if (!TextUtils.isEmpty(storyAuthorName)) return storyAuthorName;
        return !TextUtils.isEmpty(storyAuthorUsername) ? storyAuthorUsername : "Story";
    }

    private void stopStoryMusic() {
        releaseStoryMusic();
    }

    private void startProgress() {
        progressAnimator = ValueAnimator.ofInt(0, 100);
        progressAnimator.setDuration(STORY_DURATION_MS);
        progressAnimator.setInterpolator(new LinearInterpolator());
        progressAnimator.addUpdateListener(animation -> {
            int progress = (int) animation.getAnimatedValue();
            pbStory.setProgress(progress);
            if (progress >= 100) navigateNext();
        });
        progressAnimator.start();
    }

    private void pauseStoryTimer() {
        if (progressAnimator != null && progressAnimator.isStarted() && !progressAnimator.isPaused()) progressAnimator.pause();
    }

    private void resumeStoryTimer() {
        if (progressAnimator != null && progressAnimator.isStarted() && progressAnimator.isPaused()) progressAnimator.resume();
    }

    private String formatTime(String createdAt) {
        if (createdAt == null) return "";
        try {
            java.time.LocalDateTime time = java.time.LocalDateTime.parse(createdAt.substring(0, 19));
            long minutes = java.time.Duration.between(time, java.time.LocalDateTime.now()).toMinutes();
            if (minutes < 1) return "Vua xong";
            if (minutes < 60) return minutes + " phut truoc";
            long hours = minutes / 60;
            if (hours < 24) return hours + " gio truoc";
            return (hours / 24) + " ngay truoc";
        } catch (Exception e) {
            return createdAt.length() > 16 ? createdAt.substring(0, 16).replace("T", " ") : createdAt;
        }
    }

    private boolean isValidStoryAuthor() {
        return storyAuthorId != null && storyAuthorId > 0;
    }

    private void showStoryReplyDialog() {
        if (!isValidStoryAuthor()) {
            Toast.makeText(this, "Khong tim thay chu story", Toast.LENGTH_SHORT).show();
            return;
        }
        pauseStoryTimer();
        pauseStoryMusic();
        dialogVisible = true;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_story_reply, null);
        EditText input = dialogView.findViewById(R.id.et_story_reply_input);
        TextView tvPreview = dialogView.findViewById(R.id.tv_story_reply_preview);
        TextView btnCancel = dialogView.findViewById(R.id.btn_story_reply_cancel);
        TextView btnSend = dialogView.findViewById(R.id.btn_story_reply_send);

        tvPreview.setText(buildStoryQuotePreview());
        if (!TextUtils.isEmpty(storyCaption)) {
            input.setText("Ve story cua " + safeDisplayName() + ": ");
            input.setSelection(input.getText().length());
        }

        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSend.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (TextUtils.isEmpty(text)) {
                Toast.makeText(this, "Noi dung khong duoc de trong", Toast.LENGTH_SHORT).show();
                return;
            }
            sendStoryMessage(text, "TEXT", true, dialog);
        });
        dialog.setOnDismissListener(d -> {
            dialogVisible = false;
            resumeStoryTimer();
            resumeStoryMusic();
        });
        dialog.show();
    }

    private void sendQuickReaction(String reaction) {
        if (!isValidStoryAuthor()) {
            Toast.makeText(this, "Khong tim thay chu story", Toast.LENGTH_SHORT).show();
            return;
        }
        sendStoryMessage(reaction, "EMOJI", false, null);
    }

    private void sendStoryMessage(String content, String type, boolean openChatAfterSend, AlertDialog dialog) {
        CreateConversationRequest request = new CreateConversationRequest("PRIVATE", null, Collections.singletonList(storyAuthorId));
        conversationApiService.createConversation(request).enqueue(new Callback<ApiResponse<Conversation>>() {
            @Override public void onResponse(Call<ApiResponse<Conversation>> call, Response<ApiResponse<Conversation>> response) {
                if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess() || response.body().getData() == null) {
                    Toast.makeText(StoryViewerActivity.this, "Khong mo duoc chat", Toast.LENGTH_SHORT).show();
                    if (dialog != null) dialog.dismiss();
                    return;
                }
                Conversation conversation = response.body().getData();
                SendMessageRequest messageRequest = new SendMessageRequest(content, type);
                messageRequest.setReplyPreview("Story: " + buildStoryQuotePreview());
                messageRequest.setReplyContextType("STORY");
                messageRequest.setReplyContextId(-1L);
                messageRequest.setReplyContextAuthorId(storyAuthorId);
                messageRequest.setReplyContextAuthorUsername(storyAuthorUsername);
                messageRequest.setReplyContextAuthorDisplayName(safeDisplayName());
                messageRequest.setReplyContextText(storyCaption);
                messageRequest.setReplyContextMediaUrl(storyMediaUrl);
                messageApiService.sendMessage(conversation.getId(), messageRequest).enqueue(new Callback<ApiResponse<Message>>() {
                    @Override public void onResponse(Call<ApiResponse<Message>> call, Response<ApiResponse<Message>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            Toast.makeText(StoryViewerActivity.this, "Da gui vao chat", Toast.LENGTH_SHORT).show();
                            if (dialog != null) dialog.dismiss();
                            if (openChatAfterSend) openChat(conversation.getId(), conversation.getName());
                        } else {
                            Toast.makeText(StoryViewerActivity.this, "Khong gui duoc tin nhan", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onFailure(Call<ApiResponse<Message>> call, Throwable t) {
                        Toast.makeText(StoryViewerActivity.this, "Loi ket noi", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override public void onFailure(Call<ApiResponse<Conversation>> call, Throwable t) {
                Toast.makeText(StoryViewerActivity.this, "Loi tao chat", Toast.LENGTH_SHORT).show();
                if (dialog != null) dialog.dismiss();
            }
        });
    }

    private String buildStoryQuotePreview() {
        StringBuilder builder = new StringBuilder();
        builder.append(safeDisplayName()).append("\n");
        if (!TextUtils.isEmpty(musicTitle)) builder.append("Music: ").append(musicTitle).append(" - ").append(musicArtist).append("\n");
        if (!TextUtils.isEmpty(storyCaption)) builder.append(trimToLength(storyCaption.trim(), 120));
        else builder.append("(khong co caption)");
        return builder.toString();
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength - 1) + "...";
    }

    private void deleteStory() {
        if (storyId == null || storyId == -1L) return;
        pauseStoryTimer();
        pauseStoryMusic();
        new AlertDialog.Builder(this)
                .setTitle("Xóa tin")
                .setMessage("Bạn có chắc chắn muốn xóa tin này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    StoryApiService localStoryApiService = storyApiService != null ? storyApiService : RetrofitClient.getInstance(tokenManager).create(StoryApiService.class);
                    localStoryApiService.deleteStory(storyId).enqueue(new Callback<ApiResponse<Void>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(StoryViewerActivity.this, "Đã xóa tin thành công", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(StoryViewerActivity.this, "Xóa tin thất bại", Toast.LENGTH_SHORT).show();
                                resumeStoryTimer();
                                resumeStoryMusic();
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                            Toast.makeText(StoryViewerActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                            resumeStoryTimer();
                            resumeStoryMusic();
                        }
                    });
                })
                .setNegativeButton("Hủy", (dialog, which) -> {
                    resumeStoryTimer();
                    resumeStoryMusic();
                })
                .setOnCancelListener(dialog -> {
                    resumeStoryTimer();
                    resumeStoryMusic();
                })
                .show();
    }

    private void openChat(Long conversationId, String conversationName) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversationId);
        intent.putExtra(ChatActivity.EXTRA_CONVERSATION_NAME,
                conversationName != null && !conversationName.isEmpty() ? conversationName : safeDisplayName());
        startActivity(intent);
    }

    @Override protected void onPause() {
        super.onPause();
        if (!dialogVisible) {
            pauseStoryTimer();
            pauseStoryMusic();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (!dialogVisible) {
            resumeStoryTimer();
            resumeStoryMusic();
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (progressAnimator != null) progressAnimator.cancel();
        releaseStoryMusic();
    }
}






















