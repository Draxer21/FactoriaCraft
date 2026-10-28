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
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Stage 10, endgame: particle accelerator and antimatter, the antimatter reactor,
 * teleporters, and the space program (launch pad, rocket, satellites, orbital
 * receiver, uplink).
 */
public final class Endgame {
    private Endgame() {}

    static void init() {
        Orbit.init();
    }

    private static BlockBehaviour.Properties machine(String id) {
        return BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, id)))
                .strength(5.0F, 12.0F).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops();
    }

    private static DeferredHolder<Item, Item> blockItem(String id, DeferredHolder<Block, Block> block) {
        return ITEMS.registerItem(id, properties -> new BlockItem(block.value(), properties.useBlockDescriptionPrefix().rarity(Rarity.RARE)));
    }

    private static DeferredHolder<Item, Item> item(String id, int stack, Rarity rarity) {
        return ITEMS.registerItem(id, properties -> new Item(properties.stacksTo(stack).rarity(rarity)));
    }

    // ---------------------------------------------------------------- accelerator and antimatter

    public static final DeferredHolder<Block, Block> MAGNET = BLOCKS.register("superconducting_magnet", () ->
            new Block(machine("superconducting_magnet").lightLevel(state -> 4)));
    public static final DeferredHolder<Item, Item> MAGNET_ITEM = blockItem("superconducting_magnet", MAGNET);
    public static final DeferredHolder<Block, Block> ACCELERATOR = BLOCKS.register("accelerator", () ->
            new SimpleMachineBlock(machine("accelerator").lightLevel(state -> state.getValue(MachineBlock.LIT) ? 12 : 3), () -> Endgame.ACCELERATOR_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> ACCELERATOR_ITEM = blockItem("accelerator", ACCELERATOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AcceleratorBlockEntity>> ACCELERATOR_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("accelerator", () -> new BlockEntityType<>(AcceleratorBlockEntity::new, ACCELERATOR.value()));
    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMenu>> ACCELERATOR_MENU = MENUS.register("accelerator",
            () -> new MenuType<>((id, inventory) -> ProcessingMenu.powered(Endgame.ACCELERATOR_MENU.value(), id, inventory, 1), FeatureFlags.VANILLA_SET));
    public static final DeferredHolder<Item, Item> CONTAINMENT_CELL = item("containment_cell", 16, Rarity.UNCOMMON);
    public static final DeferredHolder<Item, Item> ANTIMATTER_CELL = item("antimatter_cell", 16, Rarity.EPIC);
    public static final DeferredHolder<Block, Block> ANTIMATTER_REACTOR = BLOCKS.register("antimatter_reactor", () ->
            new SimpleMachineBlock(machine("antimatter_reactor").lightLevel(state -> state.getValue(MachineBlock.LIT) ? 15 : 2), () -> Endgame.ANTIMATTER_REACTOR_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> ANTIMATTER_REACTOR_ITEM = blockItem("antimatter_reactor", ANTIMATTER_REACTOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AntimatterReactorBlockEntity>> ANTIMATTER_REACTOR_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("antimatter_reactor", () -> new BlockEntityType<>(AntimatterReactorBlockEntity::new, ANTIMATTER_REACTOR.value()));

    // ---------------------------------------------------------------- teleporter

    public static final DeferredHolder<Block, Block> TELEPORTER = BLOCKS.register("teleporter", () ->
            new SimpleMachineBlock(machine("teleporter").lightLevel(state -> state.getValue(MachineBlock.LIT) ? 10 : 3), () -> Endgame.TELEPORTER_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> TELEPORTER_ITEM = blockItem("teleporter", TELEPORTER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TeleporterBlockEntity>> TELEPORTER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("teleporter", () -> new BlockEntityType<>(TeleporterBlockEntity::new, TELEPORTER.value()));

    // ---------------------------------------------------------------- space program

    public static final DeferredHolder<Block, Block> LAUNCH_PAD = BLOCKS.register("launch_pad", () ->
            new SimpleMachineBlock(machine("launch_pad"), () -> Endgame.LAUNCH_PAD_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> LAUNCH_PAD_ITEM = blockItem("launch_pad", LAUNCH_PAD);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LaunchPadBlockEntity>> LAUNCH_PAD_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("launch_pad", () -> new BlockEntityType<>(LaunchPadBlockEntity::new, LAUNCH_PAD.value()));
    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMenu>> LAUNCH_PAD_MENU = MENUS.register("launch_pad",
            () -> new MenuType<>((id, inventory) -> ProcessingMenu.powered(Endgame.LAUNCH_PAD_MENU.value(), id, inventory, 3, 0), FeatureFlags.VANILLA_SET));
    public static final DeferredHolder<Item, Item> ROCKET = item("rocket", 1, Rarity.RARE);
    public static final DeferredHolder<Item, Item> ROCKET_FUEL = item("rocket_fuel", 16, Rarity.UNCOMMON);
    public static final DeferredHolder<Item, Item> SOLAR_SATELLITE = item("solar_satellite", 1, Rarity.EPIC);
    public static final DeferredHolder<Item, Item> SURVEY_SATELLITE = item("survey_satellite", 1, Rarity.EPIC);
    public static final DeferredHolder<Item, Item> UPLINK = ITEMS.registerItem("uplink", properties -> new UplinkItem(properties.rarity(Rarity.RARE)));
    public static final DeferredHolder<EntityType<?>, EntityType<RocketEntity>> ROCKET_ENTITY = ENTITY_TYPES.register("rocket", () ->
            EntityType.Builder.<RocketEntity>of(RocketEntity::new, MobCategory.MISC)
                    .sized(1.2F, 6.0F).clientTrackingRange(32).updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "rocket"))));
    public static final DeferredHolder<Block, Block> ORBITAL_RECEIVER = BLOCKS.register("orbital_receiver", () ->
            new SimpleMachineBlock(machine("orbital_receiver").noOcclusion(), () -> Endgame.ORBITAL_RECEIVER_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> ORBITAL_RECEIVER_ITEM = blockItem("orbital_receiver", ORBITAL_RECEIVER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OrbitalReceiverBlockEntity>> ORBITAL_RECEIVER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("orbital_receiver", () -> new BlockEntityType<>(OrbitalReceiverBlockEntity::new, ORBITAL_RECEIVER.value()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("endgame",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.factorycraft.endgame"))
                    .icon(() -> ROCKET.value().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        for (DeferredHolder<Item, Item> item : List.of(MAGNET_ITEM, ACCELERATOR_ITEM, CONTAINMENT_CELL, ANTIMATTER_CELL,
                                ANTIMATTER_REACTOR_ITEM, TELEPORTER_ITEM, LAUNCH_PAD_ITEM, ROCKET, ROCKET_FUEL, SOLAR_SATELLITE,
                                SURVEY_SATELLITE, ORBITAL_RECEIVER_ITEM, UPLINK)) {
                            output.accept(item.value());
                        }
                    }).build());
}
