/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import static com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.TRACE;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.consumer.TownThermalProjection;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticField;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticFieldIndex;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.DormantThermalCooling;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.DormantChunkThermalState;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MaterialSectionState;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MinecraftThermalChunkAttachment;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftSignatureCapture;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample;
import com.teammoeg.frostedheart.content.climate.thermal.radiation.RadiationService;
import com.teammoeg.frostedheart.content.climate.thermal.radiation.minecraft.BlockRadiationIndex;
import com.teammoeg.frostedheart.content.climate.thermal.radiation.minecraft.MinecraftRadiationOcclusion;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.async.ThermalDimensionMailbox;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.async.ThermalWorkerPool;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine.ThermalDimensionEngine;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine.ThermalDimensionLimits;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.DimensionInputAccumulator;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftEnvironmentCapture;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftPageManager;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftPhaseController;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftThermalSectionAttachment;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalCompletion;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputEnvelope;
import com.teammoeg.frostedheart.content.climate.thermal.solver.BuoyancyConductance;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.PhysicalSourceSpatialIndex;
import com.teammoeg.frostedheart.content.climate.thermal.topology.FarFieldSettings;
import com.teammoeg.frostedheart.content.climate.thermal.topology.ThermalTopologyParameters;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Minecraft 热系统的唯一公开运行时入口。
 *
 * <p>服务器主线程通过该类创建每维度 runtime、收集 20-tick 输入 cut、提交
 * mailbox、消费 completion，并从不可变 publication 回答玩法查询。它不编译
 * Brick，也不直接推进 solver。</p>
 *
 * <p>The sole public runtime facade for dimension lifecycle, fixed-cut
 * transport, and gameplay queries.</p>
 */
public final class MinecraftThermalInput implements AutoCloseable {
    static final int MAX_PUBLICATION_AGE_TICKS = 40;
    private static final int MAXIMUM_PHYSICAL_SOURCES = 65_536;
    private static final int SOURCE_DISCOVERY_CHUNKS_PER_TICK = 8;
    private static final int SOURCE_DISCOVERY_POSITIONS_PER_CHUNK = 32;
    private static final int SOURCE_DISCOVERY_POSITIONS_PER_TICK = 256;
    private final boolean nativeCheckpoint;
    private static final int MAX_NATIVE_CHECKPOINTS = 128;
    private long lastMainTickNanos, saveWaitNanos, unloadWaitNanos, saveWaitCalls, unloadWaitCalls;
    private long maxSaveWaitNanos, maxUnloadWaitNanos;
    private long maxSaveWaitChunk, maxUnloadWaitChunk;
    private long savesWhilePending, unloadsWhilePending;
    private long unrelatedSaveSkips, unrelatedUnloadSkips;
    private long unrelatedSavesWhilePending;
    private long maintenanceSaveDeferrals, maintenanceSaves;
    private long snapshotCheckpoints, snapshotFallbacks, snapshotSavePending, snapshotUnloadPending;
    private long snapshotMaximumAgeTicks, snapshotMaximumNanos, snapshotMaximumOverlapBytes;
    private long snapshotStoredMaximumAgeTicks;
    private ThermalMemoryBudget.Reservation checkpointTimeScratch;
    private long lastAcceptedLiveCheckpointTick = -1;
    private LevelChunk preparedNativeUnload;
    private LevelChunk preparedMaintenanceSave;
    private final IdentityHashMap<NativeUnloadCheckpoint, Boolean> nativeCheckpoints = new IdentityHashMap<>();
    private boolean checkpointInProgress;
    private NativeUnloadCheckpoint nativePreparation;
    private final java.util.ArrayDeque<NativeUnloadCheckpoint> nativePreparationQueue = new java.util.ArrayDeque<>();
    // Capturing production closes its tail; subsequent envelopes cannot seal before it.
    private long sourceCaptureTick = -1;
    private final Long2ObjectLinkedOpenHashMap<ColdRestore> coldRestores = new Long2ObjectLinkedOpenHashMap<>();
    private static final class ColdRestore {
        final LevelChunk chunk;
        final com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.PendingHeatCheckpoint pending;
        int seedCursor;
        boolean offered;
        long capacityWait = -1;
        ColdRestore(LevelChunk chunk, com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.PendingHeatCheckpoint pending) {
            this.chunk = chunk; this.pending = pending;
        }
    }
    private long nativeCheckpointRequests, nativeCheckpointDeferred, nativeCheckpointPrepared;
    private long nativeCheckpointCancelled, nativeCheckpointFallbacks, nativeCheckpointMaxAgeTicks;
    private int nativeCheckpointPeakPending;

    @jdk.jfr.Name("frostedheart.CheckpointSnapshot")
    @jdk.jfr.Label("Thermal checkpoint from completed publications")
    @jdk.jfr.StackTrace(false)
    private static final class CheckpointSnapshot extends jdk.jfr.Event {
        int chunkX, chunkZ, pages, failedPages, staleGeometryPages;
        long gameTick, minimumSampleTick, maximumSampleTick, allocatedBytes, overlapBytes, threadCpuNanos;
        boolean unloading, workerPending, accepted, fromStoredCheckpoint, emptyInitialization;
        String fallbackReason, captureFailure;
    }

    private record ChunkCheckpointCapture(DormantChunkThermalState state, int pages, int failedPages,
            int staleGeometryPages, long minimumSampleTick, long maximumSampleTick, String failure) {}

    @jdk.jfr.Name("frostedheart.NativeCheckpoint")
    @jdk.jfr.Label("Native unload readiness without blocking the main thread")
    @jdk.jfr.StackTrace(false)
    private static final class NativeCheckpointEvent extends jdk.jfr.Event {
        int chunkX, chunkZ, attempts;
        long requestedTick, readyTick, consumedTick, liveSampleTick, heldBytes;
        boolean prepared, cancelled, fallback;
        String failure;
    }

    @jdk.jfr.Name("frostedheart.MaintenanceSave")
    @jdk.jfr.Label("Thermal maintenance save attempt")
    @jdk.jfr.StackTrace(false)
    private static final class MaintenanceSave extends jdk.jfr.Event {
        int chunkX, chunkZ;
        long gameTick;
    }

    private static final int MAXIMUM_RADIATION_SECTIONS = 3_200;
    static final int GAMEPLAY_ITEM_ENVIRONMENT_SAMPLES_PER_TICK = 64;
    private static final ThermalMemoryBudget MEMORY = new ThermalMemoryBudget(() ->
            FHConfig.COMMON.THERMAL_RUNTIME.thermalMemoryTargetMiB.get() * 1024L * 1024L);
    private static final RadiationService.Parameters RADIATION_PARAMETERS =
            new RadiationService.Parameters(
                    MAXIMUM_RADIATION_SECTIONS,
                    128,
                    64,
                    8,
                    24,
                    8,
                    256,
                    16.0D,
                    0.1D,
                    0.5D,
                    0.1D,
                    0.9D,
                    1.62D);
    private static final IdentityHashMap<ServerLevel, MinecraftThermalInput> ACTIVE =
            new IdentityHashMap<>();
    private static final AtomicLong NEXT_GENERATION = new AtomicLong(1_000L);
    private static final ThreadLocal<BlockPos.MutableBlockPos> DORMANT_QUERY_POSITION =
            ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);
    private static final ThreadLocal<ThermalAnalyticFieldIndex.Sample> TOWN_FIELD_SAMPLE =
            ThreadLocal.withInitial(ThermalAnalyticFieldIndex.Sample::new);

    private final ServerLevel level;
    private final Thread mainThread;
    private final MinecraftThermalProfiles.Snapshot profiles;
    private final double referenceTemperatureC;
    private final ThermalAnalyticFieldIndex analyticFields;
    private final MinecraftEnvironmentCapture environment;
    private final MinecraftPageManager pages;
    private final PhysicalSourceSpatialIndex physicalSources;
    private final Long2ObjectLinkedOpenHashMap<LevelChunk> pendingSourceChunks =
            new Long2ObjectLinkedOpenHashMap<>();
    private final it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<PhysicalSourceSpatialIndex.DiscoveryCursor> sourceDiscoveryCursors =
            new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>();
    private int lastSourceDiscoveryPositions;
    private final MinecraftPhaseController phase;
    private final MinecraftRadiationOcclusion radiationOcclusion;
    private final BlockRadiationIndex blockRadiation;
    private final RadiationService radiation;
    private final QueryPublication.MutableSample querySample = new QueryPublication.MutableSample();
    private final DormantChunkThermalState.CaptureScratch dormantCapture =
            new DormantChunkThermalState.CaptureScratch();
    private final MaterialSectionState.CaptureScratch materialCapture =
            new MaterialSectionState.CaptureScratch();
    private final QueryPublication.ReadCursor checkpointCursor = new QueryPublication.ReadCursor();
    private static final ThreadLocal<MaterialSample> MATERIAL_SAMPLE =
            ThreadLocal.withInitial(MaterialSample::new);
    private final BlockPos.MutableBlockPos dormantPosition = new BlockPos.MutableBlockPos();
    private final RadiationService.MutableSample radiationSample =
            new RadiationService.MutableSample();
    private final ThermalEnvironmentSample passiveScratch = new ThermalEnvironmentSample();
    private final ThermalEnvironmentSample townScratch = new ThermalEnvironmentSample();
    private final ItemEnvironmentSampleCache itemEnvironmentCache =
            new ItemEnvironmentSampleCache(GAMEPLAY_ITEM_ENVIRONMENT_SAMPLES_PER_TICK);
    private final BlockPos.MutableBlockPos townPosition = new BlockPos.MutableBlockPos();
    private static InfraredCapture infraredCapture;

    private long dimensionGeneration;
    private DimensionInputAccumulator accumulator;
    private QueryPublication queryPublication;
    private ThermalDimensionMailbox mailbox;
    private ThermalDimensionEngine workerEngine;
    private long lastSubmittedTargetTick = -1;
    private ThermalInputEnvelope pendingSubmission;
    private long lastCompletedTargetTick;
    private long workLimitedCompletions;
    private boolean closed;
    private boolean stopping;
    private boolean recoveringWorker;

    private MinecraftThermalInput(ServerLevel level) {
        this.level = level;
        analyticFields = MinecraftGameplayFields.indexFor(level);
        referenceTemperatureC = 0;
        mainThread = Thread.currentThread();
        profiles = MinecraftThermalProfiles.prepare();
        if (jdk.jfr.FlightRecorder.isInitialized()) {
            jdk.jfr.FlightRecorder.register(CheckpointSnapshot.class);
            jdk.jfr.FlightRecorder.register(NativeCheckpointEvent.class);
        }
        // Read Forge configuration after it is loaded, then keep one mode for this instance.
        nativeCheckpoint = FHConfig.COMMON.THERMAL_RUNTIME.continuousAirNativeCheckpoint.get();
        long initialTick = alignedTick(level.getGameTime());
        dimensionGeneration = nextGeneration();
        accumulator = createAccumulator(initialTick);
        lastCompletedTargetTick = initialTick;
        MinecraftSignatureCapture signatureCapture =
                new MinecraftSignatureCapture(level, profiles.states(), profiles.signatures());
        environment = new MinecraftEnvironmentCapture(level, accumulator);
        pages = new MinecraftPageManager(this, level, accumulator, signatureCapture, environment);
        physicalSources =
                new PhysicalSourceSpatialIndex(
                        level,
                        accumulator,
                        pages,
                        profiles.tuning().campfire(),
                        64,
                        MAXIMUM_PHYSICAL_SOURCES);
        try {
            ensureCheckpointTimeScratch();
            createWorker(initialTick, referenceTemperatureC);
        } catch (RuntimeException | Error failure) {
            physicalSources.close(); pages.close(); environment.close(); accumulator.close();
            materialCapture.clearMixedTicks();
            if (checkpointTimeScratch != null) checkpointTimeScratch.close();
            throw failure;
        }
        phase = new MinecraftPhaseController(level, pages, profiles, accumulator, 8);
        radiationOcclusion =
                new MinecraftRadiationOcclusion(level, pages, MAXIMUM_RADIATION_SECTIONS);
        blockRadiation =
                profiles.states().radiationEnabled()
                        ? BlockRadiationIndex.tryCreate(
                                level,
                                pages,
                                profiles.states(),
                                MEMORY.createDimensionBudget(
                                        BlockRadiationIndex.projectedMaximumBytes(
                                                MAXIMUM_RADIATION_SECTIONS)),
                                MAXIMUM_RADIATION_SECTIONS)
                        : null;
        pages.attachOcclusion(radiationOcclusion);
        radiation =
                RadiationService.tryCreate(
                        RADIATION_PARAMETERS,
                        physicalSources,
                        blockRadiation,
                        radiationOcclusion,
                        MEMORY.createDimensionBudget(
                                RadiationService.projectedMaximumBytes(RADIATION_PARAMETERS)));
    }

    private void createWorker(long initialTick, double referenceTemperatureC) {
        var tuning = profiles.tuning();
        var limits = ThermalDimensionLimits.sharedGrid(3200, MAXIMUM_PHYSICAL_SOURCES);
        var publication = QueryPublication.createRegional(MEMORY.createCategory(), limits.maximumPages());
        ThermalDimensionEngine engine = null;
        try {
            var topology = new ThermalTopologyParameters(tuning.airHeatCapacityJPerBlockK(), referenceTemperatureC,
                    tuning.airMixingWPerBlockK(), new BuoyancyConductance.Parameters(0.25D, 4.0D, 10.0D),
                    1024, Math.toIntExact(8 * ThermalInputBatch.CUT_INTERVAL_TICKS));
            var arena = new ThermalCellArena(256);
            var farField = new FarFieldSettings(tuning.farFieldConductanceWPerK(), 32.0D);
            engine = new ThermalDimensionEngine(dimensionGeneration, initialTick, arena, profiles.signatures(),
                    profiles.materials(), topology, farField, tuning.campfire(), limits, publication,
                    MEMORY.createCategory(), tuning.continuousAirCorrectionToleranceC());
            mailbox = new ThermalDimensionMailbox(ThermalWorkerPool.shared(), engine);
            workerEngine = engine;
            queryPublication = publication;
            FHMain.LOGGER.info("Thermal runtime: regional shared-grid, dimension={}", level.dimension().location());
            FHMain.LOGGER.info("Thermal checkpoint selected: mode={},dimension={}",
                    nativeCheckpoint ? "native_ready" : "on_demand", level.dimension().location());
        } catch (RuntimeException | Error failure) {
            if (engine != null) {
                try { engine.close(); } catch (Throwable cleanupFailure) { failure.addSuppressed(cleanupFailure); }
            } else publication.close();
            throw failure;
        }
    }

    private DimensionInputAccumulator createAccumulator(long tick) {
        return new DimensionInputAccumulator(dimensionGeneration, tick, MEMORY.createCategory(), MAXIMUM_PHYSICAL_SOURCES);
    }
    private void tick() {
        var stageObserver = com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer;
        boolean observeStage = stageObserver != null;
        if (observeStage) stageObserver.stage("thermal_input_tick", true);
        try {
            requireMainThread();
            if (closed) {
                return;
            }
            long started = System.nanoTime();
            try { tickOpen(); }
            finally { lastMainTickNanos = System.nanoTime() - started; }
        } finally { if (observeStage) stageObserver.stage("thermal_input_tick", false); }

    }

    private void tickOpen() {
        var diagnostics = com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer;
        if (diagnostics != null) diagnostics.mainTick(level);
        drainCompletion();
        physicalSources.reconcileCampfireChanges();
        long gameTick = level.getGameTime();
        long alignedTick = alignedTick(gameTick);
        if (pendingSubmission != null
                || gameTick % ThermalInputBatch.CUT_INTERVAL_TICKS != 0L
                        && alignedTick > Math.max(lastSubmittedTargetTick, sourceCaptureTick)) {
            submitCut(alignedTick);
        }
        advanceColdRestores();
        pages.tick(gameTick);
        phase.tick(queryPublication);
        if (blockRadiation != null) {
            blockRadiation.tick(gameTick);
        }
        boolean cut = gameTick % ThermalInputBatch.CUT_INTERVAL_TICKS == 0L;
        if (cut) {
            physicalSources.flush(gameTick);
        }
        boolean discovered = drainPendingSources();
        // Discovery may occur between cuts. Let the next bounded page capture
        // prepare new targets while ledger registration stays on its original cut.
        if ((discovered))
            physicalSources.prepareNewTargets();
        if (cut) {
            if (discovered) physicalSources.flush(gameTick);
            submitCut(gameTick);
        }
    }

    private void attachLoadedChunk(LevelChunk chunk) {
        pages.onChunkLoad(chunk);
        var stored = dormantState(chunk);
        if ((stored != null) && !stored.coldPending().empty()) {
            stored.restorationPageFloor(pages.restorationPageFloor(chunk.getPos().toLong()));
            coldRestores.put(chunk.getPos().toLong(), new ColdRestore(chunk, stored.coldPending()));
        }
        // Unknown loaded chunks need discovery only when live or packed BEs exist.
        if (!chunk.getBlockEntities().isEmpty() || !chunk.getBlockEntitiesPos().isEmpty())
            enqueueSourceDiscovery(chunk);
    }

    /** At most two chunks and 128 seed records per chunk per tick; only refused offers await capacity. */
    private void advanceColdRestores() {
        int remaining = Math.min(2, coldRestores.size());
        while (remaining-- > 0 && !coldRestores.isEmpty()) {
            long key = coldRestores.firstLongKey();
            var restore = coldRestores.getAndMoveToLast(key);
            if (level.getChunkSource().getChunkNow(restore.chunk.getPos().x, restore.chunk.getPos().z) != restore.chunk) continue;
            var stored = dormantState(restore.chunk);
            if (stored == null) { coldRestores.remove(key); continue; }
            if (restore.offered) {
                if (!workerEngine.hasRetainedInputInChunk(key)) {
                    pages.clearRestorationSeeds(key); coldRestores.remove(key);
                }
                continue;
            }
            int end = Math.min(restore.pending.size(), restore.seedCursor + 128);
            while (restore.seedCursor < end) pages.seedRestoration(restore.pending.position(restore.seedCursor++));
            if (end != restore.pending.size() || restore.capacityWait == MEMORY.capacityRevision()) continue;
            if (workerEngine.offerRestoredHeat(key, restore.pending, level.getGameTime(), dormantHalfLifeSeconds(),
                    stored.restorationPageFloor())) {
                restore.offered = true; stored.acceptedColdPending(restore.pending);
                mailbox.controlChanged();
            } else restore.capacityWait = MEMORY.capacityRevision();
        }
    }

    public void enqueueSourceDiscovery(LevelChunk chunk) {
        requireMainThread();
        // Known refused sources can request discovery during onPlace, before the
        // new BE is installed. Take the position snapshot on the later tick.
        if (!closed) {
            long key = chunk.getPos().toLong();
            // Explicit requests may concern a BE added after the active snapshot.
            // Ordinary cursor slices requeue directly and keep their progress.
            sourceDiscoveryCursors.remove(key);
            pendingSourceChunks.put(key, chunk);
        }
    }

    private boolean drainPendingSources() {
        boolean processed = false;
        lastSourceDiscoveryPositions = 0;
        if (sourceDiscoveryCursors.isEmpty() && pendingSourceChunks.isEmpty()) return false;
        // Resume the small active set first. Unstarted chunks cannot make an
        // already active dense chunk wait a full rotation of the world queue.
        int visitedChunks = 0;
        var cursors = sourceDiscoveryCursors.values().iterator();
        while (cursors.hasNext()) {
            if (!physicalSources.hasAvailableCapacity()) break;
            var cursor = cursors.next();
            long key = cursor.chunk.getPos().toLong();
            pendingSourceChunks.remove(key, cursor.chunk);
            lastSourceDiscoveryPositions += physicalSources.discoverChunk(cursor, SOURCE_DISCOVERY_POSITIONS_PER_CHUNK);
            if (cursor.complete()) cursors.remove();
            else pendingSourceChunks.put(key, cursor.chunk);
            visitedChunks++;
            processed = true;
        }
        if (visitedChunks < SOURCE_DISCOVERY_CHUNKS_PER_TICK && !pendingSourceChunks.isEmpty()
                && physicalSources.hasAvailableCapacity()) {
            // One snapshot creation per tick. Its union-copy cost is distinct
            // from the bounded resync work, and never repeated on each slice.
            int remaining = pendingSourceChunks.size();
            while (remaining-- > 0 && sourceDiscoveryCursors.containsKey(pendingSourceChunks.firstLongKey())) {
                long key = pendingSourceChunks.firstLongKey();
                pendingSourceChunks.putAndMoveToLast(key, pendingSourceChunks.get(key));
            }
            if (remaining >= 0) {
                LevelChunk chunk = pendingSourceChunks.removeFirst();
                long key = chunk.getPos().toLong();
                var cursor = new PhysicalSourceSpatialIndex.DiscoveryCursor(chunk);
                sourceDiscoveryCursors.put(key, cursor);
                lastSourceDiscoveryPositions += physicalSources.discoverChunk(cursor,
                        Math.min(SOURCE_DISCOVERY_POSITIONS_PER_CHUNK, SOURCE_DISCOVERY_POSITIONS_PER_TICK - lastSourceDiscoveryPositions));
                if (cursor.complete()) sourceDiscoveryCursors.remove(key);
                else pendingSourceChunks.put(key, chunk);
                processed = true;
            }
        }
        return processed;
    }

    public record SourceDiscoveryStats(int lastTickPositions, int activeCursors) {}

    public static SourceDiscoveryStats sourceDiscoveryStats(ServerLevel level) {
        MinecraftThermalInput input = active(level);
        return input == null ? new SourceDiscoveryStats(0, 0)
                : new SourceDiscoveryStats(input.lastSourceDiscoveryPositions, input.sourceDiscoveryCursors.size());
    }

    private void attachLoadedWorld(BlockPos center) {
        for (var holder : level.getChunkSource().chunkMap.getChunks()) {
            ChunkPos position = holder.getPos();
            LevelChunk chunk = level.getChunkSource().getChunkNow(position.x, position.z);
            if (chunk != null) attachLoadedChunk(chunk);
        }
        int centerX = SectionPos.blockToSectionCoord(center.getX());
        int centerZ = SectionPos.blockToSectionCoord(center.getZ());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long key = ChunkPos.asLong(centerX + dx, centerZ + dz);
                LevelChunk chunk = pendingSourceChunks.get(key);
                if (chunk != null) pendingSourceChunks.putAndMoveToFirst(key, chunk);
            }
        }
    }

    private void submitCut(long targetTick) {
        targetTick = Math.max(targetTick, Math.max(sourceCaptureTick, accumulator.latestSourceTick()));
        if (pendingSubmission == null && targetTick <= lastSubmittedTargetTick || !mailbox.canQueueInput()) return;
        var batch = pendingSubmission;
        if (batch == null) {
            pages.flushCapturedGeometry();
            batch = accumulator.seal(targetTick);
            if (TRACE) FHMain.LOGGER.info("THERMAL_INPUT_CAPTURE,sequence={},targetTick={},gameTick={},nanos={}",
                    batch.sequence(), targetTick, level.getGameTime(), System.nanoTime());
        }
        if (mailbox.submit(batch)) {
            lastSubmittedTargetTick = batch.targetTick();
            pendingSubmission = null;
            var diagnostics = com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer;
            if (diagnostics != null) diagnostics.transportAcknowledged(batch.sequence(), workerEngine.hasWork());
        } else pendingSubmission = batch;
    }

    private void drainCompletion() {
        if (recoveringWorker) return;
        var diagnostics = com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer;
        if (diagnostics != null) diagnostics.stage("thermal_completion", true);
        try {
            lastCompletedTargetTick = workerEngine.minimumRegionTick();
            for (int count = 0; count < ThermalCompletion.MAXIMUM_PENDING && mailbox.peekCompletion() != null; count++)
                drainOneCompletion();
            if (diagnostics != null) diagnostics.mainIdle(pendingSubmission == null);
            pumpNativePreparation();
        } finally { if (diagnostics != null) diagnostics.stage("thermal_completion", false); }
    }

    private void drainOneCompletion() {
        var completion = mailbox.peekCompletion();
        if (completion == null) return;
        if (completion.dimensionGeneration() != dimensionGeneration)
            throw new IllegalStateException("thermal effect belongs to another dimension generation");
        if (completion.status() == ThermalCompletion.Status.ENGINE_FAILED) {
            FHMain.LOGGER.error("Thermal dimension worker failed for {}", level.dimension().location(), completion.failure());
            recoveringWorker = true;
            try {
                pages.checkpointAll(true);
                mailbox.acknowledgeCompletion(completion.effectSequence());
                mailbox.awaitClosed();
                restartWorker(level.getGameTime());
            } finally { recoveringWorker = false; }
            return;
        }
        pages.acknowledgeResync(completion.committedResyncTokens());
        for (var residency : completion.residencyUpdates()) pages.applyResidency(residency);
        phase.accept(completion.phaseRequests());
        if (completion.status() == ThermalCompletion.Status.WORK_LIMITED) workLimitedCompletions++;
        mailbox.acknowledgeCompletion(completion.effectSequence());
    }
    private void restartWorker(long gameTick) {
        // These main-thread owners survive the worker. Replace generation-local inputs before
        // reseeding; queries always use this instance's current publication.
        dimensionGeneration = nextGeneration();
        long initialTick = alignedTick(gameTick);
        accumulator.close();
        accumulator = createAccumulator(initialTick);
        lastSubmittedTargetTick = initialTick;
        lastCompletedTargetTick = initialTick;
        environment.replaceAccumulator(accumulator);
        physicalSources.replaceAccumulator(accumulator);
        phase.replaceAccumulator(accumulator);
        pendingSubmission = null;
        createWorker(initialTick, referenceTemperatureC);
        pages.reseedAll(accumulator);
        physicalSources.reseedAll(gameTick);
        for (var holder : level.getChunkSource().chunkMap.getChunks()) {
            var pos = holder.getPos();
            var chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
            var cold = chunk == null ? null : dormantState(chunk);
            if (cold != null && !cold.coldPending().empty()) {
                cold.restorationPageFloor(pages.restorationPageFloor(pos.toLong()));
                coldRestores.put(pos.toLong(), new ColdRestore(chunk, cold.coldPending()));
            }
        }
    }

    private void sampleAir(
            double x, double y, double z, long sampleTick, int maximumAgeTicks, ThermalEnvironmentSample out) {
        // Regional geometry and its Accepted field are the continuous query
        // authority. Page slot coverage belongs to scalar/route resolution; a
        // coarse publication need not retain the fine Page's Air slot.
        synchronized (queryPublication) {
            int blockX = floor(x), blockY = floor(y), blockZ = floor(z);
            long sectionKey = SectionPos.asLong(blockX >> 4, blockY >> 4, blockZ >> 4);
            var owner = pages.loadedSectionOrAttach(sectionKey);
            var page = owner == null ? null : owner.page();
            var loadedChunk = owner == null ? null : owner.chunk();
            boolean read = queryPublication.tryReadAir(x, y, z, querySample);
            if (!read && page != null && page.pendingFullGeometryResync() == null
                    && level.getBlockState(new BlockPos(blockX, blockY, blockZ)).isAir())
                read = queryPublication.tryReadCapturedAir(x, y, z, sampleTick, maximumAgeTicks, querySample);
            if (read) {
                if (querySample.minimumQuality() || sampleTick - querySample.sampleTick() <= maximumAgeTicks)
                    out.setAir(querySample.temperatureC(), querySample.sampleTick(), querySample.airBasisTerms(), querySample.minimumQuality());
                else sampleDormant(loadedChunk, x, y, z, sampleTick, out);
                return;
            }
            var publication = airPublication(page);
            if (publication == null) { sampleDormant(loadedChunk, x, y, z, sampleTick, out); return; }
            int localX = SectionPos.sectionRelative(blockX), localY = SectionPos.sectionRelative(blockY), localZ = SectionPos.sectionRelative(blockZ);
            var coverage = publication.brickAt(localX, localY, localZ);
            int slot = publication.resolveAirPoint(localX, localY, localZ);
            if (slot == PagePublication.NO_AIR_POINT) {
                if (sampleRoutedAir(page, publication, coverage, (localX & 3) | (localZ & 3) << 2 | (localY & 3) << 4,
                        sampleTick, maximumAgeTicks, out)) return;
                if (coverage.signaturePayload() == null) sampleDormant(loadedChunk, x, y, z, sampleTick, out);
                return;
            }
            sampleDormant(loadedChunk, x, y, z, sampleTick, out);
        }
    }

    private PagePublication airPublication(ThermalPageHandle page) {
        if (page == null) return null;
        return (page.currentAirPublication());
    }

    static String describeAirRead(ServerLevel level, BlockPos position) {
        MinecraftThermalInput input = ACTIVE.get(level);
        if (input == null) return "input_missing";
        long section = SectionPos.asLong(position.getX() >> 4, position.getY() >> 4, position.getZ() >> 4);
        MinecraftPageManager.SectionOwner owner = input.pages.loadedSectionOrAttach(section);
        if (owner == null) return "page_missing";
        ThermalPageHandle page = owner.page();
        if (page == null) return "page_inactive";
        PagePublication publication = input.airPublication(page);
        if (publication == null) {
            PagePublication last = page.lastPublication();
            return "page_unpublished,liveRevision=" + page.liveGeometryRevision()
                    + ",lastRevision=" + (last == null ? -1 : last.geometryRevision())
                    + ",firstStaleTick=" + page.firstStaleGeometryTick()
                    + ",resync=" + page.pendingFullGeometryResync();
        }
        return input.queryPublication.describeAirRead(position.getX() + .5, position.getY() + .5, position.getZ() + .5)
                + ",pageRevision=" + publication.geometryRevision();
    }

    static long thermalBudgetUsedBytes() { return MEMORY.usedBytes(); }
    static long observedMainTickNanos(ServerLevel level) {
        var input = active(level);
        return input == null ? 0 : input.lastMainTickNanos;
    }
    static int enabledPhysicalSourceCount(ServerLevel level) {
        MinecraftThermalInput input = ACTIVE.get(level);
        return input == null ? 0 : input.physicalSources.enabledSourceCount();
    }
    static long thermalBudgetLimitBytes() { return MEMORY.limitBytes(); }
    static long thermalBudgetPeakBytes() { return MEMORY.peakUsedBytes(); }
    static long thermalBudgetRefusals() { return MEMORY.admissionRefusals(); }
    static long workLimitedCompletions(ServerLevel level) {
        MinecraftThermalInput input = active(level);
        return input == null ? 0 : input.workLimitedCompletions;
    }

    static long independentRegionCommits(ServerLevel level) {
        var input = active(level);
        return input == null || input.workerEngine == null ? 0 : input.workerEngine.independentRegionCommits();
    }
    /** Production lifecycle observation without exposing mutable thermal state. */
    record InputProgress(long completedTick, int queuedSourceChunks, boolean sourceQueued,
            long capturedMask, ThermalPageHandle page) { }
    static InputProgress inputProgress(ServerLevel level, long section) {
        var input = active(level);
        if (input == null) return new InputProgress(-1, 0, false, 0, null);
        long chunk = ChunkPos.asLong(SectionPos.x(section), SectionPos.z(section));
        return new InputProgress(input.lastCompletedTargetTick, input.pendingSourceChunks.size(),
                input.pendingSourceChunks.containsKey(chunk), input.pages.capturedMask(section), input.pages.handle(section));
    }

    static QueryPublication queryPublicationForObservation(ServerLevel level) {
        var input = active(level);
        return input == null ? null : input.queryPublication;
    }
    static boolean pointRegionCommittedIndependently(ServerLevel level, BlockPos position) {
        var input = active(level);
        if (input == null || input.workerEngine == null) return false;
        return input.workerEngine.regionCommittedIndependently(input.queryPublication.regionAt(position.getX() + .5,
                position.getY() + .5, position.getZ() + .5));
    }

    /** Test-clock barrier: finish already submitted work without sealing any new input. */
    static boolean awaitSubmittedCut(ServerLevel level) {
        MinecraftThermalInput input = ACTIVE.get(level);
        if (input == null || input.closed) return true;
        input.requireMainThread();
        if (input.pendingSubmission != null) {
            input.submitCut(input.pendingSubmission.targetTick());
        }
        input.drainCompletion();
        return input.pendingSubmission == null;
    }

    static String describeGeometryInvalidations(ServerLevel level) {
        MinecraftThermalInput input = active(level);
        return input == null ? "input_missing" : input.pages.describeGeometryInvalidations();
    }

    static long lastGeometryInvalidationTick(ServerLevel level) {
        MinecraftThermalInput input = active(level);
        return input == null ? -1 : input.pages.lastGeometryInvalidationTick();
    }

    static String describeWorkerProgress(ServerLevel level) {
        MinecraftThermalInput input = active(level);
        if (input == null) return "input_missing";
        return input.workerEngine.regionalProgressDescription()
                + ",pendingIntakeTick=" + (input.pendingSubmission == null ? -1 : input.pendingSubmission.targetTick())
                + ",workLimited=" + input.workLimitedCompletions;

    }

    private boolean sampleRoutedAir(
            ThermalPageHandle owner,
            PagePublication ownerPublication,
            PagePublication.Brick brick,
            int block,
            long sampleTick,
            int maximumAgeTicks,
            ThermalEnvironmentSample out) {
        var layout = brick.blockLayout();
        if (layout == null || !layout.hasAirRoute(block)) return false;
        long target = layout.routedAirBlock(block);
        int x = BlockPos.getX(target), y = BlockPos.getY(target), z = BlockPos.getZ(target);
        ThermalPageHandle page = pages.handle(SectionPos.asLong(x >> 4, y >> 4, z >> 4));
        PagePublication publication = airPublication(page);
        if (publication == null) return false;
        var coverage = publication.brickAt(x & 15, y & 15, z & 15);
        int slot = publication.resolveAirPoint(x & 15, y & 15, z & 15);
        boolean read = slot >= 0 && ((queryPublication.tryReadAir(x + 0.5, y + 0.5, z + 0.5, querySample)));
        if (slot < 0
                || !read
                || !querySample.minimumQuality() && sampleTick - querySample.sampleTick() > maximumAgeTicks
                || airPublication(page) != publication
                || airPublication(owner) != ownerPublication
                || !layout.hasAirRoute(block)) return false;
        out.setAirFromRegion(querySample.temperatureC(), querySample.sampleTick(), querySample.airBasisTerms(), querySample.minimumQuality());
        return true;
    }

    /** Wait only for immutable input ownership, never for physical catch-up. */
    private <T> T awaitCheckpointCommand(CompletableFuture<T> future) {
        mailbox.controlChanged();
        while (!future.isDone()) {
            long revision = mailbox.progressRevision();
            drainCompletion();
            if (!future.isDone()) mailbox.awaitProgress(revision);
        }
        return future.join();
    }

    private void sampleDormant(
            LevelChunk loadedChunk,
            double x,
            double y,
            double z,
            long gameTick,
            ThermalEnvironmentSample out) {
        if (loadedChunk == null) loadedChunk = level.getChunkSource().getChunkNow(floor(x) >> 4, floor(z) >> 4);
        DormantChunkThermalState state = loadedChunk == null ? null : dormantState(loadedChunk);
        double temperature = state == null ? Double.NaN
                : state.sampleAt(x, y, z, gameTick, dormantHalfLifeSeconds(), level, dormantPosition);
        if (Double.isFinite(temperature)) {
            out.setAir(temperature);
        }
    }

    private void sampleRadiation(ServerPlayer player, ThermalEnvironmentSample out) {
        if (radiation == null) {
            return;
        }
        radiation.samplePlayer(
                receiverKey(player),
                player.getId() & Integer.MAX_VALUE,
                player.getX(),
                player.getY(),
                player.getEyeY(),
                player.getZ(),
                radiationSample);
        out.setRadiation(radiationSample.radiantFluxWPerM2());
    }

    public static double gameplayPlayerEnvironment(
            ServerPlayer player, double naturalTemperatureC, ThermalEnvironmentSample out) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(out, "out").clear();
        if (!Double.isFinite(naturalTemperatureC)) {
            return naturalTemperatureC;
        }
        MinecraftThermalInput input = active(player.serverLevel());
        if (input == null) {
            input = start(player.serverLevel(), player.blockPosition());
        }
        if (input == null) {
            ThermalAnalyticFieldIndex fields =
                    MinecraftGameplayFields.existing(player.serverLevel());
            double composed =
                    fields == null
                            ? naturalTemperatureC
                            : fields.compose(
                                    player.getX(),
                                    player.getEyeY(),
                                    player.getZ(),
                                    naturalTemperatureC,
                                    naturalTemperatureC);
            if (Double.compare(composed, naturalTemperatureC) != 0) out.setComposedAir(composed);
            return composed;
        }
        input.sampleAir(
                player.getX(),
                player.getEyeY(),
                player.getZ(),
                player.serverLevel().getGameTime(),
                MAX_PUBLICATION_AGE_TICKS,
                out);
        input.sampleRadiation(player, out);
        double base = out.airAvailable() ? out.airTemperatureC() : naturalTemperatureC;
        double composed =
                input.analyticFields.compose(
                        player.getX(), player.getEyeY(), player.getZ(), naturalTemperatureC, base);
        if (Double.compare(composed, base) != 0) {
            out.setComposedAir(composed);
        }
        return composed;
    }

    public static double gameplayPassiveEnvironment(
            LevelReader level, BlockPos position, double naturalTemperatureC) {
        if (!(level instanceof ServerLevel server)
                || !Double.isFinite(naturalTemperatureC)
                || !server.getServer().isSameThread()) {
            return naturalTemperatureC;
        }
        MinecraftThermalInput input = active(server);
        double base;
        if (input == null) {
            double dormant =
                    dormantTemperature(
                            server,
                            position.getX(),
                            position.getY(),
                            position.getZ(),
                            server.getGameTime(),
                            DORMANT_QUERY_POSITION.get());
            base = Double.isFinite(dormant) ? dormant : naturalTemperatureC;
        } else {
            ThermalEnvironmentSample out = input.passiveScratch;
            out.clear();
            input.sampleAir(
                    position.getX() + 0.5D,
                    position.getY() + 0.5D,
                    position.getZ() + 0.5D,
                    server.getGameTime(),
                    MAX_PUBLICATION_AGE_TICKS,
                    out);
            base = out.airAvailable() ? out.airTemperatureC() : naturalTemperatureC;
        }
        ThermalAnalyticFieldIndex fields =
                input == null ? MinecraftGameplayFields.existing(server) : input.analyticFields;
        if (fields == null || fields.isEmpty()) {
            return base;
        }
        double x = position.getX() + 0.5D;
        double y = position.getY() + 0.5D;
        double z = position.getZ() + 0.5D;
        return fields.compose(x, y, z, naturalTemperatureC, base);
    }

    /**
     * Samples a dropped reservoir without starting a physical runtime or admitting
     * Pages. Cache only physical inputs; world fields are composed at the exact
     * receiver position on every query, including same-tick field updates.
     */
    public static void gameplayItemEnvironment(
            ItemEntity entity, double naturalTemperatureC, ThermalEnvironmentSample out) {
        Objects.requireNonNull(entity, "entity");
        if (!(entity.level() instanceof ServerLevel server)) {
            out.clear();
            return;
        }
        gameplayExposedReservoirEnvironment(
                server,
                receiverKey(entity),
                entity.getId() & Integer.MAX_VALUE,
                entity.getX(),
                entity.getY() + entity.getBbHeight() * 0.5D,
                entity.getZ(),
                naturalTemperatureC,
                out);
    }

    /** Samples just above a placed reservoir using the same budget and boundary as dropped items. */
    public static void gameplayPlacedReservoirEnvironment(
            ServerLevel server,
            BlockPos position,
            double naturalTemperatureC,
            ThermalEnvironmentSample out) {
        gameplayExposedReservoirEnvironment(
                server,
                position.asLong(),
                0,
                position.getX() + 0.5D,
                position.getY() + 0.3125D,
                position.getZ() + 0.5D,
                naturalTemperatureC,
                out);
    }

    private static void gameplayExposedReservoirEnvironment(
            ServerLevel server,
            long receiverIdentity,
            int receiverGeneration,
            double x,
            double y,
            double z,
            double naturalTemperatureC,
            ThermalEnvironmentSample out) {
        Objects.requireNonNull(out, "out").clear();
        if (!server.getServer().isSameThread() || !Double.isFinite(naturalTemperatureC)) return;
        MinecraftThermalInput input = active(server);
        long tick = server.getGameTime();
        if (input == null) {
            double dormant =
                    dormantTemperatureAt(
                            server,
                            x,
                            y,
                            z,
                            tick,
                            DORMANT_QUERY_POSITION.get());
            if (Double.isFinite(dormant)) out.setAir(dormant);
        } else {
            int quarterX = floorQuarter(x);
            int quarterY = floorQuarter(y);
            int quarterZ = floorQuarter(z);
            int cached = input.itemEnvironmentCache.find(tick, quarterX, quarterY, quarterZ);
            if (cached >= 0) {
                input.itemEnvironmentCache.copyTo(cached, out);
                {
                    double radiation = out.radiantFluxWPerM2();
                    out.clear();
                    input.sampleAir(x, y, z, tick, MAX_PUBLICATION_AGE_TICKS, out);
                    out.setRadiation(radiation);
                }
            } else {
                input.sampleAir(x, y, z, tick, MAX_PUBLICATION_AGE_TICKS, out);
                if (input.itemEnvironmentCache.canAdmit() && input.radiation != null) {
                    input.radiation.sampleItem(
                            receiverIdentity, receiverGeneration, x, y, z, input.radiationSample);
                    out.setRadiation(input.radiationSample.radiantFluxWPerM2());
                }
                input.itemEnvironmentCache.store(quarterX, quarterY, quarterZ, out);
            }
        }
        double base = out.airAvailable() ? out.airTemperatureC() : naturalTemperatureC;
        ThermalAnalyticFieldIndex fields =
                input == null ? MinecraftGameplayFields.existing(server) : input.analyticFields;
        out.setComposedAir(
                fields == null ? base : fields.compose(x, y, z, naturalTemperatureC, base));
    }

    public static double gameplayCropEnvironment(
            LevelAccessor level, BlockPos position, double naturalTemperatureC) {
        return gameplayPassiveEnvironment(level, position, naturalTemperatureC);
    }

    public static double gameplayTownEnvironment(
            LevelAccessor level, TownThermalProjection projection, double naturalTemperatureC) {
        if (!(level instanceof ServerLevel server)
                || projection.voxelCount() == 0
                || !Double.isFinite(naturalTemperatureC)
                || !server.getServer().isSameThread()) {
            return naturalTemperatureC;
        }
        MinecraftThermalInput input = active(server);
        ThermalAnalyticFieldIndex fields =
                input == null ? MinecraftGameplayFields.existing(server) : input.analyticFields;
        ThermalAnalyticFieldIndex.Sample fieldSample =
                fields == null || fields.isEmpty() ? null : TOWN_FIELD_SAMPLE.get();
        int totalWeight = 0;
        double total = 0.0D;
        for (long key : projection.groupKeys()) {
            int weight = projection.weight(key);
            if (weight <= 0) continue;
            int x = projection.representativeX(key);
            int y = projection.representativeY(key);
            int z = projection.representativeZ(key);
            if (fieldSample != null) fields.sample(x + 0.5D, y + 0.5D, z + 0.5D, fieldSample);
            double natural = Double.NaN;
            double base;
            if (input != null) {
                ThermalEnvironmentSample out = input.townScratch;
                out.clear();
                input.sampleAir(
                        x + 0.5D,
                        y + 0.5D,
                        z + 0.5D,
                        server.getGameTime(),
                        MAX_PUBLICATION_AGE_TICKS,
                        out);
                if (out.airAvailable()) {
                    base = out.airTemperatureC();
                } else {
                    input.townPosition.set(x, y, z);
                    base = natural = WorldTemperature.naturalBlock(server, input.townPosition);
                }
            } else {
                double dormant =
                        dormantTemperature(
                                server,
                                x,
                                y,
                                z,
                                server.getGameTime(),
                                DORMANT_QUERY_POSITION.get());
                base =
                        Double.isFinite(dormant)
                                ? dormant
                                : (natural =
                                        WorldTemperature.naturalBlock(
                                                server, DORMANT_QUERY_POSITION.get().set(x, y, z)));
            }
            if (fieldSample != null) {
                if (fieldSample.requiresNatural() && !Double.isFinite(natural)) {
                    natural =
                            WorldTemperature.naturalBlock(
                                    server, DORMANT_QUERY_POSITION.get().set(x, y, z));
                }
                base = fieldSample.compose(natural, base);
            }
            total += base * weight;
            totalWeight += weight;
        }
        return totalWeight == 0 ? naturalTemperatureC : total / totalWeight;
    }

    public static List<ThermalAnalyticField> gameplayAnalyticFieldsAt(
            ServerLevel level, BlockPos position) {
        if (!level.getServer().isSameThread()) return List.of();
        ThermalAnalyticFieldIndex fields = MinecraftGameplayFields.existing(level);
        return fields == null
                ? List.of()
                : fields.fieldsAt(
                        position.getX() + 0.5D, position.getY() + 0.5D, position.getZ() + 0.5D);
    }

    public static boolean hasGameplayAnalyticFieldAt(ServerLevel level, BlockPos position) {
        if (!level.getServer().isSameThread()) return false;
        ThermalAnalyticFieldIndex fields = MinecraftGameplayFields.existing(level);
        return fields != null
                && fields.appliesAt(
                        position.getX() + 0.5D, position.getY() + 0.5D, position.getZ() + 0.5D);
    }

    public static InfraredSnapshot gameplayInfraredSnapshot(
            ServerPlayer player,
            boolean forceFull,
            long knownGeneration,
            long knownCenter,
            int lastEpoch,
            long[] knownPresence,
            boolean knownReadable,
            long[] knownFieldPages,
            long knownStoredEpoch,
            long knownStoredSampleTick) {
        if (knownPresence.length != InfraredCapture.INFRARED_PRESENCE_WORDS
                || knownFieldPages.length != 0
                        && knownFieldPages.length != InfraredCapture.INFRARED_PRESENCE_WORDS)
            throw new IllegalArgumentException("infrared presence requires 12 words");
        if (infraredCapture == null) infraredCapture = new InfraredCapture();
        MinecraftThermalInput input = active(player.serverLevel());
        return infraredCapture.capture(
                input == null ? null : input.pages,
                input == null ? null : input.queryPublication,
                input == null ? 0 : input.dimensionGeneration,
                player,
                forceFull,
                knownGeneration,
                knownCenter,
                lastEpoch,
                knownPresence,
                knownReadable,
                knownFieldPages,
                knownStoredEpoch,
                knownStoredSampleTick);
    }

    public static double materialTemperature(ServerLevel level, BlockPos position) {
        var sample = MATERIAL_SAMPLE.get();
        return sampleMaterial(level, position, sample) ? sample.temperatureC() : Double.NaN;
    }

    /** Reads a body without creating physical residency or substituting environmental temperature. */
    public static boolean sampleMaterial(ServerLevel level, BlockPos position, MaterialSample out) {
        out.clear();
        MinecraftThermalInput input = active(level);
        int x = position.getX(), y = position.getY(), z = position.getZ();
        long section =
                SectionPos.asLong(
                        SectionPos.blockToSectionCoord(x),
                        SectionPos.blockToSectionCoord(y),
                        SectionPos.blockToSectionCoord(z));
        if (input != null) {
            ThermalPageHandle handle = input.pages.handle(section);
            if (handle != null) {
                PagePublication publication = handle.lastPublication();
                if (readPublishedMaterial(input, handle, publication, section, position, out, false))
                    return true;
                if (readPublishedMaterial(input, handle, handle.previousPublication(), section,
                        position, out, true)) return true;
            }
        }
        LevelChunk chunk =
                level.getChunkSource().getChunkNow(SectionPos.x(section), SectionPos.z(section));
        DormantChunkThermalState stored = chunk == null ? null : dormantState(chunk);
        if (stored == null || !stored.hasMaterials(SectionPos.y(section))) return false;
        BlockState state = chunk.getBlockState(position);
        var law = MinecraftThermalProfiles.materialLaw(state);
        if (law == null) return false;
        if (stored.readMaterial(
                        SectionPos.y(section),
                        (x & 15) | (z & 15) << 4 | (y & 15) << 8,
                        Block.BLOCK_STATE_REGISTRY.getId(state),
                        out)
                != DormantChunkThermalState.MaterialRead.MATCH) return false;
        stored.projectMaterial(level, position, law, out, DORMANT_QUERY_POSITION.get());
        return true;
    }

    private static boolean readPublishedMaterial(MinecraftThermalInput input,
            ThermalPageHandle handle, PagePublication publication, long section,
            BlockPos position, MaterialSample out, boolean previous) {
        if (publication == null) return false;
        int x = position.getX(), y = position.getY(), z = position.getZ();
        int block = (x & 15) | (z & 15) << 4 | (y & 15) << 8;
        if (publication.geometryRevision() != handle.liveGeometryRevision()
                && input.pages.materialChangedSince(section, block, publication.geometryRevision()))
            return false;
        var brick = publication.brickAt(x & 15, y & 15, z & 15);
        if (!brick.resolved() || brick.firstSlot() < 0 || brick.blockLayout() == null)
            return false;
        int localBlock = (x & 3) | (z & 3) << 2 | (y & 3) << 4;
        int node = brick.blockLayout().nodeAt(localBlock);
        if (node < 0) return false;
        int stateId = input.profiles.signatures().materialStateId(brick.signatureAtBlock(localBlock));
        if (previous) {
            PagePublication current = handle.lastPublication();
            if (current == null || current.topologyGeneration() <= publication.topologyGeneration())
                return false;
            LevelChunk chunk = input.level.getChunkSource().getChunkNow(SectionPos.x(section), SectionPos.z(section));
            int signature = brick.signatureAtBlock(localBlock);
            if (chunk == null || !input.profiles.signatures().valid(signature)
                    || input.profiles.signatures().materialStateId(signature)
                    != Block.BLOCK_STATE_REGISTRY.getId(chunk.getBlockState(position))) return false;
            return input.queryPublication.tryReadMaterial(
                            brick.firstSlot() + node, brick.arenaGeneration(),
                            stateId, out)
                    && handle.previousPublication() == publication
                    && handle.lastPublication() == current;
        }
        return input.queryPublication.tryReadMaterial(
                        brick.firstSlot() + node,
                        brick.arenaGeneration(),
                        stateId, out)
                && handle.lastPublication() == publication;
    }

    /** Existing random updates select the location. This method never creates residency. */
    public static MinecraftPhaseController.PhaseAttempt tryMaterialPhaseAtRandomTick(
            ServerLevel level, LevelChunk chunk, BlockPos position, BlockState state) {
        return MinecraftPhaseController.tryAtRandomTick(level, chunk, position, state);
    }

    /** Final display transaction; readable describes the live worker baseline, independent of stored bodies and fields. */
    public record InfraredSnapshot(
            int centerChunkX,
            int centerChunkZ,
            int centerSectionY,
            long generation,
            int infraredEpoch,
            boolean readable,
            boolean full,
            long[] presence,
            long[] fieldPages,
            byte[][] brickRecords,
            long storedEpoch,
            long storedSampleTick) {}

    public static BlockPos nearestGameplayGenerator(
            Level level, BlockPos position, double maximumDistanceBlocks) {
        if (!(level instanceof ServerLevel server)
                || !server.getServer().isSameThread()
                || !Double.isFinite(maximumDistanceBlocks)
                || maximumDistanceBlocks <= 0.0D) {
            return null;
        }
        MinecraftThermalInput input = active(server);
        return input == null
                ? null
                : input.physicalSources.nearestEnabledGenerator(
                        position, maximumDistanceBlocks * maximumDistanceBlocks);
    }

    public static boolean requestGameplayPhase(
            ServerLevel level, BlockPos position, BlockState state, byte branch) {
        MinecraftThermalInput input = active(level);
        if (input == null) return false;
        int signature = input.profiles.states().signatureId(state);
        var profile =
                input.profiles
                        .materials()
                        .profileOrNull(input.profiles.signatures().materialProfileId(signature));
        if (profile == null
                || profile.thermalLaw() == null
                || profile.thermalLaw().transition(branch) == null) return false;
        var page = input.pages.handle(SectionPos.asLong(position));
        if (page == null) return false;
        int block =
                (position.getX() & 15) | (position.getZ() & 15) << 4 | (position.getY() & 15) << 8;
        input.accumulator.requestPhase(
                position.asLong(), page, block, Block.BLOCK_STATE_REGISTRY.getId(state), branch);
        return true;
    }

    public static void prepareGameplayProfiles() {
        MinecraftThermalProfiles.prepare();
    }

    public static void invalidateGameplayProfilesForRecipeReload() {
        closeAll();
        MinecraftThermalProfiles.invalidate();
    }

    /** One lifecycle scan of loaded block-entity positions; never loads chunks. */
    public static void bootstrapLoadedSources(MinecraftServer server) {
        if (!server.isSameThread()) return;
        for (ServerLevel level : server.getAllLevels()) {
            if (active(level) != null) continue;
            startLoadedSources(level);
        }
    }

    private static MinecraftThermalInput startLoadedSources(ServerLevel level) {
        for (var holder : level.getChunkSource().chunkMap.getChunks()) {
            ChunkPos pos = holder.getPos();
            LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
            MinecraftThermalInput input = chunk == null ? null : startFromCampfires(level, chunk);
            if (input != null) return input;
            if (startupRetries.containsKey(level)) break;
        }
        return null;
    }

    private static MinecraftThermalInput startFromCampfires(ServerLevel level, LevelChunk chunk) {
        if (!level.getServer().isSameThread()) return null;
        var cold = dormantState(chunk);
        if (cold != null && !cold.coldPending().empty())
            return start(level, chunk.getPos().getMiddleBlockPosition(level.getMinBuildHeight()));
        for (BlockPos pos : chunk.getBlockEntitiesPos()) {
            if (CampfireBlock.isLitCampfire(chunk.getBlockState(pos))) {
                if (MinecraftThermalProfiles.prepare().tuning().campfire().ratedPowerW() <= 0)
                    return null;
                return start(level, pos);
            }
        }
        return null;
    }

    public static void onSectionSetBlockState(
            MinecraftPageManager.SectionOwner owner,
            int localX,
            int localY,
            int localZ,
            BlockState oldState,
            BlockState newState) {
        if (oldState == newState || owner == null || !owner.isValid()) return;
        MinecraftThermalInput input = owner.input();
        if (input == null || input.closed) return;
        var states = input.profiles.states();
        int oldCode = states.code(oldState), newCode = states.code(newState);
        int flags = states.mutationFlagsFromCodes(oldCode, newCode);
        int oldSignature = states.signatureIdFromCode(oldCode);
        int newSignature = states.signatureIdFromCode(newCode);
        boolean physicalRelevant = !input.profiles.signatures().samePhysicalProperties(oldSignature, newSignature);
        if (input.profiles.signatures().materialProfileId(oldSignature) != 0
                || input.profiles.signatures().materialProfileId(newSignature) != 0) {
            int worldX =
                    SectionPos.sectionToBlockCoord(SectionPos.x(owner.sectionKey()))
                            + localX;
            int worldY =
                    SectionPos.sectionToBlockCoord(SectionPos.y(owner.sectionKey()))
                            + localY;
            int worldZ =
                    SectionPos.sectionToBlockCoord(SectionPos.z(owner.sectionKey()))
                            + localZ;
            byte cause =
                    MinecraftPhaseController.materialChangeCause(
                            oldState, newState, worldX, worldY, worldZ);
            if (cause != 0) {
                owner.recordMaterialChange(
                        localX | localZ << 4 | localY << 8,
                        oldSignature,
                        newSignature,
                        cause);
                flags |= MinecraftThermalProfiles.TOPOLOGY_MUTATION;
                physicalRelevant = true;
            }
        }
        if ((flags & MinecraftThermalProfiles.TOPOLOGY_MUTATION) != 0) {
            input.pages.onBlockMutation(
                    owner,
                    localX,
                    localY,
                    localZ,
                    physicalRelevant);
        }
        if (input.blockRadiation != null)
            input.blockRadiation.onStateChanged(owner, localX, localY, localZ);
        if ((flags & MinecraftThermalProfiles.OCCLUSION_MUTATION) != 0) {
            long sectionKey = owner.sectionKey();
            input.radiationOcclusion.onSectionMutation(
                    SectionPos.x(sectionKey),
                    SectionPos.y(sectionKey),
                    SectionPos.z(sectionKey));
        }
    }

    public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        DormantChunkThermalState state = dormantState(chunk);
        // A newly loaded chunk can replace a client's previous stored display,
        // including when the new chunk has no material records at all.
        setDormantState(chunk, state);
        MinecraftThermalInput input = active(level);
        if (input == null && state != null && !state.coldPending().empty())
            input = start(level, chunk.getPos().getMiddleBlockPosition(level.getMinBuildHeight()));
        if (input == null) input = startFromCampfires(level, chunk);
        if (input != null) {
            input.attachLoadedChunk(chunk);
            if (input.blockRadiation != null) {
                input.blockRadiation.onChunkLoad(chunk);
            }
            input.radiationOcclusion.onChunkLoad(chunk);
        }
    }

    public static void onChunkUnload(ServerLevel level, LevelChunk chunk) {
        MinecraftThermalInput input = active(level);
        if (input != null) {
            LevelChunk current =
                    level.getChunkSource().getChunkNow(chunk.getPos().x, chunk.getPos().z);
            if (current != null && current != chunk) return;
            boolean checkpointCaptured = input.preparedNativeUnload == chunk;
            if (input.needsChunkCheckpoint(chunk)) {
                if (!checkpointCaptured) checkpointCaptured = input.captureRegionalCheckpoint(chunk, true);
            }
            else input.unrelatedUnloadSkips++;
            input.coldRestores.remove(chunk.getPos().toLong());
            input.pendingSourceChunks.remove(chunk.getPos().toLong(), chunk);
            var discovery = input.sourceDiscoveryCursors.get(chunk.getPos().toLong());
            if (discovery != null && discovery.chunk == chunk) input.sourceDiscoveryCursors.remove(chunk.getPos().toLong());
            if (input.blockRadiation != null) {
                input.blockRadiation.onChunkUnload(chunk);
            }
            input.pages.onChunkUnload(chunk, checkpointCaptured);
            input.physicalSources.beforeChunkUnload(chunk, level.getGameTime());
            input.radiationOcclusion.onChunkUnload(chunk);
        }
    }

    public static void tickLevel(ServerLevel level) {
        MinecraftThermalInput input = active(level);
        if (input == null) input = retryStartup(level);
        if (input != null) input.tick();
    }

    private static MinecraftThermalInput retryStartup(ServerLevel level) {
        StartupRetry retry = startupRetries.get(level);
        if (retry == null || retry.capacityRevision() == MEMORY.capacityRevision()) return null;
        startupRetries.remove(level);
        BlockPos preferred = BlockPos.of(retry.preferredPosition());
        LevelChunk chunk = level.getChunkSource().getChunkNow(preferred.getX() >> 4, preferred.getZ() >> 4);
        var stored = chunk == null ? null : dormantState(chunk);
        if (chunk != null && ((stored != null && !stored.coldPending().empty())
                || CampfireBlock.isLitCampfire(chunk.getBlockState(preferred))
                        && MinecraftThermalProfiles.prepare().tuning().campfire().ratedPowerW() > 0))
            return start(level, preferred);
        return startLoadedSources(level);
    }

    public static void closeActiveLevel(ServerLevel level) {
        startupRetries.remove(level);
        MinecraftThermalInput input = active(level);
        if (input != null) input.close();
    }

    public static void onPlayerLogout(ServerPlayer player) {
        MinecraftThermalInput input = active(player.serverLevel());
        if (input != null) {
            if (input.radiation != null) {
                input.radiation.removeReceiver(receiverKey(player));
            }
        }
    }

    public static void onPlayerChangedDimension(ServerPlayer player, ServerLevel previousLevel) {
        MinecraftThermalInput input = active(previousLevel);
        if (input != null && input.radiation != null) {
            input.radiation.removeReceiver(receiverKey(player));
        }
    }

    public static void closeAll() {
        MinecraftThermalInput[] inputs;
        synchronized (ACTIVE) {
            inputs = ACTIVE.values().toArray(MinecraftThermalInput[]::new);
            startupRetries.clear();
        }
        for (MinecraftThermalInput input : inputs) input.close();
        ThermalWorkerPool.closeShared();
        if (infraredCapture != null) {
            infraredCapture.close();
            infraredCapture = null;
        }
    }

    public static void onRawBlockContainerReplaced(LevelChunkSection section) {
        MinecraftPageManager.SectionOwner owner =
                ((MinecraftThermalSectionAttachment) (Object) section)
                        .frostedheart$getThermalInputOwner();
        if (owner != null) {
            owner.recordFullResync(ThermalPageHandle.GeometryResyncReason.SECTION_REPLACED);
            MinecraftThermalInput input = owner.input();
            if (Thread.currentThread() == input.mainThread) {
                input.invalidateSectionDependents(owner.sectionKey());
            } else {
                input.level.getServer().execute(() -> {
                    if (!input.closed && owner.isValid()) input.invalidateSectionDependents(owner.sectionKey());
                });
            }
        }
    }

    private void invalidateSectionDependents(long sectionKey) {
        physicalSources.invalidateSection(sectionKey);
        if (blockRadiation != null) blockRadiation.onSectionIdentityReplaced(sectionKey);
    }

    public static void onSectionIdentityReplaced(
            ServerLevel level, LevelChunk chunk, int sectionIndex, LevelChunkSection previous) {
        DormantChunkThermalState stored = dormantState(chunk);
        if (stored != null && sectionIndex >= 0 && sectionIndex < chunk.getSections().length) {
            stored.replaceMaterials(chunk.getSectionYFromSectionIndex(sectionIndex), null);
            chunk.setUnsaved(true);
        }
        MinecraftThermalInput input = active(level);
        if (input != null && sectionIndex >= 0 && sectionIndex < chunk.getSections().length) {
            input.pages.onSectionIdentityReplaced(
                    chunk, sectionIndex, previous, chunk.getSections()[sectionIndex]);
            input.invalidateSectionDependents(SectionPos.asLong(
                    chunk.getPos().x, chunk.getSectionYFromSectionIndex(sectionIndex), chunk.getPos().z));
            input.radiationOcclusion.onSectionIdentityReplaced(
                    chunk.getPos().x,
                    chunk.getSectionYFromSectionIndex(sectionIndex),
                    chunk.getPos().z);
        }
    }

    public static void onGeneratorTick(
            ServerLevel level,
            BlockPos source,
            BlockPos target,
            double thermalLevel,
            boolean active) {
        observeMachine(
                level,
                source,
                target,
                MinecraftPhysicalSourceProfile.GENERATOR,
                thermalLevel,
                active);
    }

    public static void onFountainTick(
            ServerLevel level,
            BlockPos source,
            BlockPos target,
            double thermalLevel,
            boolean active) {
        observeMachine(
                level,
                source,
                target,
                MinecraftPhysicalSourceProfile.FOUNTAIN,
                thermalLevel,
                active);
    }

    public static void onRadiatorTick(
            ServerLevel level,
            BlockPos source,
            BlockPos target,
            double thermalLevel,
            boolean active) {
        observeMachine(
                level,
                source,
                target,
                MinecraftPhysicalSourceProfile.RADIATOR,
                thermalLevel,
                active);
    }

    private static void observeMachine(
            ServerLevel level,
            BlockPos source,
            BlockPos target,
            MinecraftPhysicalSourceProfile profile,
            double thermalLevel,
            boolean enabled) {
        MinecraftThermalInput input = active(level);
        if (input == null
                && enabled
                && profile.powerForLevel(thermalLevel) > 0
                && level.getServer().isSameThread()) {
            input = start(level, source);
        }
        if (input != null)
            input.physicalSources.observeMachine(source, target, profile, thermalLevel, enabled);
    }

    public static void onPhysicalSourceRemoved(ServerLevel level, BlockPos source) {
        MinecraftThermalInput input = active(level);
        if (input != null)
            input.physicalSources.remove(source.getX(), source.getY(), source.getZ());
    }

    @Override
    public void close() {
        requireMainThread();
        if (closed) return;
        stopping = true;
        settleNativePreparationForSave();
        drainCompletion();
        pages.checkpointAll(false);
        closed = true;
        for (NativeUnloadCheckpoint checkpoint : nativeCheckpoints.keySet().toArray(NativeUnloadCheckpoint[]::new)) {
            checkpoint.finishForStop();
            checkpoint.release();
        }
        nativePreparationQueue.clear(); nativePreparation = null;
        coldRestores.clear();
        pendingSourceChunks.clear();
        sourceDiscoveryCursors.clear();
        physicalSources.close();
        accumulator.close();
        if (blockRadiation != null) blockRadiation.close();
        pages.close();
        environment.close();
        if (radiation != null) radiation.close();
        itemEnvironmentCache.clear();
        mailbox.close();
        materialCapture.clearMixedTicks();
        if (checkpointTimeScratch != null) { checkpointTimeScratch.close(); checkpointTimeScratch = null; }
        synchronized (ACTIVE) {
            ACTIVE.remove(level, this);
        }
    }

    public ThermalInputBatch.DormantAirCut dormantAdmissionCut(
            long sectionKey, double naturalTemperatureC, long gameTick) {
        LevelChunk chunk =
                level.getChunkSource()
                        .getChunkNow(SectionPos.x(sectionKey), SectionPos.z(sectionKey));
        DormantChunkThermalState state = chunk == null ? null : dormantState(chunk);
        return state == null
                ? null
                : state.admissionCut(
                        SectionPos.y(sectionKey),
                        gameTick,
                        profiles.tuning().dormantTemperatureHalfLifeSeconds(),
                        naturalTemperatureC);
    }

    public boolean captureDormantPage(ThermalPageHandle page) {
        LevelChunk chunk =
                level.getChunkSource()
                        .getChunkNow(
                                SectionPos.x(page.sectionKey()), SectionPos.z(page.sectionKey()));
        if (chunk != null) {
            return captureDormantPage(page, chunk);
        }
        return false;
    }

    public ThermalInputBatch.DormantMaterialCut dormantMaterialAdmissionCut(long sectionKey) {
        LevelChunk chunk =
                level.getChunkSource()
                        .getChunkNow(SectionPos.x(sectionKey), SectionPos.z(sectionKey));
        DormantChunkThermalState stored = chunk == null ? null : dormantState(chunk);
        MaterialSectionState state =
                stored == null ? null : stored.materials(SectionPos.y(sectionKey));
        if (state == null) return null;
        long tick = level.getGameTime();
        double natural =
                stored.naturalTemperature(
                        level,
                        SectionPos.x(sectionKey),
                        SectionPos.y(sectionKey),
                        SectionPos.z(sectionKey),
                        tick,
                        dormantPosition);
        return new ThermalInputBatch.DormantMaterialCut(
                state, tick, natural, DormantThermalCooling.rate(dormantHalfLifeSeconds()));
    }

    public long materialPublicationRevision(ThermalPageHandle page) {
        try {
            PagePublication publication = page.lastPublication();
            return publication != null
                        && queryPublication.beginRead(checkpointCursor)
                        && checkpointCursor.valid()
                        && checkpointCursor.topologyGeneration() >= publication.topologyGeneration()
                        && checkpointCursor.isCurrent()
                ? publication.geometryRevision()
                : -1;
        } finally { checkpointCursor.clear(); }
    }

    /** Only campfire lifecycle callbacks produce these notifications. */
    public static void onCampfireStateChanged(ServerLevel level, BlockPos position) {
        if (!level.getServer().isSameThread()) {
            BlockPos capturedPosition = position.immutable();
            level.getServer().execute(() -> onCampfireStateChanged(level, capturedPosition));
            return;
        }
        LevelChunk chunk = level.getChunkSource().getChunkNow(position.getX() >> 4, position.getZ() >> 4);
        MinecraftThermalInput input = active(level);
        if (chunk == null) return;
        if (level.captureBlockSnapshots) {
            if (input != null) input.physicalSources.deferCampfireState(position.getX(), position.getY(), position.getZ());
            return;
        }
        BlockState state = chunk.getBlockState(position);
        if (input == null && CampfireBlock.isLitCampfire(state)
                && MinecraftThermalProfiles.prepare().tuning().campfire().ratedPowerW() > 0)
            input = start(level, position);
        if (input != null && !input.physicalSources.resyncBlock(
                position.getX(), position.getY(), position.getZ(), state))
            input.enqueueSourceDiscovery(chunk);
    }

    /** Also runs without an active dimension worker; saved matter must follow world replacement. */
    public static void onMaterialStateWritten(
            LevelChunk chunk, BlockPos position, BlockState previous, BlockState next) {
        if (previous == null
                || previous == next
                || !(chunk.getLevel() instanceof ServerLevel level)) return;
        if (!level.getServer().isSameThread()) {
            BlockPos capturedPosition = position.immutable();
            level.getServer()
                    .execute(() -> onMaterialStateWritten(chunk, capturedPosition, previous, next));
            return;
        }
        // onPlace may have performed another write before this setter returned.
        if (chunk.getBlockState(position) != next) return;
        DormantChunkThermalState stored = dormantState(chunk);
        int sectionY = SectionPos.blockToSectionCoord(position.getY());
        int sectionIndex = chunk.getSectionIndex(position.getY());
        if (sectionIndex < 0 || sectionIndex >= chunk.getSections().length) return;
        var owner =
                ((MinecraftThermalSectionAttachment) (Object) chunk.getSections()[sectionIndex])
                        .frostedheart$getThermalInputOwner();
        int block =
                (position.getX() & 15) | (position.getZ() & 15) << 4 | (position.getY() & 15) << 8;
        boolean saved = stored != null && stored.hasMaterial(sectionY, block);
        if (!saved && (owner == null || owner.page() == null)) return;
        byte cause =
                MinecraftPhaseController.materialChangeCause(
                        previous, next, position.getX(), position.getY(), position.getZ());
        int nextId = Block.BLOCK_STATE_REGISTRY.getId(next);
        if (owner != null) owner.recordMaterialCheckpointChange(block, nextId, cause);
        if (saved) {
            var nextLaw = MinecraftThermalProfiles.materialLaw(next);
            var transition = MinecraftPhaseController.dormantTransition(position, previous, next);
            if (transition != null) {
                if (stored.updateMaterial(
                        sectionY,
                        block,
                        nextId,
                        nextLaw,
                        transition.energyJ(),
                        (byte) 0,
                        transition.tick())) chunk.setUnsaved(true);
                return;
            }
            boolean live = owner != null && owner.ownsMaterialPosition(block);
            double rate = live ? 0 : DormantThermalCooling.rate(dormantHalfLifeSeconds());
            double natural = 0;
            double coolingNatural = 0;
            if (nextLaw != null) {
                if (cause == ResolvedGeometryBatch.MaterialChanges.REPLACE
                        || cause == ResolvedGeometryBatch.MaterialChanges.MASS_CHANGE)
                    natural = WorldTemperature.naturalAir(level, position);
                if (!live && cause != ResolvedGeometryBatch.MaterialChanges.REPLACE)
                    coolingNatural =
                            stored.naturalTemperature(
                                    level,
                                    chunk.getPos().x,
                                    sectionY,
                                    chunk.getPos().z,
                                    level.getGameTime(),
                                    DORMANT_QUERY_POSITION.get());
            }
            if (stored.applyMaterialChange(
                    sectionY,
                    block,
                    nextId,
                    nextLaw,
                    cause,
                    natural,
                    level.getGameTime(),
                    rate,
                    coolingNatural)) chunk.setUnsaved(true);
        }
    }

    private boolean captureDormantPage(ThermalPageHandle page, LevelChunk chunk) {
        try { return captureDormantPageWithCursor(page, chunk); }
        finally { checkpointCursor.clear(); }
    }

    private boolean captureDormantPageWithCursor(ThermalPageHandle page, LevelChunk chunk) {
        ensureCheckpointTimeScratch();
        PagePublication publication;
        DormantChunkThermalState.SectionEntry capturedAir = null;
        MaterialSectionState.Capture capturedMaterials = null;
        long materialSnapshotRevision = -1;
        int sectionX = SectionPos.x(page.sectionKey());
        int sectionY = SectionPos.y(page.sectionKey());
        int sectionZ = SectionPos.z(page.sectionKey());
        dormantPosition.set(
                SectionPos.sectionToBlockCoord(sectionX) + 8,
                SectionPos.sectionToBlockCoord(sectionY) + 8,
                SectionPos.sectionToBlockCoord(sectionZ) + 8);
        double natural = WorldTemperature.naturalAir(level, dormantPosition);
        double airNatural = natural;
        long captureTick = -1;
        DormantChunkThermalState state = dormantState(chunk);
        for (int attempt = 0; attempt < 2; attempt++) {
            publication = page.lastPublication();
            if (publication == null) return false;
            if (!queryPublication.beginRead(checkpointCursor)) continue;
            captureTick = (checkpointCursor.pageAirSampleTick(publication));
            if ((captureTick < 0)) {
                PagePublication previous = page.previousPublication();
                if (previous != null && checkpointCursor.pageBeforeTopology(previous, publication.topologyGeneration())) {
                    long previousTick = checkpointCursor.pageAirSampleTick(previous);
                    if (previousTick >= 0) { publication = previous; captureTick = previousTick; }
                }
            }
            if (captureTick < 0) continue;
            airNatural =
                    state == null
                            ? natural
                            : state.captureNatural(
                                    sectionY, captureTick, natural);
            var air = checkpointCursor.captureValidatedAir(publication, airNatural);
            capturedAir = air == null ? null : new DormantChunkThermalState.SectionEntry(captureTick, airNatural, air);
            capturedMaterials =
                    MaterialSectionState.capture(
                            publication, queryPublication, profiles.signatures(), materialCapture, true,
                            (checkpointCursor));
            if (capturedMaterials.valid() && matchesCheckpointPage(page, publication, checkpointCursor)) {
                materialSnapshotRevision = publication.geometryRevision();
                break;
            }
            capturedMaterials = null;
        }
        if (capturedMaterials == null) return false;
        if (state == null && (capturedAir != null || capturedMaterials.state() != null)) {
            state =
                    new DormantChunkThermalState(
                            chunk.getSectionYFromSectionIndex(0), chunk.getSections().length);
            setDormantState(chunk, state);
        }
        if (state == null) {
            return true;
        }
        boolean changed =
                state.mergeAir(
                        sectionY,
                        capturedAir,
                        capturedMaterials.sampledBricks(),
                        captureTick,
                        airNatural,
                        DormantThermalCooling.rate(dormantHalfLifeSeconds()),
                        dormantCapture);
        MaterialSectionState materialState = capturedMaterials.state();
        int sectionIndex = chunk.getSectionIndex(SectionPos.sectionToBlockCoord(sectionY));
        var owner =
                ((MinecraftThermalSectionAttachment) (Object) chunk.getSections()[sectionIndex])
                        .frostedheart$getThermalInputOwner();
        if (owner != null)
            materialState =
                    owner.projectMaterialCheckpoint(
                            materialState, materialSnapshotRevision, natural);
        changed |= state.mergeMaterials(sectionY, materialState, capturedMaterials.sampledBricks());
        if (state.isEmpty()) {
            setDormantState(chunk, null);
        }
        if (changed) {
            chunk.setUnsaved(true);
        }
        return true;
    }

    private static boolean matchesCheckpointPage(ThermalPageHandle page, PagePublication captured,
            QueryPublication.ReadCursor cursor) {
        PagePublication latest = page.lastPublication();
        return latest == captured || latest != null && page.previousPublication() == captured
                && cursor.pageBeforeTopology(captured, latest.topologyGeneration());
    }

    /** The ready checkpoint owns all values and writer space. No live query or arena identity is borrowed here. */
    private ChunkCheckpointCapture captureChunkSnapshot(LevelChunk chunk,
            com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.RegionalCheckpoint checkpoint) {
        var stored = dormantState(chunk);
        var target = stored == null
                ? new DormantChunkThermalState(chunk.getSectionYFromSectionIndex(0), chunk.getSections().length)
                : stored.copyForCapture();
        int stale = 0;
        long minimumTick = Long.MAX_VALUE, maximumTick = -1;
        for (var section : checkpoint.sections()) {
            int sectionY = SectionPos.y(section.sectionKey());
            dormantPosition.set(SectionPos.sectionToBlockCoord(SectionPos.x(section.sectionKey())) + 8,
                    SectionPos.sectionToBlockCoord(sectionY) + 8,
                    SectionPos.sectionToBlockCoord(SectionPos.z(section.sectionKey())) + 8);
            double natural = WorldTemperature.naturalAir(level, dormantPosition);
            double airNatural = target.captureNatural(sectionY, section.sampleTick(), natural);
            var air = section.captureAir(airNatural);
            var materials = MaterialSectionState.capture(section, materialCapture);
            var materialState = materials.state();
            var owner = ((MinecraftThermalSectionAttachment) (Object) chunk.getSections()[
                    chunk.getSectionIndex(SectionPos.sectionToBlockCoord(sectionY))]).frostedheart$getThermalInputOwner();
            if (owner != null) {
                materialState = owner.projectMaterialCheckpoint(materialState, section.geometryRevision(), natural);
                var page = owner.page();
                if (page != null && page.liveGeometryRevision() != section.geometryRevision()) stale++;
            }
            target.mergeAir(sectionY, air == null ? null : new DormantChunkThermalState.SectionEntry(
                            section.sampleTick(), airNatural, air), section.sampledBricks(), section.sampleTick(),
                    airNatural, DormantThermalCooling.rate(dormantHalfLifeSeconds()), dormantCapture);
            target.mergeMaterials(sectionY, materialState, section.sampledBricks());
            minimumTick = Math.min(minimumTick, section.sampleTick()); maximumTick = Math.max(maximumTick, section.sampleTick());
        }
        return new ChunkCheckpointCapture(target, checkpoint.sections().size(), 0, stale,
                minimumTick == Long.MAX_VALUE ? -1 : minimumTick, maximumTick, null);
    }

    /** One owner for both synchronous saves and native asynchronous preparation. */
    private final class ChunkCapture {
        final LevelChunk chunk;
        final long tick, sourceRevision;
        final ThermalPageHandle[] identities;
        final long[] geometry;
        final com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline.Capture producer;
        CompletableFuture<com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.RegionalCheckpoint> ready;
        CompletableFuture<Void> finished;
        boolean producerReleased;

        ChunkCapture(LevelChunk chunk, boolean nativeHold,
                com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.RegionalCheckpoint.Purpose purpose) {
            this.chunk = chunk;
            tick = Math.max(level.getGameTime(), Math.max(lastSubmittedTargetTick,
                    pendingSubmission == null ? -1 : pendingSubmission.targetTick()));
            sourceCaptureTick = Math.max(sourceCaptureTick, tick);
            physicalSources.flush(tick);
            pages.flushCapturedGeometry();
            producer = accumulator.captureSources(tick);
            if (producer == null) throw new IllegalStateException("Thermal producer capture already owned");
            int count = nativeHold ? chunk.getSections().length : 0;
            identities = new ThermalPageHandle[count]; geometry = new long[count];
            for (int i = 0; i < count; i++) {
                var page = pages.handle(SectionPos.asLong(chunk.getPos().x, chunk.getSectionYFromSectionIndex(i), chunk.getPos().z));
                identities[i] = page; geometry[i] = page == null ? -1 : page.liveGeometryRevision();
            }
            sourceRevision = pages.sourceRevisionNearChunk(chunk.getPos().toLong());
            try {
                var stored = dormantState(chunk);
                ready = workerEngine.requestCheckpoint(chunk.getPos().toLong(), producer,
                        stored == null ? com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.PendingHeatCheckpoint.EMPTY
                                : stored.coldPending(), dormantHalfLifeSeconds(), purpose);
                mailbox.controlChanged();
            } catch (RuntimeException | Error failure) { producer.close(); throw failure; }
        }
        boolean geometryCurrent() {
            if (sourceRevision != pages.sourceRevisionNearChunk(chunk.getPos().toLong())) return false;
            for (int i = 0; i < identities.length; i++) {
                var page = pages.handle(SectionPos.asLong(chunk.getPos().x, chunk.getSectionYFromSectionIndex(i), chunk.getPos().z));
                if (page != identities[i] || page != null && page.liveGeometryRevision() != geometry[i]) return false;
            }
            return true;
        }
        /** Called on main only after the real ready receipt; no numerical work runs here. */
        void write(boolean unloading) {
            var checkpoint = ready.join();
            long now = Math.max(tick, level.getGameTime()), started = System.nanoTime();
            var event = new CheckpointSnapshot(); event.chunkX = chunk.getPos().x; event.chunkZ = chunk.getPos().z;
            event.gameTick = now; event.unloading = unloading; event.begin();
            long allocation = event.isEnabled() ? checkpointAllocatedBytes() : -1;
            long cpu = event.isEnabled() ? checkpointThreadCpuNanos() : -1;
            try {
                event.overlapBytes = checkpoint.captureCredit == null ? 0 : checkpoint.captureCredit.bytes();
                var captured = captureChunkSnapshot(chunk, checkpoint);
                event.pages = captured.pages; event.failedPages = captured.failedPages; event.staleGeometryPages = captured.staleGeometryPages;
                event.minimumSampleTick = captured.minimumSampleTick; event.maximumSampleTick = captured.maximumSampleTick;
                event.captureFailure = captured.failure;
                var heat = checkpoint.pending();
                if (now > tick) {
                    var joined = new com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput.Builder();
                    for (int record = 0; record < heat.size(); record++)
                        joined.appendAggregate(heat.target(record), heat.fromTick(record), heat.toTick(record), heat.energyJ(record));
                    checkpoint.collectAdditionalThrough(now, joined::appendAggregate); heat = joined.freeze();
                }
                sourceCaptureTick = Math.max(sourceCaptureTick, now);
                var pending = com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.PendingHeatCheckpoint.capture(heat,
                        incarnation -> {
                            for (var field : checkpoint.fields()) for (int body = 0; body < field.materialCount(); body++)
                                if (field.materialIncarnation(body) == incarnation) return field.materialStateId(body);
                            return -1;
                        }, now);
                var stored = dormantState(chunk);
                captured.state.setPending(pending, unloading ? pending : checkpoint.coldPending(),
                        unloading ? 0 : stored == null ? pages.restorationPageFloor(chunk.getPos().toLong()) : stored.restorationPageFloor());
                setDormantState(chunk, captured.state.isEmpty() ? null : captured.state);
                // A normal capture is already inside vanilla serialization, or prepares an
                // already-dirty maintenance write. Redirtying it makes flush save forever.
                // Native unload captures run before vanilla decides whether a write is needed.
                if (unloading) chunk.setUnsaved(true);
                event.accepted = true; event.emptyInitialization = captured.pages == 0 && pending.empty();
                lastAcceptedLiveCheckpointTick = captured.minimumSampleTick;
                snapshotCheckpoints++;
                snapshotMaximumAgeTicks = Math.max(snapshotMaximumAgeTicks, captured.minimumSampleTick < 0 ? 0 : now - captured.minimumSampleTick);
                snapshotMaximumOverlapBytes = Math.max(snapshotMaximumOverlapBytes, event.overlapBytes);
            } finally {
                snapshotMaximumNanos = Math.max(snapshotMaximumNanos, System.nanoTime() - started);
                long after = allocation < 0 ? -1 : checkpointAllocatedBytes(); event.allocatedBytes = allocation < 0 ? -1 : after - allocation;
                after = cpu < 0 ? -1 : checkpointThreadCpuNanos(); event.threadCpuNanos = cpu < 0 ? -1 : after - cpu;
                event.end(); event.commit();
            }
        }
        CompletableFuture<Void> finish(boolean unloading) {
            if (finished == null) {
                if (!unloading) finished = workerEngine.cancelCheckpoint(ready);
                else {
                    var checkpoint = ready.join(); checkpoint.releaseFrames(); finished = checkpoint.finish(true);
                }
                ready = null; // A queued native callback must not retain retired field arrays.
                mailbox.controlChanged();
            }
            return finished;
        }
        void releaseProducer() {
            if (producerReleased) return;
            if (finished == null || !finished.isDone()) throw new IllegalStateException("Producer release needs its real finish receipt");
            try { finished.join(); } finally { producerReleased = true; producer.close(); }
        }
    }

    public boolean captureRegionalCheckpoint(LevelChunk chunk, boolean unloading) {
        requireMainThread();
        if ((checkpointInProgress)) return false;
        checkpointInProgress = true;
        ChunkCapture capture = null;
        boolean saved = false;
        long started = System.nanoTime();
        try {
            settleNativePreparationForSave();
            drainCompletion();
            capture = new ChunkCapture(chunk, false, unloading
                    ? com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.RegionalCheckpoint.Purpose.UNLOAD
                    : com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.RegionalCheckpoint.Purpose.SAVE);
            awaitCheckpointCommand(capture.ready);
            capture.write(unloading); saved = true;
            return true;
        } finally {
            try {
                if (capture != null) { awaitCheckpointCommand(capture.finish(saved && unloading)); capture.releaseProducer(); }
            } finally {
                checkpointInProgress = false;
                // This counter records actual synchronous control handoff. Native
                // preparation has its own event and never enters this wait path.
                long elapsed = System.nanoTime() - started;
                if (unloading) {
                    unloadWaitNanos += elapsed; unloadWaitCalls++;
                    if (elapsed > maxUnloadWaitNanos) { maxUnloadWaitNanos = elapsed; maxUnloadWaitChunk = chunk.getPos().toLong(); }
                } else {
                    saveWaitNanos += elapsed; saveWaitCalls++;
                    if (elapsed > maxSaveWaitNanos) { maxSaveWaitNanos = elapsed; maxSaveWaitChunk = chunk.getPos().toLong(); }
                }
            }
        }
    }

    /** Cancel only an unconsumed native capture; a consumed export keeps its original finish. */
    private void settleNativePreparationForSave() {
        var active = nativePreparation;
        if (active == null || active.capture == null) return;
        active.expired = true;
        var capture = active.capture;
        awaitCheckpointCommand(capture.finish(false));
        capture.releaseProducer(); active.capture = null;
        if (nativePreparation == active) nativePreparation = null;
        active.ready.complete(active);
    }

    private void pumpNativePreparation() {
        var active = nativePreparation;
        if (active != null && active.capture != null) {
            if (active.capture.finished != null) {
                if (!active.capture.finished.isDone()) return;
                active.capture.releaseProducer(); active.capture = null; nativePreparation = null;
            } else if (!active.stillPending.getAsBoolean() || level.getGameTime() - active.capture.tick > MAX_PUBLICATION_AGE_TICKS) {
                active.expired = true; active.capture.finish(false); active.ready.complete(active);
                return;
            } else if (active.capture.ready.isDone()) active.prepared();
        }
        if (nativePreparation != null || checkpointInProgress || stopping || closed) return;
        while (!nativePreparationQueue.isEmpty()) {
            var next = nativePreparationQueue.removeFirst();
            if (next.released || next.expired) continue;
            if (!next.stillPending.getAsBoolean()) { next.cancelled(); next.ready.complete(next); continue; }
            nativePreparation = next;
            next.begin(); return;
        }
    }

    /** Background withdrawal waits for retained J, without asking main to solve it. */
    public boolean mayWithdrawThermalChunk(long chunk) {
        if (checkpointInProgress || nativePreparation != null && nativePreparation.chunk.getPos().toLong() == chunk) return false;
        if (workerEngine.hasRetainedInputInChunk(chunk) || !accumulator.sourcesCapturedThrough(chunk, level.getGameTime())) return false;
        if (batchTouchesChunk(pendingSubmission, chunk)) return false;
        return true;
    }

    private static long checkpointAllocatedBytes() {
        var bean = java.lang.management.ManagementFactory.getThreadMXBean();
        return bean instanceof com.sun.management.ThreadMXBean extended && extended.isThreadAllocatedMemorySupported()
                ? extended.getThreadAllocatedBytes(Thread.currentThread().getId()) : -1;
    }

    private static long checkpointThreadCpuNanos() {
        var bean = java.lang.management.ManagementFactory.getThreadMXBean();
        return bean.isCurrentThreadCpuTimeSupported() ? bean.getCurrentThreadCpuTime() : -1;
    }

    private void ensureCheckpointTimeScratch() {
        if (checkpointTimeScratch != null) return;
        checkpointTimeScratch = MEMORY.reserveCompletion(16L + 4096L * Long.BYTES);
        materialCapture.enableMixedTicks();
    }

    static void checkpointForSave(ServerLevel level, LevelChunk chunk) {
        MinecraftThermalInput input = active(level);
        if (input != null) {
            if (input.preparedNativeUnload == chunk || input.preparedMaintenanceSave == chunk) return;
            if (!input.needsChunkCheckpoint(chunk)) {
                input.unrelatedSaveSkips++;
                if (input.mailbox.workPending()) input.unrelatedSavesWhilePending++;
                return;
            }
            if (input.captureRegionalCheckpoint(chunk, false)) return;
            input.drainCompletion();
            input.pages.checkpointChunk(chunk);
        }
    }

    /**
     * Prepare before actual maintenance serialization. A busy checkpoint defers
     * the write; an admitted checkpoint may wait for ownership handoff here.
     * The subsequent Forge save callback uses the captured state.
     */
    public static boolean runMaintenanceSave(ServerLevel level, LevelChunk chunk, java.util.function.BooleanSupplier save) {
        MinecraftThermalInput input = active(level);
        if (input == null || chunk == null) return save.getAsBoolean();
        input.requireMainThread();
        if (input.needsChunkCheckpoint(chunk) && !input.captureRegionalCheckpoint(chunk, false)) {
            input.maintenanceSaveDeferrals++;
            return false;
        }
        LevelChunk previous = input.preparedMaintenanceSave;
        input.preparedMaintenanceSave = chunk;
        try { return save.getAsBoolean(); }
        finally { input.preparedMaintenanceSave = previous; }
    }

    /** Records a successful native maintenance write, never substitutes for one. */
    public static void onMaintenanceSave(ServerLevel level, LevelChunk chunk) {
        MinecraftThermalInput input = active(level);
        // Also observe a previously deferred chunk whose thermal ownership retired
        // before retry. The native write itself is the completion evidence.
        if (input == null) return;
        input.maintenanceSaves++;
        input.recordMaintenanceSave(chunk);
    }

    private void recordMaintenanceSave(LevelChunk chunk) {
        if (!jdk.jfr.FlightRecorder.isInitialized()) return;
        MaintenanceSave observation = new MaintenanceSave();
        if (!observation.isEnabled()) return;
        observation.chunkX = chunk.getPos().x; observation.chunkZ = chunk.getPos().z;
        observation.gameTick = level.getGameTime();
        observation.commit();
    }

    /** Unowned chunks cannot need a dimension-wide barrier; pending retirements still own a checkpoint. */
    private boolean needsChunkCheckpoint(LevelChunk chunk) {
        long key = chunk.getPos().toLong();
        if (pages.hasTrackedChunk(key) || physicalSources.hasSourcesInChunk(key)
                || workerEngine.hasRetainedInputInChunk(key) || coldRestores.containsKey(key)) return true;
        return batchTouchesChunk(pendingSubmission, key);
    }

    private static boolean batchTouchesChunk(ThermalInputEnvelope input, long chunkKey) {
        if (input == null) return false;
        if (batchTouchesThermalPages(input, chunkKey)) return true;
        for (var window : input.sourceWindows()) {
            long source = window.descriptor.sourceId();
            if (ChunkPos.asLong(BlockPos.getX(source) >> 4, BlockPos.getZ(source) >> 4) == chunkKey) return true;
        }
        return false;
    }

    private static boolean batchTouchesThermalPages(ThermalInputEnvelope input, long chunkKey) {
        if (input == null || input.pageInputs() == null) return false;
        for (var page : input.pageInputs()) {
            long section = page.section();
            if (ChunkPos.asLong(SectionPos.x(section), SectionPos.z(section)) == chunkKey) return true;
        }
        return false;
    }
    public static boolean usesNativeCheckpointScheduling(ServerLevel level) {
        MinecraftThermalInput input = active(level);
        return (input != null) && input.nativeCheckpoint && !input.stopping;
    }

    /** A native pending unload still owns its chunk; only thermal preparation may read it. */
    public LevelChunk chunkForThermalCapture(int chunkX, int chunkZ) {
        LevelChunk current = level.getChunkSource().getChunkNow(chunkX, chunkZ);
        if (current != null || !nativeCheckpoint) return current;
        for (NativeUnloadCheckpoint checkpoint : nativeCheckpoints.keySet()) {
            if (!checkpoint.released && checkpoint.chunk.getPos().x == chunkX
                    && checkpoint.chunk.getPos().z == chunkZ && checkpoint.stillPending.getAsBoolean())
                return checkpoint.chunk;
        }
        return null;
    }

    /** Invoked through vanilla's unload executor, never from a background completion thread. */
    public static CompletableFuture<NativeUnloadCheckpoint> prepareNativeUnload(ServerLevel level,
            ChunkAccess access, Executor unloadQueue, BooleanSupplier stillPending) {
        MinecraftThermalInput input = active(level);
        LevelChunk chunk = access instanceof LevelChunk full ? full
                : access instanceof ImposterProtoChunk imposter ? imposter.getWrapped() : null;
        if ((input == null || !input.nativeCheckpoint || input.stopping)
                || chunk == null || !stillPending.getAsBoolean() || !input.needsChunkCheckpoint(chunk))
            return CompletableFuture.completedFuture(new NativeUnloadCheckpoint(null, access, chunk, unloadQueue,
                    stillPending, null));
        input.requireMainThread();
        ThermalMemoryBudget.Reservation reservation = input.nativeCheckpoints.size() < MAX_NATIVE_CHECKPOINTS
                ? MEMORY.reserveCompletion(512L + 16L * chunk.getSections().length) : null;
        if (reservation == null) {
            input.nativeCheckpointFallbacks++;
            return CompletableFuture.completedFuture(new NativeUnloadCheckpoint(null, access, chunk, unloadQueue,
                    stillPending, null));
        }
        NativeUnloadCheckpoint checkpoint = new NativeUnloadCheckpoint(input, access, chunk, unloadQueue,
                stillPending, reservation);
        input.nativeCheckpoints.put(checkpoint, Boolean.TRUE);
        input.nativeCheckpointRequests++;
        input.nativeCheckpointPeakPending = Math.max(input.nativeCheckpointPeakPending, input.nativeCheckpoints.size());
        checkpoint.prepare();
        var diagnostics = com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer;
        if (diagnostics != null) diagnostics.nativeUnload(level, access);
        return checkpoint.ready;
    }

    /** One bounded unload request. It borrows no old operator or query layout while waiting. */
    public static final class NativeUnloadCheckpoint {
        private final MinecraftThermalInput owner;
        private final ChunkAccess access;
        private final LevelChunk chunk;
        private final Executor unloadQueue;
        private final BooleanSupplier stillPending;
        private final ThermalMemoryBudget.Reservation reservation;
        private final CompletableFuture<NativeUnloadCheckpoint> ready = new CompletableFuture<>();
        private final NativeCheckpointEvent event;
        private ChunkCapture capture;
        private boolean released, expired;

        private NativeUnloadCheckpoint(MinecraftThermalInput owner, ChunkAccess access, LevelChunk chunk,
                Executor unloadQueue, BooleanSupplier stillPending, ThermalMemoryBudget.Reservation reservation) {
            this.owner = owner; this.access = access; this.chunk = chunk; this.unloadQueue = unloadQueue;
            this.stillPending = stillPending; this.reservation = reservation;
            if (owner == null) event = null;
            else {
                event = new NativeCheckpointEvent(); event.chunkX = chunk.getPos().x; event.chunkZ = chunk.getPos().z;
                event.requestedTick = owner.level.getGameTime(); event.heldBytes = reservation.bytes(); event.begin();
            }
        }
        private void prepare() { owner.nativePreparationQueue.addLast(this); owner.pumpNativePreparation(); }
        private void begin() {
            owner.requireMainThread(); event.attempts++;
            try {
                capture = owner.new ChunkCapture(chunk, true,
                        com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.RegionalCheckpoint.Purpose.UNLOAD);
                if (!capture.ready.isDone()) owner.nativeCheckpointDeferred++;
                capture.ready.whenComplete((value, failure) -> unloadQueue.execute(this::prepared));
            } catch (RuntimeException | Error failure) {
                ready.completeExceptionally(failure); owner.nativePreparation = null; release();
            }
        }
        private void cancelled() {
            if (event.cancelled) return;
            event.cancelled = true; owner.nativeCheckpointCancelled++;
        }
        private void prepared() {
            owner.requireMainThread();
            if (released || ready.isDone()) return;
            if (expired || !stillPending.getAsBoolean() || owner.stopping || owner.closed) {
                expired = true; if (!stillPending.getAsBoolean()) cancelled();
                capture.finish(false); ready.complete(this); return;
            }
            try {
                capture.ready.join();
                event.readyTick = owner.level.getGameTime(); ready.complete(this);
            } catch (RuntimeException failure) {
                capture.finish(false); ready.completeExceptionally(failure); release();
            }
        }
        /** The vanilla holder decides whether the prepared ownership cut may be consumed. */
        public boolean run(Consumer<ChunkAccess> unload) {
            if (owner == null) { unload.accept(access); return true; }
            owner.requireMainThread();
            boolean pending = stillPending.getAsBoolean();
            LevelChunk previous = owner.preparedNativeUnload;
            try {
                if (!pending) {
                    cancelled(); if (capture != null) capture.finish(false);
                    unload.accept(access); return true;
                }
                if (owner.closed) { unload.accept(access); return true; }
                if (expired || capture == null || !capture.geometryCurrent()) {
                    expired = true; event.failure = "prepared_inputs_changed";
                    if (capture != null) capture.finish(false);
                    return false; // Vanilla retains the same pending holder and reschedules it.
                }
                try {
                    capture.write(true);
                    capture.finish(true); // The coordinator exports before related intake/geometry resumes.
                } catch (RuntimeException | Error failure) {
                    capture.finish(false); throw failure;
                }
                event.prepared = true; owner.nativeCheckpointPrepared++;
                event.liveSampleTick = owner.lastAcceptedLiveCheckpointTick;
                owner.preparedNativeUnload = chunk;
                unload.accept(access); return true;
            } finally { owner.preparedNativeUnload = previous; release(); }
        }
        private void finishForStop() {
            expired = true;
            if (capture != null && capture.finished == null) capture.finish(false);
            ready.complete(this);
        }
        private void release() {
            if (released) return;
            released = true; owner.nativeCheckpoints.remove(this); reservation.close();
            event.consumedTick = owner.level.getGameTime();
            owner.nativeCheckpointMaxAgeTicks = Math.max(owner.nativeCheckpointMaxAgeTicks, event.consumedTick - event.requestedTick);
            event.end(); event.commit();
        }
    }

    record NativeCheckpointStatistics(long requests, long deferred, long prepared, long cancelled, long fallbacks,
            int pending, int peakPending, long maximumAgeTicks) {}

    static NativeCheckpointStatistics observedNativeCheckpoints(ServerLevel level) {
        var input = ACTIVE.get(level);
        return input == null ? new NativeCheckpointStatistics(0, 0, 0, 0, 0, 0, 0, 0)
                : new NativeCheckpointStatistics(input.nativeCheckpointRequests, input.nativeCheckpointDeferred,
                        input.nativeCheckpointPrepared, input.nativeCheckpointCancelled, input.nativeCheckpointFallbacks,
                        input.nativeCheckpoints.size(), input.nativeCheckpointPeakPending, input.nativeCheckpointMaxAgeTicks);
    }

    static boolean observedNativeRequestForChunk(ServerLevel level, ChunkAccess chunk) {
        var input = ACTIVE.get(level);
        if (input == null) return false;
        for (var checkpoint : input.nativeCheckpoints.keySet())
            if (checkpoint.access == chunk && !checkpoint.released && checkpoint.stillPending.getAsBoolean()) return true;
        return false;
    }

    static DormantChunkThermalState checkpointForObservation(ServerLevel level, LevelChunk chunk) {
        MinecraftThermalInput input = ACTIVE.get(level);
        if (input == null || !input.pages.hasLiveChunk(chunk.getPos().toLong())) return dormantState(chunk);
        {
            input.captureRegionalCheckpoint(chunk, false);
            return dormantState(chunk);
        }

    }

    record CheckpointStatistics(long pendingSaveSnapshots, long pendingUnloadSnapshots,
            long saveWaitNanos, long unloadWaitNanos) {}

    static CheckpointStatistics observedCheckpointStatistics(ServerLevel level) {
        MinecraftThermalInput input = ACTIVE.get(level);
        return input == null ? new CheckpointStatistics(0, 0, 0, 0)
                : new CheckpointStatistics(input.snapshotSavePending, input.snapshotUnloadPending,
                        input.saveWaitNanos, input.unloadWaitNanos);
    }

    static boolean observedWorkerPending(ServerLevel level) {
        var input = ACTIVE.get(level);
        return input != null && input.mailbox.workPending();
    }

    static long[] observedCheckpointCalls(ServerLevel level) {
        var input = ACTIVE.get(level);
        return input == null ? new long[5] : new long[] {input.saveWaitCalls, input.unloadWaitCalls,
                input.unrelatedSaveSkips, input.unrelatedUnloadSkips, input.unrelatedSavesWhilePending};
    }

    static long[] observedMaintenanceSaves(ServerLevel level) {
        var input = ACTIVE.get(level);
        return input == null ? new long[2]
                : new long[] {input.maintenanceSaveDeferrals, input.maintenanceSaves};
    }

    static String observedMainWaits(ServerLevel level) {
        var input = ACTIVE.get(level);
        return input == null ? "active=false" : "saveWaitCalls=" + input.saveWaitCalls + ",saveWaitUs=" + input.saveWaitNanos / 1000
                + ",saveWaitMaxUs=" + input.maxSaveWaitNanos / 1000 + ",saveWaitMaxChunk=" + input.maxSaveWaitChunk
                + ",unloadWaitCalls=" + input.unloadWaitCalls + ",unloadWaitUs=" + input.unloadWaitNanos / 1000
                + ",unloadWaitMaxUs=" + input.maxUnloadWaitNanos / 1000 + ",unloadWaitMaxChunk=" + input.maxUnloadWaitChunk
                + ",savesWhilePending=" + input.savesWhilePending + ",unloadsWhilePending=" + input.unloadsWhilePending
                + ",unrelatedSaveSkips=" + input.unrelatedSaveSkips + ",unrelatedUnloadSkips=" + input.unrelatedUnloadSkips
                + ",unrelatedSavesWhilePending=" + input.unrelatedSavesWhilePending
                + ",maintenanceSaveDeferrals=" + input.maintenanceSaveDeferrals
                + ",maintenanceSaves=" + input.maintenanceSaves
                + ",snapshotCheckpoints=" + input.snapshotCheckpoints + ",snapshotFallbacks=" + input.snapshotFallbacks
                + ",snapshotSavePending=" + input.snapshotSavePending + ",snapshotUnloadPending=" + input.snapshotUnloadPending
                + ",snapshotMaximumAgeTicks=" + input.snapshotMaximumAgeTicks
                + ",snapshotStoredMaximumAgeTicks=" + input.snapshotStoredMaximumAgeTicks
                + ",snapshotMaximumUs=" + input.snapshotMaximumNanos / 1000
                + ",snapshotMaximumOverlapBytes=" + input.snapshotMaximumOverlapBytes
                + ",nativeCheckpointRequests=" + input.nativeCheckpointRequests
                + ",nativeCheckpointDeferred=" + input.nativeCheckpointDeferred
                + ",nativeCheckpointPrepared=" + input.nativeCheckpointPrepared
                + ",nativeCheckpointCancelled=" + input.nativeCheckpointCancelled
                + ",nativeCheckpointFallbacks=" + input.nativeCheckpointFallbacks
                + ",nativeCheckpointPending=" + input.nativeCheckpoints.size()
                + ",nativeCheckpointPeakPending=" + input.nativeCheckpointPeakPending
                + ",nativeCheckpointMaxAgeTicks=" + input.nativeCheckpointMaxAgeTicks;
    }

    static void checkpointAllForStop() {
        MinecraftThermalInput[] inputs;
        synchronized (ACTIVE) {
            inputs = ACTIVE.values().toArray(MinecraftThermalInput[]::new);
        }
        for (MinecraftThermalInput input : inputs) {
            input.stopping = true;
            input.settleNativePreparationForSave();
            input.drainCompletion();
            input.pages.checkpointAll(false);
            for (NativeUnloadCheckpoint checkpoint : input.nativeCheckpoints.keySet().toArray(NativeUnloadCheckpoint[]::new))
                checkpoint.finishForStop();
        }
    }

    private static double dormantTemperature(
            ServerLevel level,
            int blockX,
            int blockY,
            int blockZ,
            long gameTick,
            BlockPos.MutableBlockPos naturalPosition) {
        LevelChunk chunk =
                level.getChunkSource()
                        .getChunkNow(
                                SectionPos.blockToSectionCoord(blockX),
                                SectionPos.blockToSectionCoord(blockZ));
        return dormantTemperature(level, chunk, blockX, blockY, blockZ, gameTick, naturalPosition);
    }

    private static double dormantTemperatureAt(ServerLevel level, double x, double y, double z,
            long gameTick, BlockPos.MutableBlockPos naturalPosition) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(floor(x) >> 4, floor(z) >> 4);
        DormantChunkThermalState state = chunk == null ? null : dormantState(chunk);
        return state == null ? Double.NaN : state.sampleAt(x, y, z, gameTick, dormantHalfLifeSeconds(), level, naturalPosition);
    }

    private static double dormantTemperature(
            ServerLevel level,
            LevelChunk chunk,
            int blockX,
            int blockY,
            int blockZ,
            long gameTick,
            BlockPos.MutableBlockPos naturalPosition) {
        DormantChunkThermalState state = chunk == null ? null : dormantState(chunk);
        if (state == null) {
            return Double.NaN;
        }
        return state.sampleAt(blockX + 0.5, blockY + 0.5, blockZ + 0.5,
                gameTick, dormantHalfLifeSeconds(), level, naturalPosition);
    }

    static DormantChunkThermalState dormantState(LevelChunk chunk) {
        return ((MinecraftThermalChunkAttachment) (Object) chunk)
                .frostedheart$getDormantThermalState();
    }

    static void setDormantState(LevelChunk chunk, DormantChunkThermalState state) {
        ((MinecraftThermalChunkAttachment) (Object) chunk)
                .frostedheart$setDormantThermalState(state);
    }

    static double dormantHalfLifeSeconds() {
        return MinecraftThermalProfiles.dormantTemperatureHalfLifeSeconds();
    }

    private record StartupRetry(long capacityRevision, long preferredPosition) {}
    private static final IdentityHashMap<ServerLevel, StartupRetry> startupRetries = new IdentityHashMap<>();
    private static MinecraftThermalInput start(ServerLevel level, BlockPos center) {
        synchronized (ACTIVE) {
            MinecraftThermalInput existing = ACTIVE.get(level);
            if (existing != null) return existing;
            long capacityRevision = MEMORY.capacityRevision();
            StartupRetry retry = startupRetries.get(level);
            if (retry != null && retry.capacityRevision() == capacityRevision) {
                if (retry.preferredPosition() != center.asLong())
                    startupRetries.put(level, new StartupRetry(capacityRevision, center.asLong()));
                return null;
            }
            MinecraftThermalInput created = null;
            try {
                created = new MinecraftThermalInput(level);
                created.attachLoadedWorld(center);
                ACTIVE.put(level, created);
                startupRetries.remove(level);
                return created;
            } catch (RuntimeException failure) {
                if (created != null) created.close();
                if (failure instanceof ThermalMemoryBudget.AllocationRefused)
                    startupRetries.put(level, new StartupRetry(MEMORY.capacityRevision(), center.asLong()));
                FHMain.LOGGER.error(
                        "Could not start thermal runtime for {}",
                        level.dimension().location(),
                        failure);
                return null;
            }
        }
    }

    private static MinecraftThermalInput active(ServerLevel level) {
        synchronized (ACTIVE) {
            return ACTIVE.get(level);
        }
    }

    private void requireMainThread() {
        if (Thread.currentThread() != mainThread) {
            throw new IllegalStateException("Minecraft thermal input requires the level thread");
        }
    }

    private static long alignedTick(long tick) {
        return Math.floorDiv(tick, ThermalInputBatch.CUT_INTERVAL_TICKS)
                * ThermalInputBatch.CUT_INTERVAL_TICKS;
    }

    private static long receiverKey(net.minecraft.world.entity.Entity entity) {
        return entity.getUUID().getMostSignificantBits()
                ^ Long.rotateLeft(entity.getUUID().getLeastSignificantBits(), 17);
    }

    private static long nextGeneration() {
        return NEXT_GENERATION.getAndUpdate(Math::incrementExact);
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static int floorQuarter(double value) {
        return (int) Math.floor(value * 4.0D);
    }

    static final class ItemEnvironmentSampleCache {
        private final int[] quarterX;
        private final int[] quarterY;
        private final int[] quarterZ;
        private final ThermalEnvironmentSample[] samples;
        private long generationTick = Long.MIN_VALUE;
        private int size;

        ItemEnvironmentSampleCache(int capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("capacity must be positive");
            }
            quarterX = new int[capacity];
            quarterY = new int[capacity];
            quarterZ = new int[capacity];
            samples = new ThermalEnvironmentSample[capacity];
            for (int index = 0; index < capacity; index++) {
                samples[index] = new ThermalEnvironmentSample();
            }
        }

        int find(long tick, int x, int y, int z) {
            beginGeneration(tick);
            for (int index = 0; index < size; index++) {
                if (quarterX[index] == x && quarterY[index] == y && quarterZ[index] == z)
                    return index;
            }
            return -1;
        }

        boolean canAdmit() {
            return size < samples.length;
        }

        boolean store(int x, int y, int z, ThermalEnvironmentSample sample) {
            if (!canAdmit()) return false;
            quarterX[size] = x;
            quarterY[size] = y;
            quarterZ[size] = z;
            samples[size].copyFrom(sample);
            size++;
            return true;
        }

        void copyTo(int index, ThermalEnvironmentSample out) {
            if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
            out.copyFrom(samples[index]);
        }

        void clear() {
            generationTick = Long.MIN_VALUE;
            size = 0;
        }

        private void beginGeneration(long tick) {
            if (tick < 0L) throw new IllegalArgumentException("tick must be non-negative");
            if (generationTick != tick) {
                generationTick = tick;
                size = 0;
            }
        }
    }
}
