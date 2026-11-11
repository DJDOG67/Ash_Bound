package com.ashbound;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Ash_Bound entry point.
 *
 * Each gameplay system (combat, loot, machinery, farming, ...) lives in its own
 * sub-package and registers itself from here. Systems can be switched off in
 * the config (see {@link AshBoundConfig}).
 */
@Mod(AshBound.MODID)
public class AshBound {
    public static final String MODID = "ashbound";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AshBound(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        modContainer.registerConfig(ModConfig.Type.LOCAL, AshBoundConfig.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Ash_Bound loaded. combat={}, machinery={}, farming={}, factions={}",
                AshBoundConfig.ENABLE_COMBAT.getAsBoolean(),
                AshBoundConfig.ENABLE_MACHINERY.getAsBoolean(),
                AshBoundConfig.ENABLE_FARMING.getAsBoolean(),
                AshBoundConfig.ENABLE_FACTIONS.getAsBoolean());
    }
}
