package com.factorycraft;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Electric furnace: any furnace recipe, {@link #BATCH} items every
 * {@link #TIME} ticks on power alone (no fuel): four times the industrial
 * furnace. Keeps the smelting experience for the player who takes the products.
 */
public final class ElectricFurnaceBlockEntity extends PoweredMachineBlockEntity implements StoredExperience {
    public static final int BATCH = 16;
    public static final int TIME = 20;
    public static final int ENERGY = 40;

    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> recipes =
            RecipeManager.createCheck(RecipeType.SMELTING);
    private float experience;
    /** Experience of the operation in progress (set by find). */
    private float pending;

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(Processing.ELECTRIC_FURNACE_BLOCK_ENTITY.value(), pos, state, 1);
    }

    @Override protected int time() { return TIME; }
    @Override protected int energyPerTick() { return ENERGY; }
    @Override protected MenuType<ProcessingMenu> menuType() { return Processing.ELECTRIC_FURNACE_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.electric_furnace"; }

    private Optional<SmeltingRecipe> recipe(ItemStack stack) {
        if (!(level instanceof ServerLevel server) || stack.isEmpty()) return Optional.empty();
        return recipes.getRecipeFor(new SingleRecipeInput(stack.copyWithCount(1)), server).map(RecipeHolder::value);
    }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return slot == 0 && recipe(stack).isPresent();
    }

    @Override
    protected @Nullable Operation find(Level level) {
        ItemStack input = items.get(0);
        Optional<SmeltingRecipe> recipe = recipe(input);
        if (recipe.isEmpty()) return null;
        ItemStack single = recipe.get().assemble(new SingleRecipeInput(input.copyWithCount(1)));
        if (single.isEmpty()) return null;
        int count = Math.min(BATCH, input.getCount());
        pending = recipe.get().experience() * count;
        return new Operation(new int[] {count}, List.of(single.copyWithCount(single.getCount() * count)));
    }

    @Override
    protected List<ItemStack> produce(Level level, Operation operation) {
        return operation.maxOutputs().stream().map(ItemStack::copy).toList();
    }

    @Override
    protected void finished(Level level, Operation operation) {
        addExperience(pending);
    }

    @Override
    public float storedExperience() {
        return experience;
    }

    @Override
    public void setStoredExperience(float value) {
        experience = value;
        setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        experience = input.getFloatOr("Experience", 0.0F);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putFloat("Experience", experience);
    }
}
