package com.factorycraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

/**
 * Datapack recipes for the FactoryCraft machines, so modpacks can add, replace
 * or remove them without code. Files go in
 * {@code data/<namespace>/factorycraft_machine_recipes/<anything>.json}:
 *
 * <pre>
 * {"type": "press",        "input": "minecraft:iron_ingot", "input_count": 1, "result": "factorycraft:iron_plate", "count": 1}
 * {"type": "crusher",      "input": "#c:ores/tin", "result": "factorycraft:tin_dust", "count": 4}
 * {"type": "centrifuge",   "input": "factorycraft:uranium_dust", "input_count": 4, "result": "factorycraft:enriched_uranium_1"}
 * {"type": "accelerator",  "input": "minecraft:coal_block", "result": "minecraft:diamond", "count": 1}
 * {"type": "alloy_smelter","input_a": "#c:ingots/copper", "count_a": 3, "input_b": "#c:ingots/tin", "count_b": 1,
 *                          "result": "factorycraft:bronze_ingot", "count": 4}
 * {"type": "ore_washer",   "input": "minecraft:gravel", "results": [{"item": "minecraft:flint", "chance": 0.3}]}
 * {"type": "remove", "machine": "press", "input": "minecraft:coal"}
 * </pre>
 *
 * Inputs starting with '#' are item tags (crusher and alloy smelter). A
 * datapack recipe for an input the mod already handles takes priority; a
 * "remove" entry drops the built-in recipes for that input. Reloaded with
 * /reload. The built-in recipes stay in the code as the defaults.
 */
@EventBusSubscriber(modid = FactoryCraft.MOD_ID)
public final class MachineRecipes {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final String FOLDER = "factorycraft_machine_recipes";
    /** Recipes loaded on the last (re)load, for the self-test. */
    static int loaded;

    private MachineRecipes() {}

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        load(event.getServer().getResourceManager());
    }

    @SubscribeEvent
    public static void onReload(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) load(event.getPlayerList().getServer().getResourceManager());
    }

    static void load(ResourceManager manager) {
        List<JsonObject> entries = new ArrayList<>();
        Map<Identifier, Resource> files = new java.util.TreeMap<>(manager.listResources(FOLDER, id -> id.getPath().endsWith(".json")));
        for (Map.Entry<Identifier, Resource> file : files.entrySet()) {
            try (Reader reader = file.getValue().openAsReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                if (json.isJsonArray()) {
                    for (JsonElement e : json.getAsJsonArray()) entries.add(e.getAsJsonObject());
                } else {
                    entries.add(json.getAsJsonObject());
                }
            } catch (Exception e) {
                LOGGER.warn("FactoryCraft: bad machine recipe file {}: {}", file.getKey(), e.toString());
            }
        }
        apply(entries);
    }

    /** Rebuilds every machine's recipe list: datapack entries first, then the built-ins not removed. */
    static synchronized void apply(List<JsonObject> entries) {
        Map<String, Set<String>> removed = new HashMap<>();
        List<PressBlockEntity.Pressing> press = new ArrayList<>();
        List<CrusherRecipes.Entry> crusher = new ArrayList<>();
        List<CentrifugeBlockEntity.Stage> centrifuge = new ArrayList<>();
        List<AcceleratorBlockEntity.Transmutation> accelerator = new ArrayList<>();
        List<AlloySmelterBlockEntity.Alloying> alloy = new ArrayList<>();
        Map<Item, List<OreWasherBlockEntity.Find>> washer = new LinkedHashMap<>();
        int count = 0;
        for (JsonObject json : entries) {
            try {
                String type = json.get("type").getAsString().replace("factorycraft:", "");
                switch (type) {
                    case "remove" -> removed.computeIfAbsent(json.get("machine").getAsString().replace("factorycraft:", ""),
                            k -> new HashSet<>()).add(json.get("input").getAsString());
                    case "press" -> press.add(new PressBlockEntity.Pressing(item(json, "input"), intOr(json, "input_count", 1),
                            item(json, "result"), intOr(json, "count", 1)));
                    case "crusher" -> {
                        String input = json.get("input").getAsString();
                        Object source = input.startsWith("#") ? TagKey.create(Registries.ITEM, Identifier.parse(input.substring(1))) : shown(input);
                        crusher.add(new CrusherRecipes.Entry(test(input), item(json, "result"), intOr(json, "count", 1), source));
                    }
                    case "centrifuge" -> centrifuge.add(new CentrifugeBlockEntity.Stage(item(json, "input"), intOr(json, "input_count", 1),
                            item(json, "result")));
                    case "accelerator" -> accelerator.add(new AcceleratorBlockEntity.Transmutation(item(json, "input"), item(json, "result"),
                            intOr(json, "count", 1)));
                    case "alloy_smelter" -> {
                        String a = json.get("input_a").getAsString();
                        String b = json.get("input_b").getAsString();
                        alloy.add(new AlloySmelterBlockEntity.Alloying(test(a), intOr(json, "count_a", 1), test(b), intOr(json, "count_b", 1),
                                item(json, "result"), intOr(json, "count", 1), shown(a), shown(b)));
                    }
                    case "ore_washer" -> {
                        List<OreWasherBlockEntity.Find> finds = new ArrayList<>();
                        JsonArray results = json.getAsJsonArray("results");
                        for (JsonElement e : results) {
                            JsonObject r = e.getAsJsonObject();
                            finds.add(new OreWasherBlockEntity.Find(item(r, "item"), r.get("chance").getAsFloat()));
                        }
                        washer.put(item(json, "input").get(), List.copyOf(finds));
                    }
                    default -> throw new IllegalArgumentException("unknown type " + type);
                }
                count++;
            } catch (RuntimeException e) {
                LOGGER.warn("FactoryCraft: skipped machine recipe {}: {}", json, e.toString());
            }
        }

        for (PressBlockEntity.Pressing r : PressBlockEntity.DEFAULTS) {
            if (!removed(removed, "press", r.input().get()) && press.stream().noneMatch(p -> p.input().get() == r.input().get())) press.add(r);
        }
        for (CentrifugeBlockEntity.Stage r : CentrifugeBlockEntity.DEFAULTS) {
            if (!removed(removed, "centrifuge", r.input().get()) && centrifuge.stream().noneMatch(p -> p.input().get() == r.input().get())) centrifuge.add(r);
        }
        for (AcceleratorBlockEntity.Transmutation r : AcceleratorBlockEntity.DEFAULTS) {
            if (!removed(removed, "accelerator", r.input().get()) && accelerator.stream().noneMatch(p -> p.input().get() == r.input().get())) accelerator.add(r);
        }
        for (CrusherRecipes.Entry r : CrusherRecipes.DEFAULTS) {
            if (!removedSource(removed, "crusher", r.source())) crusher.add(r);
        }
        for (AlloySmelterBlockEntity.Alloying r : AlloySmelterBlockEntity.DEFAULTS) {
            if (!removedSource(removed, "alloy_smelter", r.shownA()) && !removedSource(removed, "alloy_smelter", r.shownB())) alloy.add(r);
        }
        for (Map.Entry<Item, List<OreWasherBlockEntity.Find>> r : OreWasherBlockEntity.DEFAULTS.entrySet()) {
            if (!removed(removed, "ore_washer", r.getKey())) washer.putIfAbsent(r.getKey(), r.getValue());
        }

        PressBlockEntity.RECIPES = List.copyOf(press);
        CrusherRecipes.ENTRIES = List.copyOf(crusher);
        CentrifugeBlockEntity.STAGES = List.copyOf(centrifuge);
        AcceleratorBlockEntity.RECIPES = List.copyOf(accelerator);
        AlloySmelterBlockEntity.RECIPES = List.copyOf(alloy);
        OreWasherBlockEntity.TABLE = Map.copyOf(washer);
        loaded = count;
        if (count > 0 || !removed.isEmpty()) LOGGER.info("FactoryCraft: {} datapack machine recipes loaded", count);
    }

    private static boolean removed(Map<String, Set<String>> removed, String machine, Item input) {
        Set<String> set = removed.get(machine);
        return set != null && set.contains(BuiltInRegistries.ITEM.getKey(input).toString());
    }

    /** Built-in sources are an Item, a TagKey or a "c:..." tag string. */
    private static boolean removedSource(Map<String, Set<String>> removed, String machine, Object source) {
        Set<String> set = removed.get(machine);
        if (set == null) return false;
        if (source instanceof Item item) return set.contains(BuiltInRegistries.ITEM.getKey(item).toString());
        if (source instanceof TagKey<?> tag) return set.contains("#" + tag.location());
        if (source instanceof String tag) return set.contains("#" + tag) || set.contains(tag);
        return false;
    }

    private static int intOr(JsonObject json, String key, int fallback) {
        return json.has(key) ? json.get(key).getAsInt() : fallback;
    }

    private static Supplier<Item> item(JsonObject json, String key) {
        Identifier id = Identifier.parse(json.get(key).getAsString());
        Item item = BuiltInRegistries.ITEM.getValue(id);
        if (item == Items.AIR) throw new IllegalArgumentException("unknown item " + id);
        return () -> item;
    }

    private static Predicate<ItemStack> test(String input) {
        if (input.startsWith("#")) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.parse(input.substring(1)));
            return stack -> stack.is(tag);
        }
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(input));
        if (item == Items.AIR) throw new IllegalArgumentException("unknown item " + input);
        return stack -> stack.is(item);
    }

    /** What the guide and JEI show for an input: the Item, or the tag as "ns:path". */
    private static Object shown(String input) {
        if (input.startsWith("#")) return input.substring(1);
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(input));
    }
}
