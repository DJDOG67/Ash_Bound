package com.ashbound.client;

import com.ashbound.AshBound;
import com.ashbound.combat.ComboHitPayload;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Combo feedback: pips under the crosshair, camera shake, hit particles
 * and an ember edge flash on the finisher. Everything here is cosmetic.
 */
@EventBusSubscriber(modid = AshBound.MODID, value = Dist.CLIENT)
public final class ComboFxClient {
    private ComboFxClient() {
    }

    // palette from the art direction doc
    private static final int GOLD = 0xC9A24A;
    private static final int EMBER = 0xE0742C;
    private static final int DARK = 0x1E1B18;

    // shake: degrees and seconds
    private static final float SHAKE_HIT_DEG = 0.6f;
    private static final float SHAKE_FINISHER_DEG = 1.8f;
    private static final float SHAKE_HIT_SEC = 0.12f;
    private static final float SHAKE_FINISHER_SEC = 0.25f;
    private static final float FLASH_SEC = 0.22f;

    private static final RandomSource RANDOM = RandomSource.create();

    // combo HUD state (nanoTime based so it is smooth at any FPS)
    private static int hudStep;
    private static int hudLength;
    private static boolean hudFinisher;
    private static long hudStartNanos;
    private static float hudDurationSec;

    private static long shakeStartNanos;
    private static float shakeSec;
    private static float shakeDeg;

    private static long flashStartNanos;

    @SubscribeEvent
    static void onRegisterClientPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(ComboHitPayload.TYPE, ComboFxClient::onComboHit);
    }

    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR,
                Identifier.fromNamespaceAndPath(AshBound.MODID, "combo"), ComboFxClient::renderHud);
    }

    // ---------------------------------------------------------------- incoming hit

    private static void onComboHit(ComboHitPayload hit, IPayloadContext context) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) {
            return;
        }

        if (AshBoundClientConfig.HIT_PARTICLES.get()) {
            spawnParticles(level, hit);
        }

        if (hit.attackerId() != player.getId()) {
            return; // someone else's combo: particles only
        }

        long now = System.nanoTime();
        hudStep = hit.step();
        hudLength = hit.length();
        hudFinisher = hit.finisher();
        hudStartNanos = now;
        hudDurationSec = hit.windowTicks() / 20f + 0.25f;

        float shakeScale = AshBoundClientConfig.SCREEN_SHAKE.get().floatValue();
        if (shakeScale > 0f) {
            shakeStartNanos = now;
            shakeSec = hit.finisher() ? SHAKE_FINISHER_SEC : SHAKE_HIT_SEC;
            shakeDeg = (hit.finisher() ? SHAKE_FINISHER_DEG : SHAKE_HIT_DEG * hit.step()) * shakeScale;
        }
        if (hit.finisher() && AshBoundClientConfig.FINISHER_FLASH.get()) {
            flashStartNanos = now;
        }
    }

    private static void spawnParticles(ClientLevel level, ComboHitPayload hit) {
        int sparks = hit.finisher() ? 22 : 5 + hit.step() * 3;
        for (int i = 0; i < sparks; i++) {
            level.addParticle(ParticleTypes.CRIT, hit.x(), hit.y(), hit.z(),
                    RANDOM.nextGaussian() * 0.35, RANDOM.nextGaussian() * 0.25 + 0.1, RANDOM.nextGaussian() * 0.35);
        }
        if (hit.finisher()) {
            level.addParticle(ParticleTypes.SWEEP_ATTACK, hit.x(), hit.y(), hit.z(), 0, 0, 0);
            for (int i = 0; i < 10; i++) {
                level.addParticle(ParticleTypes.SMALL_FLAME, hit.x(), hit.y(), hit.z(),
                        RANDOM.nextGaussian() * 0.12, RANDOM.nextDouble() * 0.12, RANDOM.nextGaussian() * 0.12);
            }
        }
    }

    // ---------------------------------------------------------------- camera shake

    @SubscribeEvent
    static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (shakeSec <= 0f) {
            return;
        }
        float elapsed = (System.nanoTime() - shakeStartNanos) / 1.0e9f;
        if (elapsed >= shakeSec) {
            shakeSec = 0f;
            return;
        }
        float strength = shakeDeg * (1f - elapsed / shakeSec); // decays to 0
        event.setYaw(event.getYaw() + (RANDOM.nextFloat() * 2f - 1f) * strength);
        event.setPitch(event.getPitch() + (RANDOM.nextFloat() * 2f - 1f) * strength);
        event.setRoll(event.getRoll() + (RANDOM.nextFloat() * 2f - 1f) * strength * 0.5f);
    }

    // ---------------------------------------------------------------- HUD

    private static void renderHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.hud.isHidden()) {
            return;
        }
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        long now = System.nanoTime();

        renderFinisherFlash(graphics, width, height, now);

        if (hudStep <= 0 || !AshBoundClientConfig.COMBO_HUD.get()) {
            return;
        }
        // mirror the server: the combo window is paused while airborne
        LocalPlayer player = mc.player;
        if (player != null && !player.onGround() && !player.isInWater()) {
            long frameNanos = (long) (Math.min(deltaTracker.getRealtimeDeltaTicks(), 2f) * 50_000_000L);
            hudStartNanos += frameNanos;
        }
        float elapsed = (now - hudStartNanos) / 1.0e9f;
        if (elapsed >= hudDurationSec) {
            hudStep = 0;
            return;
        }
        // fully visible for most of the window, then fade
        float fadeStart = hudDurationSec * 0.6f;
        float alpha = elapsed < fadeStart ? 1f : 1f - (elapsed - fadeStart) / (hudDurationSec - fadeStart);
        // short "pop" when a new hit lands
        int pop = elapsed < 0.08f ? 1 : 0;

        int size = 5;
        int gap = 4;
        int total = hudLength * size + (hudLength - 1) * gap;
        int left = width / 2 - total / 2;
        int top = height / 2 + 10;

        for (int i = 0; i < hudLength; i++) {
            int x = left + i * (size + gap);
            boolean lit = i < hudStep;
            boolean current = i == hudStep - 1;
            int rgb = lit ? (hudFinisher && current ? EMBER : GOLD) : DARK;
            int a = (int) (alpha * (lit ? 255 : 140));
            int grow = current ? pop : 0;
            drawDiamond(graphics, x + size / 2, top + size / 2, size / 2 + grow, argb(a, rgb));
        }
    }

    /** Small pixel diamond (◆) centred on cx, cy. */
    private static void drawDiamond(GuiGraphicsExtractor graphics, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int half = radius - Math.abs(dy);
            graphics.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    private static void renderFinisherFlash(GuiGraphicsExtractor graphics, int width, int height, long now) {
        if (flashStartNanos == 0) {
            return;
        }
        float elapsed = (now - flashStartNanos) / 1.0e9f;
        if (elapsed >= FLASH_SEC) {
            flashStartNanos = 0;
            return;
        }
        float fade = 1f - elapsed / FLASH_SEC;
        // edge vignette: a few bands, stronger at the very edge
        for (int band = 0; band < 6; band++) {
            int a = (int) (fade * 110 * (1f - band / 6f));
            int color = argb(a, EMBER);
            int t = band * 4;
            int thickness = 4;
            graphics.fill(t, t, width - t, t + thickness, color);                       // top
            graphics.fill(t, height - t - thickness, width - t, height - t, color);     // bottom
            graphics.fill(t, t + thickness, t + thickness, height - t - thickness, color);           // left
            graphics.fill(width - t - thickness, t + thickness, width - t, height - t - thickness, color); // right
        }
    }

    private static int argb(int alpha, int rgb) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0xFFFFFF);
    }
}
