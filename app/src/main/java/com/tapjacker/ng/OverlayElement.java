package com.tapjacker.ng;

import android.view.Gravity;

import org.json.JSONException;
import org.json.JSONObject;

/** A label, fake button or box inside the overlay. */
public class OverlayElement {

    public enum Type {
        TEXT("Text"), BUTTON("Button"), BOX("Box");
        public final String label;
        Type(String label) { this.label = label; }
    }

    public enum Anchor {
        TOP_LEFT("Top left", Gravity.TOP | Gravity.START),
        TOP_CENTER("Top centre", Gravity.TOP | Gravity.CENTER_HORIZONTAL),
        TOP_RIGHT("Top right", Gravity.TOP | Gravity.END),
        CENTER_LEFT("Centre left", Gravity.CENTER_VERTICAL | Gravity.START),
        CENTER("Centre", Gravity.CENTER),
        CENTER_RIGHT("Centre right", Gravity.CENTER_VERTICAL | Gravity.END),
        BOTTOM_LEFT("Bottom left", Gravity.BOTTOM | Gravity.START),
        BOTTOM_CENTER("Bottom centre", Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL),
        BOTTOM_RIGHT("Bottom right", Gravity.BOTTOM | Gravity.END);

        public final String label;
        public final int gravity;
        Anchor(String label, int gravity) { this.label = label; this.gravity = gravity; }

        public static String[] labels() {
            Anchor[] v = values();
            String[] out = new String[v.length];
            for (int i = 0; i < v.length; i++) out[i] = v[i].label;
            return out;
        }
    }

    public Type type = Type.TEXT;
    public String text = "";
    public int textColor = 0xFFFFFFFF;
    public int bgColor = 0x00000000;
    public float textSizeSp = 20f;
    public boolean bold = false;
    public int widthDp = 0;   // 0 = wrap
    public int heightDp = 0;  // 0 = wrap
    public Anchor anchor = Anchor.CENTER;
    public int offsetXDp = 0;
    public int offsetYDp = 0;
    public int cornerRadiusDp = 8;
    public int paddingDp = 12;
    public int strokeWidthDp = 0;
    public int strokeColor = 0xFFFFFFFF;

    public static OverlayElement defaults(Type type) {
        OverlayElement e = new OverlayElement();
        e.type = type;
        switch (type) {
            case TEXT:
                e.text = "Tap to continue";
                e.textColor = 0xFFFFFFFF;
                e.bgColor = 0x00000000;
                e.textSizeSp = 22f;
                e.bold = true;
                break;
            case BUTTON:
                e.text = "CONTINUE";
                e.textColor = 0xFFFFFFFF;
                e.bgColor = 0xFF2962FF;
                e.textSizeSp = 18f;
                e.bold = true;
                e.widthDp = 220;
                e.heightDp = 56;
                e.cornerRadiusDp = 28;
                e.paddingDp = 8;
                break;
            case BOX:
                e.bgColor = 0xFFFF5252;
                e.widthDp = 140;
                e.heightDp = 140;
                e.cornerRadiusDp = 12;
                e.paddingDp = 0;
                break;
        }
        return e;
    }

    public OverlayElement copy() {
        try {
            return fromJson(toJson());
        } catch (JSONException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public String summary() {
        String what = type == Type.BOX ? "Box " + widthDp + "×" + heightDp + " dp" : "\"" + text + "\"";
        return what + " · " + anchor.label + " (" + offsetXDp + ", " + offsetYDp + ")";
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("type", type.name());
        o.put("text", text);
        o.put("textColor", ColorUtil.toHex(textColor));
        o.put("bgColor", ColorUtil.toHex(bgColor));
        o.put("textSizeSp", textSizeSp);
        o.put("bold", bold);
        o.put("widthDp", widthDp);
        o.put("heightDp", heightDp);
        o.put("anchor", anchor.name());
        o.put("offsetXDp", offsetXDp);
        o.put("offsetYDp", offsetYDp);
        o.put("cornerRadiusDp", cornerRadiusDp);
        o.put("paddingDp", paddingDp);
        o.put("strokeWidthDp", strokeWidthDp);
        o.put("strokeColor", ColorUtil.toHex(strokeColor));
        return o;
    }

    public static OverlayElement fromJson(JSONObject o) throws JSONException {
        OverlayElement e = new OverlayElement();
        e.type = Type.valueOf(o.optString("type", "TEXT"));
        e.text = o.optString("text", "");
        e.textColor = ColorUtil.parse(o.optString("textColor", "#FFFFFFFF"), 0xFFFFFFFF);
        e.bgColor = ColorUtil.parse(o.optString("bgColor", "#00000000"), 0);
        e.textSizeSp = (float) o.optDouble("textSizeSp", 20);
        e.bold = o.optBoolean("bold", false);
        e.widthDp = o.optInt("widthDp", 0);
        e.heightDp = o.optInt("heightDp", 0);
        e.anchor = Anchor.valueOf(o.optString("anchor", "CENTER"));
        e.offsetXDp = o.optInt("offsetXDp", 0);
        e.offsetYDp = o.optInt("offsetYDp", 0);
        e.cornerRadiusDp = o.optInt("cornerRadiusDp", 8);
        e.paddingDp = o.optInt("paddingDp", 12);
        e.strokeWidthDp = o.optInt("strokeWidthDp", 0);
        e.strokeColor = ColorUtil.parse(o.optString("strokeColor", "#FFFFFFFF"), 0xFFFFFFFF);
        return e;
    }
}
