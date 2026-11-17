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
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Independent physical islands use the same input and numerical model, with different delivery timing. */
@GameTestHolder("frostedheart_independent_progress")
@PrefixGameTestTemplate(false)
public final class ThermalIndependentProgressGameTests {
    private record Result(boolean progressedBeforeDelivery, double[] temperatures, double totalJ) { }

    @GameTest(template = "phase0a_empty", batch = "uninstalled_retirement_input_prefix", timeoutTicks = 500)
    public static void aNeverInstalledOwnerRetiresWhileAnUnrelatedBaseHasMissingInput(GameTestHelper helper) {
        var base = new BlockPos(336896, 192, 327680); var second = base.east(64); var absent = base.east(128);
        for (var origin : new BlockPos[] {base, second, absent}) clear(helper, origin);
        var fire = base.offset(1, 0, 1);
        helper.getLevel().setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        MinecraftThermalInput.closeActiveLevel(helper.getLevel());
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, base, 4, 4, 4, 0, false, 2);
                var producer = new SpatialSourceTimeline(simulation.budget.createCategory(), 16)) {
            var profile = simulation.profiles.tuning().campfire();
            helper.assertTrue(producer.register(new SpatialSourceTimeline.Descriptor(fire.asLong(), 1, ThermalSourceMode.POWER_SOURCE,
                    fire.getX(), fire.getY(), fire.getZ(), profile.profileId(), WorkerPhysicalSourceBindings.initialPorts(fire.asLong(), profile)),
                    profile.ratedPowerW(), true, 0), "Register a real source that will withhold a later control");
            var live = new ThermalPageHandle(SectionPos.asLong(second), 1); var waiting = new ThermalPageHandle(SectionPos.asLong(absent), 1);
            simulation.pages.put(live.sectionKey(), live); simulation.pages.put(waiting.sectionKey(), waiting);
            byte[] sky = new byte[256]; Arrays.fill(sky, (byte) 16);
            var admitted = new ThermalInputBatch.PageAdmission(live, 0, 1, 1,
                    simulation.capture.captureBricks(live.sectionKey(), null, simulation.capture.unresolvedPage(), 1), 0, sky, null, null);
            RegionalEngineTestDriver.submit(helper, simulation.engine, envelope(simulation, 0, new ThermalInputBatch.PageAdmission[] {admitted}, producer.seal(0)));
            RegionalEngineTestDriver.submit(helper, simulation.engine, envelope(simulation, 20, ThermalInputBatch.NO_ADMISSIONS, producer.seal(20)));
            producer.enabled(fire.asLong(), false, 20); var held = producer.seal(60);
            try {
                var refused = new ThermalInputBatch.PageAdmission(waiting, 0, 1, 1,
                        simulation.capture.captureBricks(waiting.sectionKey(), null, simulation.capture.unresolvedPage(), 1), 0, sky, null, null);
                simulation.engine.offerInput(envelope(simulation, 40, new ThermalInputBatch.PageAdmission[] {refused}, new SpatialSourceTimeline.Window[0]));
                pump(helper, simulation);
                helper.assertTrue(!simulation.fixed.hasSection(waiting.sectionKey()), "The third owner never obtains a Page slot");
                simulation.engine.offerInput(retirement(simulation, waiting, 60)); pump(helper, simulation);
                com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.TopologyInbox inbox = ThermalLoadedWorldGameTests.read(simulation.engine, "topologyInputs");
                helper.assertTrue(inbox.peek(waiting.sectionKey()) == null && simulation.fixed.sectionSampleTick(SectionPos.asLong(base)) == 20
                        && simulation.fixed.sectionSampleTick(live.sectionKey()) == 60,
                        "A never-installed retirement uses the handed-off prefix, while the real blocked base keeps its own tick 20");
            } finally {
                RegionalEngineTestDriver.submit(helper, simulation.engine, envelope(simulation, 60, ThermalInputBatch.NO_ADMISSIONS, held));
            }
            helper.assertTrue(!simulation.engine.hasWork() && simulation.fixed.tileCount() == 2, "No obsolete admission restarts when the actual source resumes");
            near(helper, profile.ratedPowerW() * profile.port(0).powerShare(), simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ(),
                    "Metadata retirement does not consume the unrelated source's production twice");
            FHMain.LOGGER.info("THERMAL_UNINSTALLED_RETIREMENT_PREFIX,blockedSampleTick=20,retirementTick=60,metadataReleasedBeforeSourceArrival=true,finalPages=2");
        } finally { MinecraftThermalInput.closeActiveLevel(helper.getLevel()); }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "native_retirement_lifetime", timeoutTicks = 500)
    public static void oldRetirementCompletesAfterANativeReceiptStartsALaterActiveLifetime(GameTestHelper helper) {
        var base = new BlockPos(334848, 192, 327680); clear(helper, base);
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, base, 4, 4, 4, 0, false);
                var producer = new SpatialSourceTimeline(simulation.budget.createCategory(), 16)) {
            RegionalEngineTestDriver.submit(helper, simulation.engine, envelope(simulation, 20, ThermalInputBatch.NO_ADMISSIONS, producer.seal(20)));
            long section = SectionPos.asLong(base); var page = simulation.pages.get(section);
            double conserved = simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ();
            try (var capture = producer.capture(20)) {
                var ready = simulation.engine.requestCheckpoint(new net.minecraft.world.level.ChunkPos(base).toLong(), capture,
                        com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.PendingHeatCheckpoint.EMPTY,
                        Double.POSITIVE_INFINITY, com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.RegionalCheckpoint.Purpose.UNLOAD);
                while (!ready.isDone()) simulation.engine.processCheckpointCommandOnMain();
                ready.join();
                simulation.engine.offerInput(retirement(simulation, page, 35));
                simulation.engine.offerInput(envelope(simulation, 80, ThermalInputBatch.NO_ADMISSIONS, producer.seal(80)));
                var cancelled = simulation.engine.cancelCheckpoint(ready);
                while (!cancelled.isDone()) simulation.engine.processCheckpointCommandOnMain();
                cancelled.join();
                helper.assertTrue(simulation.fixed.frozen(section), "The native receipt finishes before worker reentry or retirement");
                pump(helper, simulation);
                helper.assertTrue(!simulation.fixed.hasSection(section) && !simulation.engine.hasWork(),
                        "A retirement preceding explicit reentry cannot ask the new active lifetime to go backwards");
                near(helper, conserved, simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ(), "Retirement crosses the original energy boundary once");
            }
            FHMain.LOGGER.info("THERMAL_NATIVE_RETIREMENT_LIFETIME,frozenTick=20,retirementTick=35,reentryCut=80,retired=true,readyLoop=false");
        } finally { MinecraftThermalInput.closeActiveLevel(helper.getLevel()); }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "obsolete_page_retirement", timeoutTicks = 500)
    public static void oldRetirementDoesNotCancelTheNewOwnersNumericalClaim(GameTestHelper helper) {
        var base = new BlockPos(335872, 192, 327680); clear(helper, base);
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, base, 4, 4, 4, 0, false);
                var producer = new SpatialSourceTimeline(simulation.budget.createCategory(), 16)) {
            long section = SectionPos.asLong(base); var old = simulation.pages.get(section);
            var current = new ThermalPageHandle(section, old.lifecycleGeneration() + 1); simulation.pages.put(section, current);
            byte[] sky = new byte[256]; Arrays.fill(sky, (byte) 16);
            var admission = new ThermalInputBatch.PageAdmission(current, 0, 1, 1,
                    simulation.capture.captureBricks(section, null, simulation.capture.unresolvedPage(), 1), 0, sky, null, null);
            RegionalEngineTestDriver.submit(helper, simulation.engine, new ThermalInputEnvelope(new ThermalInputBatch(1, ++simulation.sequence, 20,
                    new ThermalInputBatch.PageAdmission[] {admission}, new ThermalInputBatch.PageRetirement[] {new ThermalInputBatch.PageRetirement(old)},
                    ThermalInputBatch.NO_RESIDENCY_UPDATES, ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_ENVIRONMENT_UPDATES,
                    ThermalInputBatch.NO_PHASE_ACKS, Double.NaN), producer.seal(20)));
            simulation.engine.offerInput(envelope(simulation, 40, ThermalInputBatch.NO_ADMISSIONS, producer.seal(40)));
            for (int round = 0; round < 256 && simulation.fixed.idle(); round++) simulation.engine.processWorkRound();
            Object claim = ThermalLoadedWorldGameTests.read(simulation.engine, "fixedClaim");
            helper.assertTrue(claim != null && !simulation.fixed.idle(), "Keep the current owner's real 20..40 transaction uncommitted");
            simulation.engine.offerInput(retirement(simulation, old, 35)); pump(helper, simulation);
            helper.assertTrue(simulation.fixed.hasSection(section) && simulation.fixed.sectionSampleTick(section) == 40
                    && simulation.fixed.activeSince(section) == 20 && current.lastPublication() != null,
                    "An older Page identity cannot retire or retag the current owner at the same Section");
            helper.assertTrue((Boolean) ThermalLoadedWorldGameTests.read(claim, "accepted") && !simulation.engine.hasWork(),
                    "The same current claim commits once while obsolete metadata is cleaned up");
            FHMain.LOGGER.info("THERMAL_OBSOLETE_RETIREMENT,oldGeneration={},currentGeneration={},retirementTick=35,actualTick=40,originalClaimAccepted=true", old.lifecycleGeneration(), current.lifecycleGeneration());
        } finally { MinecraftThermalInput.closeActiveLevel(helper.getLevel()); }
        helper.succeed();
    }
    private static void clear(GameTestHelper helper, BlockPos base) {
        for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            helper.getLevel().setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
        MinecraftThermalInput.closeActiveLevel(helper.getLevel());
    }
    private static ThermalInputEnvelope retirement(ThermalMaterialBehaviorGameTests.Simulation simulation, ThermalPageHandle page, long tick) {
        return new ThermalInputEnvelope(new ThermalInputBatch(1, ++simulation.sequence, tick, ThermalInputBatch.NO_ADMISSIONS,
                new ThermalInputBatch.PageRetirement[] {new ThermalInputBatch.PageRetirement(page)}, ThermalInputBatch.NO_RESIDENCY_UPDATES,
                ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN),
                new SpatialSourceTimeline.Window[0]);
    }

    @GameTest(template = "phase0a_empty", batch = "source_before_initial_capture", timeoutTicks = 500)
    public static void aSourceBornBeforeFirstCaptureInitializesThatCaptureWithoutAnImpossibleOldFence(GameTestHelper helper) {
        var reference = beforeCapture(helper, new BlockPos(332800, 192, 327680), false);
        var delayed = beforeCapture(helper, new BlockPos(333824, 192, 327680), true);
        double maximum = 0;
        for (int index = 0; index < reference.temperatures.length; index++) {
            maximum = Math.max(maximum, Math.abs(reference.temperatures[index] - delayed.temperatures[index]));
            near(helper, reference.temperatures[index], delayed.temperatures[index], "Control delivery around first capture preserves cell " + index);
        }
        near(helper, reference.totalJ, delayed.totalJ, "Pre-capture source production is owned and delivered once");
        FHMain.LOGGER.info("THERMAL_SOURCE_BEFORE_CAPTURE,birthTick=20,captureTick=40,finalTick=80,samples=127,maximumC={},conservedJ={}", maximum, delayed.totalJ);
        helper.succeed();
    }
    private static Result beforeCapture(GameTestHelper helper, BlockPos dummy, boolean afterAdmission) {
        var base = dummy.east(64);
        for (var origin : new BlockPos[] {dummy, base}) for (int x = 0; x < 8; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            helper.getLevel().setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
        var fire = base.offset(1, 0, 1);
        helper.getLevel().setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        MinecraftThermalInput.closeActiveLevel(helper.getLevel());
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, dummy, 4, 4, 4, 0, false);
                var producer = new SpatialSourceTimeline(simulation.budget.createCategory(), 16)) {
            var profile = simulation.profiles.tuning().campfire();
            helper.assertTrue(producer.register(new SpatialSourceTimeline.Descriptor(fire.asLong(), 1, ThermalSourceMode.POWER_SOURCE,
                    fire.getX(), fire.getY(), fire.getZ(), profile.profileId(), WorkerPhysicalSourceBindings.initialPorts(fire.asLong(), profile)),
                    profile.ratedPowerW(), true, 20), "The actual source exists before this area has numerical history");
            long section = SectionPos.asLong(base); var page = new ThermalPageHandle(section, 1); simulation.pages.put(section, page);
            byte[] sky = new byte[256]; Arrays.fill(sky, (byte) 16);
            var admission = new ThermalInputBatch.PageAdmission(page, 0, 3, 1,
                    simulation.capture.captureBricks(section, null, simulation.capture.unresolvedPage(), 3), 0, sky, null, null);
            RegionalEngineTestDriver.submit(helper, simulation.engine, envelope(simulation, 40,
                    new ThermalInputBatch.PageAdmission[] {admission}, afterAdmission ? new SpatialSourceTimeline.Window[0] : producer.seal(40)));
            if (afterAdmission) RegionalEngineTestDriver.submit(helper, simulation.engine,
                    envelope(simulation, 40, ThermalInputBatch.NO_ADMISSIONS, producer.seal(40)));
            helper.assertTrue(simulation.fixed.sectionSampleTick(section) == 40 && simulation.fixed.activeSince(section) == 40,
                    "First capture and existing source initialization do not invent an earlier numerical interval");
            RegionalEngineTestDriver.submit(helper, simulation.engine, envelope(simulation, 80, ThermalInputBatch.NO_ADMISSIONS, producer.seal(80)));
            var values = new double[127]; sampleCells(helper, simulation, base, values, 0, 80);
            double expected = 3 * profile.ratedPowerW() * profile.port(0).powerShare();
            double energy = simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ();
            near(helper, expected, energy, "The entire produced interval is conserved through the real first admission");
            return new Result(true, values, energy);
        } finally { MinecraftThermalInput.closeActiveLevel(helper.getLevel()); }
    }

    @GameTest(template = "phase0a_empty", batch = "independent_native_reentry", timeoutTicks = 500)
    public static void cancellingUnloadDoesNotDeadlockAnOlderControlAgainstTheReentryCut(GameTestHelper helper) {
        var base = new BlockPos(331776, 192, 327680);
        for (int x = 0; x < 32; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            helper.getLevel().setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
        var first = base.offset(14, 0, 1); var second = base.offset(17, 0, 1);
        for (var fire : new BlockPos[] {first, second}) helper.getLevel().setBlockAndUpdate(fire,
                Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        MinecraftThermalInput.closeActiveLevel(helper.getLevel());
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, base, 32, 4, 4, 0, false);
                var producer = new SpatialSourceTimeline(simulation.budget.createCategory(), 16)) {
            var profile = simulation.profiles.tuning().campfire();
            for (var fire : new BlockPos[] {first, second}) helper.assertTrue(producer.register(new SpatialSourceTimeline.Descriptor(
                    fire.asLong(), 1, ThermalSourceMode.POWER_SOURCE, fire.getX(), fire.getY(), fire.getZ(), profile.profileId(),
                    WorkerPhysicalSourceBindings.initialPorts(fire.asLong(), profile)), profile.ratedPowerW(), true, 0), "Register adjacent actual sources");
            for (long tick : new long[] {0, 20}) RegionalEngineTestDriver.submit(helper, simulation.engine,
                    envelope(simulation, tick, ThermalInputBatch.NO_ADMISSIONS, producer.seal(tick)));
            long a = SectionPos.asLong(first), b = SectionPos.asLong(second);
            try (var capture = producer.capture(20)) {
                var ready = simulation.engine.requestCheckpoint(new net.minecraft.world.level.ChunkPos(first).toLong(), capture,
                        com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.PendingHeatCheckpoint.EMPTY,
                        Double.POSITIVE_INFINITY, com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.RegionalCheckpoint.Purpose.UNLOAD);
                while (!ready.isDone()) simulation.engine.processCheckpointCommandOnMain();
                ready.join();
                producer.enabled(first.asLong(), false, 35);
                simulation.engine.offerInput(envelope(simulation, 80, ThermalInputBatch.NO_ADMISSIONS, producer.seal(80)));
                pump(helper, simulation);
                helper.assertTrue(simulation.fixed.sectionSampleTick(a) == 20 && simulation.fixed.sectionSampleTick(b) < 80,
                        "The real old source control keeps its affected active neighbour below the later reentry cut");
                var cancelled = simulation.engine.cancelCheckpoint(ready);
                while (!cancelled.isDone()) simulation.engine.processCheckpointCommandOnMain();
                cancelled.join();
                helper.assertTrue(simulation.fixed.frozen(a), "Cancelling the receipt stages reentry without making native save wait for numerical catch-up");
                pump(helper, simulation);
                helper.assertTrue(!simulation.fixed.frozen(a) && simulation.fixed.sectionSampleTick(a) == 80
                        && simulation.fixed.sectionSampleTick(b) == 80 && !simulation.engine.hasWork(),
                        "The old control reaches the active receiver, then both actual frames meet the reentry cut");
                double expected = 5.75 * profile.ratedPowerW() * profile.port(0).powerShare();
                near(helper, expected, simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ(),
                        "Freeze, old control, cancellation and reentry consume every produced J once");
                FHMain.LOGGER.info("THERMAL_NATIVE_REENTRY_CONTROL,frozenTick=20,controlTick=35,reentryTick=80,conservedJ={},nativeReceiptCompletedBeforeCatchup=true", expected);
            }
        } finally { MinecraftThermalInput.closeActiveLevel(helper.getLevel()); }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "future_source_controls", timeoutTicks = 500)
    public static void futureSourceControlsCannotChangeAnEarlierGrowthPublication(GameTestHelper helper) {
        var reference = futureControls(helper, new BlockPos(329728, 192, 327680), false);
        var future = futureControls(helper, new BlockPos(330752, 192, 327680), true);
        double maximum = 0;
        for (int index = 0; index < reference.temperatures.length; index++) {
            maximum = Math.max(maximum, Math.abs(reference.temperatures[index] - future.temperatures[index]));
            near(helper, reference.temperatures[index], future.temperatures[index], "Real source-control time preserves sample " + index);
        }
        near(helper, reference.totalJ, future.totalJ, "Future control delivery preserves exactly-once production");
        FHMain.LOGGER.info("THERMAL_FUTURE_SOURCE_CONTROL,samples={},maximumC={},actualFinalTick=80,referenceJ={},futureJ={}",
                reference.temperatures.length, maximum, reference.totalJ, future.totalJ);
        helper.succeed();
    }

    private static Result futureControls(GameTestHelper helper, BlockPos base, boolean early) {
        for (int x = 0; x < 8; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            helper.getLevel().setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
        var fire = base.offset(1, 0, 1);
        helper.getLevel().setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        MinecraftThermalInput.closeActiveLevel(helper.getLevel());
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, base, 4, 4, 4, 0, false);
                var producer = new SpatialSourceTimeline(simulation.budget.createCategory(), 16)) {
            var profile = simulation.profiles.tuning().campfire();
            helper.assertTrue(producer.register(new SpatialSourceTimeline.Descriptor(fire.asLong(), 1, ThermalSourceMode.POWER_SOURCE,
                    fire.getX(), fire.getY(), fire.getZ(), profile.profileId(), WorkerPhysicalSourceBindings.initialPorts(fire.asLong(), profile)),
                    profile.ratedPowerW(), true, 0), "Register the real source at tick zero");
            RegionalEngineTestDriver.submit(helper, simulation.engine, envelope(simulation, 0, ThermalInputBatch.NO_ADMISSIONS, producer.seal(0)));
            producer.enabled(fire.asLong(), false, 20);
            SpatialSourceTimeline.Window[] future = null;
            if (early) { producer.enabled(fire.asLong(), true, 60); future = producer.seal(80); }
            else RegionalEngineTestDriver.submit(helper, simulation.engine, envelope(simulation, 20, ThermalInputBatch.NO_ADMISSIONS, producer.seal(20)));
            long section = SectionPos.asLong(base); var page = simulation.pages.get(section);
            var growth = new ThermalInputBatch.PageResidencyUpdate(page, page.liveGeometryRevision(), 3, 1,
                    simulation.capture.captureBricks(section, null, simulation.capture.unresolvedPage(), 3), null, null);
            simulation.engine.offerInput(new ThermalInputEnvelope(new ThermalInputBatch(1, ++simulation.sequence, 40,
                    ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RETIREMENTS, new ThermalInputBatch.PageResidencyUpdate[] {growth},
                    ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN),
                    early ? future : producer.seal(40)));
            pump(helper, simulation);
            helper.assertTrue(simulation.fixed.sectionSampleTick(section) == 40 && simulation.fixed.sectionCellCount(section) == 128,
                    "The recipient genuinely reaches and publishes the tick-40 growth before the future enable");
            var values = new double[254]; int at = sampleCells(helper, simulation, base, values, 0, 40);
            if (!early) {
                producer.enabled(fire.asLong(), true, 60);
                RegionalEngineTestDriver.submit(helper, simulation.engine, envelope(simulation, 60, ThermalInputBatch.NO_ADMISSIONS, producer.seal(60)));
            }
            simulation.engine.offerInput(envelope(simulation, 80, ThermalInputBatch.NO_ADMISSIONS,
                    early ? new SpatialSourceTimeline.Window[0] : producer.seal(80)));
            pump(helper, simulation); sampleCells(helper, simulation, base, values, at, 80);
            return new Result(true, values, simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ());
        } finally { MinecraftThermalInput.closeActiveLevel(helper.getLevel()); }
    }
    private static int sampleCells(GameTestHelper helper, ThermalMaterialBehaviorGameTests.Simulation simulation,
            BlockPos base, double[] values, int at, long tick) {
        var sample = new TiledThermalField.Sample();
        for (int x = 0; x < 8; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++) {
            if (x == 1 && y == 0 && z == 1) continue;
            helper.assertTrue(simulation.fixed.read(base.offset(x, y, z).asLong(), false, sample) && sample.sampleTick() == tick,
                    "Compare actual cell values at their numerical time " + tick);
            values[at++] = sample.temperatureC();
        }
        return at;
    }
    private static void pump(GameTestHelper helper, ThermalMaterialBehaviorGameTests.Simulation simulation) {
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (simulation.engine.hasReadyWork()) {
            helper.assertTrue(System.nanoTime() < deadline, "An unresolved future control parks without a ready-work loop");
            simulation.engine.processWorkRound();
            while (simulation.engine.pollWorkCompletion() != null) { }
        }
    }

    @GameTest(template = "phase0a_empty", batch = "independent_source_progress", timeoutTicks = 500)
    public static void readyBaseAdvancesWhileAnotherBaseHasAnUndeliveredControl(GameTestHelper helper) {
        var reference = run(helper, new BlockPos(327680, 192, 327680), false);
        var delayed = run(helper, new BlockPos(328704, 192, 327680), true);
        for (int index = 0; index < reference.temperatures.length; index++)
            near(helper, reference.temperatures[index], delayed.temperatures[index], "Delivery timing preserves the physical result at sample " + index);
        near(helper, reference.totalJ, delayed.totalJ, "Delayed source control conserves the complete production");
        FHMain.LOGGER.info("THERMAL_INDEPENDENT_SOURCE_PROGRESS,readyBaseAdvancedBeforeDelivery={},sameInputMaximumErrorC=0,conservedJ={}",
                delayed.progressedBeforeDelivery, delayed.totalJ);
        helper.assertTrue(delayed.progressedBeforeDelivery,
                "A ready base must advance to tick 40 while an unrelated base retains its undelivered tick-20 control");
        helper.succeed();
    }

    private static Result run(GameTestHelper helper, BlockPos first, boolean delay) {
        var second = first.east(64);
        for (var origin : new BlockPos[] {first, second}) {
            for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
                helper.getLevel().setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(origin.offset(1, 0, 1), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        }
        MinecraftThermalInput.closeActiveLevel(helper.getLevel());
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, first, 4, 4, 4, 0, false);
                var producer = new SpatialSourceTimeline(simulation.budget.createCategory(), 16)) {
            long section = SectionPos.asLong(second);
            var page = new ThermalPageHandle(section, 1); simulation.pages.put(section, page);
            byte[] sky = new byte[256]; Arrays.fill(sky, (byte) 16);
            var admission = new ThermalInputBatch.PageAdmission(page, 0, 1, 1,
                    simulation.capture.captureBricks(section, null, simulation.capture.unresolvedPage(), 1), 0, sky, null, null);
            var profile = simulation.profiles.tuning().campfire();
            for (var origin : new BlockPos[] {first, second}) {
                var fire = origin.offset(1, 0, 1);
                helper.assertTrue(producer.register(new SpatialSourceTimeline.Descriptor(fire.asLong(), 1, ThermalSourceMode.POWER_SOURCE,
                        fire.getX(), fire.getY(), fire.getZ(), profile.profileId(), WorkerPhysicalSourceBindings.initialPorts(fire.asLong(), profile)),
                        profile.ratedPowerW(), true, 0), "Register both real physical source profiles");
            }
            RegionalEngineTestDriver.submit(helper, simulation.engine, envelope(simulation, 0,
                    new ThermalInputBatch.PageAdmission[] {admission}, producer.seal(0)));
            RegionalEngineTestDriver.submit(helper, simulation.engine, envelope(simulation, 20, ThermalInputBatch.NO_ADMISSIONS, producer.seal(20)));
            producer.enabled(first.offset(1, 0, 1).asLong(), false, 20);
            var windows = producer.seal(40);
            SpatialSourceTimeline.Window held = null;
            boolean progressed;
            if (delay) {
                SpatialSourceTimeline.Window ready = null;
                for (var window : windows) {
                    if (window.descriptor.sourceId() == first.offset(1, 0, 1).asLong()) held = window;
                    else ready = window;
                }
                helper.assertTrue(held != null && ready != null, "Both actual producer windows must exist before separating delivery");
                simulation.engine.offerInput(envelope(simulation, 40, ThermalInputBatch.NO_ADMISSIONS, new SpatialSourceTimeline.Window[] {ready}));
                try {
                    for (int round = 0; round < 256 && simulation.engine.hasReadyWork(); round++) simulation.engine.processWorkRound();
                    progressed = simulation.fixed.sectionSampleTick(section) == 40;
                    helper.assertTrue(simulation.fixed.sectionSampleTick(SectionPos.asLong(first)) == 20,
                            "The incomplete base keeps its real old sample clock");
                } finally {
                    RegionalEngineTestDriver.submit(helper, simulation.engine,
                            envelope(simulation, 40, ThermalInputBatch.NO_ADMISSIONS, new SpatialSourceTimeline.Window[] {held}));
                }
            } else {
                RegionalEngineTestDriver.submit(helper, simulation.engine, envelope(simulation, 40, ThermalInputBatch.NO_ADMISSIONS, windows));
                progressed = true;
            }
            var sample = new TiledThermalField.Sample(); var values = new double[6]; int at = 0;
            for (var origin : new BlockPos[] {first, second}) for (double offset : new double[] {.125, .5, .875}) {
                helper.assertTrue(simulation.fixed.sampleAir(origin.getX() + 1 + offset, origin.getY() + 1.5, origin.getZ() + 1.5, sample)
                        && sample.sampleTick() == 40, "Both final physical samples must be genuine tick-40 values");
                values[at++] = sample.temperatureC();
            }
            double expected = 3 * profile.ratedPowerW() * profile.port(0).powerShare();
            double energy = simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ();
            near(helper, expected, energy, "Each declared source interval is consumed once");
            return new Result(progressed, values, energy);
        } finally { MinecraftThermalInput.closeActiveLevel(helper.getLevel()); }
    }

    private static ThermalInputEnvelope envelope(ThermalMaterialBehaviorGameTests.Simulation simulation, long tick,
            ThermalInputBatch.PageAdmission[] admissions, SpatialSourceTimeline.Window[] windows) {
        return new ThermalInputEnvelope(new ThermalInputBatch(1, ++simulation.sequence, tick, admissions,
                ThermalInputBatch.NO_RETIREMENTS, ThermalInputBatch.NO_RESIDENCY_UPDATES, ResolvedGeometryBatch.EMPTY,
                ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN), windows);
    }
    private static void near(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Double.isFinite(actual) && Math.abs(expected - actual) <= 1e-8 * Math.max(1, Math.abs(expected)),
                message + ";expected=" + expected + ",actual=" + actual);
    }
}
