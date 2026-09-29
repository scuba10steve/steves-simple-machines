package io.github.scuba10steve.ssm.generator;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WindExposureCheckTest {

    private static final List<Boolean> ALL_SIX_OPEN = List.of(true, true, true, true, true, true);
    private static final List<Boolean> FIVE_OF_SIX_OPEN = List.of(true, true, true, true, true, false);
    private static final List<Boolean> FOUR_OF_SIX_OPEN = List.of(true, true, true, true, false, false);

    @Test
    void trueWhenAtMinHeightWithEnoughFreeNeighbors() {
        assertTrue(WindExposureCheck.isExposed(100, 100, ALL_SIX_OPEN, 5));
    }

    @Test
    void trueWhenAboveMinHeightWithExactlyEnoughFreeNeighbors() {
        assertTrue(WindExposureCheck.isExposed(150, 100, FIVE_OF_SIX_OPEN, 5));
    }

    @Test
    void falseWhenBelowMinHeightEvenWithAllNeighborsFree() {
        assertFalse(WindExposureCheck.isExposed(99, 100, ALL_SIX_OPEN, 5));
    }

    @Test
    void falseWhenAtMinHeightButNotEnoughFreeNeighbors() {
        assertFalse(WindExposureCheck.isExposed(100, 100, FOUR_OF_SIX_OPEN, 5));
    }
}
