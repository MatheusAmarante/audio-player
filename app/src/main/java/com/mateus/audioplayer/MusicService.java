package com.mateus.audioplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.core.app.NotificationCompat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {

    private static final String CHANNEL_ID = "audio_player_channel";
    private static final int NOTIFICATION_ID = 1;

    private MediaPlayer mediaPlayer;
    private final IBinder binder = new MusicBinder();
    private List<AudioFile> playlist = new ArrayList<>();
    private int currentIndex = -1;
    private boolean isShuffle = false;
    private int repeatMode = 0; // 0=none, 1=repeat one, 2=repeat all
    private float playbackSpeed = 1.0f;
    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;
    private boolean isPreparing = false;

    // Callback interface
    public interface MusicCallback {
        void onTrackChanged(AudioFile track);
        void onPlaybackStateChanged(boolean isPlaying);
        void onProgressUpdate(int currentMs, int totalMs);
    }

    private MusicCallback callback;

    public void setCallback(MusicCallback cb) { this.callback = cb; }

    public class MusicBinder extends Binder {
        public MusicService getService() { return MusicService.this; }
    }

    @Override
    public IBinder onBind(Intent intent) { return binder; }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        initMediaPlayer();
        initMediaSession();
        registerReceiver(notificationReceiver, new IntentFilter("com.mateus.audioplayer.NOTIFICATION_ACTION"));
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(notificationReceiver);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mediaSession != null) mediaSession.release();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case "ACTION_PLAY": play(); break;
                case "ACTION_PAUSE": pause(); break;
                case "ACTION_NEXT": next(); break;
                case "ACTION_PREV": previous(); break;
                case "ACTION_STOP": stopSelf(); break;
            }
        }
        return START_STICKY;
    }

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build());
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
    }

    private void initMediaSession() {
        mediaSession = new MediaSessionCompat(this, "AudioPlayer");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        stateBuilder = new PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE |
                       PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                       PlaybackStateCompat.ACTION_SEEK_TO);
        mediaSession.setPlaybackState(stateBuilder.build());
        mediaSession.setActive(true);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Audio Player", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Audio playback controls");
            channel.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    // ===== Playback Controls =====

    public void setPlaylist(List<AudioFile> list, int startIndex) {
        this.playlist = new ArrayList<>(list);
        this.currentIndex = startIndex;
        playTrack();
    }

    public void playTrack() {
        if (currentIndex < 0 || currentIndex >= playlist.size()) return;
        AudioFile track = playlist.get(currentIndex);
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(this, Uri.parse(track.uri.toString()));
            mediaPlayer.prepareAsync();
            isPreparing = true;
            if (callback != null) callback.onTrackChanged(track);
        } catch (IOException e) {
            e.printStackTrace();
            next();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        isPreparing = false;
        mp.setPlaybackParams(mp.getPlaybackParams().setSpeed(playbackSpeed));
        mp.start();
        updateNotification();
        if (callback != null) callback.onPlaybackStateChanged(true);
        startForeground(NOTIFICATION_ID, buildNotification());
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        isPreparing = false;
        next();
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (repeatMode == 1) {
            // Repeat one
            playTrack();
        } else if (repeatMode == 2 || isShuffle) {
            next();
        } else if (currentIndex < playlist.size() - 1) {
            next();
        } else {
            pause();
            if (callback != null) callback.onPlaybackStateChanged(false);
            stopForeground(false);
            updateNotification();
        }
    }

    public void play() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying() && !isPreparing) {
            if (mediaPlayer.getCurrentPosition() > 0) {
                mediaPlayer.start();
            } else if (currentIndex >= 0) {
                playTrack();
            }
            if (callback != null) callback.onPlaybackStateChanged(true);
            updateNotification();
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            if (callback != null) callback.onPlaybackStateChanged(false);
            updateNotification();
        }
    }

    public void togglePlayPause() {
        if (isPlaying()) pause(); else play();
    }

    public void next() {
        if (playlist.isEmpty()) return;
        if (isShuffle) {
            currentIndex = new Random().nextInt(playlist.size());
        } else {
            currentIndex = (currentIndex + 1) % playlist.size();
        }
        playTrack();
    }

    public void previous() {
        if (playlist.isEmpty()) return;
        if (mediaPlayer.getCurrentPosition() > 3000) {
            seekTo(0);
        } else {
            currentIndex = currentIndex > 0 ? currentIndex - 1 : playlist.size() - 1;
            playTrack();
        }
    }

    public void seekTo(int ms) {
        if (mediaPlayer != null) mediaPlayer.seekTo(ms);
    }

    public void setSpeed(float speed) {
        this.playbackSpeed = speed;
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(speed));
        }
    }

    public void setShuffle(boolean shuffle) { this.isShuffle = shuffle; }
    public boolean isShuffle() { return isShuffle; }

    public void setRepeatMode(int mode) { this.repeatMode = mode; }
    public int getRepeatMode() { return repeatMode; }

    public boolean isPlaying() { return mediaPlayer != null && mediaPlayer.isPlaying(); }
    public int getCurrentPosition() { return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0; }
    public int getDuration() { return mediaPlayer != null ? mediaPlayer.getDuration() : 0; }
    public AudioFile getCurrentTrack() { return (currentIndex >= 0 && currentIndex < playlist.size()) ? playlist.get(currentIndex) : null; }
    public int getCurrentIndex() { return currentIndex; }
    public List<AudioFile> getPlaylist() { return playlist; }
    public float getSpeed() { return playbackSpeed; }

    // ===== Notification =====

    private Notification buildNotification() {
        AudioFile track = getCurrentTrack();
        String title = track != null ? track.title : "No track";
        String artist = track != null ? track.getArtistOrAlbum() : "";

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.getSessionToken())
                .setShowActionsInCompactView(0, 1, 2))
            .addAction(android.R.drawable.ic_media_previous, "Prev", buildPendingIntent("ACTION_PREV"))
            .addAction(isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                       "Play", buildPendingIntent("ACTION_PLAY_PAUSE"))
            .addAction(android.R.drawable.ic_media_next, "Next", buildPendingIntent("ACTION_NEXT"));

        return builder.build();
    }

    private PendingIntent buildPendingIntent(String action) {
        Intent intent = new Intent("com.mateus.audioplayer.NOTIFICATION_ACTION");
        intent.setAction(action);
        return PendingIntent.getBroadcast(this, action.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void updateNotification() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(NOTIFICATION_ID, buildNotification());
    }

    private final BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                case "ACTION_PLAY_PAUSE": togglePlayPause(); break;
                case "ACTION_NEXT": next(); break;
                case "ACTION_PREV": previous(); break;
            }
        }
    };
}
