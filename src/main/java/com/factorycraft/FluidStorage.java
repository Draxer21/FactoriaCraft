package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * Fluid tanks for our blocks, in millibuckets, speaking NeoForge's standard fluid
 * capability: pipes and machines from any mod can fill and empty them.
 * Each slot holds one fluid; a slot can be fill-only (an intake) or drain-only (an outlet).
 */
public final class FluidStorage extends SnapshotJournal<FluidStorage.Slot[]> implements ResourceHandler<FluidResource> {
    public static final int BUCKET = 1000;

    /** Implemented by block entities that keep fluids. */
    public interface Holder {
        FluidStorage fluids();
    }

    public interface Filter {
        boolean test(Fluid fluid);
    }

    static final class Slot {
        FluidResource resource = FluidResource.EMPTY;
        int amount;
        final int capacity;
        final boolean fill;
        final boolean drain;
        final Filter filter;

        Slot(int capacity, boolean fill, boolean drain, Filter filter) {
            this.capacity = capacity;
            this.fill = fill;
            this.drain = drain;
            this.filter = filter;
        }

        Slot copy() {
            Slot s = new Slot(capacity, fill, drain, filter);
            s.resource = resource;
            s.amount = amount;
            return s;
        }
    }

    private Slot[] slots = new Slot[0];
    private final Runnable changed;

    public FluidStorage(Runnable changed) {
        this.changed = changed;
    }

    /** Adds a slot; returns its index. */
    public FluidStorage slot(int capacity, boolean fill, boolean drain, Filter filter) {
        Slot[] grown = java.util.Arrays.copyOf(slots, slots.length + 1);
        grown[slots.length] = new Slot(capacity, fill, drain, filter);
        slots = grown;
        return this;
    }

    public static boolean any(Fluid fluid) {
        return true;
    }

    public static boolean water(Fluid fluid) {
        return fluid.isSame(Fluids.WATER);
    }

    // ---------------------------------------------------------------- direct access (no transaction)

    public Fluid fluid(int index) {
        return slots[index].amount > 0 ? slots[index].resource.getFluid() : Fluids.EMPTY;
    }

    public int amount(int index) {
        return slots[index].amount;
    }

    public int capacity(int index) {
        return slots[index].capacity;
    }

    public int space(int index) {
        return slots[index].capacity - slots[index].amount;
    }

    /** Adds fluid ignoring the fill-only/drain-only rules (the machine itself filling its own slot). */
    public int add(int index, Fluid fluid, int amount) {
        Slot s = slots[index];
        if (fluid == Fluids.EMPTY || amount <= 0 || !s.filter.test(fluid)) return 0;
        if (s.amount > 0 && !s.resource.getFluid().isSame(fluid)) return 0;
        int added = Math.min(amount, s.capacity - s.amount);
        if (added <= 0) return 0;
        if (s.amount == 0) s.resource = FluidResource.of(fluid.isSame(Fluids.WATER) ? Fluids.WATER : fluid.isSame(Fluids.LAVA) ? Fluids.LAVA : fluid);
        s.amount += added;
        changed.run();
        return added;
    }

    /** Takes fluid ignoring the rules; returns how much. */
    public int take(int index, int amount) {
        Slot s = slots[index];
        int taken = Math.min(amount, s.amount);
        if (taken <= 0) return 0;
        s.amount -= taken;
        if (s.amount == 0) s.resource = FluidResource.EMPTY;
        changed.run();
        return taken;
    }

    // ---------------------------------------------------------------- the standard capability

    @Override
    public int size() {
        return slots.length;
    }

    @Override
    public FluidResource getResource(int index) {
        return slots[index].amount > 0 ? slots[index].resource : FluidResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int index) {
        return slots[index].amount;
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        return slots[index].capacity;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return slots[index].filter.test(resource.getFluid());
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Slot s = slots[index];
        if (!s.fill || amount <= 0 || resource.getFluid() == Fluids.EMPTY || !s.filter.test(resource.getFluid())) return 0;
        if (s.amount > 0 && !s.resource.equals(resource)) return 0;
        int accepted = Math.min(amount, s.capacity - s.amount);
        if (accepted <= 0) return 0;
        updateSnapshots(transaction);
        s.resource = resource;
        s.amount += accepted;
        return accepted;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Slot s = slots[index];
        if (!s.drain || amount <= 0 || s.amount == 0 || !s.resource.equals(resource)) return 0;
        int taken = Math.min(amount, s.amount);
        updateSnapshots(transaction);
        s.amount -= taken;
        if (s.amount == 0) s.resource = FluidResource.EMPTY;
        return taken;
    }

    @Override
    protected Slot[] createSnapshot() {
        Slot[] copy = new Slot[slots.length];
        for (int i = 0; i < slots.length; i++) copy[i] = slots[i].copy();
        return copy;
    }

    @Override
    protected void revertToSnapshot(Slot[] snapshot) {
        slots = snapshot;
    }

    @Override
    protected void onRootCommit(Slot[] original) {
        changed.run();
    }

    // ---------------------------------------------------------------- moving fluid around

    /** Pushes up to {@code amount} from slot {@code index} into whatever fluid handler sits at {@code side}. */
    public int pushTo(Level level, BlockPos pos, Direction side, int index, int amount) {
        Slot s = slots[index];
        if (s.amount == 0) return 0;
        ResourceHandler<FluidResource> target = level.getCapability(Capabilities.Fluid.BLOCK, pos.relative(side), side.getOpposite());
        if (target == null || target == this) return 0;
        FluidResource resource = s.resource;
        try (Transaction transaction = Transaction.openRoot()) {
            int moved = target.insert(resource, Math.min(amount, s.amount), transaction);
            if (moved > 0) {
                transaction.commit();
                take(index, moved);
            }
            return moved;
        }
    }

    /** Offers {@code amount} of {@code fluid} to the handler at {@code pos}; true if it all went in. */
    public static boolean offer(Level level, BlockPos pos, @Nullable Direction side, Fluid fluid, int amount) {
        ResourceHandler<FluidResource> target = level.getCapability(Capabilities.Fluid.BLOCK, pos, side);
        if (target == null) return false;
        try (Transaction transaction = Transaction.openRoot()) {
            if (target.insert(FluidResource.of(fluid), amount, transaction) != amount) return false;
            transaction.commit();
            return true;
        }
    }

    // ---------------------------------------------------------------- buckets

    private static @Nullable Fluid bucketFluid(ItemStack stack) {
        if (stack.is(Items.WATER_BUCKET)) return Fluids.WATER;
        if (stack.is(Items.LAVA_BUCKET)) return Fluids.LAVA;
        for (Fluid fluid : BuiltInRegistries.FLUID) {
            if (fluid != Fluids.EMPTY && fluid.isSource(fluid.defaultFluidState()) && fluid.getBucket() == stack.getItem()) return fluid;
        }
        return null;
    }

    /** Empties a filled bucket into the first slot that takes it, or fills an empty one. True if something happened. */
    public boolean useBucket(Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.BUCKET)) {
            for (int i = 0; i < slots.length; i++) {
                Slot s = slots[i];
                if (s.amount < BUCKET) continue;
                Fluid fluid = s.resource.getFluid();
                ItemStack filled = new ItemStack(fluid.getBucket());
                if (filled.isEmpty() || filled.is(Items.BUCKET)) continue;
                take(i, BUCKET);
                if (!player.isCreative()) {
                    stack.shrink(1);
                    if (stack.isEmpty()) player.setItemInHand(hand, filled);
                    else if (!player.getInventory().add(filled)) player.drop(filled, false);
                }
                player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.BUCKET_FILL,
                        net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                return true;
            }
            return false;
        }
        Fluid fluid = bucketFluid(stack);
        if (fluid == null) return false;
        for (int i = 0; i < slots.length; i++) {
            Slot s = slots[i];
            if (!s.fill || space(i) < BUCKET || !s.filter.test(fluid)) continue;
            if (s.amount > 0 && !s.resource.getFluid().isSame(fluid)) continue;
            add(i, fluid, BUCKET);
            if (!player.isCreative()) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.BUCKET_EMPTY,
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- display and saving

    public static Component fluidName(Fluid fluid) {
        if (fluid == Fluids.EMPTY) return Component.translatable("fluid.factorycraft.empty");
        return fluid.getFluidType().getDescription();
    }

    public Component describe(int index) {
        return Component.translatable("message.factorycraft.tank.status", fluidName(fluid(index)), amount(index), capacity(index));
    }

    public void save(ValueOutput output) {
        for (int i = 0; i < slots.length; i++) {
            Slot s = slots[i];
            output.putString("Fluid" + i, s.amount > 0 ? BuiltInRegistries.FLUID.getKey(s.resource.getFluid()).toString() : "");
            output.putInt("FluidAmount" + i, s.amount);
        }
    }

    public void load(ValueInput input) {
        for (int i = 0; i < slots.length; i++) {
            Slot s = slots[i];
            String id = input.getStringOr("Fluid" + i, "");
            int amount = input.getIntOr("FluidAmount" + i, 0);
            Identifier key = id.isEmpty() ? null : Identifier.tryParse(id);
            Fluid fluid = key == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.getValue(key);
            if (fluid == null || fluid == Fluids.EMPTY || amount <= 0) {
                s.resource = FluidResource.EMPTY;
                s.amount = 0;
            } else {
                s.resource = FluidResource.of(fluid);
                s.amount = Math.min(amount, s.capacity);
            }
        }
    }
}
