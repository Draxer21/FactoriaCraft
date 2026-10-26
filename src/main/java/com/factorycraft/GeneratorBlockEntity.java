package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Diesel generator: burns furnace fuel (coal, charcoal, logs, lava buckets...)
 * and stores the power for machines connected by cable. One coal = 64 000 energy.
 * It only burns while there is room in its buffer, so nothing is wasted.
 */
public final class GeneratorBlockEntity extends MachineBlockEntity implements MenuProvider, Energy.Source {
    public static final int CAPACITY = 100_000;
    /** Energy made per tick of burning fuel. */
    public static final int OUTPUT = 40;

    private int energy;
    /** Burning biofuel: no smog. */
    private boolean cleanFuel;
    private int burnLeft;
    private int burnMax;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int id) {
            return switch (id) {
                case 0 -> energy / 10;
                case 1 -> CAPACITY / 10;
                case 2 -> burnLeft;
                case 3 -> burnMax;
                case 4 -> burnLeft > 0 && energy + OUTPUT <= CAPACITY ? OUTPUT : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int id, int value) {
            switch (id) {
                case 0 -> energy = value * 10;
                case 2 -> burnLeft = value;
                case 3 -> burnMax = value;
                default -> { }
            }
        }

        @Override
        public int getCount() {
            return GeneratorMenu.DATA_COUNT;
        }
    };

    public GeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.GENERATOR_BLOCK_ENTITY.value(), pos, state, 1);
    }

    @Override
    protected boolean isInputSlot(int slot) {
        return slot == 0;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return false;
    }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return slot == 0 && level != null && level.fuelValues().isFuel(stack);
    }

    @Override
    protected boolean work(Level level) {
        if (burnLeft <= 0 && energy + OUTPUT <= CAPACITY) {
            ItemStack fuel = items.get(0);
            int burn = fuel.isEmpty() ? 0 : level.fuelValues().burnDuration(fuel);
            if (burn > 0) {
                cleanFuel = fuel.is(FactoryCraft.BIOFUEL.value());
                ItemStackTemplate remainder = fuel.getCraftingRemainder();
                fuel.shrink(1);
                if (fuel.isEmpty() && remainder != null) items.set(0, remainder.create());
                burnLeft = burnMax = burn;
                setChanged();
            }
        }
        if (burnLeft <= 0) return false;
        // Smog unless a chimney filter sits on top.
        if (level.getGameTime() % 200 == 0 && level instanceof net.minecraft.server.level.ServerLevel server
                && !hasFilter(level) && !cleanFuel) {
            Pollution.smog(server, worldPosition);
        }
        if (energy + OUTPUT > CAPACITY) return true; // full: idle, keep the fire
        burnLeft--;
        energy += OUTPUT;
        setChanged();
        return true;
    }

    public boolean hasFilter(Level level) {
        return level.getBlockState(worldPosition.above()).is(FactoryCraft.CHIMNEY_FILTER.value());
    }

    /** A bucket of lava piped in by a pump: burns like a lava bucket. */
    public boolean acceptLava() {
        if (burnLeft > 1000) return false;
        burnLeft += 20_000;
        burnMax = burnLeft;
        setChanged();
        return true;
    }

    // ---------------------------------------------------------------- energy

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

    @Override
    public int stored() {
        return energy;
    }

    @Override
    public int rate() {
        return burnLeft > 0 && energy + OUTPUT <= CAPACITY ? OUTPUT : 0;
    }

    @Override
    public int capacity() {
        return CAPACITY;
    }

    // ---------------------------------------------------------------- menu & saving

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.factorycraft.generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new GeneratorMenu(id, inventory, this, data);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy = input.getIntOr("Energy", 0);
        burnLeft = input.getIntOr("BurnLeft", 0);
        burnMax = input.getIntOr("BurnMax", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Energy", energy);
        output.putInt("BurnLeft", burnLeft);
        output.putInt("BurnMax", burnMax);
    }
}
