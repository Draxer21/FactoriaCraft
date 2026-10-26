package com.factorycraft;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.EntityDataAccessor;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractChestBoat;
import net.minecraft.world.Container;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Tunneler controls:
 *  - W / S: forward / reverse
 *  - A / D: steer (like a boat)
 *  - Mouse up / down: the machine tilts its nose toward where the pilot looks
 *    while driving (smoothly, max 50 degrees). Looking roughly straight ahead
 *    keeps it level; when stopped the tilt is held.
 * It digs out exactly the space its hull is about to occupy, so it never gets
 * stuck on the stair-step edges of a sloped tunnel.
 */
public final class TunnelerEntity extends AbstractChestBoat implements VehicleHitbox.Holder, ToolToggle, VehicleLights.Holder, VehicleEngine.Holder {
    private static final EntityDataAccessor<Boolean> ENGINE_ON =
            SynchedEntityData.defineId(TunnelerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> FUEL =
            SynchedEntityData.defineId(TunnelerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TOOL_ON =
            SynchedEntityData.defineId(TunnelerEntity.class, EntityDataSerializers.BOOLEAN);
    private final ToolToggle.Spin drillSpin = new ToolToggle.Spin();
    private static final EntityDataAccessor<Integer> UPGRADES =
            SynchedEntityData.defineId(TunnelerEntity.class, EntityDataSerializers.INT);
    private final VehicleEngine engine = new VehicleEngine(this, ENGINE_ON, FUEL, UPGRADES);
    private static final int DIG_INTERVAL = 2;
    /** Top forward speed in blocks per tick (0.32 = 6.4 blocks/s). */
    private static final double MAX_SPEED = 0.32;
    private static final double REVERSE_SPEED = 0.14;
    private static final double ACCELERATION = 0.02;
    private static final double BRAKING = 0.05;

    private static final float MAX_PITCH = 50.0F;
    /** Looking within this many degrees of horizontal keeps the machine level. */
    private static final float LEVEL_DEADZONE = 12.0F;
    private static final float PITCH_RATE = 2.5F;
    /** How far ahead (in blocks) the bore clears the hull's path. */
    private static final double BORE_REACH = 4.5;

    private boolean inputForward;
    private boolean inputBack;
    private double driveSpeed;
    /** Headlights along the (tilted) drill axis, from the drill tip up to ~14 blocks ahead. */
    private final VehicleLights lights = new VehicleLights(this,
            HULL_FORWARD_SHIFT + HULL_HALF_LENGTH, 1.6, 1.5, 4.5, 7.5, 10.5);

    public TunnelerEntity(EntityType<? extends TunnelerEntity> type, Level level) {
        super(type, level, () -> FactoryCraft.TUNNELER_ITEM.value());
    }

    /** Height of the seat cushion in blocks: (0.375 + 16 px / 16) * render scale 1.65. */
    public static final float SEAT_HEIGHT = 2.27F;
    /** Seat center is 5 model px behind the machine's center (5 / 16 * 1.65 blocks). */
    public static final float SEAT_OFFSET_BACK = 0.52F;

    @Override
    protected double rideHeight(EntityDimensions dimensions) {
        return SEAT_HEIGHT;
    }

    /** The model reaches ~1.55 blocks behind and ~3.5 ahead (drill), so the hitbox is shifted forward. */
    private static final double HULL_FORWARD_SHIFT = 0.95;
    private static final double HULL_HALF_LENGTH = 2.6;
    /** Rear tow hitch for cargo wagons: just behind the tracks (1.55 blocks back). */
    public static final double REAR_HITCH_OFFSET = 1.75;
    private static final double HULL_HALF_WIDTH = 1.25;

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        // The machine tilts around the seat itself (see TunnelerRenderer), so the pilot
        // stays put on the seat at any angle and the cab never swings into the camera.
        return new Vec3(0.0, SEAT_HEIGHT, -SEAT_OFFSET_BACK).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    protected double getDefaultGravity() {
        // Disable vanilla boat buoyancy in water so the pilot can actually dive.
        return isInWater() ? 0.0 : 0.04;
    }

    @Override
    public boolean canCollideWith(Entity other) {
        // Don't bump into our own cargo wagons.
        return !(VehicleHitbox.owner(other) instanceof CargoWagonEntity) && super.canCollideWith(other);
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
        return 1.0F;
    }

    @Override
    protected AABB makeBoundingBox(Vec3 position) {
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
    public void ejectPassengers() {
        // This is a sealed underwater vehicle. Keep the pilot aboard while submerged.
        if (getControllingPassenger() instanceof Player && isUnderWater()) return;
        super.ejectPassengers();
    }

    @Override
    public void setInput(boolean left, boolean right, boolean up, boolean down) {
        // With the engine off the machine neither drives nor steers.
        boolean on = engine.isRunning();
        super.setInput(left && on, right && on, up && on, down && on);
        this.inputForward = up && on;
        this.inputBack = down && on;
    }

    /** Direction the drill points, including the machine's tilt. */
    private Vec3 heading() {
        return Vec3.directionFromRotation(getXRot(), getYRot());
    }

    private boolean isTilted() {
        return Math.abs(getXRot()) > 0.5F;
    }

    /** Recomputed every tick: is there ground (e.g. the ramp it just dug) right under the hull? */
    private boolean supported;

    /**
     * The tunneler can only climb or hold a slope while it rests on terrain.
     * Pointing the nose up in open air must not make it fly.
     */
    private boolean isOnTerrain() {
        AABB hull = getBoundingBox();
        AABB below = new AABB(hull.minX, hull.minY - 0.6, hull.minZ, hull.maxX, hull.minY + 0.05, hull.maxZ);
        return level().getBlockCollisions(this, below).iterator().hasNext();
    }

    /** Tilted travel (and hovering on a slope) only while supported by terrain or underwater. */
    private boolean followsTilt() {
        return isInWater() || (isTilted() && supported);
    }

    @Override
    public void push(Entity entity) {
        // Our own cargo wagons ride right behind us; never shove each other.
        if (entity instanceof CargoWagonEntity) return;
        if (!hitbox.touches(entity)) return;
        super.push(entity);
    }

    @Override
    public void tick() {
        Player pilot = getControllingPassenger() instanceof Player p ? p : null;

        // Hold position while tilted (inside a sloped/vertical tunnel) or underwater;
        // fall normally when level on open ground.
        supported = isOnTerrain();
        setNoGravity(isInWater() || (pilot != null && isTilted() && supported));

        if (pilot != null && isLocalInstanceAuthoritative() && (inputForward || inputBack)) {
            // Only steer the tilt while driving, so looking around (e.g. down while
            // climbing in) doesn't tip the machine over.
            float look = Mth.clamp(pilot.getXRot(), -MAX_PITCH, MAX_PITCH);
            float target = Math.abs(look) < LEVEL_DEADZONE ? 0.0F
                    : look - Math.signum(look) * LEVEL_DEADZONE * 0.5F;
            setXRot(Mth.approach(getXRot(), target, PITCH_RATE));
        } else if (pilot == null) {
            // Unoccupied: settle back to level.
            setXRot(Mth.approach(getXRot(), 0.0F, PITCH_RATE));
        }

        super.tick();
        engine.tick(pilot, getPaddleState(0) || getPaddleState(1), this);
        lights.tick(engine.lightsWanted(pilot != null, lights.isDark(level())), heading());
        if (level().isClientSide()) drillSpin.tick(toolOn() && engine.isRunning(), 0.75F);

        if (pilot == null) return;
        if (isInWater()) pilot.setAirSupply(pilot.getMaxAirSupply());

        boolean movingForward = getPaddleState(0) && getPaddleState(1);
        if (!movingForward || !toolOn() || level().isClientSide() || tickCount % DIG_INTERVAL != 0) return;
        boreAhead(pilot);
    }

    @Override
    public void move(MoverType type, Vec3 delta) {
        if (type == MoverType.SELF && isLocalInstanceAuthoritative() && getControllingPassenger() instanceof Player) {
            double target = inputForward ? MAX_SPEED * engine.speedFactor() : (inputBack ? -REVERSE_SPEED : 0.0);
            boolean speedingUp = Math.abs(target) > Math.abs(driveSpeed)
                    && (driveSpeed == 0.0 || Math.signum(target) == Math.signum(driveSpeed));
            driveSpeed = approach(driveSpeed, target, speedingUp ? ACCELERATION : BRAKING);
            if (horizontalCollision && !isTilted()) driveSpeed *= 0.5;

            Vec3 velocity = heading().scale(driveSpeed);
            // When level, keep the boat's vertical motion (gravity / ground contact).
            // In open air (no ground under the hull) gravity wins, so it can't fly.
            double vy = followsTilt() ? velocity.y : delta.y;
            delta = new Vec3(velocity.x, vy, velocity.z);
            setDeltaMovement(delta);
        } else if (!(getControllingPassenger() instanceof Player)) {
            driveSpeed = 0.0;
        }
        super.move(type, delta);
    }

    private static double approach(double value, double target, double step) {
        return value < target ? Math.min(value + step, target) : Math.max(value - step, target);
    }

    /**
     * Clears every block the hull would touch while advancing up to BORE_REACH
     * blocks along its tilted heading. When level this leaves the floor intact;
     * when tilted it cuts a clean ramp up or down.
     */
    private void boreAhead(Player pilot) {
        Vec3 heading = heading();
        AABB hull = getBoundingBox().deflate(0.05);
        Set<BlockPos> targets = new HashSet<>();
        for (double step = 0.5; step <= BORE_REACH; step += 0.5) {
            AABB swept = hull.move(heading.scale(step));
            // Wide-drill module: one more block on each side and above (never below).
            if (engine.wide()) swept = swept.inflate(1.0, 0.0, 1.0).expandTowards(0.0, 1.0, 0.0);
            BlockPos min = BlockPos.containing(swept.minX, swept.minY, swept.minZ);
            BlockPos max = BlockPos.containing(swept.maxX, swept.maxY, swept.maxZ);
            for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
                targets.add(pos.immutable());
            }
        }
        // Drops go straight into a dump truck within range, if there is one.
        // Drops fill the tunneler's own bay first, then its wagons front to back,
        // then a dump truck within range; the rest falls on the ground.
        ServerLevel serverLevel = (ServerLevel) level();
        List<Container> sinks = new ArrayList<>();
        sinks.add(this);
        sinks.addAll(CargoWagonEntity.train(serverLevel, this));
        DumpTruckEntity truck = DumpTruckEntity.findNear(serverLevel, position());
        if (truck != null) sinks.add(truck);
        for (BlockPos pos : targets) {
            breakBlock(pos, pilot, sinks);
        }
    }

    private void breakBlock(BlockPos target, Player pilot, List<Container> sinks) {
        BlockState state = level().getBlockState(target);
        if (state.isAir() || state.is(FactoryCraft.VEHICLE_LIGHT.value()) || state.hasBlockEntity() || !state.getFluidState().isEmpty()
                || state.getDestroySpeed(level(), target) < 0) return;
        DumpTruckEntity.harvest((ServerLevel) level(), target, state, pilot, sinks);
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
        builder.define(TOOL_ON, true);
    }

    // ---------------------------------------------------------------- drill switch

    @Override
    public boolean toolOn() {
        return entityData.get(TOOL_ON);
    }

    @Override
    public void setToolOn(boolean on) {
        entityData.set(TOOL_ON, on);
    }

    @Override
    public float toolSpin(float partialTicks) {
        return drillSpin.get(partialTicks);
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
        output.putBoolean("ToolOn", toolOn());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        engine.load(input);
        setToolOn(input.getBooleanOr("ToolOn", true));
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

    // ---------------------------------------------------------------- segmented collision

    private final VehicleHitbox hitbox = new VehicleHitbox(this, VehicleShapes.of(this));

    @Override
    public VehicleHitbox hitbox() {
        return hitbox;
    }

    @Override
    public boolean solidFor(net.minecraft.world.entity.Entity other) {
        return super.canBeCollidedWith(other);
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        if (hitbox != null) hitbox.setIds(id);
    }

    @Override
    public void setPos(double x, double y, double z) {
        super.setPos(x, y, z);
        if (hitbox != null) hitbox.update();
    }

    @Override
    public void baseTick() {
        super.baseTick();
        hitbox.update();
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public net.neoforged.neoforge.entity.PartEntity<?>[] getParts() {
        return hitbox.parts();
    }

    /** Clicks land on the segments, not on the big terrain box. */
    @Override
    public boolean isPickable() {
        return false;
    }

    /** Others bump into the segments, not into the big terrain box. */
    @Override
    public boolean canBeCollidedWith(net.minecraft.world.entity.@org.jspecify.annotations.Nullable Entity other) {
        return false;
    }

    /** Only players drive: mobs bumping into the machine don't climb aboard. */
    @Override
    protected boolean canAddPassenger(net.minecraft.world.entity.Entity passenger) {
        return passenger instanceof net.minecraft.world.entity.player.Player && super.canAddPassenger(passenger);
    }
}
