package com.factorycraft;

import static com.factorycraft.FactoryCraft.BLOCKS;
import static com.factorycraft.FactoryCraft.BLOCK_ENTITY_TYPES;
import static com.factorycraft.FactoryCraft.ITEMS;
import static com.factorycraft.FactoryCraft.MENUS;
import static com.factorycraft.FactoryCraft.MOD_ID;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Crafting automation: the assembler (works alone or with the storage network). */
public final class Automation {
    private Automation() {}

    static void init() {}

    public static final DeferredHolder<Block, Block> ASSEMBLER = BLOCKS.register("assembler", () ->
            new SimpleMachineBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "assembler")))
                    .strength(3.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(MachineBlock.LIT) ? 6 : 0),
                    () -> Automation.ASSEMBLER_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> ASSEMBLER_ITEM = ITEMS.registerItem("assembler",
            properties -> new BlockItem(ASSEMBLER.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AssemblerBlockEntity>> ASSEMBLER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("assembler", () -> new BlockEntityType<>(AssemblerBlockEntity::new, ASSEMBLER.value()));
    public static final DeferredHolder<MenuType<?>, MenuType<AssemblerMenu>> ASSEMBLER_MENU = MENUS.register("assembler",
            () -> new MenuType<>(AssemblerMenu::new, FeatureFlags.VANILLA_SET));

    private static BlockBehaviour.Properties bus(String id) {
        return BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, id)))
                .strength(2.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    public static final DeferredHolder<Block, Block> IMPORT_BUS = BLOCKS.register("import_bus", () -> new BusBlock(bus("import_bus"), false));
    public static final DeferredHolder<Block, Block> EXPORT_BUS = BLOCKS.register("export_bus", () -> new BusBlock(bus("export_bus"), true));
    public static final DeferredHolder<Item, Item> IMPORT_BUS_ITEM = ITEMS.registerItem("import_bus",
            properties -> new BlockItem(IMPORT_BUS.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<Item, Item> EXPORT_BUS_ITEM = ITEMS.registerItem("export_bus",
            properties -> new BlockItem(EXPORT_BUS.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BusBlockEntity>> BUS_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("bus", () -> new BlockEntityType<>(BusBlockEntity::new, IMPORT_BUS.value(), EXPORT_BUS.value()));
    public static final DeferredHolder<MenuType<?>, MenuType<BusMenu>> BUS_MENU = MENUS.register("bus",
            () -> new MenuType<>(BusMenu::new, FeatureFlags.VANILLA_SET));
}
