package com.factorycraft;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Cargo helicopter: flies (jump = up, sprint = down, W/S/A/D as usual) and
 * lifts structures with the crane control like the crawler crane, carrying
 * them underneath. With the engine off it comes down.
 */
public final class HelicopterEntity extends CraneEntity {
    private static final double CLIMB = 0.22;
    private double vertical;

    public HelicopterEntity(EntityType<? extends HelicopterEntity> type, Level level) {
        super(type, level, () -> FactoryCraft.HELICOPTER_ITEM.value());
    }

    @Override public double reach() { return 32.0; }
    @Override protected double maxDistance() { return 14.0; }
    @Override protected int[] liftSizes() { return new int[] {1, 3, 5, 7}; }
    @Override public float hookForward() { return 0.0F; }
    @Override public float hookHeight() { return -0.2F; }
    @Override public float maxLoadHeight() { return 4.0F; }

    @Override protected double maxSpeed() { return 0.55; }
    @Override protected double reverseSpeed() { return 0.2; }
    @Override protected double seatHeight() { return 0.5; }
    @Override protected double seatForward() { return 0.15; }
    @Override protected double hullShift() { return -0.3; }
    @Override protected double hullHalfLength() { return 2.3; }
    @Override protected double hullHalfWidth() { return 1.35; }

    @Override
    public float maxUpStep() {
        return 0.6F;
    }

    @Override
    public void move(MoverType type, Vec3 delta) {
        if (type == MoverType.SELF && isLocalInstanceAuthoritative() && getControllingPassenger() instanceof Player
                && engine().isRunning()) {
            double target = HeliControls.vertical * CLIMB;
            vertical += (target - vertical) * 0.25;
            delta = new Vec3(delta.x, vertical, delta.z);
            setDeltaMovement(getDeltaMovement().x, vertical, getDeltaMovement().z);
        } else if (type == MoverType.SELF && !onGround()) {
            // Engine off or nobody at the controls: autorotate down gently.
            vertical = Math.max(vertical - 0.02, -0.35);
            delta = new Vec3(delta.x * 0.95, vertical, delta.z * 0.95);
        } else {
            vertical = 0.0;
        }
        super.move(type, delta);
    }

    @Override
    public void tick() {
        super.tick();
        resetFallDistance();
    }
}
