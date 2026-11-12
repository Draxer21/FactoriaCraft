package com.factorycraft;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Registration of the core tiered mining and woodcutting tools. */
final class FactoryTools {
    static final DeferredHolder<Item, Item> STONE_HAMMER = registerHammer("stone_hammer", ToolMaterial.STONE);
    static final DeferredHolder<Item, Item> COPPER_HAMMER = registerHammer("copper_hammer", ToolMaterial.COPPER);
    static final DeferredHolder<Item, Item> IRON_HAMMER = registerHammer("iron_hammer", ToolMaterial.IRON);
    static final DeferredHolder<Item, Item> DIAMOND_HAMMER = registerHammer("diamond_hammer", ToolMaterial.DIAMOND);
    static final DeferredHolder<Item, Item> NETHERITE_HAMMER = registerHammer("netherite_hammer", ToolMaterial.NETHERITE);

    static final DeferredHolder<Item, Item> STONE_DRILL = registerDrill("stone_drill", ToolMaterial.STONE);
    static final DeferredHolder<Item, Item> COPPER_DRILL = registerDrill("copper_drill", ToolMaterial.COPPER);
    static final DeferredHolder<Item, Item> IRON_DRILL = registerDrill("iron_drill", ToolMaterial.IRON);
    static final DeferredHolder<Item, Item> DIAMOND_DRILL = registerDrill("diamond_drill", ToolMaterial.DIAMOND);
    static final DeferredHolder<Item, Item> NETHERITE_DRILL = registerDrill("netherite_drill", ToolMaterial.NETHERITE);

    static final DeferredHolder<Item, Item> STONE_CHAINSAW = registerChainsaw("stone_chainsaw", ToolMaterial.STONE);
    static final DeferredHolder<Item, Item> COPPER_CHAINSAW = registerChainsaw("copper_chainsaw", ToolMaterial.COPPER);
    static final DeferredHolder<Item, Item> IRON_CHAINSAW = registerChainsaw("iron_chainsaw", ToolMaterial.IRON);
    static final DeferredHolder<Item, Item> DIAMOND_CHAINSAW = registerChainsaw("diamond_chainsaw", ToolMaterial.DIAMOND);
    static final DeferredHolder<Item, Item> NETHERITE_CHAINSAW = registerChainsaw("netherite_chainsaw", ToolMaterial.NETHERITE);

    static final DeferredHolder<Item, Item> STONE_EXCAVATING_SHOVEL = registerShovel("stone_excavating_shovel", ToolMaterial.STONE);
    static final DeferredHolder<Item, Item> COPPER_EXCAVATING_SHOVEL = registerShovel("copper_excavating_shovel", ToolMaterial.COPPER);
    static final DeferredHolder<Item, Item> IRON_EXCAVATING_SHOVEL = registerShovel("iron_excavating_shovel", ToolMaterial.IRON);
    static final DeferredHolder<Item, Item> DIAMOND_EXCAVATING_SHOVEL = registerShovel("diamond_excavating_shovel", ToolMaterial.DIAMOND);
    static final DeferredHolder<Item, Item> NETHERITE_EXCAVATING_SHOVEL = registerShovel("netherite_excavating_shovel", ToolMaterial.NETHERITE);

    private FactoryTools() {}

    private static DeferredHolder<Item, Item> registerShovel(String id, ToolMaterial material) {
        return FactoryCraft.ITEMS.registerItem(id, properties -> new ExcavatingShovelItem(material, 2.0F, -3.2F,
                material == ToolMaterial.NETHERITE ? properties.fireResistant() : properties));
    }

    private static DeferredHolder<Item, Item> registerHammer(String id, ToolMaterial material) {
        return FactoryCraft.ITEMS.registerItem(id, properties -> new HammerItem(properties.pickaxe(material, 2.0F, -3.2F)));
    }

    private static DeferredHolder<Item, Item> registerDrill(String id, ToolMaterial material) {
        float speed = material == ToolMaterial.IRON ? -2.4F : -2.8F;
        return FactoryCraft.ITEMS.registerItem(id, properties -> new DrillItem(properties.pickaxe(material, 1.0F, speed)));
    }

    private static DeferredHolder<Item, Item> registerChainsaw(String id, ToolMaterial material) {
        return FactoryCraft.ITEMS.registerItem(id, properties -> new ChainsawItem(material, 5.0F, -2.8F, properties));
    }
}
