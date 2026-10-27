package com.factorycraft;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Cargo ship: a fast freighter for rivers and seas with a 27-slot hold. Loading and
 * unloading stations on the quay fill and empty it, and with the autopilot module
 * it sails a route of beacons by itself. On land it can barely crawl.
 */
public final class CargoShipEntity extends WorkVehicleEntity {
    public CargoShipEntity(EntityType<? extends CargoShipEntity> type, Level level) {
        super(type, level, () -> Logistics.CARGO_SHIP_ITEM.value());
    }

    @Override protected double maxSpeed() { return 0.55; }
    @Override protected double reverseSpeed() { return 0.15; }
    @Override protected double seatHeight() { return 2.4; }
    @Override protected double seatForward() { return -2.6; }
    @Override protected double hullShift() { return 0.0; }
    @Override protected double hullHalfLength() { return 4.2; }
    @Override protected double hullHalfWidth() { return 1.6; }
    @Override protected int workInterval() { return 20; }

    @Override
    public void move(MoverType type, Vec3 delta) {
        if (type == MoverType.SELF && !isInWater() && onGround()) delta = new Vec3(delta.x * 0.15, delta.y, delta.z * 0.15);
        super.move(type, delta);
    }

    @Override
    protected void work(ServerLevel level, Player driver) {
        // Nothing to dig: it just carries.
    }
}
