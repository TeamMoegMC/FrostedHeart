/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalCompletion;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputEnvelope;
import com.teammoeg.frostedheart.content.climate.thermal.solver.TiledThermalField;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Original geometry/messages and residency effects, with the real fixed engine. */
@GameTestHolder("frostedheart_tiled_residency")
@PrefixGameTestTemplate(false)
public final class TiledThermalResidencyGameTests {
    @GameTest(template = "phase0a_empty", batch = "fixed_residency", timeoutTicks = 400)
    public static void airFrontierUsesEnterAndExitHysteresisAndReleasesColdSpace(GameTestHelper helper) {
        var origin = new BlockPos(253952, 192, 253952); fill(helper, origin, 4, false);
        MinecraftThermalInput.closeActiveLevel(helper.getLevel());
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0, false)) {
            long section = SectionPos.asLong(origin);
            helper.assertTrue(desired(helper, residency(helper, simulation, origin, 1, 0), section) == 0,
                    "Removing the last seed releases an actual cold captured Brick through the original completion");
            setAir(helper, simulation, section, .75);
            var belowEnter = residency(helper, simulation, origin, 1, 0);
            helper.assertTrue(!contains(belowEnter, section, 2), "A new frontier does not enter below 1C");
            setAir(helper, simulation, section, 1.25);
            var hot = residency(helper, simulation, origin, 1, 0);
            helper.assertTrue((desired(helper, hot, section) & 3) == 3, "A heated Air face requests the next actual Brick");
            setAir(helper, simulation, section, .75);
            residency(helper, simulation, origin, 1, 0);
            helper.assertTrue((currentDesired(simulation, section) & 3) == 3,
                    "Previously hot space stays owned down to the 0.5C release threshold");
            setAir(helper, simulation, section, .49);
            helper.assertTrue(desired(helper, residency(helper, simulation, origin, 1, 0), section) == 0,
                    "The same numerical owner emits zero desired coverage after real residuals fall below 0.5C");
            helper.assertTrue(desired(helper, residency(helper, simulation, origin, 1, 1), section) == 1,
                    "A real source seed retains cold space without recruiting cold neighbours");
            FHMain.LOGGER.info("THERMAL_FIXED_RESIDENCY_HYSTERESIS,enterC=1,exitC=0.5,newAt0_75=false,hotAt1_25=true,heldAt0_75=true,releasedAt0_49=true,seedRetained=true");
        } finally { MinecraftThermalInput.closeActiveLevel(helper.getLevel()); }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "fixed_residency", timeoutTicks = 400)
    public static void naturalPhasePlateauRetainsItsBodyWithoutRecruitingUntouchedMaterials(GameTestHelper helper) {
        var origin = new BlockPos(258048, 192, 258048); fill(helper, origin, 8, true);
        helper.getLevel().setBlockAndUpdate(origin.offset(3, 1, 1), Blocks.WATER.defaultBlockState());
        MinecraftThermalInput.closeActiveLevel(helper.getLevel());
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, -23.7, false)) {
            long section = SectionPos.asLong(origin);
            var sample = new TiledThermalField.Sample();
            helper.assertTrue(simulation.fixed.read(origin.offset(3, 1, 1).asLong(), true, sample)
                            && sample.temperatureC() > -20, "The real water law starts on its natural phase plateau");
            residency(helper, simulation, origin, 1, 0);
            long wanted = currentDesired(simulation, section);
            helper.assertTrue((wanted & 1) != 0 && (wanted & 2) == 0,
                    "Natural latent/plateau ownership cannot relay into the next untouched material Brick; desired=" + wanted);
            FHMain.LOGGER.info("THERMAL_FIXED_NATURAL_PHASE_RESIDENCY,naturalC=-23.7,waterC={},desiredMask={},nextMaterialBrick=false", sample.temperatureC(), wanted);
        } finally { MinecraftThermalInput.closeActiveLevel(helper.getLevel()); }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "fixed_residency_shrink", timeoutTicks = 400)
    public static void partialShrinkRemovesAcceptedCellsOnlyAtPublicationAndCompactsTheirStorage(GameTestHelper helper) {
        var origin = new BlockPos(262144, 192, 262144); fill(helper, origin, 8, false);
        MinecraftThermalInput.closeActiveLevel(helper.getLevel());
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 8, 4, 4, 0, false)) {
            long section = SectionPos.asLong(origin); var removed = origin.offset(5, 1, 1);
            setAir(helper, simulation, section, .25);
            double before = simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ();
            var page = simulation.page(origin);
            var cut = new ThermalInputBatch.PageResidencyUpdate(page, page.liveGeometryRevision(), 1, 1,
                    simulation.capture.captureBricks(section, null, simulation.capture.unresolvedPage(), 1), null, null);
            simulation.engine.offerInput(new ThermalInputEnvelope(input(simulation, cut), new SpatialSourceTimeline.Window[0]));
            var sample = new TiledThermalField.Sample();
            helper.assertTrue(simulation.fixed.read(removed.asLong(), false, sample), "Offered shrink keeps Accepted readable before publication");
            long deadline = System.nanoTime() + 10_000_000_000L;
            while (simulation.engine.hasWork()) {
                helper.assertTrue(simulation.engine.hasReadyWork() && System.nanoTime() < deadline, "Finish original shrink publication and cleanup");
                simulation.engine.processWorkRound(); while (simulation.engine.pollWorkCompletion() != null) { }
            }
            helper.assertTrue(!simulation.fixed.read(removed.asLong(), false, sample), "The published field no longer exposes a removed Brick");
            helper.assertTrue(simulation.fixed.activeBricks(section) == 1 && simulation.fixed.sectionCellCount(section) == 64,
                    "A partial shrink releases the old numerical arrays instead of retaining a high-water capacity forever");
            near(helper, before, simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ(), "Removed live energy crosses the explicit ownership boundary once");
            FHMain.LOGGER.info("THERMAL_FIXED_PARTIAL_SHRINK,beforeCells=128,afterCells={},acceptedUntilCommit=true,removedReadable=false,energyAccounted=true", simulation.fixed.sectionCellCount(section));
        } finally { MinecraftThermalInput.closeActiveLevel(helper.getLevel()); }
        helper.succeed();
    }

    private static ThermalCompletion residency(GameTestHelper helper, ThermalMaterialBehaviorGameTests.Simulation simulation,
            BlockPos origin, long captured, long seeds) {
        var page = simulation.page(origin);
        var update = new ThermalInputBatch.PageResidencyUpdate(page, page.liveGeometryRevision(), captured, seeds,
                simulation.capture.captureBricks(page.sectionKey(), null, simulation.capture.unresolvedPage(), captured), null, null);
        return RegionalEngineTestDriver.submit(helper, simulation.engine, input(simulation, update));
    }
    private static ThermalInputBatch input(ThermalMaterialBehaviorGameTests.Simulation simulation, ThermalInputBatch.PageResidencyUpdate update) {
        return new ThermalInputBatch(1, ++simulation.sequence, simulation.tick, ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RETIREMENTS,
                new ThermalInputBatch.PageResidencyUpdate[] {update}, ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_ENVIRONMENT_UPDATES,
                ThermalInputBatch.NO_PHASE_ACKS, Double.NaN);
    }
    private static void setAir(GameTestHelper helper, ThermalMaterialBehaviorGameTests.Simulation simulation, long section, double temperature) {
        var snapshot = new TiledThermalField.SectionSnapshot();
        helper.assertTrue(simulation.fixed.snapshotAccepted(section, snapshot), "Read canonical Accepted initial conditions");
        for (int cell = 0; cell < 4096; cell++) if (simulation.profiles.signatures().isAir(snapshot.signatures[cell]))
            snapshot.enthalpyJ[cell] = temperature * simulation.fixed.airCapacityJPerK();
        helper.assertTrue(simulation.fixed.restoreValues(section, snapshot.enthalpyJ, snapshot.branches), "Install controlled cell energies between physical steps");
    }
    private static long desired(GameTestHelper helper, ThermalCompletion completion, long section) {
        long result = -1;
        for (var update : completion.residencyUpdates()) if (update.sectionKey() == section) result = update.desiredBrickMask();
        helper.assertTrue(result != -1, "The original completion must carry an absolute desired mask for " + section);
        return result;
    }
    private static boolean contains(ThermalCompletion completion, long section, long mask) {
        for (var update : completion.residencyUpdates()) if (update.sectionKey() == section
                && (mask == 0 ? update.desiredBrickMask() == 0 : (update.desiredBrickMask() & mask) != 0)) return true;
        return false;
    }
    private static long currentDesired(ThermalMaterialBehaviorGameTests.Simulation simulation, long section) {
        Object pages = ThermalLoadedWorldGameTests.read(simulation.engine, "pages");
        it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap desired = ThermalLoadedWorldGameTests.read(pages, "desiredBySection");
        return desired.get(section);
    }
    private static void fill(GameTestHelper helper, BlockPos origin, int width, boolean stone) {
        for (int x = 0; x < width; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            helper.getLevel().setBlockAndUpdate(origin.offset(x, y, z), (stone ? Blocks.STONE : Blocks.AIR).defaultBlockState());
    }
    private static void near(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) <= 1e-7 * Math.max(1, Math.abs(expected)), message + ";expected=" + expected + ",actual=" + actual);
    }
}
