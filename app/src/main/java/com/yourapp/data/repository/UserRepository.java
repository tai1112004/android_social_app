package com.yourapp.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.yourapp.data.remote.UserApiService;
import com.yourapp.model.ApiResponse;
import com.yourapp.model.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRepository {

    private final UserApiService userApiService;

    public UserRepository(UserApiService userApiService) {
        this.userApiService = userApiService;
    }

    public LiveData<AuthRepository.Result<User>> getMe() {
        MutableLiveData<AuthRepository.Result<User>> liveData = new MutableLiveData<>();
        userApiService.getMe().enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.code() == 401) {
                    liveData.postValue(new AuthRepository.Result.Error<>("UNAUTHORIZED"));
                    return;
                }
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.postValue(new AuthRepository.Result.Success<>(response.body().getData()));
                } else {
                    liveData.postValue(new AuthRepository.Result.Error<>("Khong tai duoc ho so"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                liveData.postValue(new AuthRepository.Result.Error<>(com.yourapp.util.NetworkErrorUtil.friendlyMessage(t, "Lỗi kết nối mạng")));
            }
        });
        return liveData;
    }

    public LiveData<AuthRepository.Result<User>> updateProfile(String displayName, String avatarUrl) {
        return updateProfile(displayName, avatarUrl, null, null, null, null, null, null, null, null);
    }

    public LiveData<AuthRepository.Result<User>> updateProfile(String displayName, String avatarUrl,
                                                               String coverUrl, String bio,
                                                               String location, String website) {
        return updateProfile(displayName, avatarUrl, coverUrl, bio, location, website, null, null, null, null);
    }

    public LiveData<AuthRepository.Result<User>> updateProfile(String displayName, String avatarUrl,
                                                               String coverUrl, String bio,
                                                               String location, String website,
                                                               String dateOfBirth, String gender,
                                                               String phone, String email) {
        MutableLiveData<AuthRepository.Result<User>> liveData = new MutableLiveData<>();
        Map<String, String> body = new HashMap<>();
        if (displayName != null) body.put("displayName", displayName);
        if (avatarUrl != null) body.put("avatarUrl", avatarUrl);
        if (coverUrl != null) body.put("coverUrl", coverUrl);
        if (bio != null) body.put("bio", bio);
        if (location != null) body.put("location", location);
        if (website != null) body.put("website", website);
        if (dateOfBirth != null) body.put("dateOfBirth", dateOfBirth);
        if (gender != null) body.put("gender", gender);
        if (phone != null) body.put("phone", phone);
        if (email != null) body.put("email", email);
        userApiService.updateProfile(body).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.code() == 401) {
                    liveData.postValue(new AuthRepository.Result.Error<>("UNAUTHORIZED"));
                    return;
                }
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.postValue(new AuthRepository.Result.Success<>(response.body().getData()));
                } else {
                    liveData.postValue(new AuthRepository.Result.Error<>("Cap nhat ho so that bai"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                liveData.postValue(new AuthRepository.Result.Error<>(com.yourapp.util.NetworkErrorUtil.friendlyMessage(t, "Lỗi kết nối mạng")));
            }
        });
        return liveData;
    }

    public LiveData<AuthRepository.Result<List<User>>> searchUsers(String query) {
        MutableLiveData<AuthRepository.Result<List<User>>> liveData = new MutableLiveData<>();
        userApiService.searchUsers(query).enqueue(new Callback<ApiResponse<List<User>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<User>>> call, Response<ApiResponse<List<User>>> response) {
                if (response.code() == 401) {
                    liveData.postValue(new AuthRepository.Result.Error<>("UNAUTHORIZED"));
                    return;
                }
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.postValue(new AuthRepository.Result.Success<>(response.body().getData()));
                } else {
                    liveData.postValue(new AuthRepository.Result.Error<>("Tim kiem that bai"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<User>>> call, Throwable t) {
                liveData.postValue(new AuthRepository.Result.Error<>(com.yourapp.util.NetworkErrorUtil.friendlyMessage(t, "Lỗi kết nối mạng")));
            }
        });
        return liveData;
    }

    public LiveData<AuthRepository.Result<User>> getPublicProfile(Long userId) {
        MutableLiveData<AuthRepository.Result<User>> liveData = new MutableLiveData<>();
        userApiService.getPublicProfile(userId).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.code() == 401) {
                    liveData.postValue(new AuthRepository.Result.Error<>("UNAUTHORIZED"));
                    return;
                }
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.postValue(new AuthRepository.Result.Success<>(response.body().getData()));
                } else {
                    liveData.postValue(new AuthRepository.Result.Error<>("Khong tai duoc trang ca nhan"));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                liveData.postValue(new AuthRepository.Result.Error<>(com.yourapp.util.NetworkErrorUtil.friendlyMessage(t, "Lỗi kết nối mạng")));
            }
        });
        return liveData;
    }
}