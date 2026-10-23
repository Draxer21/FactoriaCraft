package com.factorycraft;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Tower crane: a stationary crane 17 blocks tall with a 15-block jib. It doesn't
 * drive; A / D slew the whole crane around its tower. The operator sits in the
 * cab at the top and works with the crane control as usual, but with a much
 * longer reach and loads up to 9×9×9.
 */
public final class TowerCraneEntity extends CraneEntity {
    public TowerCraneEntity(EntityType<? extends TowerCraneEntity> type, Level level) {
        super(type, level, () -> FactoryCraft.TOWER_CRANE_ITEM.value());
    }

    @Override protected double maxSpeed() { return 0.0; }
    @Override protected double reverseSpeed() { return 0.0; }
    @Override protected double seatHeight() { return 16.875; }
    @Override protected double seatForward() { return 1.08; }
    @Override protected double hullShift() { return 0.0; }
    @Override protected double hullHalfLength() { return 1.5; }
    @Override protected double hullHalfWidth() { return 1.5; }

    @Override public double reach() { return 48.0; }
    @Override protected double maxDistance() { return 40.0; }
    @Override protected int maxLiftSize() { return 9; }
    @Override public float hookForward() { return 11.25F; }
    @Override public float hookHeight() { return 6.47F; }
    @Override public float maxLoadHeight() { return 6.0F; }
}
