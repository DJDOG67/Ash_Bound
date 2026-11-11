package com.ashbound.combat;

import com.ashbound.AshBound;
import com.ashbound.AshBoundConfig;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-authoritative stamina and dodge logic.
 *
 * Flow of a dodge: client presses the key -> {@link DodgePayloads.Request} ->
 * server checks stamina / cooldown / ground, spends stamina, grants i-frames ->
 * {@link DodgePayloads.Start} -> client applies the velocity.
 */
@EventBusSubscriber(modid = AshBound.MODID)
public final class CombatEvents {
    private CombatEvents() {
    }

    static boolean enabled() {
        try {
            return AshBoundConfig.ENABLE_COMBAT.getAsBoolean();
        } catch (IllegalStateException notLoadedYet) {
            return true;
        }
    }

    static boolean ignoresStamina(Player player) {
        return player.isCreative() || player.isSpectator();
    }

    // ---------------------------------------------------------------- network

    @SubscribeEvent
    static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToServer(DodgePayloads.Request.TYPE, DodgePayloads.Request.STREAM_CODEC, CombatEvents::onDodgeRequest)
                // client handler is registered in AshBoundClient
                .playToClient(DodgePayloads.Start.TYPE, DodgePayloads.Start.STREAM_CODEC);
    }

    // ---------------------------------------------------------------- stamina

    /** Spends stamina. Returns false (and spends nothing) if the player is already at 0. */
    public static boolean trySpend(ServerPlayer player, float cost) {
        if (ignoresStamina(player)) {
            return true;
        }
        CombatBalance balance = CombatBalanceLoader.get();
        StaminaData stamina = player.getData(CombatAttachments.STAMINA);
        if (stamina.current() <= 0f) {
            return false;
        }
        float next = stamina.current() - cost;
        CombatState state = player.getData(CombatAttachments.STATE);
        if (next <= 0f) {
            next = 0f;
            state.regenDelay = balance.exhaustedDelayTicks();
        } else {
            state.regenDelay = balance.regenDelayTicks();
        }
        player.setData(CombatAttachments.STAMINA, new StaminaData(next, balance.maxStamina()));
        return true;
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !enabled()) {
            return;
        }
        CombatBalance balance = CombatBalanceLoader.get();
        CombatState state = player.getData(CombatAttachments.STATE);

        if (state.iframes > 0) state.iframes--;
        if (state.dodgeCooldown > 0) state.dodgeCooldown--;
        if (state.chainTimer > 0) {
            state.chainTimer--;
        } else {
            state.chainCount = 0;
        }

        StaminaData stamina = player.getData(CombatAttachments.STAMINA);
        float max = balance.maxStamina();
        float current = Math.min(stamina.current(), max);

        if (ignoresStamina(player)) {
            current = max;
        } else if (state.regenDelay > 0) {
            state.regenDelay--;
        } else if (current < max) {
            current = Math.min(max, current + balance.regenPerSecond() / 20f);
        }

        if (current != stamina.current() || max != stamina.max()) {
            player.setData(CombatAttachments.STAMINA, new StaminaData(current, max));
        }
    }

    // ---------------------------------------------------------------- attack

    @SubscribeEvent
    static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (!enabled() || ignoresStamina(player)) {
            return;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (!trySpend(serverPlayer, CombatBalanceLoader.get().attackCost())) {
                event.setCanceled(true);
            }
        } else if (player.getData(CombatAttachments.STAMINA).current() <= 0f) {
            // client side: don't even send the attack when out of stamina
            event.setCanceled(true);
        }
    }

    // ---------------------------------------------------------------- i-frames

    @SubscribeEvent
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !enabled()) {
            return;
        }
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return; // void, /kill
        }
        if (player.getData(CombatAttachments.STATE).iframes > 0) {
            event.setCanceled(true);
        }
    }

    // ---------------------------------------------------------------- dodge

    static void onDodgeRequest(DodgePayloads.Request request, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !enabled()) {
            return;
        }
        CombatBalance balance = CombatBalanceLoader.get();
        CombatState state = player.getData(CombatAttachments.STATE);

        if (state.dodgeCooldown > 0 || !player.onGround() || player.isPassenger()) {
            return;
        }
        if (!trySpend(player, balance.dodgeCost())) {
            return;
        }

        // spamming dodges: after chain_limit dodges in the window, i-frames are halved
        int iframes = balance.dodgeIframeTicks();
        if (state.chainCount >= balance.chainLimit()) {
            iframes = iframes / 2;
        }
        state.chainCount++;
        state.chainTimer = balance.chainWindowTicks();
        state.iframes = iframes;
        state.dodgeCooldown = balance.dodgeCooldownTicks();

        float dx = request.dirX();
        float dz = request.dirZ();
        float length = Mth.sqrt(dx * dx + dz * dz);
        if (length < 0.01f) {
            // no direction pressed: backstep, opposite to where the player looks
            float yaw = player.getYRot() * Mth.DEG_TO_RAD;
            dx = Mth.sin(yaw);
            dz = -Mth.cos(yaw);
        } else {
            dx /= length;
            dz /= length;
        }

        PacketDistributor.sendToPlayer(player, new DodgePayloads.Start(
                dx * balance.dodgeSpeed(), balance.dodgeHop(), dz * balance.dodgeSpeed()));
    }
}
