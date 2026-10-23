# Tapjacker NG

Tapjacking proof-of-concept builder for Android 8.0 and newer. Most public tapjacking PoCs stopped
working around Android 12, when the platform started filtering touches that pass through
overlays. This one is built around those rules instead of fighting them, and it still runs on the
older releases where none of the rules apply.

Minimum SDK 26 (Android 8.0). Tested on a Pixel 8a (Android 16), the API 37 emulator and the
API 30 (Android 11) emulator.

## Install

Grab the APK from the Releases page and install it. On first start the app asks for
"Display over other apps" and, on Android 13+, notification access. Both are needed: the first to
draw anything at all, the second so the Stop action in the notification is visible.

Build from source with `./gradlew assembleDebug` (JDK 17+, Android SDK with platform 37).

## What it does

### Overlay modes

- **Full screen.** Covers the whole display including the status bar and navigation area.
- **Partial.** A rounded window with width, height, X, Y and corner radius sliders scaled to the
  device. Position is in dp from the top-left of the screen.
- Switch between the two at any time, including while the overlay is up.

### Opacity

- Window alpha from 1.0 down to 0.1 in 0.05 steps, on every Android version.
- Applied on `WindowManager.LayoutParams.alpha`, which is what the system's touch filter reads.
  View alpha does not count.
- The Opacity card shows the device's `maximumObscuringOpacityForTouch` and a one-line verdict:
  within the limit taps reach the app underneath; above it the overlay is drawn at the requested
  alpha, 1.0 included, but touches WILL NOT go through (see "Android 12+ behaviour" below).
- On Android 8 to 11 there is no limit. The card says so and the slider does exactly what it says.
- Below the slider, on Android 12+, a compact "System limit" line shows the live value of the
  global setting behind the filter with two buttons: "Lift limit to 1.0" and "Restore default".
  See "Lifting the limit" below.

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
- A pass-through (`FLAG_NOT_TOUCHABLE`) overlay from another app is drawn at that limit no
  matter what you ask for; a touchable overlay can be fully opaque. So when you ask for more than
  the limit with pass-through on, the app drops `FLAG_NOT_TOUCHABLE`: the overlay is drawn at the
  alpha you set, 1.0 included, but touches WILL NOT go through, and the card and the pass-through
  hint say so. Long-press the overlay to dismiss it in that state. Opaque *and* pass-through
  needs the limit lifted (next section). If several pass-through overlays stack up and their
  combined opacity crosses the limit, the input dispatcher drops the touch instead.
- Touches that cross a pass-through overlay arrive at the target with `FLAG_WINDOW_IS_OBSCURED`.
  Views with `filterTouchesWhenObscured="true"` drop them. Activities that call
  `Window.setHideOverlayWindows(true)` hide the overlay entirely.
- Overlays from the same app as the touched window are trusted, so the built-in test screen only
  proves geometry and pass-through. Use a real app to check obscured flags and mitigations.
- The overlay lives in a `specialUse` foreground service, as Android 14+ requires.

## Android 8 to 11 behaviour

- No untrusted-touch filter. A pass-through overlay can be fully opaque and taps still land on
  whatever is underneath. The app's Opacity card says so and skips the limit section.
- `FLAG_WINDOW_IS_OBSCURED` and `filterTouchesWhenObscured` exist on these versions too, so a
  target that uses them is still protected. Nothing else is.
- `TYPE_APPLICATION_OVERLAY` windows sit below the status bar and navigation bar on these
  versions, so "Full screen" covers the app area only.
- Android 12 is also where `Window.setHideOverlayWindows()` appeared; older targets cannot hide
  the overlay.

## Lifting the limit on Android 12+ (simulating older Android)

The 0.8 cap is not hard-coded. It comes from a global system setting that `WindowManager` and
`InputDispatcher` both watch:

```
maximum_obscuring_opacity_for_touch   float, default 0.8
```

Set it to 1.0 and an Android 12+ device behaves like Android 11 for this purpose: pass-through
overlays are drawn at the alpha you request, 1.0 included, and taps still go through. Verified on
the API 37 emulator: with the setting at 1.0 a `FLAG_NOT_TOUCHABLE` overlay at alpha 1.0 was
rendered solid (`dumpsys window` shows `alpha=1.0`, the target was invisible) and every tap
reached the activity underneath. The change applies immediately, no reboot, and it survives
reboots until you remove it. It is device-wide, so put it back when you are done.

Three ways to set it:

**1. adb, no root.** The shell user may write global settings on every build, so this works on a
stock, locked phone with USB debugging:

```
adb shell settings put global maximum_obscuring_opacity_for_touch 1.0
```

**2. From the app, after one adb grant.** `WRITE_SECURE_SETTINGS` is a development permission,
so adb can hand it to the app once and the "Lift limit to 1.0" / "Restore default" buttons work
from then on, including on devices that are no longer plugged in:

```
adb shell pm grant com.tapjacker.ng android.permission.WRITE_SECURE_SETTINGS
```

**3. Root.** If `su` is available the buttons use it automatically:

```
su -c settings put global maximum_obscuring_opacity_for_touch 1.0
```

If the app has neither the permission nor root, the buttons show the adb commands above with a
Copy button instead of failing silently. The Opacity card picks the change up live and, if an
overlay is already running, re-pushes it so the new cap takes effect without a restart.

Back to stock:

```
adb shell settings delete global maximum_obscuring_opacity_for_touch
```

or the "Restore default" button.

A second setting controls what the dispatcher does when stacked overlays exceed the limit:

```
block_untrusted_touches   int: 0 = off, 1 = permissive (log only), 2 = block (default)
adb shell settings put global block_untrusted_touches 0
```

With the opacity limit at 1.0 a single overlay can never exceed it, so this one only matters for
multi-overlay setups. The app shows its current value but does not change it.

None of this touches `FLAG_WINDOW_IS_OBSCURED`, `filterTouchesWhenObscured` or
`setHideOverlayWindows()`. Those are the target's own mitigations and keep working.
