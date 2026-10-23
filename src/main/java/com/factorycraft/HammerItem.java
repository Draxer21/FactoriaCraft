package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class HammerItem extends Item {
    public HammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        super.mineBlock(stack, level, state, pos, owner);
        if (!level.isClientSide() && state.getDestroySpeed(level, pos) > 0 && owner instanceof net.minecraft.world.entity.player.Player player) {
            Vec3 look = owner.getLookAngle();
            double absX = Math.abs(look.x);
            double absY = Math.abs(look.y);
            double absZ = Math.abs(look.z);
            Direction across;
            Direction other;
            if (absY >= absX && absY >= absZ) {
                // Looking mostly up/down: mine a horizontal 3x3 plane.
                across = Direction.EAST;
                other = Direction.SOUTH;
            } else if (absX >= absZ) {
                // Looking mostly east/west: mine the vertical Z/Y plane.
                across = Direction.UP;
                other = Direction.SOUTH;
            } else {
                // Looking mostly north/south: mine the vertical X/Y plane.
                across = Direction.UP;
                other = Direction.EAST;
            }
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    if (a == 0 && b == 0) continue;
                    BlockPos target = pos.relative(across, a).relative(other, b);
                    BlockState extra = level.getBlockState(target);
                    if (extra.isAir() || extra.getDestroySpeed(level, target) < 0 || !stack.isCorrectToolForDrops(extra)) continue;
                    if (level.destroyBlock(target, true, player, 512)) {
                        stack.hurtAndBreak(1, owner, EquipmentSlot.MAINHAND);
                    }
                }
            }
        }
        return true;
    }
}
