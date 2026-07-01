package com.yourapp.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.yourapp.R;
import com.yourapp.model.MusicTrack;
import java.util.List;

/**
 * Adapter for showing Deezer/Musixmatch search results and trending tracks.
 */
public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.Holder> {
    public interface Listener {
        void onPick(MusicTrack track);
        void onPreview(MusicTrack track);
    }

    private final List<MusicTrack> items;
    private final Listener listener;
    private MusicTrack playingTrack;

    public MusicAdapter(List<MusicTrack> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    /**
     * Sets the currently previewing track and refreshes items.
     */
    public void setPlayingTrack(MusicTrack track) {
        this.playingTrack = track;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_music, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        MusicTrack track = items.get(position);
        holder.title.setText(track.getTitle());
        holder.artist.setText(track.getArtist());
        
        // Cập nhật biểu tượng nút preview phát/dừng nhạc
        if (track == playingTrack) {
            holder.preview.setText("⏸");
        } else {
            holder.preview.setText("▶");
        }

        Glide.with(holder.cover.getContext())
                .load(track.getAlbumCover())
                .placeholder(R.drawable.bg_music_cover)
                .centerCrop()
                .into(holder.cover);

        holder.itemView.setOnClickListener(v -> listener.onPick(track));
        holder.preview.setOnClickListener(v -> listener.onPreview(track));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView title;
        TextView artist;
        TextView preview;

        Holder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.iv_music_cover);
            title = itemView.findViewById(R.id.tv_music_title);
            artist = itemView.findViewById(R.id.tv_music_artist);
            preview = itemView.findViewById(R.id.btn_music_preview);
        }
    }
}
