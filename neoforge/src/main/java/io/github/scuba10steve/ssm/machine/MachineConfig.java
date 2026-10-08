package io.github.scuba10steve.ssm.machine;

import net.minecraft.world.inventory.MenuType;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

public record MachineConfig(
        String id,
        MachineRecipeLookup recipeLookup,
        int outputSlotCount,
        IntSupplier energyCapacity,
        IntSupplier energyPerTick,
        IntSupplier baseProcessTicks,
        Supplier<MenuType<ProcessingMachineMenu>> menuType
) {}
