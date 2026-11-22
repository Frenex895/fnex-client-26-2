package dev.fnex.gui;

import dev.fnex.config.ConfigManager;
import dev.fnex.module.Feature;
import dev.fnex.module.HudFeature;
import dev.fnex.module.Modules;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

public class HudEditorScreen extends FnexScreen {
    private final Screen parent;
    private HudFeature dragged;
    private int offX, offY;

    public HudEditorScreen(Screen parent) { super("HUD Editor"); this.parent = parent; }

    @Override protected void draw(GuiGraphicsExtractor c, int mx, int my) {
        Draw.rect(c, 0, 0, width, height, 0x80000000);
        int ac = Theme.accent();
        boolean any = false;
        for (Feature f : Modules.ALL) {
            if (!(f instanceof HudFeature h) || !h.enabled) continue;
            any = true;
            h.hudX = Math.max(0, Math.min(h.hudX, width - h.w));
            h.hudY = Math.max(0, Math.min(h.hudY, height - h.h));
            h.draw(c, h.hudX, h.hudY);
            boolean hov = in(mx, my, h.hudX, h.hudY, h.w, h.h) || h == dragged;
            Draw.frame(c, h.hudX - 1, h.hudY - 1, h.w + 2, h.h + 2, hov ? Theme.AMBER : Draw.alpha(ac, 0.7f));
        }
        Draw.textC(c, "HUD EDITOR", width / 2, 12, Theme.AMBER);
        Draw.textC(c, any ? "Drag the elements. ESC to finish." : "No HUD element is enabled. Enable some in the FNEX menu first.", width / 2, 26, Theme.GRAY);
    }

    @Override protected boolean click(int mx, int my, int btn) {
        if (btn != 0) return false;
        for (int i = Modules.ALL.size() - 1; i >= 0; i--) {
            if (Modules.ALL.get(i) instanceof HudFeature h && h.enabled && in(mx, my, h.hudX, h.hudY, h.w, h.h)) {
                dragged = h; offX = mx - h.hudX; offY = my - h.hudY; return true;
            }
        }
        return false;
    }
    @Override protected boolean drag(int mx, int my, int btn) {
        if (dragged == null) return false;
        dragged.hudX = Math.max(0, Math.min(mx - offX, width - dragged.w));
        dragged.hudY = Math.max(0, Math.min(my - offY, height - dragged.h));
        ConfigManager.markDirty();
        return true;
    }
    @Override protected boolean release(int mx, int my, int btn) { boolean d = dragged != null; dragged = null; return d; }
    @Override public void onClose() { ConfigManager.save(); minecraft.setScreenAndShow(parent); }
}
