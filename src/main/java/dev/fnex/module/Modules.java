package dev.fnex.module;

import com.google.gson.JsonObject;
import dev.fnex.FnexClient;
import dev.fnex.config.ConfigManager;
import dev.fnex.gui.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class Modules {
    public static final List<Feature> ALL = new ArrayList<>();

    public static InterfaceFeature interfaceFeature;
    public static AnimationsFeature animations;
    public static SoundsFeature sounds;
    public static KeybindsFeature keybinds;
    public static ProfilesFeature profiles;
    public static CrosshairFeature crosshair;
    public static CosmeticsFeature cosmetics;
    public static ZoomFeature zoom;

    private static boolean menuDown;

    private static <T extends Feature> T reg(T f) { ALL.add(f); return f; }
    private static Minecraft mc() { return Minecraft.getInstance(); }

    public static void init() {
        // VISUALS
        reg(new Watermark());
        reg(new Fullbright());
        crosshair = reg(new CrosshairFeature());
        zoom = reg(new ZoomFeature());
        cosmetics = reg(new CosmeticsFeature());
        reg(new HudEditorEntry());
        // PLAYER
        reg(new ToggleSprint());
        reg(new AutoTool());
        reg(new ItemCounter());
        reg(new Keystrokes());
        reg(new CpsCounter());
        reg(new FpsCounter());
        reg(new PingDisplay());
        // WORLD
        reg(new Coordinates());
        reg(new DirectionHud());
        reg(new TimeDisplay());
        reg(new WeatherDisplay());
        // SETTINGS
        interfaceFeature = reg(new InterfaceFeature());
        animations = reg(new AnimationsFeature());
        sounds = reg(new SoundsFeature());
        keybinds = reg(new KeybindsFeature());
        profiles = reg(new ProfilesFeature());
    }

    // ------------------------------------------------------------------ loop
    public static void tick(Minecraft mc) {
        if (mc.screen instanceof TitleScreen) mc.setScreenAndShow(new FnexTitleScreen());
        tickKeys(mc);
        if (mc.player != null && mc.level != null)
            for (Feature f : ALL) if (f.enabled) f.onTick(mc);
        ConfigManager.tick();
    }

    private static void tickKeys(Minecraft mc) {
        var h = mc.getWindow();
        boolean free = mc.screen == null;
        int mk = keybinds.menuKey.value;
        boolean md = mk >= 0 && InputConstants.isKeyDown(h, mk);
        if (free && md && !menuDown) mc.setScreenAndShow(new ClickGuiScreen(null));
        menuDown = md;
        for (Feature f : ALL) {
            int k = f.key.value;
            boolean d = k >= 0 && InputConstants.isKeyDown(h, k);
            if (f.holdKey()) { f.keyDown = d; continue; }
            if (free && d && !f.keyDown && f.hasKey) {
                if (f.isAction()) f.action.run(); else if (f.toggleable) f.toggle();
            }
            f.keyDown = d;
        }
    }

    public static void renderHud(GuiGraphicsExtractor ctx) {
        Minecraft mc = mc();
        if (mc.options.hideGui || mc.player == null) return;
        Cps.update();
        if (mc.screen instanceof HudEditorScreen) return;
        Draw.a = 1f;
        for (Feature f : ALL) if (f instanceof HudFeature h) h.renderHud(ctx);
    }

    public static void shutdown() { if (zoom != null) zoom.restore(); }

    // ------------------------------------------------------------------ persistence
    public static JsonObject toJson() {
        JsonObject root = new JsonObject();
        for (Feature f : ALL) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", f.enabled);
            if (f.hasKey) o.addProperty("key", f.key.value);
            if (f instanceof HudFeature h) { o.addProperty("x", h.hudX); o.addProperty("y", h.hudY); }
            JsonObject s = new JsonObject();
            for (Setting st : f.settings) if (st.persist) s.add(st.name, st.save());
            o.add("settings", s);
            root.add(f.name, o);
        }
        return root;
    }

    public static void fromJson(JsonObject root) {
        for (Feature f : ALL) {
            if (!root.has(f.name) || !root.get(f.name).isJsonObject()) continue;
            JsonObject o = root.getAsJsonObject(f.name);
            try {
                if (o.has("enabled") && f.toggleable) f.setEnabled(o.get("enabled").getAsBoolean());
                if (o.has("key")) f.key.value = o.get("key").getAsInt();
                if (f instanceof HudFeature h) {
                    if (o.has("x")) h.hudX = o.get("x").getAsInt();
                    if (o.has("y")) h.hudY = o.get("y").getAsInt();
                }
                if (o.has("settings")) {
                    JsonObject s = o.getAsJsonObject("settings");
                    for (Setting st : f.settings) if (st.persist && s.has(st.name)) st.load(s.get(st.name));
                }
            } catch (Exception e) { FnexClient.LOG.warn("Bad config entry for {}", f.name, e); }
        }
    }

    // ================================================================== VISUALS
    public static final class Watermark extends HudFeature {
        public Watermark() { super("FNEX HUD", "FNEX watermark with logo and version", Category.VISUALS, 4, 4); enabled = true; }
        @Override protected String label() { return "FNEX"; }
        @Override protected String value() { return FnexClient.VERSION; }
        @Override public void draw(GuiGraphicsExtractor c, int x, int y) {
            String t = "FNEX CLIENT", v = " v" + FnexClient.VERSION;
            w = Draw.tw(t + v) + 36; h = 24;
            Draw.round(c, x, y, w, h, 5, 0xB00A0405);
            Draw.round(c, x, y + 3, 2, h - 6, 1, Theme.accent());
            Draw.logo(c, x + 6, y + 3, 18);
            Draw.text(c, t, x + 28, y + 8, Theme.accent());
            Draw.text(c, v, x + 28 + Draw.tw(t), y + 8, Theme.GRAY);
        }
    }

    public static final class Fullbright extends Feature {
        private boolean applied;
        public Fullbright() { super("Fullbright", "Makes the world fully bright", Category.VISUALS); }
        @Override public void onTick(Minecraft mc) {
            if (!mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
                mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, MobEffectInstance.INFINITE_DURATION, 0, false, false, false));
                applied = true;
            }
        }
        @Override public void onDisable() {
            Minecraft mc = mc();
            if (applied && mc.player != null) mc.player.removeEffect(MobEffects.NIGHT_VISION);
            applied = false;
        }
    }

    public static final class CrosshairFeature extends Feature {
        public final Setting.Mode style = add(new Setting.Mode("Style", 0, "Cross", "Plus", "T-Shape", "Dot", "Circle", "Square"));
        public final Setting.Color color = add(new Setting.Color("Crosshair Color", 0xFFFF1E16, true));
        public final Setting.Num size = add(new Setting.Num("Crosshair Size", 6, 1, 20, 1));
        public final Setting.Num thick = add(new Setting.Num("Crosshair Thickness", 2, 1, 6, 1));
        public final Setting.Num gap = add(new Setting.Num("Line Gap", 3, 0, 12, 1));
        public final Setting.Bool dot = add(new Setting.Bool("Center Dot", false));
        public final Setting.Bool outline = add(new Setting.Bool("Outline", true));

        public CrosshairFeature() { super("Custom Crosshair", "Fully customizable crosshair", Category.VISUALS); enabled = true; }

        private void bar(GuiGraphicsExtractor c, int x, int y, int w, int h, int col) {
            if (outline.value) Draw.rect(c, x - 1, y - 1, w + 2, h + 2, col & 0xFF000000);
            Draw.rect(c, x, y, w, h, col);
        }

        /** Drawn in place of the vanilla crosshair (see FnexClient: HudElementRegistry.replaceElement). */
        public void render(GuiGraphicsExtractor c) {
            if (!mc().options.getCameraType().isFirstPerson()) return;
            int cx = c.guiWidth() / 2, cy = c.guiHeight() / 2;
            int col = color.value, s = size.i(), t = thick.i(), g = gap.i(), half = t / 2;
            String st = style.get();
            switch (st) {
                case "Cross" -> {
                    bar(c, cx - half, cy - g - s, t, s, col); bar(c, cx - half, cy + g, t, s, col);
                    bar(c, cx - g - s, cy - half, s, t, col); bar(c, cx + g, cy - half, s, t, col);
                }
                case "Plus" -> {
                    bar(c, cx - s, cy - half, 2 * s, t, col);
                    bar(c, cx - half, cy - s, t, s - half, col);
                    bar(c, cx - half, cy - half + t, t, s - (t - half), col);
                }
                case "T-Shape" -> {
                    bar(c, cx - half, cy + g, t, s, col);
                    bar(c, cx - g - s, cy - half, s, t, col); bar(c, cx + g, cy - half, s, t, col);
                }
                case "Dot" -> { int d = Math.max(2, s / 2); bar(c, cx - d / 2, cy - d / 2, d, d, col); }
                case "Circle" -> ring(c, cx, cy, s + 2, t, col);
                case "Square" -> {
                    bar(c, cx - s, cy - s, 2 * s, t, col); bar(c, cx - s, cy + s - t, 2 * s, t, col);
                    bar(c, cx - s, cy - s + t, t, 2 * s - 2 * t, col); bar(c, cx + s - t, cy - s + t, t, 2 * s - 2 * t, col);
                }
                default -> {}
            }
            if (dot.value && !st.equals("Dot")) bar(c, cx - half, cy - half, t, t, col);
        }

        private void ring(GuiGraphicsExtractor c, int cx, int cy, int r, int t, int col) {
            int ir = Math.max(0, r - t);
            for (int dy = -r; dy <= r; dy++) {
                int outer = (int) Math.sqrt(r * r - dy * dy);
                int inner = Math.abs(dy) < ir ? (int) Math.sqrt(ir * ir - dy * dy) : 0;
                if (inner == 0) Draw.rect(c, cx - outer, cy + dy, outer * 2, 1, col);
                else { Draw.rect(c, cx - outer, cy + dy, outer - inner, 1, col); Draw.rect(c, cx + inner, cy + dy, outer - inner, 1, col); }
            }
        }
    }

    public static final class ZoomFeature extends Feature {
        public final Setting.Num zoomFov = add(new Setting.Num("Zoom FOV", 30, 30, 70, 1));
        private boolean zooming; private int saved;
        public ZoomFeature() { super("Zoom", "Hold the key to zoom in (default: C)", Category.VISUALS); key.value = GLFW.GLFW_KEY_C; enabled = true; }
        @Override public boolean holdKey() { return true; }
        @Override public void onTick(Minecraft mc) {
            boolean held = mc.screen == null && key.value >= 0 && InputConstants.isKeyDown(mc.getWindow(), key.value);
            if (held && !zooming) { saved = mc.options.fov().get(); zooming = true; }
            if (zooming) {
                if (held) mc.options.fov().set(zoomFov.i());
                else { mc.options.fov().set(saved); zooming = false; }
            }
        }
        public void restore() { if (zooming) { mc().options.fov().set(saved); zooming = false; } }
        @Override public void onDisable() { restore(); }
    }

    public static final class CosmeticsFeature extends Feature {
        public final Setting.Mode trail = add(new Setting.Mode("Particle Trail", 0, "Flame", "Soul Fire", "Hearts", "Crit", "End Rod"));
        private int ticks;
        public CosmeticsFeature() { super("Cosmetics", "Particle trail that follows you while moving", Category.VISUALS); }
        private ParticleOptions effect() {
            return switch (trail.get()) {
                case "Soul Fire" -> ParticleTypes.SOUL_FIRE_FLAME;
                case "Hearts" -> ParticleTypes.HEART;
                case "Crit" -> ParticleTypes.CRIT;
                case "End Rod" -> ParticleTypes.END_ROD;
                default -> ParticleTypes.FLAME;
            };
        }
        @Override public void onTick(Minecraft mc) {
            if (++ticks % 2 != 0 || mc.player.getDeltaMovement().horizontalDistanceSqr() < 0.002) return;
            ThreadLocalRandom r = ThreadLocalRandom.current();
            mc.level.addParticle(effect(), mc.player.getX() + (r.nextDouble() - 0.5) * 0.5, mc.player.getY() + 0.1,
                    mc.player.getZ() + (r.nextDouble() - 0.5) * 0.5, 0, 0.02, 0);
        }
    }

    public static final class HudEditorEntry extends Feature {
        public HudEditorEntry() {
            super("HUD Editor", "Drag your HUD elements to where you want them", Category.VISUALS);
            hasKey = true;
            action = () -> mc().setScreenAndShow(new HudEditorScreen(mc().screen));
        }
    }

    // ================================================================== PLAYER
    public static final class ToggleSprint extends Feature {
        public ToggleSprint() { super("Toggle Sprint", "Sprints automatically while you walk forward", Category.PLAYER); }
        @Override public void onTick(Minecraft mc) {
            if (mc.screen == null) mc.options.keySprint.setDown(mc.options.keyUp.isDown());
        }
        @Override public void onDisable() { mc().options.keySprint.setDown(false); }
    }

    public static final class AutoTool extends Feature {
        public AutoTool() { super("Auto Tool", "Selects the fastest hotbar tool for the block you mine", Category.PLAYER); }
        @Override public void onTick(Minecraft mc) {
            if (mc.screen != null || !mc.options.keyAttack.isDown()) return;
            if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
            BlockState st = mc.level.getBlockState(hit.getBlockPos());
            var inv = mc.player.getInventory();
            int best = inv.getSelectedSlot();
            float bestSpeed = inv.getItem(best).getDestroySpeed(st);
            for (int i = 0; i < 9; i++) {
                float sp = inv.getItem(i).getDestroySpeed(st);
                if (sp > bestSpeed + 0.01f) { best = i; bestSpeed = sp; }
            }
            inv.setSelectedSlot(best);
        }
    }

    public static final class ItemCounter extends HudFeature {
        public ItemCounter() { super("Item Counter", "Counts the item in your hand across your inventory", Category.PLAYER, 4, 166); }
        @Override protected String label() { return "Items"; }
        @Override protected String value() {
            Minecraft mc = mc();
            if (mc.player == null) return "-";
            ItemStack held = mc.player.getMainHandItem();
            if (held.isEmpty()) return "-";
            int n = 0;
            var inv = mc.player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) { ItemStack s = inv.getItem(i); if (s.is(held.getItem())) n += s.getCount(); }
            return String.valueOf(n);
        }
    }

    public static final class Keystrokes extends HudFeature {
        public Keystrokes() { super("Keystrokes", "Shows WASD and mouse buttons", Category.PLAYER, 4, 190); enabled = true; }
        @Override protected String label() { return "Keys"; }
        @Override protected String value() { return ""; }
        private void key(GuiGraphicsExtractor c, int x, int y, int bw, int bh, String t, boolean down) {
            int ac = Theme.accent();
            Draw.round(c, x, y, bw, bh, 3, down ? Draw.alpha(ac, 0.85f) : 0xB00A0405);
            Draw.frame(c, x, y, bw, bh, Draw.alpha(ac, down ? 0.9f : 0.3f));
            Draw.textC(c, t, x + bw / 2, y + (bh - 8) / 2, 0xFFFFFFFF);
        }
        @Override public void draw(GuiGraphicsExtractor c, int x, int y) {
            var o = mc().options;
            w = 56; h = 56;
            key(c, x + 19, y, 18, 18, "W", o.keyUp.isDown());
            key(c, x, y + 20, 18, 18, "A", o.keyLeft.isDown());
            key(c, x + 19, y + 20, 18, 18, "S", o.keyDown.isDown());
            key(c, x + 38, y + 20, 18, 18, "D", o.keyRight.isDown());
            key(c, x, y + 40, 27, 16, "L " + Cps.left(), Cps.leftDown());
            key(c, x + 29, y + 40, 27, 16, "R " + Cps.right(), Cps.rightDown());
        }
    }

    public static final class CpsCounter extends HudFeature {
        public CpsCounter() { super("CPS Counter", "Clicks per second", Category.PLAYER, 4, 46); enabled = true; }
        @Override protected String label() { return "CPS"; }
        @Override protected String value() { return Cps.left() + " | " + Cps.right(); }
    }

    public static final class FpsCounter extends HudFeature {
        public FpsCounter() { super("FPS Counter", "Frames per second", Category.PLAYER, 4, 26); enabled = true; }
        @Override protected String label() { return "FPS"; }
        @Override protected String value() { return String.valueOf(mc().getFps()); }
    }

    public static final class PingDisplay extends HudFeature {
        public PingDisplay() { super("Ping Display", "Your latency to the server", Category.PLAYER, 4, 66); enabled = true; }
        @Override protected String label() { return "Ping"; }
        @Override protected String value() {
            Minecraft mc = mc();
            if (mc.getConnection() == null || mc.player == null) return "-";
            var e = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            return (e == null ? 0 : e.getLatency()) + " ms";
        }
    }

    // ================================================================== WORLD
    public static final class Coordinates extends HudFeature {
        public Coordinates() { super("Coordinates", "Your XYZ position", Category.WORLD, 4, 86); enabled = true; }
        @Override protected String label() { return "XYZ"; }
        @Override protected String value() {
            var p = mc().player;
            return p == null ? "-" : String.format(Locale.ROOT, "%.1f / %.1f / %.1f", p.getX(), p.getY(), p.getZ());
        }
    }

    public static final class DirectionHud extends HudFeature {
        public DirectionHud() { super("Direction HUD", "Facing direction and yaw", Category.WORLD, 4, 106); }
        @Override protected String label() { return "Facing"; }
        @Override protected String value() {
            var p = mc().player;
            if (p == null) return "-";
            String d = p.getDirection().getName();
            return Character.toUpperCase(d.charAt(0)) + d.substring(1) + " (" + Math.round(Mth.wrapDegrees(p.getYRot())) + "\u00B0)";
        }
    }

    public static final class TimeDisplay extends HudFeature {
        public final Setting.Mode clock = add(new Setting.Mode("Clock", 0, "World", "Real"));
        public TimeDisplay() { super("Time Display", "World time or real clock", Category.WORLD, 4, 126); }
        @Override protected String label() { return clock.get().equals("World") ? "Time" : "Clock"; }
        @Override protected String value() {
            Minecraft mc = mc();
            if (clock.get().equals("Real")) return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            if (mc.level == null) return "-";
            long t = mc.level.getGameTime() % 24000L; // game-time based clock
            return String.format(Locale.ROOT, "%02d:%02d", (t / 1000 + 6) % 24, (t % 1000) * 60 / 1000);
        }
    }

    public static final class WeatherDisplay extends HudFeature {
        public WeatherDisplay() { super("Weather Display", "Current weather", Category.WORLD, 4, 146); }
        @Override protected String label() { return "Weather"; }
        @Override protected String value() {
            var w = mc().level;
            if (w == null) return "-";
            return w.isThundering() ? "Thunder" : w.isRaining() ? "Rain" : "Clear";
        }
    }

    // ================================================================== SETTINGS
    public static final class InterfaceFeature extends Feature {
        public final Setting.Num guiScale = add(new Setting.Num("GUI Scale", 1.0, 0.6, 1.6, 0.05));
        public final Setting.Color color = add(new Setting.Color("Interface Color", 0xFFFF1E16, false));
        public InterfaceFeature() { super("Interface", "GUI scale and interface color", Category.SETTINGS); toggleable = false; hasKey = false; enabled = true; }
    }

    public static final class AnimationsFeature extends Feature {
        public final Setting.Num speed = add(new Setting.Num("Animation Speed", 1.0, 0.5, 2.5, 0.1));
        public AnimationsFeature() { super("Animations", "Menu and toggle animations", Category.SETTINGS); hasKey = false; enabled = true; }
    }

    public static final class SoundsFeature extends Feature {
        public final Setting.Num volume = add(new Setting.Num("UI Volume", 0.6, 0.0, 1.0, 0.05));
        public SoundsFeature() { super("Sounds", "Click sounds in the FNEX menus", Category.SETTINGS); hasKey = false; enabled = true; }
    }

    public static final class KeybindsFeature extends Feature {
        public final Setting.Key menuKey = add(new Setting.Key("Open Menu", GLFW.GLFW_KEY_RIGHT_SHIFT));
        public KeybindsFeature() {
            super("Keybinds", "Menu key. Module keys are set inside each module.", Category.SETTINGS);
            toggleable = false; hasKey = false; enabled = true;
            add(new Setting.Action("Reset all module keybinds", () -> {
                for (Feature f : ALL) f.key.value = (f instanceof ZoomFeature) ? GLFW.GLFW_KEY_C : -1;
                ConfigManager.markDirty();
            }));
        }
    }

    public static final class ProfilesFeature extends Feature {
        public final Setting.Mode profile = add(new Setting.Mode("Profile", 0, "default"));
        public ProfilesFeature() {
            super("Profiles", "Save and load complete setting profiles", Category.SETTINGS);
            toggleable = false; hasKey = false; enabled = true;
            profile.persist = false;
            add(new Setting.Action("Load selected profile", () -> { ConfigManager.loadProfile(profile.get()); refresh(); }));
            add(new Setting.Action("Save to selected profile", () -> { ConfigManager.saveProfileAs(profile.get()); refresh(); }));
            add(new Setting.Action("Create new profile", () -> {
                int i = 1; List<String> have = ConfigManager.profiles();
                while (have.contains("profile-" + i)) i++;
                ConfigManager.saveProfileAs("profile-" + i); refresh();
            }));
            add(new Setting.Action("Delete selected profile", () -> { ConfigManager.deleteProfile(profile.get()); refresh(); }));
        }
        public void refresh() {
            profile.options = new ArrayList<>(ConfigManager.profiles());
            profile.index = Math.max(0, profile.options.indexOf(ConfigManager.profile));
        }
    }
}
