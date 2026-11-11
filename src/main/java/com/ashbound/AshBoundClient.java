package com.ashbound;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only setup (HUD, key binds, renderers will be registered from here). */
@Mod(value = AshBound.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = AshBound.MODID, value = Dist.CLIENT)
public class AshBoundClient {
    public AshBoundClient(ModContainer container) {
        // Mods screen > Ash_Bound > Config
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        AshBound.LOGGER.info("Ash_Bound client setup done");
    }
}
