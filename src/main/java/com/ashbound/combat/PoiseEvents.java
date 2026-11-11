package com.ashbound.combat;

import com.ashbound.AshBound;
import com.ashbound.balance.Balance;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Poise (자세) for every living entity, stagger (그로기), riposte (처형 일격) and parry (패링).
 *
 * Poise only exists on entities that have been hit at least once, so idle mobs cost nothing.
 * The damage pipeline order lives in {@link CombatEvents#onIncomingDamage}.
 */
@EventBusSubscriber(modid = AshBound.MODID)
public final class PoiseEvents {
    private PoiseEvents() {
    }

    // ---------------------------------------------------------------- state helpers

    static float maxPoise(LivingEntity entity) {
        PoiseBalance balance = Balance.POISE.get();
        if (entity instanceof Player) {
            return balance.playerMax();
        }
        String key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        return balance.entityMax().getOrDefault(key, balance.defaultMax());
    }

    static PoiseState state(LivingEntity entity) {
        PoiseState state = entity.getData(CombatAttachments.POISE_STATE);
        if (!state.initialized) {
            state.initialized = true;
            state.max = maxPoise(entity);
            state.current = state.max;
        }
        return state;
    }

    public static boolean isStaggered(LivingEntity entity) {
        return entity.hasData(CombatAttachments.POISE_STATE) && entity.getData(CombatAttachments.POISE_STATE).stagger > 0;
    }

    private static void sync(LivingEntity entity, PoiseState state) {
        PoiseData next = new PoiseData(Math.round(state.current), Math.round(state.max), state.stagger > 0);
        if (!next.equals(entity.getData(CombatAttachments.POISE))) {
            entity.setData(CombatAttachments.POISE, next);
        }
    }

    static void impact(LivingEntity causer, LivingEntity at, int kind) {
        impact(causer, at, kind, 0);
    }

    static void impact(LivingEntity causer, LivingEntity at, int kind, int power) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(at, new ImpactPayload(causer.getId(), kind, power,
                (float) at.getX(), (float) (at.getY() + at.getBbHeight() * 0.6), (float) at.getZ()));
    }

    // ---------------------------------------------------------------- tick: regen + stagger

    @SubscribeEvent
    static void onEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity entity)
                || !(entity.level() instanceof ServerLevel)
                || !entity.hasData(CombatAttachments.POISE_STATE)
                || !CombatEvents.enabled()) {
            return;
        }
        PoiseBalance balance = Balance.POISE.get();
        PoiseState state = entity.getData(CombatAttachments.POISE_STATE);
        state.max = maxPoise(entity); // follows /reload

        if (state.stagger > 0) {
            state.stagger--;
            if (entity instanceof Mob mob) {
                // groggy: stop walking and drifting
                mob.getNavigation().stop();
                mob.setDeltaMovement(0, mob.getDeltaMovement().y, 0);
            }
            if (state.stagger == 0) {
                state.current = state.max; // recovers fully, like Sekiro/Elden Ring
            }
        } else if (state.regenDelay > 0) {
            state.regenDelay--;
        } else if (state.current < state.max) {
            state.current = Math.min(state.max, state.current + balance.regenPerSecond() / 20f);
        }
        sync(entity, state);
    }

    // ---------------------------------------------------------------- hits

    /**
     * Called for every damage instance with a living attacker, after i-frames, parry and the combo multiplier.
     * Riposte on a staggered target, otherwise poise damage (and maybe a break).
     */
    static void onHit(LivingIncomingDamageEvent event, LivingEntity attacker, float poiseMultiplier) {
        LivingEntity target = event.getEntity();
        PoiseBalance balance = Balance.POISE.get();
        PoiseState state = state(target);

        if (state.stagger > 0 && attacker instanceof Player) {
            // riposte: one big hit, then the target recovers
            event.setAmount(event.getAmount() * balance.riposteDamageMultiplier());
            state.stagger = 0;
            state.current = state.max;
            state.regenDelay = 0;
            sync(target, state);
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.2f, 0.55f);
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 0.7f);
            impact(attacker, target, ImpactPayload.RIPOSTE);
            if (attacker instanceof ServerPlayer player) {
                HeavyAttack.gainFocus(player, Balance.COMBAT.get().focus().perRiposte());
            }
            return;
        }
        if (state.stagger > 0) {
            return; // mobs hitting a staggered target don't stack stagger
        }

        float poiseDamage = event.getAmount() * balance.damageToPoise() * poiseMultiplier;
        damagePoise(target, attacker, state, poiseDamage);
    }

    private static void damagePoise(LivingEntity target, LivingEntity attacker, PoiseState state, float amount) {
        PoiseBalance balance = Balance.POISE.get();
        state.current -= amount;
        state.regenDelay = balance.regenDelayTicks();
        if (state.current <= 0f) {
            state.current = 0f;
            state.stagger = balance.staggerTicks();
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.HOSTILE, 1.0f, 0.6f);
            impact(attacker, target, ImpactPayload.POISE_BREAK);
        }
        sync(target, state);
    }

    // ---------------------------------------------------------------- parry

    /** Raising a shield opens a short parry window (costs stamina, has a cooldown). */
    @SubscribeEvent
    static void onStartUsingItem(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !CombatEvents.enabled()) {
            return;
        }
        if (!event.getItem().has(DataComponents.BLOCKS_ATTACKS)) {
            return;
        }
        PoiseBalance balance = Balance.POISE.get();
        CombatState combat = player.getData(CombatAttachments.STATE);
        if (combat.parryCooldown > 0 || isStaggered(player)) {
            return; // still blocks normally, just no parry
        }
        if (!CombatEvents.trySpend(player, balance.parryStaminaCost())) {
            return;
        }
        combat.parryWindow = balance.parryWindowTicks();
        combat.parryCooldown = balance.parryCooldownTicks();
    }

    /** Returns true if the hit was parried (the caller cancels the damage). */
    static boolean tryParry(LivingIncomingDamageEvent event, ServerPlayer player) {
        CombatState combat = player.getData(CombatAttachments.STATE);
        if (combat.parryWindow <= 0 || !player.isUsingItem()) {
            return false;
        }
        // melee only: the thing that touched us must be the living attacker itself (no arrows)
        if (!(event.getSource().getDirectEntity() instanceof LivingEntity attacker) || attacker == player) {
            return false;
        }
        PoiseBalance balance = Balance.POISE.get();
        combat.parryWindow = 0;
        combat.iframes = Math.max(combat.iframes, 4); // don't eat the follow-up frame of the same swing

        PoiseState attackerState = state(attacker);
        if (attackerState.stagger <= 0) {
            damagePoise(attacker, player, attackerState, attackerState.max * balance.parryPoiseDamageRatio());
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5f, 1.8f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8f, 1.4f);
        impact(player, player, ImpactPayload.PARRY);
        HeavyAttack.gainFocus(player, Balance.COMBAT.get().focus().perParry());
        return true;
    }
}
