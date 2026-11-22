package dev.fnex.gui;

import dev.fnex.module.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public final class Sounds {
    public static void click() {
        if (Modules.sounds == null || !Modules.sounds.enabled) return;
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2f, Modules.sounds.volume.f()));
    }
}
