package com.factorycraft;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Blueprint: right-click one corner of a structure, then the opposite corner, and it
 * copies everything in between (up to 32×32×32). Put it in a builder to build the copy.
 * Sneak + right-click the air clears it.
 */
public final class BlueprintItem extends Item {
    public BlueprintItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int[] corner = tag.getIntArray("CornerA").orElse(null);
        if (corner == null || corner.length != 3) {
            tag.putIntArray("CornerA", new int[] {clicked.getX(), clicked.getY(), clicked.getZ()});
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            player.sendOverlayMessage(Component.translatable("message.factorycraft.blueprint.corner"));
            return InteractionResult.SUCCESS;
        }
        Blueprint blueprint = Blueprint.capture(context.getLevel(), new BlockPos(corner[0], corner[1], corner[2]), clicked);
        if (blueprint == null) {
            tag.remove("CornerA");
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            player.sendOverlayMessage(Component.translatable("message.factorycraft.blueprint.too_big",
                    Blueprint.MAX_SIDE, Blueprint.MAX_BLOCKS));
            return InteractionResult.SUCCESS;
        }
        blueprint.writeTo(stack);
        player.sendOverlayMessage(Component.translatable("message.factorycraft.blueprint.saved",
                blueprint.sizeX(), blueprint.sizeY(), blueprint.sizeZ(), blueprint.count()));
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(net.minecraft.world.level.Level level, Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            stack.remove(DataComponents.CUSTOM_DATA);
            player.sendOverlayMessage(Component.translatable("message.factorycraft.blueprint.cleared"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        int[] summary = Blueprint.summary(stack);
        if (summary != null) {
            tooltip.accept(Component.translatable("tooltip.factorycraft.blueprint.size", summary[0], summary[1], summary[2], summary[3])
                    .withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.accept(Component.translatable("tooltip.factorycraft.blueprint.empty").withStyle(ChatFormatting.GRAY));
        }
    }
}
