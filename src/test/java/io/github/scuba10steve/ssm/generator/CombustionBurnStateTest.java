package io.github.scuba10steve.ssm.generator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CombustionBurnStateTest {

    @Test
    void burnPausesWhenBufferIsFullAndResumesWithSpace() {
        CombustionBurnState state = new CombustionBurnState();
        state.startBurning(1600); // one coal's burn time

        // Buffer full: generate() would accept 0 this tick.
        CombustionBurnState.TickResult full = state.tick(/* generationRate */ 160, /* bufferRoom */ 0);
        assertEquals(1600, full.remainingBurnTime(), "no burn time consumed while buffer is full");
        assertEquals(0, full.energyToGenerate());

        // Buffer has room again.
        CombustionBurnState.TickResult resumed = state.tick(160, 500);
        assertEquals(1599, resumed.remainingBurnTime());
        assertEquals(160, resumed.energyToGenerate());
    }
}
