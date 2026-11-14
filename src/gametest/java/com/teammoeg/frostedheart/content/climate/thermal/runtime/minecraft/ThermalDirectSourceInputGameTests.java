/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.source.EmissionPort;
import com.teammoeg.frostedheart.content.climate.thermal.source.SourceBinding;
import com.teammoeg.frostedheart.content.climate.thermal.source.SourceInputWindow;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatStore;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceLedger;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceMode;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Original producer/Store transactions, including a receiver failure between two world targets. */
@GameTestHolder("frostedheart_direct_source_input")
@PrefixGameTestTemplate(false)
public final class ThermalDirectSourceInputGameTests {
    private static final ThermalSourceLedger.BindingUpdates BINDINGS = new ThermalSourceLedger.BindingUpdates() {
        @Override public void registered(SpatialSourceTimeline.Descriptor descriptor, long tick, ThermalSourceLedger ledger) { }
        @Override public void emissionChanged(SpatialSourceTimeline.Descriptor descriptor, ThermalSourceLedger ledger) { }
        @Override public void unloaded(SpatialSourceTimeline.Descriptor descriptor) { }
    };

    @GameTest(template = "phase0a_empty", batch = "direct_source_handoff", timeoutTicks = 100)
    public static void aChunkNeverDisappearsDuringTheOriginalJHandoff(GameTestHelper helper) {
        try (var fixture = new Fixture()) {
            var window = fixture.window();
            int[] empty = {0};
            fixture.store.onPendingChanged(chunk -> { if (!fixture.store.hasRetainedInputInChunk(chunk)) empty[0]++; });
            fixture.store.bindSourcePrefix(window, 20, fixture.ledger, BINDINGS, fixture);
            helper.assertTrue(empty[0] == 0 && window.complete(), "Each Chunk stays published throughout the window-to-stream transfer");
            helper.assertTrue(fixture.chunkEnergy(0) == 50 && fixture.chunkEnergy(1) == 150
                    && fixture.store.pendingEnergyJ() == 200, "Both original world outlets retain their exact J");
            FHMain.LOGGER.info("THERMAL_DIRECT_SOURCE_HANDOFF,emptyMembershipSignals={},firstJ=50,secondJ=150,pendingJ=200", empty[0]);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "direct_source_failed_receiver", timeoutTicks = 100)
    public static void aRejectedSecondOutletKeepsAllAcceptedEnergyRecoverable(GameTestHelper helper) {
        try (var fixture = new Fixture()) {
            var window = fixture.window();
            var failure = new IllegalStateException("Intentional direct source receiver failure");
            boolean failed = false;
            try {
                fixture.store.bindSourcePrefix(window, 20, fixture.ledger, BINDINGS, (owned, target, generation, from, to, joules) -> {
                    if (SourceBinding.targetIndex(target) == 1) throw failure;
                    fixture.accept(owned, target, generation, from, to, joules);
                });
            } catch (IllegalStateException actual) { if (actual != failure) throw actual; failed = true; }
            helper.assertTrue(failed && window.pendingEnergyJ() == 150 && fixture.store.pendingEnergyJ() == 200,
                    "A receiver failure consumes only the first successful transfer, leaving the failed outlet in its original window");
            helper.assertTrue(fixture.chunkEnergy(0) == 50 && fixture.chunkEnergy(1) == 150, "Recovery can read accepted and unbound J exactly once");
            double[] exported = {0};
            for (long chunk : fixture.chunks) fixture.store.retireChunk(chunk, (target, joules) -> exported[0] += joules);
            helper.assertTrue(exported[0] == 200 && fixture.store.pendingEnergyJ() == 0,
                    "The original export path recovers both outlets after the partial failure");
            FHMain.LOGGER.info("THERMAL_DIRECT_SOURCE_RECOVERY,transferredJ=50,unboundJ=150,exportedJ={}", exported[0]);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "direct_source_atomic_read", timeoutTicks = 100)
    public static void aSaveReaderCannotObserveAHalfTransferredSourceWindow(GameTestHelper helper) {
        try (var fixture = new Fixture()) {
            var window = fixture.window();
            var entered = new CountDownLatch(1); var release = new CountDownLatch(1); var readerStarted = new CountDownLatch(1);
            var writer = CompletableFuture.runAsync(() -> fixture.store.bindSourcePrefix(window, 20, fixture.ledger, BINDINGS,
                    (owned, target, generation, from, to, joules) -> {
                        fixture.accept(owned, target, generation, from, to, joules);
                        if (SourceBinding.targetIndex(target) == 0) { entered.countDown(); await(release); }
                    }));
            CompletableFuture<Double> reader = null;
            try {
                await(entered);
                reader = CompletableFuture.supplyAsync(() -> {
                    readerStarted.countDown();
                    synchronized (fixture.store) { return fixture.chunkEnergy(0) + fixture.chunkEnergy(1); }
                });
                await(readerStarted);
                helper.assertTrue(!reader.isDone(), "The ordinary Store read transaction waits for the complete source transfer");
            } finally { release.countDown(); }
            try {
                writer.get(3, TimeUnit.SECONDS);
                helper.assertTrue(reader != null && reader.get(3, TimeUnit.SECONDS) == 200, "The reader obtains the complete original J once");
            } catch (Exception failure) { throw new IllegalStateException("Complete the real source transaction and its reader", failure); }
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "direct_source_cancel", timeoutTicks = 100)
    public static void cancellationAndAcceptanceRetireTheSameOriginalRecords(GameTestHelper helper) {
        try (var fixture = new Fixture()) {
            fixture.store.bindSourcePrefix(fixture.window(), 20, fixture.ledger, BINDINGS, fixture);
            try (var cancelled = fixture.store.claim(1, 0, 20, (target, from, to, joules) -> { })) {
                helper.assertTrue(cancelled.prepare(System.nanoTime() + TimeUnit.SECONDS.toNanos(1)) && cancelled.energyJ() == 200,
                        "The numerical preparation borrows exactly the directly transferred prefix");
            }
            fixture.store.advanceCleanup(100);
            helper.assertTrue(fixture.chunkEnergy(0) + fixture.chunkEnergy(1) == 200, "Cancellation keeps all original J");
            try (var accepted = fixture.store.claim(1, 0, 20, (target, from, to, joules) -> { })) {
                helper.assertTrue(accepted.prepare(System.nanoTime() + TimeUnit.SECONDS.toNanos(1)) && accepted.energyJ() == 200,
                        "Replay prepares the same prefix once");
                accepted.accepted();
            }
            fixture.store.advanceCleanup(100);
            helper.assertTrue(fixture.store.pendingEnergyJ() == 0 && fixture.store.targetCount() == 0
                    && !fixture.store.hasRetainedInputInChunk(fixture.chunks[0]) && !fixture.store.hasRetainedInputInChunk(fixture.chunks[1]),
                    "Accepted cleanup removes the last stream and publishes both empty Chunks");
        }
        helper.succeed();
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(3, TimeUnit.SECONDS)) throw new IllegalStateException("Direct source control timed out");
        } catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new IllegalStateException(failure); }
    }

    private static final class Fixture implements AutoCloseable, ThermalSourceLedger.HeatConsumer {
        final ThermalMemoryBudget budget = new ThermalMemoryBudget(8L << 20);
        final SpatialSourceTimeline producer = new SpatialSourceTimeline(budget, 8);
        final ThermalSourceLedger ledger = new ThermalSourceLedger(0, 8, 2, 8);
        final SpatialHeatStore store = new SpatialHeatStore();
        final SpatialHeatInput.Target[] targets = {
                new SpatialHeatInput.Target(BlockPos.asLong(366593, 192, 352257), SpatialHeatInput.MATERIAL, 0, 1, 1, 1, 7, 1),
                new SpatialHeatInput.Target(BlockPos.asLong(366625, 192, 352257), SpatialHeatInput.MATERIAL, 1, 1, 1, 2, 7, 1)};
        final long[] chunks = {ChunkPos.asLong(366593 >> 4, 352257 >> 4), ChunkPos.asLong(366625 >> 4, 352257 >> 4)};

        SourceInputWindow window() {
            var ports = new EmissionPort[] {EmissionPort.of(0, .25, SourceBinding.thermalNode(0, 1)),
                    EmissionPort.of(1, .75, SourceBinding.thermalNode(1, 1))};
            if (!producer.register(new SpatialSourceTimeline.Descriptor(7, 1, ThermalSourceMode.POWER_SOURCE,
                    366593, 192, 352257, 1, ports), 200, true, 0)) throw new IllegalStateException("Register the test's actual producer");
            return store.acceptSourceWindow(producer.seal(20)[0], 1, 0, targets, ledger);
        }
        @Override public void accept(SourceInputWindow window, long target, int generation, long from, long to, double joules) {
            store.receive(1, targets[SourceBinding.targetIndex(target)], from, to, joules);
        }
        double chunkEnergy(int index) {
            double[] energy = {0}; store.collectChunk(chunks[index], (target, from, to, joules) -> energy[0] += joules); return energy[0];
        }
        @Override public void close() { store.clear(); producer.close(); ledger.close(); }
    }
}
