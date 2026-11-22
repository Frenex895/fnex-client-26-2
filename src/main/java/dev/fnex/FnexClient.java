package dev.fnex;

import dev.fnex.config.ConfigManager;
import dev.fnex.module.Modules;
import dev.fnex.util.UpdateChecker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FnexClient implements ClientModInitializer {
    public static final String ID = "fnex";
    public static final Logger LOG = LoggerFactory.getLogger("FNEX");
    public static final String VERSION = FabricLoader.getInstance().getModContainer(ID)
            .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("1.0.0");

    @Override public void onInitializeClient() {
        Modules.init();
        ConfigManager.init();
        ClientTickEvents.END_CLIENT_TICK.register(Modules::tick);

        // HUD modules: drawn just before the chat
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(ID, "hud"),
                (graphics, deltaTracker) -> Modules.renderHud(graphics));

        // Custom crosshair replaces the vanilla one while the module is enabled
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (graphics, deltaTracker) -> {
            if (Modules.crosshair != null && Modules.crosshair.enabled) Modules.crosshair.render(graphics);
            else original.extractRenderState(graphics, deltaTracker);
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> { Modules.shutdown(); ConfigManager.save(); });
        UpdateChecker.start();
        LOG.info("FNEX CLIENT {} loaded", VERSION);
    }
}
