package com.factorycraft;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jspecify.annotations.Nullable;

/**
 * The research tree, shared by everyone in the world. Research stations turn
 * materials into research points; points unlock nodes of the tree; a node's
 * machines, vehicles and items can't be placed or used until it's unlocked
 * (unless the config turns research off, or the player is in creative mode).
 * Everything not in the tree is available from the start.
 */
public final class Research {
    private Research() {}

    /** One node of the tree. */
    public record Node(String id, int cost, List<String> requires, List<String> items) {}

    public static final List<Node> TREE = List.of(
            node("automation", 10, List.of(), "robot_arm", "sorter", "fast_conveyor", "express_conveyor", "elevator", "packager", "mixer"),
            node("electricity", 10, List.of(), "battery", "solar_generator", "windmill", "geothermal_generator", "steam_turbine",
                    "lightning_collector", "copper_coil", "copper_radiator"),
            node("advanced_vehicles", 15, List.of(), "tunneler", "bulldozer", "forester", "harvester", "paver", "tanker", "tank_wagon"),
            node("terrain", 25, List.of("automation"), "filler", "leveler", "quarry", "drain", "planter", "builder", "blueprint", "industrial_lamp"),
            node("metallurgy", 20, List.of("electricity"), "alloy_smelter", "tool_forge", "drill_mould", "hammer_mould", "chainsaw_mould",
                    "shovel_mould", "multitool_mould", "multitool"),
            node("farming", 20, List.of("electricity"), "greenhouse", "animal_farm", "fluid_tank", "sewer_grate", "desalinator", "treatment_plant"),
            node("oil", 25, List.of("electricity"), "pumpjack", "refinery", "fermenter"),
            node("advanced_power", 30, List.of("electricity"), "advanced_solar", "hydro_turbine", "wireless_charger"),
            node("heavy_lifting", 35, List.of("advanced_vehicles"), "crane", "tower_crane", "helicopter", "submarine", "crane_control"),
            node("logistics", 40, List.of("terrain"), "storage_terminal", "data_cable", "drone_port", "delivery_drone", "cable_station",
                    "steel_cable", "cargo_ship", "garage", "loading_station", "unloading_station", "route_beacon", "upgrade_autopilot"),
            node("economy", 30, List.of("automation", "farming"), "recycler", "vending_machine", "contract_board", "stats_terminal",
                    "zone_controller", "access_card"),
            node("defense", 40, List.of("metallurgy"), "turret", "alarm", "security_camera", "monitor", "shutter", "armored_door",
                    "armor_plating", "lift_pad"),
            node("gear", 40, List.of("metallurgy"), "exoskeleton", "jetpack_mk2"),
            node("nuclear", 60, List.of("advanced_power"), "nuclear_reactor", "centrifuge", "uranium_fuel_rod", "thorium_fuel_rod",
                    "plutonium_fuel_rod", "waste_barrel"),
            node("photonics", 50, List.of("advanced_power"), "photon_array"),
            node("weapons", 80, List.of("nuclear", "defense"), "mini_nuke", "gamma_rifle", "irradiated_blade"),
            node("nuclear_engines", 60, List.of("nuclear", "heavy_lifting"), "upgrade_nuclear_engine", "upgrade_fusion_engine"),
            node("space", 120, List.of("logistics", "oil"), "launch_pad", "rocket", "rocket_fuel", "solar_satellite", "survey_satellite",
                    "orbital_receiver", "uplink"),
            node("antimatter", 200, List.of("space", "nuclear"), "superconducting_magnet", "accelerator", "containment_cell",
                    "antimatter_reactor", "teleporter"));

    private static Node node(String id, int cost, List<String> requires, String... items) {
        return new Node(id, cost, requires, List.of(items));
    }

    /** Item -> the node that unlocks it. */
    private static final Map<Item, Node> LOCKS = new HashMap<>();

    private static synchronized Map<Item, Node> locks() {
        if (LOCKS.isEmpty()) {
            for (Node node : TREE) {
                for (String id : node.items()) {
                    Item item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, id));
                    if (item != net.minecraft.world.item.Items.AIR) LOCKS.put(item, node);
                }
            }
        }
        return LOCKS;
    }

    public static @Nullable Node node(String id) {
        for (Node node : TREE) if (node.id().equals(id)) return node;
        return null;
    }

    public static List<ItemStack> icons(Node node) {
        List<ItemStack> out = new ArrayList<>();
        for (String id : node.items()) {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, id));
            if (item != net.minecraft.world.item.Items.AIR) out.add(new ItemStack(item));
        }
        return out;
    }

    /** Column of a node: one more than its deepest prerequisite. */
    public static int tier(Node node) {
        int t = 0;
        for (String req : node.requires()) {
            Node r = node(req);
            if (r != null) t = Math.max(t, tier(r) + 1);
        }
        return t;
    }

    // ---------------------------------------------------------------- world state

    /** What the world has researched, and the points left to spend. */
    public record State(List<String> unlocked, int points) {
        public static final State EMPTY = new State(List.of(), 0);
        static final com.mojang.serialization.MapCodec<State> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.listOf().fieldOf("unlocked").forGetter(State::unlocked),
                Codec.INT.fieldOf("points").forGetter(State::points)).apply(i, State::new));

        public boolean has(String id) {
            return unlocked.contains(id);
        }
    }

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<State>> STATE = AirQuality.ATTACHMENTS.register("research",
            () -> AttachmentType.builder(() -> State.EMPTY).serialize(State.CODEC).build());

    static void init() {}

    public static State state(Level level) {
        MinecraftServer server = level.getServer();
        return server == null ? State.EMPTY : server.overworld().getData(STATE);
    }

    private static void set(Level level, State state) {
        MinecraftServer server = level.getServer();
        if (server != null) server.overworld().setData(STATE, state);
    }

    public static void addPoints(Level level, int points) {
        State s = state(level);
        set(level, new State(s.unlocked(), s.points() + points));
    }

    public static boolean available(State state, Node node) {
        if (state.has(node.id())) return false;
        for (String req : node.requires()) if (!state.has(req)) return false;
        return true;
    }

    /** Spends points on a node. Returns a message saying how it went. */
    public static Component unlock(Level level, String id) {
        Node node = node(id);
        State s = state(level);
        if (node == null) return Component.literal("?");
        if (s.has(id)) return Component.translatable("message.factorycraft.research.already");
        if (!available(s, node)) return Component.translatable("message.factorycraft.research.needs_first");
        if (s.points() < node.cost()) return Component.translatable("message.factorycraft.research.points", node.cost(), s.points());
        Set<String> unlocked = new LinkedHashSet<>(s.unlocked());
        unlocked.add(id);
        set(level, new State(new ArrayList<>(unlocked), s.points() - node.cost()));
        return Component.translatable("message.factorycraft.research.unlocked", Component.translatable("research.factorycraft." + id));
    }

    /** The node that still has to be researched before this item can be used, or null. */
    public static @Nullable Node lockOf(Level level, ItemStack stack) {
        if (!FactoryConfig.researchRequired() || stack.isEmpty()) return null;
        Node node = locks().get(stack.getItem());
        if (node == null || state(level).has(node.id())) return null;
        return node;
    }

    public static boolean allowed(Player player, ItemStack stack) {
        return player.isCreative() || lockOf(player.level(), stack) == null;
    }

    // ---------------------------------------------------------------- research points from materials

    private static final Map<String, Integer> VALUES = Map.ofEntries(
            Map.entry("factorycraft:iron_gear", 1), Map.entry("factorycraft:copper_wire", 1), Map.entry("factorycraft:steel_ingot", 1),
            Map.entry("factorycraft:plastic_sheet", 1), Map.entry("factorycraft:circuit_board", 4), Map.entry("factorycraft:advanced_circuit", 12),
            Map.entry("factorycraft:tin_ingot", 1), Map.entry("factorycraft:lead_ingot", 1), Map.entry("factorycraft:nickel_ingot", 1),
            Map.entry("factorycraft:lithium_ingot", 2), Map.entry("factorycraft:tungsten_ingot", 3), Map.entry("factorycraft:cobalt_ingot", 4),
            Map.entry("factorycraft:stainless_steel_ingot", 2), Map.entry("factorycraft:bronze_ingot", 1),
            Map.entry("factorycraft:voidnite_ingot", 8), Map.entry("factorycraft:stable_ender_crystal", 6), Map.entry("factorycraft:industrial_quartz", 3),
            Map.entry("factorycraft:igneous_crystal", 3), Map.entry("factorycraft:dragon_essence", 40), Map.entry("factorycraft:diesel", 2),
            Map.entry("factorycraft:biofuel", 1), Map.entry("factorycraft:uranium_ingot", 4), Map.entry("factorycraft:thorium_ingot", 4),
            Map.entry("factorycraft:plutonium_ingot", 12), Map.entry("factorycraft:antimatter_cell", 30),
            Map.entry("minecraft:book", 1), Map.entry("minecraft:enchanted_book", 5), Map.entry("minecraft:diamond", 4),
            Map.entry("minecraft:emerald", 2), Map.entry("minecraft:amethyst_shard", 1), Map.entry("minecraft:echo_shard", 6),
            Map.entry("minecraft:nether_star", 100), Map.entry("minecraft:ender_pearl", 2), Map.entry("minecraft:blaze_rod", 2));

    /** Research points one of this item is worth, or 0. */
    public static int value(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        return VALUES.getOrDefault(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), 0);
    }
}
