package com.yourapp.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.yourapp.data.remote.MessageApiService;
import com.yourapp.model.ApiResponse;
import com.yourapp.model.Message;
import com.yourapp.model.ReactionRequest;
import com.yourapp.model.SendMessageRequest;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageRepository {

    private final MessageApiService messageApiService;

    public MessageRepository(MessageApiService messageApiService) {
        this.messageApiService = messageApiService;
    }

    public LiveData<AuthRepository.Result<List<Message>>> getMessages(Long conversationId) {
        MutableLiveData<AuthRepository.Result<List<Message>>> resultLiveData = new MutableLiveData<>();

        messageApiService.getMessages(conversationId)
                .enqueue(new Callback<ApiResponse<List<Message>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<Message>>> call,
                                           Response<ApiResponse<List<Message>>> response) {
                        if (response.code() == 401) {
                            resultLiveData.postValue(new AuthRepository.Result.Error<>("UNAUTHORIZED"));
                            return;
                        }

                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<List<Message>> apiResponse = response.body();
                            if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                resultLiveData.postValue(
                                        new AuthRepository.Result.Success<>(apiResponse.getData()));
                            } else {
                                resultLiveData.postValue(new AuthRepository.Result.Error<>(
                                        apiResponse.getError() != null
                                                ? apiResponse.getError()
                                                : apiResponse.getMessage()));
                            }
                        } else {
                            resultLiveData.postValue(
                                    new AuthRepository.Result.Error<>("Không tải được tin nhắn"));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<Message>>> call, Throwable t) {
                        resultLiveData.postValue(new AuthRepository.Result.Error<>(
                                com.yourapp.util.NetworkErrorUtil.friendlyMessage(t, "Lỗi kết nối mạng")));
                    }
                });

        return resultLiveData;
    }

    public LiveData<AuthRepository.Result<Message>> sendMessage(Long conversationId, String content) {
        SendMessageRequest request = new SendMessageRequest(content, "TEXT");
        return sendMessage(conversationId, request);
    }

    public LiveData<AuthRepository.Result<Message>> sendMessage(Long conversationId, SendMessageRequest request) {
        MutableLiveData<AuthRepository.Result<Message>> resultLiveData = new MutableLiveData<>();

        messageApiService.sendMessage(conversationId, request)
                .enqueue(new Callback<ApiResponse<Message>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Message>> call,
                                           Response<ApiResponse<Message>> response) {
                        if (response.code() == 401) {
                            resultLiveData.postValue(new AuthRepository.Result.Error<>("UNAUTHORIZED"));
                            return;
                        }

                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<Message> apiResponse = response.body();
                            if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                                resultLiveData.postValue(
                                        new AuthRepository.Result.Success<>(apiResponse.getData()));
                            } else {
                                resultLiveData.postValue(new AuthRepository.Result.Error<>(
                                        apiResponse.getError() != null
                                                ? apiResponse.getError()
                                                : apiResponse.getMessage()));
                            }
                        } else {
                            resultLiveData.postValue(
                                    new AuthRepository.Result.Error<>("Không gửi được tin nhắn"));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Message>> call, Throwable t) {
                        resultLiveData.postValue(new AuthRepository.Result.Error<>(
                                com.yourapp.util.NetworkErrorUtil.friendlyMessage(t, "Lỗi kết nối mạng")));
                    }
                });

        return resultLiveData;
    }

    public LiveData<AuthRepository.Result<Object>> deleteMessage(Long messageId) {
        MutableLiveData<AuthRepository.Result<Object>> resultLiveData = new MutableLiveData<>();
        messageApiService.deleteMessage(messageId)
                .enqueue(new Callback<ApiResponse<Object>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Object>> call,
                                           Response<ApiResponse<Object>> response) {
                        if (response.code() == 401) {
                            resultLiveData.postValue(new AuthRepository.Result.Error<>("UNAUTHORIZED"));
                            return;
                        }
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            resultLiveData.postValue(new AuthRepository.Result.Success<>(response.body().getData()));
                        } else {
                            resultLiveData.postValue(new AuthRepository.Result.Error<>("Khong xoa duoc tin nhan"));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                        resultLiveData.postValue(new AuthRepository.Result.Error<>(
                                com.yourapp.util.NetworkErrorUtil.friendlyMessage(t, "Loi ket noi mang")));
                    }
                });
        return resultLiveData;
    }
    public LiveData<AuthRepository.Result<Message>> reactToMessage(Long messageId, String reaction) {
        MutableLiveData<AuthRepository.Result<Message>> resultLiveData = new MutableLiveData<>();
        messageApiService.reactToMessage(messageId, new ReactionRequest(reaction))
                .enqueue(new Callback<ApiResponse<Message>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Message>> call,
                                           Response<ApiResponse<Message>> response) {
                        if (response.code() == 401) {
                            resultLiveData.postValue(new AuthRepository.Result.Error<>("UNAUTHORIZED"));
                            return;
                        }
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()
                                && response.body().getData() != null) {
                            resultLiveData.postValue(
                                    new AuthRepository.Result.Success<>(response.body().getData()));
                        } else {
                            resultLiveData.postValue(
                                    new AuthRepository.Result.Error<>("Không thả được cảm xúc"));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Message>> call, Throwable t) {
                        resultLiveData.postValue(new AuthRepository.Result.Error<>(
                                com.yourapp.util.NetworkErrorUtil.friendlyMessage(t, "Lỗi kết nối mạng")));
                    }
                });
        return resultLiveData;
    }
}

