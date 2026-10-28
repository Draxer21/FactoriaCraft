package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Antimatter reactor: annihilates one antimatter cell into 4096 power a tick for
 * 5 minutes (over 120 million in all), with no smoke and no waste.
 */
public final class AntimatterReactorBlockEntity extends PassiveGeneratorBlockEntity {
    public static final int RATE = 4096;
    public static final int BURN = 6000;
    private int burn;

    public AntimatterReactorBlockEntity(BlockPos pos, BlockState state) {
        super(Endgame.ANTIMATTER_REACTOR_BLOCK_ENTITY.value(), pos, state, 20_000_000, RATE);
    }

    @Override protected String nameKey() { return "block.factorycraft.antimatter_reactor"; }
    @Override protected int computeRate(Level level) { return burn > 0 ? RATE : 0; }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return slot == 0 && stack.is(Endgame.ANTIMATTER_CELL.value());
    }

    @Override
    protected boolean work(Level level) {
        if (burn <= 0 && energy < capacity() - RATE && items.get(0).is(Endgame.ANTIMATTER_CELL.value())) {
            items.get(0).shrink(1);
            burn = BURN;
            setChanged();
        }
        if (burn <= 0) {
            rate = 0;
            return false;
        }
        burn--;
        rate = RATE;
        energy = Math.min(capacity(), energy + RATE);
        if (level instanceof ServerLevel server && level.getGameTime() % 10 == 0) {
            server.sendParticles(ParticleTypes.END_ROD, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5,
                    3, 0.25, 0.1, 0.25, 0.02);
        }
        setChanged();
        return true;
    }

    int burn() {
        return burn;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        burn = input.getIntOr("Burn", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Burn", burn);
    }
}
