package com.factorycraft;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ConveyorBlockEntity extends BlockEntity {
    /** Belt speed in blocks per tick (0.125 = 2.5 blocks/s). */
    public static final double SPEED = 0.125;
    /** How strongly items are pulled toward the belt's centre line. */
    private static final double CENTERING = 0.2;
    /** How far past the centre (along the belt) an item must be to drop into a container. */
    private static final double INSERT_POINT = 0.3;
    /** Item entities lose 0.04 blocks/tick to gravity every tick. */
    private static final double GRAVITY_COMPENSATION = 0.04;

    public ConveyorBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.CONVEYOR_BLOCK_ENTITY.value(), pos, state);
    }

    /** Runs on both sides: the server moves items/mobs, the client moves its own player smoothly. */
    public static void tick(Level level, BlockPos pos, BlockState state, ConveyorBlockEntity belt) {
        Direction facing = state.getValue(ConveyorBlock.FACING);
        double speed = state.getBlock() instanceof ConveyorBlock conveyor ? conveyor.speed() : SPEED;
        ConveyorSlope slope = state.getValue(ConveyorBlock.SLOPE);
        double dirX = facing.getStepX();
        double dirZ = facing.getStepZ();
        // Ramps reach up to 1.25 blocks, so look higher on them.
        double top = slope == ConveyorSlope.FLAT ? 0.6 : 1.6;
        AABB area = new AABB(pos.getX(), pos.getY() + 0.15, pos.getZ(),
                pos.getX() + 1.0, pos.getY() + top, pos.getZ() + 1.0);
        List<Entity> riders = level.getEntitiesOfClass(Entity.class, area, ConveyorBlockEntity::canMove);
        if (riders.isEmpty()) return;

        double centerX = pos.getX() + 0.5;
        double centerZ = pos.getZ() + 0.5;
        Container target = null;
        boolean targetChecked = false;

        for (Entity entity : riders) {
            Vec3 motion = entity.getDeltaMovement();
            if (entity instanceof ItemEntity item) {
                // Items ride exactly on the belt, kept on its centre line.
                double vx = dirX != 0 ? dirX * speed : (centerX - item.getX()) * CENTERING;
                double vz = dirZ != 0 ? dirZ * speed : (centerZ - item.getZ()) * CENTERING;
                double vy = motion.y;
                if (slope != ConveyorSlope.FLAT) {
                    // Keep the item on the ramp surface: climb (or descend) one block per
                    // block travelled, plus a correction toward the surface and gravity compensation.
                    double progress = Mth.clamp((item.getX() - centerX) * dirX + (item.getZ() - centerZ) * dirZ + 0.5, 0.0, 1.0);
                    double surface = pos.getY() + slope.surfaceAt(progress);
                    double climb = slope == ConveyorSlope.UP ? speed : -speed;
                    vy = climb + (surface - item.getY()) * 0.5 + GRAVITY_COMPENSATION;
                }
                item.setDeltaMovement(vx, vy, vz);

                if (!level.isClientSide() && slope == ConveyorSlope.FLAT) {
                    double progress = (item.getX() - centerX) * dirX + (item.getZ() - centerZ) * dirZ;
                    if (progress > INSERT_POINT) {
                        if (!targetChecked) {
                            target = HopperBlockEntity.getContainerAt(level, pos.relative(facing));
                            targetChecked = true;
                        }
                        if (target != null) HopperBlockEntity.addItem(target, item);
                    }
                }
            } else {
                // Mobs and players: push along the belt but let them step off sideways.
                double along = motion.x * dirX + motion.z * dirZ;
                if (along < speed) {
                    double boost = speed - along;
                    double vy = motion.y;
                    // Help walkers up a ramp so they ride it instead of hopping the steps.
                    if (slope == ConveyorSlope.UP && entity.onGround()) vy = Math.max(vy, speed);
                    entity.setDeltaMovement(motion.x + dirX * boost, vy, motion.z + dirZ * boost);
                }
            }
        }
    }

    private static boolean canMove(Entity entity) {
        if (entity.isSpectator() || entity.isPassenger() || entity.noPhysics) return false;
        if (entity instanceof Player player) {
            return !player.isShiftKeyDown() && !player.getAbilities().flying;
        }
        return true;
    }
}
