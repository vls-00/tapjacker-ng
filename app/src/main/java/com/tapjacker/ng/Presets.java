package com.tapjacker.ng;

import java.util.ArrayList;
import java.util.List;

/** Built-in templates. The first one is also the default config. */
public final class Presets {
    private Presets() {}

    public static class Preset {
        public final String name;
        public final String description;
        public final OverlayConfig config;

        Preset(String name, String description, OverlayConfig config) {
            this.name = name;
            this.description = description;
            this.config = config;
        }
    }

    private static OverlayElement text(String s, int color, float sp, boolean bold, OverlayElement.Anchor anchor, int x, int y) {
        OverlayElement e = OverlayElement.defaults(OverlayElement.Type.TEXT);
        e.text = s;
        e.textColor = color;
        e.textSizeSp = sp;
        e.bold = bold;
        e.anchor = anchor;
        e.offsetXDp = x;
        e.offsetYDp = y;
        return e;
    }

    private static OverlayElement button(String s, int bg, int fg, int w, int h, OverlayElement.Anchor anchor, int x, int y) {
        OverlayElement e = OverlayElement.defaults(OverlayElement.Type.BUTTON);
        e.text = s;
        e.bgColor = bg;
        e.textColor = fg;
        e.widthDp = w;
        e.heightDp = h;
        e.cornerRadiusDp = h / 2;
        e.anchor = anchor;
        e.offsetXDp = x;
        e.offsetYDp = y;
        return e;
    }

    private static OverlayElement box(int bg, int w, int h, int radius, OverlayElement.Anchor anchor, int x, int y) {
        OverlayElement e = OverlayElement.defaults(OverlayElement.Type.BOX);
        e.bgColor = bg;
        e.widthDp = w;
        e.heightDp = h;
        e.cornerRadiusDp = radius;
        e.anchor = anchor;
        e.offsetXDp = x;
        e.offsetYDp = y;
        return e;
    }

    private static OverlayConfig full(int bg, float opacity) {
        OverlayConfig c = new OverlayConfig();
        c.mode = OverlayConfig.Mode.FULL;
        c.bgColor = bg;
        c.opacity = opacity;
        return c;
    }

    private static OverlayConfig partial(int bg, float opacity, int w, int h, int x, int y, int radius) {
        OverlayConfig c = new OverlayConfig();
        c.mode = OverlayConfig.Mode.PARTIAL;
        c.bgColor = bg;
        c.opacity = opacity;
        c.partialWidthDp = w;
        c.partialHeightDp = h;
        c.partialXDp = x;
        c.partialYDp = y;
        c.cornerRadiusDp = radius;
        return c;
    }

    private static final int WHITE = 0xFFFFFFFF;
    private static final int INK = 0xFF212121;
    private static final int GREY = 0xFF616161;
    private static final int BLUE = 0xFF2962FF;
    private static final int GREEN = 0xFF00C853;
    private static final int RED = 0xFFD32F2F;

    /** Reference phone used when no screen size is known (default config only). */
    public static List<Preset> builtIn() {
        return builtIn(411, 914);
    }

    /**
     * Templates laid out for the given screen so that the bait lands on the screen centre,
     * which is where the preview's mock target sits. Slide or drag from there.
     */
    public static List<Preset> builtIn(int screenWdp, int screenHdp) {
        int cx = screenWdp / 2;
        int cy = screenHdp / 2;
        List<Preset> list = new ArrayList<>();
        OverlayConfig c;

        // full-screen "you won" bait, button on the centre
        c = full(0xFF1B2A41, 0.75f);
        c.elements.add(text("🎉 You won a prize!", WHITE, 28, true, OverlayElement.Anchor.CENTER, 0, -130));
        c.elements.add(text("Tap the button below to claim it", 0xFFB0BEC5, 16, false, OverlayElement.Anchor.CENTER, 0, -72));
        c.elements.add(button("CLAIM PRIZE", GREEN, WHITE, 220, 56, OverlayElement.Anchor.CENTER, 0, 0));
        list.add(new Preset("Prize game", "Full screen, bait button in the centre.", c));

        // system dialog with a single full-width action over the target
        int dw = Math.min(340, screenWdp - 24);
        c = partial(WHITE, 0.85f, dw, 220, cx - dw / 2, cy - 148, 24);
        c.elements.add(text("System update available", INK, 20, true, OverlayElement.Anchor.TOP_LEFT, 8, 8));
        c.elements.add(text("A security patch is ready to install.\nYour data will not be affected.", GREY, 15, false, OverlayElement.Anchor.TOP_LEFT, 8, 52));
        c.elements.add(button("Install now", BLUE, WHITE, dw - 40, 48, OverlayElement.Anchor.BOTTOM_CENTER, 0, -48));
        c.elements.add(text("Remind me later", BLUE, 14, true, OverlayElement.Anchor.BOTTOM_CENTER, 0, -4));
        list.add(new Preset("Fake system dialog", "Dialog with one wide action button. Drag it so the button sits on the target.", c));

        // runtime permission prompt with the three stock choices
        c = partial(WHITE, 0.85f, dw, 300, cx - dw / 2, cy - 158, 28);
        OverlayElement icon = box(0xFFE3F2FD, 40, 40, 20, OverlayElement.Anchor.TOP_CENTER, 0, 14);
        icon.strokeWidthDp = 2;
        icon.strokeColor = 0xFF90CAF9;
        c.elements.add(icon);
        c.elements.add(text("Allow Maps to access this\ndevice's location?", INK, 16, true, OverlayElement.Anchor.TOP_CENTER, 0, 56));
        c.elements.add(button("While using the app", 0xFFE8EAF6, 0xFF1A237E, dw - 40, 44, OverlayElement.Anchor.TOP_CENTER, 0, 136));
        c.elements.add(button("Only this time", 0xFFE8EAF6, 0xFF1A237E, dw - 40, 44, OverlayElement.Anchor.TOP_CENTER, 0, 188));
        c.elements.add(button("Don't allow", 0xFFE8EAF6, 0xFF1A237E, dw - 40, 44, OverlayElement.Anchor.TOP_CENTER, 0, 240));
        list.add(new Preset("Permission prompt", "Looks like the stock location prompt. The first option is the bait.", c));

        // cookie banner with a full-width accept button
        int bw = Math.min(390, screenWdp - 20);
        c = partial(0xFF263238, 0.85f, bw, 180, cx - bw / 2, cy - 142, 16);
        c.elements.add(text("We use cookies to improve your experience\nand show relevant content.", WHITE, 15, false, OverlayElement.Anchor.TOP_LEFT, 4, 6));
        c.elements.add(text("Manage options", 0xFFB0BEC5, 13, true, OverlayElement.Anchor.TOP_LEFT, 4, 70));
        c.elements.add(button("Accept all", BLUE, WHITE, bw - 32, 44, OverlayElement.Anchor.BOTTOM_CENTER, 0, -16));
        list.add(new Preset("Consent banner", "Cookie banner. The Accept button is the bait.", c));

        // captcha: the checkbox sits on the centre
        c = partial(WHITE, 0.85f, 200, 100, cx - 32, cy - 50, 8);
        OverlayElement tick = box(WHITE, 28, 28, 4, OverlayElement.Anchor.CENTER_LEFT, 18, 0);
        tick.strokeWidthDp = 2;
        tick.strokeColor = 0xFF9E9E9E;
        c.elements.add(tick);
        c.elements.add(text("I'm not a robot", INK, 15, false, OverlayElement.Anchor.CENTER_LEFT, 46, 0));
        c.elements.add(text("reCAPTCHA", 0xFF9E9E9E, 10, false, OverlayElement.Anchor.BOTTOM_RIGHT, -2, 0));
        list.add(new Preset("Captcha check", "Checkbox-style bait. The checkbox is the part that goes over the control.", c));

        // incoming call, accept button in the centre
        c = full(0xFF102027, 0.8f);
        c.elements.add(box(0xFF37474F, 96, 96, 48, OverlayElement.Anchor.CENTER, 0, -230));
        c.elements.add(text("Unknown number", WHITE, 26, true, OverlayElement.Anchor.CENTER, 0, -150));
        c.elements.add(text("Incoming call", 0xFFB0BEC5, 16, false, OverlayElement.Anchor.CENTER, 0, -110));
        c.elements.add(button("Accept", GREEN, WHITE, 220, 56, OverlayElement.Anchor.CENTER, 0, 0));
        c.elements.add(button("Decline", RED, WHITE, 220, 56, OverlayElement.Anchor.CENTER, 0, 76));
        list.add(new Preset("Incoming call", "Full screen; Accept sits on the centre, Decline below it.", c));

        // loading screen that invites a tap anywhere
        c = full(0xFF0D0D0D, 0.8f);
        c.elements.add(text("Loading…", WHITE, 24, true, OverlayElement.Anchor.CENTER, 0, -70));
        c.elements.add(box(0xFF424242, 260, 8, 4, OverlayElement.Anchor.CENTER, 0, -24));
        c.elements.add(box(0xFF2EE6A8, 170, 8, 4, OverlayElement.Anchor.CENTER, -45, -24));
        c.elements.add(text("Tap anywhere to continue", 0xFF9E9E9E, 14, false, OverlayElement.Anchor.CENTER, 0, 20));
        list.add(new Preset("Loading screen", "Dark full-screen loader that invites a tap anywhere.", c));

        // update required, button in the centre
        c = full(0xFF1A237E, 0.8f);
        c.elements.add(text("Update required", WHITE, 30, true, OverlayElement.Anchor.CENTER, 0, -140));
        c.elements.add(text("This version is no longer supported.\nUpdate to keep using the app.", 0xFFC5CAE9, 16, false, OverlayElement.Anchor.CENTER, 0, -70));
        c.elements.add(button("UPDATE NOW", WHITE, 0xFF1A237E, 240, 56, OverlayElement.Anchor.CENTER, 0, 0));
        list.add(new Preset("Update required", "Full-screen nag with the button in the centre.", c));

        // button-sized bait on the centre
        c = partial(0xFF0288D1, 0.7f, 240, 56, cx - 120, cy - 28, 28);
        c.elements.add(text("Tap to continue", WHITE, 16, true, OverlayElement.Anchor.CENTER, 0, 0));
        list.add(new Preset("Button-sized bait", "Just a button. Drag or slide it over the control you want tapped.", c));

        return list;
    }
}
