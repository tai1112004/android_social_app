package com.yourapp.data.remote;

import com.yourapp.model.ApiResponse;
import com.yourapp.model.Conversation;
import com.yourapp.model.ConversationInfo;
import com.yourapp.model.CreateConversationRequest;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.POST;

/**
 * Retrofit interface for conversation endpoints.
 */
public interface ConversationApiService {

    @GET("conversations")
    Call<ApiResponse<List<Conversation>>> getConversations();

    @POST("conversations")
    Call<ApiResponse<Conversation>> createConversation(@Body CreateConversationRequest request);

    @POST("conversations/{conversationId}/read")
    Call<ApiResponse<Object>> markConversationRead(@Path("conversationId") Long conversationId);
    @GET("conversations/{conversationId}/info")
    Call<ApiResponse<ConversationInfo>> getConversationInfo(@Path("conversationId") Long conversationId);
}