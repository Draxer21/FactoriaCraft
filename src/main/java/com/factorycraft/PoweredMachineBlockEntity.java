package com.factorycraft;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A processing machine that runs on power from generators (through cables).
 * Slots 0..inputs-1 are inputs, the next four are outputs. Each operation takes
 * {@link #time()} ticks and {@link #energyPerTick()} energy per tick; without
 * power it just waits, keeping its progress.
 */
public abstract class PoweredMachineBlockEntity extends MachineBlockEntity implements MenuProvider, Energy.Consumer {
    /** One operation: what it eats (per input slot) and what it may produce. */
    protected record Operation(int[] consume, List<ItemStack> maxOutputs) {}

    private final int inputs;
    private final int outputs;
    private final Energy.Link link = new Energy.Link(this);
    private int progress;
    private int networkStored;
    private int networkCapacity;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int id) {
            return switch (id) {
                case 0 -> progress;
                case 1 -> scaledTime(time());
                case 2 -> networkStored;
                case 3 -> networkCapacity;
                case 4 -> 1;
                default -> 0;
            };
        }

        @Override
        public void set(int id, int value) {
            switch (id) {
                case 0 -> progress = value;
                case 2 -> networkStored = value;
                case 3 -> networkCapacity = value;
                default -> { }
            }
        }

        @Override
        public int getCount() {
            return ProcessingMenu.DATA_COUNT;
        }
    };

    protected PoweredMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int inputs) {
        this(type, pos, state, inputs, ProcessingMenu.OUTPUTS);
    }

    protected PoweredMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int inputs, int outputs) {
        super(type, pos, state, inputs + outputs);
        this.inputs = inputs;
        this.outputs = outputs;
    }

    // ---------------------------------------------------------------- per machine

    protected abstract int time();

    protected abstract int energyPerTick();

    protected abstract MenuType<ProcessingMenu> menuType();

    protected abstract String nameKey();

    /** The operation the current inputs allow, or null. */
    protected abstract @Nullable Operation find(Level level);

    /** Produces the actual results of a finished operation (may be random). */
    protected abstract List<ItemStack> produce(Level level, Operation operation);

    // ---------------------------------------------------------------- machine

    @Override
    public boolean supportsUpgrades() {
        return true;
    }

    @Override
    protected boolean isInputSlot(int slot) {
        return slot < inputs;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return slot >= inputs;
    }

    @Override
    protected boolean work(Level level) {
        if (level.getGameTime() % 10 == 0) {
            networkStored = link.stored(level);
            networkCapacity = link.capacity(level);
        }
        Operation op = find(level);
        if (op == null || !canStore(op.maxOutputs())) {
            progress = 0;
            return false;
        }
        int cost = Math.max(1, (int) Math.ceil(energyPerTick() * energyFactor()));
        if (link.draw(level, cost) < cost) return false; // no power: wait
        if (++progress >= scaledTime(time())) {
            progress = 0;
            List<ItemStack> results = boost(level, produce(level, op));
            if (!canStore(results)) results = produce(level, op);
            for (int i = 0; i < inputs; i++) {
                if (op.consume()[i] > 0) items.get(i).shrink(op.consume()[i]);
            }
            store(results);
        }
        return true;
    }

    @Override
    public Component getDisplayName() {
        return withUpgrades(Component.translatable(nameKey()));
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ProcessingMenu(menuType(), id, inventory, this, data, false, inputs, outputs);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        progress = input.getIntOr("Progress", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Progress", progress);
    }
}
