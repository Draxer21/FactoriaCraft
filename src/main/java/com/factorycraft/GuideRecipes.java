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
    static final int WIDTH = 136;
    static final int HEIGHT = 74;
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
            Map.entry("belts", new String[] {"conveyor", "sorter", "elevator"}),
            Map.entry("processing", new String[] {"crusher", "industrial_furnace", "iron_ingot_from_iron_dust_smelting",
                    "gold_ingot_from_gold_dust_smelting", "copper_ingot_from_copper_dust_smelting",
                    "iron_ingot_from_iron_dust_blasting", "gold_ingot_from_gold_dust_blasting",
                    "copper_ingot_from_copper_dust_blasting", "machine:crusher"}),
            Map.entry("well_drill", new String[] {"well_drill"}),
            Map.entry("power", new String[] {"generator", "cable"}),
            Map.entry("powered", new String[] {"ore_washer", "press", "mixer", "packager", "machine:press", "machine:washer"}),
            Map.entry("automation", new String[] {"robot_arm", "pump", "fluid_pipe"}),
            Map.entry("station", new String[] {"control_station"}),
            Map.entry("mechanic", new String[] {"mechanic_bench", "trade:mechanic/1/emerald_iron_plate", "trade:mechanic/1/emerald_cable",
                    "trade:mechanic/2/emerald_iron_gear", "trade:mechanic/2/emerald_fluid_pipe", "trade:mechanic/2/emerald_wrench",
                    "trade:mechanic/3/emerald_turbo", "trade:mechanic/3/emerald_efficient_engine",
                    "trade:mechanic/3/emerald_solar_panel", "trade:mechanic/3/emerald_magnet",
                    "trade:mechanic/4/prototype_drill", "trade:mechanic/4/industrial_chainsaw", "trade:mechanic/4/titan_hammer",
                    "trade:mechanic/5/turbo_ii", "trade:mechanic/5/fortune_ii", "trade:mechanic/5/fusion_engine"}),
            Map.entry("tools", new String[] {"stone_drill", "iron_drill", "diamond_drill", "netherite_drill_smithing",
                    "stone_chainsaw", "iron_chainsaw", "diamond_chainsaw", "netherite_chainsaw_smithing",
                    "stone_hammer", "iron_hammer", "diamond_hammer", "netherite_hammer_smithing", "wrench",
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
            case "minecraft:smelting", "minecraft:blasting" -> {
                in.add(Slot.of(ItemStack.EMPTY));
                in.add(Slot.of(ItemStack.EMPTY));
                in.add(Slot.of(ItemStack.EMPTY));
                in.add(Slot.of(ItemStack.EMPTY));
                in.add(ingredient(json.get("ingredient")));
                return new Display(in, out, new ItemStack(type.equals("minecraft:smelting") ? Items.FURNACE : Items.BLAST_FURNACE));
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
            default -> { }
        }
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
        ItemStack main = display.outputs().isEmpty() ? ItemStack.EMPTY : display.outputs().get(0).shown();
        String title = font.plainSubstrByWidth(main.getHoverName().getString(), WIDTH - 8);
        graphics.text(font, title, x + 4, y + 4, 0xFF3A4047, false);

        int gridX = x + 4;
        int gridY = y + 15;
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
        int arrowX = gridX + 3 * SLOT + 6;
        int arrowY = gridY + SLOT + 4;
        graphics.fill(arrowX, arrowY + 4, arrowX + 16, arrowY + 6, 0xFF6B6B6B);
        for (int i = 0; i < 5; i++) graphics.fill(arrowX + 16 + i, arrowY + i, arrowX + 17 + i, arrowY + 10 - i, 0xFF6B6B6B);
        hovered = item(graphics, font, display.station(), null, arrowX + 2, arrowY - 18, mouseX, mouseY, hovered);

        // Results: one column, up to three rows (washer has several possible finds).
        int outX = arrowX + 30;
        List<Slot> outputs = display.outputs();
        int count = Math.min(outputs.size(), 6);
        for (int i = 0; i < count; i++) {
            int sx = outX + (i / 3) * (SLOT + 2);
            int sy = (count == 1 ? gridY + SLOT : gridY + (i % 3) * SLOT);
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
        if (note != null) {
            graphics.itemDecorations(font, stack, x + 1, y + 1, note);
        } else {
            graphics.itemDecorations(font, stack, x + 1, y + 1);
        }
        boolean over = mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT;
        return over ? stack : hovered;
    }
}
