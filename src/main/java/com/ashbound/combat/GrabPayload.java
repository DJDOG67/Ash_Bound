package com.ashbound.combat;

import com.ashbound.AshBound;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -> server: grab key pressed. If nothing is held, try to grab targetId;
 * if something is already held, throw it.
 */
public record GrabPayload(int targetId) implements CustomPacketPayload {
    public static final Type<GrabPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(AshBound.MODID, "grab"));

    public static final StreamCodec<ByteBuf, GrabPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GrabPayload::targetId,
            GrabPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
