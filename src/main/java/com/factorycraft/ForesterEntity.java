package com.factorycraft;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Forestry harvester. Drive it into a forest: every tree whose trunk is in the
 * 3-wide strip ahead is felled whole (logs and natural leaves, so you also get
 * saplings, sticks and apples), the wood goes to the bay, and a sapling from the
 * bay is planted where the trunk stood. Player-placed leaves and log buildings
 * touching no natural leaves are left alone.
 */
public final class ForesterEntity extends WorkVehicleEntity {
    private static final int HALF_WIDTH = 1;
    /** Safety cap for one tree (big jungle and dark oak trees fit easily). */
    private static final int MAX_TREE_BLOCKS = 600;
    private static final int TRUNK_SCAN_HEIGHT = 3;

    public ForesterEntity(EntityType<? extends ForesterEntity> type, Level level) {
        super(type, level, () -> FactoryCraft.FORESTER_ITEM.value());
    }

    @Override protected double maxSpeed() { return 0.24; }
    @Override protected double reverseSpeed() { return 0.12; }
    @Override protected double seatHeight() { return 1.69; }
    @Override protected double seatForward() { return -0.98; }
    @Override protected double hullShift() { return 0.52; }
    @Override protected double hullHalfLength() { return 2.77; }
    @Override protected double hullHalfWidth() { return 1.6; }
    @Override protected int workInterval() { return 4; }

    @Override
    protected void work(ServerLevel level, Player driver) {
        int ground = groundY();
        double ahead = hullFront() + 0.5;
        int half = engine().wide() ? HALF_WIDTH + 1 : HALF_WIDTH;
        for (int side = -half; side <= half; side++) {
            for (int h = 0; h < TRUNK_SCAN_HEIGHT; h++) {
                BlockPos pos = columnAt(ahead, side, ground + h);
                if (level.getBlockState(pos).is(BlockTags.LOGS)) {
                    fell(level, pos, driver);
                    break;
                }
            }
        }
    }

    /** Fells the tree that contains {@code start}, if it's a natural tree. */
    private void fell(ServerLevel level, BlockPos start, Player driver) {
        Set<BlockPos> logs = new HashSet<>();
        Set<BlockPos> leaves = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        logs.add(start);
        while (!queue.isEmpty() && logs.size() + leaves.size() < MAX_TREE_BLOCKS) {
            BlockPos pos = queue.poll();
            boolean fromLog = logs.contains(pos);
            for (BlockPos next : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                if (logs.contains(next) || leaves.contains(next)) continue;
                BlockState state = level.getBlockState(next);
                if (fromLog && state.is(BlockTags.LOGS) && next.getY() >= start.getY() - 1) {
                    BlockPos immutable = next.immutable();
                    logs.add(immutable);
                    queue.add(immutable);
                } else if (state.is(BlockTags.LEAVES) && !state.getValue(LeavesBlock.PERSISTENT)) {
                    BlockPos immutable = next.immutable();
                    leaves.add(immutable);
                    // Follow leaves a little so branches through the canopy are found.
                    if (fromLog) queue.add(immutable);
                }
            }
        }
        // No natural leaves at all: it's a building, not a tree.
        if (leaves.isEmpty()) return;

        List<Container> sinks = sinks(level);
        BlockPos stump = lowest(logs);
        for (BlockPos pos : logs) breakInto(level, pos, level.getBlockState(pos), driver, sinks);
        for (BlockPos pos : leaves) breakInto(level, pos, level.getBlockState(pos), driver, sinks);
        replant(level, stump);
    }

    private static BlockPos lowest(Set<BlockPos> blocks) {
        BlockPos best = null;
        for (BlockPos pos : blocks) {
            if (best == null || pos.getY() < best.getY()) best = pos;
        }
        return best;
    }

    private void replant(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir()) return;
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty() || !stack.is(ItemTags.SAPLINGS) || !(stack.getItem() instanceof BlockItem item)) continue;
            BlockState sapling = item.getBlock().defaultBlockState();
            if (!sapling.canSurvive(level, pos)) continue;
            level.setBlock(pos, sapling, Block.UPDATE_ALL);
            stack.shrink(1);
            setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
            return;
        }
    }
}
