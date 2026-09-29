package io.github.scuba10steve.ssm.machine;

import net.minecraft.world.item.ItemStack;

class FakeMachineInventory implements MachineInventory {
    private final ItemStack[] slots;

    FakeMachineInventory(int size) {
        slots = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            slots[i] = ItemStack.EMPTY;
        }
    }

    void setStackInSlot(int slot, ItemStack stack) {
        slots[slot] = stack;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slots[slot];
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack existing = slots[slot];
        if (existing.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        int toExtract = Math.min(amount, existing.getCount());
        ItemStack extracted = existing.copy();
        extracted.setCount(toExtract);
        if (!simulate) {
            if (toExtract == existing.getCount()) {
                slots[slot] = ItemStack.EMPTY;
            } else {
                existing.shrink(toExtract);
            }
        }
        return extracted;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack existing = slots[slot];
        if (existing.isEmpty()) {
            if (!simulate) {
                slots[slot] = stack.copy();
            }
            return ItemStack.EMPTY;
        }
        if (!ItemStack.isSameItemSameComponents(existing, stack)) {
            return stack;
        }
        int room = existing.getMaxStackSize() - existing.getCount();
        int toInsert = Math.min(room, stack.getCount());
        if (toInsert <= 0) {
            return stack;
        }
        if (!simulate) {
            existing.grow(toInsert);
        }
        ItemStack remainder = stack.copy();
        remainder.shrink(toInsert);
        return remainder;
    }
}
