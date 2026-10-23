package com.tapjacker.ng;

import java.util.Locale;

public final class ColorUtil {
    private ColorUtil() {}

    public static String toHex(int argb) {
        return String.format(Locale.ROOT, "#%08X", argb);
    }

    // #RGB, #RRGGBB or #AARRGGBB, hash optional
    public static int parse(String s, int fallback) {
        if (s == null) return fallback;
        String t = s.trim();
        if (t.startsWith("#")) t = t.substring(1);
        try {
            if (t.length() == 3) {
                StringBuilder b = new StringBuilder("FF");
                for (char c : t.toCharArray()) b.append(c).append(c);
                t = b.toString();
            } else if (t.length() == 6) {
                t = "FF" + t;
            } else if (t.length() != 8) {
                return fallback;
            }
            return (int) Long.parseLong(t, 16);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

}
