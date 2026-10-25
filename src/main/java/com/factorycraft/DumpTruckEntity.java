package com.factorycraft;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.entity.vehicle.boat.AbstractChestBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Dump truck with an 81-slot bed, three chests' worth (sneak + right-click to open it).
 *  - Collects loose items around it, and receives the blocks broken by a nearby
 *    excavator or tunneler directly (see {@link #harvest}).
 *  - Park it with its rear against a container or a conveyor belt and get out:
 *    it tips the bed and unloads.
 */
public final class DumpTruckEntity extends AbstractChestBoat implements VehicleHitbox.Holder, VehicleLights.Holder, VehicleEngine.Holder {
    private static final EntityDataAccessor<Boolean> ENGINE_ON =
            SynchedEntityData.defineId(DumpTruckEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> FUEL =
            SynchedEntityData.defineId(DumpTruckEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> UPGRADES =
            SynchedEntityData.defineId(DumpTruckEntity.class, EntityDataSerializers.INT);
    private final VehicleEngine engine = new VehicleEngine(this, ENGINE_ON, FUEL, UPGRADES);
    private static final EntityDataAccessor<Byte> FILL =
            SynchedEntityData.defineId(DumpTruckEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DUMPING =
            SynchedEntityData.defineId(DumpTruckEntity.class, EntityDataSerializers.BOOLEAN);

    /** Top forward speed in blocks per tick (0.45 = 9 blocks/s): the fastest machine. */
    private static final double MAX_SPEED = 0.45;
    private static final double REVERSE_SPEED = 0.2;
    private static final double ACCELERATION = 0.03;
    private static final double BRAKING = 0.06;

    /** Machines within this range send their broken blocks straight into the truck. */
    public static final double LINK_RANGE = 16.0;
    private static final double COLLECT_RADIUS = 3.0;
    private static final double MAGNET_RADIUS = 8.0;
    private static final int WORK_INTERVAL = 4;
    /** Ticks the bed takes to tip before items start flowing out. */
    private static final int TIP_DELAY = 20;
    private static final int DUMP_BATCH = 16;
    private static final int MAX_ITEMS_ON_BELT = 3;

    /** Seat inside the cab: (0.375 + 14 px / 16) * scale 1.5 high, 18 px forward. */
    private static final double SEAT_HEIGHT = 1.875;
    private static final double SEAT_FORWARD = 1.69;
    private static final double HULL_FORWARD_SHIFT = 0.19;
    private static final double HULL_HALF_LENGTH = 2.9;
    private static final double HULL_HALF_WIDTH = 1.57;
    /** Distance from the entity position to just behind the tailgate. */
    private static final double REAR_REACH = 3.3;
    /** Road wheel radius in blocks (2.5 px * 1.5 / 16), for the rolling animation. */
    private static final float WHEEL_RADIUS = 0.234F;
    /** Bed capacity: 9 rows of 9, three times a chest. */
    public static final int ROWS = 9;
    public static final int CAPACITY = ROWS * 9;

    private NonNullList<ItemStack> cargo = NonNullList.withSize(CAPACITY, ItemStack.EMPTY);

    private boolean inputForward;
    private boolean inputBack;
    private double driveSpeed;
    private final Autopilot autopilot = new Autopilot();
    private int dumpTicks;
    private final VehicleLights lights = new VehicleLights(this, 2.9, 1.2, 2.0, 5.0, 8.0, 11.0);

    // Client-side animation state.
    private float dumpProgress;
    private float dumpProgressO;
    private float wheelRot;
    private float wheelRotO;

    public DumpTruckEntity(EntityType<? extends DumpTruckEntity> type, Level level) {
        super(type, level, () -> FactoryCraft.DUMP_TRUCK_ITEM.value());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        VehicleEngine.define(builder, ENGINE_ON, FUEL, UPGRADES);
        builder.define(FILL, (byte) 0);
        builder.define(DUMPING, false);
    }

    // ---------------------------------------------------------------- storage

    @Override
    public int getContainerSize() {
        return CAPACITY;
    }

    @Override
    public NonNullList<ItemStack> getItemStacks() {
        return cargo;
    }

    @Override
    public void clearItemStacks() {
        cargo = NonNullList.withSize(CAPACITY, ItemStack.EMPTY);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ChestMenu(FactoryCraft.DUMP_TRUCK_MENU.value(), containerId, inventory, this, ROWS);
    }

    @Override
    public VehicleLights lights() {
        return lights;
    }

    // ---------------------------------------------------------------- geometry

    @Override
    protected double rideHeight(EntityDimensions dimensions) {
        return SEAT_HEIGHT;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0.0, SEAT_HEIGHT, SEAT_FORWARD).yRot(-getYRot() * Mth.DEG_TO_RAD);
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
    public float maxUpStep() {
        return 1.0F;
    }

    private Vec3 forward() {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
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
            double target = inputForward ? MAX_SPEED * engine.speedFactor() : (inputBack ? -REVERSE_SPEED : 0.0);
            boolean speedingUp = Math.abs(target) > Math.abs(driveSpeed)
                    && (driveSpeed == 0.0 || Math.signum(target) == Math.signum(driveSpeed));
            driveSpeed = approach(driveSpeed, target, speedingUp ? ACCELERATION : BRAKING);
            if (horizontalCollision) driveSpeed *= 0.5;
            Vec3 forward = forward();
            delta = new Vec3(forward.x * driveSpeed, delta.y, forward.z * driveSpeed);
            setDeltaMovement(delta);
        } else if (type == MoverType.SELF && !level().isClientSide()
                && autopilot.drive(this, engine, MAX_SPEED * engine.speedFactor(), true)) {
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
        boolean driven = getControllingPassenger() instanceof Player;
        engine.tick(getControllingPassenger() instanceof Player p ? p : null, getPaddleState(0) || getPaddleState(1), this);
        lights.tick(engine.lightsWanted(driven, lights.isDark(level())), forward());

        if (level().isClientSide()) {
            animate();
            return;
        }
        if (!(level() instanceof ServerLevel level)) return;

        // Unload when parked (nobody inside) with the rear against a container or belt.
        BlockPos unloadAt = driven ? null : findUnloadTarget(level);
        boolean dumping = unloadAt != null && !isChestVehicleEmpty();
        setDumping(dumping);
        dumpTicks = dumping ? dumpTicks + 1 : 0;

        if (tickCount % WORK_INTERVAL != 0) return;
        if (dumping && dumpTicks > TIP_DELAY) {
            unloadInto(level, unloadAt);
        } else if (unloadAt == null) {
            collectNearbyItems(level);
        }
        updateFill();
    }

    private void animate() {
        dumpProgressO = dumpProgress;
        dumpProgress = Mth.approach(dumpProgress, isDumping() ? 1.0F : 0.0F, 0.04F);
        wheelRotO = wheelRot;
        Vec3 forward = forward();
        double moved = (getX() - xo) * forward.x + (getZ() - zo) * forward.z;
        wheelRot += (float) (moved / WHEEL_RADIUS);
    }

    private void collectNearbyItems(ServerLevel level) {
        double radius = engine.has(Upgrade.MAGNET) ? MAGNET_RADIUS : COLLECT_RADIUS;
        AABB area = getBoundingBox().inflate(radius, 1.5, radius);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area, ItemEntity::isAlive)) {
            HopperBlockEntity.addItem(this, item);
        }
    }

    /** A container or conveyor right behind the tailgate (same level, one up or one down). */
    private @Nullable BlockPos findUnloadTarget(ServerLevel level) {
        Vec3 rear = position().add(forward().scale(-REAR_REACH));
        BlockPos base = BlockPos.containing(rear.x, getY() + 0.5, rear.z);
        for (BlockPos pos : new BlockPos[] {base, base.below(), base.above()}) {
            if (isConveyor(level, pos) || HopperBlockEntity.getContainerAt(level, pos) != null) return pos;
        }
        return null;
    }

    private static boolean isConveyor(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(FactoryCraft.CONVEYOR.value());
    }

    private void unloadInto(ServerLevel level, BlockPos pos) {
        int slot = firstFilledSlot();
        if (slot < 0) return;
        ItemStack stack = getItem(slot);

        if (isConveyor(level, pos)) {
            AABB onBelt = new AABB(pos).inflate(0.0, 0.5, 0.0);
            if (level.getEntitiesOfClass(ItemEntity.class, onBelt).size() >= MAX_ITEMS_ON_BELT) return;
            ItemStack batch = stack.split(Math.min(DUMP_BATCH, stack.getCount()));
            ItemEntity item = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, batch, 0.0, 0.0, 0.0);
            level.addFreshEntity(item);
        } else {
            Container container = HopperBlockEntity.getContainerAt(level, pos);
            if (container == null) return;
            ItemStack batch = stack.split(Math.min(DUMP_BATCH, stack.getCount()));
            ItemStack rest = HopperBlockEntity.addItem(this, container, batch, null);
            if (!rest.isEmpty()) stack.grow(rest.getCount());
        }
        setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
    }

    private int firstFilledSlot() {
        for (int i = 0; i < getContainerSize(); i++) {
            if (!getItem(i).isEmpty()) return i;
        }
        return -1;
    }

    private void updateFill() {
        float total = 0.0F;
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack stack = getItem(i);
            if (!stack.isEmpty()) total += (float) stack.getCount() / stack.getMaxStackSize();
        }
        entityData.set(FILL, (byte) Math.round(100.0F * total / getContainerSize()));
    }

    // ---------------------------------------------------------------- linking

    /** Puts a stack in the bed; returns what didn't fit. */
    public ItemStack insert(ItemStack stack) {
        return HopperBlockEntity.addItem(null, this, stack, null);
    }

    /** The closest truck within {@link #LINK_RANGE} that still has room, if any. */
    public static @Nullable DumpTruckEntity findNear(ServerLevel level, Vec3 pos) {
        List<DumpTruckEntity> trucks = level.getEntitiesOfClass(DumpTruckEntity.class,
                new AABB(pos, pos).inflate(LINK_RANGE), truck -> truck.isAlive() && truck.hasRoom());
        return trucks.stream().min(Comparator.comparingDouble(t -> t.distanceToSqr(pos))).orElse(null);
    }

    private boolean hasRoom() {
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) return true;
        }
        return false;
    }

    /**
     * Breaks a block for a digging machine. If a truck is linked, the drops go
     * straight into its bed (anything that doesn't fit drops normally).
     */
    public static void harvest(ServerLevel level, BlockPos pos, BlockState state, Entity breaker, @Nullable DumpTruckEntity truck) {
        harvest(level, pos, state, breaker, truck == null || !truck.isAlive() ? List.of() : List.of(truck));
    }

    /**
     * Breaks a block and stores its drops in the given containers, in order;
     * whatever doesn't fit drops on the ground. With no containers it breaks normally.
     */
    public static void harvest(ServerLevel level, BlockPos pos, BlockState state, Entity breaker, List<? extends Container> sinks) {
        // The machine the breaker drives decides the tool (fortune module) and the ore filter.
        VehicleEngine engine = breaker.getVehicle() == null ? null : VehicleEngine.of(breaker.getVehicle());
        ItemStack tool = engine == null ? ItemStack.EMPTY : engine.tool();
        boolean upgraded = engine != null && (!tool.isEmpty() || engine.filtersJunk());
        if (sinks.isEmpty() && !upgraded) {
            level.destroyBlock(pos, true, breaker, 512);
            return;
        }
        List<ItemStack> drops = tool.isEmpty() ? Block.getDrops(state, level, pos, null)
                : Block.getDrops(state, level, pos, null, breaker, tool);
        state.spawnAfterBreak(level, pos, tool, true);
        level.destroyBlock(pos, false, breaker, 512);
        for (ItemStack drop : drops) {
            if (engine != null && !engine.keeps(drop)) continue;
            ItemStack rest = drop;
            for (Container sink : sinks) {
                if (rest.isEmpty()) break;
                rest = HopperBlockEntity.addItem(null, sink, rest, null);
            }
            if (!rest.isEmpty()) Block.popResource(level, pos, rest);
        }
    }

    // ---------------------------------------------------------------- state

    private void setDumping(boolean dumping) {
        if (entityData.get(DUMPING) == dumping) return;
        entityData.set(DUMPING, dumping);
        level().playSound(null, getX(), getY(), getZ(), FactoryCraft.HYDRAULIC.value(),
                net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, dumping ? 1.0F : 0.85F);
    }

    public boolean isDumping() {
        return entityData.get(DUMPING);
    }

    /** 0..1 how full the bed is (synced to clients for the load model). */
    public float getFill() {
        return entityData.get(FILL) / 100.0F;
    }

    public float getDumpProgress(float partialTicks) {
        return Mth.lerp(partialTicks, dumpProgressO, dumpProgress);
    }

    public float getWheelRot(float partialTicks) {
        return Mth.lerp(partialTicks, wheelRotO, wheelRot);
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
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        InteractionResult handled = engine.interact(player, hand);
        if (handled != null) return handled;
        // Sneak + right-click (or a seat that's taken): the machine's screen with bay and module slots.
        if (player.isSecondaryUseActive() || !canAddPassenger(player)) {
            if (!level().isClientSide()) VehicleMenu.open(player, this, this, ROWS);
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
        if (!level().isClientSide()) VehicleMenu.open(player, this, this, ROWS);
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
    public boolean canBeCollidedWith(@org.jspecify.annotations.Nullable net.minecraft.world.entity.Entity other) {
        return false;
    }

    @Override
    public void push(net.minecraft.world.entity.Entity other) {
        if (hitbox.touches(other)) super.push(other);
    }
}
