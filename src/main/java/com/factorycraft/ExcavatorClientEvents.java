package com.factorycraft;

import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = FactoryCraft.MOD_ID, value = Dist.CLIENT)
public final class ExcavatorClientEvents {
    private ExcavatorClientEvents() {}

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ExcavatorModel.LAYER_LOCATION, ExcavatorModel::createLayer);
        event.registerLayerDefinition(TunnelerModel.LAYER_LOCATION, TunnelerModel::createLayer);
        event.registerLayerDefinition(DumpTruckModel.LAYER_LOCATION, DumpTruckModel::createLayer);
        event.registerLayerDefinition(CargoWagonModel.LAYER_LOCATION, CargoWagonModel::createLayer);
        event.registerLayerDefinition(PaverModel.LAYER_LOCATION, PaverModel::createLayer);
        event.registerLayerDefinition(HarvesterModel.LAYER_LOCATION, HarvesterModel::createLayer);
        event.registerLayerDefinition(CraneModel.LAYER_LOCATION, CraneModel::createLayer);
        event.registerLayerDefinition(BulldozerModel.LAYER_LOCATION, BulldozerModel::createLayer);
        event.registerLayerDefinition(TankerModel.LAYER_LOCATION, TankerModel::createLayer);
        event.registerLayerDefinition(ForesterModel.LAYER_LOCATION, ForesterModel::createLayer);
        event.registerLayerDefinition(TowerCraneModel.LAYER_LOCATION, TowerCraneModel::createLayer);
        event.registerLayerDefinition(HelicopterModel.LAYER_LOCATION, HelicopterModel::createLayer);
        event.registerLayerDefinition(SubmarineModel.LAYER_LOCATION, SubmarineModel::createLayer);
        event.registerLayerDefinition(LocomotiveModel.LAYER_LOCATION, LocomotiveModel::createLayer);
        event.registerLayerDefinition(DroneModel.LAYER_LOCATION, DroneModel::createLayer);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(FactoryCraft.DUMP_TRUCK_MENU.value(), DumpTruckScreen::new);
        event.register(FactoryCraft.CRUSHER_MENU.value(), ProcessingScreen::new);
        event.register(FactoryCraft.INDUSTRIAL_FURNACE_MENU.value(), ProcessingScreen::new);
        event.register(FactoryCraft.GENERATOR_MENU.value(), GeneratorScreen::new);
        event.register(FactoryCraft.SOLAR_GENERATOR_MENU.value(), GeneratorScreen::new);
        event.register(FactoryCraft.BATTERY_MENU.value(), GeneratorScreen::new);
        event.register(FactoryCraft.PASSIVE_GENERATOR_MENU.value(), GeneratorScreen::new);
        event.register(FactoryCraft.INDUSTRIAL_CHEST_MENU.value(), IndustrialChestScreen::new);
        event.register(Nuclear.REACTOR_MENU.value(), ReactorScreen::new);
        event.register(Nuclear.CENTRIFUGE_MENU.value(), ProcessingScreen::new);
        event.register(FactoryCraft.REPAIR_STATION_MENU.value(), ProcessingScreen::new);
        event.register(FactoryCraft.VEHICLE_MENU.value(), VehicleScreen::new);
        event.register(FactoryCraft.ORE_WASHER_MENU.value(), ProcessingScreen::new);
        event.register(FactoryCraft.PRESS_MENU.value(), ProcessingScreen::new);
        event.register(FactoryCraft.MIXER_MENU.value(), ProcessingScreen::new);
        event.register(FactoryCraft.PACKAGER_MENU.value(), ProcessingScreen::new);
        event.register(FactoryCraft.TANNERY_MENU.value(), ProcessingScreen::new);
        event.register(FactoryCraft.INCINERATOR_MENU.value(), ProcessingScreen::new);
        event.register(FactoryCraft.FILLER_MENU.value(), ProcessingScreen::new);
        event.register(Materials.ALLOY_SMELTER_MENU.value(), ProcessingScreen::new);
        for (var menu : java.util.List.of(FactoryCraft.LEVELER_MENU, FactoryCraft.QUARRY_MENU, FactoryCraft.DRAIN_MENU,
                FactoryCraft.PLANTER_MENU, FactoryCraft.BUILDER_MENU)) {
            event.register(menu.value(), ProcessingScreen::new);
        }
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(FactoryCraft.EXCAVATOR.value(), ExcavatorRenderer::new);
        event.registerEntityRenderer(FactoryCraft.TUNNELER.value(), TunnelerRenderer::new);
        event.registerEntityRenderer(FactoryCraft.DUMP_TRUCK.value(), DumpTruckRenderer::new);
        event.registerEntityRenderer(FactoryCraft.CARGO_WAGON.value(), CargoWagonRenderer::new);
        event.registerEntityRenderer(FactoryCraft.PAVER.value(), context -> new WorkVehicleRenderer(context, "paver",
                PaverModel.LAYER_LOCATION, 1.5F, PaverModel::new, PaverModel.LAMP_PARTS, PaverModel.BEAM_ORIGINS));
        event.registerEntityRenderer(FactoryCraft.HARVESTER.value(), context -> new WorkVehicleRenderer(context, "harvester",
                HarvesterModel.LAYER_LOCATION, 1.5F, HarvesterModel::new, HarvesterModel.LAMP_PARTS, HarvesterModel.BEAM_ORIGINS));
        event.registerEntityRenderer(FactoryCraft.CRANE.value(), CraneRenderer::new);
        event.registerEntityRenderer(FactoryCraft.TOWER_CRANE.value(), TowerCraneRenderer::new);
        event.registerEntityRenderer(FactoryCraft.HELICOPTER.value(), context -> new CraneRenderer(context, "helicopter",
                HelicopterModel.LAYER_LOCATION, HelicopterModel::new, HelicopterModel.LAMP_PARTS, HelicopterModel.BEAM_ORIGINS));
        event.registerEntityRenderer(FactoryCraft.SUBMARINE.value(), context -> new WorkVehicleRenderer(context, "submarine",
                SubmarineModel.LAYER_LOCATION, 1.5F, SubmarineModel::new, SubmarineModel.LAMP_PARTS, SubmarineModel.BEAM_ORIGINS));
        event.registerEntityRenderer(FactoryCraft.TANK_WAGON.value(), context -> new CargoWagonRenderer(context, "tank_wagon"));
        event.registerEntityRenderer(FactoryCraft.LOCOMOTIVE.value(), context -> new WorkVehicleRenderer(context, "locomotive",
                LocomotiveModel.LAYER_LOCATION, 1.5F, LocomotiveModel::new, LocomotiveModel.LAMP_PARTS, LocomotiveModel.BEAM_ORIGINS));
        event.registerEntityRenderer(FactoryCraft.DRONE.value(), DroneRenderer::new);
        event.registerEntityRenderer(Nuclear.MINI_NUKE_ENTITY.value(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(FactoryCraft.ORE_MARKER.value(), OreMarkerRenderer::new);
        event.registerEntityRenderer(FactoryCraft.FORESTER.value(), context -> new WorkVehicleRenderer(context, "forester",
                ForesterModel.LAYER_LOCATION, 1.5F, ForesterModel::new, ForesterModel.LAMP_PARTS, ForesterModel.BEAM_ORIGINS));
        event.registerEntityRenderer(FactoryCraft.BULLDOZER.value(), context -> new WorkVehicleRenderer(context, "bulldozer",
                BulldozerModel.LAYER_LOCATION, 1.5F, BulldozerModel::new, BulldozerModel.LAMP_PARTS, BulldozerModel.BEAM_ORIGINS));
        event.registerEntityRenderer(FactoryCraft.TANKER.value(), context -> new WorkVehicleRenderer(context, "tanker",
                TankerModel.LAYER_LOCATION, 1.5F, TankerModel::new, TankerModel.LAMP_PARTS, TankerModel.BEAM_ORIGINS));
    }

    /** Storage blocks use the vanilla iron block texture, tinted per material. */
    @SubscribeEvent
    public static void blockColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.BlockTintSources event) {
        tint(event, 0x7CFF4A, Nuclear.URANIUM_BLOCK.value());
        tint(event, 0xE2D29A, Nuclear.THORIUM_BLOCK.value());
        tint(event, 0x8E98C4, Nuclear.LEAD_BLOCK.value());
        tint(event, 0xE884E8, Nuclear.PLUTONIUM_BLOCK.value());
        for (Materials.Metal m : Materials.METALS) tint(event, Materials.COLORS.get(m.name()), m.block().value());
        for (Materials.Alloy a : Materials.ALLOYS) tint(event, Materials.COLORS.get(a.name()), a.block().value());
        tint(event, Materials.COLORS.get("cobalt"), Exotic.COBALT_BLOCK.value());
        tint(event, Materials.COLORS.get("voidnite"), Exotic.VOIDNITE_BLOCK.value());
    }

    private static void tint(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.BlockTintSources event,
                             int rgb, net.minecraft.world.level.block.Block block) {
        net.minecraft.client.color.block.BlockTintSource src = state -> 0xFF000000 | rgb;
        event.register(java.util.List.of(src), block);
    }
}
