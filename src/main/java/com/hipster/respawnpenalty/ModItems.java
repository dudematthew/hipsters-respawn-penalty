package com.hipster.respawnpenalty;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import net.minecraft.util.Rarity;

public final class ModItems {
    public static final Item HOLY_FLASK = new HolyFlaskItem(
            new Item.Settings().maxCount(16).rarity(Rarity.RARE)
    );

    private ModItems() {
    }

    public static void register() {
        Registry.register(
                Registries.ITEM,
                Identifier.of(HipstersRespawnPenalty.MOD_ID, "holy_flask"),
                HOLY_FLASK
        );
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(entries ->
                entries.add(HOLY_FLASK));
    }
}
