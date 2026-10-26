package com.factorycraft;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Energy stored in items (drills, the gamma rifle, the irradiated blade).
 * Charged by right-clicking any generator or reactor, or in the solar
 * generator's slot.
 */
public final class ItemEnergy {
    private ItemEnergy() {}

    /** An item with a battery. */
    public interface Chargeable {
        int energyCapacity();
    }

    public static boolean canCharge(ItemStack stack) {
        return stack.getItem() instanceof DrillItem || stack.getItem() instanceof Chargeable;
    }

    public static int get(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr("Energy", 0);
    }

    public static int capacity(ItemStack stack) {
        if (stack.getItem() instanceof DrillItem drill) return drill.maxEnergy();
        return stack.getItem() instanceof Chargeable c ? c.energyCapacity() : 0;
    }

    private static void set(ItemStack stack, int energy) {
        int value = Math.max(0, Math.min(capacity(stack), energy));
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("Energy", value));
    }

    /** Adds up to {@code max}; returns how much was taken. */
    public static int charge(ItemStack stack, int max) {
        if (stack.getItem() instanceof DrillItem) return DrillItem.charge(stack, max);
        if (!(stack.getItem() instanceof Chargeable)) return 0;
        int taken = Math.max(0, Math.min(capacity(stack) - get(stack), max));
        if (taken > 0) set(stack, get(stack) + taken);
        return taken;
    }

    public static boolean use(ItemStack stack, int amount) {
        int energy = get(stack);
        if (energy < amount) return false;
        set(stack, energy - amount);
        return true;
    }

    public static int percent(ItemStack stack) {
        int cap = capacity(stack);
        return cap <= 0 ? 0 : Math.round(100.0F * get(stack) / cap);
    }

    /** Right-click on a generator/reactor with a chargeable item. */
    public static InteractionResult chargeFromBlock(ItemStack stack, UseOnContext context) {
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof Energy.Source source)) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        int taken = source.extract(capacity(stack) - get(stack));
        set(stack, get(stack) + taken);
        Player player = context.getPlayer();
        if (player != null) {
            player.sendOverlayMessage(Component.translatable("message.factorycraft.item_charge", percent(stack)));
        }
        context.getLevel().playSound(null, context.getClickedPos(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.5F, 1.8F);
        return InteractionResult.SUCCESS_SERVER;
    }
}
