/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.solver.RegionalGridSwitchObservation;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Real rooms, source actions and construction; quality is the only explicit numerical input. */
@GameTestHolder("frostedheart_regional_grid_switch")
@PrefixGameTestTemplate(false)
public final class ContinuousAirRegionalGridSwitchProductionGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 2400)
    public static void oneRegionCoarsensAndRecoversFromCurrentFieldWhileAnotherStaysFine(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(BlockPos.ZERO), farOrigin = origin.offset(256, 0, 0);
        for (int z = farOrigin.getZ() >> 4; z <= (farOrigin.getZ() + 16) >> 4; z++)
            for (int x = farOrigin.getX() >> 4; x <= (farOrigin.getX() + 16) >> 4; x++) level.setChunkForced(x, z, true);
        BlockPos fire = ContinuousAirProductionGameTests.room(helper, origin);
        BlockPos control = ContinuousAirProductionGameTests.room(helper, farOrigin);
        // Keep this real region active while the selected source is extinguished;
        // a cold source-free Page is allowed to retire before a quality request.
        BlockPos maintainingFire = fire.west(4);
        ThermalConvergenceScenes.fire(helper, maintainingFire);
        ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL);
        ContinuousAirProductionGameTests.useItem(helper, control, Items.FLINT_AND_STEEL);
        var sample = new ThermalEnvironmentSample();
        var other = new ThermalEnvironmentSample();
        com.teammoeg.frostedheart.content.climate.thermal.solver.RuntimeConvergenceObservation.arm(
                control.above(), fire.above(), new BlockPos[] {fire, maintainingFire});
        ContinuousAirProductionTiming.eachTick(helper, 2380, elapsed -> {
            if (elapsed % 200 != 0) return;
            ThermalConvergenceScenes.fuel(helper, fire, 1);
            ThermalConvergenceScenes.fuel(helper, maintainingFire, 1);
            ThermalConvergenceScenes.fuel(helper, control, 1);
            read(helper, fire.above(), sample);
            FHMain.LOGGER.info("THERMAL_REGIONAL_GRID_READ,elapsed={},available={},sample={},span={},read={},state={}",
                    elapsed, sample.airAvailable(), sample.airSampleTick(), span(helper, fire.above()),
                    MinecraftThermalInput.describeAirRead(level, fire.above()),
                    com.teammoeg.frostedheart.content.climate.thermal.solver.RuntimeConvergenceObservation.snapshot());
        });
        double[] temperatures = new double[3];
        var sequence = helper.startSequence().thenIdle(400).thenWaitUntil(() -> {
            read(helper, fire.above(), sample); read(helper, control.above(), other);
            helper.assertTrue(ContinuousAirProductionTiming.sourceField(sample) && ContinuousAirProductionTiming.sourceField(other),
                    "Both actual fires must establish current detailed fields");
            helper.assertTrue(span(helper, fire.above()) == 1 && span(helper, control.above()) == 1, "Both rooms start on the fine grid");
        }).thenExecute(() -> {
            RegionalGridSwitchObservation.arm(fire, control);
            RegionalGridSwitchObservation.request(4);
        }).thenWaitUntil(() -> {
            checkObservation(helper, 1);
            helper.assertTrue(span(helper, fire.above()) == 0 && span(helper, control.above()) == 1,
                    "Only the explicitly selected region uses the whole coarse grid");
        }).thenExecute(() -> {
            read(helper, fire.above(), sample); temperatures[0] = sample.airTemperatureC();
            ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
            for (int y = 0; y < 3; y++) level.setBlockAndUpdate(fire.east(3).above(y), Blocks.STONE.defaultBlockState());
        }).thenIdle(100).thenWaitUntil(() -> {
            read(helper, fire.above(), sample);
            helper.assertTrue(ContinuousAirProductionTiming.sourceField(sample) && span(helper, fire.above()) == 0
                    && MinecraftThermalInput.describeAirRead(level, fire.above()).startsWith("geometry_current"),
                    "The coarse region handles actual construction and cooling");
        }).thenExecute(() -> {
            for (int y = 0; y < 3; y++) level.setBlockAndUpdate(fire.east(3).above(y), Blocks.AIR.defaultBlockState());
        }).thenIdle(100).thenWaitUntil(() -> {
            read(helper, fire.above(), sample); read(helper, control.above(), other);
            helper.assertTrue(ContinuousAirProductionTiming.sourceField(sample) && ContinuousAirProductionTiming.sourceField(other)
                    && span(helper, fire.above()) == 0 && span(helper, control.above()) == 1,
                    "Cooling coarse field and independent fine field remain current after removing the pillar");
        }).thenExecute(() -> {
            temperatures[1] = sample.airTemperatureC();
            RegionalGridSwitchObservation.request(1);
        }).thenWaitUntil(() -> {
            checkObservation(helper, 2);
            helper.assertTrue(span(helper, fire.above()) == 1 && span(helper, control.above()) == 1, "Both actual regions now run fine");
        }).thenExecute(() -> {
            read(helper, fire.above(), sample); temperatures[2] = sample.airTemperatureC();
            ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL);
        }).thenIdle(300).thenWaitUntil(() -> {
            read(helper, fire.above(), sample); read(helper, fire.above().east(2), other);
            helper.assertTrue(ContinuousAirProductionTiming.sourceField(sample) && ContinuousAirProductionTiming.sourceField(other)
                    && sample.airSampleTick() == other.airSampleTick() && sample.airTemperatureC() > other.airTemperatureC() + .01,
                    "Actual relighting forms fine near/far temperature detail after recovery");
        }).thenExecute(() -> {
            FHMain.LOGGER.info("THERMAL_REGIONAL_GRID_PRODUCTION,coarseEntryNearC={},coarseCurrentNearC={},recoveredNearC={},fineNearC={},fineFarC={},{}",
                    temperatures[0], temperatures[1], temperatures[2], sample.airTemperatureC(), other.airTemperatureC(), RegionalGridSwitchObservation.summary());
            RegionalGridSwitchObservation.end();
            com.teammoeg.frostedheart.content.climate.thermal.solver.RuntimeConvergenceObservation.stop();
            ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
            ContinuousAirProductionGameTests.useItem(helper, maintainingFire, Items.IRON_SHOVEL);
            ContinuousAirProductionGameTests.useItem(helper, control, Items.IRON_SHOVEL);
            for (int z = farOrigin.getZ() >> 4; z <= (farOrigin.getZ() + 16) >> 4; z++)
                for (int x = farOrigin.getX() >> 4; x <= (farOrigin.getX() + 16) >> 4; x++) level.setChunkForced(x, z, false);
        });
        sequence.thenSucceed();
    }
    private static void read(GameTestHelper helper, BlockPos point, ThermalEnvironmentSample sample) {
        MinecraftThermalInput.gameplayPlacedReservoirEnvironment(helper.getLevel(), point,
                WorldTemperature.naturalAir(helper.getLevel(), point), sample);
    }
    private static int span(GameTestHelper helper, BlockPos point) {
        var publication = MinecraftThermalInput.queryPublicationForObservation(helper.getLevel());
        var cursor = new QueryPublication.ReadCursor();
        try {
            if (publication == null || !publication.beginRead(cursor)) return -1;
            return cursor.airAggregationSpanAt(point.getX() + .5, point.getY() + .5, point.getZ() + .5);
        } finally { cursor.clear(); }
    }
    private static void checkObservation(GameTestHelper helper, int completions) {
        helper.assertTrue(RegionalGridSwitchObservation.failure() == null, RegionalGridSwitchObservation.summary());
        helper.assertTrue(RegionalGridSwitchObservation.completed() >= completions, "Waiting for a real regional grid installation");
    }
}
