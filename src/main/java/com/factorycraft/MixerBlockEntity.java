package com.factorycraft;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Concrete mixer. Slots: dye, sand, gravel. 4 sand + 4 gravel + 1 dye make 8
 * concrete powder of that colour; with water touching the mixer it pours
 * finished concrete blocks instead.
 */
public final class MixerBlockEntity extends PoweredMachineBlockEntity {
    public MixerBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.MIXER_BLOCK_ENTITY.value(), pos, state, 3);
    }

    @Override protected int time() { return 60; }
    @Override protected int energyPerTick() { return 15; }
    @Override protected MenuType<ProcessingMenu> menuType() { return FactoryCraft.MIXER_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.mixer"; }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> stack.get(DataComponents.DYE) != null;
            case 1 -> stack.is(Items.SAND) || stack.is(Items.RED_SAND);
            case 2 -> stack.is(Items.GRAVEL);
            default -> false;
        };
    }

    @Override
    protected @Nullable Operation find(Level level) {
        DyeColor color = items.get(0).get(DataComponents.DYE);
        if (color == null || items.get(1).getCount() < 4 || items.get(2).getCount() < 4) return null;
        boolean wet = hasWater(level);
        Item result = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(
                color.getSerializedName() + (wet ? "_concrete" : "_concrete_powder")));
        if (result == Items.AIR) return null;
        return new Operation(new int[] {1, 4, 4}, List.of(new ItemStack(result, 8)));
    }

    private boolean hasWater(Level level) {
        for (Direction dir : Direction.values()) {
            if (level.getFluidState(worldPosition.relative(dir)).is(FluidTags.WATER)) return true;
        }
        return false;
    }

    @Override
    protected List<ItemStack> produce(Level level, Operation operation) {
        return operation.maxOutputs().stream().map(ItemStack::copy).toList();
    }
}
