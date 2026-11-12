package com.factorycraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Registration of FactoryCraft's vehicle and cargo-station content. */
final class FactoryVehicles {
    static final DeferredHolder<EntityType<?>, EntityType<TankWagonEntity>> TANK_WAGON = FactoryCraft.ENTITY_TYPES.register("tank_wagon", () ->
            EntityType.Builder.<TankWagonEntity>of(TankWagonEntity::new, MobCategory.MISC)
                    .sized(2.2F, 1.6F).clientTrackingRange(16).updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "tank_wagon"))));
    static final DeferredHolder<Item, Item> TANK_WAGON_ITEM = FactoryCraft.ITEMS.registerItem("tank_wagon",
            properties -> new CargoWagonItem(properties, TANK_WAGON::value));
    static final DeferredHolder<EntityType<?>, EntityType<HelicopterEntity>> HELICOPTER = FactoryCraft.ENTITY_TYPES.register("helicopter", () ->
            EntityType.Builder.<HelicopterEntity>of(HelicopterEntity::new, MobCategory.MISC)
                    .sized(2.7F, 2.4F).clientTrackingRange(16).updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "helicopter"))));
    static final DeferredHolder<Item, Item> HELICOPTER_ITEM = FactoryCraft.ITEMS.registerItem("helicopter",
            properties -> new VehicleItem(properties, HELICOPTER::value));
    static final DeferredHolder<EntityType<?>, EntityType<SubmarineEntity>> SUBMARINE = FactoryCraft.ENTITY_TYPES.register("submarine", () ->
            EntityType.Builder.<SubmarineEntity>of(SubmarineEntity::new, MobCategory.MISC)
                    .sized(2.0F, 2.2F).clientTrackingRange(16).updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "submarine"))));
    static final DeferredHolder<Item, Item> SUBMARINE_ITEM = FactoryCraft.ITEMS.registerItem("submarine",
            properties -> new VehicleItem(properties, SUBMARINE::value));
    static final DeferredHolder<Block, Block> LOADING_STATION = FactoryCraft.BLOCKS.register("loading_station", () ->
            new SimpleMachineBlock(FactoryCraft.machineProperties("loading_station").lightLevel(state -> 0),
                    () -> FactoryVehicles.CARGO_STATION_BLOCK_ENTITY.value()));
    static final DeferredHolder<Block, Block> UNLOADING_STATION = FactoryCraft.BLOCKS.register("unloading_station", () ->
            new SimpleMachineBlock(FactoryCraft.machineProperties("unloading_station").lightLevel(state -> 0),
                    () -> FactoryVehicles.CARGO_STATION_BLOCK_ENTITY.value()));
    static final DeferredHolder<Item, Item> LOADING_STATION_ITEM = FactoryCraft.ITEMS.registerItem("loading_station",
            properties -> new BlockItem(LOADING_STATION.value(), properties.useBlockDescriptionPrefix()));
    static final DeferredHolder<Item, Item> UNLOADING_STATION_ITEM = FactoryCraft.ITEMS.registerItem("unloading_station",
            properties -> new BlockItem(UNLOADING_STATION.value(), properties.useBlockDescriptionPrefix()));
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CargoStationBlockEntity>> CARGO_STATION_BLOCK_ENTITY =
            FactoryCraft.BLOCK_ENTITY_TYPES.register("cargo_station", () -> new BlockEntityType<>(CargoStationBlockEntity::new,
                    LOADING_STATION.value(), UNLOADING_STATION.value()));
    static final DeferredHolder<Block, Block> ROUTE_BEACON = FactoryCraft.BLOCKS.register("route_beacon", () ->
            new RouteBeaconBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "route_beacon")))
                    .strength(1.0F).sound(SoundType.METAL).noOcclusion().lightLevel(state -> 7)));
    static final DeferredHolder<Item, Item> ROUTE_BEACON_ITEM = FactoryCraft.ITEMS.registerItem("route_beacon",
            properties -> new BlockItem(ROUTE_BEACON.value(), properties.useBlockDescriptionPrefix()));
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RouteBeaconBlockEntity>> ROUTE_BEACON_BLOCK_ENTITY =
            FactoryCraft.BLOCK_ENTITY_TYPES.register("route_beacon", () -> new BlockEntityType<>(RouteBeaconBlockEntity::new, ROUTE_BEACON.value()));

    private FactoryVehicles() {}
}
