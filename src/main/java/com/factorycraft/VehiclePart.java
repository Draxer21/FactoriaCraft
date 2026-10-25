package com.factorycraft;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jspecify.annotations.Nullable;

/**
 * One segment of a vehicle's collision: a box that follows the vehicle and turns with it.
 * Players stand on it, bump into it and click it; everything is handed to the vehicle itself.
 */
public final class VehiclePart extends PartEntity<AbstractBoat> {
    final VehicleHitbox.Box box;
    double halfX;
    double halfZ;

    VehiclePart(AbstractBoat parent, VehicleHitbox.Box box) {
        super(parent);
        this.box = box;
        this.halfX = box.halfLength();
        this.halfZ = box.halfWidth();
        this.noPhysics = true;
        refreshDimensions();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return getParent().isAlive();
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        AbstractBoat vehicle = getParent();
        if (other == null || other == vehicle || other.getRootVehicle() == vehicle) return false;
        return !(vehicle instanceof VehicleHitbox.Holder holder) || holder.solidFor(other);
    }

    @Override
    public boolean is(Entity entity) {
        return this == entity || getParent() == entity;
    }

    @Override
    public @Nullable ItemStack getPickResult() {
        return getParent().getPickResult();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // An explosion already reaches the vehicle itself; don't count it once per segment.
        if (source.is(DamageTypeTags.IS_EXPLOSION)) return false;
        return getParent().hurtServer(level, source, amount);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        return getParent().interact(player, hand, location.add(position()).subtract(getParent().position()));
    }

    @Override
    protected AABB makeBoundingBox(Vec3 position) {
        if (box == null) return super.makeBoundingBox(position);
        return new AABB(position.x - halfX, position.y, position.z - halfZ,
                position.x + halfX, position.y + box.height(), position.z + halfZ);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        if (box == null) return super.getDimensions(pose);
        return EntityDimensions.scalable((float) (2.0 * Math.max(halfX, halfZ)), (float) box.height());
    }
}
