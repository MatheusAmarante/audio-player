package com.mateus.audioplayer;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class CrashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String error = getIntent().getStringExtra("error");
        if (error == null) error = "Unknown error";

        final String finalError = error;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);
        root.setBackgroundColor(0xFF0d1117);

        // Title
        TextView title = new TextView(this);
        title.setText("💥 App Crash");
        title.setTextColor(0xFFe74c3c);
        title.setTextSize(20);
        title.setPadding(0, 0, 0, 16);
        root.addView(title);

        // Error text
        ScrollView sv = new ScrollView(this);
        TextView tv = new TextView(this);
        tv.setText(error);
        tv.setTextColor(0xFFc9d1d9);
        tv.setTextSize(11);
        tv.setTypeface(android.graphics.Typeface.MONOSPACE);
        tv.setPadding(16, 16, 16, 16);
        tv.setBackgroundColor(0xFF161b22);
        tv.setMovementMethod(new ScrollingMovementMethod());
        sv.addView(tv);
        root.addView(sv, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        // Buttons
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setPadding(0, 16, 0, 0);

        Button btnCopy = new Button(this);
        btnCopy.setText("📋 Copiar Erro");
        btnCopy.setTextColor(0xFFFFFFFF);
        btnCopy.setBackgroundColor(0xFF1e40af);
        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("crash", finalError);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "✅ Erro copiado!", Toast.LENGTH_SHORT).show();
        });

        Button btnShare = new Button(this);
        btnShare.setText("📤 Compartilhar");
        btnShare.setTextColor(0xFFFFFFFF);
        btnShare.setBackgroundColor(0xFF238636);
        btnShare.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, finalError);
            startActivity(Intent.createChooser(intent, "Compartilhar erro"));
        });

        Button btnRestart = new Button(this);
        btnRestart.setText("🔄 Reiniciar");
        btnRestart.setTextColor(0xFFFFFFFF);
        btnRestart.setBackgroundColor(0xFF30363d);
        btnRestart.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        btnParams.setMargins(0, 0, 8, 0);
        buttons.addView(btnCopy, btnParams);
        buttons.addView(btnShare, btnParams);
        buttons.addView(btnRestart, btnParams);
        root.addView(buttons);

        setContentView(root);
    }
}
