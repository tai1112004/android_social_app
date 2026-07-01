package com.yourapp.ui.auth;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.yourapp.R;
import com.yourapp.data.remote.AuthApiService;
import com.yourapp.data.remote.HealthApiService;
import com.yourapp.data.repository.AuthRepository;
import com.yourapp.network.RetrofitClient;
import com.yourapp.ui.home.HomeActivity;
import com.yourapp.util.BackendConfig;
import com.yourapp.util.TokenManager;

public class LoginActivity extends AppCompatActivity {
    private EditText usernameInput;
    private EditText passwordInput;
    private Button loginButton;
    private Button registerButton;
    private Button backendModeButton;
    private ProgressBar progressBar;
    private AuthViewModel viewModel;
    private TokenManager tokenManager;
    private boolean authLayerReady;
    private boolean connectionCheckRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameInput = findViewById(R.id.login_username);
        passwordInput = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);
        backendModeButton = findViewById(R.id.login_usb_button);
        registerButton = findViewById(R.id.login_register_link);
        progressBar = findViewById(R.id.login_progress);

        tokenManager = new TokenManager(this);
        loginButton.setEnabled(false);
        updateBackendModeButton();

        loginButton.setOnClickListener(v -> performLogin());
        registerButton.setOnClickListener(v -> navigateToRegister());
        backendModeButton.setOnClickListener(v -> showConnectionModeDialog(true));

        verifyBackendAndInitialize();
    }

    private void verifyBackendAndInitialize() {
        if (authLayerReady || connectionCheckRunning) {
            return;
        }

        RetrofitClient.reset();
        connectionCheckRunning = true;
        loginButton.setEnabled(false);
        backendModeButton.setEnabled(false);
        registerButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        HealthApiService healthApiService = RetrofitClient.getInstance(tokenManager)
                .create(HealthApiService.class);
        healthApiService.health().enqueue(new retrofit2.Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.Map<String, Object>> call,
                                   retrofit2.Response<java.util.Map<String, Object>> response) {
                connectionCheckRunning = false;
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    initializeAuthLayer();
                } else {
                    handleBackendUnreachable();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.Map<String, Object>> call, Throwable t) {
                connectionCheckRunning = false;
                progressBar.setVisibility(View.GONE);
                handleBackendUnreachable();
            }
        });
    }

    private void initializeAuthLayer() {
        if (authLayerReady) {
            return;
        }
        authLayerReady = true;

        AuthApiService authApiService = RetrofitClient.getInstance(tokenManager)
                .create(AuthApiService.class);
        AuthRepository authRepository = new AuthRepository(authApiService, tokenManager);

        viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @Override
            public <T extends androidx.lifecycle.ViewModel> T create(Class<T> modelClass) {
                return (T) new AuthViewModel(authRepository);
            }
        }).get(AuthViewModel.class);

        observeAuthState();
        loginButton.setEnabled(true);
        registerButton.setEnabled(true);
        backendModeButton.setEnabled(true);
        updateBackendModeButton();
    }

    private void observeAuthState() {
        viewModel.getLoginState().observe(this, state -> {
            if (state instanceof AuthViewModel.AuthUiState.Success) {
                Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                navigateToMain();
            } else if (state instanceof AuthViewModel.AuthUiState.Error) {
                AuthViewModel.AuthUiState.Error error = (AuthViewModel.AuthUiState.Error) state;
                Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: " + error.message, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getLoadingState().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            loginButton.setEnabled(!isLoading && authLayerReady);
            registerButton.setEnabled(!isLoading);
            backendModeButton.setEnabled(!isLoading);
        });

        viewModel.getErrorMessage().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performLogin() {
        if (!authLayerReady || viewModel == null) {
            Toast.makeText(this, "Đang kiểm tra kết nối, vui lòng chờ", Toast.LENGTH_SHORT).show();
            verifyBackendAndInitialize();
            return;
        }

        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên đăng nhập và mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.login(username, password);
    }

    private void showConnectionModeDialog(boolean fromButton) {
        String[] labels = new String[] {
                "Deploy - " + BackendConfig.getDeployBaseUrl(),
                "Local USB - 127.0.0.1:8082"
        };
        int checkedItem = BackendConfig.isDeployMode() ? 0 : 1;

        new AlertDialog.Builder(this)
                .setTitle("Chọn backend")
                .setSingleChoiceItems(labels, checkedItem, (dialog, which) -> {
                    BackendConfig.Mode selectedMode = which == 0
                            ? BackendConfig.Mode.DEPLOY
                            : BackendConfig.Mode.LOCAL;
                    applyBackendMode(selectedMode);
                    dialog.dismiss();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void applyBackendMode(BackendConfig.Mode mode) {
        if (BackendConfig.getMode() == mode && authLayerReady) {
            updateBackendModeButton();
            return;
        }
        BackendConfig.setMode(mode);
        if (mode == BackendConfig.Mode.LOCAL) {
            BackendConfig.clearOverride();
        }
        authLayerReady = false;
        viewModel = null;
        RetrofitClient.reset();
        updateBackendModeButton();
        Toast.makeText(this, "Đang dùng backend: " + BackendConfig.getDisplayName(), Toast.LENGTH_SHORT).show();
        verifyBackendAndInitialize();
    }

    private void updateBackendModeButton() {
        if (backendModeButton != null) {
            backendModeButton.setText("Backend: " + BackendConfig.getDisplayName());
        }
    }

    private void showLanHostDialog(String defaultHost) {
        showConnectionModeDialog(true);
    }

    private void showUsbLostDialog(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void handleBackendUnreachable() {
        Toast.makeText(this, "Không kết nối được backend: " + BackendConfig.getDisplayName(), Toast.LENGTH_SHORT).show();
        authLayerReady = false;
        loginButton.setEnabled(false);
        registerButton.setEnabled(true);
        backendModeButton.setEnabled(true);
        updateBackendModeButton();
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToRegister() {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }
}