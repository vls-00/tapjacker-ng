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
    private static final int RED = 0xFFD50000;

    public static List<Preset> builtIn() {
        List<Preset> list = new ArrayList<>();
        OverlayConfig c;

        // full-screen "you won" bait
        c = full(0xFF1B2A41, 0.75f);
        c.elements.add(text("🎉 You won a prize!", WHITE, 28, true, OverlayElement.Anchor.CENTER, 0, -140));
        c.elements.add(text("Tap the button below to claim it", 0xFFB0BEC5, 16, false, OverlayElement.Anchor.CENTER, 0, -80));
        c.elements.add(button("CLAIM PRIZE", GREEN, WHITE, 220, 56, OverlayElement.Anchor.CENTER, 0, 40));
        list.add(new Preset("Prize game", "Full screen, bait button in the middle.", c));

        // fake system dialog
        c = partial(WHITE, 0.8f, 320, 200, 20, 300, 24);
        c.elements.add(text("System update available", INK, 20, true, OverlayElement.Anchor.TOP_LEFT, 12, 12));
        c.elements.add(text("A security patch is ready to install.", GREY, 15, false, OverlayElement.Anchor.TOP_LEFT, 12, 60));
        c.elements.add(button("INSTALL", BLUE, WHITE, 120, 44, OverlayElement.Anchor.BOTTOM_RIGHT, -16, -16));
        c.elements.add(text("LATER", BLUE, 15, true, OverlayElement.Anchor.BOTTOM_RIGHT, -150, -16));
        list.add(new Preset("Fake system dialog", "Partial overlay dressed up as a dialog. Line it up over the target's button.", c));

        // fake runtime permission prompt
        c = partial(WHITE, 0.8f, 340, 190, 10, 560, 28);
        c.elements.add(box(0xFFE3F2FD, 40, 40, 20, OverlayElement.Anchor.TOP_CENTER, 0, 16));
        c.elements.add(text("Allow Maps to access this\ndevice's location?", INK, 17, true, OverlayElement.Anchor.TOP_CENTER, 0, 64));
        c.elements.add(button("While using the app", 0xFFE8EAF6, 0xFF1A237E, 300, 40, OverlayElement.Anchor.BOTTOM_CENTER, 0, -12));
        list.add(new Preset("Permission prompt", "Looks like a runtime permission dialog, placed where the real one shows up.", c));

        // consent banner
        c = partial(0xFF263238, 0.8f, 360, 160, 0, 600, 16);
        c.elements.add(text("We use cookies to improve your experience.", WHITE, 15, false, OverlayElement.Anchor.TOP_LEFT, 16, 16));
        c.elements.add(button("ACCEPT ALL", BLUE, WHITE, 160, 44, OverlayElement.Anchor.BOTTOM_LEFT, 16, -16));
        list.add(new Preset("Consent banner", "Partial overlay sitting low on the screen.", c));

        // captcha
        c = partial(WHITE, 0.8f, 320, 120, 20, 380, 8);
        c.elements.add(box(0xFFFFFFFF, 28, 28, 4, OverlayElement.Anchor.CENTER_LEFT, 18, 0));
        OverlayElement tick = box(0xFFFFFFFF, 28, 28, 4, OverlayElement.Anchor.CENTER_LEFT, 18, 0);
        tick.strokeWidthDp = 2;
        tick.strokeColor = 0xFF9E9E9E;
        c.elements.add(tick);
        c.elements.add(text("I'm not a robot", INK, 16, false, OverlayElement.Anchor.CENTER_LEFT, 60, 0));
        c.elements.add(text("reCAPTCHA", 0xFF9E9E9E, 11, false, OverlayElement.Anchor.BOTTOM_RIGHT, -12, -8));
        list.add(new Preset("Captcha check", "Checkbox-style bait. Put the checkbox over the target's control.", c));

        // incoming call
        c = full(0xFF102027, 0.75f);
        c.elements.add(box(0xFF37474F, 96, 96, 48, OverlayElement.Anchor.TOP_CENTER, 0, 120));
        c.elements.add(text("Unknown number", WHITE, 26, true, OverlayElement.Anchor.TOP_CENTER, 0, 236));
        c.elements.add(text("Incoming call", 0xFFB0BEC5, 16, false, OverlayElement.Anchor.TOP_CENTER, 0, 276));
        c.elements.add(button("Decline", RED, WHITE, 140, 56, OverlayElement.Anchor.BOTTOM_LEFT, 36, -80));
        c.elements.add(button("Accept", GREEN, WHITE, 140, 56, OverlayElement.Anchor.BOTTOM_RIGHT, -36, -80));
        list.add(new Preset("Incoming call", "Full screen with accept/decline buttons near the bottom.", c));

        // loading screen
        c = full(0xFF0D0D0D, 0.8f);
        c.elements.add(text("Loading…", WHITE, 24, true, OverlayElement.Anchor.CENTER, 0, -40));
        c.elements.add(box(0xFF424242, 260, 8, 4, OverlayElement.Anchor.CENTER, 0, 10));
        c.elements.add(box(0xFF2EE6A8, 170, 8, 4, OverlayElement.Anchor.CENTER, -45, 10));
        c.elements.add(text("Tap anywhere to skip", 0xFF9E9E9E, 14, false, OverlayElement.Anchor.CENTER, 0, 50));
        list.add(new Preset("Loading screen", "Dark full-screen loader that invites a tap anywhere.", c));

        // update required
        c = full(0xFF1A237E, 0.8f);
        c.elements.add(text("Update required", WHITE, 30, true, OverlayElement.Anchor.CENTER, 0, -120));
        c.elements.add(text("This version is no longer supported.\nUpdate to keep using the app.", 0xFFC5CAE9, 16, false, OverlayElement.Anchor.CENTER, 0, -40));
        c.elements.add(button("UPDATE NOW", WHITE, 0xFF1A237E, 240, 56, OverlayElement.Anchor.BOTTOM_CENTER, 0, -120));
        list.add(new Preset("Update required", "Full-screen nag with a big button low on the screen.", c));

        // button-sized bait
        c = partial(0xFF00B0FF, 0.6f, 240, 56, 86, 520, 28);
        c.elements.add(text("Tap to continue", WHITE, 16, true, OverlayElement.Anchor.CENTER, 0, 0));
        list.add(new Preset("Button-sized bait", "Just a button. Drag or slide it over the control you want tapped.", c));

        // near-invisible
        c = full(0xFFFF0000, 0.1f);
        c.elements.add(text("ghost overlay", WHITE, 14, false, OverlayElement.Anchor.TOP_CENTER, 0, 60));
        list.add(new Preset("Ghost overlay", "Full screen at 10%. Barely visible, still counts as obscuring.", c));

        // empty
        c = full(0xFF000000, 0.75f);
        list.add(new Preset("Blank canvas", "Empty full-screen overlay.", c));

        return list;
    }
}
