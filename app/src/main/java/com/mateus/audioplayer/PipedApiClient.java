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
 * Communicates with the yt-dlp proxy running on the Pi 2.
 * The proxy handles YouTube search and stream URL resolution.
 */
public class PipedApiClient {

    // Local proxy on the Pi 2
    private static final String PROXY_BASE = "http://192.168.0.5:5000";

    private final OkHttpClient client;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface SearchCallback {
        void onResults(List<YouTubeTrack> tracks);
        void onError(String message);
    }

    public PipedApiClient() {
        client = new OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    }

    /** Search for music on YouTube via the Pi proxy */
    public void search(String query, SearchCallback callback) {
        executor.execute(() -> {
            try {
                String url = PROXY_BASE + "/search?q=" +
                    java.net.URLEncoder.encode(query, "UTF-8");
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onError("HTTP " + response.code()));
                    return;
                }
                String body = response.body().string();
                JSONArray items = new JSONArray(body);
                List<YouTubeTrack> tracks = new ArrayList<>();
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    YouTubeTrack track = new YouTubeTrack();
                    track.videoId = item.optString("videoId", "");
                    if (track.videoId.isEmpty()) continue;
                    track.title = item.optString("title", "Unknown");
                    track.artist = item.optString("artist", "");
                    track.duration = item.optLong("duration", 0);
                    track.thumbnailUrl = item.optString("thumbnailUrl", "");
                    tracks.add(track);
                }
                mainHandler.post(() -> callback.onResults(tracks));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /** Get audio stream URL for a video via the Pi proxy */
    public void getStreamUrl(String videoId, StreamCallback callback) {
        executor.execute(() -> {
            try {
                String url = PROXY_BASE + "/stream?id=" + videoId;
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onError("HTTP " + response.code()));
                    return;
                }
                String body = response.body().string();
                JSONObject json = new JSONObject(body);
                String streamUrl = json.optString("url", "");
                if (!streamUrl.isEmpty()) {
                    mainHandler.post(() -> callback.onStreamUrl(streamUrl));
                } else {
                    mainHandler.post(() -> callback.onError(json.optString("error", "No stream URL")));
                }
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
