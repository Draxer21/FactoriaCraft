package com.factorycraft;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Conveyor belt. Moves items, mobs and players in the direction it faces
 * (away from the player who placed it) and feeds items into any container
 * (chest, barrel, hopper, furnace...) directly in front of its end.
 *
 * Slopes are automatic: place the next belt one block higher (or lower) and
 * one block ahead, facing the same way, and the joint becomes a 45° ramp.
 * Corners are automatic too: a belt fed only from one side shows a curve.
 */
public final class ConveyorBlock extends BaseEntityBlock {
    public static final MapCodec<ConveyorBlock> CODEC = simpleCodec(ConveyorBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<ConveyorSlope> SLOPE = EnumProperty.create("slope", ConveyorSlope.class);
    public static final EnumProperty<ConveyorTurn> TURN = EnumProperty.create("turn", ConveyorTurn.class);
    /** Thin base used for item collisions on ramps, so items glide up/down instead of hitting the steps. */
    private static final VoxelShape RAMP_ITEM_COLLISION = Block.box(0, 0, 0, 16, 2, 16);

    private static final VoxelShape FLAT_SHAPE = Block.box(0, 0, 0, 16, 4, 16);
    /** Stair-stepped ramp shapes, indexed by facing (2D data value), for UP and DOWN. */
    private static final VoxelShape[] UP_SHAPES = new VoxelShape[4];
    private static final VoxelShape[] DOWN_SHAPES = new VoxelShape[4];

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            UP_SHAPES[facing.get2DDataValue()] = rampShape(facing, true);
            DOWN_SHAPES[facing.get2DDataValue()] = rampShape(facing, false);
        }
    }

    /**
     * Eight 2 px steps following the belt surface (4 px high at the low end, 20 px
     * at the high end). Built for a north-facing belt and rotated to {@code facing}.
     */
    private static VoxelShape rampShape(Direction facing, boolean rising) {
        VoxelShape shape = Block.box(0, 0, 0, 16, 2, 16);
        for (int i = 0; i < 8; i++) {
            // Segment i counts from the low end of the ramp.
            double top = 4 + (i + 1) * 2;
            double z0 = rising ? 16 - (i + 1) * 2 : i * 2;
            double z1 = z0 + 2;
            shape = Shapes.or(shape, rotatedBox(facing, 0, 0, z0, 16, top, z1));
        }
        return shape;
    }

    /** A box given for a north-facing belt, rotated around the block centre to {@code facing}. */
    private static VoxelShape rotatedBox(Direction facing, double x0, double y0, double z0, double x1, double y1, double z1) {
        return switch (facing) {
            case SOUTH -> Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
            case EAST -> Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
            case WEST -> Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
            default -> Block.box(x0, y0, z0, x1, y1, z1);
        };
    }

    private final double speed;

    public ConveyorBlock(BlockBehaviour.Properties properties) {
        this(properties, ConveyorBlockEntity.SPEED);
    }

    /** {@code speed}: blocks per tick (normal 0.125, fast 0.25, express 0.4). */
    public ConveyorBlock(BlockBehaviour.Properties properties, double speed) {
        super(properties);
        this.speed = speed;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SLOPE, ConveyorSlope.FLAT)
                .setValue(TURN, ConveyorTurn.STRAIGHT));
    }

    @Override
    protected MapCodec<ConveyorBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SLOPE, TURN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return defaultBlockState().setValue(FACING, facing)
                .setValue(SLOPE, slopeFor(level, pos, facing))
                .setValue(TURN, turnFor(level, pos, facing));
    }

    /**
     * A belt is a curve when nothing feeds it from behind but exactly one belt on
     * its left or right side points into it.
     */
    public static ConveyorTurn turnFor(BlockGetter level, BlockPos pos, Direction facing) {
        if (isBelt(level, pos.relative(facing.getOpposite()), facing)) return ConveyorTurn.STRAIGHT;
        Direction left = facing.getCounterClockWise();
        Direction right = facing.getClockWise();
        // A belt on our left feeds us if it points right (toward us), and vice versa.
        boolean fromLeft = isBelt(level, pos.relative(left), right);
        boolean fromRight = isBelt(level, pos.relative(right), left);
        if (fromLeft == fromRight) return ConveyorTurn.STRAIGHT;
        return fromLeft ? ConveyorTurn.FROM_LEFT : ConveyorTurn.FROM_RIGHT;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState,
                                     RandomSource random) {
        if (directionToNeighbour.getAxis().isHorizontal()) {
            return state.setValue(TURN, turnFor(level, pos, state.getValue(FACING)));
        }
        return state;
    }

    /** UP if the next belt is one block higher, DOWN if the previous one is, otherwise FLAT. */
    public static ConveyorSlope slopeFor(BlockGetter level, BlockPos pos, Direction facing) {
        if (isBelt(level, pos.relative(facing).above(), facing)) return ConveyorSlope.UP;
        if (isBelt(level, pos.relative(facing.getOpposite()).above(), facing)) return ConveyorSlope.DOWN;
        return ConveyorSlope.FLAT;
    }

    private static boolean isBelt(BlockGetter level, BlockPos pos, Direction facing) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof ConveyorBlock && state.getValue(FACING) == facing;
    }

    /** Re-evaluates the slope of a belt whose diagonal neighbours may have changed. */
    private static void refresh(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ConveyorBlock)) return;
        ConveyorSlope slope = slopeFor(level, pos, state.getValue(FACING));
        if (state.getValue(SLOPE) != slope) {
            level.setBlock(pos, state.setValue(SLOPE, slope), Block.UPDATE_ALL);
        }
    }

    /** Belts below-behind and below-ahead depend on this one (diagonals get no shape updates). */
    private static void refreshDiagonals(Level level, BlockPos pos, Direction facing) {
        refresh(level, pos.relative(facing.getOpposite()).below());
        refresh(level, pos.relative(facing).below());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(this)) {
            refreshDiagonals(level, pos, state.getValue(FACING));
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        refreshDiagonals(level, pos, state.getValue(FACING));
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int index = state.getValue(FACING).get2DDataValue();
        return switch (state.getValue(SLOPE)) {
            case FLAT -> FLAT_SHAPE;
            case UP -> UP_SHAPES[index];
            case DOWN -> DOWN_SHAPES[index];
        };
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(SLOPE) != ConveyorSlope.FLAT
                && context instanceof EntityCollisionContext entityContext
                && entityContext.getEntity() instanceof ItemEntity) {
            return RAMP_ITEM_COLLISION;
        }
        return getShape(state, level, pos, context);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConveyorBlockEntity(pos, state);
    }

    public double speed() {
        return speed;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, FactoryCraft.CONVEYOR_BLOCK_ENTITY.value(), ConveyorBlockEntity::tick);
    }
}
