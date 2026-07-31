package com.mateus.audioplayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "audioplayer.db";
    private static final int DB_VERSION = 2;

    // Playlists table
    private static final String TABLE_PLAYLISTS = "playlists";
    private static final String COL_PLAYLIST_ID = "id";
    private static final String COL_PLAYLIST_NAME = "name";
    private static final String COL_PLAYLIST_CREATED = "created_at";

    // Playlist tracks table (stores serialized YouTubeTrack)
    private static final String TABLE_TRACKS = "playlist_tracks";
    private static final String COL_TRACK_ID = "id";
    private static final String COL_TRACK_PLAYLIST_ID = "playlist_id";
    private static final String COL_TRACK_DATA = "track_data";
    private static final String COL_TRACK_ORDER = "track_order";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_PLAYLISTS + " (" +
            COL_PLAYLIST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_PLAYLIST_NAME + " TEXT NOT NULL, " +
            COL_PLAYLIST_CREATED + " INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_TRACKS + " (" +
            COL_TRACK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_TRACK_PLAYLIST_ID + " INTEGER NOT NULL, " +
            COL_TRACK_DATA + " TEXT NOT NULL, " +
            COL_TRACK_ORDER + " INTEGER DEFAULT 0, " +
            "FOREIGN KEY(" + COL_TRACK_PLAYLIST_ID + ") REFERENCES " + TABLE_PLAYLISTS + "(" + COL_PLAYLIST_ID + ") ON DELETE CASCADE)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRACKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLAYLISTS);
        onCreate(db);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    // ==================== PLAYLISTS ====================

    public long createPlaylist(String name) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_PLAYLIST_NAME, name);
        cv.put(COL_PLAYLIST_CREATED, System.currentTimeMillis());
        return db.insert(TABLE_PLAYLISTS, null, cv);
    }

    public void renamePlaylist(long playlistId, String newName) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_PLAYLIST_NAME, newName);
        db.update(TABLE_PLAYLISTS, cv, COL_PLAYLIST_ID + "=?", new String[]{String.valueOf(playlistId)});
    }

    public void deletePlaylist(long playlistId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_TRACKS, COL_TRACK_PLAYLIST_ID + "=?", new String[]{String.valueOf(playlistId)});
        db.delete(TABLE_PLAYLISTS, COL_PLAYLIST_ID + "=?", new String[]{String.valueOf(playlistId)});
    }

    public List<Playlist> getAllPlaylists() {
        List<Playlist> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_PLAYLISTS, null, null, null, null, null, COL_PLAYLIST_NAME + " ASC");
        if (c != null) {
            while (c.moveToNext()) {
                Playlist p = new Playlist();
                p.id = c.getLong(c.getColumnIndex(COL_PLAYLIST_ID));
                p.name = c.getString(c.getColumnIndex(COL_PLAYLIST_NAME));
                p.createdAt = c.getLong(c.getColumnIndex(COL_PLAYLIST_CREATED));
                p.trackCount = getTrackCount(db, p.id);
                list.add(p);
            }
            c.close();
        }
        return list;
    }

    public Playlist getPlaylist(long playlistId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_PLAYLISTS, null, COL_PLAYLIST_ID + "=?",
            new String[]{String.valueOf(playlistId)}, null, null, null);
        if (c != null && c.moveToFirst()) {
            Playlist p = new Playlist();
            p.id = c.getLong(c.getColumnIndex(COL_PLAYLIST_ID));
            p.name = c.getString(c.getColumnIndex(COL_PLAYLIST_NAME));
            p.createdAt = c.getLong(c.getColumnIndex(COL_PLAYLIST_CREATED));
            p.trackCount = getTrackCount(db, p.id);
            c.close();
            return p;
        }
        if (c != null) c.close();
        return null;
    }

    private int getTrackCount(SQLiteDatabase db, long playlistId) {
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_TRACKS +
            " WHERE " + COL_TRACK_PLAYLIST_ID + "=?", new String[]{String.valueOf(playlistId)});
        int count = 0;
        if (c != null && c.moveToFirst()) {
            count = c.getInt(0);
            c.close();
        }
        return count;
    }

    // ==================== TRACKS ====================

    public long addTrackToPlaylist(long playlistId, YouTubeTrack track) {
        SQLiteDatabase db = getWritableDatabase();
        int nextOrder = getNextOrder(db, playlistId);
        ContentValues cv = new ContentValues();
        cv.put(COL_TRACK_PLAYLIST_ID, playlistId);
        cv.put(COL_TRACK_DATA, track.serialize());
        cv.put(COL_TRACK_ORDER, nextOrder);
        return db.insert(TABLE_TRACKS, null, cv);
    }

    public void removeTrackFromPlaylist(long trackId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_TRACKS, COL_TRACK_ID + "=?", new String[]{String.valueOf(trackId)});
    }

    public List<YouTubeTrack> getTracksForPlaylist(long playlistId) {
        List<YouTubeTrack> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_TRACKS, null, COL_TRACK_PLAYLIST_ID + "=?",
            new String[]{String.valueOf(playlistId)}, null, null, COL_TRACK_ORDER + " ASC");
        if (c != null) {
            while (c.moveToNext()) {
                String data = c.getString(c.getColumnIndex(COL_TRACK_DATA));
                YouTubeTrack track = YouTubeTrack.deserialize(data);
                if (track != null) {
                    list.add(track);
                }
            }
            c.close();
        }
        return list;
    }

    private int getNextOrder(SQLiteDatabase db, long playlistId) {
        Cursor c = db.rawQuery("SELECT MAX(" + COL_TRACK_ORDER + ") FROM " + TABLE_TRACKS +
            " WHERE " + COL_TRACK_PLAYLIST_ID + "=?", new String[]{String.valueOf(playlistId)});
        int max = 0;
        if (c != null && c.moveToFirst()) {
            max = c.getInt(0);
            c.close();
        }
        return max + 1;
    }

    // ==================== MODEL CLASSES ====================

    public static class Playlist {
        public long id;
        public String name;
        public long createdAt;
        public int trackCount;
    }
}
