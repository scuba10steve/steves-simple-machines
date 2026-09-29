package io.github.scuba10steve.ssm.registry;

import io.github.scuba10steve.ssm.recipe.CrushingRecipe;
import io.github.scuba10steve.ssm.recipe.SawingRecipe;
import io.github.scuba10steve.ssm.ref.RefStrings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, RefStrings.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, RefStrings.MODID);

    public static final Supplier<RecipeType<CrushingRecipe>> CRUSHING_TYPE =
            RECIPE_TYPES.register("crushing", () -> new RecipeType<CrushingRecipe>() {
                @Override
                public String toString() {
                    return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "crushing").toString();
                }
            });

    public static final Supplier<CrushingRecipe.Serializer> CRUSHING_SERIALIZER =
            RECIPE_SERIALIZERS.register("crushing", CrushingRecipe.Serializer::new);

    public static final Supplier<RecipeType<SawingRecipe>> SAWING_TYPE =
            RECIPE_TYPES.register("sawing", () -> new RecipeType<SawingRecipe>() {
                @Override
                public String toString() {
                    return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "sawing").toString();
                }
            });

    public static final Supplier<SawingRecipe.Serializer> SAWING_SERIALIZER =
            RECIPE_SERIALIZERS.register("sawing", SawingRecipe.Serializer::new);

    public static void register(IEventBus bus) {
        RECIPE_TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
    }
}
