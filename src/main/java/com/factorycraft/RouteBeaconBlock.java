package com.factorycraft;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Route beacon for the autopilot. Each beacon has a number (1-16): vehicles
 * with the autopilot module drive 1 -> 2 -> 3 ... and back to 1. Right-click
 * to change the number, sneak + right-click to make it a stop (vehicles wait
 * 10 seconds there; dump trucks unload while they wait).
 */
public final class RouteBeaconBlock extends BaseEntityBlock {
    public static final MapCodec<RouteBeaconBlock> CODEC = simpleCodec(RouteBeaconBlock::new);
    public static final IntegerProperty INDEX = IntegerProperty.create("index", 1, 16);
    public static final BooleanProperty STOP = BooleanProperty.create("stop");
    private static final VoxelShape SHAPE = Shapes.or(Block.box(5, 0, 5, 11, 2, 11), Block.box(7, 2, 7, 9, 12, 9), Block.box(5.5, 12, 5.5, 10.5, 16, 10.5));

    public RouteBeaconBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(INDEX, 1).setValue(STOP, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(INDEX, STOP);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockState next;
        if (player.isSecondaryUseActive()) {
            next = state.setValue(STOP, !state.getValue(STOP));
        } else {
            next = state.setValue(INDEX, state.getValue(INDEX) % 16 + 1);
        }
        level.setBlock(pos, next, Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.6F, 0.5F + next.getValue(INDEX) * 0.09F);
        player.sendOverlayMessage(Component.translatable(next.getValue(STOP) ? "message.factorycraft.beacon.stop" : "message.factorycraft.beacon.pass",
                next.getValue(INDEX)));
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RouteBeaconBlockEntity(pos, state);
    }
}
