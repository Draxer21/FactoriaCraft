package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Storage terminal: one screen for every inventory on its data cables. See {@link StorageNetwork}. */
public final class StorageTerminalBlockEntity extends MachineBlockEntity implements MenuProvider {
    public StorageTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(Logistics.STORAGE_TERMINAL_BLOCK_ENTITY.value(), pos, state, 0);
    }

    @Override protected boolean isInputSlot(int slot) { return false; }
    @Override protected boolean isOutputSlot(int slot) { return false; }
    @Override protected boolean accepts(int slot, ItemStack stack) { return false; }

    @Override
    protected boolean work(Level level) {
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.factorycraft.storage_terminal");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new StorageTerminalMenu(id, inventory, worldPosition);
    }
}
