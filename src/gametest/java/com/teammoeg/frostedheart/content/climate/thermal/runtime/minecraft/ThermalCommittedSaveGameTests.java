/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.PendingHeatCheckpoint;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.RegionalCheckpoint;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.async.ThermalWorkerPool;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputEnvelope;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceMode;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Ordinary reads overlap actual source transfers and the original native unload protocol. */
@GameTestHolder("frostedheart_committed_save")
@PrefixGameTestTemplate(false)
public final class ThermalCommittedSaveGameTests {
    @GameTest(template = "phase0a_empty", batch = "committed_save_unrepresented_unload", timeoutTicks = 400)
    public static void unloadWithoutResidentCellsKeepsTheUnrelatedNumericalClaim(GameTestHelper helper) {
        for (boolean unloading : new boolean[] {false, true}) {
            var origin = new BlockPos(unloading ? 303104 : 299008, 192, 299008);
            prepare(helper, origin);
            try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0, false);
                    var producer = producer(helper, simulation, origin)) {
                var engine = simulation.engine;
                for (long tick : new long[] {0, 20}) {
                    RegionalEngineTestDriver.submit(helper, engine, new ThermalInputEnvelope(new ThermalInputBatch(1, ++simulation.sequence, tick,
                            ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RETIREMENTS, ThermalInputBatch.NO_RESIDENCY_UPDATES,
                            ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN), producer.seal(tick)));
                }
                var delayed = origin.east(64).offset(1, 1, 1);
                long chunk = new ChunkPos(delayed).toLong();
                var input = new com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput.Builder();
                input.appendAggregate(new com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput.Target(
                        delayed.asLong(), (byte) 2, -1, 0, 0, 0, 0, 0), 20, 40, 700);
                helper.assertTrue(engine.offerRestoredHeat(chunk, PendingHeatCheckpoint.capture(input.freeze(), ignored -> -1, 20),
                        40, Double.POSITIVE_INFINITY, 1), "The unrelated Chunk really owns queued archive input awaiting geometry");
                engine.offerInput(new ThermalInputEnvelope(new ThermalInputBatch(1, ++simulation.sequence, 40,
                        ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RETIREMENTS, ThermalInputBatch.NO_RESIDENCY_UPDATES,
                        ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN), producer.seal(40)));
                for (int round = 0; round < 256 && simulation.fixed.idle(); round++) engine.processWorkRound();
                Object claim = ThermalLoadedWorldGameTests.read(engine, "fixedClaim");
                helper.assertTrue(claim != null && !simulation.fixed.idle() && simulation.fixed.acceptedTick() == 20,
                        "Keep the real hot-base interval 20..40 prepared but uncommitted");
                try (var source = producer.capture(40)) {
                    var ready = engine.requestCheckpoint(chunk, source, PendingHeatCheckpoint.EMPTY, Double.POSITIVE_INFINITY, RegionalCheckpoint.Purpose.UNLOAD);
                    while (!ready.isDone()) engine.processCheckpointCommandOnMain();
                    var checkpoint = ready.join();
                    try {
                        helper.assertTrue(checkpoint.sections().isEmpty(), "There are no resident cells to freeze in the unloading Chunk");
                        near(helper, 700, energy(checkpoint), "Native save retains the queued archive once");
                        helper.assertTrue(ThermalLoadedWorldGameTests.read(engine, "fixedClaim") == claim && !simulation.fixed.idle(),
                                "A Chunk without resident cells must not cancel an unrelated numerical claim during capture");
                    } finally {
                        var finished = checkpoint.finish(unloading);
                        while (!finished.isDone()) engine.processCheckpointCommandOnMain();
                        finished.join();
                    }
                }
                helper.assertTrue(ThermalLoadedWorldGameTests.read(engine, "fixedClaim") == claim && !simulation.fixed.idle(),
                        "Finishing or cancelling the unrelated unload must preserve the same numerical claim");
                for (int round = 0; round < 256 && simulation.fixed.acceptedTick() < 40; round++) engine.processWorkRound();
                helper.assertTrue(simulation.fixed.acceptedTick() == 40 && (Boolean) ThermalLoadedWorldGameTests.read(claim, "accepted"),
                        "The same original claim resumes and commits once");
                near(helper, 2 * thermalJ(simulation), simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ(),
                        "The independent source's complete production remains conserved");
                java.util.ArrayDeque<?> queued = ThermalLoadedWorldGameTests.read(engine, "restoreRequests");
                helper.assertTrue(queued.size() == (unloading ? 0 : 1), "Only a consumed unload exports its queued cold input");
                FHMain.LOGGER.info("THERMAL_UNREPRESENTED_UNLOAD,unloading={},savedPendingJ=700,claimPreserved=true,acceptedTick=40,heatJ={}",
                        unloading, simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ());
            } finally { cleanup(helper, origin); }
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "committed_save_query", timeoutTicks = 300)
    public static void pointQueryDoesNotWaitForANativePrivateCapture(GameTestHelper helper) throws Exception {
        var origin = new BlockPos(294912, 192, 294912);
        prepare(helper, origin);
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0, false);
                var producer = producer(helper, simulation, origin);
                var nativeSource = producer.capture(20)) {
            long chunk = new ChunkPos(origin).toLong();
            var ready = simulation.engine.requestCheckpoint(chunk, nativeSource, PendingHeatCheckpoint.EMPTY,
                    Double.POSITIVE_INFINITY, RegionalCheckpoint.Purpose.UNLOAD);
            var held = new CountDownLatch(1); var resume = new CountDownLatch(1);
            var forced = new AtomicBoolean(); var finished = new CompletableFuture<Void>();
            var previous = com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer;
            com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer =
                    new com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.Observer() {
                @Override public void checkpointSealed(long saving, int unbound, RegionalCheckpoint checkpoint,
                        SpatialSourceTimeline.Capture source, PendingHeatCheckpoint cold, double halfLife) {
                    if (saving != chunk) return;
                    held.countDown();
                    try {
                        if (!resume.await(2, TimeUnit.SECONDS)) { forced.set(true); resume.countDown(); }
                    } catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new IllegalStateException(failure); }
                }
            };
            try {
                helper.assertTrue(ThermalWorkerPool.shared().tryExecute(() -> {
                    try {
                        while (!ready.isDone()) simulation.engine.processCheckpointCommandOnMain();
                        finished.complete(null);
                    } catch (Throwable failure) { finished.completeExceptionally(failure); }
                }), "Capture native state on the real worker");
                helper.assertTrue(held.await(3, TimeUnit.SECONDS), "Hold a real native capture before releasing its control turn");
                var sample = new com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication.MutableSample();
                helper.assertTrue(simulation.query.tryReadAir(origin.getX() + 1.5, origin.getY() + 1.5, origin.getZ() + 1.5, sample),
                        "Read the published air while native capture owns its private copy");
                helper.assertTrue(!forced.get(), "Point queries must not wait for the native capture's private copy or its coordinator turn");
                FHMain.LOGGER.info("THERMAL_NATIVE_READ_TRANSACTION,readWhileCaptureHeld=true,forcedRelease=false");
            } finally {
                resume.countDown(); finished.get(5, TimeUnit.SECONDS);
                com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer = previous;
                if (ready.isDone() && !ready.isCompletedExceptionally()) {
                    var receipt = ready.join().finish(false);
                    while (!receipt.isDone()) simulation.engine.processCheckpointCommandOnMain();
                    receipt.join();
                }
            }
        } finally { cleanup(helper, origin); }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "committed_save_scope", timeoutTicks = 300)
    public static void ordinaryCaptureCopiesOnlyRelatedProducerRowsAndReleasesItsMetadata(GameTestHelper helper) {
        var origin = new BlockPos(290816, 192, 290816);
        prepare(helper, origin);
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0, false);
                var producer = producer(helper, simulation, origin)) {
            var profile = simulation.profiles.tuning().campfire();
            for (int index = 1; index <= 64; index++) {
                var fire = origin.offset(32 * index, 0, 1);
                helper.assertTrue(producer.register(new SpatialSourceTimeline.Descriptor(fire.asLong(), 1, ThermalSourceMode.POWER_SOURCE,
                        fire.getX(), fire.getY(), fire.getZ(), profile.profileId(), WorkerPhysicalSourceBindings.initialPorts(fire.asLong(), profile)),
                        1000, true, 0), "Register independent remote producers");
            }
            long chunk = new ChunkPos(origin).toLong();
            try (var warm = simulation.engine.captureCommittedCheckpoint(chunk, 20, producer, PendingHeatCheckpoint.EMPTY, Double.POSITIVE_INFINITY);
                    var source = warm.producerCapture()) { }
            long before = simulation.budget.usedBytes();
            try (var global = producer.capture(20);
                    var saved = simulation.engine.captureCommittedCheckpoint(chunk, 20, producer, PendingHeatCheckpoint.EMPTY, Double.POSITIVE_INFINITY);
                    var source = saved.producerCapture()) {
                java.util.List<?> allRows = ThermalLoadedWorldGameTests.read(global, "values");
                java.util.List<?> localRows = ThermalLoadedWorldGameTests.read(source, "values");
                helper.assertTrue(allRows.size() == 65 && localRows.size() == 1, "A Chunk save retains one related producer rather than all 65");
                double[] original = {0}; global.pending(chunk, simulation.engine::sourcePortDestination, (target, from, to, joules) -> original[0] += joules);
                near(helper, original[0], energy(saved), "Scoped and whole-timeline captures preserve the same Chunk input");
                near(helper, thermalJ(simulation), energy(saved), "Remote producers cannot enter this Chunk's saved J");
            }
            helper.assertTrue(simulation.budget.usedBytes() == before, "Overlapping source/copy metadata returns its complete credit after both captures close");
            FHMain.LOGGER.info("THERMAL_COMMITTED_SAVE_SCOPE,allProducers=65,copiedProducers=1,pendingJ={},metadataReleased=true", thermalJ(simulation));
        } finally { cleanup(helper, origin); }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "committed_save_transfer", timeoutTicks = 300)
    public static void saveCannotReadTheGapBetweenOriginalAndResolvedSourceInput(GameTestHelper helper) throws Exception {
        var origin = new BlockPos(278528, 192, 278528);
        prepare(helper, origin);
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0, false);
                var producer = producer(helper, simulation, origin)) {
            var engine = simulation.engine; var store = engine.inputStore();
            var gap = new CountDownLatch(1); var resume = new CountDownLatch(1);
            var attempted = new CountDownLatch(1); var finished = new CompletableFuture<Void>();
            var once = new AtomicBoolean(); var excludedPartialRead = new AtomicBoolean();
            var savingThread = Thread.currentThread();
            store.onPendingChanged(chunk -> {
                // Pause inside the direct handoff after a stream exists and
                // before its source window has relinquished the same J.
                if (store.unboundWindowsInChunk(chunk) == 0
                        || store.targetsInChunk(chunk) <= simulation.profiles.tuning().campfire().portCount()
                        || !once.compareAndSet(false, true)) return;
                gap.countDown();
                try {
                    if (!resume.await(3, TimeUnit.SECONDS)) throw new IllegalStateException("Source transfer test did not release its writer");
                } catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new IllegalStateException(failure); }
            });
            engine.offerInput(new ThermalInputEnvelope(new ThermalInputBatch(1, ++simulation.sequence, 20,
                    ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RETIREMENTS, ThermalInputBatch.NO_RESIDENCY_UPDATES,
                    ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN), producer.seal(20)));
            helper.assertTrue(ThermalWorkerPool.shared().tryExecute(() -> {
                try {
                    for (int round = 0; round < 256 && gap.getCount() != 0; round++) engine.processWorkRound();
                    finished.complete(null);
                } catch (Throwable failure) { finished.completeExceptionally(failure); }
            }), "Run source acceptance and binding on the actual pool");
            var releaser = new Thread(() -> {
                try {
                    if (!attempted.await(3, TimeUnit.SECONDS)) return;
                    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
                    while (savingThread.getState() != Thread.State.BLOCKED && System.nanoTime() < deadline) Thread.yield();
                    excludedPartialRead.set(savingThread.getState() == Thread.State.BLOCKED);
                } catch (InterruptedException failure) { Thread.currentThread().interrupt(); }
                finally { resume.countDown(); }
            }, "Thermal-save-transfer-test-release");
            try {
                helper.assertTrue(gap.await(3, TimeUnit.SECONDS), "Pause after source-window J was removed and before resolved streams receive it");
                releaser.start(); attempted.countDown();
                try (var saved = engine.captureCommittedCheckpoint(new ChunkPos(origin).toLong(), 20, producer,
                        PendingHeatCheckpoint.EMPTY, Double.POSITIVE_INFINITY);
                        var source = saved.producerCapture()) {
                    helper.assertTrue(excludedPartialRead.get(), "The save must exclude the incomplete transfer");
                    near(helper, thermalJ(simulation), energy(saved), "The complete resolved J appears once after the transfer");
                }
                finished.get(5, TimeUnit.SECONDS);
                helper.assertTrue(engine.inputStore().pendingEnergyJ() > 0 && simulation.fixed.acceptedTick() == 0,
                        "Capturing the transfer does not consume input or advance Accepted");
                FHMain.LOGGER.info("THERMAL_COMMITTED_SAVE_TRANSFER,partialReadExcluded=true,pendingJ={},acceptedTick=0", store.pendingEnergyJ());
            } finally {
                resume.countDown(); finished.get(5, TimeUnit.SECONDS); releaser.join(5000);
                store.onPendingChanged(ignored -> { });
            }
        } finally { cleanup(helper, origin); }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "committed_save_native_cancel", timeoutTicks = 300)
    public static void ordinarySaveDoesNotCancelAnUnsealedOrSealedNativeCapture(GameTestHelper helper) {
        nativeOverlap(helper, new BlockPos(282624, 192, 282624), false);
    }

    @GameTest(template = "phase0a_empty", batch = "committed_save_native_export", timeoutTicks = 300)
    public static void ordinarySaveRetainsExportedColdJAndTheLaterProducerSuffixOnce(GameTestHelper helper) {
        nativeOverlap(helper, new BlockPos(286720, 192, 286720), true);
    }

    private static void nativeOverlap(GameTestHelper helper, BlockPos origin, boolean unload) {
        prepare(helper, origin);
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0, false);
                var producer = producer(helper, simulation, origin);
                var nativeSource = producer.capture(20)) {
            long chunk = new ChunkPos(origin).toLong(); var engine = simulation.engine;
            var nativeReady = engine.requestCheckpoint(chunk, nativeSource, PendingHeatCheckpoint.EMPTY,
                    Double.POSITIVE_INFINITY, RegionalCheckpoint.Purpose.UNLOAD);
            try (var saved = engine.captureCommittedCheckpoint(chunk, 20, producer, PendingHeatCheckpoint.EMPTY, Double.POSITIVE_INFINITY);
                    var source = saved.producerCapture()) {
                near(helper, thermalJ(simulation), energy(saved), "Ordinary SAVE includes the producer tail while native control remains queued");
                helper.assertTrue(!nativeReady.isDone(), "Ordinary SAVE must not execute or cancel the native request");
            }
            while (!nativeReady.isDone()) engine.processCheckpointCommandOnMain();
            var nativeSaved = nativeReady.join();
            near(helper, thermalJ(simulation), energy(nativeSaved), "The original native capture still owns its complete producer tail");
            try (var saved = engine.captureCommittedCheckpoint(chunk, 20, producer, PendingHeatCheckpoint.EMPTY, Double.POSITIVE_INFINITY);
                    var source = saved.producerCapture()) {
                near(helper, thermalJ(simulation), energy(saved), "Ordinary SAVE also overlaps the sealed native frame");
                helper.assertTrue(nativeSaved.pending().size() > 0, "The native owner retains its input until its own finish");
            }
            var cold = unload ? PendingHeatCheckpoint.capture(nativeSaved.pending(), ignored -> -1, 20) : PendingHeatCheckpoint.EMPTY;
            var finish = nativeSaved.finish(unload);
            if (unload) {
                try (var saved = engine.captureCommittedCheckpoint(chunk, 40, producer, cold, Double.POSITIVE_INFINITY);
                        var source = saved.producerCapture()) {
                    near(helper, 2 * thermalJ(simulation), energy(saved), "A native cold copy does not duplicate the J still owned by Store/producer before export");
                    helper.assertTrue(!finish.isDone() && saved.coldPending() == cold,
                            "Reading preserves the native finish and its eventual cold cache");
                }
            }
            while (!finish.isDone()) engine.processCheckpointCommandOnMain();
            finish.join();
            try (var saved = engine.captureCommittedCheckpoint(chunk, 40, producer, cold, Double.POSITIVE_INFINITY);
                    var source = saved.producerCapture()) {
                near(helper, 2 * thermalJ(simulation), energy(saved), "The next save retains previous pending J and the new 20-tick suffix once");
                helper.assertTrue(saved.coldPending().size() == cold.size(), "Ordinary SAVE preserves the actual cold owner");
            }
            FHMain.LOGGER.info("THERMAL_COMMITTED_SAVE_NATIVE,unload={},queuedRead=true,sealedRead=true,coldJ={},laterPendingJ={}",
                    unload, unload ? thermalJ(simulation) : 0, 2 * thermalJ(simulation));
        } finally { cleanup(helper, origin); }
        helper.succeed();
    }

    private static double thermalJ(ThermalMaterialBehaviorGameTests.Simulation simulation) {
        double result = 0;
        var profile = simulation.profiles.tuning().campfire();
        for (int index = 0; index < profile.portCount(); index++) {
            var port = profile.port(index);
            if (port.kind() == com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile.PortKind.AIR_FACE)
                result += 1000 * port.powerShare();
        }
        return result;
    }
    private static SpatialSourceTimeline producer(GameTestHelper helper, ThermalMaterialBehaviorGameTests.Simulation simulation, BlockPos origin) {
        var producer = new SpatialSourceTimeline(simulation.budget.createCategory(), 128);
        var fire = origin.offset(1, 0, 1); var profile = simulation.profiles.tuning().campfire();
        helper.assertTrue(producer.register(new SpatialSourceTimeline.Descriptor(fire.asLong(), 1, ThermalSourceMode.POWER_SOURCE,
                fire.getX(), fire.getY(), fire.getZ(), profile.profileId(), WorkerPhysicalSourceBindings.initialPorts(fire.asLong(), profile)),
                1000, true, 0), "Register the actual physical campfire ports");
        return producer;
    }
    private static void prepare(GameTestHelper helper, BlockPos origin) {
        for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            helper.getLevel().setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(origin.offset(1, 0, 1), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        MinecraftThermalInput.closeActiveLevel(helper.getLevel());
    }
    private static void cleanup(GameTestHelper helper, BlockPos origin) {
        helper.getLevel().setBlockAndUpdate(origin.offset(1, 0, 1), Blocks.AIR.defaultBlockState());
        MinecraftThermalInput.closeActiveLevel(helper.getLevel());
    }
    private static double energy(RegionalCheckpoint checkpoint) {
        double result = 0; var pending = checkpoint.pending();
        for (int index = 0; index < pending.size(); index++) result += pending.energyJ(index);
        return result;
    }
    private static void near(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) <= 1e-7 * Math.max(1, Math.abs(expected)), message + ";expected=" + expected + ",actual=" + actual);
    }
}
