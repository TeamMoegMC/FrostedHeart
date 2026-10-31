/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ContinuousAirSolver;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Actual fueled-room warming and cooling, including small late cooling steps. */
@GameTestHolder("frostedheart_precision_room")
@PrefixGameTestTemplate(false)
public final class ContinuousAirPrecisionProductionGameTests {
    private ContinuousAirPrecisionProductionGameTests() {}

    @GameTest(template = "phase0a_empty", timeoutTicks = 2700)
    public static void realRoomContinuesCoolingWithOrdinaryStoppingPolicy(GameTestHelper helper) throws java.io.IOException {
        helper.assertTrue(true, "Precision trial requires continuous Air");
        var level = helper.getLevel();
        // A normal world rule holds the climate clock; the worker, fuel and game ticks still advance.
        level.setDayTime(6000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        restoreReferenceClimate(helper, false);
        BlockPos fire = ContinuousAirProductionGameTests.room(helper, helper.absolutePos(BlockPos.ZERO));
        long startedTick = level.getGameTime();
        BlockPos near = fire.above();
        ContinuousAirProductionTiming.observe(helper, "precision_room", new BlockPos[]{near, near.east(4)}, 2600);
        FHMain.LOGGER.info("THERMAL_PRECISION_START,trial={},startedTick={},fire={},dayTime={},seed={}",
                ContinuousAirSolver.TRANSPORT_TOLERANCE_C, startedTick, fire, level.getDayTime(), level.getSeed());
        var nearSample = new ThermalEnvironmentSample();
        var farSample = new ThermalEnvironmentSample();
        double[] peakRise = {0}, retainedRise = {Double.NaN}, previousRise = {Double.NaN};
        int[] coolingSamples = {0}, downwardSamples = {0}, stalledSamples = {0};
        for (int delay = 1; delay <= 2600; delay++) {
            int elapsed = delay;
            helper.runAfterDelay(delay, () -> {
                if (elapsed == 401) {
                    ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL);
                    FHMain.LOGGER.info("THERMAL_PRECISION_ACTION,elapsedTicks={},worldTick={},action=ignite", elapsed, level.getGameTime());
                }
                if (elapsed == 1201) {
                    ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
                    helper.assertTrue(!level.getBlockState(fire).getValue(CampfireBlock.LIT), "Actual shovel extinguishes the fire");
                    FHMain.LOGGER.info("THERMAL_PRECISION_ACTION,elapsedTicks={},worldTick={},action=extinguish", elapsed, level.getGameTime());
                }
                if (elapsed % 20 != 0) return;
                ContinuousAirProductionGameTests.sampleAt(level, fire.getX() + .5, fire.getY() + 1.5, fire.getZ() + .5, nearSample);
                ContinuousAirProductionGameTests.sampleAt(level, fire.getX() + 4.5, fire.getY() + 1.5, fire.getZ() + .5, farSample);
                double rise = nearSample.airTemperatureC() - farSample.airTemperatureC();
                if (elapsed > 600 && elapsed <= 1200) peakRise[0] = Math.max(peakRise[0], rise);
                if (elapsed == 1280) retainedRise[0] = rise;
                if (elapsed >= 1300) {
                    coolingSamples[0]++;
                    if (rise < previousRise[0] - 1e-7) downwardSamples[0]++;
                    if (rise == previousRise[0]) stalledSamples[0]++;
                }
                previousRise[0] = rise;
                FHMain.LOGGER.info("THERMAL_PRECISION_TRACE,toleranceC={},elapsedTicks={},worldTick={},sampleTick={},farSampleTick={},nearC={},farC={},riseC={},floorC={},naturalC={},wind={},climateSec={},basisTerms={},geometry={}",
                        ContinuousAirSolver.TRANSPORT_TOLERANCE_C, elapsed, level.getGameTime(), nearSample.airSampleTick(), farSample.airSampleTick(),
                        nearSample.airTemperatureC(), farSample.airTemperatureC(), rise, WorldTemperature.material(level, fire.below()),
                        WorldTemperature.naturalAir(level, near), WorldClimate.getWind(level), WorldClimate.getSec(level),
                        nearSample.airBasisTerms(), MinecraftThermalInput.describeAirRead(level, near).split(",", 2)[0]);
                if (elapsed >= 800) {
                    helper.assertTrue(nearSample.airAvailable() && nearSample.airSampleTick() == farSample.airSampleTick()
                            && level.getGameTime() - nearSample.airSampleTick() <= 40, "Actual room samples remain current and coherent");
                }
                if (elapsed != 2600) return;
                helper.assertTrue(peakRise[0] > .05, "Ordinary source power must enter the actual Air field; peak=" + peakRise[0]);
                helper.assertTrue(retainedRise[0] > .005, "Actual room retains heat after extinguishing; rise=" + retainedRise[0]);
                helper.assertTrue(downwardSamples[0] >= 5 && retainedRise[0] - rise > .002,
                        "Small cooling must continue rather than repeatedly accept the old temperature; retained="
                                + retainedRise[0] + ",final=" + rise + ",downwardSamples=" + downwardSamples[0]);
                FHMain.LOGGER.info("THERMAL_PRECISION_RESULT,trial={},peakRiseC={},retainedRiseC={},finalRiseC={},coolingSamples={},downwardSamples={},stalledSamples={},worker={}",
                        ContinuousAirSolver.TRANSPORT_TOLERANCE_C, peakRise[0], retainedRise[0], rise, coolingSamples[0], downwardSamples[0], stalledSamples[0],
                        MinecraftThermalInput.describeWorkerProgress(level));
                helper.succeed();
            });
        }
    }

    static void restoreReferenceClimate(GameTestHelper helper, boolean keepCurrentClock) throws java.io.IOException {
        String climateSnapshot = System.getenv("FH_PRECISION_CLIMATE_SNAPSHOT");
        if (climateSnapshot == null || climateSnapshot.isBlank()) return;
        var level = helper.getLevel();
        var data = NbtIo.readCompressed(new java.io.File(climateSnapshot)).getCompound("data");
        helper.assertTrue(data.contains("frostedheart:climate_data", Tag.TAG_COMPOUND),
                "Reference must contain climate saved by the actual Forge world");
        var saved = data.getCompound("frostedheart:climate_data");
        var climate = WorldClimate.get(level);
        if (keepCurrentClock) {
            // Replay actual saved hourly weather while this new world's ordinary clock advances.
            saved.putLong("secs", climate.getSec());
            saved.putLong("last", climate.getClockDayTime());
        }
        climate.load(saved, false);
        FHMain.LOGGER.info("THERMAL_PRECISION_CLIMATE_RESTORE,wind={},climateSec={},dayTime={},keepCurrentClock={}",
                WorldClimate.getWind(level), WorldClimate.getSec(level), level.getDayTime(), keepCurrentClock);
    }
}
