package com.factorycraft;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Remote for the crane, used from the crane's seat. Right-click a block to
 * lift the structure there or to set the load down; right-click the air to
 * change the lift size. Aims much further than a normal hand.
 */
public final class CraneControlItem extends Item {
    public CraneControlItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        // Runs before the clicked block reacts, so chests get lifted instead of opened.
        Player player = context.getPlayer();
        if (player == null || !(player.getVehicle() instanceof CraneEntity)) return InteractionResult.PASS;
        return operate(context.getLevel(), player);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player.getVehicle() instanceof CraneEntity)) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.factorycraft.crane.not_seated"));
            return InteractionResult.FAIL;
        }
        return operate(level, player);
    }

    private static InteractionResult operate(Level level, Player player) {
        if (!(level instanceof ServerLevel serverLevel) || !(player.getVehicle() instanceof CraneEntity crane)) {
            return InteractionResult.SUCCESS;
        }
        // Long-range aim that looks through the crane itself.
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(crane.reach()));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            crane.operate(serverLevel, player, null, null);
        } else {
            crane.operate(serverLevel, player, hit.getBlockPos(), hit.getDirection());
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
