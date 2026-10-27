package com.factorycraft;

import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Garage: sneak + right-click it with an empty hand to park the nearest empty
 * vehicle (within 8 blocks) inside: it becomes its item, keeping its cargo,
 * modules, fuel and everything. Parked vehicles are refuelled from the power
 * network (200 fuel for 400 power every 2 seconds). Take them out of its screen
 * and place them anywhere, just as they were.
 */
public final class GarageBlockEntity extends PoweredMachineBlockEntity {
    public static final String VEHICLE_KEY = "FactoryCraftVehicle";
    private static final int FUEL_STEP = 200;
    private static final int ENERGY_PER_STEP = 400;
    private int timer;

    public GarageBlockEntity(BlockPos pos, BlockState state) {
        super(Logistics.GARAGE_BLOCK_ENTITY.value(), pos, state, 0, ProcessingMenu.BIG_OUTPUTS);
    }

    @Override protected int time() { return 40; }
    @Override protected int energyPerTick() { return 0; }
    @Override protected MenuType<ProcessingMenu> menuType() { return Logistics.GARAGE_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.garage"; }
    @Override public boolean supportsUpgrades() { return false; }
    @Override protected boolean accepts(int slot, ItemStack stack) { return false; }
    /** Vehicles stay inside until a player takes them (no conveyor or hopper output). */
    @Override protected boolean isOutputSlot(int slot) { return false; }
    @Override protected @Nullable Operation find(Level level) { return null; }
    @Override protected List<ItemStack> produce(Level level, Operation operation) { return List.of(); }

    /** Can this entity be parked? Driveable machines placed by a vehicle item, nobody aboard. */
    static boolean parkable(Entity entity) {
        if (!(entity instanceof AbstractBoat boat) || !boat.isAlive() || boat.isVehicle()) return false;
        ItemStack pick = boat.getPickResult();
        return pick != null && pick.getItem() instanceof VehicleItem;
    }

    /** Packs a vehicle into its item, with all its data. */
    static ItemStack pack(AbstractBoat vehicle) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, vehicle.registryAccess());
        vehicle.saveWithoutId(output);
        CompoundTag data = output.buildResult();
        data.remove("UUID");
        ItemStack stack = vehicle.getPickResult().copyWithCount(1);
        CompoundTag custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        custom.put(VEHICLE_KEY, data);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
        if (vehicle instanceof net.minecraft.world.Container container) container.clearContent();
        vehicle.remove(Entity.RemovalReason.CHANGED_DIMENSION);
        return stack;
    }

    public void park(Player player) {
        player.sendOverlayMessage(parkNearest());
    }

    /** Parks the nearest empty vehicle; returns what happened. */
    Component parkNearest() {
        if (level == null) return Component.empty();
        int free = 0;
        for (int i = 0; i < getContainerSize(); i++) if (items.get(i).isEmpty()) free++;
        if (free == 0) return Component.translatable("message.factorycraft.garage.full");
        AbstractBoat vehicle = level.getEntitiesOfClass(AbstractBoat.class, new AABB(worldPosition).inflate(8.0), GarageBlockEntity::parkable)
                .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition)))).orElse(null);
        if (vehicle == null) return Component.translatable("message.factorycraft.garage.none");
        Component name = vehicle.getName();
        store(List.of(pack(vehicle)));
        setChanged();
        level.playSound(null, worldPosition, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.8F, 0.8F);
        return Component.translatable("message.factorycraft.garage.parked", name);
    }

    /** Fuel stored in a parked vehicle item, or -1. */
    static int fuelOf(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return -1;
        CompoundTag tag = data.copyTag();
        return tag.getCompound(VEHICLE_KEY).map(v -> v.getIntOr("Fuel", -1)).orElse(-1);
    }

    @Override
    protected boolean work(Level level) {
        if (++timer < 40) return false;
        timer = 0;
        boolean busy = false;
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack stack = items.get(i);
            int fuel = fuelOf(stack);
            if (fuel < 0 || fuel >= VehicleEngine.MAX_FUEL) continue;
            if (!drawPower(level, ENERGY_PER_STEP)) break;
            int newFuel = Math.min(VehicleEngine.MAX_FUEL, fuel + FUEL_STEP);
            CompoundTag custom = stack.get(DataComponents.CUSTOM_DATA).copyTag();
            custom.getCompound(VEHICLE_KEY).ifPresent(v -> v.putInt("Fuel", newFuel));
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
            busy = true;
        }
        if (busy) setChanged();
        return busy;
    }
}
