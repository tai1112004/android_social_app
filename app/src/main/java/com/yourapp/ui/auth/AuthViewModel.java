package com.yourapp.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.yourapp.data.remote.AuthApiService;
import com.yourapp.data.repository.AuthRepository;
import com.yourapp.network.RetrofitClient;
import com.yourapp.util.TokenManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AuthViewModel manages authentication state and delegates to AuthRepository.
 * Exposes LiveData for Activity observation.
 */
public class AuthViewModel extends ViewModel {
    private final AuthRepository authRepository;
    private final ExecutorService executorService;

    private MutableLiveData<AuthUiState> loginState;
    private MutableLiveData<AuthUiState> registerState;
    private MutableLiveData<Boolean> loadingState;
    private MutableLiveData<String> errorMessage;

    public AuthViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
        this.executorService = Executors.newSingleThreadExecutor();
        this.loginState = new MutableLiveData<>();
        this.registerState = new MutableLiveData<>();
        this.loadingState = new MutableLiveData<>(false);
        this.errorMessage = new MutableLiveData<>();
    }

    /**
     * Perform login
     */
    public void login(String username, String password) {
        if (username == null || username.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            errorMessage.setValue("Vui lòng nhập tên đăng nhập và mật khẩu");
            return;
        }

        loadingState.setValue(true);
        authRepository.login(username, password).observeForever(result -> {
            loadingState.postValue(false);
            if (result instanceof AuthRepository.Result.Success) {
                AuthRepository.Result.Success<Long> success = (AuthRepository.Result.Success<Long>) result;
                loginState.postValue(new AuthUiState.Success(success.data));
            } else if (result instanceof AuthRepository.Result.Error) {
                AuthRepository.Result.Error<Long> error = (AuthRepository.Result.Error<Long>) result;
                errorMessage.postValue(error.message);
                loginState.postValue(new AuthUiState.Error(error.message));
            }
        });
    }

    /**
     * Perform registration
     */
    public void register(String username, String email, String password) {
        if (username == null || username.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            errorMessage.setValue("Vui lòng nhập đầy đủ thông tin");
            return;
        }

        loadingState.setValue(true);
        authRepository.register(username, email, password).observeForever(result -> {
            loadingState.postValue(false);
            if (result instanceof AuthRepository.Result.Success) {
                AuthRepository.Result.Success<Long> success = (AuthRepository.Result.Success<Long>) result;
                registerState.postValue(new AuthUiState.Success(success.data));
            } else if (result instanceof AuthRepository.Result.Error) {
                AuthRepository.Result.Error<Long> error = (AuthRepository.Result.Error<Long>) result;
                errorMessage.postValue(error.message);
                registerState.postValue(new AuthUiState.Error(error.message));
            }
        });
    }

    public LiveData<AuthUiState> getLoginState() {
        return loginState;
    }

    public LiveData<AuthUiState> getRegisterState() {
        return registerState;
    }

    public LiveData<Boolean> getLoadingState() {
        return loadingState;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
        authRepository.shutdown();
    }

    /**
     * AuthUiState sealed class for handling UI state
     */
    public static class AuthUiState {
        public static class Success extends AuthUiState {
            public final Long userId;

            public Success(Long userId) {
                this.userId = userId;
            }
        }

        public static class Error extends AuthUiState {
            public final String message;

            public Error(String message) {
                this.message = message;
            }
        }

        public static class Loading extends AuthUiState {
        }
    }
}
