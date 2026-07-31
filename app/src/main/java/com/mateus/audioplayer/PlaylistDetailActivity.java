package com.mateus.audioplayer;

import android.content.Intent;
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
    private TextView titleView, emptyView;
    private PlaylistManager playlistManager;
    private PlayerManager playerManager;
    private long playlistId;
    private String playlistName;
    private List<YouTubeTrack> tracks = new ArrayList<>();
    private SearchAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        playlistId = getIntent().getLongExtra("playlist_id", -1);
        playlistName = getIntent().getStringExtra("playlist_name");
        if (playlistId < 0) {
            finish();
            return;
        }

        playlistManager = new PlaylistManager(this);
        playerManager = PlayerManager.getInstance();

        titleView = findViewById(R.id.playlist_detail_title);
        emptyView = findViewById(R.id.playlist_detail_empty);
        recyclerView = findViewById(R.id.recycler_playlist_detail);

        titleView.setText(playlistName != null ? playlistName : "Playlist");

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SearchAdapter(tracks, new SearchAdapter.OnTrackClickListener() {
            @Override
            public void onTrackClick(int position) {
                playerManager.setQueue(tracks, position);
                playerManager.play(PlaylistDetailActivity.this);
            }

            @Override
            public void onAddToPlaylist(YouTubeTrack track) {
                // Already in a playlist, show remove option
                Toast.makeText(PlaylistDetailActivity.this, "Long press to remove", Toast.LENGTH_SHORT).show();
            }
        });
        recyclerView.setAdapter(adapter);

        // Back button
        findViewById(R.id.playlist_detail_back).setOnClickListener(v -> finish());

        loadTracks();
    }

    private void loadTracks() {
        tracks.clear();
        tracks.addAll(playlistManager.getTracks(playlistId));
        adapter.notifyDataSetChanged();

        if (tracks.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTracks();
    }
}
