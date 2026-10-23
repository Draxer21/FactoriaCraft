package com.factorycraft;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Hydraulic press: ingots into plates, plates into gears, plus a few handy squeezes. */
public final class PressBlockEntity extends PoweredMachineBlockEntity {
    private record Pressing(Supplier<Item> input, int count, Supplier<Item> output, int outCount) {}

    private static final List<Pressing> RECIPES = List.of(
            new Pressing(() -> Items.IRON_INGOT, 1, () -> FactoryCraft.IRON_PLATE.value(), 1),
            new Pressing(() -> Items.COPPER_INGOT, 1, () -> FactoryCraft.COPPER_PLATE.value(), 1),
            new Pressing(() -> Items.GOLD_INGOT, 1, () -> FactoryCraft.GOLD_PLATE.value(), 1),
            new Pressing(() -> FactoryCraft.IRON_PLATE.value(), 2, () -> FactoryCraft.IRON_GEAR.value(), 1),
            new Pressing(() -> Items.SUGAR_CANE, 3, () -> Items.PAPER, 4),
            new Pressing(() -> Items.SNOWBALL, 4, () -> Items.ICE, 1),
            new Pressing(() -> Items.ICE, 9, () -> Items.PACKED_ICE, 1),
            new Pressing(() -> Items.PACKED_ICE, 9, () -> Items.BLUE_ICE, 1),
            new Pressing(() -> Items.COAL, 64, () -> Items.DIAMOND, 1));

    public PressBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.PRESS_BLOCK_ENTITY.value(), pos, state, 1);
    }

    @Override protected int time() { return 40; }
    @Override protected int energyPerTick() { return 30; }
    @Override protected MenuType<ProcessingMenu> menuType() { return FactoryCraft.PRESS_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.press"; }

    private static @Nullable Pressing recipeFor(ItemStack stack) {
        for (Pressing pressing : RECIPES) {
            if (stack.is(pressing.input().get())) return pressing;
        }
        return null;
    }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return slot == 0 && recipeFor(stack) != null;
    }

    @Override
    protected @Nullable Operation find(Level level) {
        ItemStack input = items.get(0);
        Pressing pressing = recipeFor(input);
        if (pressing == null || input.getCount() < pressing.count()) return null;
        return new Operation(new int[] {pressing.count()}, List.of(new ItemStack(pressing.output().get(), pressing.outCount())));
    }

    @Override
    protected List<ItemStack> produce(Level level, Operation operation) {
        return operation.maxOutputs().stream().map(ItemStack::copy).toList();
    }
}
