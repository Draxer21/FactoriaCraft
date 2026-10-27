package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Cable car cabin: runs back and forth along the cable between two stations, waiting
 * 5 seconds at each end. Right-click it to get in (up to 4 players), sneak to get out.
 * The cable itself is drawn with the cabin, so the line shows wherever the cabin is tracked.
 */
public final class CableCarEntity extends Entity {
    public static final double CABLE_HEIGHT = 4.0;
    public static final double HANG = 2.9;
    private static final double PARK = 1.8;
    private static final double SPEED = 0.3;
    private static final int WAIT = 100;
    private static final EntityDataAccessor<BlockPos> FROM = SynchedEntityData.defineId(CableCarEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<BlockPos> TO = SynchedEntityData.defineId(CableCarEntity.class, EntityDataSerializers.BLOCK_POS);

    private int cycle;

    public CableCarEntity(EntityType<? extends CableCarEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    static void create(ServerLevel level, BlockPos from, BlockPos to) {
        CableCarEntity car = new CableCarEntity(Logistics.CABLE_CAR.value(), level);
        car.entityData.set(FROM, from);
        car.entityData.set(TO, to);
        car.setPos(car.at(0));
        level.addFreshEntity(car);
    }

    public BlockPos from() {
        return entityData.get(FROM);
    }

    public BlockPos to() {
        return entityData.get(TO);
    }

    /** Where the cable leaves a station (top of its mast). */
    public static Vec3 anchor(BlockPos station) {
        return new Vec3(station.getX() + 0.5, station.getY() + CABLE_HEIGHT, station.getZ() + 0.5);
    }

    public double length() {
        return anchor(from()).distanceTo(anchor(to()));
    }

    /** Height the cable sags at mid-span. */
    public double sag() {
        return Math.min(4.0, length() * 0.035);
    }

    /** Point of the cable at {@code u} (0 = first station, 1 = second). */
    public Vec3 cable(double u) {
        Vec3 a = anchor(from());
        Vec3 b = anchor(to());
        return a.lerp(b, u).subtract(0, sag() * 4 * u * (1 - u), 0);
    }

    private double travelTicks() {
        return Math.max(20, (length() - 2 * PARK) / SPEED);
    }

    private int period() {
        return 2 * WAIT + 2 * (int) Math.ceil(travelTicks());
    }

    /** Position along the line for a point of the cycle. */
    private double progress(int tick) {
        double travel = travelTicks();
        double park = PARK / Math.max(1.0, length());
        double t;
        if (tick < WAIT) t = 0;
        else if (tick < WAIT + travel) t = (tick - WAIT) / travel;
        else if (tick < 2 * WAIT + travel) t = 1;
        else t = 1 - Math.min(1.0, (tick - 2 * WAIT - travel) / travel);
        // Smooth start and stop.
        t = t * t * (3 - 2 * t);
        return park + (1 - 2 * park) * t;
    }

    private Vec3 at(int tick) {
        return cable(progress(tick)).subtract(0, HANG, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        if (tickCount % 20 == 0 && level().isLoaded(from()) && level().isLoaded(to())
                && !CableStationBlockEntity.pairOk(level(), from(), to())) {
            ejectPassengers();
            discard();
            return;
        }
        int before = cycle;
        cycle = (cycle + 1) % period();
        if ((before == WAIT - 1 || before == 2 * WAIT + (int) Math.ceil(travelTicks()) - 1) && level() instanceof ServerLevel server) {
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.MINECART_RIDING, SoundSource.NEUTRAL, 0.4F, 0.6F);
        }
        Vec3 next = at(cycle);
        Vec3 dir = next.subtract(position());
        if (dir.horizontalDistanceSqr() > 1.0E-6) setYRot((float) (Mth.atan2(-dir.x, dir.z) * Mth.RAD_TO_DEG));
        setPos(next);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FROM, BlockPos.ZERO);
        builder.define(TO, BlockPos.ZERO);
    }

    @Override
    public boolean isPickable() {
        return isAlive();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (player.isSecondaryUseActive() || player.getVehicle() == this) return InteractionResult.PASS;
        if (!level().isClientSide()) player.startRiding(this);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Player && getPassengers().size() < 4;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        int index = Math.max(0, getPassengers().indexOf(passenger));
        double side = (index % 2 == 0 ? -0.4 : 0.4);
        double front = (index < 2 ? 0.4 : -0.4);
        return new Vec3(side, 0.15, front).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        int[] f = input.getIntArray("From").orElse(new int[] {0, 0, 0});
        int[] t = input.getIntArray("To").orElse(new int[] {0, 0, 0});
        entityData.set(FROM, new BlockPos(f[0], f[1], f[2]));
        entityData.set(TO, new BlockPos(t[0], t[1], t[2]));
        cycle = input.getIntOr("Cycle", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        BlockPos f = from();
        BlockPos t = to();
        output.putIntArray("From", new int[] {f.getX(), f.getY(), f.getZ()});
        output.putIntArray("To", new int[] {t.getX(), t.getY(), t.getZ()});
        output.putInt("Cycle", cycle);
    }
}
