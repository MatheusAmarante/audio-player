package com.mateus.audioplayer;

import android.net.Uri;

public class AudioFile {
    public long id;
    public String title;
    public String artist;
    public String album;
    public long duration;
    public Uri uri;
    public String displayName;
    public long size;

    public String getDurationFormatted() {
        long secs = duration / 1000;
        long h = secs / 3600;
        long m = (secs % 3600) / 60;
        long s = secs % 60;
        if (h > 0) return String.format("%d:%02d:%02d", h, m, s);
        return String.format("%d:%02d", m, s);
    }

    public String getArtistOrAlbum() {
        if (artist != null && !artist.isEmpty() && !artist.equals("<unknown>")) return artist;
        if (album != null && !album.isEmpty() && !album.equals("<unknown>")) return album;
        return "Unknown";
    }

    public String getSafeTitle() {
        if (title != null && !title.isEmpty()) return title;
        if (displayName != null && !displayName.isEmpty()) return displayName;
        return "Unknown";
    }
}
