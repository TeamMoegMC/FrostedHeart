/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputEnvelope;
import com.teammoeg.frostedheart.content.climate.thermal.solver.TiledThermalField;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceMode;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_unresolved_input")
@PrefixGameTestTemplate(false)
public final class ThermalUnresolvedInputGameTests {
    @GameTest(template = "phase0a_empty", batch = "fixed_unresolved_source_fence", timeoutTicks = 300)
    public static void anUnresolvedDistantReceiverDoesNotHoldTheReadyBaseClock(GameTestHelper helper) {
        run(helper, false, true);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "fixed_resolved_source_fence", timeoutTicks = 300)
    public static void aResolvedReceiverRetainsItsUndeliveredControlFence(GameTestHelper helper) {
        run(helper, true, true);
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "fixed_absent_source_fence", timeoutTicks = 300)
    public static void anAbsentReceiverProvidesTheSameReadyBaseService(GameTestHelper helper) {
        run(helper, false, false);
        helper.succeed();
    }

    private static void run(GameTestHelper helper, boolean resolvedReceiver, boolean installedReceiver) {
        var level = helper.getLevel(); var first = new BlockPos(!installedReceiver ? 358400 : resolvedReceiver ? 357376 : 356352, 192, 352256); var unknown = first.east(64);
        for (var base : new BlockPos[] {first, unknown}) {
            for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
                level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(base.offset(1, 0, 1), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        }
        MinecraftThermalInput.closeActiveLevel(level);
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, first, 4, 4, 4, 0, false);
                var producer = new SpatialSourceTimeline(simulation.budget.createCategory(), 16)) {
            long section = SectionPos.asLong(unknown); var page = new ThermalPageHandle(section, 1);
            if (installedReceiver) simulation.pages.put(section, page);
            byte[] sky = new byte[256]; java.util.Arrays.fill(sky, (byte) 16);
            var signatures = resolvedReceiver ? simulation.capture.captureBricks(section, null, simulation.capture.unresolvedPage(), 1)
                    : simulation.capture.unresolvedPage();
            var admission = new ThermalInputBatch.PageAdmission(page, 0, 1, 1, signatures, 0, sky, null, null);
            var profile = simulation.profiles.tuning().campfire();
            for (var base : new BlockPos[] {first, unknown}) {
                var fire = base.offset(1, 0, 1);
                helper.assertTrue(producer.register(new SpatialSourceTimeline.Descriptor(fire.asLong(), 1, ThermalSourceMode.POWER_SOURCE,
                        fire.getX(), fire.getY(), fire.getZ(), profile.profileId(), WorkerPhysicalSourceBindings.initialPorts(fire.asLong(), profile)),
                        profile.ratedPowerW(), true, 0), "Declare each real source before separating known and unknown geometry");
            }
            for (long tick : new long[] {0, 20}) {
                simulation.engine.offerInput(new ThermalInputEnvelope(new ThermalInputBatch(1, ++simulation.sequence, tick,
                        tick == 0 && installedReceiver ? new ThermalInputBatch.PageAdmission[] {admission} : ThermalInputBatch.NO_ADMISSIONS,
                        ThermalInputBatch.NO_RETIREMENTS, ThermalInputBatch.NO_RESIDENCY_UPDATES, ResolvedGeometryBatch.EMPTY,
                        ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN), producer.seal(tick)));
                for (int round = 0; round < 1024 && simulation.engine.hasReadyWork(); round++) {
                    simulation.engine.processWorkRound(); while (simulation.engine.pollWorkCompletion() != null) { }
                }
            }
            producer.enabled(unknown.offset(1, 0, 1).asLong(), false, 20);
            SpatialSourceTimeline.Window held = null, ready = null;
            for (var window : producer.seal(40)) {
                if (window.descriptor.sourceId() == unknown.offset(1, 0, 1).asLong()) held = window;
                else ready = window;
            }
            helper.assertTrue(held != null && ready != null, "Retain the actual undelivered shutdown while the known source supplies its complete prefix");
            simulation.engine.offerInput(new ThermalInputEnvelope(new ThermalInputBatch(1, ++simulation.sequence, 40,
                    ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RETIREMENTS, ThermalInputBatch.NO_RESIDENCY_UPDATES,
                    ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN),
                    new SpatialSourceTimeline.Window[] {ready}));
            for (int round = 0; round < 1024 && simulation.engine.hasReadyWork(); round++) {
                simulation.engine.processWorkRound(); while (simulation.engine.pollWorkCompletion() != null) { }
            }
            var sample = new TiledThermalField.Sample();
            boolean queryable = simulation.fixed.sampleAir(unknown.getX() + 1.5, unknown.getY() + 1.5, unknown.getZ() + 1.5, sample);
            long knownTick = simulation.fixed.sectionSampleTick(SectionPos.asLong(first));
            long blockedTick = simulation.fixed.sectionSampleTick(section);
            com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatBuffer heat = ThermalLoadedWorldGameTests.read(simulation.engine, "fixedHeat");
            double stored = simulation.engine.inputStore().pendingEnergyJ(), deferred = heat.committedPendingJoules();
            double pending = stored + deferred;
            com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceLedger ledger = ThermalLoadedWorldGameTests.read(simulation.engine, "sources");
            var balance = ledger.energyBalance();
            double liveEnergy = simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ();
            boolean receiverPresent = simulation.fixed.hasSection(section);
            simulation.fixed.sampleAir(first.getX() + 1.5, first.getY() + 1.5, first.getZ() + 1.5, sample);
            double beforeControlC = sample.temperatureC();
            FHMain.LOGGER.info("THERMAL_UNRESOLVED_SOURCE_FENCE,installedReceiver={},resolvedReceiver={},knownTick={},otherQueryable={},pendingJ={},liveJ={},balance={},ready={},progress={}",
                    installedReceiver, resolvedReceiver, knownTick, queryable, pending, liveEnergy, balance, simulation.engine.hasReadyWork(), simulation.engine.regionalProgressDescription());
            // Release the actual retained transport owner even when the observed clock fails below.
            var delivery = new ThermalInputEnvelope(new ThermalInputBatch(1, ++simulation.sequence, 40,
                    ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RETIREMENTS, ThermalInputBatch.NO_RESIDENCY_UPDATES,
                    ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN),
                    new SpatialSourceTimeline.Window[] {held});
            boolean finalReadable;
            if (resolvedReceiver) {
                var mailbox = new com.teammoeg.frostedheart.content.climate.thermal.runtime.async.ThermalDimensionMailbox(
                        com.teammoeg.frostedheart.content.climate.thermal.runtime.async.ThermalWorkerPool.shared(), simulation.engine);
                try {
                    mailbox.controlChanged();
                    long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(3);
                    boolean parked = false;
                    while (System.nanoTime() < deadline) {
                        synchronized (mailbox) {
                            Object waiting = ThermalLoadedWorldGameTests.read(mailbox, "waitingRound");
                            if (waiting != null) { parked = true; break; }
                        }
                        Thread.yield();
                    }
                    FHMain.LOGGER.info("THERMAL_SOURCE_WAIT_MAILBOX,parked={},progress={}", parked, mailbox.progressRevision());
                    helper.assertTrue(parked && mailbox.workPending(), "A real mailbox retains the pending interval and parks instead of rescheduling empty rounds");
                    long progress = mailbox.progressRevision();
                    try (var unrelated = simulation.budget.tryReserve(64)) {
                        helper.assertTrue(unrelated != null, "Exercise an unrelated real capacity release while source input is withheld");
                    }
                    try { Thread.sleep(50); } catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new IllegalStateException(failure); }
                    helper.assertTrue(mailbox.progressRevision() == progress, "Unrelated memory release must not restart an input-blocked coordinator");
                    helper.assertTrue(mailbox.submit(delivery), "The original mailbox accepts the retained source control");
                    deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(3);
                    while (simulation.fixed.acceptedTick() < 40 && System.nanoTime() < deadline) Thread.yield();
                    helper.assertTrue(simulation.fixed.acceptedTick() == 40, "Original input arrival wakes the real worker and completes the pending physical interval");
                    finalReadable = simulation.fixed.sampleAir(first.getX() + 1.5, first.getY() + 1.5, first.getZ() + 1.5, sample);
                    FHMain.LOGGER.info("THERMAL_SOURCE_WAIT_RESUMED,unrelatedCapacityWakes=0,acceptedTick=40,realWorker=true");
                } finally { mailbox.close(); mailbox.awaitClosed(); }
            } else {
                RegionalEngineTestDriver.submit(helper, simulation.engine, delivery);
                finalReadable = simulation.fixed.sampleAir(first.getX() + 1.5, first.getY() + 1.5, first.getZ() + 1.5, sample);
            }
            helper.assertTrue(receiverPresent == installedReceiver && queryable == resolvedReceiver, "The distant receiver has exactly the declared geometry availability");
            helper.assertTrue(knownTick == 40 && (!resolvedReceiver || blockedTick == 20),
                    "The ready base reaches 40 while only the affected receiver retains its real tick-20 control fence; ready="
                            + knownTick + ", blocked=" + blockedTick);
            helper.assertTrue(finalReadable && sample.temperatureC() > 0 && sample.sampleTick() == 40, "The ready base has a real heated tick-40 result after delivery");
            helper.assertTrue(Math.abs(sample.temperatureC() - beforeControlC) < 1e-12,
                    "Delivery of the unavailable source control cannot alter the already committed unrelated result");
            double expected = (resolvedReceiver ? 3 : 2) * profile.ratedPowerW() * profile.port(0).powerShare();
            helper.assertTrue(Math.abs(liveEnergy - expected) < 1e-7 * expected, "The known base actually receives both elapsed intervals before the unrelated control arrives");
            double accounted = balance.deliveredJ() + balance.declaredLossJ() + balance.degradedLossJ() + balance.unacceptedJ() + balance.exportedJ() + pending;
            helper.assertTrue((balance.degradedLossJ() > 0) != resolvedReceiver && Math.abs(accounted - balance.outputJ()) < 1e-7 * balance.outputJ(),
                    "Unavailable-geometry loss, actual delivery and pending J account for the complete produced energy");
        } finally { MinecraftThermalInput.closeActiveLevel(level); }
    }
}
