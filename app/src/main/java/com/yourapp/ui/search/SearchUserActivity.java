package com.yourapp.ui.search;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.yourapp.data.remote.UserApiService;
import com.yourapp.data.repository.AuthRepository;
import com.yourapp.data.repository.ConversationRepository;
import com.yourapp.data.repository.FriendRepository;
import com.yourapp.data.repository.UserRepository;
import com.yourapp.model.Conversation;
import com.yourapp.model.ConversationInfo;
import com.yourapp.model.ConversationMember;
import com.yourapp.model.CreateConversationRequest;
import com.yourapp.model.Friend;
import com.yourapp.model.User;
import com.yourapp.network.RetrofitClient;
import com.yourapp.ui.auth.LoginActivity;
import com.yourapp.ui.chat.ChatActivity;
import com.yourapp.ui.home.ConversationAdapter;
import com.yourapp.util.TokenManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SearchUserActivity extends AppCompatActivity {

    private EditText etSearch;
    private ProgressBar progressBar;
    private TextView tvUsersEmpty;
    private TextView tvGroupsEmpty;
    private UserSearchAdapter userAdapter;
    private ConversationAdapter groupAdapter;
    private UserRepository userRepository;
    private FriendRepository friendRepository;
    private ConversationRepository conversationRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_user);

        TokenManager tokenManager = new TokenManager(this);
        if (!tokenManager.hasValidTokens()) {
            redirectToLogin();
            return;
        }

        userRepository = new UserRepository(RetrofitClient.getInstance(tokenManager)
                .create(UserApiService.class));
        friendRepository = new FriendRepository(RetrofitClient.getInstance(tokenManager)
                .create(FriendApiService.class));
        conversationRepository = new ConversationRepository(RetrofitClient.getInstance(tokenManager)
                .create(ConversationApiService.class));

        setupViews();
    }

    private void setupViews() {
        Toolbar toolbar = findViewById(R.id.toolbar_search_user);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Tim nguoi va nhom");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        etSearch = findViewById(R.id.et_search_user);
        ImageButton btnSearch = findViewById(R.id.btn_search_user);
        progressBar = findViewById(R.id.progress_search_user);
        tvUsersEmpty = findViewById(R.id.tv_search_users_empty);
        tvGroupsEmpty = findViewById(R.id.tv_search_groups_empty);
        RecyclerView rvUsers = findViewById(R.id.rv_search_users);
        RecyclerView rvGroups = findViewById(R.id.rv_search_groups);

        userAdapter = new UserSearchAdapter(this::sendFriendRequest, this::messageUser);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(userAdapter);

        groupAdapter = new ConversationAdapter(this::openConversation);
        rvGroups.setLayoutManager(new LinearLayoutManager(this));
        rvGroups.setAdapter(groupAdapter);

        btnSearch.setOnClickListener(v -> search());
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search();
                return true;
            }
            return false;
        });
        focusAndShowKeyboard(etSearch);
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

    private void search() {
        String query = etSearch.getText().toString().trim();
        if (query.length() < 2) {
            Toast.makeText(this, "Nhap it nhat 2 ky tu", Toast.LENGTH_SHORT).show();
            userAdapter.setUsers(Collections.emptyList());
            groupAdapter.setConversations(Collections.emptyList());
            tvUsersEmpty.setVisibility(View.VISIBLE);
            tvGroupsEmpty.setVisibility(View.VISIBLE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        final int[] pending = {2};
        Runnable done = () -> {
            pending[0]--;
            if (pending[0] <= 0) {
                progressBar.setVisibility(View.GONE);
            }
        };

        userRepository.searchUsers(query).observe(this, result -> {
            if (result instanceof AuthRepository.Result.Success) {
                List<User> users = ((AuthRepository.Result.Success<List<User>>) result).data;
                userAdapter.setUsers(users);
                tvUsersEmpty.setVisibility(users == null || users.isEmpty() ? View.VISIBLE : View.GONE);
            } else if (result instanceof AuthRepository.Result.Error) {
                handleError(((AuthRepository.Result.Error<List<User>>) result).message);
                userAdapter.setUsers(Collections.emptyList());
                tvUsersEmpty.setVisibility(View.VISIBLE);
            }
            done.run();
        });

        conversationRepository.getConversations().observe(this, result -> {
            if (result instanceof AuthRepository.Result.Success) {
                List<Conversation> conversations = ((AuthRepository.Result.Success<List<Conversation>>) result).data;
                List<Conversation> groups = filterGroups(conversations, query);
                groupAdapter.setConversations(groups);
                tvGroupsEmpty.setVisibility(groups.isEmpty() ? View.VISIBLE : View.GONE);
            } else if (result instanceof AuthRepository.Result.Error) {
                handleError(((AuthRepository.Result.Error<List<Conversation>>) result).message);
                groupAdapter.setConversations(Collections.emptyList());
                tvGroupsEmpty.setVisibility(View.VISIBLE);
            }
            done.run();
        });
    }

    private List<Conversation> filterGroups(List<Conversation> conversations, String query) {
        List<Conversation> groups = new ArrayList<>();
        if (conversations == null) {
            return groups;
        }
        String needle = query.toLowerCase(Locale.US);
        for (Conversation conversation : conversations) {
            if (conversation == null) continue;
            if (!"GROUP".equalsIgnoreCase(conversation.getType())) continue;
            String name = conversation.getName();
            if (name != null && name.toLowerCase(Locale.US).contains(needle)) {
                groups.add(conversation);
            }
        }
        return groups;
    }

    private void openConversation(Conversation conversation) {
        if (conversation == null || conversation.getId() == null) {
            return;
        }
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversation.getId());
        intent.putExtra(ChatActivity.EXTRA_CONVERSATION_NAME,
                conversation.getName() != null ? conversation.getName() : "Group chat");
        startActivity(intent);
    }

    private void sendFriendRequest(User user) {
        friendRepository.sendRequest(user.getId()).observe(this, result -> {
            if (result instanceof AuthRepository.Result.Success) {
                Friend ignored = ((AuthRepository.Result.Success<Friend>) result).data;
                Toast.makeText(this, "Da gui loi moi ket ban", Toast.LENGTH_SHORT).show();
                search();
            } else if (result instanceof AuthRepository.Result.Error) {
                handleError(((AuthRepository.Result.Error<Friend>) result).message);
            }
        });
    }

    private void messageUser(User user) {
        CreateConversationRequest request = new CreateConversationRequest(
                "PRIVATE",
                null,
                Collections.singletonList(user.getId()));
        conversationRepository.createConversation(request).observe(this, result -> {
            if (result instanceof AuthRepository.Result.Success) {
                Conversation conversation = ((AuthRepository.Result.Success<Conversation>) result).data;
                if (conversation != null) {
                    String title = user.getDisplayName() != null && !user.getDisplayName().isEmpty()
                            ? user.getDisplayName()
                            : user.getUsername();
                    Intent intent = new Intent(this, ChatActivity.class);
                    intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversation.getId());
                    intent.putExtra(ChatActivity.EXTRA_CONVERSATION_NAME, title);
                    startActivity(intent);
                }
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
        Toast.makeText(this, message != null ? message : "Co loi xay ra", Toast.LENGTH_SHORT).show();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
