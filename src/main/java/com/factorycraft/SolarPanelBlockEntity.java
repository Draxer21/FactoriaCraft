package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Advanced solar panels. Tier 2 (advanced): 64 E/t in sunlight. Tier 3 (photon
 * array): 256 E/t by day and still 64 at night, thanks to industrial quartz.
 * Rain cuts the output to 40%, storms to a fifth; both need open sky above.
 */
public final class SolarPanelBlockEntity extends PassiveGeneratorBlockEntity {
    private final int tier;

    private SolarPanelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int tier) {
        super(type, pos, state, tier == 3 ? 2_000_000 : 400_000, tier == 3 ? 256 : 64);
        this.tier = tier;
    }

    public static SolarPanelBlockEntity advanced(BlockPos pos, BlockState state) {
        return new SolarPanelBlockEntity(FactoryCraft.ADVANCED_SOLAR_BLOCK_ENTITY.value(), pos, state, 2);
    }

    public static SolarPanelBlockEntity photon(BlockPos pos, BlockState state) {
        return new SolarPanelBlockEntity(FactoryCraft.PHOTON_ARRAY_BLOCK_ENTITY.value(), pos, state, 3);
    }

    @Override protected String nameKey() { return tier == 3 ? "block.factorycraft.photon_array" : "block.factorycraft.advanced_solar"; }

    @Override
    protected int computeRate(Level level) {
        if (!level.canSeeSky(worldPosition.above())) return 0;
        int day = tier == 3 ? 256 : 64;
        int base = level.isBrightOutside() ? day : (tier == 3 ? 64 : 0);
        if (level.isThundering()) return base / 5;
        return level.isRaining() ? base * 2 / 5 : base;
    }
}
