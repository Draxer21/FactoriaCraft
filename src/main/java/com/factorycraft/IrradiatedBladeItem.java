package com.factorycraft;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Irradiated blade: a netherite-grade sword with a uranium core. While it has
 * charge, every hit deals +8 damage and withers the target (400 E per hit).
 * Charge it like the gamma rifle.
 */
public final class IrradiatedBladeItem extends Item implements ItemEnergy.Chargeable {
    public static final int CAPACITY = 40_000;
    private static final int HIT = 400;
    private static final float BONUS = 8.0F;

    public IrradiatedBladeItem(Properties properties) {
        super(properties);
    }

    @Override
    public int energyCapacity() {
        return CAPACITY;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return ItemEnergy.chargeFromBlock(stack, context);
    }

    static void onHit(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof LivingEntity attacker) || source.getDirectEntity() != attacker) return;
        ItemStack stack = attacker.getMainHandItem();
        if (!(stack.getItem() instanceof IrradiatedBladeItem) || !ItemEnergy.use(stack, HIT)) return;
        LivingEntity target = event.getEntity();
        event.setAmount(event.getAmount() + BONUS);
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
        if (target.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.GLOW, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    12, 0.3, 0.4, 0.3, 0.05);
        }
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
        return 0x6CFF4A;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.factorycraft.charge", ItemEnergy.percent(stack)).withStyle(ChatFormatting.GREEN));
        tooltip.accept(Component.translatable("tooltip.factorycraft.irradiated_blade.help").withStyle(ChatFormatting.GRAY));
    }
}
