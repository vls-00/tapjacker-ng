package com.tapjacker.ng;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OverlayService.Listener {

    private ConfigStore store;
    private OverlayConfig cfg;
    private boolean binding;
    private int screenWdp, screenHdp;
    private float maxObscuringOpacity = 1.0f;
    private final Handler handler = new Handler(Looper.getMainLooper());
    // the limit can change under us (adb, root, the buttons below)
    private final ContentObserver limitObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange) {
            refreshLimit();
        }
    };
    private final Runnable pushToService = () -> {
        if (OverlayService.isRunning()) startService(OverlayService.updateIntent(this, cfg));
    };

    private TextView tvSystemInfo, tvOverlayPerm, tvRunningStatus, tvPassthroughHint, tvOpacityValue, tvOpacityWarning;
    private TextView tvLimitStatus;
    private View groupLimit, btnLiftLimit, btnRestoreLimit;
    private TextView tvWidth, tvHeight, tvX, tvY, tvCorner, tvBgColorHex, tvNoElements, tvTargetApp, tvDelay, tvDismiss;
    private View btnGrantOverlay, btnStart, btnStop, groupPartial, swBgColor, rowApp, tilComponent;
    private MaterialButtonToggleGroup toggleMode;
    private MaterialSwitch switchPassthrough;
    private Slider sliderWidth, sliderHeight, sliderX, sliderY, sliderCorner, sliderOpacity, sliderDelay, sliderDismiss;
    private LinearLayout elementsContainer;
    private RadioGroup rgTarget;
    private TextInputEditText etComponent;
    private ScaledPreviewView preview;

    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> launchNow());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        store = new ConfigStore(this);
        cfg = store.loadCurrent();

        Rect bounds = screenBounds();
        float density = getResources().getDisplayMetrics().density;
        screenWdp = Math.round(bounds.width() / density);
        screenHdp = Math.round(bounds.height() / density);
        maxObscuringOpacity = TouchFilter.limit(this);

        findViews();
        applyInsets();
        preview.setScreenSize(bounds.width(), bounds.height());
        wire();
        bindAll();
    }

    // full display size including the bars; WindowMetrics is API 30
    @SuppressWarnings("deprecation")
    private Rect screenBounds() {
        WindowManager wm = getSystemService(WindowManager.class);
        if (Build.VERSION.SDK_INT >= 30) return wm.getMaximumWindowMetrics().getBounds();
        android.graphics.Point p = new android.graphics.Point();
        wm.getDefaultDisplay().getRealSize(p);
        return new Rect(0, 0, p.x, p.y);
    }

    // header extends under the status bar, list gets room above the nav bar
    private void applyInsets() {
        View header = findViewById(R.id.header);
        View scroll = findViewById(R.id.scroll);
        int headerTop = header.getPaddingTop();
        int scrollBottom = scroll.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            header.setPadding(header.getPaddingLeft(), headerTop + bars.top, header.getPaddingRight(), header.getPaddingBottom());
            scroll.setPadding(scroll.getPaddingLeft(), scroll.getPaddingTop(), scroll.getPaddingRight(), scrollBottom + bars.bottom);
            return insets;
        });
    }

    private void findViews() {
        tvSystemInfo = findViewById(R.id.tvSystemInfo);
        tvOverlayPerm = findViewById(R.id.tvOverlayPerm);
        tvRunningStatus = findViewById(R.id.tvRunningStatus);
        tvPassthroughHint = findViewById(R.id.tvPassthroughHint);
        tvOpacityValue = findViewById(R.id.tvOpacityValue);
        tvOpacityWarning = findViewById(R.id.tvOpacityWarning);
        tvLimitStatus = findViewById(R.id.tvLimitStatus);
        groupLimit = findViewById(R.id.groupLimit);
        btnLiftLimit = findViewById(R.id.btnLiftLimit);
        btnRestoreLimit = findViewById(R.id.btnRestoreLimit);
        tvWidth = findViewById(R.id.tvWidth);
        tvHeight = findViewById(R.id.tvHeight);
        tvX = findViewById(R.id.tvX);
        tvY = findViewById(R.id.tvY);
        tvCorner = findViewById(R.id.tvCorner);
        tvBgColorHex = findViewById(R.id.tvBgColorHex);
        tvNoElements = findViewById(R.id.tvNoElements);
        tvTargetApp = findViewById(R.id.tvTargetApp);
        tvDelay = findViewById(R.id.tvDelay);
        tvDismiss = findViewById(R.id.tvDismiss);
        btnGrantOverlay = findViewById(R.id.btnGrantOverlay);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        groupPartial = findViewById(R.id.groupPartial);
        swBgColor = findViewById(R.id.swBgColor);
        rowApp = findViewById(R.id.rowApp);
        tilComponent = findViewById(R.id.tilComponent);
        toggleMode = findViewById(R.id.toggleMode);
        switchPassthrough = findViewById(R.id.switchPassthrough);
        sliderWidth = findViewById(R.id.sliderWidth);
        sliderHeight = findViewById(R.id.sliderHeight);
        sliderX = findViewById(R.id.sliderX);
        sliderY = findViewById(R.id.sliderY);
        sliderCorner = findViewById(R.id.sliderCorner);
        sliderOpacity = findViewById(R.id.sliderOpacity);
        sliderDelay = findViewById(R.id.sliderDelay);
        sliderDismiss = findViewById(R.id.sliderDismiss);
        elementsContainer = findViewById(R.id.elementsContainer);
        rgTarget = findViewById(R.id.rgTarget);
        etComponent = findViewById(R.id.etComponent);
        preview = findViewById(R.id.previewView);
    }

    private void wire() {
        bindSystemInfo();
        groupLimit.setVisibility(TouchFilter.exists() ? View.VISIBLE : View.GONE);
        btnLiftLimit.setOnClickListener(v -> changeLimit(1.0f));
        btnRestoreLimit.setOnClickListener(v -> changeLimit(-1f));

        btnGrantOverlay.setOnClickListener(v -> openOverlaySettings());
        btnStart.setOnClickListener(v -> startOverlay());
        btnStop.setOnClickListener(v -> stopService(new Intent(this, OverlayService.class)));

        toggleMode.addOnButtonCheckedListener((group, id, checked) -> {
            if (binding || !checked) return;
            cfg.mode = id == R.id.btnModeFull ? OverlayConfig.Mode.FULL : OverlayConfig.Mode.PARTIAL;
            bindMode();
            onChanged();
        });
        switchPassthrough.setOnCheckedChangeListener((b, checked) -> {
            if (binding) return;
            cfg.passthrough = checked;
            bindMode();
            bindOpacity();
            onChanged();
        });

        sliderWidth.setValueTo(Math.max(41, screenWdp));
        sliderHeight.setValueTo(Math.max(41, screenHdp));
        sliderX.setValueTo(Math.max(1, screenWdp));
        sliderY.setValueTo(Math.max(1, screenHdp));
        bindSlider(sliderWidth, tvWidth, "Width: %d dp", v -> cfg.partialWidthDp = v);
        bindSlider(sliderHeight, tvHeight, "Height: %d dp", v -> cfg.partialHeightDp = v);
        bindSlider(sliderX, tvX, "X: %d dp from the left", v -> cfg.partialXDp = v);
        bindSlider(sliderY, tvY, "Y: %d dp from the top", v -> cfg.partialYDp = v);
        bindSlider(sliderCorner, tvCorner, "Corner radius: %d dp", v -> cfg.cornerRadiusDp = v);
        bindSlider(sliderDelay, tvDelay, "Delay before the overlay appears: %d ms", v -> cfg.launchDelayMs = v);
        bindSlider(sliderDismiss, tvDismiss, "Auto-dismiss after: %d s (0 = never)", v -> cfg.autoDismissSec = v);

        sliderOpacity.setLabelFormatter(v -> String.format(Locale.ROOT, "%d%%", Math.round(v * 100)));
        sliderOpacity.addOnChangeListener((s, value, fromUser) -> {
            if (binding || !fromUser) return;
            cfg.opacity = OverlayConfig.clampOpacity(value);
            bindOpacity();
            onChanged();
        });
        findViewById(R.id.rowBgColor).setOnClickListener(v -> ColorPickerDialog.show(this, "Overlay background", cfg.bgColor, c -> {
            cfg.bgColor = c;
            bindBg();
            onChanged();
        }));
        findViewById(R.id.btnAddText).setOnClickListener(v -> addElement(OverlayElement.Type.TEXT));
        findViewById(R.id.btnAddButton).setOnClickListener(v -> addElement(OverlayElement.Type.BUTTON));
        findViewById(R.id.btnAddBox).setOnClickListener(v -> addElement(OverlayElement.Type.BOX));

        rgTarget.setOnCheckedChangeListener((g, id) -> {
            if (binding) return;
            if (id == R.id.rbTargetBuiltin) cfg.targetMode = OverlayConfig.TargetMode.BUILTIN;
            else if (id == R.id.rbTargetApp) cfg.targetMode = OverlayConfig.TargetMode.APP;
            else if (id == R.id.rbTargetComponent) cfg.targetMode = OverlayConfig.TargetMode.COMPONENT;
            else cfg.targetMode = OverlayConfig.TargetMode.NONE;
            bindTarget();
            onChanged();
        });
        findViewById(R.id.btnPickApp).setOnClickListener(v -> AppPickerDialog.show(this, (pkg, label) -> {
            cfg.targetPackage = pkg;
            cfg.targetLabel = label;
            bindTarget();
            onChanged();
        }));
        etComponent.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (binding) return;
                cfg.targetComponent = s.toString().trim();
                store.saveCurrent(cfg);
            }
        });

        findViewById(R.id.btnTemplates).setOnClickListener(v -> showTemplates());
        findViewById(R.id.btnSavePreset).setOnClickListener(v -> savePreset());
        findViewById(R.id.btnLoadPreset).setOnClickListener(v -> showPresets());
        findViewById(R.id.btnCopyJson).setOnClickListener(v -> {
            ClipboardManager cm = getSystemService(ClipboardManager.class);
            cm.setPrimaryClip(ClipData.newPlainText("tapjacker-config", cfg.toJsonString()));
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.btnPasteJson).setOnClickListener(v -> pasteJson());
    }

    private interface IntSetter { void set(int v); }

    private void bindSlider(Slider slider, TextView label, String fmt, IntSetter setter) {
        slider.addOnChangeListener((s, value, fromUser) -> {
            label.setText(String.format(Locale.ROOT, fmt, Math.round(value)));
            if (binding || !fromUser) return;
            setter.set(Math.round(value));
            onChanged();
        });
    }

    // Slider throws if the value is off-grid or out of range
    private static void setClamped(Slider s, int value) {
        float v = Math.max(s.getValueFrom(), Math.min(s.getValueTo(), value));
        float step = s.getStepSize();
        if (step > 0) v = s.getValueFrom() + Math.round((v - s.getValueFrom()) / step) * step;
        s.setValue(v);
    }

    private void bindAll() {
        binding = true;
        toggleMode.check(cfg.mode == OverlayConfig.Mode.FULL ? R.id.btnModeFull : R.id.btnModePartial);
        switchPassthrough.setChecked(cfg.passthrough);
        setClamped(sliderWidth, cfg.partialWidthDp);
        setClamped(sliderHeight, cfg.partialHeightDp);
        setClamped(sliderX, cfg.partialXDp);
        setClamped(sliderY, cfg.partialYDp);
        setClamped(sliderCorner, cfg.cornerRadiusDp);
        setClamped(sliderDelay, cfg.launchDelayMs);
        setClamped(sliderDismiss, cfg.autoDismissSec);
        sliderOpacity.setValue(OverlayConfig.clampOpacity(cfg.opacity));
        etComponent.setText(cfg.targetComponent);
        int rb = cfg.targetMode == OverlayConfig.TargetMode.BUILTIN ? R.id.rbTargetBuiltin
                : cfg.targetMode == OverlayConfig.TargetMode.APP ? R.id.rbTargetApp
                : cfg.targetMode == OverlayConfig.TargetMode.COMPONENT ? R.id.rbTargetComponent : R.id.rbTargetNone;
        rgTarget.check(rb);
        binding = false;
        bindMode();
        bindOpacity();
        bindBg();
        bindElements();
        bindTarget();
        refreshStatus();
        preview.setConfig(cfg);
    }

    private void bindMode() {
        groupPartial.setVisibility(cfg.mode == OverlayConfig.Mode.PARTIAL ? View.VISIBLE : View.GONE);
        if (overLimit()) {
            tvPassthroughHint.setText(String.format(Locale.ROOT,
                    "Above the %.2f system limit: the overlay is drawn at full opacity and touches WILL NOT go through. Long-press the overlay to dismiss it.",
                    maxObscuringOpacity));
        } else if (cfg.passthrough) {
            tvPassthroughHint.setText("FLAG_NOT_TOUCHABLE: taps go to whatever is underneath. Stop the overlay from the notification or from here.");
        } else {
            tvPassthroughHint.setText("The overlay eats taps. Long-press it to dismiss"
                    + (cfg.mode == OverlayConfig.Mode.PARTIAL ? ", drag to move." : "."));
        }
    }

    private boolean limitEnforced() {
        return TouchFilter.exists() && maxObscuringOpacity < 1.0f - 0.001f;
    }

    private boolean overLimit() {
        return cfg.passthrough && limitEnforced() && cfg.opacity > maxObscuringOpacity + 0.001f;
    }

    private void bindOpacity() {
        tvOpacityValue.setText(String.format(Locale.ROOT, "Window alpha: %.2f (%d%%)", cfg.opacity, Math.round(cfg.opacity * 100)));
        int normal = tvOpacityValue.getCurrentTextColor();
        int warn = ContextCompat.getColor(this, R.color.warning);
        if (!cfg.passthrough) {
            tvOpacityWarning.setText("Capture mode: no touches reach the app underneath, so any opacity is permitted.");
            tvOpacityWarning.setTextColor(normal);
        } else if (!TouchFilter.exists()) {
            tvOpacityWarning.setText(String.format(Locale.ROOT,
                    "Android %s has no untrusted-touch filter: pass-through overlays may be fully opaque and taps still reach the app underneath.", Build.VERSION.RELEASE));
            tvOpacityWarning.setTextColor(normal);
        } else if (!limitEnforced()) {
            tvOpacityWarning.setText("System limit lifted: pass-through overlays are drawn at the requested alpha, including 1.0, and taps reach the app underneath.");
            tvOpacityWarning.setTextColor(warn);
        } else if (overLimit()) {
            tvOpacityWarning.setText(String.format(Locale.ROOT,
                    "Above the %.2f limit: the overlay is drawn at %.2f but touches WILL NOT go through. Lift the system limit below to combine full opacity with pass-through.",
                    maxObscuringOpacity, cfg.opacity));
            tvOpacityWarning.setTextColor(warn);
        } else {
            tvOpacityWarning.setText(String.format(Locale.ROOT, "Within the %.2f limit: taps reach the app underneath.", maxObscuringOpacity));
            tvOpacityWarning.setTextColor(normal);
        }
        bindMode();
        bindLimit();
    }

    private void bindLimit() {
        if (!TouchFilter.exists()) return;
        tvLimitStatus.setText(TouchFilter.status(this));
        btnLiftLimit.setEnabled(limitEnforced());
        btnRestoreLimit.setEnabled(!TouchFilter.isDefault(this));
    }

    private void bindSystemInfo() {
        String limit = TouchFilter.exists()
                ? String.format(Locale.ROOT, "max obscuring opacity for touch: %.2f", maxObscuringOpacity)
                : "no untrusted-touch filter (pre-Android 12)";
        tvSystemInfo.setText(String.format(Locale.ROOT, "Android %s (API %d) · %d×%d dp · %s",
                Build.VERSION.RELEASE, Build.VERSION.SDK_INT, screenWdp, screenHdp, limit));
    }

    private void refreshLimit() {
        float before = maxObscuringOpacity;
        maxObscuringOpacity = TouchFilter.limit(this);
        bindSystemInfo();
        bindOpacity();
        if (Math.abs(before - maxObscuringOpacity) > 0.001f && OverlayService.isRunning()) {
            // the window manager re-evaluates the cap on the next layout, so push the config again
            startService(OverlayService.updateIntent(this, cfg));
        }
    }

    // value < 0 deletes the key (stock default)
    private void changeLimit(float value) {
        btnLiftLimit.setEnabled(false);
        btnRestoreLimit.setEnabled(false);
        TouchFilter.setLimit(this, value, (result, now) -> {
            refreshLimit();
            switch (result) {
                case DIRECT:
                    Toast.makeText(this, String.format(Locale.ROOT, "Limit is now %.2f (WRITE_SECURE_SETTINGS)", now), Toast.LENGTH_LONG).show();
                    break;
                case ROOT:
                    Toast.makeText(this, String.format(Locale.ROOT, "Limit is now %.2f (root)", now), Toast.LENGTH_LONG).show();
                    break;
                default:
                    showAdbCommands(value);
                    break;
            }
        });
    }

    private void showAdbCommands(float value) {
        String cmds = TouchFilter.adbCommands(value);
        TextView tv = new TextView(this);
        tv.setText(cmds);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setTextIsSelectable(true);
        int pad = OverlayRenderer.dp(this, 24);
        tv.setPadding(pad, OverlayRenderer.dp(this, 8), pad, 0);
        ScrollView sv = new ScrollView(this);
        sv.addView(tv);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Can't change it from here")
                .setMessage("This app has no WRITE_SECURE_SETTINGS and no root. Run this from a host with adb (works on any device, no root), then come back:")
                .setView(sv)
                .setPositiveButton("Copy commands", (d, w) -> {
                    ClipboardManager cm = getSystemService(ClipboardManager.class);
                    cm.setPrimaryClip(ClipData.newPlainText("tapjacker-adb", cmds));
                    Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void bindBg() {
        ElementEditorDialog.swatch(this, swBgColor, cfg.bgColor);
        tvBgColorHex.setText(ColorUtil.toHex(cfg.bgColor));
    }

    private void bindElements() {
        elementsContainer.removeAllViews();
        tvNoElements.setVisibility(cfg.elements.isEmpty() ? View.VISIBLE : View.GONE);
        LayoutInflater inf = LayoutInflater.from(this);
        for (int i = 0; i < cfg.elements.size(); i++) {
            final int index = i;
            OverlayElement e = cfg.elements.get(i);
            View row = inf.inflate(R.layout.item_element, elementsContainer, false);
            ElementEditorDialog.swatch(this, row.findViewById(R.id.swatch), e.type == OverlayElement.Type.TEXT ? e.textColor : e.bgColor);
            ((TextView) row.findViewById(R.id.tvTitle)).setText(e.type.label + (e.type == OverlayElement.Type.BOX ? "" : ": " + e.text));
            ((TextView) row.findViewById(R.id.tvSubtitle)).setText(e.summary());
            row.setOnClickListener(v -> ElementEditorDialog.show(this, e, false, saved -> {
                cfg.elements.set(index, saved);
                bindElements();
                onChanged();
            }));
            View up = row.findViewById(R.id.btnUp);
            View down = row.findViewById(R.id.btnDown);
            up.setEnabled(index > 0);
            down.setEnabled(index < cfg.elements.size() - 1);
            up.setOnClickListener(v -> {
                cfg.elements.add(index - 1, cfg.elements.remove(index));
                bindElements();
                onChanged();
            });
            down.setOnClickListener(v -> {
                cfg.elements.add(index + 1, cfg.elements.remove(index));
                bindElements();
                onChanged();
            });
            row.findViewById(R.id.btnDelete).setOnClickListener(v -> {
                cfg.elements.remove(index);
                bindElements();
                onChanged();
            });
            elementsContainer.addView(row);
        }
    }

    private void bindTarget() {
        rowApp.setVisibility(cfg.targetMode == OverlayConfig.TargetMode.APP ? View.VISIBLE : View.GONE);
        tilComponent.setVisibility(cfg.targetMode == OverlayConfig.TargetMode.COMPONENT ? View.VISIBLE : View.GONE);
        tvTargetApp.setText(cfg.targetPackage.isEmpty() ? "No app selected" : cfg.targetLabel + "\n" + cfg.targetPackage);
    }

    private void refreshStatus() {
        boolean granted = Settings.canDrawOverlays(this);
        tvOverlayPerm.setText(granted ? "Draw over other apps: granted" : "Draw over other apps: not granted");
        btnGrantOverlay.setVisibility(granted ? View.GONE : View.VISIBLE);
        boolean running = OverlayService.isRunning();
        btnStart.setEnabled(!running);
        btnStop.setEnabled(running);
        tvRunningStatus.setText(running ? "Overlay is up. Changes apply live." : "Overlay is not running.");
    }

    private void onChanged() {
        store.saveCurrent(cfg);
        preview.setConfig(cfg);
        handler.removeCallbacks(pushToService);
        handler.postDelayed(pushToService, 120);
    }

    private void addElement(OverlayElement.Type type) {
        ElementEditorDialog.show(this, OverlayElement.defaults(type), true, saved -> {
            cfg.elements.add(saved);
            bindElements();
            onChanged();
        });
    }

    private void openOverlaySettings() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Permission needed")
                .setMessage("Enable \"Display over other apps\" for Tapjacker NG on the next screen, then come back.")
                .setPositiveButton("Open settings", (d, w) -> startActivity(new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            openOverlaySettings();
            return;
        }
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        launchNow();
    }

    private void launchNow() {
        startForegroundService(OverlayService.showIntent(this, cfg));
        launchTarget();
    }

    private void launchTarget() {
        Intent intent;
        try {
            switch (cfg.targetMode) {
                case BUILTIN:
                    intent = new Intent(this, TestTargetActivity.class);
                    break;
                case APP:
                    if (cfg.targetPackage.isEmpty()) {
                        Toast.makeText(this, "No target app selected", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    intent = getPackageManager().getLaunchIntentForPackage(cfg.targetPackage);
                    if (intent == null) {
                        Toast.makeText(this, "That app has no launchable activity", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    break;
                case COMPONENT:
                    ComponentName cn = ComponentName.unflattenFromString(cfg.targetComponent);
                    if (cn == null) {
                        Toast.makeText(this, "Expected com.pkg/.Activity", Toast.LENGTH_LONG).show();
                        return;
                    }
                    intent = new Intent().setComponent(cn);
                    break;
                default:
                    return;
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException ex) {
            Toast.makeText(this, "Could not launch target: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showTemplates() {
        List<Presets.Preset> list = Presets.builtIn();
        CharSequence[] items = new CharSequence[list.size()];
        int dim = ContextCompat.getColor(this, R.color.dim_text);
        for (int i = 0; i < items.length; i++) {
            Presets.Preset p = list.get(i);
            SpannableString ss = new SpannableString(p.name + "\n" + p.description);
            ss.setSpan(new StyleSpan(Typeface.BOLD), 0, p.name.length(), 0);
            ss.setSpan(new RelativeSizeSpan(0.85f), p.name.length() + 1, ss.length(), 0);
            ss.setSpan(new ForegroundColorSpan(dim), p.name.length() + 1, ss.length(), 0);
            items[i] = ss;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Templates")
                .setItems(items, (d, which) -> applyLoaded(list.get(which).config.copy()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // swap the layout, keep the target (that part is device-specific)
    private void applyLoaded(OverlayConfig loaded) {
        loaded.targetMode = cfg.targetMode;
        loaded.targetPackage = cfg.targetPackage;
        loaded.targetLabel = cfg.targetLabel;
        loaded.targetComponent = cfg.targetComponent;
        cfg = loaded;
        bindAll();
        onChanged();
    }

    private void savePreset() {
        PresetsDialog.promptSave(this, store, cfg, null);
    }

    private void showPresets() {
        PresetsDialog.show(this, store, cfg, screenBounds(), loaded -> {
            cfg = loaded;
            bindAll();
            onChanged();
        });
    }

    private void pasteJson() {
        ClipboardManager cm = getSystemService(ClipboardManager.class);
        ClipData clip = cm.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) {
            Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        CharSequence text = clip.getItemAt(0).coerceToText(this);
        try {
            cfg = OverlayConfig.fromJsonString(String.valueOf(text));
            bindAll();
            onChanged();
            Toast.makeText(this, "Loaded from clipboard", Toast.LENGTH_SHORT).show();
        } catch (JSONException | IllegalArgumentException ex) {
            Toast.makeText(this, "Clipboard doesn't hold a valid config", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        OverlayService.setListener(this);
        refreshStatus();
        refreshLimit();
        if (TouchFilter.exists()) {
            getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor(TouchFilter.KEY_MAX_OPACITY), false, limitObserver);
            getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor(TouchFilter.KEY_BLOCK_MODE), false, limitObserver);
        }
    }

    @Override
    protected void onPause() {
        if (TouchFilter.exists()) getContentResolver().unregisterContentObserver(limitObserver);
        OverlayService.setListener(null);
        super.onPause();
    }

    @Override
    public void onRunningChanged(boolean running) {
        refreshStatus();
    }

    @Override
    public void onOverlayMoved(int xDp, int yDp) {
        cfg.partialXDp = xDp;
        cfg.partialYDp = yDp;
        binding = true;
        setClamped(sliderX, xDp);
        setClamped(sliderY, yDp);
        binding = false;
        store.saveCurrent(cfg);
        preview.setConfig(cfg);
    }
}
