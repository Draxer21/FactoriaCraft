package com.factorycraft;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Incinerator: destroys whatever is put in it (by hand, hopper or conveyor),
 * one item from each of its four slots every half second. Needs no power,
 * but the smoke pollutes like a diesel generator unless an air purifier or a
 * chimney filter is nearby.
 */
public final class IncineratorBlockEntity extends PoweredMachineBlockEntity {
    private static final int INPUTS = 4;
    private static final int BURN_TIME = 10;

    private int burn;

    public IncineratorBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.INCINERATOR_BLOCK_ENTITY.value(), pos, state, INPUTS, 0);
    }

    @Override protected int time() { return BURN_TIME; }
    @Override protected int energyPerTick() { return 0; }
    @Override protected MenuType<ProcessingMenu> menuType() { return FactoryCraft.INCINERATOR_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.incinerator"; }

    @Override
    public boolean supportsUpgrades() {
        return false;
    }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return slot < INPUTS;
    }

    @Override
    protected boolean work(Level level) {
        boolean loaded = false;
        for (int i = 0; i < INPUTS; i++) {
            if (!items.get(i).isEmpty()) loaded = true;
        }
        if (!loaded) {
            burn = 0;
            return false;
        }
        if (++burn < BURN_TIME) return true;
        burn = 0;
        for (int i = 0; i < INPUTS; i++) {
            if (!items.get(i).isEmpty()) items.get(i).shrink(1);
        }
        setChanged();
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.FLAME, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
                    worldPosition.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.01);
            if (server.getGameTime() % 40 < BURN_TIME) Pollution.smog(server, worldPosition);
        }
        return true;
    }

    @Override
    protected @Nullable Operation find(Level level) {
        return null;
    }

    @Override
    protected List<ItemStack> produce(Level level, Operation operation) {
        return List.of();
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
