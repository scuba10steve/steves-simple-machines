package io.github.scuba10steve.ssm.generator;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SkyColumnCheckTest {

    @Test
    void trueWhenAllBlocksAboveAreAir() {
        assertTrue(SkyColumnCheck.isColumnClear(List.of(true, true, true)));
    }

    @Test
    void falseWhenAnyBlockAboveIsSolid() {
        assertFalse(SkyColumnCheck.isColumnClear(List.of(true, false, true)));
    }
}
