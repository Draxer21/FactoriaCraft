package com.factorycraft;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server options (world/serverconfig/factorycraft-server.toml). */
public final class NuclearConfig {
    private NuclearConfig() {}

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.BooleanValue MELTDOWN_BREAKS_BLOCKS = BUILDER
            .comment("If true, a reactor meltdown destroys blocks. If false it still hurts and leaves fallout, but the terrain is safe.")
            .define("reactorMeltdownBreaksBlocks", true);
    private static final ModConfigSpec.BooleanValue WEAPONS_BREAK_BLOCKS = BUILDER
            .comment("If true, mini-nukes destroy blocks.")
            .define("nuclearWeaponsBreakBlocks", true);
    public static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean meltdownBreaksBlocks() {
        return !SPEC.isLoaded() || MELTDOWN_BREAKS_BLOCKS.getAsBoolean();
    }

    public static boolean weaponsBreakBlocks() {
        return !SPEC.isLoaded() || WEAPONS_BREAK_BLOCKS.getAsBoolean();
    }
}
