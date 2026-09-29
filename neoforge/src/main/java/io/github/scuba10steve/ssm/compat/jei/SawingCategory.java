package io.github.scuba10steve.ssm.compat.jei;

import io.github.scuba10steve.ssm.recipe.SawingRecipe;
import io.github.scuba10steve.ssm.registry.ModBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class SawingCategory implements IRecipeCategory<SawingRecipe> {

    public static final RecipeType<SawingRecipe> TYPE =
            RecipeType.create("ssm", "sawing", SawingRecipe.class);

    private final IGuiHelper guiHelper;

    public SawingCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;
    }

    @Override
    public RecipeType<SawingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.ssm.sawing");
    }

    @Override
    public int getWidth() {
        return 120;
    }

    @Override
    public int getHeight() {
        return 40;
    }

    @Override
    public IDrawable getIcon() {
        return guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.SAWMILL.get()));
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SawingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 5, 12).addIngredients(recipe.input());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 90, 2).addItemStack(recipe.getResults(null).get(0));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 90, 22).addItemStack(recipe.getResults(null).get(1));
    }

    @Override
    public void draw(SawingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {}
}
