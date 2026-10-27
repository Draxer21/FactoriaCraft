package com.factorycraft;

import static com.factorycraft.FactoryCraft.BLOCKS;
import static com.factorycraft.FactoryCraft.BLOCK_ENTITY_TYPES;
import static com.factorycraft.FactoryCraft.ENTITY_TYPES;
import static com.factorycraft.FactoryCraft.ITEMS;
import static com.factorycraft.FactoryCraft.MENUS;
import static com.factorycraft.FactoryCraft.MOD_ID;
import static com.factorycraft.FactoryCraft.TABS;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Stage 7, logistics: digital storage (terminal + data cables), drone delivery
 * between ports, cable car lines, the cargo ship and the garage.
 */
public final class Logistics {
    private Logistics() {}

    static void init() {}

    private static BlockBehaviour.Properties machine(String id) {
        return BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, id)))
                .strength(3.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    private static DeferredHolder<Item, Item> blockItem(String id, DeferredHolder<Block, Block> block) {
        return ITEMS.registerItem(id, properties -> new BlockItem(block.value(), properties.useBlockDescriptionPrefix()));
    }

    private static ResourceKey<EntityType<?>> key(String id) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, id));
    }

    // ---------------------------------------------------------------- digital storage

    public static final DeferredHolder<Block, Block> DATA_CABLE = BLOCKS.register("data_cable", () ->
            new DataCableBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "data_cable")))
                    .strength(0.5F).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredHolder<Item, Item> DATA_CABLE_ITEM = blockItem("data_cable", DATA_CABLE);
    public static final DeferredHolder<Block, Block> STORAGE_TERMINAL = BLOCKS.register("storage_terminal", () ->
            new SimpleMachineBlock(machine("storage_terminal").lightLevel(state -> 6), () -> Logistics.STORAGE_TERMINAL_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> STORAGE_TERMINAL_ITEM = blockItem("storage_terminal", STORAGE_TERMINAL);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StorageTerminalBlockEntity>> STORAGE_TERMINAL_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("storage_terminal", () -> new BlockEntityType<>(StorageTerminalBlockEntity::new, STORAGE_TERMINAL.value()));
    public static final DeferredHolder<MenuType<?>, MenuType<StorageTerminalMenu>> STORAGE_TERMINAL_MENU = MENUS.register("storage_terminal",
            () -> new MenuType<>(StorageTerminalMenu::client, FeatureFlags.VANILLA_SET));

    // ---------------------------------------------------------------- drone delivery

    public static final DeferredHolder<Block, Block> DRONE_PORT = BLOCKS.register("drone_port", () ->
            new SimpleMachineBlock(machine("drone_port"), () -> Logistics.DRONE_PORT_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> DRONE_PORT_ITEM = blockItem("drone_port", DRONE_PORT);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DronePortBlockEntity>> DRONE_PORT_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("drone_port", () -> new BlockEntityType<>(DronePortBlockEntity::new, DRONE_PORT.value()));
    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMenu>> DRONE_PORT_MENU = MENUS.register("drone_port",
            () -> new MenuType<>((id, inventory) -> ProcessingMenu.powered(Logistics.DRONE_PORT_MENU.value(), id, inventory,
                    DronePortBlockEntity.CARGO, ProcessingMenu.BIG_OUTPUTS), FeatureFlags.VANILLA_SET));
    public static final DeferredHolder<Item, Item> DELIVERY_DRONE_ITEM = ITEMS.registerItem("delivery_drone", properties -> new Item(properties.stacksTo(4)));
    public static final DeferredHolder<EntityType<?>, EntityType<DeliveryDroneEntity>> DELIVERY_DRONE = ENTITY_TYPES.register("delivery_drone", () ->
            EntityType.Builder.<DeliveryDroneEntity>of(DeliveryDroneEntity::new, MobCategory.MISC)
                    .sized(0.9F, 0.5F).noSave().clientTrackingRange(12).updateInterval(1)
                    .build(key("delivery_drone")));

    // ---------------------------------------------------------------- cable car

    public static final DeferredHolder<Block, Block> CABLE_STATION = BLOCKS.register("cable_station", () ->
            new SimpleMachineBlock(machine("cable_station"), () -> Logistics.CABLE_STATION_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> CABLE_STATION_ITEM = blockItem("cable_station", CABLE_STATION);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CableStationBlockEntity>> CABLE_STATION_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("cable_station", () -> new BlockEntityType<>(CableStationBlockEntity::new, CABLE_STATION.value()));
    public static final DeferredHolder<Item, Item> STEEL_CABLE = ITEMS.registerItem("steel_cable", SteelCableItem::new);
    public static final DeferredHolder<EntityType<?>, EntityType<CableCarEntity>> CABLE_CAR = ENTITY_TYPES.register("cable_car", () ->
            EntityType.Builder.<CableCarEntity>of(CableCarEntity::new, MobCategory.MISC)
                    .sized(1.9F, 2.0F).clientTrackingRange(16).updateInterval(1)
                    .build(key("cable_car")));

    // ---------------------------------------------------------------- cargo ship and garage

    public static final DeferredHolder<EntityType<?>, EntityType<CargoShipEntity>> CARGO_SHIP = ENTITY_TYPES.register("cargo_ship", () ->
            EntityType.Builder.<CargoShipEntity>of(CargoShipEntity::new, MobCategory.MISC)
                    .sized(3.2F, 2.0F).clientTrackingRange(16).updateInterval(2)
                    .build(key("cargo_ship")));
    public static final DeferredHolder<Item, Item> CARGO_SHIP_ITEM = ITEMS.registerItem("cargo_ship",
            properties -> new VehicleItem(properties, CARGO_SHIP::value));
    public static final DeferredHolder<Block, Block> GARAGE = BLOCKS.register("garage", () ->
            new SimpleMachineBlock(machine("garage"), () -> Logistics.GARAGE_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> GARAGE_ITEM = blockItem("garage", GARAGE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GarageBlockEntity>> GARAGE_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("garage", () -> new BlockEntityType<>(GarageBlockEntity::new, GARAGE.value()));
    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMenu>> GARAGE_MENU = MENUS.register("garage",
            () -> new MenuType<>((id, inventory) -> ProcessingMenu.powered(Logistics.GARAGE_MENU.value(), id, inventory,
                    0, ProcessingMenu.BIG_OUTPUTS), FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("logistics",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.factorycraft.logistics"))
                    .icon(() -> STORAGE_TERMINAL_ITEM.value().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        for (DeferredHolder<Item, Item> item : List.of(STORAGE_TERMINAL_ITEM, DATA_CABLE_ITEM, DRONE_PORT_ITEM,
                                DELIVERY_DRONE_ITEM, CABLE_STATION_ITEM, STEEL_CABLE, CARGO_SHIP_ITEM, GARAGE_ITEM)) {
                            output.accept(item.value());
                        }
                    }).build());
}
