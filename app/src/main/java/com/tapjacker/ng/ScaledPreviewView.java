package com.tapjacker.ng;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

/** Lays the overlay out at real screen size over a fake app, then scales it all down to fit. */
public class ScaledPreviewView extends FrameLayout {
    private int screenW = 1080;
    private int screenH = 2400;
    private float scale = 1f;

    public ScaledPreviewView(Context context) { super(context); init(); }
    public ScaledPreviewView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        setClipChildren(true);
        setClipToPadding(true);
    }

    public void setScreenSize(int widthPx, int heightPx) {
        screenW = Math.max(1, widthPx);
        screenH = Math.max(1, heightPx);
        requestLayout();
    }

    public void setConfig(OverlayConfig cfg) {
        removeAllViews();
        Context ctx = getContext();
        FrameLayout screen = new FrameLayout(ctx);
        screen.setBackgroundColor(0xFFECEFF1);

        // fake app underneath
        View appBar = new View(ctx);
        appBar.setBackgroundColor(0xFF546E7A);
        screen.addView(appBar, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, OverlayRenderer.dp(ctx, 88)));
        TextView targetBtn = new TextView(ctx);
        targetBtn.setText("Target button");
        targetBtn.setTextColor(Color.WHITE);
        targetBtn.setTextSize(16);
        targetBtn.setGravity(Gravity.CENTER);
        GradientDrawable bd = new GradientDrawable();
        bd.setColor(0xFF78909C);
        bd.setCornerRadius(OverlayRenderer.dp(ctx, 8));
        targetBtn.setBackground(bd);
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(OverlayRenderer.dp(ctx, 220), OverlayRenderer.dp(ctx, 56), Gravity.CENTER);
        screen.addView(targetBtn, blp);
        for (int i = 0; i < 4; i++) {
            View line = new View(ctx);
            line.setBackgroundColor(0xFFCFD8DC);
            FrameLayout.LayoutParams llp = new FrameLayout.LayoutParams(OverlayRenderer.dp(ctx, 260 - i * 40), OverlayRenderer.dp(ctx, 14), Gravity.TOP | Gravity.START);
            llp.leftMargin = OverlayRenderer.dp(ctx, 24);
            llp.topMargin = OverlayRenderer.dp(ctx, 120 + i * 28);
            screen.addView(line, llp);
        }

        FrameLayout overlay = OverlayRenderer.buildOverlay(ctx, cfg);
        overlay.setAlpha(cfg.opacity);
        FrameLayout.LayoutParams olp;
        if (cfg.mode == OverlayConfig.Mode.FULL) {
            olp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        } else {
            olp = new FrameLayout.LayoutParams(OverlayRenderer.dp(ctx, cfg.partialWidthDp), OverlayRenderer.dp(ctx, cfg.partialHeightDp), Gravity.TOP | Gravity.START);
            olp.leftMargin = OverlayRenderer.dp(ctx, cfg.partialXDp);
            olp.topMargin = OverlayRenderer.dp(ctx, cfg.partialYDp);
        }
        screen.addView(overlay, olp);
        addView(screen, new FrameLayout.LayoutParams(screenW, screenH));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        scale = width / (float) screenW;
        int height = Math.round(screenH * scale);
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).measure(MeasureSpec.makeMeasureSpec(screenW, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(screenH, MeasureSpec.EXACTLY));
        }
        setMeasuredDimension(width + getPaddingLeft() + getPaddingRight(), height + getPaddingTop() + getPaddingBottom());
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++) {
            View c = getChildAt(i);
            c.layout(getPaddingLeft(), getPaddingTop(), getPaddingLeft() + screenW, getPaddingTop() + screenH);
            c.setPivotX(0);
            c.setPivotY(0);
            c.setScaleX(scale);
            c.setScaleY(scale);
        }
    }
}
