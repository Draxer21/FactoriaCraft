package com.factorycraft;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * Oil detector: right-click to read the oil under the 5×5 chunks around you
 * (your chunk in the middle, north up), for a pumpjack placed at your height.
 * Sneak + right-click reads the deep layer (below Y 0) instead.
 */
public final class OilDetectorItem extends Item {
    public OilDetectorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        BlockPos at = player.blockPosition();
        int y = player.isShiftKeyDown() ? -32 : at.getY();
        ChunkPos center = ChunkPos.containing(at);
        player.sendSystemMessage(Component.translatable(y < 0 ? "message.factorycraft.oil_detector.deep" : "message.factorycraft.oil_detector.title")
                .withStyle(ChatFormatting.GOLD));
        for (int dz = -2; dz <= 2; dz++) {
            MutableComponent row = Component.literal("");
            for (int dx = -2; dx <= 2; dx++) {
                ChunkPos chunk = new ChunkPos(center.x() + dx, center.z() + dz);
                int r = OilDeposits.richness(server, chunk, y);
                ChatFormatting color = r <= 0 ? ChatFormatting.DARK_GRAY : r < 40 ? ChatFormatting.YELLOW : r < 75 ? ChatFormatting.GREEN : ChatFormatting.AQUA;
                String cell = r <= 0 ? " -- " : String.format(" %3d", r);
                MutableComponent part = Component.literal(cell + (dx == 0 && dz == 0 ? "*" : " ")).withStyle(color);
                if (dx == 0 && dz == 0) part = part.withStyle(ChatFormatting.BOLD);
                row.append(part);
            }
            player.sendSystemMessage(row);
        }
        player.getCooldowns().addCooldown(player.getItemInHand(hand), 40);
        level.playSound(null, at, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.6F, 1.6F);
        return InteractionResult.SUCCESS_SERVER;
    }
}
