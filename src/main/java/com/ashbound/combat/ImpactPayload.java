package com.ashbound.combat;

import com.ashbound.AshBound;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server -> clients: a special combat moment happened at (x, y, z). Cosmetic only. */
public record ImpactPayload(int causerId, int kind, int power, float x, float y, float z) implements CustomPacketPayload {
    /** The player parried an attack. causer = the parrying player. */
    public static final int PARRY = 0;
    /** An entity's poise broke and it is now staggered. causer = who broke it. */
    public static final int POISE_BREAK = 1;
    /** A staggered entity took a riposte. causer = the attacker. */
    public static final int RIPOSTE = 2;
    /** A heavy attack landed. causer = the attacker, power = focus pips spent. */
    public static final int HEAVY = 3;
    /** A grab caught something. causer = the grabbing player, power = hold ticks. */
    public static final int GRAB = 4;
    /** A thrown entity hit something or the ground. causer = the thrower. */
    public static final int THROW_HIT = 5;
    /** A held entity was slammed into the ground. causer = the player. */
    public static final int SLAM = 6;
    /** A held entity was thrown (or let go). causer = the player. */
    public static final int THROW = 7;

    public static final Type<ImpactPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(AshBound.MODID, "impact"));

    public static final StreamCodec<ByteBuf, ImpactPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ImpactPayload::causerId,
            ByteBufCodecs.VAR_INT, ImpactPayload::kind,
            ByteBufCodecs.VAR_INT, ImpactPayload::power,
            ByteBufCodecs.FLOAT, ImpactPayload::x,
            ByteBufCodecs.FLOAT, ImpactPayload::y,
            ByteBufCodecs.FLOAT, ImpactPayload::z,
            ImpactPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
