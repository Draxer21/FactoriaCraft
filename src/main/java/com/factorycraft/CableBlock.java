package com.factorycraft;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** Power cable: connects to other cables, generators and powered machines on all six sides. */
public final class CableBlock extends PipeBlock {
    public static final MapCodec<CableBlock> CODEC = simpleCodec(CableBlock::new);

    public CableBlock(BlockBehaviour.Properties properties) {
        super(6.0F, properties);
        BlockState state = stateDefinition.any();
        for (Direction dir : Direction.values()) state = state.setValue(PROPERTY_BY_DIRECTION.get(dir), false);
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<CableBlock> codec() {
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
            state = state.setValue(PROPERTY_BY_DIRECTION.get(dir),
                    connects(context.getLevel(), context.getClickedPos().relative(dir)));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connects(level, neighbourPos));
    }

    private static boolean connects(BlockGetter level, BlockPos pos) {
        if (level.getBlockState(pos).getBlock() instanceof CableBlock) return true;
        var be = level.getBlockEntity(pos);
        return be != null && Energy.connectsTo(be);
    }
}
