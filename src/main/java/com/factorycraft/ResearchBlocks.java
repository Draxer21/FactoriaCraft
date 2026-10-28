package com.factorycraft;

import static com.factorycraft.FactoryCraft.BLOCKS;
import static com.factorycraft.FactoryCraft.BLOCK_ENTITY_TYPES;
import static com.factorycraft.FactoryCraft.ITEMS;
import static com.factorycraft.FactoryCraft.MENUS;
import static com.factorycraft.FactoryCraft.MOD_ID;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Stage 11: the research station, and the rule that locked things can't be placed or used. */
@EventBusSubscriber(modid = MOD_ID)
public final class ResearchBlocks {
    private ResearchBlocks() {}

    static void init() {
        Research.init();
    }

    public static final DeferredHolder<Block, Block> STATION = BLOCKS.register("research_station", () ->
            new SimpleMachineBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, "research_station")))
                    .strength(3.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(MachineBlock.LIT) ? 9 : 3), () -> ResearchBlocks.STATION_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> STATION_ITEM = ITEMS.registerItem("research_station",
            properties -> new BlockItem(STATION.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResearchStationBlockEntity>> STATION_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("research_station", () -> new BlockEntityType<>(ResearchStationBlockEntity::new, STATION.value()));
    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMenu>> STATION_MENU = MENUS.register("research_station",
            () -> new MenuType<>((id, inventory) -> ProcessingMenu.powered(ResearchBlocks.STATION_MENU.value(), id, inventory,
                    ProcessingMenu.BIG_INPUTS, 0), FeatureFlags.VANILLA_SET));

    public static void sendTree(ServerPlayer player) {
        Research.State state = Research.state(player.level());
        PacketDistributor.sendToPlayer(player, new ResearchPayload(state.points(), state.unlocked(), FactoryConfig.researchRequired()));
    }

    private static void locked(net.minecraft.world.entity.player.Player player, Research.Node node) {
        player.sendOverlayMessage(Component.translatable("message.factorycraft.research.locked",
                Component.translatable("research.factorycraft." + node.id())));
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.player.Player player) || player.isCreative()) return;
        Research.Node node = Research.lockOf(player.level(), new net.minecraft.world.item.ItemStack(event.getPlacedBlock().getBlock().asItem()));
        if (node == null) return;
        event.setCanceled(true);
        locked(player, node);
    }

    @SubscribeEvent
    public static void onUseOnBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().isCreative() || event.getLevel().isClientSide()) return;
        Research.Node node = Research.lockOf(event.getLevel(), event.getItemStack());
        if (node == null) return;
        event.setCanceled(true);
        locked(event.getEntity(), node);
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity().isCreative() || event.getLevel().isClientSide()) return;
        Research.Node node = Research.lockOf(event.getLevel(), event.getItemStack());
        if (node == null) return;
        event.setCanceled(true);
        locked(event.getEntity(), node);
    }
}
