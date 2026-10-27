package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** Cable car station: one end of a cable line. The line's cabin belongs to the station it was strung from. */
public final class CableStationBlockEntity extends MachineBlockEntity {
    public static final int MAX_LENGTH = 160;
    private @Nullable BlockPos other;
    private boolean master;

    public CableStationBlockEntity(BlockPos pos, BlockState state) {
        super(Logistics.CABLE_STATION_BLOCK_ENTITY.value(), pos, state, 0);
    }

    @Override protected boolean isInputSlot(int slot) { return false; }
    @Override protected boolean isOutputSlot(int slot) { return false; }
    @Override protected boolean accepts(int slot, ItemStack stack) { return false; }

    public @Nullable BlockPos other() {
        return other;
    }

    public boolean isMaster() {
        return master;
    }

    public boolean linked() {
        return other != null;
    }

    void link(BlockPos to, boolean master) {
        this.other = to.immutable();
        this.master = master;
        setChanged();
    }

    void unlink() {
        other = null;
        master = false;
        setChanged();
    }

    /** True if this station and {@code pos} are the two ends of one line. */
    static boolean pairOk(Level level, BlockPos a, BlockPos b) {
        return level.getBlockEntity(a) instanceof CableStationBlockEntity sa && b.equals(sa.other)
                && level.getBlockEntity(b) instanceof CableStationBlockEntity sb && a.equals(sb.other);
    }

    @Override
    protected boolean work(Level level) {
        // The other end was broken: forget the line.
        if (other != null && level.getGameTime() % 20 == 0 && level.isLoaded(other)
                && !(level.getBlockEntity(other) instanceof CableStationBlockEntity s && worldPosition.equals(s.other))) {
            unlink();
        }
        return other != null;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int[] t = input.getIntArray("Other").orElse(new int[0]);
        other = t.length == 3 ? new BlockPos(t[0], t[1], t[2]) : null;
        master = input.getBooleanOr("Master", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (other != null) output.putIntArray("Other", new int[] {other.getX(), other.getY(), other.getZ()});
        output.putBoolean("Master", master);
    }
}
