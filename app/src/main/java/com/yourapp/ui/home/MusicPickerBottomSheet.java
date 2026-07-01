package com.yourapp.ui.home;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.tabs.TabLayout;
import com.yourapp.R;
import com.yourapp.data.remote.DeezerApiService;
import com.yourapp.data.remote.MusixmatchApiService;
import com.yourapp.model.MusicTrack;
import java.util.ArrayList;
import java.util.List;

/**
 * Bottom Sheet that allows users to search music from Deezer (by track/artist name)
 * or Musixmatch (by lyrics) and preview or pick them.
 */
public class MusicPickerBottomSheet extends BottomSheetDialogFragment {
    public interface OnMusicSelectedListener {
        void onMusicSelected(MusicTrack track);
    }

    private final OnMusicSelectedListener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final DeezerApiService deezerApiService = new DeezerApiService();
    private final MusixmatchApiService musixmatchApiService = new MusixmatchApiService();
    
    private final Runnable searchRunnable = this::runCurrentSearch;
    private final List<MusicTrack> tracks = new ArrayList<>();
    private MusicAdapter adapter;
    private EditText searchInput;
    private TextView label;
    private TextView emptyView;
    private ProgressBar progressBar;
    private int selectedTab = 0;
    private MediaPlayer mediaPlayer;
    private MusicTrack playingTrack;

    public MusicPickerBottomSheet(OnMusicSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.music_picker_bottom_sheet, container, false);
        searchInput = view.findViewById(R.id.et_music_search);
        label = view.findViewById(R.id.tv_music_label);
        emptyView = view.findViewById(R.id.tv_music_empty);
        progressBar = view.findViewById(R.id.progress_music);
        RecyclerView recyclerView = view.findViewById(R.id.rv_music_results);
        TabLayout tabs = view.findViewById(R.id.tabs_music);
        view.findViewById(R.id.btn_music_close).setOnClickListener(v -> dismiss());

        // Thiết lập tabs
        tabs.addTab(tabs.newTab().setText("Theo tên bài / nghệ sĩ"));
        tabs.addTab(tabs.newTab().setText("Theo lời bài hát"));
        
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override 
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTab = tab.getPosition();
                scheduleSearch();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Thiết lập RecyclerView và Adapter
        adapter = new MusicAdapter(tracks, new MusicAdapter.Listener() {
            @Override 
            public void onPick(MusicTrack track) {
                if (listener != null) listener.onMusicSelected(track);
                dismiss();
            }
            @Override 
            public void onPreview(MusicTrack track) {
                togglePreview(track);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // Debounce search gõ phím
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override 
            public void onTextChanged(CharSequence s, int start, int before, int count) { 
                scheduleSearch(); 
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Tải danh sách thịnh hành lúc mở
        loadChart();
        return view;
    }

    private void scheduleSearch() {
        mainHandler.removeCallbacks(searchRunnable);
        mainHandler.postDelayed(searchRunnable, 500); // Chờ 500ms debounce
    }

    private void runCurrentSearch() {
        String query = searchInput.getText().toString().trim();
        if (query.length() < 2) {
            loadChart();
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        
        if (selectedTab == 0) {
            label.setText("Kết quả tìm kiếm");
            deezerApiService.search(query, new DeezerApiService.ApiCallback<List<MusicTrack>>() {
                @Override
                public void onSuccess(List<MusicTrack> result) {
                    displayResults(result);
                }

                @Override
                public void onError(String errorMessage) {
                    showError(errorMessage);
                }
            });
        } else {
            label.setText("Kết quả theo lời bài");
            musixmatchApiService.searchByLyrics(query, deezerApiService, new DeezerApiService.ApiCallback<List<MusicTrack>>() {
                @Override
                public void onSuccess(List<MusicTrack> result) {
                    displayResults(result);
                }

                @Override
                public void onError(String errorMessage) {
                    showError(errorMessage);
                }
            });
        }
    }

    private void loadChart() {
        label.setText("Đang thịnh hành");
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        deezerApiService.getChart(new DeezerApiService.ApiCallback<List<MusicTrack>>() {
            @Override
            public void onSuccess(List<MusicTrack> result) {
                displayResults(result);
            }

            @Override
            public void onError(String errorMessage) {
                showError(errorMessage);
            }
        });
    }

    private void displayResults(List<MusicTrack> result) {
        progressBar.setVisibility(View.GONE);
        tracks.clear();
        if (result != null) {
            tracks.addAll(result);
        }
        adapter.notifyDataSetChanged();
        emptyView.setVisibility(tracks.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void togglePreview(MusicTrack track) {
        if (playingTrack == track && mediaPlayer != null && mediaPlayer.isPlaying()) {
            stopPreview();
            return;
        }
        stopPreview();
        try {
            playingTrack = track;
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            mediaPlayer.setDataSource(track.getPreviewUrl());
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
            mediaPlayer.setOnCompletionListener(mp -> stopPreview());
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                stopPreview();
                return true;
            });
            mediaPlayer.prepareAsync();
            adapter.setPlayingTrack(track);
        } catch (Exception e) {
            stopPreview();
            Toast.makeText(requireContext(), "Không phát được preview", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopPreview() {
        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); } catch (Exception ignored) {}
            mediaPlayer.release();
            mediaPlayer = null;
        }
        playingTrack = null;
        if (adapter != null) adapter.setPlayingTrack(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mainHandler.removeCallbacks(searchRunnable);
        stopPreview();
    }
}
