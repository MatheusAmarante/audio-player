package com.mateus.audioplayer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class FoldersFragment extends Fragment {

    private static final int PICK_FOLDER_REQUEST = 200;

    private RecyclerView recyclerView;
    private FloatingActionButton fabAddFolder;
    private FolderAdapter adapter;
    private PlaylistManager playlistManager;
    private List<DatabaseHelper.SavedFolder> folders = new ArrayList<>();
    private ActivityResultLauncher<Intent> folderPickerLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_folders, container, false);

        playlistManager = new PlaylistManager(getContext());

        recyclerView = view.findViewById(R.id.recycler_folders);
        fabAddFolder = view.findViewById(R.id.fab_add_folder);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FolderAdapter(folders, new FolderAdapter.OnFolderClickListener() {
            @Override
            public void onFolderClick(DatabaseHelper.SavedFolder folder) {
                openFolderBrowser(folder);
            }

            @Override
            public void onFolderDelete(DatabaseHelper.SavedFolder folder) {
                playlistManager.removeFolder(folder.id);
                loadFolders();
            }
        });
        recyclerView.setAdapter(adapter);

        fabAddFolder.setOnClickListener(v -> openFolderPicker());

        // Register for SAF folder picker result
        folderPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    Uri treeUri = result.getData().getData();
                    if (treeUri != null) {
                        // Persist permissions
                        getContext().getContentResolver().takePersistableUriPermission(
                            treeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                        String name = treeUri.getLastPathSegment();
                        if (name == null) name = "Folder";
                        playlistManager.saveFolder(treeUri.toString(), name);
                        loadFolders();
                        Toast.makeText(getContext(), "Folder added", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );

        loadFolders();

        return view;
    }

    private void openFolderPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        folderPickerLauncher.launch(intent);
    }

    private void openFolderBrowser(DatabaseHelper.SavedFolder folder) {
        Intent intent = new Intent(getContext(), FolderPickerActivity.class);
        intent.putExtra("folder_uri", folder.uri);
        intent.putExtra("folder_name", folder.name);
        startActivity(intent);
    }

    private void loadFolders() {
        folders.clear();
        folders.addAll(playlistManager.getSavedFolders());
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFolders();
    }
}
