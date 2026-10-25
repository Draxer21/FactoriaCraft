package com.factorycraft;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Keeps a per-dimension list of loaded route beacons so vehicles can find them quickly. */
public final class RouteBeaconBlockEntity extends BlockEntity {
    private static final Map<ResourceKey<Level>, Set<BlockPos>> BEACONS = new ConcurrentHashMap<>();

    public RouteBeaconBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.ROUTE_BEACON_BLOCK_ENTITY.value(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            BEACONS.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(worldPosition.immutable());
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null) {
            Set<BlockPos> set = BEACONS.get(level.dimension());
            if (set != null) set.remove(worldPosition);
        }
    }

    /** Nearest beacon with this number (or any number if {@code index} &lt;= 0) within {@code range}. */
    public static @Nullable BlockPos find(Level level, BlockPos from, int index, double range) {
        Set<BlockPos> set = BEACONS.get(level.dimension());
        if (set == null) return null;
        BlockPos best = null;
        double bestDist = range * range;
        for (BlockPos pos : set) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof RouteBeaconBlock)) continue;
            if (index > 0 && state.getValue(RouteBeaconBlock.INDEX) != index) continue;
            double d = pos.distSqr(from);
            if (d < bestDist) {
                bestDist = d;
                best = pos;
            }
        }
        return best;
    }
}
