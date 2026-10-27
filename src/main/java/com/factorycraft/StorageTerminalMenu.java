package com.factorycraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * Storage terminal: the player's inventory below; the network's contents are drawn
 * by the screen from {@link TerminalListPayload} and taken with {@link TerminalActionPayload}.
 * Shift-click in the inventory sends a stack into the network.
 */
public final class StorageTerminalMenu extends AbstractContainerMenu {
    public static final int WIDTH = 194;
    public static final int GRID_X = 8;
    public static final int GRID_Y = 32;
    public static final int COLUMNS = 9;
    public static final int ROWS = 6;
    public static final int INV_Y = GRID_Y + ROWS * 18 + 14;
    public static final int HEIGHT = INV_Y + 76 + 8;

    private final @Nullable BlockPos pos;
    private final Player player;
    private int timer;
    private int lastHash;

    public static StorageTerminalMenu client(int id, Inventory inventory) {
        return new StorageTerminalMenu(id, inventory, null);
    }

    public StorageTerminalMenu(int id, Inventory inventory, @Nullable BlockPos pos) {
        super(Logistics.STORAGE_TERMINAL_MENU.value(), id);
        this.pos = pos;
        this.player = inventory.player;
        addStandardInventorySlots(inventory, GRID_X + 9, INV_Y + 1);
    }

    private @Nullable StorageNetwork network() {
        if (pos == null || player.level().isClientSide()) return null;
        return StorageNetwork.scan(player.level(), pos);
    }

    @Override
    public boolean stillValid(Player player) {
        return pos == null || player.level().getBlockEntity(pos) instanceof StorageTerminalBlockEntity
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        StorageNetwork network = network();
        if (network == null || slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack rest = network.insert(slot.getItem().copy());
        slot.setByPlayer(rest);
        slot.setChanged();
        timer = 0;
        sendList();
        return ItemStack.EMPTY;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (pos != null && !player.level().isClientSide() && ++timer >= 10) {
            timer = 0;
            sendList();
        }
    }

    private void sendList() {
        StorageNetwork network = network();
        if (network == null || !(player instanceof ServerPlayer server)) return;
        List<TerminalListPayload.Entry> entries = new ArrayList<>();
        for (Map.Entry<ItemResource, Long> e : network.contents().entrySet()) {
            entries.add(new TerminalListPayload.Entry(e.getKey().toStack(1), e.getValue()));
            if (entries.size() >= 2000) break;
        }
        int hash = entries.hashCode() * 31 + network.inventories();
        if (hash == lastHash && timer != 0) return;
        lastHash = hash;
        PacketDistributor.sendToPlayer(server, new TerminalListPayload(network.inventories(), entries));
    }

    /** Server: a click on the network grid. */
    void handle(TerminalActionPayload action) {
        StorageNetwork network = network();
        if (network == null) return;
        ItemStack carried = getCarried();
        if (action.action() == TerminalActionPayload.INSERT_CARRIED) {
            if (carried.isEmpty()) return;
            if (action.single()) {
                ItemStack one = carried.copyWithCount(1);
                if (network.insert(one).isEmpty()) carried.shrink(1);
            } else {
                setCarried(network.insert(carried));
            }
        } else if (action.action() == TerminalActionPayload.EXTRACT && !action.stack().isEmpty()) {
            ItemResource resource = ItemResource.of(action.stack());
            int max = resource.getMaxStackSize();
            if (action.toInventory()) {
                ItemStack taken = network.extract(resource, max);
                if (!player.getInventory().add(taken) && !taken.isEmpty()) {
                    ItemStack back = network.insert(taken);
                    if (!back.isEmpty()) player.drop(back, false);
                }
            } else if (carried.isEmpty()) {
                setCarried(network.extract(resource, action.single() ? Math.max(1, max / 2) : max));
            } else if (ItemResource.of(carried).equals(resource) && carried.getCount() < max) {
                ItemStack more = network.extract(resource, action.single() ? 1 : max - carried.getCount());
                carried.grow(more.getCount());
            }
        }
        broadcastChanges();
        timer = 0;
        sendList();
    }
}
