package com.factorycraft;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Client side of the Geiger counter: clicks faster the more radiation is around. */
@EventBusSubscriber(modid = FactoryCraft.MOD_ID, value = Dist.CLIENT)
public final class GeigerClient {
    private GeigerClient() {}

    private static float level;

    private static float blockValue(BlockState state) {
        Block b = state.getBlock();
        if (b == Nuclear.PLUTONIUM_BLOCK.value()) return 25;
        if (b == Nuclear.URANIUM_BLOCK.value()) return 9;
        if (b == Nuclear.THORIUM_BLOCK.value()) return 4;
        if (b == Nuclear.URANIUM_ORE.value() || b == Nuclear.DEEPSLATE_URANIUM_ORE.value()) return 2;
        if (b == Nuclear.THORIUM_ORE.value() || b == Nuclear.DEEPSLATE_THORIUM_ORE.value()) return 1;
        if (b == Nuclear.CONTAMINATED_SOIL.value()) return 1.5F;
        if (b == Nuclear.REACTOR.value() && state.getValue(MachineBlock.LIT)) return 6;
        return 0;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;
        boolean holding = player.getMainHandItem().getItem() instanceof GeigerCounterItem
                || player.getOffhandItem().getItem() instanceof GeigerCounterItem;
        if (!holding) return;
        if (player.tickCount % 10 == 0) {
            float total = 0;
            BlockPos center = player.blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(center.offset(-6, -6, -6), center.offset(6, 6, 6))) {
                float v = blockValue(mc.level.getBlockState(p));
                if (v > 0) total += v * 8.0F / (8.0F + (float) p.distSqr(center));
            }
            for (ItemEntity item : mc.level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(6))) {
                total += Radiation.of(item.getItem()) * 0.5F;
            }
            var inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) total += Radiation.of(inv.getItem(i)) * 0.25F;
            level = total;
            int cpm = Math.round(total * 30);
            ChatFormatting color = total > 20 ? ChatFormatting.RED : total > 5 ? ChatFormatting.GOLD : ChatFormatting.GREEN;
            player.sendOverlayMessage(Component.translatable("message.factorycraft.geiger", cpm).withStyle(color));
        }
        if (level > 0.2F && player.getRandom().nextFloat() < Math.min(0.9F, level / 25.0F)) {
            mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_HAT.value(),
                    SoundSource.PLAYERS, 0.35F, 1.8F + player.getRandom().nextFloat() * 0.4F, false);
        }
    }
}
