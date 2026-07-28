package com.mateus.audioplayer;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages playlist CRUD operations using DatabaseHelper.
 */
public class PlaylistManager {

    private final DatabaseHelper dbHelper;

    public PlaylistManager(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    public DatabaseHelper getDbHelper() {
        return dbHelper;
    }

    public long createPlaylist(String name) {
        return dbHelper.createPlaylist(name);
    }

    public void renamePlaylist(long id, String newName) {
        dbHelper.renamePlaylist(id, newName);
    }

    public void deletePlaylist(long id) {
        dbHelper.deletePlaylist(id);
    }

    public List<DatabaseHelper.Playlist> getAllPlaylists() {
        return dbHelper.getAllPlaylists();
    }

    public DatabaseHelper.Playlist getPlaylist(long id) {
        return dbHelper.getPlaylist(id);
    }

    public long addTrack(long playlistId, AudioFile track) {
        return dbHelper.addTrackToPlaylist(playlistId, track);
    }

    public void removeTrack(long trackDbId) {
        dbHelper.removeTrackFromPlaylist(trackDbId);
    }

    public List<AudioFile> getTracks(long playlistId) {
        return dbHelper.getTracksForPlaylist(playlistId);
    }

    public long saveFolder(String uri, String name) {
        return dbHelper.saveFolder(uri, name);
    }

    public void removeFolder(long folderId) {
        dbHelper.removeFolder(folderId);
    }

    public List<DatabaseHelper.SavedFolder> getSavedFolders() {
        return dbHelper.getSavedFolders();
    }
}
