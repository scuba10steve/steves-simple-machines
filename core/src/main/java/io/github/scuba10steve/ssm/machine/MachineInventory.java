package io.github.scuba10steve.ssm.machine;

import net.minecraft.world.item.ItemStack;

public interface MachineInventory {
    ItemStack getStackInSlot(int slot);

    ItemStack extractItem(int slot, int amount, boolean simulate);

    ItemStack insertItem(int slot, ItemStack stack, boolean simulate);
}
