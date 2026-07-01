package com.yourapp.data.remote;

import com.yourapp.model.ApiResponse;
import com.yourapp.model.Message;
import com.yourapp.model.ReactionRequest;
import com.yourapp.model.SendMessageRequest;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface MessageApiService {

    @GET("conversations/{conversationId}/messages")
    Call<ApiResponse<List<Message>>> getMessages(@Path("conversationId") Long conversationId);

    @POST("conversations/{conversationId}/messages")
    Call<ApiResponse<Message>> sendMessage(@Path("conversationId") Long conversationId,
                                           @Body SendMessageRequest request);

    @PUT("messages/{messageId}/reaction")
    Call<ApiResponse<Message>> reactToMessage(@Path("messageId") Long messageId,
                                              @Body ReactionRequest request);

    @DELETE("messages/{messageId}")
    Call<ApiResponse<Object>> deleteMessage(@Path("messageId") Long messageId);
}
