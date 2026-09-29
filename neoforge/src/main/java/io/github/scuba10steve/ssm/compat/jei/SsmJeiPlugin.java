package io.github.scuba10steve.ssm.compat.jei;

import io.github.scuba10steve.ssm.recipe.CrushingRecipe;
import io.github.scuba10steve.ssm.recipe.SawingRecipe;
import io.github.scuba10steve.ssm.ref.RefStrings;
import io.github.scuba10steve.ssm.registry.ModBlocks;
import io.github.scuba10steve.ssm.registry.ModRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.List;

@JeiPlugin
public class SsmJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new CrushingCategory(registration.getJeiHelpers().getGuiHelper()),
                new SawingCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();

        List<CrushingRecipe> crushingRecipes = recipeManager
                .getAllRecipesFor(ModRecipes.CRUSHING_TYPE.get())
                .stream().map(holder -> holder.value()).toList();
        registration.addRecipes(CrushingCategory.TYPE, crushingRecipes);

        List<SawingRecipe> sawingRecipes = recipeManager
                .getAllRecipesFor(ModRecipes.SAWING_TYPE.get())
                .stream().map(holder -> holder.value()).toList();
        registration.addRecipes(SawingCategory.TYPE, sawingRecipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CRUSHER.get()), CrushingCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SAWMILL.get()), SawingCategory.TYPE);
        // Smelter reuses vanilla's own Smelting category rather than a new ssm category:
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SMELTER.get()), RecipeTypes.SMELTING);
    }
}
