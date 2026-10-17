package io.github.scuba10steve.ssm.registry;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.scuba10steve.ssm.feature.BlockItemEntityFeatureState;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.conditions.ICondition;

public record FeatureEnabledCondition(ResourceLocation item) implements ICondition {
    public static final MapCodec<FeatureEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(
            builder -> builder
                    .group(ResourceLocation.CODEC.fieldOf("item").forGetter(FeatureEnabledCondition::item))
                    .apply(builder, FeatureEnabledCondition::new));

    @Override
    public boolean test(IContext context) {
        return BlockItemEntityFeatureState.isEnabled(item);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
