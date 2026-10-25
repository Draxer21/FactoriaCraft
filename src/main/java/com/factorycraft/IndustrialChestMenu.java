package com.factorycraft;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** 18x6 chest grid plus the player inventory centred below it. */
public final class IndustrialChestMenu extends AbstractContainerMenu {
    public static final int WIDTH = IndustrialChestBlockEntity.COLUMNS * 18 + 16;
    public static final int HEIGHT = 18 + IndustrialChestBlockEntity.ROWS * 18 + 14 + 76 + 8;
    public static final int INV_X = (WIDTH - 162) / 2;
    public static final int INV_Y = 18 + IndustrialChestBlockEntity.ROWS * 18 + 14;

    private final Container chest;

    public static IndustrialChestMenu client(int id, Inventory inventory) {
        return new IndustrialChestMenu(id, inventory, new SimpleContainer(IndustrialChestBlockEntity.SIZE));
    }

    public IndustrialChestMenu(int id, Inventory inventory, Container chest) {
        super(FactoryCraft.INDUSTRIAL_CHEST_MENU.value(), id);
        checkContainerSize(chest, IndustrialChestBlockEntity.SIZE);
        this.chest = chest;
        chest.startOpen(inventory.player);
        for (int row = 0; row < IndustrialChestBlockEntity.ROWS; row++) {
            for (int col = 0; col < IndustrialChestBlockEntity.COLUMNS; col++) {
                addSlot(new Slot(chest, row * IndustrialChestBlockEntity.COLUMNS + col, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return container.canPlaceItem(getContainerSlot(), stack);
                    }
                });
            }
        }
        addStandardInventorySlots(inventory, INV_X + 1, INV_Y + 1);
    }

    @Override
    public boolean stillValid(Player player) {
        return chest.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        chest.stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int size = IndustrialChestBlockEntity.SIZE;
        if (index < size) {
            if (!moveItemStackTo(stack, size, size + 36, true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, size, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }
}
