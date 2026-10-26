package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/**
 * Tree planter: replants its area with the saplings in its six slots, one every
 * three blocks on grass or dirt (so the trees have room), within 16 blocks up
 * or down of its own level. Puts back the forests the forester takes.
 */
public final class PlanterBlockEntity extends AreaMachineBlockEntity {
    private static final int SPACING = 3;

    public PlanterBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.PLANTER_BLOCK_ENTITY.value(), pos, state, ProcessingMenu.BIG_INPUTS, 0, new int[] {6, 9, 12, 18}, 9);
    }

    @Override protected int time() { return 10; }
    @Override protected int energyPerTick() { return energyPerBlock(); }
    @Override protected int energyPerBlock() { return 20; }
    @Override protected MenuType<ProcessingMenu> menuType() { return FactoryCraft.PLANTER_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.planter"; }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return slot < ProcessingMenu.BIG_INPUTS && stack.is(ItemTags.SAPLINGS) && stack.getItem() instanceof BlockItem;
    }

    @Override
    protected @Nullable BlockPos findNext(ServerLevel level) {
        if (sapling() == null) return null;
        long total = (long) side() * side();
        for (int checked = 0; checked < 256 && cursor < total; checked++) {
            int x = columnX(cursor);
            int z = columnZ(cursor);
            if (Math.floorMod(x, SPACING) == 0 && Math.floorMod(z, SPACING) == 0) {
                BlockPos spot = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
                if (level.isLoaded(spot) && Math.abs(spot.getY() - worldPosition.getY()) <= 16
                        && level.getBlockState(spot).isAir() && level.getBlockState(spot.below()).is(BlockTags.DIRT)) {
                    return spot;
                }
            }
            cursor++;
        }
        return cursor >= total ? null : NOT_YET;
    }

    @Override
    protected boolean workAt(ServerLevel level, BlockPos pos) {
        Integer slot = sapling();
        if (slot == null) return false;
        Block block = ((BlockItem) items.get(slot).getItem()).getBlock();
        BlockState state = block.defaultBlockState();
        if (state.canSurvive(level, pos)) {
            level.setBlock(pos, state, Block.UPDATE_ALL);
            items.get(slot).shrink(1);
        }
        cursor++;
        return true;
    }

    /** First slot with a sapling, or null. */
    private Integer sapling() {
        for (int i = 0; i < ProcessingMenu.BIG_INPUTS; i++) {
            if (!items.get(i).isEmpty() && items.get(i).getItem() instanceof BlockItem) return i;
        }
        return null;
    }
}
