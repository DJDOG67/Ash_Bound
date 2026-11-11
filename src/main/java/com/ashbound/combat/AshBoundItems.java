package com.ashbound.combat;

import com.ashbound.AshBound;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = AshBound.MODID)
public final class AshBoundItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AshBound.MODID);

    /** Weapon types are tags, so any item (ours or another mod's) can join a moveset from JSON. */
    public static final TagKey<Item> STRAIGHT_SWORDS =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(AshBound.MODID, "weapon_types/straight_sword"));

    /** First test weapon. Iron-sword stats; the 3-hit combo comes from the straight_sword tag. */
    public static final DeferredItem<Item> STRAIGHT_SWORD = ITEMS.registerItem("straight_sword",
            props -> new Item(props.sword(ToolMaterial.IRON, 3.0F, -2.4F)));

    private AshBoundItems() {
    }

    @SubscribeEvent
    static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(STRAIGHT_SWORD.get());
        }
    }
}
