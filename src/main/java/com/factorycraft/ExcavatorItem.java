package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class ExcavatorItem extends Item {
    public ExcavatorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockPos placeAt = context.getClickedPos().relative(context.getClickedFace());
        ExcavatorEntity excavator = FactoryCraft.EXCAVATOR.value().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (excavator == null) return InteractionResult.FAIL;

        Player player = context.getPlayer();
        if (player != null) excavator.setYRot(player.getYRot());
        excavator.setInitialPos(placeAt.getX() + 0.5, placeAt.getY(), placeAt.getZ() + 0.5);
        if (!level.noCollision(excavator, excavator.getBoundingBox()) || !level.addFreshEntity(excavator)) {
            return InteractionResult.FAIL;
        }
        if (player != null) player.startRiding(excavator);
        if (player != null && !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
