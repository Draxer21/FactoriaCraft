package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** The Mechanic's titan hammer: like the hammer, but mines a 5x5 area facing the way you look. */
public final class TitanHammerItem extends Item {
    private static final int RADIUS = 2;

    public TitanHammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        super.mineBlock(stack, level, state, pos, owner);
        if (level.isClientSide() || state.getDestroySpeed(level, pos) <= 0 || !(owner instanceof Player player)) return true;
        Vec3 look = owner.getLookAngle();
        double ax = Math.abs(look.x);
        double ay = Math.abs(look.y);
        double az = Math.abs(look.z);
        Direction across;
        Direction other;
        if (ay >= ax && ay >= az) {
            across = Direction.EAST;
            other = Direction.SOUTH;
        } else if (ax >= az) {
            across = Direction.UP;
            other = Direction.SOUTH;
        } else {
            across = Direction.UP;
            other = Direction.EAST;
        }
        for (int a = -RADIUS; a <= RADIUS; a++) {
            for (int b = -RADIUS; b <= RADIUS; b++) {
                if (a == 0 && b == 0) continue;
                if (stack.isEmpty() || stack.getMaxDamage() - stack.getDamageValue() <= 1) return true;
                BlockPos target = pos.relative(across, a).relative(other, b);
                BlockState extra = level.getBlockState(target);
                if (extra.isAir() || extra.getDestroySpeed(level, target) < 0 || !stack.isCorrectToolForDrops(extra)) continue;
                if (level.destroyBlock(target, true, player, 512)) stack.hurtAndBreak(1, owner, EquipmentSlot.MAINHAND);
            }
        }
        return true;
    }
}
