package com.factorycraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import org.jspecify.annotations.Nullable;

/**
 * Forged tools: one tool of each kind (drill, hammer, chainsaw, excavating shovel,
 * multitool) made from any metal or gem, ours or another mod's. Its durability,
 * speed, mining level, damage and colour come from the material:
 * - known materials (vanilla, FactoryCraft) have hand-tuned stats;
 * - another mod's material borrows the stats of that mod's own pickaxe for it;
 * - anything else tagged as an ingot or gem gets iron-like stats.
 * Some materials add a trait: igneous crystal smelts what it mines; voidnite
 * hardly ever wears out; netherite, cobalt, igneous and voidnite survive fire.
 */
@EventBusSubscriber(modid = FactoryCraft.MOD_ID)
public final class ToolForge {
    private ToolForge() {}

    public record Stats(int durability, float speed, float attack, int enchantability, int tier, int color,
                        boolean fireproof, String trait) {}

    public enum Kind {
        DRILL(3, true, 1.0F, -2.4F), HAMMER(5, false, 2.0F, -3.2F), CHAINSAW(3, false, 5.0F, -2.8F),
        SHOVEL(3, false, 2.0F, -3.2F), MULTITOOL(4, true, 3.0F, -2.6F);

        public final int materials;
        public final boolean electric;
        final float damage;
        final float attackSpeed;

        Kind(int materials, boolean electric, float damage, float attackSpeed) {
            this.materials = materials;
            this.electric = electric;
            this.damage = damage;
            this.attackSpeed = attackSpeed;
        }

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private static final Map<String, Stats> KNOWN = new HashMap<>();

    private static void known(String id, int durability, float speed, float attack, int enchant, int tier, int color, boolean fireproof, String trait) {
        KNOWN.put(id, new Stats(durability, speed, attack, enchant, tier, color, fireproof, trait));
    }

    static {
        known("minecraft:copper_ingot", 190, 5.0F, 1.0F, 13, 1, 0xE88454, false, "");
        known("minecraft:iron_ingot", 250, 6.0F, 2.0F, 14, 2, 0xE2E2E2, false, "");
        known("minecraft:gold_ingot", 32, 12.0F, 0.0F, 22, 0, 0xFFD84A, false, "");
        known("minecraft:diamond", 1561, 8.0F, 3.0F, 10, 3, 0x6EF0E8, false, "");
        known("minecraft:emerald", 1100, 8.5F, 2.5F, 18, 3, 0x46DA78, false, "");
        known("minecraft:amethyst_shard", 300, 9.0F, 1.0F, 22, 2, 0xB98AF0, false, "");
        known("minecraft:netherite_ingot", 2031, 9.0F, 4.0F, 15, 4, 0x5A5258, true, "");
        known("factorycraft:tin_ingot", 150, 5.0F, 1.0F, 14, 1, 0xD8DEE6, false, "");
        known("factorycraft:nickel_ingot", 300, 6.0F, 2.0F, 14, 2, 0xD2C890, false, "");
        known("factorycraft:lithium_ingot", 120, 7.0F, 1.0F, 18, 1, 0xE6DCF2, false, "");
        known("factorycraft:bronze_ingot", 420, 6.5F, 2.0F, 15, 2, 0xCF8C3C, false, "");
        known("factorycraft:steel_ingot", 900, 6.5F, 2.5F, 12, 2, 0x9AA2AC, false, "");
        known("factorycraft:stainless_steel_ingot", 1250, 7.0F, 3.0F, 12, 3, 0xE2E8EE, false, "");
        known("factorycraft:tungsten_ingot", 1800, 7.0F, 3.5F, 8, 3, 0x6A727C, false, "");
        known("factorycraft:lead_ingot", 180, 4.0F, 2.5F, 9, 1, 0x8E98C4, false, "");
        known("factorycraft:cobalt_ingot", 1700, 11.0F, 3.0F, 16, 3, 0x3F6FE0, true, "");
        known("factorycraft:purpurite_ingot", 900, 10.0F, 2.0F, 20, 3, 0xC890D8, false, "");
        known("factorycraft:igneous_crystal", 1300, 8.0F, 3.0F, 12, 3, 0xFF7A2A, true, "smelt");
        known("factorycraft:voidnite_ingot", 60000, 10.0F, 5.0F, 20, 4, 0x8A60D8, true, "");
    }

    private static final Map<Item, Optional<Stats>> CACHE = new HashMap<>();

    /** Stats a tool made of {@code material} would have, or null if it isn't a usable material. */
    public static @Nullable Stats statsFor(ItemStack material) {
        if (material.isEmpty()) return null;
        return CACHE.computeIfAbsent(material.getItem(), ToolForge::compute).orElse(null);
    }

    private static Optional<Stats> compute(Item item) {
        String id = BuiltInRegistries.ITEM.getKey(item).toString();
        Stats known = KNOWN.get(id);
        if (known != null) return Optional.of(known);
        Stats borrowed = fromModPickaxe(item);
        if (borrowed != null) return Optional.of(borrowed);
        ItemStack stack = new ItemStack(item);
        if (stack.is(tag("ingots")) || stack.is(tag("gems"))) {
            return Optional.of(new Stats(250, 6.0F, 2.0F, 14, 2, 0xFFFFFF, false, ""));
        }
        return Optional.empty();
    }

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
    }

    /** Another mod's material: copy its own pickaxe (the one repaired with it). */
    private static @Nullable Stats fromModPickaxe(Item material) {
        Holder<Item> holder = material.builtInRegistryHolder();
        for (Item candidate : BuiltInRegistries.ITEM) {
            var components = candidate.components();
            Repairable repairable = components.get(DataComponents.REPAIRABLE);
            Tool tool = components.get(DataComponents.TOOL);
            if (repairable == null || tool == null || !repairable.items().contains(holder)) continue;
            float speed = 0.0F;
            int tier = 3;
            boolean pickaxe = false;
            for (Tool.Rule rule : tool.rules()) {
                var tagKey = rule.blocks().unwrapKey();
                if (tagKey.isEmpty()) continue;
                if (tagKey.get().equals(BlockTags.MINEABLE_WITH_PICKAXE) && rule.speed().isPresent()) {
                    pickaxe = true;
                    speed = rule.speed().get();
                }
                if (rule.correctForDrops().isPresent() && !rule.correctForDrops().get()) tier = tierOf(tagKey.get());
            }
            if (!pickaxe) continue;
            Integer durability = components.get(DataComponents.MAX_DAMAGE);
            return new Stats(durability == null ? 500 : durability, speed, 2.5F, 14, tier, 0xFFFFFF, false, "");
        }
        return null;
    }

    private static int tierOf(TagKey<Block> incorrect) {
        if (incorrect.equals(BlockTags.INCORRECT_FOR_WOODEN_TOOL) || incorrect.equals(BlockTags.INCORRECT_FOR_GOLD_TOOL)) return 0;
        if (incorrect.equals(BlockTags.INCORRECT_FOR_STONE_TOOL)) return 1;
        if (incorrect.equals(BlockTags.INCORRECT_FOR_IRON_TOOL)) return 2;
        if (incorrect.equals(BlockTags.INCORRECT_FOR_DIAMOND_TOOL)) return 3;
        if (incorrect.equals(BlockTags.INCORRECT_FOR_NETHERITE_TOOL)) return 4;
        return 3;
    }

    private static TagKey<Block> incorrectFor(int tier) {
        return switch (tier) {
            case 0 -> BlockTags.INCORRECT_FOR_WOODEN_TOOL;
            case 1 -> BlockTags.INCORRECT_FOR_STONE_TOOL;
            case 2 -> BlockTags.INCORRECT_FOR_IRON_TOOL;
            case 3 -> BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
            default -> BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
        };
    }

    // ---------------------------------------------------------------- forging

    public static Item itemFor(Kind kind) {
        return switch (kind) {
            case DRILL -> FactoryCraft.FORGED_DRILL.value();
            case HAMMER -> FactoryCraft.FORGED_HAMMER.value();
            case CHAINSAW -> FactoryCraft.FORGED_CHAINSAW.value();
            case SHOVEL -> FactoryCraft.FORGED_SHOVEL.value();
            case MULTITOOL -> FactoryCraft.MULTITOOL.value();
        };
    }

    /** Builds the finished tool of {@code kind} out of {@code material}. */
    public static ItemStack forge(Kind kind, ItemStack material, Stats stats, net.minecraft.core.HolderLookup.Provider registries) {
        ItemStack tool = new ItemStack(itemFor(kind));
        var blocks = BuiltInRegistries.BLOCK;
        List<Tool.Rule> rules = new ArrayList<>();
        rules.add(Tool.Rule.deniesDrops(blocks.getOrThrow(incorrectFor(stats.tier()))));
        List<TagKey<Block>> mines = switch (kind) {
            case DRILL, HAMMER -> List.of(BlockTags.MINEABLE_WITH_PICKAXE);
            case CHAINSAW -> List.of(BlockTags.MINEABLE_WITH_AXE);
            case SHOVEL -> List.of(BlockTags.MINEABLE_WITH_SHOVEL);
            case MULTITOOL -> List.of(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.MINEABLE_WITH_AXE,
                    BlockTags.MINEABLE_WITH_SHOVEL, BlockTags.MINEABLE_WITH_HOE);
        };
        for (TagKey<Block> tag : mines) rules.add(Tool.Rule.minesAndDrops(blocks.getOrThrow(tag), stats.speed()));
        tool.set(DataComponents.TOOL, new Tool(rules, 1.0F, 1, true));
        tool.set(DataComponents.MAX_DAMAGE, stats.durability());
        tool.set(DataComponents.DAMAGE, 0);
        tool.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Identifier.withDefaultNamespace("base_attack_damage"),
                        kind.damage + stats.attack(), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(Identifier.withDefaultNamespace("base_attack_speed"),
                        kind.attackSpeed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build());
        tool.set(DataComponents.ENCHANTABLE, new Enchantable(stats.enchantability()));
        tool.set(DataComponents.REPAIRABLE, new Repairable(HolderSet.direct(material.getItem().builtInRegistryHolder())));
        if (stats.fireproof()) tool.set(DataComponents.DAMAGE_RESISTANT, new DamageResistant(registries.lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypeTags.IS_FIRE)));
        tool.set(DataComponents.ITEM_NAME, Component.translatable("item.factorycraft.forged." + kind.id(), material.getHoverName()));
        tool.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(), List.of(stats.color())));
        String materialId = BuiltInRegistries.ITEM.getKey(material.getItem()).toString();
        CustomData.update(DataComponents.CUSTOM_DATA, tool, tag -> {
            tag.putString("Material", materialId);
            if (!stats.trait().isEmpty()) tag.putString("Trait", stats.trait());
        });
        return tool;
    }

    public static String trait(ItemStack tool) {
        CustomData data = tool.get(DataComponents.CUSTOM_DATA);
        return data == null ? "" : data.copyTag().getStringOr("Trait", "");
    }

    // ---------------------------------------------------------------- traits

    /** Igneous tools smelt what they mine. */
    @SubscribeEvent
    public static void onDrops(BlockDropsEvent event) {
        if (!"smelt".equals(trait(event.getTool())) || !(event.getLevel() instanceof ServerLevel level)) return;
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            var recipe = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level);
            if (recipe.isEmpty()) continue;
            ItemStack result = recipe.get().value().assemble(new SingleRecipeInput(stack));
            if (result.isEmpty()) continue;
            drop.setItem(result.copyWithCount(Math.min(result.getMaxStackSize(), result.getCount() * stack.getCount())));
        }
    }
}
