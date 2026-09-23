package io.github.scuba10steve.ssm.generator;

import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdjacentWaterCheckTest {

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
