package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Windmill: the higher and more open, the more it makes. Nothing above Y 64,
 * then 1 E/t per 2 blocks of height up to 50 E/t; needs open air around its
 * top (blocks nearby steal the wind). Rain x1.5, storms x2.
 */
public final class WindmillBlockEntity extends PassiveGeneratorBlockEntity {
    public static final int MAX = 50;

    public WindmillBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.WINDMILL_BLOCK_ENTITY.value(), pos, state, 100_000, MAX * 2);
    }

    @Override protected String nameKey() { return "block.factorycraft.windmill"; }
    @Override protected int recomputeEvery() { return 100; }

    /** Fraction (0-1) of open air in a 5x5x3 box around the top. */
    public static float openness(Level level, BlockPos pos) {
        int open = 0;
        int total = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, 0, -2), pos.offset(2, 2, 2))) {
            if (p.getX() == pos.getX() && p.getZ() == pos.getZ() && p.getY() == pos.getY()) continue;
            total++;
            if (level.getBlockState(p).isAir()) open++;
        }
        return total == 0 ? 0.0F : (float) open / total;
    }

    @Override
    protected int computeRate(Level level) {
        int height = Math.max(0, worldPosition.getY() - 64);
        float base = Math.min(MAX, height / 2.0F);
        float open = openness(level, worldPosition);
        if (open < 0.5F) return 0;
        float weather = level.isThundering() ? 2.0F : level.isRaining() ? 1.5F : 1.0F;
        return Math.round(base * open * weather);
    }
}
