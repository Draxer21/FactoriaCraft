package com.factorycraft;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Particle accelerator: needs a ring of at least 8 superconducting magnets
 * connected to it (more magnets, faster: 8 halve the time, 32 make it 5 times
 * faster). 250 power a tick. Fills containment cells with antimatter and
 * transmutes a few materials.
 */
public final class AcceleratorBlockEntity extends PoweredMachineBlockEntity {
    public static final int MIN_MAGNETS = 8;
    private static final int MAX_SCAN = 256;
    private static final int BASE_TIME = 1200;

    record Transmutation(Supplier<Item> input, Supplier<Item> output, int count) {}

    static final List<Transmutation> RECIPES = List.of(
            new Transmutation(() -> Endgame.CONTAINMENT_CELL.value(), () -> Endgame.ANTIMATTER_CELL.value(), 1),
            new Transmutation(() -> Items.COAL_BLOCK, () -> Items.DIAMOND, 1),
            new Transmutation(() -> Items.AMETHYST_BLOCK, () -> Items.ECHO_SHARD, 1),
            new Transmutation(() -> Items.ENDER_PEARL, () -> Exotic.STABLE_ENDER_CRYSTAL.value(), 1),
            new Transmutation(() -> Items.GLOWSTONE, () -> Exotic.INDUSTRIAL_QUARTZ.value(), 2));

    private int magnets;

    public AcceleratorBlockEntity(BlockPos pos, BlockState state) {
        super(Endgame.ACCELERATOR_BLOCK_ENTITY.value(), pos, state, 1);
    }

    @Override protected int time() { return Math.max(100, (int) (BASE_TIME / (1.0 + magnets / 8.0))); }
    @Override protected int energyPerTick() { return 250; }
    @Override protected MenuType<ProcessingMenu> menuType() { return Endgame.ACCELERATOR_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.accelerator"; }

    @Override
    public Component getDisplayName() {
        return super.getDisplayName().copy().append(" · ").append(Component.translatable("gui.factorycraft.accelerator.magnets", magnets, MIN_MAGNETS));
    }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return slot == 0 && RECIPES.stream().anyMatch(r -> stack.is(r.input().get()));
    }

    /** Magnets connected to the accelerator (touching it, or each other in a chain). */
    static int countMagnets(Level level, BlockPos start) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (Direction dir : Direction.values()) {
            BlockPos p = start.relative(dir);
            if (level.getBlockState(p).is(Endgame.MAGNET.value()) && seen.add(p)) queue.add(p);
        }
        while (!queue.isEmpty() && seen.size() < MAX_SCAN) {
            BlockPos pos = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (!seen.contains(next) && level.getBlockState(next).is(Endgame.MAGNET.value())) {
                    seen.add(next);
                    queue.add(next);
                }
            }
        }
        return seen.size();
    }

    int magnets() {
        return magnets;
    }

    @Override
    protected boolean work(Level level) {
        if (level.getGameTime() % 40 == 0) magnets = countMagnets(level, worldPosition);
        if (magnets < MIN_MAGNETS) return false;
        return super.work(level);
    }

    @Override
    protected @Nullable Operation find(Level level) {
        ItemStack input = items.get(0);
        for (Transmutation r : RECIPES) {
            if (input.is(r.input().get())) return new Operation(new int[] {1}, List.of(new ItemStack(r.output().get(), r.count())));
        }
        return null;
    }

    @Override
    protected List<ItemStack> produce(Level level, Operation operation) {
        return operation.maxOutputs().stream().map(ItemStack::copy).toList();
    }
}
