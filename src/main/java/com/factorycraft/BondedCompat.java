package com.factorycraft;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

/**
 * Optional support for the Bonded mod (gear that levels up as you use it).
 *
 * The item tags in data/bonded/tags/item/ already make our weapons, tools and
 * armour level up; they are plain data and do nothing when Bonded is absent.
 * This class adds the rest when Bonded is installed, through reflection so the
 * mod never needs Bonded to load:
 *  - upgrade chains: a maxed-out stone drill/hammer/chainsaw/excavating shovel
 *    can be upgraded to copper, then iron, diamond and netherite, keeping its
 *    Bonded level progress, like Bonded does for vanilla tools;
 *  - experience for the gamma rifle, whose shots are not projectiles.
 */
public final class BondedCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String API = "com.iamkaf.bonded.api.BondedApi";

    /** Upgrade chains registered with Bonded (0 when Bonded is absent). */
    static int upgrades;
    /** Whether the gamma rifle can award Bonded experience. */
    static boolean experienceHook;
    private static Object experienceEvent;
    private static Method invoker;
    private static Method experience;

    private BondedCompat() {
    }

    public static boolean loaded() {
        return ModList.get() != null && ModList.get().isLoaded("bonded");
    }

    /** Called once at common setup, after every item is registered. */
    public static void init() {
        if (!loaded()) return;
        try {
            Method addUpgrade = Class.forName(API).getMethod("addUpgrade", Item.class, Item.class, TagKey.class);
            String[][] materials = {
                    {"c", "ingots/copper"}, {"c", "ingots/iron"}, {"c", "gems/diamond"}, {"c", "ingots/netherite"}};
            for (List<Supplier<Item>> chain : chains()) {
                for (int i = 0; i + 1 < chain.size(); i++) {
                    TagKey<Item> material = TagKey.create(Registries.ITEM,
                            Identifier.fromNamespaceAndPath(materials[i][0], materials[i][1]));
                    addUpgrade.invoke(null, chain.get(i).get(), chain.get(i + 1).get(), material);
                    upgrades++;
                }
            }
            LOGGER.info("FactoryCraft: registered {} Bonded upgrade steps", upgrades);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("FactoryCraft: could not register Bonded upgrade chains", e);
        }
        try {
            Class<?> events = Class.forName("com.iamkaf.bonded.api.event.GameEvents");
            Field field = events.getField("AWARD_ITEM_EXPERIENCE");
            experienceEvent = field.get(null);
            invoker = experienceEvent.getClass().getMethod("invoker");
            invoker.setAccessible(true);
            Class<?> listener = Class.forName("com.iamkaf.bonded.api.event.GameEvents$ItemExperience");
            experience = listener.getMethod("experience", Player.class, ItemStack.class, int.class);
            experienceHook = true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("FactoryCraft: Bonded experience hook unavailable", e);
        }
    }

    /** Gives Bonded experience to a gear item (no-op without Bonded). */
    public static void award(Player player, ItemStack stack, int amount) {
        if (!experienceHook || player.level().isClientSide()) return;
        try {
            experience.invoke(invoker.invoke(experienceEvent), player, stack, amount);
        } catch (ReflectiveOperationException | RuntimeException e) {
            experienceHook = false;
            LOGGER.warn("FactoryCraft: Bonded experience hook failed, disabling it", e);
        }
    }

    private static List<List<Supplier<Item>>> chains() {
        return List.of(
                List.of(FactoryCraft.STONE_DRILL::value, FactoryCraft.COPPER_DRILL::value, FactoryCraft.IRON_DRILL::value,
                        FactoryCraft.DIAMOND_DRILL::value, FactoryCraft.NETHERITE_DRILL::value),
                List.of(FactoryCraft.STONE_HAMMER::value, FactoryCraft.COPPER_HAMMER::value, FactoryCraft.IRON_HAMMER::value,
                        FactoryCraft.DIAMOND_HAMMER::value, FactoryCraft.NETHERITE_HAMMER::value),
                List.of(FactoryCraft.STONE_CHAINSAW::value, FactoryCraft.COPPER_CHAINSAW::value, FactoryCraft.IRON_CHAINSAW::value,
                        FactoryCraft.DIAMOND_CHAINSAW::value, FactoryCraft.NETHERITE_CHAINSAW::value),
                List.of(FactoryCraft.STONE_EXCAVATING_SHOVEL::value, FactoryCraft.COPPER_EXCAVATING_SHOVEL::value,
                        FactoryCraft.IRON_EXCAVATING_SHOVEL::value, FactoryCraft.DIAMOND_EXCAVATING_SHOVEL::value,
                        FactoryCraft.NETHERITE_EXCAVATING_SHOVEL::value));
    }
}
