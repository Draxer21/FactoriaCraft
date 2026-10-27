package com.factorycraft;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Desalinator: boils sea water into salt and fresh water. Placed touching the sea
 * (ocean or beach water) it draws its own; anywhere else it can be fed water by
 * pumps, pipes or buckets. Each bucket of sea water gives a pinch of salt and
 * half a bucket of fresh water, which it hands to tanks and pipes beside it.
 */
public final class DesalinatorBlockEntity extends PoweredMachineBlockEntity implements FluidStorage.Holder {
    private static final int INTAKE = 0;
    private static final int FRESH = 1;
    private static final int TIME = 100;
    private static final int SEA_PER_SECOND = 250;
    private final FluidStorage fluids = new FluidStorage(this::setChanged)
            .slot(8 * FluidStorage.BUCKET, true, false, FluidStorage::water)
            .slot(8 * FluidStorage.BUCKET, false, true, FluidStorage::water);
    private int timer;

    public DesalinatorBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.DESALINATOR_BLOCK_ENTITY.value(), pos, state, 0);
    }

    @Override public FluidStorage fluids() { return fluids; }
    @Override protected int time() { return TIME; }
    @Override protected int energyPerTick() { return 20; }
    @Override protected MenuType<ProcessingMenu> menuType() { return FactoryCraft.DESALINATOR_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.desalinator"; }
    @Override protected boolean accepts(int slot, ItemStack stack) { return false; }

    @Override
    public Component getDisplayName() {
        return super.getDisplayName().copy().append(" · ").append(fluids.describe(INTAKE));
    }

    /** True if a sea water source touches the machine. */
    boolean touchesSea(Level level) {
        for (Direction dir : Direction.values()) {
            BlockPos pos = worldPosition.relative(dir);
            FluidState fluid = level.getFluidState(pos);
            if (!fluid.isSource() || !fluid.is(net.minecraft.tags.FluidTags.WATER)) continue;
            var biome = level.getBiome(pos);
            if (biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_BEACH)) return true;
        }
        return false;
    }

    @Override
    protected boolean work(Level level) {
        if (level.getGameTime() % 20 == 0 && touchesSea(level)) fluids.add(INTAKE, Fluids.WATER, SEA_PER_SECOND);
        if (level.getGameTime() % 10 == 0 && fluids.amount(FRESH) > 0) {
            for (Direction dir : Direction.values()) fluids.pushTo(level, worldPosition, dir, FRESH, FluidStorage.BUCKET);
        }
        List<ItemStack> salt = List.of(new ItemStack(FactoryCraft.SALT.value()));
        if (fluids.amount(INTAKE) < FluidStorage.BUCKET || fluids.space(FRESH) < FluidStorage.BUCKET / 2 || !canStore(salt)) {
            timer = 0;
            setProgress(0);
            return false;
        }
        if (!drawPower(level, energyPerTick())) return false;
        if (++timer >= scaledTime(TIME)) {
            timer = 0;
            fluids.take(INTAKE, FluidStorage.BUCKET);
            fluids.add(FRESH, Fluids.WATER, FluidStorage.BUCKET / 2);
            store(boost(level, salt));
        }
        setProgress(timer);
        return true;
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
        fluids.load(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        fluids.save(output);
    }
}
