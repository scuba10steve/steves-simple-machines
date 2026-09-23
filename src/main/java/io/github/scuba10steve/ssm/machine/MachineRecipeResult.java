package io.github.scuba10steve.ssm.machine;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public record MachineRecipeResult(List<ItemStack> outputs, int processTicks) {}
