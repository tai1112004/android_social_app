package com.yourapp.ui.home;

import android.media.MediaPlayer;
import com.yourapp.model.MusicTrack;
import com.yourapp.ui.home.MusicPickerBottomSheet;
import com.yourapp.R;


import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.os.Build;
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
import android.text.InputType;
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
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
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
import com.yourapp.data.remote.CallApiService;
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
import com.yourapp.model.CallSessionSnapshot;
import com.yourapp.model.Comment;
import com.yourapp.model.Conversation;
import com.yourapp.model.CreateConversationRequest;
import com.yourapp.model.Friend;
import com.yourapp.model.MediaUpload;
import com.yourapp.model.Message;
import com.yourapp.model.Post;
import com.yourapp.model.StoredNotification;
import com.yourapp.model.Story;
import com.yourapp.model.StoryGroup;
import com.yourapp.model.User;
import com.yourapp.network.RetrofitClient;
import com.yourapp.ui.auth.LoginActivity;
import com.yourapp.ui.chat.ChatActivity;
import com.yourapp.ui.feed.CommentAdapter;
import com.yourapp.ui.feed.PostAdapter;
import com.yourapp.ui.group.CreateGroupActivity;
import com.yourapp.ui.notification.NotificationsActivity;
import com.yourapp.ui.profile.PublicProfileActivity;
import com.yourapp.ui.search.SearchUserActivity;
import com.yourapp.util.AvatarUtil;
import com.yourapp.util.BackendConfig;
import com.yourapp.util.NotificationStore;
import com.yourapp.util.ContentUriRequestBody;
import com.yourapp.util.TokenManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.yourapp.model.RealtimeNotification;
import com.yourapp.realtime.NotificationSocketClient;
import com.yourapp.realtime.SignalingService;
import com.yourapp.ui.call.IncomingCallActivity;
import okhttp3.MultipartBody;
import okhttp3.MultipartBody.Part;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {
    private MusicTrack selectedStoryMusic;
    private MediaPlayer dialogMusicPlayer;

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
   private TextView tvProfileBio;
   private TextView tvProfileMeta;
   private TextView tvProfileFriendCount;
   private TextView tvProfilePostCount;
   private TextView tvProfileStoryCount;
   private View btnProfileEdit;
   private TextView tvFeedAvatar;
   private FloatingActionButton fabHome;
   private RadioGroup friendsFilterGroup;
   private ImageView ivHomeAvatar;
   private ImageView ivProfileAvatar;
   private ImageView ivProfileCover;
   private ImageView ivFeedMyAvatar;
   private FrameLayout frameProfileAvatar;
   private RecyclerView rvProfilePosts;
   private View profilePostsEmptyState;
   private RecyclerView rvStories;
   private RecyclerView rvFeed;
   private View feedEmptyState;
   private View btnFeedCompose;
   private StoryAdapter storyAdapter;
   private PostAdapter postAdapter;
   private PostAdapter profilePostAdapter;
   private final List<Post> cachedFeedPosts = new ArrayList<>();
   private StoryApiService storyApiService;
   private PostApiService postApiService;
   private MediaApiService mediaApiService;
   private User currentUserInfo;
   private String pendingUploadType;
   private Uri selectedPostImageUri;
   private ImageView dialogImagePreview;
   private FrameLayout dialogImagePreviewFrame;
   private final ActivityResultLauncher<Intent> editProfileLauncher = this.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), (result) -> {
      if (result.getResultCode() == -1 && result.getData() != null) {
         Intent data = result.getData();
         if (this.currentUserInfo != null) {
            String name = data.getStringExtra("displayName");
            String avatarUrl = data.getStringExtra("avatarUrl");
            String coverUrl = data.getStringExtra("coverUrl");
            String bio = data.getStringExtra("bio");
            String location = data.getStringExtra("location");
            String website = data.getStringExtra("website");
            String email = data.getStringExtra("email");
            if (name != null) this.currentUserInfo.setDisplayName(name);
            if (avatarUrl != null) this.currentUserInfo.setAvatarUrl(avatarUrl);
            if (coverUrl != null) this.currentUserInfo.setCoverUrl(coverUrl);
            if (bio != null) this.currentUserInfo.setBio(bio);
            if (location != null) this.currentUserInfo.setLocation(location);
            if (website != null) this.currentUserInfo.setWebsite(website);
            if (email != null) this.currentUserInfo.setEmail(email);
         }
         this.loadProfile();
      }
   });

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
   private CallApiService callApiService;
   private FriendApiService friendApi;
   private final Map<Long, Long> lastMessageIdMap = new HashMap();
   private final Set<Long> incomingRequestIds = new HashSet();
   private boolean isFirstFriendRequestsLoad = true;
   private final Handler pollingHandler = new Handler(Looper.getMainLooper());
   private Runnable pollingRunnable;
   private boolean incomingCallPollInFlight;
   private boolean incomingCallOpening;
   private String lastIncomingSessionId;
   private final Runnable incomingCallPollRunnable = new Runnable() {
      public void run() {
         HomeActivity.this.pollIncomingCall();
         HomeActivity.this.pollingHandler.postDelayed(this, 1500L);
      }
   };
   private NotificationSocketClient notificationSocketClient;
   private SignalingService callSocketClient;
   private NotificationStore notificationStore;
   private View notificationBar;
   private TextView tvNotificationTitle;
   private TextView tvNotificationMessage;
   private TextView tvNotificationBadge;
   private View btnNotificationClose;
   private final Handler notificationBannerHandler = new Handler(Looper.getMainLooper());
   private final Runnable hideNotificationBannerRunnable = this::hideNotificationBanner;

   public HomeActivity() {
   }

   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      this.setContentView(layout.activity_home);
      this.tokenManager = new TokenManager(this);
      this.notificationStore = createNotificationStoreForCurrentUser();
      if (!this.tokenManager.hasValidTokens()) {
         this.redirectToLogin();
      } else {
         this.bindViews();
         this.setupLists();
         this.setupActions();
         if (this.maybePromptBackendHost()) {
            return;
         }
         this.buildRepositories();
         this.refreshNotificationBadge();
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

         // Ket noi WebSocket de nhan thong bao real-time
         connectNotificationSocket();
         connectCallSocket();
      }
   }

   private NotificationStore createNotificationStoreForCurrentUser() {
      long userId = tokenManager != null ? tokenManager.getUserId() : -1L;
      if (userId > 0) {
         return new NotificationStore(this, userId);
      }
      String username = tokenManager != null ? tokenManager.getUsername() : null;
      if (username != null && !username.trim().isEmpty()) {
         return new NotificationStore(this, "username_" + username.trim());
      }
      return new NotificationStore(this);
   }
   private void connectCallSocket() {
      String token = tokenManager.getAccessToken();
      Long userId = tokenManager.getUserId();
      if (token == null || userId <= 0) return;
      if (callSocketClient != null) {
         callSocketClient.disconnect();
      }
      callSocketClient = new SignalingService(
              com.yourapp.util.Constants.getWsUrl(), token, userId,
              new SignalingService.Listener() {
                 @Override public void onConnected() {
                    runOnUiThread(() -> Toast.makeText(HomeActivity.this, "Da san sang nhan cuoc goi", Toast.LENGTH_SHORT).show());
                 }
                 @Override public void onIncomingCall(com.yourapp.model.CallSignalMessage message) {
                    runOnUiThread(() -> handleIncomingCall(message));
                 }
                 @Override public void onCallAnswered(com.yourapp.model.CallSignalMessage message) { }
                 @Override public void onIceCandidate(com.yourapp.model.CallSignalMessage message) { }
                 @Override public void onCallRejected(com.yourapp.model.CallSignalMessage message) { }
                 @Override public void onCallEnded(com.yourapp.model.CallSignalMessage message) { }
                 @Override public void onDisconnected() { }
                 @Override public void onError(String message) {
                    runOnUiThread(() -> Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show());
                 }
              });
      callSocketClient.connect();
   }

   private void handleIncomingCall(com.yourapp.model.CallSignalMessage message) {
      if (message == null || message.getSessionId() == null || message.getSessionId().isEmpty()) return;
      if (message.getSessionId().equals(lastIncomingSessionId) || incomingCallOpening) return;
      lastIncomingSessionId = message.getSessionId();
      incomingCallOpening = true;
      Toast.makeText(HomeActivity.this, "Co cuoc goi den", Toast.LENGTH_SHORT).show();
      showIncomingCallHeadsUp(message);
      openIncomingCall(message);
   }

   private void pollIncomingCall() {
      if (callApiService == null || incomingCallPollInFlight || incomingCallOpening) return;
      incomingCallPollInFlight = true;
      callApiService.getIncomingCall().enqueue(new Callback<ApiResponse<CallSessionSnapshot>>() {
         @Override public void onResponse(Call<ApiResponse<CallSessionSnapshot>> call, Response<ApiResponse<CallSessionSnapshot>> response) {
            incomingCallPollInFlight = false;
            if (!response.isSuccessful() || response.body() == null || !Boolean.TRUE.equals(response.body().isSuccess())) return;
            CallSessionSnapshot snapshot = response.body().getData();
            if (snapshot == null || snapshot.getSessionId() == null || !"RINGING".equals(snapshot.getStatus())) return;
            com.yourapp.model.CallSignalMessage message = new com.yourapp.model.CallSignalMessage();
            message.setType("INCOMING_CALL");
            message.setSessionId(snapshot.getSessionId());
            message.setCallerId(snapshot.getCallerId());
            message.setCalleeId(snapshot.getCalleeId());
            message.setCallerName(snapshot.getCallerName());
            message.setCallerAvatarUrl(snapshot.getCallerAvatarUrl());
            message.setSdp(snapshot.getOfferSdp());
            handleIncomingCall(message);
         }

         @Override public void onFailure(Call<ApiResponse<CallSessionSnapshot>> call, Throwable t) {
            incomingCallPollInFlight = false;
         }
      });
   }

   private void openIncomingCall(com.yourapp.model.CallSignalMessage message) {
      if (message == null) return;
      Intent intent = new Intent(this, IncomingCallActivity.class);
      intent.putExtra(IncomingCallActivity.EXTRA_SESSION_ID, message.getSessionId());
      intent.putExtra(IncomingCallActivity.EXTRA_CALLER_ID, message.getCallerId() != null ? message.getCallerId() : -1L);
      intent.putExtra(IncomingCallActivity.EXTRA_CALLER_NAME, message.getCallerName());
      intent.putExtra(IncomingCallActivity.EXTRA_CALLER_AVATAR, message.getCallerAvatarUrl());
      intent.putExtra(IncomingCallActivity.EXTRA_OFFER_SDP, message.getSdp());
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
      startActivity(intent);
   }

   private void showIncomingCallHeadsUp(com.yourapp.model.CallSignalMessage message) {
      if (message == null) return;
      String channelId = "incoming_call_channel";
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
         NotificationManager manager = getSystemService(NotificationManager.class);
         if (manager != null && manager.getNotificationChannel(channelId) == null) {
            NotificationChannel channel = new NotificationChannel(channelId, "Incoming calls", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Thong bao cuoc goi den");
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
            manager.createNotificationChannel(channel);
         }
      }

      Intent intent = new Intent(this, IncomingCallActivity.class);
      intent.putExtra(IncomingCallActivity.EXTRA_SESSION_ID, message.getSessionId());
      intent.putExtra(IncomingCallActivity.EXTRA_CALLER_ID, message.getCallerId() != null ? message.getCallerId() : -1L);
      intent.putExtra(IncomingCallActivity.EXTRA_CALLER_NAME, message.getCallerName());
      intent.putExtra(IncomingCallActivity.EXTRA_CALLER_AVATAR, message.getCallerAvatarUrl());
      intent.putExtra(IncomingCallActivity.EXTRA_OFFER_SDP, message.getSdp());
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

      PendingIntent pi = PendingIntent.getActivity(this, 3411, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
      NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
              .setSmallIcon(R.mipmap.ic_launcher)
              .setContentTitle(message.getCallerName() != null ? message.getCallerName() : "Cuoc goi den")
              .setContentText("Ban co mot cuoc goi moi")
              .setPriority(NotificationCompat.PRIORITY_MAX)
              .setCategory(NotificationCompat.CATEGORY_CALL)
              .setFullScreenIntent(pi, true)
              .setContentIntent(pi)
              .setOngoing(true)
              .setAutoCancel(false)
              .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
      NotificationManagerCompat.from(this).notify(3411, builder.build());
   }
   private void connectNotificationSocket() {
      String token = tokenManager.getAccessToken();
      Long userId = tokenManager.getUserId();
      if (token == null || userId <= 0) return;
      if (notificationSocketClient != null) notificationSocketClient.disconnect();
      notificationSocketClient = new NotificationSocketClient(
              com.yourapp.util.Constants.getWsUrl(), token, userId,
              this::handleRealtimeNotification);
      notificationSocketClient.connect();
   }

   private void handleRealtimeNotification(RealtimeNotification n) {
      if (n == null || n.getType() == null) return;

      if (this.notificationStore != null) {
         this.notificationStore.add(buildStoredNotification(n));
         refreshNotificationBadge();
      }

      switch (n.getType()) {
         case "NEW_MESSAGE":
            if (viewModel != null) viewModel.loadConversations();
            showNotificationBanner(
                    "Thong bao" + (n.getTitle() != null ? n.getTitle() : "Thong bao"),
                    n.getMessage() != null ? n.getMessage() : "");
            break;
         case "FRIEND_REQUEST":
            loadFriends("requests");
            showNotificationBanner(
                    "Thong bao",
                    (n.getActorDisplayName() != null ? n.getActorDisplayName() : "Thong bao") + "Thong bao");
            break;
         case "FRIEND_ACCEPTED":
            loadFriends("friends");
            showNotificationBanner(
                    "Thong bao",
                    (n.getActorDisplayName() != null ? n.getActorDisplayName() : "Thong bao") + "Thong bao");
            break;
         case "NEW_POST":
            loadFeed();
            showNotificationBanner(
                    "Thong bao" + (n.getTitle() != null ? n.getTitle() : "Thong bao"),
                    n.getMessage() != null ? n.getMessage() : "");
            break;
         case "INCOMING_CALL":
            openIncomingCallFromNotification(n);
            break;
         case "NEW_STORY":
            loadStories();
            showNotificationBanner(
                    "Thong bao" + (n.getTitle() != null ? n.getTitle() : "Thong bao"),
                    n.getMessage() != null ? n.getMessage() : "");
            break;
         default:
            showNotificationBanner(
                    n.getTitle() != null ? n.getTitle() : "Thong bao",
                    n.getMessage() != null ? n.getMessage() : "");
            break;
      }
   }

   private void openIncomingCallFromNotification(RealtimeNotification n) {
      Intent intent = new Intent(this, IncomingCallActivity.class);
      intent.putExtra(IncomingCallActivity.EXTRA_SESSION_ID, asString(n.getData(), "callSessionId"));
      intent.putExtra(IncomingCallActivity.EXTRA_CALLER_NAME, n.getActorDisplayName() != null ? n.getActorDisplayName() : n.getTitle());
      intent.putExtra(IncomingCallActivity.EXTRA_CALLER_AVATAR, asString(n.getData(), "callerAvatar"));
      Long callerId = n.getActorId();
      if (callerId != null) {
         intent.putExtra(IncomingCallActivity.EXTRA_CALLER_ID, callerId);
      }
      intent.putExtra(IncomingCallActivity.EXTRA_OFFER_SDP, asString(n.getData(), "offerSdp"));
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
      startActivity(intent);
   }

   private String asString(Map<String, Object> data, String key) {
      if (data == null || key == null) return null;
      Object value = data.get(key);
      return value != null ? String.valueOf(value) : null;
   }
   private StoredNotification buildStoredNotification(RealtimeNotification n) {
      StoredNotification item = new StoredNotification();
      item.setId(n.getId() != null ? n.getId() : String.valueOf(System.currentTimeMillis()));
      item.setType(n.getType());
      item.setTitle(n.getTitle());
      item.setMessage(n.getMessage());
      item.setActorId(n.getActorId());
      item.setActorUsername(n.getActorUsername());
      item.setActorDisplayName(n.getActorDisplayName());
      item.setActorAvatarUrl(n.getActorAvatarUrl());
      item.setCreatedAt(n.getCreatedAt());
      item.setData(n.getData());
      item.setRead(false);
      item.setReceivedAtMillis(System.currentTimeMillis());
      return item;
   }

   private void showNotificationBanner(String title, String message) {
      runOnUiThread(() -> {
         if (notificationBar == null || tvNotificationTitle == null || tvNotificationMessage == null) {
            Toast.makeText(this, title + (message.isEmpty() ? "" : "\n" + message), Toast.LENGTH_SHORT).show();
            playNotificationSoundAndVibrate();
            return;
         }

         tvNotificationTitle.setText(title);
         tvNotificationMessage.setText(message);
         notificationBar.setVisibility(View.VISIBLE);
         notificationBannerHandler.removeCallbacks(hideNotificationBannerRunnable);
         notificationBannerHandler.postDelayed(hideNotificationBannerRunnable, 4000L);
         playNotificationSoundAndVibrate();
      });
   }

   private void hideNotificationBanner() {
      if (notificationBar != null) {
         notificationBar.setVisibility(View.GONE);
      }
      notificationBannerHandler.removeCallbacks(hideNotificationBannerRunnable);
   }

   private void refreshNotificationBadge() {
      if (tvNotificationBadge == null || notificationStore == null) return;
      int unread = notificationStore.getUnreadCount();
      if (unread > 0) {
         tvNotificationBadge.setVisibility(View.VISIBLE);
         tvNotificationBadge.setText(unread > 99 ? "99+" : String.valueOf(unread));
      } else {
         tvNotificationBadge.setVisibility(View.GONE);
      }
   }


   private void openNotificationCenter() {
      hideNotificationBanner();
      startActivity(new Intent(this, NotificationsActivity.class));
   }

   private boolean maybePromptBackendHost() {
      return false;
   }

   private void showBackendHostDialog() {
      // Disabled to keep the original simple startup flow.
   }

   private void applyBackendHostChange() {
      RetrofitClient.reset();
      if (notificationSocketClient != null) {
         notificationSocketClient.disconnect();
         notificationSocketClient = null;
      }
      if (callSocketClient != null) {
         callSocketClient.disconnect();
         callSocketClient = null;
      }
      recreate();
   }

   protected void onResume() {
      super.onResume();
      this.incomingCallOpening = false;
      if (this.viewModel != null) {
         this.viewModel.loadConversations();
      }

      if (this.friendRepository != null) {
         this.refreshFriendTabCounts();
      this.loadFriends(this.friendMode);
      }

      this.pollingHandler.removeCallbacks(this.pollingRunnable);
      this.pollingHandler.postDelayed(this.pollingRunnable, 5000L);
      this.pollingHandler.removeCallbacks(this.incomingCallPollRunnable);
      this.pollingHandler.post(this.incomingCallPollRunnable);

      // Ket noi lai WebSocket khi quay lai app
      if (tokenManager != null && tokenManager.hasValidTokens()) {
         connectNotificationSocket();
         connectCallSocket();
      }
      refreshNotificationBadge();
      if (this.panelProfile != null && this.panelProfile.getVisibility() == 0) {
         this.loadProfile();
      }
   }

   protected void onPause() {
      super.onPause();
      this.pollingHandler.removeCallbacks(this.pollingRunnable);
      this.pollingHandler.removeCallbacks(this.incomingCallPollRunnable);
      if (notificationSocketClient != null) {
         notificationSocketClient.disconnect();
         notificationSocketClient = null;
      }
      if (callSocketClient != null) {
         callSocketClient.disconnect();
         callSocketClient = null;
      }
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
      this.tvProfileBio = (TextView)this.findViewById(id.tv_profile_bio);
      this.tvProfileMeta = (TextView)this.findViewById(id.tv_profile_meta);
      this.tvProfileFriendCount = (TextView)this.findViewById(id.tv_profile_friend_count);
      this.tvProfilePostCount = (TextView)this.findViewById(id.tv_profile_post_count);
      this.tvProfileStoryCount = (TextView)this.findViewById(id.tv_profile_story_count);
      this.btnProfileEdit = this.findViewById(id.btn_profile_edit);
      this.tvFeedAvatar = (TextView)this.findViewById(id.tv_feed_my_avatar);
      this.fabHome = (FloatingActionButton)this.findViewById(id.fab_home);
      this.friendsFilterGroup = (RadioGroup)this.findViewById(id.friends_filter_group);
      this.ivHomeAvatar = (ImageView)this.findViewById(id.iv_home_avatar);
      this.ivProfileAvatar = (ImageView)this.findViewById(id.iv_profile_avatar);
      this.ivFeedMyAvatar = (ImageView)this.findViewById(id.iv_feed_my_avatar);
      this.frameProfileAvatar = (FrameLayout)this.findViewById(id.frame_profile_avatar);
      this.rvProfilePosts = (RecyclerView)this.findViewById(id.rv_profile_posts);
      this.profilePostsEmptyState = this.findViewById(id.tv_profile_posts_empty);
      this.rvStories = (RecyclerView)this.findViewById(id.rv_stories);
      this.rvFeed = (RecyclerView)this.findViewById(id.rv_feed);
      this.feedEmptyState = this.findViewById(id.feed_empty_state);
      this.btnFeedCompose = this.findViewById(id.btn_feed_compose);
      this.notificationBar = this.findViewById(id.notification_bar);
      this.tvNotificationTitle = (TextView)this.findViewById(id.tv_notification_title);
      this.tvNotificationMessage = (TextView)this.findViewById(id.tv_notification_message);
      this.tvNotificationBadge = (TextView)this.findViewById(id.tv_notification_badge);
      this.btnNotificationClose = this.findViewById(id.btn_notification_close);
   }

   private void buildRepositories() {
      ConversationApiService conversationApi = (ConversationApiService)RetrofitClient.getInstance(this.tokenManager).create(ConversationApiService.class);
      FriendApiService friendApi = (FriendApiService)RetrofitClient.getInstance(this.tokenManager).create(FriendApiService.class);
      UserApiService userApi = (UserApiService)RetrofitClient.getInstance(this.tokenManager).create(UserApiService.class);
      this.friendApi = friendApi;
      this.messageApiService = (MessageApiService)RetrofitClient.getInstance(this.tokenManager).create(MessageApiService.class);
      this.callApiService = (CallApiService)RetrofitClient.getInstance(this.tokenManager).create(CallApiService.class);
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
            HomeActivity.this.showStoryComposer();
         }

         public void onViewStory(List<StoryGroup> groups, int startIndex) {
            Intent intent = new Intent(HomeActivity.this, StoryViewerActivity.class);
            intent.putExtra("storyGroupsJson", new com.google.gson.Gson().toJson(groups));
            intent.putExtra("startGroupIndex", startIndex);
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
      this.profilePostAdapter = new PostAdapter(new PostAdapter.OnPostActionListener() {
         public void onLike(Post post, int position) {
            HomeActivity.this.togglePostLike(post, position);
         }

         public void onComment(Post post) {
            HomeActivity.this.showCommentsDialog(post);
         }
      });
      if (this.rvProfilePosts != null) {
         this.rvProfilePosts.setLayoutManager(new LinearLayoutManager(this));
         this.rvProfilePosts.setAdapter(this.profilePostAdapter);
      }
   }

   private void setupActions() {
      BottomNavigationView bottomNav = (BottomNavigationView)this.findViewById(id.bottom_nav_home);
      bottomNav.setOnItemSelectedListener((item) -> {
         int itemId = item.getItemId();
         if (itemId == id.nav_friends) {
            this.showFriends();
            return true;
         } else if (itemId == id.nav_feed) {
            this.showFeed();
            return true;
         } else if (itemId == id.nav_profile) {
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
      this.frameProfileAvatar.setOnClickListener((v) -> this.openEditProfileActivity());
      if (this.btnProfileEdit != null) {
         this.btnProfileEdit.setOnClickListener((v) -> this.openEditProfileActivity());
      }
      this.fabHome.setOnClickListener((v) -> this.openSearch());
      View searchPill = this.findViewById(id.home_search_pill);
      if (searchPill != null) { searchPill.setVisibility(8); }
      View btnNotifications = this.findViewById(id.btn_home_notifications);
      btnNotifications.setOnClickListener((v) -> this.openNotificationCenter());
      btnNotifications.setOnLongClickListener((v) -> {
         this.showBackendHostDialog();
         return true;
      });
      if (this.notificationBar != null) {
         this.notificationBar.setOnClickListener((v) -> this.openNotificationCenter());
      }
      if (this.btnNotificationClose != null) {
         this.btnNotificationClose.setOnClickListener((v) -> this.hideNotificationBanner());
      }
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
      this.tvHomeTitle.setText("Tin nh\u1EAFn");
      this.tvHomeSubtitle.setText("Tin nh\u1EAFn v\u00E0 nh\u00F3m");
      this.loadStories();
   }

   private void showFriends() {
      this.panelChats.setVisibility(8);
      this.panelFriends.setVisibility(0);
      this.panelFeed.setVisibility(8);
      this.panelProfile.setVisibility(8);
      this.fabHome.show();
      this.tvHomeTitle.setText("B\u1EA1n b\u00E8");
      this.tvHomeSubtitle.setText("B\u1EA1n b\u00E8, l\u1EDDi m\u1EDDi v\u00E0 tr\u1EA1ng th\u00E1i");
      this.refreshFriendTabCounts();
      this.loadFriends(this.friendMode);
   }

   private void showFeed() {
      this.panelChats.setVisibility(8);
      this.panelFriends.setVisibility(8);
      this.panelFeed.setVisibility(0);
      this.panelProfile.setVisibility(8);
      this.fabHome.hide();
      this.tvHomeTitle.setText("B\u1EA3ng tin");
      this.tvHomeSubtitle.setText("B\u00E0i vi\u1EBFt, c\u1EA3m x\u00FAc v\u00E0 tin m\u1EDBi");
      this.loadFeed();
   }

   private void showProfile() {
      this.panelChats.setVisibility(8);
      this.panelFriends.setVisibility(8);
      this.panelFeed.setVisibility(8);
      this.panelProfile.setVisibility(0);
      this.fabHome.hide();
      this.tvHomeTitle.setText("H\u1ED3 s\u01A1");
      this.tvHomeSubtitle.setText("T\u00E0i kho\u1EA3n v\u00E0 thao t\u00E1c nhanh");
      this.loadProfile();
   }

   private void loadProfile() {
      this.userRepository.getMe().observe(this, (result) -> {
         if (result instanceof AuthRepository.Result.Success) {
            User user = (User)((AuthRepository.Result.Success)result).data;
            this.currentUserInfo = user;
            if (user != null && user.getId() != null) {
               long oldId = tokenManager.getUserId();
               tokenManager.saveUserId(user.getId());
               if (notificationStore != null) {
                  notificationStore.setUserId(user.getId());
                  refreshNotificationBadge();
               }
               if (notificationSocketClient == null || oldId != user.getId()) {
                  connectNotificationSocket();
                  connectCallSocket();
               }
            }

            String username = user != null && user.getUsername() != null ? user.getUsername() : "username";
            String name = user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty() ? user.getDisplayName() : username;
            this.tvProfileName.setText(name);
            this.tvProfileUsername.setText("@" + username);
            if (this.tvProfileBio != null) {
               String bio = user != null && user.getBio() != null && !user.getBio().trim().isEmpty() ? user.getBio().trim() : "Chua co gioi thieu";
               this.tvProfileBio.setText(bio);
            }
            if (this.tvProfileMeta != null) {
               StringBuilder meta = new StringBuilder();
               if (user != null) {
                  if (user.getLocation() != null && !user.getLocation().trim().isEmpty()) {
                     meta.append("\uD83D\uDCCD ").append(user.getLocation().trim());
                  }
                  if (user.getWebsite() != null && !user.getWebsite().trim().isEmpty()) {
                     if (meta.length() > 0) meta.append("\n");
                     meta.append("\uD83C\uDF10 ").append(user.getWebsite().trim());
                  }
                  if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                     if (meta.length() > 0) meta.append("\n");
                     meta.append("\u2709\uFE0F ").append(user.getEmail().trim());
                  }
               }
               if (meta.length() == 0) {
                  meta.append("Trang ca nhan cua ban");
               }
               this.tvProfileMeta.setText(meta.toString());
            }
            if (this.tvProfileFriendCount != null) {
               this.tvProfileFriendCount.setText(String.valueOf(user != null ? user.getFriendCount() : 0));
            }
            if (this.tvProfilePostCount != null) {
               this.tvProfilePostCount.setText(String.valueOf(user != null ? user.getPostCount() : 0));
            }
            if (this.tvProfileStoryCount != null) {
               this.tvProfileStoryCount.setText(String.valueOf(user != null ? user.getStoryCount() : 0));
            }
            if (user != null) {
               String coverSource = user.getCoverUrl();
               if ((coverSource == null || coverSource.trim().isEmpty()) && this.currentUserInfo != null) {
                  coverSource = this.currentUserInfo.getCoverUrl();
               }
               if (this.ivProfileCover != null) {
                  if (coverSource != null && !coverSource.trim().isEmpty()) {
                     Glide.with(this).load(coverSource.trim()).placeholder(drawable.bg_profile_cover_placeholder).centerCrop().into(this.ivProfileCover);
                  } else {
                     Glide.with(this).load(drawable.bg_profile_cover_placeholder).centerCrop().into(this.ivProfileCover);
                  }
               }
               AvatarUtil.loadAvatar(user.getAvatarUrl(), user.getDisplayName(), user.getUsername(), this.ivHomeAvatar, this.tvHomeAvatar);
               AvatarUtil.loadAvatar(user.getAvatarUrl(), user.getDisplayName(), user.getUsername(), this.ivProfileAvatar, this.tvProfileAvatar);
               AvatarUtil.loadAvatar(user.getAvatarUrl(), user.getDisplayName(), user.getUsername(), this.ivFeedMyAvatar, this.tvFeedAvatar);
            }
            this.loadFeed();
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
         return "Kh\u00F4ng c\u00F3 l\u1EDDi m\u1EDDi m\u1EDBi";
      } else if ("sent".equals(this.friendMode)) {
         return "Ch\u01B0a g\u1EEDi l\u1EDDi m\u1EDDi n\u00E0o";
      } else {
         return "suggestions".equals(this.friendMode) ? "Ch\u01B0a c\u00F3 g\u1EE3i \u00FD b\u1EA1n b\u00E8" : "Ch\u01B0a c\u00F3 b\u1EA1n b\u00E8";
      }
   }

   private void handleFriendAction(Friend friend) {
      if ("incoming".equals(this.friendMode)) {
         this.friendRepository.accept(friend.getFriendshipId()).observe(this, (result) -> {
            if (result instanceof AuthRepository.Result.Success) {
               this.loadFriends("incoming");
               Toast.makeText(this, "Tai anh dai dien thanh cong", 0).show();
            } else if (result instanceof AuthRepository.Result.Error) {
               this.handleError(((AuthRepository.Result.Error)result).message);
            }

         });
      } else if ("sent".equals(this.friendMode)) {
         this.friendRepository.remove(friend.getFriendshipId()).observe(this, (result) -> {
            this.loadFriends("sent");
            Toast.makeText(this, "Tai anh dai dien thanh cong", 0).show();
         });
      } else if ("suggestions".equals(this.friendMode)) {
         this.friendRepository.sendRequest(friend.getUserId()).observe(this, (result) -> {
            if (result instanceof AuthRepository.Result.Success) {
               Toast.makeText(this, "Tai anh dai dien thanh cong", 0).show();
               this.loadFriends("suggestions");
            } else if (result instanceof AuthRepository.Result.Error) {
               this.handleError(((AuthRepository.Result.Error)result).message);
            }

         });
      } else {
         this.showFriendActionsDialog(friend);
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
                  conversation.setName(fallbackName != null ? fallbackName : "Thong bao");
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

   private void refreshFriendTabCounts() {
      if (this.friendRepository == null) {
         return;
      }
      final int[] counts = new int[]{0, 0, 0, 0};
      final int[] pending = new int[]{4};
      Runnable done = () -> {
         pending[0]--;
         if (pending[0] <= 0) {
            this.updateFriendTabLabels(counts[0], counts[1], counts[2], counts[3]);
         }
      };
      this.friendRepository.getFriends().observe(this, (result) -> {
         if (result instanceof AuthRepository.Result.Success) {
            List<Friend> list = (List)((AuthRepository.Result.Success)result).data;
            counts[0] = list != null ? list.size() : 0;
         }
         done.run();
      });
      this.friendRepository.getIncomingRequests().observe(this, (result) -> {
         if (result instanceof AuthRepository.Result.Success) {
            List<Friend> list = (List)((AuthRepository.Result.Success)result).data;
            counts[1] = list != null ? list.size() : 0;
         }
         done.run();
      });
      this.friendRepository.getSentRequests().observe(this, (result) -> {
         if (result instanceof AuthRepository.Result.Success) {
            List<Friend> list = (List)((AuthRepository.Result.Success)result).data;
            counts[2] = list != null ? list.size() : 0;
         }
         done.run();
      });
      this.friendRepository.getSuggestions().observe(this, (result) -> {
         if (result instanceof AuthRepository.Result.Success) {
            List<Friend> list = (List)((AuthRepository.Result.Success)result).data;
            counts[3] = list != null ? list.size() : 0;
         }
         done.run();
      });
   }

   private void updateFriendTabLabels(int friendsCount, int incomingCount, int sentCount, int suggestionCount) {
      if (this.friendsFilterGroup == null) {
         return;
      }
      android.widget.RadioButton rbFriends = this.findViewById(id.rb_friends);
      android.widget.RadioButton rbIncoming = this.findViewById(id.rb_incoming);
      android.widget.RadioButton rbSent = this.findViewById(id.rb_sent);
      android.widget.RadioButton rbSuggestions = this.findViewById(id.rb_suggestions);
      if (rbFriends != null) rbFriends.setText("B\u1EA1n b\u00E8 (" + friendsCount + ")");
      if (rbIncoming != null) rbIncoming.setText("L\u1EDDi m\u1EDDi (" + incomingCount + ")");
      if (rbSent != null) rbSent.setText("\u0110\u00E3 g\u1EEDi (" + sentCount + ")");
      if (rbSuggestions != null) rbSuggestions.setText("G\u1EE3i \u00FD (" + suggestionCount + ")");
   }

   private void showFriendActionsDialog(Friend friend) {
      if (friend == null) {
         return;
      }
      LinearLayout layout = new LinearLayout(this);
      layout.setOrientation(1);
      layout.setPadding(24, 18, 24, 18);
      Button btnMessage = new Button(this);
      btnMessage.setText("Nh\u1EAFn tin");
      Button btnUnfriend = new Button(this);
      btnUnfriend.setText("H\u1EE7y k\u1EBFt b\u1EA1n");
      Button btnBlock = new Button(this);
      btnBlock.setText("Ch\u1EB7n");
      layout.addView(btnMessage);
      layout.addView(btnUnfriend);
      layout.addView(btnBlock);
      AlertDialog dialog = new AlertDialog.Builder(this).setView(layout).create();
      btnMessage.setOnClickListener((v) -> { dialog.dismiss(); this.messageFriend(friend); });
      btnUnfriend.setOnClickListener((v) -> {
         dialog.dismiss();
         if (friend.getFriendshipId() != null) {
            this.friendRepository.remove(friend.getFriendshipId()).observe(this, (result) -> {
               if (result instanceof AuthRepository.Result.Success) {
                  Toast.makeText(this, "Tai anh dai dien thanh cong", 0).show();
                  this.loadFriends(this.friendMode);
                  this.refreshFriendTabCounts();
               } else if (result instanceof AuthRepository.Result.Error) {
                  this.handleError(((AuthRepository.Result.Error)result).message);
               }
            });
         }
      });
      btnBlock.setOnClickListener((v) -> {
         dialog.dismiss();
         if (friend.getFriendshipId() != null) {
            this.friendRepository.block(friend.getFriendshipId()).observe(this, (result) -> {
               if (result instanceof AuthRepository.Result.Success) {
                  Toast.makeText(this, "Tai anh dai dien thanh cong", 0).show();
                  this.loadFriends(this.friendMode);
                  this.refreshFriendTabCounts();
               } else if (result instanceof AuthRepository.Result.Error) {
                  this.handleError(((AuthRepository.Result.Error)result).message);
               }
            });
         }
      });
      dialog.show();
   }
   private void openSearch() {
      this.startActivity(new Intent(this, SearchUserActivity.class));
   }

   private void openCreateGroup() {
      this.startActivity(new Intent(this, CreateGroupActivity.class));
   }

   private void openEditProfileActivity() {
      Intent intent = new Intent(this, com.yourapp.ui.profile.EditProfileActivity.class);
      this.editProfileLauncher.launch(intent);
   }

   private void showEditProfileDialog() {
      if (this.currentUserInfo == null) {
         return;
      }

      LinearLayout layout = new LinearLayout(this);
      layout.setOrientation(1);
      layout.setPadding(40, 24, 40, 24);

      EditText etName = new EditText(this);
      etName.setHint("Thong bao");
      etName.setText(this.currentUserInfo.getDisplayName() != null ? this.currentUserInfo.getDisplayName() : "");
      etName.setPadding(20, 24, 20, 24);
      etName.setBackgroundResource(drawable.bg_message_input);
      layout.addView(etName);

      Button btnChangeAvatar = new Button(this);
      btnChangeAvatar.setText("Thong bao");
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
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

      new AlertDialog.Builder(this)
         .setTitle("Thong bao")
         .setView(layout)
         .setNegativeButton("Thong bao", (d, which) -> d.dismiss())
         .setPositiveButton("Thong bao", (d, which) -> {
            String newName = etName.getText() != null ? etName.getText().toString().trim() : "";
            if (newName.isEmpty()) {
               newName = null;
            }

            this.userRepository.updateProfile(newName, (String)null).observe(this, (res) -> {
               if (res instanceof AuthRepository.Result.Success) {
                  this.loadProfile();
               } else if (res instanceof AuthRepository.Result.Error) {
                  this.handleError(((AuthRepository.Result.Error)res).message);
               }
            });
         })
         .show();
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
      Toast.makeText(this, "Tai anh dai dien thanh cong", 0).show();
      this.mediaApiService.upload(part).enqueue(new Callback<ApiResponse<MediaUpload>>() {
         public void onResponse(Call<ApiResponse<MediaUpload>> call, Response<ApiResponse<MediaUpload>> response) {
            HomeActivity.this.progressBar.setVisibility(8);
            if (response.isSuccessful() && response.body() != null && ((ApiResponse)response.body()).isSuccess() && ((ApiResponse)response.body()).getData() != null) {
               String url = ((MediaUpload)((ApiResponse)response.body()).getData()).getUrl();
               HomeActivity.this.userRepository.updateProfile((String)null, url).observe(HomeActivity.this, (res) -> {
                  if (res instanceof AuthRepository.Result.Success) {
                     Toast.makeText(HomeActivity.this, "Thong bao", 0).show();
                     HomeActivity.this.loadProfile();
                  } else {
                     Toast.makeText(HomeActivity.this, "Thong bao", 0).show();
                  }

               });
            } else {
               Toast.makeText(HomeActivity.this, "Thong bao", 0).show();
            }

         }

         public void onFailure(Call<ApiResponse<MediaUpload>> call, Throwable t) {
            HomeActivity.this.progressBar.setVisibility(8);
            Toast.makeText(HomeActivity.this, "Thong bao", 0).show();
         }
      });
   }

   private void uploadAndCreateStory(Uri uri) {
      this.progressBar.setVisibility(0);
      String mimeType = this.getContentResolver().getType(uri);
      ContentUriRequestBody body = new ContentUriRequestBody(this.getContentResolver(), uri, mimeType);
      String filename = "story_" + System.currentTimeMillis() + this.defaultExtension("IMAGE", mimeType);
      MultipartBody.Part part = Part.createFormData("file", filename, body);
      Toast.makeText(this, "Tai anh dai dien thanh cong", 0).show();
      this.mediaApiService.upload(part).enqueue(new Callback<ApiResponse<MediaUpload>>() {
         public void onResponse(Call<ApiResponse<MediaUpload>> call, Response<ApiResponse<MediaUpload>> response) {
            HomeActivity.this.progressBar.setVisibility(8);
            if (response.isSuccessful() && response.body() != null && ((ApiResponse)response.body()).isSuccess() && ((ApiResponse)response.body()).getData() != null) {
               String url = ((MediaUpload)((ApiResponse)response.body()).getData()).getUrl();
               Map<String, String> storyBody = new HashMap<>();
               storyBody.put("mediaUrl", url);
               if (HomeActivity.this.selectedStoryMusic != null) {
                  storyBody.put("musicTitle", HomeActivity.this.selectedStoryMusic.getTitle());
                  storyBody.put("musicArtist", HomeActivity.this.selectedStoryMusic.getArtist());
                  storyBody.put("musicPreviewUrl", HomeActivity.this.selectedStoryMusic.getPreviewUrl());
                  storyBody.put("musicAlbumCover", HomeActivity.this.selectedStoryMusic.getAlbumCover());
                  storyBody.put("musicDeezerTrackId", HomeActivity.this.selectedStoryMusic.getDeezerTrackId());
               }
               HomeActivity.this.storyApiService.createStory(storyBody).enqueue(new Callback<ApiResponse<Story>>() {
                  public void onResponse(Call<ApiResponse<Story>> call, Response<ApiResponse<Story>> storyRes) {
                     if (storyRes.isSuccessful() && storyRes.body() != null && ((ApiResponse)storyRes.body()).isSuccess()) {
                        Toast.makeText(HomeActivity.this, "Thong bao", 0).show();
                        HomeActivity.this.selectedStoryMusic = null;
                        HomeActivity.this.loadStories();
                     } else {
                        Toast.makeText(HomeActivity.this, "Thong bao", 0).show();
                     }

                  }

                  public void onFailure(Call<ApiResponse<Story>> call, Throwable t) {
                     Toast.makeText(HomeActivity.this, "Thong bao", 0).show();
                  }
               });
            } else {
               Toast.makeText(HomeActivity.this, "Thong bao", 0).show();
            }

         }

         public void onFailure(Call<ApiResponse<MediaUpload>> call, Throwable t) {
            HomeActivity.this.progressBar.setVisibility(8);
            Toast.makeText(HomeActivity.this, "Thong bao", 0).show();
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
         this.storyApiService.getStories().enqueue(new Callback<ApiResponse<List<StoryGroup>>>() {
            public void onResponse(Call<ApiResponse<List<StoryGroup>>> call, Response<ApiResponse<List<StoryGroup>>> response) {
               if (response.isSuccessful() && response.body() != null && ((ApiResponse)response.body()).isSuccess() && ((ApiResponse)response.body()).getData() != null) {
                  HomeActivity.this.storyAdapter.setStoryGroups((List)((ApiResponse)response.body()).getData(), HomeActivity.this.currentUserInfo);
               }

            }

            public void onFailure(Call<ApiResponse<List<StoryGroup>>> call, Throwable t) {
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
                  HomeActivity.this.cachedFeedPosts.clear();
                  HomeActivity.this.cachedFeedPosts.addAll(list);
                  HomeActivity.this.postAdapter.setPosts(list);
                  HomeActivity.this.refreshProfilePosts();
                  boolean empty = list.isEmpty();
                  HomeActivity.this.rvFeed.setVisibility(empty ? 8 : 0);
                  HomeActivity.this.feedEmptyState.setVisibility(empty ? 0 : 8);
               }

            }

            public void onFailure(Call<ApiResponse<List<Post>>> call, Throwable t) {
               HomeActivity.this.progressBar.setVisibility(8);
               Toast.makeText(HomeActivity.this, "Khong tai duoc bang tin", 0).show();
            }
         });
      }
   }

   private void refreshProfilePosts() {
      if (this.profilePostAdapter == null || this.rvProfilePosts == null || this.profilePostsEmptyState == null) {
         return;
      }
      List<Post> mine = new ArrayList<>();
      Long currentUserId = this.currentUserInfo != null ? this.currentUserInfo.getId() : null;
      if (currentUserId != null) {
         for (Post post : this.cachedFeedPosts) {
            if (post != null && currentUserId.equals(post.getAuthorId())) {
               mine.add(post);
            }
         }
      }
      this.profilePostAdapter.setPosts(mine);
      boolean empty = mine.isEmpty();
      this.rvProfilePosts.setVisibility(empty ? 8 : 0);
      this.profilePostsEmptyState.setVisibility(empty ? 0 : 8);
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
               Toast.makeText(HomeActivity.this, "Thong bao", 0).show();
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
            tvReplying.setText("Thong bao" + name + "Thong bao");
            etCommentInput.setHint("Thong bao" + name + "...");
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
               Toast.makeText(HomeActivity.this, "Thong bao", 0).show();
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
                        Toast.makeText(HomeActivity.this, "Thong bao", 0).show();
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
                        Toast.makeText(HomeActivity.this, "Thong bao", 0).show();
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
      etCommentInput.setHint("Thong bao");
      tvReplying.setVisibility(8);
      tvReplying.setText("");
   }

   private void showCommentReactionPicker(Post post, Comment comment, CommentAdapter commentAdapter) {
      if (this.postApiService != null && comment != null && comment.getId() != null) {
         String[] reactions = new String[]{"Thong bao", "\ud83d\udc4d", "\ud83d\ude02", "\ud83d\ude2e", "\ud83d\ude22", "\ud83d\ude21", "Thong bao"};
         (new AlertDialog.Builder(this)).setTitle("Thong bao").setItems(reactions, (dialog, which) -> {
            Map<String, String> body = new HashMap();
            body.put("reaction", which == reactions.length - 1 ? "" : reactions[which]);
            this.postApiService.reactToComment(post.getId(), comment.getId(), body).enqueue(new Callback<ApiResponse<Comment>>() {
               public void onResponse(Call<ApiResponse<Comment>> call, Response<ApiResponse<Comment>> response) {
                  if (response.isSuccessful() && response.body() != null && ((ApiResponse)response.body()).isSuccess() && ((ApiResponse)response.body()).getData() != null) {
                     commentAdapter.updateComment((Comment)((ApiResponse)response.body()).getData());
                  }

               }

               public void onFailure(Call<ApiResponse<Comment>> call, Throwable t) {
                  Toast.makeText(HomeActivity.this, "Thong bao", 0).show();
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
            Toast.makeText(this, "Tai anh dai dien thanh cong", 0).show();
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
         Toast.makeText(this, "Tai anh dai dien thanh cong", 0).show();
         this.mediaApiService.upload(part).enqueue(new Callback<ApiResponse<MediaUpload>>() {
            public void onResponse(Call<ApiResponse<MediaUpload>> call, Response<ApiResponse<MediaUpload>> response) {
               HomeActivity.this.progressBar.setVisibility(8);
               if (response.isSuccessful() && response.body() != null && ((ApiResponse)response.body()).isSuccess() && ((ApiResponse)response.body()).getData() != null) {
                  String url = ((MediaUpload)((ApiResponse)response.body()).getData()).getUrl();
                  HomeActivity.this.createPostRequest(content, url);
               } else {
                  Toast.makeText(HomeActivity.this, "Thong bao", 0).show();
               }

            }

            public void onFailure(Call<ApiResponse<MediaUpload>> call, Throwable t) {
               HomeActivity.this.progressBar.setVisibility(8);
               Toast.makeText(HomeActivity.this, "Thong bao", 0).show();
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
                  Toast.makeText(HomeActivity.this, "Thong bao", 0).show();
                  HomeActivity.this.loadFeed();
               } else {
                  Toast.makeText(HomeActivity.this, "Thong bao", 0).show();
               }

            }

            public void onFailure(Call<ApiResponse<Post>> call, Throwable t) {
               HomeActivity.this.progressBar.setVisibility(8);
               Toast.makeText(HomeActivity.this, "Thong bao", 0).show();
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

    private void showStoryComposer() {
        this.selectedStoryMusic = null;
        View view = getLayoutInflater().inflate(R.layout.dialog_story_composer, null);
        View layoutAddMusic = view.findViewById(R.id.layout_story_add_music);
        TextView btnAddMusic = view.findViewById(R.id.btn_story_add_music);
        TextView btnRemoveMusicPill = view.findViewById(R.id.btn_story_remove_music_pill);
        View musicPreview = view.findViewById(R.id.story_music_preview);
        ImageView cover = view.findViewById(R.id.iv_story_music_cover);
        TextView title = view.findViewById(R.id.tv_story_music_title);
        TextView artist = view.findViewById(R.id.tv_story_music_artist);
        TextView btnPlayPreview = view.findViewById(R.id.btn_story_play_preview);
        TextView remove = view.findViewById(R.id.btn_story_remove_music);
        TextView chooseImage = view.findViewById(R.id.btn_story_choose_image);

        AlertDialog dialog = new AlertDialog.Builder(this).setView(view).create();

        Runnable renderMusic = new Runnable() {
            @Override
            public void run() {
                if (HomeActivity.this.selectedStoryMusic == null) {
                    musicPreview.setVisibility(View.GONE);
                    btnRemoveMusicPill.setVisibility(View.GONE);
                    btnAddMusic.setText("Thong bao");
                    HomeActivity.this.stopDialogPreview(btnPlayPreview);
                } else {
                    musicPreview.setVisibility(View.VISIBLE);
                    btnRemoveMusicPill.setVisibility(View.VISIBLE);
                    btnAddMusic.setText(HomeActivity.this.selectedStoryMusic.getTitle());
                    title.setText(HomeActivity.this.selectedStoryMusic.getTitle());
                    title.setSelected(true);
                    artist.setText(HomeActivity.this.selectedStoryMusic.getArtist());
                    Glide.with(HomeActivity.this).load(HomeActivity.this.selectedStoryMusic.getAlbumCover()).centerCrop().into(cover);
                    btnPlayPreview.setText("Thong bao");
                }
            }
        };

        layoutAddMusic.setOnClickListener(v -> {
            MusicPickerBottomSheet sheet = new MusicPickerBottomSheet(track -> {
                HomeActivity.this.selectedStoryMusic = track;
                renderMusic.run();
            });
            sheet.show(getSupportFragmentManager(), "music_picker");
        });

        btnRemoveMusicPill.setOnClickListener(v -> {
            HomeActivity.this.selectedStoryMusic = null;
            renderMusic.run();
        });

        remove.setOnClickListener(v -> {
            HomeActivity.this.selectedStoryMusic = null;
            renderMusic.run();
        });

        btnPlayPreview.setOnClickListener(v -> {
            HomeActivity.this.toggleDialogPreview(HomeActivity.this.selectedStoryMusic, btnPlayPreview);
        });

        chooseImage.setOnClickListener(v -> {
            dialog.dismiss();
            HomeActivity.this.pickStoryImage();
        });

        dialog.setOnDismissListener(d -> {
            HomeActivity.this.stopDialogPreview(null);
        });

        dialog.show();
    }

    private void stopDialogPreview(TextView btnPlay) {
        if (dialogMusicPlayer != null) {
            try { dialogMusicPlayer.stop(); } catch (Exception ignored) {}
            dialogMusicPlayer.release();
            dialogMusicPlayer = null;
        }
        if (btnPlay != null) {
            btnPlay.setText("Thong bao");
        }
    }

    private void toggleDialogPreview(MusicTrack track, TextView btnPlay) {
        if (dialogMusicPlayer != null && dialogMusicPlayer.isPlaying()) {
            stopDialogPreview(btnPlay);
            return;
        }
        stopDialogPreview(btnPlay);
        if (track == null) return;
        try {
            dialogMusicPlayer = new MediaPlayer();
            dialogMusicPlayer.setAudioAttributes(new android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            dialogMusicPlayer.setDataSource(track.getPreviewUrl());
            dialogMusicPlayer.setOnPreparedListener(mp -> {
                mp.start();
                if (btnPlay != null) btnPlay.setText("Thong bao");
            });
            dialogMusicPlayer.setOnCompletionListener(mp -> stopDialogPreview(btnPlay));
            dialogMusicPlayer.setOnErrorListener((mp, what, extra) -> {
                stopDialogPreview(btnPlay);
                return true;
            });
            dialogMusicPlayer.prepareAsync();
        } catch (Exception e) {
            stopDialogPreview(btnPlay);
            Toast.makeText(this, "Thong bao", Toast.LENGTH_SHORT).show();
        }
    }

}

























