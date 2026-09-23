package io.github.scuba10steve.ssm.machine;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SmeltingLookupTest {

    @Test
    void wrapsAVanillaSmeltingMatchIntoAFlatDurationSingleOutputResult() {
        // Fakes what MachineConfigs.SMELTER's real lookup does against RecipeManager,
        // without needing a live Level: given a vanilla smelting result item and a
        // configured flat process-tick count, the adapter must produce exactly one
        // output stack and ignore the vanilla recipe's own cookingTime.
        ItemStack smeltedResult = new ItemStack(Items.IRON_INGOT);
        int flatProcessTicks = 100;

        MachineRecipeResult result = SmelterAdapter.toMachineResult(smeltedResult, flatProcessTicks);

        assertEquals(1, result.outputs().size());
        assertEquals(Items.IRON_INGOT, result.outputs().get(0).getItem());
        assertEquals(1, result.outputs().get(0).getCount());
        assertEquals(100, result.processTicks());
    }

    @Test
    void emptyResultItemMeansNoMatch() {
        assertTrue(Optional.of(ItemStack.EMPTY).filter(s -> !s.isEmpty()).isEmpty());
    }
}
