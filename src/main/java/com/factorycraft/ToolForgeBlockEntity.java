package com.factorycraft;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Tool forge: a mould in the first slot (not used up) picks the tool, the second
 * slot takes the material (any metal or gem, from any mod) and the third a part:
 * a circuit board for the electric tools (drill, multitool), an iron gear for the rest.
 */
public final class ToolForgeBlockEntity extends PoweredMachineBlockEntity {
    private static final TagKey<net.minecraft.world.item.Item> GEARS = TagKey.create(net.minecraft.core.registries.Registries.ITEM,
            net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "gears/iron"));

    public ToolForgeBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.TOOL_FORGE_BLOCK_ENTITY.value(), pos, state, 3);
    }

    @Override protected int time() { return 200; }
    @Override protected int energyPerTick() { return 40; }
    @Override protected MenuType<ProcessingMenu> menuType() { return FactoryCraft.TOOL_FORGE_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.tool_forge"; }

    static ToolForge.@Nullable Kind kindOf(ItemStack mould) {
        return mould.getItem() instanceof MouldItem item ? item.kind() : null;
    }

    private static boolean isPart(ToolForge.Kind kind, ItemStack stack) {
        return kind.electric ? stack.is(Materials.CIRCUIT.value()) : stack.is(GEARS);
    }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> stack.getItem() instanceof MouldItem;
            case 1 -> ToolForge.statsFor(stack) != null;
            case 2 -> stack.is(Materials.CIRCUIT.value()) || stack.is(GEARS);
            default -> false;
        };
    }

    @Override
    protected @Nullable Operation find(Level level) {
        ToolForge.Kind kind = kindOf(items.get(0));
        ItemStack material = items.get(1);
        if (kind == null || material.getCount() < kind.materials || !isPart(kind, items.get(2))) return null;
        ToolForge.Stats stats = ToolForge.statsFor(material);
        if (stats == null) return null;
        return new Operation(new int[] {0, kind.materials, 1}, List.of(ToolForge.forge(kind, material, stats, level.registryAccess())));
    }

    @Override
    protected List<ItemStack> produce(Level level, Operation operation) {
        return List.of(operation.maxOutputs().get(0).copy());
    }
}
