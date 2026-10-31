/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.solver.RegionalAcceptanceObservation;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_regional_acceptance")
@PrefixGameTestTemplate(false)
public final class ContinuousAirRegionalAcceptanceProductionGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 2400)
    public static void actualSmallRegionCommitsBeforeDenseRegionFinishes(GameTestHelper helper) throws java.io.IOException {
        ContinuousAirPrecisionProductionGameTests.restoreReferenceClimate(helper, true);
        var level = helper.getLevel();
        BlockPos small = ContinuousAirProductionGameTests.room(helper, helper.absolutePos(BlockPos.ZERO));
        ContinuousAirProductionGameTests.useItem(helper, small, Items.FLINT_AND_STEEL);
        BlockPos remote = helper.absolutePos(new BlockPos(517, 0, 7));
        for (int dx = -1; dx <= 4; dx++) for (int dz = -1; dz <= 2; dz++)
            level.setChunkForced((remote.getX() >> 4) + dx, (remote.getZ() >> 4) + dz, true);
        var built = ContinuousAirLargeBaseProductionGameTests.buildBase(helper, remote);
        var chunks = new java.util.HashSet<net.minecraft.world.level.chunk.LevelChunk>();
        for (BlockPos fire : built.fires()) chunks.add(level.getChunkAt(fire));
        for (var chunk : chunks) MinecraftThermalInput.onChunkLoad(level, chunk);
        var first = new ThermalEnvironmentSample();
        var second = new ThermalEnvironmentSample();
        helper.startSequence().thenIdle(400).thenWaitUntil(() -> {
            MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, small.above(), WorldTemperature.naturalAir(level, small.above()), first);
            BlockPos large = built.probes()[0];
            MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, large, WorldTemperature.naturalAir(level, large), second);
            helper.assertTrue(ContinuousAirProductionTiming.sourceField(first) && ContinuousAirProductionTiming.sourceField(second),
                    "Both actual fine regions must exist before observing their acceptance");
            helper.assertTrue(MinecraftThermalInput.enabledPhysicalSourceCount(level) == 129, "Use one real fire and the full 128-source base");
        }).thenExecute(() -> RegionalAcceptanceObservation.arm(small, built.probes()[0])).thenWaitUntil(() -> {
            helper.assertTrue(RegionalAcceptanceObservation.failure() == null, RegionalAcceptanceObservation.summary());
            helper.assertTrue(RegionalAcceptanceObservation.completed(), "A real small region must confirm while the dense region still runs");
        }).thenIdle(60).thenExecute(() -> {
            FHMain.LOGGER.info("THERMAL_REGIONAL_ACCEPTANCE_PRODUCTION,{}", RegionalAcceptanceObservation.summary());
            RegionalAcceptanceObservation.end();
            MinecraftThermalInput.closeActiveLevel(level);
        }).thenWaitUntil(() -> helper.assertTrue(MinecraftThermalInput.thermalBudgetUsedBytes() == 0,
                "The real close must settle helpers and release the completion admission"))
        .thenExecute(() -> {
            for (int dx = -1; dx <= 4; dx++) for (int dz = -1; dz <= 2; dz++)
                level.setChunkForced((remote.getX() >> 4) + dx, (remote.getZ() >> 4) + dz, false);
        }).thenSucceed();
    }
}
