package com.ashbound.client;

import com.ashbound.AshBound;
import com.ashbound.combat.ComboHitPayload;
import com.ashbound.combat.ImpactPayload;

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
    private static int flashColor = EMBER;
    private static float flashSec = FLASH_SEC;

    @SubscribeEvent
    static void onRegisterClientPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(ComboHitPayload.TYPE, ComboFxClient::onComboHit);
        event.register(ImpactPayload.TYPE, ComboFxClient::onImpact);
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
        if (hit.finisher()) {
            flash(EMBER, FLASH_SEC);
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

    // ---------------------------------------------------------------- shared effect helpers

    private static void shake(float degrees, float seconds) {
        float scale = AshBoundClientConfig.SCREEN_SHAKE.get().floatValue();
        if (scale <= 0f) {
            return;
        }
        shakeStartNanos = System.nanoTime();
        shakeSec = seconds;
        shakeDeg = degrees * scale;
    }

    private static void flash(int rgb, float seconds) {
        if (!AshBoundClientConfig.FINISHER_FLASH.get()) {
            return;
        }
        flashStartNanos = System.nanoTime();
        flashColor = rgb;
        flashSec = seconds;
    }

    // ---------------------------------------------------------------- parry / poise break / riposte

    private static final int PARRY_WHITE = 0xF2EEE4;

    private static void onImpact(ImpactPayload impact, IPayloadContext context) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) {
            return;
        }
        boolean mine = impact.causerId() == player.getId();
        boolean particles = AshBoundClientConfig.HIT_PARTICLES.get();
        double x = impact.x(), y = impact.y(), z = impact.z();

        switch (impact.kind()) {
            case ImpactPayload.PARRY -> {
                if (particles) {
                    for (int i = 0; i < 14; i++) {
                        level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z,
                                RANDOM.nextGaussian() * 0.4, RANDOM.nextGaussian() * 0.3 + 0.1, RANDOM.nextGaussian() * 0.4);
                    }
                    for (int i = 0; i < 8; i++) {
                        level.addParticle(ParticleTypes.CRIT, x, y, z,
                                RANDOM.nextGaussian() * 0.3, RANDOM.nextGaussian() * 0.3, RANDOM.nextGaussian() * 0.3);
                    }
                }
                if (mine) {
                    shake(1.2f, 0.15f);
                    flash(PARRY_WHITE, 0.15f);
                }
            }
            case ImpactPayload.POISE_BREAK -> {
                if (particles) {
                    for (int i = 0; i < 10; i++) {
                        level.addParticle(ParticleTypes.CLOUD, x, y, z,
                                RANDOM.nextGaussian() * 0.08, RANDOM.nextDouble() * 0.08, RANDOM.nextGaussian() * 0.08);
                    }
                    for (int i = 0; i < 16; i++) {
                        level.addParticle(ParticleTypes.CRIT, x, y + 0.4, z,
                                RANDOM.nextGaussian() * 0.25, RANDOM.nextDouble() * 0.3, RANDOM.nextGaussian() * 0.25);
                    }
                }
                if (mine) {
                    shake(1.0f, 0.18f);
                }
            }
            case ImpactPayload.RIPOSTE -> {
                if (particles) {
                    level.addParticle(ParticleTypes.SWEEP_ATTACK, x, y, z, 0, 0, 0);
                    for (int i = 0; i < 30; i++) {
                        level.addParticle(ParticleTypes.CRIT, x, y, z,
                                RANDOM.nextGaussian() * 0.45, RANDOM.nextGaussian() * 0.35 + 0.1, RANDOM.nextGaussian() * 0.45);
                    }
                    for (int i = 0; i < 18; i++) {
                        level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z,
                                RANDOM.nextGaussian() * 0.15, RANDOM.nextDouble() * 0.15, RANDOM.nextGaussian() * 0.15);
                    }
                }
                if (mine) {
                    shake(2.4f, 0.3f);
                    flash(EMBER, 0.3f);
                }
            }
            case ImpactPayload.HEAVY -> {
                int pips = impact.power();
                if (particles) {
                    level.addParticle(ParticleTypes.SWEEP_ATTACK, x, y, z, 0, 0, 0);
                    for (int i = 0; i < 12 + pips * 10; i++) {
                        level.addParticle(ParticleTypes.CRIT, x, y, z,
                                RANDOM.nextGaussian() * 0.4, RANDOM.nextGaussian() * 0.3 + 0.1, RANDOM.nextGaussian() * 0.4);
                    }
                    for (int i = 0; i < pips * 8; i++) {
                        level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z,
                                RANDOM.nextGaussian() * 0.18, RANDOM.nextDouble() * 0.18, RANDOM.nextGaussian() * 0.18);
                    }
                    if (pips >= 3) {
                        for (int i = 0; i < 10; i++) {
                            level.addParticle(ParticleTypes.LAVA, x, y, z, 0, 0, 0);
                        }
                    }
                }
                if (mine) {
                    shake(1.4f + pips * 0.6f, 0.2f + pips * 0.05f);
                    if (pips > 0) {
                        flash(EMBER, 0.15f + pips * 0.07f);
                    }
                }
            }
            case ImpactPayload.GRAB -> {
                if (particles) {
                    for (int i = 0; i < 6; i++) {
                        level.addParticle(ParticleTypes.CRIT, x, y, z,
                                RANDOM.nextGaussian() * 0.2, RANDOM.nextGaussian() * 0.2, RANDOM.nextGaussian() * 0.2);
                    }
                }
                if (mine) {
                    shake(0.8f, 0.12f);
                    GrabClient.onGrabbed(impact.power());
                }
            }
            case ImpactPayload.THROW -> {
                if (mine) {
                    GrabClient.onReleased();
                    shake(0.6f, 0.1f);
                }
            }
            case ImpactPayload.THROW_HIT -> {
                if (particles) {
                    for (int i = 0; i < 12; i++) {
                        level.addParticle(ParticleTypes.CLOUD, x, y - 0.4, z,
                                RANDOM.nextGaussian() * 0.1, RANDOM.nextDouble() * 0.06, RANDOM.nextGaussian() * 0.1);
                    }
                    for (int i = 0; i < 14; i++) {
                        level.addParticle(ParticleTypes.CRIT, x, y, z,
                                RANDOM.nextGaussian() * 0.35, RANDOM.nextGaussian() * 0.3, RANDOM.nextGaussian() * 0.35);
                    }
                }
                if (mine) {
                    shake(1.0f, 0.15f);
                }
            }
            case ImpactPayload.SLAM -> {
                if (particles) {
                    // dust ring on the ground
                    for (int i = 0; i < 24; i++) {
                        double angle = i / 24.0 * Math.PI * 2;
                        level.addParticle(ParticleTypes.CLOUD, x, y - 0.6, z,
                                Math.cos(angle) * 0.25, 0.02, Math.sin(angle) * 0.25);
                    }
                    for (int i = 0; i < 24; i++) {
                        level.addParticle(ParticleTypes.CRIT, x, y, z,
                                RANDOM.nextGaussian() * 0.4, RANDOM.nextDouble() * 0.4, RANDOM.nextGaussian() * 0.4);
                    }
                    for (int i = 0; i < 10; i++) {
                        level.addParticle(ParticleTypes.SMALL_FLAME, x, y - 0.3, z,
                                RANDOM.nextGaussian() * 0.15, RANDOM.nextDouble() * 0.1, RANDOM.nextGaussian() * 0.15);
                    }
                }
                if (mine) {
                    GrabClient.onReleased();
                    shake(2.6f, 0.3f);
                    flash(EMBER, 0.25f);
                }
            }
            default -> {
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
        if (elapsed >= flashSec) {
            flashStartNanos = 0;
            return;
        }
        float fade = 1f - elapsed / flashSec;
        // edge vignette: a few bands, stronger at the very edge
        for (int band = 0; band < 6; band++) {
            int a = (int) (fade * 110 * (1f - band / 6f));
            int color = argb(a, flashColor);
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
