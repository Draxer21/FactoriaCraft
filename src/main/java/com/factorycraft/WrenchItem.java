package com.factorycraft;

import net.minecraft.world.item.Item;

/**
 * Right-click a machine to take out all its upgrade modules; sneak + right-click
 * a face of a machine to choose what that face does (normal, input, output, off).
 */
public final class WrenchItem extends Item {
    public WrenchItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** Sneak-clicking a machine with the wrench still reaches the machine (side configuration). */
    @Override
    public boolean doesSneakBypassUse(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos,
                                      net.minecraft.world.entity.player.Player player) {
        return true;
    }
}
