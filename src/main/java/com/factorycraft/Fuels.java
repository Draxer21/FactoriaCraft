package com.factorycraft;

import static com.factorycraft.FactoryCraft.MOD_ID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Liquid fuels: crude oil, diesel and biofuel as real fluids, so they can go
 * through fluid pipes and tanks (and other mods' pipes). They only exist inside
 * tanks and machines: there is no world block or bucket. 1000 mB = one canister.
 */
public final class Fuels {
    private Fuels() {}

    static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, MOD_ID);
    static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, MOD_ID);

    static void register(IEventBus bus) {
        FLUID_TYPES.register(bus);
        FLUIDS.register(bus);
    }

    private static DeferredHolder<FluidType, FluidType> type(String name, int density, int viscosity) {
        return FLUID_TYPES.register(name, () -> new FluidType(FluidType.Properties.create()
                .descriptionId("fluid_type." + MOD_ID + "." + name).density(density).viscosity(viscosity)));
    }

    public static final DeferredHolder<FluidType, FluidType> CRUDE_OIL_TYPE = type("crude_oil", 900, 3000);
    public static final DeferredHolder<FluidType, FluidType> DIESEL_TYPE = type("diesel", 830, 1500);
    public static final DeferredHolder<FluidType, FluidType> BIOFUEL_TYPE = type("biofuel", 880, 1500);

    public static final DeferredHolder<Fluid, Fluid> CRUDE_OIL = FLUIDS.register("crude_oil", () -> new FuelFluid(CRUDE_OIL_TYPE));
    public static final DeferredHolder<Fluid, Fluid> DIESEL = FLUIDS.register("diesel", () -> new FuelFluid(DIESEL_TYPE));
    public static final DeferredHolder<Fluid, Fluid> BIOFUEL = FLUIDS.register("biofuel", () -> new FuelFluid(BIOFUEL_TYPE));

    /** Fuel a generator can burn. */
    public static boolean burnable(Fluid fluid) {
        return fluid == DIESEL.value() || fluid == BIOFUEL.value();
    }

    /** The canister item that matches a fuel fluid (for burn time). */
    public static Item canister(Fluid fluid) {
        if (fluid == DIESEL.value()) return FactoryCraft.DIESEL.value();
        if (fluid == BIOFUEL.value()) return FactoryCraft.BIOFUEL.value();
        if (fluid == CRUDE_OIL.value()) return FactoryCraft.CRUDE_OIL.value();
        return Items.AIR;
    }

    /** A source-only fluid that never exists in the world. */
    static final class FuelFluid extends Fluid {
        private final DeferredHolder<FluidType, FluidType> type;

        FuelFluid(DeferredHolder<FluidType, FluidType> type) {
            this.type = type;
        }

        @Override public FluidType getFluidType() { return type.value(); }
        @Override public Item getBucket() { return Items.AIR; }
        @Override protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) { return true; }
        @Override protected Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState state) { return Vec3.ZERO; }
        @Override public int getTickDelay(LevelReader level) { return 5; }
        @Override protected float getExplosionResistance() { return 100.0F; }
        @Override protected BlockState createLegacyBlock(FluidState state) { return Blocks.AIR.defaultBlockState(); }
        @Override public boolean isSource(FluidState state) { return true; }
        @Override public int getAmount(FluidState state) { return 8; }
        @Override public float getHeight(FluidState state, BlockGetter level, BlockPos pos) { return 1.0F; }
        @Override public float getOwnHeight(FluidState state) { return 1.0F; }
        @Override public VoxelShape getShape(FluidState state, BlockGetter level, BlockPos pos) { return Shapes.block(); }
    }
}
