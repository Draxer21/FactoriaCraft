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
 * Assembler screen: the 3x3 pattern (ghost slots, set by clicking with an item;
 * nothing is taken), the 3x3 ingredient buffer, 4 outputs and the player inventory.
 */
public final class AssemblerMenu extends AbstractContainerMenu implements GhostMenu {
    public static final int PATTERN_X = 8;
    public static final int BUFFER_X = 70;
    public static final int GRID_Y = 18;
    public static final int[][] OUTPUT_POS = {{134, 27}, {152, 27}, {134, 45}, {152, 45}};
    public static final int INVENTORY_Y = 90;

    private final Container machine;
    private final Container pattern;
    private final ContainerData data;
    private final AssemblerBlockEntity assembler;

    /** Client side: empty stand-ins, filled by the server's sync. */
    public AssemblerMenu(int id, Inventory inventory) {
        this(id, inventory, null, new SimpleContainer(AssemblerBlockEntity.PATTERN), new SimpleContainerData(ProcessingMenu.DATA_COUNT));
    }

    public AssemblerMenu(int id, Inventory inventory, AssemblerBlockEntity assembler, Container pattern, ContainerData data) {
        super(Automation.ASSEMBLER_MENU.value(), id);
        this.assembler = assembler;
        this.machine = assembler != null ? assembler : new SimpleContainer(AssemblerBlockEntity.BUFFER + 4);
        this.pattern = pattern;
        this.data = data;
        machine.startOpen(inventory.player);
        for (int i = 0; i < AssemblerBlockEntity.PATTERN; i++) {
            addSlot(new PatternSlot(pattern, i, PATTERN_X + (i % 3) * 18, GRID_Y + (i / 3) * 18));
        }
        for (int i = 0; i < AssemblerBlockEntity.BUFFER; i++) {
            addSlot(new BufferSlot(machine, i, BUFFER_X + (i % 3) * 18, GRID_Y + (i / 3) * 18));
        }
        for (int i = 0; i < 4; i++) {
            addSlot(new OutputSlot(machine, AssemblerBlockEntity.BUFFER + i, OUTPUT_POS[i][0], OUTPUT_POS[i][1]));
        }
        addStandardInventorySlots(inventory, 8, INVENTORY_Y);
        addDataSlots(data);
    }

    /** Server: a pattern slot set from the screen. */
    @Override
    public void setGhost(int slot, ItemStack stack) {
        if (assembler != null) assembler.setPattern(slot, stack);
        broadcastChanges();
    }

    public float progress() {
        int max = data.get(1);
        return max <= 0 ? 0.0F : Math.min(1.0F, (float) data.get(0) / max);
    }

    public float energy() {
        int max = data.get(3);
        return max <= 0 ? 0.0F : Math.min(1.0F, (float) data.get(2) / max);
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
        if (index < AssemblerBlockEntity.PATTERN) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int machineEnd = AssemblerBlockEntity.PATTERN + AssemblerBlockEntity.BUFFER + 4;
        if (index < machineEnd) {
            if (!moveItemStackTo(stack, machineEnd, machineEnd + 36, true)) return ItemStack.EMPTY;
        } else {
            int start = AssemblerBlockEntity.PATTERN;
            if (!moveItemStackTo(stack, start, start + AssemblerBlockEntity.BUFFER, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    /** Shows the pattern; clicks are handled by the screen (AssemblerPatternPayload). */
    static final class PatternSlot extends Slot {
        PatternSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    private static final class BufferSlot extends Slot {
        BufferSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return container.canPlaceItem(getContainerSlot(), stack);
        }
    }

    private static final class OutputSlot extends Slot {
        OutputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
