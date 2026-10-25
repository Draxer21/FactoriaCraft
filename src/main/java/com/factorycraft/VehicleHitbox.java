package com.factorycraft;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.entity.PartEntity;

/**
 * Segmented collision for a vehicle: a few boxes fitted to the model (cab, body, blade, tail…)
 * that turn with it, instead of one square box around the whole machine.
 *
 * <p>The vehicle keeps its big box only for driving over terrain; other entities collide with,
 * stand on and click the segments.
 */
public final class VehicleHitbox {
    /** A segment in the vehicle's own frame, in blocks: forward, left (from the centre) and up (from the ground). */
    public record Box(double back, double front, double right, double left, double bottom, double top) {
        double centerForward() {
            return (back + front) / 2.0;
        }

        double centerLeft() {
            return (right + left) / 2.0;
        }

        double halfLength() {
            return (front - back) / 2.0;
        }

        double halfWidth() {
            return (left - right) / 2.0;
        }

        double height() {
            return top - bottom;
        }
    }

    /** Implemented by segmented vehicles: whether {@code other} bumps into the segments. */
    public interface Holder {
        VehicleHitbox hitbox();

        boolean solidFor(Entity other);
    }

    public static Box box(double back, double front, double right, double left, double bottom, double top) {
        return new Box(back, front, right, left, bottom, top);
    }

    /** The vehicle behind a segment (or the entity itself). */
    public static Entity owner(Entity entity) {
        return entity instanceof VehiclePart part ? part.getParent() : entity;
    }

    private final AbstractBoat owner;
    private final VehiclePart[] parts;

    public VehicleHitbox(AbstractBoat owner, Box... boxes) {
        this.owner = owner;
        this.parts = new VehiclePart[boxes.length];
        for (int i = 0; i < boxes.length; i++) {
            parts[i] = new VehiclePart(owner, boxes[i]);
        }
        // Server entities get their id in the constructor (the segments' come right after it);
        // on the client the id arrives later with the spawn packet, through setId.
        if (!owner.level().isClientSide()) setIds(owner.getId());
        update();
    }

    public PartEntity<?>[] parts() {
        return parts;
    }

    /** Segment ids follow the vehicle's, so client and server agree on them. */
    public void setIds(int id) {
        for (int i = 0; i < parts.length; i++) {
            parts[i].setId(id + i + 1);
        }
    }

    /** Moves and turns the segments with the vehicle; call every tick on both sides. */
    public void update() {
        double yaw = Math.toRadians(owner.getYRot());
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        for (VehiclePart part : parts) {
            Box b = part.box;
            double f = b.centerForward();
            double l = b.centerLeft();
            double x = owner.getX() - sin * f + cos * l;
            double z = owner.getZ() + cos * f + sin * l;
            part.halfX = Math.abs(sin) * b.halfLength() + Math.abs(cos) * b.halfWidth();
            part.halfZ = Math.abs(cos) * b.halfLength() + Math.abs(sin) * b.halfWidth();
            double y = owner.getY() + b.bottom();
            part.xo = part.getX();
            part.yo = part.getY();
            part.zo = part.getZ();
            part.xOld = part.getX();
            part.yOld = part.getY();
            part.zOld = part.getZ();
            part.setPos(x, y, z);
        }
    }

    /** Whether {@code entity} actually touches a segment (vehicles only push what they touch). */
    public boolean touches(Entity entity) {
        AABB box = entity.getBoundingBox();
        for (VehiclePart part : parts) {
            if (part.getBoundingBox().inflate(0.2, 0.0, 0.2).intersects(box)) return true;
        }
        return false;
    }
}
