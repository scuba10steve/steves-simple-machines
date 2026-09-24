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
public class SawmillGameTest {

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void sawmillConvertsOakLogToPlanksAndSawdustWithSufficientEnergy(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.SAWMILL.get());
        ProcessingMachineBlockEntity be = (ProcessingMachineBlockEntity) helper.getBlockEntity(pos);
        be.inventory.setStackInSlot(0, new ItemStack(Items.OAK_LOG));
        be.energyStorage.receiveEnergy(100_000, false);

        helper.runAfterDelay(120, () -> {
            ItemStack planks = be.inventory.getStackInSlot(1);
            ItemStack sawdust = be.inventory.getStackInSlot(2);
            helper.assertTrue(planks.getItem() == Items.OAK_PLANKS && planks.getCount() == 6,
                    "expected 6x oak_planks in output slot 1");
            helper.assertTrue(sawdust.getItem() == ModItems.SAWDUST.get() && sawdust.getCount() == 1,
                    "expected 1x ssm:sawdust in output slot 2");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void sawmillDoesNotProgressWithoutEnergy(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.SAWMILL.get());
        ProcessingMachineBlockEntity be = (ProcessingMachineBlockEntity) helper.getBlockEntity(pos);
        be.inventory.setStackInSlot(0, new ItemStack(Items.OAK_LOG));
        // No energy given.

        helper.runAfterDelay(120, () -> {
            helper.assertTrue(be.getProgress() == 0, "progress should stay at 0 with no energy");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void sawmillPausesWhenOutputBufferIsFullWithoutLosingInput(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.SAWMILL.get());
        ProcessingMachineBlockEntity be = (ProcessingMachineBlockEntity) helper.getBlockEntity(pos);
        be.inventory.setStackInSlot(0, new ItemStack(Items.OAK_LOG));
        // Sawdust output slot already full: a multi-output recipe needs ALL its outputs to fit,
        // so a full sawdust slot must block the whole operation even though the planks slot is empty.
        be.inventory.setStackInSlot(2, new ItemStack(ModItems.SAWDUST.get(), 64));
        be.energyStorage.receiveEnergy(100_000, false);

        helper.runAfterDelay(120, () -> {
            helper.assertTrue(!be.inventory.getStackInSlot(0).isEmpty(),
                    "input must not be consumed while one of the outputs has no room");
            helper.assertTrue(be.inventory.getStackInSlot(1).isEmpty(),
                    "planks slot must stay empty since the paired sawdust output could not fit");
            helper.assertTrue(be.inventory.getStackInSlot(2).getCount() == 64,
                    "sawdust slot must remain exactly 64, nothing duplicated or lost");
            helper.succeed();
        });
    }
}
