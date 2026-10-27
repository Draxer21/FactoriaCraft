package com.factorycraft;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Steel scaffolding: right-click a scaffold with more scaffolding to build the
 * column straight up (up to 64 high). Climb inside it holding jump; you can
 * stand on its top deck. Breaking one brings down everything above it.
 */
public final class SteelScaffoldBlock extends Block {
    public static final MapCodec<SteelScaffoldBlock> CODEC = simpleCodec(SteelScaffoldBlock::new);
    private static final int MAX_HEIGHT = 64;
    private static final VoxelShape LEGS = Shapes.or(
            Block.box(0, 0, 0, 2, 16, 2), Block.box(14, 0, 0, 16, 16, 2),
            Block.box(0, 0, 14, 2, 16, 16), Block.box(14, 0, 14, 16, 16, 16));
    private static final VoxelShape DECK = Shapes.or(LEGS, Block.box(0, 14, 0, 16, 16, 16));

    public SteelScaffoldBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<SteelScaffoldBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return DECK;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Solid deck only for whoever is above it and not sneaking down; inside the column you climb.
        if (context.isAbove(Shapes.block(), pos, true) && !context.isDescending()) return DECK;
        return LEGS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(asItem())) return InteractionResult.TRY_WITH_EMPTY_HAND;
        BlockPos top = pos;
        for (int i = 0; i < MAX_HEIGHT && level.getBlockState(top.above()).is(this); i++) top = top.above();
        BlockPos place = top.above();
        if (place.getY() >= level.getMaxY() || !level.getBlockState(place).canBeReplaced()) return InteractionResult.FAIL;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        level.setBlock(place, defaultBlockState(), Block.UPDATE_ALL);
        level.playSound(null, place, SoundEvents.SCAFFOLDING_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
        if (!player.isCreative()) stack.shrink(1);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            for (int i = 1; i <= MAX_HEIGHT && level.getBlockState(pos.above(i)).is(this); i++) {
                level.destroyBlock(pos.above(i), !player.isCreative());
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
