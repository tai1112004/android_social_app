package com.yourapp.ui.auth;

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
import com.yourapp.data.repository.AuthRepository;
import com.yourapp.network.RetrofitClient;
import com.yourapp.util.TokenManager;

/**
 * RegisterActivity handles new user registration.
 * On successful registration, navigates back to LoginActivity.
 */
public class RegisterActivity extends AppCompatActivity {
    private EditText usernameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private Button registerButton;
    private Button loginButton;
    private ProgressBar progressBar;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize views
        usernameInput = findViewById(R.id.register_username);
        emailInput = findViewById(R.id.register_email);
        passwordInput = findViewById(R.id.register_password);
        registerButton = findViewById(R.id.register_button);
        loginButton = findViewById(R.id.register_login_link);
        progressBar = findViewById(R.id.register_progress);

        // Initialize TokenManager and ViewModel
        TokenManager tokenManager = new TokenManager(this);
        AuthApiService authApiService = RetrofitClient.getInstance(tokenManager)
            .create(AuthApiService.class);
        AuthRepository authRepository = new AuthRepository(authApiService, tokenManager);

        viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @Override
            public <T extends androidx.lifecycle.ViewModel> T create(Class<T> modelClass) {
                return (T) new AuthViewModel(authRepository);
            }
        }).get(AuthViewModel.class);

        // Setup click listeners
        registerButton.setOnClickListener(v -> performRegister());
        loginButton.setOnClickListener(v -> navigateToLogin());

        // Observe register state
        viewModel.getRegisterState().observe(this, state -> {
            if (state instanceof AuthViewModel.AuthUiState.Success) {
                Toast.makeText(RegisterActivity.this, "Đăng ký thành công! Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show();
                navigateToLogin();
            } else if (state instanceof AuthViewModel.AuthUiState.Error) {
                AuthViewModel.AuthUiState.Error error = (AuthViewModel.AuthUiState.Error) state;
                Toast.makeText(RegisterActivity.this, "Đăng ký thất bại: " + error.message, Toast.LENGTH_SHORT).show();
            }
        });

        // Observe loading state
        viewModel.getLoadingState().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            registerButton.setEnabled(!isLoading);
            loginButton.setEnabled(!isLoading);
        });

        // Observe error messages
        viewModel.getErrorMessage().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performRegister() {
        String username = usernameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!email.contains("@")) {
            Toast.makeText(this, "Vui lòng nhập email hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.register(username, email, password);
    }

    private void navigateToLogin() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
