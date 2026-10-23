package com.tapjacker.ng;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

/** Lists launchable apps with a filter box. */
public final class AppPickerDialog {
    private AppPickerDialog() {}

    public interface Callback {
        void onPicked(String packageName, String label);
    }

    private static class AppEntry {
        final String label;
        final String pkg;
        final ResolveInfo ri;
        Drawable icon;

        AppEntry(String label, String pkg, ResolveInfo ri) {
            this.label = label;
            this.pkg = pkg;
            this.ri = ri;
        }
    }

    public static void show(Context ctx, Callback cb) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.dialog_app_picker, null);
        EditText filter = v.findViewById(R.id.etFilter);
        ListView list = v.findViewById(R.id.listApps);
        ProgressBar progress = v.findViewById(R.id.progress);

        PackageManager pm = ctx.getPackageManager();
        List<AppEntry> all = new ArrayList<>();
        ArrayAdapter<AppEntry> adapter = new ArrayAdapter<AppEntry>(ctx, R.layout.item_app) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View row = convertView != null ? convertView : LayoutInflater.from(ctx).inflate(R.layout.item_app, parent, false);
                AppEntry e = getItem(position);
                if (e == null) return row;
                if (e.icon == null) e.icon = e.ri.loadIcon(pm);
                ((ImageView) row.findViewById(R.id.ivIcon)).setImageDrawable(e.icon);
                ((TextView) row.findViewById(R.id.tvLabel)).setText(e.label);
                ((TextView) row.findViewById(R.id.tvPackage)).setText(e.pkg);
                return row;
            }
        };
        list.setAdapter(adapter);

        AlertDialog dlg = new MaterialAlertDialogBuilder(ctx)
                .setTitle("Pick target app")
                .setView(v)
                .setNegativeButton("Cancel", null)
                .create();

        Runnable applyFilter = () -> {
            String q = filter.getText().toString().trim().toLowerCase(Locale.ROOT);
            adapter.clear();
            for (AppEntry e : all) {
                if (q.isEmpty() || e.label.toLowerCase(Locale.ROOT).contains(q) || e.pkg.toLowerCase(Locale.ROOT).contains(q)) {
                    adapter.add(e);
                }
            }
        };
        filter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { applyFilter.run(); }
        });
        list.setOnItemClickListener((parent, view, position, id) -> {
            AppEntry e = adapter.getItem(position);
            if (e != null) cb.onPicked(e.pkg, e.label);
            dlg.dismiss();
        });

        Handler main = new Handler(Looper.getMainLooper());
        Executors.newSingleThreadExecutor().execute(() -> {
            Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> ris = pm.queryIntentActivities(launcher, PackageManager.ResolveInfoFlags.of(0));
            List<AppEntry> found = new ArrayList<>();
            for (ResolveInfo ri : ris) {
                String pkg = ri.activityInfo.packageName;
                if (ctx.getPackageName().equals(pkg)) continue;
                found.add(new AppEntry(String.valueOf(ri.loadLabel(pm)), pkg, ri));
            }
            Collections.sort(found, (a, b) -> a.label.compareToIgnoreCase(b.label));
            main.post(() -> {
                all.addAll(found);
                progress.setVisibility(View.GONE);
                applyFilter.run();
            });
        });

        dlg.show();
    }
}
