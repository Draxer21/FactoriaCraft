package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Autopilot (needs the autopilot module): once the driver gets out, the
 * vehicle follows the route beacons 1 -> 2 -> 3 ... and loops back to 1,
 * waiting 10 s at stop beacons. Getting back in takes control again.
 */
public final class Autopilot {
    private static final double RANGE = 128.0;
    private static final int START_DELAY = 60;
    private static final int STOP_TIME = 200;

    private int target = -1;
    private int wait;
    private int idle;
    private int leaveCooldown;
    /** Speed the vehicle should drive at this tick (blocks/tick, forward). */
    public double speed;

    private void reset() {
        target = -1;
        wait = 0;
        idle = 0;
        speed = 0.0;
    }

    /**
     * Server side, every tick while nobody drives. Steers the vehicle (unless
     * {@code steer} is false, e.g. a locomotive on rails) and sets {@link #speed}.
     * Returns true while the autopilot is in control.
     */
    public boolean drive(Entity vehicle, VehicleEngine engine, double maxSpeed, boolean steer) {
        if (!(vehicle.level() instanceof ServerLevel level) || vehicle.getControllingPassenger() != null
                || !engine.has(Upgrade.AUTOPILOT)) {
            reset();
            return false;
        }
        if (++idle < START_DELAY) return false;
        speed = 0.0;
        if (!engine.isRunning() && !engine.autopilotStart()) return false;
        if (wait > 0) {
            wait--;
            return true;
        }
        if (leaveCooldown > 0) leaveCooldown--;
        BlockPos from = vehicle.blockPosition();

        if (!steer) {
            // On rails: just drive, and stop at stop beacons along the track.
            BlockPos stop = RouteBeaconBlockEntity.find(level, from, -1, 2.5);
            if (stop != null && leaveCooldown == 0 && level.getBlockState(stop).getValue(RouteBeaconBlock.STOP)) {
                wait = STOP_TIME;
                leaveCooldown = 80;
                return true;
            }
            speed = maxSpeed * 0.8;
            return true;
        }

        if (target <= 0) {
            BlockPos nearest = RouteBeaconBlockEntity.find(level, from, -1, RANGE);
            if (nearest == null) return true;
            target = level.getBlockState(nearest).getValue(RouteBeaconBlock.INDEX);
        }
        BlockPos beacon = RouteBeaconBlockEntity.find(level, from, target, RANGE);
        if (beacon == null) {
            target = target == 1 ? -1 : 1;
            return true;
        }
        double dx = beacon.getX() + 0.5 - vehicle.getX();
        double dz = beacon.getZ() + 0.5 - vehicle.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 2.5) {
            BlockState state = level.getBlockState(beacon);
            if (state.getValue(RouteBeaconBlock.STOP)) wait = STOP_TIME;
            int next = target + 1;
            target = RouteBeaconBlockEntity.find(level, from, next, RANGE) != null ? next : 1;
            return true;
        }
        float yaw = (float) (Mth.atan2(-dx, dz) * Mth.RAD_TO_DEG);
        float diff = Math.abs(Mth.degreesDifference(vehicle.getYRot(), yaw));
        vehicle.setYRot(Mth.approachDegrees(vehicle.getYRot(), yaw, 5.0F));
        speed = maxSpeed * (diff < 30.0F ? 0.8 : 0.25);
        return true;
    }
}
