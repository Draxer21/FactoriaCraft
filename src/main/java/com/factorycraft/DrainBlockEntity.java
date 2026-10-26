package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jspecify.annotations.Nullable;

/**
 * Drain pump: empties all water and lava in its area, from 8 blocks above its
 * level down to 32 below, top down (waterlogged blocks are dried too). Drain a
 * lake before filling it, building in it or quarrying under it.
 */
public final class DrainBlockEntity extends AreaMachineBlockEntity {
    private static final int ABOVE = 8;
    private static final int BELOW = 32;

    public DrainBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.DRAIN_BLOCK_ENTITY.value(), pos, state, 0, 0, new int[] {4, 8, 16, 24}, 8);
    }

    @Override protected int time() { return 1; }
    @Override protected int energyPerTick() { return energyPerBlock(); }
    @Override protected int energyPerBlock() { return 8; }
    @Override protected int perTick() { return 16; }
    @Override protected MenuType<ProcessingMenu> menuType() { return FactoryCraft.DRAIN_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.drain"; }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return false;
    }

    @Override
    protected @Nullable BlockPos findNext(ServerLevel level) {
        long columns = (long) side() * side();
        int topY = Math.min(level.getMaxY() - 1, worldPosition.getY() + ABOVE);
        int bottomY = Math.max(level.getMinY(), worldPosition.getY() - BELOW);
        long total = columns * (topY - bottomY + 1);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int checked = 0; checked < 4096 && cursor < total; checked++) {
            int y = topY - (int) (cursor / columns);
            long column = cursor % columns;
            pos.set(columnX(column), y, columnZ(column));
            if (level.isLoaded(pos) && !level.getFluidState(pos).isEmpty()) return pos.immutable();
            cursor++;
        }
        return cursor >= total ? null : NOT_YET;
    }

    @Override
    protected boolean workAt(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, false), Block.UPDATE_ALL);
        } else if (state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock || state.canBeReplaced()) {
            // Without neighbour updates, so the surrounding water doesn't rush back in mid-drain.
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        } else {
            cursor++; // a fluid in something we can't empty (kelp, a cauldron...): skip it
        }
        return true;
    }
}
