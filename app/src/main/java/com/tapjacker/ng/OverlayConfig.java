package com.tapjacker.ng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** One overlay, start to finish. JSON in/out for presets and export. */
public class OverlayConfig {

    public enum Mode { FULL, PARTIAL }

    public enum TargetMode { NONE, BUILTIN, APP, COMPONENT }

    public Mode mode = Mode.FULL;
    public float opacity = 0.75f;     // window alpha, 0.1..1.0
    public boolean passthrough = true; // FLAG_NOT_TOUCHABLE on/off
    public int bgColor = 0xFF1B2A41;
    public int cornerRadiusDp = 16;
    public int partialWidthDp = 320;
    public int partialHeightDp = 220;
    public int partialXDp = 20;
    public int partialYDp = 320;
    public List<OverlayElement> elements = new ArrayList<>();

    public TargetMode targetMode = TargetMode.NONE;
    public String targetPackage = "";
    public String targetLabel = "";
    public String targetComponent = "";
    public int launchDelayMs = 500;  // after launching the target
    public int autoDismissSec = 0;   // 0 = never

    public static OverlayConfig defaults() {
        return Presets.builtIn().get(0).config.copy();
    }

    public String summary() {
        StringBuilder b = new StringBuilder();
        b.append(mode == Mode.FULL ? "Full screen" : partialWidthDp + "×" + partialHeightDp + " dp");
        b.append(" · ").append(Math.round(opacity * 100)).append("%");
        b.append(" · ").append(elements.size()).append(elements.size() == 1 ? " element" : " elements");
        b.append(" · ").append(passthrough ? "pass-through" : "captures taps");
        switch (targetMode) {
            case BUILTIN: b.append(" · test screen"); break;
            case APP: b.append(" · ").append(targetLabel.isEmpty() ? targetPackage : targetLabel); break;
            case COMPONENT: b.append(" · ").append(targetComponent); break;
            default: break;
        }
        return b.toString();
    }

    public OverlayConfig copy() {
        try {
            return fromJson(toJson());
        } catch (JSONException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("version", 1);
        o.put("mode", mode.name());
        o.put("opacity", opacity);
        o.put("passthrough", passthrough);
        o.put("bgColor", ColorUtil.toHex(bgColor));
        o.put("cornerRadiusDp", cornerRadiusDp);
        o.put("partialWidthDp", partialWidthDp);
        o.put("partialHeightDp", partialHeightDp);
        o.put("partialXDp", partialXDp);
        o.put("partialYDp", partialYDp);
        JSONArray arr = new JSONArray();
        for (OverlayElement e : elements) arr.put(e.toJson());
        o.put("elements", arr);
        o.put("targetMode", targetMode.name());
        o.put("targetPackage", targetPackage);
        o.put("targetLabel", targetLabel);
        o.put("targetComponent", targetComponent);
        o.put("launchDelayMs", launchDelayMs);
        o.put("autoDismissSec", autoDismissSec);
        return o;
    }

    public String toJsonString() {
        try {
            return toJson().toString(2);
        } catch (JSONException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static OverlayConfig fromJsonString(String s) throws JSONException {
        return fromJson(new JSONObject(s));
    }

    public static OverlayConfig fromJson(JSONObject o) throws JSONException {
        OverlayConfig c = new OverlayConfig();
        c.mode = Mode.valueOf(o.optString("mode", "FULL"));
        c.opacity = clampOpacity((float) o.optDouble("opacity", 0.75));
        c.passthrough = o.optBoolean("passthrough", true);
        c.bgColor = ColorUtil.parse(o.optString("bgColor", "#FF1B2A41"), 0xFF1B2A41);
        c.cornerRadiusDp = o.optInt("cornerRadiusDp", 16);
        c.partialWidthDp = o.optInt("partialWidthDp", 320);
        c.partialHeightDp = o.optInt("partialHeightDp", 220);
        c.partialXDp = o.optInt("partialXDp", 20);
        c.partialYDp = o.optInt("partialYDp", 320);
        JSONArray arr = o.optJSONArray("elements");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) c.elements.add(OverlayElement.fromJson(arr.getJSONObject(i)));
        }
        c.targetMode = TargetMode.valueOf(o.optString("targetMode", "NONE"));
        c.targetPackage = o.optString("targetPackage", "");
        c.targetLabel = o.optString("targetLabel", "");
        c.targetComponent = o.optString("targetComponent", "");
        c.launchDelayMs = o.optInt("launchDelayMs", 500);
        c.autoDismissSec = o.optInt("autoDismissSec", 0);
        return c;
    }

    // slider works in 0.05 steps
    public static float clampOpacity(float v) {
        float snapped = Math.round(v * 20f) / 20f;
        return Math.max(0.1f, Math.min(1.0f, snapped));
    }
}
