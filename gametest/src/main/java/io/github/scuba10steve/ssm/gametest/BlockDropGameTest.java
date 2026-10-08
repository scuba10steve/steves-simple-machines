package io.github.scuba10steve.ssm.gametest;

import io.github.scuba10steve.ssm.generator.CombustionGeneratorBlockEntity;
import io.github.scuba10steve.ssm.machine.ProcessingMachineBlockEntity;
import io.github.scuba10steve.ssm.registry.ModBlocks;
import io.github.scuba10steve.ssm.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Regression coverage for the drop-on-break fix wave: loot tables (block item drops) and
 * onRemove (contents drop) for the machine blocks, exercised through the REAL survival-mining
 * path rather than a direct level.destroyBlock bypass.
 *
 * A previous bug had all 7 blocks tagged requiresCorrectToolForDrops() with no matching
 * mineable/pickaxe block tag, which made every block unharvestable by any tool in real
 * survival play — the loot table was correct but never consulted. level.destroyBlock(pos,
 * true, null, ...) skips the harvest-tool check entirely, so it would NOT have caught that
 * regression. ServerPlayerGameMode#destroyBlock (the method a real player mining a block goes
 * through) DOES run the harvest-tool check (BlockState#canHarvestBlock), so these tests use a
 * survival-mode mock player and go through player.gameMode.destroyBlock(pos) instead.
 *
 * Note: a bare empty hand is NOT sufficient here, and deliberately isn't used. These blocks
 * still carry requiresCorrectToolForDrops(), and an empty hand has no Tool data component at
 * all, so Item#isCorrectToolForDrops always returns false for it regardless of any block tag
 * (confirmed against decompiled net.minecraft.world.item.Item / component.Tool sources) — a
 * bare-hand test would fail even against fully-correct code. What the fix actually guarantees
 * is that these blocks are NOT tool-TIER gated (no incorrect_for_*_tool tag was added), so the
 * lowest tier pickaxe is deliberately used below to prove any pickaxe works, not just an iron
 * or diamond one.
 */
@GameTestHolder("ssm")
@PrefixGameTestTemplate(false)
public class BlockDropGameTest {

    private static ServerPlayer survivalMinerWithWoodenPickaxe(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WOODEN_PICKAXE));
        return player;
    }

    private static List<ItemEntity> itemDropsNear(GameTestHelper helper, BlockPos relativePos) {
        BlockPos absolute = helper.absolutePos(relativePos);
        AABB searchBox = new AABB(absolute).inflate(2.0);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, searchBox);
    }

    private static boolean anyDropIs(List<ItemEntity> drops, net.minecraft.world.item.Item item) {
        return drops.stream().anyMatch(e -> e.getItem().getItem() == item);
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void breakingCrusherWithSurvivalPickaxeDropsBlockAndContents(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.CRUSHER.get());
        ProcessingMachineBlockEntity be = (ProcessingMachineBlockEntity) helper.getBlockEntity(pos);
        be.inventory.setStackInSlot(0, new ItemStack(Items.RAW_IRON));

        ServerPlayer player = survivalMinerWithWoodenPickaxe(helper);

        // No tick delay: destroy in the same instant the fuel/input is placed, before any block
        // ticking can run, so nothing has a chance to consume the item out of the slot first.
        boolean destroyed = player.gameMode.destroyBlock(helper.absolutePos(pos));
        helper.assertTrue(destroyed, "expected the survival player's destroyBlock call to succeed");
        helper.assertTrue(helper.getBlockState(pos).isAir(), "expected the crusher block to be removed");

        List<ItemEntity> drops = itemDropsNear(helper, pos);
        helper.assertTrue(anyDropIs(drops, ModItems.CRUSHER.get()),
                "expected a ssm:crusher item drop from the loot table");
        helper.assertTrue(anyDropIs(drops, Items.RAW_IRON),
                "expected the raw iron from the input slot to drop via onRemove");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void breakingCombustionGeneratorWithSurvivalPickaxeDropsBlockAndFuel(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.COMBUSTION_GENERATOR.get());
        CombustionGeneratorBlockEntity be = (CombustionGeneratorBlockEntity) helper.getBlockEntity(pos);
        be.fuelHandler.setStackInSlot(0, new ItemStack(Items.COAL));

        ServerPlayer player = survivalMinerWithWoodenPickaxe(helper);

        // No tick delay here either: CombustionGeneratorBlockEntity#tick immediately pulls the
        // coal out of fuelHandler and into its internal burn-state counter on the very first
        // tick it gets (see CombustionGeneratorBlockEntity.tick), which would empty the slot
        // before onRemove ever saw it. Destroying synchronously, before any level tick runs,
        // is required to actually exercise the "fuel item still in the handler" drop path.
        boolean destroyed = player.gameMode.destroyBlock(helper.absolutePos(pos));
        helper.assertTrue(destroyed, "expected the survival player's destroyBlock call to succeed");
        helper.assertTrue(helper.getBlockState(pos).isAir(), "expected the combustion generator block to be removed");

        List<ItemEntity> drops = itemDropsNear(helper, pos);
        helper.assertTrue(anyDropIs(drops, ModItems.COMBUSTION_GENERATOR.get()),
                "expected a ssm:combustion_generator item drop from the loot table");
        helper.assertTrue(anyDropIs(drops, Items.COAL),
                "expected the coal from the fuel slot to drop via GeneratorBlock's onRemove");
        helper.succeed();
    }
}
