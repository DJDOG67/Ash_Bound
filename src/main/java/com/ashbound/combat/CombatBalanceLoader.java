package com.ashbound.combat;

import java.util.Map;

import com.ashbound.AshBound;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

/**
 * Reads data/ashbound/balance/combat.json on world load and on /reload.
 * Edit the JSON, run /reload in game, and the new numbers apply immediately.
 */
@EventBusSubscriber(modid = AshBound.MODID)
public class CombatBalanceLoader extends SimpleJsonResourceReloadListener<CombatBalance> {
    private static final Identifier FILE_ID = Identifier.fromNamespaceAndPath(AshBound.MODID, "combat");
    private static final CombatBalanceLoader INSTANCE = new CombatBalanceLoader();

    private static volatile CombatBalance current = CombatBalance.DEFAULT;

    private CombatBalanceLoader() {
        super(CombatBalance.CODEC, FileToIdConverter.json("balance"));
    }

    public static CombatBalance get() {
        return current;
    }

    @Override
    protected void apply(Map<Identifier, CombatBalance> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        current = files.getOrDefault(FILE_ID, CombatBalance.DEFAULT);
        AshBound.LOGGER.info("Ash_Bound combat balance loaded: {}", current);
    }

    @SubscribeEvent
    static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(AshBound.MODID, "combat_balance"), INSTANCE);
    }
}
