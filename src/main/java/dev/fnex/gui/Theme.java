package dev.fnex.gui;

import dev.fnex.module.Modules;

public final class Theme {
    public static final int RED = 0xFFFF1E16, ORANGE = 0xFFFF7800, AMBER = 0xFFFFB000;
    public static final int CARD = 0xFF140708, CARD2 = 0xFF1E0A0B, GRAY = 0xFF9A8A88, WHITE = 0xFFFFFFFF;

    public static int accent() {
        return Modules.interfaceFeature != null ? (Modules.interfaceFeature.color.value | 0xFF000000) : RED;
    }
    public static int mix2() { return accent2(); }
    public static int accent2() { return Draw.mix(accent(), AMBER, 0.6f); }
}
