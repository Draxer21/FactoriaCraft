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
}
