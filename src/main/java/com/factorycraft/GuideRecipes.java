package com.factorycraft;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * Recipes shown in the manual, so players without JEI/REI/EMI can see how to
 * make everything. Crafting, smelting and smithing recipes are read straight
 * from the mod's own recipe files (always in sync with the game); machine
 * conversions come from the machines' tables and Mechanic-only items from the
 * villager trade files.
 */
final class GuideRecipes {
    static final int WIDTH = 118;
    static final int HEIGHT = 68;
    private static final int SLOT = 18;

    /** A slot cycles through its options (tags) once a second. */
    record Slot(List<ItemStack> options, @Nullable String note) {
        static Slot of(ItemStack stack) {
            return new Slot(stack.isEmpty() ? List.of() : List.of(stack), null);
        }

        ItemStack shown() {
            if (options.isEmpty()) return ItemStack.EMPTY;
            return options.get((int) ((System.currentTimeMillis() / 1000L) % options.size()));
        }
    }

    /** {@code inputs} fill a 3x3 grid left to right; {@code station} is where it's made. */
    record Display(List<Slot> inputs, List<Slot> outputs, ItemStack station) {}

    /** Which recipes each chapter shows: recipe file names, or "trade:<path>", "press", "crusher", "washer". */
    private static final Map<String, String[]> CHAPTERS = Map.ofEntries(
            Map.entry("intro", new String[] {"guide_book"}),
            Map.entry("upgrades", new String[] {"wrench", "upgrade_turbo", "upgrade_efficient_engine", "upgrade_hybrid_engine",
                    "upgrade_solar_panel", "upgrade_big_tank", "upgrade_wide_drill", "upgrade_fortune", "upgrade_ore_filter",
                    "upgrade_magnet", "trade:mechanic/5/turbo_ii", "trade:mechanic/5/fortune_ii", "trade:mechanic/5/fusion_engine"}),
            Map.entry("excavator", new String[] {"excavator"}),
            Map.entry("tunneler", new String[] {"tunneler", "cargo_wagon"}),
            Map.entry("dump_truck", new String[] {"dump_truck"}),
            Map.entry("paver", new String[] {"paver"}),
            Map.entry("harvester", new String[] {"harvester"}),
            Map.entry("bulldozer", new String[] {"bulldozer"}),
            Map.entry("tanker", new String[] {"tanker"}),
            Map.entry("forester", new String[] {"forester"}),
            Map.entry("crane", new String[] {"crane", "crane_control"}),
            Map.entry("tower_crane", new String[] {"tower_crane"}),
            Map.entry("locomotive", new String[] {"locomotive", "cargo_wagon"}),
            Map.entry("drone", new String[] {"drone", "drone_controller"}),
            Map.entry("belts", new String[] {"conveyor", "fast_conveyor", "express_conveyor", "sorter", "elevator"}),
            Map.entry("processing", new String[] {"crusher", "industrial_furnace", "machine_upgrade_speed", "machine_upgrade_efficiency", "machine_upgrade_output", "iron_ingot_from_iron_dust_smelting",
                    "gold_ingot_from_gold_dust_smelting", "copper_ingot_from_copper_dust_smelting",
                    "iron_ingot_from_iron_dust_blasting", "gold_ingot_from_gold_dust_blasting",
                    "copper_ingot_from_copper_dust_blasting", "machine:crusher"}),
            Map.entry("well_drill", new String[] {"well_drill"}),
            Map.entry("nuclear", new String[] {"lead_ingot_from_raw_lead_smelting", "uranium_ingot_from_raw_uranium_smelting",
                    "thorium_ingot_from_raw_thorium_smelting", "uranium_ingot_from_uranium_dust_smelting",
                    "thorium_ingot_from_thorium_dust_smelting", "lead_ingot_from_lead_dust_smelting",
                    "plutonium_ingot", "uranium_block", "thorium_block", "lead_block", "plutonium_block",
                    "lead_helmet", "lead_chestplate", "lead_leggings", "lead_boots", "machine:nuclear"}),
            Map.entry("reactor", new String[] {"nuclear_reactor", "upgrade_nuclear_engine", "machine:rods"}),
            Map.entry("clean_power", new String[] {"chimney_filter", "air_purifier", "waste_barrel", "plant_inspector", "geiger_counter", "decontamination_kit"}),
            Map.entry("logistics", new String[] {"loading_station", "unloading_station", "tank_wagon", "route_beacon", "upgrade_autopilot"}),
            Map.entry("aircraft", new String[] {"helicopter", "submarine"}),
            Map.entry("materials", new String[] {"alloy_smelter", "machine:alloy", "circuit_board", "advanced_circuit",
                    "tin_ingot_from_raw_smelting", "nickel_ingot_from_raw_smelting", "lithium_ingot_from_raw_smelting",
                    "tungsten_ingot_from_raw_smelting", "steel_ingot_from_blasting_iron"}),
            Map.entry("exotic", new String[] {"igneous_crystal_from_ore_smelting", "gunpowder_from_sulfur_coal", "cobalt_ingot_from_raw_blasting",
                    "voidnite_ingot_from_raw_blasting", "cobalt_block", "voidnite_block"}),
            Map.entry("forge", new String[] {"tool_forge", "drill_mould", "hammer_mould", "chainsaw_mould", "shovel_mould", "multitool_mould",
                    "exoskeleton", "work_helmet", "work_jacket", "work_pants", "work_boots", "jetpack_mk2"}),
            Map.entry("research", new String[] {"research_station"}),
            Map.entry("endgame", new String[] {"superconducting_magnet", "accelerator", "containment_cell", "antimatter_reactor", "teleporter", "launch_pad", "rocket", "rocket_fuel", "solar_satellite", "survey_satellite", "orbital_receiver", "uplink"}),
            Map.entry("economy", new String[] {"air_meter", "recycler", "vending_machine", "contract_board", "stats_terminal", "zone_controller", "access_card"}),
            Map.entry("defense", new String[] {"turret", "alarm", "security_camera", "monitor", "shutter", "armored_door", "reinforced_concrete", "armor_plating", "steel_scaffold", "lift_pad", "hazard_block", "steel_grate"}),
            Map.entry("transport", new String[] {"storage_terminal", "data_cable", "drone_port", "delivery_drone", "cable_station", "steel_cable", "cargo_ship", "garage"}),
            Map.entry("farm", new String[] {"greenhouse", "animal_farm", "fluid_tank", "sewer_grate", "desalinator", "treatment_plant", "salt_block", "leather_from_salt"}),
            Map.entry("oil", new String[] {"pumpjack", "refinery", "fermenter", "hydro_turbine", "advanced_solar", "photon_array", "wireless_charger"}),
            Map.entry("terrain", new String[] {"filler", "leveler", "quarry", "drain", "planter", "blueprint", "builder", "industrial_lamp"}),
            Map.entry("weapons", new String[] {"gamma_rifle", "irradiated_blade", "centrifuge", "machine:centrifuge",
                    "mini_nuke_grade_1", "mini_nuke_grade_2", "mini_nuke_grade_3", "mini_nuke_plutonium"}),
            Map.entry("power", new String[] {"generator", "solar_generator", "windmill", "geothermal_generator", "steam_turbine", "lightning_collector", "copper_coil", "copper_radiator", "battery", "energy_meter", "cable"}),
            Map.entry("powered", new String[] {"ore_washer", "press", "mixer", "packager", "machine:press", "machine:washer",
                    "tannery", "machine:tannery", "leather_from_rotten_flesh_smoking", "incinerator", "filler"}),
            Map.entry("automation", new String[] {"robot_arm", "pump", "fluid_pipe"}),
            Map.entry("station", new String[] {"control_station"}),
            Map.entry("mechanic", new String[] {"mechanic_bench", "trade:mechanic/1/emerald_iron_plate", "trade:mechanic/1/emerald_cable",
                    "trade:mechanic/2/emerald_iron_gear", "trade:mechanic/2/emerald_fluid_pipe", "trade:mechanic/2/emerald_wrench",
                    "trade:mechanic/3/emerald_turbo", "trade:mechanic/3/emerald_efficient_engine",
                    "trade:mechanic/3/emerald_solar_panel", "trade:mechanic/3/emerald_magnet",
                    "trade:mechanic/4/prototype_drill", "trade:mechanic/4/industrial_chainsaw", "trade:mechanic/4/titan_hammer",
                    "trade:mechanic/5/turbo_ii", "trade:mechanic/5/fortune_ii", "trade:mechanic/5/fusion_engine"}),
            Map.entry("tools", new String[] {"stone_drill", "copper_drill", "iron_drill", "diamond_drill", "netherite_drill_smithing",
                    "stone_chainsaw", "copper_chainsaw", "iron_chainsaw", "diamond_chainsaw", "netherite_chainsaw_smithing",
                    "stone_hammer", "copper_hammer", "iron_hammer", "diamond_hammer", "netherite_hammer_smithing",
                    "stone_excavating_shovel", "copper_excavating_shovel", "iron_excavating_shovel", "diamond_excavating_shovel",
                    "netherite_excavating_shovel_smithing", "wrench", "repair_station", "giant_drill", "jetpack", "magnet", "industrial_chest",
                    "trade:mechanic/4/prototype_drill", "trade:mechanic/4/industrial_chainsaw", "trade:mechanic/4/titan_hammer"}));

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final Map<String, List<Display>> CACHE = new HashMap<>();

    private GuideRecipes() {}

    static List<Display> forChapter(String chapter) {
        return CACHE.computeIfAbsent(chapter, key -> {
            List<Display> result = new ArrayList<>();
            for (String source : CHAPTERS.getOrDefault(key, new String[0])) {
                try {
                    if (source.startsWith("machine:")) {
                        machine(source.substring(8), result);
                    } else if (source.startsWith("trade:")) {
                        Display display = trade(source.substring(6));
                        if (display != null) result.add(display);
                    } else {
                        Display display = recipe(source);
                        if (display != null) result.add(display);
                    }
                } catch (RuntimeException e) {
                    LOGGER.warn("Guide: couldn't show recipe {}", source, e);
                }
            }
            return result;
        });
    }

    // ---------------------------------------------------------------- loading

    private static @Nullable JsonObject json(String path) {
        try (InputStream in = GuideRecipes.class.getResourceAsStream("/data/factorycraft/" + path + ".json")) {
            if (in == null) return null;
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    private static @Nullable Display recipe(String name) {
        JsonObject json = json("recipe/" + name);
        if (json == null) return null;
        String type = json.get("type").getAsString();
        ItemStack result = stack(json.get("result"));
        List<Slot> out = List.of(Slot.of(result));
        List<Slot> in = new ArrayList<>();
        switch (type) {
            case "minecraft:crafting_shaped" -> {
                JsonObject key = json.getAsJsonObject("key");
                var pattern = json.getAsJsonArray("pattern");
                for (int row = 0; row < 3; row++) {
                    String line = row < pattern.size() ? pattern.get(row).getAsString() : "";
                    for (int col = 0; col < 3; col++) {
                        char c = col < line.length() ? line.charAt(col) : ' ';
                        in.add(c == ' ' || !key.has(String.valueOf(c)) ? Slot.of(ItemStack.EMPTY)
                                : ingredient(key.get(String.valueOf(c))));
                    }
                }
                return new Display(in, out, new ItemStack(Items.CRAFTING_TABLE));
            }
            case "minecraft:crafting_shapeless" -> {
                for (JsonElement element : json.getAsJsonArray("ingredients")) in.add(ingredient(element));
                return new Display(in, out, new ItemStack(Items.CRAFTING_TABLE));
            }
            case "minecraft:smelting", "minecraft:blasting", "minecraft:smoking" -> {
                in.add(Slot.of(ItemStack.EMPTY));
                in.add(Slot.of(ItemStack.EMPTY));
                in.add(Slot.of(ItemStack.EMPTY));
                in.add(Slot.of(ItemStack.EMPTY));
                in.add(ingredient(json.get("ingredient")));
                return new Display(in, out, new ItemStack(type.equals("minecraft:smelting") ? Items.FURNACE
                        : type.equals("minecraft:smoking") ? Items.SMOKER : Items.BLAST_FURNACE));
            }
            case "minecraft:smithing_transform" -> {
                in.add(Slot.of(ItemStack.EMPTY));
                in.add(Slot.of(ItemStack.EMPTY));
                in.add(Slot.of(ItemStack.EMPTY));
                in.add(ingredient(json.get("template")));
                in.add(ingredient(json.get("base")));
                in.add(ingredient(json.get("addition")));
                return new Display(in, out, new ItemStack(Items.SMITHING_TABLE));
            }
            default -> {
                return null;
            }
        }
    }

    private static @Nullable Display trade(String path) {
        JsonObject json = json("villager_trade/" + path);
        if (json == null) return null;
        List<Slot> in = new ArrayList<>();
        in.add(Slot.of(ItemStack.EMPTY));
        in.add(Slot.of(ItemStack.EMPTY));
        in.add(Slot.of(ItemStack.EMPTY));
        in.add(Slot.of(stack(json.get("wants"))));
        if (json.has("additional_wants")) in.add(Slot.of(stack(json.get("additional_wants"))));
        return new Display(in, List.of(Slot.of(stack(json.get("gives")))), item("factorycraft:mechanic_bench"));
    }

    /** Machine conversions for recipe viewers (crusher, washer, press, centrifuge). */
    static List<Display> machineDisplays(String which) {
        List<Display> result = new ArrayList<>();
        machine(which, result);
        return result;
    }

    static ItemStack itemStack(String id) {
        return item(id);
    }

    private static void machine(String which, List<Display> result) {
        switch (which) {
            case "press" -> {
                ItemStack station = item("factorycraft:press");
                for (PressBlockEntity.Pressing p : PressBlockEntity.RECIPES) {
                    result.add(single(new ItemStack(p.input().get(), p.count()),
                            List.of(Slot.of(new ItemStack(p.output().get(), p.outCount()))), station));
                }
            }
            case "crusher" -> {
                ItemStack station = item("factorycraft:crusher");
                for (CrusherRecipes.Entry e : CrusherRecipes.ENTRIES) {
                    Slot input = e.source() instanceof Item item ? Slot.of(new ItemStack(item))
                            : e.source() instanceof TagKey<?> tag ? tagSlot(tag.location()) : Slot.of(ItemStack.EMPTY);
                    result.add(single(input, List.of(Slot.of(new ItemStack(e.output().get(), e.count()))), station));
                }
            }
            case "washer" -> {
                ItemStack station = item("factorycraft:ore_washer");
                OreWasherBlockEntity.TABLE.entrySet().stream()
                        .sorted((a, b) -> BuiltInRegistries.ITEM.getKey(a.getKey()).compareTo(BuiltInRegistries.ITEM.getKey(b.getKey())))
                        .forEach(entry -> {
                            List<Slot> outs = new ArrayList<>();
                            for (OreWasherBlockEntity.Find find : entry.getValue()) {
                                outs.add(new Slot(List.of(new ItemStack(find.item().get())),
                                        Math.round(find.chance() * 100) + "%"));
                            }
                            result.add(single(new ItemStack(entry.getKey()), outs, station));
                        });
            }
            case "nuclear" -> {
                List<Display> crusher = new ArrayList<>();
                machine("crusher", crusher);
                for (Display d : crusher) {
                    ItemStack in = d.inputs().get(4).shown();
                    if (in.getItem() instanceof Radiation.Source || in.is(Nuclear.RAW_LEAD.value())
                            || in.is(Nuclear.LEAD_ORE_ITEM.value()) || in.is(Nuclear.DEEPSLATE_LEAD_ORE_ITEM.value())) {
                        result.add(d);
                    }
                }
                List<Display> washer = new ArrayList<>();
                machine("washer", washer);
                for (Display d : washer) {
                    if (d.inputs().get(4).shown().is(Nuclear.DEPLETED_FUEL_ROD.value())) result.add(d);
                }
            }
            case "alloy" -> {
                ItemStack station = item("factorycraft:alloy_smelter");
                for (AlloySmelterBlockEntity.Alloying r : AlloySmelterBlockEntity.RECIPES) {
                    List<Slot> in = new ArrayList<>();
                    for (int i = 0; i < 3; i++) in.add(Slot.of(ItemStack.EMPTY));
                    in.add(shown(r.shownA(), r.countA()));
                    in.add(Slot.of(ItemStack.EMPTY));
                    in.add(shown(r.shownB(), r.countB()));
                    result.add(new Display(in, List.of(Slot.of(new ItemStack(r.output().get(), r.outCount()))), station));
                }
            }
            case "tannery" -> {
                ItemStack station = item("factorycraft:tannery");
                for (Item hide : List.of(Items.ROTTEN_FLESH, Items.RABBIT_HIDE)) {
                    result.add(single(new ItemStack(hide, TanneryBlockEntity.perLeather(hide)),
                            List.of(Slot.of(new ItemStack(Items.LEATHER))), station));
                }
            }
            case "centrifuge" -> {
                ItemStack station = item("factorycraft:centrifuge");
                for (CentrifugeBlockEntity.Stage stage : CentrifugeBlockEntity.STAGES) {
                    result.add(single(new ItemStack(stage.input().get(), stage.count()),
                            List.of(Slot.of(new ItemStack(stage.output().get()))), station));
                }
            }
            case "rods" -> {
                List<Display> press = new ArrayList<>();
                machine("press", press);
                for (Display d : press) {
                    if (d.outputs().get(0).shown().getItem() instanceof Radiation.Source) result.add(d);
                }
            }
            default -> { }
        }
    }

    /** An item or a "c:..." tag, shown {@code count} at a time. */
    private static Slot shown(Object what, int count) {
        Slot base = what instanceof String tag ? tagSlot(Identifier.parse(tag)) : Slot.of(new ItemStack((Item) what));
        List<ItemStack> options = new ArrayList<>();
        for (ItemStack option : base.options()) options.add(option.copyWithCount(count));
        return new Slot(options, null);
    }

    private static Display single(ItemStack input, List<Slot> outputs, ItemStack station) {
        return single(Slot.of(input), outputs, station);
    }

    private static Display single(Slot input, List<Slot> outputs, ItemStack station) {
        List<Slot> in = new ArrayList<>();
        for (int i = 0; i < 4; i++) in.add(Slot.of(ItemStack.EMPTY));
        in.add(input);
        return new Display(in, outputs, station);
    }

    private static Slot ingredient(JsonElement element) {
        if (element.isJsonArray()) {
            List<ItemStack> options = new ArrayList<>();
            for (JsonElement e : element.getAsJsonArray()) options.addAll(ingredient(e).options());
            return new Slot(options, null);
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("tag")) return tagSlot(Identifier.parse(object.get("tag").getAsString()));
            if (object.has("item")) return Slot.of(item(object.get("item").getAsString()));
            return Slot.of(stack(object));
        }
        String id = element.getAsString();
        if (id.startsWith("#")) return tagSlot(Identifier.parse(id.substring(1)));
        return Slot.of(item(id));
    }

    /** Tags are synced to the client, so we just test every item against it. */
    private static Slot tagSlot(Identifier id) {
        TagKey<Item> tag = TagKey.create(Registries.ITEM, id);
        List<ItemStack> options = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item);
            if (!stack.isEmpty() && stack.is(tag)) options.add(stack);
        }
        return new Slot(options, null);
    }

    private static ItemStack stack(JsonElement element) {
        if (element.isJsonPrimitive()) return item(element.getAsString());
        JsonObject object = element.getAsJsonObject();
        ItemStack stack = item(object.get("id").getAsString());
        if (object.has("count")) stack.setCount(Math.max(1, (int) object.get("count").getAsDouble()));
        if (object.has("components") && object.getAsJsonObject("components").has("minecraft:custom_data")) {
            JsonElement data = object.getAsJsonObject("components").get("minecraft:custom_data");
            if (data.isJsonObject() && data.getAsJsonObject().has("Yield")) {
                int yield = data.getAsJsonObject().get("Yield").getAsInt();
                net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                        stack, tag -> tag.putInt("Yield", yield));
            }
        }
        return stack;
    }

    private static ItemStack item(String id) {
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    // ---------------------------------------------------------------- drawing

    /** Draws one recipe block; returns the stack under the mouse, if any. */
    static ItemStack draw(GuiGraphicsExtractor graphics, Font font, Display display, int x, int y, int mouseX, int mouseY) {
        ItemStack hovered = ItemStack.EMPTY;
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, 0xFFDCD3BC);
        graphics.fill(x, y, x + WIDTH, y + 1, 0xFFB9AE92);

        // Title: the result's name.
        // Title: the result's name (or the input's, when one input gives several possible finds).
        ItemStack main = display.outputs().isEmpty() ? ItemStack.EMPTY : display.outputs().get(0).shown();
        if (display.outputs().size() > 1 && display.inputs().size() > 4) main = display.inputs().get(4).shown();
        String name = main.getHoverName().getString();
        if (main.getItem() instanceof MiniNukeItem) {
            name += " · " + net.minecraft.network.chat.Component.translatable(
                    "tooltip.factorycraft.mini_nuke.yield." + MiniNukeItem.coreGrade(main)).getString();
        }
        String title = font.width(name) <= WIDTH - 8 ? name : font.plainSubstrByWidth(name, WIDTH - 8 - font.width("…")) + "…";
        graphics.text(font, title, x + 4, y + 3, 0xFF3A4047, false);

        int gridX = x + 4;
        int gridY = y + 13;
        for (int i = 0; i < 9; i++) {
            int sx = gridX + (i % 3) * SLOT;
            int sy = gridY + (i / 3) * SLOT;
            Slot slot = i < display.inputs().size() ? display.inputs().get(i) : null;
            ItemStack stack = slot == null ? ItemStack.EMPTY : slot.shown();
            if (isGridRecipe(display) || !stack.isEmpty()) {
                drawSlot(graphics, sx, sy);
                hovered = item(graphics, font, stack, slot == null ? null : slot.note(), sx, sy, mouseX, mouseY, hovered);
            }
        }

        // Arrow with the station on top.
        int arrowX = gridX + 3 * SLOT + 3;
        int arrowY = gridY + SLOT + 4;
        graphics.fill(arrowX, arrowY + 4, arrowX + 10, arrowY + 6, 0xFF6B6B6B);
        for (int i = 0; i < 5; i++) graphics.fill(arrowX + 10 + i, arrowY + i, arrowX + 11 + i, arrowY + 10 - i, 0xFF6B6B6B);
        hovered = item(graphics, font, display.station(), null, arrowX - 2, arrowY - 18, mouseX, mouseY, hovered);

        // Results: one column, up to three rows (washer has several possible finds).
        int outX = arrowX + 18;
        List<Slot> outputs = display.outputs();
        int count = Math.min(outputs.size(), 6);
        int rows = (count + 1) / 2;
        for (int i = 0; i < count; i++) {
            int sx = count == 1 ? outX : outX + (i % 2) * SLOT;
            int sy = count == 1 ? gridY + SLOT : gridY + SLOT + SLOT / 2 - rows * SLOT / 2 + (i / 2) * SLOT;
            drawSlot(graphics, sx, sy);
            Slot slot = outputs.get(i);
            hovered = item(graphics, font, slot.shown(), slot.note(), sx, sy, mouseX, mouseY, hovered);
        }
        return hovered;
    }

    private static boolean isGridRecipe(Display display) {
        return display.station().is(Items.CRAFTING_TABLE);
    }

    private static void drawSlot(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + SLOT, y + SLOT, 0xFF8B8B8B);
        graphics.fill(x, y, x + SLOT - 1, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + SLOT - 1, 0xFF373737);
        graphics.fill(x + 1, y + SLOT - 1, x + SLOT, y + SLOT, 0xFFFFFFFF);
        graphics.fill(x + SLOT - 1, y + 1, x + SLOT, y + SLOT, 0xFFFFFFFF);
    }

    private static ItemStack item(GuiGraphicsExtractor graphics, Font font, ItemStack stack, @Nullable String note,
                                  int x, int y, int mouseX, int mouseY, ItemStack hovered) {
        if (stack.isEmpty()) return hovered;
        graphics.item(stack, x + 1, y + 1);
        graphics.itemDecorations(font, stack, x + 1, y + 1);
        if (note != null) {
            // Small (half size) label in the slot's bottom-right corner, e.g. a drop chance.
            graphics.pose().pushMatrix();
            graphics.pose().translate(x + SLOT - 1 - font.width(note) / 2.0F, y + SLOT - 5);
            graphics.pose().scale(0.5F, 0.5F);
            graphics.text(font, note, 0, 0, 0xFFFFFFFF, true);
            graphics.pose().popMatrix();
        }
        boolean over = mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT;
        return over ? stack : hovered;
    }
}
