package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * Talking to other mods through NeoForge's standard capabilities:
 * - items: pipes, conveyors and hoppers from any mod can fill and empty our machines and vehicles;
 * - energy (FE): other mods' generators can power our machines, and they can draw from our
 *   generators, reactor and batteries; our generators also push power into neighbouring
 *   machines from other mods. 1 FactoryCraft energy = 1 FE.
 */
@EventBusSubscriber(modid = FactoryCraft.MOD_ID)
public final class ModCompat {
    /** Most FE a generator pushes into one neighbour per tick. */
    private static final int PUSH_PER_TICK = 4_000;

    private ModCompat() {}

    @SubscribeEvent
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        for (DeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<?>> holder : FactoryCraft.BLOCK_ENTITY_TYPES.getEntries()) {
            BlockEntityType type = holder.value();
            event.registerBlockEntity(Capabilities.Item.BLOCK, type, (be, side) -> items((BlockEntity) be, (Direction) side));
            event.registerBlockEntity(Capabilities.Energy.BLOCK, type, (be, side) -> energy((BlockEntity) be));
        }
        // Other mods' pipes can keep the reactor topped up with water.
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, Nuclear.REACTOR_BLOCK_ENTITY.value(), (reactor, side) -> new ReactorWater(reactor));
        for (DeferredHolder<EntityType<?>, ? extends EntityType<?>> holder : FactoryCraft.ENTITY_TYPES.getEntries()) {
            EntityType type = holder.value();
            event.registerEntity(Capabilities.Item.ENTITY, type,
                    (entity, nothing) -> entity instanceof Container container ? VanillaContainerWrapper.of(container) : null);
            event.registerEntity(Capabilities.Item.ENTITY_AUTOMATION, type,
                    (entity, side) -> entity instanceof Container container ? VanillaContainerWrapper.of(container) : null);
        }
    }

    private static @Nullable ResourceHandler<ItemResource> items(BlockEntity be, @Nullable Direction side) {
        if (be instanceof WorldlyContainer worldly && side != null) return new WorldlyContainerWrapper(worldly, side);
        if (be instanceof Container container) return VanillaContainerWrapper.of(container);
        return null;
    }

    private static @Nullable EnergyHandler energy(BlockEntity be) {
        if (be instanceof Energy.Source source) return new SourceHandler(source);
        if (be instanceof PoweredMachineBlockEntity machine) return machine.feInput();
        return null;
    }

    // ---------------------------------------------------------------- pushing power out

    /** Called every tick for our generators: feed neighbouring machines from other mods. */
    static void pushToNeighbours(Level level, BlockPos pos, Energy.Source source) {
        if (source.isStorage() || source.stored() <= 0) return;
        for (Direction direction : Direction.values()) {
            BlockPos next = pos.relative(direction);
            BlockEntity neighbour = level.getBlockEntity(next);
            // Our own machines and cables already draw through the network.
            if (neighbour == null || Energy.connectsTo(neighbour)) continue;
            EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, next, direction.getOpposite());
            if (handler == null) continue;
            int offer = Math.min(source.stored(), PUSH_PER_TICK);
            if (offer <= 0) return;
            try (Transaction transaction = Transaction.openRoot()) {
                int accepted = handler.insert(offer, transaction);
                if (accepted > 0 && source.extract(accepted) == accepted) transaction.commit();
            }
        }
    }

    // ---------------------------------------------------------------- our sources as FE

    /**
     * One of our generators, the reactor or a battery seen as an FE handler. Other mods can
     * draw from all of them; only batteries also accept power.
     */
    private static final class SourceHandler extends SnapshotJournal<Integer> implements EnergyHandler {
        private final Energy.Source source;

        SourceHandler(Energy.Source source) {
            this.source = source;
        }

        @Override
        public long getAmountAsLong() {
            return source.stored();
        }

        @Override
        public long getCapacityAsLong() {
            return source.capacity();
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            if (!source.isStorage() || amount <= 0) return 0;
            int accepted = Math.min(amount, source.capacity() - source.stored());
            if (accepted <= 0) return 0;
            updateSnapshots(transaction);
            source.setStored(source.stored() + accepted);
            return accepted;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            int taken = Math.min(amount, source.stored());
            if (taken <= 0) return 0;
            updateSnapshots(transaction);
            source.setStored(source.stored() - taken);
            return taken;
        }

        @Override
        protected Integer createSnapshot() {
            return source.stored();
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            source.setStored(snapshot);
        }
    }

    // ---------------------------------------------------------------- reactor water

    /** The reactor's cooling water as a fluid tank that only takes water. */
    private static final class ReactorWater extends SnapshotJournal<Integer>
            implements ResourceHandler<net.neoforged.neoforge.transfer.fluid.FluidResource> {
        private final NuclearReactorBlockEntity reactor;

        ReactorWater(NuclearReactorBlockEntity reactor) {
            this.reactor = reactor;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public net.neoforged.neoforge.transfer.fluid.FluidResource getResource(int index) {
            return reactor.water() > 0
                    ? net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER)
                    : net.neoforged.neoforge.transfer.fluid.FluidResource.EMPTY;
        }

        @Override
        public long getAmountAsLong(int index) {
            return reactor.water();
        }

        @Override
        public long getCapacityAsLong(int index, net.neoforged.neoforge.transfer.fluid.FluidResource resource) {
            return NuclearReactorBlockEntity.WATER_CAPACITY;
        }

        @Override
        public boolean isValid(int index, net.neoforged.neoforge.transfer.fluid.FluidResource resource) {
            return resource.getFluid() == net.minecraft.world.level.material.Fluids.WATER;
        }

        @Override
        public int insert(int index, net.neoforged.neoforge.transfer.fluid.FluidResource resource, int amount, TransactionContext transaction) {
            if (!isValid(index, resource) || amount <= 0) return 0;
            int accepted = Math.min(amount, NuclearReactorBlockEntity.WATER_CAPACITY - reactor.water());
            if (accepted <= 0) return 0;
            updateSnapshots(transaction);
            reactor.setWater(reactor.water() + accepted);
            return accepted;
        }

        @Override
        public int extract(int index, net.neoforged.neoforge.transfer.fluid.FluidResource resource, int amount, TransactionContext transaction) {
            return 0; // cooling water stays in
        }

        @Override
        protected Integer createSnapshot() {
            return reactor.water();
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            reactor.setWater(snapshot);
        }
    }
}
