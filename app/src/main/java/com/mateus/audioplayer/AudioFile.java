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
    public String folderPath; // for folder-scanned files

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

    /** Serialize to a string for storing in playlist DB */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(uri != null ? uri.toString() : "").append("|||");
        sb.append(title != null ? title : "").append("|||");
        sb.append(artist != null ? artist : "").append("|||");
        sb.append(album != null ? album : "").append("|||");
        sb.append(duration).append("|||");
        sb.append(displayName != null ? displayName : "").append("|||");
        sb.append(size).append("|||");
        sb.append(id);
        return sb.toString();
    }

    /** Deserialize from a string stored in playlist DB */
    public static AudioFile deserialize(String data) {
        if (data == null || data.isEmpty()) return null;
        String[] parts = data.split("\\|\\|\\|", -1);
        if (parts.length < 8) return null;
        AudioFile af = new AudioFile();
        try {
            af.uri = !parts[0].isEmpty() ? Uri.parse(parts[0]) : null;
            af.title = parts[1];
            af.artist = parts[2];
            af.album = parts[3];
            af.duration = Long.parseLong(parts[4]);
            af.displayName = parts[5];
            af.size = Long.parseLong(parts[6]);
            af.id = Long.parseLong(parts[7]);
        } catch (Exception e) {
            return null;
        }
        return af;
    }
}
