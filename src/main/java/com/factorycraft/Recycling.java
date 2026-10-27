package com.factorycraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.jspecify.annotations.Nullable;

/**
 * Reverse crafting for the recycler: for each item, the crafting recipe that makes
 * it (first found), as the list of ingredients one craft takes and how many items
 * that craft makes. Built from the server's recipes the first time it's needed.
 */
public final class Recycling {
    /** One craft: what goes in, how many come out. */
    public record Breakdown(List<ItemStack> parts, int makes) {}

    private static @Nullable RecipeManager builtFor;
    private static final Map<Item, Breakdown> TABLE = new HashMap<>();

    private Recycling() {}

    private static ItemStack first(Optional<Ingredient> ingredient) {
        if (ingredient.isEmpty()) return ItemStack.EMPTY;
        return ingredient.get().items().findFirst().map(Holder::value).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    private static synchronized void build(MinecraftServer server) {
        RecipeManager manager = server.getRecipeManager();
        if (manager == builtFor) return;
        builtFor = manager;
        TABLE.clear();
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            try {
                ItemStack result;
                List<ItemStack> parts = new ArrayList<>();
                if (holder.value() instanceof ShapedRecipe shaped) {
                    List<ItemStack> grid = new ArrayList<>();
                    for (Optional<Ingredient> ingredient : shaped.getIngredients()) {
                        ItemStack part = first(ingredient);
                        grid.add(part);
                        if (!part.isEmpty()) parts.add(part);
                    }
                    result = shaped.assemble(CraftingInput.of(shaped.getWidth(), shaped.getHeight(), grid));
                } else if (holder.value() instanceof ShapelessRecipe shapeless) {
                    result = shapeless.result().create();
                    for (Ingredient ingredient : shapeless.placementInfo().ingredients()) {
                        ItemStack part = first(Optional.of(ingredient));
                        if (!part.isEmpty()) parts.add(part);
                    }
                } else {
                    continue;
                }
                if (result.isEmpty() || parts.isEmpty() || TABLE.containsKey(result.getItem())) continue;
                Item made = result.getItem();
                if (parts.stream().anyMatch(p -> p.is(made))) continue;
                // Skip unpacking recipes (a storage block into 9 ingots): the reverse would multiply.
                if (parts.size() == 1 && result.getCount() > 1) continue;
                TABLE.put(made, new Breakdown(merge(parts), Math.max(1, result.getCount())));
            } catch (RuntimeException ignored) {
                // A recipe we can't read: just not recyclable.
            }
        }
    }

    private static List<ItemStack> merge(List<ItemStack> parts) {
        List<ItemStack> merged = new ArrayList<>();
        outer:
        for (ItemStack part : parts) {
            for (ItemStack m : merged) {
                if (ItemStack.isSameItemSameComponents(m, part)) {
                    m.grow(1);
                    continue outer;
                }
            }
            merged.add(part.copyWithCount(1));
        }
        return merged;
    }

    public static @Nullable Breakdown of(@Nullable MinecraftServer server, ItemStack stack) {
        if (server == null || stack.isEmpty()) return null;
        build(server);
        return TABLE.get(stack.getItem());
    }
}
