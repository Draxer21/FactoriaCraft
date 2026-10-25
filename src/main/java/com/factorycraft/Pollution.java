package com.factorycraft;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Environmental damage, done in the world itself so it's visible and saved:
 *  - SMOG from diesel generators without a chimney filter: grass turns to
 *    coarse dirt, flowers wilt and leaves fall around the generator.
 *  - RADIOACTIVE CONTAMINATION from reactors without shielding, nuclear waste
 *    left on the ground, meltdowns and bombs: the ground turns into
 *    contaminated soil, which makes whoever walks on it sick.
 * Air purifiers clean it up and stop smog around them; contaminated soil also
 * recovers by itself, slowly.
 */
@EventBusSubscriber(modid = FactoryCraft.MOD_ID)
public final class Pollution {
    private Pollution() {}

    /** Working air purifiers: position -> last game tick they ran (per dimension). */
    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> PURIFIERS = new ConcurrentHashMap<>();
    public static final int PURIFIER_RANGE = 12;

    static void purifierActive(Level level, BlockPos pos) {
        PURIFIERS.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>()).put(pos.immutable(), level.getGameTime());
    }

    /** True if a working air purifier covers {@code pos}. */
    public static boolean purified(Level level, BlockPos pos) {
        Map<BlockPos, Long> map = PURIFIERS.get(level.dimension());
        if (map == null) return false;
        long now = level.getGameTime();
        for (Iterator<Map.Entry<BlockPos, Long>> it = map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<BlockPos, Long> entry = it.next();
            if (now - entry.getValue() > 100) {
                it.remove();
                continue;
            }
            if (entry.getKey().distSqr(pos) <= PURIFIER_RANGE * PURIFIER_RANGE) return true;
        }
        return false;
    }

    private static boolean roll(RandomSource random) {
        return FactoryConfig.pollution() && random.nextDouble() < Math.min(1.0, FactoryConfig.pollutionStrength());
    }

    private static BlockPos surface(ServerLevel level, BlockPos center, int radius, RandomSource random) {
        int x = center.getX() + random.nextInt(radius * 2 + 1) - radius;
        int z = center.getZ() + random.nextInt(radius * 2 + 1) - radius;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
        return new BlockPos(x, y, z);
    }

    /** One step of smog damage around a polluting generator. */
    public static void smog(ServerLevel level, BlockPos source) {
        RandomSource random = level.getRandom();
        level.sendParticles(ParticleTypes.LARGE_SMOKE, source.getX() + 0.5, source.getY() + 1.2, source.getZ() + 0.5, 8, 0.2, 0.3, 0.2, 0.02);
        if (purified(level, source) || !roll(random)) return;
        BlockPos pos = surface(level, source, 10, random);
        BlockState state = level.getBlockState(pos);
        if (state.is(BlockTags.LEAVES)) {
            level.destroyBlock(pos, false);
        } else if (state.is(BlockTags.FLOWERS) || state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN)) {
            level.destroyBlock(pos, false);
        } else if (state.is(Blocks.GRASS_BLOCK)) {
            level.setBlock(pos, Blocks.COARSE_DIRT.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /** Turns some ground within {@code radius} into contaminated soil. */
    public static void contaminate(ServerLevel level, BlockPos center, int radius, int count) {
        if (!FactoryConfig.pollution()) return;
        RandomSource random = level.getRandom();
        for (int i = 0; i < count; i++) {
            if (!roll(random)) continue;
            BlockPos pos = surface(level, center, radius, random);
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.DIRT) || state.is(Blocks.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.FARMLAND)) {
                if (purified(level, pos)) continue;
                level.setBlock(pos, Nuclear.CONTAMINATED_SOIL.value().defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    /** Nuclear waste lying on the ground leaks. */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity item) || !(item.level() instanceof ServerLevel level)) return;
        if (item.tickCount % 100 != 0) return;
        if (item.getItem().is(Nuclear.NUCLEAR_WASTE.value())) {
            contaminate(level, item.blockPosition(), 3, 2);
            level.sendParticles(ParticleTypes.GLOW, item.getX(), item.getY() + 0.3, item.getZ(), 4, 0.2, 0.2, 0.2, 0.01);
        }
    }
}
