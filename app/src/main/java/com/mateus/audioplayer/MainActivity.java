package com.mateus.audioplayer;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "AudioPlayer";

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNav;

    // Mini player views
    private View miniPlayer;
    private ImageView miniThumbnail;
    private TextView miniTitle, miniArtist;
    private ImageButton miniPlayPause, miniNext;

    private PlayerManager playerManager;
    private final Handler handler = new Handler(Looper.getMainLooper());

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
            } catch (Exception e2) {
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
            viewPager.setUserInputEnabled(false);

            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_search) {
                    viewPager.setCurrentItem(0, true);
                    return true;
                } else if (id == R.id.nav_playlists) {
                    viewPager.setCurrentItem(1, true);
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
            miniThumbnail = findViewById(R.id.mini_thumbnail);
            miniTitle = findViewById(R.id.mini_title);
            miniArtist = findViewById(R.id.mini_artist);
            miniPlayPause = findViewById(R.id.mini_play_pause);
            miniNext = findViewById(R.id.mini_next);

            miniPlayPause.setOnClickListener(v -> {
                if (playerManager.getCurrentTrack() != null) {
                    playerManager.togglePlayPause(this);
                }
            });
            miniNext.setOnClickListener(v -> {
                playerManager.next();
                playerManager.play(this);
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
                public void onTrackChanged(YouTubeTrack track) {
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
                public void onProgressChanged(int position, int duration) {}

                @Override
                public void onSpeedChanged(float speed) {}

                @Override
                public void onSleepTimerChanged(int remainingSeconds) {}
            });

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

    private void updateMiniPlayer(YouTubeTrack track) {
        if (track == null) {
            miniPlayer.setVisibility(View.GONE);
            return;
        }
        miniPlayer.setVisibility(View.VISIBLE);
        miniTitle.setText(track.title);
        miniArtist.setText(track.getArtistOrUploader());
        miniPlayPause.setImageResource(playerManager.isPlaying() ?
            android.R.drawable.ic_media_pause :
            android.R.drawable.ic_media_play);

        if (track.thumbnailUrl != null && !track.thumbnailUrl.isEmpty()) {
            Glide.with(this).load(track.thumbnailUrl)
                .placeholder(R.drawable.album_art_placeholder)
                .into(miniThumbnail);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (playerManager != null) {
            playerManager.removeCallback(null);
        }
    }
}
