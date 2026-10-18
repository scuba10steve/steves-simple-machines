package io.github.scuba10steve.ssm.registry;

import com.mojang.serialization.MapCodec;
import io.github.scuba10steve.ssm.ref.RefStrings;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModConditions {
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, RefStrings.MODID);

    public static final Supplier<MapCodec<FeatureEnabledCondition>> FEATURE_ENABLED =
            CONDITIONS.register("feature_enabled", () -> FeatureEnabledCondition.CODEC);

    public static void register(IEventBus bus) {
        CONDITIONS.register(bus);
    }
}
