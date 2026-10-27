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
            if (ticks == START + 100) garageTest(level);
            if (ticks == START + 100) defenseMobs(level);
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
        for (int x = base.getX() - 16; x <= base.getX() + 336; x += 16) {
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
        setUpEnergy(level);
        setUpFarm(level);
        setUpLogistics(level);
        setUpDefense(level);
        BlockPos builder = base.offset(160, 0, 0);
        powered(level, builder, FactoryCraft.BUILDER.value());
        if (level.getBlockEntity(builder) instanceof BuilderBlockEntity be) {
            be.setItem(0, paper);
            be.setItem(1, new ItemStack(Items.COBBLESTONE, 16));
            be.setItem(2, new ItemStack(Items.OAK_PLANKS, 16));
        }
    }

    private static void setUpEnergy(ServerLevel level) {
        BlockPos turbine = base.offset(240, 0, 0);
        level.setBlockAndUpdate(turbine, FactoryCraft.HYDRO_TURBINE.value().defaultBlockState());
        level.setBlockAndUpdate(turbine.above(3), Blocks.WATER.defaultBlockState());
        level.setBlockAndUpdate(base.offset(250, 0, 0), FactoryCraft.PHOTON_ARRAY.value().defaultBlockState());
        BlockPos refinery = base.offset(260, 0, 0);
        powered(level, refinery, FactoryCraft.REFINERY.value());
        if (level.getBlockEntity(refinery) instanceof RefineryBlockEntity be) be.setItem(0, new ItemStack(FactoryCraft.CRUDE_OIL.value(), 2));
        BlockPos fermenter = base.offset(270, 0, 0);
        powered(level, fermenter, FactoryCraft.FERMENTER.value());
        if (level.getBlockEntity(fermenter) instanceof FermenterBlockEntity be) be.setItem(0, new ItemStack(Items.WHEAT, 16));
        BlockPos generator = base.offset(280, 0, 0);
        level.setBlockAndUpdate(generator, FactoryCraft.GENERATOR.value().defaultBlockState());
        if (level.getBlockEntity(generator) instanceof GeneratorBlockEntity be) be.setItem(0, new ItemStack(FactoryCraft.BIOFUEL.value(), 1));
    }

    private static void setUpFarm(ServerLevel level) {
        BlockPos greenhouse = base.offset(290, 0, 0);
        powered(level, greenhouse, FactoryCraft.GREENHOUSE.value());
        if (level.getBlockEntity(greenhouse) instanceof AreaMachineBlockEntity be) be.radius = 4;
        for (int i = 0; i < 6; i++) {
            BlockPos soil = greenhouse.offset(-2 + i, -2, 2);
            level.setBlockAndUpdate(soil.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(soil, Blocks.FARMLAND.defaultBlockState());
            level.setBlockAndUpdate(soil.above(), ((net.minecraft.world.level.block.CropBlock) Blocks.WHEAT).getStateForAge(i < 3 ? 7 : 0));
        }
        BlockPos tank = base.offset(300, 0, 0);
        level.setBlockAndUpdate(tank.below(), FactoryCraft.FLUID_TANK.value().defaultBlockState());
        level.setBlockAndUpdate(tank, FactoryCraft.FLUID_TANK.value().defaultBlockState());
        FluidStorage.offer(level, tank, Direction.UP, net.minecraft.world.level.material.Fluids.WATER, 5000);
        var handler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, tank, Direction.UP);
        if (handler != null) {
            try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                tankExtracted = handler.extract(net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER), 2000, transaction);
                transaction.commit();
            }
        }
        BlockPos desalinator = base.offset(310, 0, 0);
        powered(level, desalinator, FactoryCraft.DESALINATOR.value());
        level.setBlockAndUpdate(desalinator.east(), FactoryCraft.FLUID_TANK.value().defaultBlockState());
        FluidStorage.offer(level, desalinator, Direction.WEST, net.minecraft.world.level.material.Fluids.WATER, 3000);
        BlockPos plant = base.offset(320, 0, 0);
        powered(level, plant, FactoryCraft.TREATMENT_PLANT.value());
        if (level.getBlockEntity(plant) instanceof AreaMachineBlockEntity be) be.radius = 4;
        box(level, plant.offset(-2, -2, -2), plant.offset(2, -2, 2), Blocks.STONE.defaultBlockState());
        box(level, plant.offset(-2, -1, 1), plant.offset(2, -1, 2), Nuclear.CONTAMINATED_SOIL.value().defaultBlockState());
        FluidStorage.offer(level, plant, Direction.WEST, net.minecraft.world.level.material.Fluids.WATER, 4000);
        BlockPos grate = base.offset(330, 0, 0);
        level.setBlockAndUpdate(grate.below(), FactoryCraft.FLUID_TANK.value().defaultBlockState());
        level.setBlockAndUpdate(grate, FactoryCraft.SEWER_GRATE.value().defaultBlockState());
        level.setBlockAndUpdate(grate.above(), Blocks.WATER.defaultBlockState());
    }

    private static int tankExtracted = -1;
    private static String storageResult = "";
    private static String garageResult = "";

    private static void setUpLogistics(ServerLevel level) {
        BlockPos row = base.offset(0, 0, 16);
        // Storage: terminal - cable - cable - chest, an industrial chest above the first cable.
        BlockPos terminal = row;
        level.setBlockAndUpdate(terminal, Logistics.STORAGE_TERMINAL.value().defaultBlockState());
        level.setBlockAndUpdate(terminal.east(), Logistics.DATA_CABLE.value().defaultBlockState());
        level.setBlockAndUpdate(terminal.east(2), Logistics.DATA_CABLE.value().defaultBlockState());
        level.setBlockAndUpdate(terminal.east(3), Blocks.CHEST.defaultBlockState());
        if (level.getBlockEntity(terminal.east(3)) instanceof net.minecraft.world.Container chest) chest.setItem(0, new ItemStack(Items.IRON_INGOT, 10));
        level.setBlockAndUpdate(terminal.east().above(), FactoryCraft.INDUSTRIAL_CHEST.value().defaultBlockState());
        if (level.getBlockEntity(terminal.east().above()) instanceof net.minecraft.world.Container chest) chest.setItem(5, new ItemStack(Items.GOLD_INGOT, 5));
        StorageNetwork network = StorageNetwork.scan(level, terminal);
        int inventories = network.inventories();
        long iron = network.count(net.neoforged.neoforge.transfer.item.ItemResource.of(Items.IRON_INGOT));
        long gold = network.count(net.neoforged.neoforge.transfer.item.ItemResource.of(Items.GOLD_INGOT));
        ItemStack rest = network.insert(new ItemStack(Items.DIAMOND, 20));
        ItemStack taken = network.extract(net.neoforged.neoforge.transfer.item.ItemResource.of(Items.IRON_INGOT), 4);
        storageResult = "inventories=" + inventories + " iron=" + iron + " gold=" + gold + " insertRest=" + rest.getCount()
                + " diamonds=" + StorageNetwork.scan(level, terminal).count(net.neoforged.neoforge.transfer.item.ItemResource.of(Items.DIAMOND))
                + " extracted=" + taken.getCount() + " ironLeft=" + StorageNetwork.scan(level, terminal).count(net.neoforged.neoforge.transfer.item.ItemResource.of(Items.IRON_INGOT));

        // Drone delivery: port A -> port B, 40 blocks.
        BlockPos portA = row.offset(20, 0, 0);
        BlockPos portB = row.offset(60, 0, 0);
        powered(level, portA, Logistics.DRONE_PORT.value());
        powered(level, portB, Logistics.DRONE_PORT.value());
        if (level.getBlockEntity(portA) instanceof DronePortBlockEntity a) {
            a.setTarget(portB);
            a.dock();
            a.setItem(0, new ItemStack(Items.IRON_INGOT, 16));
            a.setItem(1, new ItemStack(Items.COAL, 8));
        }

        // Garage: park a cargo ship with coal in its hold, then take it out again.
        BlockPos garage = row.offset(80, 0, 0);
        box(level, garage.offset(-2, -1, -6), garage.offset(12, -1, 6), Blocks.STONE.defaultBlockState());
        powered(level, garage, Logistics.GARAGE.value());
        CargoShipEntity ship = Logistics.CARGO_SHIP.value().create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (ship != null) {
            ship.setPos(garage.getX() + 6.5, garage.getY(), garage.getZ() + 0.5);
            ship.setItem(0, new ItemStack(Items.COAL, 3));
            level.addFreshEntity(ship);
        }

        // Cable car: two stations 30 blocks apart.
        BlockPos stationA = row.offset(100, 0, 0);
        BlockPos stationB = row.offset(130, 2, 0);
        level.setBlockAndUpdate(stationA, Logistics.CABLE_STATION.value().defaultBlockState());
        level.setBlockAndUpdate(stationB, Logistics.CABLE_STATION.value().defaultBlockState());
        if (level.getBlockEntity(stationA) instanceof CableStationBlockEntity sa && level.getBlockEntity(stationB) instanceof CableStationBlockEntity sb) {
            sa.link(stationB, true);
            sb.link(stationA, false);
            CableCarEntity.create(level, stationA, stationB);
        }
    }

    /** Later than the set-up, once the forced chunks have their entities loaded. */
    private static void garageTest(ServerLevel level) {
        BlockPos garage = base.offset(80, 0, 16);
        if (!(level.getBlockEntity(garage) instanceof GarageBlockEntity be)) return;
        java.util.List<CargoShipEntity> ships = level.getEntitiesOfClass(CargoShipEntity.class, new net.minecraft.world.phys.AABB(garage).inflate(12));
        String holdBefore = ships.isEmpty() ? "none" : ships.get(0).getItem(0).toString();
        if (!ships.isEmpty()) ships.get(0).setItem(1, new ItemStack(Items.REDSTONE, 7));
        String parked = be.parkNearest().getString();
        ItemStack packed = be.getItem(0).copy();
        CargoShipEntity back = Logistics.CARGO_SHIP.value().create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (back != null) VehicleItem.restore(packed, back);
        garageResult = "shipsNear=" + ships.size() + " parked='" + parked + "' item=" + packed.getItem()
                + " shipGone=" + (ships.isEmpty() ? "?" : String.valueOf(ships.get(0).isRemoved()))
                + " holdBefore=" + holdBefore + " restoredHold=" + (back == null ? "null" : back.getItem(0) + "," + back.getItem(1)) + " fuel=" + GarageBlockEntity.fuelOf(packed)
                + " tag=" + packed.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY)
                        .copyTag().getCompound(GarageBlockEntity.VEHICLE_KEY).map(t -> t.keySet() + " items=" + t.get("Items")).orElse("none");
    }

    private static String blastResult = "";

    private static BlockPos defenseRow() {
        return base.offset(0, 0, -16);
    }

    private static void setUpDefense(ServerLevel level) {
        BlockPos row = defenseRow();
        BlockState stone = Blocks.STONE.defaultBlockState();
        // Turret on a stone floor, its generator beside it.
        box(level, row.offset(-2, -1, -3), row.offset(10, -1, 3), stone);
        level.setBlockAndUpdate(row, Defense.TURRET.value().defaultBlockState());
        level.setBlockAndUpdate(row.west(), FactoryCraft.GENERATOR.value().defaultBlockState());
        if (level.getBlockEntity(row.west()) instanceof Energy.Source generator) generator.setStored(100_000);
        // Alarm with a caged husk next to it.
        BlockPos alarm = row.offset(60, 0, 0);
        box(level, alarm.offset(-1, -1, -1), alarm.offset(5, -1, 1), stone);
        level.setBlockAndUpdate(alarm, Defense.ALARM.value().defaultBlockState());
        box(level, alarm.offset(3, 0, -1), alarm.offset(5, 2, 1), Blocks.GLASS.defaultBlockState());
        box(level, alarm.offset(4, 0, 0), alarm.offset(4, 1, 0), Blocks.AIR.defaultBlockState());
        // Shutter: two controllers 5 above a floor, a redstone block beside the first.
        BlockPos shutter = row.offset(100, 5, 0);
        box(level, shutter.offset(-1, -6, -1), shutter.offset(2, -6, 1), stone);
        level.setBlockAndUpdate(shutter, Defense.SHUTTER.value().defaultBlockState());
        level.setBlockAndUpdate(shutter.east(), Defense.SHUTTER.value().defaultBlockState());
        level.setBlockAndUpdate(shutter.west(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        // Lift pads 10 apart.
        BlockPos pad = row.offset(120, 0, 0);
        level.setBlockAndUpdate(pad, Defense.LIFT_PAD.value().defaultBlockState());
        level.setBlockAndUpdate(pad.above(10), Defense.LIFT_PAD.value().defaultBlockState());
        // TNT-sized blast between stone, reinforced concrete and armour plating.
        BlockPos blast = row.offset(140, 0, 0);
        level.setBlockAndUpdate(blast.west(), Defense.REINFORCED_CONCRETE.value().defaultBlockState());
        level.setBlockAndUpdate(blast.east(), stone);
        level.setBlockAndUpdate(blast.north(), Defense.ARMOR_PLATING.value().defaultBlockState());
        level.explode(null, blast.getX() + 0.5, blast.getY() + 0.5, blast.getZ() + 0.5, 4.0F, net.minecraft.world.level.Level.ExplosionInteraction.TNT);
        blastResult = "concrete=" + level.getBlockState(blast.west()).is(Defense.REINFORCED_CONCRETE.value())
                + " plating=" + level.getBlockState(blast.north()).is(Defense.ARMOR_PLATING.value())
                + " stone=" + level.getBlockState(blast.east()).is(Blocks.STONE);
    }

    private static net.minecraft.world.entity.@org.jspecify.annotations.Nullable Entity husk(ServerLevel level, double x, double y, double z) {
        var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("husk"));
        var husk = type.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (husk == null) return null;
        husk.setPos(x, y, z);
        if (husk instanceof net.minecraft.world.entity.Mob mob) mob.setPersistenceRequired();
        level.addFreshEntity(husk);
        return husk;
    }

    private static net.minecraft.world.entity.@org.jspecify.annotations.Nullable Entity turretTarget;

    private static void defenseMobs(ServerLevel level) {
        BlockPos row = defenseRow();
        turretTarget = husk(level, row.getX() + 7.5, row.getY(), row.getZ() + 0.5);
        husk(level, row.getX() + 64.5, row.getY(), row.getZ() + 0.5);
    }

    private static void logDefense(ServerLevel level) {
        BlockPos row = defenseRow();
        int shots = level.getBlockEntity(row) instanceof TurretBlockEntity t ? t.shots : -1;
        BlockPos alarm = row.offset(60, 0, 0);
        BlockPos shutter = row.offset(100, 5, 0);
        int depthA = level.getBlockEntity(shutter) instanceof ShutterControllerBlockEntity a ? a.depth(level) : -1;
        int depthB = level.getBlockEntity(shutter.east()) instanceof ShutterControllerBlockEntity b ? b.depth(level) : -1;
        BlockPos pad = row.offset(120, 0, 0);
        com.mojang.logging.LogUtils.getLogger().info(
                "DEFENSETEST turretShots={} targetDead={} alarmOn={} alarmSignal={} shutterDepth={}/{} liftUp={} liftDown={} blast[{}]",
                shots, turretTarget == null ? "null" : String.valueOf(!turretTarget.isAlive()),
                level.getBlockState(alarm).getValue(MachineBlock.LIT), level.hasNeighborSignal(alarm.north()),
                depthA, depthB, LiftPadBlock.next(level, pad, true), LiftPadBlock.next(level, pad.above(10), false), blastResult);
    }

    private static void logLogistics(ServerLevel level) {
        BlockPos row = base.offset(0, 0, 16);
        int drones = level.getBlockEntity(row.offset(20, 0, 0)) instanceof DronePortBlockEntity a ? a.drones() : -1;
        java.util.List<CableCarEntity> cars = level.getEntitiesOfClass(CableCarEntity.class,
                new net.minecraft.world.phys.AABB(net.minecraft.world.phys.Vec3.atCenterOf(row.offset(95, -10, -5)), net.minecraft.world.phys.Vec3.atCenterOf(row.offset(135, 10, 5))));
        String car = cars.isEmpty() ? "none" : String.format(java.util.Locale.ROOT, "x=%.1f y=%.1f", cars.get(0).getX() - row.getX(), cars.get(0).getY() - row.getY());
        int fuel = level.getBlockEntity(row.offset(80, 0, 0)) instanceof GarageBlockEntity g ? GarageBlockEntity.fuelOf(g.getItem(0)) : -2;
        com.mojang.logging.LogUtils.getLogger().info("LOGISTICSTEST storage[{}] droneB=[{}] dronesHomeA={} garage[{}] garageFuelNow={} cableCars={} car={}",
                storageResult, outputs(level, row.offset(60, 0, 0)), drones, garageResult, fuel, cars.size(), car);
    }

    private static String tank(ServerLevel level, BlockPos pos, int slot) {
        return level.getBlockEntity(pos) instanceof FluidStorage.Holder holder ? String.valueOf(holder.fluids().amount(slot)) : "none";
    }

    private static void logFarm(ServerLevel level) {
        BlockPos greenhouse = base.offset(290, 0, 0);
        int young = 0;
        for (int i = 0; i < 6; i++) {
            BlockState crop = level.getBlockState(greenhouse.offset(-2 + i, -1, 2));
            if (crop.is(Blocks.WHEAT) && !((net.minecraft.world.level.block.CropBlock) Blocks.WHEAT).isMaxAge(crop)) young++;
        }
        com.mojang.logging.LogUtils.getLogger().info(
                "FARMTEST greenhouse=[{}] notRipe={} wet={} tank={} extracted={} lowerTank={} desalinator=[{}] intake={} fresh={} sideTank={} contaminatedLeft={} plantWater={} sewerTank={} saltMelts={}",
                outputs(level, greenhouse), young,
                level.getBlockState(greenhouse.offset(0, -2, 2)).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.MOISTURE),
                tank(level, base.offset(300, 0, 0), 0), tankExtracted, tank(level, base.offset(300, -1, 0), 0),
                outputs(level, base.offset(310, 0, 0)), tank(level, base.offset(310, 0, 0), 0), tank(level, base.offset(310, 0, 0), 1),
                tank(level, base.offset(311, 0, 0), 0),
                count(level, base.offset(318, -1, -2), base.offset(322, -1, 2), Nuclear.CONTAMINATED_SOIL.value()),
                tank(level, base.offset(320, 0, 0), 0), tank(level, base.offset(330, -1, 0), 0),
                new ItemStack(FactoryCraft.SALT.value()).is(net.minecraft.tags.ItemTags.create(net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "dusts/salt"))));
    }

    private static String outputs(ServerLevel level, BlockPos pos) {
        StringBuilder out = new StringBuilder();
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity be) {
            for (int i = 0; i < be.getContainerSize(); i++) if (!be.getItem(i).isEmpty()) out.append(be.getItem(i)).append(' ');
        }
        return out.toString().trim();
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
            ItemStack drill = ToolForge.forge(ToolForge.Kind.DRILL, cobalt, ToolForge.statsFor(cobalt), level.registryAccess());
            ItemStack tinIngot = new ItemStack(Materials.TIN.ingot().value());
            ItemStack tinHammer = ToolForge.forge(ToolForge.Kind.HAMMER, tinIngot, ToolForge.statsFor(tinIngot), level.registryAccess());
            ItemStack igneous = new ItemStack(Exotic.IGNEOUS_CRYSTAL.value());
            ItemStack multitool = ToolForge.forge(ToolForge.Kind.MULTITOOL, igneous, ToolForge.statsFor(igneous), level.registryAccess());
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
                    smelt.map(r -> r.value().assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(Items.RAW_IRON))).toString()).orElse("none"));
            logFarm(level);
            logLogistics(level);
            logDefense(level);
            int turbineRate = level.getBlockEntity(base.offset(240, 0, 0)) instanceof Energy.Source t ? t.rate() : -1;
            int photonRate = level.getBlockEntity(base.offset(250, 0, 0)) instanceof Energy.Source t ? t.rate() : -1;
            com.mojang.logging.LogUtils.getLogger().info(
                    "ENERGYTEST turbine={} photon={} refinery=[{}] fermenter=[{}] generatorStored={} dieselBurn={} biofuelBurn={} oilHere={}",
                    turbineRate, photonRate, outputs(level, base.offset(260, 0, 0)), outputs(level, base.offset(270, 0, 0)),
                    level.getBlockEntity(base.offset(280, 0, 0)) instanceof Energy.Source g ? g.stored() : -1,
                    level.fuelValues().burnDuration(new ItemStack(FactoryCraft.DIESEL.value())),
                    level.fuelValues().burnDuration(new ItemStack(FactoryCraft.BIOFUEL.value())),
                    OilDeposits.richness(level, net.minecraft.world.level.ChunkPos.containing(base)));
            com.mojang.logging.LogUtils.getLogger().info("MATERIALSTEST smelter={} oreFeatures={}/4 tinTagged={} pressWire={}",
                    out, ores, new ItemStack(Materials.TIN.ingot().value()).is(tin),
                    PressBlockEntity.RECIPES.stream().anyMatch(r -> r.output().get() == Materials.COPPER_WIRE.value()));
        }
    }
}
