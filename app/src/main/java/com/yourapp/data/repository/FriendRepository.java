package com.yourapp.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.yourapp.data.remote.FriendApiService;
import com.yourapp.model.ApiResponse;
import com.yourapp.model.Friend;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FriendRepository {

    private final FriendApiService friendApiService;

    public FriendRepository(FriendApiService friendApiService) {
        this.friendApiService = friendApiService;
    }

    public LiveData<AuthRepository.Result<List<Friend>>> getFriends() {
        return list(friendApiService.getFriends(), "Không tải được danh sách bạn bè");
    }

    public LiveData<AuthRepository.Result<List<Friend>>> getIncomingRequests() {
        return list(friendApiService.getIncomingRequests(), "Không tải được lời mời kết bạn");
    }

    public LiveData<AuthRepository.Result<List<Friend>>> getSentRequests() {
        return list(friendApiService.getSentRequests(), "Không tải được lời mời đã gửi");
    }

    public LiveData<AuthRepository.Result<List<Friend>>> getSuggestions() {
        return list(friendApiService.getSuggestions(), "Không tải được gợi ý bạn bè");
    }

    public LiveData<AuthRepository.Result<Friend>> sendRequest(Long userId) {
        return single(friendApiService.sendRequest(userId), "Không gửi được lời mời");
    }

    public LiveData<AuthRepository.Result<Friend>> accept(Long friendshipId) {
        return single(friendApiService.accept(friendshipId), "Không chấp nhận được lời mời");
    }

    public LiveData<AuthRepository.Result<Object>> block(Long friendshipId) {
        MutableLiveData<AuthRepository.Result<Object>> liveData = new MutableLiveData<>();
        friendApiService.block(friendshipId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.code() == 401) {
                    liveData.postValue(new AuthRepository.Result.Error<>("UNAUTHORIZED"));
                    return;
                }
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.postValue(new AuthRepository.Result.Success<>(response.body().getData()));
                } else {
                    liveData.postValue(new AuthRepository.Result.Error<>("Khong chan duoc ban be"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                liveData.postValue(new AuthRepository.Result.Error<>(
                        com.yourapp.util.NetworkErrorUtil.friendlyMessage(t, "Loi ket noi mang")));
            }
        });
        return liveData;
    }
    public LiveData<AuthRepository.Result<Object>> remove(Long friendshipId) {
        MutableLiveData<AuthRepository.Result<Object>> liveData = new MutableLiveData<>();
        friendApiService.remove(friendshipId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.code() == 401) {
                    liveData.postValue(new AuthRepository.Result.Error<>("UNAUTHORIZED"));
                    return;
                }
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.postValue(new AuthRepository.Result.Success<>(response.body().getData()));
                } else {
                    liveData.postValue(new AuthRepository.Result.Error<>("Không cập nhật được lời mời"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                liveData.postValue(new AuthRepository.Result.Error<>(
                        com.yourapp.util.NetworkErrorUtil.friendlyMessage(t, "Lỗi kết nối mạng")));
            }
        });
        return liveData;
    }

    private LiveData<AuthRepository.Result<List<Friend>>> list(
            Call<ApiResponse<List<Friend>>> call,
            String fallbackError) {
        MutableLiveData<AuthRepository.Result<List<Friend>>> liveData = new MutableLiveData<>();
        call.enqueue(new Callback<ApiResponse<List<Friend>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Friend>>> call,
                                   Response<ApiResponse<List<Friend>>> response) {
                if (response.code() == 401) {
                    liveData.postValue(new AuthRepository.Result.Error<>("UNAUTHORIZED"));
                    return;
                }
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.postValue(new AuthRepository.Result.Success<>(response.body().getData()));
                } else {
                    liveData.postValue(new AuthRepository.Result.Error<>(fallbackError));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Friend>>> call, Throwable t) {
                liveData.postValue(new AuthRepository.Result.Error<>(
                        com.yourapp.util.NetworkErrorUtil.friendlyMessage(t, "Lỗi kết nối mạng")));
            }
        });
        return liveData;
    }

    private LiveData<AuthRepository.Result<Friend>> single(
            Call<ApiResponse<Friend>> call,
            String fallbackError) {
        MutableLiveData<AuthRepository.Result<Friend>> liveData = new MutableLiveData<>();
        call.enqueue(new Callback<ApiResponse<Friend>>() {
            @Override
            public void onResponse(Call<ApiResponse<Friend>> call, Response<ApiResponse<Friend>> response) {
                if (response.code() == 401) {
                    liveData.postValue(new AuthRepository.Result.Error<>("UNAUTHORIZED"));
                    return;
                }
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.postValue(new AuthRepository.Result.Success<>(response.body().getData()));
                } else {
                    liveData.postValue(new AuthRepository.Result.Error<>(fallbackError));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Friend>> call, Throwable t) {
                liveData.postValue(new AuthRepository.Result.Error<>(
                        com.yourapp.util.NetworkErrorUtil.friendlyMessage(t, "Lỗi kết nối mạng")));
            }
        });
        return liveData;
    }
}

