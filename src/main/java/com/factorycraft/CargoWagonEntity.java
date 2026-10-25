package com.factorycraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractChestBoat;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Tracked cargo wagon (54 slots, right-click to open). Hitches itself behind a
 * tunneler, or behind the last wagon of its train, and follows it with a tow
 * bar, including up and down sloped tunnels. The tunneler fills its own bay
 * first and then the wagons, front to back.
 */
public class CargoWagonEntity extends AbstractChestBoat implements VehicleHitbox.Holder {
    private static final EntityDataAccessor<Byte> FILL =
            SynchedEntityData.defineId(CargoWagonEntity.class, EntityDataSerializers.BYTE);

    public static final int ROWS = 6;
    public static final int CAPACITY = ROWS * 9;

    /** Distance from the wagon centre to its front tow hitch (and to its rear hitch). */
    public static final double HITCH_OFFSET = 1.6;
    /** Gap kept between the leader's rear hitch and the wagon's front hitch. */
    private static final double TOW_GAP = 0.6;
    /** Unhitched wagons look for a free leader whose rear hitch is this close to their front. */
    private static final double HITCH_RANGE = 4.0;
    /** A train breaks if the leader gets further than this (teleported, unloaded...). */
    private static final double BREAK_DISTANCE = 12.0;
    private static final double TRAIN_SEARCH_RADIUS = 64.0;

    private static final double HULL_FORWARD_SHIFT = 0.14;
    private static final double HULL_HALF_LENGTH = 1.7;
    private static final double HULL_HALF_WIDTH = 1.03;

    private NonNullList<ItemStack> cargo = NonNullList.withSize(CAPACITY, ItemStack.EMPTY);
    private @Nullable UUID leaderId;
    private @Nullable Entity leader;

    public CargoWagonEntity(EntityType<? extends CargoWagonEntity> type, Level level) {
        super(type, level, () -> FactoryCraft.CARGO_WAGON_ITEM.value());
    }

    protected CargoWagonEntity(EntityType<? extends CargoWagonEntity> type, Level level, java.util.function.Supplier<net.minecraft.world.item.Item> drop) {
        super(type, level, drop);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FILL, (byte) 0);
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
        return ChestMenu.sixRows(containerId, inventory, this);
    }

    public float getFill() {
        return entityData.get(FILL) / 100.0F;
    }

    protected void setFill(float fraction) {
        entityData.set(FILL, (byte) Math.round(100.0F * Math.max(0.0F, Math.min(1.0F, fraction))));
    }

    protected void updateFill() {
        float total = 0.0F;
        for (ItemStack stack : cargo) {
            if (!stack.isEmpty()) total += (float) stack.getCount() / stack.getMaxStackSize();
        }
        entityData.set(FILL, (byte) Math.round(100.0F * total / CAPACITY));
    }

    // ---------------------------------------------------------------- not a ride

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        // Nobody rides the wagon, so a plain right-click opens its inventory.
        return false;
    }

    @Override
    protected double rideHeight(EntityDimensions dimensions) {
        return 1.0;
    }

    @Override
    public boolean canCollideWith(Entity other) {
        return !isTrainVehicle(other) && super.canCollideWith(other);
    }

    @Override
    public void push(Entity entity) {
        // Pushing the tunneler (or another wagon) would make the whole train creep forward.
        if (isTrainVehicle(entity)) return;
        if (!hitbox.touches(entity)) return;
        super.push(entity);
    }

    @Override
    public boolean isPushable() {
        // The tow bar sets our position; nothing should shove us out of line.
        return leaderId == null && super.isPushable();
    }

    static boolean isTrainVehicle(@Nullable Entity entity) {
        entity = VehicleHitbox.owner(entity);
        return entity instanceof TunnelerEntity || entity instanceof LocomotiveEntity || entity instanceof CargoWagonEntity;
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

    // ---------------------------------------------------------------- towing

    /** Rear tow hitch of a tunneler or wagon, in world space. */
    public static Vec3 rearHitch(Entity entity) {
        double reach = entity instanceof TunnelerEntity ? TunnelerEntity.REAR_HITCH_OFFSET
                : entity instanceof LocomotiveEntity ? LocomotiveEntity.REAR_HITCH_OFFSET : HITCH_OFFSET;
        Vec3 heading = Vec3.directionFromRotation(entity.getXRot(), entity.getYRot());
        return entity.position().add(0.0, 0.5, 0.0).subtract(heading.scale(reach));
    }

    private Vec3 frontHitch() {
        Vec3 heading = Vec3.directionFromRotation(getXRot(), getYRot());
        return position().add(0.0, 0.5, 0.0).add(heading.scale(HITCH_OFFSET));
    }

    public void hitchTo(Entity newLeader) {
        this.leader = newLeader;
        this.leaderId = newLeader.getUUID();
    }

    public @Nullable UUID getLeaderId() {
        return leaderId;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) return;

        Entity current = resolveLeader(level);
        if (current != null) {
            follow(current);
        } else if (tickCount % 20 == 0) {
            tryAutoHitch(level);
        }
        if (tickCount % 10 == 0) updateFill();
    }

    private @Nullable Entity resolveLeader(ServerLevel level) {
        if (leaderId == null) return null;
        if (leader == null || !leader.isAlive() || !leader.getUUID().equals(leaderId)) {
            leader = level.getEntity(leaderId);
        }
        if (leader == null || !leader.isAlive() || leader.distanceTo(this) > BREAK_DISTANCE) {
            // Keep the id while the leader may just be unloaded; drop it if it's gone for good.
            if (leader != null && (!leader.isAlive() || leader.distanceTo(this) > BREAK_DISTANCE)) {
                leaderId = null;
            }
            leader = null;
            return null;
        }
        return leader;
    }

    /** Tow-bar kinematics: keep the front hitch at a fixed distance from the leader's rear hitch. */
    private void follow(Entity leader) {
        Vec3 hitch = rearHitch(leader);
        Vec3 center = position().add(0.0, 0.5, 0.0);
        Vec3 toLeader = hitch.subtract(center);
        if (toLeader.lengthSqr() < 1.0E-4) return;
        Vec3 dir = toLeader.normalize();
        Vec3 newCenter = hitch.subtract(dir.scale(HITCH_OFFSET + TOW_GAP));

        float yaw = (float) (Mth.atan2(dir.z, dir.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) (-Math.asin(Mth.clamp(dir.y, -1.0, 1.0)) * Mth.RAD_TO_DEG);
        setYRot(yaw);
        setXRot(pitch);
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        setPos(newCenter.x, newCenter.y - 0.5, newCenter.z);
    }

    /** Hitch to the closest free tunneler or train tail whose rear is near our front. */
    private void tryAutoHitch(ServerLevel level) {
        setNoGravity(false);
        Entity best = findFreeLeader(level, frontHitch(), HITCH_RANGE, this);
        if (best != null) hitchTo(best);
    }

    /**
     * The closest tunneler or wagon with nothing hitched behind it whose rear
     * hitch is within {@code range} of {@code point}.
     */
    public static @Nullable Entity findFreeLeader(ServerLevel level, Vec3 point, double range, @Nullable Entity exclude) {
        AABB area = new AABB(point, point).inflate(range + 4.0);
        List<Entity> candidates = level.getEntities((Entity) null, area,
                e -> e != exclude && e.isAlive() && isTrainVehicle(e));
        List<CargoWagonEntity> wagons = level.getEntitiesOfClass(CargoWagonEntity.class,
                new AABB(point, point).inflate(TRAIN_SEARCH_RADIUS));
        Entity best = null;
        double bestDistance = range;
        for (Entity candidate : candidates) {
            if (hasFollower(candidate, wagons, exclude)) continue;
            if (candidate instanceof CargoWagonEntity wagon && wagon.leaderId == null) continue;
            double distance = rearHitch(candidate).distanceTo(point);
            if (distance <= bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static boolean hasFollower(Entity candidate, List<CargoWagonEntity> wagons, @Nullable Entity exclude) {
        for (CargoWagonEntity wagon : wagons) {
            if (wagon != exclude && candidate.getUUID().equals(wagon.leaderId)) return true;
        }
        return false;
    }

    /** The wagons hitched behind {@code head}, in order from front to back. */
    public static List<CargoWagonEntity> train(ServerLevel level, Entity head) {
        List<CargoWagonEntity> nearby = level.getEntitiesOfClass(CargoWagonEntity.class,
                head.getBoundingBox().inflate(TRAIN_SEARCH_RADIUS), Entity::isAlive);
        Map<UUID, CargoWagonEntity> byLeader = new HashMap<>();
        for (CargoWagonEntity wagon : nearby) {
            if (wagon.leaderId != null) byLeader.putIfAbsent(wagon.leaderId, wagon);
        }
        List<CargoWagonEntity> train = new ArrayList<>();
        UUID current = head.getUUID();
        while (true) {
            CargoWagonEntity next = byLeader.get(current);
            if (next == null || train.contains(next)) break;
            train.add(next);
            current = next.getUUID();
        }
        return train;
    }

    // ---------------------------------------------------------------- saving

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (leaderId != null) output.store("Leader", UUIDUtil.CODEC, leaderId);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        Optional<UUID> id = input.read("Leader", UUIDUtil.CODEC);
        leaderId = id.orElse(null);
    }

    @Override
    protected net.minecraft.sounds.@org.jspecify.annotations.Nullable SoundEvent getPaddleSound() {
        // Engines and tracks have their own sounds: no boat paddling.
        return null;
    }

    // ---------------------------------------------------------------- segmented collision

    private final VehicleHitbox hitbox = new VehicleHitbox(this, VehicleShapes.of(this));

    @Override
    public VehicleHitbox hitbox() {
        return hitbox;
    }

    @Override
    public boolean solidFor(net.minecraft.world.entity.Entity other) {
        return !isTrainVehicle(other) && super.canBeCollidedWith(other);
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
}
