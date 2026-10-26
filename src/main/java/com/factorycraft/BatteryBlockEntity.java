package com.factorycraft;

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

/**
 * Big battery: soaks up spare power from the generators on its cable network
 * (up to 2 000 E/t) and gives it back to machines when the generators can't
 * keep up, e.g. solar power at night. Holds 10 000 000 E. A chargeable item
 * (drill, rifle...) in its slot is charged from it.
 */
public final class BatteryBlockEntity extends MachineBlockEntity implements MenuProvider, Energy.Source {
    public static final int CAPACITY = 10_000_000;
    private static final int TRANSFER = 2_000;

    private final Energy.Link link = new Energy.Link(this).generatorsOnly();
    private int energy;
    private int rate;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int id) {
            return switch (id) {
                case 0 -> energy / 1000;
                case 1 -> CAPACITY / 1000;
                case 2, 4 -> rate;
                case 3 -> TRANSFER;
                default -> 0;
            };
        }

        @Override
        public void set(int id, int value) {
            switch (id) {
                case 0 -> energy = value * 1000;
                case 2, 4 -> rate = value;
                default -> { }
            }
        }

        @Override
        public int getCount() {
            return GeneratorMenu.DATA_COUNT;
        }
    };

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.BATTERY_BLOCK_ENTITY.value(), pos, state, 1);
    }

    @Override protected boolean isInputSlot(int slot) { return slot == 0; }
    @Override protected boolean isOutputSlot(int slot) { return false; }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return slot == 0 && ItemEnergy.canCharge(stack);
    }

    @Override
    protected boolean work(Level level) {
        rate = 0;
        if (energy < CAPACITY) {
            int got = link.draw(level, Math.min(TRANSFER, CAPACITY - energy));
            if (got > 0) {
                energy += got;
                rate = got;
                setChanged();
            }
        }
        ItemStack tool = items.get(0);
        if (!tool.isEmpty() && energy > 0) {
            int taken = ItemEnergy.charge(tool, Math.min(500, energy));
            if (taken > 0) {
                energy -= taken;
                setChanged();
            }
        }
        return energy > 0;
    }

    @Override
    public int extract(int max) {
        int taken = Math.min(max, energy);
        if (taken > 0) {
            energy -= taken;
            setChanged();
        }
        return taken;
    }

    @Override
    public void setStored(int value) {
        energy = value;
        setChanged();
    }

    @Override public int stored() { return energy; }
    @Override public int capacity() { return CAPACITY; }
    @Override public boolean isStorage() { return true; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.factorycraft.battery");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new GeneratorMenu(FactoryCraft.BATTERY_MENU.value(), id, inventory, this, data);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy = input.getIntOr("Energy", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Energy", energy);
    }
}
