# Tapjacker NG

Tapjacking proof-of-concept builder for Android 12 and newer. Most public tapjacking PoCs stopped
working around Android 12, when the platform started filtering touches that pass through
overlays. This one is built around those rules instead of fighting them.

Minimum SDK 31. Tested on a Pixel 8a (Android 16) and the API 37 emulator.

## Install

Grab the APK from the Releases page and install it. On first start the app asks for
"Display over other apps" and, on Android 13+, notification access. Both are needed: the first to
draw anything at all, the second so the Stop action in the notification is visible.

## What it does

### Overlay modes

- **Full screen.** Covers the whole display including the status bar and navigation area.
- **Partial.** A rounded window with width, height, X, Y and corner radius sliders scaled to the
  device. Position is in dp from the top-left of the screen.
- Switch between the two at any time, including while the overlay is up.

### Opacity

- Window alpha from 1.0 down to 0.1 in 0.05 steps.
- Applied on `WindowManager.LayoutParams.alpha`, which is what the system's touch filter reads.
  View alpha does not count.
- The Opacity card shows the device's `maximumObscuringOpacityForTouch` and the effective alpha
  when the requested one is above it (see "Android 12+ behaviour" below).

### Touch handling

- **Pass-through on.** `FLAG_NOT_TOUCHABLE` is set. Taps land on whatever is underneath. This is
  the tapjacking case.
- **Pass-through off.** The overlay captures taps. Long-press it to dismiss. Partial overlays can
  be dragged around; the sliders in the app follow the new position.

### Overlay content

- Background colour of the overlay.
- Any number of elements of three kinds:
  - **Text**: content, colour, size, bold.
  - **Button**: label, fill, text colour, size, corner radius, drop shadow.
  - **Box**: fill, size, corner radius.
- Every element also has an optional border (width and colour), padding, a nine-point anchor
  (corners, edges, centre) and an X/Y offset in dp.
- Colour picker with swatches, hex entry (`#RGB`, `#RRGGBB`, `#AARRGGBB`) and a per-element alpha
  slider.
- Reorder elements with the arrows, delete with the bin, tap a row to edit.

### Targets

Launched underneath the overlay when you press Start:

- Nothing, just the overlay.
- The built-in test screen, which counts taps that get through.
- Any installed app, chosen from a searchable list.
- An explicit activity, typed as `package/.Activity`.

Plus a delay before the overlay appears (0 to 10 s) and an auto-dismiss timer (0 to 5 min).

### Templates and presets

Eleven built-in templates to start from: prize game, fake system dialog, permission prompt,
consent banner, captcha check, incoming call, loading screen, update required, button-sized bait,
ghost overlay, blank canvas. Loading a template keeps your target settings.

Save your own under a name. The presets manager shows each one with a thumbnail and a one-line
summary (mode, opacity, element count, target). Tap to load, long-press to rename, bin to delete,
or save the current setup from the same dialog. Copy the whole configuration as JSON to the
clipboard for reports and paste it back to restore it on any device.

### Live preview and live editing

- The preview renders the overlay at real screen size over a mock app, scaled to fit.
- While the overlay is running, every change in the app is pushed to it straight away.

### Run card

- Android version, API level, screen size in dp and the system opacity limit.
- Overlay permission state with a shortcut to the settings page.
- Start and Stop buttons. A persistent notification carries a Stop action too, and brings you
  back to the app when tapped.

## Android 12+ behaviour

- The system limit is `InputManager.getMaximumObscuringOpacityForTouch()`, 0.8 on stock builds.
- A pass-through overlay from another app is drawn at that limit no matter what you ask for.
  Request 1.0 and the window manager renders 0.8; taps still go through.
- Touches that cross a pass-through overlay arrive at the target with `FLAG_WINDOW_IS_OBSCURED`.
  Views with `filterTouchesWhenObscured="true"` drop them. Activities that call
  `Window.setHideOverlayWindows(true)` hide the overlay entirely.
- Overlays from the same app as the touched window are trusted, so the built-in test screen only
  proves geometry and pass-through. Use a real app to check obscured flags and mitigations.
- The overlay lives in a `specialUse` foreground service, as Android 14+ requires.

## Workflow

1. Pick a template or build the overlay in the Overlay content card.
2. Set mode, geometry and opacity. The preview follows along.
3. Choose a target.
4. Start overlay. The target launches, the overlay appears after the delay.
5. Tap the bait. If the target reacts, the tap went through.
6. Copy JSON and paste it into the report.

Only use this against apps you are authorised to test.
