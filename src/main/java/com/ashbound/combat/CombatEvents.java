package com.ashbound.combat;

import com.ashbound.AshBound;
import com.ashbound.AshBoundConfig;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
        if (state.comboTimer > 0) {
            state.comboTimer--;
        } else {
            state.comboStep = 0;
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
        if (!enabled()) {
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            // client side: don't even send the attack when out of stamina
            if (!ignoresStamina(player) && player.getData(CombatAttachments.STAMINA).current() <= 0f) {
                event.setCanceled(true);
            }
            return;
        }

        CombatBalance balance = CombatBalanceLoader.get();
        CombatState state = serverPlayer.getData(CombatAttachments.STATE);
        state.pendingDamageMultiplier = 1f;
        state.pendingFinisher = false;

        if (!serverPlayer.getMainHandItem().is(AshBoundItems.STRAIGHT_SWORDS)) {
            // plain weapon / fist: flat cost, no combo
            if (!trySpend(serverPlayer, balance.attackCost())) {
                event.setCanceled(true);
            }
            return;
        }

        // weapon combo: hit 1 -> 2 -> 3 while inside the combo window, then back to 1
        int length = balance.comboLength();
        int step = state.comboTimer > 0 ? (state.comboStep % length) + 1 : 1;
        if (!trySpend(serverPlayer, balance.comboStaminaCosts().get(step - 1))) {
            event.setCanceled(true);
            return;
        }
        state.comboStep = step;
        state.comboTimer = balance.comboWindowTicks();
        state.pendingDamageMultiplier = balance.comboDamageMultipliers().get(step - 1);
        state.pendingFinisher = step == length && length > 1;
    }

    /** Applies the combo multiplier / finisher to the hit the player just swung. */
    private static void applyCombo(LivingIncomingDamageEvent event, ServerPlayer attacker) {
        CombatState state = attacker.getData(CombatAttachments.STATE);
        if (state.pendingDamageMultiplier != 1f) {
            event.setAmount(event.getAmount() * state.pendingDamageMultiplier);
        }
        if (state.pendingFinisher) {
            float strength = CombatBalanceLoader.get().comboFinisherKnockback();
            float yaw = attacker.getYRot() * Mth.DEG_TO_RAD;
            event.getEntity().knockback(strength, Mth.sin(yaw), -Mth.cos(yaw), event.getSource(), event.getAmount());
            attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                    SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 0.75f);
        }
        state.pendingDamageMultiplier = 1f;
        state.pendingFinisher = false;
    }

    // ---------------------------------------------------------------- i-frames

    @SubscribeEvent
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!enabled()) {
            return;
        }
        // outgoing: a player's melee hit -> combo multiplier
        if (event.getSource().getDirectEntity() instanceof ServerPlayer attacker
                && event.getSource().getEntity() == attacker) {
            applyCombo(event, attacker);
        }
        // incoming: dodge i-frames
        if (!(event.getEntity() instanceof ServerPlayer player)) {
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
