package com.ashbound.client;

import com.ashbound.AshBound;
import com.ashbound.combat.GrabPayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Grab key (G) and the "holding" hint under the crosshair. */
@EventBusSubscriber(modid = AshBound.MODID, value = Dist.CLIENT)
public final class GrabClient {
    private GrabClient() {
    }

    public static final KeyMapping GRAB_KEY =
            new KeyMapping("key.ashbound.grab", InputConstants.KEY_G, CombatClient.CATEGORY);

    /** nanoTime when the current hold ends; 0 = not holding. Set by the server's GRAB impact. */
    private static long holdEndNanos;
    private static long holdStartNanos;

    static void onGrabbed(int holdTicks) {
        holdStartNanos = System.nanoTime();
        holdEndNanos = holdStartNanos + holdTicks * 50_000_000L;
    }

    static void onReleased() {
        holdEndNanos = 0;
    }

    @SubscribeEvent
    static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(GRAB_KEY);
    }

    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR,
                Identifier.fromNamespaceAndPath(AshBound.MODID, "grab_hint"), GrabClient::renderHint);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (GRAB_KEY.consumeClick()) {
            if (mc.player == null || mc.gui.screen() != null) {
                continue;
            }
            ClientPacketDistributor.sendToServer(new GrabPayload(HeavyClient.pickTarget(mc)));
        }
    }

    private static void renderHint(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (holdEndNanos == 0 || mc.player == null || mc.gui.hud.isHidden()) {
            return;
        }
        long now = System.nanoTime();
        if (now >= holdEndNanos) {
            holdEndNanos = 0;
            return;
        }
        float left = (float) (holdEndNanos - now) / (holdEndNanos - holdStartNanos);
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        int top = height / 2 + 28;

        graphics.centeredText(mc.font, Component.translatable("ashbound.hud.grab_hint",
                GRAB_KEY.getTranslatedKeyMessage(), HeavyClient.HEAVY_KEY.getTranslatedKeyMessage()),
                width / 2, top, 0xFFE8DCC0);
        int barWidth = 40;
        int barLeft = width / 2 - barWidth / 2;
        graphics.fill(barLeft - 1, top + 11, barLeft + barWidth + 1, top + 14, 0xA0000000);
        graphics.fill(barLeft, top + 12, barLeft + Math.round(barWidth * Mth.clamp(left, 0f, 1f)), top + 13, 0xFFE0742C);
    }
}
