package com.yourapp.data.repository;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.yourapp.data.remote.AuthApiService;
import com.yourapp.model.ApiResponse;
import com.yourapp.model.AuthResponse;
import com.yourapp.model.LoginRequest;
import com.yourapp.model.RegisterRequest;
import com.yourapp.model.RefreshRequest;
import com.yourapp.util.TokenManager;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.google.gson.Gson;

/**
 * AuthRepository handles all authentication API calls and token management.
 * Uses LiveData to communicate results to ViewModels on the main thread.
 */
public class AuthRepository {
    private final AuthApiService authApiService;
    private final TokenManager tokenManager;
    private final ExecutorService executorService;

    public AuthRepository(AuthApiService authApiService, TokenManager tokenManager) {
        this.authApiService = authApiService;
        this.tokenManager = tokenManager;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    private String parseErrorBody(Response<?> response, String fallbackMessage) {
        try {
            if (response.errorBody() != null) {
                String errorBodyStr = response.errorBody().string();
                Gson gson = new Gson();
                ApiResponse<?> apiResponse = gson.fromJson(errorBodyStr, ApiResponse.class);
                if (apiResponse != null) {
                    if (apiResponse.getError() != null && !apiResponse.getError().isEmpty()) {
                        return apiResponse.getError();
                    } else if (apiResponse.getMessage() != null && !apiResponse.getMessage().isEmpty()) {
                        return apiResponse.getMessage();
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return fallbackMessage;
    }

    /**
     * Register a new user.
     * On success: returns LiveData with userId
     * On failure: returns LiveData with error message
     */
    public LiveData<Result<Long>> register(String username, String email, String password) {
        MutableLiveData<Result<Long>> resultLiveData = new MutableLiveData<>();

        RegisterRequest request = new RegisterRequest(username, email, password);
        authApiService.register(request).enqueue(new Callback<ApiResponse<HashMap<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<HashMap<String, Object>>> call,
                                   Response<ApiResponse<HashMap<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<HashMap<String, Object>> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        Long userId = ((Number) apiResponse.getData().get("userId")).longValue();
                        resultLiveData.postValue(new Result.Success<>(userId));
                    } else {
                        String errorMsg = apiResponse.getError() != null ?
                            apiResponse.getError() : apiResponse.getMessage();
                        resultLiveData.postValue(new Result.Error(errorMsg));
                    }
                } else {
                    resultLiveData.postValue(new Result.Error(parseErrorBody(response, "Đăng ký thất bại")));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<HashMap<String, Object>>> call, Throwable t) {
                resultLiveData.postValue(new Result.Error(t.getMessage() != null ?
                    t.getMessage() : "Lỗi kết nối mạng"));
            }
        });

        return resultLiveData;
    }

    /**
     * Login with username and password.
     * On success: saves tokens and returns userId
     * On failure: returns error message
     */
    public LiveData<Result<Long>> login(String username, String password) {
        MutableLiveData<Result<Long>> resultLiveData = new MutableLiveData<>();

        LoginRequest request = new LoginRequest(username, password);
        authApiService.login(request).enqueue(new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthResponse>> call,
                                   Response<ApiResponse<AuthResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<AuthResponse> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        AuthResponse authResponse = apiResponse.getData();
                        // Save tokens
                        tokenManager.saveAccessToken(authResponse.getAccessToken());
                        tokenManager.saveRefreshToken(authResponse.getRefreshToken());
                        tokenManager.saveUsername(username);

                        // Parse user ID from token (or could be returned from backend)
                        // For now, just return a placeholder
                        resultLiveData.postValue(new Result.Success<>(1L));
                    } else {
                        String errorMsg = apiResponse.getError() != null ?
                            apiResponse.getError() : apiResponse.getMessage();
                        resultLiveData.postValue(new Result.Error(errorMsg));
                    }
                } else {
                    resultLiveData.postValue(new Result.Error(parseErrorBody(response, "Đăng nhập thất bại")));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                resultLiveData.postValue(new Result.Error(t.getMessage() != null ?
                    t.getMessage() : "Lỗi kết nối mạng"));
            }
        });

        return resultLiveData;
    }

    /**
     * Refresh access token using refresh token.
     * On success: saves new access token
     * On failure: returns error message
     */
    public LiveData<Result<String>> refresh() {
        MutableLiveData<Result<String>> resultLiveData = new MutableLiveData<>();

        String refreshToken = tokenManager.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            resultLiveData.postValue(new Result.Error("Không có refresh token"));
            return resultLiveData;
        }

        RefreshRequest request = new RefreshRequest(refreshToken);
        authApiService.refresh(request).enqueue(new Callback<ApiResponse<HashMap<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<HashMap<String, Object>>> call,
                                   Response<ApiResponse<HashMap<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<HashMap<String, Object>> apiResponse = response.body();
                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        String newAccessToken = (String) apiResponse.getData().get("accessToken");
                        if (newAccessToken != null) {
                            tokenManager.saveAccessToken(newAccessToken);
                            resultLiveData.postValue(new Result.Success<>(newAccessToken));
                        } else {
                            resultLiveData.postValue(new Result.Error("Không nhận được token"));
                        }
                    } else {
                        String errorMsg = apiResponse.getError() != null ?
                            apiResponse.getError() : apiResponse.getMessage();
                        resultLiveData.postValue(new Result.Error(errorMsg));
                    }
                } else {
                    resultLiveData.postValue(new Result.Error(parseErrorBody(response, "Làm mới token thất bại")));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<HashMap<String, Object>>> call, Throwable t) {
                resultLiveData.postValue(new Result.Error(t.getMessage() != null ?
                    t.getMessage() : "Lỗi kết nối mạng"));
            }
        });

        return resultLiveData;
    }

    /**
     * Logout: clear all tokens
     */
    public void logout() {
        tokenManager.clearAll();
    }

    /**
     * Shutdown executor service
     */
    public void shutdown() {
        executorService.shutdown();
    }

    /**
     * Result wrapper for success/error handling
     */
    public static class Result<T> {
        public static class Success<T> extends Result<T> {
            public final T data;

            public Success(T data) {
                this.data = data;
            }
        }

        public static class Error<T> extends Result<T> {
            public final String message;

            public Error(String message) {
                this.message = message;
            }
        }
    }
}
