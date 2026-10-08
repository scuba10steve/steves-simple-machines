package io.github.scuba10steve.ssm.gametest;

import io.github.scuba10steve.ssm.machine.ProcessingMachineBlockEntity;
import io.github.scuba10steve.ssm.registry.ModBlocks;
import io.github.scuba10steve.ssm.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

@GameTestHolder("ssm")
@PrefixGameTestTemplate(false)
public class CrusherGameTest {

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void crusherFillsFirstOutputSlotBeforeOverflowingToSecond(GameTestHelper helper) {
        // Crusher's 2 output slots are overflow buffer capacity, not a forced 1-and-1 split: both
        // units of a completion's output land in the first slot that has room (merging normally,
        // same as any stackable item), and the second slot only starts filling once the first
        // reaches its max stack size. See ProcessingMachineState's slot-search order.
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.CRUSHER.get());
        ProcessingMachineBlockEntity be = (ProcessingMachineBlockEntity) helper.getBlockEntity(pos);
        be.inventory.setStackInSlot(0, new ItemStack(Items.RAW_IRON));
        be.energyStorage.receiveEnergy(100_000, false);

        helper.runAfterDelay(120, () -> {
            ItemStack slot1 = be.inventory.getStackInSlot(1);
            ItemStack slot2 = be.inventory.getStackInSlot(2);
            helper.assertTrue(slot1.getItem().toString().contains("crushed_iron") && slot1.getCount() == 2,
                    "expected both units of crushed_iron in the first output slot");
            helper.assertTrue(slot2.isEmpty(), "expected the second output slot to remain empty until the first is full");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void crusherActuallyDrainsItsEnergyBuffer(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.CRUSHER.get());
        ProcessingMachineBlockEntity be = (ProcessingMachineBlockEntity) helper.getBlockEntity(pos);
        be.inventory.setStackInSlot(0, new ItemStack(Items.RAW_IRON));
        int initialEnergy = 1000;
        be.energyStorage.receiveEnergy(initialEnergy, false);

        helper.runAfterDelay(120, () -> {
            int remaining = be.energyStorage.getEnergyStored();
            helper.assertTrue(remaining < initialEnergy,
                    "expected energy buffer to actually decrease from processing, was still " + remaining);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void crusherDoesNotProgressWithoutEnergy(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.CRUSHER.get());
        ProcessingMachineBlockEntity be = (ProcessingMachineBlockEntity) helper.getBlockEntity(pos);
        be.inventory.setStackInSlot(0, new ItemStack(Items.RAW_IRON));
        // No energy given.

        helper.runAfterDelay(120, () -> {
            helper.assertTrue(be.getProgress() == 0, "progress should stay at 0 with no energy");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void crusherPausesWhenOutputBufferIsFullWithoutLosingInput(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.CRUSHER.get());
        ProcessingMachineBlockEntity be = (ProcessingMachineBlockEntity) helper.getBlockEntity(pos);
        be.inventory.setStackInSlot(0, new ItemStack(Items.RAW_IRON));
        // Both output slots already full: no room for either of the 2 separate crushed_iron
        // stacks the recipe would produce. Filling only one slot would not block completion,
        // since the recipe's 2 outputs would simply both land in the other, still-open slot.
        be.inventory.setStackInSlot(1, new ItemStack(ModItems.CRUSHED_IRON.get(), 64));
        be.inventory.setStackInSlot(2, new ItemStack(ModItems.CRUSHED_IRON.get(), 64));
        be.energyStorage.receiveEnergy(100_000, false);

        helper.runAfterDelay(120, () -> {
            helper.assertTrue(!be.inventory.getStackInSlot(0).isEmpty(),
                    "input must not be consumed while output has no room");
            helper.assertTrue(be.inventory.getStackInSlot(1).getCount() == 64
                            && be.inventory.getStackInSlot(2).getCount() == 64,
                    "both output slots must remain exactly 64, nothing duplicated or lost");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void crusherRestoresLegacySizedInventoryWithoutLosingItems(GameTestHelper helper) {
        // Simulates loading a Crusher placed under an older MachineConfig with only 1 output
        // slot (2 total: input + 1 output), before the 2-output-slot redesign. deserializeNBT
        // resizes the handler down to the saved size, then loadAdditional must grow it back to
        // the current size (3) without losing the items that were just loaded into it.
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.CRUSHER.get());
        ProcessingMachineBlockEntity be = (ProcessingMachineBlockEntity) helper.getBlockEntity(pos);
        HolderLookup.Provider registries = helper.getLevel().registryAccess();

        ItemStackHandler legacy = new ItemStackHandler(2);
        legacy.setStackInSlot(0, new ItemStack(Items.RAW_IRON, 5));
        legacy.setStackInSlot(1, new ItemStack(Items.IRON_INGOT, 3));
        CompoundTag saved = new CompoundTag();
        saved.put("inventory", legacy.serializeNBT(registries));
        saved.putInt("energy", 1234);
        saved.putInt("progress", 0);

        be.loadAdditional(saved, registries);

        helper.assertTrue(be.inventory.getSlots() == 3,
                "expected inventory resized to the current output slot count (3 total)");
        ItemStack input = be.inventory.getStackInSlot(0);
        helper.assertTrue(input.is(Items.RAW_IRON) && input.getCount() == 5,
                "expected input slot contents preserved across resize, was " + input);
        ItemStack firstOutput = be.inventory.getStackInSlot(1);
        helper.assertTrue(firstOutput.is(Items.IRON_INGOT) && firstOutput.getCount() == 3,
                "expected first output slot contents preserved across resize, was " + firstOutput);
        helper.assertTrue(be.inventory.getStackInSlot(2).isEmpty(),
                "expected the newly added second output slot to start empty");
        helper.succeed();
    }
}
