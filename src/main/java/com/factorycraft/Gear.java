package com.factorycraft;

import static com.factorycraft.FactoryCraft.ITEMS;
import static com.factorycraft.FactoryCraft.MOD_ID;

import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Worn gear: the powered exoskeleton, the work suit and the jetpack mark II.
 */
@EventBusSubscriber(modid = MOD_ID)
public final class Gear {
    private Gear() {}

    static void init() {}

    private static ResourceKey<EquipmentAsset> asset(String name) {
        return ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(MOD_ID, name));
    }

    private static TagKey<Item> repair(String name) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "repairs_" + name));
    }

    // ---------------------------------------------------------------- exoskeleton

    public static final ArmorMaterial EXOSKELETON_MATERIAL = new ArmorMaterial(35,
            Map.of(ArmorType.HELMET, 2, ArmorType.CHESTPLATE, 7, ArmorType.LEGGINGS, 5, ArmorType.BOOTS, 2, ArmorType.BODY, 6),
            15, SoundEvents.ARMOR_EQUIP_NETHERITE, 2.0F, 0.05F, repair("exoskeleton"), asset("exoskeleton"));

    public static final DeferredHolder<Item, Item> EXOSKELETON = ITEMS.registerItem("exoskeleton",
            properties -> new ExoskeletonItem(properties.humanoidArmor(EXOSKELETON_MATERIAL, ArmorType.CHESTPLATE)
                    .stacksTo(1).rarity(Rarity.RARE)));

    /** Powered frame: strength, haste, speed and jump while charged; also takes fall damage for you. */
    public static final class ExoskeletonItem extends Item implements ItemEnergy.Chargeable {
        public static final int CAPACITY = 200_000;
        static final int PER_SECOND = 60;

        public ExoskeletonItem(Properties properties) {
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
            return 0xC890D8;
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                    Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("tooltip.factorycraft.charge", ItemEnergy.percent(stack)).withStyle(ChatFormatting.GREEN));
            tooltip.accept(Component.translatable("tooltip.factorycraft.exoskeleton.help").withStyle(ChatFormatting.GRAY));
        }
    }

    // ---------------------------------------------------------------- work suit

    public static final ArmorMaterial WORK_SUIT_MATERIAL = new ArmorMaterial(28,
            Map.of(ArmorType.HELMET, 2, ArmorType.CHESTPLATE, 6, ArmorType.LEGGINGS, 5, ArmorType.BOOTS, 2, ArmorType.BODY, 5),
            12, SoundEvents.ARMOR_EQUIP_LEATHER, 1.0F, 0.0F, repair("work_suit"), asset("work_suit"));

    private static DeferredHolder<Item, Item> suit(String id, ArmorType type) {
        return ITEMS.registerItem(id, properties -> new Item(properties.humanoidArmor(WORK_SUIT_MATERIAL, type)));
    }

    public static final DeferredHolder<Item, Item> WORK_HELMET = suit("work_helmet", ArmorType.HELMET);
    public static final DeferredHolder<Item, Item> WORK_JACKET = suit("work_jacket", ArmorType.CHESTPLATE);
    public static final DeferredHolder<Item, Item> WORK_PANTS = suit("work_pants", ArmorType.LEGGINGS);
    public static final DeferredHolder<Item, Item> WORK_BOOTS = suit("work_boots", ArmorType.BOOTS);

    /** Work suit pieces worn (0-4). */
    public static int suitPieces(Player player) {
        int pieces = 0;
        if (player.getItemBySlot(EquipmentSlot.HEAD).is(WORK_HELMET.value())) pieces++;
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(WORK_JACKET.value())) pieces++;
        if (player.getItemBySlot(EquipmentSlot.LEGS).is(WORK_PANTS.value())) pieces++;
        if (player.getItemBySlot(EquipmentSlot.FEET).is(WORK_BOOTS.value())) pieces++;
        return pieces;
    }

    // ---------------------------------------------------------------- jetpack II

    public static final DeferredHolder<Item, Item> JETPACK_MK2 = ITEMS.registerItem("jetpack_mk2",
            properties -> new JetpackItem(properties.humanoidArmor(FactoryCraft.JETPACK_MATERIAL, ArmorType.CHESTPLATE)
                    .stacksTo(1).rarity(Rarity.RARE), 2));

    // ---------------------------------------------------------------- effects

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % 20 != 0) return;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof ExoskeletonItem && (player.isCreative() || ItemEnergy.use(chest, ExoskeletonItem.PER_SECOND))) {
            effect(player, MobEffects.STRENGTH, 0);
            effect(player, MobEffects.HASTE, 1);
            effect(player, MobEffects.SPEED, 0);
            effect(player, MobEffects.JUMP_BOOST, 0);
        }
        if (suitPieces(player) == 4) effect(player, MobEffects.FIRE_RESISTANCE, 0);
    }

    private static void effect(Player player, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int level) {
        player.addEffect(new MobEffectInstance(effect, 50, level, true, false, true));
    }

    /** The exoskeleton absorbs falls (energy permitting). */
    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !event.getSource().is(DamageTypeTags.IS_FALL)) return;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof ExoskeletonItem && ItemEnergy.use(chest, Math.round(event.getAmount() * 200))) {
            event.setCanceled(true);
        }
    }
}
