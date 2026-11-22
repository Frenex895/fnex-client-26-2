package dev.fnex.gui;

import dev.fnex.FnexClient;
import dev.fnex.module.Modules;
import dev.fnex.util.UpdateChecker;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.KeyEvent;

import java.util.Random;

public class FnexTitleScreen extends FnexScreen {
    private static boolean seen = false;
    private static final String[] LABELS = {"Play Singleplayer", "Multiplayer", "Settings", "Cosmetics", "Quit Game"};
    private static final int BW = 200, BH = 24, GAP = 28, N = 64;

    private float t = 0f;
    private final float[] hover = new float[LABELS.length];
    private final float[] px = new float[N], py = new float[N], ps = new float[N], pz = new float[N];
    private final Random rnd = new Random();
    private float hoverMc;

    public FnexTitleScreen() {
        super("FNEX CLIENT");
        if (seen) t = 3f;          // intro animation plays only the first time
        seen = true;
        for (int i = 0; i < N; i++) respawn(i, true);
    }

    private void respawn(int i, boolean anywhere) {
        px[i] = rnd.nextFloat();
        py[i] = anywhere ? rnd.nextFloat() : 1.05f;
        ps[i] = 0.03f + rnd.nextFloat() * 0.09f;
        pz[i] = 1 + rnd.nextInt(3);
    }

    @Override protected float uiScale() { return Math.min(1f, height / 340f); }
    private int oy() { return Math.max(0, (vh() - 330) / 3); }

    @Override protected void draw(GuiGraphicsExtractor c, int mx, int my) {
        float dt = dt(); t += dt;
        int vw = vw(), vh = vh(), oy = oy(), ac = Theme.accent();

        // background
        Draw.grad(c, 0, 0, vw, vh, 0xFF040102, 0xFF1E0505);
        for (int i = 0; i < 3; i++) {
            int bx = (int) (vw * (0.5f + 0.38f * Math.sin(t * 0.30f + i * 2.1f)));
            int by = (int) (vh * (0.55f + 0.25f * Math.cos(t * 0.25f + i * 1.7f)));
            Draw.circleGlow(c, bx, by, 120 + (int) (25 * Math.sin(t * 0.5f + i)), i % 2 == 0 ? Theme.RED : Theme.ORANGE, 0.06f);
        }
        // particles
        for (int i = 0; i < N; i++) {
            py[i] -= ps[i] * dt;
            if (py[i] < -0.05f) respawn(i, false);
            float flick = 0.5f + 0.5f * (float) Math.sin(t * 3f + i);
            int col = i % 3 == 0 ? Theme.AMBER : (i % 3 == 1 ? Theme.ORANGE : Theme.RED);
            Draw.rect(c, (int) (px[i] * vw + Math.sin(t + i) * 8), (int) (py[i] * vh), (int) pz[i], (int) pz[i], Draw.alpha(col, 0.2f + 0.55f * flick));
        }

        // logo + title
        float e = Anim.ease(Math.min(1f, t / 1.2f));
        float logoSize = 96 + 4 * (float) Math.sin(t * 2f);
        int lx = vw / 2, ly = 78 + oy;
        Draw.a = e;
        Draw.circleGlow(c, lx, ly, 95, ac, 0.05f);
        Draw.logo(c, lx - logoSize / 2f, ly - logoSize / 2f, logoSize);
        Draw.gradTextC(c, "FNEX CLIENT", vw / 2, 136 + oy, 2f, Theme.RED, Theme.AMBER);
        Draw.textC(c, "PREMIUM PVP CLIENT", vw / 2, 158 + oy, Theme.GRAY);
        Draw.a = 1f;

        // buttons
        int bx = (vw - BW) / 2;
        for (int i = 0; i < LABELS.length; i++) {
            float bp = Anim.ease((t - 0.5f - i * 0.1f) / 0.45f);
            int x = bx + (int) ((1 - bp) * 40), y = 178 + oy + i * GAP;
            boolean hov = bp > 0.9f && in(mx, my, x, y, BW, BH);
            hover[i] = Anim.step(hover[i], hov ? 1f : 0f, dt, 1.4f);
            float hv = hover[i];
            Draw.a = bp;
            if (hv > 0.02f) Draw.glow(c, x, y, BW, BH, 6, ac, 7, hv);
            Draw.outlined(c, x, y, BW, BH, 6, Draw.mix(0xF0120607, 0xF0260A0A, hv), Draw.mix(0x88FF1E16, Theme.ORANGE, hv));
            if (hv > 0.02f) Draw.round(c, x + 2, y + 2, (int) ((BW - 4) * hv), BH - 4, 5, Draw.alpha(ac, 0.18f));
            Draw.textC(c, LABELS[i], x + BW / 2, y + 8, Draw.mix(0xFFCCBBB8, 0xFFFFFFFF, hv));
            Draw.a = 1f;
        }

        // minecraft options link
        String opt = "Minecraft Options";
        int oyl = 178 + oy + LABELS.length * GAP + 2;
        boolean ho = in(mx, my, vw / 2 - Draw.tw(opt) / 2 - 4, oyl - 2, Draw.tw(opt) + 8, 12);
        hoverMc = Anim.step(hoverMc, ho ? 1f : 0f, dt, 1.4f);
        Draw.textC(c, opt, vw / 2, oyl, Draw.mix(0xFF776664, Theme.AMBER, hoverMc));

        // footer
        String mcv = FabricLoader.getInstance().getModContainer("minecraft")
                .map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("?");
        Draw.text(c, "FPS: " + minecraft.getFps(), 6, 6, Theme.GRAY);
        Draw.text(c, "Minecraft " + mcv, 6, vh - 12, Theme.GRAY);
        String cv = "FNEX Client v" + FnexClient.VERSION;
        Draw.text(c, cv, vw - Draw.tw(cv) - 6, vh - 12, Theme.mix2());
        Draw.textC(c, "Right Shift = FNEX Menu", vw / 2, vh - 12, 0xFF665554);
        String up = UpdateChecker.latest;
        if (up != null) {
            String s = "Update available: v" + up;
            int w = Draw.tw(s) + 14;
            Draw.glow(c, vw - w - 8, 8, w, 18, 5, Theme.AMBER, 4, 0.8f);
            Draw.outlined(c, vw - w - 8, 8, w, 18, 5, 0xF0140708, Theme.AMBER);
            Draw.text(c, s, vw - w - 1, 13, Theme.AMBER);
        }
    }

    @Override protected boolean click(int mx, int my, int btn) {
        if (btn != 0 || t < 1.5f) return false;
        int vw = vw(), oy = oy(), bx = (vw - BW) / 2;
        for (int i = 0; i < LABELS.length; i++) {
            if (in(mx, my, bx, 178 + oy + i * GAP, BW, BH)) { Sounds.click(); run(i); return true; }
        }
        String opt = "Minecraft Options";
        int oyl = 178 + oy + LABELS.length * GAP + 2;
        if (in(mx, my, vw / 2 - Draw.tw(opt) / 2 - 4, oyl - 2, Draw.tw(opt) + 8, 12)) {
            Sounds.click(); minecraft.setScreenAndShow(new OptionsScreen(this, minecraft.options)); return true;
        }
        return false;
    }

    private void run(int i) {
        switch (i) {
            case 0 -> minecraft.setScreenAndShow(new SelectWorldScreen(this));
            case 1 -> minecraft.setScreenAndShow(new JoinMultiplayerScreen(this));
            case 2 -> minecraft.setScreenAndShow(new ClickGuiScreen(this));
            case 3 -> minecraft.setScreenAndShow(new CosmeticsScreen(this));
            default -> minecraft.stop();
        }
    }

    @Override public boolean keyPressed(KeyEvent ev) {
        if (ev.key() == Modules.keybinds.menuKey.value) { minecraft.setScreenAndShow(new ClickGuiScreen(this)); return true; }
        return false; // ESC does nothing on the main menu
    }
    @Override public boolean shouldCloseOnEsc() { return false; }
}
