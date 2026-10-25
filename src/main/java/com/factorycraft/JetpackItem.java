package com.factorycraft;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Jetpack: worn in the chest slot. Hold jump in the air to fly up; it uses
 * 20 E per tick of thrust and cancels fall damage. Charge it like the drill
 * (right-click a generator, or a solar generator / battery slot).
 */
@EventBusSubscriber(modid = FactoryCraft.MOD_ID)
public final class JetpackItem extends Item implements ItemEnergy.Chargeable {
    public static final int CAPACITY = 80_000;
    public static final int PER_TICK = 20;
    private static final Set<UUID> THRUSTING = ConcurrentHashMap.newKeySet();

    public JetpackItem(Properties properties) {
        super(properties);
    }

    @Override
    public int energyCapacity() {
        return CAPACITY;
    }

    public static ItemStack worn(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return chest.getItem() instanceof JetpackItem ? chest : ItemStack.EMPTY;
    }

    static void setThrusting(Player player, boolean thrusting) {
        if (thrusting) THRUSTING.add(player.getUUID());
        else THRUSTING.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !THRUSTING.contains(player.getUUID())) return;
        ItemStack jetpack = worn(player);
        if (jetpack.isEmpty() || (!player.isCreative() && !ItemEnergy.use(jetpack, PER_TICK))) {
            THRUSTING.remove(player.getUUID());
            return;
        }
        player.resetFallDistance();
        if (player.level() instanceof ServerLevel level && player.tickCount % 2 == 0) {
            level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.6, player.getZ(), 2, 0.12, 0.05, 0.12, 0.01);
            level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 0.4, player.getZ(), 1, 0.1, 0.05, 0.1, 0.01);
        }
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return ItemEnergy.chargeFromBlock(stack, context);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * ItemEnergy.get(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFFB43C;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.factorycraft.charge", ItemEnergy.percent(stack)).withStyle(ChatFormatting.GREEN));
        tooltip.accept(Component.translatable("tooltip.factorycraft.jetpack.help").withStyle(ChatFormatting.GRAY));
    }
}
