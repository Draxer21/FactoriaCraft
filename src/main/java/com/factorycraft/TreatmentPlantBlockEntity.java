package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Water treatment plant: uses clean water to wash the ground around it. It turns
 * contaminated soil back into dirt and smog-scorched coarse dirt back into grass,
 * a quarter bucket each, and while it runs the area is protected from new smog
 * and fallout like under an air purifier. Feed it water with pumps, pipes,
 * buckets, a desalinator or any mod's pipes.
 */
public final class TreatmentPlantBlockEntity extends AreaMachineBlockEntity implements FluidStorage.Holder {
    private static final int WATER_PER_BLOCK = FluidStorage.BUCKET / 4;
    private static final int BELOW = 4;
    private static final int ABOVE = 3;
    private final FluidStorage fluids = new FluidStorage(this::setChanged).slot(16 * FluidStorage.BUCKET, true, false, FluidStorage::water);

    public TreatmentPlantBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.TREATMENT_PLANT_BLOCK_ENTITY.value(), pos, state, 0, 0, new int[] {4, 8, 12}, 8);
    }

    @Override public FluidStorage fluids() { return fluids; }
    @Override protected int time() { return 4; }
    @Override protected int energyPerTick() { return energyPerBlock(); }
    @Override protected int energyPerBlock() { return 60; }
    @Override protected int idleTicks() { return 100; }
    @Override protected MenuType<ProcessingMenu> menuType() { return FactoryCraft.TREATMENT_PLANT_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.treatment_plant"; }
    @Override protected boolean accepts(int slot, ItemStack stack) { return false; }

    @Override
    public Component getDisplayName() {
        return super.getDisplayName().copy().append(" · ").append(fluids.describe(0));
    }

    @Override
    protected boolean work(Level level) {
        if (fluids.amount(0) >= WATER_PER_BLOCK && level.getGameTime() % 20 == 0) Pollution.purifierActive(level, worldPosition);
        return super.work(level);
    }

    static boolean dirty(Level level, BlockPos pos, BlockState state) {
        if (state.is(Nuclear.CONTAMINATED_SOIL.value())) return true;
        return state.is(Blocks.COARSE_DIRT) && level.getBlockState(pos.above()).isAir() && level.canSeeSky(pos.above());
    }

    @Override
    protected @Nullable BlockPos findNext(ServerLevel level) {
        long columns = (long) side() * side();
        long total = columns * (BELOW + ABOVE + 1);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int checked = 0; checked < 2048 && cursor < total; checked++) {
            long column = cursor % columns;
            int y = worldPosition.getY() - BELOW + (int) (cursor / columns);
            pos.set(columnX(column), y, columnZ(column));
            if (level.isLoaded(pos) && dirty(level, pos, level.getBlockState(pos))) return pos.immutable();
            cursor++;
        }
        return cursor >= total ? null : NOT_YET;
    }

    @Override
    protected boolean workAt(ServerLevel level, BlockPos pos) {
        if (fluids.amount(0) < WATER_PER_BLOCK) return false;
        BlockState state = level.getBlockState(pos);
        boolean open = level.getBlockState(pos.above()).isAir();
        BlockState clean = state.is(Blocks.COARSE_DIRT) || open ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState();
        level.setBlock(pos, clean, Block.UPDATE_ALL);
        fluids.take(0, WATER_PER_BLOCK);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5,
                6, 0.3, 0.0, 0.3, 0.0);
        cursor++;
        return true;
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
