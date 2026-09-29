package io.github.scuba10steve.ssm.recipe;

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

public record CrushingRecipe(Ingredient input, ItemStack result, int outputCount) implements SsmMachineRecipe {

    @Override
    public boolean matches(SingleRecipeInput recipeInput, net.minecraft.world.level.Level level) {
        return input.test(recipeInput.item());
    }

    @Override
    public List<ItemStack> getResults(HolderLookup.Provider registries) {
        ItemStack out = result.copy();
        out.setCount(outputCount);
        return List.of(out);
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public RecipeSerializer<? extends CrushingRecipe> getSerializer() {
        return ModRecipes.CRUSHING_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends CrushingRecipe> getType() {
        return ModRecipes.CRUSHING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<CrushingRecipe> {
        public static final MapCodec<CrushingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("input").forGetter(CrushingRecipe::input),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(CrushingRecipe::result),
                com.mojang.serialization.Codec.INT.fieldOf("output_count").forGetter(CrushingRecipe::outputCount)
        ).apply(instance, CrushingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, CrushingRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, CrushingRecipe::input,
                ItemStack.STREAM_CODEC, CrushingRecipe::result,
                ByteBufCodecs.VAR_INT, CrushingRecipe::outputCount,
                CrushingRecipe::new
        );

        @Override
        public MapCodec<CrushingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CrushingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
