package com.factorycraft;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * A nuclear blast far bigger than a vanilla explosion can handle: the crater is carved
 * a slice per tick (no item drops, so the server doesn't drown), everything inside the
 * blast radius is flattened by the shock wave, the rim catches fire and a mushroom
 * cloud rises over the site.
 */
@EventBusSubscriber(modid = FactoryCraft.MOD_ID)
public final class NukeBlast {
    /** Block positions examined per tick; keeps a 60-block crater to a couple of seconds. */
    private static final int CHECKS_PER_TICK = 120_000;
    private static final int CLOUD_TICKS = 200;

    private static final List<NukeBlast> ACTIVE = new ArrayList<>();

    private final ServerLevel level;
    private final double x;
    private final double y;
    private final double z;
    private final int radius;
    private final boolean carve;
    private final long seed;
    private final int side;
    private final long volume;
    private long cursor;
    private int age;

    private NukeBlast(ServerLevel level, double x, double y, double z, int radius, boolean carve) {
        this.level = level;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.carve = carve;
        this.seed = level.getRandom().nextLong();
        this.side = 2 * radius + 1;
        this.volume = (long) side * side * side;
    }

    /** Crater radius in blocks for each core (1-3 enriched uranium, 4 plutonium). */
    public static int radius(int coreGrade) {
        int base = switch (coreGrade) {
            case 1 -> 18;
            case 2 -> 28;
            case 3 -> 40;
            default -> 58;
        };
        return Mth.clamp((int) Math.round(base * FactoryConfig.miniNukePower()), 4, 160);
    }

    public static void detonate(ServerLevel level, Entity bomb, Vec3 at, int coreGrade) {
        int radius = radius(coreGrade);
        NukeBlast blast = new NukeBlast(level, at.x, at.y, at.z, radius, FactoryConfig.weaponsBreakBlocks());
        ACTIVE.add(blast);
        blast.flash();
        blast.shockWave(bomb);
        NuclearReactorBlockEntity.fallout(level, at.x, at.y, at.z, radius * 0.8F, 600 + 300 * coreGrade);
    }

    // ---------------------------------------------------------------- the first instant

    /** Sound heard far away, a wall of explosion particles. */
    private void flash() {
        for (ServerPlayer player : level.players()) {
            double distance = Math.sqrt(player.distanceToSqr(x, y, z));
            if (distance > radius * 12) continue;
            // Played at the player so the boom carries over any distance, quieter far away.
            float volume = (float) Mth.clamp(4.0 - distance / (radius * 3.0), 0.6, 4.0);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.BLOCKS, volume, 0.35F);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                    SoundSource.WEATHER, volume, 0.5F);
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, true, true, x, y, z,
                Math.min(80, radius * 2), radius * 0.5, radius * 0.3, radius * 0.5, 0.0);
        level.sendParticles(ParticleTypes.FLASH, true, true, x, y + 2, z, 1, 0, 0, 0, 0);
    }

    /** Everything within twice the radius is hurt and thrown outwards; walls don't shelter. */
    private void shockWave(Entity bomb) {
        double reach = radius * 2.0;
        AABB area = new AABB(x - reach, y - reach, z - reach, x + reach, y + reach, z + reach);
        DamageSource source = level.damageSources().explosion(bomb, bomb instanceof MiniNukeEntity nuke ? nuke.getOwner() : null);
        for (Entity entity : level.getEntities(bomb, area)) {
            if (entity instanceof VehiclePart) continue; // the vehicle itself gets hit
            double distance = Math.sqrt(entity.distanceToSqr(x, y, z));
            if (distance > reach) continue;
            double strength = 1.0 - distance / reach;
            entity.hurtServer(level, source, (float) (20.0 + 380.0 * strength * strength));
            entity.igniteForSeconds((float) (4 + 10 * strength));
            Vec3 away = entity.position().subtract(x, y, z);
            if (away.lengthSqr() < 1.0E-4) away = new Vec3(0, 1, 0);
            Vec3 push = away.normalize().scale(3.0 * strength).add(0, 0.8 * strength, 0);
            entity.push(push.x, push.y, push.z);
            entity.hurtMarked = true;
            if (entity instanceof LivingEntity living && strength > 0.3) {
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.BLINDNESS, 100, 0));
            }
        }
    }

    // ---------------------------------------------------------------- ticking

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) return;
        Iterator<NukeBlast> it = ACTIVE.iterator();
        while (it.hasNext()) {
            NukeBlast blast = it.next();
            if (!blast.tick()) it.remove();
        }
    }

    /** One step; false when done. */
    private boolean tick() {
        age++;
        mushroomCloud();
        if (carve && cursor < volume) {
            carveSlice();
            if (cursor >= volume) burnRim();
        }
        return age < CLOUD_TICKS || (carve && cursor < volume);
    }

    /** The crater: a rough sphere centred a little above the impact, so it bites deep and wide. */
    private void carveSlice() {
        double centerY = y + radius * 0.25;
        int ox = Mth.floor(x) - radius;
        int oy = Mth.floor(centerY) - radius;
        int oz = Mth.floor(z) - radius;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        long end = Math.min(volume, cursor + CHECKS_PER_TICK);
        for (; cursor < end; cursor++) {
            int dx = (int) (cursor % side);
            int dz = (int) ((cursor / side) % side);
            int dy = (int) (cursor / ((long) side * side));
            // Top-down, so falling blocks have nothing left to land on.
            int py = oy + (side - 1 - dy);
            int px = ox + dx;
            int pz = oz + dz;
            double rx = px + 0.5 - x;
            double ry = py + 0.5 - centerY;
            double rz = pz + 0.5 - z;
            double r = radius * (0.88 + 0.12 * noise(px >> 2, py >> 2, pz >> 2));
            double d2 = rx * rx + ry * ry * 1.3 + rz * rz;
            if (d2 > r * r) {
                // Shock zone just outside the crater: flimsy blocks (leaves, glass, plants, wood) go too.
                if (d2 < r * r * 2.6) shatter(pos.set(px, py, pz));
                continue;
            }
            if (py < level.getMinY() || py >= level.getMaxY()) continue;
            pos.set(px, py, pz);
            if (!level.isLoaded(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.getDestroySpeed(level, pos) < 0) continue;
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    private void shatter(BlockPos pos) {
        if (pos.getY() < level.getMinY() || pos.getY() >= level.getMaxY() || !level.isLoaded(pos)) return;
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness >= 0 && hardness <= 2.0F && state.getFluidState().isEmpty()) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    /** Fires all around the crater's rim. */
    private void burnRim() {
        var random = level.getRandom();
        int fires = radius * 10;
        for (int i = 0; i < fires; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = radius * (0.9 + random.nextDouble() * 0.8);
            int px = Mth.floor(x + Math.cos(angle) * distance);
            int pz = Mth.floor(z + Math.sin(angle) * distance);
            BlockPos ground = new BlockPos(px, 0, pz);
            if (!level.isLoaded(ground)) continue;
            int py = level.getHeight(Heightmap.Types.MOTION_BLOCKING, px, pz);
            BlockPos at = new BlockPos(px, py, pz);
            if (level.getBlockState(at).isAir() && level.getBlockState(at.below()).isFaceSturdy(level, at.below(), net.minecraft.core.Direction.UP)) {
                level.setBlock(at, Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        Pollution.contaminate(level, BlockPos.containing(x, y, z), radius * 2, radius * 12);
    }

    /** A rising column with a spreading cap, visible from far away. */
    private void mushroomCloud() {
        if (age > CLOUD_TICKS || age % 2 != 0) return;
        double rise = Math.min(1.0, age / 80.0);
        double top = y + radius * (0.6 + 1.6 * rise);
        double stem = radius * 0.18;
        for (double h = y; h < top; h += Math.max(2.0, radius / 10.0)) {
            level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, true, x, h, z, 2, stem, 1.0, stem, 0.01);
        }
        double cap = radius * (0.4 + 0.8 * rise);
        level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, true, x, top, z, 12, cap, radius * 0.12, cap, 0.02);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, true, true, x, top, z, 10, cap, radius * 0.1, cap, 0.05);
        if (age < 40) {
            level.sendParticles(ParticleTypes.LAVA, true, true, x, y + radius * 0.3, z, 20, radius * 0.4, radius * 0.2, radius * 0.4, 0.2);
            level.sendParticles(ParticleTypes.EXPLOSION, true, true, x, y, z, 10, radius * 0.7, radius * 0.3, radius * 0.7, 0.0);
        }
    }

    /** Smooth-ish value noise in [-1, 1] so the crater's edge isn't a perfect ball. */
    private double noise(int ix, int iy, int iz) {
        long h = seed ^ (ix * 0x9E3779B97F4A7C15L) ^ (iy * 0xC2B2AE3D27D4EB4FL) ^ (iz * 0x165667B19E3779F9L);
        h = (h ^ (h >>> 31)) * 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return ((h & 0xFFFF) / 32767.5) - 1.0;
    }
}
