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

/** Actual chunk save/reload followed by a new doorway must preserve and extend the temperature field. */
@GameTestHolder("frostedheart_restored_support_production")
@PrefixGameTestTemplate(false)
public final class ContinuousAirRestoredSupportProductionGameTests {
    private ContinuousAirRestoredSupportProductionGameTests() {}

    @GameTest(template = "phase0a_empty", timeoutTicks = 3800)
    public static void restoredSourceHeatsNewDoorwayWithoutResettingHistory(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = new BlockPos(1024, 80, 1024);
        int chunkX = origin.getX() >> 4, chunkZ = origin.getZ() >> 4;
        level.setChunkForced(chunkX, chunkZ, true);
        BlockPos fire = ContinuousAirProductionGameTests.room(helper, origin);
        for (int y = 1; y < 8; y++) for (int z = 1; z < 16; z++)
            level.setBlockAndUpdate(origin.offset(10, y, z), Blocks.STONE.defaultBlockState());
        BlockPos near = fire.above(), far = near.west(2), beyond = origin.offset(12, 2, 8);
        ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL);
        var nearAir = new ThermalEnvironmentSample();
        var farAir = new ThermalEnvironmentSample();
        var beyondAir = new ThermalEnvironmentSample();
        double[] before = new double[2], sealedBeyond = new double[1];
        int[] stage = {0};
        for (int delay = 100; delay < 3800; delay += 100) {
            helper.runAfterDelay(delay, () -> FHMain.LOGGER.info(
                    "THERMAL_RESTORED_SUPPORT,stage={},tick={},near={},far={},nearTick={},nearBasis={},beyond={},beyondTick={},read={},worker={}",
                    stage[0], level.getGameTime(), nearAir.airTemperatureC(), farAir.airTemperatureC(),
                    nearAir.airSampleTick(), nearAir.airBasisTerms(), beyondAir.airTemperatureC(), beyondAir.airSampleTick(),
                    MinecraftThermalInput.describeAirRead(level, near),
                    MinecraftThermalInput.describeWorkerProgress(level)));
        }
        helper.startSequence().thenIdle(400).thenWaitUntil(() -> {
            sample(helper, near, nearAir);
            sample(helper, far, farAir);
            helper.assertTrue(fresh(helper, nearAir),
                    "Actual source must publish fresh Air before saving: " + MinecraftThermalInput.describeAirRead(level, near));
            helper.assertTrue(nearAir.airTemperatureC() > farAir.airTemperatureC() + .01,
                    "Real source must establish spatial heat before saving");
        }).thenExecute(() -> {
            stage[0] = 1;
            ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
        }).thenIdle(60).thenWaitUntil(() -> {
            sample(helper, near, nearAir);
            sample(helper, far, farAir);
            helper.assertTrue(fresh(helper, nearAir),
                    "Extinguished residual heat must be published before saving");
        }).thenExecute(() -> {
            before[0] = nearAir.airTemperatureC();
            before[1] = farAir.airTemperatureC();
            level.getServer().saveEverything(true, true, true);
            level.setChunkForced(chunkX, chunkZ, false);
            stage[0] = 2;
        }).thenWaitUntil(() -> helper.assertTrue(level.getChunkSource().getChunkNow(chunkX, chunkZ) == null,
                "Saved real chunk must unload"))
                .thenExecute(() -> { level.setChunkForced(chunkX, chunkZ, true); stage[0] = 3; })
                .thenWaitUntil(() -> helper.assertTrue(level.getChunkSource().getChunkNow(chunkX, chunkZ) != null,
                        "Saved real chunk must reload"))
                .thenExecute(() -> {
                    sample(helper, near, nearAir);
                    sample(helper, far, farAir);
                    helper.assertTrue(Math.abs(nearAir.airTemperatureC() - before[0]) < .25
                                    && Math.abs(farAir.airTemperatureC() - before[1]) < .25,
                            "Actual reload must retain spatial residual heat");
                    helper.assertTrue(nearAir.airTemperatureC() > farAir.airTemperatureC() + .01,
                            "Reload must preserve the actual near/far gradient");
                    ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL);
                }).thenIdle(120).thenWaitUntil(() -> {
                    sample(helper, near, nearAir);
                    helper.assertTrue(fresh(helper, nearAir),
                            "Restored source must rejoin the live spatial solver");
                }).thenExecute(() -> {
                    sample(helper, beyond, beyondAir);
                    helper.assertTrue(Double.isFinite(beyondAir.airTemperatureC()),
                            "The sealed room must have a finite baseline before opening");
                    sealedBeyond[0] = beyondAir.airTemperatureC();
                    for (int y = 1; y <= 3; y++) for (int z = 7; z <= 9; z++)
                        level.setBlockAndUpdate(origin.offset(10, y, z), Blocks.AIR.defaultBlockState());
                    stage[0] = 4;
                }).thenIdle(400).thenWaitUntil(() -> {
                    sample(helper, near, nearAir);
                    sample(helper, beyond, beyondAir);
                    helper.assertTrue(fresh(helper, nearAir) && fresh(helper, beyondAir),
                            "Both rooms must publish fresh Air after opening the real doorway");
                    helper.assertTrue(MinecraftThermalInput.describeAirRead(level, beyond).startsWith("geometry_current"),
                            "The new room must use the committed doorway geometry");
                    helper.assertTrue(beyondAir.airTemperatureC() > sealedBeyond[0] + .005,
                            "The actual new branch must warm after opening");
                }).thenExecute(() -> {
                    FHMain.LOGGER.info("THERMAL_RESTORED_SUPPORT_PASS,beforeNear={},beforeFar={},openedNear={},openedBeyond={},sampleTick={}",
                            before[0], before[1], nearAir.airTemperatureC(), beyondAir.airTemperatureC(),
                            beyondAir.airSampleTick());
                    ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
                    stage[0] = 5;
                }).thenIdle(60).thenExecute(() -> level.setBlockAndUpdate(fire.above(), Blocks.STONE.defaultBlockState()))
                .thenIdle(100).thenWaitUntil(() -> {
                    sample(helper, beyond, beyondAir);
                    helper.assertTrue(fresh(helper, beyondAir)
                                    && Math.abs(beyondAir.airTemperatureC() - WorldTemperature.naturalAir(level, beyond)) > .005,
                            "Blocking the extinguished source's former Air outlet must preserve the remaining local heat history");
                    helper.assertTrue(MinecraftThermalInput.describeAirRead(level, beyond).startsWith("geometry_current"),
                            "Residual heat must use the committed outlet-blocking geometry");
                }).thenExecute(() -> {
                    FHMain.LOGGER.info("THERMAL_RESTORED_OUTLET_HISTORY_PASS,tick={},beyondC={},naturalC={},sampleTick={}",
                            level.getGameTime(), beyondAir.airTemperatureC(), WorldTemperature.naturalAir(level, beyond),
                            beyondAir.airSampleTick());
                    level.setChunkForced(chunkX, chunkZ, false);
                }).thenSucceed();
    }

    private static void sample(GameTestHelper helper, BlockPos pos, ThermalEnvironmentSample out) {
        MinecraftThermalInput.gameplayPlacedReservoirEnvironment(helper.getLevel(), pos,
                WorldTemperature.naturalAir(helper.getLevel(), pos), out);
    }

    private static boolean fresh(GameTestHelper helper, ThermalEnvironmentSample sample) {
        long age = helper.getLevel().getGameTime() - sample.airSampleTick();
        return sample.airAvailable() && Double.isFinite(sample.airTemperatureC()) && sample.airBasisTerms() >= 8
                && sample.airSampleTick() >= 0 && age >= 0 && age <= 40;
    }
}
