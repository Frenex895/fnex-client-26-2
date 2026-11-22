package dev.fnex.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import dev.fnex.config.ConfigManager;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.ArrayList;
import java.util.List;

public abstract class Setting {
    public final String name;
    public boolean persist = true;

    protected Setting(String name) { this.name = name; }
    public abstract JsonElement save();
    public abstract void load(JsonElement e);

    public static final class Bool extends Setting {
        public boolean value;
        public float anim;
        public Bool(String n, boolean d) { super(n); value = d; anim = d ? 1f : 0f; }
        public void set(boolean v) { if (v != value) { value = v; ConfigManager.markDirty(); } }
        @Override public JsonElement save() { return new JsonPrimitive(value); }
        @Override public void load(JsonElement e) { try { value = e.getAsBoolean(); anim = value ? 1f : 0f; } catch (Exception ignored) {} }
    }

    public static final class Num extends Setting {
        public double value;
        public final double min, max, step;
        public Num(String n, double d, double min, double max, double step) { super(n); this.value = d; this.min = min; this.max = max; this.step = step; }
        public void set(double v) {
            v = Math.max(min, Math.min(max, v));
            if (step > 0) v = min + Math.round((v - min) / step) * step;
            if (v != value) { value = v; ConfigManager.markDirty(); }
        }
        public int i() { return (int) Math.round(value); }
        public float f() { return (float) value; }
        @Override public JsonElement save() { return new JsonPrimitive(value); }
        @Override public void load(JsonElement e) { try { set(e.getAsDouble()); } catch (Exception ignored) {} }
    }

    public static final class Mode extends Setting {
        public List<String> options;
        public int index;
        public Mode(String n, int idx, String... o) { super(n); options = new ArrayList<>(List.of(o)); index = idx; }
        public String get() { return options.isEmpty() ? "" : options.get(Math.max(0, Math.min(index, options.size() - 1))); }
        public void cycle(int d) { if (options.isEmpty()) return; index = (index + d + options.size()) % options.size(); ConfigManager.markDirty(); }
        public void set(String s) { int i = options.indexOf(s); if (i >= 0) { index = i; ConfigManager.markDirty(); } }
        @Override public JsonElement save() { return new JsonPrimitive(get()); }
        @Override public void load(JsonElement e) { try { set(e.getAsString()); } catch (Exception ignored) {} }
    }

    public static final class Color extends Setting {
        public int value;
        public float h, s, v, a = 1f;
        public boolean open;
        public final boolean allowAlpha;
        public Color(String n, int argb, boolean allowAlpha) { super(n); this.allowAlpha = allowAlpha; setArgb(argb); }
        public void setArgb(int argb) {
            if (!allowAlpha) argb |= 0xFF000000;
            value = argb;
            float[] hsv = rgbToHsv(argb);
            h = hsv[0]; s = hsv[1]; v = hsv[2];
            a = ((argb >>> 24) & 255) / 255f;
            ConfigManager.markDirty();
        }
        public void setHsva(float h, float s, float v, float a) {
            this.h = h; this.s = s; this.v = v; this.a = allowAlpha ? a : 1f;
            value = ((int) (this.a * 255f + 0.5f) << 24) | hsvToRgb(h, s, v);
            ConfigManager.markDirty();
        }
        public int rgb() { return hsvToRgb(h, s, v); }
        @Override public JsonElement save() { return new JsonPrimitive(value); }
        @Override public void load(JsonElement e) { try { setArgb(e.getAsInt()); } catch (Exception ignored) {} }
    }

    public static final class Key extends Setting {
        public int value;
        public final int def;
        public Key(String n, int d) { super(n); value = d; def = d; }
        public String display() {
            if (value < 0) return "None";
            try { return InputConstants.Type.KEYSYM.getOrCreate(value).getDisplayName().getString(); }
            catch (Exception e) { return "Key " + value; }
        }
        @Override public JsonElement save() { return new JsonPrimitive(value); }
        @Override public void load(JsonElement e) { try { value = e.getAsInt(); } catch (Exception ignored) {} }
    }

    public static final class Action extends Setting {
        public final Runnable run;
        public Action(String n, Runnable r) { super(n); run = r; persist = false; }
        @Override public JsonElement save() { return JsonNull.INSTANCE; }
        @Override public void load(JsonElement e) {}
    }

    public static int hsvToRgb(float h, float s, float v) {
        h = ((h % 1f) + 1f) % 1f;
        float c = v * s, x = c * (1 - Math.abs((h * 6f) % 2f - 1)), m = v - c, r, g, b;
        switch ((int) (h * 6f) % 6) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        return ((int) ((r + m) * 255 + .5f) << 16) | ((int) ((g + m) * 255 + .5f) << 8) | (int) ((b + m) * 255 + .5f);
    }

    public static float[] rgbToHsv(int argb) {
        float r = ((argb >> 16) & 255) / 255f, g = ((argb >> 8) & 255) / 255f, b = (argb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min, h;
        if (d == 0) h = 0;
        else if (max == r) h = (((g - b) / d) % 6f) / 6f;
        else if (max == g) h = ((b - r) / d + 2f) / 6f;
        else h = ((r - g) / d + 4f) / 6f;
        if (h < 0) h += 1f;
        return new float[]{h, max == 0 ? 0 : d / max, max};
    }
}
