package io.github.scuba10steve.ssm.generator;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WindExposureCheckTest {

    // Same rationale as AdjacentWaterCheckTest: this module's neoFormVersion-only classpath has
    // no modded harness to bootstrap vanilla's registries, and Blocks' static init touches
    // BuiltInRegistries, which asserts Bootstrap.bootStrap() already ran. bootStrap() is
    // idempotent, so calling it here is safe regardless of what other test classes also do.
    @BeforeAll
    static void bootstrapRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static final List<BlockState> ALL_SIX_AIR = List.of(
            Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(),
            Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState());

    private static final List<BlockState> FIVE_OF_SIX_AIR = List.of(
            Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(),
            Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), Blocks.STONE.defaultBlockState());

    private static final List<BlockState> FOUR_OF_SIX_AIR = List.of(
            Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(),
            Blocks.AIR.defaultBlockState(), Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState());

    @Test
    void trueWhenAtMinHeightWithEnoughFreeNeighbors() {
        assertTrue(WindExposureCheck.isExposed(100, 100, ALL_SIX_AIR, 5));
    }

    @Test
    void trueWhenAboveMinHeightWithExactlyEnoughFreeNeighbors() {
        assertTrue(WindExposureCheck.isExposed(150, 100, FIVE_OF_SIX_AIR, 5));
    }

    @Test
    void falseWhenBelowMinHeightEvenWithAllNeighborsFree() {
        assertFalse(WindExposureCheck.isExposed(99, 100, ALL_SIX_AIR, 5));
    }

    @Test
    void falseWhenAtMinHeightButNotEnoughFreeNeighbors() {
        assertFalse(WindExposureCheck.isExposed(100, 100, FOUR_OF_SIX_AIR, 5));
    }
}
