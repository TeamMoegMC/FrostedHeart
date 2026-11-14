/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputEnvelope;
import com.teammoeg.frostedheart.content.climate.thermal.solver.TiledThermalField;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceMode;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Admission pressure on the real input/geometry/numerical ownership chain. */
@GameTestHolder("frostedheart_tiled_saturation")
@PrefixGameTestTemplate(false)
public final class TiledThermalSaturationGameTests {
    @GameTest(template = "phase0a_empty", batch = "fixed_saturation", timeoutTicks = 600)
    public static void aFullNewBaseCannotStopAnExistingBaseOrAccumulateUnboundedInput(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = new BlockPos(266240, 192, 266240);
        var expansion = origin.offset(64, 0, 0);
        var secondExpansion = origin.offset(80, 0, 0);
        var oldFire = origin.offset(1, 0, 1);
        var newFire = expansion.offset(1, 0, 1);
        for (var base : new BlockPos[] {origin, expansion, secondExpansion}) {
            for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
                level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        for (var fire : new BlockPos[] {oldFire, newFire})
            level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        MinecraftThermalInput.closeActiveLevel(level);
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0, false);
                var producer = new SpatialSourceTimeline(simulation.budget.createCategory(), 16)) {
            register(helper, simulation, producer, oldFire, 0);
            RegionalEngineTestDriver.submit(helper, simulation.engine, envelope(simulation, 0, ThermalInputBatch.NO_ADMISSIONS, producer));
            RegionalEngineTestDriver.submit(helper, simulation.engine, envelope(simulation, 20, ThermalInputBatch.NO_ADMISSIONS, producer));
            double power = simulation.profiles.tuning().campfire().ratedPowerW()
                    * simulation.profiles.tuning().campfire().port(0).powerShare();
            long section = SectionPos.asLong(expansion);
            var page = new ThermalPageHandle(section, 1);
            byte[] sky = new byte[256]; java.util.Arrays.fill(sky, (byte) 16);
            var admission = new ThermalInputBatch.PageAdmission(page, 0, 1, 1,
                    simulation.capture.captureBricks(section, null, simulation.capture.unresolvedPage(), 1), 0, sky, null, null);
            long secondSection = SectionPos.asLong(secondExpansion);
            var secondAdmission = new ThermalInputBatch.PageAdmission(new ThermalPageHandle(secondSection, 1), 0, 1, 1,
                    simulation.capture.captureBricks(secondSection, null, simulation.capture.unresolvedPage(), 1), 0, sky, null, null);
            register(helper, simulation, producer, newFire, 20);
            long maxInputBytes = 0, refusals;
            // Occupy accounting capacity, without allocating a synthetic multi-megabyte heap object.
            try (var occupied = simulation.budget.tryReserve(simulation.budget.limitBytes() - simulation.budget.usedBytes() - 2048)) {
                helper.assertTrue(occupied != null, "Establish pressure after the existing source has its normal working storage");
                simulation.engine.offerInput(envelope(simulation, 20, new ThermalInputBatch.PageAdmission[] {admission, secondAdmission}, producer));
                pump(helper, simulation);
                helper.assertTrue(!simulation.fixed.hasSection(section), "A refused base must not partially enter the numerical field");
                // Allow one Tile's storage, while the atomic batch needs two.
                long release = simulation.budget.usedBytes() + TiledThermalField.tileBytes(1) + 256 - simulation.budget.limitBytes();
                helper.assertTrue(release > 0 && occupied.tryResize(occupied.bytes() - release), "Make room for only one member of the waiting batch");
                pump(helper, simulation);
                helper.assertTrue(!simulation.fixed.hasSection(section) && !simulation.fixed.hasSection(secondSection),
                        "A batch that cannot fit as a whole must leave both new Sections absent");
                for (int cut = 2; cut <= 121; cut++) {
                    long tick = cut * 20L;
                    simulation.engine.offerInput(envelope(simulation, tick, ThermalInputBatch.NO_ADMISSIONS, producer));
                    pump(helper, simulation);
                    helper.assertTrue(simulation.fixed.acceptedTick() == tick,
                            "The existing base must advance while another base waits for capacity; expected=" + tick
                                    + ",actual=" + simulation.fixed.acceptedTick());
                    helper.assertTrue(!simulation.fixed.hasSection(section) && !simulation.fixed.hasSection(secondSection),
                            "Waiting cannot expose any part of either new base");
                    near(helper, tick / 20.0 * power, simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ(),
                            "The existing source continues delivering exactly once while admission is full");
                    maxInputBytes = Math.max(maxInputBytes, simulation.engine.inputStore().retainedBytes());
                }
                helper.assertTrue(maxInputBytes < 1L << 20, "Two steady sources cannot grow an input history with elapsed wait time");
                refusals = simulation.budget.admissionRefusals();
            }
            RegionalEngineTestDriver.submit(helper, simulation.engine, envelope(simulation, 2420, ThermalInputBatch.NO_ADMISSIONS, producer));
            helper.assertTrue(simulation.fixed.hasSection(section), "A real capacity release admits the waiting base");
            helper.assertTrue(simulation.fixed.hasSection(secondSection), "The other waiting Section publishes through the same admission chain");
            double producerTail = pendingProducer(producer, simulation, new net.minecraft.world.level.ChunkPos(expansion).toLong(), 2420);
            near(helper, (2420 + 2400) / 20.0 * power,
                    simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ() + producerTail,
                    "The unoffered producer tail still owns every J while its earlier windows retire");
            for (int flush = 0; flush < 4; flush++)
                RegionalEngineTestDriver.submit(helper, simulation.engine, envelope(simulation, 2420, ThermalInputBatch.NO_ADMISSIONS, producer));
            near(helper, (2420 + 2400) / 20.0 * power, simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ(),
                    "Delayed source windows retain all J and rebase once to the current numerical clock");
            FHMain.LOGGER.info("THERMAL_FIXED_SATURATION,oldBaseThrough=2420,waitingCuts=120,maxInputBytes={},admissionRefusals={},producerTailBeforeNextSealJ={},resumed=true,energyJ={}",
                    maxInputBytes, refusals, producerTail, simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ());
        } finally {
            for (var fire : new BlockPos[] {oldFire, newFire}) level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState());
            MinecraftThermalInput.closeActiveLevel(level);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "fixed_saturation_local", timeoutTicks = 600)
    public static void waitingGrowthKeepsItsExistingBaseAndARemoteBaseAdvancing(GameTestHelper helper) {
        var level = helper.getLevel(); var origin = new BlockPos(282624, 192, 282624); var neighbour = origin.east(64);
        var firstFire = origin.offset(1, 0, 1); var secondFire = neighbour.offset(1, 0, 1);
        for (var base : new BlockPos[] {origin, neighbour})
            for (int x = 0; x < 8; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
                level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
        for (var fire : new BlockPos[] {firstFire, secondFire})
            level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        MinecraftThermalInput.closeActiveLevel(level);
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0, false);
                var producer = new SpatialSourceTimeline(simulation.budget.createCategory(), 16)) {
            long first = SectionPos.asLong(origin), second = SectionPos.asLong(neighbour);
            var page = new ThermalPageHandle(second, 1); simulation.pages.put(second, page);
            byte[] sky = new byte[256]; java.util.Arrays.fill(sky, (byte) 16);
            var admission = new ThermalInputBatch.PageAdmission(page, 0, 1, 1,
                    simulation.capture.captureBricks(second, null, simulation.capture.unresolvedPage(), 1), 0, sky, null, null);
            register(helper, simulation, producer, firstFire, 0); register(helper, simulation, producer, secondFire, 0);
            RegionalEngineTestDriver.submit(helper, simulation.engine, envelope(simulation, 0, new ThermalInputBatch.PageAdmission[] {admission}, producer));
            RegionalEngineTestDriver.submit(helper, simulation.engine, envelope(simulation, 20, ThermalInputBatch.NO_ADMISSIONS, producer));
            double power = simulation.profiles.tuning().campfire().ratedPowerW() * simulation.profiles.tuning().campfire().port(0).powerShare();
            var before = new TiledThermalField.Sample();
            helper.assertTrue(simulation.fixed.read(secondFire.above().asLong(), false, before), "Read the real local base before its pending expansion");
            double beforeC = before.temperatureC();
            try (var occupied = simulation.budget.tryReserve(simulation.budget.limitBytes() - simulation.budget.usedBytes() - 2048)) {
                helper.assertTrue(occupied != null, "Fill admission capacity after both old sources are active");
                var growth = new ThermalInputBatch.PageResidencyUpdate(page, page.liveGeometryRevision(), 3, 1,
                        simulation.capture.captureBricks(second, null, simulation.capture.unresolvedPage(), 3), null, null);
                simulation.engine.offerInput(new ThermalInputEnvelope(new ThermalInputBatch(1, ++simulation.sequence, 20,
                        ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RETIREMENTS,
                        new ThermalInputBatch.PageResidencyUpdate[] {growth}, ResolvedGeometryBatch.EMPTY,
                        ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN), producer.seal(20)));
                pump(helper, simulation);
                for (int cut = 2; cut <= 41; cut++) {
                    long tick = 20L * cut;
                    simulation.engine.offerInput(envelope(simulation, tick, ThermalInputBatch.NO_ADMISSIONS, producer));
                    pump(helper, simulation);
                    helper.assertTrue(simulation.fixed.sectionSampleTick(first) == tick && simulation.fixed.sectionSampleTick(second) == tick,
                            "Waiting numerical storage leaves both existing bases at their real physical time");
                    helper.assertTrue(simulation.fixed.sectionCellCount(second) == 64,
                            "Continued physics does not admit the unpaid additional Brick");
                    helper.assertTrue(simulation.fixed.read(secondFire.above().asLong(), false, before) && before.temperatureC() > beforeC,
                            "The existing base actually exchanges heat instead of only updating its sample clock");
                    near(helper, 2 * tick / 20.0 * power, simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ(),
                            "Both existing sources keep delivering their current input exactly once");
                }
            }
            for (int flush = 0; flush < 4; flush++)
                RegionalEngineTestDriver.submit(helper, simulation.engine, envelope(simulation, 820, ThermalInputBatch.NO_ADMISSIONS, producer));
            helper.assertTrue(simulation.fixed.sectionCellCount(second) == 128 && simulation.fixed.sectionSampleTick(second) == 820,
                    "Capacity release adds the prepaid Brick to the continuously advancing old base");
            near(helper, 82 * power, simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ(), "Both sources' elapsed input is delivered exactly once");
            FHMain.LOGGER.info("THERMAL_FIXED_LOCAL_SATURATION,existingBaseThrough=820,remoteBaseThrough=820,waitingCuts=40,growthAdmitted=true,conservedJ={}",
                    simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ());
        } finally {
            for (var fire : new BlockPos[] {firstFire, secondFire}) level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState());
            MinecraftThermalInput.closeActiveLevel(level);
        }
        helper.succeed();
    }

    private static double pendingProducer(SpatialSourceTimeline producer, ThermalMaterialBehaviorGameTests.Simulation simulation, long chunk, long tick) {
        double[] pending = {0};
        try (var capture = producer.capture(tick)) {
            capture.pending(chunk, simulation.engine::sourcePortDestination, (target, from, to, joules) -> pending[0] += joules);
        }
        return pending[0];
    }

    @GameTest(template = "phase0a_empty", batch = "fixed_chunk_input_concurrency", timeoutTicks = 300)
    public static void chunkPresenceCanBeReadWhileTheCoordinatorReceivesAndRetiresInput(GameTestHelper helper) throws Exception {
        var budget = new com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget(4L << 20);
        var store = new com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatStore();
        var position = new BlockPos(290816, 192, 290816);
        long chunk = new net.minecraft.world.level.ChunkPos(position).toLong();
        var target = new com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput.Target(position.asLong(), (byte) 2, -1, 0, 0, 0, 0, 0);
        var failure = new java.util.concurrent.atomic.AtomicReference<Throwable>();
        var entered = new java.util.concurrent.CountDownLatch(1);
        var finish = new java.util.concurrent.CountDownLatch(1);
        try (var banks = new com.teammoeg.frostedheart.content.climate.thermal.source.SpatialEnergyBanks(budget, 4, 8, 0)) {
            var coordinator = new Thread(() -> {
                try {
                    store.receive(1, target, 0, 20, 1);
                    long receiver = banks.retain(new com.teammoeg.frostedheart.content.climate.thermal.source.SpatialEnergyBanks.Address(position.asLong(), -1, (byte) 2));
                    entered.countDown();
                    if (!finish.await(5, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("The main reader did not observe the retained input");
                    banks.release(receiver);
                    for (int index = 0; index < 25000; index++) {
                        store.receive(1, target, 0, 20, 1);
                        var claim = store.claim(1, 0, 20); claim.prepare(Long.MAX_VALUE); claim.accepted(); store.advanceCleanup(128);
                        receiver = banks.retain(new com.teammoeg.frostedheart.content.climate.thermal.source.SpatialEnergyBanks.Address(position.asLong(), -1, (byte) 2));
                        banks.release(receiver);
                    }
                } catch (Throwable problem) { failure.set(problem); entered.countDown(); }
            }, "thermal-input-presence-regression");
            coordinator.start();
            helper.assertTrue(entered.await(5, java.util.concurrent.TimeUnit.SECONDS), "Start the actual input writer");
            helper.assertTrue(store.hasRetainedInputInChunk(chunk) && banks.hasReceiversInChunk(chunk), "Main sees live input ownership before cleanup");
            finish.countDown();
            long deadline = System.nanoTime() + 10_000_000_000L, reads = 0;
            while (coordinator.isAlive() && System.nanoTime() < deadline) {
                store.hasRetainedInputInChunk(chunk); banks.hasReceiversInChunk(chunk); reads++;
            }
            coordinator.join(1000);
            helper.assertTrue(!coordinator.isAlive() && failure.get() == null, "Concurrent input presence failed: " + failure.get());
            helper.assertTrue(!store.hasRetainedInputInChunk(chunk) && !banks.hasReceiversInChunk(chunk), "Main observes actual final retirement without retaining visited chunks");
            near(helper, 0, store.pendingEnergyJ(), "The real input claims consume every record once");
            FHMain.LOGGER.info("THERMAL_FIXED_CHUNK_INPUT_CONCURRENCY,cycles=25000,mainReads={},retainedAfter=false,pendingJ=0", reads);
        } finally { finish.countDown(); store.clear(); }
        helper.assertTrue(budget.usedBytes() == 0, "Concurrent presence has no retained numerical bank after close"); helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "fixed_capacity_reclamation", timeoutTicks = 600)
    public static void aWaitingAdmissionCannotPreventTheRetirementThatMakesRoomForIt(GameTestHelper helper) {
        var level = helper.getLevel(); var origin = new BlockPos(294912, 192, 294912);
        var retired = origin.east(64); var waiting = origin.east(128);
        var oldFire = origin.offset(1, 0, 1); var newFire = waiting.offset(1, 0, 1);
        for (var base : new BlockPos[] {origin, retired, waiting})
            for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
                level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
        for (var fire : new BlockPos[] {oldFire, newFire})
            level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        MinecraftThermalInput.closeActiveLevel(level);
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0, false, 2);
                var producer = new SpatialSourceTimeline(simulation.budget.createCategory(), 16)) {
            var oldPage = admission(simulation, retired);
            register(helper, simulation, producer, oldFire, 0);
            RegionalEngineTestDriver.submit(helper, simulation.engine, envelope(simulation, 0,
                    new ThermalInputBatch.PageAdmission[] {oldPage}, producer));
            RegionalEngineTestDriver.submit(helper, simulation.engine, envelope(simulation, 20, ThermalInputBatch.NO_ADMISSIONS, producer));
            var pending = admission(simulation, waiting);
            register(helper, simulation, producer, newFire, 20);
            simulation.engine.offerInput(envelope(simulation, 20, new ThermalInputBatch.PageAdmission[] {pending}, producer));
            pump(helper, simulation);
            helper.assertTrue(simulation.fixed.tileCount() == 2 && !simulation.fixed.hasSection(SectionPos.asLong(waiting)),
                    "The third Page waits at the configured two-Page limit");
            simulation.tick = 40;
            simulation.engine.offerInput(new ThermalInputEnvelope(new ThermalInputBatch(1, ++simulation.sequence, 40,
                    ThermalInputBatch.NO_ADMISSIONS, new ThermalInputBatch.PageRetirement[] {new ThermalInputBatch.PageRetirement(oldPage.page())},
                    ThermalInputBatch.NO_RESIDENCY_UPDATES, ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_ENVIRONMENT_UPDATES,
                    ThermalInputBatch.NO_PHASE_ACKS, Double.NaN), producer.seal(40)));
            pump(helper, simulation);
            helper.assertTrue(!simulation.fixed.hasSection(SectionPos.asLong(retired)) && simulation.fixed.hasSection(SectionPos.asLong(waiting)),
                    "Retirement must bypass a waiting admission and make its actual Page slot available");
            for (int flush = 0; flush < 4; flush++)
                RegionalEngineTestDriver.submit(helper, simulation.engine, envelope(simulation, 40, ThermalInputBatch.NO_ADMISSIONS, producer));
            double power = simulation.profiles.tuning().campfire().ratedPowerW() * simulation.profiles.tuning().campfire().port(0).powerShare();
            near(helper, 3 * power, simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ(),
                    "The unretired source and the newly admitted source each keep their original elapsed J exactly once");
            helper.assertTrue(simulation.fixed.tileCount() == 2, "Reclamation does not raise the configured live Page count");
            var neverInstalled = origin.east(192);
            for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
                level.setBlockAndUpdate(neverInstalled.offset(x, y, z), Blocks.AIR.defaultBlockState());
            var cancelled = admission(simulation, neverInstalled);
            simulation.engine.offerInput(envelope(simulation, 40, new ThermalInputBatch.PageAdmission[] {cancelled}, producer));
            pump(helper, simulation);
            helper.assertTrue(!simulation.fixed.hasSection(cancelled.page().sectionKey()), "The next owner also waits at the unchanged Page limit");
            simulation.tick = 60;
            simulation.engine.offerInput(new ThermalInputEnvelope(new ThermalInputBatch(1, ++simulation.sequence, 60,
                    ThermalInputBatch.NO_ADMISSIONS, new ThermalInputBatch.PageRetirement[] {new ThermalInputBatch.PageRetirement(cancelled.page())},
                    ThermalInputBatch.NO_RESIDENCY_UPDATES, ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_ENVIRONMENT_UPDATES,
                    ThermalInputBatch.NO_PHASE_ACKS, Double.NaN), producer.seal(60)));
            pump(helper, simulation);
            helper.assertTrue(!simulation.fixed.hasSection(cancelled.page().sectionKey()) && !simulation.engine.hasWork(),
                    "A never-installed Page can retire without first obtaining the slot it could not get");
            near(helper, 5 * power, simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ(), "The cancelled empty owner leaves both live source timelines intact");
            FHMain.LOGGER.info("THERMAL_FIXED_CAPACITY_RECLAMATION,limit=2,thirdWaited=true,retirementBypassed=true,finalPages=2,finalTick={},energyJ={}",
                    simulation.fixed.acceptedTick(), simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ());
        } finally {
            for (var fire : new BlockPos[] {oldFire, newFire}) level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState());
            MinecraftThermalInput.closeActiveLevel(level);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "fixed_capacity_maintenance", timeoutTicks = 600)
    public static void oldEditsAndTheWaitingPagesOwnRetirementFinishAtTheMemoryTarget(GameTestHelper helper) throws Exception {
        var level = helper.getLevel(); var origin = new BlockPos(299008, 192, 299008); var growing = origin.east(64);
        var fire = origin.offset(1, 0, 1); var wall = origin.offset(3, 1, 2);
        for (var base : new BlockPos[] {origin, growing})
            for (int x = 0; x < 8; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
                level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        MinecraftThermalInput.closeActiveLevel(level);
        var owner = new com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget[1];
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0, false);
                var producer = new SpatialSourceTimeline(simulation.budget.createCategory(), 16)) {
            owner[0] = simulation.budget;
            var oldPage = admission(simulation, growing); long section = oldPage.page().sectionKey();
            register(helper, simulation, producer, fire, 0);
            RegionalEngineTestDriver.submit(helper, simulation.engine, envelope(simulation, 0,
                    new ThermalInputBatch.PageAdmission[] {oldPage}, producer));
            RegionalEngineTestDriver.submit(helper, simulation.engine, envelope(simulation, 20, ThermalInputBatch.NO_ADMISSIONS, producer));
            try (var occupied = simulation.budget.tryReserve(simulation.budget.limitBytes() - simulation.budget.usedBytes() - 2048)) {
                helper.assertTrue(occupied != null, "Fill optional admission capacity while the old bases are live");
                var growth = new ThermalInputBatch.PageResidencyUpdate(oldPage.page(), oldPage.page().liveGeometryRevision(), 3, 1,
                        simulation.capture.captureBricks(section, null, simulation.capture.unresolvedPage(), 3), null, null);
                simulation.engine.offerInput(new ThermalInputEnvelope(new ThermalInputBatch(1, ++simulation.sequence, 20,
                        ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RETIREMENTS, new ThermalInputBatch.PageResidencyUpdate[] {growth},
                        ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN), producer.seal(20)));
                pump(helper, simulation);
                helper.assertTrue(simulation.fixed.sectionCellCount(section) == 64, "Growth has not changed the old field while waiting");
                var cancelledPreparation = new java.lang.ref.WeakReference<Object>(ThermalLoadedWorldGameTests.read(simulation.engine, "fixedGeometry"));
                helper.assertTrue(cancelledPreparation.get() != null, "Observe the actual private preparation before maintenance makes it yield");
                var change = simulation.change(wall, Blocks.STONE.defaultBlockState(), ResolvedGeometryBatch.MaterialChanges.REPLACE);
                simulation.tick = 40;
                simulation.engine.offerInput(new ThermalInputEnvelope(new ThermalInputBatch(1, ++simulation.sequence, 40,
                        ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RETIREMENTS, ThermalInputBatch.NO_RESIDENCY_UPDATES,
                        change, ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN), producer.seal(40)));
                pump(helper, simulation);
                var sample = new TiledThermalField.Sample();
                helper.assertTrue(simulation.fixed.read(wall.asLong(), true, sample)
                                && sample.materialStateId() == net.minecraft.world.level.block.Block.getId(Blocks.STONE.defaultBlockState()),
                        "A real edit in an existing unrelated base must finish while growth cannot fit");
                helper.assertTrue(simulation.fixed.sectionCellCount(section) == 64, "Maintenance cannot accidentally admit the waiting growth");
                simulation.tick = 60;
                simulation.engine.offerInput(new ThermalInputEnvelope(new ThermalInputBatch(1, ++simulation.sequence, 60,
                        ThermalInputBatch.NO_ADMISSIONS, new ThermalInputBatch.PageRetirement[] {new ThermalInputBatch.PageRetirement(oldPage.page())},
                        ThermalInputBatch.NO_RESIDENCY_UPDATES, ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_ENVIRONMENT_UPDATES,
                        ThermalInputBatch.NO_PHASE_ACKS, Double.NaN), producer.seal(60)));
                pump(helper, simulation);
                helper.assertTrue(!simulation.fixed.hasSection(section), "A waiting private growth must yield to the same Page's later retirement");
                helper.assertTrue(simulation.fixed.acceptedTick() == 60 && !simulation.engine.hasWork(), "Requeued growth cannot restart forever after its owner retires");
                double power = simulation.profiles.tuning().campfire().ratedPowerW() * simulation.profiles.tuning().campfire().port(0).powerShare();
                near(helper, 3 * power, simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ(), "Edit, cancelled preparation and retirement preserve source J once");
                helper.assertTrue(simulation.fixed.read(fire.above().asLong(), false, sample) && sample.temperatureC() > 0,
                        "Keep the real engine and a heated base live during the cancelled-preparation collection check");
                for (int attempt = 0; attempt < 4 && cancelledPreparation.get() != null; attempt++) {
                    System.gc(); Thread.sleep(10);
                }
                helper.assertTrue(cancelledPreparation.get() == null, "A cancelled preparation must be collectible while its engine stays heated");
                FHMain.LOGGER.info("THERMAL_FIXED_CAPACITY_MAINTENANCE,editWhileFull=true,ownRetirementWhileFull=true,pendingGeometry=false,cancelledPreparationCollected=true,engineStillLive=true,diagnosticGc=true,finalTick=60,energyJ={},completionPeakOverTargetBytes={}",
                        simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ(), Math.max(0, simulation.budget.peakUsedBytes() - simulation.budget.limitBytes()));
            }
        } finally {
            level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState()); MinecraftThermalInput.closeActiveLevel(level);
        }
        helper.assertTrue(owner[0] != null && owner[0].usedBytes() == 0, "Cancelled geometry and both original owners release their full accounting");
        helper.succeed();
    }

    private static ThermalInputBatch.PageAdmission admission(ThermalMaterialBehaviorGameTests.Simulation simulation, BlockPos base) {
        long section = SectionPos.asLong(base); var page = new ThermalPageHandle(section, 1);
        simulation.pages.put(section, page);
        byte[] sky = new byte[256]; java.util.Arrays.fill(sky, (byte) 16);
        return new ThermalInputBatch.PageAdmission(page, 0, 1, 1,
                simulation.capture.captureBricks(section, null, simulation.capture.unresolvedPage(), 1), 0, sky, null, null);
    }

    private static void pump(GameTestHelper helper, ThermalMaterialBehaviorGameTests.Simulation simulation) {
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (simulation.engine.hasReadyWork()) {
            if (System.nanoTime() >= deadline) {
                com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceIngress ingress = ThermalLoadedWorldGameTests.read(simulation.engine, "sourceIngress");
                com.teammoeg.frostedheart.content.climate.thermal.topology.TopologyUpdatePlanner planner = ThermalLoadedWorldGameTests.read(simulation.engine, "topologyPlan");
                var sections = new it.unimi.dsi.fastutil.longs.LongOpenHashSet(simulation.pages.keySet());
                Object held = ThermalLoadedWorldGameTests.read(simulation.engine, "fixedPhysicalCuts");
                throw new IllegalStateException("Capacity wait must park without spinning: " + simulation.engine.regionalProgressDescription()
                        + ",held=" + held + ",routing=" + simulation.engine.inputStore().hasReadyRouting()
                        + ",source=" + ingress.firstBlocker(planner.continuationBoundary(sections, true), simulation.tick, simulation.sequence));
            }
            simulation.engine.processWorkRound();
            while (simulation.engine.pollWorkCompletion() != null) { }
        }
    }
    private static ThermalInputEnvelope envelope(ThermalMaterialBehaviorGameTests.Simulation simulation, long tick,
            ThermalInputBatch.PageAdmission[] admissions, SpatialSourceTimeline producer) {
        simulation.tick = tick;
        return new ThermalInputEnvelope(new ThermalInputBatch(1, ++simulation.sequence, tick, admissions,
                ThermalInputBatch.NO_RETIREMENTS, ThermalInputBatch.NO_RESIDENCY_UPDATES, ResolvedGeometryBatch.EMPTY,
                ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN), producer.seal(tick));
    }
    private static void register(GameTestHelper helper, ThermalMaterialBehaviorGameTests.Simulation simulation,
            SpatialSourceTimeline producer, BlockPos position, long tick) {
        var profile = simulation.profiles.tuning().campfire();
        helper.assertTrue(producer.register(new SpatialSourceTimeline.Descriptor(position.asLong(), 1, ThermalSourceMode.POWER_SOURCE,
                position.getX(), position.getY(), position.getZ(), profile.profileId(), WorkerPhysicalSourceBindings.initialPorts(position.asLong(), profile)),
                profile.ratedPowerW(), true, tick), "Register a physical source through the ordinary producer");
    }
    private static void near(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) <= 1e-7 * Math.max(1, Math.abs(expected)), message + ";expected=" + expected + ",actual=" + actual);
    }
}
