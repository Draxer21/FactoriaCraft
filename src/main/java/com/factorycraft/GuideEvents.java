package com.factorycraft;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Gives every player the FactoryCraft manual the first time they join a world. */
@EventBusSubscriber(modid = FactoryCraft.MOD_ID)
public final class GuideEvents {
    private static final String GIVEN = "factorycraft_guide_given";

    private GuideEvents() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        CompoundTag data = player.getPersistentData();
        CompoundTag persisted = data.getCompoundOrEmpty(Player.PERSISTED_NBT_TAG);
        if (persisted.getBooleanOr(GIVEN, false)) return;
        ItemStack book = new ItemStack(FactoryCraft.GUIDE_BOOK.value());
        if (!player.getInventory().add(book)) player.drop(book, false);
        persisted.putBoolean(GIVEN, true);
        data.put(Player.PERSISTED_NBT_TAG, persisted);
    }
}
