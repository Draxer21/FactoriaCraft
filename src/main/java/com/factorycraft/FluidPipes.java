package com.factorycraft;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/** Sends fluid from a machine through the fluid pipes to the first tank or machine that takes it all. */
public final class FluidPipes {
    public static final int MAX_PIPES = 256;

    private FluidPipes() {}

    /**
     * Offers {@code amount} of {@code fluid} to the fluid handlers next to
     * {@code origin} and along the pipes connected to it. True if one took it all.
     */
    public static boolean deliver(Level level, BlockPos origin, Fluid fluid, int amount) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(origin);
        queue.add(origin);
        int pipes = 0;
        while (!queue.isEmpty() && pipes < MAX_PIPES) {
            BlockPos pos = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (!seen.add(next)) continue;
                if (level.getBlockState(next).getBlock() instanceof FluidPipeBlock) {
                    pipes++;
                    queue.add(next);
                } else if (level.getBlockEntity(next) != null && FluidStorage.offer(level, next, dir.getOpposite(), fluid, amount)) {
                    return true;
                }
            }
        }
        return false;
    }
}
