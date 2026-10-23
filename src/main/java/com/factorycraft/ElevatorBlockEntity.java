package com.factorycraft;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

public final class ElevatorBlockEntity extends BlockEntity {
    /** Climb speed in blocks per tick (0.2 = 4 blocks/s). */
    private static final double LIFT_SPEED = 0.2;
    private static final double SINK_SPEED = -0.12;
    private static final double CENTERING = 0.25;
    /** Height in the top block at which items get pushed out of the front. */
    private static final double EXIT_HEIGHT = 0.6;

    public ElevatorBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.ELEVATOR_BLOCK_ENTITY.value(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ElevatorBlockEntity elevator) {
        AABB shaft = new AABB(pos).deflate(0.05, 0.0, 0.05);
        List<Entity> riders = level.getEntitiesOfClass(Entity.class, shaft,
                e -> !e.isSpectator() && !e.isPassenger() && !e.noPhysics);
        if (riders.isEmpty()) return;

        Direction front = state.getValue(ElevatorBlock.FACING);
        boolean top = !(level.getBlockState(pos.above()).getBlock() instanceof ElevatorBlock);
        double centerX = pos.getX() + 0.5;
        double centerZ = pos.getZ() + 0.5;

        for (Entity entity : riders) {
            Vec3 motion = entity.getDeltaMovement();
            if (entity instanceof Player player && (player.isShiftKeyDown() || player.getAbilities().flying)) {
                if (player.isShiftKeyDown()) entity.setDeltaMovement(motion.x, SINK_SPEED, motion.z);
                entity.resetFallDistance();
                continue;
            }
            double height = entity.getY() - pos.getY();
            if (entity instanceof ItemEntity item && top && height >= EXIT_HEIGHT) {
                // Leave through the front, onto a belt or into a container.
                item.setDeltaMovement(front.getStepX() * ConveyorBlockEntity.SPEED, 0.02, front.getStepZ() * ConveyorBlockEntity.SPEED);
                if (!level.isClientSide()) {
                    double out = (item.getX() - centerX) * front.getStepX() + (item.getZ() - centerZ) * front.getStepZ();
                    if (out > 0.3) {
                        Container target = HopperBlockEntity.getContainerAt(level, pos.relative(front));
                        if (target != null) HopperBlockEntity.addItem(target, item);
                    }
                }
                continue;
            }
            double vx = entity instanceof ItemEntity ? (centerX - entity.getX()) * CENTERING : motion.x;
            double vz = entity instanceof ItemEntity ? (centerZ - entity.getZ()) * CENTERING : motion.z;
            // At the very top, stop living things at the rim instead of launching them.
            double vy = top && !(entity instanceof ItemEntity) && height > 0.9 ? Math.max(motion.y, 0.0) : LIFT_SPEED;
            entity.setDeltaMovement(vx, vy, vz);
            entity.resetFallDistance();
        }
    }
}
