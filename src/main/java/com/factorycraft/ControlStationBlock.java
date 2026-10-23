package com.factorycraft;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractChestBoat;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Control station: right-click to see every machine within 256 blocks (fuel,
 * engine, cargo, where it is) and every generator within 64 blocks (charge).
 */
public final class ControlStationBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ControlStationBlock> CODEC = simpleCodec(ControlStationBlock::new);
    private static final double VEHICLE_RANGE = 256.0;
    private static final int GENERATOR_CHUNKS = 4;

    public ControlStationBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected MapCodec<ControlStationBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Screen faces the player.
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new StationDataPayload(collect(server, pos)));
        }
        return InteractionResult.SUCCESS;
    }

    private static List<StationDataPayload.Line> collect(ServerLevel level, BlockPos pos) {
        List<StationDataPayload.Line> lines = new ArrayList<>();
        List<Entity> machines = level.getEntities((Entity) null, new AABB(pos).inflate(VEHICLE_RANGE),
                e -> e instanceof VehicleEngine.Holder && e.isAlive());
        machines.sort(Comparator.comparingDouble(e -> e.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))));
        for (Entity entity : machines) {
            VehicleEngine engine = ((VehicleEngine.Holder) entity).engine();
            int fill = -1;
            if (entity instanceof AbstractChestBoat bay && bay.getContainerSize() > 0) {
                int used = 0;
                for (int i = 0; i < bay.getContainerSize(); i++) if (!bay.getItem(i).isEmpty()) used++;
                fill = Math.round(100.0F * used / bay.getContainerSize());
            }
            BlockPos at = entity.blockPosition();
            lines.add(new StationDataPayload.Line(entity.getType().getDescriptionId(), at.getX(), at.getY(), at.getZ(),
                    Math.round(engine.getFuelFraction() * 100), engine.isRunning(), fill));
        }
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        for (int dx = -GENERATOR_CHUNKS; dx <= GENERATOR_CHUNKS; dx++) {
            for (int dz = -GENERATOR_CHUNKS; dz <= GENERATOR_CHUNKS; dz++) {
                if (!level.hasChunk(cx + dx, cz + dz)) continue;
                LevelChunk chunk = level.getChunk(cx + dx, cz + dz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof GeneratorBlockEntity generator)) continue;
                    BlockPos at = be.getBlockPos();
                    int charge = Math.round(100.0F * generator.stored() / generator.capacity());
                    lines.add(new StationDataPayload.Line("block.factorycraft.generator", at.getX(), at.getY(), at.getZ(),
                            charge, be.getBlockState().getValue(MachineBlock.LIT), -1));
                }
            }
        }
        return lines.size() > 256 ? lines.subList(0, 256) : lines;
    }
}
