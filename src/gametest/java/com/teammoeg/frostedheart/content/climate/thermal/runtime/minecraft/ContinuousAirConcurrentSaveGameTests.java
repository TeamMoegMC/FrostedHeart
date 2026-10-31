/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.solver.SaveDuringSolveObservation;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_concurrent_save")
@PrefixGameTestTemplate(false)
public final class ContinuousAirConcurrentSaveGameTests {
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

    @GameTest(template = "phase0a_empty", timeoutTicks = 1800)
    public static void ordinarySaveSerializesClaimedHeatWithoutCancellingRunningHelper(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos firstOrigin = helper.absolutePos(new BlockPos(5, 0, 7)), secondOrigin = firstOrigin.east(160);
        ContinuousAirTwoBaseProductionGameTests.forceBase(level, firstOrigin, true);
        ContinuousAirTwoBaseProductionGameTests.forceBase(level, secondOrigin, true);
        BlockPos first = ContinuousAirProductionGameTests.room(helper, firstOrigin);
        BlockPos second = ContinuousAirProductionGameTests.room(helper, secondOrigin);
        ContinuousAirProductionGameTests.useItem(helper, first, Items.FLINT_AND_STEEL);
        ContinuousAirProductionGameTests.useItem(helper, second, Items.FLINT_AND_STEEL);
        SaveDuringSolveObservation[] observation = {null};
        helper.startSequence().thenIdle(400).thenWaitUntil(() -> {
            helper.assertTrue(MinecraftThermalInput.describeAirRead(level, first.above()).startsWith("geometry_current")
                            && MinecraftThermalInput.describeAirRead(level, second.above()).startsWith("geometry_current"),
                    "Both actual rooms must be ready before overlapping a save with numerical work");
        }).thenExecute(() -> observation[0] = new SaveDuringSolveObservation(first.above(), second.above()))
                .thenWaitUntil(() -> helper.assertTrue(observation[0].blocked(), "Observe an actual pooled numerical helper"))
                .thenExecute(() -> {
                    try {
                        var position = new ChunkPos(observation[0].chunk());
                        level.getChunk(position.x, position.z).setUnsaved(true);
                        level.getServer().saveEverything(true, true, true);
                        helper.assertTrue(observation[0].savedWhileRunning() && observation[0].failure() == null,
                                "Save must serialize pending heat while the same helper remains running: " + observation[0].summary());
                    } finally { observation[0].resume(); }
                }).thenWaitUntil(() -> {
                    helper.assertTrue(observation[0].failure() == null, observation[0].summary());
                    helper.assertTrue(observation[0].completed(), "The original numerical job must resume and publish once");
                }).thenExecute(() -> {
                    FHMain.LOGGER.info("THERMAL_CONCURRENT_SAVE,{}", observation[0].summary());
                    observation[0].close();
                    ContinuousAirProductionGameTests.useItem(helper, first, Items.IRON_SHOVEL);
                    ContinuousAirProductionGameTests.useItem(helper, second, Items.IRON_SHOVEL);
                }).thenSucceed();
        helper.runAfterDelay(1790, () -> { if (observation[0] != null) observation[0].close(); });
    }
}
