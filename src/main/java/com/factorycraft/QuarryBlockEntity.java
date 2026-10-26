package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Quarry: digs out its area layer by layer, from just under itself down to
 * bedrock, and keeps everything it digs (ores included) in its 8 output slots,
 * sent out the front onto a belt or into a chest. Fluids and containers are
 * left in place.
 */
public final class QuarryBlockEntity extends AreaMachineBlockEntity {
    public QuarryBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.QUARRY_BLOCK_ENTITY.value(), pos, state, 0, ProcessingMenu.BIG_OUTPUTS, new int[] {2, 4, 8, 16}, 4);
    }

    @Override protected int time() { return 3; }
    @Override protected int energyPerTick() { return energyPerBlock(); }
    @Override protected int energyPerBlock() { return 40; }
    @Override protected MenuType<ProcessingMenu> menuType() { return FactoryCraft.QUARRY_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.quarry"; }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return false;
    }

    /** cursor = layer * columns + column, layers counted down from just under the machine. */
    @Override
    protected @Nullable BlockPos findNext(ServerLevel level) {
        long columns = (long) side() * side();
        int topY = worldPosition.getY() - 1;
        int bottomY = level.getMinY();
        long total = columns * (topY - bottomY + 1);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int checked = 0; checked < 2048 && cursor < total; checked++) {
            int y = topY - (int) (cursor / columns);
            long column = cursor % columns;
            pos.set(columnX(column), y, columnZ(column));
            if (level.isLoaded(pos)) {
                BlockState state = level.getBlockState(pos);
                if (isMineable(level, pos, state)) return pos.immutable();
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
