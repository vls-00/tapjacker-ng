package com.tapjacker.ng;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.input.InputManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The Android 12+ untrusted-touch filter and the two global settings behind it.
 *
 * maximum_obscuring_opacity_for_touch: the window manager caps the alpha of a pass-through
 * overlay from another app at this value and the input dispatcher drops touches when stacked
 * overlays exceed it. Default 0.8. Set it to 1.0 and the device behaves like Android 11:
 * fully opaque pass-through overlays, taps still land underneath.
 *
 * block_untrusted_touches: 0 = off, 1 = permissive (log only), 2 = block (default).
 *
 * Both are world-readable and take effect immediately. Writing needs WRITE_SECURE_SETTINGS,
 * which adb can grant to this app, or root.
 */
public final class TouchFilter {
    private TouchFilter() {}

    private static final String TAG = "TouchFilter";
    public static final String KEY_MAX_OPACITY = "maximum_obscuring_opacity_for_touch";
    public static final String KEY_BLOCK_MODE = "block_untrusted_touches";
    public static final float DEFAULT_MAX_OPACITY = 0.8f;
    public static final int DEFAULT_BLOCK_MODE = 2;

    public enum Result { DIRECT, ROOT, FAILED }

    public interface Callback { void onDone(Result result, float newLimit); }

    /** The filter exists from Android 12 (API 31). */
    public static boolean exists() {
        return Build.VERSION.SDK_INT >= 31;
    }

    /** Current limit. 1.0 on Android 11 and older, which have no filter. */
    public static float limit(Context ctx) {
        if (!exists()) return 1.0f;
        InputManager im = ctx.getSystemService(InputManager.class);
        if (im != null) {
            try {
                return im.getMaximumObscuringOpacityForTouch();
            } catch (RuntimeException ignored) {
            }
        }
        return Settings.Global.getFloat(ctx.getContentResolver(), KEY_MAX_OPACITY, DEFAULT_MAX_OPACITY);
    }

    public static boolean isDefault(Context ctx) {
        return Settings.Global.getString(ctx.getContentResolver(), KEY_MAX_OPACITY) == null;
    }

    public static boolean isLifted(Context ctx) {
        return limit(ctx) >= 1.0f - 0.001f;
    }

    public static int blockMode(Context ctx) {
        return Settings.Global.getInt(ctx.getContentResolver(), KEY_BLOCK_MODE, DEFAULT_BLOCK_MODE);
    }

    public static String blockModeLabel(int mode) {
        switch (mode) {
            case 0: return "0 (disabled)";
            case 1: return "1 (permissive, log only)";
            case 2: return "2 (block)";
            default: return String.valueOf(mode);
        }
    }

    /** adb granted WRITE_SECURE_SETTINGS to this app. */
    public static boolean canWriteDirectly(Context ctx) {
        return ctx.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * The overlay can only pass touches through if it stays within the limit. Above it, the
     * window is made touchable so the system draws it at the requested alpha.
     */
    public static boolean effectivePassthrough(Context ctx, OverlayConfig cfg) {
        if (!cfg.passthrough) return false;
        if (!exists()) return true;
        float limit = limit(ctx);
        if (limit >= 1.0f - 0.001f) return true;
        return cfg.opacity <= limit + 0.001f;
    }

    /** One-line summary for the UI. */
    public static String status(Context ctx) {
        if (!exists()) return "No untrusted-touch filter before Android 12.";
        return String.format(Locale.ROOT, "%s = %.2f%s\n%s = %s",
                KEY_MAX_OPACITY, limit(ctx), isDefault(ctx) ? " (default)" : "",
                KEY_BLOCK_MODE, blockModeLabel(blockMode(ctx)));
    }

    public static String writeAccess(Context ctx) {
        if (canWriteDirectly(ctx)) return "WRITE_SECURE_SETTINGS granted: the buttons change the setting directly.";
        return "WRITE_SECURE_SETTINGS not granted: the buttons try root, otherwise they show the adb commands.";
    }

    /** Commands to run from a host, shown when neither direct write nor root is available. */
    public static String adbCommands(float value) {
        String v = formatValue(value);
        return "# set the limit directly (no root needed)\n"
                + "adb shell settings put global " + KEY_MAX_OPACITY + " " + v + "\n\n"
                + "# or let Tapjacker NG change it from the app from now on\n"
                + "adb shell pm grant com.tapjacker.ng android.permission.WRITE_SECURE_SETTINGS\n\n"
                + "# back to stock\n"
                + "adb shell settings delete global " + KEY_MAX_OPACITY;
    }

    public static String formatValue(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    /**
     * Sets the limit: direct write first, then su. Reports on the main thread.
     * Pass a value below 0 to delete the key and go back to the stock default.
     */
    public static void setLimit(Context ctx, float value, Callback cb) {
        Context app = ctx.getApplicationContext();
        Handler main = new Handler(Looper.getMainLooper());
        Executors.newSingleThreadExecutor().execute(() -> {
            Result r = tryDirect(app, value) ? Result.DIRECT : tryRoot(value) ? Result.ROOT : Result.FAILED;
            float now = limit(app);
            main.post(() -> cb.onDone(r, now));
        });
    }

    private static boolean tryDirect(Context ctx, float value) {
        try {
            if (value < 0) return Settings.Global.putString(ctx.getContentResolver(), KEY_MAX_OPACITY, null);
            return Settings.Global.putFloat(ctx.getContentResolver(), KEY_MAX_OPACITY, value);
        } catch (SecurityException ex) {
            Log.i(TAG, "direct write refused: " + ex.getMessage());
            return false;
        }
    }

    private static boolean tryRoot(float value) {
        String cmd = value < 0
                ? "settings delete global " + KEY_MAX_OPACITY
                : "settings put global " + KEY_MAX_OPACITY + " " + formatValue(value);
        try {
            Process p = new ProcessBuilder("su", "-c", cmd).redirectErrorStream(true).start();
            p.getOutputStream().close();
            if (!p.waitFor(10, TimeUnit.SECONDS)) {
                p.destroy();
                Log.i(TAG, "su timed out");
                return false;
            }
            StringBuilder out = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) out.append(line).append('\n');
            }
            Log.i(TAG, "su exit=" + p.exitValue() + " out=" + out.toString().trim());
            return p.exitValue() == 0;
        } catch (Exception ex) {
            Log.i(TAG, "no root: " + ex.getMessage());
            return false;
        }
    }
}
