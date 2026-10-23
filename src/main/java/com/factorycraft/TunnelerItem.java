package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class TunnelerItem extends Item {
    public TunnelerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockPos placeAt = context.getClickedPos().relative(context.getClickedFace());
        TunnelerEntity tunneler = FactoryCraft.TUNNELER.value().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (tunneler == null) return InteractionResult.FAIL;

        Player player = context.getPlayer();
        if (player != null) tunneler.setYRot(player.getYRot());
        tunneler.setInitialPos(placeAt.getX() + 0.5, placeAt.getY(), placeAt.getZ() + 0.5);
        if (!level.noCollision(tunneler, tunneler.getBoundingBox()) || !level.addFreshEntity(tunneler)) {
            return InteractionResult.FAIL;
        }
        if (player != null) player.startRiding(tunneler);
        if (player != null && !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
