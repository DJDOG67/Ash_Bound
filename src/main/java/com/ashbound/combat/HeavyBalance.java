package com.ashbound.combat;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Charged heavy attack, the "heavy" block of combat.json. Index of the lists = focus pips spent (0..3). */
public record HeavyBalance(
        float staminaCost,
        int minChargeTicks,
        int fullChargeTicks,
        float partialChargeDamage,
        int cooldownTicks,
        List<Float> damageByFocus,
        List<Float> poiseByFocus,
        List<Float> knockbackByFocus) {

    public static final HeavyBalance DEFAULT = new HeavyBalance(
            25f, 8, 20, 0.7f, 16,
            List.of(1.6f, 2.2f, 3.0f, 4.2f),
            List.of(2.0f, 3.0f, 4.0f, 6.0f),
            List.of(0.6f, 0.8f, 1.1f, 1.6f));

    public static final Codec<HeavyBalance> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.optionalFieldOf("stamina_cost", DEFAULT.staminaCost).forGetter(HeavyBalance::staminaCost),
            Codec.INT.optionalFieldOf("min_charge_ticks", DEFAULT.minChargeTicks).forGetter(HeavyBalance::minChargeTicks),
            Codec.INT.optionalFieldOf("full_charge_ticks", DEFAULT.fullChargeTicks).forGetter(HeavyBalance::fullChargeTicks),
            Codec.FLOAT.optionalFieldOf("partial_charge_damage", DEFAULT.partialChargeDamage).forGetter(HeavyBalance::partialChargeDamage),
            Codec.INT.optionalFieldOf("cooldown_ticks", DEFAULT.cooldownTicks).forGetter(HeavyBalance::cooldownTicks),
            Codec.FLOAT.listOf().optionalFieldOf("damage_by_focus", DEFAULT.damageByFocus).forGetter(HeavyBalance::damageByFocus),
            Codec.FLOAT.listOf().optionalFieldOf("poise_by_focus", DEFAULT.poiseByFocus).forGetter(HeavyBalance::poiseByFocus),
            Codec.FLOAT.listOf().optionalFieldOf("knockback_by_focus", DEFAULT.knockbackByFocus).forGetter(HeavyBalance::knockbackByFocus)
    ).apply(i, HeavyBalance::new));

    /** Value for the given number of pips, clamped to the last entry. */
    public static float pick(List<Float> list, int pips) {
        if (list.isEmpty()) {
            return 1f;
        }
        return list.get(Math.min(Math.max(pips, 0), list.size() - 1));
    }
}
