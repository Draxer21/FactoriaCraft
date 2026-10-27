package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Security camera: looks the way it was placed. Watched from a monitor (see {@link MonitorBlockEntity}). */
public final class SecurityCameraBlockEntity extends MachineBlockEntity {
    public SecurityCameraBlockEntity(BlockPos pos, BlockState state) {
        super(Defense.CAMERA_BLOCK_ENTITY.value(), pos, state, 0);
    }

    @Override protected boolean isInputSlot(int slot) { return false; }
    @Override protected boolean isOutputSlot(int slot) { return false; }
    @Override protected boolean accepts(int slot, ItemStack stack) { return false; }

    @Override
    protected boolean work(Level level) {
        return true;
    }
}
