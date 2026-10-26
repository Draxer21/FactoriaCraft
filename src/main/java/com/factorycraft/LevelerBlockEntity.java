package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Leveller: cuts away everything that sticks out above the level it stands on
 * (hills, trees, buildings without containers), top down, and keeps what it
 * cuts in its 8 output slots (sent out the front onto a belt or into a chest).
 * Together with the terrain filler it turns any land into a flat plain.
 */
public final class LevelerBlockEntity extends AreaMachineBlockEntity {
    /** How high above its level it clears. */
    private static final int MAX_HEIGHT = 64;

    public LevelerBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.LEVELER_BLOCK_ENTITY.value(), pos, state, 0, ProcessingMenu.BIG_OUTPUTS, new int[] {4, 8, 16, 24}, 8);
    }

    @Override protected int time() { return 2; }
    @Override protected int energyPerTick() { return energyPerBlock(); }
    @Override protected int energyPerBlock() { return 30; }
    @Override protected MenuType<ProcessingMenu> menuType() { return FactoryCraft.LEVELER_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.leveler"; }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return false;
    }

    @Override
    protected @Nullable BlockPos findNext(ServerLevel level) {
        long total = (long) side() * side();
        int bottom = worldPosition.getY();
        int top = Math.min(level.getMaxY() - 1, bottom + MAX_HEIGHT);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int checked = 0; checked < 48 && cursor < total; checked++) {
            int x = columnX(cursor);
            int z = columnZ(cursor);
            pos.set(x, bottom, z);
            if (level.isLoaded(pos)) {
                // Highest block of the column first, so nothing is left floating.
                for (int y = top; y >= bottom; y--) {
                    pos.setY(y);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) continue;
                    if (isMineable(level, pos, state)) return pos.immutable();
                }
            }
            cursor++;
        }
        return cursor >= total ? null : NOT_YET;
    }

    @Override
    protected boolean workAt(ServerLevel level, BlockPos pos) {
        return breakIntoOutputs(level, pos);
    }
}
