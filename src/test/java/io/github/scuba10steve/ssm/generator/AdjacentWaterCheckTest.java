package io.github.scuba10steve.ssm.generator;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AdjacentWaterCheckTest {

    // AdjacentWaterCheck's production code checks FluidTags.WATER (the correct, mod-compatible
    // behavior: any fluid tagged #minecraft:water powers the Water Wheel, not just the two
    // vanilla Fluid instances). Tag membership is normally bound by the datapack/resource
    // reload a live Level performs on world load; this project's neoForge.unitTest harness
    // boots the game far enough to statically register built-ins (Fluids.WATER, etc.) but
    // never runs that reload, so FluidTags.WATER starts out empty here. Rather than weakening
    // production code to something the harness can exercise, or deferring this whole test to
    // the not-yet-built gametest suite (Tasks 15/16) the way Task 9's SmeltingLookupTest defers
    // its live-RecipeManager case, we bind the exact tag membership a real reload would supply
    // via the registry's own public bindTags API. This keeps the test exercising the real
    // production logic (not a fake/mock) against real FluidState objects.
    @BeforeAll
    static void bindWaterTagForTest() {
        BuiltInRegistries.FLUID.bindTags(Map.of(
                FluidTags.WATER, List.of(
                        Fluids.WATER.builtInRegistryHolder(),
                        Fluids.FLOWING_WATER.builtInRegistryHolder())
        ));
    }

    @Test
    void trueWhenAnyNeighborIsWaterSourceOrFlowing() {
        assertTrue(AdjacentWaterCheck.hasAdjacentWater(List.of(
                Fluids.EMPTY.defaultFluidState(),
                Fluids.WATER.defaultFluidState(),
                Fluids.EMPTY.defaultFluidState(),
                Fluids.EMPTY.defaultFluidState(),
                Fluids.EMPTY.defaultFluidState(),
                Fluids.EMPTY.defaultFluidState()
        )));
    }

    @Test
    void falseWhenNoNeighborIsWater() {
        assertFalse(AdjacentWaterCheck.hasAdjacentWater(List.of(
                Fluids.EMPTY.defaultFluidState(),
                Fluids.LAVA.defaultFluidState(),
                Fluids.EMPTY.defaultFluidState(),
                Fluids.EMPTY.defaultFluidState(),
                Fluids.EMPTY.defaultFluidState(),
                Fluids.EMPTY.defaultFluidState()
        )));
    }
}
