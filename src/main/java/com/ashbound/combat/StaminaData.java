package com.ashbound.combat;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Player stamina, synced to the owning client for the HUD. */
public record StaminaData(float current, float max) {
    public static final StaminaData FULL = new StaminaData(CombatBalance.DEFAULT.maxStamina(), CombatBalance.DEFAULT.maxStamina());

    public static final StreamCodec<ByteBuf, StaminaData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, StaminaData::current,
            ByteBufCodecs.FLOAT, StaminaData::max,
            StaminaData::new);

    public float ratio() {
        return max <= 0 ? 0 : Math.clamp(current / max, 0f, 1f);
    }
}
