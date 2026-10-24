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

/**
 * Reactor screen: fuel rod in, depleted rod out.
 * Data: 0 energy/100, 1 capacity/100, 2 burn left/10, 3 burn max/10, 4 E/t, 5 heat (0-10000), 6 water (mB).
 */
public final class ReactorMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 7;

    private final Container machine;
    private final ContainerData data;

    public static ReactorMenu client(int id, Inventory inventory) {
        return new ReactorMenu(id, inventory, new SimpleContainer(2), new SimpleContainerData(DATA_COUNT));
    }

    public ReactorMenu(int id, Inventory inventory, Container machine, ContainerData data) {
        super(Nuclear.REACTOR_MENU.value(), id);
        checkContainerSize(machine, 2);
        this.machine = machine;
        this.data = data;
        machine.startOpen(inventory.player);
        addSlot(new Slot(machine, 0, 26, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return container.canPlaceItem(getContainerSlot(), stack);
            }
        });
        addSlot(new Slot(machine, 1, 62, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addStandardInventorySlots(inventory, 8, 84);
        addDataSlots(data);
    }

    public int energy() { return data.get(0) * 100; }
    public float energyFraction() { int max = data.get(1); return max <= 0 ? 0 : Math.min(1.0F, (float) data.get(0) / max); }
    public float burn() { int max = data.get(3); return max <= 0 ? 0 : Math.min(1.0F, (float) data.get(2) / max); }
    public int rate() { return data.get(4); }
    public float heat() { return Math.min(1.0F, data.get(5) / (float) NuclearReactorBlockEntity.MAX_HEAT); }
    public int water() { return data.get(6); }
    public float waterFraction() { return Math.min(1.0F, water() / (float) NuclearReactorBlockEntity.WATER_CAPACITY); }

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
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, 38, true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }
}
