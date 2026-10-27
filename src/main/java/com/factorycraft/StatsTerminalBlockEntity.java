package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Statistics terminal: right-click to see what the factory has produced (see {@link ProductionStats}). */
public final class StatsTerminalBlockEntity extends MachineBlockEntity {
    public StatsTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(Economy.STATS_TERMINAL_BLOCK_ENTITY.value(), pos, state, 0);
    }

    @Override protected boolean isInputSlot(int slot) { return false; }
    @Override protected boolean isOutputSlot(int slot) { return false; }
    @Override protected boolean accepts(int slot, ItemStack stack) { return false; }

    @Override
    protected boolean work(Level level) {
        return true;
    }
}
