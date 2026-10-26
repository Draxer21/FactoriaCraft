package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** CI only (-Dfactorycraft.selfTest=true): checks the capabilities other mods see, and logs the results. */
@EventBusSubscriber(modid = FactoryCraft.MOD_ID)
public final class CompatSelfTest {
    private CompatSelfTest() {}

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        if (!Boolean.getBoolean("factorycraft.selfTest")) return;
        ServerLevel level = event.getServer().overworld();
        BlockPos base = new BlockPos(0, level.getMaxY() - 8, 0);
        StringBuilder log = new StringBuilder("SELFTEST");
        try {
            level.setBlockAndUpdate(base, FactoryCraft.GENERATOR.value().defaultBlockState());
            if (level.getBlockEntity(base) instanceof Energy.Source generator) generator.setStored(50_000);
            var genFe = level.getCapability(Capabilities.Energy.BLOCK, base, Direction.UP);
            try (Transaction tx = Transaction.openRoot()) {
                log.append(" extract=").append(genFe == null ? "none" : genFe.extract(1_000, tx));
                tx.commit();
            }
            log.append(" left=").append(genFe == null ? -1 : genFe.getAmountAsLong());
            try (Transaction tx = Transaction.openRoot()) {
                if (genFe != null) genFe.extract(5_000, tx); // not committed: must roll back
            }
            log.append(" afterRollback=").append(genFe == null ? -1 : genFe.getAmountAsLong());

            BlockPos crusher = base.east(3);
            level.setBlockAndUpdate(crusher, FactoryCraft.PRESS.value().defaultBlockState());
            var machineFe = level.getCapability(Capabilities.Energy.BLOCK, crusher, Direction.UP);
            try (Transaction tx = Transaction.openRoot()) {
                log.append(" machineIn=").append(machineFe == null ? "none" : machineFe.insert(1_500, tx));
                tx.commit();
            }
            var crusherItems = level.getCapability(Capabilities.Item.BLOCK, crusher, Direction.UP);
            try (Transaction tx = Transaction.openRoot()) {
                log.append(" itemsIn=").append(crusherItems == null ? "none" : crusherItems.insert(ItemResource.of(Items.IRON_INGOT), 4, tx));
                tx.commit();
            }

            BlockPos reactor = base.west(3);
            level.setBlockAndUpdate(reactor, Nuclear.REACTOR.value().defaultBlockState());
            var water = level.getCapability(Capabilities.Fluid.BLOCK, reactor, Direction.UP);
            try (Transaction tx = Transaction.openRoot()) {
                log.append(" waterIn=").append(water == null ? "none" : water.insert(FluidResource.of(net.minecraft.world.level.material.Fluids.WATER), 2_000, tx));
                tx.commit();
            }
            log.append(" rawUraniumCrushes=").append(CrusherRecipes.resultFor(new net.minecraft.world.item.ItemStack(Nuclear.RAW_URANIUM.value())));
            for (BlockPos pos : new BlockPos[] {base, crusher, reactor}) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        } catch (RuntimeException e) {
            log.append(" FAILED ").append(e);
        }
        com.mojang.logging.LogUtils.getLogger().info(log.toString());
    }
}
