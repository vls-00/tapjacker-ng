package com.tapjacker.ng;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

/** Swatches, hex field, alpha slider. */
public final class ColorPickerDialog {
    private ColorPickerDialog() {}

    public interface Callback {
        void onColor(int argb);
    }

    private static final int[] SWATCHES = {
            0xFFFFFFFF, 0xFFECEFF1, 0xFF9E9E9E, 0xFF424242, 0xFF212121, 0xFF000000,
            0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7, 0xFF3F51B5, 0xFF2962FF,
            0xFF03A9F4, 0xFF00BCD4, 0xFF009688, 0xFF00C853, 0xFF8BC34A, 0xFFCDDC39,
            0xFFFFEB3B, 0xFFFFC107, 0xFFFF9800, 0xFFFF5722, 0xFF795548, 0xFF1B2A41,
    };

    public static void show(Context ctx, String title, int initial, Callback cb) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.dialog_color_picker, null);
        GridLayout grid = v.findViewById(R.id.swatchGrid);
        TextInputEditText hex = v.findViewById(R.id.etHex);
        Slider alpha = v.findViewById(R.id.sliderAlpha);
        View preview = v.findViewById(R.id.colorPreview);

        final int[] current = {initial};
        final boolean[] updating = {false};

        Runnable refresh = () -> {
            updating[0] = true;
            GradientDrawable d = new GradientDrawable();
            d.setColor(current[0]);
            d.setCornerRadius(OverlayRenderer.dp(ctx, 8));
            d.setStroke(OverlayRenderer.dp(ctx, 1), 0x66000000);
            preview.setBackground(d);
            String h = ColorUtil.toHex(current[0]);
            if (hex.getText() == null || !h.equalsIgnoreCase(hex.getText().toString().trim())) hex.setText(h);
            alpha.setValue(Color.alpha(current[0]));
            updating[0] = false;
        };

        int size = OverlayRenderer.dp(ctx, 40);
        int margin = OverlayRenderer.dp(ctx, 4);
        for (int c : SWATCHES) {
            View sw = new View(ctx);
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(c);
            d.setStroke(OverlayRenderer.dp(ctx, 1), 0x66000000);
            sw.setBackground(d);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = size;
            lp.height = size;
            lp.setMargins(margin, margin, margin, margin);
            sw.setLayoutParams(lp);
            sw.setOnClickListener(x -> {
                int a = Math.round(alpha.getValue());
                current[0] = (a << 24) | (c & 0x00FFFFFF);
                refresh.run();
            });
            grid.addView(sw);
        }

        hex.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (updating[0]) return;
                int parsed = ColorUtil.parse(s.toString(), Integer.MIN_VALUE);
                if (parsed != Integer.MIN_VALUE) {
                    current[0] = parsed;
                    refresh.run();
                }
            }
        });
        alpha.addOnChangeListener((s, value, fromUser) -> {
            if (!fromUser) return;
            current[0] = (Math.round(value) << 24) | (current[0] & 0x00FFFFFF);
            refresh.run();
        });
        refresh.run();

        new MaterialAlertDialogBuilder(ctx)
                .setTitle(title)
                .setView(v)
                .setPositiveButton("Use colour", (d, w) -> cb.onColor(current[0]))
                .setNegativeButton("Cancel", null)
                .show();
    }
}
