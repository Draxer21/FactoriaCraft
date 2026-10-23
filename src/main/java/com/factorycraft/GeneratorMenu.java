package com.factorycraft;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Generator screen: one fuel slot. Data: 0 energy/10, 1 capacity/10, 2 burn left, 3 burn max. */
public final class GeneratorMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 4;

    private final Container machine;
    private final ContainerData data;

    public static GeneratorMenu client(int id, Inventory inventory) {
        return new GeneratorMenu(id, inventory, new SimpleContainer(1), new SimpleContainerData(DATA_COUNT));
    }

    public GeneratorMenu(int id, Inventory inventory, Container machine, ContainerData data) {
        super(FactoryCraft.GENERATOR_MENU.value(), id);
        checkContainerSize(machine, 1);
        this.machine = machine;
        this.data = data;
        machine.startOpen(inventory.player);
        addSlot(new Slot(machine, 0, 56, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return container.canPlaceItem(getContainerSlot(), stack);
            }
        });
        addStandardInventorySlots(inventory, 8, 84);
        addDataSlots(data);
    }

    public float energy() {
        int max = data.get(1);
        return max <= 0 ? 0.0F : Math.min(1.0F, (float) data.get(0) / max);
    }

    public int energyValue() {
        return data.get(0) * 10;
    }

    public float burn() {
        int max = data.get(3);
        return max <= 0 ? 0.0F : Math.min(1.0F, (float) data.get(2) / max);
    }

    public boolean isBurning() {
        return data.get(2) > 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        machine.stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, 37, true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }
}
