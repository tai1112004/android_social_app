package com.yourapp.ui.group;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.yourapp.R;
import com.yourapp.data.remote.ConversationApiService;
import com.yourapp.data.remote.FriendApiService;
import com.yourapp.data.repository.AuthRepository;
import com.yourapp.data.repository.ConversationRepository;
import com.yourapp.data.repository.FriendRepository;
import com.yourapp.model.Conversation;
import com.yourapp.model.CreateConversationRequest;
import com.yourapp.model.Friend;
import com.yourapp.network.RetrofitClient;
import com.yourapp.ui.auth.LoginActivity;
import com.yourapp.ui.chat.ChatActivity;
import com.yourapp.util.TokenManager;
import java.util.List;

public class CreateGroupActivity extends AppCompatActivity {

    private EditText etGroupName;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private GroupFriendAdapter adapter;
    private FriendRepository friendRepository;
    private ConversationRepository conversationRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        TokenManager tokenManager = new TokenManager(this);
        if (!tokenManager.hasValidTokens()) {
            redirectToLogin();
            return;
        }

        friendRepository = new FriendRepository(RetrofitClient.getInstance(tokenManager)
                .create(FriendApiService.class));
        conversationRepository = new ConversationRepository(RetrofitClient.getInstance(tokenManager)
                .create(ConversationApiService.class));

        setupViews();
        loadFriends();
    }

    private void setupViews() {
        Toolbar toolbar = findViewById(R.id.toolbar_create_group);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Tạo nhóm");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        etGroupName = findViewById(R.id.et_group_name);
        progressBar = findViewById(R.id.progress_create_group);
        tvEmpty = findViewById(R.id.tv_create_group_empty);
        Button btnCreate = findViewById(R.id.btn_create_group);
        RecyclerView rvFriends = findViewById(R.id.rv_group_friends);

        adapter = new GroupFriendAdapter();
        rvFriends.setLayoutManager(new LinearLayoutManager(this));
        rvFriends.setAdapter(adapter);

        btnCreate.setOnClickListener(v -> createGroup());
        focusAndShowKeyboard(etGroupName);
    }

    private void focusAndShowKeyboard(EditText editText) {
        editText.postDelayed(() -> {
            editText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 250);
    }

    private void loadFriends() {
        progressBar.setVisibility(View.VISIBLE);
        friendRepository.getFriends().observe(this, result -> {
            progressBar.setVisibility(View.GONE);
            if (result instanceof AuthRepository.Result.Success) {
                List<Friend> friends = ((AuthRepository.Result.Success<List<Friend>>) result).data;
                adapter.setFriends(friends);
                tvEmpty.setVisibility(friends == null || friends.isEmpty() ? View.VISIBLE : View.GONE);
            } else if (result instanceof AuthRepository.Result.Error) {
                handleError(((AuthRepository.Result.Error<List<Friend>>) result).message);
            }
        });
    }

    private void createGroup() {
        String name = etGroupName.getText().toString().trim();
        List<Long> selected = adapter.getSelectedUserIds();

        if (name.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên nhóm", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selected.size() < 2) {
            Toast.makeText(this, "Chọn ít nhất 2 người bạn", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        CreateConversationRequest request = new CreateConversationRequest("GROUP", name, selected);
        conversationRepository.createConversation(request).observe(this, result -> {
            progressBar.setVisibility(View.GONE);
            if (result instanceof AuthRepository.Result.Success) {
                Conversation conversation = ((AuthRepository.Result.Success<Conversation>) result).data;
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversation.getId());
                intent.putExtra(ChatActivity.EXTRA_CONVERSATION_NAME, conversation.getName());
                startActivity(intent);
                finish();
            } else if (result instanceof AuthRepository.Result.Error) {
                handleError(((AuthRepository.Result.Error<Conversation>) result).message);
            }
        });
    }

    private void handleError(String message) {
        if ("UNAUTHORIZED".equals(message)) {
            redirectToLogin();
            return;
        }
        Toast.makeText(this, message != null ? message : "Có lỗi xảy ra", Toast.LENGTH_SHORT).show();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
