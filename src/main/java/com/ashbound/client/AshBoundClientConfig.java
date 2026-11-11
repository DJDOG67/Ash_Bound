package com.ashbound.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Per-player feel settings (camera, HUD). Editable in game:
 * Mods > Ash_Bound > Config > Client. Saved in run/config/ashbound-client.toml.
 * Gameplay balance does NOT go here (that is data/ashbound/balance/*.json).
 */
public final class AshBoundClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.comment("Lock-on camera").push("lockon");
    }

    public static final ModConfigSpec.DoubleValue LOCKON_RANGE = BUILDER
            .comment("Max distance (blocks) to acquire a lock-on target. Default 16.")
            .defineInRange("range", 16.0, 6.0, 40.0);

    public static final ModConfigSpec.DoubleValue LOCKON_TURN_SPEED = BUILDER
            .comment("How fast the camera turns to the target. 0.1 = lazy, 1.0 = snap. Default 0.35.")
            .defineInRange("turnSpeed", 0.35, 0.05, 1.0);

    public static final ModConfigSpec.IntValue LOCKON_ANGLE = BUILDER
            .comment("Only targets within this angle (degrees) of the crosshair are picked. Default 50.")
            .defineInRange("angle", 50, 15, 90);

    public static final ModConfigSpec.BooleanValue LOCKON_SHOW_TARGET_BAR = BUILDER
            .comment("Show the target name and health bar at the top of the screen.")
            .define("showTargetBar", true);

    static {
        BUILDER.pop();
        BUILDER.comment("Hit feel and combo effects").push("effects");
    }

    public static final ModConfigSpec.BooleanValue COMBO_HUD = BUILDER
            .comment("Show combo pips under the crosshair.")
            .define("comboHud", true);

    public static final ModConfigSpec.DoubleValue SCREEN_SHAKE = BUILDER
            .comment("Camera shake strength on hits. 0 = off, 1 = default, 2 = heavy.")
            .defineInRange("screenShake", 1.0, 0.0, 2.0);

    public static final ModConfigSpec.BooleanValue HIT_PARTICLES = BUILDER
            .comment("Spark / ember particles on combo hits.")
            .define("hitParticles", true);

    public static final ModConfigSpec.BooleanValue FINISHER_FLASH = BUILDER
            .comment("Ember flash at the screen edges on a combo finisher.")
            .define("finisherFlash", true);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private AshBoundClientConfig() {
    }
}
