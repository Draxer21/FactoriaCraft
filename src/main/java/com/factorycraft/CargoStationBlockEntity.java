package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Loading / unloading station. Put it beside the track (or where trucks park)
 * with a chest, barrel or industrial chest touching it.
 *  - Loading station: moves items from the chest into wagons and trucks
 *    within 3 blocks.
 *  - Unloading station: empties them into the chest. Tank wagons are emptied
 *    into a touching reactor (water), diesel generator (lava) or cauldron.
 * Up to 32 items per half second.
 */
public final class CargoStationBlockEntity extends MachineBlockEntity {
    private static final int PER_CYCLE = 32;

    public CargoStationBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.CARGO_STATION_BLOCK_ENTITY.value(), pos, state, 0);
    }

    @Override protected boolean isInputSlot(int slot) { return false; }
    @Override protected boolean isOutputSlot(int slot) { return false; }
    @Override protected boolean accepts(int slot, ItemStack stack) { return false; }

    private boolean loading() {
        return getBlockState().is(FactoryCraft.LOADING_STATION.value());
    }

    @Override
    protected boolean work(Level level) {
        if (level.getGameTime() % 10 != 0) return getBlockState().getValue(MachineBlock.LIT);
        boolean moved = false;
        AABB area = new AABB(worldPosition).inflate(3.0);
        for (Entity entity : level.getEntities((Entity) null, area,
                e -> e instanceof CargoWagonEntity || e instanceof DumpTruckEntity || e instanceof WorkVehicleEntity)) {
            if (entity instanceof TankWagonEntity tank) {
                if (!loading()) moved |= drainTank(level, tank);
                continue;
            }
            if (!(entity instanceof Container vehicle) || vehicle.getContainerSize() == 0) continue;
            for (Direction dir : Direction.values()) {
                BlockEntity be = level.getBlockEntity(worldPosition.relative(dir));
                if (!(be instanceof Container chest)) continue;
                moved |= loading() ? transfer(chest, vehicle) : transfer(vehicle, chest);
            }
        }
        return moved;
    }

    /** Moves up to PER_CYCLE items from {@code from} into {@code to}. */
    private static boolean transfer(Container from, Container to) {
        int budget = PER_CYCLE;
        boolean moved = false;
        for (int i = 0; i < from.getContainerSize() && budget > 0; i++) {
            ItemStack stack = from.getItem(i);
            if (stack.isEmpty()) continue;
            int count = Math.min(budget, stack.getCount());
            ItemStack offer = stack.copyWithCount(count);
            ItemStack rest = HopperBlockEntity.addItem(from, to, offer, null);
            int done = count - rest.getCount();
            if (done > 0) {
                stack.shrink(done);
                from.setChanged();
                to.setChanged();
                budget -= done;
                moved = true;
            }
        }
        return moved;
    }

    private boolean drainTank(Level level, TankWagonEntity tank) {
        if (tank.amount() <= 0) return false;
        TankerEntity.Fluid fluid = tank.fluid();
        for (Direction dir : Direction.values()) {
            BlockPos target = worldPosition.relative(dir);
            BlockEntity be = level.getBlockEntity(target);
            boolean took = false;
            if (fluid == TankerEntity.Fluid.WATER && be instanceof NuclearReactorBlockEntity reactor) took = reactor.acceptWater();
            else if (fluid == TankerEntity.Fluid.LAVA && be instanceof GeneratorBlockEntity generator) took = generator.acceptLava();
            else if (level.getBlockState(target).is(Blocks.CAULDRON)) {
                level.setBlock(target, fluid == TankerEntity.Fluid.LAVA ? Blocks.LAVA_CAULDRON.defaultBlockState()
                        : Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3), 3);
                took = true;
            }
            if (took) {
                tank.drainOne();
                return true;
            }
        }
        return false;
    }
}
