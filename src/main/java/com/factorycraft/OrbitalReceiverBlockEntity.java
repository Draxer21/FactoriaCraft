package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Orbital receiver: catches the power beamed down by solar satellites, 256 a tick
 * for each one in orbit (up to 4 per receiver), day and night, rain or shine.
 * Needs open sky above.
 */
public final class OrbitalReceiverBlockEntity extends PassiveGeneratorBlockEntity {
    public static final int PER_SATELLITE = 256;
    public static final int MAX_SATELLITES = 4;

    public OrbitalReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(Endgame.ORBITAL_RECEIVER_BLOCK_ENTITY.value(), pos, state, 4_000_000, PER_SATELLITE * MAX_SATELLITES);
    }

    @Override protected String nameKey() { return "block.factorycraft.orbital_receiver"; }

    @Override
    protected int computeRate(Level level) {
        if (!level.canSeeSky(worldPosition.above())) return 0;
        return Math.min(MAX_SATELLITES, Orbit.get(level).solar()) * PER_SATELLITE;
    }
}
