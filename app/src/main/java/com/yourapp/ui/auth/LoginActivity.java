package com.yourapp.ui.auth;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
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
    private Button usbButton;
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
        usbButton = findViewById(R.id.login_usb_button);
        registerButton = findViewById(R.id.login_register_link);
        progressBar = findViewById(R.id.login_progress);

        tokenManager = new TokenManager(this);
        loginButton.setEnabled(false);

        loginButton.setOnClickListener(v -> performLogin());
        registerButton.setOnClickListener(v -> navigateToRegister());
        usbButton.setOnClickListener(v -> verifyBackendAndInitialize());

        verifyBackendAndInitialize();
    }

    private void verifyBackendAndInitialize() {
        if (authLayerReady || connectionCheckRunning) {
            return;
        }

        RetrofitClient.reset();
        connectionCheckRunning = true;
        loginButton.setEnabled(false);
        usbButton.setEnabled(false);
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
        usbButton.setEnabled(true);
    }

    private void observeAuthState() {
        viewModel.getLoginState().observe(this, state -> {
            if (state instanceof AuthViewModel.AuthUiState.Success) {
                Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                navigateToMain();
            } else if (state instanceof AuthViewModel.AuthUiState.Error) {
                AuthViewModel.AuthUiState.Error error = (AuthViewModel.AuthUiState.Error) state;
                if (error.message != null && error.message.contains("Kết nối USB bị mất")) {
                    showUsbLostDialog(error.message);
                } else {
                    Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: " + error.message, Toast.LENGTH_SHORT).show();
                }
            }
        });

        viewModel.getLoadingState().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            loginButton.setEnabled(!isLoading && authLayerReady);
            registerButton.setEnabled(!isLoading);
            usbButton.setEnabled(!isLoading);
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
        verifyBackendAndInitialize();
    }

    private void showLanHostDialog(String defaultHost) {
        verifyBackendAndInitialize();
    }

    private void showUsbLostDialog(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void handleBackendUnreachable() {
        Toast.makeText(this, "Cannot connect to backend", Toast.LENGTH_SHORT).show();
        authLayerReady = false;
        loginButton.setEnabled(false);
        registerButton.setEnabled(true);
        usbButton.setEnabled(true);
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







