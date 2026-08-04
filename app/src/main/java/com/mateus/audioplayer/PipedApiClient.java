package com.mateus.audioplayer;

import android.content.Context;
import android.content.SharedPreferences;
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
 * Tries local IP first (WiFi), falls back to Cloudflare Tunnel (4G).
 */
public class PipedApiClient {

    // Local proxy on the Pi 2 (WiFi)
    private static final String PROXY_LOCAL = "http://192.168.0.5:5000";
    // Cloudflare Tunnel URL (updated automatically)
    private static final String PREF_TUNNEL_URL = "proxy_tunnel_url";
    private static final String DEFAULT_TUNNEL = "https://below-removal-conservative-benz.trycloudflare.com";

    private final OkHttpClient client;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final SharedPreferences prefs;
    private String activeBaseUrl;

    public interface SearchCallback {
        void onResults(List<YouTubeTrack> tracks);
        void onError(String message);
    }

    public PipedApiClient(Context context) {
        prefs = context.getSharedPreferences("yt_proxy", Context.MODE_PRIVATE);
        client = new OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    }

    /** Resolve which proxy URL to use. Call once before search/stream. */
    public void resolveBaseUrl(Runnable onReady) {
        executor.execute(() -> {
            // Try local first
            try {
                Request r = new Request.Builder().url(PROXY_LOCAL + "/health").build();
                Response resp = client.newCall(r).execute();
                if (resp.isSuccessful()) {
                    // Local works, fetch tunnel URL for later
                    activeBaseUrl = PROXY_LOCAL;
                    try {
                        Request cr = new Request.Builder().url(PROXY_LOCAL + "/config").build();
                        Response cresp = client.newCall(cr).execute();
                        if (cresp.isSuccessful()) {
                            JSONObject cfg = new JSONObject(cresp.body().string());
                            String tunnel = cfg.optString("tunnel_url", "");
                            if (!tunnel.isEmpty()) {
                                prefs.edit().putString(PREF_TUNNEL_URL, tunnel).apply();
                            }
                        }
                    } catch (Exception ignored) {}
                    mainHandler.post(onReady);
                    return;
                }
            } catch (Exception ignored) {}

            // Local failed, try saved tunnel URL
            String tunnel = prefs.getString(PREF_TUNNEL_URL, DEFAULT_TUNNEL);
            try {
                Request r = new Request.Builder().url(tunnel + "/health").build();
                Response resp = client.newCall(r).execute();
                if (resp.isSuccessful()) {
                    activeBaseUrl = tunnel;
                    mainHandler.post(onReady);
                    return;
                }
            } catch (Exception ignored) {}

            // Last resort: default tunnel
            activeBaseUrl = DEFAULT_TUNNEL;
            mainHandler.post(onReady);
        });
    }

    /** Search for music on YouTube via the Pi proxy */
    public void search(String query, SearchCallback callback) {
        executor.execute(() -> {
            try {
                String url = activeBaseUrl + "/search?q=" +
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
                String url = activeBaseUrl + "/stream?id=" + videoId;
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

    /** Fetch trending music from the proxy */
    public void fetchTrending(SearchCallback callback) {
        executor.execute(() -> {
            try {
                String url = activeBaseUrl + "/trending";
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

    /** Fetch synced lyrics from the proxy */
    public void fetchLyrics(String title, String artist, LyricsCallback callback) {
        executor.execute(() -> {
            try {
                String url = activeBaseUrl + "/lyrics?title=" +
                    java.net.URLEncoder.encode(title, "UTF-8") +
                    "&artist=" + java.net.URLEncoder.encode(artist != null ? artist : "", "UTF-8");
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onError("HTTP " + response.code()));
                    return;
                }
                String body = response.body().string();
                JSONObject json = new JSONObject(body);
                String synced = json.optString("synced", "");
                String plain = json.optString("plain", "");
                mainHandler.post(() -> callback.onLyrics(synced, plain));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /** Get download URL for a track */
    public String getDownloadUrl(String videoId) {
        return activeBaseUrl + "/download/" + videoId;
    }

    public interface LyricsCallback {
        void onLyrics(String synced, String plain);
        void onError(String message);
    }
}
