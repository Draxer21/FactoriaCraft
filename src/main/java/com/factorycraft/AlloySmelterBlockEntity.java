package com.factorycraft;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Alloy smelter: melts two ingredients together (either slot order). Steel,
 * bronze, stainless steel, plastic from dried kelp and coal, and tempered glass.
 * Ingredients accept the matching ingots and dusts of other mods (common tags).
 */
public final class AlloySmelterBlockEntity extends PoweredMachineBlockEntity {
    /** One recipe: {@code countA} of A plus {@code countB} of B. */
    public record Alloying(Predicate<ItemStack> a, int countA, Predicate<ItemStack> b, int countB,
                           Supplier<Item> output, int outCount, Object shownA, Object shownB) {}

    private static Predicate<ItemStack> tag(String path) {
        var key = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", path));
        return stack -> stack.is(key);
    }

    private static Predicate<ItemStack> item(Supplier<Item> item) {
        return stack -> stack.is(item.get());
    }

    static final List<Alloying> RECIPES = List.of(
            // Steel: iron with carbon.
            new Alloying(tag("ingots/iron"), 1, item(() -> Items.COAL), 2, () -> Materials.STEEL.ingot().value(), 1, "c:ingots/iron", Items.COAL),
            new Alloying(tag("ingots/iron"), 1, item(() -> Items.CHARCOAL), 2, () -> Materials.STEEL.ingot().value(), 1, "c:ingots/iron", Items.CHARCOAL),
            new Alloying(tag("dusts/iron"), 1, item(() -> Items.COAL), 1, () -> Materials.STEEL.ingot().value(), 1, "c:dusts/iron", Items.COAL),
            // Bronze: 3 copper + 1 tin = 4.
            new Alloying(tag("ingots/copper"), 3, tag("ingots/tin"), 1, () -> Materials.BRONZE.ingot().value(), 4, "c:ingots/copper", "c:ingots/tin"),
            new Alloying(tag("dusts/copper"), 3, tag("dusts/tin"), 1, () -> Materials.BRONZE.ingot().value(), 4, "c:dusts/copper", "c:dusts/tin"),
            // Stainless steel: 3 steel + 1 nickel = 4.
            new Alloying(tag("ingots/steel"), 3, tag("ingots/nickel"), 1, () -> Materials.STAINLESS_STEEL.ingot().value(), 4, "c:ingots/steel", "c:ingots/nickel"),
            // Plastic (bio-plastic until there's oil): dried kelp and coal.
            new Alloying(item(() -> Items.DRIED_KELP), 4, item(() -> Items.COAL), 1, () -> Materials.PLASTIC.value(), 2, Items.DRIED_KELP, Items.COAL),
            new Alloying(item(() -> Items.SLIME_BALL), 1, item(() -> Items.COAL), 1, () -> Materials.PLASTIC.value(), 2, Items.SLIME_BALL, Items.COAL),
            // Tempered glass: glass with a steel ingot.
            new Alloying(item(() -> Items.GLASS), 4, tag("ingots/steel"), 1, () -> Materials.TEMPERED_GLASS_ITEM.value(), 4, Items.GLASS, "c:ingots/steel"),
            // Nether.
            new Alloying(item(() -> Items.SOUL_SAND), 4, item(() -> Items.COAL), 1, () -> Exotic.SOUL_FUEL.value(), 2, Items.SOUL_SAND, Items.COAL),
            new Alloying(item(() -> Items.SOUL_SOIL), 4, item(() -> Items.COAL), 1, () -> Exotic.SOUL_FUEL.value(), 2, Items.SOUL_SOIL, Items.COAL),
            new Alloying(item(() -> Items.QUARTZ), 4, item(() -> Items.GLOWSTONE_DUST), 1, () -> Exotic.INDUSTRIAL_QUARTZ.value(), 2, Items.QUARTZ, Items.GLOWSTONE_DUST),
            // End.
            new Alloying(item(() -> Items.ENDER_PEARL), 1, item(() -> Items.POPPED_CHORUS_FRUIT), 2, () -> Exotic.STABLE_ENDER_CRYSTAL.value(), 1, Items.ENDER_PEARL, Items.POPPED_CHORUS_FRUIT),
            new Alloying(item(() -> Exotic.PURPURITE_DUST.value()), 4, tag("ingots/lithium"), 1, () -> Exotic.PURPURITE_INGOT.value(), 2, Exotic.PURPURITE_DUST.value(), "c:ingots/lithium"),
            new Alloying(item(() -> Items.DRAGON_BREATH), 4, item(() -> Exotic.STABLE_ENDER_CRYSTAL.value()), 1, () -> Exotic.DRAGON_ESSENCE.value(), 1, Items.DRAGON_BREATH, Exotic.STABLE_ENDER_CRYSTAL.value()),
            new Alloying(item(() -> Items.CHORUS_FRUIT), 4, item(() -> Materials.PLASTIC.value()), 1, () -> Exotic.CHORUS_INSULATION.value(), 2, Items.CHORUS_FRUIT, Materials.PLASTIC.value()));

    public AlloySmelterBlockEntity(BlockPos pos, BlockState state) {
        super(Materials.ALLOY_SMELTER_BLOCK_ENTITY.value(), pos, state, 2);
    }

    @Override protected int time() { return 100; }
    @Override protected int energyPerTick() { return 30; }
    @Override protected MenuType<ProcessingMenu> menuType() { return Materials.ALLOY_SMELTER_MENU.value(); }
    @Override protected String nameKey() { return "block.factorycraft.alloy_smelter"; }

    @Override
    protected boolean accepts(int slot, ItemStack stack) {
        if (slot >= 2) return false;
        for (Alloying r : RECIPES) if (r.a().test(stack) || r.b().test(stack)) return true;
        return false;
    }

    @Override
    protected @Nullable Operation find(Level level) {
        ItemStack first = items.get(0);
        ItemStack second = items.get(1);
        for (Alloying r : RECIPES) {
            if (matches(r.a(), r.countA(), first) && matches(r.b(), r.countB(), second)) {
                return new Operation(new int[] {r.countA(), r.countB()}, List.of(new ItemStack(r.output().get(), r.outCount())));
            }
            if (matches(r.a(), r.countA(), second) && matches(r.b(), r.countB(), first)) {
                return new Operation(new int[] {r.countB(), r.countA()}, List.of(new ItemStack(r.output().get(), r.outCount())));
            }
        }
        return null;
    }

    private static boolean matches(Predicate<ItemStack> test, int count, ItemStack stack) {
        return !stack.isEmpty() && test.test(stack) && stack.getCount() >= count;
    }

    @Override
    protected List<ItemStack> produce(Level level, Operation operation) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack stack : operation.maxOutputs()) out.add(stack.copy());
        return out;
    }
}
