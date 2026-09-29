package io.github.scuba10steve.ssm.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SawingRecipeTest {

    @Test
    void producesPlanksAndSawdust() {
        SawingRecipe recipe = new SawingRecipe(
                Ingredient.of(Items.OAK_LOG),
                new ItemStack(Items.OAK_PLANKS), 6,
                new ItemStack(Items.STICK), 1); // sawdust item substituted for a real vanilla stand-in until Step 3

        var results = recipe.getResults(null);
        assertEquals(2, results.size());
        assertEquals(6, results.get(0).getCount());
        assertEquals(1, results.get(1).getCount());
    }

    @Test
    void matchesOnlyConfiguredLog() {
        SawingRecipe recipe = new SawingRecipe(
                Ingredient.of(Items.OAK_LOG),
                new ItemStack(Items.OAK_PLANKS), 6,
                new ItemStack(Items.STICK), 1);

        assertTrue(recipe.matches(new SingleRecipeInput(new ItemStack(Items.OAK_LOG)), null));
        assertFalse(recipe.matches(new SingleRecipeInput(new ItemStack(Items.SPRUCE_LOG)), null));
    }
}
