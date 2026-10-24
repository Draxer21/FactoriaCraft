package com.factorycraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Smelts {@link #BATCH} items at a time using normal furnace recipes, 40 ticks
 * per batch (a furnace does 1 item in 200), with the same fuel cost per item.
 * Slot 0 = input, 1 = fuel, 2..5 = output.
 */
public final class IndustrialFurnaceBlockEntity extends MachineBlockEntity implements MenuProvider {
    public static final int BATCH = 8;
    /** Ticks per batch. */
    private static final int BATCH_TIME = 40;
    /** Fuel ticks burned per working tick: keeps 1 coal = 8 items, like a furnace. */
    private static final int FUEL_PER_TICK = 40;

    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> recipes =
            RecipeManager.createCheck(RecipeType.SMELTING);
    private int progress;
    private int fuelLeft;
    private int fuelMax;

    /** Synced to the screen: progress, progress max, fuel left, fuel of the current item. */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int id) {
            return switch (id) {
                case 0 -> progress;
                case 1 -> BATCH_TIME;
                case 2 -> Math.max(fuelLeft, 0);
                case 3 -> fuelMax;
                default -> 0;
            };
        }

        @Override
        public void set(int id, int value) {
            switch (id) {
                case 0 -> progress = value;
                case 2 -> fuelLeft = value;
                case 3 -> fuelMax = value;
                default -> { }
            }
        }

        @Override
        public int getCount() {
            return ProcessingMenu.DATA_COUNT;
        }
    };

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.factorycraft.industrial_furnace");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ProcessingMenu(FactoryCraft.INDUSTRIAL_FURNACE_MENU.value(), id, inventory, this, data, true);
    }

    public IndustrialFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.INDUSTRIAL_FURNACE_BLOCK_ENTITY.value(), pos, state, 6);
    }

    @Override
    protected boolean isInputSlot(int slot) {
        return slot == 0;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return slot >= 2;
    }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        if (level == null) return false;
        boolean fuel = level.fuelValues().isFuel(stack);
        if (slot == 1) return fuel;
        // Fuel goes to the fuel slot, everything smeltable to the input.
        return slot == 0 && !fuel && smeltResult(stack).isPresent();
    }

    private Optional<ItemStack> smeltResult(ItemStack stack) {
        if (!(level instanceof ServerLevel serverLevel) || stack.isEmpty()) return Optional.empty();
        SingleRecipeInput input = new SingleRecipeInput(stack.copyWithCount(1));
        return recipes.getRecipeFor(input, serverLevel).map(RecipeHolder::value).map(r -> r.assemble(input));
    }

    @Override
    protected boolean work(Level level) {
        ItemStack input = items.get(0);
        Optional<ItemStack> single = smeltResult(input);
        if (single.isEmpty()) {
            progress = 0;
            return false;
        }
        int count = Math.min(BATCH, input.getCount());
        List<ItemStack> products = new ArrayList<>();
        products.add(single.get().copyWithCount(single.get().getCount() * count));
        if (!canStore(products)) {
            progress = 0;
            return false;
        }
        if (fuelLeft <= 0 && !refuel(level)) {
            return false;
        }
        fuelLeft -= FUEL_PER_TICK;
        if (++progress >= BATCH_TIME) {
            progress = 0;
            input.shrink(count);
            store(products);
        }
        return true;
    }

    private boolean refuel(Level level) {
        ItemStack fuel = items.get(1);
        if (fuel.isEmpty()) return false;
        int burn = level.fuelValues().burnDuration(fuel);
        if (burn <= 0) return false;
        // Query the remainder (e.g. empty bucket from lava) before shrinking, like the vanilla furnace.
        ItemStackTemplate remainder = fuel.getCraftingRemainder();
        fuel.shrink(1);
        if (fuel.isEmpty()) items.set(1, remainder != null ? remainder.create() : ItemStack.EMPTY);
        fuelLeft += burn;
        fuelMax = burn;
        setChanged();
        return true;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        progress = input.getIntOr("Progress", 0);
        fuelLeft = input.getIntOr("Fuel", 0);
        fuelMax = input.getIntOr("FuelMax", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Progress", progress);
        output.putInt("Fuel", fuelLeft);
        output.putInt("FuelMax", fuelMax);
    }
}
