package com.factorycraft;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/** Enforces protected zones (see {@link ZoneControllerBlockEntity}). */
@EventBusSubscriber(modid = FactoryCraft.MOD_ID)
public final class ZoneProtection {
    private ZoneProtection() {}

    private static void deny(Player player) {
        player.sendOverlayMessage(Component.translatable("message.factorycraft.zone.denied"));
    }

    @SubscribeEvent
    public static void onBreak(net.neoforged.neoforge.event.level.block.BreakBlockEvent event) {
        if (!(event.getLevel() instanceof Level level)) return;
        if (!ZoneControllerBlockEntity.allowed(level, event.getPos(), event.getPlayer())) {
            event.setCanceled(true);
            deny(event.getPlayer());
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof Level level)) return;
        Player player = event.getEntity() instanceof Player p ? p : null;
        if (!ZoneControllerBlockEntity.allowed(level, event.getPos(), player)) {
            event.setCanceled(true);
            if (player != null) deny(player);
        }
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;
        if (!ZoneControllerBlockEntity.allowed(level, event.getPos(), event.getEntity())) {
            event.setCanceled(true);
            deny(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        event.getAffectedBlocks().removeIf(pos -> ZoneControllerBlockEntity.zoneAt(level, pos) != null);
    }
}
