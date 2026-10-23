package com.factorycraft;

import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** A machine block whose behaviour is all in its block entity (generator, washer, press, mixer...). */
public final class SimpleMachineBlock extends MachineBlock {
    private final Supplier<? extends BlockEntityType<? extends MachineBlockEntity>> type;
    private final MapCodec<SimpleMachineBlock> codec;

    public SimpleMachineBlock(BlockBehaviour.Properties properties,
                              Supplier<? extends BlockEntityType<? extends MachineBlockEntity>> type) {
        super(properties);
        this.type = type;
        this.codec = MapCodec.unit(this);
    }

    @Override
    protected MapCodec<SimpleMachineBlock> codec() {
        return codec;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return type.get().create(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide() || blockEntityType != type.get()) return null;
        return (lvl, pos, st, be) -> MachineBlockEntity.serverTick(lvl, pos, st, (MachineBlockEntity) be);
    }
}
