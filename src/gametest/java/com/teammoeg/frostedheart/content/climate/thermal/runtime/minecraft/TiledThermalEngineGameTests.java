/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MaterialSectionState;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.PendingHeatCheckpoint;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.RegionalCheckpoint;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputEnvelope;
import com.teammoeg.frostedheart.content.climate.thermal.solver.TiledThermalField;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceMode;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings;
import com.teammoeg.frostedheart.util.mixin.ICampfireExtra;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Existing engine receives original geometry and producer messages; no test drives the numerical core. */
@GameTestHolder("frostedheart_tiled_engine")
@PrefixGameTestTemplate(false)
public final class TiledThermalEngineGameTests {
    @GameTest(template = "phase0a_empty", batch = "fixed_geometry_unload_material", timeoutTicks = 500)
    public static void unloadKeepsTheAlreadyPreparedOrderedGameplayAndColdMassResult(GameTestHelper helper) throws Exception {
        var level = helper.getLevel(); var origin = new BlockPos(311296, 192, 311296); var body = origin.offset(1, 1, 1);
        for (int x = 0; x < 8; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(body, Blocks.OAK_TRAPDOOR.defaultBlockState()); MinecraftThermalInput.closeActiveLevel(level);
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0, false);
                var producer = new SpatialSourceTimeline(simulation.budget.createCategory(), 16)) {
            var material = new MaterialSample(); simulation.material.readMaterial(simulation.slot(body), material);
            simulation.material.applyMaterialControl(simulation.slot(body), material.law().enthalpyAtTemperature(32), material.law().initialBranch(32));
            int trapdoor = simulation.profiles.states().signatureId(Blocks.OAK_TRAPDOOR.defaultBlockState());
            int wood = simulation.profiles.states().signatureId(Blocks.OAK_PLANKS.defaultBlockState());
            int stone = simulation.profiles.states().signatureId(Blocks.STONE.defaultBlockState());
            var woodLaw = simulation.profiles.materials().profileOrNull(simulation.profiles.signatures().materialProfileId(wood)).thermalLaw();
            var stoneLaw = simulation.profiles.materials().profileOrNull(simulation.profiles.signatures().materialProfileId(stone)).thermalLaw();
            double expectedC = 32 * woodLaw.capacityJPerK() / stoneLaw.capacityJPerK();
            var page = simulation.page(body); Object preparation;
            try (var occupied = simulation.budget.tryReserve(simulation.budget.limitBytes() - simulation.budget.usedBytes() - 2048)) {
                helper.assertTrue(occupied != null, "Hold a real growth preparation at admission");
                level.setBlockAndUpdate(body, Blocks.OAK_PLANKS.defaultBlockState());
                level.setBlockAndUpdate(body, Blocks.STONE.defaultBlockState());
                var geometry = new ResolvedGeometryBatch.Builder();
                geometry.addResolvedCenter(page, page.beginGeometryMutation(), local(body), stone);
                geometry.addMaterialChange(page, local(body), trapdoor, wood, ResolvedGeometryBatch.MaterialChanges.GAMEPLAY_TRANSITION);
                geometry.addMaterialChange(page, local(body), wood, stone, ResolvedGeometryBatch.MaterialChanges.MASS_CHANGE);
                var growth = new ThermalInputBatch.PageResidencyUpdate(page, page.liveGeometryRevision(), 3, 1,
                        simulation.capture.captureBricks(page.sectionKey(), null, simulation.capture.unresolvedPage(), 3), null, null);
                simulation.engine.offerInput(new ThermalInputEnvelope(new ThermalInputBatch(1, ++simulation.sequence, 0,
                        ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RETIREMENTS, new ThermalInputBatch.PageResidencyUpdate[] {growth},
                        geometry.buildAndReset(), ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN), producer.seal(0)));
                for (int round = 0; round < 256 && simulation.engine.hasReadyWork(); round++) simulation.engine.processWorkRound();
                preparation = ThermalLoadedWorldGameTests.read(simulation.engine, "fixedGeometry");
                helper.assertTrue(preparation != null && ((Integer) ThermalLoadedWorldGameTests.read(preparation, "stage")) == 1,
                        "Prepare the actual compound migration before its field admission; stage="
                                + (preparation == null ? "none" : ThermalLoadedWorldGameTests.read(preparation, "stage"))
                                + ",progress=" + simulation.engine.regionalProgressDescription());
            }
            var advance = preparation.getClass().getDeclaredMethod("advance", long.class); advance.setAccessible(true);
            helper.assertTrue(!(boolean) advance.invoke(preparation, System.nanoTime()) && !simulation.fixed.idle(),
                    "Yield the real zero-time geometry step after its ordered gameplay value was prepared");
            try (var capture = producer.capture(0)) {
                var ready = simulation.engine.requestCheckpoint(new net.minecraft.world.level.ChunkPos(origin).toLong(), capture,
                        PendingHeatCheckpoint.EMPTY, Double.POSITIVE_INFINITY, RegionalCheckpoint.Purpose.UNLOAD);
                while (!ready.isDone()) simulation.engine.processCheckpointCommandOnMain();
                var checkpoint = ready.join(); var materials = checkpoint.sections().get(0).acceptedMaterials();
                helper.assertTrue(checkpoint.sections().get(0).geometryRevision() == page.liveGeometryRevision(),
                        "The frozen prepared material must carry its own geometry revision through checkpoint projection");
                int saved = materials == null ? -1 : materials.find(local(body));
                helper.assertTrue(saved >= 0 && materials.stateId(saved) == Block.getId(Blocks.STONE.defaultBlockState()), "Capture the actual final body identity");
                near(helper, stoneLaw.enthalpyAtTemperature(expectedC), materials.enthalpyJ(saved),
                        "UNLOAD must keep the ordered temperature-preserving conversion followed by added cold mass");
                var cancelled = checkpoint.finish(false);
                while (!cancelled.isDone()) simulation.engine.processCheckpointCommandOnMain();
            }
            for (int round = 0; round < 256 && simulation.engine.hasReadyWork(); round++) {
                simulation.engine.processWorkRound(); while (simulation.engine.pollWorkCompletion() != null) { }
            }
            var sample = new TiledThermalField.Sample();
            helper.assertTrue(simulation.fixed.read(body.asLong(), true, sample) && !simulation.engine.hasWork(), "The same geometry owner resumes after ticket return");
            near(helper, expectedC, sample.temperatureC(), "Live and saved compound conversions agree");
            near(helper, 0, simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ(), "Snapshot and restart do not duplicate conversion energy");
            FHMain.LOGGER.info("THERMAL_FIXED_GEOMETRY_UNLOAD_MATERIAL,beforeC=32,expectedC={},savedAndLiveC={},orderedMassPreserved=true", expectedC, sample.temperatureC());
        } finally { MinecraftThermalInput.closeActiveLevel(level); }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "fixed_geometry_unload", timeoutTicks = 500)
    public static void unloadDuringPrivateGeometryDoesNotLeaveTheCoordinatorOnACancelledStep(GameTestHelper helper) throws Exception {
        var level = helper.getLevel(); var origin = new BlockPos(307200, 192, 307200); var nextOrigin = origin.east(64);
        var fire = origin.offset(1, 0, 1);
        for (var base : new BlockPos[] {origin, nextOrigin})
            for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
                level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        MinecraftThermalInput.closeActiveLevel(level);
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0, false);
                var producer = new SpatialSourceTimeline(simulation.budget.createCategory(), 16)) {
            register(helper, simulation, producer, fire);
            submit(helper, simulation, producer, 0); submit(helper, simulation, producer, 20);
            long section = net.minecraft.core.SectionPos.asLong(nextOrigin);
            var page = new com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle(section, 1);
            byte[] sky = new byte[256]; java.util.Arrays.fill(sky, (byte) 16);
            var admission = new ThermalInputBatch.PageAdmission(page, 0, 1, 1,
                    simulation.capture.captureBricks(section, null, simulation.capture.unresolvedPage(), 1), 0, sky, null, null);
            Object geometry;
            try (var occupied = simulation.budget.tryReserve(simulation.budget.limitBytes() - simulation.budget.usedBytes() - 2048)) {
                helper.assertTrue(occupied != null, "Pause the real preparation before numerical storage changes");
                simulation.engine.offerInput(new ThermalInputEnvelope(new ThermalInputBatch(1, ++simulation.sequence, 20,
                        new ThermalInputBatch.PageAdmission[] {admission}, ThermalInputBatch.NO_RETIREMENTS, ThermalInputBatch.NO_RESIDENCY_UPDATES,
                        ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN), producer.seal(20)));
                for (int round = 0; round < 256 && simulation.engine.hasReadyWork(); round++) simulation.engine.processWorkRound();
                geometry = ThermalLoadedWorldGameTests.read(simulation.engine, "fixedGeometry");
                helper.assertTrue(geometry != null && ((Integer) ThermalLoadedWorldGameTests.read(geometry, "stage")) == 1,
                        "The actual geometry task must be waiting at its field-admission boundary");
            }
            // End this real coordinator slice after PREPARE, before the zero-time publication.
            var advance = geometry.getClass().getDeclaredMethod("advance", long.class); advance.setAccessible(true);
            helper.assertTrue(!(boolean) advance.invoke(geometry, System.nanoTime()) && !simulation.fixed.idle()
                            && !simulation.fixed.hasPublishedSection(section), "Hold a real private geometry step without publishing its initialized values");
            try (var capture = producer.capture(20)) {
                long chunk = new net.minecraft.world.level.ChunkPos(nextOrigin).toLong();
                var ready = simulation.engine.requestCheckpoint(chunk, capture, PendingHeatCheckpoint.EMPTY, Double.POSITIVE_INFINITY, RegionalCheckpoint.Purpose.UNLOAD);
                while (!ready.isDone()) simulation.engine.processCheckpointCommandOnMain();
                var checkpoint = ready.join();
                helper.assertTrue(checkpoint.sections().isEmpty(), "The precisely unpublished field must not invent saved Air");
                var finished = checkpoint.finish(true);
                while (!finished.isDone()) simulation.engine.processCheckpointCommandOnMain();
            }
            simulation.engine.offerInput(new ThermalInputEnvelope(new ThermalInputBatch(1, ++simulation.sequence, 40,
                    ThermalInputBatch.NO_ADMISSIONS, new ThermalInputBatch.PageRetirement[] {new ThermalInputBatch.PageRetirement(page)},
                    ThermalInputBatch.NO_RESIDENCY_UPDATES, ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_ENVIRONMENT_UPDATES,
                    ThermalInputBatch.NO_PHASE_ACKS, Double.NaN), producer.seal(40)));
            for (int round = 0; round < 256 && simulation.engine.hasReadyWork(); round++) {
                simulation.engine.processWorkRound(); while (simulation.engine.pollWorkCompletion() != null) { }
            }
            helper.assertTrue(simulation.fixed.acceptedTick() == 40 && !simulation.fixed.hasSection(section) && !simulation.engine.hasWork(),
                    "UNLOAD must not leave the geometry owner advancing an already cancelled numerical step: " + simulation.engine.regionalProgressDescription());
            double power = simulation.profiles.tuning().campfire().ratedPowerW() * simulation.profiles.tuning().campfire().port(0).powerShare();
            near(helper, 2 * power, simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ(), "The independent source continues once after geometry cancellation");
            FHMain.LOGGER.info("THERMAL_FIXED_GEOMETRY_UNLOAD,privateBeforeCapture=true,savedSections=0,resumedTick=40,retired=true,energyJ={}",
                    simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ());
        } finally {
            level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState()); MinecraftThermalInput.closeActiveLevel(level);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "tiled_engine_gameplay", timeoutTicks = 500)
    public static void gameplayAndMassConversionsKeepTheOriginalOrderedMaterialRules(GameTestHelper helper) {
        var origin = new BlockPos(274432, 192, 274432); var position = origin.offset(1, 1, 1);
        for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            helper.getLevel().setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(position, Blocks.OAK_TRAPDOOR.defaultBlockState());
        MinecraftThermalInput.closeActiveLevel(helper.getLevel());
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0, false)) {
            var material = new MaterialSample(); simulation.material.readMaterial(simulation.slot(position), material);
            simulation.acceptEnergyJ(simulation.slot(position), material.law().enthalpyAtTemperature(32) - material.enthalpyJ());
            var fixed = new TiledThermalField.Sample(); simulation.fixed.read(position.asLong(), true, fixed);
            int generation = fixed.materialGeneration();
            simulation.publish(simulation.change(position, Blocks.OAK_PLANKS.defaultBlockState(), ResolvedGeometryBatch.MaterialChanges.GAMEPLAY_TRANSITION),
                    ThermalInputBatch.NO_PHASE_ACKS);
            simulation.material.readMaterial(simulation.slot(position), material);
            near(helper, 32, material.temperatureC(), "An actual gameplay material conversion retains temperature instead of resetting to ambient");
            helper.assertTrue(simulation.fixed.read(position.asLong(), true, fixed) && fixed.materialGeneration() == generation,
                    "Gameplay conversion preserves the physical body's identity");
            simulation.publish(simulation.change(position, Blocks.OAK_TRAPDOOR.defaultBlockState(), ResolvedGeometryBatch.MaterialChanges.GAMEPLAY_TRANSITION),
                    ThermalInputBatch.NO_PHASE_ACKS);
            var page = simulation.page(position); var changes = new ResolvedGeometryBatch.Builder();
            int wood = simulation.profiles.states().signatureId(Blocks.OAK_PLANKS.defaultBlockState());
            int trapdoor = simulation.profiles.states().signatureId(Blocks.OAK_TRAPDOOR.defaultBlockState());
            int stone = simulation.profiles.states().signatureId(Blocks.STONE.defaultBlockState());
            var woodLaw = simulation.profiles.materials().profileOrNull(simulation.profiles.signatures().materialProfileId(wood)).thermalLaw();
            var stoneLaw = simulation.profiles.materials().profileOrNull(simulation.profiles.signatures().materialProfileId(stone)).thermalLaw();
            helper.assertTrue(stoneLaw.capacityJPerK() > woodLaw.capacityJPerK(), "The final mass change adds colder mass");
            helper.getLevel().setBlockAndUpdate(position, Blocks.OAK_PLANKS.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(position, Blocks.STONE.defaultBlockState());
            changes.addResolvedCenter(page, page.beginGeometryMutation(), local(position), stone);
            changes.addMaterialChange(page, local(position), trapdoor, wood, ResolvedGeometryBatch.MaterialChanges.GAMEPLAY_TRANSITION);
            changes.addMaterialChange(page, local(position), wood, stone, ResolvedGeometryBatch.MaterialChanges.MASS_CHANGE);
            simulation.publish(changes.buildAndReset(), ThermalInputBatch.NO_PHASE_ACKS);
            simulation.material.readMaterial(simulation.slot(position), material);
            double expected = 32 * woodLaw.capacityJPerK() / stoneLaw.capacityJPerK();
            near(helper, expected, material.temperatureC(), "A later mass change uses the preceding converted material, in the original event order");
            near(helper, 0, simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ(), "Conversions account their actual energy changes once");
            FHMain.LOGGER.info("THERMAL_FIXED_GAMEPLAY_CONVERSION,gameplayC=32,orderedMassC={},expectedMassC={},bodyIdentityPreserved=true", material.temperatureC(), expected);
        } finally { MinecraftThermalInput.closeActiveLevel(helper.getLevel()); }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "fixed_closed_continuation", timeoutTicks = 600)
    public static void queuedContinuationFinishesAfterItsLifecycleOwnerHasClosed(GameTestHelper helper) throws ReflectiveOperationException, InterruptedException {
        var level = helper.getLevel(); var origin = new BlockPos(203776, 192, 203776);
        level.setBlockAndUpdate(origin.offset(1, 1, 1), Blocks.STONE.defaultBlockState());
        MinecraftThermalInput.closeActiveLevel(level);
        var failures = new java.util.concurrent.atomic.AtomicInteger();
        var logDrained = new java.util.concurrent.CountDownLatch(1);
        var logger = (org.apache.logging.log4j.core.Logger) org.apache.logging.log4j.LogManager.getRootLogger();
        var existingAppenders = java.util.Map.copyOf(logger.getAppenders());
        var appender = new org.apache.logging.log4j.core.appender.AbstractAppender("thermal-closed-continuation-test", null,
                org.apache.logging.log4j.core.layout.PatternLayout.createDefaultLayout(), false,
                org.apache.logging.log4j.core.config.Property.EMPTY_ARRAY) {
            @Override public void append(org.apache.logging.log4j.core.LogEvent event) {
                String message = event.getMessage().getFormattedMessage();
                if ("Uncaught thermal worker task failure".equals(message)) failures.incrementAndGet();
                if ("THERMAL_CLOSED_CONTINUATION_LOG_BARRIER".equals(message)) logDrained.countDown();
            }
        };
        appender.start(); logger.addAppender(appender);
        var budget = new com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget[1];
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0, false)) {
            budget[0] = simulation.budget;
            var constructor = com.teammoeg.frostedheart.content.climate.thermal.runtime.async.ThermalWorkerPool.class.getDeclaredConstructor(int.class);
            constructor.setAccessible(true); var pool = constructor.newInstance(1);
            try {
                var mailbox = new com.teammoeg.frostedheart.content.climate.thermal.runtime.async.ThermalDimensionMailbox(pool, simulation.engine);
                synchronized (mailbox) {
                    helper.assertTrue(mailbox.submit(envelope(simulation, 20, ResolvedGeometryBatch.EMPTY, new SpatialSourceTimeline.Window[0])),
                            "Queue the actual coordinator on the real pool before lifecycle close wins ownership");
                    mailbox.close();
                    // Execute the same lifecycle callback while the queued coordinator cannot claim this monitor.
                    var close = mailbox.getClass().getDeclaredMethod("closeEngine"); close.setAccessible(true); close.invoke(mailbox);
                }
                var drained = new java.util.concurrent.CountDownLatch(1);
                pool.executeContinuation(drained::countDown);
                helper.assertTrue(drained.await(5, java.util.concurrent.TimeUnit.SECONDS), "The queued coordinator and its FIFO barrier must execute");
                FHMain.LOGGER.info("THERMAL_CLOSED_CONTINUATION_LOG_BARRIER");
                helper.assertTrue(logDrained.await(5, java.util.concurrent.TimeUnit.SECONDS), "Observe worker diagnostics through the logging barrier");
                helper.assertTrue(failures.get() == 0 && !mailbox.workPending() && mailbox.peekCompletion() == null,
                        "A closed queued continuation must finish without a task error or resurrecting work: errors=" + failures.get());
            } finally { pool.close(); }
        } catch (ReflectiveOperationException | InterruptedException | RuntimeException | Error failure) {
            FHMain.LOGGER.error("THERMAL_CLOSED_CONTINUATION_CONTROL_FAILURE", failure); throw failure;
        } finally {
            logger.removeAppender(appender); appender.stop(); MinecraftThermalInput.closeActiveLevel(level);
        }
        helper.assertTrue(logger.getAppenders().entrySet().containsAll(existingAppenders.entrySet()),
                "The diagnostic appender must preserve every existing log destination");
        helper.assertTrue(budget[0].usedBytes() == 0, "The queued close returns all real engine and fixture credits");
        FHMain.LOGGER.info("THERMAL_CLOSED_QUEUED_CONTINUATION,taskErrors=0,closed=true,creditsReturned=true");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "fixed_running_failure_ack", timeoutTicks = 600)
    public static void terminalAcknowledgementWaitsForTheExecutingCheckpointTurn(GameTestHelper helper) throws Exception {
        var level = helper.getLevel(); var origin = new BlockPos(204800, 192, 204800);
        level.setBlockAndUpdate(origin.offset(1, 1, 1), Blocks.STONE.defaultBlockState());
        MinecraftThermalInput.closeActiveLevel(level);
        var release = new java.util.concurrent.CountDownLatch(1);
        var blocked = new java.util.concurrent.CountDownLatch(1);
        var first = new java.util.concurrent.atomic.AtomicBoolean(true);
        var holdAfterFinish = new java.util.concurrent.atomic.AtomicBoolean();
        var held = new java.util.concurrent.atomic.AtomicBoolean();
        var previous = com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer;
        var budget = new com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget[1];
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0, false);
                var producer = new SpatialSourceTimeline(simulation.budget.createCategory(), 16)) {
            budget[0] = simulation.budget;
            var constructor = com.teammoeg.frostedheart.content.climate.thermal.runtime.async.ThermalWorkerPool.class.getDeclaredConstructor(int.class);
            constructor.setAccessible(true); var pool = constructor.newInstance(2);
            var mailbox = new com.teammoeg.frostedheart.content.climate.thermal.runtime.async.ThermalDimensionMailbox(pool, simulation.engine);
            try {
                com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer =
                        new com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.Observer() {
                    @Override public void fixedRoundFinished(com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine.ThermalDimensionEngine engine) {
                        if (engine != simulation.engine) return;
                        if (first.getAndSet(false)) throw new IllegalStateException("Intentional checkpoint ownership control failure");
                        if (holdAfterFinish.get() && ThermalLoadedWorldGameTests.read(engine, "activeSnapshot") == null && held.compareAndSet(false, true)) {
                            blocked.countDown();
                            try {
                                if (!release.await(10, java.util.concurrent.TimeUnit.SECONDS))
                                    throw new IllegalStateException("Checkpoint ownership control was not released");
                            } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException(interrupted); }
                        }
                    }
                };
                mailbox.submit(envelope(simulation, 20, ResolvedGeometryBatch.EMPTY, new SpatialSourceTimeline.Window[0]));
                long deadline = System.nanoTime() + 5_000_000_000L;
                while (mailbox.peekCompletion() == null && System.nanoTime() < deadline)
                    java.util.concurrent.locks.LockSupport.parkNanos(1_000_000);
                var failure = mailbox.peekCompletion();
                helper.assertTrue(failure != null && failure.status()
                                == com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalCompletion.Status.ENGINE_FAILED,
                        "The real coordinator must publish its actual failure notification");
                try (var capture = producer.capture(20)) {
                    var ready = simulation.engine.requestCheckpoint(new net.minecraft.world.level.ChunkPos(origin).toLong(), capture,
                            PendingHeatCheckpoint.EMPTY, Double.POSITIVE_INFINITY, RegionalCheckpoint.Purpose.SAVE);
                    mailbox.controlChanged(); var checkpoint = ready.get(5, java.util.concurrent.TimeUnit.SECONDS);
                    holdAfterFinish.set(true);
                    var finished = checkpoint.finish(false); mailbox.controlChanged();
                    helper.assertTrue(blocked.await(5, java.util.concurrent.TimeUnit.SECONDS) && finished.isDone(),
                            "Hold the actual executing turn after its checkpoint finish receipt, before RUNNING ownership is released");
                    mailbox.acknowledgeCompletion(failure.effectSequence());
                    var lifecycleDrained = new java.util.concurrent.CountDownLatch(1);
                    pool.executeLifecycle(lifecycleDrained::countDown);
                    helper.assertTrue(lifecycleDrained.await(5, java.util.concurrent.TimeUnit.SECONDS), "The other worker executes the real lifecycle queue");
                    helper.assertTrue(simulation.query.fixedState() != null && simulation.fixed.hasPublishedSection(SectionPos.asLong(origin)),
                            "Lifecycle cleanup must wait until the executing checkpoint turn releases its engine");
                    release.countDown();
                    deadline = System.nanoTime() + 5_000_000_000L;
                    while (mailbox.workPending() && System.nanoTime() < deadline)
                        java.util.concurrent.locks.LockSupport.parkNanos(1_000_000);
                    helper.assertTrue(!mailbox.workPending() && simulation.fixed.tileCount() == 0,
                            "The executing owner closes normally after release");
                }
            } finally {
                release.countDown();
                com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer = previous;
                synchronized (mailbox) { if (!(Boolean) ThermalLoadedWorldGameTests.read(mailbox, "engineClosed")) mailbox.close(); }
                pool.close();
            }
        } finally {
            release.countDown(); com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer = previous;
            MinecraftThermalInput.closeActiveLevel(level);
        }
        helper.assertTrue(budget[0].usedBytes() == 0, "Failure ACK, checkpoint and lifecycle return all engine credits");
        FHMain.LOGGER.info("THERMAL_RUNNING_FAILURE_ACK,acceptedVisibleWhileRunning=true,closedAfterRelease=true,creditsReturned=true");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "fixed_worker_failure", timeoutTicks = 600)
    public static void failedFixedWorkerServicesCheckpointsWithoutResumingOrdinaryWork(GameTestHelper helper) {
        var level = helper.getLevel(); var origin = new BlockPos(199680, 192, 199680); var fire = origin.offset(1, 0, 1);
        for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        MinecraftThermalInput.closeActiveLevel(level);
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0, false);
                var producer = new SpatialSourceTimeline(simulation.budget.createCategory(), 16)) {
            register(helper, simulation, producer, fire);
            submit(helper, simulation, producer, 0); submit(helper, simulation, producer, 20);
            double acceptedJ = simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ();
            simulation.engine.offerInput(envelope(simulation, 40, ResolvedGeometryBatch.EMPTY, producer.seal(40)));
            long deadline = System.nanoTime() + 10_000_000_000L;
            while (simulation.fixed.idle()) {
                helper.assertTrue(System.nanoTime() < deadline, "Begin a real fixed claim before failure recovery");
                simulation.engine.processWorkRound();
            }
            simulation.engine.beginFailureRecovery();
            helper.assertTrue(simulation.fixed.idle() && simulation.fixed.acceptedTick() == 20,
                    "Failure cancels only private physical work");
            helper.assertTrue(!simulation.engine.hasReadyWork() && !simulation.engine.hasWork(),
                    "A failed fixed worker must wait for checkpoint control instead of retrying ordinary work");
            for (int round = 0; round < 8; round++) simulation.engine.processWorkRound();
            helper.assertTrue(simulation.fixed.idle() && simulation.fixed.acceptedTick() == 20,
                    "Even a queued continuation cannot advance the failed generation");
            near(helper, acceptedJ, simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ(),
                    "Failure recovery keeps the last Accepted energy");
            try (var capture = producer.capture(40)) {
                var ready = simulation.engine.requestCheckpoint(new net.minecraft.world.level.ChunkPos(origin).toLong(), capture,
                        PendingHeatCheckpoint.EMPTY, Double.POSITIVE_INFINITY, RegionalCheckpoint.Purpose.UNLOAD);
                while (!ready.isDone()) {
                    helper.assertTrue(System.nanoTime() < deadline, "The failed fixed worker must still serve native checkpoint control");
                    simulation.engine.processCheckpointCommandOnMain();
                }
                var checkpoint = ready.join();
                double power = simulation.profiles.tuning().campfire().ratedPowerW() * simulation.profiles.tuning().campfire().port(0).powerShare();
                helper.assertTrue(checkpoint.sections().size() == 1 && checkpoint.sections().get(0).sampleTick() == 20,
                        "Recovery saves the actual prior Accepted field");
                near(helper, power, sum(checkpoint.pending()), "The cancelled claim is saved exactly once");
                var finished = checkpoint.finish(true);
                while (!finished.isDone()) {
                    helper.assertTrue(System.nanoTime() < deadline, "Recovery completes input export without normal computation");
                    simulation.engine.processCheckpointCommandOnMain();
                }
                finished.join();
                near(helper, 0, simulation.engine.inputStore().pendingEnergyJ(), "The real unload exports the saved claim");
                helper.assertTrue(!simulation.engine.hasReadyWork() && !simulation.engine.hasWork(),
                        "Old input and deferred cleanup cannot keep a failed worker runnable");
                FHMain.LOGGER.info("THERMAL_FIXED_FAILURE_CHECKPOINT,acceptedTick=20,failedContinuationAdvances=0,savedPendingJ={},exported=true", power);
            }
        } finally {
            level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState()); MinecraftThermalInput.closeActiveLevel(level);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "fixed_partial_claim", timeoutTicks = 600)
    public static void partialStreamedClaimSavesAndReturnsAllJOnCapacityRefusal(GameTestHelper helper) throws ReflectiveOperationException {
        var level = helper.getLevel(); var origin = new BlockPos(176128, 192, 176128);
        var fires = new BlockPos[] {origin.offset(1, 0, 1), origin.offset(2, 0, 2)};
        for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
        for (var fire : fires) level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        MinecraftThermalInput.closeActiveLevel(level);
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0, false);
                var producer = new SpatialSourceTimeline(simulation.budget.createCategory(), 16)) {
            for (var fire : fires) register(helper, simulation, producer, fire);
            submit(helper, simulation, producer, 0);
            simulation.engine.offerInput(envelope(simulation, 20, ResolvedGeometryBatch.EMPTY, producer.seal(20)));
            com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceIngress ingress = ThermalLoadedWorldGameTests.read(simulation.engine, "sourceIngress");
            long deadline = System.nanoTime() + 10_000_000_000L;
            while (ingress.hasReadyWork()) {
                helper.assertTrue(System.nanoTime() < deadline, "Bind the two real producer windows before numerical preparation");
                ingress.advance(System.nanoTime() + 2_000_000L);
            }
            double power = simulation.profiles.tuning().campfire().ratedPowerW() * simulation.profiles.tuning().campfire().port(0).powerShare();
            double expectedJ = 2 * power;
            near(helper, expectedJ, simulation.engine.inputStore().pendingEnergyJ(), "Both real windows retain their recorded J");
            var prepare = simulation.engine.getClass().getDeclaredMethod("prepareFixedHeat", it.unimi.dsi.fastutil.longs.LongSet.class,
                    long.class, long.class, long.class); prepare.setAccessible(true);
            var scope = it.unimi.dsi.fastutil.longs.LongSets.singleton(net.minecraft.core.SectionPos.asLong(origin));
            var cancel = simulation.engine.getClass().getDeclaredMethod("cancelFixedStep"); cancel.setAccessible(true);
            helper.assertTrue(!(boolean) prepare.invoke(simulation.engine, scope, 0L, 20L, Long.MIN_VALUE), "An expired preparation slice yields after its first target");
            com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatStore.Claim claim = ThermalLoadedWorldGameTests.read(simulation.engine, "fixedClaim");
            near(helper, power, claim.energyJ(), "Only the first source prefix was prepared");
            com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatBuffer banks = ThermalLoadedWorldGameTests.read(simulation.engine, "fixedHeat");
            long chunk = new net.minecraft.world.level.ChunkPos(origin).toLong();
            double[] staged = {0}; banks.pendingInChunk(chunk, (target, joules, since) -> staged[0] += joules);
            near(helper, power, staged[0], "The first target has a private bank copy before the numerical step starts");
            try (var capture = producer.capture(20)) {
                var ready = simulation.engine.requestCheckpoint(chunk, capture, PendingHeatCheckpoint.EMPTY, Double.POSITIVE_INFINITY, RegionalCheckpoint.Purpose.SAVE);
                while (!ready.isDone()) {
                    helper.assertTrue(System.nanoTime() < deadline, "SAVE may inspect a partially prepared input without advancing it");
                    simulation.engine.processCheckpointCommandOnMain();
                }
                near(helper, expectedJ, sum(ready.join().pending()), "SAVE counts the retained source J once, excluding its private bank copy");
                helper.assertTrue(simulation.fixed.idle() && simulation.fixed.acceptedTick() == 0
                        && ThermalLoadedWorldGameTests.read(simulation.engine, "fixedClaim") == claim, "SAVE preserves the partial claim and Accepted frame");
                var finished = simulation.engine.cancelCheckpoint(ready);
                while (!finished.isDone()) {
                    helper.assertTrue(System.nanoTime() < deadline, "The partial-input SAVE releases its real ownership receipt");
                    simulation.engine.processCheckpointCommandOnMain();
                }
            }
            cancel.invoke(simulation.engine);
            staged[0] = 0; banks.pendingInChunk(chunk, (target, joules, since) -> staged[0] += joules);
            near(helper, 0, staged[0], "Cancellation removes the private bank copy");
            near(helper, expectedJ, simulation.engine.inputStore().pendingEnergyJ(), "Cancellation returns every source J");

            Object[] addresses = ThermalLoadedWorldGameTests.read(banks, "addresses");
            for (int index = 0; index < addresses.length - 1; index++) {
                helper.assertTrue(banks.add(new com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatBuffer.Address(
                        origin.offset(512 + index, 1, 0).asLong(), -1, 0), 1), "Occupy prepaid preparation slots, leaving exactly one available");
            }
            try (var full = simulation.budget.tryReserve(simulation.budget.limitBytes() - simulation.budget.usedBytes())) {
                helper.assertTrue(full != null, "The capacity control owns all remaining optional credit");
                helper.assertTrue(!(boolean) prepare.invoke(simulation.engine, scope, 0L, 20L, System.nanoTime() + 2_000_000_000L), "The second streamed target must refuse growth at full capacity");
                helper.assertTrue(ThermalLoadedWorldGameTests.read(simulation.engine, "fixedClaim") == null, "A refused partial preparation relinquishes its claim");
                staged[0] = 0; banks.pendingInChunk(chunk, (target, joules, since) -> staged[0] += joules);
                near(helper, 0, staged[0], "Capacity refusal rolls back the first staged target");
                near(helper, expectedJ, simulation.engine.inputStore().pendingEnergyJ(), "Capacity refusal retains all unconsumed J");
            } finally { banks.discardPreparation(); }
            submit(helper, simulation, producer, 20);
            near(helper, expectedJ, simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ(), "The same input commits once after capacity returns");
            near(helper, 0, simulation.engine.inputStore().pendingEnergyJ(), "The committed prefix leaves no duplicate source input");
            FHMain.LOGGER.info("THERMAL_FIXED_STREAMED_INPUT,partialJ={},savedJ={},capacityRollback=true,finalTick={},conservedJ={}",
                    power, expectedJ, simulation.fixed.acceptedTick(), simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ());
        } finally {
            for (var fire : fires) level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState());
            MinecraftThermalInput.closeActiveLevel(level);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "tiled_engine_checkpoint", timeoutTicks = 600)
    public static void originalCheckpointQueueCapturesAcceptedAndClaimOnceWhileTheTrialContinues(GameTestHelper helper) {
        var level = helper.getLevel(); var origin = new BlockPos(155648, 192, 155648);
        var fire = origin.offset(1, 0, 1); var wall = origin.offset(3, 1, 2);
        for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        level.setBlockAndUpdate(wall, Blocks.IRON_BLOCK.defaultBlockState());
        MinecraftThermalInput.closeActiveLevel(level);
        var budget = new com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget[1];
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0, false);
                var producer = new SpatialSourceTimeline(simulation.budget.createCategory(), 16)) {
            budget[0] = simulation.budget;
            register(helper, simulation, producer, fire);
            submit(helper, simulation, producer, 0); submit(helper, simulation, producer, 20);
            var sample = new MaterialSample(); simulation.material.readMaterial(simulation.slot(wall), sample);
            simulation.material.applyMaterialControl(simulation.slot(wall), sample.law().enthalpyAtTemperature(32), sample.law().initialBranch(32));
            var point = fire.above(); var before = new QueryPublication.MutableSample();
            helper.assertTrue(simulation.query.tryReadAir(point.getX() + .875, point.getY() + .5, point.getZ() + .5, before), "Read Accepted before a new claim");
            double acceptedQueryC = before.temperatureC();
            var canonical = new TiledThermalField.Sample();
            helper.assertTrue(simulation.fixed.read(point.asLong(), false, canonical), "Read the canonical Accepted cell energy before the claim");
            double acceptedC = canonical.temperatureC();
            simulation.engine.offerInput(envelope(simulation, 40, ResolvedGeometryBatch.EMPTY, producer.seal(40)));
            long deadline = System.nanoTime() + 10_000_000_000L;
            while (simulation.fixed.idle()) {
                helper.assertTrue(System.nanoTime() < deadline, "Start the original engine's next physical trial");
                simulation.engine.processWorkRound();
            }
            try (var capture = producer.capture(40)) {
                var ready = simulation.engine.requestCheckpoint(new net.minecraft.world.level.ChunkPos(origin).toLong(), capture,
                        PendingHeatCheckpoint.EMPTY, Double.POSITIVE_INFINITY, RegionalCheckpoint.Purpose.SAVE);
                while (!ready.isDone()) {
                    helper.assertTrue(System.nanoTime() < deadline, "Main checkpoint control cannot wait for the unfinished numerical trial");
                    simulation.engine.processCheckpointCommandOnMain();
                }
                var checkpoint = ready.join();
                helper.assertTrue(!simulation.fixed.idle() && simulation.fixed.acceptedTick() == 20,
                        "Saving leaves the actual unfinished trial in place and does no numerical catch-up");
                double power = simulation.profiles.tuning().campfire().ratedPowerW() * simulation.profiles.tuning().campfire().port(0).powerShare();
                near(helper, power, sum(checkpoint.pending()), "The store claim and copied bank must appear exactly once in native pending input");
                var section = checkpoint.sections().get(0); var air = section.captureAir(0);
                near(helper, acceptedC, air.sampleC(point.getX() + .875, point.getY() + .5, point.getZ() + .5, 0, 20, 0, 20), "The checkpoint contains canonical Accepted energy, not a derived query vertex");
                helper.assertTrue(simulation.query.tryReadAir(point.getX() + .875, point.getY() + .5, point.getZ() + .5, before), "The unfinished trial leaves the query available");
                near(helper, acceptedQueryC, before.temperatureC(), "Saving leaves the previously published fractional query unchanged");
                var materials = MaterialSectionState.capture(section, new MaterialSectionState.CaptureScratch(), 0).state();
                int savedBody = materials == null ? -1 : materials.find((wall.getX() & 15) | (wall.getZ() & 15) << 4 | (wall.getY() & 15) << 8);
                helper.assertTrue(savedBody >= 0 && materials.stateId(savedBody) == Block.getId(Blocks.IRON_BLOCK.defaultBlockState()),
                        "The existing native material writer receives captured values, not a live numerical slot");
                near(helper, sample.law().enthalpyAtTemperature(32), materials.enthalpyJ(savedBody), "The native material value is the Accepted 32C body");
                while (simulation.fixed.acceptedTick() < 40) {
                    helper.assertTrue(System.nanoTime() < deadline, "A sealed SAVE cannot stall ordinary numerical work");
                    simulation.engine.processWorkRound();
                }
                near(helper, acceptedC, air.sampleC(point.getX() + .875, point.getY() + .5, point.getZ() + .5, 0, 20, 0, 20), "Later commits cannot mutate captured Air");
                near(helper, sample.law().enthalpyAtTemperature(32), materials.enthalpyJ(savedBody), "Later commits cannot mutate captured material H");
                var extra = new com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput.Builder();
                checkpoint.collectAdditionalThrough(60, extra::appendAggregate);
                double extraJ = sum(extra.freeze());
                near(helper, power, extraJ, "Producer tail stays relative to the captured prefix after live acceptance advances");
                var finished = checkpoint.finish(false);
                while (!finished.isDone()) simulation.engine.processCheckpointCommandOnMain();
                helper.assertTrue(checkpoint.sections().isEmpty() && checkpoint.pending().size() == 0,
                        "Finishing releases the captured arrays while the engine stays live");
                FHMain.LOGGER.info("THERMAL_FIXED_NATIVE_SAVE,acceptedTick=20,trialContinuesTo=40,pendingJ={},tailJ={},savedCellMeanC={},originalQueue=true", power, extraJ, acceptedC);
            }
        } finally { level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState()); MinecraftThermalInput.closeActiveLevel(level); }
        helper.assertTrue(budget[0] != null && budget[0].usedBytes() == 0, "Snapshot scratch, payload and engine credits all return on close");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "tiled_engine_checkpoint_unload", timeoutTicks = 600)
    public static void anUnloadCheckpointFreezesOnlyItsChunkAndCancellationResumesIt(GameTestHelper helper) {
        var level = helper.getLevel(); var origin = new BlockPos(159744, 192, 159744);
        var a = origin.offset(1, 0, 1); var b = origin.offset(17, 0, 1);
        for (int x = 0; x < 20; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
        for (var fire : new BlockPos[] {a, b}) level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        MinecraftThermalInput.closeActiveLevel(level);
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 20, 4, 4, 0, false);
                var producer = new SpatialSourceTimeline(simulation.budget.createCategory(), 16)) {
            register(helper, simulation, producer, a); register(helper, simulation, producer, b);
            submit(helper, simulation, producer, 0); submit(helper, simulation, producer, 20);
            try (var capture = producer.capture(20)) {
                long chunk = new net.minecraft.world.level.ChunkPos(a).toLong();
                var ready = simulation.engine.requestCheckpoint(chunk, capture, PendingHeatCheckpoint.EMPTY, Double.POSITIVE_INFINITY, RegionalCheckpoint.Purpose.UNLOAD);
                while (!ready.isDone()) simulation.engine.processCheckpointCommandOnMain();
                var checkpoint = ready.join();
                helper.assertTrue(simulation.fixed.frozen(net.minecraft.core.SectionPos.asLong(a))
                        && !simulation.fixed.frozen(net.minecraft.core.SectionPos.asLong(b)), "Only the unloading chunk is frozen");
                simulation.engine.offerInput(envelope(simulation, 40, ResolvedGeometryBatch.EMPTY, producer.seal(40)));
                long deadline = System.nanoTime() + 10_000_000_000L;
                while (simulation.fixed.acceptedTick() < 40) {
                    helper.assertTrue(System.nanoTime() < deadline, "Other chunks must keep advancing during native unload preparation");
                    simulation.engine.processWorkRound();
                }
                helper.assertTrue(simulation.fixed.sectionSampleTick(net.minecraft.core.SectionPos.asLong(a)) == 20
                        && simulation.fixed.sectionSampleTick(net.minecraft.core.SectionPos.asLong(b)) == 40, "Frozen and live chunks keep their real sample clocks");
                var cancelled = simulation.engine.cancelCheckpoint(ready);
                while (!cancelled.isDone()) simulation.engine.processCheckpointCommandOnMain();
                while (simulation.fixed.frozen(net.minecraft.core.SectionPos.asLong(a)) && simulation.engine.hasReadyWork()) simulation.engine.processWorkRound();
                helper.assertTrue(!simulation.fixed.frozen(net.minecraft.core.SectionPos.asLong(a)), "An invalidated native capture rejoins the same chunk");
                submit(helper, simulation, producer, 60);
                double power = simulation.profiles.tuning().campfire().ratedPowerW() * simulation.profiles.tuning().campfire().port(0).powerShare();
                near(helper, 6 * power, simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ(), "Cancellation preserves both sources and all queued heat exactly once");
                FHMain.LOGGER.info("THERMAL_FIXED_NATIVE_UNLOAD_CANCEL,frozenTick=20,otherChunkTick=40,resumedTick={},conservedJ={},originalQueue=true", simulation.fixed.acceptedTick(), simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ());
            }
            simulation.engine.offerInput(envelope(simulation, 80, ResolvedGeometryBatch.EMPTY, producer.seal(80)));
            long deadline = System.nanoTime() + 10_000_000_000L;
            while (simulation.fixed.idle()) {
                helper.assertTrue(System.nanoTime() < deadline, "Prepare the next actual source claim before unloading");
                simulation.engine.processWorkRound();
            }
            try (var capture = producer.capture(80)) {
                long chunk = new net.minecraft.world.level.ChunkPos(a).toLong();
                var ready = simulation.engine.requestCheckpoint(chunk, capture, PendingHeatCheckpoint.EMPTY, Double.POSITIVE_INFINITY, RegionalCheckpoint.Purpose.UNLOAD);
                while (!ready.isDone()) simulation.engine.processCheckpointCommandOnMain();
                var checkpoint = ready.join(); double pendingJ = sum(checkpoint.pending());
                double power = simulation.profiles.tuning().campfire().ratedPowerW() * simulation.profiles.tuning().campfire().port(0).powerShare();
                near(helper, power, pendingJ, "The unloading chunk owns exactly its one source interval");
                while (simulation.fixed.acceptedTick() < 80) {
                    helper.assertTrue(System.nanoTime() < deadline, "A second chunk still progresses while the first remains sealed");
                    simulation.engine.processWorkRound();
                }
                com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatBuffer banks = ThermalLoadedWorldGameTests.read(simulation.engine, "fixedHeat");
                near(helper, power, banks.committedPendingJoules() + simulation.engine.inputStore().pendingEnergyJ(),
                        "The frozen receiver's unconsumed J remains in the original Store or committed debt, exactly once");
                var finished = checkpoint.finish(true);
                while (!finished.isDone()) simulation.engine.processCheckpointCommandOnMain();
                near(helper, 0, banks.committedPendingJoules(), "Successful native handoff releases the exported bank debt");
                near(helper, 0, simulation.engine.inputStore().pendingEnergyJ(), "No copy of exported input remains in the store");
                com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceLedger ledger = ThermalLoadedWorldGameTests.read(simulation.engine, "sources");
                near(helper, power, ledger.energyBalance().exportedJ(), "The original ledger records the same one export");
                FHMain.LOGGER.info("THERMAL_FIXED_NATIVE_EXPORT,savedPendingJ={},exportedJ={},remainingDebtJ={},otherChunkTick={},originalQueue=true", pendingJ, ledger.energyBalance().exportedJ(), banks.committedPendingJoules(), simulation.fixed.sectionSampleTick(net.minecraft.core.SectionPos.asLong(b)));
            }
        } finally {
            for (var fire : new BlockPos[] {a, b}) level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState());
            MinecraftThermalInput.closeActiveLevel(level);
        }
        helper.succeed();
    }

    private static void register(GameTestHelper helper, ThermalMaterialBehaviorGameTests.Simulation simulation, SpatialSourceTimeline producer, BlockPos fire) {
        var profile = simulation.profiles.tuning().campfire();
        helper.assertTrue(producer.register(new SpatialSourceTimeline.Descriptor(fire.asLong(), 1, ThermalSourceMode.POWER_SOURCE,
                fire.getX(), fire.getY(), fire.getZ(), profile.profileId(), WorkerPhysicalSourceBindings.initialPorts(fire.asLong(), profile)),
                profile.ratedPowerW(), true, 0), "Use the original producer with its real physical profile");
    }

    private static double sum(com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput heat) {
        double result = 0; for (int index = 0; index < heat.size(); index++) result += heat.energyJ(index); return result;
    }

    @GameTest(template = "phase0a_empty", batch = "tiled_engine_late_input", timeoutTicks = 600)
    public static void lateProducerPrefixCancelsTheTrialWithoutDuplicatingItsClaim(GameTestHelper helper) {
        var level = helper.getLevel(); var origin = new BlockPos(135168, 192, 135168);
        var firstFire = origin.offset(1, 0, 1); var secondFire = origin.offset(2, 0, 2);
        for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
        for (var position : new BlockPos[] {firstFire, secondFire}) {
            level.setBlockAndUpdate(position, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
            ((ICampfireExtra) level.getBlockEntity(position)).setLifeTime(20_000);
        }
        MinecraftThermalInput.closeActiveLevel(level);
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0, false);
                var early = new SpatialSourceTimeline(simulation.budget.createCategory(), 16);
                var late = new SpatialSourceTimeline(simulation.budget.createCategory(), 16)) {
            var profile = simulation.profiles.tuning().campfire();
            int index = 0;
            for (var producer : new SpatialSourceTimeline[] {early, late}) {
                var position = index++ == 0 ? firstFire : secondFire;
                helper.assertTrue(producer.register(new SpatialSourceTimeline.Descriptor(position.asLong(), 1, ThermalSourceMode.POWER_SOURCE,
                        position.getX(), position.getY(), position.getZ(), profile.profileId(), WorkerPhysicalSourceBindings.initialPorts(position.asLong(), profile)),
                        profile.ratedPowerW(), true, 0), "Register two real producer streams before delivering either prefix");
            }
            var a = early.seal(0); var b = late.seal(0);
            var initial = java.util.Arrays.copyOf(a, a.length + b.length); System.arraycopy(b, 0, initial, a.length, b.length);
            RegionalEngineTestDriver.submit(helper, simulation.engine, envelope(simulation, 0, ResolvedGeometryBatch.EMPTY, initial));
            double initialJ = simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ();
            simulation.engine.offerInput(envelope(simulation, 20, ResolvedGeometryBatch.EMPTY, early.seal(20)));
            long deadline = System.nanoTime() + 10_000_000_000L;
            while (simulation.fixed.idle()) {
                helper.assertTrue(simulation.engine.hasReadyWork() && System.nanoTime() < deadline, "The engine must claim the first real source prefix");
                simulation.engine.processWorkRound();
            }
            helper.assertTrue(simulation.fixed.acceptedTick() == 0
                            && ThermalLoadedWorldGameTests.read(simulation.engine, "fixedClaim") != null,
                    "Pause between ordinary coordinator rounds after the bank copied the still-unaccepted store claim");
            RegionalEngineTestDriver.submit(helper, simulation.engine, envelope(simulation, 20, ResolvedGeometryBatch.EMPTY, late.seal(20)));
            double expected = 2 * profile.ratedPowerW() * profile.port(0).powerShare();
            double actual = simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ() - initialJ;
            near(helper, expected, actual, "Cancelling a copied source claim and retrying consumes each producer prefix exactly once");
            helper.assertTrue(simulation.fixed.acceptedTick() == 20 && simulation.engine.inputStore().pendingEnergyJ() == 0,
                    "The original engine finishes both input streams without a retained duplicate");
            FHMain.LOGGER.info("THERMAL_FIXED_LATE_INPUT,expectedJ={},actualJ={},sampleTick=20,manualNumericalCalls=0", expected, actual);
        } finally {
            level.setBlockAndUpdate(firstFire, Blocks.AIR.defaultBlockState()); level.setBlockAndUpdate(secondFire, Blocks.AIR.defaultBlockState());
            MinecraftThermalInput.closeActiveLevel(level);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "tiled_engine_retention", timeoutTicks = 1000)
    public static void completedGeometryIsCollectedWhileTheSameEngineContinuesHeating(GameTestHelper helper) {
        var level = helper.getLevel(); var origin = new BlockPos(122880, 192, 122880);
        var stableFire = origin.offset(1, 0, 1); var changingFire = origin.offset(17, 0, 1); var wall = origin.offset(19, 1, 2);
        for (int x = 0; x < 20; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
        for (var position : new BlockPos[] {stableFire, changingFire}) {
            level.setBlockAndUpdate(position, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
            ((ICampfireExtra) level.getBlockEntity(position)).setLifeTime(20_000);
        }
        MinecraftThermalInput.closeActiveLevel(level);
        var completed = new java.util.ArrayList<java.lang.ref.WeakReference<Object>>();
        var previous = ThermalDiagnostics.observer;
        var observer = (ThermalDiagnostics.Observer) java.lang.reflect.Proxy.newProxyInstance(ThermalDiagnostics.Observer.class.getClassLoader(),
                new Class<?>[] {ThermalDiagnostics.Observer.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("geometry") && Boolean.TRUE.equals(arguments[4]))
                        completed.add(new java.lang.ref.WeakReference<>(arguments[0]));
                    try {
                        if (previous != null) return method.invoke(previous, arguments);
                        return java.lang.reflect.InvocationHandler.invokeDefault(proxy, method, arguments == null ? new Object[0] : arguments);
                    } catch (java.lang.reflect.InvocationTargetException failure) { throw failure.getCause(); }
                });
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 20, 4, 4, 0, false);
                var sources = new SpatialSourceTimeline(simulation.budget.createCategory(), 16)) {
            ThermalDiagnostics.observer = observer;
            var profile = simulation.profiles.tuning().campfire();
            for (var position : new BlockPos[] {stableFire, changingFire}) helper.assertTrue(sources.register(
                    new SpatialSourceTimeline.Descriptor(position.asLong(), 1, ThermalSourceMode.POWER_SOURCE, position.getX(), position.getY(), position.getZ(),
                            profile.profileId(), WorkerPhysicalSourceBindings.initialPorts(position.asLong(), profile)), profile.ratedPowerW(), true, 0),
                    "Use actual producer declarations in two real Sections");
            submit(helper, simulation, sources, 0);
            long tick = 0;
            for (int edit = 0; edit < 12; edit++) {
                var state = (edit % 2 == 0 ? Blocks.STONE : Blocks.IRON_BLOCK).defaultBlockState();
                var geometry = simulation.change(wall, state, ResolvedGeometryBatch.MaterialChanges.REPLACE);
                submit(helper, simulation, sources, tick += 20, geometry);
            }
            helper.assertTrue(completed.size() >= 12, "Observe completed real geometry tasks before asking the GC to collect them");
            for (int attempt = 0; attempt < 5; attempt++) {
                System.gc();
                submit(helper, simulation, sources, tick += 20);
                if (completed.stream().allMatch(reference -> reference.get() == null)) break;
            }
            long retained = completed.stream().filter(reference -> reference.get() != null).count();
            helper.assertTrue(retained == 0, "A live fixed engine must not retain completed geometry through callbacks or current state; retained=" + retained);
            var air = new QueryPublication.MutableSample(); var probe = stableFire.above();
            helper.assertTrue(simulation.budget.usedBytes() > 0 && simulation.query.tryReadAir(probe.getX() + .5, probe.getY() + .5, probe.getZ() + .5, air)
                            && air.temperatureC() > 0 && air.sampleTick() == tick,
                    "The same engine and unchanged heated Section continue after completed tasks are collected");
            FHMain.LOGGER.info("THERMAL_FIXED_GEOMETRY_COLLECTION,completed={},retained={},engineStillLive=true,controlSampleTick={},diagnosticGc=true",
                    completed.size(), retained, air.sampleTick());
        } finally {
            ThermalDiagnostics.observer = previous;
            level.setBlockAndUpdate(stableFire, Blocks.AIR.defaultBlockState()); level.setBlockAndUpdate(changingFire, Blocks.AIR.defaultBlockState());
            MinecraftThermalInput.closeActiveLevel(level);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "tiled_engine_source", timeoutTicks = 600)
    public static void originalProducerWindowsDriveTheFixedEngineAndExtinction(GameTestHelper helper) {
        var level = helper.getLevel(); var origin = new BlockPos(94208, 192, 94208); var fire = origin.offset(1, 0, 1);
        level.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, true);
        for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        ((ICampfireExtra) level.getBlockEntity(fire)).setLifeTime(20_000);
        MinecraftThermalInput.closeActiveLevel(level);
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0, false);
                var sources = new SpatialSourceTimeline(simulation.budget.createCategory(), 16)) {
            helper.assertTrue(simulation.query.fixedState() == simulation.fixed
                            && java.util.Arrays.stream(simulation.engine.getClass().getDeclaredFields()).noneMatch(field -> field.getName().equals("regional")),
                    "The engine owns the queried fixed field and has no retired backend selector");
            var profile = simulation.profiles.tuning().campfire();
            helper.assertTrue(sources.register(new SpatialSourceTimeline.Descriptor(fire.asLong(), 1, ThermalSourceMode.POWER_SOURCE,
                            fire.getX(), fire.getY(), fire.getZ(), profile.profileId(), WorkerPhysicalSourceBindings.initialPorts(fire.asLong(), profile)),
                    profile.ratedPowerW(), true, 0), "Use the original producer and the actual physical campfire profile");
            submit(helper, simulation, sources, 0);
            var sample = new QueryPublication.MutableSample(); var probe = fire.above();
            for (int tick = 20; tick <= 120; tick += 20) submit(helper, simulation, sources, tick);
            helper.assertTrue(simulation.query.tryReadAir(probe.getX() + .5, probe.getY() + .5, probe.getZ() + .5, sample)
                            && sample.temperatureC() > .05 && sample.sampleTick() == 120 && sample.airBasisTerms() == 8,
                    "Geometry, input, numerical stepping and query publication run through the original engine");
            double heated = sample.temperatureC();
            sources.enabled(fire.asLong(), false, 120); submit(helper, simulation, sources, 120);
            double stopped = simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ();
            for (int tick = 140; tick <= 240; tick += 20) submit(helper, simulation, sources, tick);
            near(helper, stopped, simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ(),
                    "Extinction stops production while retained heat continues its numerical life");
            helper.assertTrue(simulation.query.tryReadAir(probe.getX() + .5, probe.getY() + .5, probe.getZ() + .5, sample)
                            && sample.sampleTick() == 240, "Ordinary input cuts continue to publish after extinction");
            FHMain.LOGGER.info("THERMAL_FIXED_ENGINE_SOURCE,regionalRuntimeCreated=false,manualNumericalCalls=0,heatedC={},finalSampleTick={},retainedJ={}",
                    heated, sample.sampleTick(), simulation.fixed.totalEnthalpyJ());
        } finally {
            level.setBlockAndUpdate(fire, Blocks.AIR.defaultBlockState()); MinecraftThermalInput.closeActiveLevel(level);
            level.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, false);
        }
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "tiled_engine_material", timeoutTicks = 400)
    public static void originalGeometryAndMaterialControlsReplaceTheSameStateBody(GameTestHelper helper) {
        var level = helper.getLevel(); var origin = new BlockPos(98304, 192, 98304); var matter = origin.offset(1, 1, 1);
        level.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, true);
        for (int x = 0; x < 4; x++) for (int y = 0; y < 4; y++) for (int z = 0; z < 4; z++)
            level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(matter, Blocks.STONE.defaultBlockState()); MinecraftThermalInput.closeActiveLevel(level);
        try (var simulation = new ThermalMaterialBehaviorGameTests.Simulation(helper, origin, 4, 4, 4, 0, false)) {
            var sample = new TiledThermalField.Sample(); simulation.fixed.read(matter.asLong(), true, sample);
            int generation = sample.materialGeneration();
            simulation.acceptEnergyJ(simulation.slot(matter), sample.law().enthalpyAtTemperature(32) - sample.enthalpyJ());
            simulation.publish(ResolvedGeometryBatch.EMPTY, ThermalInputBatch.NO_PHASE_ACKS);
            var material = new MaterialSample();
            helper.assertTrue(simulation.query.tryReadMaterial(simulation.page(matter).currentPublication(), local(matter),
                            Block.getId(Blocks.STONE.defaultBlockState()), material) && material.temperatureC() == 32,
                    "The engine's existing material-control interface publishes the sole fixed state");
            simulation.publish(simulation.change(matter, Blocks.STONE.defaultBlockState(), ResolvedGeometryBatch.MaterialChanges.REPLACE),
                    ThermalInputBatch.NO_PHASE_ACKS);
            helper.assertTrue(simulation.fixed.read(matter.asLong(), true, sample) && sample.materialGeneration() != generation
                            && sample.enthalpyJ() == 0 && sample.requestSequence() == 0,
                    "The original geometry path captures a same-state replacement without old heat or request identity");
            near(helper, 0, simulation.fixed.totalEnthalpyJ() + simulation.fixed.externalEnergyJ(),
                    "External control and removed material remain in one conserved energy account");
            FHMain.LOGGER.info("THERMAL_FIXED_ENGINE_MATERIAL,manualGeometryProjection=0,controlC=32,replacementC={},newIncarnation=true", sample.temperatureC());
        } finally {
            MinecraftThermalInput.closeActiveLevel(level);
            level.setChunkForced(origin.getX() >> 4, origin.getZ() >> 4, false);
        }
        helper.succeed();
    }

    private static int local(BlockPos position) { return (position.getX() & 15) | (position.getZ() & 15) << 4 | (position.getY() & 15) << 8; }
    private static void submit(GameTestHelper helper, ThermalMaterialBehaviorGameTests.Simulation simulation, SpatialSourceTimeline source, long tick) {
        submit(helper, simulation, source, tick, ResolvedGeometryBatch.EMPTY);
    }
    private static void submit(GameTestHelper helper, ThermalMaterialBehaviorGameTests.Simulation simulation, SpatialSourceTimeline source, long tick, ResolvedGeometryBatch geometry) {
        var completion = RegionalEngineTestDriver.submit(helper, simulation.engine, envelope(simulation, tick, geometry, source.seal(tick)));
        helper.assertTrue(completion.status() == com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalCompletion.Status.COMPLETED,
                "The original engine completes fixed input transport");
    }
    private static ThermalInputEnvelope envelope(ThermalMaterialBehaviorGameTests.Simulation simulation, long tick,
            ResolvedGeometryBatch geometry, SpatialSourceTimeline.Window[] windows) {
        var controls = new ThermalInputBatch(1, ++simulation.sequence, tick, ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RETIREMENTS,
                ThermalInputBatch.NO_RESIDENCY_UPDATES, geometry, ThermalInputBatch.NO_ENVIRONMENT_UPDATES,
                ThermalInputBatch.NO_PHASE_ACKS, Double.NaN);
        return new ThermalInputEnvelope(controls, windows);
    }
    private static void near(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) <= 1e-6 * Math.max(1, Math.abs(expected)), message + "; expected=" + expected + ",actual=" + actual);
    }
}
