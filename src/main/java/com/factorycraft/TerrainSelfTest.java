package com.factorycraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * CI only (-Dfactorycraft.selfTest=true): sets up a small scene in the sky for each
 * terrain machine, lets them run for 40 seconds and logs what they did.
 */
@EventBusSubscriber(modid = FactoryCraft.MOD_ID)
public final class TerrainSelfTest {
    private static final int START = 40;
    private static final int CHECK = START + 900;
    private static int ticks;
    private static BlockPos base;

    private TerrainSelfTest() {}

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (!Boolean.getBoolean("factorycraft.selfTest")) return;
        ticks++;
        ServerLevel level = event.getServer().overworld();
        try {
            if (ticks == START) setUp(level);
            if (ticks == START + 300 || ticks == START + 600 || ticks == CHECK) check(level);
        } catch (RuntimeException e) {
            com.mojang.logging.LogUtils.getLogger().info("TERRAINTEST FAILED {}", e.toString());
        }
    }

    private static void powered(ServerLevel level, BlockPos machine, Block block) {
        level.setBlockAndUpdate(machine, block.defaultBlockState());
        level.setBlockAndUpdate(machine.above(), FactoryCraft.GENERATOR.value().defaultBlockState());
        if (level.getBlockEntity(machine.above()) instanceof Energy.Source generator) generator.setStored(100_000);
    }

    private static void box(ServerLevel level, BlockPos from, BlockPos to, BlockState state) {
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) level.setBlockAndUpdate(pos, state);
    }

    private static void setUp(ServerLevel level) {
        base = new BlockPos(64, level.getMaxY() - 60, 64);
        // Keep the test scenes ticking even with nobody around.
        for (int x = base.getX() - 16; x <= base.getX() + 216; x += 16) {
            for (int z = base.getZ() - 16; z <= base.getZ() + 16; z += 16) level.setChunkForced(x >> 4, z >> 4, true);
        }
        BlockState stone = Blocks.STONE.defaultBlockState();

        // Filler: a 9x9 stone floor 4 below it, to fill 3 deep with dirt.
        BlockPos filler = base;
        box(level, filler.offset(-4, -4, -4), filler.offset(4, -4, 4), stone);
        powered(level, filler, FactoryCraft.FILLER.value());
        if (level.getBlockEntity(filler) instanceof FillerBlockEntity be) {
            be.radius = 4;
            for (int i = 0; i < 4; i++) be.setItem(i, new ItemStack(Items.DIRT, 64));
        }

        // Leveller: a 3x3x3 stone heap next to it, above its level.
        BlockPos leveler = base.offset(40, 0, 0);
        box(level, leveler.offset(-1, -1, -4), leveler.offset(1, -1, 4), stone);
        box(level, leveler.offset(-1, 0, 2), leveler.offset(1, 2, 4), stone);
        powered(level, leveler, FactoryCraft.LEVELER.value());
        if (level.getBlockEntity(leveler) instanceof AreaMachineBlockEntity be) be.radius = 4;

        // Quarry: a 5x5x3 stone block under it.
        BlockPos quarry = base.offset(80, 0, 0);
        box(level, quarry.offset(-2, -3, -2), quarry.offset(2, -1, 2), stone);
        powered(level, quarry, FactoryCraft.QUARRY.value());
        if (level.getBlockEntity(quarry) instanceof AreaMachineBlockEntity be) be.radius = 2;

        // Drain: a 3x3 pool in a stone basin next to it.
        BlockPos drain = base.offset(120, 0, 0);
        box(level, drain.offset(-3, -2, -3), drain.offset(3, -2, 3), stone);
        box(level, drain.offset(-3, -1, -3), drain.offset(3, -1, 3), stone);
        box(level, drain.offset(1, -1, -1), drain.offset(3, -1, 1), Blocks.WATER.defaultBlockState());
        powered(level, drain, FactoryCraft.DRAIN.value());
        if (level.getBlockEntity(drain) instanceof AreaMachineBlockEntity be) be.radius = 4;

        // Builder: copy a 3x3x2 cobblestone/plank thing and build it again.
        BlockPos model = base.offset(160, 10, 0);
        box(level, model, model.offset(2, 0, 2), Blocks.COBBLESTONE.defaultBlockState());
        box(level, model.above(), model.offset(2, 1, 2), Blocks.OAK_PLANKS.defaultBlockState());
        Blueprint blueprint = Blueprint.capture(level, model, model.offset(2, 1, 2));
        ItemStack paper = new ItemStack(FactoryCraft.BLUEPRINT.value());
        if (blueprint != null) blueprint.writeTo(paper);
        setUpMaterials(level);
        BlockPos builder = base.offset(160, 0, 0);
        powered(level, builder, FactoryCraft.BUILDER.value());
        if (level.getBlockEntity(builder) instanceof BuilderBlockEntity be) {
            be.setItem(0, paper);
            be.setItem(1, new ItemStack(Items.COBBLESTONE, 16));
            be.setItem(2, new ItemStack(Items.OAK_PLANKS, 16));
        }
    }

    private static void setUpMaterials(ServerLevel level) {
        BlockPos smelter = base.offset(200, 0, 0);
        powered(level, smelter, Materials.ALLOY_SMELTER.value());
        if (level.getBlockEntity(smelter) instanceof AlloySmelterBlockEntity be) {
            be.setItem(0, new ItemStack(Items.COAL, 4));
            be.setItem(1, new ItemStack(Items.IRON_INGOT, 2));
        }
    }

    private static int count(ServerLevel level, BlockPos from, BlockPos to, Block block) {
        int n = 0;
        for (BlockPos pos : BlockPos.betweenClosed(from, to)) if (level.getBlockState(pos).is(block)) n++;
        return n;
    }

    private static void check(ServerLevel level) {
        BlockPos leveler = base.offset(40, 0, 0);
        BlockPos quarry = base.offset(80, 0, 0);
        BlockPos drain = base.offset(120, 0, 0);
        BlockPos builder = base.offset(160, 0, 0);
        Direction front = level.getBlockState(builder).getValue(MachineBlock.FACING);
        int built = 0;
        for (BlockPos pos : BlockPos.betweenClosed(builder.offset(-6, 0, -6), builder.offset(6, 1, 6))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.COBBLESTONE) || state.is(Blocks.OAK_PLANKS)) built++;
        }
        com.mojang.logging.LogUtils.getLogger().info(
                "TERRAINTEST filled={}/243 levelled={}/27-left quarried={}/75-left drained={}/9-left built={}/18 front={}",
                count(level, base.offset(-4, -3, -4), base.offset(4, -1, 4), Blocks.DIRT),
                count(level, leveler.offset(-1, 0, 2), leveler.offset(1, 2, 4), Blocks.STONE),
                count(level, quarry.offset(-2, -3, -2), quarry.offset(2, -1, 2), Blocks.STONE),
                level.getFluidState(drain.offset(2, -1, 0)).isEmpty() ? 0
                        : count(level, drain.offset(1, -1, -1), drain.offset(3, -1, 1), Blocks.WATER),
                built, front);
        if (level.getBlockEntity(builder) instanceof BuilderBlockEntity be) {
            com.mojang.logging.LogUtils.getLogger().info("TERRAINTEST builder {}", be.debugState(level));
        }
        if (level.getBlockEntity(base.offset(200, 0, 0)) instanceof AlloySmelterBlockEntity be) {
            StringBuilder out = new StringBuilder();
            for (int i = 2; i < be.getContainerSize(); i++) if (!be.getItem(i).isEmpty()) out.append(be.getItem(i)).append(' ');
            var features = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.PLACED_FEATURE);
            int ores = 0;
            for (Materials.Metal m : Materials.METALS) {
                if (features.get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.PLACED_FEATURE,
                        net.minecraft.resources.Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, m.name() + "_ore"))).isPresent()) ores++;
            }
            var tin = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "ingots/tin"));
            int exotic = 0;
            for (String ore : new String[] {"igneous_ore", "sulfur_ore", "cobalt_ore", "voidnite_ore"}) {
                if (features.get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.PLACED_FEATURE,
                        net.minecraft.resources.Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, ore))).isPresent()) exotic++;
            }
            com.mojang.logging.LogUtils.getLogger().info("EXOTICTEST features={}/4 soulFuelBurn={} purpurCrushes={} alloyRecipes={}",
                    exotic, level.fuelValues().burnDuration(new ItemStack(Exotic.SOUL_FUEL.value())),
                    CrusherRecipes.resultFor(new ItemStack(Items.PURPUR_BLOCK)), AlloySmelterBlockEntity.RECIPES.size());
            ItemStack cobalt = new ItemStack(Exotic.COBALT_INGOT.value(), 3);
            ItemStack drill = ToolForge.forge(ToolForge.Kind.DRILL, cobalt, ToolForge.statsFor(cobalt));
            ItemStack tin = new ItemStack(Materials.TIN.ingot().value());
            ItemStack tinHammer = ToolForge.forge(ToolForge.Kind.HAMMER, tin, ToolForge.statsFor(tin));
            ItemStack igneous = new ItemStack(Exotic.IGNEOUS_CRYSTAL.value());
            ItemStack multitool = ToolForge.forge(ToolForge.Kind.MULTITOOL, igneous, ToolForge.statsFor(igneous));
            var smelt = level.recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,
                    new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(Items.RAW_IRON)), level);
            com.mojang.logging.LogUtils.getLogger().info(
                    "FORGETEST name={} durability={} cobaltMinesObsidian={} tinMinesObsidian={} tinMinesStone={} multitoolLogs={} multitoolTrait={} stickMaterial={} smeltRawIron={}",
                    drill.getHoverName().getString(), drill.getMaxDamage(),
                    drill.isCorrectToolForDrops(net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState()),
                    tinHammer.isCorrectToolForDrops(net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState()),
                    tinHammer.isCorrectToolForDrops(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()),
                    multitool.getDestroySpeed(net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState()),
                    ToolForge.trait(multitool), ToolForge.statsFor(new ItemStack(Items.STICK)),
                    smelt.map(r -> r.value().assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(Items.RAW_IRON)), level.registryAccess()).toString()).orElse("none"));
            com.mojang.logging.LogUtils.getLogger().info("MATERIALSTEST smelter={} oreFeatures={}/4 tinTagged={} pressWire={}",
                    out, ores, new ItemStack(Materials.TIN.ingot().value()).is(tin),
                    PressBlockEntity.RECIPES.stream().anyMatch(r -> r.output().get() == Materials.COPPER_WIRE.value()));
        }
    }
}
