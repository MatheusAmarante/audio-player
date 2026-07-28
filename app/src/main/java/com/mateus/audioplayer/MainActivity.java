package com.mateus.audioplayer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "AudioPlayer";
    private static final int PERMISSION_REQUEST = 100;

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNav;

    // Mini player views
    private View miniPlayer;
    private TextView miniTitle, miniArtist, miniCurrentTime, miniTotalTime;
    private ImageButton miniPlayPause, miniPrev, miniNext;
    private SeekBar miniSeekBar;

    private PlayerManager playerManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isUserSeeking = false;

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
                Intent intent = new Intent(MainActivity.this, CrashActivity.class);
                intent.putExtra("error", stack);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                android.os.Process.killProcess(android.os.Process.myPid());
            } catch (Exception e) {
                if (defaultHandler != null) defaultHandler.uncaughtException(thread, ex);
            }
        });

        try {
            log("onCreate start");
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            log("setContentView done");

            playerManager = PlayerManager.getInstance();

            // Setup ViewPager2
            viewPager = findViewById(R.id.view_pager);
            bottomNav = findViewById(R.id.bottom_nav);

            ViewPagerAdapter pagerAdapter = new ViewPagerAdapter(this);
            viewPager.setAdapter(pagerAdapter);
            viewPager.setUserInputEnabled(false); // disable swipe, use bottom nav only

            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_library) {
                    viewPager.setCurrentItem(0, true);
                    return true;
                } else if (id == R.id.nav_folders) {
                    viewPager.setCurrentItem(1, true);
                    return true;
                } else if (id == R.id.nav_playlists) {
                    viewPager.setCurrentItem(2, true);
                    return true;
                }
                return false;
            });

            viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    bottomNav.getMenu().getItem(position).setChecked(true);
                }
            });

            // Mini player
            miniPlayer = findViewById(R.id.mini_player);
            miniTitle = findViewById(R.id.mini_title);
            miniArtist = findViewById(R.id.mini_artist);
            miniCurrentTime = findViewById(R.id.mini_current_time);
            miniTotalTime = findViewById(R.id.mini_total_time);
            miniPlayPause = findViewById(R.id.mini_play_pause);
            miniPrev = findViewById(R.id.mini_prev);
            miniNext = findViewById(R.id.mini_next);
            miniSeekBar = findViewById(R.id.mini_seekbar);

            miniPlayPause.setOnClickListener(v -> {
                if (playerManager.getCurrentTrack() != null) {
                    playerManager.togglePlayPause(this);
                }
            });
            miniPrev.setOnClickListener(v -> {
                playerManager.previous();
                playerManager.play(this);
            });
            miniNext.setOnClickListener(v -> {
                playerManager.next();
                playerManager.play(this);
            });

            miniSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                    if (fromUser) {
                        playerManager.seekTo(progress);
                        miniCurrentTime.setText(formatTime(progress));
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

            // Tap mini player to open full player
            miniPlayer.setOnClickListener(v -> {
                if (playerManager.getCurrentTrack() != null) {
                    startActivity(new Intent(this, FullPlayerActivity.class));
                }
            });

            // Register player callbacks
            playerManager.addCallback(new PlayerManager.PlayerCallback() {
                @Override
                public void onTrackChanged(AudioFile track) {
                    runOnUiThread(() -> updateMiniPlayer(track));
                }

                @Override
                public void onPlayStateChanged(boolean isPlaying) {
                    runOnUiThread(() -> {
                        miniPlayPause.setImageResource(isPlaying ?
                            android.R.drawable.ic_media_pause :
                            android.R.drawable.ic_media_play);
                    });
                }

                @Override
                public void onProgressChanged(int position, int duration) {
                    runOnUiThread(() -> {
                        if (!isUserSeeking) {
                            miniSeekBar.setMax(duration > 0 ? duration : 100);
                            miniSeekBar.setProgress(position);
                            miniCurrentTime.setText(formatTime(position));
                            miniTotalTime.setText(formatTime(duration));
                        }
                    });
                }

                @Override
                public void onSpeedChanged(float speed) {}

                @Override
                public void onSleepTimerChanged(int remainingSeconds) {}
            });

            log("requesting permissions");
            requestPermissions();
            log("onCreate complete");
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String stack = "onCreate crash:\n" + sw.toString() + "\n\nDebug log:\n" + debugLog.toString();
            Log.e(TAG, stack);
            Intent intent = new Intent(this, CrashActivity.class);
            intent.putExtra("error", stack);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void updateMiniPlayer(AudioFile track) {
        if (track == null) {
            miniPlayer.setVisibility(View.GONE);
            return;
        }
        miniPlayer.setVisibility(View.VISIBLE);
        miniTitle.setText(track.getSafeTitle());
        miniArtist.setText(track.getArtistOrAlbum());
        miniPlayPause.setImageResource(playerManager.isPlaying() ?
            android.R.drawable.ic_media_pause :
            android.R.drawable.ic_media_play);
    }

    private void requestPermissions() {
        String perm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ? Manifest.permission.READ_MEDIA_AUDIO
            : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{perm}, PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == PERMISSION_REQUEST && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            // Refresh library fragment
            Fragment frag = getSupportFragmentManager().findFragmentByTag("f0");
            if (frag instanceof LibraryFragment) {
                ((LibraryFragment) frag).refresh();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playerManager.removeCallback(null); // clear all
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
