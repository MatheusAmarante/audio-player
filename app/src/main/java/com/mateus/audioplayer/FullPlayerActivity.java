package com.mateus.audioplayer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FullPlayerActivity extends AppCompatActivity {

    private PlayerManager playerManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isUserSeeking = false;

    private ImageView thumbnail;
    private TextView titleText, artistText, currentTimeText, totalTimeText;
    private TextView speedLabel, sleepCountdown;
    private ImageButton btnPlayPause, btnPrev, btnNext, btnShuffle, btnRepeat, btnDownload, btnLyrics;
    private SeekBar seekBar;
    private View speedPanel, sleepPanel, lyricsPanel;
    private TextView lyricsText;
    private ScrollView lyricsScroll;

    // Lyrics sync
    private List<LyricLine> lyricLines = new ArrayList<>();
    private int currentLyricIndex = -1;
    private boolean lyricsVisible = false;

    private static class LyricLine {
        long timeMs;
        String text;
        LyricLine(long t, String txt) { timeMs = t; text = txt; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_player);

        playerManager = PlayerManager.getInstance();

        // Bind views
        thumbnail = findViewById(R.id.full_thumbnail);
        titleText = findViewById(R.id.full_title);
        artistText = findViewById(R.id.full_artist);
        currentTimeText = findViewById(R.id.full_current_time);
        totalTimeText = findViewById(R.id.full_total_time);
        speedLabel = findViewById(R.id.full_speed_label);
        sleepCountdown = findViewById(R.id.full_sleep_countdown);
        btnPlayPause = findViewById(R.id.full_play_pause);
        btnPrev = findViewById(R.id.full_prev);
        btnNext = findViewById(R.id.full_next);
        btnShuffle = findViewById(R.id.full_shuffle);
        btnRepeat = findViewById(R.id.full_repeat);
        btnDownload = findViewById(R.id.full_download);
        btnLyrics = findViewById(R.id.full_lyrics);
        seekBar = findViewById(R.id.full_seekbar);
        speedPanel = findViewById(R.id.full_speed_panel);
        sleepPanel = findViewById(R.id.full_sleep_panel);
        lyricsPanel = findViewById(R.id.full_lyrics_panel);
        lyricsText = findViewById(R.id.full_lyrics_text);
        lyricsScroll = findViewById(R.id.full_lyrics_scroll);

        lyricsText.setMovementMethod(new ScrollingMovementMethod());

        // Back button
        findViewById(R.id.full_back).setOnClickListener(v -> finish());

        // Playback controls
        btnPlayPause.setOnClickListener(v -> playerManager.togglePlayPause(this));
        btnPrev.setOnClickListener(v -> { playerManager.previous(); playerManager.play(this); });
        btnNext.setOnClickListener(v -> { playerManager.next(); playerManager.play(this); });

        // Shuffle
        btnShuffle.setOnClickListener(v -> {
            boolean s = !playerManager.isShuffle();
            playerManager.setShuffle(s);
            btnShuffle.setColorFilter(s ? 0xFF1e40af : 0xFF8b8b9e);
        });

        // Repeat
        btnRepeat.setOnClickListener(v -> {
            int mode = (playerManager.getRepeatMode() + 1) % 3;
            playerManager.setRepeatMode(mode);
            btnRepeat.setColorFilter(mode > 0 ? 0xFF1e40af : 0xFF8b8b9e);
        });

        // Download
        btnDownload.setOnClickListener(v -> {
            YouTubeTrack track = playerManager.getCurrentTrack();
            if (track == null) return;
            PipedApiClient client = new PipedApiClient(this);
            client.resolveBaseUrl(() -> {
                String url = client.getDownloadUrl(track.videoId);
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(i);
                Toast.makeText(this, "Downloading " + track.title, Toast.LENGTH_SHORT).show();
            });
        });

        // Lyrics toggle
        btnLyrics.setOnClickListener(v -> {
            lyricsVisible = !lyricsVisible;
            lyricsPanel.setVisibility(lyricsVisible ? View.VISIBLE : View.GONE);
            btnLyrics.setColorFilter(lyricsVisible ? 0xFF1e40af : 0xFF8b8b9e);
            if (lyricsVisible && lyricLines.isEmpty()) {
                loadLyrics();
            }
        });

        // Speed
        speedLabel.setOnClickListener(v ->
            speedPanel.setVisibility(speedPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));

        int[] speedIds = {R.id.full_speed_05, R.id.full_speed_075, R.id.full_speed_1,
                          R.id.full_speed_125, R.id.full_speed_15, R.id.full_speed_2};
        float[] speeds = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
        for (int i = 0; i < speedIds.length; i++) {
            final float s = speeds[i];
            findViewById(speedIds[i]).setOnClickListener(v -> {
                playerManager.setSpeed(s);
                speedLabel.setText(String.format(Locale.US, "%.2fx", s));
                speedPanel.setVisibility(View.GONE);
            });
        }

        // Sleep timer
        sleepCountdown.setOnClickListener(v ->
            sleepPanel.setVisibility(sleepPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));

        int[] sleepIds = {R.id.full_sleep_15, R.id.full_sleep_30, R.id.full_sleep_60, R.id.full_sleep_off};
        int[] sleepMins = {15, 30, 60, 0};
        for (int i = 0; i < sleepIds.length; i++) {
            final int mins = sleepMins[i];
            findViewById(sleepIds[i]).setOnClickListener(v -> {
                playerManager.setSleepTimer(mins);
                if (mins == 0) {
                    sleepCountdown.setText("Sleep");
                    sleepPanel.setVisibility(View.GONE);
                } else {
                    sleepPanel.setVisibility(View.VISIBLE);
                }
            });
        }

        // SeekBar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) {
                    playerManager.seekTo(progress);
                    currentTimeText.setText(formatTime(progress));
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar sb) { isUserSeeking = true; playerManager.setUserSeeking(true); }
            @Override
            public void onStopTrackingTouch(SeekBar sb) { isUserSeeking = false; playerManager.setUserSeeking(false); }
        });

        // Register callbacks
        playerManager.addCallback(new PlayerManager.PlayerCallback() {
            @Override
            public void onTrackChanged(YouTubeTrack track) {
                runOnUiThread(() -> {
                    updateTrackInfo(track);
                    lyricLines.clear();
                    currentLyricIndex = -1;
                    if (lyricsVisible) loadLyrics();
                });
            }

            @Override
            public void onPlayStateChanged(boolean isPlaying) {
                runOnUiThread(() -> btnPlayPause.setImageResource(isPlaying ?
                    android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play));
            }

            @Override
            public void onProgressChanged(int position, int duration) {
                runOnUiThread(() -> {
                    if (!isUserSeeking) {
                        seekBar.setMax(duration > 0 ? duration : 100);
                        seekBar.setProgress(position);
                        currentTimeText.setText(formatTime(position));
                        totalTimeText.setText(formatTime(duration));
                        updateLyricsHighlight(position);
                    }
                });
            }

            @Override
            public void onSpeedChanged(float speed) {
                runOnUiThread(() -> speedLabel.setText(String.format(Locale.US, "%.2fx", speed)));
            }

            @Override
            public void onSleepTimerChanged(int remainingSeconds) {
                runOnUiThread(() -> {
                    if (remainingSeconds <= 0) sleepCountdown.setText("Sleep");
                    else {
                        int m = remainingSeconds / 60;
                        int s = remainingSeconds % 60;
                        sleepCountdown.setText(String.format(Locale.US, "Sleep: %d:%02d", m, s));
                    }
                });
            }
        });

        // Initial state
        YouTubeTrack current = playerManager.getCurrentTrack();
        if (current != null) updateTrackInfo(current);
        btnPlayPause.setImageResource(playerManager.isPlaying() ?
            android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
        btnShuffle.setColorFilter(playerManager.isShuffle() ? 0xFF1e40af : 0xFF8b8b9e);
        btnRepeat.setColorFilter(playerManager.getRepeatMode() > 0 ? 0xFF1e40af : 0xFF8b8b9e);
        speedLabel.setText(String.format(Locale.US, "%.2fx", playerManager.getSpeed()));

        long sleepEnd = playerManager.getSleepEndMillis();
        if (sleepEnd > 0) sleepPanel.setVisibility(View.VISIBLE);
    }

    private void updateTrackInfo(YouTubeTrack track) {
        if (track == null) return;
        titleText.setText(track.title);
        artistText.setText(track.getArtistOrUploader());
        if (track.thumbnailUrl != null && !track.thumbnailUrl.isEmpty()) {
            Glide.with(this).load(track.thumbnailUrl)
                .placeholder(R.drawable.album_art_placeholder).into(thumbnail);
        }
    }

    private void loadLyrics() {
        YouTubeTrack track = playerManager.getCurrentTrack();
        if (track == null) return;
        PipedApiClient client = new PipedApiClient(this);
        client.resolveBaseUrl(() -> {
            client.fetchLyrics(track.title, track.artist, new PipedApiClient.LyricsCallback() {
                @Override
                public void onLyrics(String synced, String plain) {
                    runOnUiThread(() -> {
                        if (!synced.isEmpty()) {
                            parseSyncedLyrics(synced);
                        } else if (!plain.isEmpty()) {
                            lyricsText.setText(plain);
                        } else {
                            lyricsText.setText("No lyrics found");
                        }
                    });
                }
                @Override
                public void onError(String msg) {
                    runOnUiThread(() -> lyricsText.setText("Lyrics unavailable"));
                }
            });
        });
    }

    private void parseSyncedLyrics(String lrc) {
        lyricLines.clear();
        Pattern p = Pattern.compile("\\[(\\d+):(\\d+)\\.(\\d+)\\](.*)");
        for (String line : lrc.split("\n")) {
            Matcher m = p.matcher(line.trim());
            if (m.find()) {
                int min = Integer.parseInt(m.group(1));
                int sec = Integer.parseInt(m.group(2));
                int centi = Integer.parseInt(m.group(3));
                long ms = (min * 60L + sec) * 1000L + centi * 10L;
                String text = m.group(4).trim();
                if (!text.isEmpty()) {
                    lyricLines.add(new LyricLine(ms, text));
                }
            }
        }
        // Build display text
        StringBuilder sb = new StringBuilder();
        for (LyricLine ll : lyricLines) {
            sb.append(ll.text).append("\n");
        }
        lyricsText.setText(sb.toString());
    }

    private void updateLyricsHighlight(int positionMs) {
        if (lyricLines.isEmpty() || !lyricsVisible) return;
        int newIndex = -1;
        for (int i = lyricLines.size() - 1; i >= 0; i--) {
            if (positionMs >= lyricLines.get(i).timeMs) {
                newIndex = i;
                break;
            }
        }
        if (newIndex != currentLyricIndex) {
            currentLyricIndex = newIndex;
            // Rebuild with highlight
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lyricLines.size(); i++) {
                if (i == currentLyricIndex) {
                    sb.append("▶ ").append(lyricLines.get(i).text).append("\n");
                } else {
                    sb.append(lyricLines.get(i).text).append("\n");
                }
            }
            lyricsText.setText(sb.toString());
            // Auto-scroll
            if (currentLyricIndex >= 0) {
                int lineHeight = lyricsText.getLineHeight();
                int scrollTo = lineHeight * currentLyricIndex - lyricsScroll.getHeight() / 3;
                lyricsScroll.smoothScrollTo(0, Math.max(0, scrollTo));
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private String formatTime(int ms) {
        if (ms <= 0) return "00:00";
        int secs = ms / 1000;
        int h = secs / 3600;
        int m = (secs % 3600) / 60;
        int s = secs % 60;
        if (h > 0) return String.format(Locale.US, "%d:%02d:%02d", h, m, s);
        return String.format(Locale.US, "%d:%02d", m, s);
    }
}
