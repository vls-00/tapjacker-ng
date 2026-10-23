package com.tapjacker.ng;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/** Saved presets: thumbnail per row, tap to load, long-press to rename, bin to delete. */
public final class PresetsDialog {
    private PresetsDialog() {}

    public interface Callback {
        void onLoad(OverlayConfig cfg);
    }

    public static void show(Context ctx, ConfigStore store, OverlayConfig current, Rect screen, Callback cb) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.dialog_presets, null);
        LinearLayout list = v.findViewById(R.id.presetList);
        View empty = v.findViewById(R.id.tvEmpty);

        AlertDialog dlg = new MaterialAlertDialogBuilder(ctx)
                .setTitle("My presets")
                .setView(v)
                .setNeutralButton("Save current…", null)
                .setPositiveButton("Close", null)
                .create();

        Runnable refresh = () -> populate(ctx, store, screen, list, empty, dlg, cb);
        dlg.setOnShowListener(d -> {
            refresh.run();
            // handled by hand so the dialog stays open
            dlg.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(x -> promptSave(ctx, store, current, refresh));
        });
        dlg.show();
    }

    private static void populate(Context ctx, ConfigStore store, Rect screen, LinearLayout list, View empty,
                                 AlertDialog dlg, Callback cb) {
        list.removeAllViews();
        List<String> names = store.presetNames();
        empty.setVisibility(names.isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inf = LayoutInflater.from(ctx);
        for (String name : names) {
            OverlayConfig cfg = store.loadPreset(name);
            if (cfg == null) continue;
            View row = inf.inflate(R.layout.item_preset, list, false);
            ScaledPreviewView thumb = row.findViewById(R.id.thumb);
            thumb.setScreenSize(screen.width(), screen.height());
            thumb.setConfig(cfg);
            ((TextView) row.findViewById(R.id.tvName)).setText(name);
            ((TextView) row.findViewById(R.id.tvSummary)).setText(cfg.summary());
            row.setOnClickListener(x -> {
                cb.onLoad(cfg);
                dlg.dismiss();
                Toast.makeText(ctx, "Loaded \"" + name + "\"", Toast.LENGTH_SHORT).show();
            });
            row.setOnLongClickListener(x -> {
                promptRename(ctx, store, name, () -> populate(ctx, store, screen, list, empty, dlg, cb));
                return true;
            });
            row.findViewById(R.id.btnDelete).setOnClickListener(x -> new MaterialAlertDialogBuilder(ctx)
                    .setTitle("Delete \"" + name + "\"?")
                    .setPositiveButton("Delete", (d, w) -> {
                        store.deletePreset(name);
                        populate(ctx, store, screen, list, empty, dlg, cb);
                    })
                    .setNegativeButton("Cancel", null)
                    .show());
            list.addView(row);
        }
    }

    public static void promptSave(Context ctx, ConfigStore store, OverlayConfig cfg, Runnable after) {
        EditText input = nameField(ctx, "");
        new MaterialAlertDialogBuilder(ctx)
                .setTitle("Save preset")
                .setView(wrap(ctx, input))
                .setPositiveButton("Save", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) return;
                    if (store.hasPreset(name)) {
                        new MaterialAlertDialogBuilder(ctx)
                                .setTitle("Replace \"" + name + "\"?")
                                .setMessage("A preset with that name already exists.")
                                .setPositiveButton("Replace", (dd, ww) -> {
                                    store.savePreset(name, cfg);
                                    Toast.makeText(ctx, "Saved", Toast.LENGTH_SHORT).show();
                                    if (after != null) after.run();
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                        return;
                    }
                    store.savePreset(name, cfg);
                    Toast.makeText(ctx, "Saved \"" + name + "\"", Toast.LENGTH_SHORT).show();
                    if (after != null) after.run();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void promptRename(Context ctx, ConfigStore store, String oldName, Runnable after) {
        EditText input = nameField(ctx, oldName);
        new MaterialAlertDialogBuilder(ctx)
                .setTitle("Rename preset")
                .setView(wrap(ctx, input))
                .setPositiveButton("Rename", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty() || name.equals(oldName)) return;
                    if (store.hasPreset(name)) {
                        Toast.makeText(ctx, "\"" + name + "\" already exists", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    store.renamePreset(oldName, name);
                    after.run();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static EditText nameField(Context ctx, String initial) {
        EditText input = new EditText(ctx);
        input.setHint("Preset name");
        input.setSingleLine(true);
        input.setText(initial);
        input.setSelection(initial.length());
        return input;
    }

    private static View wrap(Context ctx, View child) {
        FrameLayout wrap = new FrameLayout(ctx);
        int pad = OverlayRenderer.dp(ctx, 24);
        wrap.setPadding(pad, 0, pad, 0);
        wrap.addView(child);
        return wrap;
    }
}
