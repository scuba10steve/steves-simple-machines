package io.github.scuba10steve.ssm.generator;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;

import java.util.List;

public final class AdjacentWaterCheck {
    private AdjacentWaterCheck() {}

    public static boolean hasAdjacentWater(List<FluidState> neighborFluidStates) {
        for (FluidState state : neighborFluidStates) {
            if (state.is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }
}
