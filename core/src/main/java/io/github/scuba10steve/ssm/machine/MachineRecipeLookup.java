package io.github.scuba10steve.ssm.machine;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

public interface MachineRecipeLookup {
    Optional<MachineRecipeResult> find(Level level, ItemStack input);
}
