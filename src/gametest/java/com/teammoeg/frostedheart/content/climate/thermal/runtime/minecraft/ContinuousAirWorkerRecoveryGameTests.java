/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.PendingHeatCheckpoint;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.RegionalCheckpoint;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine.ThermalDimensionEngine;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.solver.TiledThermalField;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatBuffer;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceLedger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Real fixed-worker faults across claim, geometry and save entry; native restart, disk readback and live collection. */
@GameTestHolder("frostedheart_worker_recovery")
@PrefixGameTestTemplate(false)
public final class ContinuousAirWorkerRecoveryGameTests {
    private enum FailurePoint {
        CLAIM("CLAIM"), TOPOLOGY("TOPOLOGY"), BEFORE_PUBLICATION("BEFORE_PUBLICATION"),
        PUBLISHED("PUBLISHED"), CAPTURE_QUEUED("BEFORE_PUBLICATION");
        final String phase;
        FailurePoint(String phase) { this.phase = phase; }
    }
    private static volatile Fault active;
    private static Runnable restoreWorld;
    private static final class Fault {
        final BlockPos point;
        final BlockPos material;
        final FailurePoint site;
        final boolean compound;
        final long section;
        volatile boolean armed, recreated, captured, materialPrepared, diskCapture;
        volatile long generation = -1;
        volatile long snapshotRevision = -1;
        volatile long observations, energyFailures;
        volatile double maximumImbalanceJ, claimedChunkJ, expectedJ, savedJ, acceptedC, savedC;
        volatile double materialJ, materialC, savedMaterialJ, expectedMaterialJ, checkpointMaterialJ, massNaturalC;
        volatile double diskExpectedMaterialJ = Double.NaN;
        volatile String failure;
        volatile int injected;
        java.lang.ref.WeakReference<ThermalDimensionEngine> failedEngine;
        java.lang.ref.WeakReference<ThermalDimensionEngine> currentEngine;
        java.lang.ref.WeakReference<TiledThermalField> failedField;
        Runnable queuedCapture;
        Fault(BlockPos point, BlockPos material, FailurePoint site, boolean compound) {
            this.point = point; this.material = material; this.site = site; this.compound = compound; section = SectionPos.asLong(point);
        }
        void fail(String message) { if (failure == null) failure = message; }
    }

    /** Runs under the actual coordinator's executing ownership; only the deliberate exception changes control flow. */
    public static void fixedRoundFinished(ThermalDimensionEngine engine) {
        var fault = active;
        if (fault == null) return;
        TiledThermalField field = read(engine, "fixedField");
        if (field == null || !field.hasPublishedSection(fault.section)) return;
        if (fault.currentEngine == null || fault.currentEngine.get() != engine)
            fault.currentEngine = new java.lang.ref.WeakReference<>(engine);
        if (!fault.materialPrepared && field.idle()) {
            com.teammoeg.frostedheart.content.climate.thermal.topology.WorkerPageStore pages = read(engine, "pages");
            var page = pages.pageAt(fault.section);
            int slot = page == null ? -1 : pages.materialSlot(page.handle, local(fault.material), Block.getId(Blocks.OAK_LOG.defaultBlockState()));
            var material = new TiledThermalField.Sample();
            if (slot >= 0 && field.read(fault.material.asLong(), true, material)) {
                engine.applyMaterialControl(slot, material.law().enthalpyAtTemperature(32), material.law().initialBranch(32));
                fault.materialPrepared = true;
            }
        }
        long generation = read(engine, "dimensionGeneration");
        ThermalSourceLedger ledger = read(engine, "sources");
        SpatialHeatBuffer banks = read(engine, "fixedHeat");
        var balance = ledger.energyBalance();
        double error = balance.outputJ() - balance.deliveredJ() - balance.declaredLossJ() - balance.degradedLossJ()
                - balance.unacceptedJ() - balance.exportedJ() - engine.inputStore().pendingEnergyJ() - banks.committedPendingJoules();
        fault.maximumImbalanceJ = Math.max(fault.maximumImbalanceJ, Math.abs(error)); fault.observations++;
        if (!Double.isFinite(error) || Math.abs(error) > Math.max(1e-6, 1e-7 * Math.abs(balance.outputJ()))) fault.energyFailures++;
        if (fault.generation >= 0 && generation > fault.generation) fault.recreated = true;
        if (fault.site != FailurePoint.CLAIM || !fault.armed || field.idle() || engine.checkpointRecovery()) return;
        com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatStore.Claim claim = read(engine, "fixedClaim");
        if (claim == null) return;
        long chunk = new net.minecraft.world.level.ChunkPos(fault.point).toLong();
        double chunkJ = claim.energyJInChunk(chunk);
        if (chunkJ <= 0) return;
        inject(engine, fault, chunkJ);
    }

    public static void geometry(Object task, ThermalInputBatch input, String phase, boolean completed) {
        var fault = active;
        if (fault == null || !fault.armed || completed || !fault.site.phase.equals(phase) || fault.currentEngine == null) return;
        var engine = fault.currentEngine.get();
        if (engine == null || read(engine, "fixedGeometry") != task) return;
        TiledThermalField field = read(engine, "fixedField");
        var table = field.signatureTable();
        int expected = Block.getId(fault.compound ? Blocks.STONE.defaultBlockState()
                : Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        var geometry = input.geometry();
        for (int index = 0; index < geometry.size(); index++) if (geometry.page(index).sectionKey() == fault.section) {
            var full = geometry.fullPageSignatures(index);
            if (full == null && geometry.blockIndex(index) != local(fault.material)) continue;
            int signature = full == null ? geometry.signatureId(index) : full.get(local(fault.material));
            if (table.valid(signature) && table.materialStateId(signature) == expected) inject(engine, fault, 0);
        }
    }

    public static void stage(String phase, boolean entering) {
        var fault = active;
        if (fault == null || fault.injected == 0 || fault.queuedCapture == null || !entering || !"thermal_completion".equals(phase)) return;
        var action = fault.queuedCapture; fault.queuedCapture = null; action.run();
    }

    private static void inject(ThermalDimensionEngine engine, Fault fault, double chunkJ) {
        TiledThermalField field = read(engine, "fixedField");
        long generation = read(engine, "dimensionGeneration");
        var sample = new TiledThermalField.Sample(); field.read(fault.point.asLong(), false, sample);
        fault.claimedChunkJ = chunkJ; fault.acceptedC = sample.temperatureC();
        field.read(fault.material.asLong(), true, sample); fault.materialJ = sample.enthalpyJ(); fault.materialC = sample.temperatureC();
        fault.expectedMaterialJ = fault.materialJ;
        if (fault.compound) {
            var wood = MinecraftThermalProfiles.materialLaw(Blocks.OAK_PLANKS.defaultBlockState());
            var stone = MinecraftThermalProfiles.materialLaw(Blocks.STONE.defaultBlockState());
            double woodCapacity = wood.enthalpyAtTemperature(1) - wood.enthalpyAtTemperature(0);
            double stoneCapacity = stone.enthalpyAtTemperature(1) - stone.enthalpyAtTemperature(0);
            fault.expectedMaterialJ = wood.enthalpyAtTemperature(fault.materialC) + (stoneCapacity - woodCapacity) * fault.massNaturalC;
        }
        fault.failedEngine = new java.lang.ref.WeakReference<>(engine);
        fault.failedField = new java.lang.ref.WeakReference<>(field);
        fault.armed = false; fault.generation = generation; fault.injected++;
        FHMain.LOGGER.info("THERMAL_EXPECTED_WORKER_FAILURE,scope=real_fixed_worker,site={},compound={},generation={},acceptedTick={},chunkClaimedJ={},materialJ={}",
                fault.site, fault.compound, generation, field.acceptedTick(), chunkJ, fault.materialJ);
        throw new IllegalStateException("Intentional GameTest fixed-worker failure at " + fault.site);
    }

    public static void checkpointSealed(long chunk, RegionalCheckpoint checkpoint, SpatialSourceTimeline.Capture producer,
            PendingHeatCheckpoint cold, double halfLife) {
        var fault = active;
        if (fault != null && fault.diskCapture) captureDiskMaterial(fault, chunk, checkpoint);
        if (fault == null || fault.captured || fault.failedEngine == null || chunk != new net.minecraft.world.level.ChunkPos(fault.point).toLong()) return;
        var engine = fault.failedEngine.get();
        if (engine == null || !engine.checkpointRecovery()) return;
        SpatialHeatBuffer banks = read(engine, "fixedHeat");
        double[] expected = {0};
        engine.inputStore().collectChunk(chunk, (target, from, to, joules) -> expected[0] += joules);
        banks.committedPendingInChunk(chunk, (address, joules, since) -> expected[0] += joules);
        producer.pending(chunk, engine::sourcePortDestination, (target, from, to, joules) -> expected[0] += joules);
        cold.collectRemainingForSave(0, producer.tick, halfLife, (target, from, to, joules) -> expected[0] += joules);
        java.util.ArrayDeque<?> restores = read(engine, "restoreRequests");
        for (var restore : restores) {
            long restoring = read(restore, "chunk"); if (restoring != chunk) continue;
            PendingHeatCheckpoint pending = read(restore, "pending"); int cursor = read(restore, "cursor");
            pending.collectRemainingForSave(cursor, producer.tick, halfLife, (target, from, to, joules) -> expected[0] += joules);
        }
        double saved = 0; var pending = checkpoint.pending();
        for (int index = 0; index < pending.size(); index++) saved += pending.energyJ(index);
        fault.expectedJ = expected[0]; fault.savedJ = saved;
        if (!near(saved, expected[0]) || saved + 1e-6 < fault.claimedChunkJ) fault.fail("Failure checkpoint lost or duplicated pending heat");
        for (var section : checkpoint.sections()) if (section.sectionKey() == fault.section) {
            fault.snapshotRevision = section.geometryRevision();
            var materials = section.acceptedMaterials();
            int body = materials == null ? -1 : materials.find(local(fault.material));
            if (body >= 0) fault.checkpointMaterialJ = materials.enthalpyJ(body);
            var air = section.captureAir(0);
            fault.savedC = air == null ? Double.NaN : air.sampleC(fault.point.getX() + .5, fault.point.getY() + .5, fault.point.getZ() + .5);
            if (!near(fault.savedC, fault.acceptedC)) fault.fail("Failure checkpoint changed the actual Accepted temperature");
            fault.captured = true;
            fault.failedEngine.clear();
        }
    }

    private static <T> T read(Object target, String name) { return ThermalLoadedWorldGameTests.read(target, name); }
    private static int local(BlockPos position) { return (position.getX() & 15) | (position.getZ() & 15) << 4 | (position.getY() & 15) << 8; }
    private static boolean near(double first, double second) {
        return Double.isFinite(first) && Double.isFinite(second) && Math.abs(first - second) <= 1e-7 * Math.max(1, Math.max(Math.abs(first), Math.abs(second)));
    }

    @GameTest(template = "phase0a_empty", batch = "fixed_worker_replacement", timeoutTicks = 2400)
    public static void routedRoomsSurviveNativeWorkerReplacement(GameTestHelper helper) throws java.io.IOException {
        run(helper, FailurePoint.CLAIM);
    }
    @GameTest(template = "phase0a_empty", batch = "fixed_worker_topology_failure", timeoutTicks = 2400)
    public static void routedRoomsSurviveFailureBeforeTopologyPreparation(GameTestHelper helper) throws java.io.IOException {
        run(helper, FailurePoint.TOPOLOGY);
    }
    @GameTest(template = "phase0a_empty", batch = "fixed_worker_prepublication_failure", timeoutTicks = 2400)
    public static void routedRoomsSurviveFailureBeforeFixedPublication(GameTestHelper helper) throws java.io.IOException {
        run(helper, FailurePoint.BEFORE_PUBLICATION);
    }
    @GameTest(template = "phase0a_empty", batch = "fixed_worker_postpublication_failure", timeoutTicks = 2400)
    public static void routedRoomsSurviveFailureAfterFixedPublication(GameTestHelper helper) throws java.io.IOException {
        run(helper, FailurePoint.PUBLISHED);
    }
    @GameTest(template = "phase0a_empty", batch = "fixed_worker_material_failure", timeoutTicks = 2400)
    public static void compoundMaterialChangeIsProjectedOnceAfterPrivateGeometryFailure(GameTestHelper helper) throws java.io.IOException {
        run(helper, FailurePoint.BEFORE_PUBLICATION, true);
    }
    @GameTest(template = "phase0a_empty", batch = "fixed_worker_queued_save_failure", timeoutTicks = 2400)
    public static void queuedWorkerFailureIsRecoveredBeforeStartingOrdinaryCapture(GameTestHelper helper) throws java.io.IOException {
        run(helper, FailurePoint.CAPTURE_QUEUED);
    }

    private static void run(GameTestHelper helper, FailurePoint site) throws java.io.IOException {
        run(helper, site, false);
    }
    private static void run(GameTestHelper helper, FailurePoint site, boolean compound) throws java.io.IOException {
        var level = helper.getLevel();
        MinecraftThermalInput.closeActiveLevel(level);
        ContinuousAirPrecisionProductionGameTests.restoreReferenceClimate(helper, true);
        BlockPos[] origins = {new BlockPos(2048, 80, 2048), new BlockPos(2176, 80, 2048)};
        BlockPos[] fires = new BlockPos[2];
        for (int i = 0; i < origins.length; i++) {
            ThermalConvergenceScenes.force(helper, origins[i], 16, true);
            fires[i] = ContinuousAirProductionGameTests.room(helper, origins[i]);
            level.setBlockAndUpdate(fires[i].above(), Blocks.STONE_SLAB.defaultBlockState());
            ContinuousAirProductionGameTests.useItem(helper, fires[i], Items.FLINT_AND_STEEL);
        }
        var material = fires[0].east(2);
        level.setBlockAndUpdate(material, Blocks.OAK_LOG.defaultBlockState());
        var fault = new Fault(fires[0].above(2), material, site, compound); active = fault;
        if (site == FailurePoint.CAPTURE_QUEUED) fault.queuedCapture = () -> captureQueuedFailure(helper, fault);
        restoreWorld = () -> {
            for (int i = 0; i < fires.length; i++) {
                if (level.getBlockState(fires[i]).is(Blocks.CAMPFIRE))
                    level.setBlockAndUpdate(fires[i], level.getBlockState(fires[i]).setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false));
                ThermalConvergenceScenes.force(helper, origins[i], 16, false);
            }
            MinecraftThermalInput.closeActiveLevel(level);
        };
        var sample = new ThermalEnvironmentSample();
        long[] before = new long[2], recovered = new long[2];
        ContinuousAirGameTestPacing.beginMeasurement();
        helper.startSequence().thenIdle(500).thenWaitUntil(() -> {
            helper.assertTrue(fault.materialPrepared, "Set a controlled 32 C material through the existing worker-owned material interface");
            for (int i = 0; i < fires.length; i++) {
                var point = fires[i].above();
                ContinuousAirProductionGameTests.sampleAt(level, point.getX() + .5, point.getY() + .5, point.getZ() + .5, sample);
                helper.assertTrue(sample.airAvailable() && sample.airSampleTick() >= 0 && sample.sharedAirRegion(),
                        "Both actual slab routes must have an online field before recovery");
                before[i] = sample.airSampleTick();
                var direct = fires[i].above(2);
                ContinuousAirProductionGameTests.sampleAt(level, direct.getX() + .5, direct.getY() + .5, direct.getZ() + .5, sample);
                helper.assertTrue(sample.airAvailable() && sample.airSampleTick() >= 0
                                && MinecraftThermalInput.describeAirRead(level, direct).startsWith("geometry_current"),
                        "Both direct fields must also be current before the deliberate failure");
            }
        }).thenExecute(() -> {
            fault.massNaturalC = com.teammoeg.frostedheart.content.climate.WorldTemperature.naturalAir(level,
                    new BlockPos((material.getX() & ~15) + 8, (material.getY() & ~15) + 8, (material.getZ() & ~15) + 8));
            fault.armed = true;
            if (compound) {
                changeMaterial(level, material, Blocks.OAK_PLANKS.defaultBlockState(),
                        com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch.MaterialChanges.GAMEPLAY_TRANSITION);
                changeMaterial(level, material, Blocks.STONE.defaultBlockState(),
                        com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch.MaterialChanges.MASS_CHANGE);
            } else level.setBlockAndUpdate(material, Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        }).thenWaitUntil(() -> {
            helper.assertTrue(fault.recreated, "The normal worker-failure handler must replace its generation");
            for (int i = 0; i < fires.length; i++) {
                var point = fires[i].above();
                ContinuousAirProductionGameTests.sampleAt(level, point.getX() + .5, point.getY() + .5, point.getZ() + .5, sample);
                helper.assertTrue(sample.airAvailable() && sample.sharedAirRegion() && sample.airSampleTick() > before[i]
                                && level.getGameTime() - sample.airSampleTick() <= 40,
                        "Both real rooms must recover current routed geometry and advance beyond the old generation");
                recovered[i] = sample.airSampleTick();
                var direct = fires[i].above(2);
                ContinuousAirProductionGameTests.sampleAt(level, direct.getX() + .5, direct.getY() + .5, direct.getZ() + .5, sample);
                helper.assertTrue(sample.airAvailable() && sample.airSampleTick() > before[i]
                                && level.getGameTime() - sample.airSampleTick() <= 40
                                && MinecraftThermalInput.describeAirRead(level, direct).startsWith("geometry_current"),
                        "The direct fields must recover current geometry and new physical time too");
            }
        }).thenExecute(() -> {
            helper.assertTrue(fault.failure == null, fault.failure == null ? "The queued capture keeps old ownership" : fault.failure);
            var stored = MinecraftThermalInput.dormantState(level.getChunkAt(material));
            var savedMaterial = stored == null ? null : stored.materials(material.getY() >> 4);
            int body = savedMaterial == null ? -1 : savedMaterial.find(local(material));
            helper.assertTrue(body >= 0 && savedMaterial.stateId(body) == Block.getId(level.getBlockState(material)),
                    "The written checkpoint must project the actual material state through the ordered world edits");
            fault.savedMaterialJ = savedMaterial.enthalpyJ(body);
            FHMain.LOGGER.info("THERMAL_FAILURE_MATERIAL_PROJECTION,site={},compound={},beforeJ={},expectedJ={},snapshotJ={},savedJ={},naturalC={},snapshotRevision={}",
                    site, compound, fault.materialJ, fault.expectedMaterialJ, fault.checkpointMaterialJ, fault.savedMaterialJ, fault.massNaturalC, fault.snapshotRevision);
            helper.assertTrue(near(fault.savedMaterialJ, fault.expectedMaterialJ),
                    "Ordered material changes must apply once: site=" + site + ",compound=" + compound
                            + ",expected=" + fault.expectedMaterialJ + ",actual=" + fault.savedMaterialJ);
        }).thenIdle(200).thenExecute(() -> {
            verifyDisk(helper, fault);
            FHMain.LOGGER.info("THERMAL_WORKER_RECOVERY_RESULT,scope=real_fixed_worker,site={},compound={},generation={},before={},recovered={},injected={},captured={},claimedChunkJ={},expectedJ={},savedJ={},acceptedC={},savedC={},energyObservations={},energyFailures={},maximumImbalanceJ={},materialJ={},savedMaterialJ={},failure={}",
                    site, compound, fault.generation, java.util.Arrays.toString(before), java.util.Arrays.toString(recovered), fault.injected, fault.captured,
                    fault.claimedChunkJ, fault.expectedJ, fault.savedJ, fault.acceptedC, fault.savedC,
                    fault.observations, fault.energyFailures, fault.maximumImbalanceJ, fault.materialJ, fault.savedMaterialJ, fault.failure);
            helper.assertTrue(fault.injected == 1 && fault.captured && fault.observations > 0 && fault.energyFailures == 0 && fault.failure == null,
                    "The actual failed claim must survive normal checkpoint and worker replacement: " + fault.failure);
            for (int attempt = 0; attempt < 3; attempt++) System.gc();
            var current = MinecraftThermalInput.queryPublicationForObservation(level);
            helper.assertTrue(fault.failedField != null && fault.failedField.get() == null
                            && current != null && current.fixedState().hasPublishedSection(fault.section),
                    "The failed field must collect while its replacement still has the heated Section");
            FHMain.LOGGER.info("THERMAL_WORKER_RECOVERY_COLLECTION,site={},compound={},failedFieldCollected=true,replacementStillHeated=true,diagnosticGc=true", site, compound);
        }).thenSucceed();
    }

    @AfterBatch(batch = "fixed_worker_replacement")
    public static void cleanup(net.minecraft.server.level.ServerLevel level) {
        active = null;
        if (restoreWorld != null) { var restore = restoreWorld; restoreWorld = null; restore.run(); }
    }
    @AfterBatch(batch = "fixed_worker_topology_failure")
    public static void cleanupTopology(net.minecraft.server.level.ServerLevel level) { cleanup(level); }
    @AfterBatch(batch = "fixed_worker_prepublication_failure")
    public static void cleanupBeforePublication(net.minecraft.server.level.ServerLevel level) { cleanup(level); }
    @AfterBatch(batch = "fixed_worker_postpublication_failure")
    public static void cleanupAfterPublication(net.minecraft.server.level.ServerLevel level) { cleanup(level); }
    @AfterBatch(batch = "fixed_worker_material_failure")
    public static void cleanupMaterial(net.minecraft.server.level.ServerLevel level) { cleanup(level); }
    @AfterBatch(batch = "fixed_worker_queued_save_failure")
    public static void cleanupQueuedSave(net.minecraft.server.level.ServerLevel level) { cleanup(level); }

    private static void captureQueuedFailure(GameTestHelper helper, Fault fault) {
        java.util.IdentityHashMap<net.minecraft.server.level.ServerLevel, MinecraftThermalInput> inputs = read(MinecraftThermalInput.class, "ACTIVE");
        var input = inputs.get(helper.getLevel());
        com.teammoeg.frostedheart.content.climate.thermal.runtime.async.ThermalDimensionMailbox mailbox = read(input, "mailbox");
        long deadline = System.nanoTime() + 10_000_000_000L;
        boolean failed = false;
        while (!failed) {
            synchronized (mailbox) {
                java.util.ArrayDeque<com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalCompletion> queued = read(mailbox, "completions");
                for (var completion : queued) if (completion.status()
                        == com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalCompletion.Status.ENGINE_FAILED) failed = true;
            }
            if (!failed) {
                if (System.nanoTime() >= deadline) { fault.fail("The real worker must queue its failure before this ordinary capture"); return; }
                java.util.concurrent.locks.LockSupport.parkNanos(1_000_000);
            }
        }
        boolean parentCapture = read(input, "checkpointInProgress");
        boolean completed = input.captureRegionalCheckpoint(helper.getLevel().getChunkAt(fault.material), false);
        if (!fault.captured) fault.fail("A queued worker failure must capture its old generation before the ordinary save starts");
        FHMain.LOGGER.info("THERMAL_QUEUED_FAILURE_CAPTURE,parentCapture={},oldCheckpointCaptured={},ordinaryCaptureCompleted={}",
                parentCapture, fault.captured, completed);
    }

    private static void captureDiskMaterial(Fault fault, long chunk, RegionalCheckpoint checkpoint) {
        if (chunk != new net.minecraft.world.level.ChunkPos(fault.material).toLong() || fault.currentEngine == null) return;
        var engine = fault.currentEngine.get(); if (engine == null) return;
        for (var section : checkpoint.sections()) if (section.sectionKey() == fault.section) {
            var materials = section.acceptedMaterials(); int body = materials == null ? -1 : materials.find(local(fault.material));
            if (body >= 0) fault.diskExpectedMaterialJ = materials.enthalpyJ(body);
        }
    }

    private static void verifyDisk(GameTestHelper helper, Fault fault) {
        var level = helper.getLevel(); var chunk = level.getChunkAt(fault.material);
        fault.diskCapture = true;
        try {
            chunk.setUnsaved(true);
            level.getServer().saveEverything(true, true, true);
            level.getChunkSource().chunkMap.flushWorker();
            var disk = level.getChunkSource().chunkMap.read(chunk.getPos()).join().orElseThrow();
            var stored = com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState
                    .decode(disk, chunk.getSectionYFromSectionIndex(0), chunk.getSections().length);
            var materials = stored == null ? null : stored.materials(fault.material.getY() >> 4);
            int body = materials == null ? -1 : materials.find(local(fault.material));
            helper.assertTrue(body >= 0 && materials.stateId(body) == Block.getId(level.getBlockState(fault.material))
                            && near(materials.enthalpyJ(body), fault.diskExpectedMaterialJ),
                    "Flushed native disk material must equal the actual save checkpoint after worker recovery");
            FHMain.LOGGER.info("THERMAL_RECOVERY_DISK_MATERIAL,site={},compound={},expectedJ={},diskJ={},verified=true",
                    fault.site, fault.compound, fault.diskExpectedMaterialJ, materials.enthalpyJ(body));
        } finally { fault.diskCapture = false; }
    }

    /** Exercise the original scoped gameplay mutation, including its actual cause and world notifications. */
    private static void changeMaterial(net.minecraft.server.level.ServerLevel level, BlockPos position,
            net.minecraft.world.level.block.state.BlockState target, byte cause) {
        try {
            var method = com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftPhaseController.class
                    .getDeclaredMethod("setMaterialBlock", net.minecraft.server.level.ServerLevel.class, BlockPos.class,
                            net.minecraft.world.level.block.state.BlockState.class, net.minecraft.world.level.block.state.BlockState.class, byte.class);
            method.setAccessible(true); method.invoke(null, level, position, level.getBlockState(position), target, cause);
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Original gameplay mutation could not run", failure); }
    }
}
