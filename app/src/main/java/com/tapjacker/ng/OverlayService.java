package com.tapjacker.ng;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import org.json.JSONException;

import java.util.Locale;

/**
 * Owns the overlay window. Opacity goes on LayoutParams.alpha, which is what the
 * untrusted-touch check reads; view alpha doesn't count.
 */
public class OverlayService extends Service {
    private static final String TAG = "OverlayService";
    public static final String ACTION_SHOW = "com.tapjacker.ng.action.SHOW";
    public static final String ACTION_UPDATE = "com.tapjacker.ng.action.UPDATE";
    public static final String ACTION_STOP = "com.tapjacker.ng.action.STOP";
    public static final String EXTRA_CONFIG = "config";
    private static final String CHANNEL_ID = "overlay";
    private static final int NOTIFICATION_ID = 1;

    public interface Listener {
        void onRunningChanged(boolean running);
        void onOverlayMoved(int xDp, int yDp);
    }

    private static volatile boolean running;
    @Nullable private static Listener listener;

    public static boolean isRunning() { return running; }
    public static void setListener(@Nullable Listener l) { listener = l; }

    public static Intent showIntent(Context ctx, OverlayConfig cfg) {
        return new Intent(ctx, OverlayService.class).setAction(ACTION_SHOW).putExtra(EXTRA_CONFIG, cfg.toJsonString());
    }

    public static Intent updateIntent(Context ctx, OverlayConfig cfg) {
        return new Intent(ctx, OverlayService.class).setAction(ACTION_UPDATE).putExtra(EXTRA_CONFIG, cfg.toJsonString());
    }

    public static Intent stopIntent(Context ctx) {
        return new Intent(ctx, OverlayService.class).setAction(ACTION_STOP);
    }

    private WindowManager wm;
    @Nullable private FrameLayout overlay;
    private OverlayConfig cfg;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable showRunnable = this::attach;
    private final Runnable dismissRunnable = this::stopSelf;

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(CHANNEL_ID, getString(R.string.notif_channel), NotificationManager.IMPORTANCE_LOW);
        ch.setDescription("Shown while an overlay is up");
        nm.createNotificationChannel(ch);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_STOP : intent.getAction();
        if (action == null || ACTION_STOP.equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        OverlayConfig parsed = parse(intent);
        if (parsed == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        cfg = parsed;

        if (ACTION_SHOW.equals(action)) {
            startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            setRunning(true);
            handler.removeCallbacks(showRunnable);
            detach();
            handler.postDelayed(showRunnable, Math.max(0, cfg.launchDelayMs));
        } else if (ACTION_UPDATE.equals(action)) {
            if (!running) {
                stopSelf();
                return START_NOT_STICKY;
            }
            if (overlay != null) attach();
            getSystemService(NotificationManager.class).notify(NOTIFICATION_ID, buildNotification());
        }
        return START_NOT_STICKY;
    }

    @Nullable
    private OverlayConfig parse(Intent intent) {
        String json = intent.getStringExtra(EXTRA_CONFIG);
        if (json == null) return null;
        try {
            return OverlayConfig.fromJsonString(json);
        } catch (JSONException ex) {
            Log.e(TAG, "Bad config", ex);
            return null;
        }
    }

    private void attach() {
        detach();
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "SYSTEM_ALERT_WINDOW not granted");
            stopSelf();
            return;
        }
        FrameLayout view = OverlayRenderer.buildOverlay(this, cfg);
        WindowManager.LayoutParams lp = buildParams(cfg);
        if (!cfg.passthrough) installTouchHandling(view, lp);
        try {
            Log.d(TAG, "show " + cfg.mode + " alpha=" + lp.alpha + " passthrough=" + cfg.passthrough);
            wm.addView(view, lp);
            overlay = view;
        } catch (RuntimeException ex) {
            Log.e(TAG, "addView failed", ex);
            stopSelf();
            return;
        }
        handler.removeCallbacks(dismissRunnable);
        if (cfg.autoDismissSec > 0) handler.postDelayed(dismissRunnable, cfg.autoDismissSec * 1000L);
    }

    private void detach() {
        if (overlay != null) {
            try {
                wm.removeViewImmediate(overlay);
            } catch (RuntimeException ignored) {
            }
            overlay = null;
        }
    }

    private WindowManager.LayoutParams buildParams(OverlayConfig c) {
        boolean full = c.mode == OverlayConfig.Mode.FULL;
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        if (c.passthrough) flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;

        int w = full ? ViewGroup.LayoutParams.MATCH_PARENT : OverlayRenderer.dp(this, c.partialWidthDp);
        int h = full ? ViewGroup.LayoutParams.MATCH_PARENT : OverlayRenderer.dp(this, c.partialHeightDp);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, flags, PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = full ? 0 : OverlayRenderer.dp(this, c.partialXDp);
        lp.y = full ? 0 : OverlayRenderer.dp(this, c.partialYDp);
        lp.alpha = c.opacity;
        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        lp.setFitInsetsTypes(0);
        lp.setTitle("tapjacker-ng");
        return lp;
    }

    // capture mode: long-press to dismiss, drag to move (partial only)
    private void installTouchHandling(FrameLayout view, WindowManager.LayoutParams lp) {
        GestureDetector longPress = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                stopSelf();
            }
        });
        boolean draggable = cfg.mode == OverlayConfig.Mode.PARTIAL;
        final int[] start = new int[2];
        final float[] down = new float[2];
        view.setOnTouchListener((v, event) -> {
            longPress.onTouchEvent(event);
            if (!draggable) return true;
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    start[0] = lp.x;
                    start[1] = lp.y;
                    down[0] = event.getRawX();
                    down[1] = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    lp.x = start[0] + Math.round(event.getRawX() - down[0]);
                    lp.y = start[1] + Math.round(event.getRawY() - down[1]);
                    if (overlay != null) wm.updateViewLayout(overlay, lp);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    float density = getResources().getDisplayMetrics().density;
                    int xDp = Math.round(lp.x / density);
                    int yDp = Math.round(lp.y / density);
                    cfg.partialXDp = xDp;
                    cfg.partialYDp = yDp;
                    Listener l = listener;
                    if (l != null && (xDp != start[0] || yDp != start[1])) l.onOverlayMoved(xDp, yDp);
                    return true;
                default:
                    return true;
            }
        });
    }

    private Notification buildNotification() {
        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop = PendingIntent.getService(this, 1, stopIntent(this),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        String mode = cfg.mode == OverlayConfig.Mode.FULL ? "Full screen" : "Partial";
        String text = String.format(Locale.ROOT, "%s · %d%% opacity · %s", mode, Math.round(cfg.opacity * 100),
                cfg.passthrough ? "touches pass through" : "touches captured");
        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_layers)
                .setContentTitle("Tapjacker NG overlay is up")
                .setContentText(text)
                .setContentIntent(open)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(null, "Stop", stop).build())
                .build();
    }

    private void setRunning(boolean value) {
        running = value;
        Listener l = listener;
        if (l != null) handler.post(() -> l.onRunningChanged(value));
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        detach();
        stopForeground(STOP_FOREGROUND_REMOVE);
        setRunning(false);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
