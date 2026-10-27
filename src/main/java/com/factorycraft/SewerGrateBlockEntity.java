package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * Sewer grate: set it into a street or a yard. Water standing on it drains away,
 * rain falling on it is collected, and rubbish dropped on it falls through.
 * The water goes into the tank (or any mod's fluid handler) below; the items
 * into the chest, hopper or pipe below. With nothing below, the water just
 * disappears down the drain.
 */
public final class SewerGrateBlockEntity extends MachineBlockEntity implements FluidStorage.Holder {
    private static final int CAPACITY = 4 * FluidStorage.BUCKET;
    private static final int RAIN_PER_SECOND = 50;
    private final FluidStorage fluids = new FluidStorage(this::setChanged).slot(CAPACITY, false, true, FluidStorage::water);

    public SewerGrateBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.SEWER_GRATE_BLOCK_ENTITY.value(), pos, state, 0);
    }

    @Override public FluidStorage fluids() { return fluids; }
    @Override protected boolean isInputSlot(int slot) { return false; }
    @Override protected boolean isOutputSlot(int slot) { return false; }
    @Override protected boolean accepts(int slot, ItemStack stack) { return false; }

    @Override
    protected boolean work(Level level) {
        if (level.getGameTime() % 20 != 0) return getBlockState().getValue(MachineBlock.LIT);
        boolean busy = false;
        BlockPos above = worldPosition.above();
        if (level.isRainingAt(above)) {
            fluids.add(0, net.minecraft.world.level.material.Fluids.WATER, RAIN_PER_SECOND);
            busy = true;
        }
        // Water on top drains away (sources only; flowing water dries up by itself).
        FluidState fluid = level.getFluidState(above);
        if (fluid.isSource() && FluidStorage.water(fluid.getType()) && fluids.space(0) >= FluidStorage.BUCKET) {
            BlockState state = level.getBlockState(above);
            if (state.getBlock() instanceof LiquidBlock) {
                level.setBlock(above, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                fluids.add(0, fluid.getType(), FluidStorage.BUCKET);
                busy = true;
            } else if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
                level.setBlock(above, state.setValue(BlockStateProperties.WATERLOGGED, false), Block.UPDATE_ALL);
                fluids.add(0, fluid.getType(), FluidStorage.BUCKET);
                busy = true;
            }
        }
        // Rubbish falls through into a container below.
        Container below = HopperBlockEntity.getContainerAt(level, worldPosition.below());
        if (below != null) {
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(above).inflate(0.0, 0.2, 0.0), ItemEntity::isAlive)) {
                ItemStack rest = HopperBlockEntity.addItem(null, below, item.getItem().copy(), Direction.UP);
                if (rest.getCount() == item.getItem().getCount()) continue;
                if (rest.isEmpty()) item.discard();
                else item.setItem(rest);
                busy = true;
            }
        }
        // Water goes down into a tank or another mod's pipe; with nowhere to go, it's lost to the drain.
        if (fluids.amount(0) > 0 && fluids.pushTo(level, worldPosition, Direction.DOWN, 0, CAPACITY) == 0
                && level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, worldPosition.below(), Direction.UP) == null) {
            fluids.take(0, CAPACITY);
        }
        return busy;
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
