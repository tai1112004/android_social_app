package com.yourapp.data.remote;

import com.yourapp.model.ApiResponse;
import com.yourapp.model.Story;
import com.yourapp.model.StoryGroup;
import com.yourapp.model.StoryViewer;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface StoryApiService {

    @GET("stories")
    Call<ApiResponse<List<StoryGroup>>> getStories();

    @POST("stories")
    Call<ApiResponse<Story>> createStory(@Body Map<String, String> body);

    @POST("stories/{storyId}/view")
    Call<ApiResponse<Object>> markStoryViewed(@Path("storyId") Long storyId);

    @GET("stories/{storyId}/viewers")
    Call<ApiResponse<List<StoryViewer>>> getStoryViewers(@Path("storyId") Long storyId);

    @DELETE("stories/{id}")
    Call<ApiResponse<Void>> deleteStory(@Path("id") Long id);
}
