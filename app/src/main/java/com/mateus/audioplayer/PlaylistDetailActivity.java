package com.mateus.audioplayer;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class PlaylistDetailActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView emptyText;
    private AudioAdapter adapter;
    private PlaylistManager playlistManager;
    private PlayerManager playerManager;
    private long playlistId;
    private String playlistName;
    private List<AudioFile> tracks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        playlistManager = new PlaylistManager(this);
        playerManager = PlayerManager.getInstance();

        playlistId = getIntent().getLongExtra("playlist_id", -1);
        playlistName = getIntent().getStringExtra("playlist_name");

        TextView titleView = findViewById(R.id.playlist_detail_title);
        titleView.setText(playlistName != null ? playlistName : "Playlist");

        recyclerView = findViewById(R.id.recycler_playlist_tracks);
        emptyText = findViewById(R.id.playlist_empty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AudioAdapter(tracks, this::onTrackClick);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.playlist_detail_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_add_tracks).setOnClickListener(v -> showAddTracksDialog());

        loadTracks();
    }

    private void loadTracks() {
        tracks.clear();
        tracks.addAll(playlistManager.getTracks(playlistId));
        adapter.notifyDataSetChanged();

        if (tracks.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void onTrackClick(int position) {
        if (position < 0 || position >= tracks.size()) return;
        playerManager.setQueue(tracks, position);
        playerManager.play(this);
    }

    private void showAddTracksDialog() {
        // Load all audio from MediaStore for selection
        List<AudioFile> allAudio = AudioLoader.loadAllAudio(this);
        if (allAudio.isEmpty()) {
            Toast.makeText(this, "No audio files found", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] titles = new String[allAudio.size()];
        boolean[] checked = new boolean[allAudio.size()];
        for (int i = 0; i < allAudio.size(); i++) {
            titles[i] = allAudio.get(i).getSafeTitle() + " - " + allAudio.get(i).getArtistOrAlbum();
        }

        new AlertDialog.Builder(this)
            .setTitle("Add Tracks to " + playlistName)
            .setMultiChoiceItems(titles, checked, (dialog, which, isChecked) -> {
                checked[which] = isChecked;
            })
            .setPositiveButton("Add", (dialog, which) -> {
                int added = 0;
                for (int i = 0; i < checked.length; i++) {
                    if (checked[i]) {
                        playlistManager.addTrack(playlistId, allAudio.get(i));
                        added++;
                    }
                }
                if (added > 0) {
                    Toast.makeText(this, "Added " + added + " tracks", Toast.LENGTH_SHORT).show();
                    loadTracks();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTracks();
    }
}
