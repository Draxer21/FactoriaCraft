package com.factorycraft;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /factorycraft air          shows the air quality where you stand;
 * /factorycraft air <0-1000> sets it (creative mode, command blocks and the console only).
 */
@EventBusSubscriber(modid = FactoryCraft.MOD_ID)
public final class FactoryCommands {
    private FactoryCommands() {}

    private static boolean mayChange(CommandSourceStack source) {
        return !(source.getEntity() instanceof ServerPlayer player) || player.isCreative();
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("factorycraft")
                .then(Commands.literal("research")
                        .executes(context -> {
                            if (context.getSource().getEntity() instanceof ServerPlayer player) ResearchBlocks.sendTree(player);
                            return Research.state(context.getSource().getLevel()).points();
                        })
                        .then(Commands.literal("unlockall").executes(context -> {
                            CommandSourceStack source = context.getSource();
                            if (!mayChange(source)) {
                                source.sendFailure(Component.translatable("message.factorycraft.command.creative"));
                                return 0;
                            }
                            for (Research.Node node : Research.TREE) {
                                Research.addPoints(source.getLevel(), node.cost());
                                Research.unlock(source.getLevel(), node.id());
                            }
                            source.sendSuccess(() -> Component.translatable("message.factorycraft.research.all"), true);
                            return Research.TREE.size();
                        })))
                .then(Commands.literal("air")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            int air = AirQuality.get(source.getLevel(), BlockPos.containing(source.getPosition()));
                            source.sendSuccess(() -> Component.translatable("message.factorycraft.air_meter", air,
                                    Component.translatable("message.factorycraft.air_meter." + AirMeterItem.band(air))), false);
                            return air;
                        })
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, AirQuality.MAX))
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();
                                    if (!mayChange(source)) {
                                        source.sendFailure(Component.translatable("message.factorycraft.command.creative"));
                                        return 0;
                                    }
                                    BlockPos pos = BlockPos.containing(source.getPosition());
                                    int want = IntegerArgumentType.getInteger(context, "value");
                                    AirQuality.add(source.getLevel(), pos, want - AirQuality.get(source.getLevel(), pos));
                                    int now = AirQuality.get(source.getLevel(), pos);
                                    source.sendSuccess(() -> Component.translatable("message.factorycraft.air_meter", now,
                                            Component.translatable("message.factorycraft.air_meter." + AirMeterItem.band(now))), true);
                                    return now;
                                }))));
    }
}
