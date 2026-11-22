package dev.fnex.gui;

import dev.fnex.module.Modules;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

public class CosmeticsScreen extends FnexScreen {
    private static final int CW = 130, CH = 64, GAP = 12;
    private final Screen parent;
    private final float[] hover = new float[8];

    public CosmeticsScreen(Screen parent) { super("Cosmetics"); this.parent = parent; }

    private List<String> names() {
        List<String> l = new ArrayList<>();
        l.add("None");
        l.addAll(Modules.cosmetics.trail.options);
        return l;
    }
    private int selected() {
        return Modules.cosmetics.enabled ? 1 + Modules.cosmetics.trail.index : 0;
    }
    @Override protected float uiScale() { return Math.min(1f, height / 300f); }

    @Override protected void draw(GuiGraphicsExtractor c, int mx, int my) {
        float dt = dt(); int vw = vw(), vh = vh(), ac = Theme.accent();
        Draw.grad(c, 0, 0, vw, vh, 0xFF040102, 0xFF1E0505);
        Draw.circleGlow(c, vw / 2, vh / 2, 160, Theme.RED, 0.04f);
        Draw.gradTextC(c, "COSMETICS", vw / 2, 28, 2f, Theme.RED, Theme.AMBER);
        Draw.textC(c, "Particle Trail - follows you while you move", vw / 2, 52, Theme.GRAY);

        List<String> n = names();
        int x0 = (vw - (3 * CW + 2 * GAP)) / 2, y0 = 80;
        for (int i = 0; i < n.size(); i++) {
            int x = x0 + (i % 3) * (CW + GAP), y = y0 + (i / 3) * (CH + GAP);
            boolean hov = in(mx, my, x, y, CW, CH), sel = i == selected();
            hover[i] = Anim.step(hover[i], hov ? 1f : 0f, dt, 1.4f);
            if (sel || hover[i] > 0.05f) Draw.glow(c, x, y, CW, CH, 8, sel ? Theme.AMBER : ac, 6, sel ? 1f : hover[i] * 0.7f);
            Draw.outlined(c, x, y, CW, CH, 8, Draw.mix(0xF0140708, 0xF0260A0A, hover[i]), sel ? Theme.AMBER : Draw.mix(0x88FF1E16, Theme.ORANGE, hover[i]));
            Draw.textC(c, n.get(i), x + CW / 2, y + CH / 2 - 8, sel ? Theme.AMBER : 0xFFFFFFFF);
            Draw.textC(c, sel ? "ACTIVE" : "Click to equip", x + CW / 2, y + CH / 2 + 6, sel ? Theme.ORANGE : Theme.GRAY);
        }
        int by = y0 + 2 * (CH + GAP) + 6, bx = (vw - 120) / 2;
        boolean hb = in(mx, my, bx, by, 120, 22);
        hover[7] = Anim.step(hover[7], hb ? 1f : 0f, dt, 1.4f);
        if (hover[7] > 0.05f) Draw.glow(c, bx, by, 120, 22, 6, ac, 5, hover[7]);
        Draw.outlined(c, bx, by, 120, 22, 6, 0xF0120607, Draw.mix(0x88FF1E16, Theme.ORANGE, hover[7]));
        Draw.textC(c, "Back", bx + 60, by + 7, 0xFFFFFFFF);
    }

    @Override protected boolean click(int mx, int my, int btn) {
        if (btn != 0) return false;
        int vw = vw(), x0 = (vw - (3 * CW + 2 * GAP)) / 2, y0 = 80;
        List<String> n = names();
        for (int i = 0; i < n.size(); i++) {
            if (in(mx, my, x0 + (i % 3) * (CW + GAP), y0 + (i / 3) * (CH + GAP), CW, CH)) {
                Sounds.click();
                if (i == 0) Modules.cosmetics.setEnabled(false);
                else { Modules.cosmetics.trail.set(n.get(i)); Modules.cosmetics.setEnabled(true); }
                return true;
            }
        }
        if (in(mx, my, (vw - 120) / 2, y0 + 2 * (CH + GAP) + 6, 120, 22)) { Sounds.click(); onClose(); return true; }
        return false;
    }
    @Override public void onClose() { minecraft.setScreenAndShow(parent); }
}
