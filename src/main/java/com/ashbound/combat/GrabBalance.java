package com.ashbound.combat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Grab / throw / slam tuning, loaded from data/ashbound/balance/grab.json. */
public record GrabBalance(
        float staminaCost,
        float range,
        float poiseThreshold,
        float maxWidth,
        int holdTicks,
        int cooldownTicks,
        float throwSpeed,
        float throwLift,
        float throwImpactDamage,
        float throwLandingDamage,
        float throwPoiseDamage,
        float slamDamageMultiplier,
        float slamPoiseDamage) {

    public static final GrabBalance DEFAULT = new GrabBalance(
            20f, 2.5f, 0.3f, 1.4f, 30, 20,
            1.3f, 0.35f, 6f, 3f, 25f,
            2.5f, 40f);

    public static final Codec<GrabBalance> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.optionalFieldOf("stamina_cost", DEFAULT.staminaCost).forGetter(GrabBalance::staminaCost),
            Codec.FLOAT.optionalFieldOf("range", DEFAULT.range).forGetter(GrabBalance::range),
            Codec.FLOAT.optionalFieldOf("poise_threshold", DEFAULT.poiseThreshold).forGetter(GrabBalance::poiseThreshold),
            Codec.FLOAT.optionalFieldOf("max_width", DEFAULT.maxWidth).forGetter(GrabBalance::maxWidth),
            Codec.INT.optionalFieldOf("hold_ticks", DEFAULT.holdTicks).forGetter(GrabBalance::holdTicks),
            Codec.INT.optionalFieldOf("cooldown_ticks", DEFAULT.cooldownTicks).forGetter(GrabBalance::cooldownTicks),
            Codec.FLOAT.optionalFieldOf("throw_speed", DEFAULT.throwSpeed).forGetter(GrabBalance::throwSpeed),
            Codec.FLOAT.optionalFieldOf("throw_lift", DEFAULT.throwLift).forGetter(GrabBalance::throwLift),
            Codec.FLOAT.optionalFieldOf("throw_impact_damage", DEFAULT.throwImpactDamage).forGetter(GrabBalance::throwImpactDamage),
            Codec.FLOAT.optionalFieldOf("throw_landing_damage", DEFAULT.throwLandingDamage).forGetter(GrabBalance::throwLandingDamage),
            Codec.FLOAT.optionalFieldOf("throw_poise_damage", DEFAULT.throwPoiseDamage).forGetter(GrabBalance::throwPoiseDamage),
            Codec.FLOAT.optionalFieldOf("slam_damage_multiplier", DEFAULT.slamDamageMultiplier).forGetter(GrabBalance::slamDamageMultiplier),
            Codec.FLOAT.optionalFieldOf("slam_poise_damage", DEFAULT.slamPoiseDamage).forGetter(GrabBalance::slamPoiseDamage)
    ).apply(i, GrabBalance::new));
}
