package io.github.scuba10steve.ssm.gametest;

import io.github.scuba10steve.ssm.machine.ProcessingMachineBlockEntity;
import io.github.scuba10steve.ssm.registry.ModBlocks;
import io.github.scuba10steve.ssm.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ssm")
@PrefixGameTestTemplate(false)
public class CrusherGameTest {

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void crusherDoublesRawIronWithSufficientEnergy(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.CRUSHER.get());
        ProcessingMachineBlockEntity be = (ProcessingMachineBlockEntity) helper.getBlockEntity(pos);
        be.inventory.setStackInSlot(0, new ItemStack(Items.RAW_IRON));
        be.energyStorage.receiveEnergy(100_000, false);

        helper.runAfterDelay(120, () -> {
            ItemStack output = be.inventory.getStackInSlot(1);
            helper.assertTrue(output.getItem().toString().contains("crushed_iron") && output.getCount() == 2,
                    "expected 2x crushed_iron in output slot");
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
        // Output slot already full: no room for the 2 crushed_iron the recipe would produce.
        be.inventory.setStackInSlot(1, new ItemStack(ModItems.CRUSHED_IRON.get(), 64));
        be.energyStorage.receiveEnergy(100_000, false);

        helper.runAfterDelay(120, () -> {
            helper.assertTrue(!be.inventory.getStackInSlot(0).isEmpty(),
                    "input must not be consumed while output has no room");
            helper.assertTrue(be.inventory.getStackInSlot(1).getCount() == 64,
                    "output slot must remain exactly 64, nothing duplicated or lost");
            helper.succeed();
        });
    }
}
