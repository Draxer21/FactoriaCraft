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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A clean generator that needs no fuel (wind, geothermal, steam turbine):
 * it makes {@link #computeRate} E/t into its buffer, feeds cables like any
 * generator, and charges a tool in its slot.
 */
public abstract class PassiveGeneratorBlockEntity extends MachineBlockEntity implements MenuProvider, Energy.Source {
    private final int capacity;
    protected int energy;
    protected int rate;
    private int rateMax;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int id) {
            return switch (id) {
                case 0 -> energy / 100;
                case 1 -> capacity / 100;
                case 2, 4 -> rate;
                case 3 -> rateMax;
                default -> 0;
            };
        }

        @Override
        public void set(int id, int value) {
            switch (id) {
                case 0 -> energy = value * 100;
                case 2, 4 -> rate = value;
                case 3 -> rateMax = value;
                default -> { }
            }
        }

        @Override
        public int getCount() {
            return GeneratorMenu.DATA_COUNT;
        }
    };

    protected PassiveGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity, int rateMax) {
        super(type, pos, state, 1);
        this.capacity = capacity;
        this.rateMax = rateMax;
    }

    /** Energy per tick right now. */
    protected abstract int computeRate(Level level);

    protected abstract String nameKey();

    /** Recompute only every so often (scans the surroundings). */
    protected int recomputeEvery() {
        return 20;
    }

    @Override protected boolean isInputSlot(int slot) { return slot == 0; }
    @Override protected boolean isOutputSlot(int slot) { return false; }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return slot == 0 && ItemEnergy.canCharge(stack);
    }

    @Override
    protected boolean work(Level level) {
        if (level.getGameTime() % recomputeEvery() == 0) rate = computeRate(level);
        if (rate > 0 && energy < capacity) {
            energy = Math.min(capacity, energy + rate);
            setChanged();
        }
        ItemStack tool = items.get(0);
        if (!tool.isEmpty() && energy > 0) {
            int taken = ItemEnergy.charge(tool, Math.min(200, energy));
            if (taken > 0) {
                energy -= taken;
                setChanged();
            }
        }
        return rate > 0;
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

    @Override public int stored() { return energy; }
    @Override public int capacity() { return capacity; }
    @Override public int rate() { return rate; }

    @Override
    public Component getDisplayName() {
        return Component.translatable(nameKey());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new GeneratorMenu(FactoryCraft.PASSIVE_GENERATOR_MENU.value(), id, inventory, this, data);
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
