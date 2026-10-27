package com.factorycraft;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Recycler: takes crafted things apart again. Each ingredient of the recipe comes
 * back with a 60% chance (less for worn tools and armour). Works with any crafting
 * recipe, other mods' too. Every batch recycled also clears the air a little.
 */
public final class RecyclerBlockEntity extends PoweredMachineBlockEntity {
    private static final double RECOVERY = 0.6;

    public RecyclerBlockEntity(BlockPos pos, BlockState state) {
        super(Economy.RECYCLER_BLOCK_ENTITY.value(), pos, state, 1, ProcessingMenu.BIG_OUTPUTS);
    }

    @Override protected int time() { return 80; }
    @Override protected int energyPerTick() { return 30; }
    @Override protected MenuType<ProcessingMenu> menuType() { return Economy.RECYCLER_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.recycler"; }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return slot == 0 && level != null && Recycling.of(level.getServer(), stack) != null;
    }

    @Override
    protected @Nullable Operation find(Level level) {
        ItemStack input = items.get(0);
        Recycling.Breakdown breakdown = Recycling.of(level.getServer(), input);
        if (breakdown == null || input.getCount() < breakdown.makes()) return null;
        List<ItemStack> most = new ArrayList<>();
        for (ItemStack part : breakdown.parts()) most.add(part.copy());
        return new Operation(new int[] {breakdown.makes()}, most);
    }

    @Override
    protected List<ItemStack> produce(Level level, Operation operation) {
        ItemStack input = items.get(0);
        double wear = input.isDamageableItem() && input.getMaxDamage() > 0
                ? 1.0 - input.getDamageValue() / (double) input.getMaxDamage() : 1.0;
        RandomSource random = level.getRandom();
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack part : operation.maxOutputs()) {
            int back = 0;
            for (int i = 0; i < part.getCount(); i++) if (random.nextDouble() < RECOVERY * wear) back++;
            if (back > 0) out.add(part.copyWithCount(back));
        }
        AirQuality.add(level, worldPosition, -2);
        return out;
    }
}
