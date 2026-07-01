package com.yourapp.data.remote;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import com.google.gson.Gson;
import com.yourapp.model.DeezerTrack;
import com.yourapp.model.MusicTrack;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Service class for interact with Deezer Public API.
 * Handles search and popular tracks query.
 */
public class DeezerApiService {
    private final OkHttpClient client;
    private final Handler mainHandler;
    private final Gson gson;

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }

    public DeezerApiService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.gson = new Gson();
    }

    /**
     * Get top 20 trending tracks from Deezer Chart API.
     */
    public void getChart(ApiCallback<List<MusicTrack>> callback) {
        String url = "https://api.deezer.com/chart/0/tracks?limit=20";
        executeRequest(url, callback);
    }

    /**
     * Search tracks by title, artist, or general query on Deezer API.
     */
    public void search(String query, ApiCallback<List<MusicTrack>> callback) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
            String url = "https://api.deezer.com/search?q=" + encoded + "&limit=20";
            executeRequest(url, callback);
        } catch (Exception e) {
            callback.onError("Lỗi mã hóa từ khóa tìm kiếm");
        }
    }

    /**
     * Search specific track name and artist name on Deezer API.
     */
    public void searchTrackAndArtist(String trackName, String artistName, ApiCallback<List<MusicTrack>> callback) {
        try {
            String q = "track:\"" + trackName + "\" artist:\"" + artistName + "\"";
            String encoded = URLEncoder.encode(q, StandardCharsets.UTF_8.name());
            String url = "https://api.deezer.com/search?q=" + encoded + "&limit=20";
            executeRequest(url, callback);
        } catch (Exception e) {
            callback.onError("Lỗi mã hóa thông tin tìm kiếm");
        }
    }

    private void executeRequest(String url, ApiCallback<List<MusicTrack>> callback) {
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mainHandler.post(() -> callback.onError("Không có kết nối mạng"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onError("Lỗi phản hồi từ Deezer: " + response.code()));
                    return;
                }
                String body = response.body() != null ? response.body().string() : "";
                List<MusicTrack> tracks = new ArrayList<>();
                try {
                    JSONObject jsonObject = new JSONObject(body);
                    JSONArray data = jsonObject.optJSONArray("data");
                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject item = data.getJSONObject(i);
                            DeezerTrack deezerTrack = gson.fromJson(item.toString(), DeezerTrack.class);
                            if (deezerTrack != null && deezerTrack.getPreview() != null && !deezerTrack.getPreview().isEmpty()) {
                                String id = String.valueOf(deezerTrack.getId());
                                String title = deezerTrack.getTitle();
                                String artistName = deezerTrack.getArtist() != null ? deezerTrack.getArtist().getName() : "Không rõ nghệ sĩ";
                                String previewUrl = deezerTrack.getPreview();
                                String coverUrl = deezerTrack.getAlbum() != null ? deezerTrack.getAlbum().getCover_medium() : "";
                                tracks.add(new MusicTrack(id, title, artistName, previewUrl, coverUrl));
                            }
                        }
                    }
                    mainHandler.post(() -> callback.onSuccess(tracks));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError("Lỗi xử lý dữ liệu từ Deezer"));
                }
            }
        });
    }
}
