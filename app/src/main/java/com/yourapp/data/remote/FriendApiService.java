package com.yourapp.data.remote;

import com.yourapp.model.ApiResponse;
import com.yourapp.model.Friend;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface FriendApiService {

    @POST("friends/request")
    Call<ApiResponse<Friend>> sendRequest(@Query("userId") Long userId);

    @GET("friends")
    Call<ApiResponse<List<Friend>>> getFriends();

    @GET("friends/requests/incoming")
    Call<ApiResponse<List<Friend>>> getIncomingRequests();

    @GET("friends/requests/sent")
    Call<ApiResponse<List<Friend>>> getSentRequests();

    @GET("friends/suggestions")
    Call<ApiResponse<List<Friend>>> getSuggestions();

    @PUT("friends/{friendshipId}/accept")
    Call<ApiResponse<Friend>> accept(@Path("friendshipId") Long friendshipId);

    @PUT("friends/{friendshipId}/block")
    Call<ApiResponse<Object>> block(@Path("friendshipId") Long friendshipId);

    @DELETE("friends/{friendshipId}")
    Call<ApiResponse<Object>> remove(@Path("friendshipId") Long friendshipId);
}
