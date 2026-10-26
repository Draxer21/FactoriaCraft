package com.factorycraft;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * Crude oil lies under some chunks of the overworld: each chunk has a fixed
 * richness from 0 to 100% (about 4 in 10 chunks have none), decided by the
 * world seed so it's the same every time you look.
 */
public final class OilDeposits {
    private OilDeposits() {}

    public static int richness(Level level, ChunkPos chunk) {
        if (!(level instanceof ServerLevel server) || level.dimension() != Level.OVERWORLD) return 0;
        long h = server.getSeed() ^ (chunk.x() * 0x9E3779B97F4A7C15L) ^ (chunk.z() * 0xC2B2AE3D27D4EB4FL) ^ 0x6F696CL;
        h = (h ^ (h >>> 33)) * 0xFF51AFD7ED558CCDL;
        h ^= h >>> 29;
        int roll = (int) Math.floorMod(h, 100L);
        return roll < 40 ? 0 : Math.min(100, (roll - 40) * 100 / 60 + 10);
    }
}
