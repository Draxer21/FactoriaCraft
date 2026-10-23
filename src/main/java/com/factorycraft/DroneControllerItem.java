package com.factorycraft;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Drone remote: right-click to send your drone to where you're looking (up to
 * 96 blocks; it scans for ores when it gets there). Sneak + right-click to call
 * it back to you.
 */
public final class DroneControllerItem extends Item {
    private static final double REACH = 96.0;
    private static final double FIND_RANGE = 256.0;

    public DroneControllerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        List<DroneEntity> drones = server.getEntitiesOfClass(DroneEntity.class,
                player.getBoundingBox().inflate(FIND_RANGE), drone -> drone.isOwnedBy(player));
        if (drones.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.factorycraft.drone.none"));
            return InteractionResult.FAIL;
        }
        if (player.isSecondaryUseActive()) {
            drones.forEach(DroneEntity::recall);
            player.sendOverlayMessage(Component.translatable("message.factorycraft.drone.recall"));
            return InteractionResult.SUCCESS_SERVER;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(REACH));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 point = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
        drones.forEach(drone -> drone.sendTo(point));
        player.sendOverlayMessage(Component.translatable("message.factorycraft.drone.sent",
                (int) Math.round(point.distanceTo(player.position()))));
        return InteractionResult.SUCCESS_SERVER;
    }
}
