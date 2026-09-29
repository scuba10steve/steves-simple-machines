package io.github.scuba10steve.ssm.generator;

import java.util.List;

public final class SkyColumnCheck {
    private SkyColumnCheck() {}

    public static boolean isColumnClear(List<Boolean> isAirAboveEachStep) {
        return isAirAboveEachStep.stream().allMatch(Boolean::booleanValue);
    }
}
