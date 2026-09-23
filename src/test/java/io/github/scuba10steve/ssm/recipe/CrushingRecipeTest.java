package io.github.scuba10steve.ssm.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CrushingRecipeTest {

    @Test
    void matchesConfiguredIngredientOnly() {
        Ingredient rawIron = Ingredient.of(Items.RAW_IRON);
        ItemStack output = new ItemStack(Items.IRON_NUGGET); // placeholder result item, real item added in Step 4
        CrushingRecipe recipe = new CrushingRecipe(rawIron, output, 2);

        assertTrue(recipe.matches(new SingleRecipeInput(new ItemStack(Items.RAW_IRON)), null));
        assertFalse(recipe.matches(new SingleRecipeInput(new ItemStack(Items.RAW_GOLD)), null));
    }

    @Test
    void producesDoubledOutputCount() {
        Ingredient rawIron = Ingredient.of(Items.RAW_IRON);
        ItemStack output = new ItemStack(Items.IRON_NUGGET);
        CrushingRecipe recipe = new CrushingRecipe(rawIron, output, 2);

        var results = recipe.getResults(null);
        assertEquals(1, results.size());
        assertEquals(2, results.get(0).getCount());
    }
}
