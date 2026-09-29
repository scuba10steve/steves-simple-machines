package io.github.scuba10steve.ssm.machine;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import java.util.List;

/** A machine recipe with a full output list, beyond vanilla Recipe#assemble's single ItemStack. */
public interface SsmMachineRecipe extends Recipe<SingleRecipeInput> {
    List<ItemStack> getResults(HolderLookup.Provider registries);

    /** -1 means "use the machine's configured baseProcessTicks". */
    default int getProcessTicksOverride() {
        return -1;
    }

    @Override
    default ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        List<ItemStack> results = getResults(registries);
        return results.isEmpty() ? ItemStack.EMPTY : results.get(0).copy();
    }

    @Override
    default boolean canCraftInDimensions(int width, int height) {
        return true;
    }
}
