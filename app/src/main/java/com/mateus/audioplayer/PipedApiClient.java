package com.mateus.audioplayer;

import android.os.Handler;
import android.os.Looper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Searches YouTube music via Piped API (no API key needed).
 * Piped is an open-source YouTube frontend that provides a REST API.
 */
public class PipedApiClient {

    // Use a reliable Piped instance
    private static final String PIPED_BASE = "https://pipedapi.kavin.rocks";

    private final OkHttpClient client;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface SearchCallback {
        void onResults(List<YouTubeTrack> tracks);
        void onError(String message);
    }

    public PipedApiClient() {
        client = new OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    }

    /** Search for music on YouTube via Piped */
    public void search(String query, SearchCallback callback) {
        executor.execute(() -> {
            try {
                String url = PIPED_BASE + "/search?q=" +
                    java.net.URLEncoder.encode(query, "UTF-8") +
                    "&filter=music_songs";
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onError("HTTP " + response.code()));
                    return;
                }
                String body = response.body().string();
                JSONObject json = new JSONObject(body);
                JSONArray items = json.getJSONArray("items");
                List<YouTubeTrack> tracks = new ArrayList<>();
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    YouTubeTrack track = new YouTubeTrack();
                    track.videoId = item.optString("url", "").replace("/watch?v=", "");
                    if (track.videoId.isEmpty()) continue;
                    track.title = item.optString("title", "Unknown");
                    track.artist = item.optString("uploaderName", "");
                    track.duration = item.optLong("duration", 0);
                    track.thumbnailUrl = item.optString("thumbnail", "");
                    // Piped provides audio stream URL via /streams endpoint
                    track.audioStreamUrl = PIPED_BASE + "/streams/" + track.videoId;
                    tracks.add(track);
                }
                mainHandler.post(() -> callback.onResults(tracks));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /** Get audio stream URL for a video */
    public void getStreamUrl(String videoId, StreamCallback callback) {
        executor.execute(() -> {
            try {
                String url = PIPED_BASE + "/streams/" + videoId;
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onError("HTTP " + response.code()));
                    return;
                }
                String body = response.body().string();
                JSONObject json = new JSONObject(body);
                JSONArray audioStreams = json.optJSONArray("audioStreams");
                if (audioStreams != null && audioStreams.length() > 0) {
                    // Get the best quality audio stream
                    String streamUrl = audioStreams.getJSONObject(0).optString("url", "");
                    if (!streamUrl.isEmpty()) {
                        mainHandler.post(() -> callback.onStreamUrl(streamUrl));
                        return;
                    }
                }
                // Fallback: try video streams
                JSONArray videoStreams = json.optJSONArray("videoStreams");
                if (videoStreams != null && videoStreams.length() > 0) {
                    String streamUrl = videoStreams.getJSONObject(0).optString("url", "");
                    if (!streamUrl.isEmpty()) {
                        mainHandler.post(() -> callback.onStreamUrl(streamUrl));
                        return;
                    }
                }
                mainHandler.post(() -> callback.onError("No audio stream found"));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public interface StreamCallback {
        void onStreamUrl(String url);
        void onError(String message);
    }
}
