package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Steam turbine: place it touching a nuclear reactor. While the reactor runs
 * with water, the turbine turns its steam into extra power (half of the
 * reactor's output) and carries heat away, keeping the core cooler. It uses
 * more of the reactor's water. Up to 4 turbines per reactor.
 */
public final class SteamTurbineBlockEntity extends PassiveGeneratorBlockEntity {
    public SteamTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.STEAM_TURBINE_BLOCK_ENTITY.value(), pos, state, 1_000_000, 250);
    }

    @Override protected String nameKey() { return "block.factorycraft.steam_turbine"; }
    @Override protected int recomputeEvery() { return 1; }

    @Override
    protected int computeRate(Level level) {
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(dir)) instanceof NuclearReactorBlockEntity reactor) {
                int steam = reactor.takeSteam();
                if (steam > 0) return steam / 2;
            }
        }
        return 0;
    }
}
