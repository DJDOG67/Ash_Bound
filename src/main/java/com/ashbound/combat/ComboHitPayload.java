package com.ashbound.combat;

import com.ashbound.AshBound;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server -> clients: a combo hit landed. Everyone nearby gets particles;
 * the attacker also gets the combo HUD, camera shake and finisher flash.
 */
public record ComboHitPayload(int attackerId, int step, int length, boolean finisher, int windowTicks,
                              float x, float y, float z) implements CustomPacketPayload {

    public static final Type<ComboHitPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(AshBound.MODID, "combo_hit"));

    public static final StreamCodec<ByteBuf, ComboHitPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ComboHitPayload::attackerId,
            ByteBufCodecs.VAR_INT, ComboHitPayload::step,
            ByteBufCodecs.VAR_INT, ComboHitPayload::length,
            ByteBufCodecs.BOOL, ComboHitPayload::finisher,
            ByteBufCodecs.VAR_INT, ComboHitPayload::windowTicks,
            ByteBufCodecs.FLOAT, ComboHitPayload::x,
            ByteBufCodecs.FLOAT, ComboHitPayload::y,
            ByteBufCodecs.FLOAT, ComboHitPayload::z,
            ComboHitPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
