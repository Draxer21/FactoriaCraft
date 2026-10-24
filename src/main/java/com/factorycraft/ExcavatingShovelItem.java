package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Excavating shovel: digs a 3x3 square of dirt, sand, gravel, snow, clay...
 * facing the player, like the hammer does with stone. Sneak to dig a single
 * block. Right-click still makes paths like a normal shovel.
 */
public final class ExcavatingShovelItem extends ShovelItem {
    public ExcavatingShovelItem(ToolMaterial material, float damage, float speed, Properties properties) {
        super(material, damage, speed, properties);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        super.mineBlock(stack, level, state, pos, owner);
        HammerItem.mineArea(stack, level, state, pos, owner);
        return true;
    }
}
