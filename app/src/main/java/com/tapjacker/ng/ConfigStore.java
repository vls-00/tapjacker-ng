package com.tapjacker.ng;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/** Current config and saved presets, in SharedPreferences. */
public class ConfigStore {
    private static final String PREFS = "tapjacker";
    private static final String KEY_CURRENT = "current";
    private static final String KEY_PRESETS = "presets";

    private final SharedPreferences prefs;

    public ConfigStore(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public OverlayConfig loadCurrent() {
        String s = prefs.getString(KEY_CURRENT, null);
        if (s != null) {
            try {
                return OverlayConfig.fromJsonString(s);
            } catch (JSONException ignored) {
            }
        }
        return OverlayConfig.defaults();
    }

    public void saveCurrent(OverlayConfig cfg) {
        prefs.edit().putString(KEY_CURRENT, cfg.toJsonString()).apply();
    }

    private JSONObject presets() {
        try {
            return new JSONObject(prefs.getString(KEY_PRESETS, "{}"));
        } catch (JSONException ex) {
            return new JSONObject();
        }
    }

    public List<String> presetNames() {
        List<String> names = new ArrayList<>();
        Iterator<String> it = presets().keys();
        while (it.hasNext()) names.add(it.next());
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public OverlayConfig loadPreset(String name) {
        try {
            JSONObject o = presets().optJSONObject(name);
            return o == null ? null : OverlayConfig.fromJson(o);
        } catch (JSONException ex) {
            return null;
        }
    }

    public void savePreset(String name, OverlayConfig cfg) {
        try {
            JSONObject all = presets();
            all.put(name, cfg.toJson());
            prefs.edit().putString(KEY_PRESETS, all.toString()).apply();
        } catch (JSONException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public boolean hasPreset(String name) {
        return presets().has(name);
    }

    public void renamePreset(String from, String to) {
        JSONObject all = presets();
        JSONObject cfg = all.optJSONObject(from);
        if (cfg == null) return;
        all.remove(from);
        try {
            all.put(to, cfg);
        } catch (JSONException ex) {
            throw new IllegalStateException(ex);
        }
        prefs.edit().putString(KEY_PRESETS, all.toString()).apply();
    }

    public void deletePreset(String name) {
        JSONObject all = presets();
        all.remove(name);
        prefs.edit().putString(KEY_PRESETS, all.toString()).apply();
    }
}
