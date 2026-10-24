package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * Solar generator: free power while the sun shines on it (less when it rains,
 * none at night). Like the diesel generator it feeds machines through cables,
 * and it also works as a charging station:
 *  - a drill in its slot is charged;
 *  - machines parked next to it (within 4 blocks) are refuelled from its
 *    battery, 40 energy per unit of fuel.
 */
public final class SolarGeneratorBlockEntity extends MachineBlockEntity implements MenuProvider, Energy.Source {
    public static final int CAPACITY = 40_000;
    /** Energy per tick in full sunlight. */
    public static final int OUTPUT = 16;
    public static final int ENERGY_PER_FUEL = 40;
    private static final int CHARGE_RATE = 200;
    private static final double VEHICLE_RANGE = 4.0;

    private int energy;
    private int rate;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int id) {
            return switch (id) {
                case 0 -> energy / 10;
                case 1 -> CAPACITY / 10;
                case 2, 4 -> rate;
                case 3 -> OUTPUT;
                default -> 0;
            };
        }

        @Override
        public void set(int id, int value) {
            switch (id) {
                case 0 -> energy = value * 10;
                case 2, 4 -> rate = value;
                default -> { }
            }
        }

        @Override
        public int getCount() {
            return GeneratorMenu.DATA_COUNT;
        }
    };

    public SolarGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.SOLAR_GENERATOR_BLOCK_ENTITY.value(), pos, state, 1);
    }

    @Override protected boolean isInputSlot(int slot) { return slot == 0; }
    @Override protected boolean isOutputSlot(int slot) { return false; }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return slot == 0 && stack.getItem() instanceof DrillItem;
    }

    /** Current production, 0..OUTPUT. */
    private int sunlight(Level level) {
        if (!level.isBrightOutside() || !level.canSeeSky(worldPosition.above())) return 0;
        if (level.isThundering()) return 0;
        return level.isRaining() ? OUTPUT * 2 / 5 : OUTPUT;
    }

    @Override
    protected boolean work(Level level) {
        rate = sunlight(level);
        if (rate > 0 && energy < CAPACITY) {
            energy = Math.min(CAPACITY, energy + rate);
            setChanged();
        }
        // Charge the drill in the slot.
        ItemStack tool = items.get(0);
        if (!tool.isEmpty() && energy > 0) {
            int taken = DrillItem.charge(tool, Math.min(CHARGE_RATE, energy));
            if (taken > 0) {
                energy -= taken;
                setChanged();
            }
        }
        // Refuel parked machines once a second.
        if (level.getGameTime() % 20 == 0 && energy >= ENERGY_PER_FUEL) {
            AABB area = new AABB(worldPosition).inflate(VEHICLE_RANGE);
            for (Entity entity : level.getEntities((Entity) null, area, e -> e instanceof VehicleEngine.Holder)) {
                VehicleEngine engine = ((VehicleEngine.Holder) entity).engine();
                int room = engine.maxFuel() - engine.getFuel();
                int units = Math.min(room, energy / ENERGY_PER_FUEL);
                if (units > 0 && engine.addFuel(units)) {
                    energy -= units * ENERGY_PER_FUEL;
                    setChanged();
                }
            }
        }
        return rate > 0;
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

    @Override public int stored() { return energy; }
    @Override public int capacity() { return CAPACITY; }

    // ---------------------------------------------------------------- menu & saving

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.factorycraft.solar_generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new GeneratorMenu(FactoryCraft.SOLAR_GENERATOR_MENU.value(), id, inventory, this, data);
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
