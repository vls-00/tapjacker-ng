package com.tapjacker.ng;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

/** Add or edit one element. */
public final class ElementEditorDialog {
    private ElementEditorDialog() {}

    public interface Callback {
        void onSaved(OverlayElement element);
    }

    public static void show(Context ctx, OverlayElement original, boolean isNew, Callback cb) {
        final OverlayElement e = original.copy();
        View v = LayoutInflater.from(ctx).inflate(R.layout.dialog_element_editor, null);

        MaterialButtonToggleGroup typeGroup = v.findViewById(R.id.toggleType);
        View groupText = v.findViewById(R.id.groupTextFields);
        TextInputEditText etText = v.findViewById(R.id.etText);
        Slider sliderTextSize = v.findViewById(R.id.sliderTextSize);
        TextView tvTextSize = v.findViewById(R.id.tvTextSize);
        MaterialSwitch switchBold = v.findViewById(R.id.switchBold);
        View swTextColor = v.findViewById(R.id.swTextColor);
        View rowTextColor = v.findViewById(R.id.rowTextColor);
        View swBgColor = v.findViewById(R.id.swBgColor);
        View rowBgColor = v.findViewById(R.id.rowBgColor);
        View swStrokeColor = v.findViewById(R.id.swStrokeColor);
        View rowStrokeColor = v.findViewById(R.id.rowStrokeColor);
        TextInputEditText etStroke = v.findViewById(R.id.etStrokeWidth);
        AutoCompleteTextView acAnchor = v.findViewById(R.id.acAnchor);
        TextInputEditText etWidth = v.findViewById(R.id.etWidth);
        TextInputEditText etHeight = v.findViewById(R.id.etHeight);
        TextInputEditText etOffsetX = v.findViewById(R.id.etOffsetX);
        TextInputEditText etOffsetY = v.findViewById(R.id.etOffsetY);
        TextInputEditText etCorner = v.findViewById(R.id.etCorner);
        TextInputEditText etPadding = v.findViewById(R.id.etPadding);

        Runnable paintSwatches = () -> {
            swatch(ctx, swTextColor, e.textColor);
            swatch(ctx, swBgColor, e.bgColor);
            swatch(ctx, swStrokeColor, e.strokeColor);
        };

        Runnable fill = () -> {
            etText.setText(e.text);
            sliderTextSize.setValue(Math.max(8, Math.min(72, Math.round(e.textSizeSp))));
            tvTextSize.setText(String.format(Locale.ROOT, "Text size: %d sp", Math.round(sliderTextSize.getValue())));
            switchBold.setChecked(e.bold);
            etStroke.setText(String.valueOf(e.strokeWidthDp));
            acAnchor.setText(e.anchor.label, false);
            etWidth.setText(String.valueOf(e.widthDp));
            etHeight.setText(String.valueOf(e.heightDp));
            etOffsetX.setText(String.valueOf(e.offsetXDp));
            etOffsetY.setText(String.valueOf(e.offsetYDp));
            etCorner.setText(String.valueOf(e.cornerRadiusDp));
            etPadding.setText(String.valueOf(e.paddingDp));
            boolean isBox = e.type == OverlayElement.Type.BOX;
            groupText.setVisibility(isBox ? View.GONE : View.VISIBLE);
            paintSwatches.run();
        };

        int checkedId = e.type == OverlayElement.Type.TEXT ? R.id.btnTypeText
                : e.type == OverlayElement.Type.BUTTON ? R.id.btnTypeButton : R.id.btnTypeBox;
        typeGroup.check(checkedId);
        typeGroup.addOnButtonCheckedListener((group, id, isChecked) -> {
            if (!isChecked) return;
            OverlayElement.Type t = id == R.id.btnTypeText ? OverlayElement.Type.TEXT
                    : id == R.id.btnTypeButton ? OverlayElement.Type.BUTTON : OverlayElement.Type.BOX;
            if (t == e.type) return;
            // take the new type's defaults, keep text and placement
            OverlayElement d = OverlayElement.defaults(t);
            String keepText = etText.getText() == null ? e.text : etText.getText().toString();
            OverlayElement.Anchor keepAnchor = e.anchor;
            int kx = parseInt(etOffsetX, e.offsetXDp), ky = parseInt(etOffsetY, e.offsetYDp);
            e.type = t;
            e.text = keepText.isEmpty() ? d.text : keepText;
            e.textColor = d.textColor;
            e.bgColor = d.bgColor;
            e.textSizeSp = d.textSizeSp;
            e.bold = d.bold;
            e.widthDp = d.widthDp;
            e.heightDp = d.heightDp;
            e.cornerRadiusDp = d.cornerRadiusDp;
            e.paddingDp = d.paddingDp;
            e.anchor = keepAnchor;
            e.offsetXDp = kx;
            e.offsetYDp = ky;
            fill.run();
        });

        sliderTextSize.addOnChangeListener((s, value, fromUser) ->
                tvTextSize.setText(String.format(Locale.ROOT, "Text size: %d sp", Math.round(value))));

        acAnchor.setAdapter(new ArrayAdapter<>(ctx, android.R.layout.simple_list_item_1, OverlayElement.Anchor.labels()));
        acAnchor.setOnItemClickListener((parent, view, position, id) -> e.anchor = OverlayElement.Anchor.values()[position]);

        rowTextColor.setOnClickListener(x -> ColorPickerDialog.show(ctx, "Text colour", e.textColor, c -> { e.textColor = c; paintSwatches.run(); }));
        rowBgColor.setOnClickListener(x -> ColorPickerDialog.show(ctx, "Background colour", e.bgColor, c -> { e.bgColor = c; paintSwatches.run(); }));
        rowStrokeColor.setOnClickListener(x -> ColorPickerDialog.show(ctx, "Border colour", e.strokeColor, c -> { e.strokeColor = c; paintSwatches.run(); }));

        fill.run();

        new MaterialAlertDialogBuilder(ctx)
                .setTitle(isNew ? "Add element" : "Edit element")
                .setView(v)
                .setPositiveButton("Save", (d, w) -> {
                    e.text = etText.getText() == null ? "" : etText.getText().toString();
                    e.textSizeSp = sliderTextSize.getValue();
                    e.bold = switchBold.isChecked();
                    e.strokeWidthDp = Math.max(0, parseInt(etStroke, 0));
                    e.widthDp = Math.max(0, parseInt(etWidth, 0));
                    e.heightDp = Math.max(0, parseInt(etHeight, 0));
                    e.offsetXDp = parseInt(etOffsetX, 0);
                    e.offsetYDp = parseInt(etOffsetY, 0);
                    e.cornerRadiusDp = Math.max(0, parseInt(etCorner, 0));
                    e.paddingDp = Math.max(0, parseInt(etPadding, 0));
                    if (e.type == OverlayElement.Type.BOX && (e.widthDp == 0 || e.heightDp == 0)) {
                        e.widthDp = e.widthDp == 0 ? 120 : e.widthDp;
                        e.heightDp = e.heightDp == 0 ? 120 : e.heightDp;
                    }
                    cb.onSaved(e);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static int parseInt(TextInputEditText et, int fallback) {
        try {
            return Integer.parseInt(String.valueOf(et.getText()).trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    static void swatch(Context ctx, View v, int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(OverlayRenderer.dp(ctx, 8));
        d.setStroke(OverlayRenderer.dp(ctx, 1), 0x66000000);
        v.setBackground(d);
    }
}
