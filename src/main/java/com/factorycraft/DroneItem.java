package com.factorycraft;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Right-click to launch the drone next to you; then steer it with the drone controller. */
public final class DroneItem extends Item {
    public DroneItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        DroneEntity drone = FactoryCraft.DRONE.value().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (drone == null) return InteractionResult.FAIL;
        drone.setPos(player.getX(), player.getEyeY() + 1.0, player.getZ());
        drone.setOwner(player);
        level.addFreshEntity(drone);
        if (!player.getAbilities().instabuild) player.getItemInHand(hand).shrink(1);
        return InteractionResult.SUCCESS_SERVER;
    }
}
