package com.factorycraft;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SorterBlockEntity extends BlockEntity {
    private static final double CENTERING = 0.2;
    private static final double INSERT_POINT = 0.3;

    private ItemStack filter = ItemStack.EMPTY;

    public SorterBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.SORTER_BLOCK_ENTITY.value(), pos, state);
    }

    public ItemStack getFilter() {
        return filter;
    }

    public void setFilter(ItemStack stack) {
        filter = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        setChanged();
    }

    private boolean matches(ItemStack stack) {
        return !filter.isEmpty() && ItemStack.isSameItem(stack, filter);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SorterBlockEntity sorter) {
        Direction facing = state.getValue(SorterBlock.FACING);
        Direction left = facing.getCounterClockWise();
        AABB area = new AABB(pos.getX(), pos.getY() + 0.15, pos.getZ(),
                pos.getX() + 1.0, pos.getY() + 0.6, pos.getZ() + 1.0);
        List<Entity> riders = level.getEntitiesOfClass(Entity.class, area,
                e -> !e.isSpectator() && !e.isPassenger() && !(e instanceof Player p && p.isShiftKeyDown()));
        double centerX = pos.getX() + 0.5;
        double centerZ = pos.getZ() + 0.5;

        for (Entity entity : riders) {
            Vec3 motion = entity.getDeltaMovement();
            if (entity instanceof ItemEntity item) {
                Direction out = sorter.matches(item.getItem()) ? left : facing;
                double dirX = out.getStepX();
                double dirZ = out.getStepZ();
                double speed = ConveyorBlockEntity.SPEED;
                double vx = dirX != 0 ? dirX * speed : (centerX - item.getX()) * CENTERING;
                double vz = dirZ != 0 ? dirZ * speed : (centerZ - item.getZ()) * CENTERING;
                item.setDeltaMovement(vx, motion.y, vz);

                if (!level.isClientSide()) {
                    double progress = (item.getX() - centerX) * dirX + (item.getZ() - centerZ) * dirZ;
                    if (progress > INSERT_POINT) {
                        Container target = HopperBlockEntity.getContainerAt(level, pos.relative(out));
                        if (target != null) HopperBlockEntity.addItem(target, item);
                    }
                }
            } else {
                double along = motion.x * facing.getStepX() + motion.z * facing.getStepZ();
                double speed = ConveyorBlockEntity.SPEED;
                if (along < speed) {
                    double boost = speed - along;
                    entity.setDeltaMovement(motion.x + facing.getStepX() * boost, motion.y, motion.z + facing.getStepZ() * boost);
                }
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        filter = input.read("Filter", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!filter.isEmpty()) output.store("Filter", ItemStack.CODEC, filter);
    }
}
