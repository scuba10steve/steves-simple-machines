package io.github.scuba10steve.ssm.machine;

import io.github.scuba10steve.ssm.config.SsmConfig;
import io.github.scuba10steve.ssm.recipe.CrushingRecipe;
import io.github.scuba10steve.ssm.recipe.SawingRecipe;
import io.github.scuba10steve.ssm.registry.ModRecipes;
import io.github.scuba10steve.ssm.registry.ModMenuTypes;
import net.minecraft.world.item.crafting.RecipeType;
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
            2,
            SsmConfig.CRUSHER_CAPACITY::get,
            SsmConfig.CRUSHER_ENERGY_PER_TICK::get,
            SsmConfig.CRUSHER_PROCESS_TICKS::get,
            ModMenuTypes.CRUSHER
    );

    public static final MachineConfig SMELTER = new MachineConfig(
            "smelter",
            (level, input) -> {
                SingleRecipeInput recipeInput = new SingleRecipeInput(input);
                return level.getRecipeManager()
                        .getRecipeFor(RecipeType.SMELTING, recipeInput, level)
                        .map(holder -> holder.value().assemble(recipeInput, level.registryAccess()))
                        .filter(result -> !result.isEmpty())
                        .map(result -> SmelterAdapter.toMachineResult(result, SsmConfig.SMELTER_PROCESS_TICKS.get()));
            },
            1,
            SsmConfig.SMELTER_CAPACITY::get,
            SsmConfig.SMELTER_ENERGY_PER_TICK::get,
            SsmConfig.SMELTER_PROCESS_TICKS::get,
            ModMenuTypes.SMELTER
    );

    public static final MachineConfig SAWMILL = new MachineConfig(
            "sawmill",
            (level, input) -> level.getRecipeManager()
                    .getRecipeFor(ModRecipes.SAWING_TYPE.get(), new SingleRecipeInput(input), level)
                    .map(holder -> new MachineRecipeResult(
                            holder.value().getResults(level.registryAccess()),
                            holder.value().getProcessTicksOverride() >= 0
                                    ? holder.value().getProcessTicksOverride()
                                    : SsmConfig.SAWMILL_PROCESS_TICKS.get())),
            2,
            SsmConfig.SAWMILL_CAPACITY::get,
            SsmConfig.SAWMILL_ENERGY_PER_TICK::get,
            SsmConfig.SAWMILL_PROCESS_TICKS::get,
            ModMenuTypes.SAWMILL
    );
}
