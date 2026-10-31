/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ScatteredLoadObservation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** One deliberately failed private preparation, followed by the actual native recovery path. */
@GameTestHolder("frostedheart_worker_recovery")
@PrefixGameTestTemplate(false)
public final class ContinuousAirWorkerRecoveryGameTests {
    private static volatile Fault active;
    private static final class Fault {
        final long section;
        volatile boolean armed, recreated;
        volatile long generation = -1;
        Fault(BlockPos point) { section = SectionPos.asLong(point.getX() >> 4, point.getY() >> 4, point.getZ() >> 4); }
    }

    /** Called only by the GameTest bridge; no production exception or scheduler mode is added. */
    public static void geometry(ThermalInputBatch input, String stage, boolean completed) {
        var fault = active;
        if (fault == null) return;
        if (fault.generation >= 0 && input.dimensionGeneration() > fault.generation) fault.recreated = true;
        if (!fault.armed || completed || !"TOPOLOGY".equals(stage)) return;
        boolean touches = false;
        for (int index = 0; index < input.geometry().size(); index++)
            touches |= input.geometry().page(index).sectionKey() == fault.section;
        if (!touches) return;
        fault.armed = false; fault.generation = input.dimensionGeneration();
        FHMain.LOGGER.info("THERMAL_EXPECTED_WORKER_FAILURE,generation={},tick={},section={}",
                fault.generation, input.targetTick(), fault.section);
        throw new IllegalStateException("Intentional GameTest failure before topology preparation");
    }

    @GameTest(template = "phase0a_empty", timeoutTicks = 2400)
    public static void routedRoomsSurviveNativeWorkerReplacement(GameTestHelper helper) throws java.io.IOException {
        var level = helper.getLevel();
        ContinuousAirPrecisionProductionGameTests.restoreReferenceClimate(helper, true);
        BlockPos[] origins = {new BlockPos(2048, 80, 2048), new BlockPos(2176, 80, 2048)};
        BlockPos[] fires = new BlockPos[2];
        for (int i = 0; i < origins.length; i++) {
            ThermalConvergenceScenes.force(helper, origins[i], 16, true);
            fires[i] = ContinuousAirProductionGameTests.room(helper, origins[i]);
            level.setBlockAndUpdate(fires[i].above(), Blocks.STONE_SLAB.defaultBlockState());
            ContinuousAirProductionGameTests.useItem(helper, fires[i], Items.FLINT_AND_STEEL);
        }
        var material = fires[0].east(2);
        level.setBlockAndUpdate(material, Blocks.OAK_LOG.defaultBlockState());
        var fault = new Fault(material); active = fault;
        var observation = new ScatteredLoadObservation(fires[0].asLong());
        var sample = new ThermalEnvironmentSample();
        long[] before = new long[2], recovered = new long[2];
        ContinuousAirGameTestPacing.beginMeasurement();
        helper.startSequence().thenIdle(500).thenWaitUntil(() -> {
            for (int i = 0; i < fires.length; i++) {
                var point = fires[i].above();
                ContinuousAirProductionGameTests.sampleAt(level, point.getX() + .5, point.getY() + .5, point.getZ() + .5, sample);
                helper.assertTrue(sample.airAvailable() && sample.airSampleTick() >= 0 && sample.sharedAirRegion(),
                        "Both actual slab routes must have an online field before recovery");
                before[i] = sample.airSampleTick();
                var direct = fires[i].above(2);
                ContinuousAirProductionGameTests.sampleAt(level, direct.getX() + .5, direct.getY() + .5, direct.getZ() + .5, sample);
                helper.assertTrue(sample.airAvailable() && sample.airSampleTick() >= 0
                                && MinecraftThermalInput.describeAirRead(level, direct).startsWith("geometry_current"),
                        "Both direct fields must also be current before the deliberate failure");
            }
        }).thenExecute(() -> {
            fault.armed = true;
            level.setBlockAndUpdate(material, Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        }).thenWaitUntil(() -> {
            helper.assertTrue(fault.recreated, "The normal worker-failure handler must replace its generation");
            for (int i = 0; i < fires.length; i++) {
                var point = fires[i].above();
                ContinuousAirProductionGameTests.sampleAt(level, point.getX() + .5, point.getY() + .5, point.getZ() + .5, sample);
                helper.assertTrue(sample.airAvailable() && sample.sharedAirRegion() && sample.airSampleTick() > before[i]
                                && level.getGameTime() - sample.airSampleTick() <= 40,
                        "Both real rooms must recover current routed geometry and advance beyond the old generation");
                recovered[i] = sample.airSampleTick();
                var direct = fires[i].above(2);
                ContinuousAirProductionGameTests.sampleAt(level, direct.getX() + .5, direct.getY() + .5, direct.getZ() + .5, sample);
                helper.assertTrue(sample.airAvailable() && sample.airSampleTick() > before[i]
                                && level.getGameTime() - sample.airSampleTick() <= 40
                                && MinecraftThermalInput.describeAirRead(level, direct).startsWith("geometry_current"),
                        "The direct fields must recover current geometry and new physical time too");
            }
        }).thenIdle(200).thenExecute(() -> {
            level.getServer().saveEverything(true, true, true);
            var result = observation.snapshot();
            FHMain.LOGGER.info("THERMAL_WORKER_RECOVERY_RESULT,generation={},before={},recovered={},observation={}",
                    fault.generation, java.util.Arrays.toString(before), java.util.Arrays.toString(recovered), result);
            active = null; observation.close();
            helper.assertTrue(result.runtimeFailures() == 1 && result.energyObservations() > 0
                            && result.energyFailures() == 0 && result.orderingFailures() == 0,
                    "One deliberate replacement must recover without another failure or ownership imbalance: " + result);
            for (int i = 0; i < fires.length; i++) {
                ContinuousAirProductionGameTests.useItem(helper, fires[i], Items.IRON_SHOVEL);
                ThermalConvergenceScenes.force(helper, origins[i], 16, false);
            }
        }).thenSucceed();
    }
}
