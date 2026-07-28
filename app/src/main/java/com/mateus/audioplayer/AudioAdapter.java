package com.mateus.audioplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AudioAdapter extends RecyclerView.Adapter<AudioAdapter.ViewHolder> {

    private List<AudioFile> audioList;
    private OnItemClickListener listener;
    private int selectedPosition = -1;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public AudioAdapter(List<AudioFile> list, OnItemClickListener listener) {
        this.audioList = list;
        this.listener = listener;
    }

    public void setSelectedPosition(int pos) {
        int old = selectedPosition;
        selectedPosition = pos;
        notifyItemChanged(old);
        notifyItemChanged(pos);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_audio, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AudioFile af = audioList.get(position);
        holder.titleText.setText(af.title);
        holder.artistText.setText(af.getArtistOrAlbum());
        holder.durationText.setText(af.getDurationFormatted());

        // Highlight selected
        holder.itemView.setSelected(position == selectedPosition);
        holder.itemView.setBackgroundColor(position == selectedPosition ? 0x1A1E40AF : 0x00000000);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(position);
        });
    }

    @Override
    public int getItemCount() {
        return audioList != null ? audioList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, artistText, durationText;

        ViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.tv_title);
            artistText = itemView.findViewById(R.id.tv_artist);
            durationText = itemView.findViewById(R.id.tv_duration);
        }
    }
}
