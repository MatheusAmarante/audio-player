package com.mateus.audioplayer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class LibraryFragment extends Fragment {

    private RecyclerView recyclerView;
    private EditText searchInput;
    private AudioAdapter adapter;
    private List<AudioFile> allAudio = new ArrayList<>();
    private List<AudioFile> filteredAudio = new ArrayList<>();
    private PlayerManager playerManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);

        playerManager = PlayerManager.getInstance();

        recyclerView = view.findViewById(R.id.recycler_library);
        searchInput = view.findViewById(R.id.search_library);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AudioAdapter(filteredAudio, this::onTrackClick);
        recyclerView.setAdapter(adapter);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { filter(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadAudio();

        return view;
    }

    private void loadAudio() {
        if (getContext() == null) return;
        String perm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ? Manifest.permission.READ_MEDIA_AUDIO
            : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(getContext(), perm) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        new Thread(() -> {
            List<AudioFile> loaded = AudioLoader.loadAllAudio(getContext());
            allAudio = loaded;
            filteredAudio = new ArrayList<>(loaded);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    adapter = new AudioAdapter(filteredAudio, this::onTrackClick);
                    recyclerView.setAdapter(adapter);
                });
            }
        }).start();
    }

    private void filter(String query) {
        filteredAudio.clear();
        if (query.isEmpty()) {
            filteredAudio.addAll(allAudio);
        } else {
            String q = query.toLowerCase();
            for (AudioFile af : allAudio) {
                if (af.getSafeTitle().toLowerCase().contains(q) ||
                    af.getArtistOrAlbum().toLowerCase().contains(q)) {
                    filteredAudio.add(af);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void onTrackClick(int position) {
        if (position < 0 || position >= filteredAudio.size()) return;
        playerManager.setQueue(filteredAudio, position);
        if (getContext() != null) {
            playerManager.play(getContext());
        }
    }

    public void refresh() {
        loadAudio();
    }
}
