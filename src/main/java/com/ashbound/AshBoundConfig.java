package com.ashbound;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Global switches for each Ash_Bound system.
 *
 * Balance numbers do NOT belong here: they live in data pack JSON under
 * src/main/resources/data/ashbound/. This file only turns whole systems on/off.
 */
public final class AshBoundConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.comment("Turn whole Ash_Bound systems on or off.").push("systems");
    }

    public static final ModConfigSpec.BooleanValue ENABLE_COMBAT = BUILDER
            .comment("Stamina, dodge, parry, poise, lock-on.")
            .define("combat", true);

    public static final ModConfigSpec.BooleanValue ENABLE_MACHINERY = BUILDER
            .comment("Ember Engineering: rotational power and machines.")
            .define("machinery", true);

    public static final ModConfigSpec.BooleanValue ENABLE_FARMING = BUILDER
            .comment("Crops with quality and fertility, cooking buffs, livestock.")
            .define("farming", true);

    public static final ModConfigSpec.BooleanValue ENABLE_FACTIONS = BUILDER
            .comment("Faction reputation, territories and mob ecosystem.")
            .define("factions", true);

    static {
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    private AshBoundConfig() {
    }
}
