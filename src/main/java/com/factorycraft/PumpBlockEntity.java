package com.factorycraft;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * Electric pump. Place it touching water or lava (usually on top): it pumps out
 * the whole connected body of fluid, top layer first, one bucket every half
 * second (100 energy each). The fluid goes through pipes to empty cauldrons,
 * generators (lava = fuel) or a tanker truck parked against a pipe.
 * Right-click shows its tank; sneak + right-click switches to DRAIN mode,
 * where the fluid is simply disposed of (to empty flooded caves).
 */
public final class PumpBlockEntity extends MachineBlockEntity implements Energy.Consumer {
    private static final int CAPACITY = 16;
    private static final int INTERVAL = 10;
    private static final int ENERGY_PER_BUCKET = 100;
    private static final int SEARCH_LIMIT = 4096;
    private static final int SEARCH_RADIUS = 32;
    private static final int MAX_PIPES = 512;

    private final Energy.Link link = new Energy.Link(this);
    private TankerEntity.Fluid fluid = TankerEntity.Fluid.NONE;
    private int amount;
    private boolean drainMode;

    public PumpBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryCraft.PUMP_BLOCK_ENTITY.value(), pos, state, 0);
    }

    @Override protected boolean isInputSlot(int slot) { return false; }
    @Override protected boolean isOutputSlot(int slot) { return false; }
    @Override protected boolean accepts(int slot, ItemStack stack) { return false; }

    public void click(Player player) {
        if (player.isShiftKeyDown()) {
            drainMode = !drainMode;
            setChanged();
        }
        Component fluidName = Component.translatable("hud.factorycraft.tanker.fluid." + fluid.name().toLowerCase());
        Component mode = Component.translatable(drainMode ? "message.factorycraft.pump.drain" : "message.factorycraft.pump.pipes");
        player.sendOverlayMessage(Component.translatable("message.factorycraft.pump.status", fluidName, amount, CAPACITY, mode));
    }

    @Override
    protected boolean work(Level level) {
        if (level.getGameTime() % INTERVAL != 0) return getBlockState().getValue(MachineBlock.LIT);
        boolean busy = false;
        if (amount < CAPACITY && link.draw(level, ENERGY_PER_BUCKET) >= ENERGY_PER_BUCKET) busy = pumpOne(level);
        if (amount > 0) busy |= drainMode ? dispose() : deliver(level);
        return busy;
    }

    // ---------------------------------------------------------------- pumping

    private static TankerEntity.Fluid typeOf(FluidState state) {
        if (state.isEmpty()) return TankerEntity.Fluid.NONE;
        if (state.getType() == Fluids.WATER || state.getType() == Fluids.FLOWING_WATER) return TankerEntity.Fluid.WATER;
        if (state.getType() == Fluids.LAVA || state.getType() == Fluids.FLOWING_LAVA) return TankerEntity.Fluid.LAVA;
        return TankerEntity.Fluid.NONE;
    }

    /** Removes one source block of the connected fluid, highest and farthest first. */
    private boolean pumpOne(Level level) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        TankerEntity.Fluid want = amount > 0 ? fluid : TankerEntity.Fluid.NONE;
        for (Direction dir : Direction.values()) {
            BlockPos start = worldPosition.relative(dir);
            TankerEntity.Fluid type = typeOf(level.getFluidState(start));
            if (type == TankerEntity.Fluid.NONE || (want != TankerEntity.Fluid.NONE && type != want)) continue;
            want = type;
            seen.add(start);
            queue.add(start);
        }
        if (queue.isEmpty()) return false;
        BlockPos best = null;
        while (!queue.isEmpty() && seen.size() < SEARCH_LIMIT) {
            BlockPos pos = queue.poll();
            FluidState state = level.getFluidState(pos);
            if (state.isSource() && (best == null || pos.getY() > best.getY()
                    || pos.getY() == best.getY() && pos.distSqr(worldPosition) > best.distSqr(worldPosition))) {
                best = pos;
            }
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (seen.contains(next) || next.distManhattan(worldPosition) > SEARCH_RADIUS) continue;
                if (typeOf(level.getFluidState(next)) != want) continue;
                seen.add(next);
                queue.add(next);
            }
        }
        if (best == null) return false;
        BlockState state = level.getBlockState(best);
        if (state.getBlock() instanceof LiquidBlock) {
            level.setBlock(best, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        } else if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            level.setBlock(best, state.setValue(BlockStateProperties.WATERLOGGED, false), Block.UPDATE_ALL);
        } else {
            return false;
        }
        fluid = want;
        amount++;
        setChanged();
        if (level.getGameTime() % 40 == 0) {
            level.playSound(null, worldPosition, want == TankerEntity.Fluid.LAVA ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL,
                    SoundSource.BLOCKS, 0.4F, 0.7F);
        }
        return true;
    }

    private boolean dispose() {
        take();
        return true;
    }

    private void take() {
        if (--amount <= 0) {
            amount = 0;
            fluid = TankerEntity.Fluid.NONE;
        }
        setChanged();
    }

    // ---------------------------------------------------------------- delivering

    /** Sends one bucket down the pipes to the first place that takes it. */
    private boolean deliver(Level level) {
        List<BlockPos> pipes = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(worldPosition);
        queue.add(worldPosition);
        while (!queue.isEmpty() && pipes.size() < MAX_PIPES) {
            BlockPos pos = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (!seen.add(next)) continue;
                BlockState state = level.getBlockState(next);
                if (state.getBlock() instanceof FluidPipeBlock) {
                    pipes.add(next);
                    queue.add(next);
                } else if (offerToBlock(level, next, state)) {
                    take();
                    return true;
                }
            }
        }
        // Tanker trucks and tank wagons parked against any pipe.
        for (BlockPos pipe : pipes) {
            for (TankWagonEntity wagon : level.getEntitiesOfClass(TankWagonEntity.class, new AABB(pipe).inflate(1.5))) {
                if (wagon.receive(fluid)) {
                    take();
                    return true;
                }
            }
            for (TankerEntity tanker : level.getEntitiesOfClass(TankerEntity.class, new AABB(pipe).inflate(1.0))) {
                if (tanker.receive(fluid)) {
                    take();
                    return true;
                }
            }
        }
        return false;
    }

    private boolean offerToBlock(Level level, BlockPos pos, BlockState state) {
        if (state.is(Blocks.CAULDRON)) {
            BlockState full = fluid == TankerEntity.Fluid.LAVA ? Blocks.LAVA_CAULDRON.defaultBlockState()
                    : Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3);
            level.setBlock(pos, full, Block.UPDATE_ALL);
            return true;
        }
        if (fluid == TankerEntity.Fluid.WATER && level.getBlockEntity(pos) instanceof NuclearReactorBlockEntity reactor) {
            return reactor.acceptWater();
        }
        return fluid == TankerEntity.Fluid.LAVA && level.getBlockEntity(pos) instanceof GeneratorBlockEntity generator
                && generator.acceptLava();
    }

    // ---------------------------------------------------------------- saving

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fluid = TankerEntity.Fluid.values()[Math.min(input.getByteOr("Fluid", (byte) 0), 2)];
        amount = input.getIntOr("Amount", 0);
        drainMode = input.getBooleanOr("Drain", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putByte("Fluid", (byte) fluid.ordinal());
        output.putInt("Amount", amount);
        output.putBoolean("Drain", drainMode);
    }
}
