package com.factorycraft;

import static com.factorycraft.FactoryCraft.BLOCKS;
import static com.factorycraft.FactoryCraft.ITEMS;
import static com.factorycraft.FactoryCraft.MOD_ID;
import static com.factorycraft.FactoryCraft.TABS;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Materials from the Nether (heat and raw power) and the End (stability and the void),
 * one tier above the overworld metals:
 * Nether: igneous crystal, sulfur, infernal cobalt, industrial quartz, soul fuel.
 * End: voidnite, stable ender crystal, purpurite, dragon essence, chorus insulation.
 */
@EventBusSubscriber(modid = MOD_ID)
public final class Exotic {
    private Exotic() {}

    static void init() {}

    private static BlockBehaviour.Properties props(String id) {
        return BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, id)))
                .requiresCorrectToolForDrops();
    }

    private static DeferredHolder<Block, Block> ore(String id, float hardness, SoundType sound, int light) {
        return BLOCKS.register(id, () -> new Block(props(id).strength(hardness, hardness + 3.0F).sound(sound).lightLevel(state -> light)));
    }

    private static DeferredHolder<Block, Block> storage(String id, float hardness, float resistance) {
        return BLOCKS.register(id, () -> new Block(props(id).strength(hardness, resistance).sound(SoundType.NETHERITE_BLOCK)));
    }

    private static DeferredHolder<Item, Item> blockItem(String id, DeferredHolder<Block, Block> block) {
        return ITEMS.registerItem(id, properties -> new BlockItem(block.value(), properties.useBlockDescriptionPrefix()));
    }

    private static DeferredHolder<Item, Item> item(String id) {
        return ITEMS.registerItem(id, Item::new);
    }

    private static DeferredHolder<Item, Item> rare(String id, Rarity rarity) {
        return ITEMS.registerItem(id, properties -> new Item(properties.rarity(rarity)));
    }

    // ---------------------------------------------------------------- Nether

    public static final DeferredHolder<Block, Block> IGNEOUS_ORE = ore("igneous_ore", 3.0F, SoundType.NETHER_ORE, 6);
    public static final DeferredHolder<Item, Item> IGNEOUS_ORE_ITEM = blockItem("igneous_ore", IGNEOUS_ORE);
    public static final DeferredHolder<Item, Item> IGNEOUS_CRYSTAL = item("igneous_crystal");

    public static final DeferredHolder<Block, Block> SULFUR_ORE = ore("sulfur_ore", 2.5F, SoundType.NETHER_ORE, 0);
    public static final DeferredHolder<Item, Item> SULFUR_ORE_ITEM = blockItem("sulfur_ore", SULFUR_ORE);
    public static final DeferredHolder<Item, Item> SULFUR = item("sulfur");

    public static final DeferredHolder<Block, Block> COBALT_ORE = ore("cobalt_ore", 4.0F, SoundType.NETHER_ORE, 0);
    public static final DeferredHolder<Item, Item> COBALT_ORE_ITEM = blockItem("cobalt_ore", COBALT_ORE);
    public static final DeferredHolder<Item, Item> RAW_COBALT = item("raw_cobalt");
    public static final DeferredHolder<Item, Item> COBALT_DUST = item("cobalt_dust");
    public static final DeferredHolder<Item, Item> COBALT_INGOT = item("cobalt_ingot");
    public static final DeferredHolder<Item, Item> COBALT_NUGGET = item("cobalt_nugget");
    public static final DeferredHolder<Block, Block> COBALT_BLOCK = storage("cobalt_block", 6.0F, 12.0F);
    public static final DeferredHolder<Item, Item> COBALT_BLOCK_ITEM = blockItem("cobalt_block", COBALT_BLOCK);

    public static final DeferredHolder<Item, Item> INDUSTRIAL_QUARTZ = item("industrial_quartz");
    public static final DeferredHolder<Item, Item> SOUL_FUEL = item("soul_fuel");

    // ---------------------------------------------------------------- End

    public static final DeferredHolder<Block, Block> VOIDNITE_ORE = ore("voidnite_ore", 6.0F, SoundType.STONE, 3);
    public static final DeferredHolder<Item, Item> VOIDNITE_ORE_ITEM = blockItem("voidnite_ore", VOIDNITE_ORE);
    public static final DeferredHolder<Item, Item> RAW_VOIDNITE = rare("raw_voidnite", Rarity.UNCOMMON);
    public static final DeferredHolder<Item, Item> VOIDNITE_DUST = rare("voidnite_dust", Rarity.UNCOMMON);
    public static final DeferredHolder<Item, Item> VOIDNITE_INGOT = rare("voidnite_ingot", Rarity.RARE);
    public static final DeferredHolder<Item, Item> VOIDNITE_NUGGET = rare("voidnite_nugget", Rarity.UNCOMMON);
    public static final DeferredHolder<Block, Block> VOIDNITE_BLOCK = storage("voidnite_block", 50.0F, 1200.0F);
    public static final DeferredHolder<Item, Item> VOIDNITE_BLOCK_ITEM = blockItem("voidnite_block", VOIDNITE_BLOCK);

    public static final DeferredHolder<Item, Item> STABLE_ENDER_CRYSTAL = rare("stable_ender_crystal", Rarity.UNCOMMON);
    public static final DeferredHolder<Item, Item> PURPURITE_DUST = item("purpurite_dust");
    public static final DeferredHolder<Item, Item> PURPURITE_INGOT = item("purpurite_ingot");
    public static final DeferredHolder<Item, Item> DRAGON_ESSENCE = rare("dragon_essence", Rarity.EPIC);
    public static final DeferredHolder<Item, Item> CHORUS_INSULATION = item("chorus_insulation");

    static {
        Materials.COLORS.put("cobalt", 0x3F6FE0);
        Materials.COLORS.put("voidnite", 0x4A2E7A);
    }

    // ---------------------------------------------------------------- dragon essence

    /** The dragon leaves some essence on the island when it dies (each time it's killed). */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon) || !(event.getEntity().level() instanceof ServerLevel level)) return;
        int x = 4;
        int z = 0;
        BlockPos ground = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
        ItemEntity drop = new ItemEntity(level, ground.getX() + 0.5, ground.getY() + 0.5, ground.getZ() + 0.5,
                new ItemStack(DRAGON_ESSENCE.value(), 6));
        drop.setUnlimitedLifetime();
        drop.setNoGravity(false);
        level.addFreshEntity(drop);
    }

    // ---------------------------------------------------------------- creative tab

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("exotic",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.factorycraft.exotic"))
                    .icon(() -> VOIDNITE_INGOT.value().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        for (DeferredHolder<Item, Item> item : List.of(IGNEOUS_ORE_ITEM, IGNEOUS_CRYSTAL, SULFUR_ORE_ITEM, SULFUR,
                                COBALT_ORE_ITEM, RAW_COBALT, COBALT_DUST, COBALT_INGOT, COBALT_NUGGET, COBALT_BLOCK_ITEM,
                                INDUSTRIAL_QUARTZ, SOUL_FUEL, VOIDNITE_ORE_ITEM, RAW_VOIDNITE, VOIDNITE_DUST, VOIDNITE_INGOT,
                                VOIDNITE_NUGGET, VOIDNITE_BLOCK_ITEM, STABLE_ENDER_CRYSTAL, PURPURITE_DUST, PURPURITE_INGOT,
                                DRAGON_ESSENCE, CHORUS_INSULATION)) {
                            output.accept(item.value());
                        }
                    }).build());
}
