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

/** Ore processing tier 3 (the chemical bath) and the electric furnace. */
public final class Processing {
    private Processing() {}

    static void init() {}

    public static final DeferredHolder<Block, Block> CHEMICAL_BATH = BLOCKS.register("chemical_bath", () ->
            new SimpleMachineBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "chemical_bath")))
                    .strength(3.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(MachineBlock.LIT) ? 7 : 0),
                    () -> Processing.CHEMICAL_BATH_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> CHEMICAL_BATH_ITEM = ITEMS.registerItem("chemical_bath",
            properties -> new BlockItem(CHEMICAL_BATH.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChemicalBathBlockEntity>> CHEMICAL_BATH_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("chemical_bath", () -> new BlockEntityType<>(ChemicalBathBlockEntity::new, CHEMICAL_BATH.value()));
    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMenu>> CHEMICAL_BATH_MENU = MENUS.register("chemical_bath",
            () -> new MenuType<>((id, inventory) -> ProcessingMenu.powered(Processing.CHEMICAL_BATH_MENU.value(), id, inventory, 2), FeatureFlags.VANILLA_SET));

    /** Electric furnace: furnace recipes on power, 16 items a second. */
    public static final DeferredHolder<Block, Block> ELECTRIC_FURNACE = BLOCKS.register("electric_furnace", () ->
            new SimpleMachineBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "electric_furnace")))
                    .strength(3.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(MachineBlock.LIT) ? 13 : 0),
                    () -> Processing.ELECTRIC_FURNACE_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> ELECTRIC_FURNACE_ITEM = ITEMS.registerItem("electric_furnace",
            properties -> new BlockItem(ELECTRIC_FURNACE.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("electric_furnace", () -> new BlockEntityType<>(ElectricFurnaceBlockEntity::new, ELECTRIC_FURNACE.value()));
    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMenu>> ELECTRIC_FURNACE_MENU = MENUS.register("electric_furnace",
            () -> new MenuType<>((id, inventory) -> ProcessingMenu.powered(Processing.ELECTRIC_FURNACE_MENU.value(), id, inventory, 1), FeatureFlags.VANILLA_SET));
}
