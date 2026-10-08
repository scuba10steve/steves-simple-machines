package io.github.scuba10steve.ssm.machine;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProcessingMachineStateTest {

    // Same rationale as AdjacentWaterCheckTest: this module's neoFormVersion-only classpath has
    // no modded harness to bootstrap vanilla's registries, and Items/ItemStack's static init
    // touches BuiltInRegistries, which asserts Bootstrap.bootStrap() already ran. bootStrap() is
    // idempotent, so calling it here is safe regardless of what other test classes also do.
    @BeforeAll
    static void bootstrapRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static MachineRecipeLookup fixedResultLookup(ItemStack expectedInput, MachineRecipeResult result) {
        return (level, input) -> ItemStack.isSameItemSameComponents(input, expectedInput)
                ? Optional.of(result)
                : Optional.empty();
    }

    @Test
    void advancesProgressWhenEnergyAvailableAndRecipeMatches() {
        ItemStack input = new ItemStack(Items.RAW_IRON);
        MachineRecipeResult result = new MachineRecipeResult(List.of(new ItemStack(Items.IRON_NUGGET, 2)), 5);
        FakeMachineInventory inventory = new FakeMachineInventory(2);
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
        FakeMachineInventory inventory = new FakeMachineInventory(2);
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
        FakeMachineInventory inventory = new FakeMachineInventory(2);
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
        FakeMachineInventory inventory = new FakeMachineInventory(2);
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
        FakeMachineInventory inventory = new FakeMachineInventory(2);
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
        FakeMachineInventory inventory = new FakeMachineInventory(2);
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
        // Two output slots, both starting completely empty. The point of this test is that a
        // naive per-output-in-isolation simulation (checking "does this fit anywhere?" against
        // an unmodified copy of the starting state for EACH output) would let both outputs claim
        // "slot 1" as their landing spot, since each individually fits in the first empty slot it
        // sees. The correct implementation threads a running scratch state through all outputs in
        // one simulation pass, so the first output's claim on slot 1 is visible when the second
        // output is considered, correctly routing it to slot 2 instead.
        ItemStack input = new ItemStack(Items.OAK_LOG, 1);
        ItemStack planks = new ItemStack(Items.OAK_PLANKS, 6);
        ItemStack sawdust = new ItemStack(Items.STICK, 1); // stand-in for ssm:sawdust
        MachineRecipeResult result = new MachineRecipeResult(List.of(planks, sawdust), 1);

        FakeMachineInventory inventory = new FakeMachineInventory(3); // 1 input + 2 output slots
        inventory.setStackInSlot(0, input);

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

    @Test
    void combinedOutputsRequiringDistinctSlotsMustAllFitOrNoneAreInserted() {
        // Unlike the test above, this scenario has only ONE truly free output slot, but the
        // recipe needs TWO slots for two distinct new items. This is the case that actually
        // distinguishes cumulative simulation from a naive per-output-independent check: a naive
        // implementation that tests each output "does this fit anywhere?" against a fresh,
        // unmodified copy of the live inventory would see the one free slot as available for
        // EACH output considered on its own, and wrongly report both as fitting. The correct
        // cumulative check must recognize that only one of the two outputs can actually claim
        // that single free slot, so the whole batch must be rejected and the machine held at
        // completion — same as pausesAtCompletionWhenOutputSlotFull, but reached via insufficient
        // combined capacity across multiple outputs rather than one already-full slot.
        ItemStack input = new ItemStack(Items.OAK_LOG, 1);
        ItemStack planks = new ItemStack(Items.OAK_PLANKS, 6);
        ItemStack sawdust = new ItemStack(Items.STICK, 1); // stand-in for ssm:sawdust
        MachineRecipeResult result = new MachineRecipeResult(List.of(planks, sawdust), 1);

        FakeMachineInventory inventory = new FakeMachineInventory(3); // 1 input + 2 output slots
        inventory.setStackInSlot(0, input);
        // Slot 1 is full of an unrelated item (matches neither output), leaving slot 2 as the
        // only free slot — but two distinct new items need two distinct slots.
        inventory.setStackInSlot(1, new ItemStack(Items.COBBLESTONE, 64));

        ProcessingMachineState state = new ProcessingMachineState(inventory, 2, fixedResultLookup(input, result));
        ProcessingMachineState.TickOutcome outcome = state.tick(null, true);

        assertFalse(outcome.completed(), "only one free slot exists for two distinct new-item outputs");
        assertFalse(inventory.getStackInSlot(0).isEmpty(), "input must not be consumed when not all outputs fit together");
        assertEquals(1, state.getProgress(), "progress holds at the completion point");
    }
}
