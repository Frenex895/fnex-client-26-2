package dev.fnex.config;

import com.google.gson.*;
import dev.fnex.FnexClient;
import dev.fnex.module.Modules;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Persists settings to .minecraft/config/fnex/ (config.json + profiles/*.json). */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path dir;
    private static boolean dirty;
    private static long last;
    public static String profile = "default";

    public static void init() {
        dir = FabricLoader.getInstance().getConfigDir().resolve("fnex");
        try { Files.createDirectories(dir.resolve("profiles")); } catch (IOException e) { FnexClient.LOG.error("Cannot create config dir", e); }
        try {
            Path main = dir.resolve("config.json");
            if (Files.exists(main)) {
                JsonObject o = JsonParser.parseString(Files.readString(main)).getAsJsonObject();
                if (o.has("profile")) profile = sanitize(o.get("profile").getAsString());
            }
        } catch (Exception e) { FnexClient.LOG.warn("Bad config.json, using defaults", e); }
        loadProfile(profile);
        Modules.profiles.refresh();
        save();
    }

    private static String sanitize(String n) {
        String s = n.replaceAll("[^a-zA-Z0-9_-]", "");
        return s.isEmpty() ? "default" : s;
    }
    private static Path pf(String n) { return dir.resolve("profiles").resolve(sanitize(n) + ".json"); }

    public static void markDirty() { dirty = true; }
    public static void tick() { if (dirty && System.currentTimeMillis() - last > 1500) save(); }

    public static synchronized void save() {
        if (dir == null) return;
        dirty = false; last = System.currentTimeMillis();
        try {
            Files.writeString(pf(profile), GSON.toJson(Modules.toJson()));
            JsonObject m = new JsonObject(); m.addProperty("profile", profile);
            Files.writeString(dir.resolve("config.json"), GSON.toJson(m));
        } catch (IOException e) { FnexClient.LOG.error("Saving config failed", e); }
    }

    public static void loadProfile(String name) {
        name = sanitize(name);
        Path p = pf(name);
        if (Files.exists(p)) {
            try { Modules.fromJson(JsonParser.parseString(Files.readString(p)).getAsJsonObject()); }
            catch (Exception e) { FnexClient.LOG.warn("Cannot read profile {}", name, e); }
        }
        profile = name;
        dirty = true;
    }

    public static void saveProfileAs(String name) { profile = sanitize(name); save(); }

    public static void deleteProfile(String name) {
        name = sanitize(name);
        if (name.equals("default")) return;
        try { Files.deleteIfExists(pf(name)); } catch (IOException ignored) {}
        if (profile.equals(name)) loadProfile("default");
    }

    public static List<String> profiles() {
        List<String> l = new ArrayList<>();
        try (Stream<Path> s = Files.list(dir.resolve("profiles"))) {
            s.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".json"))
                    .map(n -> n.substring(0, n.length() - 5)).sorted().forEach(l::add);
        } catch (Exception ignored) {}
        if (!l.contains("default")) l.add(0, "default");
        return l;
    }
}
