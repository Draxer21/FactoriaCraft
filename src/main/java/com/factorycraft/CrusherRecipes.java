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
    private record Entry(Predicate<ItemStack> input, Supplier<Item> output, int count) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();

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
    }

    private CrusherRecipes() {}

    private static void add(Item input, Supplier<Item> output, int count) {
        ENTRIES.add(new Entry(stack -> stack.is(input), output, count));
    }

    private static void add(TagKey<Item> input, Supplier<Item> output, int count) {
        ENTRIES.add(new Entry(stack -> stack.is(input), output, count));
    }

    /** The product of crushing one {@code input}, or null if it can't be crushed. */
    public static @Nullable ItemStack resultFor(ItemStack input) {
        if (input.isEmpty()) return null;
        for (Entry entry : ENTRIES) {
            if (entry.input().test(input)) return new ItemStack(entry.output().get(), entry.count());
        }
        return null;
    }
}
