package com.factorycraft;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.EntityDataAccessor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractChestBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Shared base for driveable work machines (paver, harvester):
 * engine-style driving, a 27-slot bay (sneak + right-click), headlights,
 * a hitbox fitted to the model, and a work step that runs while driving forward.
 * Whatever the bay can't hold goes to a dump truck within range, or drops.
 */
public abstract class WorkVehicleEntity extends AbstractChestBoat implements VehicleLights.Holder, VehicleEngine.Holder {
    private static final EntityDataAccessor<Boolean> ENGINE_ON =
            SynchedEntityData.defineId(WorkVehicleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> FUEL =
            SynchedEntityData.defineId(WorkVehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> UPGRADES =
            SynchedEntityData.defineId(WorkVehicleEntity.class, EntityDataSerializers.INT);
    private final VehicleEngine engine = new VehicleEngine(this, ENGINE_ON, FUEL, UPGRADES);
    private static final double ACCELERATION = 0.025;
    private static final double BRAKING = 0.05;

    private boolean inputForward;
    private boolean inputBack;
    private double driveSpeed;
    private final VehicleLights lights;
    private final Autopilot autopilot = new Autopilot();

    /** Whether the autopilot should steer (a locomotive on rails steers itself). */
    protected boolean autopilotSteers() {
        return true;
    }

    // Client-side animation.
    private float travelled;
    private float travelledO;

    protected WorkVehicleEntity(EntityType<? extends WorkVehicleEntity> type, Level level, java.util.function.Supplier<Item> drop) {
        super(type, level, drop);
        this.lights = new VehicleLights(this, hullFront(), 1.4, 1.5, 4.5, 7.5, 10.5);
    }

    // ---------------------------------------------------------------- per machine

    protected abstract double maxSpeed();

    protected abstract double reverseSpeed();

    /** Seat cushion height and forward offset (blocks) for the driver. */
    protected abstract double seatHeight();

    protected abstract double seatForward();

    /** Hitbox: centre shift forward, half length, half width (blocks). Must not read fields. */
    protected abstract double hullShift();

    protected abstract double hullHalfLength();

    protected abstract double hullHalfWidth();

    /** Ticks between work steps while driving forward. */
    protected int workInterval() {
        return 2;
    }

    /** Does the machine's job on the strip ahead (server side, only while driving forward). */
    protected abstract void work(ServerLevel level, Player driver);

    // ---------------------------------------------------------------- geometry

    protected final double hullFront() {
        return hullShift() + hullHalfLength();
    }

    protected Vec3 forward() {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
    }

    /** Unit vector pointing to the machine's left. */
    protected Vec3 left() {
        Vec3 f = forward();
        return new Vec3(f.z, 0.0, -f.x);
    }

    /** Block position {@code ahead} blocks in front of the centre and {@code side} blocks to the left, at height {@code y}. */
    protected BlockPos columnAt(double ahead, double side, int y) {
        Vec3 p = position().add(forward().scale(ahead)).add(left().scale(side));
        return BlockPos.containing(p.x, y + 0.5, p.z);
    }

    /** The block level the machine is standing on (its wheels' block). */
    protected int groundY() {
        return Mth.floor(getY() + 0.5);
    }

    @Override
    protected double rideHeight(EntityDimensions dimensions) {
        return seatHeight();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0.0, seatHeight(), seatForward()).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    protected AABB makeBoundingBox(Vec3 position) {
        double yaw = Math.toRadians(getYRot());
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double halfX = Math.abs(sin) * hullHalfLength() + Math.abs(cos) * hullHalfWidth();
        double halfZ = Math.abs(cos) * hullHalfLength() + Math.abs(sin) * hullHalfWidth();
        double centerX = position.x - sin * hullShift();
        double centerZ = position.z + cos * hullShift();
        return new AABB(centerX - halfX, position.y, centerZ - halfZ,
                centerX + halfX, position.y + getBbHeight(), centerZ + halfZ);
    }

    @Override
    public float maxUpStep() {
        return 1.0F;
    }

    // ---------------------------------------------------------------- driving

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
        if (type == MoverType.SELF && isLocalInstanceAuthoritative() && getControllingPassenger() instanceof Player) {
            double target = inputForward ? maxSpeed() * engine.speedFactor() : (inputBack ? -reverseSpeed() : 0.0);
            boolean speedingUp = Math.abs(target) > Math.abs(driveSpeed)
                    && (driveSpeed == 0.0 || Math.signum(target) == Math.signum(driveSpeed));
            driveSpeed = approach(driveSpeed, target, speedingUp ? ACCELERATION : BRAKING);
            if (horizontalCollision) driveSpeed *= 0.5;
            Vec3 forward = forward();
            delta = new Vec3(forward.x * driveSpeed, delta.y, forward.z * driveSpeed);
            setDeltaMovement(delta);
        } else if (type == MoverType.SELF && !level().isClientSide()
                && autopilot.drive(this, engine, maxSpeed() * engine.speedFactor(), autopilotSteers())) {
            driveSpeed = approach(driveSpeed, autopilot.speed, ACCELERATION);
            if (horizontalCollision) driveSpeed *= 0.5;
            Vec3 forward = forward();
            delta = new Vec3(forward.x * driveSpeed, delta.y, forward.z * driveSpeed);
            setDeltaMovement(delta);
        } else if (!(getControllingPassenger() instanceof Player)) {
            driveSpeed = 0.0;
        }
        super.move(type, delta);
    }

    private static double approach(double value, double target, double step) {
        return value < target ? Math.min(value + step, target) : Math.max(value - step, target);
    }

    // ---------------------------------------------------------------- ticking

    @Override
    public void tick() {
        super.tick();
        Player driver = getControllingPassenger() instanceof Player p ? p : null;
        engine.tick(driver, getPaddleState(0) || getPaddleState(1), this);
        lights.tick(engine.lightsWanted(driver != null, lights.isDark(level())), forward());

        if (level().isClientSide()) {
            travelledO = travelled;
            Vec3 f = forward();
            travelled += (float) ((getX() - xo) * f.x + (getZ() - zo) * f.z);
            return;
        }
        if (driver == null || !(level() instanceof ServerLevel level)) return;
        if (!getPaddleState(0) || !getPaddleState(1)) return; // only while driving forward
        if (tickCount % workInterval() != 0) return;
        work(level, driver);
    }

    /** Distance driven (blocks, client side), for rolling/spinning animations. */
    public float getTravelled(float partialTicks) {
        return Mth.lerp(partialTicks, travelledO, travelled);
    }

    // ---------------------------------------------------------------- storage helpers

    /** The bay first, then a dump truck within range. */
    protected List<Container> sinks(ServerLevel level) {
        List<Container> sinks = new ArrayList<>();
        sinks.add(this);
        DumpTruckEntity truck = DumpTruckEntity.findNear(level, position());
        if (truck != null) sinks.add(truck);
        return sinks;
    }

    /** Breaks a block, storing its drops (see {@link DumpTruckEntity#harvest}). */
    protected void breakInto(ServerLevel level, BlockPos pos, BlockState state, Player driver, List<Container> sinks) {
        DumpTruckEntity.harvest(level, pos, state, driver, sinks);
    }

    /** Whether a block can be removed by a machine (not air, fluid, unbreakable, a container or our lights). */
    protected boolean isBreakable(ServerLevel level, BlockPos pos, BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && !state.hasBlockEntity()
                && !state.is(FactoryCraft.VEHICLE_LIGHT.value()) && state.getDestroySpeed(level, pos) >= 0;
    }

    /** Takes one item matching {@code test} out of the bay, or returns empty. */
    protected ItemStack takeOne(java.util.function.Predicate<ItemStack> test) {
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack stack = getItem(i);
            if (!stack.isEmpty() && test.test(stack)) {
                ItemStack one = stack.split(1);
                setItem(i, stack.isEmpty() ? ItemStack.EMPTY : stack);
                return one;
            }
        }
        return ItemStack.EMPTY;
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
        // Sneak + right-click (or a seat that's taken): the machine's screen with bay and module slots.
        if (player.isSecondaryUseActive() || !canAddPassenger(player)) {
            if (!level().isClientSide()) VehicleMenu.open(player, this, this, 3);
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

    @Override
    public void openCustomInventoryScreen(Player player) {
        // Inventory key while riding: the machine's screen.
        if (!level().isClientSide()) VehicleMenu.open(player, this, this, 3);
    }
}
