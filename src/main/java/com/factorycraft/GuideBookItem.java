package com.factorycraft;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** The FactoryCraft manual: right-click to read about every machine, upgrade and system. */
public final class GuideBookItem extends Item {
    public GuideBookItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) VehicleClient.openGuide();
        return InteractionResult.SUCCESS;
    }
}
