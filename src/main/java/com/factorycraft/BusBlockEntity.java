package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * Moves items between the inventory in front and the storage network, every
 * half second. Import bus: up to 16 items from the inventory into the network
 * (only the filtered items, or everything with an empty filter). Export bus:
 * keeps putting the filtered items from the network into the inventory.
 */
public final class BusBlockEntity extends BlockEntity implements MenuProvider {
    public static final int FILTER = 9;
    public static final int PER_MOVE = 16;
    public static final int INTERVAL = 10;

    private final SimpleContainer filter = new SimpleContainer(FILTER) {
        @Override
        public void setChanged() {
            super.setChanged();
            BusBlockEntity.this.setChanged();
        }
    };
    /** Items moved since the block was loaded (self-test / Jade). */
    long moved;

    public BusBlockEntity(BlockPos pos, BlockState state) {
        super(Automation.BUS_BLOCK_ENTITY.value(), pos, state);
    }

    public SimpleContainer filter() {
        return filter;
    }

    public boolean export() {
        return getBlockState().getBlock() instanceof BusBlock bus && bus.export();
    }

    public void setFilter(int slot, ItemStack stack) {
        if (slot < 0 || slot >= FILTER) return;
        filter.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
    }

    private boolean filterEmpty() {
        return filter.isEmpty();
    }

    private boolean passes(ItemResource resource) {
        if (filterEmpty()) return true;
        for (int i = 0; i < FILTER; i++) {
            ItemStack f = filter.getItem(i);
            if (!f.isEmpty() && resource.toStack(1).is(f.getItem())) return true;
        }
        return false;
    }

    void tick(Level level) {
        if (level.getGameTime() % INTERVAL != Math.floorMod(worldPosition.asLong(), INTERVAL)) return;
        move(level);
    }

    /** One transfer; returns how many items moved. */
    int move(Level level) {
        Direction facing = getBlockState().getValue(BusBlock.FACING);
        BlockPos targetPos = worldPosition.relative(facing);
        ResourceHandler<ItemResource> target = level.getCapability(Capabilities.Item.BLOCK, targetPos, facing.getOpposite());
        if (target == null) return 0;
        StorageNetwork network = network(level);
        if (network == null) return 0;
        network = network.without(targetPos);
        int done = export() ? exportTo(network, target) : importFrom(network, target);
        moved += done;
        return done;
    }

    private @Nullable StorageNetwork network(Level level) {
        Direction facing = getBlockState().getValue(BusBlock.FACING);
        for (Direction dir : Direction.values()) {
            if (dir == facing) continue;
            BlockPos next = worldPosition.relative(dir);
            if (level.getBlockState(next).getBlock() instanceof DataCableBlock
                    || level.getBlockEntity(next) instanceof StorageTerminalBlockEntity) {
                return StorageNetwork.scan(level, next);
            }
        }
        return null;
    }

    private int importFrom(StorageNetwork network, ResourceHandler<ItemResource> target) {
        int budget = PER_MOVE;
        for (int i = 0; i < target.size() && budget > 0; i++) {
            ItemResource resource = target.getResource(i);
            if (resource.isEmpty() || !passes(resource)) continue;
            int got;
            try (Transaction transaction = Transaction.openRoot()) {
                got = target.extract(resource, budget, transaction);
                if (got > 0) transaction.commit();
            }
            if (got <= 0) continue;
            ItemStack left = network.insert(resource.toStack(got));
            if (!left.isEmpty()) {
                try (Transaction transaction = Transaction.openRoot()) {
                    target.insert(resource, left.getCount(), transaction);
                    transaction.commit();
                }
            }
            int stored = got - left.getCount();
            budget -= stored;
            if (stored == 0) break;
        }
        return PER_MOVE - budget;
    }

    private int exportTo(StorageNetwork network, ResourceHandler<ItemResource> target) {
        if (filterEmpty()) return 0;
        int budget = PER_MOVE;
        for (int i = 0; i < FILTER && budget > 0; i++) {
            ItemStack f = filter.getItem(i);
            if (f.isEmpty()) continue;
            ItemResource resource = ItemResource.of(f);
            // How much the inventory takes, before taking anything out of the network.
            int room;
            try (Transaction transaction = Transaction.openRoot()) {
                room = target.insert(resource, budget, transaction);
            }
            if (room <= 0) continue;
            ItemStack got = network.extract(resource, room);
            if (got.isEmpty()) continue;
            int put;
            try (Transaction transaction = Transaction.openRoot()) {
                put = target.insert(resource, got.getCount(), transaction);
                transaction.commit();
            }
            if (put < got.getCount()) network.insert(resource.toStack(got.getCount() - put));
            budget -= put;
        }
        return PER_MOVE - budget;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BusMenu(id, inventory, this, filter);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        NonNullList<ItemStack> saved = NonNullList.withSize(FILTER, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input.childOrEmpty("Filter"), saved);
        for (int i = 0; i < FILTER; i++) filter.getItems().set(i, saved.get(i));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        NonNullList<ItemStack> saved = NonNullList.withSize(FILTER, ItemStack.EMPTY);
        for (int i = 0; i < FILTER; i++) saved.set(i, filter.getItem(i));
        ContainerHelper.saveAllItems(output.child("Filter"), saved);
    }
}
