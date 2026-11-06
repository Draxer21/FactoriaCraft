package com.factorycraft;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Bus screen: the 9 filter ghost slots and the player inventory. */
public final class BusMenu extends AbstractContainerMenu implements GhostMenu {
    public static final int FILTER_X = 62;
    public static final int FILTER_Y = 18;
    public static final int INVENTORY_Y = 84;

    private final @Nullable BusBlockEntity bus;

    public BusMenu(int id, Inventory inventory) {
        this(id, inventory, null, new SimpleContainer(BusBlockEntity.FILTER));
    }

    public BusMenu(int id, Inventory inventory, @Nullable BusBlockEntity bus, Container filter) {
        super(Automation.BUS_MENU.value(), id);
        this.bus = bus;
        for (int i = 0; i < BusBlockEntity.FILTER; i++) {
            addSlot(new AssemblerMenu.PatternSlot(filter, i, FILTER_X + (i % 3) * 18, FILTER_Y + (i / 3) * 18));
        }
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
    }

    @Override
    public void setGhost(int slot, ItemStack stack) {
        if (bus != null) bus.setFilter(slot, stack);
        broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return bus == null || (!bus.isRemoved() && net.minecraft.world.phys.Vec3.atCenterOf(bus.getBlockPos()).distanceToSqr(player.position()) <= 64.0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
