package com.ashbound.combat;

import com.ashbound.AshBound;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class DodgePayloads {
    private DodgePayloads() {
    }

    /** Client -> server: "I want to dodge in this horizontal world direction" (0,0 = backstep). */
    public record Request(float dirX, float dirZ) implements CustomPacketPayload {
        public static final Type<Request> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(AshBound.MODID, "dodge_request"));
        public static final StreamCodec<ByteBuf, Request> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, Request::dirX,
                ByteBufCodecs.FLOAT, Request::dirZ,
                Request::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server -> client: dodge accepted, apply this velocity. */
    public record Start(float velX, float velY, float velZ) implements CustomPacketPayload {
        public static final Type<Start> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(AshBound.MODID, "dodge_start"));
        public static final StreamCodec<ByteBuf, Start> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, Start::velX,
                ByteBufCodecs.FLOAT, Start::velY,
                ByteBufCodecs.FLOAT, Start::velZ,
                Start::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
