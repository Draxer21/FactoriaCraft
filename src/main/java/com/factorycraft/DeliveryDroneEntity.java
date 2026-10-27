package com.factorycraft;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * The delivery drone you see flying between drone ports. It only shows the trip:
 * the cargo itself travels with the ports (so nothing is lost if the drone flies
 * through unloaded land). It climbs, cruises 12 blocks up and lands.
 */
public final class DeliveryDroneEntity extends Entity {
    private static final double CRUISE = 12.0;
    private Vec3 from = Vec3.ZERO;
    private Vec3 to = Vec3.ZERO;
    private int duration = 1;
    private int age;

    public DeliveryDroneEntity(EntityType<? extends DeliveryDroneEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public static void launch(ServerLevel level, Vec3 from, Vec3 to, int duration) {
        DeliveryDroneEntity drone = new DeliveryDroneEntity(Logistics.DELIVERY_DRONE.value(), level);
        drone.from = from;
        drone.to = to;
        drone.duration = Math.max(20, duration);
        drone.setPos(from);
        drone.setYRot((float) (Math.atan2(-(to.x - from.x), to.z - from.z) * 180.0 / Math.PI));
        level.addFreshEntity(drone);
    }

    /** Position along the trip (0..1): up, across, down. */
    private Vec3 at(double t) {
        double climb = 0.15;
        double height;
        double along;
        if (t < climb) {
            height = CRUISE * (t / climb);
            along = 0.0;
        } else if (t > 1 - climb) {
            height = CRUISE * ((1 - t) / climb);
            along = 1.0;
        } else {
            height = CRUISE;
            along = (t - climb) / (1 - 2 * climb);
        }
        double top = Math.max(from.y, to.y);
        double baseY = t < 0.5 ? from.y : to.y;
        double y = baseY + (top - baseY) * Math.min(1.0, height / CRUISE) + height;
        return new Vec3(from.x + (to.x - from.x) * along, y, from.z + (to.z - from.z) * along);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        age++;
        if (age >= duration) {
            discard();
            return;
        }
        setPos(at(age / (double) duration));
        if (age % 4 == 0 && level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, getX(), getY() - 0.2, getZ(), 1, 0.1, 0.0, 0.1, 0.0);
        }
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }
}
