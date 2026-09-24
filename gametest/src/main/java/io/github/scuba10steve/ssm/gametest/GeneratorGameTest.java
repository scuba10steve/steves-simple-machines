package io.github.scuba10steve.ssm.gametest;

import io.github.scuba10steve.ssm.generator.CombustionGeneratorBlockEntity;
import io.github.scuba10steve.ssm.generator.SolarPanelBlockEntity;
import io.github.scuba10steve.ssm.generator.WaterWheelBlockEntity;
import io.github.scuba10steve.ssm.generator.WindTurbineBlockEntity;
import io.github.scuba10steve.ssm.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ssm")
@PrefixGameTestTemplate(false)
public class GeneratorGameTest {

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    /**
     * Fully seals the given cell (ceiling + all 4 horizontal neighbors) with stone. A single
     * block directly above a generator is NOT enough to cut sky light to zero in this tiny 3x3
     * test arena: sky light leaks in sideways from the still-open adjacent columns, stabilizing
     * around 14 instead of 0 (confirmed by instrumenting the actual getBrightness() values during
     * development). Sealing every horizontal neighbor too removes any path for light to leak in.
     */
    private static void enclose(GameTestHelper helper, BlockPos cell) {
        helper.setBlock(cell.above(), Blocks.STONE);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            helper.setBlock(cell.relative(dir), Blocks.STONE);
        }
    }

    // --- Combustion Generator ---

    @GameTest(template = "empty")
    public static void combustionGeneratorProducesEnergyWithFuel(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.COMBUSTION_GENERATOR.get());
        CombustionGeneratorBlockEntity be = (CombustionGeneratorBlockEntity) helper.getBlockEntity(POS);
        be.fuelHandler.setStackInSlot(0, new ItemStack(Items.COAL));

        helper.runAfterDelay(10, () -> {
            helper.assertTrue(be.energyStorage.getEnergyStored() > 0, "expected energy stored with coal in fuel slot");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void combustionGeneratorAcceptsNonHardcodedVanillaFuel(GameTestHelper helper) {
        // Regression test for getBurnTime(ItemStack) delegating to the real furnace fuel
        // registry (ItemStack#getBurnTime(RecipeType)) instead of a hardcoded 3-item table.
        // Items.STICK was never one of the 3 hardcoded items (coal/charcoal/coal block) but
        // has a real vanilla furnace fuel value, so this only passes with the real lookup.
        helper.setBlock(POS, ModBlocks.COMBUSTION_GENERATOR.get());
        CombustionGeneratorBlockEntity be = (CombustionGeneratorBlockEntity) helper.getBlockEntity(POS);
        be.fuelHandler.setStackInSlot(0, new ItemStack(Items.STICK));

        helper.runAfterDelay(10, () -> {
            helper.assertTrue(be.energyStorage.getEnergyStored() > 0, "expected energy stored with stick in fuel slot");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void combustionGeneratorProducesNoEnergyWithoutFuel(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.COMBUSTION_GENERATOR.get());
        CombustionGeneratorBlockEntity be = (CombustionGeneratorBlockEntity) helper.getBlockEntity(POS);
        // No fuel given.

        helper.runAfterDelay(10, () -> {
            helper.assertTrue(be.energyStorage.getEnergyStored() == 0, "expected no energy stored without fuel");
            helper.succeed();
        });
    }

    // --- Solar Panel ---

    @GameTest(template = "empty")
    public static void solarPanelProducesEnergyWithSkyAccessAtDay(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.SOLAR_PANEL.get());
        SolarPanelBlockEntity be = (SolarPanelBlockEntity) helper.getBlockEntity(POS);
        helper.setDayTime(6000);

        helper.runAfterDelay(20, () -> {
            helper.assertTrue(be.energyStorage.getEnergyStored() > 0, "expected energy stored with sky access at day");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void solarPanelProducesNoEnergyWhenCovered(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.SOLAR_PANEL.get());
        SolarPanelBlockEntity be = (SolarPanelBlockEntity) helper.getBlockEntity(POS);
        helper.setDayTime(6000);
        enclose(helper, POS.above());

        // Asserts the live generation rate rather than accumulated stored energy: the panel is
        // briefly exposed before the enclosure's sky-light change finishes propagating (this is a
        // real, if tiny, one-tick window — not a test artifact), so a few energy units land in the
        // buffer and never drain (nothing here extracts it). getCurrentRate() reflects only
        // whether the panel is generating THIS tick, so it isn't polluted by that earlier residue.
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(be.getCurrentRate() == 0, "expected zero generation rate when covered");
            helper.succeed();
        });
    }

    // --- Water Wheel ---

    @GameTest(template = "empty")
    public static void waterWheelProducesEnergyWithAdjacentWater(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.WATER_WHEEL.get());
        WaterWheelBlockEntity be = (WaterWheelBlockEntity) helper.getBlockEntity(POS);
        helper.setBlock(POS.relative(Direction.NORTH), Blocks.WATER);

        helper.runAfterDelay(10, () -> {
            helper.assertTrue(be.energyStorage.getEnergyStored() > 0, "expected energy stored with adjacent water");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void waterWheelProducesNoEnergyWithoutAdjacentWater(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.WATER_WHEEL.get());
        WaterWheelBlockEntity be = (WaterWheelBlockEntity) helper.getBlockEntity(POS);
        // No adjacent water.

        helper.runAfterDelay(10, () -> {
            helper.assertTrue(be.energyStorage.getEnergyStored() == 0, "expected no energy stored without adjacent water");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void waterWheelDoesNotReportGeneratingWhenBufferFull(GameTestHelper helper) {
        // Regression test for the drift where currentlyGenerating tracked "is the condition met"
        // instead of "was energy actually accepted": a full buffer must report not-generating even
        // though the environmental condition (adjacent water) is met.
        helper.setBlock(POS, ModBlocks.WATER_WHEEL.get());
        WaterWheelBlockEntity be = (WaterWheelBlockEntity) helper.getBlockEntity(POS);
        be.energyStorage.generate(be.energyStorage.getMaxEnergyStored());
        helper.setBlock(POS.relative(Direction.NORTH), Blocks.WATER);

        helper.runAfterDelay(10, () -> {
            helper.assertTrue(be.energyStorage.getEnergyStored() == be.energyStorage.getMaxEnergyStored(),
                    "expected buffer to remain full");
            helper.assertTrue(!be.isGenerating(), "expected wheel to not report generating when buffer is full");
            helper.succeed();
        });
    }

    // --- Wind Turbine ---

    @GameTest(template = "empty")
    public static void windTurbineProducesEnergyWithClearSky(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.WIND_TURBINE.get());
        WindTurbineBlockEntity be = (WindTurbineBlockEntity) helper.getBlockEntity(POS);
        // Clear sky above by default in the empty test arena.

        helper.runAfterDelay(20, () -> {
            helper.assertTrue(be.energyStorage.getEnergyStored() > 0, "expected energy stored with clear sky above");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void windTurbineProducesNoEnergyUnderRoof(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.WIND_TURBINE.get());
        WindTurbineBlockEntity be = (WindTurbineBlockEntity) helper.getBlockEntity(POS);
        enclose(helper, POS.above());

        // Same reasoning as the covered-solar-panel test: check the live isGenerating() flag,
        // not accumulated stored energy, since a stray tick of energy generated before the
        // enclosure's sky-light change finishes propagating would otherwise never drain away.
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(!be.isGenerating(), "expected turbine to not be generating under a roof");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void windTurbineDoesNotReportGeneratingWhenBufferFull(GameTestHelper helper) {
        // Regression test for the drift where currentlyGenerating tracked "is the condition met"
        // instead of "was energy actually accepted": a full buffer must report not-generating even
        // though the environmental condition (clear sky) is met.
        helper.setBlock(POS, ModBlocks.WIND_TURBINE.get());
        WindTurbineBlockEntity be = (WindTurbineBlockEntity) helper.getBlockEntity(POS);
        be.energyStorage.generate(be.energyStorage.getMaxEnergyStored());
        // Clear sky above by default in the empty test arena.

        helper.runAfterDelay(20, () -> {
            helper.assertTrue(be.energyStorage.getEnergyStored() == be.energyStorage.getMaxEnergyStored(),
                    "expected buffer to remain full");
            helper.assertTrue(!be.isGenerating(), "expected turbine to not report generating when buffer is full");
            helper.succeed();
        });
    }
}
