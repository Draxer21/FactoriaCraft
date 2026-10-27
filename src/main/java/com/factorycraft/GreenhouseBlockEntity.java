package com.factorycraft;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Greenhouse controller: tends the crops around it. It waters the farmland,
 * makes crops, saplings and other plants grow fast, and harvests ripe crops
 * into its 8 output slots, replanting them with their own seeds.
 */
public final class GreenhouseBlockEntity extends AreaMachineBlockEntity {
    private static final int BELOW = 2;
    private static final int ABOVE = 3;

    public GreenhouseBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.GREENHOUSE_BLOCK_ENTITY.value(), pos, state, 0, ProcessingMenu.BIG_OUTPUTS, new int[] {4, 6, 8}, 6);
    }

    @Override protected int time() { return 1; }
    @Override protected int energyPerTick() { return energyPerBlock(); }
    @Override protected int energyPerBlock() { return 15; }
    @Override protected int perTick() { return 2; }
    @Override protected int idleTicks() { return 20; }
    @Override protected MenuType<ProcessingMenu> menuType() { return FactoryCraft.GREENHOUSE_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.greenhouse"; }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return false;
    }

    private boolean needsWork(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof FarmBlock) return state.getValue(FarmBlock.MOISTURE) < FarmBlock.MAX_MOISTURE;
        if (state.getBlock() instanceof CropBlock crop) return true;
        return state.getBlock() instanceof BonemealableBlock plant && plant.isValidBonemealTarget(level, pos, state)
                && !(state.getBlock() instanceof net.minecraft.world.level.block.GrassBlock);
    }

    @Override
    protected @Nullable BlockPos findNext(ServerLevel level) {
        long columns = (long) side() * side();
        int layers = BELOW + ABOVE + 1;
        long total = columns * layers;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int checked = 0; checked < 1024 && cursor < total; checked++) {
            long column = cursor % columns;
            int y = worldPosition.getY() - BELOW + (int) (cursor / columns);
            pos.set(columnX(column), y, columnZ(column));
            if (level.isLoaded(pos) && needsWork(level, pos, level.getBlockState(pos))) return pos.immutable();
            cursor++;
        }
        return cursor >= total ? null : NOT_YET;
    }

    @Override
    protected boolean workAt(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof FarmBlock) {
            level.setBlock(pos, state.setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE), Block.UPDATE_CLIENTS);
        } else if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
            List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, level, pos, null));
            Item seed = crop.asItem();
            for (ItemStack drop : drops) {
                if (drop.is(seed)) {
                    drop.shrink(1);
                    break;
                }
            }
            drops.removeIf(ItemStack::isEmpty);
            if (!canStore(drops)) return false;
            store(drops);
            level.setBlock(pos, crop.getStateForAge(0), Block.UPDATE_ALL);
        } else {
            state.randomTick(level, pos, level.getRandom());
            state = level.getBlockState(pos);
            state.randomTick(level, pos, level.getRandom());
        }
        cursor++;
        return true;
    }
}
