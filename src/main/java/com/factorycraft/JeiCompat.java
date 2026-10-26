package com.factorycraft;

import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * JEI integration (only loaded when JEI is installed): shows the recipes of
 * the crusher, ore washer, press and centrifuge, with the machines as
 * crafting stations. Crafting/furnace/smithing recipes already show in JEI.
 */
@JeiPlugin
public final class JeiCompat implements IModPlugin {
    /** One machine conversion: inputs (each a list of options), outputs with optional notes (chances). */
    public record MachineRecipe(List<List<ItemStack>> inputs, List<ItemStack> outputs, List<String> notes) {}

    private record Machine(String id, String block, IRecipeType<MachineRecipe> type) {}

    private static final List<Machine> MACHINES = List.of(
            machine("crusher", "factorycraft:crusher"),
            machine("washer", "factorycraft:ore_washer"),
            machine("press", "factorycraft:press"),
            machine("centrifuge", "factorycraft:centrifuge"),
            machine("tannery", "factorycraft:tannery"));

    private static Machine machine(String id, String block) {
        return new Machine(id, block, IRecipeType.create(FactoryCraft.MOD_ID, id, MachineRecipe.class));
    }

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(FactoryCraft.MOD_ID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        for (Machine machine : MACHINES) registration.addRecipeCategories(new Category(machine, gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (Machine machine : MACHINES) {
            List<MachineRecipe> recipes = new ArrayList<>();
            for (GuideRecipes.Display d : GuideRecipes.machineDisplays(machine.id())) {
                List<List<ItemStack>> in = new ArrayList<>();
                for (GuideRecipes.Slot slot : d.inputs()) if (!slot.options().isEmpty()) in.add(slot.options());
                List<ItemStack> out = new ArrayList<>();
                List<String> notes = new ArrayList<>();
                for (GuideRecipes.Slot slot : d.outputs()) {
                    if (slot.options().isEmpty()) continue;
                    out.add(slot.options().get(0));
                    notes.add(slot.note());
                }
                recipes.add(new MachineRecipe(in, out, notes));
            }
            registration.addRecipes(machine.type(), recipes);
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (Machine machine : MACHINES) {
            ItemStack station = GuideRecipes.itemStack(machine.block());
            if (!station.isEmpty()) registration.addCraftingStation(machine.type(), station);
        }
    }

    private static final class Category implements IRecipeCategory<MachineRecipe> {
        private final Machine machine;
        private final IGuiHelper gui;
        private final IDrawable icon;

        Category(Machine machine, IGuiHelper gui) {
            this.machine = machine;
            this.gui = gui;
            this.icon = gui.createDrawableItemStack(GuideRecipes.itemStack(machine.block()));
        }

        @Override public IRecipeType<MachineRecipe> getRecipeType() { return machine.type(); }
        @Override public Component getTitle() { return GuideRecipes.itemStack(machine.block()).getHoverName(); }
        @Override public int getWidth() { return 130; }
        @Override public int getHeight() { return 40; }
        @Override public IDrawable getIcon() { return icon; }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, MachineRecipe recipe, IFocusGroup focuses) {
            for (int i = 0; i < recipe.inputs().size(); i++) {
                builder.addSlot(RecipeIngredientRole.INPUT, 1 + i * 18, 12).addItemStacks(recipe.inputs().get(i)).setStandardSlotBackground();
            }
            for (int i = 0; i < recipe.outputs().size(); i++) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, 60 + (i % 4) * 18, i < 4 ? 3 : 21)
                        .addItemStack(recipe.outputs().get(i)).setOutputSlotBackground();
            }
        }

        @Override
        public void draw(MachineRecipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
            gui.getRecipeArrow().draw(graphics, 32, 12);
            var font = Minecraft.getInstance().font;
            for (int i = 0; i < recipe.notes().size(); i++) {
                String note = recipe.notes().get(i);
                if (note == null) continue;
                graphics.text(font, note, 60 + (i % 4) * 18, i < 4 ? 30 : 38, 0xFF404040, false);
            }
        }
    }
}
