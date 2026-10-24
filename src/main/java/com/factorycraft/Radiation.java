package com.factorycraft;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Radiation: carrying radioactive items without protection makes you sick.
 * Each lead armour piece blocks a quarter; a full set blocks it all.
 * Dose = sum of (count x item radiation):
 *   4+ hunger, 12+ nausea, 24+ weakness, 48+ poison, 96+ wither.
 */
@EventBusSubscriber(modid = FactoryCraft.MOD_ID)
public final class Radiation {
    private Radiation() {}

    /** An item that gives off radiation. */
    public interface Source {
        float radiation();
    }

    public static float of(ItemStack stack) {
        return stack.getItem() instanceof Source source ? source.radiation() * stack.getCount() : 0.0F;
    }

    public static int leadPieces(Player player) {
        int pieces = 0;
        if (player.getItemBySlot(EquipmentSlot.HEAD).is(Nuclear.LEAD_HELMET.value())) pieces++;
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(Nuclear.LEAD_CHESTPLATE.value())) pieces++;
        if (player.getItemBySlot(EquipmentSlot.LEGS).is(Nuclear.LEAD_LEGGINGS.value())) pieces++;
        if (player.getItemBySlot(EquipmentSlot.FEET).is(Nuclear.LEAD_BOOTS.value())) pieces++;
        return pieces;
    }

    /** Fraction of radiation that gets through the player's armour. */
    public static float exposure(Player player) {
        return 1.0F - 0.25F * leadPieces(player);
    }

    public static Component tooltip(float radiation) {
        String level = radiation >= 4.0F ? "high" : radiation >= 1.0F ? "medium" : "low";
        ChatFormatting color = radiation >= 4.0F ? ChatFormatting.RED : radiation >= 1.0F ? ChatFormatting.GOLD : ChatFormatting.YELLOW;
        return Component.translatable("tooltip.factorycraft.radioactive",
                Component.translatable("tooltip.factorycraft.radioactive." + level)).withStyle(color);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % 40 != 0) return;
        if (player.isCreative() || player.isSpectator()) return;
        Inventory inventory = player.getInventory();
        float total = 0.0F;
        for (int i = 0; i < inventory.getContainerSize(); i++) total += of(inventory.getItem(i));
        float dose = total * exposure(player);
        if (dose < 4.0F) return;
        apply(player, dose);
        if (player.getVehicle() == null) {
            player.sendOverlayMessage(Component.translatable("message.factorycraft.radiation", Math.round(dose))
                    .withStyle(dose >= 48.0F ? ChatFormatting.RED : ChatFormatting.GOLD));
        }
    }

    /** Radiation sickness for a given dose (also used by fallout clouds). */
    public static void apply(net.minecraft.world.entity.LivingEntity entity, float dose) {
        int time = 100;
        entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, time, dose >= 24.0F ? 1 : 0));
        if (dose >= 12.0F) entity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, time + 60, 0));
        if (dose >= 24.0F) entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, time, 0));
        if (dose >= 48.0F) entity.addEffect(new MobEffectInstance(MobEffects.POISON, time, 0));
        if (dose >= 96.0F) entity.addEffect(new MobEffectInstance(MobEffects.WITHER, time, 0));
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        IrradiatedBladeItem.onHit(event);
    }

    static boolean isRadioactive(Item item) {
        return item instanceof Source;
    }
}
