package com.factorycraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Factory production statistics: every item a machine produces is counted per
 * minute of game time for the last hour (server-wide, kept while the server runs).
 * The statistics terminal shows them as charts.
 */
public final class ProductionStats {
    public static final int MINUTES = 60;
    public static final int SHOWN = 30;
    private static final Map<Item, long[]> COUNTS = new HashMap<>();
    private static final Map<Item, long[]> STAMPS = new HashMap<>();

    private ProductionStats() {}

    static long minute(Level level) {
        return level.getGameTime() / 1200L;
    }

    public static synchronized void record(Level level, List<ItemStack> products) {
        if (level.isClientSide()) return;
        long now = minute(level);
        int slot = (int) (now % MINUTES);
        for (ItemStack stack : products) {
            if (stack.isEmpty()) continue;
            long[] counts = COUNTS.computeIfAbsent(stack.getItem(), k -> new long[MINUTES]);
            long[] stamps = STAMPS.computeIfAbsent(stack.getItem(), k -> new long[MINUTES]);
            if (stamps[slot] != now) {
                stamps[slot] = now;
                counts[slot] = 0;
            }
            counts[slot] += stack.getCount();
        }
    }

    /** Counts for the last {@link #SHOWN} minutes, oldest first. */
    static synchronized int[] series(Item item, long now) {
        int[] out = new int[SHOWN];
        long[] counts = COUNTS.get(item);
        long[] stamps = STAMPS.get(item);
        if (counts == null) return out;
        for (int i = 0; i < SHOWN; i++) {
            long m = now - (SHOWN - 1 - i);
            int slot = (int) (Math.floorMod(m, MINUTES));
            if (stamps[slot] == m) out[i] = (int) Math.min(Integer.MAX_VALUE, counts[slot]);
        }
        return out;
    }

    /** The items made most in the last half hour, with their per-minute series. */
    public static synchronized StatsPayload snapshot(Level level, int top) {
        long now = minute(level);
        List<StatsPayload.Line> lines = new ArrayList<>();
        int[] total = new int[SHOWN];
        for (Item item : COUNTS.keySet()) {
            int[] series = series(item, now);
            long sum = 0;
            for (int i = 0; i < SHOWN; i++) {
                sum += series[i];
                total[i] += series[i];
            }
            if (sum > 0) lines.add(new StatsPayload.Line(new ItemStack(item), series, sum));
        }
        lines.sort((a, b) -> Long.compare(b.sum(), a.sum()));
        if (lines.size() > top) lines = new ArrayList<>(lines.subList(0, top));
        return new StatsPayload(total, lines);
    }
}
