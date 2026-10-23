package com.factorycraft;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Places a driveable machine facing the way the player looks, and seats the player in it. */
public final class VehicleItem extends Item {
    private final Supplier<? extends EntityType<? extends AbstractBoat>> type;

    public VehicleItem(Properties properties, Supplier<? extends EntityType<? extends AbstractBoat>> type) {
        super(properties.stacksTo(1));
        this.type = type;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockPos placeAt = context.getClickedPos().relative(context.getClickedFace());
        AbstractBoat vehicle = type.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (vehicle == null) return InteractionResult.FAIL;

        Player player = context.getPlayer();
        if (player != null) vehicle.setYRot(player.getYRot());
        vehicle.setInitialPos(placeAt.getX() + 0.5, placeAt.getY(), placeAt.getZ() + 0.5);
        if (!level.noCollision(vehicle, vehicle.getBoundingBox()) || !level.addFreshEntity(vehicle)) {
            return InteractionResult.FAIL;
        }
        if (player != null) player.startRiding(vehicle);
        if (player != null && !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
