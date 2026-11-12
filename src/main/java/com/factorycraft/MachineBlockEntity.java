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
public abstract class MachineBlockEntity extends BlockEntity implements WorldlyContainer, JadeInfo {
    private static final int OUTPUT_INTERVAL = 8;
    private static final int MAX_ITEMS_ON_BELT = 3;

    protected NonNullList<ItemStack> items;
    @org.jspecify.annotations.Nullable
    private MachineDiagnostic diagnostic;

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
        machine.diagnostic = null;
        boolean working = machine.work(level);
        if (machine instanceof Energy.Source source) ModCompat.pushToNeighbours(level, pos, source);
        if (state.getValue(MachineBlock.LIT) != working) {
            // Fresh state: work() may have changed other properties this tick (tank gauge).
            BlockState now = level.getBlockState(pos);
            if (now.hasProperty(MachineBlock.LIT)) level.setBlock(pos, now.setValue(MachineBlock.LIT, working), 3);
        }
        if (level.getGameTime() % OUTPUT_INTERVAL == 0) machine.pushOutputs(level, pos, state);
    }

    /** Products out of every face set to "output", or out of the front if none is. */
    void pushOutputs(Level level, BlockPos pos, BlockState state) {
        if (anyOutputFace()) {
            for (Direction face : Direction.values()) {
                if (side(face) == SideMode.OUTPUT) pushOutput(level, pos, face);
            }
        } else {
            pushOutput(level, pos, state.getValue(MachineBlock.FACING));
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
        if (level != null && !(this instanceof DronePortBlockEntity)) ProductionStats.record(level, products);
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

    /**
     * Saves from before the 6-in/8-out update had 1 input + 4 outputs (slots 1-4):
     * move those outputs to where the outputs live now.
     */
    protected void migrateToBigLayout(ValueInput input, int inputs) {
        if (input.getIntOr("Layout", 1) >= 2) return;
        for (int old = 4; old >= 1; old--) {
            ItemStack stack = items.get(old);
            items.set(old, ItemStack.EMPTY);
            items.set(inputs + old - 1, stack);
        }
    }

    // ---------------------------------------------------------------- upgrade modules

    /** Machine upgrade modules: right-click the machine with one to install it (wrench takes them out). */
    public enum MachineUpgrade { SPEED, EFFICIENCY, OUTPUT }

    public static final int MAX_UPGRADES = 4;
    protected final int[] upgrades = new int[MachineUpgrade.values().length];

    /** Processing machines take upgrades; generators, batteries etc. don't. */
    public boolean supportsUpgrades() {
        return false;
    }

    public int upgradeCount() {
        int total = 0;
        for (int n : upgrades) total += n;
        return total;
    }

    public int upgrades(MachineUpgrade type) {
        return upgrades[type.ordinal()];
    }

    public boolean installUpgrade(MachineUpgrade type) {
        if (!supportsUpgrades() || upgradeCount() >= MAX_UPGRADES) return false;
        upgrades[type.ordinal()]++;
        setChanged();
        return true;
    }

    /** Takes all modules out; returns them as items. */
    public java.util.List<ItemStack> removeUpgrades() {
        java.util.List<ItemStack> out = new java.util.ArrayList<>();
        for (MachineUpgrade type : MachineUpgrade.values()) {
            int n = upgrades[type.ordinal()];
            if (n > 0) out.add(new ItemStack(MachineUpgradeItem.item(type), n));
            upgrades[type.ordinal()] = 0;
        }
        setChanged();
        return out;
    }

    /** Multiplier for the time an operation takes (speed modules and the server config). */
    protected double speedFactor() {
        return Math.pow(0.7, upgrades[MachineUpgrade.SPEED.ordinal()]) / FactoryConfig.machineSpeed();
    }

    /** Multiplier for energy/fuel per tick: efficiency saves, speed costs a bit more. */
    protected double energyFactor() {
        return Math.pow(0.7, upgrades[MachineUpgrade.EFFICIENCY.ordinal()])
                * (1.0 + 0.25 * upgrades[MachineUpgrade.SPEED.ordinal()]) * FactoryConfig.machineEnergy();
    }

    protected int scaledTime(int base) {
        return Math.max(1, (int) Math.round(base * speedFactor()));
    }

    /** Output modules: each gives a 25% chance per product to come out doubled. */
    protected java.util.List<ItemStack> boost(Level level, java.util.List<ItemStack> results) {
        int output = upgrades[MachineUpgrade.OUTPUT.ordinal()];
        if (output == 0) return results;
        java.util.List<ItemStack> boosted = new java.util.ArrayList<>();
        for (ItemStack stack : results) {
            ItemStack copy = stack.copy();
            if (level.getRandom().nextFloat() < 0.25F * output) copy.setCount(Math.min(copy.getMaxStackSize(), copy.getCount() * 2));
            boosted.add(copy);
        }
        return boosted;
    }

    /** Jade overlay: installed machine upgrades. Subclasses add their own state. */
    @Override
    public void jadeInfo(java.util.List<net.minecraft.network.chat.Component> lines) {
        if (diagnostic != null) {
            lines.add(net.minecraft.network.chat.Component.translatable("jade.factorycraft.diagnostic", diagnostic.displayName()));
        }
        for (Direction face : Direction.values()) {
            if (side(face) != SideMode.NORMAL) {
                lines.add(net.minecraft.network.chat.Component.translatable("jade.factorycraft.side",
                        net.minecraft.network.chat.Component.translatable("direction.factorycraft." + face.getSerializedName()),
                        net.minecraft.network.chat.Component.translatable("side.factorycraft." + side(face).name().toLowerCase())));
            }
        }
        if (upgradeCount() == 0) return;
        net.minecraft.network.chat.MutableComponent text = net.minecraft.network.chat.Component.translatable("jade.factorycraft.upgrades");
        boolean first = true;
        for (MachineUpgrade type : MachineUpgrade.values()) {
            int n = upgrades[type.ordinal()];
            if (n == 0) continue;
            text.append(first ? " " : " · ").append(net.minecraft.network.chat.Component.translatable(
                    "upgrade.factorycraft.machine." + type.name().toLowerCase() + ".short")).append(" " + n);
            first = false;
        }
        lines.add(text);
    }

    /** Sets a transient status for the Jade overlay. Cleared before each server tick. */
    protected final void diagnose(MachineDiagnostic status) {
        diagnostic = status;
    }

    /** Whether any slot used for ingredients currently contains an item. */
    protected final boolean hasItemsInInputSlots() {
        for (int i = 0; i < items.size(); i++) {
            if (isInputSlot(i) && !items.get(i).isEmpty()) return true;
        }
        return false;
    }

    /** " [Speed 2 · Eff. 1]" after the machine's name in its screen. */
    protected net.minecraft.network.chat.Component withUpgrades(net.minecraft.network.chat.Component name) {
        if (upgradeCount() == 0) return name;
        net.minecraft.network.chat.MutableComponent text = name.copy().append(" [");
        boolean first = true;
        for (MachineUpgrade type : MachineUpgrade.values()) {
            int n = upgrades[type.ordinal()];
            if (n == 0) continue;
            if (!first) text.append(" · ");
            text.append(net.minecraft.network.chat.Component.translatable("upgrade.factorycraft.machine." + type.name().toLowerCase() + ".short"))
                    .append(" " + n);
            first = false;
        }
        return text.append("]");
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

    // ---------------------------------------------------------------- side configuration

    /** What one face of the machine does (set with sneak + right-click of the wrench). */
    public enum SideMode {
        /** Items go in and products come out (the default). */
        NORMAL,
        /** Only accepts items. */
        INPUT,
        /** Only gives products, and the machine pushes them out of this face. */
        OUTPUT,
        /** Closed. */
        OFF;

        public SideMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private final SideMode[] sides = {SideMode.NORMAL, SideMode.NORMAL, SideMode.NORMAL, SideMode.NORMAL, SideMode.NORMAL, SideMode.NORMAL};

    public SideMode side(Direction face) {
        return sides[face.get3DDataValue()];
    }

    /** Wrench: next mode for that face; returns the new mode. */
    public SideMode cycleSide(Direction face) {
        SideMode mode = side(face).next();
        sides[face.get3DDataValue()] = mode;
        setChanged();
        return mode;
    }

    private boolean anyOutputFace() {
        for (SideMode mode : sides) if (mode == SideMode.OUTPUT) return true;
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        if (side(direction) == SideMode.OFF) return new int[0];
        int[] slots = new int[items.size()];
        for (int i = 0; i < slots.length; i++) slots[i] = i;
        return slots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        if (direction != null && (side(direction) == SideMode.OUTPUT || side(direction) == SideMode.OFF)) return false;
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        if (side(direction) == SideMode.INPUT || side(direction) == SideMode.OFF) return false;
        return isOutputSlot(slot);
    }

    // ---------------------------------------------------------------- saving

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        for (MachineUpgrade type : MachineUpgrade.values()) {
            upgrades[type.ordinal()] = input.getIntOr("Upgrade" + type.name(), 0);
        }
        int[] saved = input.getIntArray("Sides").orElse(new int[0]);
        for (int i = 0; i < 6; i++) sides[i] = i < saved.length && saved[i] >= 0 && saved[i] < SideMode.values().length
                ? SideMode.values()[saved[i]] : SideMode.NORMAL;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        for (MachineUpgrade type : MachineUpgrade.values()) {
            if (upgrades[type.ordinal()] > 0) output.putInt("Upgrade" + type.name(), upgrades[type.ordinal()]);
        }
        boolean custom = false;
        int[] saved = new int[6];
        for (int i = 0; i < 6; i++) {
            saved[i] = sides[i].ordinal();
            custom |= sides[i] != SideMode.NORMAL;
        }
        if (custom) output.putIntArray("Sides", saved);
    }
}
