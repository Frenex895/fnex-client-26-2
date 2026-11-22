package dev.fnex.gui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

import java.io.InputStream;

/** Immediate-mode drawing helpers. All colors are ARGB; {@link #a} is a global alpha multiplier. */
public final class Draw {
    public static final Identifier LOGO = Identifier.fromNamespaceAndPath("fnex", "textures/gui/logo.png");
    public static float a = 1f;
    private static int lw = -1, lh = -1;

    private static int ca(int c) {
        int al = (int) (((c >>> 24) & 255) * a);
        return (al << 24) | (c & 0xFFFFFF);
    }
    public static int alpha(int c, float f) {
        int al = (int) (((c >>> 24) & 255) * Math.max(0f, Math.min(1f, f)));
        return (al << 24) | (c & 0xFFFFFF);
    }
    public static int mix(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int out = 0;
        for (int s = 24; s >= 0; s -= 8)
            out |= ((int) (((c1 >> s) & 255) * (1 - t) + ((c2 >> s) & 255) * t) & 255) << s;
        return out;
    }

    public static void rect(GuiGraphicsExtractor c, int x, int y, int w, int h, int col) {
        if (w <= 0 || h <= 0) return;
        col = ca(col);
        if ((col >>> 24) < 2) return;
        c.fill(x, y, x + w, y + h, col);
    }
    public static void frame(GuiGraphicsExtractor c, int x, int y, int w, int h, int col) {
        rect(c, x, y, w, 1, col); rect(c, x, y + h - 1, w, 1, col);
        rect(c, x, y + 1, 1, h - 2, col); rect(c, x + w - 1, y + 1, 1, h - 2, col);
    }
    public static void grad(GuiGraphicsExtractor c, int x, int y, int w, int h, int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        c.fillGradient(x, y, x + w, y + h, ca(top), ca(bottom));
    }

    public static void round(GuiGraphicsExtractor c, int x, int y, int w, int h, int r, int col) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        col = ca(col);
        if ((col >>> 24) < 2) return;
        if (r == 0) { c.fill(x, y, x + w, y + h, col); return; }
        c.fill(x, y + r, x + w, y + h - r, col);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, col);
            c.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, col);
        }
    }
    public static void outlined(GuiGraphicsExtractor c, int x, int y, int w, int h, int r, int fill, int border) {
        round(c, x, y, w, h, r, border);
        round(c, x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), fill);
    }
    public static void glow(GuiGraphicsExtractor c, int x, int y, int w, int h, int r, int col, int spread, float intensity) {
        for (int i = spread; i >= 1; i--)
            round(c, x - i, y - i, w + 2 * i, h + 2 * i, r + i, alpha(col, intensity * 0.05f));
    }
    public static void circle(GuiGraphicsExtractor c, int cx, int cy, int r, int col, int step) {
        for (int dy = -r; dy < r; dy += step) {
            double yy = Math.abs(dy + step / 2.0);
            if (yy > r) continue;
            int half = (int) Math.sqrt(r * r - yy * yy);
            rect(c, cx - half, cy + dy, half * 2, step, col);
        }
    }
    public static void circleGlow(GuiGraphicsExtractor c, int cx, int cy, int r, int col, float perLayer) {
        for (int k = 0; k < 8; k++) circle(c, cx, cy, (int) (r * (1 - k / 8f)), alpha(col, perLayer), 3);
    }

    // ---- text ----
    public static Font font() { return Minecraft.getInstance().font; }
    public static int tw(String s) { return font().width(s); }
    public static void text(GuiGraphicsExtractor c, String s, int x, int y, int col) {
        col = ca(col);
        if ((col >>> 24) < 8) return;
        c.text(font(), s, x, y, col, true);
    }
    public static void textC(GuiGraphicsExtractor c, String s, int cx, int y, int col) { text(c, s, cx - tw(s) / 2, y, col); }
    public static void textS(GuiGraphicsExtractor c, String s, float x, float y, float scale, int col) {
        col = ca(col);
        if ((col >>> 24) < 8) return;
        Matrix3x2fStack m = c.pose();
        m.pushMatrix(); m.translate(x, y); m.scale(scale, scale);
        c.text(font(), s, 0, 0, col, true);
        m.popMatrix();
    }
    public static void gradTextC(GuiGraphicsExtractor c, String s, int cx, int y, float scale, int c1, int c2) {
        float x = cx - tw(s) * scale / 2f;
        for (int i = 0; i < s.length(); i++) {
            String ch = s.substring(i, i + 1);
            textS(c, ch, x, y, scale, mix(c1, c2, i / (float) Math.max(1, s.length() - 1)));
            x += tw(ch) * scale;
        }
    }

    // ---- logo ----
    private static void loadLogoSize() {
        if (lw > 0) return;
        lw = lh = 512;
        try (InputStream in = Minecraft.getInstance().getResourceManager().getResource(LOGO).orElseThrow().open();
             NativeImage img = NativeImage.read(in)) {
            lw = img.getWidth(); lh = img.getHeight();
        } catch (Exception ignored) {}
    }
    /** Draws the FNEX logo with its top-left corner at (x, y), longest side = size (GUI px). */
    public static void logo(GuiGraphicsExtractor c, float x, float y, float size) {
        loadLogoSize();
        int col = ((int) (255 * a) << 24) | 0xFFFFFF;
        if ((col >>> 24) < 2) return;
        float sc = size / Math.max(lw, lh);
        Matrix3x2fStack m = c.pose();
        m.pushMatrix(); m.translate(x, y); m.scale(sc, sc);
        c.blit(RenderPipelines.GUI_TEXTURED, LOGO, 0, 0, 0f, 0f, lw, lh, lw, lh, lw, lh, col);
        m.popMatrix();
    }
}
