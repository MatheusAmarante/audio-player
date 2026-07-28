package com.mateus.audioplayer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "AudioPlayer";
    private static final int PERMISSION_REQUEST = 100;

    private MediaPlayer mediaPlayer;
    private List<AudioFile> allAudio = new ArrayList<>();
    private List<AudioFile> filteredAudio = new ArrayList<>();
    private AudioAdapter adapter;
    private int currentIndex = -1;
    private boolean isShuffle = false;
    private int repeatMode = 0;
    private float speed = 1.0f;
    private boolean isUserSeeking = false;

    private RecyclerView recyclerView;
    private EditText searchInput;
    private View miniPlayer;
    private TextView miniTitle, miniArtist, currentTimeText, totalTimeText;
    private ImageButton btnPlay, btnPrev, btnNext, btnShuffle, btnRepeat;
    private SeekBar seekBar;
    private TextView speedLabel;
    private View speedPanel;
    private View sleepPanel;
    private TextView sleepCountdown;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;
    private int sleepEndSeconds = -1;
    private Handler sleepHandler = new Handler(Looper.getMainLooper());

    private final StringBuilder debugLog = new StringBuilder();

    private void log(String msg) {
        Log.d(TAG, msg);
        debugLog.append(msg).append("\n");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Global crash handler
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            String stack = "Thread: " + thread.getName() + "\n" + sw.toString() + "\n\nDebug log:\n" + debugLog.toString();
            Log.e(TAG, stack);
            try {
                android.content.Intent intent = new android.content.Intent(MainActivity.this, CrashActivity.class);
                intent.putExtra("error", stack);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                android.os.Process.killProcess(android.os.Process.myPid());
            } catch (Exception e) {
                if (defaultHandler != null) defaultHandler.uncaughtException(thread, ex);
            }
        });

        try {
            log("onCreate start");
            super.onCreate(savedInstanceState);
            log("super.onCreate done");

            setContentView(R.layout.activity_main);
            log("setContentView done");

            recyclerView = findViewById(R.id.recycler_view);
            log("recyclerView found");
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
            seekBar = findViewById(R.id.seek_bar);
            speedLabel = findViewById(R.id.speed_label);
            speedPanel = findViewById(R.id.speed_panel);
            sleepPanel = findViewById(R.id.sleep_panel);
            sleepCountdown = findViewById(R.id.sleep_countdown);
            log("all views found");

            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            log("layoutManager set");

            adapter = new AudioAdapter(filteredAudio, this::playAtIndex);
            recyclerView.setAdapter(adapter);
            log("adapter set");

            searchInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) { filter(s.toString()); }
                @Override public void afterTextChanged(Editable s) {}
            });
            log("search listener set");

            btnPlay.setOnClickListener(v -> togglePlayPause());
            btnPrev.setOnClickListener(v -> previous());
            btnNext.setOnClickListener(v -> next());
            log("button listeners set");

            btnShuffle.setOnClickListener(v -> {
                isShuffle = !isShuffle;
                btnShuffle.setColorFilter(isShuffle ? 0xFF1e40af : 0xFF8b8b9e);
            });

            btnRepeat.setOnClickListener(v -> {
                repeatMode = (repeatMode + 1) % 3;
                btnRepeat.setColorFilter(repeatMode > 0 ? 0xFF1e40af : 0xFF8b8b9e);
            });

            findViewById(R.id.btn_speed).setOnClickListener(v ->
                speedPanel.setVisibility(speedPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));

            int[] speedIds = {R.id.speed_05, R.id.speed_075, R.id.speed_1, R.id.speed_125, R.id.speed_15, R.id.speed_2};
            float[] speeds = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
            for (int i = 0; i < speedIds.length; i++) {
                final float s = speeds[i];
                findViewById(speedIds[i]).setOnClickListener(v -> {
                    speed = s;
                    if (mediaPlayer != null) {
                        try { mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(s)); } catch (Exception ignored) {}
                    }
                    speedLabel.setText(String.format(Locale.US, "%.2fx", s));
                    speedPanel.setVisibility(View.GONE);
                });
            }

            int[] sleepIds = {R.id.sleep_15, R.id.sleep_30, R.id.sleep_60, R.id.sleep_off};
            int[] sleepMins = {15, 30, 60, 0};
            for (int i = 0; i < sleepIds.length; i++) {
                final int mins = sleepMins[i];
                findViewById(sleepIds[i]).setOnClickListener(v -> setSleepTimer(mins));
            }
            log("speed/sleep listeners set");

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                    if (fromUser && mediaPlayer != null) {
                        mediaPlayer.seekTo(p);
                        currentTimeText.setText(formatTime(p));
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar sb) { isUserSeeking = true; }
                @Override public void onStopTrackingTouch(SeekBar sb) { isUserSeeking = false; }
            });
            log("seekbar listener set");

            log("calling requestPermissions");
            requestPermissions();
            log("onCreate complete");
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String stack = "onCreate crash:\n" + sw.toString() + "\n\nDebug log:\n" + debugLog.toString();
            Log.e(TAG, stack);
            android.content.Intent intent = new android.content.Intent(this, CrashActivity.class);
            intent.putExtra("error", stack);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(progressRunnable);
        sleepHandler.removeCallbacksAndMessages(null);
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
    }

    private void requestPermissions() {
        String perm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ? Manifest.permission.READ_MEDIA_AUDIO
            : Manifest.permission.READ_EXTERNAL_STORAGE;

        log("checking permission: " + perm);
        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            log("permission not granted, requesting");
            ActivityCompat.requestPermissions(this, new String[]{perm}, PERMISSION_REQUEST);
        } else {
            log("permission already granted, loading audio");
            loadAudio();
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        log("onRequestPermissionsResult: code=" + code + " granted=" + (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED));
        if (code == PERMISSION_REQUEST && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            loadAudio();
        }
    }

    private void loadAudio() {
        log("loadAudio started");
        new Thread(() -> {
            try {
                allAudio = AudioLoader.loadAllAudio(this);
                log("loaded " + allAudio.size() + " audio files");
                filteredAudio = new ArrayList<>(allAudio);
                runOnUiThread(() -> {
                    adapter = new AudioAdapter(filteredAudio, this::playAtIndex);
                    recyclerView.setAdapter(adapter);
                    log("adapter updated with " + filteredAudio.size() + " items");
                });
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                log("loadAudio error: " + sw.toString());
                runOnUiThread(() -> Toast.makeText(this, "Error loading: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void filter(String query) {
        filteredAudio.clear();
        if (query.isEmpty()) {
            filteredAudio.addAll(allAudio);
        } else {
            String q = query.toLowerCase();
            for (AudioFile af : allAudio) {
                if (af.getSafeTitle().toLowerCase().contains(q) || af.getArtistOrAlbum().toLowerCase().contains(q)) {
                    filteredAudio.add(af);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void playAtIndex(int index) {
        if (index < 0 || index >= filteredAudio.size()) return;
        currentIndex = index;
        playCurrent();
    }

    private void playCurrent() {
        if (currentIndex < 0 || currentIndex >= filteredAudio.size()) return;
        AudioFile track = filteredAudio.get(currentIndex);
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
            mediaPlayer.setDataSource(this, track.uri);
            mediaPlayer.setOnPreparedListener(mp -> {
                try { mp.setPlaybackParams(mp.getPlaybackParams().setSpeed(speed)); } catch (Exception ignored) {}
                mp.start();
                updateUI(track);
                startProgressUpdates();
            });
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            Toast.makeText(this, "Cannot play: " + track.getSafeTitle(), Toast.LENGTH_SHORT).show();
            next();
        }
    }

    private void onTrackComplete() {
        if (repeatMode == 1) {
            playCurrent();
        } else if (repeatMode == 2 || isShuffle) {
            next();
        } else if (currentIndex < filteredAudio.size() - 1) {
            next();
        } else {
            pause();
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            btnPlay.setImageResource(android.R.drawable.ic_media_play);
        } else {
            mediaPlayer.start();
            btnPlay.setImageResource(android.R.drawable.ic_media_pause);
        }
    }

    private void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            btnPlay.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private void next() {
        if (filteredAudio.isEmpty()) return;
        if (isShuffle) {
            currentIndex = new Random().nextInt(filteredAudio.size());
        } else {
            currentIndex = (currentIndex + 1) % filteredAudio.size();
        }
        playCurrent();
    }

    private void previous() {
        if (filteredAudio.isEmpty()) return;
        if (mediaPlayer != null && mediaPlayer.getCurrentPosition() > 3000) {
            mediaPlayer.seekTo(0);
        } else {
            currentIndex = currentIndex > 0 ? currentIndex - 1 : filteredAudio.size() - 1;
            playCurrent();
        }
    }

    private void updateUI(AudioFile track) {
        miniPlayer.setVisibility(View.VISIBLE);
        miniTitle.setText(track.getSafeTitle());
        miniArtist.setText(track.getArtistOrAlbum());
        btnPlay.setImageResource(android.R.drawable.ic_media_pause);
        int dur = mediaPlayer != null ? mediaPlayer.getDuration() : 0;
        seekBar.setMax(dur > 0 ? dur : 100);
        totalTimeText.setText(formatTime(dur));
        speedLabel.setText(String.format(Locale.US, "%.2fx", speed));
        adapter.setSelectedPosition(currentIndex);
        recyclerView.scrollToPosition(currentIndex);
    }

    private void startProgressUpdates() {
        handler.removeCallbacks(progressRunnable);
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying() && !isUserSeeking) {
                    int pos = mediaPlayer.getCurrentPosition();
                    seekBar.setProgress(pos);
                    currentTimeText.setText(formatTime(pos));
                }
                handler.postDelayed(this, 250);
            }
        };
        handler.post(progressRunnable);
    }

    private void setSleepTimer(int minutes) {
        sleepHandler.removeCallbacksAndMessages(null);
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
            pause();
            sleepEndSeconds = -1;
            sleepCountdown.setText("");
            sleepPanel.setVisibility(View.GONE);
            return;
        }
        int m = remaining / 60;
        int s = remaining % 60;
        sleepCountdown.setText(String.format(Locale.US, "%d:%02d", m, s));
        sleepHandler.postDelayed(this::tickSleep, 1000);
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
