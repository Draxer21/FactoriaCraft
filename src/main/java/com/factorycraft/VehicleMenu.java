package com.factorycraft;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * A machine's screen: its storage bay (0 to 9 rows), the four upgrade-module
 * slots on the left, and the player inventory. Modules are installed simply by
 * putting them in a module slot.
 */
public final class VehicleMenu extends AbstractContainerMenu {
    public static final int MODULE_SLOTS = VehicleEngine.MAX_UPGRADES;

    private final Container bay;
    private final Container modules;
    private final int rows;
    private final @Nullable Entity vehicle;

    /** Opens the screen of {@code vehicle} for {@code player} (server side). */
    public static void open(Player player, Entity vehicle, Container bay, int rows) {
        VehicleEngine engine = VehicleEngine.of(vehicle);
        if (engine == null) return;
        player.openMenu(new SimpleMenuProvider(
                        (id, inventory, p) -> new VehicleMenu(id, inventory, bay, engine.modules(), rows, vehicle),
                        vehicle.getDisplayName()),
                buf -> {
                    buf.writeVarInt(rows);
                    buf.writeVarInt(vehicle.getId());
                });
    }

    /** Client side: empty stand-ins filled by the server's sync. */
    public static VehicleMenu client(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        int rows = buf.readVarInt();
        Entity vehicle = inventory.player.level().getEntity(buf.readVarInt());
        return new VehicleMenu(id, inventory, new SimpleContainer(rows * 9), new SimpleContainer(MODULE_SLOTS), rows, vehicle);
    }

    public VehicleMenu(int id, Inventory inventory, Container bay, Container modules, int rows, @Nullable Entity vehicle) {
        super(FactoryCraft.VEHICLE_MENU.value(), id);
        this.bay = bay;
        this.modules = modules;
        this.rows = rows;
        this.vehicle = vehicle;
        bay.startOpen(inventory.player);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < 9; c++) addSlot(new Slot(bay, c + r * 9, 8 + c * 18, 18 + r * 18));
        }
        for (int i = 0; i < MODULE_SLOTS; i++) addSlot(new ModuleSlot(i, -21, 18 + i * 18));
        int offset = (rows - 4) * 18;
        addStandardInventorySlots(inventory, 8, 103 + offset);
    }

    public int rows() {
        return rows;
    }

    public @Nullable VehicleEngine engine() {
        return vehicle == null ? null : VehicleEngine.of(vehicle);
    }

    @Override
    public boolean stillValid(Player player) {
        return vehicle != null && vehicle.isAlive() && player.distanceToSqr(vehicle) < 64.0
                && (rows == 0 || bay.stillValid(player));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        bay.stopOpen(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int bayEnd = rows * 9;
        int modulesEnd = bayEnd + MODULE_SLOTS;
        int playerEnd = modulesEnd + 36;
        if (index < modulesEnd) {
            if (!moveItemStackTo(stack, modulesEnd, playerEnd, true)) return ItemStack.EMPTY;
        } else {
            boolean moved = stack.getItem() instanceof UpgradeModuleItem && moveItemStackTo(stack, bayEnd, modulesEnd, false);
            if (!moved && (bayEnd == 0 || !moveItemStackTo(stack, 0, bayEnd, false))) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    /** Takes one fitting module; refuses doubles and anything over the 4 module slots. */
    private final class ModuleSlot extends Slot {
        ModuleSlot(int index, int x, int y) {
            super(modules, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            VehicleEngine engine = engine();
            if (!(stack.getItem() instanceof UpgradeModuleItem)) return false;
            return engine == null || engine.canInstall(stack, getContainerSlot());
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
