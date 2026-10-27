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

/** Data cable: links a storage terminal to chests, barrels and other inventories (any mod's). */
public final class DataCableBlock extends PipeBlock {
    public static final MapCodec<DataCableBlock> CODEC = simpleCodec(DataCableBlock::new);

    public DataCableBlock(BlockBehaviour.Properties properties) {
        super(6.0F, properties);
        BlockState state = stateDefinition.any();
        for (Direction dir : Direction.values()) state = state.setValue(PROPERTY_BY_DIRECTION.get(dir), false);
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<DataCableBlock> codec() {
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
        if (level.getBlockState(pos).getBlock() instanceof DataCableBlock) return true;
        return level.getBlockEntity(pos) != null;
    }
}
