package com.ashbound.combat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Focus gauge (집중), the "focus" block of combat.json. Measured in pips: 1.0 = one full pip. */
public record FocusBalance(
        int maxPips,
        float perComboHit,
        float perFinisher,
        float perParry,
        float perRiposte,
        int decayDelayTicks,
        float decayPerSecond) {

    public static final FocusBalance DEFAULT = new FocusBalance(3, 0.25f, 0.5f, 1.0f, 0.5f, 200, 0.2f);

    public static final Codec<FocusBalance> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("max_pips", DEFAULT.maxPips).forGetter(FocusBalance::maxPips),
            Codec.FLOAT.optionalFieldOf("per_combo_hit", DEFAULT.perComboHit).forGetter(FocusBalance::perComboHit),
            Codec.FLOAT.optionalFieldOf("per_finisher", DEFAULT.perFinisher).forGetter(FocusBalance::perFinisher),
            Codec.FLOAT.optionalFieldOf("per_parry", DEFAULT.perParry).forGetter(FocusBalance::perParry),
            Codec.FLOAT.optionalFieldOf("per_riposte", DEFAULT.perRiposte).forGetter(FocusBalance::perRiposte),
            Codec.INT.optionalFieldOf("decay_delay_ticks", DEFAULT.decayDelayTicks).forGetter(FocusBalance::decayDelayTicks),
            Codec.FLOAT.optionalFieldOf("decay_per_second", DEFAULT.decayPerSecond).forGetter(FocusBalance::decayPerSecond)
    ).apply(i, FocusBalance::new));
}
