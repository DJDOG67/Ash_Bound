package com.ashbound.combat;

import java.util.List;

import com.ashbound.AshBound;
import com.ashbound.balance.Balance;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Grab (잡기): seize a weakened enemy, then throw it (G again) or slam it (R).
 * A thrown enemy damages whatever it crashes into, and itself on impact or landing.
 */
public final class GrabAttack {
    /** Entities in this tag can never be grabbed (bosses, giants). data/ashbound/tags/entity_type/grab_immune.json */
    public static final TagKey<EntityType<?>> GRAB_IMMUNE =
            TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(AshBound.MODID, "grab_immune"));

    private static final SwingAnimation GRAB_SWING = new SwingAnimation(SwingAnimationType.WHACK, 6);
    private static final int MAX_FLIGHT_TICKS = 30;

    private GrabAttack() {
    }

    // ---------------------------------------------------------------- input

    static void onGrabRequest(GrabPayload request, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !CombatEvents.enabled()) {
            return;
        }
        CombatState state = player.getData(CombatAttachments.STATE);
        if (state.grabbedId >= 0) {
            throwHeld(player, state); // second press = throw
            return;
        }
        tryGrab(player, state, request.targetId());
    }

    private static void tryGrab(ServerPlayer player, CombatState state, int targetId) {
        GrabBalance grab = Balance.GRAB.get();
        if (state.grabCooldown > 0 || PoiseEvents.isStaggered(player) || !player.isAlive()) {
            return;
        }
        LivingEntity target = grabbable(player, targetId, grab);
        if (target == null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 0.6f, 1.2f);
            return;
        }
        if (!CombatEvents.trySpend(player, grab.staminaCost())) {
            return;
        }
        state.grabbedId = target.getId();
        state.grabTimer = grab.holdTicks();
        state.grabCooldown = grab.cooldownTicks();
        state.comboStep = 0;
        state.comboTimer = 0;
        PoiseState targetState = PoiseEvents.state(target);
        targetState.held = grab.holdTicks() + 2;
        player.swing(InteractionHand.MAIN_HAND, GRAB_SWING, true);
        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0f, 0.8f);
        PoiseEvents.impact(player, target, ImpactPayload.GRAB, grab.holdTicks());
    }

    /** Only weakened (low poise or staggered), small, non-player, non-immune targets in reach. */
    private static LivingEntity grabbable(ServerPlayer player, int targetId, GrabBalance grab) {
        if (targetId < 0) {
            return null;
        }
        Entity entity = player.level().getEntity(targetId);
        if (!(entity instanceof LivingEntity living) || !living.isAlive() || living instanceof Player
                || living.getType().is(GRAB_IMMUNE) || living.getBbWidth() > grab.maxWidth() || living.isPassenger()) {
            return null;
        }
        double distance = player.getEyePosition().distanceTo(living.getBoundingBox().getCenter()) - living.getBbWidth() * 0.5;
        if (distance > grab.range()) {
            return null;
        }
        PoiseState poise = PoiseEvents.state(living);
        boolean weakened = poise.stagger > 0 || (poise.max > 0 && poise.current / poise.max <= grab.poiseThreshold());
        return weakened ? living : null;
    }

    // ---------------------------------------------------------------- tick

    static void tick(ServerPlayer player, CombatState state) {
        if (state.grabCooldown > 0) {
            state.grabCooldown--;
        }
        tickHold(player, state);
        tickFlight(player, state);
    }

    private static void tickHold(ServerPlayer player, CombatState state) {
        if (state.grabbedId < 0) {
            return;
        }
        LivingEntity held = livingById(player, state.grabbedId);
        if (held == null || !player.isAlive() || PoiseEvents.isStaggered(player)) {
            release(player, state);
            return;
        }
        // carry it in front of the player, slightly lifted
        Vec3 look = player.getViewVector(1.0f);
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        double distance = 0.9 + held.getBbWidth() * 0.5;
        held.setPos(player.getX() + flat.x * distance, player.getY() + 0.25, player.getZ() + flat.z * distance);
        held.setDeltaMovement(Vec3.ZERO);
        held.fallDistance = 0;
        PoiseEvents.state(held).held = Math.max(PoiseEvents.state(held).held, 2);

        if (--state.grabTimer <= 0) {
            throwHeld(player, state); // held too long: it gets thrown anyway
        }
    }

    private static void tickFlight(ServerPlayer player, CombatState state) {
        if (state.thrownId < 0) {
            return;
        }
        LivingEntity flying = livingById(player, state.thrownId);
        state.thrownTicks++;
        if (flying == null || state.thrownTicks > MAX_FLIGHT_TICKS) {
            state.thrownId = -1;
            return;
        }
        GrabBalance grab = Balance.GRAB.get();
        List<LivingEntity> hits = flying.level().getEntitiesOfClass(LivingEntity.class,
                flying.getBoundingBox().inflate(0.3),
                e -> e != flying && e != player && e.isAlive());
        if (!hits.isEmpty()) {
            // crashed into someone: both get hurt
            LivingEntity victim = hits.get(0);
            dealGrabDamage(player, state, victim, grab.throwImpactDamage(), grab.throwPoiseDamage());
            dealGrabDamage(player, state, flying, grab.throwImpactDamage(), grab.throwPoiseDamage());
            flying.setDeltaMovement(flying.getDeltaMovement().scale(0.2));
            impactSound(flying);
            PoiseEvents.impact(player, victim, ImpactPayload.THROW_HIT);
            state.thrownId = -1;
            return;
        }
        if (state.thrownTicks > 3 && flying.onGround()) {
            dealGrabDamage(player, state, flying, grab.throwLandingDamage(), grab.throwPoiseDamage() * 0.5f);
            impactSound(flying);
            PoiseEvents.impact(player, flying, ImpactPayload.THROW_HIT);
            state.thrownId = -1;
        }
    }

    // ---------------------------------------------------------------- throw / slam / release

    private static void throwHeld(ServerPlayer player, CombatState state) {
        LivingEntity held = livingById(player, state.grabbedId);
        GrabBalance grab = Balance.GRAB.get();
        state.grabbedId = -1;
        if (held == null) {
            return;
        }
        Vec3 look = player.getViewVector(1.0f);
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        held.setDeltaMovement(flat.x * grab.throwSpeed(), grab.throwLift() + Math.max(0, look.y) * 0.3, flat.z * grab.throwSpeed());
        PoiseState heldState = PoiseEvents.state(held);
        heldState.held = 0;
        heldState.thrown = MAX_FLIGHT_TICKS;
        state.thrownId = held.getId();
        state.thrownTicks = 0;
        player.swing(InteractionHand.MAIN_HAND, GRAB_SWING, true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.6f);
        PoiseEvents.impact(player, held, ImpactPayload.THROW);
    }

    static void slam(ServerPlayer player, CombatState state) {
        LivingEntity held = livingById(player, state.grabbedId);
        GrabBalance grab = Balance.GRAB.get();
        state.grabbedId = -1;
        if (held == null) {
            return;
        }
        PoiseState heldState = PoiseEvents.state(held);
        heldState.held = 0;
        held.setDeltaMovement(0, -1.2, 0);
        float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        player.swingAndResetAttackStrength(InteractionHand.MAIN_HAND, new SwingAnimation(SwingAnimationType.WHACK, 10), true);
        dealGrabDamage(player, state, held, base * grab.slamDamageMultiplier(), grab.slamPoiseDamage());
        held.level().playSound(null, held.getX(), held.getY(), held.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.3f, 0.5f);
        held.level().playSound(null, held.getX(), held.getY(), held.getZ(),
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.4f, 0.5f);
        PoiseEvents.impact(player, held, ImpactPayload.SLAM);
    }

    /** Let go without throwing (dodge, death, target gone). */
    static void release(ServerPlayer player, CombatState state) {
        if (state.grabbedId < 0) {
            return;
        }
        LivingEntity held = livingById(player, state.grabbedId);
        state.grabbedId = -1;
        if (held != null) {
            PoiseEvents.state(held).held = 0;
            PoiseEvents.impact(player, held, ImpactPayload.THROW);
        }
    }

    // ---------------------------------------------------------------- helpers

    /** Damage through the normal pipeline, with a flat poise amount instead of damage-based poise. */
    private static void dealGrabDamage(ServerPlayer player, CombatState state, LivingEntity target, float damage, float poise) {
        if (damage <= 0f) {
            return;
        }
        DamageSource source = player.damageSources().playerAttack(player);
        float damageToPoise = Math.max(0.0001f, Balance.POISE.get().damageToPoise());
        state.pendingHeavyPoise = poise / (damage * damageToPoise);
        target.hurtServer((ServerLevel) player.level(), source, damage);
        state.pendingHeavyPoise = 1f;
    }

    private static void impactSound(LivingEntity at) {
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(),
                SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0f, 0.7f);
    }

    private static LivingEntity livingById(ServerPlayer player, int id) {
        Entity entity = player.level().getEntity(id);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }
}
