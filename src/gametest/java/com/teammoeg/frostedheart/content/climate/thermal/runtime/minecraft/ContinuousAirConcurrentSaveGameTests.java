/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_concurrent_save")
@PrefixGameTestTemplate(false)
public final class ContinuousAirConcurrentSaveGameTests {
    private static SaveDuringSolveObservation activeObservation;
    private static Runnable restoreWorld;
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void savingIncludesUncommittedClaimsAndExcludesAcceptedCleanup(GameTestHelper helper) {
        var store = new com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatStore();
        var first = new com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput.Target(
                BlockPos.asLong(1, 64, 1), (byte) 0, -1, 0, 0, 0, 0, 0);
        var second = new com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput.Target(
                BlockPos.asLong(2, 64, 1), (byte) 0, -1, 0, 0, 0, 0, 0);
        store.receive(1, first, 0, 20, 100);
        store.receive(1, second, 0, 20, -30);
        var claim = store.claim(1, 0, 10);
        helper.assertTrue(!claim.prepare(0), "The claim is still being prepared after its first target");
        assertPending(helper, store, 100, -30);
        while (!claim.prepare(0)) { }
        assertPending(helper, store, 100, -30);
        claim.accepted();
        assertPending(helper, store, 50, -15);
        store.advanceCleanup(8);
        assertPending(helper, store, 50, -15);
        var cancelled = store.claim(1, 10, 20);
        while (!cancelled.prepare(0)) { }
        cancelled.close();
        assertPending(helper, store, 50, -15);
        store.advanceCleanup(8);
        assertPending(helper, store, 50, -15);
        store.clear(); helper.succeed();
    }
    private static void assertPending(GameTestHelper helper,
            com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatStore store,
            double positive, double negative) {
        double[] total = {0, 0};
        store.collectChunk(ChunkPos.asLong(0, 0), (target, from, to, joules) -> total[joules >= 0 ? 0 : 1] += joules);
        helper.assertTrue(total[0] == positive && total[1] == negative,
                "Saving must retain each uncommitted sign once across claim preparation, acceptance and cancellation");
    }

    @GameTest(template = "phase0a_empty", batch = "concurrent_save_actual_world", timeoutTicks = 1800)
    public static void ordinarySaveSerializesClaimedHeatWithoutCancellingTheFixedTrial(GameTestHelper helper) {
        var level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        BlockPos firstOrigin = helper.absolutePos(new BlockPos(221184, 192, 221184)), secondOrigin = firstOrigin.east(160);
        ThermalConvergenceScenes.force(helper, firstOrigin, 32, true);
        ThermalConvergenceScenes.force(helper, secondOrigin, 32, true);
        var firstHall = new ThermalConvergenceScenes.HallBuild(helper, firstOrigin, 8, 8, 4);
        var secondHall = new ThermalConvergenceScenes.HallBuild(helper, secondOrigin, 8, 8, 4);
        BlockPos first = firstHall.fires[0], second = secondHall.fires[0];
        restoreWorld = () -> {
            for (var fire : firstHall.fires) if (level.getBlockState(fire).is(net.minecraft.world.level.block.Blocks.CAMPFIRE))
                ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
            for (var fire : secondHall.fires) if (level.getBlockState(fire).is(net.minecraft.world.level.block.Blocks.CAMPFIRE))
                ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
            ThermalConvergenceScenes.force(helper, firstOrigin, 32, false);
            ThermalConvergenceScenes.force(helper, secondOrigin, 32, false);
        };
        SaveDuringSolveObservation[] observation = {null};
        helper.startSequence().thenWaitUntil(() -> {
            boolean firstReady = firstHall.advance(), secondReady = secondHall.advance();
            helper.assertTrue(firstReady && secondReady, "Build both actual halls with their 128 ordinary fuelled sources");
        }).thenIdle(400).thenWaitUntil(() -> {
            helper.assertTrue(MinecraftThermalInput.describeAirRead(level, first.above()).startsWith("geometry_current")
                            && MinecraftThermalInput.describeAirRead(level, second.above()).startsWith("geometry_current"),
                    "Both actual rooms must be ready before overlapping a save with numerical work");
        }).thenExecute(() -> activeObservation = observation[0] = new SaveDuringSolveObservation(MinecraftThermalInput.queryPublicationForObservation(level), first.above(), second.above()))
                .thenWaitUntil(() -> helper.assertTrue(observation[0].blocked(), "Observe a private fixed trial while the coordinator still owns RUNNING; "
                        + observation[0].summary() + "; " + MinecraftThermalInput.describeWorkerProgress(level)))
                .thenExecute(() -> {
                    try {
                        long started = System.nanoTime();
                        var position = new ChunkPos(observation[0].chunk());
                        var saving = level.getChunk(position.x, position.z);
                        helper.assertTrue(MinecraftThermalInput.runMaintenanceSave(level, saving, () -> true),
                                "Ordinary capture must complete without owning the coordinator turn");
                        helper.assertTrue(observation[0].capturedWhileRunning() && observation[0].failure() == null,
                                "Capture must complete while the private trial owns RUNNING: " + observation[0].summary());
                        saving.setUnsaved(true);
                        level.getServer().saveEverything(true, true, true);
                        helper.assertTrue(observation[0].savedWhileRunning() && observation[0].failure() == null,
                                "Save must serialize pending heat while the same fixed trial remains unfinished: " + observation[0].summary());
                        level.getChunkSource().chunkMap.flushWorker();
                        var disk = level.getChunkSource().chunkMap.read(position).join().orElseThrow();
                        var chunk = level.getChunk(position.x, position.z);
                        observation[0].verifyDisk(disk, chunk.getSectionYFromSectionIndex(0), chunk.getSections().length);
                        observation[0].saveDuration(System.nanoTime() - started);
                        helper.assertTrue(observation[0].failure() == null, observation[0].summary());
                    } finally { observation[0].resume(); }
                }).thenWaitUntil(() -> {
                    helper.assertTrue(observation[0].failure() == null, observation[0].summary());
                    helper.assertTrue(observation[0].completed(), "The original fixed trial must resume and publish once");
                }).thenIdle(40).thenExecute(() -> {
                    FHMain.LOGGER.info("THERMAL_CONCURRENT_SAVE,scope=real_fixed_trial,{}", observation[0].summary());
                }).thenSucceed();
    }

    @AfterBatch(batch = "concurrent_save_actual_world")
    public static void releaseObservationAndWorld(net.minecraft.server.level.ServerLevel level) {
        try {
            if (activeObservation != null) activeObservation.close();
            if (restoreWorld != null) restoreWorld.run();
        } finally {
            activeObservation = null; restoreWorld = null; MinecraftThermalInput.closeActiveLevel(level);
        }
    }
}
