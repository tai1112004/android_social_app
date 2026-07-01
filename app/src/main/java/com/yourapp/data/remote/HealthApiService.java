package com.yourapp.data.remote;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.GET;

public interface HealthApiService {
    @GET("health")
    Call<Map<String, Object>> health();
}
