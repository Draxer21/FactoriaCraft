package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;

/**
 * Hydro turbine: water moving past it makes power. Falling water landing on top
 * gives the most (40 E/t); flowing water beside it 15 E/t per side; still water
 * hardly anything. Stack turbines down a waterfall or line a river with them.
 */
public final class HydroTurbineBlockEntity extends PassiveGeneratorBlockEntity {
    public static final int MAX = 100;

    public HydroTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.HYDRO_TURBINE_BLOCK_ENTITY.value(), pos, state, 200_000, MAX);
    }

    @Override protected String nameKey() { return "block.factorycraft.hydro_turbine"; }

    @Override
    protected int computeRate(Level level) {
        int rate = 0;
        FluidState above = level.getFluidState(worldPosition.above());
        if (above.is(FluidTags.WATER) && (above.getValueOrElse(FlowingFluid.FALLING, false) || !above.isSource())) rate += 40;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            FluidState fluid = level.getFluidState(worldPosition.relative(side));
            if (!fluid.is(FluidTags.WATER)) continue;
            rate += fluid.isSource() ? 2 : 15;
        }
        return Math.min(MAX, rate);
    }
}
