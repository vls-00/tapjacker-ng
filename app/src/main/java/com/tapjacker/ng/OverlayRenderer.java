package com.tapjacker.ng;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Config -> view tree. Shared by the service and the preview so they match.
 * Framework widgets only: a Service context has no Material theme.
 */
public final class OverlayRenderer {
    private OverlayRenderer() {}

    public static int dp(Context c, float dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, c.getResources().getDisplayMetrics()));
    }

    // caller sets the size via LayoutParams
    public static FrameLayout buildOverlay(Context ctx, OverlayConfig cfg) {
        FrameLayout root = new FrameLayout(ctx);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(cfg.bgColor);
        if (cfg.mode == OverlayConfig.Mode.PARTIAL) {
            bg.setCornerRadius(dp(ctx, cfg.cornerRadiusDp));
            root.setClipToOutline(true);
        }
        root.setBackground(bg);
        root.setClipChildren(true);

        for (OverlayElement e : cfg.elements) {
            View v = buildElement(ctx, e);
            int w = e.widthDp > 0 ? dp(ctx, e.widthDp) : ViewGroup.LayoutParams.WRAP_CONTENT;
            int h = e.heightDp > 0 ? dp(ctx, e.heightDp) : ViewGroup.LayoutParams.WRAP_CONTENT;
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h, e.anchor.gravity);
            v.setTranslationX(dp(ctx, e.offsetXDp));
            v.setTranslationY(dp(ctx, e.offsetYDp));
            root.addView(v, lp);
        }
        return root;
    }

    public static View buildElement(Context ctx, OverlayElement e) {
        if (e.type == OverlayElement.Type.BOX) {
            View v = new View(ctx);
            v.setBackground(shape(ctx, e));
            return v;
        }
        TextView tv = new TextView(ctx);
        tv.setText(e.text);
        tv.setTextColor(e.textColor);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, e.textSizeSp);
        tv.setTypeface(Typeface.DEFAULT, e.bold ? Typeface.BOLD : Typeface.NORMAL);
        tv.setGravity(textGravity(e));
        int pad = dp(ctx, e.paddingDp);
        tv.setPadding(pad, pad, pad, pad);
        tv.setBackground(shape(ctx, e));
        if (e.type == OverlayElement.Type.BUTTON) {
            tv.setElevation(dp(ctx, 4));
            tv.setAllCaps(false);
        }
        return tv;
    }

    // multi-line text follows its anchor: left anchors read left-aligned, right anchors right-aligned
    private static int textGravity(OverlayElement e) {
        if (e.type == OverlayElement.Type.BUTTON) return Gravity.CENTER;
        switch (e.anchor) {
            case TOP_LEFT: case CENTER_LEFT: case BOTTOM_LEFT:
                return Gravity.START | Gravity.CENTER_VERTICAL;
            case TOP_RIGHT: case CENTER_RIGHT: case BOTTOM_RIGHT:
                return Gravity.END | Gravity.CENTER_VERTICAL;
            default:
                return Gravity.CENTER;
        }
    }

    private static GradientDrawable shape(Context ctx, OverlayElement e) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(e.bgColor);
        d.setCornerRadius(dp(ctx, e.cornerRadiusDp));
        if (e.strokeWidthDp > 0) d.setStroke(dp(ctx, e.strokeWidthDp), e.strokeColor);
        return d;
    }
}
