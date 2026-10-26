package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Builder: builds the structure on the blueprint in its first slot, in front of
 * itself (the side with the panel), from the bottom up, using the blocks in its
 * other five slots. Missing a block, it waits until you add it. It never breaks
 * anything: spots already taken by another block are skipped.
 */
public final class BuilderBlockEntity extends PoweredMachineBlockEntity {
    private static final int INPUTS = ProcessingMenu.BIG_INPUTS;
    private static final int ENERGY_PER_BLOCK = 20;

    private int next;
    private int timer;
    /** Identifies the blueprint being built, so a new one restarts. */
    private int blueprintHash;
    private @Nullable Blueprint cached;

    public BuilderBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.BUILDER_BLOCK_ENTITY.value(), pos, state, INPUTS, 0);
    }

    @Override protected int time() { return 2; }
    @Override protected int energyPerTick() { return ENERGY_PER_BLOCK; }
    @Override protected MenuType<ProcessingMenu> menuType() { return FactoryCraft.BUILDER_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.builder"; }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        if (slot == 0) return stack.getItem() instanceof BlueprintItem;
        return slot < INPUTS && stack.getItem() instanceof BlockItem;
    }

    private @Nullable Blueprint blueprint(Level level) {
        ItemStack paper = items.get(0);
        if (paper.isEmpty() || !(paper.getItem() instanceof BlueprintItem)) {
            cached = null;
            return null;
        }
        int hash = paper.getComponents().hashCode();
        if (cached == null || hash != blueprintHash) {
            cached = Blueprint.readFrom(paper, level);
            if (hash != blueprintHash) next = 0;
            blueprintHash = hash;
        }
        return cached;
    }

    /** Lowest corner of the build: centred in front of the machine, at its level. */
    private BlockPos origin(Blueprint blueprint) {
        Direction front = getBlockState().getValue(MachineBlock.FACING);
        int reach = 2 + Math.max(blueprint.sizeX(), blueprint.sizeZ()) / 2;
        BlockPos center = worldPosition.relative(front, reach);
        return center.offset(-blueprint.sizeX() / 2, 0, -blueprint.sizeZ() / 2);
    }

    @Override
    protected boolean work(Level level) {
        Blueprint blueprint = blueprint(level);
        if (blueprint == null || next >= blueprint.count()) return false;
        if (++timer < scaledTime(time())) return true;
        timer = 0;
        BlockPos origin = origin(blueprint);
        // Skip what is already done or can't be built, a few per tick.
        for (int skipped = 0; skipped < 64 && next < blueprint.count(); skipped++) {
            BlockPos target = origin.offset(blueprint.positions().get(next));
            BlockState wanted = blueprint.states().get(next);
            BlockState there = level.getBlockState(target);
            if (there == wanted || !(there.isAir() || there.canBeReplaced()) || level.isOutsideBuildHeight(target)) {
                next++;
                continue;
            }
            boolean free = isSecondHalf(wanted); // the top of a door or the head of a bed comes with the other half
            int slot = free ? -1 : materialSlot(wanted);
            if (!free && slot < 0) return false; // waiting for material
            if (!drawPower(level, ENERGY_PER_BLOCK)) return false;
            level.setBlock(target, wanted, Block.UPDATE_ALL);
            if (slot >= 0) items.get(slot).shrink(1);
            next++;
            setChanged();
            return true;
        }
        return next < blueprint.count();
    }

    private static boolean isSecondHalf(BlockState state) {
        return (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER)
                || (state.hasProperty(BlockStateProperties.BED_PART) && state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD);
    }

    private int materialSlot(BlockState wanted) {
        Item item = wanted.getBlock().asItem();
        for (int i = 1; i < INPUTS; i++) {
            if (!items.get(i).isEmpty() && items.get(i).is(item)) return i;
        }
        return -1;
    }

    /** CI self-test: what the builder thinks it is doing. */
    String debugState(Level level) {
        Blueprint blueprint = blueprint(level);
        return "blueprint=" + (blueprint == null ? "none" : blueprint.count()) + " next=" + next
                + " slot0=" + items.get(0) + " slot1=" + items.get(1);
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
        next = input.getIntOr("Next", 0);
        blueprintHash = input.getIntOr("Blueprint", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Next", next);
        output.putInt("Blueprint", blueprintHash);
    }
}
