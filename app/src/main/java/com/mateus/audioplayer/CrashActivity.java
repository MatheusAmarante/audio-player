package com.mateus.audioplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.PrintWriter;
import java.io.StringWriter;

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
        sv.addView(tv);
        setContentView(sv);
    }
}
