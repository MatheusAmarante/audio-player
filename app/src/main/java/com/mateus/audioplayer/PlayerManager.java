package com.mateus.audioplayer;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Singleton player manager using ExoPlayer for YouTube audio streaming.
 */
public class PlayerManager {

    private static PlayerManager instance;

    private ExoPlayer exoPlayer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;

    private List<YouTubeTrack> queue = new ArrayList<>();
    private int currentIndex = -1;
    private boolean isShuffle = false;
    private int repeatMode = 0; // 0=none, 1=one, 2=all
    private float speed = 1.0f;
    private boolean isUserSeeking = false;

    // Sleep timer
    private long sleepEndMillis = -1;
    private final Handler sleepHandler = new Handler(Looper.getMainLooper());
    private Runnable sleepRunnable;

    // Callbacks
    private final List<PlayerCallback> callbacks = new ArrayList<>();

    public interface PlayerCallback {
        void onTrackChanged(YouTubeTrack track);
        void onPlayStateChanged(boolean isPlaying);
        void onProgressChanged(int position, int duration);
        void onSpeedChanged(float speed);
        void onSleepTimerChanged(int remainingSeconds);
    }

    private PlayerManager() {}

    public static synchronized PlayerManager getInstance() {
        if (instance == null) {
            instance = new PlayerManager();
        }
        return instance;
    }

    public void addCallback(PlayerCallback cb) {
        if (!callbacks.contains(cb)) callbacks.add(cb);
    }

    public void removeCallback(PlayerCallback cb) {
        callbacks.remove(cb);
    }

    private void notifyTrackChanged() {
        YouTubeTrack track = getCurrentTrack();
        for (PlayerCallback cb : callbacks) cb.onTrackChanged(track);
    }

    private void notifyPlayState(boolean playing) {
        for (PlayerCallback cb : callbacks) cb.onPlayStateChanged(playing);
    }

    private void notifyProgress(int pos, int dur) {
        for (PlayerCallback cb : callbacks) cb.onProgressChanged(pos, dur);
    }

    private void notifySpeed(float s) {
        for (PlayerCallback cb : callbacks) cb.onSpeedChanged(s);
    }

    private void notifySleepTimer(int remaining) {
        for (PlayerCallback cb : callbacks) cb.onSleepTimerChanged(remaining);
    }

    // ==================== QUEUE ====================

    public void setQueue(List<YouTubeTrack> tracks, int startIndex) {
        queue.clear();
        if (tracks != null) queue.addAll(tracks);
        currentIndex = (startIndex >= 0 && startIndex < queue.size()) ? startIndex : -1;
    }

    public List<YouTubeTrack> getQueue() { return queue; }
    public int getCurrentIndex() { return currentIndex; }

    public YouTubeTrack getCurrentTrack() {
        if (currentIndex >= 0 && currentIndex < queue.size()) {
            return queue.get(currentIndex);
        }
        return null;
    }

    public boolean isShuffle() { return isShuffle; }
    public void setShuffle(boolean s) { isShuffle = s; }

    public int getRepeatMode() { return repeatMode; }
    public void setRepeatMode(int mode) { repeatMode = mode % 3; }

    public float getSpeed() { return speed; }

    // ==================== PLAYBACK ====================

    public void play(Context context) {
        YouTubeTrack track = getCurrentTrack();
        if (track == null) return;

        if (exoPlayer == null) {
            exoPlayer = new ExoPlayer.Builder(context).build();
            exoPlayer.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_ENDED) {
                        onTrackComplete();
                    }
                }

                @Override
                public void onPlayerError(PlaybackException error) {
                    next();
                    play(context);
                }
            });
        }

        // Resolve stream URL and play
        PipedApiClient client = new PipedApiClient();
        client.getStreamUrl(track.videoId, new PipedApiClient.StreamCallback() {
            @Override
            public void onStreamUrl(String url) {
                track.audioStreamUrl = url;
                MediaItem mediaItem = MediaItem.fromUri(url);
                exoPlayer.setMediaItem(mediaItem);
                exoPlayer.setPlaybackSpeed(speed);
                exoPlayer.prepare();
                exoPlayer.play();
                notifyTrackChanged();
                notifyPlayState(true);
                startProgressUpdates();
            }

            @Override
            public void onError(String message) {
                // Try next track
                next();
                play(context);
            }
        });
    }

    public void togglePlayPause(Context context) {
        if (exoPlayer == null) {
            if (currentIndex < 0 && !queue.isEmpty()) {
                currentIndex = 0;
            }
            play(context);
            return;
        }
        if (exoPlayer.isPlaying()) {
            exoPlayer.pause();
            notifyPlayState(false);
        } else {
            exoPlayer.play();
            notifyPlayState(true);
            startProgressUpdates();
        }
    }

    public void pause() {
        if (exoPlayer != null && exoPlayer.isPlaying()) {
            exoPlayer.pause();
            notifyPlayState(false);
        }
    }

    public boolean isPlaying() {
        return exoPlayer != null && exoPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        return exoPlayer != null ? (int) exoPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return exoPlayer != null ? (int) exoPlayer.getDuration() : 0;
    }

    public void seekTo(int ms) {
        if (exoPlayer != null) {
            exoPlayer.seekTo(ms);
        }
    }

    public void next() {
        if (queue.isEmpty()) return;
        if (isShuffle) {
            currentIndex = new Random().nextInt(queue.size());
        } else {
            currentIndex = (currentIndex + 1) % queue.size();
        }
    }

    public void previous() {
        if (queue.isEmpty()) return;
        if (exoPlayer != null && exoPlayer.getCurrentPosition() > 3000) {
            exoPlayer.seekTo(0);
            return;
        }
        currentIndex = currentIndex > 0 ? currentIndex - 1 : queue.size() - 1;
    }

    public void playIndex(Context context, int index) {
        if (index < 0 || index >= queue.size()) return;
        currentIndex = index;
        play(context);
    }

    private void onTrackComplete() {
        if (repeatMode == 1) {
            if (exoPlayer != null) {
                exoPlayer.seekTo(0);
                exoPlayer.play();
            }
        } else if (repeatMode == 2 || isShuffle) {
            next();
        } else if (currentIndex < queue.size() - 1) {
            next();
        } else {
            pause();
            notifyPlayState(false);
        }
    }

    // ==================== SPEED ====================

    public void setSpeed(float s) {
        speed = s;
        if (exoPlayer != null) {
            exoPlayer.setPlaybackSpeed(s);
        }
        notifySpeed(s);
    }

    // ==================== SLEEP TIMER ====================

    public void setSleepTimer(int minutes) {
        sleepHandler.removeCallbacks(sleepRunnable);
        if (minutes <= 0) {
            sleepEndMillis = -1;
            notifySleepTimer(-1);
            return;
        }
        sleepEndMillis = System.currentTimeMillis() + minutes * 60L * 1000L;
        tickSleep();
    }

    public long getSleepEndMillis() { return sleepEndMillis; }

    private void tickSleep() {
        if (sleepEndMillis < 0) return;
        long remaining = sleepEndMillis - System.currentTimeMillis();
        if (remaining <= 0) {
            pause();
            sleepEndMillis = -1;
            notifySleepTimer(0);
            return;
        }
        notifySleepTimer((int) (remaining / 1000));
        sleepRunnable = this::tickSleep;
        sleepHandler.postDelayed(sleepRunnable, 1000);
    }

    // ==================== PROGRESS ====================

    private void startProgressUpdates() {
        handler.removeCallbacks(progressRunnable);
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (exoPlayer != null && exoPlayer.isPlaying() && !isUserSeeking) {
                    int pos = (int) exoPlayer.getCurrentPosition();
                    int dur = (int) exoPlayer.getDuration();
                    notifyProgress(pos, dur);
                }
                handler.postDelayed(this, 250);
            }
        };
        handler.post(progressRunnable);
    }

    public void setUserSeeking(boolean seeking) {
        isUserSeeking = seeking;
    }

    // ==================== CLEANUP ====================

    public void release() {
        handler.removeCallbacks(progressRunnable);
        sleepHandler.removeCallbacks(sleepRunnable);
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
        callbacks.clear();
    }
}
