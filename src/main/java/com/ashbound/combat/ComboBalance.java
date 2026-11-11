package com.ashbound.combat;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Weapon combo tuning, the "combo" block of combat.json.
 *
 * The combo window scales with the weapon: window = time to fully charge the
 * vanilla attack meter + windowGraceTicks. Only hits landed with the meter at
 * least minCharge full advance the combo, so mashing resets it.
 */
public record ComboBalance(
        int windowGraceTicks,
        float minCharge,
        List<Float> damageMultipliers,
        List<Float> staminaCosts,
        float finisherKnockback) {

    public static final ComboBalance DEFAULT = new ComboBalance(
            10, 0.9f, List.of(1.0f, 1.1f, 1.5f), List.of(12f, 12f, 18f), 0.8f);

    public static final Codec<ComboBalance> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("window_grace_ticks", DEFAULT.windowGraceTicks).forGetter(ComboBalance::windowGraceTicks),
            Codec.FLOAT.optionalFieldOf("min_charge", DEFAULT.minCharge).forGetter(ComboBalance::minCharge),
            Codec.FLOAT.listOf().optionalFieldOf("damage_multipliers", DEFAULT.damageMultipliers).forGetter(ComboBalance::damageMultipliers),
            Codec.FLOAT.listOf().optionalFieldOf("stamina_costs", DEFAULT.staminaCosts).forGetter(ComboBalance::staminaCosts),
            Codec.FLOAT.optionalFieldOf("finisher_knockback", DEFAULT.finisherKnockback).forGetter(ComboBalance::finisherKnockback)
    ).apply(i, ComboBalance::new));

    /** Number of hits in a combo (shortest of the two lists, at least 1). */
    public int length() {
        return Math.max(1, Math.min(damageMultipliers.size(), staminaCosts.size()));
    }
}
