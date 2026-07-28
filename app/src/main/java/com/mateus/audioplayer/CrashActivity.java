package com.mateus.audioplayer;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;

public class CrashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String error = getIntent().getStringExtra("error");
        ScrollView sv = new ScrollView(this);
        TextView tv = new TextView(this);
        tv.setText("CRASH:\n\n" + (error != null ? error : "Unknown error"));
        tv.setTextSize(12);
        tv.setPadding(32, 32, 32, 32);
        tv.setTextColor(0xFFc9d1d9);
        sv.setBackgroundColor(0xFF0d1117);
        sv.addView(tv);
        setContentView(sv);
    }
}
