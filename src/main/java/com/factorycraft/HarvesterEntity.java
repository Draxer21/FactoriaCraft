package com.factorycraft;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Combine harvester. While driving forward over a field it harvests a 5-wide
 * strip ahead: ripe crops (wheat, carrots, potatoes, beetroot... and nether
 * wart) are collected and replanted, melons and pumpkins are picked, and sugar
 * cane, bamboo and cactus are cut leaving the bottom block to regrow.
 * Unripe crops are left alone. The harvest goes to its bay (sneak + right-click),
 * then to a dump truck within range.
 */
public final class HarvesterEntity extends WorkVehicleEntity {
    private static final int HALF_WIDTH = 2;
    private static final int TALL_PLANT_MAX = 5;

    public HarvesterEntity(EntityType<? extends HarvesterEntity> type, Level level) {
        super(type, level, () -> FactoryCraft.HARVESTER_ITEM.value());
    }

    @Override protected double maxSpeed() { return 0.25; }
    @Override protected double reverseSpeed() { return 0.12; }
    @Override protected double seatHeight() { return 2.25; }
    @Override protected double seatForward() { return 0.98; }
    @Override protected double hullShift() { return 0.42; }
    @Override protected double hullHalfLength() { return 2.39; }
    @Override protected double hullHalfWidth() { return 2.25; }

    @Override
    protected void work(ServerLevel level, Player driver) {
        List<Container> sinks = sinks(level);
        int cropY = groundY();
        double ahead = hullFront() + 0.3;
        for (int side = -halfWidth(); side <= halfWidth(); side++) {
            // Crops sit on farmland, which is a bit lower than a full block: check both levels.
            for (int y = cropY - 1; y <= cropY; y++) {
                harvestAt(level, columnAt(ahead, side, y), driver, sinks);
            }
        }
    }

    private void harvestAt(ServerLevel level, BlockPos pos, Player driver, List<Container> sinks) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        if (block instanceof CropBlock crop) {
            if (crop.isMaxAge(state)) {
                collect(level, pos, state, sinks);
                level.setBlock(pos, crop.getStateForAge(0), Block.UPDATE_ALL);
            }
        } else if (block instanceof NetherWartBlock) {
            if (state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE) {
                collect(level, pos, state, sinks);
                level.setBlock(pos, state.setValue(NetherWartBlock.AGE, 0), Block.UPDATE_ALL);
            }
        } else if (state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN)) {
            breakInto(level, pos, state, driver, sinks);
        } else if (state.is(Blocks.SUGAR_CANE) || state.is(Blocks.BAMBOO) || state.is(Blocks.CACTUS)) {
            // Only cut from the second block up, so the plant regrows.
            if (!level.getBlockState(pos.below()).is(block)) {
                for (int h = TALL_PLANT_MAX; h >= 1; h--) {
                    BlockPos part = pos.above(h);
                    BlockState partState = level.getBlockState(part);
                    if (partState.is(block)) breakInto(level, part, partState, driver, sinks);
                }
            }
        }
    }

    /** Stores a ripe plant's drops without breaking it (it's replanted right after). */
    private void collect(ServerLevel level, BlockPos pos, BlockState state, List<Container> sinks) {
        ItemStack tool = engine().tool();
        List<ItemStack> drops = tool.isEmpty() ? Block.getDrops(state, level, pos, null)
                : Block.getDrops(state, level, pos, null, this, tool);
        for (ItemStack drop : drops) {
            ItemStack rest = drop;
            for (Container sink : sinks) {
                if (rest.isEmpty()) break;
                rest = net.minecraft.world.level.block.entity.HopperBlockEntity.addItem(null, sink, rest, null);
            }
            if (!rest.isEmpty()) Block.popResource(level, pos, rest);
        }
        level.levelEvent(2001, pos, Block.getId(state));
    }

    /** Half the work width; the wide-drill module adds a block on each side. */
    private int halfWidth() {
        return engine().wide() ? HALF_WIDTH + 1 : HALF_WIDTH;
    }
}
