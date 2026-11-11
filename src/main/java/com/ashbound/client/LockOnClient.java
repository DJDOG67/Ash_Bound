package com.ashbound.client;

import java.util.Comparator;

import com.ashbound.AshBound;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Soulslike lock-on. Purely client side: the camera turns toward the target,
 * so movement, dodges and attacks naturally become target-relative.
 */
@EventBusSubscriber(modid = AshBound.MODID, value = Dist.CLIENT)
public final class LockOnClient {
    private LockOnClient() {
    }

    public static final KeyMapping LOCK_KEY =
            new KeyMapping("key.ashbound.lock_on", InputConstants.KEY_V, CombatClient.CATEGORY);

    /** Lock breaks after the target is out of sight this long (ticks). */
    private static final int LOST_SIGHT_GRACE_TICKS = 30;
    /** Lock breaks when the target is this much farther than the acquire range. */
    private static final double BREAK_RANGE_FACTOR = 1.5;

    private static LivingEntity target;
    private static int lostSightTicks;
    private static long lastFrameNanos;

    public static LivingEntity target() {
        return target;
    }

    @SubscribeEvent
    static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(LOCK_KEY);
    }

    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY,
                Identifier.fromNamespaceAndPath(AshBound.MODID, "lock_on_target"), LockOnClient::renderTargetBar);
    }

    // ---------------------------------------------------------------- acquire / release

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            target = null;
            return;
        }
        while (LOCK_KEY.consumeClick()) {
            if (mc.gui.screen() != null) {
                continue;
            }
            target = target == null ? findTarget(player) : null;
            lostSightTicks = 0;
        }
        if (target != null && !stillValid(player, target)) {
            target = null;
        }
    }

    private static LivingEntity findTarget(LocalPlayer player) {
        double range = AshBoundClientConfig.LOCKON_RANGE.get();
        double minDot = Math.cos(Math.toRadians(AshBoundClientConfig.LOCKON_ANGLE.get()));
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0f);

        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                        e -> e != player && e.isAlive() && !(e instanceof ArmorStand) && !e.isInvisible())
                .stream()
                .filter(e -> e.getEyePosition().distanceTo(eye) <= range)
                .filter(e -> look.dot(e.getEyePosition().subtract(eye).normalize()) >= minDot)
                .filter(player::hasLineOfSight)
                // closest to the crosshair wins, distance breaks ties
                .max(Comparator.comparingDouble(e ->
                        look.dot(e.getEyePosition().subtract(eye).normalize()) - e.distanceTo(player) * 0.01))
                .orElse(null);
    }

    private static boolean stillValid(LocalPlayer player, LivingEntity e) {
        if (!e.isAlive() || e.isRemoved() || e.level() != player.level()) {
            return false;
        }
        if (e.distanceTo(player) > AshBoundClientConfig.LOCKON_RANGE.get() * BREAK_RANGE_FACTOR) {
            return false;
        }
        lostSightTicks = player.hasLineOfSight(e) ? 0 : lostSightTicks + 1;
        return lostSightTicks < LOST_SIGHT_GRACE_TICKS;
    }

    // ---------------------------------------------------------------- camera

    /** Turn every frame (not every tick) so the camera stays smooth at any FPS. */
    @SubscribeEvent
    static void onRenderFrame(RenderFrameEvent.Pre event) {
        long now = System.nanoTime();
        float seconds = lastFrameNanos == 0 ? 0f : Math.min((now - lastFrameNanos) / 1.0e9f, 0.1f);
        lastFrameNanos = now;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || target == null || seconds <= 0f) {
            return;
        }
        Vec3 to = target.getEyePosition().subtract(0, target.getEyeHeight() * 0.3, 0).subtract(player.getEyePosition());
        float wantYaw = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90f;
        float wantPitch = (float) -(Mth.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)) * Mth.RAD_TO_DEG);

        // turnSpeed is "fraction per tick"; convert to this frame's share
        float perTick = AshBoundClientConfig.LOCKON_TURN_SPEED.get().floatValue();
        float t = 1f - (float) Math.pow(1f - perTick, seconds * 20f);

        float yaw = player.getYRot() + Mth.wrapDegrees(wantYaw - player.getYRot()) * t;
        float pitch = Mth.clamp(player.getXRot() + (wantPitch - player.getXRot()) * t, -60f, 60f);
        player.setYRot(yaw);
        player.setXRot(pitch);
    }

    // ---------------------------------------------------------------- HUD

    private static void renderTargetBar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (target == null || mc.gui.hud.isHidden() || !AshBoundClientConfig.LOCKON_SHOW_TARGET_BAR.get()) {
            return;
        }
        int width = mc.getWindow().getGuiScaledWidth();
        int barWidth = 120;
        int left = width / 2 - barWidth / 2;
        int top = 30;
        float ratio = Mth.clamp(target.getHealth() / Math.max(1f, target.getMaxHealth()), 0f, 1f);

        graphics.centeredText(mc.font, target.getDisplayName(), width / 2, top - 11, 0xFFE8DCC0);
        graphics.fill(left - 1, top - 1, left + barWidth + 1, top + 4, 0xA0000000);
        graphics.fill(left, top, left + Math.round(barWidth * ratio), top + 3, 0xFFA8432B);
    }
}
