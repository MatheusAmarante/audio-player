package com.mateus.audioplayer;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Singleton that manages audio playback across the app.
 */
public class PlayerManager {

    private static PlayerManager instance;

    private MediaPlayer mediaPlayer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;

    private List<AudioFile> queue = new ArrayList<>();
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
        void onTrackChanged(AudioFile track);
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
        AudioFile track = getCurrentTrack();
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

    public void setQueue(List<AudioFile> tracks, int startIndex) {
        queue.clear();
        if (tracks != null) queue.addAll(tracks);
        currentIndex = (startIndex >= 0 && startIndex < queue.size()) ? startIndex : -1;
    }

    public List<AudioFile> getQueue() {
        return queue;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public AudioFile getCurrentTrack() {
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
        AudioFile track = getCurrentTrack();
        if (track == null || track.uri == null) return;

        if (mediaPlayer != null) {
            try { mediaPlayer.reset(); } catch (Exception ignored) {}
        } else {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA).build());
            mediaPlayer.setOnCompletionListener(mp -> onTrackComplete());
            mediaPlayer.setOnErrorListener((mp, w, e) -> { next(); return true; });
        }

        try {
            mediaPlayer.setDataSource(context, track.uri);
            mediaPlayer.setOnPreparedListener(mp -> {
                try { mp.setPlaybackParams(mp.getPlaybackParams().setSpeed(speed)); } catch (Exception ignored) {}
                mp.start();
                notifyTrackChanged();
                notifyPlayState(true);
                startProgressUpdates();
            });
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
            next();
        }
    }

    public void togglePlayPause(Context context) {
        if (mediaPlayer == null) {
            if (currentIndex < 0 && !queue.isEmpty()) {
                currentIndex = 0;
            }
            play(context);
            return;
        }
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            notifyPlayState(false);
        } else {
            mediaPlayer.start();
            notifyPlayState(true);
            startProgressUpdates();
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            notifyPlayState(false);
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

    public void seekTo(int ms) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(ms);
        }
    }

    public void next() {
        if (queue.isEmpty()) return;
        if (isShuffle) {
            currentIndex = new Random().nextInt(queue.size());
        } else {
            currentIndex = (currentIndex + 1) % queue.size();
        }
        // We need context to play; caller should call play(context) after
    }

    public void previous() {
        if (queue.isEmpty()) return;
        if (mediaPlayer != null && mediaPlayer.getCurrentPosition() > 3000) {
            mediaPlayer.seekTo(0);
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
            // repeat one - just play again
            if (mediaPlayer != null) {
                mediaPlayer.seekTo(0);
                mediaPlayer.start();
            }
        } else if (repeatMode == 2 || isShuffle) {
            next();
            // need context - we'll use a stored context
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
        if (mediaPlayer != null) {
            try {
                mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(s));
            } catch (Exception ignored) {}
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

    public long getSleepEndMillis() {
        return sleepEndMillis;
    }

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
                if (mediaPlayer != null && mediaPlayer.isPlaying() && !isUserSeeking) {
                    int pos = mediaPlayer.getCurrentPosition();
                    int dur = mediaPlayer.getDuration();
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
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        callbacks.clear();
    }
}
