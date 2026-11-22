package dev.fnex.module;

import dev.fnex.gui.Draw;
import dev.fnex.gui.Theme;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public abstract class HudFeature extends Feature {
    public int hudX, hudY, w = 60, h = 18;

    protected HudFeature(String n, String d, Category c, int x, int y) { super(n, d, c); hudX = x; hudY = y; }

    protected abstract String label();
    protected abstract String value();

    public void draw(GuiGraphicsExtractor c, int x, int y) {
        String l = label() + " ", v = value();
        w = Draw.tw(l + v) + 14; h = 18;
        Draw.round(c, x, y, w, h, 4, 0xB00A0405);
        Draw.round(c, x, y + 2, 2, h - 4, 1, Theme.accent());
        Draw.text(c, l, x + 7, y + 5, Theme.accent2());
        Draw.text(c, v, x + 7 + Draw.tw(l), y + 5, 0xFFFFFFFF);
    }

    public void renderHud(GuiGraphicsExtractor c) {
        if (!enabled) return;
        int x = Math.max(0, Math.min(hudX, c.guiWidth() - w));
        int y = Math.max(0, Math.min(hudY, c.guiHeight() - h));
        draw(c, x, y);
    }
}
