package com.factorycraft;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Tannery: cures rotten flesh (2 = 1 leather) and rabbit hide (4 = 1 leather)
 * into leather. Two input slots work in parallel.
 */
public final class TanneryBlockEntity extends PoweredMachineBlockEntity {
    private static final int INPUTS = 2;

    /** How many of an item make one leather, or 0 if it can't be tanned. */
    static int perLeather(Item item) {
        if (item == Items.ROTTEN_FLESH) return 2;
        if (item == Items.RABBIT_HIDE) return 4;
        return 0;
    }

    public TanneryBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.TANNERY_BLOCK_ENTITY.value(), pos, state, INPUTS);
    }

    @Override protected int time() { return 80; }
    @Override protected int energyPerTick() { return 12; }
    @Override protected MenuType<ProcessingMenu> menuType() { return FactoryCraft.TANNERY_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.tannery"; }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return slot < INPUTS && perLeather(stack.getItem()) > 0;
    }

    @Override
    protected @Nullable Operation find(Level level) {
        int[] consume = new int[INPUTS];
        int leather = 0;
        for (int i = 0; i < INPUTS; i++) {
            ItemStack stack = items.get(i);
            int need = perLeather(stack.getItem());
            if (need > 0 && stack.getCount() >= need) {
                consume[i] = need;
                leather++;
            }
        }
        return leather == 0 ? null : new Operation(consume, List.of(new ItemStack(Items.LEATHER, leather)));
    }

    @Override
    protected List<ItemStack> produce(Level level, Operation operation) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack stack : operation.maxOutputs()) out.add(stack.copy());
        return out;
    }
}
