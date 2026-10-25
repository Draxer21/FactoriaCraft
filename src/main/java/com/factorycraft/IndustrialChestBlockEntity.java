package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Industrial chest: 108 slots that belts, hoppers and robot arms can fill and
 * empty. Right-click with the wrench to lock its filter: then it only accepts
 * item types it already holds (great for sorting lines).
 */
public final class IndustrialChestBlockEntity extends MachineBlockEntity implements MenuProvider {
    public static final int COLUMNS = 18;
    public static final int ROWS = 6;
    public static final int SIZE = COLUMNS * ROWS;
    private boolean filtered;

    public IndustrialChestBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.INDUSTRIAL_CHEST_BLOCK_ENTITY.value(), pos, state, SIZE);
    }

    @Override protected boolean isInputSlot(int slot) { return true; }
    @Override protected boolean isOutputSlot(int slot) { return false; }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        if (!filtered) return true;
        for (ItemStack held : items) {
            if (!held.isEmpty() && ItemStack.isSameItem(held, stack)) return true;
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return true;
    }

    @Override
    protected boolean work(Level level) {
        return filtered;
    }

    public boolean toggleFilter() {
        filtered = !filtered;
        setChanged();
        return filtered;
    }

    @Override
    public Component getDisplayName() {
        Component name = Component.translatable("block.factorycraft.industrial_chest");
        return filtered ? name.copy().append(" ").append(Component.translatable("gui.factorycraft.chest.filtered")) : name;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new IndustrialChestMenu(id, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        filtered = input.getIntOr("Filtered", 0) == 1;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Filtered", filtered ? 1 : 0);
    }
}
