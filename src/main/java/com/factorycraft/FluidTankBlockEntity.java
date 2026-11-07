package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Fluid tank: 32 buckets of any one fluid (water, lava, or other mods' oils and fuels).
 * Fill and empty it with buckets, our pump and pipes, or any mod's pipes. Stacked tanks
 * drain into the one below, so a column of tanks works as one big tank.
 */
public final class FluidTankBlockEntity extends MachineBlockEntity implements FluidStorage.Holder {
    public static final int CAPACITY = 32 * FluidStorage.BUCKET;
    private final FluidStorage fluids = new FluidStorage(this::setChanged).slot(CAPACITY, true, true, FluidStorage::any);

    public FluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.FLUID_TANK_BLOCK_ENTITY.value(), pos, state, 0);
    }

    @Override public FluidStorage fluids() { return fluids; }
    @Override protected boolean isInputSlot(int slot) { return false; }
    @Override protected boolean isOutputSlot(int slot) { return false; }
    @Override protected boolean accepts(int slot, ItemStack stack) { return false; }

    public void click(Player player) {
        player.sendOverlayMessage(fluids.describe(0));
    }

    /** Keeps the gauge (block state) in step with the contents. */
    private void showLevel(Level level) {
        BlockState state = level.getBlockState(worldPosition);
        if (!state.hasProperty(FluidTankBlock.FILL)) return;
        int amount = fluids.amount(0);
        int fill = amount <= 0 ? 0 : Math.max(1, (int) Math.round(amount * 8.0 / CAPACITY));
        FluidTankBlock.Content content = amount <= 0 ? FluidTankBlock.Content.EMPTY : FluidTankBlock.Content.of(fluids.fluid(0));
        if (state.getValue(FluidTankBlock.FILL) != fill || state.getValue(FluidTankBlock.CONTENT) != content) {
            level.setBlock(worldPosition, state.setValue(FluidTankBlock.FILL, fill).setValue(FluidTankBlock.CONTENT, content), 2);
        }
    }

    void showLevelForTest(Level level) {
        showLevel(level);
    }

    @Override
    protected boolean work(Level level) {
        if (level.getGameTime() % 10 == 5) showLevel(level);
        if (level.getGameTime() % 10 == 0 && fluids.amount(0) > 0
                && level.getBlockEntity(worldPosition.below()) instanceof FluidTankBlockEntity) {
            fluids.pushTo(level, worldPosition, Direction.DOWN, 0, 4 * FluidStorage.BUCKET);
        }
        return fluids.amount(0) > 0;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fluids.load(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        fluids.save(output);
    }
}
