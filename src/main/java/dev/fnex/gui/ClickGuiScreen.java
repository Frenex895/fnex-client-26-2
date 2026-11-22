package dev.fnex.gui;

import dev.fnex.FnexClient;
import dev.fnex.config.ConfigManager;
import dev.fnex.module.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/** The FNEX client menu (opened with Right Shift). */
public class ClickGuiScreen extends FnexScreen {
    private static final int W = 540, H = 320, SW = 118, CARD_H = 30, ROW_H = 22, PICK_H = 96;
    private static final int[] PRESETS = {0xFFFF1E16, 0xFFFF7800, 0xFFFFB000, 0xFFFFFFFF, 0xFF00E5FF, 0xFF7CFF00, 0xFFB400FF, 0xFFFF2D95};
    private static Category selected = Category.VISUALS;

    private final Screen parent;
    private float open = 0f;
    private boolean closing = false;
    private String search = "";
    private float scroll = 0f, scrollTarget = 0f;
    private int maxScroll = 0;
    private final Set<Feature> expanded = new HashSet<>();
    private final float[] catAnim = new float[Category.values().length];
    private Setting dragging;
    private int dragMode;
    private Setting.Key listening;
    private final List<Row> rows = new ArrayList<>();
    private int listX, listY, listW, listH;

    private static final class Row { int kind, x, y, w, h; Feature f; Setting s; }

    public ClickGuiScreen(Screen parent) { super("FNEX"); this.parent = parent; }

    @Override protected float uiScale() {
        float fit = Math.min(width / (W + 16f), height / (H + 16f));
        return Math.max(0.4f, Math.min(Modules.interfaceFeature.guiScale.f(), fit));
    }

    @Override public void onClose() { if (!closing) { closing = true; ConfigManager.save(); } }
    @Override public void removed() { ConfigManager.save(); }

    private boolean inList(int mx, int my) { return in(mx, my, listX - 4, listY, listW + 8, listH); }

    private List<Setting> settingsOf(Feature f) {
        List<Setting> l = new ArrayList<>();
        if (f.hasKey) l.add(f.key);
        l.addAll(f.settings);
        return l;
    }

    // ------------------------------------------------------------------ drawing
    @Override protected void draw(GuiGraphicsExtractor c, int mx, int my) {
        float dt = dt();
        open = Anim.step(open, closing ? 0f : 1f, dt, 1.6f);
        if (closing && open <= 0.02f) { minecraft.setScreenAndShow(parent); return; }
        Draw.a = open;
        int vw = vw(), vh = vh(), ac = Theme.accent();
        int px = (vw - W) / 2, py = (vh - H) / 2;

        Draw.rect(c, 0, 0, vw, vh, 0x90000000);
        Draw.grad(c, 0, vh / 2, vw, vh - vh / 2, 0x00FF1E16, Draw.alpha(ac, 0.22f));

        var ms = c.pose();
        float sc = 0.94f + 0.06f * open;
        ms.pushMatrix(); ms.translate(vw / 2f, vh / 2f); ms.scale(sc, sc); ms.translate(-vw / 2f, -vh / 2f);

        Draw.glow(c, px, py, W, H, 10, ac, 10, 1f);
        Draw.outlined(c, px, py, W, H, 10, 0xF00C0506, Draw.alpha(ac, 0.85f));

        // sidebar
        Draw.round(c, px + 6, py + 6, SW, H - 12, 8, 0xFF140708);
        Draw.circleGlow(c, px + 6 + SW / 2, py + 48, 40, ac, 0.05f);
        Draw.logo(c, px + 6 + SW / 2f - 24, py + 24, 48);
        Draw.gradTextC(c, "FNEX", px + 6 + SW / 2, py + 76, 1.4f, Theme.RED, Theme.AMBER);
        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            int x = px + 14, y = py + 100 + i * 26, w = SW - 16;
            boolean sel = cats[i] == selected && search.isEmpty();
            boolean hov = in(mx, my, x, y, w, 22);
            catAnim[i] = Anim.step(catAnim[i], sel ? 1f : (hov ? 0.4f : 0f), dt, 1.4f);
            Draw.round(c, x, y, w, 22, 6, Draw.alpha(ac, 0.28f * catAnim[i]));
            if (sel) Draw.round(c, x, y + 4, 3, 14, 1, ac);
            Draw.text(c, cats[i].label, x + 12, y + 7, Draw.mix(Theme.GRAY, Theme.WHITE, catAnim[i]));
        }
        Draw.text(c, "Profile: " + ConfigManager.profile, px + 14, py + H - 32, Theme.GRAY);
        Draw.text(c, "v" + FnexClient.VERSION, px + 14, py + H - 20, 0xFF665554);

        // search
        listX = px + SW + 18; listW = W - SW - 30; listY = py + 44; listH = H - 56;
        Draw.outlined(c, listX, py + 12, listW, 22, 6, 0xFF140708, Draw.alpha(ac, 0.35f));
        if (search.isEmpty()) Draw.text(c, "Search modules...", listX + 8, py + 19, 0xFF776664);
        else Draw.text(c, search + ((System.currentTimeMillis() / 500) % 2 == 0 ? "_" : ""), listX + 8, py + 19, Theme.WHITE);

        // module list
        List<Feature> mods = new ArrayList<>();
        String q = search.toLowerCase(Locale.ROOT);
        for (Feature f : Modules.ALL) {
            if (q.isEmpty() ? f.category == selected : (f.name.toLowerCase(Locale.ROOT).contains(q) || f.desc.toLowerCase(Locale.ROOT).contains(q))) mods.add(f);
        }
        rows.clear();
        c.enableScissor(listX - 4, listY, listX + listW + 4, listY + listH);
        int y = listY - Math.round(scroll);
        for (Feature f : mods) {
            Row r = new Row(); r.kind = 0; r.f = f; r.x = listX; r.y = y; r.w = listW; r.h = CARD_H; rows.add(r);
            if (visible(r)) drawCard(c, r, mx, my, dt);
            y += CARD_H + 4;
            if (expanded.contains(f)) {
                for (Setting s : settingsOf(f)) {
                    Row sr = new Row(); sr.kind = 1; sr.f = f; sr.s = s; sr.x = listX + 10; sr.w = listW - 10; sr.y = y; sr.h = ROW_H; rows.add(sr);
                    if (visible(sr)) drawSetting(c, sr, mx, my, dt);
                    y += ROW_H + 2;
                    if (s instanceof Setting.Color col && col.open) {
                        Row pr = new Row(); pr.kind = 2; pr.f = f; pr.s = s; pr.x = listX + 10; pr.w = listW - 10; pr.y = y; pr.h = PICK_H; rows.add(pr);
                        if (visible(pr)) drawPicker(c, pr);
                        y += PICK_H + 4;
                    }
                }
                y += 4;
            }
        }
        if (mods.isEmpty()) Draw.textC(c, "No modules found", listX + listW / 2, listY + 30, Theme.GRAY);
        int contentH = y + Math.round(scroll) - listY;
        maxScroll = Math.max(0, contentH - listH);
        scrollTarget = Math.max(0, Math.min(scrollTarget, maxScroll));
        scroll = Anim.step(scroll, scrollTarget, dt, 1.5f);
        c.disableScissor();

        ms.popMatrix();
        Draw.a = 1f;
    }

    private boolean visible(Row r) { return r.y + r.h >= listY && r.y <= listY + listH; }

    private void drawCard(GuiGraphicsExtractor c, Row r, int mx, int my, float dt) {
        Feature f = r.f; int ac = Theme.accent();
        boolean hov = in(mx, my, r.x, r.y, r.w, r.h) && inList(mx, my);
        f.anim = Anim.step(f.anim, f.enabled ? 1f : 0f, dt, 1.4f);
        if (f.anim > 0.05f && f.toggleable) Draw.glow(c, r.x, r.y, r.w, r.h, 6, ac, 3, f.anim * 0.8f);
        Draw.outlined(c, r.x, r.y, r.w, r.h, 6, Draw.mix(Theme.CARD, Theme.CARD2, hov ? 1f : 0f),
                Draw.mix(0xFF2A1010, ac, f.toggleable ? f.anim * 0.8f : (expanded.contains(f) ? 0.6f : 0f)));
        Draw.text(c, f.name, r.x + 10, r.y + 6, Theme.WHITE);
        Draw.textS(c, f.desc, r.x + 10, r.y + 18, 0.85f, Theme.GRAY);
        if (f.isAction()) Draw.text(c, "OPEN", r.x + r.w - 10 - Draw.tw("OPEN"), r.y + 11, Theme.AMBER);
        else if (f.toggleable) {
            pill(c, r.x + r.w - 40, r.y + 8, 28, 14, f.anim);
            if (!settingsOf(f).isEmpty()) Draw.text(c, expanded.contains(f) ? "v" : ">", r.x + r.w - 54, r.y + 11, Theme.GRAY);
        } else Draw.text(c, expanded.contains(f) ? "v" : ">", r.x + r.w - 18, r.y + 11, Theme.GRAY);
    }

    private void pill(GuiGraphicsExtractor c, int x, int y, int w, int h, float t) {
        Draw.round(c, x, y, w, h, h / 2, Draw.mix(0xFF2C2022, Theme.accent(), t));
        Draw.round(c, x + 2 + (int) ((w - h) * t), y + 2, h - 4, h - 4, (h - 4) / 2, 0xFFFFFFFF);
    }

    private int sliderX(Row r) { return r.x + r.w - 170; }

    private void drawSetting(GuiGraphicsExtractor c, Row r, int mx, int my, float dt) {
        Setting s = r.s; int ac = Theme.accent();
        boolean hov = in(mx, my, r.x, r.y, r.w, r.h) && inList(mx, my);
        Draw.round(c, r.x, r.y, r.w, r.h, 5, hov ? 0xFF1A0A0B : 0xFF120708);
        int ty = r.y + (r.h - 8) / 2;
        if (s instanceof Setting.Action a) {
            Draw.round(c, r.x, r.y, r.w, r.h, 5, Draw.alpha(ac, hov ? 0.3f : 0.12f));
            Draw.textC(c, a.name, r.x + r.w / 2, ty, Theme.WHITE);
            return;
        }
        Draw.text(c, s.name, r.x + 8, ty, 0xFFD8C8C4);
        if (s instanceof Setting.Bool b) {
            b.anim = Anim.step(b.anim, b.value ? 1f : 0f, dt, 1.4f);
            pill(c, r.x + r.w - 36, r.y + 4, 28, 14, b.anim);
        } else if (s instanceof Setting.Num n) {
            int sx = sliderX(r), sw = 110;
            double t = (n.value - n.min) / (n.max - n.min);
            int fill = (int) (sw * t);
            Draw.round(c, sx, r.y + 9, sw, 4, 2, 0xFF2C2022);
            Draw.round(c, sx, r.y + 9, Math.max(4, fill), 4, 2, ac);
            Draw.round(c, sx + fill - 3, r.y + 7, 6, 8, 3, 0xFFFFFFFF);
            String v = n.step >= 1 ? String.valueOf(n.i()) : String.format(Locale.ROOT, "%.2f", n.value);
            Draw.text(c, v, sx + sw + 8, ty, Theme.AMBER);
        } else if (s instanceof Setting.Mode m) {
            box(c, r, m.get(), hov, false);
        } else if (s instanceof Setting.Key k) {
            box(c, r, listening == k ? "press a key..." : k.display(), hov, listening == k);
        } else if (s instanceof Setting.Color col) {
            String hex = String.format("#%06X", col.value & 0xFFFFFF) + (col.allowAlpha ? String.format("%02X", (col.value >>> 24) & 255) : "");
            Draw.text(c, hex, r.x + r.w - 40 - Draw.tw(hex), ty, Theme.GRAY);
            Draw.round(c, r.x + r.w - 34, r.y + 4, 26, 14, 4, 0xFF3A2A2A);
            Draw.round(c, r.x + r.w - 33, r.y + 5, 24, 12, 3, col.value);
        }
    }

    private void box(GuiGraphicsExtractor c, Row r, String text, boolean hov, boolean active) {
        int bw = Draw.tw(text) + 14, bx = r.x + r.w - bw - 6;
        Draw.outlined(c, bx, r.y + 3, bw, 16, 5, 0xFF1C0A0B, Draw.mix(0xFF3A1A1A, active ? Theme.AMBER : Theme.accent(), active ? 1f : (hov ? 0.9f : 0.4f)));
        Draw.textC(c, text, bx + bw / 2, r.y + 7, active ? Theme.AMBER : Theme.WHITE);
    }

    private void drawPicker(GuiGraphicsExtractor c, Row r) {
        Setting.Color col = (Setting.Color) r.s;
        Draw.round(c, r.x, r.y, r.w, r.h, 5, 0xFF0E0607);
        int sx = r.x + 10, sy = r.y + 6;
        for (int i = 0; i < 22; i++)
            for (int j = 0; j < 12; j++)
                Draw.rect(c, sx + i * 5, sy + j * 5, 5, 5, 0xFF000000 | Setting.hsvToRgb(col.h, i / 21f, 1f - j / 11f));
        int mx = sx + (int) (col.s * 110), my = sy + (int) ((1f - col.v) * 60);
        Draw.round(c, mx - 3, my - 3, 6, 6, 3, 0xFFFFFFFF);
        Draw.round(c, mx - 2, my - 2, 4, 4, 2, 0xFF000000 | col.rgb());
        for (int i = 0; i < 55; i++) Draw.rect(c, sx + i * 2, r.y + 72, 2, 10, 0xFF000000 | Setting.hsvToRgb(i / 55f, 1f, 1f));
        Draw.rect(c, sx + (int) (col.h * 110) - 1, r.y + 70, 3, 14, 0xFFFFFFFF);
        if (col.allowAlpha) {
            int ax = r.x + 128;
            Draw.rect(c, ax, sy, 10, 60, 0xFF202020);
            Draw.grad(c, ax, sy, 10, 60, 0xFF000000 | col.rgb(), col.rgb());
            Draw.rect(c, ax - 2, sy + (int) ((1f - col.a) * 60) - 1, 14, 2, 0xFFFFFFFF);
        }
        Draw.round(c, r.x + 150, r.y + 8, 90, 24, 5, 0xFF202020);
        Draw.round(c, r.x + 150, r.y + 8, 90, 24, 5, col.value);
        for (int i = 0; i < PRESETS.length; i++)
            Draw.outlined(c, r.x + 150 + (i % 4) * 23, r.y + 40 + (i / 4) * 23, 20, 20, 4, PRESETS[i], 0xFF3A2A2A);
    }

    // ------------------------------------------------------------------ input
    @Override protected boolean click(int mx, int my, int btn) {
        if (closing) return true;
        int px = (vw() - W) / 2, py = (vh() - H) / 2;
        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            if (in(mx, my, px + 14, py + 100 + i * 26, SW - 16, 22)) {
                selected = cats[i]; search = ""; scrollTarget = 0; listening = null; Sounds.click(); return true;
            }
        }
        if (!inList(mx, my)) return true;
        listening = null;
        for (Row r : rows) {
            if (!in(mx, my, r.x, r.y, r.w, r.h)) continue;
            if (r.kind == 0) {
                Feature f = r.f;
                Sounds.click();
                if (f.isAction()) { if (btn == 0) f.action.run(); }
                else if (f.toggleable && btn == 0 && mx >= r.x + r.w - 46) f.toggle();
                else if (!settingsOf(f).isEmpty()) { if (!expanded.remove(f)) expanded.add(f); }
                else if (f.toggleable && btn == 0) f.toggle();
            } else if (r.kind == 1) {
                Setting s = r.s;
                Sounds.click();
                if (s instanceof Setting.Bool b) b.set(!b.value);
                else if (s instanceof Setting.Num n) { dragging = n; slide(n, r, mx); }
                else if (s instanceof Setting.Mode m) m.cycle(btn == 1 ? -1 : 1);
                else if (s instanceof Setting.Color col) col.open = !col.open;
                else if (s instanceof Setting.Key k) listening = k;
                else if (s instanceof Setting.Action a) { a.run.run(); }
            } else {
                Setting.Color col = (Setting.Color) r.s;
                int sx = r.x + 10, sy = r.y + 6;
                if (in(mx, my, sx, sy, 110, 60)) dragMode = 1;
                else if (in(mx, my, sx, r.y + 70, 110, 14)) dragMode = 2;
                else if (col.allowAlpha && in(mx, my, r.x + 126, sy, 14, 60)) dragMode = 3;
                else {
                    dragMode = 0;
                    for (int i = 0; i < PRESETS.length; i++)
                        if (in(mx, my, r.x + 150 + (i % 4) * 23, r.y + 40 + (i / 4) * 23, 20, 20)) { col.setArgb(PRESETS[i]); Sounds.click(); }
                }
                if (dragMode != 0) { dragging = col; pick(col, r, mx, my); }
            }
            return true;
        }
        return true;
    }

    private void slide(Setting.Num n, Row r, int mx) {
        double t = Math.max(0, Math.min(1, (mx - sliderX(r)) / 110.0));
        n.set(n.min + t * (n.max - n.min));
    }

    private void pick(Setting.Color col, Row r, int mx, int my) {
        int sx = r.x + 10, sy = r.y + 6;
        switch (dragMode) {
            case 1 -> col.setHsva(col.h, clamp((mx - sx) / 110f), 1f - clamp((my - sy) / 60f), col.a);
            case 2 -> col.setHsva(Math.min(0.9999f, clamp((mx - sx) / 110f)), col.s, col.v, col.a);
            case 3 -> col.setHsva(col.h, col.s, col.v, 1f - clamp((my - sy) / 60f));
            default -> {}
        }
    }
    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }

    @Override protected boolean drag(int mx, int my, int btn) {
        if (dragging == null) return false;
        for (Row r : rows) {
            if (r.s != dragging) continue;
            if (dragging instanceof Setting.Num n && r.kind == 1) { slide(n, r, mx); return true; }
            if (dragging instanceof Setting.Color col && r.kind == 2) { pick(col, r, mx, my); return true; }
        }
        return true;
    }
    @Override protected boolean release(int mx, int my, int btn) { dragging = null; dragMode = 0; return false; }
    @Override protected boolean scroll(int mx, int my, double amount) {
        scrollTarget = (float) Math.max(0, Math.min(maxScroll, scrollTarget - amount * 28));
        return true;
    }

    @Override public boolean keyPressed(KeyEvent ev) {
        int key = ev.key();
        if (listening != null) {
            listening.value = key == GLFW.GLFW_KEY_ESCAPE ? -1 : key;
            listening = null; ConfigManager.markDirty(); Sounds.click();
            return true;
        }
        if (key == Modules.keybinds.menuKey.value || key == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) { search = search.substring(0, search.length() - 1); scrollTarget = 0; return true; }
        return super.keyPressed(ev);
    }
    @Override public boolean charTyped(CharacterEvent ev) {
        char chr = (char) ev.codepoint();
        if (listening == null && chr >= 32 && search.length() < 24) { search += chr; scrollTarget = 0; return true; }
        return false;
    }
}
