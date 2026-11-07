package com.factorycraft;

import static com.factorycraft.FactoryCraft.BLOCKS;
import static com.factorycraft.FactoryCraft.ITEMS;
import static com.factorycraft.FactoryCraft.MOD_ID;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Visible oil: oil shale deep underground (Y -64 to 0) and tar sand on the sea
 * floor, both dropping crude oil, plus the oil detector that reads the hidden
 * deposits under the chunks around you.
 */
public final class OilFields {
    private OilFields() {}

    static void init() {}

    private static BlockBehaviour.Properties props(String id) {
        return BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, id)));
    }

    public static final DeferredHolder<Block, Block> OIL_SHALE = BLOCKS.register("oil_shale", () ->
            new Block(props("oil_shale").strength(4.0F, 3.0F).sound(SoundType.DEEPSLATE).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, Block> TAR_SAND = BLOCKS.register("tar_sand", () ->
            new Block(props("tar_sand").strength(0.8F).sound(SoundType.SAND)));
    public static final DeferredHolder<Item, Item> OIL_SHALE_ITEM = ITEMS.registerItem("oil_shale",
            properties -> new BlockItem(OIL_SHALE.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<Item, Item> TAR_SAND_ITEM = ITEMS.registerItem("tar_sand",
            properties -> new BlockItem(TAR_SAND.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<Item, Item> OIL_DETECTOR = ITEMS.registerItem("oil_detector",
            properties -> new OilDetectorItem(properties.stacksTo(1)));
}
