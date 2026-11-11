package com.ashbound.balance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ashbound.AshBound;
import com.ashbound.combat.CombatBalance;
import com.ashbound.combat.GrabBalance;
import com.ashbound.combat.PoiseBalance;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

/**
 * All balance files in data/ashbound/balance/. Loaded on world load and on /reload.
 *
 * To add a new file: write a record + Codec + DEFAULT, then register it here:
 * {@code public static final BalanceFile<X> X = register("x", X.CODEC, X.DEFAULT);}
 */
@EventBusSubscriber(modid = AshBound.MODID)
public final class Balance {
    private static final List<BalanceFile<?>> FILES = new ArrayList<>();

    public static final BalanceFile<CombatBalance> COMBAT = register("combat", CombatBalance.CODEC, CombatBalance.DEFAULT);
    public static final BalanceFile<PoiseBalance> POISE = register("poise", PoiseBalance.CODEC, PoiseBalance.DEFAULT);
    public static final BalanceFile<GrabBalance> GRAB = register("grab", GrabBalance.CODEC, GrabBalance.DEFAULT);

    private Balance() {
    }

    private static <T> BalanceFile<T> register(String name, Codec<T> codec, T defaultValue) {
        BalanceFile<T> file = new BalanceFile<>(Identifier.fromNamespaceAndPath(AshBound.MODID, name), codec, defaultValue);
        FILES.add(file);
        return file;
    }

    @SubscribeEvent
    static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(AshBound.MODID, "balance"), new Loader());
    }

    /** Reads every JSON in balance/ as raw data, then decodes each with its own codec. */
    private static final class Loader extends SimpleJsonResourceReloadListener<Dynamic<?>> {
        Loader() {
            super(Codec.PASSTHROUGH, FileToIdConverter.json("balance"));
        }

        @Override
        protected void apply(Map<Identifier, Dynamic<?>> files, ResourceManager resourceManager, ProfilerFiller profiler) {
            for (BalanceFile<?> file : FILES) {
                load(file, files.get(file.id()));
            }
        }

        private static <T> void load(BalanceFile<T> file, Dynamic<?> raw) {
            if (raw == null) {
                AshBound.LOGGER.warn("Ash_Bound balance file {} missing, using defaults", file.id());
                file.set(file.defaultValue());
                return;
            }
            file.codec().parse(raw)
                    .ifSuccess(value -> {
                        file.set(value);
                        AshBound.LOGGER.info("Ash_Bound balance loaded: {} = {}", file.id(), value);
                    })
                    .ifError(error -> {
                        AshBound.LOGGER.error("Ash_Bound balance file {} is broken ({}), keeping previous values",
                                file.id(), error.message());
                    });
        }
    }
}
