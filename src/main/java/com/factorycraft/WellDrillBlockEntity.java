package com.factorycraft;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Stationary well drill. Bores a 3x3 shaft straight down under itself, one
 * block every {@link #DIG_INTERVAL} ticks, layer by layer until bedrock or the
 * bottom of the world, and keeps everything it digs in its 27-slot store.
 * Products leave through its front like any FactoryCraft machine.
 * A redstone signal pauses it; when the shaft is finished it stops for good.
 */
public final class WellDrillBlockEntity extends MachineBlockEntity implements MenuProvider {
    private static final int DIG_INTERVAL = 4;
    private static final int RADIUS = 1;

    /** Layer currently being dug, as an offset below the drill (1 = right under it). */
    private int depth = 1;
    /** Position inside the current 3x3 layer (0..8). */
    private int cursor;
    private boolean finished;
    private int cooldown;

    public WellDrillBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.WELL_DRILL_BLOCK_ENTITY.value(), pos, state, 27);
    }

    @Override
    protected boolean isInputSlot(int slot) {
        return false;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return true;
    }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return false;
    }

    /** The title shows the drill's status and current depth. */
    @Override
    public Component getDisplayName() {
        int y = worldPosition.getY() - depth;
        if (finished) return Component.translatable("container.factorycraft.well_drill.finished", y);
        if (level != null && level.hasNeighborSignal(worldPosition)) {
            return Component.translatable("container.factorycraft.well_drill.paused", y);
        }
        return Component.translatable("container.factorycraft.well_drill.digging", y);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return ChestMenu.threeRows(id, inventory, this);
    }

    public boolean isFinished() {
        return finished;
    }

    @Override
    protected boolean work(Level level) {
        if (finished || !(level instanceof ServerLevel serverLevel)) return false;
        if (level.hasNeighborSignal(worldPosition)) return false;
        if (--cooldown > 0) return true;
        cooldown = DIG_INTERVAL;

        // Advance to the next block that actually needs digging (skipping air and fluids).
        for (int tries = 0; tries < 9 * 4; tries++) {
            int y = worldPosition.getY() - depth;
            if (y < level.getMinY()) {
                finish();
                return false;
            }
            int dx = cursor % 3 - RADIUS;
            int dz = cursor / 3 - RADIUS;
            BlockPos target = new BlockPos(worldPosition.getX() + dx, y, worldPosition.getZ() + dz);
            BlockState state = level.getBlockState(target);

            if (state.getDestroySpeed(level, target) < 0) {
                // Bedrock (or another unbreakable block) means we've hit the floor.
                finish();
                return false;
            }
            boolean diggable = !state.isAir() && state.getFluidState().isEmpty() && !state.hasBlockEntity();
            if (diggable) {
                List<ItemStack> drops = Block.getDrops(state, serverLevel, target, null);
                if (!canStore(drops)) return false; // full: wait for the output to drain
                state.spawnAfterBreak(serverLevel, target, ItemStack.EMPTY, true);
                level.destroyBlock(target, false);
                store(drops);
                advance();
                return true;
            }
            advance();
        }
        return true;
    }

    private void advance() {
        if (++cursor >= 9) {
            cursor = 0;
            depth++;
        }
        setChanged();
    }

    private void finish() {
        finished = true;
        setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        depth = input.getIntOr("Depth", 1);
        cursor = input.getIntOr("Cursor", 0);
        finished = input.getBooleanOr("Finished", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Depth", depth);
        output.putInt("Cursor", cursor);
        output.putBoolean("Finished", finished);
    }
}
