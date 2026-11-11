package com.ashbound.combat;

import com.ashbound.AshBound;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client -> server: heavy attack released after holding the key for chargeTicks. targetId -1 = whiff. */
public record HeavyPayload(int targetId, int chargeTicks) implements CustomPacketPayload {
    public static final Type<HeavyPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(AshBound.MODID, "heavy_attack"));

    public static final StreamCodec<ByteBuf, HeavyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HeavyPayload::targetId,
            ByteBufCodecs.VAR_INT, HeavyPayload::chargeTicks,
            HeavyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
