package com.factorycraft;

import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;

/**
 * Upgrade modules for the machines. Right-click a machine with a module to
 * install it (up to {@link VehicleEngine#MAX_UPGRADES}); right-click with the
 * wrench to take them all back out.
 */
public enum Upgrade {
    /** +35% top speed, burns 50% more fuel. */
    TURBO("turbo", e -> true),
    /** Burns 40% less fuel. */
    EFFICIENT_ENGINE("efficient_engine", e -> true),
    /** Electric idling (no fuel while stopped) and 25% less fuel while driving. */
    HYBRID_ENGINE("hybrid_engine", e -> true),
    /** Charges the tank in daylight under open sky, even with the engine off. */
    SOLAR_PANEL("solar_panel", e -> true),
    /** Doubles the fuel tank. */
    BIG_TANK("big_tank", e -> true),
    /** Wider (and taller) work area for digging and road/field machines. */
    WIDE_DRILL("wide_drill", e -> e instanceof ExcavatorEntity || e instanceof TunnelerEntity
            || e instanceof PaverEntity || e instanceof HarvesterEntity || e instanceof BulldozerEntity
            || e instanceof TankerEntity || e instanceof ForesterEntity),
    /** Mines and harvests as if with Fortune III. */
    FORTUNE("fortune", e -> e instanceof ExcavatorEntity || e instanceof TunnelerEntity || e instanceof HarvesterEntity
            || e instanceof ForesterEntity),
    /** Throws away stone, dirt, gravel and the like; keeps ores and everything else. */
    ORE_FILTER("ore_filter", e -> e instanceof ExcavatorEntity || e instanceof TunnelerEntity),
    /** Pulls in loose items from much further away. */
    MAGNET("magnet", e -> e instanceof DumpTruckEntity),

    // ---- Mechanic-only modules (bought from the Mechanic villager, no recipe).
    /** +70% top speed, burns twice the fuel. */
    TURBO_II("turbo_ii", e -> true),
    /** Mines and harvests as if with Fortune IV. */
    FORTUNE_II("fortune_ii", e -> FORTUNE.fits(e)),
    /** Never needs fuel. Takes two module slots. */
    FUSION_ENGINE("fusion_engine", e -> true, 2);

    private final String id;
    private final Predicate<Entity> fits;
    private final int slots;

    Upgrade(String id, Predicate<Entity> fits) {
        this(id, fits, 1);
    }

    Upgrade(String id, Predicate<Entity> fits, int slots) {
        this.id = id;
        this.fits = fits;
        this.slots = slots;
    }

    /** Module slots it takes in the machine. */
    public int slots() {
        return slots;
    }

    public String id() {
        return id;
    }

    public boolean fits(Entity vehicle) {
        return fits.test(vehicle);
    }

    public int bit() {
        return 1 << ordinal();
    }

    public String translationKey() {
        return "upgrade.factorycraft." + id;
    }
}
