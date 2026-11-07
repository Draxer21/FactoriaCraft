package com.factorycraft;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Hold Shift on a FactoryCraft item to see its key numbers (power, range,
 * capacity...). The numbers come straight from the code, so they never drift
 * from what the machine really does. Texts: tooltip.factorycraft.info.*.
 */
@EventBusSubscriber(modid = FactoryCraft.MOD_ID, value = Dist.CLIENT)
public final class Tooltips {
    private record Info(String key, Supplier<Object[]> args) {}

    private static final Map<String, Info> INFO = new HashMap<>();

    static {
        info("chemical_bath", "chemical_bath", () -> new Object[] {ChemicalBathBlockEntity.ENERGY});
        info("oil_detector", "oil_detector", () -> new Object[0]);
        info("import_bus", "import_bus", () -> new Object[] {BusBlockEntity.PER_MOVE});
        info("export_bus", "export_bus", () -> new Object[] {BusBlockEntity.PER_MOVE});
        info("assembler", "assembler", () -> new Object[] {AssemblerBlockEntity.ENERGY});
        info("crusher", "crusher", () -> new Object[0]);
        info("industrial_furnace", "industrial_furnace", () -> new Object[] {IndustrialFurnaceBlockEntity.BATCH});
        info("generator", "generator", () -> new Object[] {GeneratorBlockEntity.OUTPUT, n(GeneratorBlockEntity.CAPACITY)});
        info("solar_generator", "solar_generator", () -> new Object[] {SolarGeneratorBlockEntity.OUTPUT, n(SolarGeneratorBlockEntity.CAPACITY), (int) SolarGeneratorBlockEntity.VEHICLE_RANGE});
        info("battery", "battery", () -> new Object[] {n(BatteryBlockEntity.CAPACITY)});
        info("windmill", "windmill", () -> new Object[] {WindmillBlockEntity.MAX});
        info("geothermal_generator", "geothermal_generator", () -> new Object[] {GeothermalGeneratorBlockEntity.PER_LAVA});
        info("hydro_turbine", "hydro_turbine", () -> new Object[] {HydroTurbineBlockEntity.MAX});
        info("lightning_collector", "lightning_collector", () -> new Object[] {n(LightningCollectorBlockEntity.PER_STRIKE)});
        info("nuclear_reactor", "nuclear_reactor", () -> new Object[] {n(NuclearReactorBlockEntity.CAPACITY), NuclearReactorBlockEntity.SHIELDING_NEEDED});
        info("antimatter_reactor", "antimatter_reactor", () -> new Object[] {AntimatterReactorBlockEntity.RATE, AntimatterReactorBlockEntity.BURN / 20});
        info("accelerator", "accelerator", () -> new Object[] {AcceleratorBlockEntity.MIN_MAGNETS});
        info("teleporter", "teleporter", () -> new Object[] {n(TeleporterBlockEntity.BASE_COST), n(TeleporterBlockEntity.CROSS_DIMENSION_COST)});
        info("turret", "turret", () -> new Object[] {(int) TurretBlockEntity.RANGE, (int) TurretBlockEntity.DAMAGE, TurretBlockEntity.ENERGY_PER_SHOT});
        info("alarm", "alarm", () -> new Object[] {(int) AlarmBlockEntity.MOB_RANGE, (int) AlarmBlockEntity.PLAYER_RANGE});
        info("research_station", "research_station", () -> new Object[0]);
        info("wireless_charger", "wireless_charger", () -> new Object[] {WirelessChargerBlockEntity.RANGE});
        info("drone_port", "drone_port", () -> new Object[] {DronePortBlockEntity.STACKS_PER_TRIP, n(DronePortBlockEntity.RANGE), DronePortBlockEntity.ENERGY_PER_TRIP});
        info("cable_station", "cable_station", () -> new Object[] {CableStationBlockEntity.MAX_LENGTH});
        info("fluid_tank", "fluid_tank", () -> new Object[] {FluidTankBlockEntity.CAPACITY / 1000});
        info("animal_farm", "animal_farm", () -> new Object[] {AnimalFarmBlockEntity.RANGE, AnimalFarmBlockEntity.CAP});
        info("recycler", "recycler", () -> new Object[] {(int) Math.round(RecyclerBlockEntity.RECOVERY * 100)});
        info("orbital_receiver", "orbital_receiver", () -> new Object[] {OrbitalReceiverBlockEntity.PER_SATELLITE, OrbitalReceiverBlockEntity.MAX_SATELLITES});
        info("launch_pad", "launch_pad", () -> new Object[] {LaunchPadBlockEntity.FUEL});
        info("industrial_lamp", "industrial_lamp", () -> new Object[] {IndustrialLampBlockEntity.RANGE, IndustrialLampBlockEntity.ENERGY});
        info("tanker", "tanker", () -> new Object[] {TankerEntity.CAPACITY});
        info("stone_drill", "stone_drill", () -> new Object[] {DrillItem.VEIN_MAX, DrillItem.TUNNEL_DEPTH});
        info("copper_drill", "stone_drill", () -> new Object[] {DrillItem.VEIN_MAX, DrillItem.TUNNEL_DEPTH});
        info("iron_drill", "stone_drill", () -> new Object[] {DrillItem.VEIN_MAX, DrillItem.TUNNEL_DEPTH});
        info("diamond_drill", "stone_drill", () -> new Object[] {DrillItem.VEIN_MAX, DrillItem.TUNNEL_DEPTH});
        info("netherite_drill", "stone_drill", () -> new Object[] {DrillItem.VEIN_MAX, DrillItem.TUNNEL_DEPTH});
        info("forged_drill", "stone_drill", () -> new Object[] {DrillItem.VEIN_MAX, DrillItem.TUNNEL_DEPTH});
        info("stone_hammer", "stone_hammer", () -> new Object[0]);
        info("copper_hammer", "stone_hammer", () -> new Object[0]);
        info("iron_hammer", "stone_hammer", () -> new Object[0]);
        info("diamond_hammer", "stone_hammer", () -> new Object[0]);
        info("netherite_hammer", "stone_hammer", () -> new Object[0]);
        info("forged_hammer", "stone_hammer", () -> new Object[0]);
        info("stone_chainsaw", "stone_chainsaw", () -> new Object[0]);
        info("copper_chainsaw", "stone_chainsaw", () -> new Object[0]);
        info("iron_chainsaw", "stone_chainsaw", () -> new Object[0]);
        info("diamond_chainsaw", "stone_chainsaw", () -> new Object[0]);
        info("netherite_chainsaw", "stone_chainsaw", () -> new Object[0]);
        info("forged_chainsaw", "stone_chainsaw", () -> new Object[0]);
        info("stone_excavating_shovel", "stone_excavating_shovel", () -> new Object[0]);
        info("copper_excavating_shovel", "stone_excavating_shovel", () -> new Object[0]);
        info("iron_excavating_shovel", "stone_excavating_shovel", () -> new Object[0]);
        info("diamond_excavating_shovel", "stone_excavating_shovel", () -> new Object[0]);
        info("netherite_excavating_shovel", "stone_excavating_shovel", () -> new Object[0]);
        info("forged_shovel", "stone_excavating_shovel", () -> new Object[0]);
    }

    private Tooltips() {}

    private static void info(String id, String key, Supplier<Object[]> args) {
        INFO.put(id, new Info("tooltip.factorycraft.info." + key, args));
    }

    /** Thousands separators, so 10000000 reads 10,000,000. */
    static String n(long value) {
        return NumberFormat.getIntegerInstance(Locale.ROOT).format(value);
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        Identifier id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (!id.getNamespace().equals(FactoryCraft.MOD_ID)) return;
        Info info = INFO.get(id.getPath());
        if (info == null) return;
        List<Component> lines = event.getToolTip();
        if (!Minecraft.getInstance().hasShiftDown()) {
            lines.add(Component.translatable("tooltip.factorycraft.shift").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        String text = Component.translatable(info.key(), info.args().get()).getString();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() > 0 && line.length() + word.length() > 40) {
                lines.add(Component.literal(line.toString()).withStyle(ChatFormatting.GRAY));
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) lines.add(Component.literal(line.toString()).withStyle(ChatFormatting.GRAY));
    }
}
