package com.mateus.audioplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.ViewHolder> {

    private List<DatabaseHelper.SavedFolder> folders;
    private final OnFolderClickListener listener;

    public interface OnFolderClickListener {
        void onFolderClick(DatabaseHelper.SavedFolder folder);
        void onFolderDelete(DatabaseHelper.SavedFolder folder);
    }

    public FolderAdapter(List<DatabaseHelper.SavedFolder> folders, OnFolderClickListener listener) {
        this.folders = folders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_folder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DatabaseHelper.SavedFolder folder = folders.get(position);
        holder.nameText.setText(folder.name);
        holder.uriText.setText(folder.uri);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onFolderClick(folder);
        });
        holder.deleteBtn.setOnClickListener(v -> {
            if (listener != null) listener.onFolderDelete(folder);
        });
    }

    @Override
    public int getItemCount() {
        return folders != null ? folders.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, uriText;
        View deleteBtn;

        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.tv_folder_name);
            uriText = itemView.findViewById(R.id.tv_folder_uri);
            deleteBtn = itemView.findViewById(R.id.btn_delete_folder);
        }
    }
}
