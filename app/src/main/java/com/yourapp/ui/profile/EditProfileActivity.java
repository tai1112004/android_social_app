package com.yourapp.ui.profile;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.yourapp.R;
import com.yourapp.data.remote.MediaApiService;
import com.yourapp.data.remote.UserApiService;
import com.yourapp.data.repository.AuthRepository;
import com.yourapp.data.repository.UserRepository;
import com.yourapp.model.ApiResponse;
import com.yourapp.model.MediaUpload;
import com.yourapp.model.User;
import com.yourapp.network.RetrofitClient;
import com.yourapp.ui.auth.LoginActivity;
import com.yourapp.util.AvatarUtil;
import com.yourapp.util.ContentUriRequestBody;
import com.yourapp.util.TokenManager;
import java.util.Calendar;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private static final String PICK_AVATAR = "AVATAR";
    private static final String PICK_COVER = "COVER";

    private UserRepository userRepository;
    private MediaApiService mediaApiService;
    private User currentUser;
    private String pendingPickType;
    private String avatarUrl;
    private String coverUrl;

    private ImageView ivCover;
    private ImageView ivAvatar;
    private TextView tvAvatar;
    private EditText etName;
    private EditText etBio;
    private EditText etDob;
    private EditText etGender;
    private EditText etPhone;
    private EditText etEmail;
    private EditText etLocation;
    private EditText etWebsite;
    private TextView tvBioCounter;
    private ProgressBar progressBar;

    private final ActivityResultLauncher<Intent> imagePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    uploadPickedImage(result.getData().getData(), pendingPickType);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        TokenManager tokenManager = new TokenManager(this);
        if (!tokenManager.hasValidTokens()) {
            redirectToLogin();
            return;
        }

        userRepository = new UserRepository(RetrofitClient.getInstance(tokenManager).create(UserApiService.class));
        mediaApiService = RetrofitClient.getInstance(tokenManager).create(MediaApiService.class);

        bindViews();
        loadProfile();
    }

    private void bindViews() {
        findViewById(R.id.btn_edit_cancel).setOnClickListener(v -> finish());
        findViewById(R.id.btn_edit_save).setOnClickListener(v -> saveProfile());
        findViewById(R.id.btn_edit_avatar).setOnClickListener(v -> pickImage(PICK_AVATAR));
        findViewById(R.id.btn_edit_cover).setOnClickListener(v -> pickImage(PICK_COVER));

        ivCover = findViewById(R.id.iv_edit_cover);
        ivAvatar = findViewById(R.id.iv_edit_avatar);
        tvAvatar = findViewById(R.id.tv_edit_avatar);
        etName = findViewById(R.id.et_edit_name);
        etBio = findViewById(R.id.et_edit_bio);
        etDob = findViewById(R.id.et_edit_dob);
        etGender = findViewById(R.id.et_edit_gender);
        etPhone = findViewById(R.id.et_edit_phone);
        etEmail = findViewById(R.id.et_edit_email);
        etLocation = findViewById(R.id.et_edit_location);
        etWebsite = findViewById(R.id.et_edit_website);
        tvBioCounter = findViewById(R.id.tv_edit_bio_counter);
        progressBar = findViewById(R.id.progress_edit_profile);

        etDob.setOnClickListener(v -> showDatePicker());
        etGender.setOnClickListener(v -> showGenderPicker());
        etBio.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvBioCounter.setText(s.length() + "/150");
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void loadProfile() {
        setLoading(true);
        userRepository.getMe().observe(this, result -> {
            setLoading(false);
            if (result instanceof AuthRepository.Result.Success) {
                currentUser = ((AuthRepository.Result.Success<User>) result).data;
                renderProfile();
            } else if (result instanceof AuthRepository.Result.Error) {
                handleError(((AuthRepository.Result.Error<User>) result).message);
            }
        });
    }

    private void renderProfile() {
        if (currentUser == null) return;
        avatarUrl = currentUser.getAvatarUrl();
        coverUrl = currentUser.getCoverUrl();
        etName.setText(orEmpty(currentUser.getDisplayName()));
        etBio.setText(orEmpty(currentUser.getBio()));
        etDob.setText(orEmpty(currentUser.getDateOfBirth()));
        etGender.setText(orEmpty(currentUser.getGender()));
        etPhone.setText(orEmpty(currentUser.getPhone()));
        etEmail.setText(orEmpty(currentUser.getEmail()));
        etLocation.setText(orEmpty(currentUser.getLocation()));
        etWebsite.setText(orEmpty(currentUser.getWebsite()));
        tvBioCounter.setText(etBio.getText().length() + "/150");

        AvatarUtil.loadAvatar(avatarUrl, currentUser.getDisplayName(), currentUser.getUsername(), ivAvatar, tvAvatar);
        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(this).load(coverUrl).centerCrop().into(ivCover);
        } else {
            Glide.with(this).clear(ivCover);
            ivCover.setBackgroundResource(R.drawable.bg_profile_cover_placeholder);
        }
    }

    private void pickImage(String type) {
        pendingPickType = type;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        imagePicker.launch(intent);
    }

    private void uploadPickedImage(Uri uri, String type) {
        if (uri == null || type == null) return;
        setLoading(true);
        String mimeType = getContentResolver().getType(uri);
        ContentUriRequestBody body = new ContentUriRequestBody(getContentResolver(), uri, mimeType);
        String filename = (PICK_COVER.equals(type) ? "cover_" : "avatar_") + System.currentTimeMillis() + defaultExtension(mimeType);
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", filename, body);
        mediaApiService.upload(part).enqueue(new Callback<ApiResponse<MediaUpload>>() {
            @Override
            public void onResponse(Call<ApiResponse<MediaUpload>> call, Response<ApiResponse<MediaUpload>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess() && response.body().getData() != null) {
                    String url = response.body().getData().getUrl();
                    if (PICK_COVER.equals(type)) {
                        coverUrl = url;
                        Glide.with(EditProfileActivity.this).load(url).centerCrop().into(ivCover);
                    } else {
                        avatarUrl = url;
                        AvatarUtil.loadAvatar(url, etName.getText().toString(), currentUser != null ? currentUser.getUsername() : "U", ivAvatar, tvAvatar);
                    }
                } else {
                    Toast.makeText(EditProfileActivity.this, "Tai anh that bai", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MediaUpload>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(EditProfileActivity.this, "Loi upload anh", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String value = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            etDob.setText(value);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void showGenderPicker() {
        String[] labels = {"Nam", "Nu", "Tuy chinh", "Khong muon tiet lo"};
        String[] values = {"male", "female", "custom", "undisclosed"};
        new AlertDialog.Builder(this)
                .setTitle("Chon gioi tinh")
                .setItems(labels, (dialog, which) -> etGender.setText(values[which]))
                .show();
    }

    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        if (name.isEmpty()) {
            etName.setError("Ten khong duoc de trong");
            return;
        }
        if (!email.isEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email khong hop le");
            return;
        }
        setLoading(true);
        userRepository.updateProfile(
                name,
                avatarUrl,
                coverUrl,
                etBio.getText().toString(),
                etLocation.getText().toString(),
                etWebsite.getText().toString(),
                etDob.getText().toString(),
                etGender.getText().toString(),
                etPhone.getText().toString(),
                email
        ).observe(this, result -> {
            setLoading(false);
            if (result instanceof AuthRepository.Result.Success) {
                Toast.makeText(this, "Da luu ho so", Toast.LENGTH_SHORT).show();
                Intent data = new Intent();
                data.putExtra("displayName", name);
                data.putExtra("avatarUrl", avatarUrl);
                data.putExtra("coverUrl", coverUrl);
                data.putExtra("bio", etBio.getText().toString());
                data.putExtra("location", etLocation.getText().toString());
                data.putExtra("website", etWebsite.getText().toString());
                data.putExtra("email", email);
                setResult(RESULT_OK, data);
                finish();
            } else if (result instanceof AuthRepository.Result.Error) {
                handleError(((AuthRepository.Result.Error<User>) result).message);
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        findViewById(R.id.btn_edit_save).setEnabled(!loading);
    }

    private String defaultExtension(String mimeType) {
        if (mimeType != null) {
            if (mimeType.contains("png")) return ".png";
            if (mimeType.contains("webp")) return ".webp";
        }
        return ".jpg";
    }

    private String orEmpty(String value) {
        return value != null ? value : "";
    }

    private void handleError(String message) {
        if ("UNAUTHORIZED".equals(message)) {
            redirectToLogin();
        } else {
            Toast.makeText(this, message != null ? message : "Co loi xay ra", Toast.LENGTH_SHORT).show();
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}