package com.factorycraft;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Road paver / leveller. While driving forward it clears a 3-wide, 3-high
 * corridor ahead at its own level and lays a road under it: holes, water and
 * uneven ground are replaced by the paving block. The paving block is the
 * first block item in its bay (sneak + right-click to load it, e.g. stone
 * bricks or concrete). With no blocks loaded it just levels.
 */
public final class PaverEntity extends WorkVehicleEntity {
    private static final int HALF_WIDTH = 1;
    private static final int CLEAR_HEIGHT = 3;

    public PaverEntity(EntityType<? extends PaverEntity> type, Level level) {
        super(type, level, () -> FactoryCraft.PAVER_ITEM.value());
    }

    @Override protected double maxSpeed() { return 0.22; }
    @Override protected double reverseSpeed() { return 0.12; }
    @Override protected double seatHeight() { return 1.69; }
    @Override protected double seatForward() { return -0.70; }
    @Override protected double hullShift() { return 0.375; }
    @Override protected double hullHalfLength() { return 2.16; }
    @Override protected double hullHalfWidth() { return 1.41; }

    @Override
    protected void work(ServerLevel level, Player driver) {
        List<Container> sinks = sinks(level);
        int roadY = groundY() - 1;
        double ahead = hullFront() + 0.5;
        for (int side = -halfWidth(); side <= halfWidth(); side++) {
            // Clear obstacles above the road.
            for (int h = 1; h <= CLEAR_HEIGHT; h++) {
                BlockPos pos = columnAt(ahead, side, roadY + h);
                BlockState state = level.getBlockState(pos);
                if (isBreakable(level, pos, state)) breakInto(level, pos, state, driver, sinks);
            }
            // Lay the road surface.
            BlockPos road = columnAt(ahead, side, roadY);
            BlockState current = level.getBlockState(road);
            Block paving = pavingBlock();
            if (paving == null || current.is(paving)) continue;
            boolean replaceable = current.isAir() || !current.getFluidState().isEmpty() || current.canBeReplaced();
            if (!replaceable && !isBreakable(level, road, current)) continue;
            ItemStack used = takeOne(stack -> stack.getItem() instanceof BlockItem item && item.getBlock() == paving);
            if (used.isEmpty()) continue;
            if (!replaceable) breakInto(level, road, current, driver, sinks);
            level.setBlock(road, paving.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /** The first block item in the bay is what the road is made of. */
    private Block pavingBlock() {
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack stack = getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem item
                    && item.getBlock().defaultBlockState().isSolid()) {
                return item.getBlock();
            }
        }
        return null;
    }

    /** Half the work width; the wide-drill module adds a block on each side. */
    private int halfWidth() {
        return engine().wide() ? HALF_WIDTH + 1 : HALF_WIDTH;
    }
}
