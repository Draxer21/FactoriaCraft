package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/** Geothermal generator: 15 E/t for every lava source block touching it. Clean and endless. */
public final class GeothermalGeneratorBlockEntity extends PassiveGeneratorBlockEntity {
    public static final int PER_LAVA = 15;

    public GeothermalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.GEOTHERMAL_BLOCK_ENTITY.value(), pos, state, 100_000, PER_LAVA * 6);
    }

    @Override protected String nameKey() { return "block.factorycraft.geothermal_generator"; }

    public static int lavaAround(Level level, BlockPos pos) {
        int lava = 0;
        for (Direction dir : Direction.values()) {
            var fluid = level.getFluidState(pos.relative(dir));
            if (fluid.is(Fluids.LAVA) && fluid.isSource()) lava++;
        }
        return lava;
    }

    @Override
    protected int computeRate(Level level) {
        return lavaAround(level, worldPosition) * PER_LAVA;
    }
}
