package com.yourapp.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.yourapp.data.remote.ConversationApiService;
import com.yourapp.model.ApiResponse;
import com.yourapp.model.Conversation;
import com.yourapp.model.ConversationInfo;
import com.yourapp.model.CreateConversationRequest;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for conversation data.
 * Follows the same pattern as AuthRepository:
 * wraps Retrofit calls in LiveData<Result<T>>.
 *
 * On HTTP 401: posts Result.Error("UNAUTHORIZED") so HomeActivity
 * can redirect to LoginActivity.
 */
public class ConversationRepository {

    private final ConversationApiService conversationApiService;

    public ConversationRepository(ConversationApiService conversationApiService) {
        this.conversationApiService = conversationApiService;
    }

    /**
     * Fetches the conversation list for the authenticated user.
     *
     * @return LiveData wrapping Result.Success<List<Conversation>> or Result.Error
     */
    public LiveData<AuthRepository.Result<List<Conversation>>> getConversations() {
        MutableLiveData<AuthRepository.Result<List<Conversation>>> resultLiveData =
                new MutableLiveData<>();

        conversationApiService.getConversations()
                .enqueue(new Callback<ApiResponse<List<Conversation>>>() {

                    @Override
                    public void onResponse(Call<ApiResponse<List<Conversation>>> call,
                                           Response<ApiResponse<List<Conversation>>> response) {
                        if (response.code() == 401) {
                            resultLiveData.postValue(
                                    new AuthRepository.Result.Error<>("UNAUTHORIZED"));
                            return;
                        }

                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<List<Conversation>> apiResponse = response.body();
                            if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                resultLiveData.postValue(
                                        new AuthRepository.Result.Success<>(apiResponse.getData()));
                            } else {
                                String errorMsg = apiResponse.getError() != null
                                        ? apiResponse.getError()
                                        : apiResponse.getMessage();
                                resultLiveData.postValue(
                                        new AuthRepository.Result.Error<>(errorMsg));
                            }
                        } else {
                            resultLiveData.postValue(
                                    new AuthRepository.Result.Error<>("Không tải được cuộc trò chuyện"));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<Conversation>>> call, Throwable t) {
                        resultLiveData.postValue(
                                new AuthRepository.Result.Error<>(
                                        com.yourapp.util.NetworkErrorUtil.friendlyMessage(t, "Lỗi kết nối mạng")));
                    }
                });

        return resultLiveData;
    }

    public LiveData<AuthRepository.Result<Conversation>> createConversation(
            CreateConversationRequest request) {
        MutableLiveData<AuthRepository.Result<Conversation>> resultLiveData =
                new MutableLiveData<>();

        conversationApiService.createConversation(request)
                .enqueue(new Callback<ApiResponse<Conversation>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Conversation>> call,
                                           Response<ApiResponse<Conversation>> response) {
                        if (response.code() == 401) {
                            resultLiveData.postValue(
                                    new AuthRepository.Result.Error<>("UNAUTHORIZED"));
                            return;
                        }
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) {
                            resultLiveData.postValue(
                                    new AuthRepository.Result.Success<>(response.body().getData()));
                        } else {
                            resultLiveData.postValue(
                                    new AuthRepository.Result.Error<>("Không tạo được cuộc trò chuyện"));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Conversation>> call, Throwable t) {
                        resultLiveData.postValue(
                                new AuthRepository.Result.Error<>(
                                        com.yourapp.util.NetworkErrorUtil.friendlyMessage(t, "Lỗi kết nối mạng")));
                    }
                });

        return resultLiveData;
    }

    public LiveData<AuthRepository.Result<Object>> markConversationRead(Long conversationId) {
        MutableLiveData<AuthRepository.Result<Object>> resultLiveData = new MutableLiveData<>();
        conversationApiService.markConversationRead(conversationId)
                .enqueue(new Callback<ApiResponse<Object>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Object>> call,
                                           Response<ApiResponse<Object>> response) {
                        if (response.code() == 401) {
                            resultLiveData.postValue(new AuthRepository.Result.Error<>("UNAUTHORIZED"));
                            return;
                        }
                        if (response.isSuccessful()) {
                            resultLiveData.postValue(new AuthRepository.Result.Success<>(null));
                        } else {
                            resultLiveData.postValue(new AuthRepository.Result.Error<>("Không đánh dấu được đã đọc"));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                        resultLiveData.postValue(new AuthRepository.Result.Error<>(
                                com.yourapp.util.NetworkErrorUtil.friendlyMessage(t, "Lỗi kết nối mạng")));
                    }
                });
        return resultLiveData;
    }
    public LiveData<AuthRepository.Result<ConversationInfo>> getConversationInfo(Long conversationId) {
        MutableLiveData<AuthRepository.Result<ConversationInfo>> resultLiveData =
                new MutableLiveData<>();
        conversationApiService.getConversationInfo(conversationId)
                .enqueue(new Callback<ApiResponse<ConversationInfo>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<ConversationInfo>> call,
                                           Response<ApiResponse<ConversationInfo>> response) {
                        if (response.code() == 401) {
                            resultLiveData.postValue(new AuthRepository.Result.Error<>("UNAUTHORIZED"));
                            return;
                        }
                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) {
                            resultLiveData.postValue(
                                    new AuthRepository.Result.Success<>(response.body().getData()));
                        } else {
                            resultLiveData.postValue(
                                    new AuthRepository.Result.Error<>("Không tải được thông tin chat"));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<ConversationInfo>> call, Throwable t) {
                        resultLiveData.postValue(
                                new AuthRepository.Result.Error<>(
                                        com.yourapp.util.NetworkErrorUtil.friendlyMessage(t, "Lỗi kết nối mạng")));
                    }
                });
        return resultLiveData;
    }
}
