package com.factorycraft;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/** What the crusher turns things into. Ores and raw metals give double yield. */
public final class CrusherRecipes {
    /** {@code source} is the Item or TagKey shown in the guide. */
    record Entry(Predicate<ItemStack> input, Supplier<Item> output, int count, Object source) {}

    static final List<Entry> ENTRIES = new ArrayList<>();

    static {
        // Raw metals: 1 -> 2 dust. Ore blocks (silk touch): 1 -> 4 dust.
        add(Items.RAW_IRON, () -> FactoryCraft.IRON_DUST.value(), 2);
        add(Items.RAW_GOLD, () -> FactoryCraft.GOLD_DUST.value(), 2);
        add(Items.RAW_COPPER, () -> FactoryCraft.COPPER_DUST.value(), 2);
        add(Items.RAW_IRON_BLOCK, () -> FactoryCraft.IRON_DUST.value(), 18);
        add(Items.RAW_GOLD_BLOCK, () -> FactoryCraft.GOLD_DUST.value(), 18);
        add(Items.RAW_COPPER_BLOCK, () -> FactoryCraft.COPPER_DUST.value(), 18);
        add(ItemTags.IRON_ORES, () -> FactoryCraft.IRON_DUST.value(), 4);
        add(ItemTags.GOLD_ORES, () -> FactoryCraft.GOLD_DUST.value(), 4);
        add(ItemTags.COPPER_ORES, () -> FactoryCraft.COPPER_DUST.value(), 8);
        add(ItemTags.COAL_ORES, () -> Items.COAL, 3);
        add(ItemTags.DIAMOND_ORES, () -> Items.DIAMOND, 2);
        add(ItemTags.EMERALD_ORES, () -> Items.EMERALD, 2);
        add(ItemTags.REDSTONE_ORES, () -> Items.REDSTONE, 10);
        add(ItemTags.LAPIS_ORES, () -> Items.LAPIS_LAZULI, 12);
        add(Items.NETHER_QUARTZ_ORE, () -> Items.QUARTZ, 3);
        add(Items.NETHER_GOLD_ORE, () -> FactoryCraft.GOLD_DUST.value(), 2);
        add(Items.ANCIENT_DEBRIS, () -> Items.NETHERITE_SCRAP, 2);
        // Stone line: stone -> cobble -> gravel -> sand.
        add(Items.STONE, () -> Items.COBBLESTONE, 1);
        add(Items.COBBLESTONE, () -> Items.GRAVEL, 1);
        add(Items.COBBLED_DEEPSLATE, () -> Items.GRAVEL, 1);
        add(Items.GRAVEL, () -> Items.SAND, 1);
        add(Items.SANDSTONE, () -> Items.SAND, 4);
        add(Items.RED_SANDSTONE, () -> Items.RED_SAND, 4);
        // Handy extras.
        add(Items.GLOWSTONE, () -> Items.GLOWSTONE_DUST, 4);
        add(Items.BONE, () -> Items.BONE_MEAL, 6);
        add(Items.BLAZE_ROD, () -> Items.BLAZE_POWDER, 4);
        add(ItemTags.WOOL, () -> Items.STRING, 4);
        // Nuclear materials.
        add(Nuclear.RAW_URANIUM.value(), () -> Nuclear.URANIUM_DUST.value(), 2);
        add(Nuclear.RAW_THORIUM.value(), () -> Nuclear.THORIUM_DUST.value(), 2);
        add(Nuclear.RAW_LEAD.value(), () -> Nuclear.LEAD_DUST.value(), 2);
        add(Nuclear.URANIUM_ORE_ITEM.value(), () -> Nuclear.URANIUM_DUST.value(), 4);
        add(Nuclear.DEEPSLATE_URANIUM_ORE_ITEM.value(), () -> Nuclear.URANIUM_DUST.value(), 4);
        add(Nuclear.THORIUM_ORE_ITEM.value(), () -> Nuclear.THORIUM_DUST.value(), 4);
        add(Nuclear.DEEPSLATE_THORIUM_ORE_ITEM.value(), () -> Nuclear.THORIUM_DUST.value(), 4);
        add(Nuclear.LEAD_ORE_ITEM.value(), () -> Nuclear.LEAD_DUST.value(), 4);
        add(Nuclear.DEEPSLATE_LEAD_ORE_ITEM.value(), () -> Nuclear.LEAD_DUST.value(), 4);
        // Industrial metals.
        for (Materials.Metal m : Materials.METALS) {
            add(m.raw().value(), () -> m.dust().value(), 2);
            add(m.oreItem().value(), () -> m.dust().value(), 4);
            add(m.deepOreItem().value(), () -> m.dust().value(), 4);
        }
    }

    private CrusherRecipes() {}

    private static void add(Item input, Supplier<Item> output, int count) {
        ENTRIES.add(new Entry(stack -> stack.is(input), output, count, input));
    }

    private static void add(TagKey<Item> input, Supplier<Item> output, int count) {
        ENTRIES.add(new Entry(stack -> stack.is(input), output, count, input));
    }

    /** The product of crushing one {@code input}, or null if it can't be crushed. */
    public static @Nullable ItemStack resultFor(ItemStack input) {
        if (input.isEmpty()) return null;
        for (Entry entry : ENTRIES) {
            if (entry.input().test(input)) return new ItemStack(entry.output().get(), entry.count());
        }
        return fromCommonTags(input);
    }

    /**
     * Ores and raw metals from other mods: anything tagged {@code c:ores/<metal>} (x4) or
     * {@code c:raw_materials/<metal>} (x2) becomes that metal's dust, if some mod has a
     * {@code c:dusts/<metal>}. Our own dusts are preferred when there are several.
     */
    private static @Nullable ItemStack fromCommonTags(ItemStack input) {
        for (TagKey<Item> tag : (Iterable<TagKey<Item>>) input.getItem().builtInRegistryHolder().tags()::iterator) {
            if (!tag.location().getNamespace().equals("c")) continue;
            String path = tag.location().getPath();
            int count;
            String metal;
            if (path.startsWith("ores/")) {
                count = 4;
                metal = path.substring(5);
            } else if (path.startsWith("raw_materials/")) {
                count = 2;
                metal = path.substring(14);
            } else {
                continue;
            }
            Item dust = dustFor(metal);
            if (dust != null) return new ItemStack(dust, count);
        }
        return null;
    }

    private static @Nullable Item dustFor(String metal) {
        TagKey<Item> dusts = TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dusts/" + metal));
        Item found = null;
        for (net.minecraft.core.Holder<Item> holder : net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(dusts)) {
            Item item = holder.value();
            if (found == null || net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(FactoryCraft.MOD_ID)) {
                found = item;
            }
        }
        return found;
    }
}
