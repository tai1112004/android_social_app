package com.yourapp.data.remote;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import com.yourapp.model.MusicTrack;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Service class to call Musixmatch API for lyric search,
 * and integrate with Deezer API to get the preview URL.
 */
public class MusixmatchApiService {
    // Đăng ký key miễn phí tại developer.musixmatch.com và thay vào đây
    private static final String API_KEY = "da2542a25ff0cf92f96cfb5e7d56e7e0";

    private final OkHttpClient client;
    private final Handler mainHandler;

    public MusixmatchApiService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Search songs by lyrics, then resolve each matching song using Deezer API.
     */
    public void searchByLyrics(String lyrics, DeezerApiService deezerApiService, DeezerApiService.ApiCallback<List<MusicTrack>> callback) {
        try {
            String encodedLyrics = URLEncoder.encode(lyrics, StandardCharsets.UTF_8.name());
            String url = "https://api.musixmatch.com/ws/1.1/track.search"
                    + "?q_lyrics=" + encodedLyrics
                    + "&apikey=" + API_KEY
                    + "&s_track_rating=desc"
                    + "&page_size=10";

            Request request = new Request.Builder().url(url).build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    mainHandler.post(() -> callback.onError("Không có kết nối mạng với Musixmatch"));
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        mainHandler.post(() -> callback.onError("Lỗi phản hồi từ Musixmatch"));
                        return;
                    }
                    String body = response.body() != null ? response.body().string() : "";
                    try {
                        JSONObject jsonObject = new JSONObject(body);
                        JSONObject message = jsonObject.optJSONObject("message");
                        if (message == null) {
                            mainHandler.post(() -> callback.onError("Lỗi cấu trúc dữ liệu Musixmatch"));
                            return;
                        }
                        JSONObject header = message.optJSONObject("header");
                        int statusCode = header != null ? header.optInt("status_code", 200) : 200;
                        if (statusCode == 401 || statusCode == 402) {
                            mainHandler.post(() -> callback.onError("Lỗi xác thực Musixmatch API Key"));
                            return;
                        }

                        JSONObject messageBody = message.optJSONObject("body");
                        if (messageBody == null) {
                            mainHandler.post(() -> callback.onSuccess(new ArrayList<>()));
                            return;
                        }
                        JSONArray trackList = messageBody.optJSONArray("track_list");
                        if (trackList == null || trackList.length() == 0) {
                            mainHandler.post(() -> callback.onSuccess(new ArrayList<>()));
                            return;
                        }

                        // Lấy danh sách track name + artist name
                        List<TrackQuery> queries = new ArrayList<>();
                        for (int i = 0; i < trackList.length(); i++) {
                            JSONObject item = trackList.getJSONObject(i);
                            JSONObject track = item.optJSONObject("track");
                            if (track != null) {
                                String trackName = track.optString("track_name", "");
                                String artistName = track.optString("artist_name", "");
                                if (!trackName.isEmpty() && !artistName.isEmpty()) {
                                    queries.add(new TrackQuery(trackName, artistName));
                                }
                            }
                        }

                        if (queries.isEmpty()) {
                            mainHandler.post(() -> callback.onSuccess(new ArrayList<>()));
                            return;
                        }

                        // Resolve từng bài thông qua Deezer API để lấy preview URL
                        resolveDeezerTracks(queries, deezerApiService, callback);

                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onError("Lỗi xử lý dữ liệu từ Musixmatch"));
                    }
                }
            });
        } catch (Exception e) {
            callback.onError("Lỗi mã hóa từ khóa tìm kiếm");
        }
    }

    private void resolveDeezerTracks(List<TrackQuery> queries, DeezerApiService deezerApiService, DeezerApiService.ApiCallback<List<MusicTrack>> callback) {
        List<MusicTrack> resolvedTracks = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger pendingRequests = new AtomicInteger(queries.size());

        for (TrackQuery query : queries) {
            deezerApiService.searchTrackAndArtist(query.trackName, query.artistName, new DeezerApiService.ApiCallback<List<MusicTrack>>() {
                @Override
                public void onSuccess(List<MusicTrack> result) {
                    if (result != null && !result.isEmpty()) {
                        resolvedTracks.add(result.get(0)); // Lấy bài khớp đầu tiên
                    }
                    checkCompletion();
                }

                @Override
                public void onError(String errorMessage) {
                    // Tiếp tục xử lý các bài khác ngay cả khi bài này bị lỗi
                    checkCompletion();
                }

                private void checkCompletion() {
                    if (pendingRequests.decrementAndGet() == 0) {
                        // Sắp xếp lại hoặc trả về kết quả trên main thread
                        mainHandler.post(() -> callback.onSuccess(new ArrayList<>(resolvedTracks)));
                    }
                }
            });
        }
    }

    private static class TrackQuery {
        String trackName;
        String artistName;

        TrackQuery(String trackName, String artistName) {
            this.trackName = trackName;
            this.artistName = artistName;
        }
    }
}
