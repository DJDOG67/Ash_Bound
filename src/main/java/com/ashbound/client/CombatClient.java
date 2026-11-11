package com.ashbound.client;

import org.lwjgl.glfw.GLFW;

import com.ashbound.AshBound;
import com.ashbound.combat.CombatAttachments;
import com.ashbound.combat.DodgePayloads;
import com.ashbound.combat.StaminaData;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
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
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Dodge key, stamina HUD bar and the client half of the dodge. */
@EventBusSubscriber(modid = AshBound.MODID, value = Dist.CLIENT)
public final class CombatClient {
    private CombatClient() {
    }

    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(AshBound.MODID, "combat"));

    public static final KeyMapping DODGE_KEY =
            new KeyMapping("key.ashbound.dodge", GLFW.GLFW_KEY_LEFT_ALT, CATEGORY);

    // HUD layout (GUI pixels). Bar sits above the health/armor rows, left of center.
    private static final int BAR_WIDTH = 81;
    private static final int BAR_HEIGHT = 3;
    private static final int BAR_OFFSET_FROM_BOTTOM = 55;

    private static final int COLOR_BACK = 0xA0000000;
    private static final int COLOR_FILL = 0xFFC9A24A;      // faded gold (art direction palette)
    private static final int COLOR_EMPTY_FLASH = 0xFFE0742C; // ember orange when drained

    @SubscribeEvent
    static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(DODGE_KEY);
    }

    @SubscribeEvent
    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL,
                Identifier.fromNamespaceAndPath(AshBound.MODID, "stamina"), CombatClient::renderStamina);
    }

    @SubscribeEvent
    static void onRegisterClientPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(DodgePayloads.Start.TYPE, CombatClient::onDodgeStart);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        while (DODGE_KEY.consumeClick()) {
            if (player == null || mc.screen != null) {
                continue;
            }
            if (!player.isCreative() && player.getData(CombatAttachments.STAMINA).current() <= 0f) {
                continue;
            }
            // input direction relative to where the player looks -> world direction
            float forward = (mc.options.keyUp.isDown() ? 1f : 0f) - (mc.options.keyDown.isDown() ? 1f : 0f);
            float right = (mc.options.keyRight.isDown() ? 1f : 0f) - (mc.options.keyLeft.isDown() ? 1f : 0f);
            float yaw = player.getYRot() * Mth.DEG_TO_RAD;
            float sin = Mth.sin(yaw);
            float cos = Mth.cos(yaw);
            float dirX = forward * -sin + right * -cos;
            float dirZ = forward * cos + right * -sin;
            ClientPacketDistributor.sendToServer(new DodgePayloads.Request(dirX, dirZ));
        }
    }

    private static void onDodgeStart(DodgePayloads.Start start, IPayloadContext context) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        double velY = start.velY() > 0f ? start.velY() : player.getDeltaMovement().y;
        player.setDeltaMovement(start.velX(), velY, start.velZ());
    }

    private static void renderStamina(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || player.isCreative() || player.isSpectator()) {
            return;
        }
        StaminaData stamina = player.getData(CombatAttachments.STAMINA);

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int left = screenWidth / 2 - 91;
        int top = screenHeight - BAR_OFFSET_FROM_BOTTOM;

        graphics.fill(left - 1, top - 1, left + BAR_WIDTH + 1, top + BAR_HEIGHT + 1, COLOR_BACK);
        int filled = Math.round(BAR_WIDTH * stamina.ratio());
        int color = stamina.current() <= 0f ? COLOR_EMPTY_FLASH : COLOR_FILL;
        if (filled > 0) {
            graphics.fill(left, top, left + filled, top + BAR_HEIGHT, color);
        } else {
            // drained: thin ember line so the player sees why they can't act
            graphics.fill(left, top + 1, left + BAR_WIDTH, top + 2, color);
        }
    }
}
