package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/**
 * Lift pad: a people elevator. Put pads on each floor in one column; stand on a
 * pad and jump to go up to the next pad, sneak to go down (up to 64 blocks apart).
 */
public final class LiftPadBlock extends Block {
    public static final int RANGE = 64;

    public LiftPadBlock(Properties properties) {
        super(properties);
    }

    /** The next pad above (or below) with room to stand on it, or null. */
    static @Nullable BlockPos next(Level level, BlockPos pad, boolean up) {
        for (int i = 2; i <= RANGE; i++) {
            BlockPos p = up ? pad.above(i) : pad.below(i);
            if (p.getY() < level.getMinY() || p.getY() >= level.getMaxY()) return null;
            if (!level.getBlockState(p).is(Defense.LIFT_PAD.value())) continue;
            if (level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()
                    && level.getBlockState(p.above(2)).getCollisionShape(level, p.above(2)).isEmpty()) return p;
        }
        return null;
    }
}
