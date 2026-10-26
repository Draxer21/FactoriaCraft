package com.factorycraft;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A powered machine that works a square area around itself, one block at a time
 * (leveller, quarry, drain, tree planter). Sneak + right-click with an empty hand
 * cycles the area size; the border shows with particles. When the job is done it
 * waits and re-checks the area every 10 seconds.
 */
public abstract class AreaMachineBlockEntity extends PoweredMachineBlockEntity implements AreaMachine {
    private final int[] radii;
    protected int radius;
    /** Where the scan is: meaning depends on the machine. */
    protected long cursor;
    private boolean done;
    private int timer;

    protected AreaMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int inputs, int outputs,
                                     int[] radii, int defaultRadius) {
        super(type, pos, state, inputs, outputs);
        this.radii = radii;
        this.radius = defaultRadius;
    }

    // ---------------------------------------------------------------- per machine

    /** Energy for each block worked. */
    protected abstract int energyPerBlock();

    /**
     * Finds the next block to work, scanning from {@link #cursor} (at most a few thousand
     * checks per call). Returns {@link #NOT_YET} if it ran out of budget, null when finished.
     */
    protected abstract @Nullable BlockPos findNext(ServerLevel level);

    /** Works {@code pos} (already powered). Returns false if it couldn't (missing material, full output). */
    protected abstract boolean workAt(ServerLevel level, BlockPos pos);

    /** Blocks worked per tick at full speed. */
    protected int perTick() {
        return 1;
    }

    protected static final BlockPos NOT_YET = new BlockPos(0, Integer.MIN_VALUE, 0);

    // ---------------------------------------------------------------- area

    @Override
    public void cycleRadius(Player player) {
        int next = radii[0];
        for (int r : radii) {
            if (r > radius) {
                next = r;
                break;
            }
        }
        radius = next;
        restart();
        setChanged();
        int side = radius * 2 + 1;
        player.sendOverlayMessage(Component.translatable("message.factorycraft.area", side, side));
        if (level instanceof ServerLevel server) outline(server);
    }

    protected void restart() {
        cursor = 0;
        done = false;
    }

    private void outline(ServerLevel level) {
        int y = worldPosition.getY();
        for (int i = -radius; i <= radius; i++) {
            for (int[] p : new int[][] {{i, -radius}, {i, radius}, {-radius, i}, {radius, i}}) {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, worldPosition.getX() + p[0] + 0.5, y + 1.2,
                        worldPosition.getZ() + p[1] + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    /** Columns in the area, row by row: index -> world x/z. */
    protected int side() {
        return radius * 2 + 1;
    }

    protected int columnX(long column) {
        return worldPosition.getX() + (int) (column % side()) - radius;
    }

    protected int columnZ(long column) {
        return worldPosition.getZ() + (int) (column / side()) - radius;
    }

    // ---------------------------------------------------------------- ticking

    @Override
    protected boolean work(Level level) {
        if (!(level instanceof ServerLevel server)) return false;
        if (done) {
            if (level.getGameTime() % 200 != 0) return false;
            restart();
        }
        if (++timer < scaledTime(time())) return true;
        timer = 0;
        boolean worked = false;
        for (int n = 0; n < perTick(); n++) {
            BlockPos target = findNext(server);
            if (target == null) {
                done = true;
                return worked;
            }
            if (target == NOT_YET) return true;
            if (!drawPower(level, energyPerBlock())) return worked;
            if (!workAt(server, target)) return worked;
            worked = true;
            setChanged();
        }
        return true;
    }

    // ---------------------------------------------------------------- helpers

    /** Breaks {@code pos} into the output slots; false (nothing broken) if they can't take the drops. */
    protected boolean breakIntoOutputs(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, level, pos, level.getBlockEntity(pos)));
        if (!canStore(drops)) return false;
        level.destroyBlock(pos, false);
        store(drops);
        return true;
    }

    /** Whether a machine may break it: solid-ish, breakable, not a container, not us. */
    protected boolean isMineable(ServerLevel level, BlockPos pos, BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && !state.hasBlockEntity()
                && state.getDestroySpeed(level, pos) >= 0 && !pos.equals(worldPosition);
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
        radius = input.getIntOr("Radius", radius);
        cursor = input.getLongOr("Cursor", 0L);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Radius", radius);
        output.putLong("Cursor", cursor);
    }
}
