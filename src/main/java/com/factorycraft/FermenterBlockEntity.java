package com.factorycraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Fermenter: turns crops into biofuel, a clean fuel (generators burning it make
 * no smog). 8 crops (sugar cane, wheat, potatoes, carrots, beetroot, melon,
 * kelp, berries, apples) per canister; both slots ferment at once.
 */
public final class FermenterBlockEntity extends PoweredMachineBlockEntity {
    static final int PER_CANISTER = 8;
    static final Set<Item> CROPS = Set.of(Items.SUGAR_CANE, Items.WHEAT, Items.POTATO, Items.CARROT, Items.BEETROOT,
            Items.MELON_SLICE, Items.KELP, Items.SWEET_BERRIES, Items.APPLE, Items.PUMPKIN);

    public FermenterBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.FERMENTER_BLOCK_ENTITY.value(), pos, state, 2);
    }

    @Override protected int time() { return 160; }
    @Override protected int energyPerTick() { return 15; }
    @Override protected MenuType<ProcessingMenu> menuType() { return FactoryCraft.FERMENTER_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.fermenter"; }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return slot < 2 && CROPS.contains(stack.getItem());
    }

    @Override
    protected @Nullable Operation find(Level level) {
        int[] consume = new int[2];
        int made = 0;
        for (int i = 0; i < 2; i++) {
            if (CROPS.contains(items.get(i).getItem()) && items.get(i).getCount() >= PER_CANISTER) {
                consume[i] = PER_CANISTER;
                made++;
            }
        }
        return made == 0 ? null : new Operation(consume, List.of(new ItemStack(FactoryCraft.BIOFUEL.value(), made)));
    }

    @Override
    protected List<ItemStack> produce(Level level, Operation operation) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack stack : operation.maxOutputs()) out.add(stack.copy());
        return out;
    }
}
