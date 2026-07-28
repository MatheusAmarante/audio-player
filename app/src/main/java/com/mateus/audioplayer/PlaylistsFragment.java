package com.mateus.audioplayer;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class PlaylistsFragment extends Fragment {

    private RecyclerView recyclerView;
    private FloatingActionButton fabAddPlaylist;
    private PlaylistAdapter adapter;
    private PlaylistManager playlistManager;
    private List<DatabaseHelper.Playlist> playlists = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlists, container, false);

        playlistManager = new PlaylistManager(getContext());

        recyclerView = view.findViewById(R.id.recycler_playlists);
        fabAddPlaylist = view.findViewById(R.id.fab_add_playlist);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PlaylistAdapter(playlists, new PlaylistAdapter.OnPlaylistClickListener() {
            @Override
            public void onPlaylistClick(DatabaseHelper.Playlist playlist) {
                openPlaylist(playlist);
            }

            @Override
            public void onPlaylistLongClick(DatabaseHelper.Playlist playlist) {
                showPlaylistOptions(playlist);
            }
        });
        recyclerView.setAdapter(adapter);

        fabAddPlaylist.setOnClickListener(v -> showCreatePlaylistDialog());

        loadPlaylists();

        return view;
    }

    private void loadPlaylists() {
        playlists.clear();
        playlists.addAll(playlistManager.getAllPlaylists());
        adapter.notifyDataSetChanged();
    }

    private void showCreatePlaylistDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("New Playlist");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Playlist name");
        input.setPadding(48, 32, 48, 32);
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                playlistManager.createPlaylist(name);
                loadPlaylists();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showPlaylistOptions(DatabaseHelper.Playlist playlist) {
        String[] options = {"Open", "Rename", "Delete"};
        new AlertDialog.Builder(getContext())
            .setTitle(playlist.name)
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        openPlaylist(playlist);
                        break;
                    case 1:
                        showRenameDialog(playlist);
                        break;
                    case 2:
                        showDeleteConfirm(playlist);
                        break;
                }
            })
            .show();
    }

    private void showRenameDialog(DatabaseHelper.Playlist playlist) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Rename Playlist");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(playlist.name);
        input.setPadding(48, 32, 48, 32);
        builder.setView(input);

        builder.setPositiveButton("Rename", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                playlistManager.renamePlaylist(playlist.id, name);
                loadPlaylists();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showDeleteConfirm(DatabaseHelper.Playlist playlist) {
        new AlertDialog.Builder(getContext())
            .setTitle("Delete Playlist")
            .setMessage("Delete \"" + playlist.name + "\"? This cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                playlistManager.deletePlaylist(playlist.id);
                loadPlaylists();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void openPlaylist(DatabaseHelper.Playlist playlist) {
        Intent intent = new Intent(getContext(), PlaylistDetailActivity.class);
        intent.putExtra("playlist_id", playlist.id);
        intent.putExtra("playlist_name", playlist.name);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPlaylists();
    }
}
