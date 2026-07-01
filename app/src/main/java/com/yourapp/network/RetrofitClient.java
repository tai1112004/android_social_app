package com.yourapp.network;

import com.google.gson.Gson;
import com.yourapp.util.Constants;
import com.yourapp.util.TokenManager;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit singleton client with OkHttp interceptor for JWT authentication.
 * The interceptor automatically adds Authorization: Bearer <token> header
 * to all outgoing requests if a valid access token is available.
 */
public class RetrofitClient {
    private static Retrofit retrofit;
    private static final Object lock = new Object();

    public static Retrofit getInstance(TokenManager tokenManager) {
        if (retrofit == null) {
            synchronized (lock) {
                if (retrofit == null) {
                    // Create HttpLoggingInterceptor
                    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
                    loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

                    // Create OkHttpClient with JWT interceptor and logging
                    OkHttpClient okHttpClient = new OkHttpClient.Builder()
                        .addInterceptor(chain -> {
                            okhttp3.Request originalRequest = chain.request();
                            String accessToken = tokenManager.getAccessToken();

                            // Add Authorization header if token exists
                            if (accessToken != null && !accessToken.isEmpty()) {
                                okhttp3.Request authorizedRequest = originalRequest
                                    .newBuilder()
                                    .header("Authorization", "Bearer " + accessToken)
                                    .build();
                                return chain.proceed(authorizedRequest);
                            }

                            return chain.proceed(originalRequest);
                        })
                        .addInterceptor(loggingInterceptor)
                        .connectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
                        .readTimeout(Constants.READ_TIMEOUT, TimeUnit.MILLISECONDS)
                        .writeTimeout(Constants.WRITE_TIMEOUT, TimeUnit.MILLISECONDS)
                        .build();

                    // Create Retrofit instance
                    retrofit = new Retrofit.Builder()
                        .baseUrl(Constants.getBaseUrl())
                        .client(okHttpClient)
                        .addConverterFactory(GsonConverterFactory.create(new Gson()))
                        .build();
                }
            }
        }
        return retrofit;
    }

    /**
     * Reset Retrofit instance (useful for testing or token refresh scenarios)
     */
    public static void reset() {
        synchronized (lock) {
            retrofit = null;
        }
    }
}
