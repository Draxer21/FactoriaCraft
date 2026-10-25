package com.factorycraft;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Crushes one item from each of its six input slots at once, every
 * {@link #CRUSH_TIME} ticks. Slots 0..5 = input, 6..13 = output.
 */
public final class CrusherBlockEntity extends MachineBlockEntity implements MenuProvider {
    public static final int CRUSH_TIME = 30;
    private static final int INPUTS = ProcessingMenu.BIG_INPUTS;
    private int progress;

    /** Synced to the screen: progress, progress max, (no fuel). */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int id) {
            return switch (id) {
                case 0 -> progress;
                case 1 -> scaledTime(CRUSH_TIME);
                default -> 0;
            };
        }

        @Override
        public void set(int id, int value) {
            if (id == 0) progress = value;
        }

        @Override
        public int getCount() {
            return ProcessingMenu.DATA_COUNT;
        }
    };

    @Override
    public Component getDisplayName() {
        return withUpgrades(Component.translatable("block.factorycraft.crusher"));
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ProcessingMenu(FactoryCraft.CRUSHER_MENU.value(), id, inventory, this, data, false,
                INPUTS, ProcessingMenu.BIG_OUTPUTS);
    }

    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.CRUSHER_BLOCK_ENTITY.value(), pos, state, INPUTS + ProcessingMenu.BIG_OUTPUTS);
    }

    @Override
    public boolean supportsUpgrades() {
        return true;
    }

    @Override
    protected boolean isInputSlot(int slot) {
        return slot < INPUTS;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return slot >= INPUTS;
    }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return slot < INPUTS && CrusherRecipes.resultFor(stack) != null;
    }

    /** Results of crushing one item from every input slot that has something crushable. */
    private List<ItemStack> batch() {
        List<ItemStack> results = new ArrayList<>();
        for (int i = 0; i < INPUTS; i++) {
            ItemStack result = CrusherRecipes.resultFor(items.get(i));
            if (result != null && !items.get(i).isEmpty()) results.add(result);
        }
        return results;
    }

    @Override
    protected boolean work(Level level) {
        List<ItemStack> results = batch();
        if (results.isEmpty() || !canStore(results)) {
            progress = 0;
            return false;
        }
        if (++progress >= scaledTime(CRUSH_TIME)) {
            progress = 0;
            for (int i = 0; i < INPUTS; i++) {
                if (CrusherRecipes.resultFor(items.get(i)) != null) items.get(i).shrink(1);
            }
            List<ItemStack> boosted = boost(level, results);
            store(canStore(boosted) ? boosted : results);
        }
        return true;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        migrateToBigLayout(input, INPUTS);
        progress = input.getIntOr("Progress", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Layout", 2);
        output.putInt("Progress", progress);
    }
}
