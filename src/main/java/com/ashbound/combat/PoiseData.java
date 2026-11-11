package com.ashbound.combat;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Poise as seen by clients (rounded to whole numbers so regen doesn't spam packets).
 * The precise value lives server-side in {@link PoiseState}.
 */
public record PoiseData(int current, int max, boolean staggered) {
    public static final PoiseData NONE = new PoiseData(0, 0, false);

    public static final StreamCodec<ByteBuf, PoiseData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PoiseData::current,
            ByteBufCodecs.VAR_INT, PoiseData::max,
            ByteBufCodecs.BOOL, PoiseData::staggered,
            PoiseData::new);

    public float ratio() {
        return max <= 0 ? 1f : Math.clamp((float) current / max, 0f, 1f);
    }
}
