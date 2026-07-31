package com.mateus.audioplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {

    private final List<DatabaseHelper.Playlist> playlists;
    private final OnPlaylistClickListener listener;

    public interface OnPlaylistClickListener {
        void onPlaylistClick(DatabaseHelper.Playlist playlist);
        void onPlaylistLongClick(DatabaseHelper.Playlist playlist);
    }

    public PlaylistAdapter(List<DatabaseHelper.Playlist> playlists, OnPlaylistClickListener listener) {
        this.playlists = playlists;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_playlist, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DatabaseHelper.Playlist playlist = playlists.get(position);
        holder.nameText.setText(playlist.name);
        holder.trackCountText.setText(playlist.trackCount + " tracks");

        holder.itemView.setOnClickListener(v -> listener.onPlaylistClick(playlist));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onPlaylistLongClick(playlist);
            return true;
        });
        holder.moreBtn.setOnClickListener(v -> listener.onPlaylistLongClick(playlist));
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, trackCountText;
        ImageButton moreBtn;

        ViewHolder(View v) {
            super(v);
            nameText = v.findViewById(R.id.playlist_name);
            trackCountText = v.findViewById(R.id.playlist_track_count);
            moreBtn = v.findViewById(R.id.playlist_more_btn);
        }
    }
}
