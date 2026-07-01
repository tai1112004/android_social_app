package com.yourapp.ui.chat;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.LayoutInflater;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.yourapp.R;
import com.yourapp.data.remote.MediaApiService;
import com.yourapp.data.remote.MessageApiService;
import com.yourapp.data.remote.ConversationApiService;
import com.yourapp.data.repository.AuthRepository;
import com.yourapp.data.repository.ConversationRepository;
import com.yourapp.data.repository.MessageRepository;
import com.yourapp.model.ApiResponse;
import com.yourapp.model.MediaUpload;
import com.yourapp.model.Message;
import com.yourapp.model.SendMessageRequest;
import com.yourapp.network.RetrofitClient;
import com.yourapp.realtime.ChatSocketClient;
import com.yourapp.ui.auth.LoginActivity;
import com.yourapp.util.ContentUriRequestBody;
import com.yourapp.util.TokenManager;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_CONVERSATION_ID = "conversation_id";
    public static final String EXTRA_CONVERSATION_NAME = "conversation_name";

    private Long conversationId;
    private EditText etMessage;
    private View btnSend;
    private View btnMicrophone;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private View replyPreviewBar;
    private TextView tvReplyPreview;
    private RecyclerView rvMessages;
    private MessageAdapter adapter;
    private ChatViewModel viewModel;
    private MediaApiService mediaApiService;
    private ConversationRepository conversationRepository;
    private ChatSocketClient chatSocketClient;
    private Message replyingTo;
    private ActivityResultLauncher<Intent> mediaPickerLauncher;
    private String pendingMediaType;

    private final java.util.Set<Long> messageIds = new java.util.HashSet<>();
    private boolean isFirstLoad = true;
    private String currentUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        conversationId = getIntent().getLongExtra(EXTRA_CONVERSATION_ID, -1L);
        String conversationName = getIntent().getStringExtra(EXTRA_CONVERSATION_NAME);
        if (conversationId == -1L) {
            Toast.makeText(this, "Không tìm thấy cuộc trò chuyện", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TokenManager tokenManager = new TokenManager(this);
        if (!tokenManager.hasValidTokens()) {
            redirectToLogin();
            return;
        }

        registerMediaPicker();
        setupViews(conversationName);
        setupViewModel(tokenManager);
        observeViewModel(tokenManager);

        btnSend.setOnClickListener(v -> sendMessage());
        findViewById(R.id.btn_chat_info).setOnClickListener(v -> openChatInfo());
        findViewById(R.id.btn_chat_call).setOnClickListener(v -> Toast.makeText(this, "Tinh nang goi thoai se bo sung sau", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btn_chat_video_call).setOnClickListener(v -> Toast.makeText(this, "Tinh nang goi video se bo sung sau", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btn_cancel_reply).setOnClickListener(v -> clearReply());
        findViewById(R.id.btn_send_emoji).setOnClickListener(v -> Toast.makeText(this, "Sticker, file va location se bo sung sau", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btn_send_image).setOnClickListener(v -> openMediaPicker("IMAGE"));

        viewModel.loadMessages(conversationId);
        markConversationRead();
        connectChatSocket();
    }

    @Override
    protected void onResume() {
        super.onResume();
        markConversationRead();
        connectChatSocket();
    }

    @Override
    protected void onPause() {
        super.onPause();
        disconnectChatSocket();
    }

    private void registerMediaPicker() {
        mediaPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK
                            && result.getData() != null
                            && result.getData().getData() != null) {
                        Uri uri = result.getData().getData();
                        int flags = result.getData().getFlags()
                                & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        try {
                            getContentResolver().takePersistableUriPermission(uri, flags);
                        } catch (SecurityException ignored) {
                            // Some providers grant temporary read access only.
                        }
                        sendPickedMedia(uri);
                    }
                });
    }

    private void setupViews(String conversationName) {
        Toolbar toolbar = findViewById(R.id.toolbar_chat);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setTitle("");
        }

        // Inflate toolbar rieng de tranh title/subtitle mac dinh lam lech avatar va trang thai.
        toolbar.removeAllViews();
        GradientDrawable toolbarBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF0084FF, 0xFF9B59B6});
        toolbar.setBackground(toolbarBg);
        LayoutInflater.from(this).inflate(R.layout.toolbar_chat, toolbar, true);
        String title = conversationName != null && !conversationName.isEmpty() ? conversationName : "Tin nhan";
        ((TextView) findViewById(R.id.tv_chat_title)).setText(title);
        ((TextView) findViewById(R.id.tv_chat_subtitle)).setText("Đang hoạt động");
        ((TextView) findViewById(R.id.tv_chat_avatar)).setText(title.substring(0, 1).toUpperCase());
        GradientDrawable onlineDot = new GradientDrawable();
        onlineDot.setShape(GradientDrawable.OVAL);
        onlineDot.setColor(0xFF44FF44);
        onlineDot.setStroke(dp(2), 0xFFFFFFFF);
        findViewById(R.id.view_chat_online).setBackground(onlineDot);
        findViewById(R.id.btn_chat_back).setOnClickListener(v -> finish());

        // Inflate thanh input rieng de chi con: add, camera, edit text, micro/send.
        LinearLayout inputBar = findViewById(R.id.message_input_bar);
        inputBar.removeAllViews();
        LayoutInflater.from(this).inflate(R.layout.view_chat_input, inputBar, true);

        rvMessages = findViewById(R.id.rv_messages);
        progressBar = findViewById(R.id.progress_chat);
        tvEmptyState = findViewById(R.id.tv_chat_empty_state);
        replyPreviewBar = findViewById(R.id.reply_preview_bar);
        tvReplyPreview = findViewById(R.id.tv_reply_preview);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        btnMicrophone = findViewById(R.id.btn_microphone);
        setupInputToggle();

        TokenManager tokenManager = new TokenManager(this);
        adapter = new MessageAdapter(tokenManager.getUsername(), new MessageAdapter.OnMessageActionListener() {
            @Override
            public void onLongPress(Message message) {
                showMessageActions(message);
            }

            @Override
            public void onMediaClick(Message message) {
                openPhotoViewer(message);
            }

            @Override
            public void onDelete(Message message) {
                confirmDeleteMessage(message);
            }
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);
        setupSwipeToReply();
    }

    private void setupInputToggle() {
        updateSendMode(false);
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSendMode(s != null && s.toString().trim().length() > 0);
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void updateSendMode(boolean hasText) {
        // Crossfade 150ms: co text thi hien nut gui, rong thi hien micro.
        View showView = hasText ? btnSend : btnMicrophone;
        View hideView = hasText ? btnMicrophone : btnSend;
        showView.setVisibility(View.VISIBLE);
        showView.setAlpha(0f);
        showView.animate().alpha(1f).setDuration(150).start();
        hideView.animate().alpha(0f).setDuration(150).withEndAction(() -> hideView.setVisibility(View.GONE)).start();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void setupSwipeToReply() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView,
                                  RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
                return 0.22f;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) {
                    return;
                }
                Message message = adapter.getMessageAt(position);
                adapter.notifyItemChanged(position);
                if (message != null) {
                    startReply(message);
                }
            }

            @Override
            public void onChildDraw(Canvas c,
                                    RecyclerView recyclerView,
                                    RecyclerView.ViewHolder viewHolder,
                                    float dX,
                                    float dY,
                                    int actionState,
                                    boolean isCurrentlyActive) {
                float limitedDx = Math.max(0f, Math.min(dX, 160f));
                super.onChildDraw(c, recyclerView, viewHolder, limitedDx, dY, actionState, isCurrentlyActive);
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(rvMessages);
    }

    private void setupViewModel(TokenManager tokenManager) {
        currentUsername = tokenManager.getUsername();
        MessageApiService apiService = RetrofitClient.getInstance(tokenManager)
                .create(MessageApiService.class);
        MessageRepository repository = new MessageRepository(apiService);
        mediaApiService = RetrofitClient.getInstance(tokenManager).create(MediaApiService.class);

        ConversationApiService conversationApiService = RetrofitClient.getInstance(tokenManager)
                .create(ConversationApiService.class);
        conversationRepository = new ConversationRepository(conversationApiService);

        viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @Override
            @SuppressWarnings("unchecked")
            public <T extends androidx.lifecycle.ViewModel> T create(Class<T> modelClass) {
                return (T) new ChatViewModel(repository);
            }
        }).get(ChatViewModel.class);
    }

    private void observeViewModel(TokenManager tokenManager) {
        viewModel.getMessages().observe(this, messages -> {
            adapter.setMessages(messages);
            boolean empty = messages == null || messages.isEmpty();
            tvEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            rvMessages.setVisibility(empty ? View.GONE : View.VISIBLE);
            if (messages != null && !messages.isEmpty()) {
                boolean hasNewIncoming = false;

                for (Message m : messages) {
                    if (m.getId() != null && !messageIds.contains(m.getId())) {
                        messageIds.add(m.getId());
                        if (!isFirstLoad && currentUsername != null && !currentUsername.equals(m.getSenderUsername())) {
                            hasNewIncoming = true;
                        }
                    }
                }
                if (hasNewIncoming) {
                    playNotificationSoundAndVibrate();
                }
                isFirstLoad = false;
                rvMessages.scrollToPosition(messages.size() - 1);
                markConversationRead();
            } else {
                isFirstLoad = false;
            }
        });

        viewModel.getLoading().observe(this, isLoading ->
                progressBar.setVisibility(isLoading != null && isLoading
                        ? View.VISIBLE
                        : View.GONE));

        viewModel.getSending().observe(this, isSending ->
                btnSend.setEnabled(isSending == null || !isSending));

        viewModel.getError().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                if ("UNAUTHORIZED".equals(errorMsg)) {
                    redirectToLogin();
                } else {
                    Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void connectChatSocket() {
        if (chatSocketClient != null) {
            return;
        }
        TokenManager tokenManager = new TokenManager(this);
        String token = tokenManager.getAccessToken();
        Long userId = tokenManager.getUserId();
        if (token == null || userId == null || userId <= 0) {
            return;
        }
        chatSocketClient = new ChatSocketClient(
                com.yourapp.util.Constants.getWsUrl(),
                token,
                conversationId,
                message -> {
                    if (message == null || message.getId() == null) {
                        return;
                    }
                    if (messageIds.contains(message.getId())) {
                        return;
                    }
                    messageIds.add(message.getId());
                    viewModel.appendIncomingMessage(message);
                    if (currentUsername == null || !currentUsername.equals(message.getSenderUsername())) {
                        markConversationRead();
                    }
                });
        chatSocketClient.connect();
    }

    private void disconnectChatSocket() {
        if (chatSocketClient != null) {
            chatSocketClient.disconnect();
            chatSocketClient = null;
        }
    }

    private void markConversationRead() {
        if (conversationRepository == null || conversationId == null || conversationId == -1L) {
            return;
        }
        conversationRepository.markConversationRead(conversationId).observe(this, result -> {
            if (result instanceof AuthRepository.Result.Error) {
                AuthRepository.Result.Error<Object> err = (AuthRepository.Result.Error<Object>) result;
                if ("UNAUTHORIZED".equals(err.message)) {
                    redirectToLogin();
                }
            }
        });
    }
    private void sendMessage() {
        String content = etMessage.getText().toString().trim();
        if (content.isEmpty()) {
            return;
        }
        etMessage.setText("");
        hideKeyboard();
        SendMessageRequest request = new SendMessageRequest(content, "TEXT");
        attachReply(request);
        clearReply();
        viewModel.sendMessage(conversationId, request);
    }

    private void showMessageActions(Message message) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(18, 16, 18, 16);

        LinearLayout reactionRow = new LinearLayout(this);
        reactionRow.setOrientation(LinearLayout.HORIZONTAL);
        reactionRow.setGravity(android.view.Gravity.CENTER);

        final AlertDialog[] dialogRef = new AlertDialog[1];
        String[] reactions = {"❤️", "😍", "😂", "😮", "😢", "😡"};
        for (String reaction : reactions) {
            TextView chip = new TextView(this);
            chip.setText(reaction);
            chip.setTextSize(28f);
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setMinWidth(54);
            chip.setMinHeight(54);
            chip.setOnClickListener(v -> {
                sendReaction(message, reaction);
                if (dialogRef[0] != null) {
                    dialogRef[0].dismiss();
                }
            });
            reactionRow.addView(chip);
        }

        LinearLayout actionsRow = new LinearLayout(this);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setPadding(0, 16, 0, 0);
        actionsRow.setGravity(android.view.Gravity.CENTER_HORIZONTAL);

        TextView btnReply = new TextView(this);
        btnReply.setText("Trả lời");
        btnReply.setPadding(20, 14, 20, 14);
        btnReply.setBackgroundResource(R.drawable.bg_soft_pill);
        btnReply.setOnClickListener(v -> {
            if (dialogRef[0] != null) dialogRef[0].dismiss();
            startReply(message);
        });
        actionsRow.addView(btnReply);

        if (currentUsername != null && currentUsername.equals(message.getSenderUsername())) {
            TextView btnDelete = new TextView(this);
            btnDelete.setText("Xóa");
            btnDelete.setPadding(20, 14, 20, 14);
            btnDelete.setBackgroundResource(R.drawable.bg_soft_pill);
            btnDelete.setTextColor(0xFFDC2626);
            LinearLayout.LayoutParams deleteLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            deleteLp.leftMargin = 12;
            btnDelete.setLayoutParams(deleteLp);
            btnDelete.setOnClickListener(v -> {
                if (dialogRef[0] != null) dialogRef[0].dismiss();
                confirmDeleteMessage(message);
            });
            actionsRow.addView(btnDelete);
        }

        row.addView(reactionRow);
        row.addView(actionsRow);

        dialogRef[0] = new AlertDialog.Builder(this)
                .setView(row)
                .create();
        dialogRef[0].show();
    }

    private void confirmDeleteMessage(Message message) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa tin nhắn")
                .setMessage("Bạn muốn xóa tin nhắn này?")
                .setPositiveButton("Xóa", (dialog, which) -> viewModel.deleteMessage(message))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void startReply(Message message) {
        replyingTo = message;
        replyPreviewBar.setVisibility(View.VISIBLE);
        tvReplyPreview.setText("Đang trả lời: " + previewFor(message));
        etMessage.requestFocus();
    }

    private String previewFor(Message message) {
        if (message.getContent() != null && !message.getContent().isEmpty()) {
            return message.getContent();
        }
        if ("IMAGE".equals(message.getType())) {
            return "Ảnh";
        }
        if ("VIDEO".equals(message.getType())) {
            return "Video";
        }
        return "Tin nhắn";
    }

    private void clearReply() {
        replyingTo = null;
        replyPreviewBar.setVisibility(View.GONE);
        tvReplyPreview.setText("");
    }

    private void attachReply(SendMessageRequest request) {
        if (replyingTo != null) {
            request.setReplyToMessageId(replyingTo.getId());
        }
    }

    private void sendQuickEmoji() {
        SendMessageRequest request = new SendMessageRequest("Ã°Å¸â€˜Â", "EMOJI");
        attachReply(request);
        clearReply();
        viewModel.sendMessage(conversationId, request);
    }

    private void sendReaction(Message message, String reaction) {
        viewModel.reactToMessage(message, reaction);
    }

    private void openMediaPicker(String type) {
        pendingMediaType = type;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(type.equals("IMAGE") ? "image/*" : "video/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        mediaPickerLauncher.launch(intent);
    }

    private void sendPickedMedia(Uri uri) {
        String type = pendingMediaType != null ? pendingMediaType : "IMAGE";
        uploadAndSendMedia(uri, type);
    }

    private void uploadAndSendMedia(Uri uri, String type) {
        String mimeType = getContentResolver().getType(uri);
        ContentUriRequestBody body = new ContentUriRequestBody(getContentResolver(), uri, mimeType);
        String filename = type.toLowerCase() + "_" + System.currentTimeMillis() + defaultExtension(type, mimeType);
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", filename, body);

        Toast.makeText(this, "Đang tải " + (type.equals("IMAGE") ? "ảnh" : "video") + "...", Toast.LENGTH_SHORT).show();
        mediaApiService.upload(part).enqueue(new Callback<ApiResponse<MediaUpload>>() {
            @Override
            public void onResponse(Call<ApiResponse<MediaUpload>> call, Response<ApiResponse<MediaUpload>> response) {
                if (response.code() == 401) {
                    redirectToLogin();
                    return;
                }
                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().isSuccess()
                        && response.body().getData() != null
                        && response.body().getData().getUrl() != null) {
                    sendMediaMessage(response.body().getData().getUrl(), type);
                } else {
                    Toast.makeText(ChatActivity.this, uploadErrorMessage(response), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MediaUpload>> call, Throwable t) {
                Toast.makeText(ChatActivity.this,
                        com.yourapp.util.NetworkErrorUtil.friendlyMessage(t, "Lỗi tải media"),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String uploadErrorMessage(Response<ApiResponse<MediaUpload>> response) {
        String detail = null;
        if (response.body() != null) {
            detail = response.body().getError() != null
                    ? response.body().getError()
                    : response.body().getMessage();
        }
        if (detail == null && response.errorBody() != null) {
            try {
                detail = response.errorBody().string();
            } catch (IOException ignored) {
                detail = null;
            }
        }
        String hint;
        if (response.code() == 404) {
            hint = "Backend chưa có API media hoặc chưa restart";
        } else if (response.code() == 413) {
            hint = "File quá lớn";
        } else if (response.code() == 415) {
            hint = "Định dạng file không được hỗ trợ";
        } else {
            hint = "Không tải được media";
        }
        return hint + " (HTTP " + response.code() + ")" + (detail != null ? ": " + detail : "");
    }

    private void sendMediaMessage(String mediaUrl, String type) {
        SendMessageRequest request = new SendMessageRequest(
                type.equals("IMAGE") ? "Ảnh" : "Video",
                type);
        request.setMediaUrl(mediaUrl);
        attachReply(request);
        clearReply();
        viewModel.sendMessage(conversationId, request);
    }

    private String defaultExtension(String type, String mimeType) {
        if (mimeType != null && mimeType.contains("/")) {
            String ext = mimeType.substring(mimeType.indexOf('/') + 1);
            if ("jpeg".equals(ext)) {
                return ".jpg";
            }
            if (!ext.isEmpty()) {
                return "." + ext;
            }
        }
        return type.equals("IMAGE") ? ".jpg" : ".mp4";
    }

    private void openChatInfo() {
        Intent intent = new Intent(this, ChatInfoActivity.class);
        intent.putExtra(ChatInfoActivity.EXTRA_CONVERSATION_ID, conversationId);
        startActivity(intent);
    }

    private void openPhotoViewer(Message message) {
        if (message == null || message.getMediaUrl() == null || message.getMediaUrl().isEmpty()) {
            return;
        }
        Intent intent = new Intent(this, PhotoViewerActivity.class);
        intent.putExtra(PhotoViewerActivity.EXTRA_IMAGE_URL, message.getMediaUrl());
        startActivity(intent);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(ChatActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void playNotificationSoundAndVibrate() {
        try {
            android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(200, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(200);
                }
            }
            android.net.Uri notificationUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION);
            android.media.Ringtone ringtone = android.media.RingtoneManager.getRingtone(getApplicationContext(), notificationUri);
            if (ringtone != null) {
                ringtone.play();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}






