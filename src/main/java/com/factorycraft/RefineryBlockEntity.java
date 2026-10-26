package com.factorycraft;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Refinery: one canister of crude oil makes a diesel canister, a plastic sheet and two asphalt blocks. */
public final class RefineryBlockEntity extends PoweredMachineBlockEntity {
    public RefineryBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.REFINERY_BLOCK_ENTITY.value(), pos, state, 1);
    }

    @Override protected int time() { return 120; }
    @Override protected int energyPerTick() { return 40; }
    @Override protected MenuType<ProcessingMenu> menuType() { return FactoryCraft.REFINERY_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.refinery"; }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return slot == 0 && stack.is(FactoryCraft.CRUDE_OIL.value());
    }

    static List<ItemStack> products() {
        return List.of(new ItemStack(FactoryCraft.DIESEL.value()), new ItemStack(Materials.PLASTIC.value()),
                new ItemStack(FactoryCraft.ASPHALT_ITEM.value(), 2));
    }

    @Override
    protected @Nullable Operation find(Level level) {
        if (!items.get(0).is(FactoryCraft.CRUDE_OIL.value())) return null;
        return new Operation(new int[] {1}, products());
    }

    @Override
    protected List<ItemStack> produce(Level level, Operation operation) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack stack : operation.maxOutputs()) out.add(stack.copy());
        return out;
    }
}
