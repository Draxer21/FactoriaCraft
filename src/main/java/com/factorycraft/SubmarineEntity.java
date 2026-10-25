package com.factorycraft;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Mining submarine: moves freely underwater (jump = up, sprint = down) and its
 * nose drill digs a 3x3 hole ahead while driving forward, ores and all, into
 * its 27-slot bay. The pilot can breathe inside. Out of water it crawls slowly.
 */
public final class SubmarineEntity extends WorkVehicleEntity {
    private static final double DIVE = 0.12;
    private double vertical;

    public SubmarineEntity(EntityType<? extends SubmarineEntity> type, Level level) {
        super(type, level, () -> FactoryCraft.SUBMARINE_ITEM.value());
    }

    @Override protected double maxSpeed() { return 0.32; }
    @Override protected double reverseSpeed() { return 0.15; }
    @Override protected double seatHeight() { return 1.6; }
    @Override protected double seatForward() { return 0.0; }
    @Override protected double hullShift() { return 0.2; }
    @Override protected double hullHalfLength() { return 3.0; }
    @Override protected double hullHalfWidth() { return 1.0; }
    @Override protected int workInterval() { return 6; }

    @Override
    public void ejectPassengers() {
        // Sealed: keep the pilot aboard while submerged.
        if (getControllingPassenger() instanceof Player && isUnderWater()) return;
        super.ejectPassengers();
    }

    @Override
    public void move(MoverType type, Vec3 delta) {
        if (type == MoverType.SELF && isInWater()) {
            boolean piloted = isLocalInstanceAuthoritative() && getControllingPassenger() instanceof Player && engine().isRunning();
            double target = piloted ? HeliControls.vertical * DIVE : 0.0;
            vertical += (target - vertical) * 0.2;
            delta = new Vec3(delta.x, vertical, delta.z);
            setDeltaMovement(getDeltaMovement().x, vertical, getDeltaMovement().z);
        } else {
            vertical = 0.0;
            if (type == MoverType.SELF && onGround()) delta = new Vec3(delta.x * 0.35, delta.y, delta.z * 0.35);
        }
        super.move(type, delta);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount % 20 == 0) {
            for (Entity passenger : getPassengers()) {
                if (passenger instanceof LivingEntity living) {
                    living.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0, true, false));
                    living.setAirSupply(living.getMaxAirSupply());
                }
            }
        }
    }

    /** Drills 3x3 ahead of the nose, centred on the hull's middle height. */
    @Override
    protected void work(ServerLevel level, Player driver) {
        List<Container> sinks = sinks(level);
        int centerY = (int) Math.floor(getY() + 0.9);
        double ahead = hullFront() + 0.6;
        int half = engine().wide() ? 2 : 1;
        for (int side = -half; side <= half; side++) {
            for (int dy = -1; dy <= 1; dy++) {
                BlockPos pos = columnAt(ahead, side, centerY + dy);
                BlockState state = level.getBlockState(pos);
                if (state.isAir() || !state.getFluidState().isEmpty() && state.getFluidState().isSource() && state.canBeReplaced()) continue;
                if (!isBreakable(level, pos, state)) continue;
                breakInto(level, pos, state, driver, sinks);
            }
        }
    }
}
