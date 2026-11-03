package com.factorycraft;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Factory production statistics: every item a machine produces is counted per
 * minute of game time for the last hour. Saved with the world (overworld
 * attachment) and reset when another world is loaded.
 * The statistics terminal shows them as charts.
 */
@EventBusSubscriber(modid = FactoryCraft.MOD_ID)
public final class ProductionStats {
    public static final int MINUTES = 60;
    public static final int SHOWN = 30;
    private static final Map<Item, long[]> COUNTS = new HashMap<>();
    private static final Map<Item, long[]> STAMPS = new HashMap<>();

    record Entry(Item item, List<Long> counts, List<Long> stamps) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Entry::item),
                Codec.LONG.listOf().fieldOf("counts").forGetter(Entry::counts),
                Codec.LONG.listOf().fieldOf("stamps").forGetter(Entry::stamps)).apply(i, Entry::new));
    }

    static final Codec<List<Entry>> CODEC = Entry.CODEC.listOf();
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<List<Entry>>> SAVED = AirQuality.ATTACHMENTS.register(
            "production_stats", () -> AttachmentType.<List<Entry>>builder(() -> List.of()).serialize(CODEC.fieldOf("entries")).build());

    private ProductionStats() {}

    /** Loads the class so its attachment type is registered in time. */
    static void init() {}

    /** A new world: forget the previous one's numbers and load this one's. */
    @SubscribeEvent
    public static synchronized void onStarted(ServerStartedEvent event) {
        COUNTS.clear();
        STAMPS.clear();
        for (Entry entry : event.getServer().overworld().getData(SAVED)) {
            if (entry.counts().size() != MINUTES || entry.stamps().size() != MINUTES) continue;
            COUNTS.put(entry.item(), entry.counts().stream().mapToLong(Long::longValue).toArray());
            STAMPS.put(entry.item(), entry.stamps().stream().mapToLong(Long::longValue).toArray());
        }
    }

    @SubscribeEvent
    public static void onSave(LevelEvent.Save event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level && level.dimension() == Level.OVERWORLD) store(level);
    }

    @SubscribeEvent
    public static synchronized void onStopping(ServerStoppingEvent event) {
        store(event.getServer().overworld());
    }

    private static synchronized void store(net.minecraft.server.level.ServerLevel level) {
        List<Entry> entries = new ArrayList<>();
        for (Map.Entry<Item, long[]> e : COUNTS.entrySet()) {
            long[] stamps = STAMPS.get(e.getKey());
            if (stamps == null) continue;
            entries.add(new Entry(e.getKey(), java.util.Arrays.stream(e.getValue()).boxed().toList(),
                    java.util.Arrays.stream(stamps).boxed().toList()));
        }
        level.setData(SAVED, entries);
    }

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
