package io.github.scuba10steve.ssm.machine;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProcessingMachineStateTest {

    private static MachineRecipeLookup fixedResultLookup(ItemStack expectedInput, MachineRecipeResult result) {
        return (level, input) -> ItemStack.isSameItemSameComponents(input, expectedInput)
                ? Optional.of(result)
                : Optional.empty();
    }

    @Test
    void advancesProgressWhenEnergyAvailableAndRecipeMatches() {
        ItemStack input = new ItemStack(Items.RAW_IRON);
        MachineRecipeResult result = new MachineRecipeResult(List.of(new ItemStack(Items.IRON_NUGGET, 2)), 5);
        ItemStackHandler inventory = new ItemStackHandler(2);
        inventory.setStackInSlot(0, input);

        ProcessingMachineState state = new ProcessingMachineState(inventory, 1, fixedResultLookup(input, result));
        ProcessingMachineState.TickOutcome outcome = state.tick(null, /* energyAvailable */ true);

        assertEquals(1, state.getProgress());
        assertFalse(outcome.completed());
    }

    @Test
    void doesNotAdvanceProgressWithoutEnergy() {
        ItemStack input = new ItemStack(Items.RAW_IRON);
        MachineRecipeResult result = new MachineRecipeResult(List.of(new ItemStack(Items.IRON_NUGGET, 2)), 5);
        ItemStackHandler inventory = new ItemStackHandler(2);
        inventory.setStackInSlot(0, input);

        ProcessingMachineState state = new ProcessingMachineState(inventory, 1, fixedResultLookup(input, result));
        state.tick(null, false);

        assertEquals(0, state.getProgress());
    }

    @Test
    void completesAndMovesOutputWhenProgressReachesTarget() {
        ItemStack input = new ItemStack(Items.RAW_IRON, 1);
        ItemStack outputStack = new ItemStack(Items.IRON_NUGGET, 2);
        MachineRecipeResult result = new MachineRecipeResult(List.of(outputStack), 1);
        ItemStackHandler inventory = new ItemStackHandler(2);
        inventory.setStackInSlot(0, input);

        ProcessingMachineState state = new ProcessingMachineState(inventory, 1, fixedResultLookup(input, result));
        state.tick(null, true); // progress 0 -> 1 == target, completes on this same tick

        assertEquals(0, state.getProgress());
        assertTrue(inventory.getStackInSlot(0).isEmpty());
        assertEquals(Items.IRON_NUGGET, inventory.getStackInSlot(1).getItem());
        assertEquals(2, inventory.getStackInSlot(1).getCount());
    }

    @Test
    void pausesAtCompletionWhenOutputSlotFull() {
        ItemStack input = new ItemStack(Items.RAW_IRON, 1);
        MachineRecipeResult result = new MachineRecipeResult(List.of(new ItemStack(Items.IRON_NUGGET, 2)), 1);
        ItemStackHandler inventory = new ItemStackHandler(2);
        inventory.setStackInSlot(0, input);
        inventory.setStackInSlot(1, new ItemStack(Items.IRON_NUGGET, 64)); // full, blocks insertion

        ProcessingMachineState state = new ProcessingMachineState(inventory, 1, fixedResultLookup(input, result));
        state.tick(null, true);

        assertFalse(inventory.getStackInSlot(0).isEmpty(), "input must not be consumed while output has no room");
        assertEquals(1, state.getProgress(), "progress holds at the completion point");
    }

    @Test
    void resetsProgressWhenInputNoLongerMatches() {
        ItemStack input = new ItemStack(Items.RAW_IRON, 1);
        MachineRecipeResult result = new MachineRecipeResult(List.of(new ItemStack(Items.IRON_NUGGET, 2)), 5);
        ItemStackHandler inventory = new ItemStackHandler(2);
        inventory.setStackInSlot(0, input);

        ProcessingMachineState state = new ProcessingMachineState(inventory, 1, fixedResultLookup(input, result));
        state.tick(null, true); // progress -> 1

        inventory.setStackInSlot(0, new ItemStack(Items.DIRT)); // recipe no longer matches
        state.tick(null, true);

        assertEquals(0, state.getProgress());
    }

    @Test
    void doesNotConsumeEnergyOnTicksSpentWaitingForOutputSpace() {
        ItemStack input = new ItemStack(Items.RAW_IRON, 1);
        MachineRecipeResult result = new MachineRecipeResult(List.of(new ItemStack(Items.IRON_NUGGET, 2)), 1);
        ItemStackHandler inventory = new ItemStackHandler(2);
        inventory.setStackInSlot(0, input);
        inventory.setStackInSlot(1, new ItemStack(Items.IRON_NUGGET, 64)); // full, blocks insertion

        ProcessingMachineState state = new ProcessingMachineState(inventory, 1, fixedResultLookup(input, result));
        ProcessingMachineState.TickOutcome first = state.tick(null, true); // reaches completion, fails to insert
        assertTrue(first.energyConsumed(), "the tick that reaches completion still did work");

        ProcessingMachineState.TickOutcome second = state.tick(null, true); // still parked at completion
        assertFalse(second.energyConsumed(), "a tick spent only waiting for output space must not cost energy");
        assertFalse(second.completed());
    }

    @Test
    void twoOutputsThatIndividuallyFitMayStillNotFitTogether() {
        // Two output slots, each already 63/64 full of a DIFFERENT item than either result —
        // each result alone would fit in "a" slot considered independently, but there are only
        // 2 free single-item slots total and this recipe needs 2 slots for 2 distinct new items,
        // so simulating them independently against the same starting state (as a naive
        // per-output simulate-in-isolation implementation would) wrongly reports success twice
        // against slot 1 instead of reserving slot 1 for the first output and slot 2 for the second.
        ItemStack input = new ItemStack(Items.OAK_LOG, 1);
        ItemStack planks = new ItemStack(Items.OAK_PLANKS, 6);
        ItemStack sawdust = new ItemStack(Items.STICK, 1); // stand-in for ssm:sawdust
        MachineRecipeResult result = new MachineRecipeResult(List.of(planks, sawdust), 1);

        ItemStackHandler inventory = new ItemStackHandler(3); // 1 input + 2 output slots
        inventory.setStackInSlot(0, input);
        // Both output slots start completely empty; the point is that a per-output-in-isolation
        // simulation would let BOTH outputs claim "slot 1" as their landing spot, so verify the
        // real implementation actually lands them in two distinct slots.

        ProcessingMachineState state = new ProcessingMachineState(inventory, 2, fixedResultLookup(input, result));
        ProcessingMachineState.TickOutcome outcome = state.tick(null, true);

        assertTrue(outcome.completed());
        boolean slot1HasPlanks = inventory.getStackInSlot(1).is(Items.OAK_PLANKS) && inventory.getStackInSlot(1).getCount() == 6;
        boolean slot2HasSawdust = inventory.getStackInSlot(2).is(Items.STICK) && inventory.getStackInSlot(2).getCount() == 1;
        boolean slot1HasSawdust = inventory.getStackInSlot(1).is(Items.STICK) && inventory.getStackInSlot(1).getCount() == 1;
        boolean slot2HasPlanks = inventory.getStackInSlot(2).is(Items.OAK_PLANKS) && inventory.getStackInSlot(2).getCount() == 6;
        assertTrue((slot1HasPlanks && slot2HasSawdust) || (slot1HasSawdust && slot2HasPlanks),
                "both outputs must land, each in its own slot, with nothing dropped");
    }
}
