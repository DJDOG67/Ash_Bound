package com.ashbound.combat;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What the owning client needs for the focus pips and the heavy-attack charge bar.
 * The charge timings ride along so the client HUD matches the server's balance file.
 */
public record FocusData(float focus, int maxPips, int minChargeTicks, int fullChargeTicks) {
    public static final FocusData EMPTY = new FocusData(0f,
            FocusBalance.DEFAULT.maxPips(), HeavyBalance.DEFAULT.minChargeTicks(), HeavyBalance.DEFAULT.fullChargeTicks());

    public static final StreamCodec<ByteBuf, FocusData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, FocusData::focus,
            ByteBufCodecs.VAR_INT, FocusData::maxPips,
            ByteBufCodecs.VAR_INT, FocusData::minChargeTicks,
            ByteBufCodecs.VAR_INT, FocusData::fullChargeTicks,
            FocusData::new);
}
