package com.factorycraft;

import java.util.function.Supplier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Fluid tank: a gauge window on every side shows how full it is (FILL 0-8)
 * and what it holds (CONTENT, which picks the gauge's colour).
 */
public final class FluidTankBlock extends SimpleMachineBlock {
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 8);
    public static final EnumProperty<Content> CONTENT = EnumProperty.create("content", Content.class);

    public enum Content implements StringRepresentable {
        EMPTY(0x000000), WATER(0x3F76E4), LAVA(0xFF7A1A), CRUDE(0x2A2018), DIESEL(0xD8B040), BIOFUEL(0x7AB83A), OTHER(0xB0B0B0);

        public final int color;

        Content(int color) {
            this.color = color;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }

        static Content of(Fluid fluid) {
            if (fluid == Fluids.EMPTY) return EMPTY;
            if (fluid.isSame(Fluids.WATER)) return WATER;
            if (fluid.isSame(Fluids.LAVA)) return LAVA;
            if (fluid == Fuels.CRUDE_OIL.value()) return CRUDE;
            if (fluid == Fuels.DIESEL.value()) return DIESEL;
            if (fluid == Fuels.BIOFUEL.value()) return BIOFUEL;
            return OTHER;
        }
    }

    public FluidTankBlock(BlockBehaviour.Properties properties,
                          Supplier<? extends BlockEntityType<? extends MachineBlockEntity>> type) {
        super(properties, type);
        registerDefaultState(defaultBlockState().setValue(FILL, 0).setValue(CONTENT, Content.EMPTY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FILL, CONTENT);
    }
}
