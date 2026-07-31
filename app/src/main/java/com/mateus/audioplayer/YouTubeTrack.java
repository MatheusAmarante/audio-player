package com.mateus.audioplayer;

/**
 * Represents a YouTube music track from Piped API.
 */
public class YouTubeTrack {
    public String videoId;
    public String title;
    public String artist;
    public long duration; // seconds
    public String thumbnailUrl;
    public String audioStreamUrl; // resolved stream URL

    public String getDurationFormatted() {
        long h = duration / 3600;
        long m = (duration % 3600) / 60;
        long s = duration % 60;
        if (h > 0) return String.format("%d:%02d:%02d", h, m, s);
        return String.format("%d:%02d", m, s);
    }

    public String getArtistOrUploader() {
        if (artist != null && !artist.isEmpty()) return artist;
        return "YouTube";
    }

    /** Serialize for playlist storage */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(videoId != null ? videoId : "").append("|||");
        sb.append(title != null ? title : "").append("|||");
        sb.append(artist != null ? artist : "").append("|||");
        sb.append(duration).append("|||");
        sb.append(thumbnailUrl != null ? thumbnailUrl : "");
        return sb.toString();
    }

    /** Deserialize from playlist storage */
    public static YouTubeTrack deserialize(String data) {
        if (data == null || data.isEmpty()) return null;
        String[] parts = data.split("\\|\\|\\|", -1);
        if (parts.length < 5) return null;
        YouTubeTrack t = new YouTubeTrack();
        t.videoId = parts[0];
        t.title = parts[1];
        t.artist = parts[2];
        try { t.duration = Long.parseLong(parts[3]); } catch (Exception e) { t.duration = 0; }
        t.thumbnailUrl = parts[4];
        return t;
    }
}
