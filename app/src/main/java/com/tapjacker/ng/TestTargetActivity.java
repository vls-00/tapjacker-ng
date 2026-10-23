package com.tapjacker.ng;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

/** Quick way to check geometry and pass-through without a real target. */
public class TestTargetActivity extends AppCompatActivity {
    private int taps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_target);
        TextView count = findViewById(R.id.tvTapCount);
        findViewById(R.id.btnApprove).setOnClickListener(v -> {
            taps++;
            count.setText(String.format(Locale.ROOT, "Taps that got through: %d", taps));
            Toast.makeText(this, "Tap reached the target", Toast.LENGTH_SHORT).show();
        });
    }
}
