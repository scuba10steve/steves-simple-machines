package io.github.scuba10steve.ssm.machine;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class SmelterAdapter {
    private SmelterAdapter() {}

    public static MachineRecipeResult toMachineResult(ItemStack vanillaResult, int flatProcessTicks) {
        return new MachineRecipeResult(List.of(vanillaResult.copyWithCount(1)), flatProcessTicks);
    }
}
