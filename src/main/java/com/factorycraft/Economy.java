package com.factorycraft;

import static com.factorycraft.FactoryCraft.BLOCKS;
import static com.factorycraft.FactoryCraft.BLOCK_ENTITY_TYPES;
import static com.factorycraft.FactoryCraft.ITEMS;
import static com.factorycraft.FactoryCraft.MENUS;
import static com.factorycraft.FactoryCraft.MOD_ID;
import static com.factorycraft.FactoryCraft.TABS;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
 * Stage 9, environment and economy: recycler, vending machine, contract board,
 * production statistics, protected zones, and the air quality meter.
 */
public final class Economy {
    private Economy() {}

    static void init() {}

    private static BlockBehaviour.Properties machine(String id) {
        return BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, id)))
                .strength(3.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    private static DeferredHolder<Item, Item> blockItem(String id, DeferredHolder<Block, Block> block) {
        return ITEMS.registerItem(id, properties -> new BlockItem(block.value(), properties.useBlockDescriptionPrefix()));
    }

    public static final DeferredHolder<Block, Block> RECYCLER = BLOCKS.register("recycler", () ->
            new SimpleMachineBlock(machine("recycler"), () -> Economy.RECYCLER_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> RECYCLER_ITEM = blockItem("recycler", RECYCLER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RecyclerBlockEntity>> RECYCLER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("recycler", () -> new BlockEntityType<>(RecyclerBlockEntity::new, RECYCLER.value()));
    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMenu>> RECYCLER_MENU = MENUS.register("recycler",
            () -> new MenuType<>((id, inventory) -> ProcessingMenu.powered(Economy.RECYCLER_MENU.value(), id, inventory, 1, ProcessingMenu.BIG_OUTPUTS), FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<Block, Block> VENDING_MACHINE = BLOCKS.register("vending_machine", () ->
            new SimpleMachineBlock(machine("vending_machine").lightLevel(state -> state.getValue(MachineBlock.LIT) ? 8 : 0), () -> Economy.VENDING_MACHINE_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> VENDING_MACHINE_ITEM = blockItem("vending_machine", VENDING_MACHINE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VendingMachineBlockEntity>> VENDING_MACHINE_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("vending_machine", () -> new BlockEntityType<>(VendingMachineBlockEntity::new, VENDING_MACHINE.value()));
    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMenu>> VENDING_MACHINE_MENU = MENUS.register("vending_machine",
            () -> new MenuType<>((id, inventory) -> ProcessingMenu.powered(Economy.VENDING_MACHINE_MENU.value(), id, inventory,
                    ProcessingMenu.BIG_INPUTS, ProcessingMenu.BIG_OUTPUTS), FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<Block, Block> CONTRACT_BOARD = BLOCKS.register("contract_board", () ->
            new SimpleMachineBlock(machine("contract_board").sound(SoundType.WOOD), () -> Economy.CONTRACT_BOARD_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> CONTRACT_BOARD_ITEM = blockItem("contract_board", CONTRACT_BOARD);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ContractBoardBlockEntity>> CONTRACT_BOARD_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("contract_board", () -> new BlockEntityType<>(ContractBoardBlockEntity::new, CONTRACT_BOARD.value()));

    public static final DeferredHolder<Block, Block> STATS_TERMINAL = BLOCKS.register("stats_terminal", () ->
            new SimpleMachineBlock(machine("stats_terminal").lightLevel(state -> 6), () -> Economy.STATS_TERMINAL_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> STATS_TERMINAL_ITEM = blockItem("stats_terminal", STATS_TERMINAL);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StatsTerminalBlockEntity>> STATS_TERMINAL_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("stats_terminal", () -> new BlockEntityType<>(StatsTerminalBlockEntity::new, STATS_TERMINAL.value()));

    public static final DeferredHolder<Block, Block> ZONE_CONTROLLER = BLOCKS.register("zone_controller", () ->
            new SimpleMachineBlock(machine("zone_controller").strength(30.0F, 1200.0F).lightLevel(state -> 7), () -> Economy.ZONE_CONTROLLER_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> ZONE_CONTROLLER_ITEM = blockItem("zone_controller", ZONE_CONTROLLER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ZoneControllerBlockEntity>> ZONE_CONTROLLER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("zone_controller", () -> new BlockEntityType<>(ZoneControllerBlockEntity::new, ZONE_CONTROLLER.value()));
    public static final DeferredHolder<Item, Item> ACCESS_CARD = ITEMS.registerItem("access_card", properties -> new Item(properties.stacksTo(1)));
    public static final DeferredHolder<Item, Item> AIR_METER = ITEMS.registerItem("air_meter", AirMeterItem::new);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("economy",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.factorycraft.economy"))
                    .icon(() -> VENDING_MACHINE_ITEM.value().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        for (DeferredHolder<Item, Item> item : List.of(AIR_METER, RECYCLER_ITEM, VENDING_MACHINE_ITEM, CONTRACT_BOARD_ITEM,
                                STATS_TERMINAL_ITEM, ZONE_CONTROLLER_ITEM, ACCESS_CARD)) {
                            output.accept(item.value());
                        }
                    }).build());
}
