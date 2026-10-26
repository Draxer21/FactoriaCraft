package com.factorycraft;

import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Terrain filler: fills every hole, pond, ravine and crater around it up to the
 * level it stands on, from the bottom up, so nothing is left hollow. It works a
 * square area (sneak + right-click with an empty hand: radius 4, 8, 16 or 24)
 * column by column, using the dirt, stone and rubble in its six slots.
 * Caves sealed under solid ground are left alone; only open holes are filled.
 */
public final class FillerBlockEntity extends PoweredMachineBlockEntity implements AreaMachineBlockEntity.Area {
    private static final int INPUTS = ProcessingMenu.BIG_INPUTS;
    private static final int[] RADII = {4, 8, 16, 24};
    /** Deepest hole it fills under its own level. */
    private static final int MAX_DEPTH = 96;
    private static final int TIME = 2;
    private static final int ENERGY_PER_BLOCK = 30;

    private static final List<Item> FILL = List.of(Items.DIRT, Items.COARSE_DIRT, Items.ROOTED_DIRT, Items.COBBLESTONE,
            Items.COBBLED_DEEPSLATE, Items.STONE, Items.DEEPSLATE, Items.TUFF, Items.ANDESITE, Items.DIORITE,
            Items.GRANITE, Items.NETHERRACK, Items.MUD, Items.PACKED_MUD, Items.CLAY, Items.TERRACOTTA, Items.GRAVEL,
            Items.SAND, Items.RED_SAND, Items.SANDSTONE, Items.CALCITE, Items.BLACKSTONE, Items.BASALT, Items.END_STONE,
            Items.MOSS_BLOCK, Items.GRASS_BLOCK);
    private static final Set<Item> FILL_SET = Set.copyOf(FILL);

    int radius = 8; // package-private for the CI self-test
    /** Column being worked: index into the square, row by row. */
    private int column;
    private int timer;
    private boolean done;

    public FillerBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.FILLER_BLOCK_ENTITY.value(), pos, state, INPUTS, 0);
    }

    @Override protected int time() { return TIME; }
    @Override protected int energyPerTick() { return ENERGY_PER_BLOCK; }
    @Override protected MenuType<ProcessingMenu> menuType() { return FactoryCraft.FILLER_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.filler"; }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return slot < INPUTS && FILL_SET.contains(stack.getItem());
    }

    @Override
    public void cycleRadius(Player player) {
        int next = RADII[0];
        for (int r : RADII) {
            if (r > radius) {
                next = r;
                break;
            }
        }
        radius = next;
        column = 0;
        done = false;
        setChanged();
        int side = radius * 2 + 1;
        player.sendOverlayMessage(Component.translatable("message.factorycraft.filler.area", side, side));
        if (level instanceof ServerLevel server) outline(server);
    }

    /** Shows the area's border for a moment. */
    private void outline(ServerLevel level) {
        int y = worldPosition.getY();
        for (int i = -radius; i <= radius; i++) {
            for (int[] p : new int[][] {{i, -radius}, {i, radius}, {-radius, i}, {radius, i}}) {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, worldPosition.getX() + p[0] + 0.5, y + 0.2,
                        worldPosition.getZ() + p[1] + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    protected boolean work(Level level) {
        if (done) {
            // Re-check now and then: the world may have changed (new holes, water flowing in).
            if (level.getGameTime() % 200 != 0) return false;
            done = false;
            column = 0;
        }
        if (!hasFill()) return false;
        if (++timer < scaledTime(TIME)) return true;
        timer = 0;
        BlockPos target = nextTarget(level);
        if (target == null) {
            done = true;
            setProgress(0);
            return false;
        }
        if (target == NOT_YET) return true;
        if (!drawPower(level, ENERGY_PER_BLOCK)) return false;
        Block fill = takeFill();
        if (fill == null) return false;
        level.setBlock(target, fill.defaultBlockState(), Block.UPDATE_ALL);
        setChanged();
        return true;
    }

    /** The lowest open block of the first column that still has a hole, or null when all are full. */
    private @Nullable BlockPos nextTarget(Level level) {
        int side = radius * 2 + 1;
        int total = side * side;
        // Check a limited number of columns per tick so a big, finished area stays cheap.
        for (int checked = 0; checked < 64 && column < total; checked++) {
            int dx = column % side - radius;
            int dz = column / side - radius;
            BlockPos low = lowestOpen(level, worldPosition.getX() + dx, worldPosition.getZ() + dz);
            if (low != null) return low;
            column++;
        }
        return column >= total ? null : nextTargetLater();
    }

    /** Still scanning: nothing this tick, but not done either. */
    private @Nullable BlockPos nextTargetLater() {
        timer = scaledTime(TIME); // try again next tick without waiting
        return NOT_YET;
    }

    private static final BlockPos NOT_YET = new BlockPos(0, Integer.MIN_VALUE, 0);

    /** Walks down from just under the machine's level while the column is open; returns the bottom. */
    private @Nullable BlockPos lowestOpen(Level level, int x, int z) {
        int top = worldPosition.getY() - 1;
        int bottom = Math.max(level.getMinY(), top - MAX_DEPTH);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, top, z);
        if (!level.isLoaded(pos) || !isOpen(level.getBlockState(pos))) return null;
        int y = top;
        while (y - 1 >= bottom && isOpen(level.getBlockState(pos.setY(y - 1)))) y--;
        return new BlockPos(x, y, z);
    }

    private static boolean isOpen(BlockState state) {
        return (state.isAir() || state.canBeReplaced()) && !state.hasBlockEntity();
    }

    private boolean hasFill() {
        for (int i = 0; i < INPUTS; i++) {
            if (!items.get(i).isEmpty()) return true;
        }
        return false;
    }

    private @Nullable Block takeFill() {
        for (int i = 0; i < INPUTS; i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem item) {
                stack.shrink(1);
                return item.getBlock();
            }
        }
        return null;
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
        radius = input.getIntOr("Radius", 8);
        column = input.getIntOr("Column", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Radius", radius);
        output.putInt("Column", column);
    }
}
