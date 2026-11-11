package com.ashbound.combat;

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
        ComboBalance combo) {

    public static final CombatBalance DEFAULT = new CombatBalance(
            100f, 20f, 12, 30,
            12f, 20f, 1.0f, 0.1f,
            6, 8, 20, 3,
            ComboBalance.DEFAULT);

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
            ComboBalance.CODEC.optionalFieldOf("combo", DEFAULT.combo).forGetter(CombatBalance::combo)
    ).apply(i, CombatBalance::new));
}
