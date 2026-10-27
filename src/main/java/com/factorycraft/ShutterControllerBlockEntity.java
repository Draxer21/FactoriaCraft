package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Industrial shutter: put a row of controllers above a doorway. A redstone signal
 * on any controller of the row rolls the shutter down (steel panels, up to 12 deep,
 * until they reach the floor); without a signal it rolls back up. Panels are
 * armoured: they survive explosions.
 */
public final class ShutterControllerBlockEntity extends MachineBlockEntity {
    public static final int MAX_DEPTH = 12;
    private static final int ROW = 16;
    private int timer;

    public ShutterControllerBlockEntity(BlockPos pos, BlockState state) {
        super(Defense.SHUTTER_BLOCK_ENTITY.value(), pos, state, 0);
    }

    @Override protected boolean isInputSlot(int slot) { return false; }
    @Override protected boolean isOutputSlot(int slot) { return false; }
    @Override protected boolean accepts(int slot, ItemStack stack) { return false; }

    /** Is this controller, or any in the same straight row, powered? */
    boolean rowPowered(Level level) {
        if (level.hasNeighborSignal(worldPosition)) return true;
        Direction facing = getBlockState().getValue(MachineBlock.FACING);
        for (Direction side : new Direction[] {facing.getClockWise(), facing.getCounterClockWise()}) {
            for (int i = 1; i <= ROW; i++) {
                BlockPos p = worldPosition.relative(side, i);
                if (!level.getBlockState(p).is(Defense.SHUTTER.value())) break;
                if (level.hasNeighborSignal(p)) return true;
            }
        }
        return false;
    }

    /** Panels currently hanging below. */
    int depth(Level level) {
        int depth = 0;
        while (depth < MAX_DEPTH && level.getBlockState(worldPosition.below(depth + 1)).is(Defense.SHUTTER_PANEL.value())) depth++;
        return depth;
    }

    @Override
    protected boolean work(Level level) {
        if (++timer < 4) return getBlockState().getValue(MachineBlock.LIT);
        timer = 0;
        boolean closing = rowPowered(level);
        int depth = depth(level);
        Direction facing = getBlockState().getValue(MachineBlock.FACING);
        BlockState panel = Defense.SHUTTER_PANEL.value().defaultBlockState()
                .setValue(ShutterPanelBlock.AXIS, facing.getClockWise().getAxis());
        if (closing && depth < MAX_DEPTH) {
            BlockPos next = worldPosition.below(depth + 1);
            BlockState there = level.getBlockState(next);
            if (there.canBeReplaced() && there.getFluidState().isEmpty() || there.isAir()) {
                level.setBlock(next, panel, Block.UPDATE_ALL);
                if (depth % 3 == 0) level.playSound(null, next, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.6F, 0.6F);
            }
        } else if (!closing && depth > 0) {
            BlockPos last = worldPosition.below(depth);
            level.setBlock(last, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            if (depth % 3 == 0) level.playSound(null, last, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 0.6F, 0.6F);
        }
        return closing;
    }
}
