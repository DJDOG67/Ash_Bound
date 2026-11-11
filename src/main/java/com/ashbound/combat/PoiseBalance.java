package com.ashbound.combat;

import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Poise / stagger / parry tuning, loaded from data/ashbound/balance/poise.json. */
public record PoiseBalance(
        float defaultMax,
        float playerMax,
        Map<String, Float> entityMax,
        float damageToPoise,
        float finisherPoiseMultiplier,
        int regenDelayTicks,
        float regenPerSecond,
        int staggerTicks,
        float riposteDamageMultiplier,
        int parryWindowTicks,
        int parryCooldownTicks,
        float parryStaminaCost,
        float parryPoiseDamageRatio) {

    public static final PoiseBalance DEFAULT = new PoiseBalance(
            30f, 50f, Map.of("minecraft:iron_golem", 150f, "minecraft:ravager", 200f, "minecraft:warden", 400f),
            1.0f, 2.0f, 60, 15f, 40, 3.0f,
            4, 10, 10f, 0.6f);

    public static final Codec<PoiseBalance> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.optionalFieldOf("default_max", DEFAULT.defaultMax).forGetter(PoiseBalance::defaultMax),
            Codec.FLOAT.optionalFieldOf("player_max", DEFAULT.playerMax).forGetter(PoiseBalance::playerMax),
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("entity_max", DEFAULT.entityMax).forGetter(PoiseBalance::entityMax),
            Codec.FLOAT.optionalFieldOf("damage_to_poise", DEFAULT.damageToPoise).forGetter(PoiseBalance::damageToPoise),
            Codec.FLOAT.optionalFieldOf("finisher_poise_multiplier", DEFAULT.finisherPoiseMultiplier).forGetter(PoiseBalance::finisherPoiseMultiplier),
            Codec.INT.optionalFieldOf("regen_delay_ticks", DEFAULT.regenDelayTicks).forGetter(PoiseBalance::regenDelayTicks),
            Codec.FLOAT.optionalFieldOf("regen_per_second", DEFAULT.regenPerSecond).forGetter(PoiseBalance::regenPerSecond),
            Codec.INT.optionalFieldOf("stagger_ticks", DEFAULT.staggerTicks).forGetter(PoiseBalance::staggerTicks),
            Codec.FLOAT.optionalFieldOf("riposte_damage_multiplier", DEFAULT.riposteDamageMultiplier).forGetter(PoiseBalance::riposteDamageMultiplier),
            Codec.INT.optionalFieldOf("parry_window_ticks", DEFAULT.parryWindowTicks).forGetter(PoiseBalance::parryWindowTicks),
            Codec.INT.optionalFieldOf("parry_cooldown_ticks", DEFAULT.parryCooldownTicks).forGetter(PoiseBalance::parryCooldownTicks),
            Codec.FLOAT.optionalFieldOf("parry_stamina_cost", DEFAULT.parryStaminaCost).forGetter(PoiseBalance::parryStaminaCost),
            Codec.FLOAT.optionalFieldOf("parry_poise_damage_ratio", DEFAULT.parryPoiseDamageRatio).forGetter(PoiseBalance::parryPoiseDamageRatio)
    ).apply(i, PoiseBalance::new));
}
