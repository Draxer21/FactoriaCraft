package com.factorycraft;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** Fluid pipe: links a pump to cauldrons, generators (lava) and tanker trucks parked against it. */
public final class FluidPipeBlock extends PipeBlock {
    public static final MapCodec<FluidPipeBlock> CODEC = simpleCodec(FluidPipeBlock::new);

    public FluidPipeBlock(BlockBehaviour.Properties properties) {
        super(6.0F, properties);
        BlockState state = stateDefinition.any();
        for (Direction dir : Direction.values()) state = state.setValue(PROPERTY_BY_DIRECTION.get(dir), false);
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<FluidPipeBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction dir : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(dir), connects(context.getLevel(), context.getClickedPos().relative(dir)));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connects(level, neighbourPos));
    }

    static boolean connects(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof FluidPipeBlock || state.getBlock() instanceof AbstractCauldronBlock
                || level.getBlockEntity(pos) instanceof FluidStorage.Holder || level.getBlockEntity(pos) instanceof PumpBlockEntity || level.getBlockEntity(pos) instanceof GeneratorBlockEntity;
    }
}
