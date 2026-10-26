package com.factorycraft;

import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Bulldozer. While driving forward its blade levels a 5-wide strip at track
 * height: everything up to 4 blocks above the ground is scraped off into the
 * bay, and holes (down to the bottom, up to 32 deep, water included) are filled with the dirt and
 * rubble it has collected. Drive across rough land and it leaves it flat.
 * Unlike the paver it uses whatever earth it scrapes, so it never needs loading.
 */
public final class BulldozerEntity extends WorkVehicleEntity {
    private static final int HALF_WIDTH = 2;
    private static final int CLEAR_HEIGHT = 4;
    /** Deepest hole the blade fills, bottom up, so nothing is left hollow. */
    private static final int FILL_DEPTH = 32;
    /** What the blade fills holes with, in order of preference. */
    private static final List<Item> FILL = List.of(Items.DIRT, Items.COARSE_DIRT, Items.COBBLESTONE,
            Items.COBBLED_DEEPSLATE, Items.NETHERRACK, Items.STONE, Items.DEEPSLATE, Items.TUFF,
            Items.ANDESITE, Items.DIORITE, Items.GRANITE, Items.MUD, Items.CLAY, Items.TERRACOTTA);
    private static final Set<Item> FILL_SET = Set.copyOf(FILL);

    public BulldozerEntity(EntityType<? extends BulldozerEntity> type, Level level) {
        super(type, level, () -> FactoryCraft.BULLDOZER_ITEM.value());
    }

    @Override protected double maxSpeed() { return 0.26; }
    @Override protected double reverseSpeed() { return 0.14; }
    @Override protected double seatHeight() { return 1.69; }
    @Override protected double seatForward() { return -1.08; }
    @Override protected double hullShift() { return 0.09; }
    @Override protected double hullHalfLength() { return 2.9; }
    @Override protected double hullHalfWidth() { return 2.5; }

    @Override
    protected void work(ServerLevel level, Player driver) {
        List<Container> sinks = sinks(level);
        int ground = groundY();
        double ahead = hullFront() + 0.4;
        int half = engine().wide() ? HALF_WIDTH + 1 : HALF_WIDTH;
        for (int side = -half; side <= half; side++) {
            // Scrape everything above the ground off into the bay.
            for (int h = 0; h < CLEAR_HEIGHT; h++) {
                BlockPos pos = columnAt(ahead, side, ground + h);
                BlockState state = level.getBlockState(pos);
                if (isBreakable(level, pos, state)) breakInto(level, pos, state, driver, sinks);
            }
            // Fill the hole under the blade, deepest first so every block is supported.
            BlockPos surface = columnAt(ahead, side, ground - 1);
            if (!isHole(level.getBlockState(surface))) continue;
            int depth = 1;
            while (depth < FILL_DEPTH && isHole(level.getBlockState(columnAt(ahead, side, ground - depth - 1)))) depth++;
            for (int d = depth; d >= 1; d--) {
                BlockPos pos = columnAt(ahead, side, ground - d);
                if (!isHole(level.getBlockState(pos))) continue;
                Block fill = takeFill();
                if (fill == null) return;
                level.setBlock(pos, fill.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    private static boolean isHole(BlockState state) {
        return state.isAir() || state.canBeReplaced(); // includes water, lava and grass
    }

    /** Takes one fill block from the bay (dirt first, then rubble). */
    private Block takeFill() {
        for (Item wanted : FILL) {
            ItemStack one = takeOne(stack -> stack.is(wanted));
            if (!one.isEmpty() && one.getItem() instanceof BlockItem item) return item.getBlock();
        }
        return null;
    }

    static boolean isFillMaterial(ItemStack stack) {
        return FILL_SET.contains(stack.getItem());
    }
}
