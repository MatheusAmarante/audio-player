package com.mateus.audioplayer;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class FolderPickerActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView emptyText;
    private ProgressBar progressBar;
    private AudioAdapter adapter;
    private List<AudioFile> folderAudio = new ArrayList<>();
    private PlayerManager playerManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_picker);

        playerManager = PlayerManager.getInstance();

        String folderUri = getIntent().getStringExtra("folder_uri");
        String folderName = getIntent().getStringExtra("folder_name");

        TextView titleView = findViewById(R.id.folder_title);
        titleView.setText(folderName != null ? folderName : "Folder");

        recyclerView = findViewById(R.id.recycler_folder_files);
        emptyText = findViewById(R.id.folder_empty);
        progressBar = findViewById(R.id.folder_progress);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AudioAdapter(folderAudio, this::onTrackClick);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.folder_back).setOnClickListener(v -> finish());

        if (folderUri != null) {
            scanFolder(Uri.parse(folderUri));
        }
    }

    private void scanFolder(Uri folderUri) {
        progressBar.setVisibility(View.VISIBLE);
        emptyText.setVisibility(View.GONE);

        new Thread(() -> {
            List<AudioFile> files = AudioLoader.scanFolder(this, folderUri);
            folderAudio.clear();
            folderAudio.addAll(files);

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (folderAudio.isEmpty()) {
                    emptyText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyText.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                }
            });
        }).start();
    }

    private void onTrackClick(int position) {
        if (position < 0 || position >= folderAudio.size()) return;
        playerManager.setQueue(folderAudio, position);
        playerManager.play(this);
    }
}
