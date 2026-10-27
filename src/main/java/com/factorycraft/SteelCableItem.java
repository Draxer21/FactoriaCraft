package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Steel cable spool: right-click a cable car station, then another one (up to 160
 * blocks away) to string a line between them. A cabin starts running right away.
 */
public final class SteelCableItem extends Item {
    private static final String KEY = "CableFrom";

    public SteelCableItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof CableStationBlockEntity station)) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int[] from = tag.getIntArray(KEY).orElse(new int[0]);
        if (from.length != 3 || station.linked()) {
            if (station.linked()) {
                if (player != null) player.sendOverlayMessage(Component.translatable("message.factorycraft.cable.busy"));
                return InteractionResult.FAIL;
            }
            tag.putIntArray(KEY, new int[] {pos.getX(), pos.getY(), pos.getZ()});
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            if (player != null) player.sendOverlayMessage(Component.translatable("message.factorycraft.cable.start"));
            return InteractionResult.SUCCESS_SERVER;
        }
        BlockPos start = new BlockPos(from[0], from[1], from[2]);
        tag.remove(KEY);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        Component problem = null;
        double horizontal = Math.sqrt(Math.pow(start.getX() - pos.getX(), 2) + Math.pow(start.getZ() - pos.getZ(), 2));
        if (start.equals(pos) || horizontal < 4) problem = Component.translatable("message.factorycraft.cable.short");
        else if (start.distSqr(pos) > (double) CableStationBlockEntity.MAX_LENGTH * CableStationBlockEntity.MAX_LENGTH)
            problem = Component.translatable("message.factorycraft.cable.far", CableStationBlockEntity.MAX_LENGTH);
        else if (!(level.getBlockEntity(start) instanceof CableStationBlockEntity first) || first.linked())
            problem = Component.translatable("message.factorycraft.cable.lost");
        else {
            first.link(pos, true);
            station.link(start, false);
            CableCarEntity.create(server, start, pos);
            level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.0F, 0.6F);
            if (player != null) {
                player.sendOverlayMessage(Component.translatable("message.factorycraft.cable.done", (int) Math.sqrt(start.distSqr(pos))));
                if (!player.isCreative()) stack.shrink(1);
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        if (player != null) player.sendOverlayMessage(problem);
        return InteractionResult.FAIL;
    }
}
