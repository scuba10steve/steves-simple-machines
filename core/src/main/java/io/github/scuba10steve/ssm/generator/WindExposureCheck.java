package io.github.scuba10steve.ssm.generator;

import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class WindExposureCheck {
    private WindExposureCheck() {}

    public static boolean isExposed(int y, int minHeight, List<BlockState> neighborStates, int minFreeNeighbors) {
        if (y < minHeight) {
            return false;
        }
        long freeNeighbors = neighborStates.stream().filter(BlockState::isAir).count();
        return freeNeighbors >= minFreeNeighbors;
    }
}
