package com.factorycraft;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Satellite uplink: with a survey satellite in orbit, right-click to get a report:
 * the ores in the chunk you stand in, the richest oil deposit within 8 chunks,
 * and the air quality.
 */
public final class UplinkItem extends Item {
    private static final TagKey<Block> ORES = TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
            net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "ores"));

    public UplinkItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** Ore counts in a chunk, most common first. */
    static Map<Block, Integer> ores(Level level, ChunkPos chunk) {
        Map<Block, Integer> found = new HashMap<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                for (int y = level.getMinY(); y < level.getMaxY(); y++) {
                    BlockState state = level.getBlockState(pos.set(x, y, z));
                    if (state.is(ORES)) found.merge(state.getBlock(), 1, Integer::sum);
                }
            }
        }
        return found;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (Orbit.get(level).survey() <= 0) {
            player.sendOverlayMessage(Component.translatable("message.factorycraft.uplink.no_satellite"));
            return InteractionResult.FAIL;
        }
        ChunkPos here = ChunkPos.containing(player.blockPosition());
        player.sendSystemMessage(Component.translatable("message.factorycraft.uplink.header", here.getMinBlockX() >> 4, here.getMinBlockZ() >> 4));
        Map<Block, Integer> ores = ores(level, here);
        if (ores.isEmpty()) player.sendSystemMessage(Component.translatable("message.factorycraft.uplink.no_ores"));
        ores.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(6).forEach(e ->
                player.sendSystemMessage(Component.translatable("message.factorycraft.uplink.ore", e.getKey().getName(), e.getValue())));
        ChunkPos best = null;
        int bestOil = 0;
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                ChunkPos c = ChunkPos.containing(new BlockPos(here.getMinBlockX() + dx * 16, 0, here.getMinBlockZ() + dz * 16));
                int oil = OilDeposits.richness(level, c);
                if (oil > bestOil) {
                    bestOil = oil;
                    best = c;
                }
            }
        }
        if (best != null) {
            player.sendSystemMessage(Component.translatable("message.factorycraft.uplink.oil", bestOil,
                    best.getMinBlockX() + 8, best.getMinBlockZ() + 8));
        }
        int air = AirQuality.get(level, player.blockPosition());
        player.sendSystemMessage(Component.translatable("message.factorycraft.air_meter", air,
                Component.translatable("message.factorycraft.air_meter." + AirMeterItem.band(air))));
        return InteractionResult.SUCCESS_SERVER;
    }

    static String id(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }
}
