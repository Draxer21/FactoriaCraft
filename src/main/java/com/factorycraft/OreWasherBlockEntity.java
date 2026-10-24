package com.factorycraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Ore washer: rinses gravel, sand and dirt for whatever is hidden in them.
 * Each item has a chance of giving nuggets, dusts, flint, clay... Power-hungry
 * but turns the tunneler's rubble into something useful.
 */
public final class OreWasherBlockEntity extends PoweredMachineBlockEntity {
    record Find(Supplier<Item> item, float chance) {}

    static final Map<Item, List<Find>> TABLE = Map.of(
            Items.GRAVEL, List.of(new Find(() -> Items.FLINT, 0.30F), new Find(() -> Items.IRON_NUGGET, 0.25F),
                    new Find(() -> FactoryCraft.IRON_DUST.value(), 0.06F), new Find(() -> FactoryCraft.COPPER_DUST.value(), 0.10F)),
            Items.SAND, List.of(new Find(() -> Items.CLAY_BALL, 0.25F), new Find(() -> Items.GOLD_NUGGET, 0.12F),
                    new Find(() -> FactoryCraft.GOLD_DUST.value(), 0.03F)),
            Items.RED_SAND, List.of(new Find(() -> Items.GOLD_NUGGET, 0.25F), new Find(() -> Items.REDSTONE, 0.10F)),
            Items.DIRT, List.of(new Find(() -> Items.CLAY_BALL, 0.15F), new Find(() -> Items.BONE_MEAL, 0.10F),
                    new Find(() -> Items.WHEAT_SEEDS, 0.10F)),
            Items.MUD, List.of(new Find(() -> Items.CLAY_BALL, 0.40F)),
            Items.SOUL_SAND, List.of(new Find(() -> Items.QUARTZ, 0.20F), new Find(() -> Items.GOLD_NUGGET, 0.15F)),
            Items.COBBLED_DEEPSLATE, List.of(new Find(() -> Items.IRON_NUGGET, 0.15F), new Find(() -> Items.LAPIS_LAZULI, 0.05F)),
            Nuclear.DEPLETED_FUEL_ROD.value(), List.of(new Find(() -> Nuclear.PLUTONIUM_NUGGET.value(), 0.60F),
                    new Find(() -> Nuclear.LEAD_DUST.value(), 0.40F), new Find(() -> Nuclear.URANIUM_DUST.value(), 0.20F)));

    public OreWasherBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.ORE_WASHER_BLOCK_ENTITY.value(), pos, state, ProcessingMenu.BIG_INPUTS, ProcessingMenu.BIG_OUTPUTS);
    }

    private static final int INPUTS = ProcessingMenu.BIG_INPUTS;

    @Override protected int time() { return 20; }
    @Override protected int energyPerTick() { return 20; }
    @Override protected MenuType<ProcessingMenu> menuType() { return FactoryCraft.ORE_WASHER_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.ore_washer"; }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return slot < INPUTS && TABLE.containsKey(stack.getItem());
    }

    /** Washes one item from every input slot at once. */
    @Override
    protected @Nullable Operation find(Level level) {
        int[] consume = new int[INPUTS];
        List<ItemStack> max = new ArrayList<>();
        for (int i = 0; i < INPUTS; i++) {
            List<Find> finds = TABLE.get(items.get(i).getItem());
            if (finds == null || items.get(i).isEmpty()) continue;
            consume[i] = 1;
            for (Find find : finds) max.add(new ItemStack(find.item().get()));
        }
        return max.isEmpty() ? null : new Operation(consume, max);
    }

    @Override
    protected List<ItemStack> produce(Level level, Operation operation) {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < INPUTS; i++) {
            if (operation.consume()[i] == 0) continue;
            for (Find find : TABLE.get(items.get(i).getItem())) {
                if (level.getRandom().nextFloat() < find.chance()) out.add(new ItemStack(find.item().get()));
            }
        }
        return out;
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        migrateToBigLayout(input, INPUTS);
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Layout", 2);
    }
}
