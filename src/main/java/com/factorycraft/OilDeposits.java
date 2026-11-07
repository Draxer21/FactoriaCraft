package com.factorycraft;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * Crude oil lies under some chunks of the overworld: each chunk has a fixed
 * richness from 0 to 100% (about 4 in 10 chunks have none), decided by the
 * world seed so it's the same every time you look. Under the sea there is
 * always oil, and richer; a pumpjack below Y 0 reaches the deep layer, which
 * is richer still and has oil even under most dry chunks.
 */
public final class OilDeposits {
    private OilDeposits() {}

    /** Richness for a pumpjack at height {@code y} in that chunk (sea and deep bonuses). */
    public static int richness(Level level, ChunkPos chunk, int y) {
        int base = richness(level, chunk);
        if (!(level instanceof ServerLevel server) || level.dimension() != Level.OVERWORLD) return 0;
        net.minecraft.core.BlockPos middle = new net.minecraft.core.BlockPos((chunk.x() << 4) + 8, 62, (chunk.z() << 4) + 8);
        if (server.getBiome(middle).is(net.minecraft.tags.BiomeTags.IS_OCEAN)) base = Math.min(100, Math.max(base, 30) + 30);
        if (y < 0) base = base == 0 ? (deepRoll(server, chunk) < 70 ? 25 : 0) : Math.min(100, base + 25);
        return base;
    }

    private static int deepRoll(ServerLevel server, ChunkPos chunk) {
        long h = server.getSeed() ^ (chunk.x() * 0xC2B2AE3D27D4EB4FL) ^ (chunk.z() * 0x9E3779B97F4A7C15L) ^ 0x64656570L;
        h = (h ^ (h >>> 31)) * 0xBF58476D1CE4E5B9L;
        h ^= h >>> 27;
        return (int) Math.floorMod(h, 100L);
    }

    public static int richness(Level level, ChunkPos chunk) {
        if (!(level instanceof ServerLevel server) || level.dimension() != Level.OVERWORLD) return 0;
        long h = server.getSeed() ^ (chunk.x() * 0x9E3779B97F4A7C15L) ^ (chunk.z() * 0xC2B2AE3D27D4EB4FL) ^ 0x6F696CL;
        h = (h ^ (h >>> 33)) * 0xFF51AFD7ED558CCDL;
        h ^= h >>> 29;
        int roll = (int) Math.floorMod(h, 100L);
        return roll < 40 ? 0 : Math.min(100, (roll - 40) * 100 / 60 + 10);
    }
}
