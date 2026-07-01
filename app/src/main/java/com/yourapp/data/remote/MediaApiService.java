package com.yourapp.data.remote;

import com.yourapp.model.ApiResponse;
import com.yourapp.model.MediaUpload;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface MediaApiService {

    @Multipart
    @POST("media")
    Call<ApiResponse<MediaUpload>> upload(@Part MultipartBody.Part file);
}
