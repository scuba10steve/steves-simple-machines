package io.github.scuba10steve.ssm.registry;

import io.github.scuba10steve.ssm.feature.BlockItemEntityFeatureState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeatureEnabledConditionTest {

    @Test
    void falseWhenFeatureDisabled() {
        BlockItemEntityFeatureState.of("test", "cond_disabled", false, () -> Items.STICK, null, null);
        FeatureEnabledCondition condition = new FeatureEnabledCondition(
                ResourceLocation.fromNamespaceAndPath("test", "cond_disabled"));

        assertFalse(condition.test(ICondition.IContext.EMPTY));
    }

    @Test
    void trueWhenFeatureEnabled() {
        BlockItemEntityFeatureState.of("test", "cond_enabled", true, () -> Items.STICK, null, null);
        FeatureEnabledCondition condition = new FeatureEnabledCondition(
                ResourceLocation.fromNamespaceAndPath("test", "cond_enabled"));

        assertTrue(condition.test(ICondition.IContext.EMPTY));
    }

    @Test
    void trueWhenFeatureNeverRegistered() {
        FeatureEnabledCondition condition = new FeatureEnabledCondition(
                ResourceLocation.fromNamespaceAndPath("test", "cond_never_registered"));

        assertTrue(condition.test(ICondition.IContext.EMPTY));
    }
}
