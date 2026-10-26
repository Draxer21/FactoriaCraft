package com.factorycraft;

import static com.factorycraft.FactoryCraft.BLOCKS;
import static com.factorycraft.FactoryCraft.BLOCK_ENTITY_TYPES;
import static com.factorycraft.FactoryCraft.ITEMS;
import static com.factorycraft.FactoryCraft.MENUS;
import static com.factorycraft.FactoryCraft.MOD_ID;
import static com.factorycraft.FactoryCraft.TABS;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Industrial materials: four new metals (tin, nickel, lithium, tungsten) with ores,
 * the alloys made from them in the alloy smelter (steel, bronze, stainless steel),
 * plastic, wire, circuits and tempered glass.
 */
public final class Materials {
    private Materials() {}

    static void init() {}

    /** One mined metal: its ore blocks and items. */
    public record Metal(String name, DeferredHolder<Block, Block> ore, DeferredHolder<Block, Block> deepOre,
                        DeferredHolder<Block, Block> block, DeferredHolder<Item, Item> oreItem,
                        DeferredHolder<Item, Item> deepOreItem, DeferredHolder<Item, Item> blockItem,
                        DeferredHolder<Item, Item> raw, DeferredHolder<Item, Item> dust,
                        DeferredHolder<Item, Item> ingot, DeferredHolder<Item, Item> nugget) {}

    /** An alloy: ingot, storage block and plate. */
    public record Alloy(String name, DeferredHolder<Block, Block> block, DeferredHolder<Item, Item> blockItem,
                        DeferredHolder<Item, Item> ingot, DeferredHolder<Item, Item> plate) {}

    private static BlockBehaviour.Properties props(String id) {
        return BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, id)))
                .requiresCorrectToolForDrops();
    }

    private static DeferredHolder<Block, Block> block(String id, float hardness, SoundType sound) {
        return BLOCKS.register(id, () -> new Block(props(id).strength(hardness, hardness == 5.0F ? 6.0F : 3.0F).sound(sound)));
    }

    private static DeferredHolder<Item, Item> blockItem(String id, DeferredHolder<Block, Block> block) {
        return ITEMS.registerItem(id, properties -> new BlockItem(block.value(), properties.useBlockDescriptionPrefix()));
    }

    private static DeferredHolder<Item, Item> item(String id) {
        return ITEMS.registerItem(id, Item::new);
    }

    private static Metal metal(String name) {
        var ore = block(name + "_ore", 3.0F, SoundType.STONE);
        var deep = block("deepslate_" + name + "_ore", 4.5F, SoundType.DEEPSLATE);
        var storage = block(name + "_block", 5.0F, SoundType.METAL);
        return new Metal(name, ore, deep, storage, blockItem(name + "_ore", ore), blockItem("deepslate_" + name + "_ore", deep),
                blockItem(name + "_block", storage), item("raw_" + name), item(name + "_dust"), item(name + "_ingot"),
                item(name + "_nugget"));
    }

    private static Alloy alloy(String name) {
        var storage = block(name + "_block", 5.0F, SoundType.METAL);
        return new Alloy(name, storage, blockItem(name + "_block", storage), item(name + "_ingot"), item(name + "_plate"));
    }

    public static final Metal TIN = metal("tin");
    public static final Metal NICKEL = metal("nickel");
    public static final Metal LITHIUM = metal("lithium");
    public static final Metal TUNGSTEN = metal("tungsten");
    public static final List<Metal> METALS = List.of(TIN, NICKEL, LITHIUM, TUNGSTEN);

    public static final Alloy STEEL = alloy("steel");
    public static final Alloy BRONZE = alloy("bronze");
    public static final Alloy STAINLESS_STEEL = alloy("stainless_steel");
    public static final List<Alloy> ALLOYS = List.of(STEEL, BRONZE, STAINLESS_STEEL);

    /** Block tints for the storage blocks (same colours as the items). */
    public static final Map<String, Integer> COLORS = new LinkedHashMap<>();

    static {
        COLORS.put("tin", 0xD8DEE6);
        COLORS.put("nickel", 0xD2C890);
        COLORS.put("lithium", 0xE6DCF2);
        COLORS.put("tungsten", 0x6A727C);
        COLORS.put("steel", 0x8E959E);
        COLORS.put("bronze", 0xCF8C3C);
        COLORS.put("stainless_steel", 0xE2E8EE);
    }

    public static final DeferredHolder<Item, Item> PLASTIC = item("plastic_sheet");
    public static final DeferredHolder<Item, Item> COPPER_WIRE = item("copper_wire");
    public static final DeferredHolder<Item, Item> CIRCUIT = item("circuit_board");
    public static final DeferredHolder<Item, Item> ADVANCED_CIRCUIT = item("advanced_circuit");

    public static final DeferredHolder<Block, Block> TEMPERED_GLASS = BLOCKS.register("tempered_glass", () ->
            new TransparentBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "tempered_glass")))
                    .strength(4.0F, 1200.0F).sound(SoundType.GLASS).noOcclusion()
                    .isValidSpawn((state, level, pos, type) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)));
    public static final DeferredHolder<Item, Item> TEMPERED_GLASS_ITEM = blockItem("tempered_glass", TEMPERED_GLASS);

    // ---------------------------------------------------------------- alloy smelter

    public static final DeferredHolder<Block, Block> ALLOY_SMELTER = BLOCKS.register("alloy_smelter", () ->
            new SimpleMachineBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "alloy_smelter")))
                    .strength(3.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(MachineBlock.LIT) ? 13 : 0),
                    () -> Materials.ALLOY_SMELTER_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> ALLOY_SMELTER_ITEM = blockItem("alloy_smelter", ALLOY_SMELTER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlloySmelterBlockEntity>> ALLOY_SMELTER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("alloy_smelter", () -> new BlockEntityType<>(AlloySmelterBlockEntity::new, ALLOY_SMELTER.value()));
    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMenu>> ALLOY_SMELTER_MENU = MENUS.register("alloy_smelter",
            () -> new MenuType<>((id, inventory) -> ProcessingMenu.powered(Materials.ALLOY_SMELTER_MENU.value(), id, inventory, 2), FeatureFlags.VANILLA_SET));

    // ---------------------------------------------------------------- creative tab

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("materials",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.factorycraft.materials"))
                    .icon(() -> STEEL.ingot().value().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        List<DeferredHolder<Item, Item>> items = new ArrayList<>();
                        for (Metal m : METALS) items.addAll(List.of(m.oreItem(), m.deepOreItem(), m.raw(), m.dust(), m.ingot(), m.nugget(), m.blockItem()));
                        for (Alloy a : ALLOYS) items.addAll(List.of(a.ingot(), a.plate(), a.blockItem()));
                        items.addAll(List.of(PLASTIC, COPPER_WIRE, CIRCUIT, ADVANCED_CIRCUIT, TEMPERED_GLASS_ITEM, ALLOY_SMELTER_ITEM));
                        for (DeferredHolder<Item, Item> item : items) output.accept(item.value());
                    }).build());
}
