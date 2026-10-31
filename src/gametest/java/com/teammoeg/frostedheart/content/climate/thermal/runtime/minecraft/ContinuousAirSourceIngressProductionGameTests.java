/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.solver.RuntimeConvergenceObservation;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Real producer windows, Page changes, source reincarnation and ordinary saves share one accounting owner. */
@GameTestHolder("frostedheart_source_ingress")
@PrefixGameTestTemplate(false)
public final class ContinuousAirSourceIngressProductionGameTests {
    private record TicketReturn(GameTestHelper helper, BlockPos origin, net.minecraft.world.level.chunk.LevelChunk chunk) { }
    private static TicketReturn returnOnRequest;
    private static boolean returned;
    /** A real returned ticket and synchronous chunk request at vanilla's existing request boundary. */
    public static void nativeRequestCreated(net.minecraft.server.level.ServerLevel level, net.minecraft.world.level.chunk.ChunkAccess access) {
        var request = returnOnRequest;
        var chunk = access instanceof net.minecraft.world.level.chunk.ImposterProtoChunk imposter ? imposter.getWrapped() : access;
        if (request == null || returned || level != request.helper.getLevel() || chunk != request.chunk
                || !MinecraftThermalInput.observedNativeRequestForChunk(level, access)) return;
        ThermalConvergenceScenes.force(request.helper, request.origin, 8, true);
        level.getChunkSource().getChunkFuture(chunk.getPos().x, chunk.getPos().z, net.minecraft.world.level.chunk.ChunkStatus.FULL, true);
        returned = true;
    }
    @GameTest(template = "phase0a_empty", batch = "source_ingress_capacity", timeoutTicks = 2200)
    public static void acceptedUnboundWindowsReachDiskDuringActualResourcePressure(GameTestHelper helper) {
        ContinuousAirGeometryCapacityProductionGameTests.run(helper, true);
    }

    static void observeUnboundDiskWrite(GameTestHelper helper) {
        var level = helper.getLevel();
        RuntimeConvergenceObservation.whenBindingWait(chunk -> level.getServer().execute(() -> {
            try {
                var position = new net.minecraft.world.level.ChunkPos(chunk);
                var actual = level.getChunkSource().getChunkNow(position.x, position.z);
                if (actual != null) {
                    actual.setUnsaved(true);
                    level.getServer().saveAllChunks(true, false, false);
                }
            } finally { RuntimeConvergenceObservation.bindingSaveCompleted(); }
        }));
        RuntimeConvergenceObservation.whenUnboundWrite((chunk, pending) -> helper.runAfterDelay(1, () -> {
            try {
                var storage = level.getChunkSource().chunkMap;
                // The serializer has returned to vanilla before this next-tick read.
                // Observe its real write; no second serialization consumes the live pending cache.
                storage.flushWorker();
                var disk = storage.read(new net.minecraft.world.level.ChunkPos(chunk)).join().orElseThrow();
                helper.assertTrue(disk.getCompound("FrostedHeartThermal").getCompound("pending").equals(pending),
                        "The completed region-file write must contain the same unbound records");
                RuntimeConvergenceObservation.unboundDiskWritten();
            } finally { RuntimeConvergenceObservation.unboundReadCompleted(); }
        }));
    }

    @GameTest(template = "phase0a_empty", batch = "source_ingress_controls", timeoutTicks = 5000)
    public static void sourceWindowsLeaveTransportBeforeBindingAndSurviveRealControlChanges(GameTestHelper helper) throws java.io.IOException {
        ContinuousAirPrecisionProductionGameTests.restoreReferenceClimate(helper, true);
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(512, 0, 0));
        var controlOrigin = helper.absolutePos(BlockPos.ZERO);
        ThermalConvergenceScenes.force(helper, origin, 8, true);
        ThermalConvergenceScenes.force(helper, controlOrigin, 16, true);
        var control = ContinuousAirProductionGameTests.room(helper, controlOrigin);
        ContinuousAirProductionGameTests.useItem(helper, control, Items.FLINT_AND_STEEL);
        var sample = new ThermalEnvironmentSample();
        helper.startSequence().thenIdle(200).thenWaitUntil(() -> helper.assertTrue(
                ThermalConvergenceScenes.fresh(helper, control.above(), sample), "Establish a real independent normal field"))
        .thenExecute(() -> {
            var building = new ThermalConvergenceScenes.HallBuild(helper, origin, 8);
            var fires = building.fires;
            var probe = fires[18].above();
            RuntimeConvergenceObservation.arm(control.above(), probe, fires);
            observeUnboundDiskWrite(helper);
            long[] lastControl = {-1}; int[] controlAdvances = {0}, controlFailures = {0};
            net.minecraft.world.level.chunk.LevelChunk[] original = {null};
            long[] cancelledBefore = {0}, savedTick = {-1};
            helper.startSequence().thenWaitUntil(() -> helper.assertTrue(building.advance(), "Build the real source Page in bounded batches"))
            .thenExecute(() -> {
            ContinuousAirProductionTiming.eachTick(helper, 1000, elapsed -> {
                if (elapsed == 80) {
                    // Real same-tick off/on events, with the ordinary event ordering.
                    ContinuousAirProductionGameTests.useItem(helper, fires[0], Items.IRON_SHOVEL);
                    ContinuousAirProductionGameTests.useItem(helper, fires[0], Items.FLINT_AND_STEEL);
                }
                if (elapsed == 160) level.setBlockAndUpdate(fires[0], Blocks.AIR.defaultBlockState());
                if (elapsed == 180) ThermalConvergenceScenes.fire(helper, fires[0]);
                if (elapsed == 240) level.setBlockAndUpdate(fires[1].above(), Blocks.STONE.defaultBlockState());
                if (elapsed == 300) level.setBlockAndUpdate(fires[1].above(), Blocks.AIR.defaultBlockState());
                if (elapsed == 100 || elapsed == 280 || elapsed == 500) level.getServer().saveEverything(true, true, true);
                if (elapsed % 200 == 0) {
                    for (var fire : fires) ThermalConvergenceScenes.fuel(helper, fire, 1);
                    ThermalConvergenceScenes.fuel(helper, control, 1);
                }
                if (!ThermalConvergenceScenes.fresh(helper, control.above(), sample)
                        || ThermalConvergenceScenes.step(helper, control.above()) != 1) controlFailures[0]++;
                if (sample.airSampleTick() > lastControl[0]) { lastControl[0] = sample.airSampleTick(); controlAdvances[0]++; }
                if (elapsed % 200 == 0) FHMain.LOGGER.info("THERMAL_SOURCE_INGRESS_PROGRESS,elapsed={},stats={},controlFailures={}",
                        elapsed, RuntimeConvergenceObservation.snapshot(), controlFailures[0]);
            });
            helper.startSequence().thenIdle(1001).thenWaitUntil(() -> {
                var state = RuntimeConvergenceObservation.snapshot();
                helper.assertTrue(state.boundSources() == 64 && state.region() != state.controlRegion()
                        && ThermalConvergenceScenes.fresh(helper, probe, sample), "All real sources must drain into current world targets: " + state);
            }).thenExecute(() -> {
                var state = RuntimeConvergenceObservation.snapshot();
                helper.assertTrue(state.releasedWindows() > 64 && state.acceptedAheadOfBinding() > 0
                        && state.maximumOwnedWindows() <= 2 && state.orderingFailures() == 0,
                        "Source ownership must leave transport, remain bounded and preserve ordered control generations: " + state);
                helper.assertTrue(controlFailures[0] == 0 && controlAdvances[0] >= 4,
                        "A busy source Page must leave the independent normal region current");
                helper.assertTrue(state.energyObservations() > 0 && state.maximumImbalanceJ() < 1e-6, "Saving and reincarnation must not duplicate or drop J: " + state);
                helper.assertTrue(state.boundReplacements() > 0, "A real successor source generation must bind in order: " + state);
                FHMain.LOGGER.info("THERMAL_SOURCE_INGRESS_RESULT,stats={},controlAdvances={},controlFailures={}", state, controlAdvances[0], controlFailures[0]);
                original[0] = level.getChunkAt(fires[0]);
                cancelledBefore[0] = MinecraftThermalInput.observedNativeCheckpoints(level).cancelled();
                returnOnRequest = new TicketReturn(helper, origin, original[0]); returned = false;
                level.setBlockAndUpdate(fires[1].above(), Blocks.STONE.defaultBlockState());
                ThermalConvergenceScenes.force(helper, origin, 8, false);
            }).thenWaitUntil(() -> {
                helper.assertTrue(returned, "Return actual tickets at the native request boundary; no fabricated pending future");
            }).thenWaitUntil(() -> {
                var nativeState = MinecraftThermalInput.observedNativeCheckpoints(level);
                helper.assertTrue(nativeState.cancelled() > cancelledBefore[0] && nativeState.pending() == 0
                        && level.getChunkSource().getChunkNow(original[0].getPos().x, original[0].getPos().z) == original[0],
                        "Ticket return must cancel the original pending unload without exporting its live input");
                returnOnRequest = null;
            }).thenExecute(() -> {
                level.setBlockAndUpdate(fires[1].above(), Blocks.AIR.defaultBlockState());
                for (var fire : fires) ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
            }).thenIdle(80).thenWaitUntil(() -> helper.assertTrue(ThermalConvergenceScenes.fresh(helper, probe, sample),
                    "Resume the same source owner after cancelling the native checkpoint"))
            .thenExecute(() -> ThermalConvergenceScenes.force(helper, origin, 8, false))
            .thenWaitUntil(() -> helper.assertTrue(level.getChunkSource().getChunkNow(original[0].getPos().x, original[0].getPos().z) == null
                    && MinecraftThermalInput.observedNativeCheckpoints(level).pending() == 0, "The second real ticket removal must actually unload"))
            .thenExecute(() -> {
                var stored = MinecraftThermalInput.dormantState(original[0]);
                helper.assertTrue(stored != null && stored.oldestAirSavedTick() >= 0, "The actual unload must retain a temperature field");
                var tag = new net.minecraft.nbt.CompoundTag(); stored.encode(tag);
                helper.assertTrue(tag.getCompound("FrostedHeartThermal").getInt("version") == 6, "Write the current system format");
                savedTick[0] = stored.oldestAirSavedTick();
                ThermalConvergenceScenes.force(helper, origin, 8, true);
            }).thenWaitUntil(() -> {
                var loaded = level.getChunkSource().getChunkNow(original[0].getPos().x, original[0].getPos().z);
                helper.assertTrue(loaded != null && loaded != original[0], "Read the saved data into a new real chunk instance");
                var stored = MinecraftThermalInput.dormantState(loaded);
                helper.assertTrue(stored != null && stored.oldestAirSavedTick() >= savedTick[0], "Reload the saved current-format temperature history");
            }).thenExecute(() -> {
                for (var fire : fires) { ThermalConvergenceScenes.fuel(helper, fire, 1); ContinuousAirProductionGameTests.useItem(helper, fire, Items.FLINT_AND_STEEL); }
            }).thenIdle(200).thenWaitUntil(() -> helper.assertTrue(ThermalConvergenceScenes.fresh(helper, probe, sample),
                    "Restored accepted heat and reloaded real producers must continue advancing"))
            .thenExecute(() -> {
                var state = RuntimeConvergenceObservation.snapshot();
                helper.assertTrue(state.energyObservations() > 0 && state.maximumImbalanceJ() < 1e-6 && state.orderingFailures() == 0,
                        "Actual unload cancellation and disk reload must preserve single ownership: " + state);
                FHMain.LOGGER.info("THERMAL_SOURCE_INGRESS_RELOAD,stats={},savedTick={}", state, savedTick[0]);
                for (var fire : fires) ContinuousAirProductionGameTests.useItem(helper, fire, Items.IRON_SHOVEL);
                ContinuousAirProductionGameTests.useItem(helper, control, Items.IRON_SHOVEL);
                RuntimeConvergenceObservation.stop(); MinecraftThermalInput.closeActiveLevel(level);
            }).thenWaitUntil(() -> helper.assertTrue(MinecraftThermalInput.thermalBudgetUsedBytes() == 0,
                    "All source ingress, accepted windows, fields and helpers must release their reservations"))
            .thenExecute(() -> {
                ThermalConvergenceScenes.force(helper, origin, 8, false);
                ThermalConvergenceScenes.force(helper, controlOrigin, 16, false);
            }).thenSucceed();
            });
        });
    }
}
