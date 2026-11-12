package com.factorycraft;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Builds FactoryCraft's creative tab and keeps its item ordering in one place. */
final class FactoryCreativeTab {
    private FactoryCreativeTab() {}

    static DeferredHolder<CreativeModeTab, CreativeModeTab> register() {
        return FactoryCraft.TABS.register("factorycraft", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.factorycraft"))
                .icon(() -> FactoryCraft.IRON_HAMMER.value().getDefaultInstance())
                .displayItems((parameters, output) -> {
                    output.accept(FactoryCraft.STONE_HAMMER.value());
                    output.accept(FactoryCraft.COPPER_HAMMER.value());
                    output.accept(FactoryCraft.IRON_HAMMER.value());
                    output.accept(FactoryCraft.DIAMOND_HAMMER.value());
                    output.accept(FactoryCraft.NETHERITE_HAMMER.value());
                    output.accept(FactoryCraft.STONE_DRILL.value());
                    output.accept(FactoryCraft.COPPER_DRILL.value());
                    output.accept(FactoryCraft.IRON_DRILL.value());
                    output.accept(FactoryCraft.DIAMOND_DRILL.value());
                    output.accept(FactoryCraft.NETHERITE_DRILL.value());
                    output.accept(FactoryCraft.STONE_CHAINSAW.value());
                    output.accept(FactoryCraft.COPPER_CHAINSAW.value());
                    output.accept(FactoryCraft.IRON_CHAINSAW.value());
                    output.accept(FactoryCraft.DIAMOND_CHAINSAW.value());
                    output.accept(FactoryCraft.NETHERITE_CHAINSAW.value());
                    output.accept(FactoryCraft.STONE_EXCAVATING_SHOVEL.value());
                    output.accept(FactoryCraft.COPPER_EXCAVATING_SHOVEL.value());
                    output.accept(FactoryCraft.IRON_EXCAVATING_SHOVEL.value());
                    output.accept(FactoryCraft.DIAMOND_EXCAVATING_SHOVEL.value());
                    output.accept(FactoryCraft.NETHERITE_EXCAVATING_SHOVEL.value());
                    output.accept(FactoryCraft.EXCAVATOR_ITEM.value());
                    output.accept(FactoryCraft.TUNNELER_ITEM.value());
                    output.accept(FactoryCraft.DUMP_TRUCK_ITEM.value());
                    output.accept(FactoryCraft.CARGO_WAGON_ITEM.value());
                    output.accept(FactoryCraft.PAVER_ITEM.value());
                    output.accept(FactoryCraft.HARVESTER_ITEM.value());
                    output.accept(FactoryCraft.CRANE_ITEM.value());
                    output.accept(FactoryCraft.CRANE_CONTROL.value());
                    output.accept(FactoryCraft.BULLDOZER_ITEM.value());
                    output.accept(FactoryCraft.TANKER_ITEM.value());
                    output.accept(FactoryCraft.FORESTER_ITEM.value());
                    output.accept(FactoryCraft.TOWER_CRANE_ITEM.value());
                    output.accept(FactoryCraft.GUIDE_BOOK.value());
                    output.accept(FactoryCraft.WRENCH.value());
                    output.accept(FactoryCraft.MECHANIC_BENCH_ITEM.value());
                    output.accept(FactoryCraft.PROTOTYPE_DRILL.value());
                    output.accept(FactoryCraft.INDUSTRIAL_CHAINSAW.value());
                    output.accept(FactoryCraft.TITAN_HAMMER.value());
                    output.accept(FactoryCraft.GENERATOR_ITEM.value());
                    output.accept(FactoryCraft.SOLAR_GENERATOR_ITEM.value());
                    output.accept(FactoryCraft.REPAIR_STATION_ITEM.value());
                    output.accept(FactoryCraft.BATTERY_ITEM.value());
                    output.accept(FactoryCraft.COPPER_COIL.value());
                    output.accept(FactoryCraft.COPPER_RADIATOR_ITEM.value());
                    output.accept(FactoryCraft.LIGHTNING_COLLECTOR_ITEM.value());
                    output.accept(FactoryCraft.WINDMILL_ITEM.value());
                    output.accept(FactoryCraft.GEOTHERMAL_ITEM.value());
                    output.accept(FactoryCraft.STEAM_TURBINE_ITEM.value());
                    output.accept(FactoryCraft.CHIMNEY_FILTER_ITEM.value());
                    output.accept(FactoryCraft.AIR_PURIFIER_ITEM.value());
                    output.accept(FactoryCraft.PLANT_INSPECTOR.value());
                    output.accept(FactoryCraft.JETPACK.value());
                    output.accept(FactoryCraft.MAGNET.value());
                    output.accept(FactoryCraft.GIANT_DRILL.value());
                    output.accept(FactoryCraft.INDUSTRIAL_CHEST_ITEM.value());
                    output.accept(FactoryCraft.TANK_WAGON_ITEM.value());
                    output.accept(FactoryCraft.LOADING_STATION_ITEM.value());
                    output.accept(FactoryCraft.UNLOADING_STATION_ITEM.value());
                    output.accept(FactoryCraft.ROUTE_BEACON_ITEM.value());
                    output.accept(FactoryCraft.HELICOPTER_ITEM.value());
                    output.accept(FactoryCraft.SUBMARINE_ITEM.value());
                    output.accept(FactoryCraft.ENERGY_METER.value());
                    output.accept(FactoryCraft.MACHINE_UPGRADE_SPEED.value());
                    output.accept(FactoryCraft.MACHINE_UPGRADE_EFFICIENCY.value());
                    output.accept(FactoryCraft.MACHINE_UPGRADE_OUTPUT.value());
                    output.accept(FactoryCraft.CABLE_ITEM.value());
                    output.accept(FactoryCraft.ORE_WASHER_ITEM.value());
                    output.accept(FactoryCraft.PRESS_ITEM.value());
                    output.accept(FactoryCraft.MIXER_ITEM.value());
                    output.accept(FactoryCraft.IRON_PLATE.value());
                    output.accept(FactoryCraft.COPPER_PLATE.value());
                    output.accept(FactoryCraft.GOLD_PLATE.value());
                    output.accept(FactoryCraft.IRON_GEAR.value());
                    output.accept(FactoryCraft.ROBOT_ARM_ITEM.value());
                    output.accept(FactoryCraft.PUMP_ITEM.value());
                    output.accept(FactoryCraft.FLUID_PIPE_ITEM.value());
                    output.accept(FactoryCraft.PACKAGER_ITEM.value());
                    output.accept(FactoryCraft.TANNERY_ITEM.value());
                    output.accept(FactoryCraft.INCINERATOR_ITEM.value());
                    output.accept(FactoryCraft.FILLER_ITEM.value());
                    output.accept(FactoryCraft.LEVELER_ITEM.value());
                    output.accept(FactoryCraft.QUARRY_ITEM.value());
                    output.accept(FactoryCraft.DRAIN_ITEM.value());
                    output.accept(FactoryCraft.PLANTER_ITEM.value());
                    output.accept(FactoryCraft.BUILDER_ITEM.value());
                    output.accept(FactoryCraft.BLUEPRINT.value());
                    output.accept(FactoryCraft.INDUSTRIAL_LAMP_ITEM.value());
                    output.accept(FactoryCraft.TOOL_FORGE_ITEM.value());
                    for (DeferredHolder<Item, Item> item : java.util.List.of(FactoryCraft.HYDRO_TURBINE_ITEM,
                            FactoryCraft.ADVANCED_SOLAR_ITEM, FactoryCraft.PHOTON_ARRAY_ITEM, FactoryCraft.PUMPJACK_ITEM,
                            Processing.CHEMICAL_BATH_ITEM, OilFields.OIL_DETECTOR, OilFields.OIL_SHALE_ITEM,
                            OilFields.TAR_SAND_ITEM, FactoryCraft.CRUDE_OIL, FactoryCraft.REFINERY_ITEM,
                            FactoryCraft.DIESEL, FactoryCraft.ASPHALT_ITEM, FactoryCraft.FERMENTER_ITEM,
                            FactoryCraft.BIOFUEL, FactoryCraft.WIRELESS_CHARGER_ITEM, FactoryCraft.GREENHOUSE_ITEM,
                            FactoryCraft.ANIMAL_FARM_ITEM, FactoryCraft.FLUID_TANK_ITEM, FactoryCraft.SEWER_GRATE_ITEM,
                            FactoryCraft.DESALINATOR_ITEM, FactoryCraft.SALT, FactoryCraft.SALT_BLOCK_ITEM,
                            FactoryCraft.TREATMENT_PLANT_ITEM)) {
                        output.accept(item.value());
                    }
                    for (DeferredHolder<Item, Item> mould : FactoryCraft.MOULDS.values()) output.accept(mould.value());
                    output.accept(Gear.EXOSKELETON.value());
                    output.accept(Gear.WORK_HELMET.value());
                    output.accept(Gear.WORK_JACKET.value());
                    output.accept(Gear.WORK_PANTS.value());
                    output.accept(Gear.WORK_BOOTS.value());
                    output.accept(Gear.JETPACK_MK2.value());
                    output.accept(FactoryCraft.LOCOMOTIVE_ITEM.value());
                    output.accept(FactoryCraft.DRONE_ITEM.value());
                    output.accept(FactoryCraft.DRONE_CONTROLLER.value());
                    output.accept(FactoryCraft.CONTROL_STATION_ITEM.value());
                    for (DeferredHolder<Item, Item> module : FactoryCraft.UPGRADE_ITEMS.values()) output.accept(module.value());
                    output.accept(FactoryCraft.CONVEYOR_ITEM.value());
                    output.accept(FactoryCraft.FAST_CONVEYOR_ITEM.value());
                    output.accept(FactoryCraft.EXPRESS_CONVEYOR_ITEM.value());
                    output.accept(FactoryCraft.SORTER_ITEM.value());
                    output.accept(FactoryCraft.ELEVATOR_ITEM.value());
                    output.accept(FactoryCraft.WELL_DRILL_ITEM.value());
                    output.accept(FactoryCraft.CRUSHER_ITEM.value());
                    output.accept(FactoryCraft.INDUSTRIAL_FURNACE_ITEM.value());
                    output.accept(FactoryCraft.IRON_DUST.value());
                    output.accept(FactoryCraft.GOLD_DUST.value());
                    output.accept(FactoryCraft.COPPER_DUST.value());
                })
                .build());
    }
}
