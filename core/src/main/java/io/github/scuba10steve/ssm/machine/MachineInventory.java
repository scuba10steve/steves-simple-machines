package io.github.scuba10steve.ssm.machine;

import net.minecraft.world.item.ItemStack;

/** Implementations should follow {@code net.neoforged.neoforge.items.IItemHandler}-equivalent semantics: cap-and-remainder on insert, partial-or-full on extract. */
public interface MachineInventory {
    ItemStack getStackInSlot(int slot);

    ItemStack extractItem(int slot, int amount, boolean simulate);

    ItemStack insertItem(int slot, ItemStack stack, boolean simulate);
}
