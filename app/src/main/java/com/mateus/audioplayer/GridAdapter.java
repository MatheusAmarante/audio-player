package com.mateus.audioplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class GridAdapter extends RecyclerView.Adapter<GridAdapter.ViewHolder> {

    private final List<YouTubeTrack> tracks;
    private final OnTrackClickListener listener;

    public interface OnTrackClickListener {
        void onTrackClick(int position);
    }

    public GridAdapter(List<YouTubeTrack> tracks, OnTrackClickListener listener) {
        this.tracks = tracks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_grid_track, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        YouTubeTrack track = tracks.get(position);
        holder.titleText.setText(track.title);
        holder.artistText.setText(track.getArtistOrUploader());

        if (track.thumbnailUrl != null && !track.thumbnailUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                .load(track.thumbnailUrl)
                .placeholder(R.drawable.album_art_placeholder)
                .centerCrop()
                .into(holder.thumbnail);
        } else {
            holder.thumbnail.setImageResource(R.drawable.album_art_placeholder);
        }

        holder.itemView.setOnClickListener(v -> listener.onTrackClick(position));
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView titleText, artistText;

        ViewHolder(View v) {
            super(v);
            thumbnail = v.findViewById(R.id.grid_thumbnail);
            titleText = v.findViewById(R.id.grid_title);
            artistText = v.findViewById(R.id.grid_artist);
        }
    }
}
