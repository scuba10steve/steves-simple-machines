package io.github.scuba10steve.ssm.generator;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
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
    // reload a live Level performs on world load, which this plain unit test never runs, so
    // FluidTags.WATER starts out empty here. Bootstrap.bootStrap() runs vanilla's own static
    // registration (satisfying BuiltInRegistries' "Not bootstrapped" guard on first touch, since
    // this module's neoFormVersion-only classpath has no modded harness to do it for us), then
    // we bind the exact tag membership a real reload would supply via the registry's own public
    // bindTags API. This keeps the test exercising the real production logic (not a fake/mock)
    // against real FluidState objects.
    @BeforeAll
    static void bindWaterTagForTest() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
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
