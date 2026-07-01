package com.yourapp.data.remote;

import com.yourapp.model.ApiResponse;
import com.yourapp.model.AuthResponse;
import com.yourapp.model.LoginRequest;
import com.yourapp.model.RegisterRequest;
import com.yourapp.model.RefreshRequest;
import java.util.HashMap;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Retrofit service interface for authentication endpoints.
 * All endpoints use the ApiResponse wrapper format.
 */
public interface AuthApiService {

    /**
     * Register a new user.
     * POST /api/auth/register
     * Response data: { userId: Long }
     */
    @POST("auth/register")
    Call<ApiResponse<HashMap<String, Object>>> register(@Body RegisterRequest request);

    /**
     * Login with username and password.
     * POST /api/auth/login
     * Response data: AuthResponse { accessToken, refreshToken, tokenType }
     */
    @POST("auth/login")
    Call<ApiResponse<AuthResponse>> login(@Body LoginRequest request);

    /**
     * Refresh access token using refresh token.
     * POST /api/auth/refresh
     * Response data: { accessToken, tokenType }
     */
    @POST("auth/refresh")
    Call<ApiResponse<HashMap<String, Object>>> refresh(@Body RefreshRequest request);
}
