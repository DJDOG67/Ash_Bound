package com.ashbound.combat;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Combat tuning values, loaded from data/ashbound/balance/combat.json.
 * Every field is optional in the JSON; missing fields fall back to DEFAULT.
 */
public record CombatBalance(
        float maxStamina,
        float regenPerSecond,
        int regenDelayTicks,
        int exhaustedDelayTicks,
        float attackCost,
        float dodgeCost,
        float dodgeSpeed,
        float dodgeHop,
        int dodgeIframeTicks,
        int dodgeCooldownTicks,
        int chainWindowTicks,
        int chainLimit,
        int comboWindowTicks,
        List<Float> comboDamageMultipliers,
        List<Float> comboStaminaCosts,
        float comboFinisherKnockback) {

    public static final CombatBalance DEFAULT = new CombatBalance(
            100f, 20f, 12, 30,
            12f, 20f, 1.0f, 0.1f,
            6, 8, 20, 3,
            16, List.of(1.0f, 1.1f, 1.5f), List.of(12f, 12f, 18f), 0.8f);

    public static final Codec<CombatBalance> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.FLOAT.optionalFieldOf("max_stamina", DEFAULT.maxStamina).forGetter(CombatBalance::maxStamina),
            Codec.FLOAT.optionalFieldOf("regen_per_second", DEFAULT.regenPerSecond).forGetter(CombatBalance::regenPerSecond),
            Codec.INT.optionalFieldOf("regen_delay_ticks", DEFAULT.regenDelayTicks).forGetter(CombatBalance::regenDelayTicks),
            Codec.INT.optionalFieldOf("exhausted_delay_ticks", DEFAULT.exhaustedDelayTicks).forGetter(CombatBalance::exhaustedDelayTicks),
            Codec.FLOAT.optionalFieldOf("attack_cost", DEFAULT.attackCost).forGetter(CombatBalance::attackCost),
            Codec.FLOAT.optionalFieldOf("dodge_cost", DEFAULT.dodgeCost).forGetter(CombatBalance::dodgeCost),
            Codec.FLOAT.optionalFieldOf("dodge_speed", DEFAULT.dodgeSpeed).forGetter(CombatBalance::dodgeSpeed),
            Codec.FLOAT.optionalFieldOf("dodge_hop", DEFAULT.dodgeHop).forGetter(CombatBalance::dodgeHop),
            Codec.INT.optionalFieldOf("dodge_iframe_ticks", DEFAULT.dodgeIframeTicks).forGetter(CombatBalance::dodgeIframeTicks),
            Codec.INT.optionalFieldOf("dodge_cooldown_ticks", DEFAULT.dodgeCooldownTicks).forGetter(CombatBalance::dodgeCooldownTicks),
            Codec.INT.optionalFieldOf("chain_window_ticks", DEFAULT.chainWindowTicks).forGetter(CombatBalance::chainWindowTicks),
            Codec.INT.optionalFieldOf("chain_limit", DEFAULT.chainLimit).forGetter(CombatBalance::chainLimit),
            Codec.INT.optionalFieldOf("combo_window_ticks", DEFAULT.comboWindowTicks).forGetter(CombatBalance::comboWindowTicks),
            Codec.FLOAT.listOf().optionalFieldOf("combo_damage_multipliers", DEFAULT.comboDamageMultipliers).forGetter(CombatBalance::comboDamageMultipliers),
            Codec.FLOAT.listOf().optionalFieldOf("combo_stamina_costs", DEFAULT.comboStaminaCosts).forGetter(CombatBalance::comboStaminaCosts),
            Codec.FLOAT.optionalFieldOf("combo_finisher_knockback", DEFAULT.comboFinisherKnockback).forGetter(CombatBalance::comboFinisherKnockback)
    ).apply(i, CombatBalance::new));

    /** Number of hits in a combo (shortest of the two lists, at least 1). */
    public int comboLength() {
        return Math.max(1, Math.min(comboDamageMultipliers.size(), comboStaminaCosts.size()));
    }
}
