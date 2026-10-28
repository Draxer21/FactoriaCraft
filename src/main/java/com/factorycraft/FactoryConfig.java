package com.factorycraft;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server options (world/serverconfig/factorycraft-server.toml). */
public final class FactoryConfig {
    private FactoryConfig() {}

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static { BUILDER.push("machines"); }
    private static final ModConfigSpec.DoubleValue MACHINE_SPEED = BUILDER
            .comment("Speed of the processing machines (crusher, washer, press, furnace...). 2.0 = twice as fast.")
            .defineInRange("machineSpeed", 1.0, 0.1, 10.0);
    private static final ModConfigSpec.DoubleValue MACHINE_ENERGY = BUILDER
            .comment("Energy used by powered machines. 0.5 = half.")
            .defineInRange("machineEnergyUse", 1.0, 0.0, 10.0);
    static { BUILDER.pop().push("vehicles"); }
    private static final ModConfigSpec.DoubleValue VEHICLE_FUEL = BUILDER
            .comment("Fuel burned by the vehicles. 0.5 = half, 0 = they never need fuel.")
            .defineInRange("vehicleFuelUse", 1.0, 0.0, 10.0);
    static { BUILDER.pop().push("nuclear"); }
    private static final ModConfigSpec.BooleanValue MELTDOWN_BREAKS_BLOCKS = BUILDER
            .comment("If true, a reactor meltdown destroys blocks. If false it still hurts and leaves fallout, but the terrain is safe.")
            .define("reactorMeltdownBreaksBlocks", true);
    private static final ModConfigSpec.BooleanValue WEAPONS_BREAK_BLOCKS = BUILDER
            .comment("If true, mini-nukes destroy blocks.")
            .define("nuclearWeaponsBreakBlocks", true);
    private static final ModConfigSpec.DoubleValue MINI_NUKE_POWER = BUILDER
            .comment("Mini-nuke crater size multiplier (radius). 0.5 = half as wide, 2.0 = twice as wide; big craters take a few seconds to carve.")
            .defineInRange("miniNukePower", 1.0, 0.1, 3.0);
    private static final ModConfigSpec.BooleanValue RADIATION = BUILDER
            .comment("Radioactive items make players sick.")
            .define("radiation", true);
    private static final ModConfigSpec.DoubleValue RADIATION_STRENGTH = BUILDER
            .comment("Radiation dose multiplier. 0.5 = half.")
            .defineInRange("radiationStrength", 1.0, 0.0, 10.0);
    static { BUILDER.pop().push("environment"); }
    private static final ModConfigSpec.BooleanValue POLLUTION = BUILDER
            .comment("Diesel generators, reactors and nuclear waste pollute the area around them.")
            .define("pollution", true);
    private static final ModConfigSpec.DoubleValue POLLUTION_STRENGTH = BUILDER
            .comment("Pollution multiplier. 0.5 = half.")
            .defineInRange("pollutionStrength", 1.0, 0.0, 10.0);
    static { BUILDER.pop().push("research"); }
    private static final ModConfigSpec.BooleanValue RESEARCH = BUILDER
            .comment("Advanced machines, vehicles and items must be researched (research station) before they can be placed or used")
            .define("researchRequired", true);
    static { BUILDER.pop(); }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean loaded() {
        return SPEC.isLoaded();
    }

    public static double machineSpeed() { return loaded() ? MACHINE_SPEED.get() : 1.0; }
    public static double machineEnergy() { return loaded() ? MACHINE_ENERGY.get() : 1.0; }
    public static double vehicleFuel() { return loaded() ? VEHICLE_FUEL.get() : 1.0; }
    public static boolean meltdownBreaksBlocks() { return !loaded() || MELTDOWN_BREAKS_BLOCKS.getAsBoolean(); }
    public static boolean weaponsBreakBlocks() { return !loaded() || WEAPONS_BREAK_BLOCKS.getAsBoolean(); }
    public static double miniNukePower() { return loaded() ? MINI_NUKE_POWER.get() : 1.0; }
    public static boolean radiation() { return !loaded() || RADIATION.getAsBoolean(); }
    public static double radiationStrength() { return loaded() ? RADIATION_STRENGTH.get() : 1.0; }
    public static boolean pollution() { return !loaded() || POLLUTION.getAsBoolean(); }
    public static boolean researchRequired() { return !loaded() || RESEARCH.getAsBoolean(); }
    public static double pollutionStrength() { return loaded() ? POLLUTION_STRENGTH.get() : 1.0; }
}
