package io.github.scuba10steve.ssm.generator;

import java.util.List;

public final class WindExposureCheck {
    private WindExposureCheck() {}

    public static boolean isExposed(int y, int minHeight, List<Boolean> isNeighborAir, int minFreeNeighbors) {
        if (y < minHeight) {
            return false;
        }
        long freeNeighbors = isNeighborAir.stream().filter(Boolean::booleanValue).count();
        return freeNeighbors >= minFreeNeighbors;
    }
}
