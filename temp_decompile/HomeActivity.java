package com.yourapp.ui.home;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.Build.VERSION;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.yourapp.R.drawable;
import com.yourapp.R.id;
import com.yourapp.R.layout;
import com.yourapp.data.remote.AuthApiService;
import com.yourapp.data.remote.ConversationApiService;
import com.yourapp.data.remote.FriendApiService;
import com.yourapp.data.remote.MediaApiService;
import com.yourapp.data.remote.MessageApiService;
import com.yourapp.data.remote.PostApiService;
import com.yourapp.data.remote.StoryApiService;
import com.yourapp.data.remote.UserApiService;
import com.yourapp.data.repository.AuthRepository;
import com.yourapp.data.repository.ConversationRepository;
import com.yourapp.data.repository.FriendRepository;
import com.yourapp.data.repository.UserRepository;
import com.yourapp.model.ApiResponse;
import com.yourapp.model.Comment;
import com.yourapp.model.Conversation;
import com.yourapp.model.CreateConversationRequest;
import com.yourapp.model.Friend;
import com.yourapp.model.MediaUpload;
import com.yourapp.model.Message;
import com.yourapp.model.Post;
import com.yourapp.model.Story;
import com.yourapp.model.User;
import com.yourapp.network.RetrofitClient;
import com.yourapp.ui.auth.LoginActivity;
import com.yourapp.ui.chat.ChatActivity;
import com.yourapp.ui.feed.CommentAdapter;
import com.yourapp.ui.feed.PostAdapter;
import com.yourapp.ui.group.CreateGroupActivity;
import com.yourapp.ui.profile.PublicProfileActivity;
import com.yourapp.ui.search.SearchUserActivity;
import com.yourapp.util.AvatarUtil;
import com.yourapp.util.ContentUriRequestBody;
import com.yourapp.util.TokenManager;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import okhttp3.MultipartBody;
import okhttp3.MultipartBody.Part;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {
   private View panelChats;
   private View panelFriends;
   private View panelFeed;
   private View panelProfile;
   private View chatsEmptyState;
   private RecyclerView rvConversations;
   private RecyclerView rvFriends;
   private ProgressBar progressBar;
   private TextView tvHomeAvatar;
   private TextView tvHomeTitle;
   private TextView tvHomeSubtitle;
   private TextView tvFriendsEmpty;
   private TextView tvProfileAvatar;
   private TextView tvProfileName;
   private TextView tvProfileUsername;
   private TextView tvFeedAvatar;
   private FloatingActionButton fabHome;
   private RadioGroup friendsFilterGroup;
   private ImageView ivHomeAvatar;
   private ImageView ivProfileAvatar;
   private ImageView ivFeedMyAvatar;
   private FrameLayout frameProfileAvatar;
   private RecyclerView rvStories;
   private RecyclerView rvFeed;
   private View feedEmptyState;
   private View btnFeedCompose;
   private StoryAdapter storyAdapter;
   private PostAdapter postAdapter;
   private StoryApiService storyApiService;
   private PostApiService postApiService;
   private MediaApiService mediaApiService;
   private User currentUserInfo;
   private String pendingUploadType;
   private Uri selectedPostImageUri;
   private ImageView dialogImagePreview;
   private FrameLayout dialogImagePreviewFrame;
   private final ActivityResultLauncher<Intent> mediaPickerLauncher = this.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), (result) -> {
      if (result.getResultCode() == -1 && result.getData() != null && result.getData().getData() != null) {
         Uri uri = result.getData().getData();
         int flags = result.getData().getFlags() & 3;

         try {
            this.getContentResolver().takePersistableUriPermission(uri, flags);
         } catch (SecurityException var5) {
         }

         this.handlePickedMedia(uri);
      }

   });
   private HomeViewModel viewModel;
   private ConversationAdapter conversationAdapter;
   private FriendAdapter friendAdapter;
   private ConversationRepository conversationRepository;
   private FriendRepository friendRepository;
   private UserRepository userRepository;
   private AuthRepository authRepository;
   private TokenManager tokenManager;
   private String friendMode = "friends";
   private MessageApiService messageApiService;
   private FriendApiService friendApi;
   private final Map<Long, Long> lastMessageIdMap = new HashMap();
   private final Set<Long> incomingRequestIds = new HashSet();
   private boolean isFirstFriendRequestsLoad = true;
   private final Handler pollingHandler = new Handler(Looper.getMainLooper());
   private Runnable pollingRunnable;

   public HomeActivity() {
   }

   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      this.setContentView(layout.activity_home);
      this.tokenManager = new TokenManager(this);
      if (!this.tokenManager.hasValidTokens()) {
         this.redirectToLogin();
      } else {
         this.bindViews();
         this.buildRepositories();
         this.setupLists();
         this.setupActions();
         this.observeViewModel();
         this.showChats();
         this.loadProfile();
         this.loadFriends("friends");
         this.viewModel.loadConversations();
         this.pollingRunnable = new Runnable() {
            public void run() {
               if (HomeActivity.this.viewModel != null) {
                  HomeActivity.this.viewModel.loadConversations();
               }

               HomeActivity.this.pollIncomingFriendRequests();
               HomeActivity.this.pollingHandler.postDelayed(this, 5000L);
            }
         };
      }
   }

   protected void onResume() {
      super.onResume();
      if (this.viewModel != null) {
         this.viewModel.loadConversations();
      }

      if (this.friendRepository != null) {
         this.loadFriends(this.friendMode);
      }

      this.pollingHandler.removeCallbacks(this.pollingRunnable);
      this.pollingHandler.postDelayed(this.pollingRunnable, 5000L);
   }

   protected void onPause() {
      super.onPause();
      this.pollingHandler.removeCallbacks(this.pollingRunnable);
   }

   private void bindViews() {
      this.panelChats = this.findViewById(id.panel_chats);
      this.panelFriends = this.findViewById(id.panel_friends);
      this.panelFeed = this.findViewById(id.panel_feed);
      this.panelProfile = this.findViewById(id.panel_profile);
      this.chatsEmptyState = this.findViewById(id.chats_empty_state);
      this.rvConversations = (RecyclerView)this.findViewById(id.rv_conversations);
      this.rvFriends = (RecyclerView)this.findViewById(id.rv_friends);
      this.progressBar = (ProgressBar)this.findViewById(id.progress_home);
      this.tvHomeAvatar = (TextView)this.findViewById(id.tv_home_avatar);
      this.tvHomeTitle = (TextView)this.findViewById(id.tv_home_title);
      this.tvHomeSubtitle = (TextView)this.findViewById(id.tv_home_subtitle);
      this.tvFriendsEmpty = (TextView)this.findViewById(id.tv_friends_empty);
      this.tvProfileAvatar = (TextView)this.findViewById(id.tv_profile_avatar);
      this.tvProfileName = (TextView)this.findViewById(id.tv_profile_name);
      this.tvProfileUsername = (TextView)this.findViewById(id.tv_profile_username);
      this.tvFeedAvatar = (TextView)this.findViewById(id.tv_feed_my_avatar);
      this.fabHome = (FloatingActionButton)this.findViewById(id.fab_home);
      this.friendsFilterGroup = (RadioGroup)this.findViewById(id.friends_filter_group);
      this.ivHomeAvatar = (ImageView)this.findViewById(id.iv_home_avatar);
      this.ivProfileAvatar = (ImageView)this.findViewById(id.iv_profile_avatar);
      this.ivFeedMyAvatar = (ImageView)this.findViewById(id.iv_feed_my_avatar);
      this.frameProfileAvatar = (FrameLayout)this.findViewById(id.frame_profile_avatar);
      this.rvStories = (RecyclerView)this.findViewById(id.rv_stories);
      this.rvFeed = (RecyclerView)this.findViewById(id.rv_feed);
      this.feedEmptyState = this.findViewById(id.feed_empty_state);
      this.btnFeedCompose = this.findViewById(id.btn_feed_compose);
   }

   private void buildRepositories() {
      ConversationApiService conversationApi = (ConversationApiService)RetrofitClient.getInstance(this.tokenManager).create(ConversationApiService.class);
      FriendApiService friendApi = (FriendApiService)RetrofitClient.getInstance(this.tokenManager).create(FriendApiService.class);
      UserApiService userApi = (UserApiService)RetrofitClient.getInstance(this.tokenManager).create(UserApiService.class);
      this.friendApi = friendApi;
      this.messageApiService = (MessageApiService)RetrofitClient.getInstance(this.tokenManager).create(MessageApiService.class);
      this.storyApiService = (StoryApiService)RetrofitClient.getInstance(this.tokenManager).create(StoryApiService.class);
      this.postApiService = (PostApiService)RetrofitClient.getInstance(this.tokenManager).create(PostApiService.class);
      this.mediaApiService = (MediaApiService)RetrofitClient.getInstance(this.tokenManager).create(MediaApiService.class);
      this.conversationRepository = new ConversationRepository(conversationApi);
      this.friendRepository = new FriendRepository(friendApi);
      this.userRepository = new UserRepository(userApi);
      this.authRepository = new AuthRepository((AuthApiService)RetrofitClient.getInstance(this.tokenManager).create(AuthApiService.class), this.tokenManager);
      this.viewModel = (HomeViewModel)(new ViewModelProvider(this, new ViewModelProvider.Factory() {
         public <T extends ViewModel> T create(Class<T> modelClass) {
            return (T)(new HomeViewModel(HomeActivity.this.conversationRepository));
         }
      })).get(HomeViewModel.class);
   }

   private void setupLists() {
      this.conversationAdapter = new ConversationAdapter(this::openConversation);
      this.rvConversations.setLayoutManager(new LinearLayoutManager(this));
      this.rvConversations.setAdapter(this.conversationAdapter);
      this.friendAdapter = new FriendAdapter(this::handleFriendAction, this::openFriendProfile);
      this.rvFriends.setLayoutManager(new LinearLayoutManager(this));
      this.rvFriends.setAdapter(this.friendAdapter);
      this.storyAdapter = new StoryAdapter(new StoryAdapter.OnStoryActionListener() {
         public void onAddStory() {
            HomeActivity.this.pickStoryImage();
         }

         public void onViewStory(Story story) {
            Intent intent = new Intent(HomeActivity.this, StoryViewerActivity.class);
            intent.putExtra("mediaUrl", story.getMediaUrl());
            intent.putExtra("content", story.getContent());
            intent.putExtra("authorId", story.getAuthorId());
            intent.putExtra("authorName", story.getAuthorDisplayName());
            intent.putExtra("authorUsername", story.getAuthorUsername());
            intent.putExtra("authorAvatarUrl", story.getAuthorAvatarUrl());
            intent.putExtra("time", story.getCreatedAt());
            HomeActivity.this.startActivity(intent);
         }
      });
      this.rvStories.setLayoutManager(new LinearLayoutManager(this, 0, false));
      this.rvStories.setAdapter(this.storyAdapter);
      this.postAdapter = new PostAdapter(new PostAdapter.OnPostActionListener() {
         public void onLike(Post post, int position) {
            HomeActivity.this.togglePostLike(post, position);
         }

         public void onComment(Post post) {
            HomeActivity.this.showCommentsDialog(post);
         }
      });
      this.rvFeed.setLayoutManager(new LinearLayoutManager(this));
      this.rvFeed.setAdapter(this.postAdapter);
   }

   private void setupActions() {
      BottomNavigationView bottomNav = (BottomNavigationView)this.findViewById(id.bottom_nav_home);
      bottomNav.setOnItemSelectedListener((item) -> {
         int id = item.getItemId();
         if (id == id.nav_friends) {
            this.showFriends();
            return true;
         } else if (id == id.nav_feed) {
            this.showFeed();
            return true;
         } else if (id == id.nav_profile) {
            this.showProfile();
            return true;
         } else {
            this.showChats();
            return true;
         }
      });
      this.findViewById(id.btn_home_search).setOnClickListener((v) -> this.openSearch());
      this.findViewById(id.home_search_pill).setOnClickListener((v) -> this.openSearch());
      this.findViewById(id.btn_empty_find_people).setOnClickListener((v) -> this.openSearch());
      this.findViewById(id.btn_empty_new_group).setOnClickListener((v) -> this.openCreateGroup());
      if (this.btnFeedCompose != null) {
         this.btnFeedCompose.setOnClickListener((v) -> this.showPostComposer());
      }

      this.findViewById(id.btn_profile_find_people).setOnClickListener((v) -> this.openSearch());
      this.findViewById(id.btn_profile_create_group).setOnClickListener((v) -> this.openCreateGroup());
      this.findViewById(id.btn_profile_logout).setOnClickListener((v) -> {
         this.authRepository.logout();
         this.redirectToLogin();
      });
      this.frameProfileAvatar.setOnClickListener((v) -> this.showEditProfileDialog());
      this.fabHome.setOnClickListener((v) -> this.openSearch());
      this.friendsFilterGroup.setOnCheckedChangeListener((group, checkedId) -> {
         if (checkedId == id.rb_incoming) {
            this.loadFriends("incoming");
         } else if (checkedId == id.rb_sent) {
            this.loadFriends("sent");
         } else if (checkedId == id.rb_suggestions) {
            this.loadFriends("suggestions");
         } else {
            this.loadFriends("friends");
         }

      });
   }

   private void observeViewModel() {
      this.viewModel.getConversationList().observe(this, (conversations) -> {
         this.conversationAdapter.setConversations(conversations);
         boolean empty = conversations == null || conversations.isEmpty();
         this.chatsEmptyState.setVisibility(empty ? 0 : 8);
         this.rvConversations.setVisibility(empty ? 8 : 0);
         if (conversations != null) {
            for(Conversation c : conversations) {
               this.checkNewMessagesForConversation(c.getId());
            }
         }

      });
      this.viewModel.getLoading().observe(this, (isLoading) -> this.progressBar.setVisibility(isLoading != null && isLoading ? 0 : 8));
      this.viewModel.getError().observe(this, this::handleError);
   }

   private void showChats() {
      this.panelChats.setVisibility(0);
      this.panelFriends.setVisibility(8);
      this.panelFeed.setVisibility(8);
      this.panelProfile.setVisibility(8);
      this.fabHome.show();
      this.tvHomeTitle.setText("Tin nhắn");
      this.tvHomeSubtitle.setText("Tin nhắn và nhóm");
      this.loadStories();
   }

   private void showFriends() {
      this.panelChats.setVisibility(8);
      this.panelFriends.setVisibility(0);
      this.panelFeed.setVisibility(8);
      this.panelProfile.setVisibility(8);
      this.fabHome.show();
      this.tvHomeTitle.setText("Bạn bè");
      this.tvHomeSubtitle.setText("Bạn bè, lời mời và trạng thái");
      this.loadFriends(this.friendMode);
   }

   private void showFeed() {
      this.panelChats.setVisibility(8);
      this.panelFriends.setVisibility(8);
      this.panelFeed.setVisibility(0);
      this.panelProfile.setVisibility(8);
      this.fabHome.hide();
      this.tvHomeTitle.setText("Bảng tin");
      this.tvHomeSubtitle.setText("Bài viết, cảm xúc và tin mới");
      this.loadFeed();
   }

   private void showProfile() {
      this.panelChats.setVisibility(8);
      this.panelFriends.setVisibility(8);
      this.panelFeed.setVisibility(8);
      this.panelProfile.setVisibility(0);
      this.fabHome.hide();
      this.tvHomeTitle.setText("Hồ sơ");
      this.tvHomeSubtitle.setText("Tài khoản và thao tác nhanh");
      this.loadProfile();
   }

   private void loadProfile() {
      this.userRepository.getMe().observe(this, (result) -> {
         if (result instanceof AuthRepository.Result.Success) {
            User user = (User)((AuthRepository.Result.Success)result).data;
            this.currentUserInfo = user;
            String username = user != null && user.getUsername() != null ? user.getUsername() : "Tôi";
            String name = user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty() ? user.getDisplayName() : username;
            this.tvProfileName.setText(name);
            this.tvProfileUsername.setText("@" + username);
            if (user != null) {
               AvatarUtil.loadAvatar(user.getAvatarUrl(), user.getDisplayName(), user.getUsername(), this.ivHomeAvatar, this.tvHomeAvatar);
               AvatarUtil.loadAvatar(user.getAvatarUrl(), user.getDisplayName(), user.getUsername(), this.ivProfileAvatar, this.tvProfileAvatar);
               AvatarUtil.loadAvatar(user.getAvatarUrl(), user.getDisplayName(), user.getUsername(), this.ivFeedMyAvatar, this.tvFeedAvatar);
            }
         } else if (result instanceof AuthRepository.Result.Error) {
            this.handleError(((AuthRepository.Result.Error)result).message);
         }

      });
   }

   private void loadFriends(String mode) {
      this.friendMode = mode;
      this.friendAdapter.setMode(mode);
      if ("incoming".equals(mode)) {
         this.friendRepository.getIncomingRequests().observe(this, this::renderFriendsResult);
      } else if ("sent".equals(mode)) {
         this.friendRepository.getSentRequests().observe(this, this::renderFriendsResult);
      } else if ("suggestions".equals(mode)) {
         this.friendRepository.getSuggestions().observe(this, this::renderFriendsResult);
      } else {
         this.friendRepository.getFriends().observe(this, this::renderFriendsResult);
      }

   }

   private void renderFriendsResult(AuthRepository.Result<List<Friend>> result) {
      if (result instanceof AuthRepository.Result.Success) {
         List<Friend> friends = (List)((AuthRepository.Result.Success)result).data;
         this.friendAdapter.setFriends(friends);
         boolean empty = friends == null || friends.isEmpty();
         this.rvFriends.setVisibility(empty ? 8 : 0);
         this.tvFriendsEmpty.setVisibility(empty ? 0 : 8);
         this.tvFriendsEmpty.setText(this.emptyTextForMode());
      } else if (result instanceof AuthRepository.Result.Error) {
         this.handleError(((AuthRepository.Result.Error)result).message);
      }

   }

   private String emptyTextForMode() {
      if ("incoming".equals(this.friendMode)) {
         return "Không có lời mời mới";
      } else if ("sent".equals(this.friendMode)) {
         return "Chưa gửi lời mời nào";
      } else {
         return "suggestions".equals(this.friendMode) ? "Chưa có gợi ý bạn bè" : "Chưa có bạn bè";
      }
   }

   private void handleFriendAction(Friend friend) {
      if ("incoming".equals(this.friendMode)) {
         this.friendRepository.accept(friend.getFriendshipId()).observe(this, (result) -> {
            if (result instanceof AuthRepository.Result.Success) {
               this.loadFriends("incoming");
               Toast.makeText(this, "Đã chấp nhận lời mời", 0).show();
            } else if (result instanceof AuthRepository.Result.Error) {
               this.handleError(((AuthRepository.Result.Error)result).message);
            }

         });
      } else if ("sent".equals(this.friendMode)) {
         this.friendRepository.remove(friend.getFriendshipId()).observe(this, (result) -> {
            this.loadFriends("sent");
            Toast.makeText(this, "Đã xóa lời mời", 0).show();
         });
      } else if ("suggestions".equals(this.friendMode)) {
         this.friendRepository.sendRequest(friend.getUserId()).observe(this, (result) -> {
            if (result instanceof AuthRepository.Result.Success) {
               Toast.makeText(this, "Đã gửi lời mời kết bạn", 0).show();
               this.loadFriends("suggestions");
            } else if (result instanceof AuthRepository.Result.Error) {
               this.handleError(((AuthRepository.Result.Error)result).message);
            }

         });
      } else {
         this.messageFriend(friend);
      }
   }

   private void openFriendProfile(Friend friend) {
      if (friend != null && friend.getUserId() != null) {
         Intent intent = new Intent(this, PublicProfileActivity.class);
         intent.putExtra("user_id", friend.getUserId());
         this.startActivity(intent);
      }
   }

   private void messageFriend(Friend friend) {
      if (friend != null && friend.getUserId() != null) {
         this.createPrivateConversation(friend.getUserId(), friend.getDisplayName() != null ? friend.getDisplayName() : friend.getUsername());
      }
   }

   private void createPrivateConversation(Long userId, String fallbackName) {
      CreateConversationRequest request = new CreateConversationRequest("PRIVATE", (String)null, Collections.singletonList(userId));
      this.conversationRepository.createConversation(request).observe(this, (result) -> {
         if (result instanceof AuthRepository.Result.Success) {
            Conversation conversation = (Conversation)((AuthRepository.Result.Success)result).data;
            if (conversation != null) {
               if (conversation.getName() == null || conversation.getName().isEmpty()) {
                  conversation.setName(fallbackName != null ? fallbackName : "Chat riêng");
               }

               this.openConversation(conversation);
            }
         } else if (result instanceof AuthRepository.Result.Error) {
            this.handleError(((AuthRepository.Result.Error)result).message);
         }

      });
   }

   private void openConversation(Conversation conversation) {
      Intent intent = new Intent(this, ChatActivity.class);
      intent.putExtra("conversation_id", conversation.getId());
      intent.putExtra("conversation_name", conversation.getName());
      this.startActivity(intent);
   }

   private void openSearch() {
      this.startActivity(new Intent(this, SearchUserActivity.class));
   }

   private void openCreateGroup() {
      this.startActivity(new Intent(this, CreateGroupActivity.class));
   }

   private void showEditProfileDialog() {
      if (this.currentUserInfo != null) {
         LinearLayout layout = new LinearLayout(this);
         layout.setOrientation(1);
         layout.setPadding(40, 20, 40, 20);
         EditText etName = new EditText(this);
         etName.setHint("Tên hiển thị");
         etName.setText(this.currentUserInfo.getDisplayName() != null ? this.currentUserInfo.getDisplayName() : "");
         etName.setPadding(20, 24, 20, 24);
         etName.setBackgroundResource(drawable.bg_message_input);
         layout.addView(etName);
         Button btnChangeAvatar = new Button(this);
         btnChangeAvatar.setText("\ud83d\udcf7 Thay đổi ảnh đại diện");
         LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
         lp.topMargin = 20;
         btnChangeAvatar.setLayoutParams(lp);
         btnChangeAvatar.setBackgroundResource(drawable.bg_soft_pill);
         btnChangeAvatar.setTextColor(-14326805);
         btnChangeAvatar.setOnClickListener((v) -> {
            this.pendingUploadType = "AVATAR";
            Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
            intent.addCategory("android.intent.category.OPENABLE");
            intent.setType("image/*");
            intent.addFlags(65);
            this.mediaPickerLauncher.launch(intent);
         });
         layout.addView(btnChangeAvatar);
         (new AlertDialog.Builder(this)).setTitle("Chỉnh sửa hồ sơ").setView(layout).setNegativeButton("Hủy", (DialogInterface.OnClickListener)null).setPositiveButton("Lưu", (d, which) -> {
            String newName = etName.getText().toString().trim();
            if (!newName.isEmpty()) {
               this.userRepository.updateProfile(newName, (String)null).observe(this, (res) -> {
                  if (res instanceof AuthRepository.Result.Success) {
                     Toast.makeText(this, "Đã cập nhật tên hiển thị", 0).show();
                     this.loadProfile();
                  } else if (res instanceof AuthRepository.Result.Error) {
                     this.handleError(((AuthRepository.Result.Error)res).message);
                  }

               });
            }

         }).show();
      }
   }

   private void pickStoryImage() {
      this.pendingUploadType = "STORY";
      Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
      intent.addCategory("android.intent.category.OPENABLE");
      intent.setType("image/*");
      intent.addFlags(65);
      this.mediaPickerLauncher.launch(intent);
   }

   private void handlePickedMedia(Uri uri) {
      if ("AVATAR".equals(this.pendingUploadType)) {
         this.uploadAndSetAvatar(uri);
      } else if ("STORY".equals(this.pendingUploadType)) {
         this.uploadAndCreateStory(uri);
      } else if ("POST_IMAGE".equals(this.pendingUploadType)) {
         this.selectedPostImageUri = uri;
         if (this.dialogImagePreview != null && this.dialogImagePreviewFrame != null) {
            this.dialogImagePreviewFrame.setVisibility(0);
            ((RequestBuilder)Glide.with(this).load(uri).centerCrop()).into(this.dialogImagePreview);
         }
      }

   }

   private void uploadAndSetAvatar(Uri uri) {
      this.progressBar.setVisibility(0);
      String mimeType = this.getContentResolver().getType(uri);
      ContentUriRequestBody body = new ContentUriRequestBody(this.getContentResolver(), uri, mimeType);
      String filename = "avatar_" + System.currentTimeMillis() + this.defaultExtension("IMAGE", mimeType);
      MultipartBody.Part part = Part.createFormData("file", filename, body);
      Toast.makeText(this, "Đang tải ảnh đại diện lên...", 0).show();
      this.mediaApiService.upload(part).enqueue(new Callback<ApiResponse<MediaUpload>>() {
         public void onResponse(Call<ApiResponse<MediaUpload>> call, Response<ApiResponse<MediaUpload>> response) {
            HomeActivity.this.progressBar.setVisibility(8);
            if (response.isSuccessful() && response.body() != null && ((ApiResponse)response.body()).isSuccess() && ((ApiResponse)response.body()).getData() != null) {
               String url = ((MediaUpload)((ApiResponse)response.body()).getData()).getUrl();
               HomeActivity.this.userRepository.updateProfile((String)null, url).observe(HomeActivity.this, (res) -> {
                  if (res instanceof AuthRepository.Result.Success) {
                     Toast.makeText(HomeActivity.this, "Đã cập nhật ảnh đại diện", 0).show();
                     HomeActivity.this.loadProfile();
                  } else {
                     Toast.makeText(HomeActivity.this, "Lưu ảnh đại diện thất bại", 0).show();
                  }

               });
            } else {
               Toast.makeText(HomeActivity.this, "Tải ảnh lên thất bại", 0).show();
            }

         }

         public void onFailure(Call<ApiResponse<MediaUpload>> call, Throwable t) {
            HomeActivity.this.progressBar.setVisibility(8);
            Toast.makeText(HomeActivity.this, "Lỗi kết nối", 0).show();
         }
      });
   }

   private void uploadAndCreateStory(Uri uri) {
      this.progressBar.setVisibility(0);
      String mimeType = this.getContentResolver().getType(uri);
      ContentUriRequestBody body = new ContentUriRequestBody(this.getContentResolver(), uri, mimeType);
      String filename = "story_" + System.currentTimeMillis() + this.defaultExtension("IMAGE", mimeType);
      MultipartBody.Part part = Part.createFormData("file", filename, body);
      Toast.makeText(this, "Đang tải ảnh tin lên...", 0).show();
      this.mediaApiService.upload(part).enqueue(new Callback<ApiResponse<MediaUpload>>() {
         public void onResponse(Call<ApiResponse<MediaUpload>> call, Response<ApiResponse<MediaUpload>> response) {
            HomeActivity.this.progressBar.setVisibility(8);
            if (response.isSuccessful() && response.body() != null && ((ApiResponse)response.body()).isSuccess() && ((ApiResponse)response.body()).getData() != null) {
               String url = ((MediaUpload)((ApiResponse)response.body()).getData()).getUrl();
               Map<String, String> storyBody = new HashMap();
               storyBody.put("mediaUrl", url);
               HomeActivity.this.storyApiService.createStory(storyBody).enqueue(new Callback<ApiResponse<Story>>() {
                  public void onResponse(Call<ApiResponse<Story>> call, Response<ApiResponse<Story>> storyRes) {
                     if (storyRes.isSuccessful() && storyRes.body() != null && ((ApiResponse)storyRes.body()).isSuccess()) {
                        Toast.makeText(HomeActivity.this, "Đăng tin thành công", 0).show();
                        HomeActivity.this.loadStories();
                     } else {
                        Toast.makeText(HomeActivity.this, "Đăng tin thất bại", 0).show();
                     }

                  }

                  public void onFailure(Call<ApiResponse<Story>> call, Throwable t) {
                     Toast.makeText(HomeActivity.this, "Lỗi tạo tin", 0).show();
                  }
               });
            } else {
               Toast.makeText(HomeActivity.this, "Tải ảnh lên thất bại", 0).show();
            }

         }

         public void onFailure(Call<ApiResponse<MediaUpload>> call, Throwable t) {
            HomeActivity.this.progressBar.setVisibility(8);
            Toast.makeText(HomeActivity.this, "Lỗi kết nối", 0).show();
         }
      });
   }

   private String defaultExtension(String type, String mimeType) {
      if (mimeType != null) {
         if (mimeType.contains("png")) {
            return ".png";
         }

         if (mimeType.contains("gif")) {
            return ".gif";
         }

         if (mimeType.contains("webp")) {
            return ".webp";
         }
      }

      return ".jpg";
   }

   private void loadStories() {
      if (this.storyApiService != null) {
         this.storyApiService.getStories().enqueue(new Callback<ApiResponse<List<Story>>>() {
            public void onResponse(Call<ApiResponse<List<Story>>> call, Response<ApiResponse<List<Story>>> response) {
               if (response.isSuccessful() && response.body() != null && ((ApiResponse)response.body()).isSuccess() && ((ApiResponse)response.body()).getData() != null) {
                  HomeActivity.this.storyAdapter.setStories((List)((ApiResponse)response.body()).getData(), HomeActivity.this.currentUserInfo);
               }

            }

            public void onFailure(Call<ApiResponse<List<Story>>> call, Throwable t) {
            }
         });
      }
   }

   private void loadFeed() {
      if (this.postApiService != null) {
         this.progressBar.setVisibility(0);
         this.postApiService.getFeed().enqueue(new Callback<ApiResponse<List<Post>>>() {
            public void onResponse(Call<ApiResponse<List<Post>>> call, Response<ApiResponse<List<Post>>> response) {
               HomeActivity.this.progressBar.setVisibility(8);
               if (response.isSuccessful() && response.body() != null && ((ApiResponse)response.body()).isSuccess() && ((ApiResponse)response.body()).getData() != null) {
                  List<Post> list = (List)((ApiResponse)response.body()).getData();
                  HomeActivity.this.postAdapter.setPosts(list);
                  boolean empty = list.isEmpty();
                  HomeActivity.this.rvFeed.setVisibility(empty ? 8 : 0);
                  HomeActivity.this.feedEmptyState.setVisibility(empty ? 0 : 8);
               }

            }

            public void onFailure(Call<ApiResponse<List<Post>>> call, Throwable t) {
               HomeActivity.this.progressBar.setVisibility(8);
               Toast.makeText(HomeActivity.this, "Không tải được bảng tin", 0).show();
            }
         });
      }
   }

   private void togglePostLike(Post post, final int position) {
      if (this.postApiService != null) {
         this.postApiService.toggleLike(post.getId()).enqueue(new Callback<ApiResponse<Post>>() {
            public void onResponse(Call<ApiResponse<Post>> call, Response<ApiResponse<Post>> response) {
               if (response.isSuccessful() && response.body() != null && ((ApiResponse)response.body()).isSuccess() && ((ApiResponse)response.body()).getData() != null) {
                  HomeActivity.this.postAdapter.updatePost(position, (Post)((ApiResponse)response.body()).getData());
               }

            }

            public void onFailure(Call<ApiResponse<Post>> call, Throwable t) {
               Toast.makeText(HomeActivity.this, "Lỗi kết nối", 0).show();
            }
         });
      }
   }

   private void showCommentsDialog(final Post post) {
      View view = LayoutInflater.from(this).inflate(layout.dialog_comments, (ViewGroup)null);
      final RecyclerView rvComments = (RecyclerView)view.findViewById(id.rv_dialog_comments);
      final EditText etCommentInput = (EditText)view.findViewById(id.et_dialog_comment_input);
      ImageButton btnCommentSend = (ImageButton)view.findViewById(id.btn_dialog_comment_send);
      final TextView tvReplying = (TextView)view.findViewById(id.tv_dialog_comment_replying);
      final CommentAdapter commentAdapter = new CommentAdapter();
      rvComments.setLayoutManager(new LinearLayoutManager(this));
      rvComments.setAdapter(commentAdapter);
      final Comment[] replyingTo = new Comment[1];
      commentAdapter.setListener(new CommentAdapter.OnCommentActionListener() {
         public void onReply(Comment comment) {
            replyingTo[0] = comment;
            String name = comment.getAuthorDisplayName() != null && !comment.getAuthorDisplayName().isEmpty() ? comment.getAuthorDisplayName() : comment.getAuthorUsername();
            tvReplying.setVisibility(0);
            tvReplying.setText("Đang trả lời " + name + ". Chạm để huỷ");
            etCommentInput.setHint("Trả lời " + name + "...");
            etCommentInput.requestFocus();
         }

         public void onReact(Comment comment) {
            HomeActivity.this.showCommentReactionPicker(post, comment, commentAdapter);
         }
      });
      tvReplying.setOnClickListener((v) -> this.clearCommentReplyState(etCommentInput, tvReplying, replyingTo));
      AlertDialog dialog = (new AlertDialog.Builder(this)).setView(view).create();
      if (this.postApiService != null) {
         this.postApiService.getComments(post.getId()).enqueue(new Callback<ApiResponse<List<Comment>>>() {
            public void onResponse(Call<ApiResponse<List<Comment>>> call, Response<ApiResponse<List<Comment>>> response) {
               if (response.isSuccessful() && response.body() != null && ((ApiResponse)response.body()).isSuccess() && ((ApiResponse)response.body()).getData() != null) {
                  commentAdapter.setComments((List)((ApiResponse)response.body()).getData());
                  if (((List)((ApiResponse)response.body()).getData()).size() > 0) {
                     rvComments.scrollToPosition(((List)((ApiResponse)response.body()).getData()).size() - 1);
                  }
               }

            }

            public void onFailure(Call<ApiResponse<List<Comment>>> call, Throwable t) {
               Toast.makeText(HomeActivity.this, "Không tải được bình luận", 0).show();
            }
         });
      }

      btnCommentSend.setOnClickListener((v) -> {
         String text = etCommentInput.getText().toString().trim();
         if (!text.isEmpty()) {
            Map<String, String> body = new HashMap();
            body.put("content", text);
            Comment replyTarget = replyingTo[0];
            if (this.postApiService != null) {
               if (replyTarget != null && replyTarget.getId() != null) {
                  this.postApiService.replyToComment(post.getId(), replyTarget.getId(), body).enqueue(new Callback<ApiResponse<Comment>>() {
                     public void onResponse(Call<ApiResponse<Comment>> call, Response<ApiResponse<Comment>> response) {
                        if (response.isSuccessful() && response.body() != null && ((ApiResponse)response.body()).isSuccess() && ((ApiResponse)response.body()).getData() != null) {
                           commentAdapter.addComment((Comment)((ApiResponse)response.body()).getData());
                           rvComments.scrollToPosition(commentAdapter.getItemCount() - 1);
                           post.setCommentCount(post.getCommentCount() + 1);
                           HomeActivity.this.postAdapter.notifyDataSetChanged();
                           etCommentInput.setText("");
                           HomeActivity.this.clearCommentReplyState(etCommentInput, tvReplying, replyingTo);
                        }

                     }

                     public void onFailure(Call<ApiResponse<Comment>> call, Throwable t) {
                        Toast.makeText(HomeActivity.this, "Gửi trả lời thất bại", 0).show();
                     }
                  });
               } else {
                  this.postApiService.addComment(post.getId(), body).enqueue(new Callback<ApiResponse<Comment>>() {
                     public void onResponse(Call<ApiResponse<Comment>> call, Response<ApiResponse<Comment>> response) {
                        if (response.isSuccessful() && response.body() != null && ((ApiResponse)response.body()).isSuccess() && ((ApiResponse)response.body()).getData() != null) {
                           commentAdapter.addComment((Comment)((ApiResponse)response.body()).getData());
                           rvComments.scrollToPosition(commentAdapter.getItemCount() - 1);
                           post.setCommentCount(post.getCommentCount() + 1);
                           HomeActivity.this.postAdapter.notifyDataSetChanged();
                           etCommentInput.setText("");
                        }

                     }

                     public void onFailure(Call<ApiResponse<Comment>> call, Throwable t) {
                        Toast.makeText(HomeActivity.this, "Gửi bình luận thất bại", 0).show();
                     }
                  });
               }
            }

         }
      });
      dialog.show();
   }

   private void clearCommentReplyState(EditText etCommentInput, TextView tvReplying, Comment[] replyingTo) {
      replyingTo[0] = null;
      etCommentInput.setHint("Viết bình luận...");
      tvReplying.setVisibility(8);
      tvReplying.setText("");
   }

   private void showCommentReactionPicker(Post post, Comment comment, CommentAdapter commentAdapter) {
      if (this.postApiService != null && comment != null && comment.getId() != null) {
         String[] reactions = new String[]{"❤️", "\ud83d\udc4d", "\ud83d\ude02", "\ud83d\ude2e", "\ud83d\ude22", "\ud83d\ude21", "Xóa phản ứng"};
         (new AlertDialog.Builder(this)).setTitle("Chọn biểu cảm").setItems(reactions, (dialog, which) -> {
            Map<String, String> body = new HashMap();
            body.put("reaction", which == reactions.length - 1 ? "" : reactions[which]);
            this.postApiService.reactToComment(post.getId(), comment.getId(), body).enqueue(new Callback<ApiResponse<Comment>>() {
               public void onResponse(Call<ApiResponse<Comment>> call, Response<ApiResponse<Comment>> response) {
                  if (response.isSuccessful() && response.body() != null && ((ApiResponse)response.body()).isSuccess() && ((ApiResponse)response.body()).getData() != null) {
                     commentAdapter.updateComment((Comment)((ApiResponse)response.body()).getData());
                  }

               }

               public void onFailure(Call<ApiResponse<Comment>> call, Throwable t) {
                  Toast.makeText(HomeActivity.this, "Không thả được biểu cảm", 0).show();
               }
            });
         }).show();
      }
   }

   private void showPostComposer() {
      this.selectedPostImageUri = null;
      View view = LayoutInflater.from(this).inflate(layout.dialog_create_post, (ViewGroup)null);
      EditText etContent = (EditText)view.findViewById(id.et_post_content);
      Button btnAttach = (Button)view.findViewById(id.btn_post_attach_image);
      Button btnSubmit = (Button)view.findViewById(id.btn_post_submit);
      Button btnCancel = (Button)view.findViewById(id.btn_post_cancel);
      this.dialogImagePreview = (ImageView)view.findViewById(id.iv_post_image_preview);
      this.dialogImagePreviewFrame = (FrameLayout)view.findViewById(id.frame_post_image_preview);
      ImageButton btnRemoveImage = (ImageButton)view.findViewById(id.btn_remove_post_image);
      AlertDialog dialog = (new AlertDialog.Builder(this)).setView(view).create();
      btnAttach.setOnClickListener((v) -> {
         this.pendingUploadType = "POST_IMAGE";
         Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
         intent.addCategory("android.intent.category.OPENABLE");
         intent.setType("image/*");
         this.mediaPickerLauncher.launch(intent);
      });
      btnRemoveImage.setOnClickListener((v) -> {
         this.selectedPostImageUri = null;
         this.dialogImagePreviewFrame.setVisibility(8);
         this.dialogImagePreview.setImageDrawable((Drawable)null);
      });
      btnCancel.setOnClickListener((v) -> dialog.dismiss());
      btnSubmit.setOnClickListener((v) -> {
         String text = etContent.getText().toString().trim();
         if (text.isEmpty() && this.selectedPostImageUri == null) {
            Toast.makeText(this, "Vui lòng nhập nội dung hoặc chọn ảnh", 0).show();
         } else {
            dialog.dismiss();
            this.uploadAndCreatePost(text, this.selectedPostImageUri);
         }
      });
      dialog.show();
   }

   private void uploadAndCreatePost(final String content, Uri imageUri) {
      if (imageUri == null) {
         this.createPostRequest(content, (String)null);
      } else {
         this.progressBar.setVisibility(0);
         String mimeType = this.getContentResolver().getType(imageUri);
         ContentUriRequestBody body = new ContentUriRequestBody(this.getContentResolver(), imageUri, mimeType);
         String filename = "post_" + System.currentTimeMillis() + this.defaultExtension("IMAGE", mimeType);
         MultipartBody.Part part = Part.createFormData("file", filename, body);
         Toast.makeText(this, "Đang tải ảnh bài viết...", 0).show();
         this.mediaApiService.upload(part).enqueue(new Callback<ApiResponse<MediaUpload>>() {
            public void onResponse(Call<ApiResponse<MediaUpload>> call, Response<ApiResponse<MediaUpload>> response) {
               HomeActivity.this.progressBar.setVisibility(8);
               if (response.isSuccessful() && response.body() != null && ((ApiResponse)response.body()).isSuccess() && ((ApiResponse)response.body()).getData() != null) {
                  String url = ((MediaUpload)((ApiResponse)response.body()).getData()).getUrl();
                  HomeActivity.this.createPostRequest(content, url);
               } else {
                  Toast.makeText(HomeActivity.this, "Tải ảnh bài viết thất bại", 0).show();
               }

            }

            public void onFailure(Call<ApiResponse<MediaUpload>> call, Throwable t) {
               HomeActivity.this.progressBar.setVisibility(8);
               Toast.makeText(HomeActivity.this, "Lỗi kết nối", 0).show();
            }
         });
      }
   }

   private void createPostRequest(String content, String mediaUrl) {
      if (this.postApiService != null) {
         this.progressBar.setVisibility(0);
         Map<String, String> body = new HashMap();
         if (content != null && !content.isEmpty()) {
            body.put("content", content);
         }

         if (mediaUrl != null && !mediaUrl.isEmpty()) {
            body.put("mediaUrl", mediaUrl);
         }

         this.postApiService.createPost(body).enqueue(new Callback<ApiResponse<Post>>() {
            public void onResponse(Call<ApiResponse<Post>> call, Response<ApiResponse<Post>> response) {
               HomeActivity.this.progressBar.setVisibility(8);
               if (response.isSuccessful() && response.body() != null && ((ApiResponse)response.body()).isSuccess() && ((ApiResponse)response.body()).getData() != null) {
                  Toast.makeText(HomeActivity.this, "Đăng bài viết thành công", 0).show();
                  HomeActivity.this.loadFeed();
               } else {
                  Toast.makeText(HomeActivity.this, "Đăng bài viết thất bại", 0).show();
               }

            }

            public void onFailure(Call<ApiResponse<Post>> call, Throwable t) {
               HomeActivity.this.progressBar.setVisibility(8);
               Toast.makeText(HomeActivity.this, "Lỗi tạo bài viết", 0).show();
            }
         });
      }
   }

   private void handleError(String errorMsg) {
      if (errorMsg != null && !errorMsg.isEmpty()) {
         if ("UNAUTHORIZED".equals(errorMsg)) {
            this.redirectToLogin();
         } else {
            Toast.makeText(this, errorMsg, 0).show();
         }

      }
   }

   private void redirectToLogin() {
      Intent intent = new Intent(this, LoginActivity.class);
      intent.setFlags(268468224);
      this.startActivity(intent);
      this.finish();
   }

   private void pollIncomingFriendRequests() {
      if (this.friendApi != null) {
         this.friendApi.getIncomingRequests().enqueue(new Callback<ApiResponse<List<Friend>>>() {
            public void onResponse(Call<ApiResponse<List<Friend>>> call, Response<ApiResponse<List<Friend>>> response) {
               if (response.isSuccessful() && response.body() != null && ((ApiResponse)response.body()).isSuccess() && ((ApiResponse)response.body()).getData() != null) {
                  List<Friend> list = (List)((ApiResponse)response.body()).getData();
                  boolean hasNewRequest = false;

                  for(Friend f : list) {
                     if (f.getFriendshipId() != null && !HomeActivity.this.incomingRequestIds.contains(f.getFriendshipId())) {
                        HomeActivity.this.incomingRequestIds.add(f.getFriendshipId());
                        if (!HomeActivity.this.isFirstFriendRequestsLoad) {
                           hasNewRequest = true;
                        }
                     }
                  }

                  HomeActivity.this.isFirstFriendRequestsLoad = false;
                  if (hasNewRequest) {
                     HomeActivity.this.playNotificationSoundAndVibrate();
                  }

                  if ("incoming".equals(HomeActivity.this.friendMode)) {
                     HomeActivity.this.friendAdapter.setFriends(list);
                     boolean empty = list.isEmpty();
                     HomeActivity.this.rvFriends.setVisibility(empty ? 8 : 0);
                     HomeActivity.this.tvFriendsEmpty.setVisibility(empty ? 0 : 8);
                     HomeActivity.this.tvFriendsEmpty.setText(HomeActivity.this.emptyTextForMode());
                  }
               }

            }

            public void onFailure(Call<ApiResponse<List<Friend>>> call, Throwable t) {
            }
         });
      }
   }

   private void checkNewMessagesForConversation(final Long conversationId) {
      if (this.messageApiService != null) {
         this.messageApiService.getMessages(conversationId).enqueue(new Callback<ApiResponse<List<Message>>>() {
            public void onResponse(Call<ApiResponse<List<Message>>> call, Response<ApiResponse<List<Message>>> response) {
               if (response.isSuccessful() && response.body() != null && ((ApiResponse)response.body()).isSuccess() && ((ApiResponse)response.body()).getData() != null) {
                  List<Message> messages = (List)((ApiResponse)response.body()).getData();
                  if (!messages.isEmpty()) {
                     Message lastMessage = (Message)messages.get(messages.size() - 1);
                     Long lastId = lastMessage.getId();
                     if (lastId != null) {
                        if (HomeActivity.this.lastMessageIdMap.containsKey(conversationId)) {
                           Long prevId = (Long)HomeActivity.this.lastMessageIdMap.get(conversationId);
                           if (prevId != null && !prevId.equals(lastId)) {
                              String currentUsername = HomeActivity.this.tokenManager.getUsername();
                              if (currentUsername != null && !currentUsername.equals(lastMessage.getSenderUsername())) {
                                 HomeActivity.this.playNotificationSoundAndVibrate();
                              }
                           }
                        }

                        HomeActivity.this.lastMessageIdMap.put(conversationId, lastId);
                     }
                  }
               }

            }

            public void onFailure(Call<ApiResponse<List<Message>>> call, Throwable t) {
            }
         });
      }
   }

   private void playNotificationSoundAndVibrate() {
      try {
         Vibrator vibrator = (Vibrator)this.getSystemService("vibrator");
         if (vibrator != null) {
            if (VERSION.SDK_INT >= 26) {
               vibrator.vibrate(VibrationEffect.createOneShot(200L, -1));
            } else {
               vibrator.vibrate(200L);
            }
         }

         Uri notificationUri = RingtoneManager.getDefaultUri(2);
         Ringtone ringtone = RingtoneManager.getRingtone(this.getApplicationContext(), notificationUri);
         if (ringtone != null) {
            ringtone.play();
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

   }
}
