package com.factorycraft;

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

/** Crushes one item every {@link #CRUSH_TIME} ticks. Slot 0 = input, 1..4 = output. */
public final class CrusherBlockEntity extends MachineBlockEntity implements MenuProvider {
    public static final int CRUSH_TIME = 50;
    private int progress;

    /** Synced to the screen: progress, progress max, (no fuel). */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int id) {
            return switch (id) {
                case 0 -> progress;
                case 1 -> CRUSH_TIME;
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
        return Component.translatable("block.factorycraft.crusher");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ProcessingMenu(FactoryCraft.CRUSHER_MENU.value(), id, inventory, this, data, false);
    }

    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.CRUSHER_BLOCK_ENTITY.value(), pos, state, 5);
    }

    @Override
    protected boolean isInputSlot(int slot) {
        return slot == 0;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return slot >= 1;
    }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return slot == 0 && CrusherRecipes.resultFor(stack) != null;
    }

    @Override
    protected boolean work(Level level) {
        ItemStack input = items.get(0);
        ItemStack result = CrusherRecipes.resultFor(input);
        if (result == null || !canStore(List.of(result))) {
            progress = 0;
            return false;
        }
        if (++progress >= CRUSH_TIME) {
            progress = 0;
            input.shrink(1);
            store(List.of(result));
        }
        return true;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        progress = input.getIntOr("Progress", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Progress", progress);
    }
}
