package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Wireless charger: tops up the electric tools, weapons and gear (drills, rifle,
 * jetpack, exoskeleton...) of every player within 8 blocks, from the power network.
 */
public final class WirelessChargerBlockEntity extends MachineBlockEntity implements Energy.Consumer {
    public static final int RANGE = 8;
    /** Most energy given to each player every half second. */
    private static final int PER_PLAYER = 1_000;

    private final Energy.Link link = new Energy.Link(this);

    public WirelessChargerBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.WIRELESS_CHARGER_BLOCK_ENTITY.value(), pos, state, 0);
    }

    @Override protected boolean isInputSlot(int slot) { return false; }
    @Override protected boolean isOutputSlot(int slot) { return false; }
    @Override protected boolean accepts(int slot, ItemStack stack) { return false; }

    @Override
    protected boolean work(Level level) {
        if (level.getGameTime() % 10 != 0) return getBlockState().getValue(MachineBlock.LIT);
        boolean charged = false;
        for (Player player : level.players()) {
            if (player.distanceToSqr(worldPosition.getCenter()) > RANGE * RANGE) continue;
            int budget = PER_PLAYER;
            for (int i = 0; i < player.getInventory().getContainerSize() && budget > 0; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.isEmpty() || !ItemEnergy.canCharge(stack) || ItemEnergy.get(stack) >= ItemEnergy.capacity(stack)) continue;
                int want = Math.min(budget, ItemEnergy.capacity(stack) - ItemEnergy.get(stack));
                int got = link.draw(level, want);
                if (got <= 0) return charged;
                ItemEnergy.charge(stack, got);
                budget -= got;
                charged = true;
            }
        }
        return charged;
    }

    @Override
    public Energy.Link energyLink() {
        return link;
    }
}
