package com.factorycraft;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * FactoryCraft power: generators store energy, cables carry it, and machines
 * pull it through any connected cable network (or straight from a generator
 * touching them). Networks are rescanned every couple of seconds.
 */
public final class Energy {
    private Energy() {}

    /** Something that stores energy machines can draw from (the diesel generator). */
    public interface Source {
        /** Takes up to {@code max} energy; returns how much was taken. */
        int extract(int max);

        int stored();

        int capacity();
    }

    /** A machine that runs on power (cables connect to it). */
    public interface Consumer {}

    private static final int RESCAN_TICKS = 40;
    private static final int MAX_CABLES = 1024;

    /** True if a cable should visually connect to this block entity. */
    public static boolean connectsTo(BlockEntity be) {
        return be instanceof Source || be instanceof Consumer;
    }

    /** A machine's cached view of the generators it can reach. */
    public static final class Link {
        private final BlockEntity owner;
        private List<BlockPos> sources = List.of();
        private long nextScan;

        public Link(BlockEntity owner) {
            this.owner = owner;
        }

        /** Draws up to {@code amount} from the network; returns what it got. */
        public int draw(Level level, int amount) {
            refresh(level);
            int got = 0;
            for (BlockPos pos : sources) {
                if (got >= amount) break;
                if (level.getBlockEntity(pos) instanceof Source source) got += source.extract(amount - got);
            }
            return got;
        }

        /** Energy stored across the reachable generators (for the screen). */
        public int stored(Level level) {
            refresh(level);
            int total = 0;
            for (BlockPos pos : sources) {
                if (level.getBlockEntity(pos) instanceof Source source) total += source.stored();
            }
            return total;
        }

        public int capacity(Level level) {
            refresh(level);
            int total = 0;
            for (BlockPos pos : sources) {
                if (level.getBlockEntity(pos) instanceof Source source) total += source.capacity();
            }
            return total;
        }

        private void refresh(Level level) {
            if (level.getGameTime() < nextScan) return;
            nextScan = level.getGameTime() + RESCAN_TICKS;
            sources = scan(level, owner.getBlockPos());
        }
    }

    /** Breadth-first walk over cables from {@code start}, collecting every generator touched. */
    public static List<BlockPos> scan(Level level, BlockPos start) {
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        int cables = 0;
        while (!queue.isEmpty() && cables < MAX_CABLES) {
            BlockPos pos = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (!seen.add(next)) continue;
                if (level.getBlockState(next).getBlock() instanceof CableBlock) {
                    queue.add(next);
                    cables++;
                } else if (level.getBlockEntity(next) instanceof Source) {
                    found.add(next);
                }
            }
        }
        return found;
    }
}
