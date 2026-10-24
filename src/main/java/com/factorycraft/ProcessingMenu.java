package com.factorycraft;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Furnace-style menu shared by the processing machines: one to three input
 * slots, an optional fuel slot, four output slots, plus the player inventory.
 * Data slots: 0 progress, 1 progress max, 2 fuel left / energy stored,
 * 3 fuel max / energy capacity, 4 = 1 for machines that run on power.
 */
public final class ProcessingMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 5;
    public static final int OUTPUTS = 4;
    /** Big machines (crusher, ore washer): 6 inputs and 8 outputs. */
    public static final int BIG_INPUTS = 6;
    public static final int BIG_OUTPUTS = 8;
    /** Input slot positions for 1, 2, 3, 4 or 6 inputs, shared with the screen. */
    public static final int[][][] INPUT_POS = {
            {{56, 17}},
            {{47, 17}, {65, 17}},
            {{38, 17}, {56, 17}, {74, 17}},
            {{20, 17}, {38, 17}, {20, 35}, {38, 35}},
            {},
            {{14, 17}, {32, 17}, {50, 17}, {14, 35}, {32, 35}, {50, 35}}};
    /** Output grid positions (2x2), shared with the screen. */
    public static final int[][] OUTPUT_POS = {{112, 26}, {130, 26}, {112, 44}, {130, 44}};
    /** Output grid for the big machines (4x2). */
    public static final int[][] BIG_OUTPUT_POS = {{98, 26}, {116, 26}, {134, 26}, {152, 26}, {98, 44}, {116, 44}, {134, 44}, {152, 44}};

    public static int[][] outputPos(int outputs) {
        return outputs == BIG_OUTPUTS ? BIG_OUTPUT_POS : OUTPUT_POS;
    }

    private final Container machine;
    private final ContainerData data;
    private final boolean hasFuel;
    private final int inputs;
    private final int outputs;
    private final int machineSlots;

    /** Client side: empty stand-ins, filled in by the server's sync. */
    public static ProcessingMenu crusher(int id, Inventory inventory) {
        return new ProcessingMenu(FactoryCraft.CRUSHER_MENU.value(), id, inventory,
                new SimpleContainer(BIG_INPUTS + BIG_OUTPUTS), new SimpleContainerData(DATA_COUNT), false, BIG_INPUTS, BIG_OUTPUTS);
    }

    public static ProcessingMenu furnace(int id, Inventory inventory) {
        return new ProcessingMenu(FactoryCraft.INDUSTRIAL_FURNACE_MENU.value(), id, inventory,
                new SimpleContainer(2 + OUTPUTS), new SimpleContainerData(DATA_COUNT), true);
    }

    /** Client side for powered machines with {@code inputs} input slots. */
    public static ProcessingMenu powered(MenuType<?> type, int id, Inventory inventory, int inputs) {
        return powered(type, id, inventory, inputs, OUTPUTS);
    }

    public static ProcessingMenu powered(MenuType<?> type, int id, Inventory inventory, int inputs, int outputs) {
        return new ProcessingMenu(type, id, inventory, new SimpleContainer(inputs + outputs),
                new SimpleContainerData(DATA_COUNT), false, inputs, outputs);
    }

    public ProcessingMenu(MenuType<?> type, int id, Inventory inventory, Container machine, ContainerData data, boolean hasFuel) {
        this(type, id, inventory, machine, data, hasFuel, 1);
    }

    public ProcessingMenu(MenuType<?> type, int id, Inventory inventory, Container machine, ContainerData data,
                          boolean hasFuel, int inputs) {
        this(type, id, inventory, machine, data, hasFuel, inputs, OUTPUTS);
    }

    public ProcessingMenu(MenuType<?> type, int id, Inventory inventory, Container machine, ContainerData data,
                          boolean hasFuel, int inputs, int outputs) {
        super(type, id);
        checkContainerSize(machine, inputs + (hasFuel ? 1 : 0) + outputs);
        this.outputs = outputs;
        this.machine = machine;
        this.data = data;
        this.hasFuel = hasFuel;
        this.inputs = inputs;
        this.machineSlots = machine.getContainerSize();
        machine.startOpen(inventory.player);

        for (int i = 0; i < inputs; i++) {
            int[] pos = INPUT_POS[inputs - 1][i];
            addSlot(new MachineSlot(machine, i, pos[0], pos[1]));
        }
        int firstOutput = inputs;
        if (hasFuel) {
            addSlot(new MachineSlot(machine, inputs, 56, 53));
            firstOutput = inputs + 1;
        }
        int[][] outPos = outputPos(outputs);
        for (int i = 0; i < outputs; i++) {
            addSlot(new OutputSlot(machine, firstOutput + i, outPos[i][0], outPos[i][1]));
        }
        addStandardInventorySlots(inventory, 8, 84);
        addDataSlots(data);
    }

    public boolean hasFuel() {
        return hasFuel;
    }

    public int inputs() {
        return inputs;
    }

    public int outputs() {
        return outputs;
    }

    /** 6-in / 8-out layout. */
    public boolean isBig() {
        return inputs == BIG_INPUTS;
    }

    /** Runs on power from generators (shows an energy bar instead of a flame). */
    public boolean isPowered() {
        return data.get(4) == 1;
    }

    /** 0..1 energy stored in the connected network. */
    public float energy() {
        int max = data.get(3);
        return max <= 0 ? 0.0F : Math.min(1.0F, (float) data.get(2) / max);
    }

    /** 0..1 progress of the current operation. */
    public float progress() {
        int max = data.get(1);
        return max <= 0 ? 0.0F : Math.min(1.0F, (float) data.get(0) / max);
    }

    /** 0..1 fuel remaining in the current item. */
    public float fuel() {
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
        int playerStart = machineSlots;
        int playerEnd = playerStart + 36;

        if (index < machineSlots) {
            // Machine -> player inventory.
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) return ItemStack.EMPTY;
        } else {
            // Player -> input (or fuel); slots refuse what the machine can't use.
            int intoSlots = inputs + (hasFuel ? 1 : 0);
            if (!moveItemStackTo(stack, 0, intoSlots, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    /** Input/fuel slot: only what the machine accepts there. */
    private static final class MachineSlot extends Slot {
        MachineSlot(Container container, int index, int x, int y) {
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
