package com.mateus.audioplayer;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements MusicService.MusicCallback {

    private static final int PERMISSION_REQUEST = 100;

    private MusicService musicService;
    private boolean serviceBound = false;
    private AudioAdapter adapter;
    private List<AudioFile> allAudio = new ArrayList<>();
    private List<AudioFile> filteredAudio = new ArrayList<>();

    // UI
    private RecyclerView recyclerView;
    private EditText searchInput;
    private View miniPlayer;
    private TextView miniTitle, miniArtist, currentTimeText, totalTimeText;
    private ImageButton btnPlay, btnPrev, btnNext, btnShuffle, btnRepeat, btnSpeed;
    private SeekBar seekBar;
    private TextView speedLabel;
    private View speedPanel;
    private View sleepPanel;
    private TextView sleepCountdown;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;
    private boolean isUserSeeking = false;
    private int sleepEndSeconds = -1;
    private Handler sleepHandler = new Handler(Looper.getMainLooper());
    private Runnable sleepRunnable;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            musicService.setCallback(MainActivity.this);
            serviceBound = true;
            updateMiniPlayer();
            startProgressUpdates();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind views
        recyclerView = findViewById(R.id.recycler_view);
        searchInput = findViewById(R.id.search_input);
        miniPlayer = findViewById(R.id.mini_player);
        miniTitle = findViewById(R.id.mini_title);
        miniArtist = findViewById(R.id.mini_artist);
        currentTimeText = findViewById(R.id.current_time);
        totalTimeText = findViewById(R.id.total_time);
        btnPlay = findViewById(R.id.btn_play);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);
        btnShuffle = findViewById(R.id.btn_shuffle);
        btnRepeat = findViewById(R.id.btn_repeat);
        btnSpeed = findViewById(R.id.btn_speed);
        seekBar = findViewById(R.id.seek_bar);
        speedLabel = findViewById(R.id.speed_label);
        speedPanel = findViewById(R.id.speed_panel);
        sleepPanel = findViewById(R.id.sleep_panel);
        sleepCountdown = findViewById(R.id.sleep_countdown);

        // RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AudioAdapter(filteredAudio, position -> {
            if (musicService != null) {
                musicService.setPlaylist(filteredAudio, position);
                updateMiniPlayer();
            }
        });
        recyclerView.setAdapter(adapter);

        // Search
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterAudio(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Controls
        btnPlay.setOnClickListener(v -> { if (musicService != null) musicService.togglePlayPause(); });
        btnPrev.setOnClickListener(v -> { if (musicService != null) musicService.previous(); });
        btnNext.setOnClickListener(v -> { if (musicService != null) musicService.next(); });

        btnShuffle.setOnClickListener(v -> {
            if (musicService != null) {
                boolean newState = !musicService.isShuffle();
                musicService.setShuffle(newState);
                btnShuffle.setColorFilter(newState ? 0xFF1e40af : 0xFF8b8b9e);
            }
        });

        btnRepeat.setOnClickListener(v -> {
            if (musicService != null) {
                int mode = (musicService.getRepeatMode() + 1) % 3;
                musicService.setRepeatMode(mode);
                switch (mode) {
                    case 0: btnRepeat.setColorFilter(0xFF8b8b9e); btnRepeat.setAlpha(1.0f); break;
                    case 1: btnRepeat.setColorFilter(0xFF1e40af); btnRepeat.setAlpha(1.0f); btnRepeat.setImageResource(android.R.drawable.ic_menu_revert); break;
                    case 2: btnRepeat.setColorFilter(0xFF1e40af); btnRepeat.setAlpha(1.0f); btnRepeat.setImageResource(android.R.drawable.ic_popup_sync); break;
                }
            }
        });

        btnSpeed.setOnClickListener(v -> speedPanel.setVisibility(speedPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));

        // Speed buttons
        int[] speedBtnIds = {R.id.speed_05, R.id.speed_075, R.id.speed_1, R.id.speed_125, R.id.speed_15, R.id.speed_2};
        float[] speeds = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
        for (int i = 0; i < speedBtnIds.length; i++) {
            final float spd = speeds[i];
            findViewById(speedBtnIds[i]).setOnClickListener(v -> {
                if (musicService != null) musicService.setSpeed(spd);
                speedLabel.setText(String.format(Locale.US, "%.2fx", spd));
                speedPanel.setVisibility(View.GONE);
            });
        }

        // Sleep timer
        int[] sleepBtnIds = {R.id.sleep_15, R.id.sleep_30, R.id.sleep_60, R.id.sleep_off};
        int[] sleepMins = {15, 30, 60, 0};
        for (int i = 0; i < sleepBtnIds.length; i++) {
            final int mins = sleepMins[i];
            findViewById(sleepBtnIds[i]).setOnClickListener(v -> setSleepTimer(mins));
        }

        // SeekBar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && musicService != null) {
                    musicService.seekTo(progress);
                    currentTimeText.setText(formatTime(progress));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { isUserSeeking = true; }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { isUserSeeking = false; }
        });

        // Request permissions
        requestPermissions();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
        stopProgressUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(progressRunnable);
        sleepHandler.removeCallbacks(sleepRunnable);
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST);
            } else {
                loadAudio();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST);
            } else {
                loadAudio();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadAudio();
            } else {
                Toast.makeText(this, "Permission needed to load audio files", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadAudio() {
        new Thread(() -> {
            allAudio = AudioLoader.loadAllAudio(this);
            filteredAudio = new ArrayList<>(allAudio);
            runOnUiThread(() -> {
                adapter = new AudioAdapter(filteredAudio, position -> {
                    if (musicService != null) {
                        musicService.setPlaylist(filteredAudio, position);
                        updateMiniPlayer();
                    }
                });
                recyclerView.setAdapter(adapter);
                updateMiniPlayer();
            });
        }).start();
    }

    private void filterAudio(String query) {
        if (allAudio == null) return;
        filteredAudio.clear();
        if (query.isEmpty()) {
            filteredAudio.addAll(allAudio);
        } else {
            String q = query.toLowerCase();
            for (AudioFile af : allAudio) {
                if (af.title.toLowerCase().contains(q) || af.getArtistOrAlbum().toLowerCase().contains(q)) {
                    filteredAudio.add(af);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    // ===== Mini Player =====

    private void updateMiniPlayer() {
        if (musicService == null) return;
        AudioFile track = musicService.getCurrentTrack();
        if (track != null) {
            miniPlayer.setVisibility(View.VISIBLE);
            miniTitle.setText(track.title);
            miniArtist.setText(track.getArtistOrAlbum());
            btnPlay.setImageResource(musicService.isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
            seekBar.setMax(musicService.getDuration());
            totalTimeText.setText(formatTime(musicService.getDuration()));
            speedLabel.setText(String.format(Locale.US, "%.2fx", musicService.getSpeed()));

            // Highlight current track
            int idx = musicService.getCurrentIndex();
            if (idx >= 0 && idx < filteredAudio.size()) {
                adapter.setSelectedPosition(idx);
                recyclerView.scrollToPosition(idx);
            }
        } else {
            miniPlayer.setVisibility(View.GONE);
        }
    }

    private void startProgressUpdates() {
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (musicService != null && musicService.isPlaying() && !isUserSeeking) {
                    int pos = musicService.getCurrentPosition();
                    seekBar.setProgress(pos);
                    currentTimeText.setText(formatTime(pos));
                }
                handler.postDelayed(this, 250);
            }
        };
        handler.post(progressRunnable);
    }

    private void stopProgressUpdates() {
        handler.removeCallbacks(progressRunnable);
    }

    // ===== Sleep Timer =====

    private void setSleepTimer(int minutes) {
        sleepHandler.removeCallbacks(sleepRunnable);
        if (minutes == 0) {
            sleepEndSeconds = -1;
            sleepCountdown.setText("");
            sleepPanel.setVisibility(View.GONE);
            return;
        }
        sleepEndSeconds = (int) (System.currentTimeMillis() / 1000) + minutes * 60;
        sleepPanel.setVisibility(View.VISIBLE);
        tickSleep();
    }

    private void tickSleep() {
        if (sleepEndSeconds < 0) return;
        int remaining = sleepEndSeconds - (int) (System.currentTimeMillis() / 1000);
        if (remaining <= 0) {
            if (musicService != null) musicService.pause();
            sleepEndSeconds = -1;
            sleepCountdown.setText("");
            sleepPanel.setVisibility(View.GONE);
            return;
        }
        int m = remaining / 60;
        int s = remaining % 60;
        sleepCountdown.setText(String.format(Locale.US, "%d:%02d", m, s));
        sleepRunnable = this::tickSleep;
        sleepHandler.postDelayed(sleepRunnable, 1000);
    }

    // ===== Callbacks =====

    @Override
    public void onTrackChanged(AudioFile track) {
        runOnUiThread(this::updateMiniPlayer);
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        runOnUiThread(() -> {
            btnPlay.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
        });
    }

    @Override
    public void onProgressUpdate(int currentMs, int totalMs) {
        // Not used — we poll via handler
    }

    // ===== Helpers =====

    private String formatTime(int ms) {
        int secs = ms / 1000;
        int h = secs / 3600;
        int m = (secs % 3600) / 60;
        int s = secs % 60;
        if (h > 0) return String.format(Locale.US, "%d:%02d:%02d", h, m, s);
        return String.format(Locale.US, "%d:%02d", m, s);
    }
}
