package io.github.scuba10steve.ssm.feature;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlockItemEntityFeatureStateTest {

    // Same rationale as SmeltingLookupTest/ProcessingMachineStateTest/AdjacentWaterCheckTest:
    // core's neoFormVersion-only classpath has no modded harness to bootstrap vanilla's
    // registries, and Items' static init touches BuiltInRegistries, which asserts
    // Bootstrap.bootStrap() already ran. bootStrap() is idempotent.
    @BeforeAll
    static void bootstrapRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    // Each test uses its own unique namespace/path pair — BY_ITEM_ID is a shared static map for
    // the whole JVM test run, and the constructor intentionally throws on a duplicate id (see
    // duplicateRegistrationThrows below), so reusing an id across test methods would make test
    // order-dependent.

    @Test
    void enabledFeatureReportsEnabled() {
        BlockItemEntityFeatureState state = BlockItemEntityFeatureState.of(
                "test", "enabled_item", true, () -> Items.STICK, null, null);

        assertTrue(state.isEnabled());
        assertTrue(BlockItemEntityFeatureState.isEnabled(
                ResourceLocation.fromNamespaceAndPath("test", "enabled_item")));
    }

    @Test
    void disabledFeatureReportsDisabled() {
        BlockItemEntityFeatureState state = BlockItemEntityFeatureState.of(
                "test", "disabled_item", false, () -> Items.STICK, null, null);

        assertFalse(state.isEnabled());
        assertFalse(BlockItemEntityFeatureState.isEnabled(
                ResourceLocation.fromNamespaceAndPath("test", "disabled_item")));
    }

    @Test
    void unregisteredIdDefaultsToEnabled() {
        assertTrue(BlockItemEntityFeatureState.isEnabled(
                ResourceLocation.fromNamespaceAndPath("test", "never_registered")));
    }

    @Test
    void duplicateRegistrationThrows() {
        BlockItemEntityFeatureState.of("test", "dup_item", true, () -> Items.STICK, null, null);

        assertThrows(IllegalStateException.class, () ->
                BlockItemEntityFeatureState.of("test", "dup_item", true, () -> Items.STICK, null, null));
    }

    @Test
    void blockAndBlockEntityTypeAreNullWhenNotProvided() {
        BlockItemEntityFeatureState state = BlockItemEntityFeatureState.of(
                "test", "item_only", true, () -> Items.STICK, null, null);

        assertNull(state.block());
        assertNull(state.blockEntityType());
    }

    @Test
    void allIncludesRegisteredStates() {
        BlockItemEntityFeatureState state = BlockItemEntityFeatureState.of(
                "test", "enumerable_item", true, () -> Items.STICK, null, null);

        assertTrue(BlockItemEntityFeatureState.all().contains(state));
    }
}
