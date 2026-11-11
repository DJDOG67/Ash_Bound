package com.ashbound.combat;

import com.ashbound.balance.Balance;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Focus gauge (집중) and the charged heavy attack (강공격).
 *
 * Focus is earned by fighting well (combo hits, parries, ripostes) and spent all at once
 * by a heavy attack: more pips = more damage, poise damage, knockback and spectacle.
 */
public final class HeavyAttack {
    private HeavyAttack() {
    }

    // ---------------------------------------------------------------- focus

    public static void gainFocus(ServerPlayer player, float pips) {
        if (pips <= 0f) {
            return;
        }
        FocusBalance balance = Balance.COMBAT.get().focus();
        CombatState state = player.getData(CombatAttachments.STATE);
        state.focus = Math.min(balance.maxPips(), state.focus + pips);
        state.focusIdle = 0;
        sync(player, state);
    }

    /** Called every server tick from CombatEvents.onPlayerTick. */
    static void tick(ServerPlayer player, CombatState state) {
        if (state.heavyCooldown > 0) {
            state.heavyCooldown--;
        }
        FocusBalance balance = Balance.COMBAT.get().focus();
        if (state.focus > 0f) {
            if (state.focusIdle < balance.decayDelayTicks()) {
                state.focusIdle++;
            } else {
                state.focus = Math.max(0f, state.focus - balance.decayPerSecond() / 20f);
            }
        }
        sync(player, state);
    }

    private static void sync(ServerPlayer player, CombatState state) {
        CombatBalance balance = Balance.COMBAT.get();
        // round to 1/100 pip so slow decay doesn't send a packet every tick
        float rounded = Math.round(state.focus * 100f) / 100f;
        FocusData next = new FocusData(rounded, balance.focus().maxPips(),
                balance.heavy().minChargeTicks(), balance.heavy().fullChargeTicks());
        if (!next.equals(player.getData(CombatAttachments.FOCUS))) {
            player.setData(CombatAttachments.FOCUS, next);
        }
    }

    // ---------------------------------------------------------------- heavy attack

    static void onHeavyRequest(HeavyPayload request, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !CombatEvents.enabled()) {
            return;
        }
        HeavyBalance heavy = Balance.COMBAT.get().heavy();
        CombatState state = player.getData(CombatAttachments.STATE);
        if (state.heavyCooldown > 0 || request.chargeTicks() < heavy.minChargeTicks()
                || PoiseEvents.isStaggered(player) || !player.isAlive()) {
            return;
        }
        if (!CombatEvents.trySpend(player, heavy.staminaCost())) {
            return;
        }
        state.heavyCooldown = heavy.cooldownTicks();
        state.comboStep = 0; // a heavy attack ends any light combo
        state.comboTimer = 0;
        player.swing(InteractionHand.MAIN_HAND, true);

        LivingEntity target = validTarget(player, request.targetId());
        if (target == null) {
            // whiff: stamina is still spent, focus is kept
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 1.0f, 0.7f);
            return;
        }

        int pips = (int) Math.floor(state.focus + 1.0e-4f);
        state.focus -= pips;
        state.focusIdle = 0;

        float charge = heavy.fullChargeTicks() <= heavy.minChargeTicks() ? 1f
                : Mth.clamp((float) (request.chargeTicks() - heavy.minChargeTicks())
                        / (heavy.fullChargeTicks() - heavy.minChargeTicks()), 0f, 1f);
        float chargeScale = heavy.partialChargeDamage() + (1f - heavy.partialChargeDamage()) * charge;
        float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float damage = base * HeavyBalance.pick(heavy.damageByFocus(), pips) * chargeScale;

        ServerLevel level = (ServerLevel) player.level();
        DamageSource source = player.damageSources().playerAttack(player);
        state.pendingHeavyPoise = HeavyBalance.pick(heavy.poiseByFocus(), pips);
        boolean hurt = target.hurtServer(level, source, damage);
        state.pendingHeavyPoise = 1f;

        if (hurt) {
            float yaw = player.getYRot() * Mth.DEG_TO_RAD;
            target.knockback(HeavyBalance.pick(heavy.knockbackByFocus(), pips), Mth.sin(yaw), -Mth.cos(yaw), source, damage);
        }
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.2f, 0.6f - 0.05f * pips);
        if (pips > 0) {
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.6f + 0.15f * pips, 0.8f);
        }
        PoiseEvents.impact(player, target, ImpactPayload.HEAVY, pips);
        sync(player, state);
    }

    /** The target must exist, be alive and be within the player's normal melee reach (+1 block slack). */
    private static LivingEntity validTarget(ServerPlayer player, int targetId) {
        if (targetId < 0) {
            return null;
        }
        Entity entity = player.level().getEntity(targetId);
        if (!(entity instanceof LivingEntity living) || !living.isAlive() || living == player) {
            return null;
        }
        double reach = player.entityInteractionRange() + 1.0;
        double distance = player.getEyePosition().distanceTo(living.getBoundingBox().getCenter())
                - living.getBbWidth() * 0.5;
        return distance <= reach ? living : null;
    }
}
