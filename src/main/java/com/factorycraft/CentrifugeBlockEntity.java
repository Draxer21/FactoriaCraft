package com.factorycraft;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Uranium centrifuge: enriches uranium step by step. Every grade needs four
 * of the grade below: 4 uranium dust -> Grade I, 4 Grade I -> Grade II,
 * 4 Grade II -> Grade III. The grade of the core decides how strong a
 * mini-nuke is.
 */
public final class CentrifugeBlockEntity extends PoweredMachineBlockEntity {
    record Stage(Supplier<Item> input, int count, Supplier<Item> output) {}

    static final List<Stage> STAGES = List.of(
            new Stage(() -> Nuclear.URANIUM_DUST.value(), 4, () -> Nuclear.ENRICHED_URANIUM_1.value()),
            new Stage(() -> Nuclear.ENRICHED_URANIUM_1.value(), 4, () -> Nuclear.ENRICHED_URANIUM_2.value()),
            new Stage(() -> Nuclear.ENRICHED_URANIUM_2.value(), 4, () -> Nuclear.ENRICHED_URANIUM_3.value()));

    public CentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(Nuclear.CENTRIFUGE_BLOCK_ENTITY.value(), pos, state, 1);
    }

    @Override protected int time() { return 160; }
    @Override protected int energyPerTick() { return 80; }
    @Override protected MenuType<ProcessingMenu> menuType() { return Nuclear.CENTRIFUGE_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.centrifuge"; }

    private static @Nullable Stage stageFor(ItemStack stack) {
        for (Stage stage : STAGES) {
            if (stack.is(stage.input().get())) return stage;
        }
        return null;
    }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return slot == 0 && stageFor(stack) != null;
    }

    @Override
    protected @Nullable Operation find(Level level) {
        ItemStack in = items.get(0);
        Stage stage = stageFor(in);
        if (stage == null || in.getCount() < stage.count()) return null;
        return new Operation(new int[] {stage.count()}, List.of(new ItemStack(stage.output().get())));
    }

    @Override
    protected List<ItemStack> produce(Level level, Operation operation) {
        return operation.maxOutputs();
    }
}
