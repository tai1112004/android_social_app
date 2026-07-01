package com.yourapp.data.remote;

import com.yourapp.model.ApiResponse;
import com.yourapp.model.Comment;
import com.yourapp.model.Post;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.PUT;

public interface PostApiService {

    /** GET /api/posts - Feed bài viết từ bạn bè + bản thân */
    @GET("posts")
    Call<ApiResponse<List<Post>>> getFeed();

    /** POST /api/posts - Đăng bài mới */
    @POST("posts")
    Call<ApiResponse<Post>> createPost(@Body Map<String, String> body);

    /** POST /api/posts/{postId}/like - Thích/bỏ thích bài viết */
    @POST("posts/{postId}/like")
    Call<ApiResponse<Post>> toggleLike(@Path("postId") Long postId);

    /** GET /api/posts/{postId}/comments - Lấy danh sách bình luận */
    @GET("posts/{postId}/comments")
    Call<ApiResponse<List<Comment>>> getComments(@Path("postId") Long postId);

    /** POST /api/posts/{postId}/comments - Thêm bình luận */
    @POST("posts/{postId}/comments")
    Call<ApiResponse<Comment>> addComment(@Path("postId") Long postId, @Body Map<String, String> body);

    /** POST /api/posts/{postId}/comments/{commentId}/reply - Trả lời comment */
    @POST("posts/{postId}/comments/{commentId}/reply")
    Call<ApiResponse<Comment>> replyToComment(@Path("postId") Long postId, @Path("commentId") Long commentId, @Body Map<String, String> body);

    /** PUT /api/posts/{postId}/comments/{commentId}/reaction - Thả icon vào comment */
    @PUT("posts/{postId}/comments/{commentId}/reaction")
    Call<ApiResponse<Comment>> reactToComment(@Path("postId") Long postId, @Path("commentId") Long commentId, @Body Map<String, String> body);
}