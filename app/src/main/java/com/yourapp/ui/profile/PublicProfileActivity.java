package com.yourapp.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.bumptech.glide.Glide;
import com.yourapp.R;
import com.yourapp.data.remote.ConversationApiService;
import com.yourapp.data.remote.FriendApiService;
import com.yourapp.data.remote.UserApiService;
import com.yourapp.data.repository.AuthRepository;
import com.yourapp.data.repository.ConversationRepository;
import com.yourapp.data.repository.FriendRepository;
import com.yourapp.data.repository.UserRepository;
import com.yourapp.model.Conversation;
import com.yourapp.model.CreateConversationRequest;
import com.yourapp.model.Friend;
import com.yourapp.model.User;
import com.yourapp.network.RetrofitClient;
import com.yourapp.ui.auth.LoginActivity;
import com.yourapp.ui.chat.ChatActivity;
import com.yourapp.util.AvatarUtil;
import com.yourapp.util.TokenManager;
import java.util.Collections;

public class PublicProfileActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "user_id";

    private Long userId;
    private User loadedUser;
    private UserRepository userRepository;
    private FriendRepository friendRepository;
    private ConversationRepository conversationRepository;
    private ImageView ivCover;
    private ImageView ivAvatar;
    private TextView tvAvatar;
    private TextView tvName;
    private TextView tvUsername;
    private TextView tvFriendLine;
    private TextView tvBio;
    private TextView tvMeta;
    private TextView tvFriendCount;
    private TextView tvPostCount;
    private TextView tvStoryCount;
    private TextView tvStorySummary;
    private Button btnAdd;
    private Button btnMessage;
    private Button btnMore;
    private View btnChangeCover;
    private View btnChangeAvatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_profile);

        userId = getIntent().getLongExtra(EXTRA_USER_ID, -1L);
        if (userId == -1L) {
            finish();
            return;
        }

        TokenManager tokenManager = new TokenManager(this);
        if (!tokenManager.hasValidTokens()) {
            redirectToLogin();
            return;
        }

        bindViews();
        buildRepositories(tokenManager);
        loadProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userRepository != null) {
            loadProfile();
        }
    }

    private void bindViews() {
        Toolbar toolbar = findViewById(R.id.toolbar_public_profile);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        ivCover = findViewById(R.id.iv_public_cover);
        ivAvatar = findViewById(R.id.iv_public_avatar);
        tvAvatar = findViewById(R.id.tv_public_avatar);
        tvName = findViewById(R.id.tv_public_name);
        tvUsername = findViewById(R.id.tv_public_username);
        tvFriendLine = findViewById(R.id.tv_public_friend_line);
        tvBio = findViewById(R.id.tv_public_bio);
        tvMeta = findViewById(R.id.tv_public_meta);
        tvFriendCount = findViewById(R.id.tv_public_friend_count);
        tvPostCount = findViewById(R.id.tv_public_post_count);
        tvStoryCount = findViewById(R.id.tv_public_story_count);
        tvStorySummary = findViewById(R.id.tv_public_story_summary);
        btnAdd = findViewById(R.id.btn_public_add);
        btnMessage = findViewById(R.id.btn_public_message);
        btnMore = findViewById(R.id.btn_public_more);
        btnChangeCover = findViewById(R.id.btn_public_change_cover);
        btnChangeAvatar = findViewById(R.id.btn_public_change_avatar);

        btnAdd.setOnClickListener(v -> handlePrimaryAction());
        btnMessage.setOnClickListener(v -> handleSecondaryAction());
        btnMore.setOnClickListener(v -> Toast.makeText(this, "Thêm tuỳ chọn sẽ được bổ sung sau", Toast.LENGTH_SHORT).show());
        btnChangeCover.setOnClickListener(v -> openEditProfile());
        btnChangeAvatar.setOnClickListener(v -> openEditProfile());
    }

    private void buildRepositories(TokenManager tokenManager) {
        userRepository = new UserRepository(RetrofitClient.getInstance(tokenManager).create(UserApiService.class));
        friendRepository = new FriendRepository(RetrofitClient.getInstance(tokenManager).create(FriendApiService.class));
        conversationRepository = new ConversationRepository(RetrofitClient.getInstance(tokenManager).create(ConversationApiService.class));
    }

    private void loadProfile() {
        userRepository.getPublicProfile(userId).observe(this, result -> {
            if (result instanceof AuthRepository.Result.Success) {
                loadedUser = ((AuthRepository.Result.Success<User>) result).data;
                renderProfile();
            } else if (result instanceof AuthRepository.Result.Error) {
                handleError(((AuthRepository.Result.Error<User>) result).message);
            }
        });
    }

    private void renderProfile() {
        if (loadedUser == null) return;

        String username = nonEmpty(loadedUser.getUsername()) ? loadedUser.getUsername() : "user";
        String name = nonEmpty(loadedUser.getDisplayName()) ? loadedUser.getDisplayName() : username;

        tvName.setText(name);
        tvUsername.setText("@" + username);
        tvFriendLine.setText(loadedUser.getFriendCount() + " bạn bè");
        AvatarUtil.loadAvatar(loadedUser.getAvatarUrl(), loadedUser.getDisplayName(), username, ivAvatar, tvAvatar);

        if (nonEmpty(loadedUser.getCoverUrl())) {
            Glide.with(this).load(loadedUser.getCoverUrl()).centerCrop().into(ivCover);
        } else {
            Glide.with(this).clear(ivCover);
            ivCover.setBackgroundResource(R.drawable.bg_profile_cover_placeholder);
        }

        tvBio.setText(nonEmpty(loadedUser.getBio())
                ? loadedUser.getBio()
                : "Chưa có tiểu sử. Hãy thêm một câu giới thiệu ngắn về bạn.");

        StringBuilder meta = new StringBuilder();
        appendInfo(meta, "Nơi sống", loadedUser.getLocation());
        appendInfo(meta, "Sinh nhật", loadedUser.getDateOfBirth());
        appendInfo(meta, "Giới tính", genderLabel(loadedUser.getGender()));
        appendInfo(meta, "Điện thoại", loadedUser.getPhone());
        appendInfo(meta, "Email", loadedUser.getEmail());
        appendInfo(meta, "Website", loadedUser.getWebsite());
        if (meta.length() == 0) {
            meta.append("Chưa có thông tin giới thiệu.");
        }
        tvMeta.setText(meta.toString());

        tvFriendCount.setText(String.valueOf(loadedUser.getFriendCount()));
        tvPostCount.setText(String.valueOf(loadedUser.getPostCount()));
        tvStoryCount.setText(String.valueOf(loadedUser.getStoryCount()));
        tvStorySummary.setText(loadedUser.getStoryCount() + " tin đang hoạt động");

        boolean owner = loadedUser.isOwner() || "ME".equals(loadedUser.getFriendshipStatus());
        btnChangeCover.setVisibility(owner ? View.VISIBLE : View.GONE);
        btnChangeAvatar.setVisibility(owner ? View.VISIBLE : View.GONE);
        if (owner) {
            btnAdd.setEnabled(true);
            btnAdd.setText("Chỉnh sửa trang cá nhân");
            btnMessage.setVisibility(View.VISIBLE);
            btnMessage.setText("Thêm vào tin");
            return;
        }

        String status = loadedUser.getFriendshipStatus();
        boolean alreadyConnected = "ACCEPTED".equals(status) || "PENDING".equals(status);
        btnAdd.setEnabled(!alreadyConnected);
        btnAdd.setText("ACCEPTED".equals(status) ? "Bạn bè" : "PENDING".equals(status) ? "Đã gửi lời mời" : "Thêm bạn");
        btnMessage.setVisibility(View.VISIBLE);
        btnMessage.setText("Nhắn tin");
    }

    private void handlePrimaryAction() {
        if (loadedUser == null) return;
        if (loadedUser.isOwner() || "ME".equals(loadedUser.getFriendshipStatus())) {
            openEditProfile();
        } else {
            sendFriendRequest();
        }
    }

    private void handleSecondaryAction() {
        if (loadedUser == null) return;
        if (loadedUser.isOwner() || "ME".equals(loadedUser.getFriendshipStatus())) {
            Toast.makeText(this, "Thêm vào tin nằm ở màn Tin nhắn", Toast.LENGTH_SHORT).show();
        } else {
            startPrivateChat();
        }
    }

    private void openEditProfile() {
        startActivity(new Intent(this, EditProfileActivity.class));
    }

    private void sendFriendRequest() {
        friendRepository.sendRequest(userId).observe(this, result -> {
            if (result instanceof AuthRepository.Result.Success) {
                Toast.makeText(this, "Đã gửi lời mời kết bạn", Toast.LENGTH_SHORT).show();
                loadedUser.setFriendshipStatus("PENDING");
                renderProfile();
            } else if (result instanceof AuthRepository.Result.Error) {
                handleError(((AuthRepository.Result.Error<Friend>) result).message);
            }
        });
    }

    private void startPrivateChat() {
        CreateConversationRequest request = new CreateConversationRequest("PRIVATE", null, Collections.singletonList(userId));
        conversationRepository.createConversation(request).observe(this, result -> {
            if (result instanceof AuthRepository.Result.Success) {
                Conversation conversation = ((AuthRepository.Result.Success<Conversation>) result).data;
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversation.getId());
                intent.putExtra(ChatActivity.EXTRA_CONVERSATION_NAME, tvName.getText().toString());
                startActivity(intent);
            } else if (result instanceof AuthRepository.Result.Error) {
                handleError(((AuthRepository.Result.Error<Conversation>) result).message);
            }
        });
    }

    private boolean nonEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void appendInfo(StringBuilder builder, String label, String value) {
        if (!nonEmpty(value)) return;
        if (builder.length() > 0) builder.append("\n");
        builder.append(label).append(": ").append(value);
    }

    private String genderLabel(String value) {
        if ("male".equals(value)) return "Nam";
        if ("female".equals(value)) return "Nữ";
        if ("custom".equals(value)) return "Tùy chỉnh";
        if ("undisclosed".equals(value)) return "Không tiết lộ";
        return value;
    }

    private void handleError(String message) {
        if ("UNAUTHORIZED".equals(message)) {
            redirectToLogin();
        } else {
            Toast.makeText(this, message != null ? message : "Có lỗi xảy ra", Toast.LENGTH_SHORT).show();
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}