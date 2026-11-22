package dev.fnex.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Base class: scaled virtual coordinate space + immediate-mode input hooks. */
public abstract class FnexScreen extends Screen {
    private long last = System.nanoTime();

    protected FnexScreen(String title) { super(Component.literal(title)); }

    protected float uiScale() { return 1f; }
    protected int vw() { return (int) (width / uiScale()); }
    protected int vh() { return (int) (height / uiScale()); }
    protected float dt() {
        long n = System.nanoTime();
        float d = (n - last) / 1e9f;
        last = n;
        return Math.min(d, 0.1f);
    }

    protected abstract void draw(GuiGraphicsExtractor c, int mx, int my);
    protected boolean click(int mx, int my, int btn) { return false; }
    protected boolean drag(int mx, int my, int btn) { return false; }
    protected boolean release(int mx, int my, int btn) { return false; }
    protected boolean scroll(int mx, int my, double amount) { return false; }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        float s = uiScale();
        Draw.a = 1f;
        g.pose().pushMatrix();
        g.pose().scale(s, s);
        draw(g, (int) (mouseX / s), (int) (mouseY / s));
        g.pose().popMatrix();
        Draw.a = 1f;
    }
    @Override public boolean isPauseScreen() { return false; }

    @Override public boolean mouseClicked(MouseButtonEvent e, boolean dbl) { float s = uiScale(); return click((int) (e.x() / s), (int) (e.y() / s), e.button()) || super.mouseClicked(e, dbl); }
    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) { float s = uiScale(); return drag((int) (e.x() / s), (int) (e.y() / s), e.button()) || super.mouseDragged(e, dx, dy); }
    @Override public boolean mouseReleased(MouseButtonEvent e) { float s = uiScale(); return release((int) (e.x() / s), (int) (e.y() / s), e.button()) || super.mouseReleased(e); }
    @Override public boolean mouseScrolled(double x, double y, double h, double v) { float s = uiScale(); return scroll((int) (x / s), (int) (y / s), v) || super.mouseScrolled(x, y, h, v); }

    protected static boolean in(int mx, int my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
}
