package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Places a cargo wagon; it hitches straight away to a free tunneler or train tail nearby. */
public final class CargoWagonItem extends Item {
    /** How far from the placed wagon a tunneler or wagon can be to hitch on placement. */
    private static final double PLACE_HITCH_RANGE = 8.0;

    public CargoWagonItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;

        BlockPos placeAt = context.getClickedPos().relative(context.getClickedFace());
        CargoWagonEntity wagon = FactoryCraft.CARGO_WAGON.value().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (wagon == null) return InteractionResult.FAIL;

        Player player = context.getPlayer();
        if (player != null) wagon.setYRot(player.getYRot());
        wagon.setInitialPos(placeAt.getX() + 0.5, placeAt.getY(), placeAt.getZ() + 0.5);

        Entity leader = CargoWagonEntity.findFreeLeader(serverLevel, wagon.position(), PLACE_HITCH_RANGE, wagon);
        if (leader != null) wagon.hitchTo(leader);

        if (!level.addFreshEntity(wagon)) return InteractionResult.FAIL;
        if (player != null && !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
