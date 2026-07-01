package com.yourapp.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.yourapp.R;
import com.yourapp.data.remote.ConversationApiService;
import com.yourapp.data.repository.AuthRepository;
import com.yourapp.data.repository.ConversationRepository;
import com.yourapp.model.ConversationInfo;
import com.yourapp.model.ConversationMember;
import com.yourapp.network.RetrofitClient;
import com.yourapp.ui.auth.LoginActivity;
import com.yourapp.ui.profile.PublicProfileActivity;
import com.yourapp.util.TokenManager;

public class ChatInfoActivity extends AppCompatActivity {

    public static final String EXTRA_CONVERSATION_ID = "conversation_id";

    private Long conversationId;
    private TextView tvAvatar;
    private TextView tvTitle;
    private TextView tvMeta;
    private LinearLayout memberList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_info);

        conversationId = getIntent().getLongExtra(EXTRA_CONVERSATION_ID, -1L);
        if (conversationId == -1L) {
            finish();
            return;
        }

        TokenManager tokenManager = new TokenManager(this);
        if (!tokenManager.hasValidTokens()) {
            redirectToLogin();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar_chat_info);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvAvatar = findViewById(R.id.tv_info_avatar);
        tvTitle = findViewById(R.id.tv_info_title);
        tvMeta = findViewById(R.id.tv_info_meta);
        memberList = findViewById(R.id.list_chat_members);

        ConversationRepository repository = new ConversationRepository(
                RetrofitClient.getInstance(tokenManager).create(ConversationApiService.class));
        repository.getConversationInfo(conversationId).observe(this, result -> {
            if (result instanceof AuthRepository.Result.Success) {
                render(((AuthRepository.Result.Success<ConversationInfo>) result).data);
            } else if (result instanceof AuthRepository.Result.Error) {
                handleError(((AuthRepository.Result.Error<ConversationInfo>) result).message);
            }
        });
    }

    private void render(ConversationInfo info) {
        String title = info.getName() != null && !info.getName().isEmpty() ? info.getName() : "Cuộc trò chuyện";
        tvAvatar.setText(title.substring(0, 1).toUpperCase());
        tvTitle.setText(title);
        tvMeta.setText(("GROUP".equals(info.getType()) ? "Nhóm chat" : "Chat riêng")
                + " · " + info.getMemberCount() + " thành viên");
        memberList.removeAllViews();
        if (info.getMembers() == null || info.getMembers().isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Chưa có thông tin thành viên");
            empty.setTextColor(0xFF64748B);
            empty.setTextSize(14f);
            empty.setPadding(0, 16, 0, 16);
            memberList.addView(empty);
            return;
        }
        for (ConversationMember member : info.getMembers()) {
            memberList.addView(memberRow(member));
        }
    }

    private View memberRow(ConversationMember member) {
        TextView row = new TextView(this);
        String name = member.getDisplayName() != null && !member.getDisplayName().isEmpty()
                ? member.getDisplayName()
                : member.getUsername();
        row.setText(name + " · " + ("ADMIN".equals(member.getRole()) ? "Quản trị viên" : "Thành viên")
                + " · " + (member.isOnline() ? "Đang online" : "Ngoại tuyến"));
        row.setTextColor(0xFF111827);
        row.setTextSize(15f);
        row.setPadding(0, 18, 0, 18);
        row.setOnClickListener(v -> {
            Intent intent = new Intent(this, PublicProfileActivity.class);
            intent.putExtra(PublicProfileActivity.EXTRA_USER_ID, member.getUserId());
            startActivity(intent);
        });
        return row;
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
