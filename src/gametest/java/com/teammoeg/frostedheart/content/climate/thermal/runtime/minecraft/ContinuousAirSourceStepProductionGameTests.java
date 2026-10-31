/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.solver.SourceStepProductionObservation;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_source_step")
@PrefixGameTestTemplate(false)
public final class ContinuousAirSourceStepProductionGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 1800)
    public static void actualSourceCutCancelsReclaimsAndPublishesOnce(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos fire = ContinuousAirProductionGameTests.room(helper, helper.absolutePos(BlockPos.ZERO));
        ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL);
        var sample = new ThermalEnvironmentSample();
        helper.startSequence().thenIdle(160).thenWaitUntil(() -> {
            MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, fire.above(), WorldTemperature.naturalAir(level, fire.above()), sample);
            helper.assertTrue(ContinuousAirProductionTiming.sourceField(sample), "Actual detailed room field must exist before cancellation");
        }).thenExecute(() -> SourceStepProductionObservation.arm(fire)).thenWaitUntil(() -> {
            helper.assertTrue(SourceStepProductionObservation.failure() == null, SourceStepProductionObservation.summary());
            helper.assertTrue(SourceStepProductionObservation.completed(), "A real source interval must cancel, reclaim and publish");
            MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, fire.above(), WorldTemperature.naturalAir(level, fire.above()), sample);
            helper.assertTrue(ContinuousAirProductionTiming.sourceField(sample)
                            && MinecraftThermalInput.describeAirRead(level, fire.above()).startsWith("geometry_current"),
                    "The accepted replacement must provide current detailed Air");
        }).thenExecute(() -> {
            FHMain.LOGGER.info("THERMAL_SOURCE_STEP_PRODUCTION,{}", SourceStepProductionObservation.summary());
            SourceStepProductionObservation.end();
            ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
        }).thenIdle(60).thenWaitUntil(() -> {
            MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, fire.above(), WorldTemperature.naturalAir(level, fire.above()), sample);
            helper.assertTrue(ContinuousAirProductionTiming.sourceField(sample),
                    "Ordinary cooling continues after the accepted replacement: available=" + sample.airAvailable()
                            + ",tick=" + sample.airSampleTick() + ",basis=" + sample.airBasisTerms()
                            + ",temperature=" + sample.airTemperatureC() + ",read=" + MinecraftThermalInput.describeAirRead(level, fire.above())
                            + ",worker=" + MinecraftThermalInput.describeWorkerProgress(level));
        }).thenSucceed();
    }
}
