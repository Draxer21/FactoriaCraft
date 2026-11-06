package com.factorycraft;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Refinery: one canister of crude oil makes a diesel canister, a plastic sheet and two asphalt blocks. */
public final class RefineryBlockEntity extends PoweredMachineBlockEntity implements FluidStorage.Holder {
    /** Piped crude oil (8 canisters' worth). */
    private final FluidStorage fluids = new FluidStorage(this::setChanged)
            .slot(8 * FluidStorage.BUCKET, true, false, fluid -> fluid == Fuels.CRUDE_OIL.value());
    private boolean fromTank;

    @Override public FluidStorage fluids() { return fluids; }

    public RefineryBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.REFINERY_BLOCK_ENTITY.value(), pos, state, 1);
    }

    @Override protected int time() { return 120; }
    @Override protected int energyPerTick() { return 40; }
    @Override protected MenuType<ProcessingMenu> menuType() { return FactoryCraft.REFINERY_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.refinery"; }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        return slot == 0 && stack.is(FactoryCraft.CRUDE_OIL.value());
    }

    static List<ItemStack> products() {
        return List.of(new ItemStack(FactoryCraft.DIESEL.value()), new ItemStack(Materials.PLASTIC.value()),
                new ItemStack(FactoryCraft.ASPHALT_ITEM.value(), 2));
    }

    @Override
    protected @Nullable Operation find(Level level) {
        if (items.get(0).is(FactoryCraft.CRUDE_OIL.value())) {
            fromTank = false;
            return new Operation(new int[] {1}, products());
        }
        if (fluids.amount(0) >= FluidStorage.BUCKET) {
            fromTank = true;
            return new Operation(new int[] {0}, products());
        }
        return null;
    }

    @Override
    protected List<ItemStack> produce(Level level, Operation operation) {
        if (fromTank) fluids.take(0, FluidStorage.BUCKET);
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack stack : operation.maxOutputs()) {
            // Diesel goes down the pipes as fluid when something takes it.
            if (stack.is(FactoryCraft.DIESEL.value()) && FluidPipes.deliver(level, worldPosition, Fuels.DIESEL.value(), FluidStorage.BUCKET)) continue;
            out.add(stack.copy());
        }
        return out;
    }

    @Override
    public void jadeInfo(List<net.minecraft.network.chat.Component> lines) {
        if (fluids.amount(0) > 0) lines.add(fluids.describe(0));
        super.jadeInfo(lines);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        fluids.load(input);
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        fluids.save(output);
    }
}
