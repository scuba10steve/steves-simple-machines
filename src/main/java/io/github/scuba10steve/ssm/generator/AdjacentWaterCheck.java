package io.github.scuba10steve.ssm.generator;

import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import java.util.List;

public final class AdjacentWaterCheck {
    private AdjacentWaterCheck() {}

    // Checks fluid identity (Fluids.WATER / Fluids.FLOWING_WATER) rather than FluidTags.WATER:
    // fluid tags are populated by a datapack/resource reload that a live Level performs, but
    // this is intentionally a pure function testable without one (see AdjacentWaterCheckTest).
    // Vanilla water is exactly these two fluids, so this is equivalent for the vanilla case.
    public static boolean hasAdjacentWater(List<FluidState> neighborFluidStates) {
        for (FluidState state : neighborFluidStates) {
            if (state.getType() == Fluids.WATER || state.getType() == Fluids.FLOWING_WATER) {
                return true;
            }
        }
        return false;
    }
}
