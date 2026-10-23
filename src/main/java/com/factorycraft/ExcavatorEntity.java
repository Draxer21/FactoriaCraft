package com.factorycraft;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ExcavatorEntity extends Boat implements VehicleLights.Holder, VehicleEngine.Holder {
    private static final EntityDataAccessor<Boolean> ENGINE_ON =
            SynchedEntityData.defineId(ExcavatorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> FUEL =
            SynchedEntityData.defineId(ExcavatorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> UPGRADES =
            SynchedEntityData.defineId(ExcavatorEntity.class, EntityDataSerializers.INT);
    private final VehicleEngine engine = new VehicleEngine(this, ENGINE_ON, FUEL, UPGRADES);
    private static final int DIG_INTERVAL = 3;
    /** Top forward speed in blocks per tick (0.40 = 8 blocks/s). */
    private static final double MAX_SPEED = 0.40;
    private static final double REVERSE_SPEED = 0.16;
    private static final double ACCELERATION = 0.025;
    private static final double BRAKING = 0.05;

    private static final double SEAT_HEIGHT = 1.52;
    private static final double HULL_FORWARD_SHIFT = 1.51;
    private static final double HULL_HALF_LENGTH = 3.04;
    private static final double HULL_HALF_WIDTH = 1.64;

    private boolean inputForward;
    private boolean inputBack;
    private double driveSpeed;
    /** Headlights: cab light plus a beam reaching ~13 blocks ahead of the bucket. */
    private final VehicleLights lights = new VehicleLights(this, 2.0, 2.0, 2.0, 5.0, 8.0, 11.0);

    public ExcavatorEntity(EntityType<? extends Boat> type, Level level) {
        super(type, level, () -> FactoryCraft.EXCAVATOR_ITEM.value());
    }

    @Override
    protected AABB makeBoundingBox(Vec3 position) {
        // Fitted to the model: tracks reach 1.53 blocks behind the centre, the bucket
        // teeth 4.55 ahead, and the bucket is 3.3 blocks wide.
        double yaw = Math.toRadians(getYRot());
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double halfX = Math.abs(sin) * HULL_HALF_LENGTH + Math.abs(cos) * HULL_HALF_WIDTH;
        double halfZ = Math.abs(cos) * HULL_HALF_LENGTH + Math.abs(sin) * HULL_HALF_WIDTH;
        double centerX = position.x - sin * HULL_FORWARD_SHIFT;
        double centerZ = position.z + cos * HULL_FORWARD_SHIFT;
        return new AABB(centerX - halfX, position.y, centerZ - halfZ,
                centerX + halfX, position.y + getBbHeight(), centerZ + halfZ);
    }

    @Override
    protected double rideHeight(EntityDimensions dimensions) {
        // The model seat is about 1.52 blocks above the ground.
        return SEAT_HEIGHT;
    }

    @Override
    public VehicleLights lights() {
        return lights;
    }

    @Override
    public void remove(RemovalReason reason) {
        lights.clear();
        engine.dropModules(reason);
        super.remove(reason);
    }

    @Override
    public float maxUpStep() {
        // Tracks climb single-block steps instead of getting stuck like a boat.
        return 1.0F;
    }

    @Override
    public void setInput(boolean left, boolean right, boolean up, boolean down) {
        // With the engine off the machine neither drives nor steers.
        boolean on = engine.isRunning();
        super.setInput(left && on, right && on, up && on, down && on);
        this.inputForward = up && on;
        this.inputBack = down && on;
    }

    @Override
    public void move(MoverType type, Vec3 delta) {
        // Replace boat physics (heavy land friction) with a proper engine model on
        // the controlling client: smooth acceleration, braking and reverse.
        if (type == MoverType.SELF && isLocalInstanceAuthoritative() && getControllingPassenger() instanceof Player) {
            double target = inputForward ? MAX_SPEED * engine.speedFactor() : (inputBack ? -REVERSE_SPEED : 0.0);
            double rate = Math.abs(target) > Math.abs(driveSpeed) && Math.signum(target) != -Math.signum(driveSpeed)
                    ? ACCELERATION : BRAKING;
            driveSpeed = approach(driveSpeed, target, rate);
            if (horizontalCollision) driveSpeed *= 0.5;

            float yaw = getYRot() * Mth.DEG_TO_RAD;
            double vx = -Mth.sin(yaw) * driveSpeed;
            double vz = Mth.cos(yaw) * driveSpeed;
            delta = new Vec3(vx, delta.y, vz);
            setDeltaMovement(delta);
        } else if (!(getControllingPassenger() instanceof Player)) {
            driveSpeed = 0.0;
        }
        super.move(type, delta);
    }

    private static double approach(double value, double target, double step) {
        return value < target ? Math.min(value + step, target) : Math.max(value - step, target);
    }

    @Override
    public void tick() {
        super.tick();
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        Player driverNow = getControllingPassenger() instanceof Player p ? p : null;
        engine.tick(driverNow, getPaddleState(0) || getPaddleState(1), null);
        lights.tick(engine.lightsWanted(driverNow != null, lights.isDark(level())), new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw)));
        if (!(getControllingPassenger() instanceof Player driver)) return;

        if (level().isClientSide() || tickCount % DIG_INTERVAL != 0
                || !getPaddleState(0) || !getPaddleState(1)) return;

        Direction forward = getDirection();
        Direction across = forward.getClockWise();
        BlockPos front = blockPosition().relative(forward, 3);
        // Drops go straight into a dump truck parked within range, if there is one.
        DumpTruckEntity truck = DumpTruckEntity.findNear((ServerLevel) level(), position());
        // Eight-wide, six-high, four-deep cut centered on the machine.
        // The wide-drill module makes it twelve wide and eight high.
        int extra = engine.wide() ? 2 : 0;
        for (int depth = 0; depth < 4; depth++) {
            for (int side = -3 - extra; side < 5 + extra; side++) {
                for (int height = 0; height < 6 + extra; height++) {
                    breakBlock(front.relative(forward, depth).relative(across, side).above(height), driver, truck);
                }
            }
        }
    }

    private void breakBlock(BlockPos target, Player driver, DumpTruckEntity truck) {
        BlockState state = level().getBlockState(target);
        if (state.isAir() || state.is(FactoryCraft.VEHICLE_LIGHT.value()) || state.hasBlockEntity() || !state.getFluidState().isEmpty()
                || state.getDestroySpeed(level(), target) < 0) return;
        DumpTruckEntity.harvest((ServerLevel) level(), target, state, driver, truck);
    }

    // ---------------------------------------------------------------- engine

    @Override
    public VehicleEngine engine() {
        return engine;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        VehicleEngine.define(builder, ENGINE_ON, FUEL, UPGRADES);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        InteractionResult handled = engine.interact(player, hand);
        if (handled != null) return handled;
        // Sneak + right-click: the module slots (the excavator has no storage bay).
        if (player.isSecondaryUseActive()) {
            if (!level().isClientSide()) VehicleMenu.open(player, this, new net.minecraft.world.SimpleContainer(0), 0);
            return InteractionResult.SUCCESS;
        }
        return super.interact(player, hand, location);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        engine.save(output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        engine.load(input);
    }

    @Override
    protected net.minecraft.sounds.@org.jspecify.annotations.Nullable SoundEvent getPaddleSound() {
        // Engines and tracks have their own sounds: no boat paddling.
        return null;
    }
}
