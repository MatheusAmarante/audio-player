package com.mateus.audioplayer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyText;
    private GridAdapter adapter;
    private List<YouTubeTrack> trending = new ArrayList<>();
    private PlayerManager playerManager;
    private PipedApiClient apiClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        playerManager = PlayerManager.getInstance();
        apiClient = new PipedApiClient(getContext());

        recyclerView = view.findViewById(R.id.recycler_home);
        progressBar = view.findViewById(R.id.home_progress);
        emptyText = view.findViewById(R.id.home_empty);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new GridAdapter(trending, position -> {
            playerManager.setQueue(trending, position);
            if (getContext() != null) {
                playerManager.play(getContext());
            }
        });
        recyclerView.setAdapter(adapter);

        loadTrending();

        return view;
    }

    private void loadTrending() {
        progressBar.setVisibility(View.VISIBLE);
        emptyText.setVisibility(View.GONE);

        apiClient.resolveBaseUrl(() -> {
            apiClient.fetchTrending(new PipedApiClient.SearchCallback() {
                @Override
                public void onResults(List<YouTubeTrack> tracks) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        trending.clear();
                        trending.addAll(tracks);
                        adapter.notifyDataSetChanged();
                        if (trending.isEmpty()) {
                            emptyText.setVisibility(View.VISIBLE);
                            emptyText.setText("No trending music found");
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
                    });
                }
            });
        });
    }

    public void refresh() {
        loadTrending();
    }
}
