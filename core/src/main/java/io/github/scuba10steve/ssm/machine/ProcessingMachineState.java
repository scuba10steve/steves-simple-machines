package io.github.scuba10steve.ssm.machine;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/** Progress/inventory state machine for a processing machine, extracted so it's testable without a live Level. */
public class ProcessingMachineState {

    /** energyConsumed is true only on a tick that actually incremented progress — never on a tick spent parked at completion waiting for output space. */
    public record TickOutcome(boolean completed, boolean energyConsumed) {}

    private final MachineInventory inventory;
    private final int outputSlotCount;
    private final MachineRecipeLookup recipeLookup;
    private int progress;

    public ProcessingMachineState(MachineInventory inventory, int outputSlotCount, MachineRecipeLookup recipeLookup) {
        this.inventory = inventory;
        this.outputSlotCount = outputSlotCount;
        this.recipeLookup = recipeLookup;
    }

    public int getProgress() {
        return progress;
    }

    /** Used only by ProcessingMachineBlockEntity#loadAdditional to restore progress after a chunk reload. */
    public void setProgress(int progress) {
        this.progress = progress;
    }

    public TickOutcome tick(Level level, boolean energyAvailableForOneTick) {
        ItemStack input = inventory.getStackInSlot(0);
        Optional<MachineRecipeResult> current = recipeLookup.find(level, input);

        if (current.isEmpty()) {
            progress = 0;
            return new TickOutcome(false, false);
        }

        MachineRecipeResult result = current.get();
        boolean energyConsumedThisTick = false;

        if (progress < result.processTicks()) {
            if (!energyAvailableForOneTick) {
                return new TickOutcome(false, false);
            }
            progress++;
            energyConsumedThisTick = true;
        }

        if (progress < result.processTicks()) {
            return new TickOutcome(false, energyConsumedThisTick);
        }

        // progress == processTicks: ready to complete, but only if all outputs fit.
        if (canInsertAll(result.outputs())) {
            insertAll(result.outputs());
            inventory.extractItem(0, 1, false);
            progress = 0;
            return new TickOutcome(true, energyConsumedThisTick);
        }

        // Held at completion, waiting for output space. energyConsumedThisTick is only true
        // the one tick where progress just arrived here — every subsequent tick spent parked
        // here takes the `progress < result.processTicks()` == false branch above without ever
        // reaching the `if (!energyAvailableForOneTick)` check, so no further energy is charged.
        return new TickOutcome(false, energyConsumedThisTick);
    }

    /** Simulates inserting every output against a scratch copy of the output slots' current contents, so two outputs that would each individually fit are correctly rejected if they don't BOTH fit together. */
    private boolean canInsertAll(List<ItemStack> outputs) {
        ItemStack[] simulated = new ItemStack[outputSlotCount];
        for (int i = 0; i < outputSlotCount; i++) {
            simulated[i] = inventory.getStackInSlot(1 + i).copy();
        }
        for (ItemStack stack : outputs) {
            if (!tryMergeInto(simulated, stack.copy())) {
                return false;
            }
        }
        return true;
    }

    private boolean tryMergeInto(ItemStack[] simulatedSlots, ItemStack toInsert) {
        for (int i = 0; i < simulatedSlots.length; i++) {
            ItemStack existing = simulatedSlots[i];
            if (existing.isEmpty()) {
                simulatedSlots[i] = toInsert;
                return true;
            }
            if (ItemStack.isSameItemSameComponents(existing, toInsert)) {
                int room = existing.getMaxStackSize() - existing.getCount();
                if (room >= toInsert.getCount()) {
                    existing.grow(toInsert.getCount());
                    return true;
                }
            }
        }
        return false;
    }

    /** Only called after canInsertAll has already verified the full batch fits, so real insertion here cannot fail partway through. */
    private void insertAll(List<ItemStack> outputs) {
        for (ItemStack stack : outputs) {
            ItemStack remaining = stack.copy();
            for (int slot = 1; slot <= outputSlotCount && !remaining.isEmpty(); slot++) {
                remaining = inventory.insertItem(slot, remaining, false);
            }
        }
    }
}
