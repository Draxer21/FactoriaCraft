package com.factorycraft;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Packager: compresses items into their storage blocks using the normal
 * crafting recipes (9 ingots into a block, 9 nuggets into an ingot, 4 quartz
 * into a quartz block...). Saves a lot of space in wagons and chests.
 */
public final class PackagerBlockEntity extends PoweredMachineBlockEntity {
    private record Packing(int count, ItemStack result) {}

    /** 2x2 packing only for these, so planks don't turn into crafting tables. */
    private static final java.util.Set<Item> TWO_BY_TWO = java.util.Set.of(
            net.minecraft.world.item.Items.QUARTZ, net.minecraft.world.item.Items.CLAY_BALL,
            net.minecraft.world.item.Items.SNOWBALL, net.minecraft.world.item.Items.GLOWSTONE_DUST,
            net.minecraft.world.item.Items.STRING, net.minecraft.world.item.Items.HONEYCOMB,
            net.minecraft.world.item.Items.AMETHYST_SHARD);

    private final RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> recipes = RecipeManager.createCheck(RecipeType.CRAFTING);
    private final Map<Item, Optional<Packing>> cache = new HashMap<>();
    private long cacheTime;

    public PackagerBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.PACKAGER_BLOCK_ENTITY.value(), pos, state, 1);
    }

    @Override protected int time() { return 20; }
    @Override protected int energyPerTick() { return 10; }
    @Override protected MenuType<ProcessingMenu> menuType() { return FactoryCraft.PACKAGER_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.packager"; }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return slot == 0 && level instanceof ServerLevel server ? packingFor(server, stack).isPresent() : slot == 0;
    }

    private Optional<Packing> packingFor(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        if (level.getGameTime() - cacheTime > 6000) { // recipes may have been reloaded
            cache.clear();
            cacheTime = level.getGameTime();
        }
        return cache.computeIfAbsent(stack.getItem(), item -> {
            for (int size : new int[] {3, 2}) {
                if (size == 2 && !TWO_BY_TWO.contains(item)) continue;
                ItemStack one = new ItemStack(item);
                CraftingInput input = CraftingInput.of(size, size, Collections.nCopies(size * size, one));
                Optional<ItemStack> result = recipes.getRecipeFor(input, level)
                        .map(RecipeHolder::value).map(r -> r.assemble(input));
                if (result.isPresent() && !result.get().isEmpty() && !result.get().is(item)) {
                    return Optional.of(new Packing(size * size, result.get()));
                }
            }
            return Optional.empty();
        });
    }

    @Override
    protected @Nullable Operation find(Level level) {
        if (!(level instanceof ServerLevel server)) return null;
        ItemStack input = items.get(0);
        Optional<Packing> packing = packingFor(server, input);
        if (packing.isEmpty() || input.getCount() < packing.get().count()) return null;
        return new Operation(new int[] {packing.get().count()}, List.of(packing.get().result().copy()));
    }

    @Override
    protected List<ItemStack> produce(Level level, Operation operation) {
        return operation.maxOutputs().stream().map(ItemStack::copy).toList();
    }
}
