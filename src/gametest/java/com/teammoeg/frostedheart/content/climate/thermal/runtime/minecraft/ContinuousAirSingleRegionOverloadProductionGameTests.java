/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalWorkBudget;
import com.teammoeg.frostedheart.content.climate.thermal.solver.RuntimeConvergenceObservation;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_single_region_overload")
@PrefixGameTestTemplate(false)
public final class ContinuousAirSingleRegionOverloadProductionGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 20000)
    public static void oneThousandRealFiresAdvanceCoarselyAndRecoverWithoutStarvingAFineRoom(GameTestHelper helper) throws java.io.IOException {
        helper.assertTrue(ThermalWorkBudget.automatic(), "Use automatic cost policy; no forced quality switch");
        ContinuousAirPrecisionProductionGameTests.restoreReferenceClimate(helper, true);
        var level = helper.getLevel();
        var memoryTarget = FHConfig.COMMON.THERMAL_RUNTIME.thermalMemoryTargetMiB;
        int initialMemoryTarget = memoryTarget.get();
        var controlOrigin = helper.absolutePos(BlockPos.ZERO);
        var origin = helper.absolutePos(new BlockPos(512, 0, 0));
        ThermalConvergenceScenes.force(helper, controlOrigin, 16, true);
        ThermalConvergenceScenes.force(helper, origin, 64, true);
        var control = ContinuousAirProductionGameTests.room(helper, controlOrigin);
        ContinuousAirProductionGameTests.useItem(helper, control, Items.FLINT_AND_STEEL);
        var sample = new ThermalEnvironmentSample();
        BlockPos[][] fires = {null};
        ThermalConvergenceScenes.HallBuild[] building = {null};
        java.util.function.BooleanSupplier[] shrinking = {null};
        long[][] resources = {null};
        BlockPos retained = origin.offset(3, 1, 3), probe = retained.above();
        int[] phase = {0}, controlFailures = {0}, saves = {0};
        int[] unavailable = new int[4]; long[] maximumAge = new long[4];
        int[] unavailableRun = {0}, maximumUnavailableRun = {0};
        long[] pressure = {-1}, monitoring = {-1}, maximumControlAge = {0};
        String[] firstFailure = {null};
        ContinuousAirProductionTiming.eachTick(helper, 19900, elapsed -> {
            if (phase[0] == 0 || phase[0] == 4) return;
            long tick = level.getGameTime();
            if (elapsed % 200 == 0) {
                ThermalConvergenceScenes.fuel(helper, control, 1);
                if (phase[0] <= 2 && building[0] != null && building[0].complete)
                    for (var fire : fires[0]) ThermalConvergenceScenes.fuel(helper, fire, 1);
                else if (phase[0] == 3) ThermalConvergenceScenes.fuel(helper, retained, 1);
            }
            boolean current = ThermalConvergenceScenes.readable(helper, control.above(), sample);
            maximumControlAge[0] = Math.max(maximumControlAge[0], tick - sample.airSampleTick());
            if (!current) {
                controlFailures[0]++;
                if (firstFailure[0] == null) {
                    firstFailure[0] = "tick=" + tick + ",sample=" + sample.airSampleTick()
                            + ",read=" + MinecraftThermalInput.describeAirRead(level, control.above());
                    FHMain.LOGGER.info("THERMAL_SINGLE_REGION_FIRST_CONTROL_FAILURE,{},worker={}", firstFailure[0],
                            MinecraftThermalInput.describeWorkerProgress(level));
                }
            }
            if (phase[0] == 2) {
                int age = Math.toIntExact(tick - pressure[0]);
                if (age > 0 && age <= 1600) {
                    int window = Math.min(3, (age - 1) / 400);
                    ThermalConvergenceScenes.read(helper, probe, sample);
                    if (!sample.airAvailable() || sample.airSampleTick() < 0) {
                        maximumUnavailableRun[0] = Math.max(maximumUnavailableRun[0], ++unavailableRun[0]);
                        if (unavailable[window]++ == 0) FHMain.LOGGER.info(
                                "THERMAL_SINGLE_REGION_UNAVAILABLE,window={},gameTick={},read={}",
                                window, tick, MinecraftThermalInput.describeAirRead(level, probe));
                    }
                    else {
                        unavailableRun[0] = 0;
                        maximumAge[window] = Math.max(maximumAge[window], tick - sample.airSampleTick());
                    }
                }
                if (age == 200 || age == 600 || age == 1000)
                    ContinuousAirProductionGameTests.useItem(helper, fires[0][0], Items.IRON_SHOVEL);
                if (age == 220 || age == 620 || age == 1020)
                    ContinuousAirProductionGameTests.useItem(helper, fires[0][0], Items.FLINT_AND_STEEL);
                if (age == 400 || age == 1200) level.setBlockAndUpdate(origin.offset(16, 2, 16), Blocks.STONE.defaultBlockState());
                if (age == 440 || age == 1240) level.setBlockAndUpdate(origin.offset(16, 2, 16), Blocks.AIR.defaultBlockState());
                if (age == 300 || age == 800 || age == 1300) {
                    level.getServer().saveEverything(true, true, true); saves[0]++;
                    FHMain.LOGGER.info("THERMAL_SINGLE_REGION_SAVE,gameTick={},save={}", tick, saves[0]);
                }
            }
            if (elapsed % 200 == 0) FHMain.LOGGER.info("THERMAL_SINGLE_REGION_PROGRESS,gameTick={},phase={},stats={},rootBytes={},controlFailures={},worker={}",
                    tick, phase[0], RuntimeConvergenceObservation.snapshot(), MinecraftThermalInput.thermalBudgetUsedBytes(),
                    controlFailures[0], MinecraftThermalInput.describeWorkerProgress(level));
        });
        helper.startSequence().thenIdle(200).thenWaitUntil(() -> helper.assertTrue(
                ThermalConvergenceScenes.fresh(helper, control.above(), sample) && ThermalConvergenceScenes.step(helper, control.above()) == 1,
                "Warm the independent normal room before applying the thousand-source load"))
        .thenExecute(() -> {
            phase[0] = 1; monitoring[0] = level.getGameTime();
            resources[0] = ContinuousAirProductionTiming.resources();
            building[0] = new ThermalConvergenceScenes.HallBuild(helper, origin, 32, 20, 2);
            fires[0] = building[0].fires;
            RuntimeConvergenceObservation.arm(control.above(), probe, fires[0]);
        }).thenWaitUntil(() -> helper.assertTrue(building[0].advance(), "Build and ignite the real hall in bounded world-edit batches"))
        .thenWaitUntil(() -> {
            var state = RuntimeConvergenceObservation.snapshot();
            helper.assertTrue(state.boundSources() == 1024 && state.region() != 0 && state.region() != state.controlRegion(),
                    "All 1024 actual enabled sources must feed one real region, separate from the normal room: " + state);
            helper.assertTrue(state.minimum() && state.step() == 4 && state.commits() >= 2, "Automatic minimum quality must publish and keep advancing: " + state);
        }).thenExecute(() -> {
            phase[0] = 2; pressure[0] = level.getGameTime(); RuntimeConvergenceObservation.pressure(pressure[0]);
            FHMain.LOGGER.info("THERMAL_SINGLE_REGION_PRESSURE,gameTick={},sources=1024,region={}", pressure[0], RuntimeConvergenceObservation.snapshot().region());
        }).thenIdle(1600).thenExecute(() -> {
            var state = RuntimeConvergenceObservation.snapshot();
            for (int window = 0; window < 4; window++) {
                FHMain.LOGGER.info("THERMAL_SINGLE_REGION_WINDOW,index={},commits={},unavailableTicks={},maximumAgeTicks={}",
                        window, state.pressureCommits()[window], unavailable[window], maximumAge[window]);
                helper.assertTrue(state.pressureCommits()[window] >= 2, "Each fixed 400-tick stress window needs two new Accepted commits");
                helper.assertTrue(unavailable[window] < 400, "Every pressure window must expose an actual readable Air result");
            }
            helper.assertTrue(saves[0] == 3, "Complete three real saves during pressure");
            helper.assertTrue(controlFailures[0] == 0, "Independent room lost readable service under pressure: " + firstFailure[0]);
            if (initialMemoryTarget < memoryTarget.getDefault()) {
                // This fixture deliberately compresses the resource target. Remove
                // that pressure in the running config before requiring fine recovery.
                memoryTarget.set(memoryTarget.getDefault());
                helper.assertTrue(MinecraftThermalInput.thermalBudgetLimitBytes() == memoryTarget.getDefault() * 1024L * 1024L,
                        "The live worker budget must observe the withdrawn memory pressure");
                FHMain.LOGGER.info("THERMAL_SINGLE_REGION_MEMORY_RELEASE,gameTick={},fromMiB={},toMiB={},liveTargetBytes={}",
                        level.getGameTime(), initialMemoryTarget, memoryTarget.getDefault(), MinecraftThermalInput.thermalBudgetLimitBytes());
            }
            phase[0] = 3;
            shrinking[0] = ThermalConvergenceScenes.shrink(helper, origin, 64, 20, retained);
            FHMain.LOGGER.info("THERMAL_SINGLE_REGION_RELEASE,gameTick={},remainingSources=1", level.getGameTime());
        }).thenWaitUntil(() -> helper.assertTrue(shrinking[0] != null && shrinking[0].getAsBoolean(), "Remove excess sources and structure in bounded real-world batches"))
        .thenWaitUntil(() -> {
            helper.assertTrue(MinecraftThermalInput.enabledPhysicalSourceCount(level) == 2, "Only the two real remaining campfires supply heat");
            helper.assertTrue(ThermalConvergenceScenes.fresh(helper, probe, sample) && ThermalConvergenceScenes.step(helper, probe) == 1,
                    "Original recovery policy must restore fine quality and normal freshness without a test-selected model");
            helper.assertTrue(ThermalConvergenceScenes.fresh(helper, control.above(), sample)
                            && ThermalConvergenceScenes.step(helper, control.above()) == 1,
                    "After pressure ends the control room also recovers its normal quality");
        }).thenIdle(400).thenExecute(() -> {
            var state = RuntimeConvergenceObservation.snapshot();
            helper.assertTrue(ThermalConvergenceScenes.fresh(helper, probe, sample) && ThermalConvergenceScenes.step(helper, probe) == 1,
                    "Fine recovery must remain stable for 400 real ticks");
            helper.assertTrue(controlFailures[0] == 0 && state.orderingFailures() == 0
                    && state.energyObservations() > 0 && state.maximumImbalanceJ() < 1e-6,
                    "Preserve source ordering, single consumption and the normal room: " + state + ",first=" + firstFailure[0]);
            helper.assertTrue(state.acceptedAheadOfBinding() > 0 && state.maximumOwnedWindows() <= 2,
                    "Source acceptance returns producer slots before physical binding, with bounded retained windows");
            FHMain.LOGGER.info("THERMAL_SINGLE_REGION_RESULT,gameTick={},elapsedTicks={},maxControlAge={},saves={},stats={},peakRootBytes={},maximumUnavailableRun={}",
                    level.getGameTime(), level.getGameTime() - monitoring[0], maximumControlAge[0], saves[0], state,
                    MinecraftThermalInput.thermalBudgetPeakBytes(), maximumUnavailableRun[0]);
            var completedResources = ContinuousAirProductionTiming.resources();
            FHMain.LOGGER.info("THERMAL_SINGLE_REGION_COST,workerCpuNs={},workerAllocatedBytes={},mainCpuNs={},mainAllocatedBytes={},gcCount={},gcMillis={},heapBytes={}",
                    completedResources[0] - resources[0][0], completedResources[1] - resources[0][1],
                    completedResources[2] - resources[0][2], completedResources[3] - resources[0][3],
                    completedResources[4] - resources[0][4], completedResources[5] - resources[0][5], completedResources[6]);
            phase[0] = 4;
            ContinuousAirProductionGameTests.useItem(helper, control, Items.IRON_SHOVEL);
            ContinuousAirProductionGameTests.useItem(helper, retained, Items.IRON_SHOVEL);
            RuntimeConvergenceObservation.stop(); MinecraftThermalInput.closeActiveLevel(level);
        }).thenWaitUntil(() -> helper.assertTrue(MinecraftThermalInput.thermalBudgetUsedBytes() == 0, "All source, task, candidate and query ownership must release"))
        .thenExecute(() -> {
            memoryTarget.set(initialMemoryTarget);
            ThermalConvergenceScenes.force(helper, origin, 64, false);
            ThermalConvergenceScenes.force(helper, controlOrigin, 16, false);
        }).thenSucceed();
    }
}
