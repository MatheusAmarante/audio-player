package com.mateus.audioplayer;

import android.os.Bundle;
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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private RecyclerView recyclerView;
    private EditText searchInput;
    private ProgressBar progressBar;
    private TextView emptyText;
    private SearchAdapter adapter;
    private List<YouTubeTrack> results = new ArrayList<>();
    private PlayerManager playerManager;
    private PipedApiClient apiClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        playerManager = PlayerManager.getInstance();
        apiClient = new PipedApiClient(getContext());

        // Resolve proxy URL (local WiFi or Cloudflare tunnel)
        apiClient.resolveBaseUrl(() -> {});

        recyclerView = view.findViewById(R.id.recycler_search);
        searchInput = view.findViewById(R.id.search_input);
        progressBar = view.findViewById(R.id.search_progress);
        emptyText = view.findViewById(R.id.search_empty);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SearchAdapter(results, new SearchAdapter.OnTrackClickListener() {
            @Override
            public void onTrackClick(int position) {
                playerManager.setQueue(results, position);
                if (getContext() != null) {
                    playerManager.play(getContext());
                }
            }

            @Override
            public void onAddToPlaylist(YouTubeTrack track) {
                showAddToPlaylistDialog(track);
            }
        });
        recyclerView.setAdapter(adapter);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                String query = s.toString().trim();
                if (query.length() >= 2) {
                    performSearch(query);
                } else {
                    results.clear();
                    adapter.notifyDataSetChanged();
                    emptyText.setVisibility(View.VISIBLE);
                    emptyText.setText("Search for music on YouTube");
                }
            }
        });

        return view;
    }

    private void performSearch(String query) {
        progressBar.setVisibility(View.VISIBLE);
        emptyText.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        apiClient.search(query, new PipedApiClient.SearchCallback() {
            @Override
            public void onResults(List<YouTubeTrack> tracks) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    results.clear();
                    results.addAll(tracks);
                    adapter.notifyDataSetChanged();
                    if (results.isEmpty()) {
                        emptyText.setVisibility(View.VISIBLE);
                        emptyText.setText("No results found");
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        emptyText.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onError(String message) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    emptyText.setVisibility(View.VISIBLE);
                    emptyText.setText("Error: " + message);
                    recyclerView.setVisibility(View.GONE);
                });
            }
        });
    }

    private void showAddToPlaylistDialog(YouTubeTrack track) {
        if (getContext() == null) return;
        PlaylistManager pm = new PlaylistManager(getContext());
        List<DatabaseHelper.Playlist> playlists = pm.getAllPlaylists();

        if (playlists.isEmpty()) {
            Toast.makeText(getContext(), "Create a playlist first", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[playlists.size()];
        for (int i = 0; i < playlists.size(); i++) {
            names[i] = playlists.get(i).name;
        }

        new android.app.AlertDialog.Builder(getContext())
            .setTitle("Add to playlist")
            .setItems(names, (dialog, which) -> {
                pm.addTrack(playlists.get(which).id, track);
                Toast.makeText(getContext(), "Added to " + playlists.get(which).name, Toast.LENGTH_SHORT).show();
            })
            .show();
    }
}
