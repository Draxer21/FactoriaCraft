package com.factorycraft;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Chemical bath (ore processing tier 3): dissolves raw ores and ore blocks in
 * sulfuric water, for twice what the crusher gives (raw ore ×4, ore block ×8).
 * Each batch uses one sulfur and a quarter bucket of water (piped or by bucket).
 */
public final class ChemicalBathBlockEntity extends PoweredMachineBlockEntity implements FluidStorage.Holder {
    public static final int TIME = 100;
    public static final int ENERGY = 60;
    public static final int WATER_PER_BATCH = FluidStorage.BUCKET / 4;
    public static final int MULTIPLIER = 2;
    private static final TagKey<Item> RAW = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "raw_materials"));
    private static final TagKey<Item> ORES = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ores"));

    private final FluidStorage fluids = new FluidStorage(this::setChanged)
            .slot(8 * FluidStorage.BUCKET, true, false, FluidStorage::water);

    public ChemicalBathBlockEntity(BlockPos pos, BlockState state) {
        super(Processing.CHEMICAL_BATH_BLOCK_ENTITY.value(), pos, state, 2);
    }

    @Override public FluidStorage fluids() { return fluids; }
    @Override protected int time() { return TIME; }
    @Override protected int energyPerTick() { return ENERGY; }
    @Override protected MenuType<ProcessingMenu> menuType() { return Processing.CHEMICAL_BATH_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.chemical_bath"; }

    /** What one batch of {@code input} gives, or null if the bath can't dissolve it. */
    static @Nullable ItemStack result(ItemStack input) {
        if (!input.is(RAW) && !input.is(ORES)) return null;
        ItemStack crushed = CrusherRecipes.resultFor(input);
        if (crushed == null || crushed.isEmpty()) return null;
        return crushed.copyWithCount(Math.min(crushed.getMaxStackSize(), crushed.getCount() * MULTIPLIER));
    }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        if (slot == 0) return result(stack) != null;
        if (slot == 1) return stack.is(Exotic.SULFUR.value());
        return false;
    }

    @Override
    protected @Nullable Operation find(Level level) {
        ItemStack ore = items.get(0);
        if (!items.get(1).is(Exotic.SULFUR.value()) || fluids.amount(0) < WATER_PER_BATCH) return null;
        ItemStack out = result(ore);
        if (out == null) return null;
        return new Operation(new int[] {1, 1}, List.of(out));
    }

    @Override
    protected List<ItemStack> produce(Level level, Operation operation) {
        fluids.take(0, WATER_PER_BATCH);
        return operation.maxOutputs().stream().map(ItemStack::copy).toList();
    }

    @Override
    public Component getDisplayName() {
        return super.getDisplayName().copy().append(" · ").append(fluids.describe(0));
    }

    @Override
    public void jadeInfo(List<Component> lines) {
        lines.add(fluids.describe(0));
        super.jadeInfo(lines);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fluids.load(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        fluids.save(output);
    }
}
