package io.github.scuba10steve.ssm.machine;

import io.github.scuba10steve.ssm.config.SsmConfig;
import io.github.scuba10steve.ssm.recipe.CrushingRecipe;
import io.github.scuba10steve.ssm.registry.ModRecipes;
import io.github.scuba10steve.ssm.registry.ModMenuTypes;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import java.util.Optional;

public class MachineConfigs {
    public static final MachineConfig CRUSHER = new MachineConfig(
            "crusher",
            (level, input) -> level.getRecipeManager()
                    .getRecipeFor(ModRecipes.CRUSHING_TYPE.get(), new SingleRecipeInput(input), level)
                    .map(holder -> new MachineRecipeResult(
                            holder.value().getResults(level.registryAccess()),
                            holder.value().getProcessTicksOverride() >= 0
                                    ? holder.value().getProcessTicksOverride()
                                    : SsmConfig.CRUSHER_PROCESS_TICKS.get())),
            1,
            SsmConfig.CRUSHER_CAPACITY::get,
            SsmConfig.CRUSHER_ENERGY_PER_TICK::get,
            SsmConfig.CRUSHER_PROCESS_TICKS::get,
            ModMenuTypes.CRUSHER
    );
}
