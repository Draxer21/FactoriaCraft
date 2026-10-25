package com.factorycraft;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Energy meter: right-click a cable, generator, battery or machine to see its
 * whole network: generators and what they make right now, stored energy,
 * batteries, machines and cable length.
 */
public final class EnergyMeterItem extends Item {
    public EnergyMeterItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        boolean onNetwork = level.getBlockState(pos).getBlock() instanceof CableBlock
                || level.getBlockEntity(pos) instanceof Energy.Source || level.getBlockEntity(pos) instanceof Energy.Consumer;
        if (!onNetwork) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        Energy.Network network = Energy.survey(level, pos);
        int generators = 0;
        int batteries = 0;
        long stored = 0;
        long capacity = 0;
        long making = 0;
        long batteryStored = 0;
        for (BlockPos p : network.sources()) {
            if (!(level.getBlockEntity(p) instanceof Energy.Source source)) continue;
            if (source.isStorage()) {
                batteries++;
                batteryStored += source.stored();
            } else {
                generators++;
                making += source.rate();
            }
            stored += source.stored();
            capacity += source.capacity();
        }
        player.sendSystemMessage(Component.translatable("message.factorycraft.meter.title").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.translatable("message.factorycraft.meter.generators", generators, making));
        player.sendSystemMessage(Component.translatable("message.factorycraft.meter.stored",
                String.format("%,d", stored), String.format("%,d", capacity)));
        player.sendSystemMessage(Component.translatable("message.factorycraft.meter.batteries", batteries,
                String.format("%,d", batteryStored)));
        player.sendSystemMessage(Component.translatable("message.factorycraft.meter.machines",
                network.consumers().size(), network.cables()));
        if (generators == 0 && batteries == 0) {
            player.sendSystemMessage(Component.translatable("message.factorycraft.meter.none").withStyle(ChatFormatting.RED));
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
