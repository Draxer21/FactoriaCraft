package com.factorycraft;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Magnet backpack: right-click to switch it on or off. While on and anywhere
 * in your inventory, loose items within 8 blocks fly to you.
 */
@EventBusSubscriber(modid = FactoryCraft.MOD_ID)
public final class MagnetItem extends Item {
    private static final double RANGE = 8.0;

    public MagnetItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static boolean isOn(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean on = !isOn(stack);
        if (on) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        else stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.6F, on ? 1.4F : 0.8F);
        if (!level.isClientSide()) {
            player.sendOverlayMessage(Component.translatable(on ? "message.factorycraft.magnet.on" : "message.factorycraft.magnet.off"));
        }
        return InteractionResult.SUCCESS;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.isSpectator() || player.tickCount % 2 != 0) return;
        Inventory inventory = player.getInventory();
        boolean active = false;
        for (int i = 0; i < inventory.getContainerSize() && !active; i++) {
            ItemStack stack = inventory.getItem(i);
            active = stack.getItem() instanceof MagnetItem && isOn(stack);
        }
        if (!active) return;
        Vec3 target = player.position().add(0, 0.5, 0);
        for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(RANGE))) {
            if (!item.isAlive() || item.getItem().isEmpty()) continue;
            Vec3 pull = target.subtract(item.position());
            double dist = pull.length();
            if (dist < 0.5) continue;
            item.setDeltaMovement(pull.normalize().scale(Math.min(0.6, 0.15 + dist * 0.05)));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(isOn(stack) ? "tooltip.factorycraft.magnet.on" : "tooltip.factorycraft.magnet.off")
                .withStyle(isOn(stack) ? ChatFormatting.GREEN : ChatFormatting.GRAY));
    }
}
