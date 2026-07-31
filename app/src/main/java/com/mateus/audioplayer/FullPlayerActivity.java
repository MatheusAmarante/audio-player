package com.mateus.audioplayer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import java.util.Locale;

public class FullPlayerActivity extends AppCompatActivity {

    private PlayerManager playerManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isUserSeeking = false;

    private ImageView thumbnail;
    private TextView titleText, artistText, currentTimeText, totalTimeText;
    private TextView speedLabel, sleepCountdown;
    private ImageButton btnPlayPause, btnPrev, btnNext, btnShuffle, btnRepeat;
    private SeekBar seekBar;
    private View speedPanel, sleepPanel;

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
        seekBar = findViewById(R.id.full_seekbar);
        speedPanel = findViewById(R.id.full_speed_panel);
        sleepPanel = findViewById(R.id.full_sleep_panel);

        // Back button
        findViewById(R.id.full_back).setOnClickListener(v -> finish());

        // Playback controls
        btnPlayPause.setOnClickListener(v -> playerManager.togglePlayPause(this));
        btnPrev.setOnClickListener(v -> {
            playerManager.previous();
            playerManager.play(this);
        });
        btnNext.setOnClickListener(v -> {
            playerManager.next();
            playerManager.play(this);
        });

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
            public void onStartTrackingTouch(SeekBar sb) {
                isUserSeeking = true;
                playerManager.setUserSeeking(true);
            }
            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                isUserSeeking = false;
                playerManager.setUserSeeking(false);
            }
        });

        // Register callbacks
        playerManager.addCallback(new PlayerManager.PlayerCallback() {
            @Override
            public void onTrackChanged(YouTubeTrack track) {
                runOnUiThread(() -> updateTrackInfo(track));
            }

            @Override
            public void onPlayStateChanged(boolean isPlaying) {
                runOnUiThread(() -> {
                    btnPlayPause.setImageResource(isPlaying ?
                        android.R.drawable.ic_media_pause :
                        android.R.drawable.ic_media_play);
                });
            }

            @Override
            public void onProgressChanged(int position, int duration) {
                runOnUiThread(() -> {
                    if (!isUserSeeking) {
                        seekBar.setMax(duration > 0 ? duration : 100);
                        seekBar.setProgress(position);
                        currentTimeText.setText(formatTime(position));
                        totalTimeText.setText(formatTime(duration));
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
                    if (remainingSeconds <= 0) {
                        sleepCountdown.setText("Sleep");
                    } else {
                        int m = remainingSeconds / 60;
                        int s = remainingSeconds % 60;
                        sleepCountdown.setText(String.format(Locale.US, "Sleep: %d:%02d", m, s));
                    }
                });
            }
        });

        // Initial state
        YouTubeTrack current = playerManager.getCurrentTrack();
        if (current != null) {
            updateTrackInfo(current);
        }
        btnPlayPause.setImageResource(playerManager.isPlaying() ?
            android.R.drawable.ic_media_pause :
            android.R.drawable.ic_media_play);
        btnShuffle.setColorFilter(playerManager.isShuffle() ? 0xFF1e40af : 0xFF8b8b9e);
        btnRepeat.setColorFilter(playerManager.getRepeatMode() > 0 ? 0xFF1e40af : 0xFF8b8b9e);
        speedLabel.setText(String.format(Locale.US, "%.2fx", playerManager.getSpeed()));

        long sleepEnd = playerManager.getSleepEndMillis();
        if (sleepEnd > 0) {
            sleepPanel.setVisibility(View.VISIBLE);
        }
    }

    private void updateTrackInfo(YouTubeTrack track) {
        if (track == null) return;
        titleText.setText(track.title);
        artistText.setText(track.getArtistOrUploader());

        if (track.thumbnailUrl != null && !track.thumbnailUrl.isEmpty()) {
            Glide.with(this).load(track.thumbnailUrl)
                .placeholder(R.drawable.album_art_placeholder)
                .into(thumbnail);
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
