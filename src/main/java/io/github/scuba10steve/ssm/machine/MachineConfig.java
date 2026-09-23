package io.github.scuba10steve.ssm.machine;

import java.util.function.IntSupplier;

public record MachineConfig(
        String id,
        MachineRecipeLookup recipeLookup,
        int outputSlotCount,
        IntSupplier energyCapacity,
        IntSupplier energyPerTick,
        IntSupplier baseProcessTicks
) {}
