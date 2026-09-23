package io.github.scuba10steve.ssm.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.scuba10steve.ssm.machine.SsmMachineRecipe;
import io.github.scuba10steve.ssm.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import java.util.List;

public record SawingRecipe(
        Ingredient input,
        ItemStack planksResult, int planksCount,
        ItemStack sawdustResult, int sawdustCount
) implements SsmMachineRecipe {

    @Override
    public boolean matches(SingleRecipeInput recipeInput, net.minecraft.world.level.Level level) {
        return input.test(recipeInput.item());
    }

    @Override
    public List<ItemStack> getResults(HolderLookup.Provider registries) {
        ItemStack planks = planksResult.copy();
        planks.setCount(planksCount);
        ItemStack sawdust = sawdustResult.copy();
        sawdust.setCount(sawdustCount);
        return List.of(planks, sawdust);
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return planksResult.copy();
    }

    @Override
    public RecipeSerializer<? extends SawingRecipe> getSerializer() {
        return ModRecipes.SAWING_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends SawingRecipe> getType() {
        return ModRecipes.SAWING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<SawingRecipe> {
        public static final MapCodec<SawingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("input").forGetter(SawingRecipe::input),
                ItemStack.STRICT_CODEC.fieldOf("planks_result").forGetter(SawingRecipe::planksResult),
                Codec.INT.fieldOf("planks_count").forGetter(SawingRecipe::planksCount),
                ItemStack.STRICT_CODEC.fieldOf("sawdust_result").forGetter(SawingRecipe::sawdustResult),
                Codec.INT.fieldOf("sawdust_count").forGetter(SawingRecipe::sawdustCount)
        ).apply(instance, SawingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, SawingRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, SawingRecipe::input,
                ItemStack.STREAM_CODEC, SawingRecipe::planksResult,
                ByteBufCodecs.VAR_INT, SawingRecipe::planksCount,
                ItemStack.STREAM_CODEC, SawingRecipe::sawdustResult,
                ByteBufCodecs.VAR_INT, SawingRecipe::sawdustCount,
                SawingRecipe::new
        );

        @Override
        public MapCodec<SawingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SawingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
