package dev.fnex.gui;

import dev.fnex.module.Modules;

public final class Anim {
    public static float step(float cur, float target, float dt, float mul) {
        if (Modules.animations != null && !Modules.animations.enabled) return target;
        float speed = Modules.animations != null ? Modules.animations.speed.f() : 1f;
        float k = Math.min(1f, dt * 10f * speed * mul);
        float n = cur + (target - cur) * k;
        return Math.abs(target - n) < 0.004f ? target : n;
    }
    public static float ease(float x) { x = Math.max(0, Math.min(1, x)); return 1f - (1f - x) * (1f - x) * (1f - x); }
}
