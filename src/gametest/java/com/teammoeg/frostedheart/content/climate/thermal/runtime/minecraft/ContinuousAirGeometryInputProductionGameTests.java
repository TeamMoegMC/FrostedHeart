/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Actual room construction and a real borrowed input; no thermal injection or scheduling delays. */
@GameTestHolder("frostedheart_geometry_input")
@PrefixGameTestTemplate(false)
public final class ContinuousAirGeometryInputProductionGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 1800)
    public static void retainedGeometrySurvivesActualPillarReplacement(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos fire = ContinuousAirProductionGameTests.room(helper, helper.absolutePos(BlockPos.ZERO));
        BlockPos pillar = fire.above().east();
        helper.assertTrue(level.getBlockState(pillar).isAir(), "The pillar must replace actual room Air");
        GeometryInputProductionObservation.begin(fire, pillar);
        ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL);
        var sample = new ThermalEnvironmentSample();
        helper.startSequence().thenIdle(160).thenWaitUntil(() -> {
            helper.assertTrue(GeometryInputProductionObservation.failure() == null, GeometryInputProductionObservation.summary());
            MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, fire.above(),
                    WorldTemperature.naturalAir(level, fire.above()), sample);
            helper.assertTrue(GeometryInputProductionObservation.borrowed() && ContinuousAirProductionTiming.sourceField(sample),
                    "An actual build input and its detailed field must exist before construction");
        }).thenExecute(() -> level.setBlockAndUpdate(pillar, Blocks.STONE.defaultBlockState()))
        .thenWaitUntil(() -> {
            helper.assertTrue(GeometryInputProductionObservation.failure() == null, GeometryInputProductionObservation.summary());
            helper.assertTrue(GeometryInputProductionObservation.completed(),
                    "Actual construction must replace the borrowed geometry and retire its node span");
            helper.assertTrue(GeometryInputProductionObservation.retirementPassed(),
                    "Old nodes must stay read-only until the actual reader releases them: " + GeometryInputProductionObservation.summary());
            MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, fire.above(),
                    WorldTemperature.naturalAir(level, fire.above()), sample);
            helper.assertTrue(ContinuousAirProductionTiming.sourceField(sample)
                            && MinecraftThermalInput.describeAirRead(level, fire.above()).startsWith("geometry_current"),
                    "The real source publishes current geometry while the obsolete input is released");
        }).thenExecute(() -> level.setBlockAndUpdate(pillar, Blocks.AIR.defaultBlockState()))
        .thenIdle(60).thenWaitUntil(() -> {
            MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, fire.above(),
                    WorldTemperature.naturalAir(level, fire.above()), sample);
            helper.assertTrue(ContinuousAirProductionTiming.sourceField(sample)
                            && MinecraftThermalInput.describeAirRead(level, fire.above()).startsWith("geometry_current"),
                    "Removing the actual pillar restores the ordinary current field");
        }).thenExecute(() -> {
            FHMain.LOGGER.info("THERMAL_GEOMETRY_INPUT_PRODUCTION,{}", GeometryInputProductionObservation.summary());
            GeometryInputProductionObservation.end();
            ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
        }).thenSucceed();
    }
}
