package dev.fnex.module;

import dev.fnex.config.ConfigManager;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/** A client feature ("module"). */
public class Feature {
    public final String name, desc;
    public final Category category;
    public boolean enabled;
    public boolean toggleable = true;
    public boolean hasKey = true;
    public boolean keyDown;
    public float anim;
    public final Setting.Key key = new Setting.Key("Keybind", -1);
    public final List<Setting> settings = new ArrayList<>();
    public Runnable action;

    public Feature(String name, String desc, Category category) { this.name = name; this.desc = desc; this.category = category; }

    protected <T extends Setting> T add(T s) { settings.add(s); return s; }
    protected static Minecraft mc() { return Minecraft.getInstance(); }

    public void setEnabled(boolean b) {
        if (b == enabled) return;
        enabled = b;
        if (b) onEnable(); else onDisable();
        ConfigManager.markDirty();
    }
    public void toggle() { setEnabled(!enabled); }
    public boolean isAction() { return action != null; }
    public boolean holdKey() { return false; }
    public void onEnable() {}
    public void onDisable() {}
    public void onTick(Minecraft mc) {}
}
