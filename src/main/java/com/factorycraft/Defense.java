package com.factorycraft;

import static com.factorycraft.FactoryCraft.BLOCKS;
import static com.factorycraft.FactoryCraft.BLOCK_ENTITY_TYPES;
import static com.factorycraft.FactoryCraft.ITEMS;
import static com.factorycraft.FactoryCraft.MOD_ID;
import static com.factorycraft.FactoryCraft.TABS;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Stage 8, defense and building: turret, alarm, security cameras and monitor,
 * industrial shutter, armoured door, reinforced concrete and armour plating,
 * steel scaffolding, lift pads and decorative factory blocks.
 */
public final class Defense {
    private Defense() {}

    static void init() {}

    /** Blocks at least this blast resistant survive even a mini-nuke. */
    public static final float BLAST_PROOF = 3_000_000.0F;

    private static BlockBehaviour.Properties props(String id) {
        return BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, id)));
    }

    private static BlockBehaviour.Properties machine(String id) {
        return props(id).strength(3.5F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    private static DeferredHolder<Item, Item> blockItem(String id, DeferredHolder<Block, Block> block) {
        return ITEMS.registerItem(id, properties -> new BlockItem(block.value(), properties.useBlockDescriptionPrefix()));
    }

    private static DeferredHolder<Block, Block> simple(String id, BlockBehaviour.Properties properties) {
        return BLOCKS.register(id, () -> new Block(properties));
    }

    // ---------------------------------------------------------------- security

    public static final DeferredHolder<Block, Block> TURRET = BLOCKS.register("turret", () ->
            new SimpleMachineBlock(machine("turret").strength(6.0F, 40.0F).noOcclusion(), () -> Defense.TURRET_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> TURRET_ITEM = blockItem("turret", TURRET);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TurretBlockEntity>> TURRET_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("turret", () -> new BlockEntityType<>(TurretBlockEntity::new, TURRET.value()));
    public static final DeferredHolder<Block, Block> ALARM = BLOCKS.register("alarm", () ->
            new AlarmBlock(machine("alarm").lightLevel(state -> state.getValue(MachineBlock.LIT) ? 13 : 0), () -> Defense.ALARM_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> ALARM_ITEM = blockItem("alarm", ALARM);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlarmBlockEntity>> ALARM_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("alarm", () -> new BlockEntityType<>(AlarmBlockEntity::new, ALARM.value()));
    public static final DeferredHolder<Block, Block> SECURITY_CAMERA = BLOCKS.register("security_camera", () ->
            new SimpleMachineBlock(machine("security_camera").noOcclusion(), () -> Defense.CAMERA_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> SECURITY_CAMERA_ITEM = blockItem("security_camera", SECURITY_CAMERA);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SecurityCameraBlockEntity>> CAMERA_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("security_camera", () -> new BlockEntityType<>(SecurityCameraBlockEntity::new, SECURITY_CAMERA.value()));
    public static final DeferredHolder<Block, Block> MONITOR = BLOCKS.register("monitor", () ->
            new SimpleMachineBlock(machine("monitor").lightLevel(state -> state.getValue(MachineBlock.LIT) ? 7 : 0), () -> Defense.MONITOR_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> MONITOR_ITEM = blockItem("monitor", MONITOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MonitorBlockEntity>> MONITOR_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("monitor", () -> new BlockEntityType<>(MonitorBlockEntity::new, MONITOR.value()));

    // ---------------------------------------------------------------- doors and armour

    public static final DeferredHolder<Block, Block> SHUTTER = BLOCKS.register("shutter", () ->
            new SimpleMachineBlock(machine("shutter").strength(8.0F, 1200.0F), () -> Defense.SHUTTER_BLOCK_ENTITY.value()));
    public static final DeferredHolder<Item, Item> SHUTTER_ITEM = blockItem("shutter", SHUTTER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShutterControllerBlockEntity>> SHUTTER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("shutter", () -> new BlockEntityType<>(ShutterControllerBlockEntity::new, SHUTTER.value()));
    public static final DeferredHolder<Block, Block> SHUTTER_PANEL = BLOCKS.register("shutter_panel", () ->
            new ShutterPanelBlock(props("shutter_panel").strength(50.0F, 3_600_000.0F).sound(SoundType.METAL)
                    .noOcclusion().pushReaction(PushReaction.BLOCK)));
    public static final DeferredHolder<Block, Block> ARMORED_DOOR = BLOCKS.register("armored_door", () ->
            new DoorBlock(BlockSetType.IRON, props("armored_door").strength(25.0F, 3_600_000.0F).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops().noOcclusion().pushReaction(PushReaction.BLOCK)));
    public static final DeferredHolder<Item, Item> ARMORED_DOOR_ITEM = ITEMS.registerItem("armored_door",
            properties -> new DoubleHighBlockItem(ARMORED_DOOR.value(), properties.useBlockDescriptionPrefix()));
    public static final DeferredHolder<Block, Block> REINFORCED_CONCRETE = simple("reinforced_concrete",
            props("reinforced_concrete").strength(12.0F, 1200.0F).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredHolder<Item, Item> REINFORCED_CONCRETE_ITEM = blockItem("reinforced_concrete", REINFORCED_CONCRETE);
    public static final DeferredHolder<Block, Block> ARMOR_PLATING = simple("armor_plating",
            props("armor_plating").strength(40.0F, 3_600_000.0F).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops());
    public static final DeferredHolder<Item, Item> ARMOR_PLATING_ITEM = blockItem("armor_plating", ARMOR_PLATING);

    // ---------------------------------------------------------------- building

    public static final DeferredHolder<Block, Block> STEEL_SCAFFOLD = BLOCKS.register("steel_scaffold", () ->
            new SteelScaffoldBlock(props("steel_scaffold").strength(1.5F, 6.0F).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredHolder<Item, Item> STEEL_SCAFFOLD_ITEM = blockItem("steel_scaffold", STEEL_SCAFFOLD);
    public static final DeferredHolder<Block, Block> LIFT_PAD = BLOCKS.register("lift_pad", () ->
            new LiftPadBlock(props("lift_pad").strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().lightLevel(state -> 5)));
    public static final DeferredHolder<Item, Item> LIFT_PAD_ITEM = blockItem("lift_pad", LIFT_PAD);
    public static final DeferredHolder<Block, Block> HAZARD_BLOCK = simple("hazard_block",
            props("hazard_block").strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredHolder<Item, Item> HAZARD_BLOCK_ITEM = blockItem("hazard_block", HAZARD_BLOCK);
    public static final DeferredHolder<Block, Block> STEEL_PANEL = simple("steel_panel",
            props("steel_panel").strength(4.0F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    public static final DeferredHolder<Item, Item> STEEL_PANEL_ITEM = blockItem("steel_panel", STEEL_PANEL);
    public static final DeferredHolder<Block, Block> STEEL_GRATE = simple("steel_grate",
            props("steel_grate").strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()
                    .isViewBlocking((state, level, pos) -> false).isSuffocating((state, level, pos) -> false));
    public static final DeferredHolder<Item, Item> STEEL_GRATE_ITEM = blockItem("steel_grate", STEEL_GRATE);
    public static final DeferredHolder<Block, Block> INDUSTRIAL_BRICKS = simple("industrial_bricks",
            props("industrial_bricks").strength(2.5F, 8.0F).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredHolder<Item, Item> INDUSTRIAL_BRICKS_ITEM = blockItem("industrial_bricks", INDUSTRIAL_BRICKS);
    public static final DeferredHolder<Block, Block> WARNING_LAMP = simple("warning_lamp",
            props("warning_lamp").strength(1.0F).sound(SoundType.GLASS).lightLevel(state -> 15));
    public static final DeferredHolder<Item, Item> WARNING_LAMP_ITEM = blockItem("warning_lamp", WARNING_LAMP);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("defense",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.factorycraft.defense"))
                    .icon(() -> TURRET_ITEM.value().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        for (DeferredHolder<Item, Item> item : List.of(TURRET_ITEM, ALARM_ITEM, SECURITY_CAMERA_ITEM, MONITOR_ITEM,
                                SHUTTER_ITEM, ARMORED_DOOR_ITEM, REINFORCED_CONCRETE_ITEM, ARMOR_PLATING_ITEM, STEEL_SCAFFOLD_ITEM,
                                LIFT_PAD_ITEM, HAZARD_BLOCK_ITEM, STEEL_PANEL_ITEM, STEEL_GRATE_ITEM, INDUSTRIAL_BRICKS_ITEM, WARNING_LAMP_ITEM)) {
                            output.accept(item.value());
                        }
                    }).build());
}
