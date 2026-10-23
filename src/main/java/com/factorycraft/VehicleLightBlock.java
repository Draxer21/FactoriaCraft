package com.factorycraft;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Invisible, air-like light source placed by vehicle headlights. It checks on
 * itself every second and disappears if no nearby vehicle still owns it, so
 * stray lights never stay behind (e.g. after a crash or server restart).
 */
public final class VehicleLightBlock extends Block {
    public static final MapCodec<VehicleLightBlock> CODEC = simpleCodec(VehicleLightBlock::new);
    private static final int CHECK_INTERVAL = 20;
    private static final double OWNER_SEARCH_RADIUS = 24.0;

    public VehicleLightBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<VehicleLightBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        level.scheduleTick(pos, this, CHECK_INTERVAL);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        AABB area = new AABB(pos).inflate(OWNER_SEARCH_RADIUS);
        boolean owned = !level.getEntitiesOfClass(Entity.class, area,
                e -> e instanceof VehicleLights.Holder holder && holder.lights().owns(pos)).isEmpty();
        if (owned) {
            level.scheduleTick(pos, this, CHECK_INTERVAL);
        } else {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
