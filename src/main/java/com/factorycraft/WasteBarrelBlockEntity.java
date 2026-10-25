package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lead waste barrel: safe storage for nuclear waste, depleted rods and other
 * radioactive items (9 slots). Hoppers and belts can fill it; nothing leaks.
 */
public final class WasteBarrelBlockEntity extends MachineBlockEntity implements MenuProvider {
    public WasteBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(Nuclear.WASTE_BARREL_BLOCK_ENTITY.value(), pos, state, 9);
    }

    @Override protected boolean isInputSlot(int slot) { return true; }
    @Override protected boolean isOutputSlot(int slot) { return false; }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return stack.getItem() instanceof Radiation.Source;
    }

    @Override
    protected boolean work(Level level) {
        for (ItemStack stack : items) if (!stack.isEmpty()) return true;
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.factorycraft.waste_barrel");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x1, id, inventory, this, 1);
    }
}
