package com.factorycraft;

import static com.factorycraft.FactoryCraft.BLOCKS;
import static com.factorycraft.FactoryCraft.BLOCK_ENTITY_TYPES;
import static com.factorycraft.FactoryCraft.ENTITY_TYPES;
import static com.factorycraft.FactoryCraft.ITEMS;
import static com.factorycraft.FactoryCraft.MENUS;
import static com.factorycraft.FactoryCraft.MOD_ID;
import static com.factorycraft.FactoryCraft.TABS;

import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Everything nuclear: uranium, thorium (weaker) and plutonium (stronger),
 * lead for shielding, the reactor, radiation and the nuclear weapons.
 */
public final class Nuclear {
    private Nuclear() {}

    /** Loads this class so its registrations are queued before the registers fire. */
    static void init() {}

    // ---------------------------------------------------------------- ores & storage blocks

    private static BlockBehaviour.Properties props(String id) {
        return BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, id)))
                .requiresCorrectToolForDrops();
    }

    private static DeferredHolder<Block, Block> ore(String id, boolean deep, int light) {
        return BLOCKS.register(id, () -> new Block(props(id)
                .strength(deep ? 4.5F : 3.0F, 3.0F)
                .sound(deep ? SoundType.DEEPSLATE : SoundType.STONE)
                .lightLevel(state -> light)));
    }

    private static DeferredHolder<Block, Block> storage(String id, int light) {
        return BLOCKS.register(id, () -> new Block(props(id).strength(5.0F, 6.0F).sound(SoundType.METAL)
                .lightLevel(state -> light)));
    }

    private static DeferredHolder<Item, Item> blockItem(String id, DeferredHolder<Block, Block> block) {
        return ITEMS.registerItem(id, properties -> new BlockItem(block.value(), properties.useBlockDescriptionPrefix()));
    }

    private static DeferredHolder<Item, Item> blockItem(String id, DeferredHolder<Block, Block> block, float radiation) {
        return ITEMS.registerItem(id, properties -> new RadioactiveBlockItem(block.value(), properties.useBlockDescriptionPrefix(), radiation));
    }

    public static final DeferredHolder<Block, Block> URANIUM_ORE = ore("uranium_ore", false, 4);
    public static final DeferredHolder<Block, Block> DEEPSLATE_URANIUM_ORE = ore("deepslate_uranium_ore", true, 4);
    public static final DeferredHolder<Block, Block> THORIUM_ORE = ore("thorium_ore", false, 2);
    public static final DeferredHolder<Block, Block> DEEPSLATE_THORIUM_ORE = ore("deepslate_thorium_ore", true, 2);
    public static final DeferredHolder<Block, Block> LEAD_ORE = ore("lead_ore", false, 0);
    public static final DeferredHolder<Block, Block> DEEPSLATE_LEAD_ORE = ore("deepslate_lead_ore", true, 0);
    public static final DeferredHolder<Block, Block> URANIUM_BLOCK = storage("uranium_block", 7);
    public static final DeferredHolder<Block, Block> THORIUM_BLOCK = storage("thorium_block", 4);
    public static final DeferredHolder<Block, Block> LEAD_BLOCK = storage("lead_block", 0);
    public static final DeferredHolder<Block, Block> PLUTONIUM_BLOCK = storage("plutonium_block", 10);

    public static final DeferredHolder<Item, Item> URANIUM_ORE_ITEM = blockItem("uranium_ore", URANIUM_ORE, 0.5F);
    public static final DeferredHolder<Item, Item> DEEPSLATE_URANIUM_ORE_ITEM = blockItem("deepslate_uranium_ore", DEEPSLATE_URANIUM_ORE, 0.5F);
    public static final DeferredHolder<Item, Item> THORIUM_ORE_ITEM = blockItem("thorium_ore", THORIUM_ORE, 0.25F);
    public static final DeferredHolder<Item, Item> DEEPSLATE_THORIUM_ORE_ITEM = blockItem("deepslate_thorium_ore", DEEPSLATE_THORIUM_ORE, 0.25F);
    public static final DeferredHolder<Item, Item> LEAD_ORE_ITEM = blockItem("lead_ore", LEAD_ORE);
    public static final DeferredHolder<Item, Item> DEEPSLATE_LEAD_ORE_ITEM = blockItem("deepslate_lead_ore", DEEPSLATE_LEAD_ORE);
    public static final DeferredHolder<Item, Item> URANIUM_BLOCK_ITEM = blockItem("uranium_block", URANIUM_BLOCK, 9.0F);
    public static final DeferredHolder<Item, Item> THORIUM_BLOCK_ITEM = blockItem("thorium_block", THORIUM_BLOCK, 4.5F);
    public static final DeferredHolder<Item, Item> LEAD_BLOCK_ITEM = blockItem("lead_block", LEAD_BLOCK);
    public static final DeferredHolder<Item, Item> PLUTONIUM_BLOCK_ITEM = blockItem("plutonium_block", PLUTONIUM_BLOCK, 27.0F);

    // ---------------------------------------------------------------- materials

    private static DeferredHolder<Item, Item> hot(String id, float radiation) {
        return ITEMS.registerItem(id, properties -> new RadioactiveItem(properties, radiation));
    }

    public static final DeferredHolder<Item, Item> RAW_URANIUM = hot("raw_uranium", 1.0F);
    public static final DeferredHolder<Item, Item> URANIUM_DUST = hot("uranium_dust", 1.0F);
    public static final DeferredHolder<Item, Item> URANIUM_INGOT = hot("uranium_ingot", 1.0F);
    public static final DeferredHolder<Item, Item> RAW_THORIUM = hot("raw_thorium", 0.5F);
    public static final DeferredHolder<Item, Item> THORIUM_DUST = hot("thorium_dust", 0.5F);
    public static final DeferredHolder<Item, Item> THORIUM_INGOT = hot("thorium_ingot", 0.5F);
    public static final DeferredHolder<Item, Item> PLUTONIUM_NUGGET = hot("plutonium_nugget", 0.35F);
    public static final DeferredHolder<Item, Item> PLUTONIUM_INGOT = hot("plutonium_ingot", 3.0F);
    public static final DeferredHolder<Item, Item> ENRICHED_URANIUM_1 = hot("enriched_uranium_1", 1.5F);
    public static final DeferredHolder<Item, Item> ENRICHED_URANIUM_2 = hot("enriched_uranium_2", 2.5F);
    public static final DeferredHolder<Item, Item> ENRICHED_URANIUM_3 = hot("enriched_uranium_3", 4.0F);
    public static final DeferredHolder<Item, Item> RAW_LEAD = ITEMS.registerItem("raw_lead", Item::new);
    public static final DeferredHolder<Item, Item> LEAD_DUST = ITEMS.registerItem("lead_dust", Item::new);
    public static final DeferredHolder<Item, Item> LEAD_INGOT = ITEMS.registerItem("lead_ingot", Item::new);

    // ---------------------------------------------------------------- fuel rods

    public static final DeferredHolder<Item, Item> THORIUM_FUEL_ROD = ITEMS.registerItem("thorium_fuel_rod",
            properties -> new RadioactiveItem(properties.stacksTo(16), 2.0F));
    public static final DeferredHolder<Item, Item> URANIUM_FUEL_ROD = ITEMS.registerItem("uranium_fuel_rod",
            properties -> new RadioactiveItem(properties.stacksTo(16), 4.0F));
    public static final DeferredHolder<Item, Item> PLUTONIUM_FUEL_ROD = ITEMS.registerItem("plutonium_fuel_rod",
            properties -> new RadioactiveItem(properties.stacksTo(16).rarity(Rarity.RARE), 8.0F));
    public static final DeferredHolder<Item, Item> DEPLETED_FUEL_ROD = ITEMS.registerItem("depleted_fuel_rod",
            properties -> new RadioactiveItem(properties.stacksTo(16), 1.5F));

    /** What each rod gives in the reactor. */
    public record Fuel(int power, int ticks, int heat) {}

    public static Fuel fuelOf(net.minecraft.world.item.ItemStack stack) {
        if (stack.is(THORIUM_FUEL_ROD.value())) return new Fuel(80, 6_000, 3);
        if (stack.is(URANIUM_FUEL_ROD.value())) return new Fuel(200, 10_000, 6);
        if (stack.is(PLUTONIUM_FUEL_ROD.value())) return new Fuel(500, 12_000, 12);
        return null;
    }

    // ---------------------------------------------------------------- reactor

    public static final DeferredHolder<Block, Block> REACTOR = BLOCKS.register("nuclear_reactor", () ->
            new SimpleMachineBlock(props("nuclear_reactor").strength(6.0F, 12.0F).sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(MachineBlock.LIT) ? 12 : 0),
                    () -> Nuclear.REACTOR_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> REACTOR_ITEM = blockItem("nuclear_reactor", REACTOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NuclearReactorBlockEntity>> REACTOR_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("nuclear_reactor", () -> new BlockEntityType<>(NuclearReactorBlockEntity::new, REACTOR.value()));
    public static final DeferredHolder<MenuType<?>, MenuType<ReactorMenu>> REACTOR_MENU = MENUS.register("nuclear_reactor",
            () -> new MenuType<>(ReactorMenu::client, FeatureFlags.VANILLA_SET));

    // ---------------------------------------------------------------- waste & contamination

    public static final DeferredHolder<Item, Item> NUCLEAR_WASTE = ITEMS.registerItem("nuclear_waste",
            properties -> new RadioactiveItem(properties, 6.0F));
    public static final DeferredHolder<Block, Block> CONTAMINATED_SOIL = BLOCKS.register("contaminated_soil", () ->
            new ContaminatedSoilBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "contaminated_soil")))
                    .strength(0.6F).sound(SoundType.GRAVEL).lightLevel(state -> 2)));
    public static final DeferredHolder<Item, Item> CONTAMINATED_SOIL_ITEM = blockItem("contaminated_soil", CONTAMINATED_SOIL, 0.5F);
    public static final DeferredHolder<Block, Block> WASTE_BARREL = BLOCKS.register("waste_barrel", () ->
            new SimpleMachineBlock(props("waste_barrel").strength(4.0F, 8.0F).sound(SoundType.METAL).lightLevel(state -> 0),
                    () -> Nuclear.WASTE_BARREL_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> WASTE_BARREL_ITEM = blockItem("waste_barrel", WASTE_BARREL);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WasteBarrelBlockEntity>> WASTE_BARREL_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("waste_barrel", () -> new BlockEntityType<>(WasteBarrelBlockEntity::new, WASTE_BARREL.value()));
    public static final DeferredHolder<Item, Item> GEIGER_COUNTER = ITEMS.registerItem("geiger_counter", GeigerCounterItem::new);
    public static final DeferredHolder<Item, Item> DECONTAMINATION_KIT = ITEMS.registerItem("decontamination_kit", DecontaminationKitItem::new);

    // ---------------------------------------------------------------- centrifuge

    public static final DeferredHolder<Block, Block> CENTRIFUGE = BLOCKS.register("centrifuge", () ->
            new SimpleMachineBlock(props("centrifuge").strength(3.5F, 6.0F).sound(SoundType.METAL)
                    .lightLevel(state -> state.getValue(MachineBlock.LIT) ? 6 : 0),
                    () -> Nuclear.CENTRIFUGE_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> CENTRIFUGE_ITEM = blockItem("centrifuge", CENTRIFUGE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CentrifugeBlockEntity>> CENTRIFUGE_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("centrifuge", () -> new BlockEntityType<>(CentrifugeBlockEntity::new, CENTRIFUGE.value()));
    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMenu>> CENTRIFUGE_MENU = MENUS.register("centrifuge",
            () -> new MenuType<>((id, inventory) -> ProcessingMenu.powered(Nuclear.CENTRIFUGE_MENU.value(), id, inventory, 1), FeatureFlags.VANILLA_SET));

    // ---------------------------------------------------------------- lead armour

    public static final TagKey<Item> LEAD_REPAIR = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "repairs_lead_armor"));
    public static final ResourceKey<EquipmentAsset> LEAD_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID,
            Identifier.fromNamespaceAndPath(MOD_ID, "lead"));
    public static final ArmorMaterial LEAD_ARMOR = new ArmorMaterial(20,
            Map.of(ArmorType.HELMET, 2, ArmorType.CHESTPLATE, 6, ArmorType.LEGGINGS, 5, ArmorType.BOOTS, 2, ArmorType.BODY, 5),
            9, SoundEvents.ARMOR_EQUIP_IRON, 0.5F, 0.1F, LEAD_REPAIR, LEAD_ASSET);

    private static DeferredHolder<Item, Item> armor(String id, ArmorType type) {
        return ITEMS.registerItem(id, properties -> new Item(properties.humanoidArmor(LEAD_ARMOR, type)));
    }

    public static final DeferredHolder<Item, Item> LEAD_HELMET = armor("lead_helmet", ArmorType.HELMET);
    public static final DeferredHolder<Item, Item> LEAD_CHESTPLATE = armor("lead_chestplate", ArmorType.CHESTPLATE);
    public static final DeferredHolder<Item, Item> LEAD_LEGGINGS = armor("lead_leggings", ArmorType.LEGGINGS);
    public static final DeferredHolder<Item, Item> LEAD_BOOTS = armor("lead_boots", ArmorType.BOOTS);

    // ---------------------------------------------------------------- weapons

    public static final DeferredHolder<Item, Item> GAMMA_RIFLE = ITEMS.registerItem("gamma_rifle",
            properties -> new GammaRifleItem(properties.stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredHolder<Item, Item> IRRADIATED_BLADE = ITEMS.registerItem("irradiated_blade",
            properties -> new IrradiatedBladeItem(properties.sword(ToolMaterial.NETHERITE, 4.0F, -2.4F).fireResistant().rarity(Rarity.EPIC)));
    public static final DeferredHolder<Item, Item> MINI_NUKE = ITEMS.registerItem("mini_nuke",
            properties -> new MiniNukeItem(properties.stacksTo(4).rarity(Rarity.RARE)));
    public static final DeferredHolder<EntityType<?>, EntityType<MiniNukeEntity>> MINI_NUKE_ENTITY = ENTITY_TYPES.register("mini_nuke", () ->
            EntityType.Builder.<MiniNukeEntity>of(MiniNukeEntity::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F)
                    .clientTrackingRange(8)
                    .updateInterval(5)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "mini_nuke"))));

    // ---------------------------------------------------------------- creative tab

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("nuclear",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.factorycraft.nuclear"))
                    .icon(() -> URANIUM_FUEL_ROD.value().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        for (DeferredHolder<Item, Item> item : java.util.List.of(
                                URANIUM_ORE_ITEM, DEEPSLATE_URANIUM_ORE_ITEM, THORIUM_ORE_ITEM, DEEPSLATE_THORIUM_ORE_ITEM,
                                LEAD_ORE_ITEM, DEEPSLATE_LEAD_ORE_ITEM,
                                RAW_URANIUM, URANIUM_DUST, URANIUM_INGOT, URANIUM_BLOCK_ITEM,
                                RAW_THORIUM, THORIUM_DUST, THORIUM_INGOT, THORIUM_BLOCK_ITEM,
                                RAW_LEAD, LEAD_DUST, LEAD_INGOT, LEAD_BLOCK_ITEM,
                                PLUTONIUM_NUGGET, PLUTONIUM_INGOT, PLUTONIUM_BLOCK_ITEM,
                                CENTRIFUGE_ITEM, ENRICHED_URANIUM_1, ENRICHED_URANIUM_2, ENRICHED_URANIUM_3,
                                NUCLEAR_WASTE, WASTE_BARREL_ITEM, CONTAMINATED_SOIL_ITEM, GEIGER_COUNTER, DECONTAMINATION_KIT,
                                THORIUM_FUEL_ROD, URANIUM_FUEL_ROD, PLUTONIUM_FUEL_ROD, DEPLETED_FUEL_ROD,
                                REACTOR_ITEM, LEAD_HELMET, LEAD_CHESTPLATE, LEAD_LEGGINGS, LEAD_BOOTS,
                                GAMMA_RIFLE, IRRADIATED_BLADE, MINI_NUKE)) {
                            output.accept(item.value());
                        }
                        output.accept(FactoryCraft.UPGRADE_ITEMS.get(Upgrade.NUCLEAR_ENGINE).value());
                    }).build());
}
