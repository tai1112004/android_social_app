package com.yourapp.data.remote;

import com.yourapp.model.ApiResponse;
import com.yourapp.model.User;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface UserApiService {

    @GET("users/me")
    Call<ApiResponse<User>> getMe();

    @PUT("users/me")
    Call<ApiResponse<User>> updateProfile(@Body Map<String, String> body);

    @GET("users/search")
    Call<ApiResponse<List<User>>> searchUsers(@Query("q") String query);

    @GET("users/{userId}")
    Call<ApiResponse<User>> getPublicProfile(@Path("userId") Long userId);
}
