package com.ashbound.client;

import com.ashbound.AshBound;
import com.ashbound.combat.CombatAttachments;
import com.ashbound.combat.FocusData;
import com.ashbound.combat.HeavyPayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Heavy attack: hold R to charge, release to strike. Plus the focus pips next to the stamina bar. */
@EventBusSubscriber(modid = AshBound.MODID, value = Dist.CLIENT)
public final class HeavyClient {
    private HeavyClient() {
    }

    public static final KeyMapping HEAVY_KEY =
            new KeyMapping("key.ashbound.heavy_attack", InputConstants.KEY_R, CombatClient.CATEGORY);

    private static final int GOLD = 0xC9A24A;
    private static final int EMBER = 0xE0742C;
    private static final int DARK = 0x1E1B18;
    private static final int CHARGE_CAP_TICKS = 60;

    /** Ticks the key has been held; -1 = not charging. */
    private static int chargeTicks = -1;

    @SubscribeEvent
    static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(HEAVY_KEY);
    }

    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR,
                Identifier.fromNamespaceAndPath(AshBound.MODID, "heavy_charge"), HeavyClient::renderCharge);
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL,
                Identifier.fromNamespaceAndPath(AshBound.MODID, "focus"), HeavyClient::renderFocus);
    }

    // ---------------------------------------------------------------- input

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gui.screen() != null) {
            chargeTicks = -1;
            return;
        }
        if (HEAVY_KEY.isDown()) {
            chargeTicks = chargeTicks < 0 ? 0 : Math.min(chargeTicks + 1, CHARGE_CAP_TICKS);
            return;
        }
        if (chargeTicks >= 0) {
            // released: strike
            ClientPacketDistributor.sendToServer(new HeavyPayload(pickTarget(mc), chargeTicks));
            chargeTicks = -1;
        }
    }

    /** Whatever is under the crosshair, or the lock-on target if the crosshair is off it. */
    private static int pickTarget(Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
            return living.getId();
        }
        LivingEntity locked = LockOnClient.target();
        return locked != null ? locked.getId() : -1;
    }

    // ---------------------------------------------------------------- HUD

    /** Charge bar under the crosshair (below the combo pips) while R is held. */
    private static void renderCharge(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (chargeTicks < 0 || player == null || mc.gui.hud.isHidden()) {
            return;
        }
        FocusData focus = player.getData(CombatAttachments.FOCUS);
        float ratio = Mth.clamp((float) chargeTicks / Math.max(1, focus.fullChargeTicks()), 0f, 1f);
        boolean ready = chargeTicks >= focus.minChargeTicks();
        boolean full = chargeTicks >= focus.fullChargeTicks();

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        int barWidth = 32;
        int left = width / 2 - barWidth / 2;
        int top = height / 2 + 20;

        graphics.fill(left - 1, top - 1, left + barWidth + 1, top + 3, 0xA0000000);
        int color = full ? 0xFF000000 | EMBER : ready ? 0xFF000000 | GOLD : 0xFF7A6230;
        graphics.fill(left, top, left + Math.round(barWidth * ratio), top + 2, color);
        // tick mark where the attack becomes valid
        int minX = left + Math.round(barWidth * Math.min(1f, (float) focus.minChargeTicks() / Math.max(1, focus.fullChargeTicks())));
        graphics.fill(minX, top - 1, minX + 1, top + 3, 0xFFE8DCC0);
    }

    /** Focus pips to the right of the stamina bar. */
    private static void renderFocus(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gui.hud.isHidden() || player.isSpectator()) {
            return;
        }
        FocusData focus = player.getData(CombatAttachments.FOCUS);
        if (focus.maxPips() <= 0) {
            return;
        }
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        // stamina bar spans [width/2 - 91, width/2 - 10] at height - 55 (see CombatClient)
        int left = width / 2 - 91 + 81 + 5;
        int centerY = height - 55 + 1;
        boolean charging = chargeTicks >= 0;

        for (int i = 0; i < focus.maxPips(); i++) {
            float fill = Mth.clamp(focus.focus() - i, 0f, 1f);
            int cx = left + 2 + i * 7;
            int rgb = fill >= 1f ? (charging ? EMBER : GOLD) : DARK;
            int alpha = fill >= 1f ? 255 : 170;
            drawDiamond(graphics, cx, centerY, 2, (alpha << 24) | rgb);
            if (fill > 0f && fill < 1f) {
                // partially filled pip: a small gold core that grows
                int r = fill >= 0.5f ? 1 : 0;
                drawDiamond(graphics, cx, centerY, r, 0xFF000000 | GOLD);
            }
        }
    }

    private static void drawDiamond(GuiGraphicsExtractor graphics, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int half = radius - Math.abs(dy);
            graphics.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }
}
