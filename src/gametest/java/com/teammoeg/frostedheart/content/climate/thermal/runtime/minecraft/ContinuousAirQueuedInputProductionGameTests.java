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
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_queued_input")
@PrefixGameTestTemplate(false)
public final class ContinuousAirQueuedInputProductionGameTests {
    @GameTest(template = "phase0a_empty", timeoutTicks = 2400)
    public static void realChangesQueueWithoutAcknowledgingUnfinishedHeat(GameTestHelper helper) throws java.io.IOException {
        ContinuousAirPrecisionProductionGameTests.restoreReferenceClimate(helper, true);
        QueuedInputObservation.arm();
        var level = helper.getLevel();
        BlockPos small = ContinuousAirProductionGameTests.room(helper, helper.absolutePos(BlockPos.ZERO));
        ContinuousAirProductionGameTests.useItem(helper, small, Items.FLINT_AND_STEEL);
        BlockPos remote = helper.absolutePos(new BlockPos(517, 0, 7));
        for (int dx = -1; dx <= 4; dx++) for (int dz = -1; dz <= 2; dz++)
            level.setChunkForced((remote.getX() >> 4) + dx, (remote.getZ() >> 4) + dz, true);
        var base = ContinuousAirLargeBaseProductionGameTests.buildBase(helper, remote);
        var chunks = new java.util.HashSet<net.minecraft.world.level.chunk.LevelChunk>();
        for (var fire : base.fires()) chunks.add(level.getChunkAt(fire));
        for (var chunk : chunks) MinecraftThermalInput.onChunkLoad(level, chunk);
        // Exercise a real source removal while the initial remote construction is
        // unfinished, immediately before normal page/source capture seals a cut.
        QueuedInputObservation.requestCaptureAction(level, level.getGameTime() + 200, () -> {
            helper.assertTrue(level.getBlockState(small).getValue(CampfireBlock.LIT), "The actual cold-build probe starts lit");
            ContinuousAirProductionGameTests.useItem(helper, small, Items.IRON_SHOVEL);
            helper.assertTrue(!level.getBlockState(small).getValue(CampfireBlock.LIT), "Actual source removal must extinguish the campfire");
            FHMain.LOGGER.info("THERMAL_QUEUED_SOURCE_ACTION,phase=cold_capture,gameTick={},requestedLit=false,alreadyCurrent=false", level.getGameTime());
        });
        helper.runAfterDelay(220, () -> {
            if (!level.getBlockState(small).getValue(CampfireBlock.LIT))
                ContinuousAirProductionGameTests.useItem(helper, small, Items.FLINT_AND_STEEL);
        });
        long[] geometryTicks = new long[8];
        for (int cycle = 0; cycle < 8; cycle++) {
            int iteration = cycle;
            helper.runAfterDelay(401 + cycle * 80, () -> {
                boolean close = (iteration & 1) == 0;
                for (var fire : base.fires()) for (int dy = 1; dy <= 4; dy++)
                    level.setBlockAndUpdate(fire.west().above(dy), close ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
                geometryTicks[iteration] = level.getGameTime();
            });
            // Observe the actual structural input before changing the real source.
            // Fixed delays can put both world actions in the initiating input.
            helper.runAfterDelay(421 + cycle * 80, () -> {
                boolean close = (iteration & 1) == 0;
                changeSourceDuringStructure(helper, small, close, geometryTicks[iteration], geometryTicks[iteration] + 60);
            });
        }
        helper.runAfterDelay(1100, () -> {
            if (!level.getBlockState(small).getValue(CampfireBlock.LIT))
                ContinuousAirProductionGameTests.useItem(helper, small, Items.FLINT_AND_STEEL);
        });
        for (int elapsed = 200; elapsed <= 1000; elapsed += 200) {
            int observedTick = elapsed;
            helper.runAfterDelay(elapsed, () -> FHMain.LOGGER.info("THERMAL_QUEUED_INPUT_PROGRESS,elapsed={},{}",
                    observedTick, QueuedInputObservation.summary()));
        }
        var sample = new ThermalEnvironmentSample();
        helper.startSequence().thenIdle(1200).thenWaitUntil(() -> {
            helper.assertTrue(QueuedInputObservation.failure() == null, QueuedInputObservation.summary());
            helper.assertTrue(QueuedInputObservation.observedAckDecoupling(), "A naturally queued input must start before the earlier main ACK: " + QueuedInputObservation.summary());
            helper.assertTrue(QueuedInputObservation.observedYield(), "Real coordinator work must yield without completing the cut: " + QueuedInputObservation.summary());
            helper.assertTrue(QueuedInputObservation.observedGeneralWhileRunning() && QueuedInputObservation.acknowledged() > 0,
                    "Real non-steady input must be admitted while an earlier batch actually runs: " + QueuedInputObservation.summary());
            helper.assertTrue(QueuedInputObservation.payloadReleases() > 0,
                    "Actual Page intake must retire source-window payloads independently of transport receipt: " + QueuedInputObservation.summary());
            helper.assertTrue(level.getBlockState(small.above()).isAir(),
                    "The actual Air probe above the campfire was replaced: " + level.getBlockState(small.above()));
            MinecraftThermalInput.gameplayPlacedReservoirEnvironment(level, small.above(), WorldTemperature.naturalAir(level, small.above()), sample);
            helper.assertTrue(ContinuousAirProductionTiming.sourceField(sample)
                            && MinecraftThermalInput.describeAirRead(level, small.above()).startsWith("geometry_current"),
                    "The actual relit room must recover current fine Air");
            helper.assertTrue(MinecraftThermalInput.enabledPhysicalSourceCount(level) == 129, "All actual sources must remain registered");
        }).thenExecute(() -> {
            FHMain.LOGGER.info("THERMAL_QUEUED_INPUT_PRODUCTION,{}", QueuedInputObservation.summary());
            MinecraftThermalInput.closeActiveLevel(level);
        }).thenWaitUntil(() -> helper.assertTrue(MinecraftThermalInput.thermalBudgetUsedBytes() == 0,
                "Closing the real engine must release all accepted input reservations"))
        .thenExecute(() -> {
            helper.assertTrue(QueuedInputObservation.failure() == null, QueuedInputObservation.summary());
            QueuedInputObservation.end();
            for (int dx = -1; dx <= 4; dx++) for (int dz = -1; dz <= 2; dz++)
                level.setChunkForced((remote.getX() >> 4) + dx, (remote.getZ() >> 4) + dz, false);
        }).thenSucceed();
    }

    private static void changeSourceDuringStructure(GameTestHelper helper, BlockPos fire, boolean extinguish,
            long geometryTick, long deadline) {
        if (helper.getLevel().getGameTime() > deadline) return;
        // A preceding construction can finish before its planned extinguish.
        // Flint on an already lit campfire places FIRE at the Air probe instead.
        boolean lit = helper.getLevel().getBlockState(fire).getValue(CampfireBlock.LIT);
        if (lit == !extinguish) {
            FHMain.LOGGER.info("THERMAL_QUEUED_SOURCE_ACTION,gameTick={},requestedLit={},alreadyCurrent=true",
                    helper.getLevel().getGameTime(), !extinguish);
            return;
        }
        if (QueuedInputObservation.processingStructureSince(geometryTick)) {
            ContinuousAirProductionGameTests.useItem(helper, fire, extinguish ? Items.IRON_SHOVEL : Items.FLINT_AND_STEEL);
            helper.assertTrue(helper.getLevel().getBlockState(fire).getValue(CampfireBlock.LIT) == !extinguish,
                    "Actual campfire interaction must change its requested source state");
            FHMain.LOGGER.info("THERMAL_QUEUED_SOURCE_ACTION,gameTick={},requestedLit={},alreadyCurrent=false",
                    helper.getLevel().getGameTime(), !extinguish);
        } else helper.runAfterDelay(1, () -> changeSourceDuringStructure(helper, fire, extinguish, geometryTick, deadline));
    }
}
