package com.yourapp.data.remote;

import com.yourapp.model.ApiResponse;
import com.yourapp.model.CallHistory;
import com.yourapp.model.CallHistoryPage;
import com.yourapp.model.CallSessionSnapshot;
import com.yourapp.model.CallSignalMessage;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface CallApiService {

    @POST("calls/initiate")
    Call<ApiResponse<CallSignalMessage>> initiateCall(@Body CallSignalMessage message);

    @POST("calls/answer")
    Call<ApiResponse<CallSignalMessage>> answerCall(@Body CallSignalMessage message);

    @POST("calls/end")
    Call<ApiResponse<CallSignalMessage>> endCall(@Body CallSignalMessage message);

    @POST("calls/ice-candidate")
    Call<ApiResponse<CallSignalMessage>> sendIceCandidate(@Body CallSignalMessage message);

    @GET("calls/history")
    Call<ApiResponse<CallHistoryPage>> getCallHistory(@Query("userId") Long userId,
                                                      @Query("page") int page,
                                                      @Query("size") int size);

    @GET("calls/incoming")
    Call<ApiResponse<CallSessionSnapshot>> getIncomingCall();

    @GET("calls/sessions/{sessionId}")
    Call<ApiResponse<CallSessionSnapshot>> getCallSession(@Path("sessionId") String sessionId);
}
