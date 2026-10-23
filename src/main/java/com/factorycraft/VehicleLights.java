package com.factorycraft;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Headlights for vehicles. Minecraft has no moving light sources, so this keeps a
 * short trail of invisible {@link VehicleLightBlock}s in front of the machine and
 * removes the ones it leaves behind. Server side only; clients light up normally.
 *
 * Lights switch on automatically while someone is driving and it's dark: at night,
 * underground/in caves, or in dimensions without a sky.
 */
public final class VehicleLights {
    /** How often (ticks) the light trail is recomputed. */
    private static final int UPDATE_INTERVAL = 3;
    /** Sky light (after night darkening) below this counts as dark. */
    private static final int DARK_THRESHOLD = 8;

    private final Entity vehicle;
    /** Distances (blocks) in front of the hull where a light is placed. */
    private final double[] beamSteps;
    private final double frontOffset;
    private final double lampHeight;
    private final Set<BlockPos> placed = new HashSet<>();

    /**
     * @param frontOffset distance from the entity position to the front of the hull
     * @param lampHeight  height above the entity's feet where the beam is cast
     * @param beamSteps   distances beyond the front where lights go (the cab light is added at 0)
     */
    public VehicleLights(Entity vehicle, double frontOffset, double lampHeight, double... beamSteps) {
        this.vehicle = vehicle;
        this.frontOffset = frontOffset;
        this.lampHeight = lampHeight;
        this.beamSteps = beamSteps;
    }

    public boolean owns(BlockPos pos) {
        return placed.contains(pos);
    }

    /** Call every tick from the vehicle. {@code heading} is the (possibly tilted) forward direction. */
    public void tick(boolean on, Vec3 heading) {
        if (!(vehicle.level() instanceof ServerLevel level)) return;
        if (vehicle.tickCount % UPDATE_INTERVAL != 0) return;

        Set<BlockPos> wanted = new HashSet<>();
        if (on) {
            Vec3 origin = vehicle.position().add(0.0, lampHeight, 0.0);
            // Cab / work light so the machine itself is visible.
            addLight(level, BlockPos.containing(origin), wanted);
            Vec3 start = origin.add(heading.scale(frontOffset));
            for (double step : beamSteps) {
                BlockPos target = BlockPos.containing(start.add(heading.scale(step)));
                if (!addLight(level, target, wanted)) {
                    // Beam hit solid ground or a wall: light the face right in front of it and stop.
                    break;
                }
            }
        }

        for (BlockPos pos : placed) {
            if (!wanted.contains(pos)) removeLight(level, pos);
        }
        placed.clear();
        placed.addAll(wanted);
    }

    /** Removes every light this vehicle placed (dismounted, destroyed or unloaded). */
    public void clear() {
        if (vehicle.level() instanceof ServerLevel level) {
            for (BlockPos pos : placed) removeLight(level, pos);
        }
        placed.clear();
    }

    /** True at night, underground, or in skyless dimensions. Works on client and server. */
    public boolean isDark(Level level) {
        BlockPos probe = BlockPos.containing(vehicle.position().add(0.0, lampHeight, 0.0));
        int sky = level.getBrightness(LightLayer.SKY, probe) - level.getSkyDarken();
        return sky < DARK_THRESHOLD;
    }

    /** Places (or keeps) a light at {@code pos}. Returns false if the spot is blocked. */
    private boolean addLight(ServerLevel level, BlockPos pos, Set<BlockPos> wanted) {
        if (!level.isLoaded(pos)) return false;
        BlockState state = level.getBlockState(pos);
        Block light = FactoryCraft.VEHICLE_LIGHT.value();
        if (state.is(light)) {
            wanted.add(pos.immutable());
            return true;
        }
        // Only ever take the place of real air, never water or other blocks.
        if (state.is(Blocks.AIR) || state.is(Blocks.CAVE_AIR)) {
            level.setBlock(pos, light.defaultBlockState(), Block.UPDATE_CLIENTS);
            wanted.add(pos.immutable());
            return true;
        }
        return false;
    }

    private static void removeLight(ServerLevel level, BlockPos pos) {
        if (level.isLoaded(pos) && level.getBlockState(pos).is(FactoryCraft.VEHICLE_LIGHT.value())) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Implemented by vehicles that own a {@link VehicleLights}. */
    public interface Holder {
        VehicleLights lights();
    }
}
