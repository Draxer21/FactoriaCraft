package com.factorycraft;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * Shared behaviour of FactoryCraft processing machines (no GUI, conveyor-friendly):
 *  - Items come in from any side: conveyors, hoppers, or right-clicking with a stack.
 *  - Finished products go out of the FRONT face, onto a conveyor or into a container.
 *  - Right-click with an empty hand to take the finished products by hand.
 *  - Hoppers underneath can also pull the products out.
 */
public abstract class MachineBlockEntity extends BlockEntity implements WorldlyContainer {
    private static final int OUTPUT_INTERVAL = 8;
    private static final int MAX_ITEMS_ON_BELT = 3;

    protected NonNullList<ItemStack> items;

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size) {
        super(type, pos, state);
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    // ---------------------------------------------------------------- slot roles

    /** Slots that take raw material. */
    protected abstract boolean isInputSlot(int slot);

    /** Slots holding finished products. */
    protected abstract boolean isOutputSlot(int slot);

    /** Whether {@code stack} may go in input/fuel {@code slot}. */
    protected abstract boolean accepts(int slot, ItemStack stack);

    /** Runs the machine for one tick (server side). Returns true while it is working. */
    protected abstract boolean work(Level level);

    // ---------------------------------------------------------------- ticking

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineBlockEntity machine) {
        boolean working = machine.work(level);
        if (state.getValue(MachineBlock.LIT) != working) {
            level.setBlock(pos, state.setValue(MachineBlock.LIT, working), 3);
        }
        if (level.getGameTime() % OUTPUT_INTERVAL == 0) {
            machine.pushOutput(level, pos, state.getValue(MachineBlock.FACING));
        }
    }

    /** Sends one product stack out of the front: onto a belt or into a container. */
    private void pushOutput(Level level, BlockPos pos, Direction front) {
        int slot = firstOutput();
        if (slot < 0) return;
        BlockPos target = pos.relative(front);
        ItemStack stack = items.get(slot);

        Block targetBlock = level.getBlockState(target).getBlock();
        if (targetBlock instanceof ConveyorBlock || targetBlock instanceof SorterBlock) {
            AABB onBelt = new AABB(target).inflate(0.0, 0.5, 0.0);
            if (level.getEntitiesOfClass(ItemEntity.class, onBelt).size() >= MAX_ITEMS_ON_BELT) return;
            ItemStack out = stack.split(Math.min(16, stack.getCount()));
            ItemEntity item = new ItemEntity(level, target.getX() + 0.5, target.getY() + 0.3, target.getZ() + 0.5,
                    out, 0.0, 0.0, 0.0);
            level.addFreshEntity(item);
        } else {
            Container container = HopperBlockEntity.getContainerAt(level, target);
            if (container == null) return;
            ItemStack out = stack.split(Math.min(16, stack.getCount()));
            ItemStack rest = HopperBlockEntity.addItem(this, container, out, front.getOpposite());
            if (!rest.isEmpty()) stack.grow(rest.getCount());
        }
        setChanged();
    }

    private int firstOutput() {
        for (int i = 0; i < items.size(); i++) {
            if (isOutputSlot(i) && !items.get(i).isEmpty()) return i;
        }
        return -1;
    }

    /** Adds products to the output slots; returns false (and adds nothing) if they don't all fit. */
    protected boolean canStore(List<ItemStack> products) {
        NonNullList<ItemStack> copy = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) copy.set(i, items.get(i).copy());
        for (ItemStack product : products) {
            if (!storeInto(copy, product.copy())) return false;
        }
        return true;
    }

    protected void store(List<ItemStack> products) {
        for (ItemStack product : products) storeInto(items, product.copy());
        setChanged();
    }

    private boolean storeInto(NonNullList<ItemStack> target, ItemStack product) {
        for (int i = 0; i < target.size() && !product.isEmpty(); i++) {
            if (!isOutputSlot(i)) continue;
            ItemStack existing = target.get(i);
            if (existing.isEmpty()) {
                target.set(i, product.copy());
                product.setCount(0);
            } else if (ItemStack.isSameItemSameComponents(existing, product)) {
                int room = existing.getMaxStackSize() - existing.getCount();
                int moved = Math.min(room, product.getCount());
                existing.grow(moved);
                product.shrink(moved);
            }
        }
        return product.isEmpty();
    }

    // ---------------------------------------------------------------- hand interaction

    /** Right-click with a stack: put as much of it in as fits. Returns true if anything went in. */
    public boolean insertFromHand(ItemStack held) {
        ItemStack rest = HopperBlockEntity.addItem(null, this, held.copy(), null);
        int inserted = held.getCount() - rest.getCount();
        if (inserted <= 0) return false;
        held.shrink(inserted);
        setChanged();
        return true;
    }

    /** Right-click with an empty hand: hand over all finished products. */
    public boolean giveOutputs(Player player) {
        boolean gave = false;
        for (int i = 0; i < items.size(); i++) {
            if (!isOutputSlot(i) || items.get(i).isEmpty()) continue;
            ItemStack stack = items.get(i);
            items.set(i, ItemStack.EMPTY);
            if (!player.getInventory().add(stack)) player.drop(stack, false);
            gave = true;
        }
        if (gave) setChanged();
        return gave;
    }

    // ---------------------------------------------------------------- container

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack result = ContainerHelper.removeItem(items, slot, count);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return !isOutputSlot(slot) && accepts(slot, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        int[] slots = new int[items.size()];
        for (int i = 0; i < slots.length; i++) slots[i] = i;
        return slots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return isOutputSlot(slot);
    }

    // ---------------------------------------------------------------- saving

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }
}
