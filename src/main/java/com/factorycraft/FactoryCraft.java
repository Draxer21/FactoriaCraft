package com.factorycraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(FactoryCraft.MOD_ID)
public final class FactoryCraft {
    public static final String MOD_ID = "factorycraft";
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    public static final DeferredRegister<net.minecraft.world.entity.ai.village.poi.PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, MOD_ID);
    public static final DeferredRegister<net.minecraft.world.entity.npc.villager.VillagerProfession> PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, MOD_ID);
    public static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);

    // Machine sounds (procedurally generated .ogg files in assets/factorycraft/sounds).
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> ENGINE_LOOP = sound("engine_loop");
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> ENGINE_START = sound("engine_start");
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> ENGINE_STOP = sound("engine_stop");
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> TRACKS_LOOP = sound("tracks_loop");
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> DRILL_LOOP = sound("drill_loop");
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> CRANE_WINCH = sound("crane_winch");
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> HYDRAULIC = sound("hydraulic");

    private static DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> sound(String name) {
        return SOUND_EVENTS.register(name, () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(
                Identifier.fromNamespaceAndPath(MOD_ID, name)));
    }

    /** Conveyor belt: moves items/mobs/players and feeds containers at its end. */
    public static final DeferredHolder<Block, Block> CONVEYOR = BLOCKS.register("conveyor", () ->
            new ConveyorBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "conveyor")))
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));
    public static final DeferredHolder<Item, Item> CONVEYOR_ITEM = ITEMS.registerItem("conveyor",
            properties -> new BlockItem(CONVEYOR.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<Block, Block> FAST_CONVEYOR = conveyor("fast_conveyor", 0.25);
    public static final DeferredHolder<Block, Block> EXPRESS_CONVEYOR = conveyor("express_conveyor", 0.4);
    public static final DeferredHolder<Item, Item> FAST_CONVEYOR_ITEM = ITEMS.registerItem("fast_conveyor",
            properties -> new BlockItem(FAST_CONVEYOR.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<Item, Item> EXPRESS_CONVEYOR_ITEM = ITEMS.registerItem("express_conveyor",
            properties -> new BlockItem(EXPRESS_CONVEYOR.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConveyorBlockEntity>> CONVEYOR_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("conveyor", () -> new BlockEntityType<>(ConveyorBlockEntity::new,
                    CONVEYOR.value(), FAST_CONVEYOR.value(), EXPRESS_CONVEYOR.value()));

    private static DeferredHolder<Block, Block> conveyor(String id, double speed) {
        return BLOCKS.register(id, () -> new ConveyorBlock(BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, id)))
                .strength(1.5F, 6.0F)
                .sound(SoundType.METAL)
                .noOcclusion(), speed));
    }

    // ---- Processing line: crusher -> industrial furnace, with a sorting belt.
    public static final DeferredHolder<Block, Block> CRUSHER = BLOCKS.register("crusher", () ->
            new CrusherBlock(machineProperties("crusher").lightLevel(state -> 0)));
    public static final DeferredHolder<Item, Item> CRUSHER_ITEM = ITEMS.registerItem("crusher",
            properties -> new BlockItem(CRUSHER.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrusherBlockEntity>> CRUSHER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("crusher", () -> new BlockEntityType<>(CrusherBlockEntity::new, CRUSHER.value()));

    public static final DeferredHolder<Block, Block> INDUSTRIAL_FURNACE = BLOCKS.register("industrial_furnace", () ->
            new IndustrialFurnaceBlock(machineProperties("industrial_furnace")
                    .lightLevel(state -> state.getValue(MachineBlock.LIT) ? 13 : 0)));
    public static final DeferredHolder<Item, Item> INDUSTRIAL_FURNACE_ITEM = ITEMS.registerItem("industrial_furnace",
            properties -> new BlockItem(INDUSTRIAL_FURNACE.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IndustrialFurnaceBlockEntity>> INDUSTRIAL_FURNACE_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("industrial_furnace", () -> new BlockEntityType<>(IndustrialFurnaceBlockEntity::new, INDUSTRIAL_FURNACE.value()));

    public static final DeferredHolder<Block, Block> SORTER = BLOCKS.register("sorter", () ->
            new SorterBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "sorter")))
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));
    public static final DeferredHolder<Item, Item> SORTER_ITEM = ITEMS.registerItem("sorter",
            properties -> new BlockItem(SORTER.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SorterBlockEntity>> SORTER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("sorter", () -> new BlockEntityType<>(SorterBlockEntity::new, SORTER.value()));

    // ---- Well drill (stationary miner) and vertical conveyor.
    public static final DeferredHolder<Block, Block> WELL_DRILL = BLOCKS.register("well_drill", () ->
            new WellDrillBlock(machineProperties("well_drill")
                    .lightLevel(state -> state.getValue(MachineBlock.LIT) ? 7 : 0)));
    public static final DeferredHolder<Item, Item> WELL_DRILL_ITEM = ITEMS.registerItem("well_drill",
            properties -> new BlockItem(WELL_DRILL.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WellDrillBlockEntity>> WELL_DRILL_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("well_drill", () -> new BlockEntityType<>(WellDrillBlockEntity::new, WELL_DRILL.value()));

    public static final DeferredHolder<Block, Block> ELEVATOR = BLOCKS.register("elevator", () ->
            new ElevatorBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "elevator")))
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));
    public static final DeferredHolder<Item, Item> ELEVATOR_ITEM = ITEMS.registerItem("elevator",
            properties -> new BlockItem(ELEVATOR.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElevatorBlockEntity>> ELEVATOR_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("elevator", () -> new BlockEntityType<>(ElevatorBlockEntity::new, ELEVATOR.value()));

    public static final DeferredHolder<Item, Item> IRON_DUST = ITEMS.registerItem("iron_dust", Item::new);
    public static final DeferredHolder<Item, Item> GOLD_DUST = ITEMS.registerItem("gold_dust", Item::new);
    public static final DeferredHolder<Item, Item> COPPER_DUST = ITEMS.registerItem("copper_dust", Item::new);

    private static BlockBehaviour.Properties machineProperties(String id) {
        return BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, id)))
                .strength(3.5F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops();
    }

    /** Invisible light source used by vehicle headlights (see VehicleLights). */
    public static final DeferredHolder<Block, Block> VEHICLE_LIGHT = BLOCKS.register("vehicle_light", () ->
            new VehicleLightBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "vehicle_light")))
                    .replaceable()
                    .noCollision()
                    .noOcclusion()
                    .noLootTable()
                    .instabreak()
                    .pushReaction(PushReaction.DESTROY)
                    .lightLevel(state -> 15)));

    public static final DeferredHolder<EntityType<?>, EntityType<ExcavatorEntity>> EXCAVATOR = ENTITY_TYPES.register("excavator", () ->
            EntityType.Builder.of(ExcavatorEntity::new, MobCategory.MISC)
                    .sized(3.3F, 4.2F)
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "excavator"))));
    public static final DeferredHolder<Item, Item> EXCAVATOR_ITEM = ITEMS.registerItem("excavator", ExcavatorItem::new);
    public static final DeferredHolder<EntityType<?>, EntityType<TunnelerEntity>> TUNNELER = ENTITY_TYPES.register("tunneler", () ->
            EntityType.Builder.of(TunnelerEntity::new, MobCategory.MISC)
                    .sized(4.2F, 4.0F)
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "tunneler"))));
    public static final DeferredHolder<Item, Item> TUNNELER_ITEM = ITEMS.registerItem("tunneler", TunnelerItem::new);
    public static final DeferredHolder<EntityType<?>, EntityType<DumpTruckEntity>> DUMP_TRUCK = ENTITY_TYPES.register("dump_truck", () ->
            EntityType.Builder.of(DumpTruckEntity::new, MobCategory.MISC)
                    .sized(3.0F, 3.6F)
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "dump_truck"))));
    public static final DeferredHolder<Item, Item> DUMP_TRUCK_ITEM = ITEMS.registerItem("dump_truck", DumpTruckItem::new);
    public static final DeferredHolder<EntityType<?>, EntityType<PaverEntity>> PAVER = ENTITY_TYPES.register("paver", () ->
            EntityType.Builder.of(PaverEntity::new, MobCategory.MISC)
                    .sized(2.9F, 3.6F)
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "paver"))));
    public static final DeferredHolder<Item, Item> PAVER_ITEM = ITEMS.registerItem("paver",
            properties -> new VehicleItem(properties, PAVER::value));
    public static final DeferredHolder<EntityType<?>, EntityType<HarvesterEntity>> HARVESTER = ENTITY_TYPES.register("harvester", () ->
            EntityType.Builder.of(HarvesterEntity::new, MobCategory.MISC)
                    .sized(4.5F, 4.0F)
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "harvester"))));
    public static final DeferredHolder<Item, Item> HARVESTER_ITEM = ITEMS.registerItem("harvester",
            properties -> new VehicleItem(properties, HARVESTER::value));
    public static final DeferredHolder<EntityType<?>, EntityType<CraneEntity>> CRANE = ENTITY_TYPES.register("crane", () ->
            EntityType.Builder.<CraneEntity>of(CraneEntity::new, MobCategory.MISC)
                    .sized(3.4F, 3.6F)
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "crane"))));
    public static final DeferredHolder<Item, Item> CRANE_ITEM = ITEMS.registerItem("crane",
            properties -> new VehicleItem(properties, CRANE::value));
    public static final DeferredHolder<Item, Item> CRANE_CONTROL = ITEMS.registerItem("crane_control", CraneControlItem::new);
    public static final DeferredHolder<EntityType<?>, EntityType<BulldozerEntity>> BULLDOZER = ENTITY_TYPES.register("bulldozer", () ->
            EntityType.Builder.of(BulldozerEntity::new, MobCategory.MISC)
                    .sized(5.0F, 3.8F)
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "bulldozer"))));
    public static final DeferredHolder<Item, Item> BULLDOZER_ITEM = ITEMS.registerItem("bulldozer",
            properties -> new VehicleItem(properties, BULLDOZER::value));
    public static final DeferredHolder<EntityType<?>, EntityType<TankerEntity>> TANKER = ENTITY_TYPES.register("tanker", () ->
            EntityType.Builder.of(TankerEntity::new, MobCategory.MISC)
                    .sized(3.2F, 3.6F)
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "tanker"))));
    public static final DeferredHolder<Item, Item> TANKER_ITEM = ITEMS.registerItem("tanker",
            properties -> new VehicleItem(properties, TANKER::value));
    public static final DeferredHolder<EntityType<?>, EntityType<ForesterEntity>> FORESTER = ENTITY_TYPES.register("forester", () ->
            EntityType.Builder.of(ForesterEntity::new, MobCategory.MISC)
                    .sized(3.4F, 3.8F)
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "forester"))));
    public static final DeferredHolder<Item, Item> FORESTER_ITEM = ITEMS.registerItem("forester",
            properties -> new VehicleItem(properties, FORESTER::value));
    public static final DeferredHolder<EntityType<?>, EntityType<TowerCraneEntity>> TOWER_CRANE = ENTITY_TYPES.register("tower_crane", () ->
            EntityType.Builder.of(TowerCraneEntity::new, MobCategory.MISC)
                    .sized(3.0F, 16.0F)
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "tower_crane"))));
    public static final DeferredHolder<Item, Item> TOWER_CRANE_ITEM = ITEMS.registerItem("tower_crane",
            properties -> new VehicleItem(properties, TOWER_CRANE::value));

    // ---- Power: diesel generator, cables and the machines that run on them.
    public static final DeferredHolder<Block, Block> GENERATOR = BLOCKS.register("generator", () ->
            new SimpleMachineBlock(machineProperties("generator").lightLevel(state -> state.getValue(MachineBlock.LIT) ? 10 : 0), () -> FactoryCraft.GENERATOR_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> GENERATOR_ITEM = ITEMS.registerItem("generator",
            properties -> new BlockItem(GENERATOR.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeneratorBlockEntity>> GENERATOR_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("generator", () -> new BlockEntityType<>(GeneratorBlockEntity::new, GENERATOR.value()));
    public static final DeferredHolder<Block, Block> ORE_WASHER = BLOCKS.register("ore_washer", () ->
            new SimpleMachineBlock(machineProperties("ore_washer").lightLevel(state -> 0), () -> FactoryCraft.ORE_WASHER_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> ORE_WASHER_ITEM = ITEMS.registerItem("ore_washer",
            properties -> new BlockItem(ORE_WASHER.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OreWasherBlockEntity>> ORE_WASHER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("ore_washer", () -> new BlockEntityType<>(OreWasherBlockEntity::new, ORE_WASHER.value()));
    public static final DeferredHolder<Block, Block> PRESS = BLOCKS.register("press", () ->
            new SimpleMachineBlock(machineProperties("press").lightLevel(state -> 0), () -> FactoryCraft.PRESS_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> PRESS_ITEM = ITEMS.registerItem("press",
            properties -> new BlockItem(PRESS.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PressBlockEntity>> PRESS_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("press", () -> new BlockEntityType<>(PressBlockEntity::new, PRESS.value()));
    public static final DeferredHolder<Block, Block> MIXER = BLOCKS.register("mixer", () ->
            new SimpleMachineBlock(machineProperties("mixer").lightLevel(state -> 0), () -> FactoryCraft.MIXER_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> MIXER_ITEM = ITEMS.registerItem("mixer",
            properties -> new BlockItem(MIXER.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MixerBlockEntity>> MIXER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("mixer", () -> new BlockEntityType<>(MixerBlockEntity::new, MIXER.value()));
    public static final DeferredHolder<Block, Block> CABLE = BLOCKS.register("cable", () ->
            new CableBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "cable")))
                    .strength(0.5F).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredHolder<Item, Item> CABLE_ITEM = ITEMS.registerItem("cable",
            properties -> new BlockItem(CABLE.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<Item, Item> IRON_PLATE = ITEMS.registerItem("iron_plate", Item::new);
    public static final DeferredHolder<Item, Item> COPPER_PLATE = ITEMS.registerItem("copper_plate", Item::new);
    public static final DeferredHolder<Item, Item> GOLD_PLATE = ITEMS.registerItem("gold_plate", Item::new);
    public static final DeferredHolder<Item, Item> IRON_GEAR = ITEMS.registerItem("iron_gear", Item::new);
    public static final DeferredHolder<MenuType<?>, MenuType<GeneratorMenu>> GENERATOR_MENU = MENUS.register("generator",
            () -> new MenuType<>(GeneratorMenu::client, FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<Block, Block> SOLAR_GENERATOR = BLOCKS.register("solar_generator", () ->
            new SimpleMachineBlock(machineProperties("solar_generator").lightLevel(state -> 0), () -> FactoryCraft.SOLAR_GENERATOR_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> SOLAR_GENERATOR_ITEM = ITEMS.registerItem("solar_generator",
            properties -> new BlockItem(SOLAR_GENERATOR.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarGeneratorBlockEntity>> SOLAR_GENERATOR_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("solar_generator", () -> new BlockEntityType<>(SolarGeneratorBlockEntity::new, SOLAR_GENERATOR.value()));
    public static final DeferredHolder<Block, Block> REPAIR_STATION = BLOCKS.register("repair_station", () ->
            new SimpleMachineBlock(machineProperties("repair_station").lightLevel(state -> state.getValue(MachineBlock.LIT) ? 8 : 0),
                    () -> FactoryCraft.REPAIR_STATION_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> REPAIR_STATION_ITEM = ITEMS.registerItem("repair_station",
            properties -> new BlockItem(REPAIR_STATION.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RepairStationBlockEntity>> REPAIR_STATION_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("repair_station", () -> new BlockEntityType<>(RepairStationBlockEntity::new, REPAIR_STATION.value()));
    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMenu>> REPAIR_STATION_MENU = MENUS.register("repair_station",
            () -> new MenuType<>((id, inventory) -> ProcessingMenu.powered(FactoryCraft.REPAIR_STATION_MENU.value(), id, inventory, 4), FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<Block, Block> BATTERY = BLOCKS.register("battery", () ->
            new SimpleMachineBlock(machineProperties("battery").lightLevel(state -> 0), () -> FactoryCraft.BATTERY_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> BATTERY_ITEM = ITEMS.registerItem("battery",
            properties -> new BlockItem(BATTERY.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryBlockEntity>> BATTERY_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("battery", () -> new BlockEntityType<>(BatteryBlockEntity::new, BATTERY.value()));
    public static final DeferredHolder<MenuType<?>, MenuType<GeneratorMenu>> BATTERY_MENU = MENUS.register("battery",
            () -> new MenuType<>(GeneratorMenu::batteryClient, FeatureFlags.VANILLA_SET));
    // ---- Clean power: windmill, geothermal, steam turbine; environment: chimney filter, air purifier, inspector.
    public static final DeferredHolder<MenuType<?>, MenuType<GeneratorMenu>> PASSIVE_GENERATOR_MENU = MENUS.register("passive_generator",
            () -> new MenuType<>(GeneratorMenu::passiveClient, FeatureFlags.VANILLA_SET));
    public static final DeferredHolder<Block, Block> WINDMILL = BLOCKS.register("windmill", () ->
            new SimpleMachineBlock(machineProperties("windmill").lightLevel(state -> 0), () -> FactoryCraft.WINDMILL_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> WINDMILL_ITEM = ITEMS.registerItem("windmill",
            properties -> new BlockItem(WINDMILL.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WindmillBlockEntity>> WINDMILL_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("windmill", () -> new BlockEntityType<>(WindmillBlockEntity::new, WINDMILL.value()));
    public static final DeferredHolder<Block, Block> GEOTHERMAL = BLOCKS.register("geothermal_generator", () ->
            new SimpleMachineBlock(machineProperties("geothermal_generator").lightLevel(state -> state.getValue(MachineBlock.LIT) ? 9 : 0),
                    () -> FactoryCraft.GEOTHERMAL_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> GEOTHERMAL_ITEM = ITEMS.registerItem("geothermal_generator",
            properties -> new BlockItem(GEOTHERMAL.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeothermalGeneratorBlockEntity>> GEOTHERMAL_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("geothermal_generator", () -> new BlockEntityType<>(GeothermalGeneratorBlockEntity::new, GEOTHERMAL.value()));
    public static final DeferredHolder<Block, Block> STEAM_TURBINE = BLOCKS.register("steam_turbine", () ->
            new SimpleMachineBlock(machineProperties("steam_turbine").lightLevel(state -> 0), () -> FactoryCraft.STEAM_TURBINE_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> STEAM_TURBINE_ITEM = ITEMS.registerItem("steam_turbine",
            properties -> new BlockItem(STEAM_TURBINE.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamTurbineBlockEntity>> STEAM_TURBINE_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("steam_turbine", () -> new BlockEntityType<>(SteamTurbineBlockEntity::new, STEAM_TURBINE.value()));
    public static final DeferredHolder<Block, Block> CHIMNEY_FILTER = BLOCKS.register("chimney_filter", () ->
            new Block(machineProperties("chimney_filter")));
    public static final DeferredHolder<Item, Item> CHIMNEY_FILTER_ITEM = ITEMS.registerItem("chimney_filter",
            properties -> new BlockItem(CHIMNEY_FILTER.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<Block, Block> AIR_PURIFIER = BLOCKS.register("air_purifier", () ->
            new SimpleMachineBlock(machineProperties("air_purifier").lightLevel(state -> state.getValue(MachineBlock.LIT) ? 6 : 0),
                    () -> FactoryCraft.AIR_PURIFIER_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> AIR_PURIFIER_ITEM = ITEMS.registerItem("air_purifier",
            properties -> new BlockItem(AIR_PURIFIER.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AirPurifierBlockEntity>> AIR_PURIFIER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("air_purifier", () -> new BlockEntityType<>(AirPurifierBlockEntity::new, AIR_PURIFIER.value()));
    public static final DeferredHolder<Item, Item> PLANT_INSPECTOR = ITEMS.registerItem("plant_inspector", PlantInspectorItem::new);

    // ---- Tools & storage: jetpack, magnet, giant drill, industrial chest.
    public static final ResourceKey<net.minecraft.world.item.equipment.EquipmentAsset> JETPACK_ASSET = ResourceKey.create(
            net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(MOD_ID, "jetpack"));
    public static final net.minecraft.world.item.equipment.ArmorMaterial JETPACK_MATERIAL = new net.minecraft.world.item.equipment.ArmorMaterial(
            25, java.util.Map.of(net.minecraft.world.item.equipment.ArmorType.HELMET, 1, net.minecraft.world.item.equipment.ArmorType.CHESTPLATE, 4,
                    net.minecraft.world.item.equipment.ArmorType.LEGGINGS, 3, net.minecraft.world.item.equipment.ArmorType.BOOTS, 1,
                    net.minecraft.world.item.equipment.ArmorType.BODY, 4),
            10, net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F, Nuclear.LEAD_REPAIR, JETPACK_ASSET);
    public static final DeferredHolder<Item, Item> JETPACK = ITEMS.registerItem("jetpack",
            properties -> new JetpackItem(properties.humanoidArmor(JETPACK_MATERIAL, net.minecraft.world.item.equipment.ArmorType.CHESTPLATE)
                    .stacksTo(1)));
    public static final DeferredHolder<Item, Item> MAGNET = ITEMS.registerItem("magnet", MagnetItem::new);
    public static final DeferredHolder<Item, Item> GIANT_DRILL = ITEMS.registerItem("giant_drill",
            properties -> new DrillItem(properties.pickaxe(ToolMaterial.NETHERITE, 2.0F, -2.8F).fireResistant()
                    .rarity(net.minecraft.world.item.Rarity.EPIC), 2));
    public static final DeferredHolder<Block, Block> INDUSTRIAL_CHEST = BLOCKS.register("industrial_chest", () ->
            new SimpleMachineBlock(machineProperties("industrial_chest").lightLevel(state -> 0), () -> FactoryCraft.INDUSTRIAL_CHEST_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> INDUSTRIAL_CHEST_ITEM = ITEMS.registerItem("industrial_chest",
            properties -> new BlockItem(INDUSTRIAL_CHEST.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IndustrialChestBlockEntity>> INDUSTRIAL_CHEST_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("industrial_chest", () -> new BlockEntityType<>(IndustrialChestBlockEntity::new, INDUSTRIAL_CHEST.value()));
    public static final DeferredHolder<MenuType<?>, MenuType<IndustrialChestMenu>> INDUSTRIAL_CHEST_MENU = MENUS.register("industrial_chest",
            () -> new MenuType<>(IndustrialChestMenu::client, FeatureFlags.VANILLA_SET));

    // ---- Vehicles & logistics: tank wagon, cargo stations, route beacons, helicopter, submarine.
    public static final DeferredHolder<EntityType<?>, EntityType<TankWagonEntity>> TANK_WAGON = ENTITY_TYPES.register("tank_wagon", () ->
            EntityType.Builder.<TankWagonEntity>of(TankWagonEntity::new, MobCategory.MISC)
                    .sized(2.2F, 1.6F).clientTrackingRange(16).updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "tank_wagon"))));
    public static final DeferredHolder<Item, Item> TANK_WAGON_ITEM = ITEMS.registerItem("tank_wagon",
            properties -> new CargoWagonItem(properties, TANK_WAGON::value));
    public static final DeferredHolder<EntityType<?>, EntityType<HelicopterEntity>> HELICOPTER = ENTITY_TYPES.register("helicopter", () ->
            EntityType.Builder.<HelicopterEntity>of(HelicopterEntity::new, MobCategory.MISC)
                    .sized(2.7F, 2.4F).clientTrackingRange(16).updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "helicopter"))));
    public static final DeferredHolder<Item, Item> HELICOPTER_ITEM = ITEMS.registerItem("helicopter",
            properties -> new VehicleItem(properties, HELICOPTER::value));
    public static final DeferredHolder<EntityType<?>, EntityType<SubmarineEntity>> SUBMARINE = ENTITY_TYPES.register("submarine", () ->
            EntityType.Builder.<SubmarineEntity>of(SubmarineEntity::new, MobCategory.MISC)
                    .sized(2.0F, 2.2F).clientTrackingRange(16).updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "submarine"))));
    public static final DeferredHolder<Item, Item> SUBMARINE_ITEM = ITEMS.registerItem("submarine",
            properties -> new VehicleItem(properties, SUBMARINE::value));
    public static final DeferredHolder<Block, Block> LOADING_STATION = BLOCKS.register("loading_station", () ->
            new SimpleMachineBlock(machineProperties("loading_station").lightLevel(state -> 0), () -> FactoryCraft.CARGO_STATION_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Block, Block> UNLOADING_STATION = BLOCKS.register("unloading_station", () ->
            new SimpleMachineBlock(machineProperties("unloading_station").lightLevel(state -> 0), () -> FactoryCraft.CARGO_STATION_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> LOADING_STATION_ITEM = ITEMS.registerItem("loading_station",
            properties -> new BlockItem(LOADING_STATION.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<Item, Item> UNLOADING_STATION_ITEM = ITEMS.registerItem("unloading_station",
            properties -> new BlockItem(UNLOADING_STATION.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CargoStationBlockEntity>> CARGO_STATION_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("cargo_station", () -> new BlockEntityType<>(CargoStationBlockEntity::new,
                    LOADING_STATION.value(), UNLOADING_STATION.value()));
    public static final DeferredHolder<Block, Block> ROUTE_BEACON = BLOCKS.register("route_beacon", () ->
            new RouteBeaconBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "route_beacon")))
                    .strength(1.0F).sound(SoundType.METAL).noOcclusion().lightLevel(state -> 7)));
    public static final DeferredHolder<Item, Item> ROUTE_BEACON_ITEM = ITEMS.registerItem("route_beacon",
            properties -> new BlockItem(ROUTE_BEACON.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RouteBeaconBlockEntity>> ROUTE_BEACON_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("route_beacon", () -> new BlockEntityType<>(RouteBeaconBlockEntity::new, ROUTE_BEACON.value()));

    // ---- Copper: coils, reactor radiators, lightning collector.
    public static final DeferredHolder<Item, Item> COPPER_COIL = ITEMS.registerItem("copper_coil", Item::new);
    public static final DeferredHolder<Block, Block> COPPER_RADIATOR = BLOCKS.register("copper_radiator", () ->
            new Block(machineProperties("copper_radiator").sound(SoundType.COPPER)));
    public static final DeferredHolder<Item, Item> COPPER_RADIATOR_ITEM = ITEMS.registerItem("copper_radiator",
            properties -> new BlockItem(COPPER_RADIATOR.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<Block, Block> LIGHTNING_COLLECTOR = BLOCKS.register("lightning_collector", () ->
            new SimpleMachineBlock(machineProperties("lightning_collector").sound(SoundType.COPPER).lightLevel(state -> state.getValue(MachineBlock.LIT) ? 7 : 0),
                    () -> FactoryCraft.LIGHTNING_COLLECTOR_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> LIGHTNING_COLLECTOR_ITEM = ITEMS.registerItem("lightning_collector",
            properties -> new BlockItem(LIGHTNING_COLLECTOR.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LightningCollectorBlockEntity>> LIGHTNING_COLLECTOR_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("lightning_collector", () -> new BlockEntityType<>(LightningCollectorBlockEntity::new, LIGHTNING_COLLECTOR.value()));

    public static final DeferredHolder<Item, Item> ENERGY_METER = ITEMS.registerItem("energy_meter", EnergyMeterItem::new);
    public static final DeferredHolder<Item, Item> MACHINE_UPGRADE_SPEED = ITEMS.registerItem("machine_upgrade_speed",
            properties -> new MachineUpgradeItem(properties, MachineBlockEntity.MachineUpgrade.SPEED));
    public static final DeferredHolder<Item, Item> MACHINE_UPGRADE_EFFICIENCY = ITEMS.registerItem("machine_upgrade_efficiency",
            properties -> new MachineUpgradeItem(properties, MachineBlockEntity.MachineUpgrade.EFFICIENCY));
    public static final DeferredHolder<Item, Item> MACHINE_UPGRADE_OUTPUT = ITEMS.registerItem("machine_upgrade_output",
            properties -> new MachineUpgradeItem(properties, MachineBlockEntity.MachineUpgrade.OUTPUT));

    public static final DeferredHolder<MenuType<?>, MenuType<GeneratorMenu>> SOLAR_GENERATOR_MENU = MENUS.register("solar_generator",
            () -> new MenuType<>(GeneratorMenu::solarClient, FeatureFlags.VANILLA_SET));
    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMenu>> ORE_WASHER_MENU = MENUS.register("ore_washer",
            () -> new MenuType<>((id, inventory) -> ProcessingMenu.powered(FactoryCraft.ORE_WASHER_MENU.value(), id, inventory, ProcessingMenu.BIG_INPUTS, ProcessingMenu.BIG_OUTPUTS), FeatureFlags.VANILLA_SET));
    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMenu>> PRESS_MENU = MENUS.register("press",
            () -> new MenuType<>((id, inventory) -> ProcessingMenu.powered(FactoryCraft.PRESS_MENU.value(), id, inventory, 1), FeatureFlags.VANILLA_SET));
    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMenu>> MIXER_MENU = MENUS.register("mixer",
            () -> new MenuType<>((id, inventory) -> ProcessingMenu.powered(FactoryCraft.MIXER_MENU.value(), id, inventory, 3), FeatureFlags.VANILLA_SET));

    // ---- Automation: robot arm, pump with fluid pipes, packager.
    public static final DeferredHolder<Block, Block> PUMP = BLOCKS.register("pump", () ->
            new SimpleMachineBlock(machineProperties("pump"), () -> FactoryCraft.PUMP_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> PUMP_ITEM = ITEMS.registerItem("pump",
            properties -> new BlockItem(PUMP.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PumpBlockEntity>> PUMP_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("pump", () -> new BlockEntityType<>(PumpBlockEntity::new, PUMP.value()));
    public static final DeferredHolder<Block, Block> PACKAGER = BLOCKS.register("packager", () ->
            new SimpleMachineBlock(machineProperties("packager"), () -> FactoryCraft.PACKAGER_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> PACKAGER_ITEM = ITEMS.registerItem("packager",
            properties -> new BlockItem(PACKAGER.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PackagerBlockEntity>> PACKAGER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("packager", () -> new BlockEntityType<>(PackagerBlockEntity::new, PACKAGER.value()));
    public static final DeferredHolder<Block, Block> ROBOT_ARM = BLOCKS.register("robot_arm", () ->
            new RobotArmBlock(machineProperties("robot_arm").noOcclusion()));
    public static final DeferredHolder<Item, Item> ROBOT_ARM_ITEM = ITEMS.registerItem("robot_arm",
            properties -> new BlockItem(ROBOT_ARM.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RobotArmBlockEntity>> ROBOT_ARM_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("robot_arm", () -> new BlockEntityType<>(RobotArmBlockEntity::new, ROBOT_ARM.value()));
    public static final DeferredHolder<Block, Block> FLUID_PIPE = BLOCKS.register("fluid_pipe", () ->
            new FluidPipeBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "fluid_pipe")))
                    .strength(0.8F).sound(SoundType.COPPER).noOcclusion()));
    public static final DeferredHolder<Item, Item> FLUID_PIPE_ITEM = ITEMS.registerItem("fluid_pipe",
            properties -> new BlockItem(FLUID_PIPE.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMenu>> PACKAGER_MENU = MENUS.register("packager",
            () -> new MenuType<>((id, inventory) -> ProcessingMenu.powered(FactoryCraft.PACKAGER_MENU.value(), id, inventory, 1), FeatureFlags.VANILLA_SET));

    // ---- Tannery (rotten flesh / rabbit hide into leather) and incinerator (destroys items).
    public static final DeferredHolder<Block, Block> TANNERY = BLOCKS.register("tannery", () ->
            new SimpleMachineBlock(machineProperties("tannery"), () -> FactoryCraft.TANNERY_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> TANNERY_ITEM = ITEMS.registerItem("tannery",
            properties -> new BlockItem(TANNERY.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TanneryBlockEntity>> TANNERY_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("tannery", () -> new BlockEntityType<>(TanneryBlockEntity::new, TANNERY.value()));
    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMenu>> TANNERY_MENU = MENUS.register("tannery",
            () -> new MenuType<>((id, inventory) -> ProcessingMenu.powered(FactoryCraft.TANNERY_MENU.value(), id, inventory, 2), FeatureFlags.VANILLA_SET));
    public static final DeferredHolder<Block, Block> FILLER = BLOCKS.register("filler", () ->
            new SimpleMachineBlock(machineProperties("filler"), () -> FactoryCraft.FILLER_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> FILLER_ITEM = ITEMS.registerItem("filler",
            properties -> new BlockItem(FILLER.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FillerBlockEntity>> FILLER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("filler", () -> new BlockEntityType<>(FillerBlockEntity::new, FILLER.value()));
    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMenu>> FILLER_MENU = MENUS.register("filler",
            () -> new MenuType<>((id, inventory) -> ProcessingMenu.powered(FactoryCraft.FILLER_MENU.value(), id, inventory,
                    ProcessingMenu.BIG_INPUTS, 0), FeatureFlags.VANILLA_SET));
    public static final DeferredHolder<Block, Block> INCINERATOR = BLOCKS.register("incinerator", () ->
            new SimpleMachineBlock(machineProperties("incinerator").lightLevel(state -> state.getValue(MachineBlock.LIT) ? 12 : 0),
                    () -> FactoryCraft.INCINERATOR_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> INCINERATOR_ITEM = ITEMS.registerItem("incinerator",
            properties -> new BlockItem(INCINERATOR.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IncineratorBlockEntity>> INCINERATOR_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("incinerator", () -> new BlockEntityType<>(IncineratorBlockEntity::new, INCINERATOR.value()));
    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMenu>> INCINERATOR_MENU = MENUS.register("incinerator",
            () -> new MenuType<>((id, inventory) -> ProcessingMenu.powered(FactoryCraft.INCINERATOR_MENU.value(), id, inventory, 4, 0), FeatureFlags.VANILLA_SET));
    public static final DeferredHolder<EntityType<?>, EntityType<LocomotiveEntity>> LOCOMOTIVE = ENTITY_TYPES.register("locomotive", () ->
            EntityType.Builder.of(LocomotiveEntity::new, MobCategory.MISC)
                    .sized(2.6F, 3.4F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "locomotive"))));
    public static final DeferredHolder<Item, Item> LOCOMOTIVE_ITEM = ITEMS.registerItem("locomotive",
            properties -> new VehicleItem(properties, LOCOMOTIVE::value));
    public static final DeferredHolder<EntityType<?>, EntityType<DroneEntity>> DRONE = ENTITY_TYPES.register("drone", () ->
            EntityType.Builder.<DroneEntity>of(DroneEntity::new, MobCategory.MISC)
                    .sized(0.9F, 0.5F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "drone"))));
    public static final DeferredHolder<EntityType<?>, EntityType<OreMarkerEntity>> ORE_MARKER = ENTITY_TYPES.register("ore_marker", () ->
            EntityType.Builder.<OreMarkerEntity>of(OreMarkerEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .noSave()
                    .clientTrackingRange(8)
                    .updateInterval(100)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "ore_marker"))));
    public static final DeferredHolder<Item, Item> DRONE_ITEM = ITEMS.registerItem("drone", DroneItem::new);
    public static final DeferredHolder<Item, Item> DRONE_CONTROLLER = ITEMS.registerItem("drone_controller", DroneControllerItem::new);
    public static final DeferredHolder<Block, Block> CONTROL_STATION = BLOCKS.register("control_station", () ->
            new ControlStationBlock(machineProperties("control_station").lightLevel(state -> 7)));
    public static final DeferredHolder<Item, Item> CONTROL_STATION_ITEM = ITEMS.registerItem("control_station",
            properties -> new BlockItem(CONTROL_STATION.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<Item, Item> GUIDE_BOOK = ITEMS.registerItem("guide_book", GuideBookItem::new);
    // ---- The Mechanic: a villager profession that sells special machine gear.
    public static final DeferredHolder<Block, Block> MECHANIC_BENCH = BLOCKS.register("mechanic_bench", () ->
            new Block(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "mechanic_bench")))
                    .strength(2.5F).sound(SoundType.METAL)));
    public static final DeferredHolder<Item, Item> MECHANIC_BENCH_ITEM = ITEMS.registerItem("mechanic_bench",
            properties -> new BlockItem(MECHANIC_BENCH.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<net.minecraft.world.entity.ai.village.poi.PoiType, net.minecraft.world.entity.ai.village.poi.PoiType> MECHANIC_POI =
            POI_TYPES.register("mechanic", () -> new net.minecraft.world.entity.ai.village.poi.PoiType(
                    java.util.Set.copyOf(MECHANIC_BENCH.value().getStateDefinition().getPossibleStates()), 1, 1));
    public static final DeferredHolder<net.minecraft.world.entity.npc.villager.VillagerProfession, net.minecraft.world.entity.npc.villager.VillagerProfession> MECHANIC =
            PROFESSIONS.register("mechanic", () -> new net.minecraft.world.entity.npc.villager.VillagerProfession(
                    Component.translatable("entity.factorycraft.villager.mechanic"),
                    poi -> poi.is(MECHANIC_POI.getKey()),
                    poi -> poi.is(MECHANIC_POI.getKey()),
                    com.google.common.collect.ImmutableSet.of(),
                    com.google.common.collect.ImmutableSet.of(),
                    net.minecraft.sounds.SoundEvents.VILLAGER_WORK_TOOLSMITH,
                    mechanicTrades()));

    private static it.unimi.dsi.fastutil.ints.Int2ObjectMap<ResourceKey<net.minecraft.world.item.trading.TradeSet>> mechanicTrades() {
        it.unimi.dsi.fastutil.ints.Int2ObjectMap<ResourceKey<net.minecraft.world.item.trading.TradeSet>> map =
                new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>();
        for (int level = 1; level <= 5; level++) {
            map.put(level, ResourceKey.create(Registries.TRADE_SET,
                    Identifier.fromNamespaceAndPath(MOD_ID, "mechanic/level_" + level)));
        }
        return map;
    }

    // Special gear sold by the Mechanic (no crafting recipe).
    public static final DeferredHolder<Item, Item> PROTOTYPE_DRILL = ITEMS.registerItem("prototype_drill",
            properties -> new DrillItem(properties.pickaxe(ToolMaterial.NETHERITE, 1.0F, -2.4F).fireResistant(), true));
    public static final DeferredHolder<Item, Item> INDUSTRIAL_CHAINSAW = ITEMS.registerItem("industrial_chainsaw",
            properties -> new ChainsawItem(ToolMaterial.NETHERITE, 6.0F, -2.8F, properties.fireResistant(), true));
    public static final DeferredHolder<Item, Item> TITAN_HAMMER = ITEMS.registerItem("titan_hammer",
            properties -> new TitanHammerItem(properties.pickaxe(ToolMaterial.NETHERITE, 3.0F, -3.3F).fireResistant()));

    public static final DeferredHolder<Item, Item> WRENCH = ITEMS.registerItem("wrench", WrenchItem::new);
    /** One module item per {@link Upgrade}: factorycraft:upgrade_<id>. */
    public static final java.util.Map<Upgrade, DeferredHolder<Item, Item>> UPGRADE_ITEMS = registerUpgrades();

    private static java.util.Map<Upgrade, DeferredHolder<Item, Item>> registerUpgrades() {
        java.util.Map<Upgrade, DeferredHolder<Item, Item>> map = new java.util.EnumMap<>(Upgrade.class);
        for (Upgrade upgrade : Upgrade.values()) {
            map.put(upgrade, ITEMS.registerItem("upgrade_" + upgrade.id(), properties -> new UpgradeModuleItem(properties, upgrade)));
        }
        return map;
    }
    public static final DeferredHolder<EntityType<?>, EntityType<CargoWagonEntity>> CARGO_WAGON = ENTITY_TYPES.register("cargo_wagon", () ->
            EntityType.Builder.<CargoWagonEntity>of(CargoWagonEntity::new, MobCategory.MISC)
                    .sized(2.1F, 2.1F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "cargo_wagon"))));
    public static final DeferredHolder<Item, Item> CARGO_WAGON_ITEM = ITEMS.registerItem("cargo_wagon", CargoWagonItem::new);
    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMenu>> CRUSHER_MENU = MENUS.register("crusher",
            () -> new MenuType<>(ProcessingMenu::crusher, FeatureFlags.VANILLA_SET));
    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMenu>> INDUSTRIAL_FURNACE_MENU = MENUS.register("industrial_furnace",
            () -> new MenuType<>(ProcessingMenu::furnace, FeatureFlags.VANILLA_SET));
    /** Machine screen: storage bay, module slots and fuel gauge (see VehicleMenu). */
    public static final DeferredHolder<MenuType<?>, MenuType<VehicleMenu>> VEHICLE_MENU = MENUS.register("vehicle",
            () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(VehicleMenu::client));
    /** Nine-row chest menu for the truck bed (the client side gets an empty container of the same size). */
    public static final DeferredHolder<MenuType<?>, MenuType<ChestMenu>> DUMP_TRUCK_MENU = MENUS.register("dump_truck",
            () -> new MenuType<>((id, inventory) -> new ChestMenu(FactoryCraft.DUMP_TRUCK_MENU.value(), id, inventory,
                    new SimpleContainer(DumpTruckEntity.CAPACITY), DumpTruckEntity.ROWS), FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<Item, Item> STONE_HAMMER = registerHammer("stone_hammer", ToolMaterial.STONE);
    public static final DeferredHolder<Item, Item> COPPER_HAMMER = registerHammer("copper_hammer", ToolMaterial.COPPER);
    public static final DeferredHolder<Item, Item> IRON_HAMMER = registerHammer("iron_hammer", ToolMaterial.IRON);
    public static final DeferredHolder<Item, Item> DIAMOND_HAMMER = registerHammer("diamond_hammer", ToolMaterial.DIAMOND);
    public static final DeferredHolder<Item, Item> NETHERITE_HAMMER = registerHammer("netherite_hammer", ToolMaterial.NETHERITE);

    public static final DeferredHolder<Item, Item> STONE_DRILL = registerDrill("stone_drill", ToolMaterial.STONE);
    public static final DeferredHolder<Item, Item> COPPER_DRILL = registerDrill("copper_drill", ToolMaterial.COPPER);
    public static final DeferredHolder<Item, Item> IRON_DRILL = registerDrill("iron_drill", ToolMaterial.IRON);
    public static final DeferredHolder<Item, Item> DIAMOND_DRILL = registerDrill("diamond_drill", ToolMaterial.DIAMOND);
    public static final DeferredHolder<Item, Item> NETHERITE_DRILL = registerDrill("netherite_drill", ToolMaterial.NETHERITE);

    public static final DeferredHolder<Item, Item> STONE_CHAINSAW = registerChainsaw("stone_chainsaw", ToolMaterial.STONE);
    public static final DeferredHolder<Item, Item> COPPER_CHAINSAW = registerChainsaw("copper_chainsaw", ToolMaterial.COPPER);
    public static final DeferredHolder<Item, Item> IRON_CHAINSAW = registerChainsaw("iron_chainsaw", ToolMaterial.IRON);
    public static final DeferredHolder<Item, Item> DIAMOND_CHAINSAW = registerChainsaw("diamond_chainsaw", ToolMaterial.DIAMOND);
    public static final DeferredHolder<Item, Item> NETHERITE_CHAINSAW = registerChainsaw("netherite_chainsaw", ToolMaterial.NETHERITE);

    public static final DeferredHolder<Item, Item> STONE_EXCAVATING_SHOVEL = registerShovel("stone_excavating_shovel", ToolMaterial.STONE);
    public static final DeferredHolder<Item, Item> COPPER_EXCAVATING_SHOVEL = registerShovel("copper_excavating_shovel", ToolMaterial.COPPER);
    public static final DeferredHolder<Item, Item> IRON_EXCAVATING_SHOVEL = registerShovel("iron_excavating_shovel", ToolMaterial.IRON);
    public static final DeferredHolder<Item, Item> DIAMOND_EXCAVATING_SHOVEL = registerShovel("diamond_excavating_shovel", ToolMaterial.DIAMOND);
    public static final DeferredHolder<Item, Item> NETHERITE_EXCAVATING_SHOVEL = registerShovel("netherite_excavating_shovel", ToolMaterial.NETHERITE);

    private static DeferredHolder<Item, Item> registerShovel(String id, ToolMaterial material) {
        return ITEMS.registerItem(id, properties -> new ExcavatingShovelItem(material, 2.0F, -3.2F,
                material == ToolMaterial.NETHERITE ? properties.fireResistant() : properties));
    }

    private static DeferredHolder<Item, Item> registerHammer(String id, ToolMaterial material) {
        return ITEMS.registerItem(id, properties -> new HammerItem(properties.pickaxe(material, 2.0F, -3.2F)));
    }

    private static DeferredHolder<Item, Item> registerDrill(String id, ToolMaterial material) {
        float speed = material == ToolMaterial.IRON ? -2.4F : -2.8F;
        return ITEMS.registerItem(id, properties -> new DrillItem(properties.pickaxe(material, 1.0F, speed)));
    }

    private static DeferredHolder<Item, Item> registerChainsaw(String id, ToolMaterial material) {
        return ITEMS.registerItem(id, properties -> new ChainsawItem(material, 5.0F, -2.8F, properties));
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FACTORY_TAB = TABS.register("factorycraft",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.factorycraft"))
                    .icon(() -> IRON_HAMMER.value().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(STONE_HAMMER.value());
                        output.accept(COPPER_HAMMER.value());
                        output.accept(IRON_HAMMER.value());
                        output.accept(DIAMOND_HAMMER.value());
                        output.accept(NETHERITE_HAMMER.value());
                        output.accept(STONE_DRILL.value());
                        output.accept(COPPER_DRILL.value());
                        output.accept(IRON_DRILL.value());
                        output.accept(DIAMOND_DRILL.value());
                        output.accept(NETHERITE_DRILL.value());
                        output.accept(STONE_CHAINSAW.value());
                        output.accept(COPPER_CHAINSAW.value());
                        output.accept(IRON_CHAINSAW.value());
                        output.accept(DIAMOND_CHAINSAW.value());
                        output.accept(NETHERITE_CHAINSAW.value());
                        output.accept(STONE_EXCAVATING_SHOVEL.value());
                        output.accept(COPPER_EXCAVATING_SHOVEL.value());
                        output.accept(IRON_EXCAVATING_SHOVEL.value());
                        output.accept(DIAMOND_EXCAVATING_SHOVEL.value());
                        output.accept(NETHERITE_EXCAVATING_SHOVEL.value());
                        output.accept(EXCAVATOR_ITEM.value());
                        output.accept(TUNNELER_ITEM.value());
                        output.accept(DUMP_TRUCK_ITEM.value());
                        output.accept(CARGO_WAGON_ITEM.value());
                        output.accept(PAVER_ITEM.value());
                        output.accept(HARVESTER_ITEM.value());
                        output.accept(CRANE_ITEM.value());
                        output.accept(CRANE_CONTROL.value());
                        output.accept(BULLDOZER_ITEM.value());
                        output.accept(TANKER_ITEM.value());
                        output.accept(FORESTER_ITEM.value());
                        output.accept(TOWER_CRANE_ITEM.value());
                        output.accept(GUIDE_BOOK.value());
                        output.accept(WRENCH.value());
                        output.accept(MECHANIC_BENCH_ITEM.value());
                        output.accept(PROTOTYPE_DRILL.value());
                        output.accept(INDUSTRIAL_CHAINSAW.value());
                        output.accept(TITAN_HAMMER.value());
                        output.accept(GENERATOR_ITEM.value());
                        output.accept(SOLAR_GENERATOR_ITEM.value());
                        output.accept(REPAIR_STATION_ITEM.value());
                        output.accept(BATTERY_ITEM.value());
                        output.accept(COPPER_COIL.value());
                        output.accept(COPPER_RADIATOR_ITEM.value());
                        output.accept(LIGHTNING_COLLECTOR_ITEM.value());
                        output.accept(WINDMILL_ITEM.value());
                        output.accept(GEOTHERMAL_ITEM.value());
                        output.accept(STEAM_TURBINE_ITEM.value());
                        output.accept(CHIMNEY_FILTER_ITEM.value());
                        output.accept(AIR_PURIFIER_ITEM.value());
                        output.accept(PLANT_INSPECTOR.value());
                        output.accept(JETPACK.value());
                        output.accept(MAGNET.value());
                        output.accept(GIANT_DRILL.value());
                        output.accept(INDUSTRIAL_CHEST_ITEM.value());
                        output.accept(TANK_WAGON_ITEM.value());
                        output.accept(LOADING_STATION_ITEM.value());
                        output.accept(UNLOADING_STATION_ITEM.value());
                        output.accept(ROUTE_BEACON_ITEM.value());
                        output.accept(HELICOPTER_ITEM.value());
                        output.accept(SUBMARINE_ITEM.value());
                        output.accept(ENERGY_METER.value());
                        output.accept(MACHINE_UPGRADE_SPEED.value());
                        output.accept(MACHINE_UPGRADE_EFFICIENCY.value());
                        output.accept(MACHINE_UPGRADE_OUTPUT.value());
                        output.accept(CABLE_ITEM.value());
                        output.accept(ORE_WASHER_ITEM.value());
                        output.accept(PRESS_ITEM.value());
                        output.accept(MIXER_ITEM.value());
                        output.accept(IRON_PLATE.value());
                        output.accept(COPPER_PLATE.value());
                        output.accept(GOLD_PLATE.value());
                        output.accept(IRON_GEAR.value());
                        output.accept(ROBOT_ARM_ITEM.value());
                        output.accept(PUMP_ITEM.value());
                        output.accept(FLUID_PIPE_ITEM.value());
                        output.accept(PACKAGER_ITEM.value());
                        output.accept(TANNERY_ITEM.value());
                        output.accept(INCINERATOR_ITEM.value());
                        output.accept(FILLER_ITEM.value());
                        output.accept(LOCOMOTIVE_ITEM.value());
                        output.accept(DRONE_ITEM.value());
                        output.accept(DRONE_CONTROLLER.value());
                        output.accept(CONTROL_STATION_ITEM.value());
                        for (DeferredHolder<Item, Item> module : UPGRADE_ITEMS.values()) output.accept(module.value());
                        output.accept(CONVEYOR_ITEM.value());
                        output.accept(FAST_CONVEYOR_ITEM.value());
                        output.accept(EXPRESS_CONVEYOR_ITEM.value());
                        output.accept(SORTER_ITEM.value());
                        output.accept(ELEVATOR_ITEM.value());
                        output.accept(WELL_DRILL_ITEM.value());
                        output.accept(CRUSHER_ITEM.value());
                        output.accept(INDUSTRIAL_FURNACE_ITEM.value());
                        output.accept(IRON_DUST.value());
                        output.accept(GOLD_DUST.value());
                        output.accept(COPPER_DUST.value());
                    }).build());

    public FactoryCraft(IEventBus modEventBus, net.neoforged.fml.ModContainer container) {
        Nuclear.init();
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, FactoryConfig.SPEC);
        BLOCKS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENUS.register(modEventBus);
        ITEMS.register(modEventBus);
        TABS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);
        POI_TYPES.register(modEventBus);
        PROFESSIONS.register(modEventBus);
    }
}
