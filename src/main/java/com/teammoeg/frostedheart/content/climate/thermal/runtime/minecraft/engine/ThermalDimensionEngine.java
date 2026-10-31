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
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalCompletion;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputEnvelope;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.source.AirLoadTable;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSolver;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ContinuousAirSolver;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceLedger;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceIngress;
import com.teammoeg.frostedheart.content.climate.thermal.source.SourceInputWindow;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.TopologyIngress;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.RegionalCheckpoint;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.PendingHeatCheckpoint;
import com.teammoeg.frostedheart.content.climate.thermal.solver.RegionalAirRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.solver.RegionalQualityControl;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.TopologyInbox;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import com.teammoeg.frostedheart.content.climate.thermal.topology.BrickTopologyCompiler;
import com.teammoeg.frostedheart.content.climate.thermal.topology.FarFieldSettings;
import com.teammoeg.frostedheart.content.climate.thermal.topology.FrozenAirGeometry;
import com.teammoeg.frostedheart.content.climate.thermal.topology.RegionalContinuationBoundary;
import com.teammoeg.frostedheart.content.climate.thermal.topology.PreparedTopologyChange;
import com.teammoeg.frostedheart.content.climate.thermal.topology.ThermalTopologyParameters;
import com.teammoeg.frostedheart.content.climate.thermal.topology.TopologyUpdatePlanner;
import com.teammoeg.frostedheart.content.climate.thermal.topology.WorkerPageStore;

import java.util.Objects;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/** Shared-grid coordinator. Regional jobs own physical time; no dimension-wide cut exists. */
public final class ThermalDimensionEngine implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean PROFILE = Boolean.parseBoolean(System.getenv("FH_CONTINUOUS_AIR_PROFILE"));
    private final long dimensionGeneration;
    private final ThermalTopologyParameters parameters;
    private final ThermalDimensionLimits limits;
    private final ThermalCellArena arena;
    private final QueryPublication queries;
    private final WorkerPageStore pages;
    private final PhaseTransitionRuntime phases;
    private final ThermalSolver solver;
    private final ThermalSourceLedger sources;
    private final WorkerPhysicalSourceBindings sourceBindings;
    private final TopologyUpdatePlanner topologyPlan;
    private final RegionalAirRuntime regional;
    private final AirLoadTable airLoads;
    private final SpatialSourceIngress sourceIngress;
    private final TopologyIngress topologyIngress;
    private final ThermalMemoryBudget airBudget;
    private ThermalMemoryBudget.Reservation airReservation;
    private AirBackingMeter backingMeter;

    private long lastBatchSequence;
    private boolean closed;
    private boolean closing, closeContinuationReady;
    private volatile boolean failureRecovery;
    private volatile int lastWorkerAirCoefficients;
    private volatile String lastWorkerContactMode;
    private volatile String lastWorkLimitedReason;
    private long transportThrough;

    public ThermalDimensionEngine(long dimensionGeneration, long initialTick, ThermalCellArena arena,
            ThermalSignatureTable signatures, MaterialBoundaryRegistry materials, ThermalTopologyParameters parameters,
            FarFieldSettings farField, MinecraftPhysicalSourceProfile campfireProfile,
            ThermalDimensionLimits limits, QueryPublication queries, ThermalMemoryBudget airBudget,
            double correctionToleranceC) {
        if (dimensionGeneration < 0L || initialTick < 0L) {
            throw new IllegalArgumentException("engine identity is invalid");
        }
        this.dimensionGeneration = dimensionGeneration;
        this.parameters = Objects.requireNonNull(parameters, "parameters");
        this.limits = Objects.requireNonNull(limits, "limits");
        this.arena = Objects.requireNonNull(arena, "arena");
        this.queries = Objects.requireNonNull(queries, "queries");
        ThermalSignatureTable catalog = Objects.requireNonNull(
                signatures, "signatures");
        pages = new WorkerPageStore(limits.maximumPages());
        solver = new ThermalSolver(
                arena,
                64,
                16);
        sources = new ThermalSourceLedger(
                initialTick, 64, 3, limits.maximumSources());
        offeredThrough = initialTick;
        airLoads = new AirLoadTable();
        sourceBindings = new WorkerPhysicalSourceBindings(
                pages, catalog, Objects.requireNonNull(campfireProfile, "campfireProfile"), airLoads);
        regional = new RegionalAirRuntime(arena, pages, solver, parameters, materials, airLoads, catalog,
                correctionToleranceC, queries);
        phases = new PhaseTransitionRuntime(arena, regional, parameters.phaseRequestCapacity());
        pages.phaseMaintenance(phases::markMaterialDirty);
        phases.onMaterialControlChanged(regional::materialControlChanged);
        this.airBudget = Objects.requireNonNull(airBudget, "airBudget");
        topologyIngress = new TopologyIngress(airBudget);
        sourceIngress = new SpatialSourceIngress(sources, sourceBindings, regional, arena, airLoads,
                this::sourceBindingLimit, this::sourceFrozen, this::sourceControlChanged);
        BrickTopologyCompiler compiler = new BrickTopologyCompiler(
                arena, catalog,
                Objects.requireNonNull(materials, "materials"),
                parameters,
                Objects.requireNonNull(farField, "farField"),
                limits.maximumArenaSlots(), sourceBindings::collectMixingSources);
        topologyPlan = new TopologyUpdatePlanner(
                pages,
                arena,
                regional,
                solver,
                phases,
                catalog,
                materials,
                compiler,
                parameters,
                limits);
        try {
            backingMeter = new AirBackingMeter(airBudget);
            reserveAirBacking();
            arena.storageOwner(airReservation); solver.storageOwner(airReservation); sources.storageOwner(airReservation);
            phases.storageOwner(airReservation);
            transportThrough = initialTick;
            minimumRegionTick = initialTick;
            regional.attachIntake(airBudget, airReservation, sources, initialTick);
            regional.onBackingReleased(() -> backingNeedsReclaim = true);
            regional.oldestUninstalledTick(() -> oldestStructureTick);
            regional.geometryBoundary(this::geometryBoundary);
            regional.sourceIntakeBoundary(sourceIngress::pendingThrough);
            regional.incomingSourceBoundary(sourceIngress::pendingFor);
            sourceBindings.bindingGeometryCurrent(id -> sourceBindings.geometryCurrent(id,
                    section -> topologyInputs.firstTick(section) == Long.MAX_VALUE));
            // Prepaid coordinator metadata and a small startup checkpoint slot.
            geometryRetention = airBudget.reserveCompletion(4096 + 258L * 256);
        } catch (RuntimeException | Error failure) {
            try {
                regional.clearRegionalInputs();
                if (geometryRetention != null) geometryRetention.close();
                sources.close(); pages.close();
                if (airReservation != null) airReservation.close();
            } finally {
                if (backingMeter != null) backingMeter.close();
            }
            throw failure;
        }

    }


    private volatile long lastIntakeNanos, lastRoundNanos, maximumRoundNanos;
    private volatile String currentGeometryPhase = "none";
    private volatile int currentTopologyPhase = -1;
    private volatile long currentRouteVisits;
    private volatile double publishedPendingJ;
    @jdk.jfr.Name("frostedheart.ThermalCoordinatorRound")
    @jdk.jfr.StackTrace(false)
    private static final class RoundObservation extends jdk.jfr.Event {
        public long sequence, intakeTick, coordinatorRegionNanos, geometryNanos, maintenanceNanos, progressNanos, inputNanos, qualityNanos;
        public int geometryPhase;
    }
    public String regionalProgressDescription() {
        return "runtime=independent_regions,intakeTick=" + transportThrough + ",minimumSampleTick=" + minimumRegionTick
                + ",pendingChunks=" + publishedBusyChunks + ",pendingSourceJ=" + publishedPendingJ
                + ",lastIntakeUs=" + lastIntakeNanos / 1000 + ",lastCoordinatorRoundUs=" + lastRoundNanos / 1000
                + ",maxCoordinatorRoundUs=" + maximumRoundNanos / 1000 + ",airCoefficients=" + lastWorkerAirCoefficients
                + ",geometryPhase=" + currentGeometryPhase + ",topologyPhase=" + currentTopologyPhase + ",routeVisits=" + currentRouteVisits
                + ",rootBytes=" + airBudget.rootUsedBytes()
                + ",contactMode=" + lastWorkerContactMode + ",lastLimit=" + lastWorkLimitedReason;
    }

    public long independentRegionCommits() { return regional.independentCommits(); }
    public boolean regionCommittedIndependently(long id) { return regional.regionCommittedIndependently(id); }

    private final TopologyInbox topologyInputs = new TopologyInbox();
    private ThermalMemoryBudget.Reservation geometryRetention;
    private final java.util.ArrayDeque<GeometryWork> geometryWorks = new java.util.ArrayDeque<>();
    private boolean backingNeedsReclaim;
    private final it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap<com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle.GeometryResyncToken> resyncEffects = new it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap<>();
    private final it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap<ThermalCompletion.BrickResidency> residencyEffects = new it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap<>();
    private boolean limitedEffect;
    private long effectSequence;
    private volatile long minimumRegionTick;
    private long oldestStructureTick = Long.MAX_VALUE, structureCommits;
    private volatile int publishedBusyChunks;
    private long selectionIntake = -1, selectionCommit = -1, selectionTopology = -1;
    private long selectionRouting = -1, selectionSources = -1;
    private int selectionRemaining;
    private record BoundaryCandidate(TopologyInbox.Input input, long topology, RegionalContinuationBoundary boundary,
            ThermalMemoryBudget.Reservation reservation) { }
    private final it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<BoundaryCandidate> pendingBoundaries = new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>();
    private Runnable capacityContinuation;

    private final Object intakeLock = new Object();
    private final java.util.concurrent.ConcurrentHashMap<Long, Long> pendingChunkInput = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile long offeredSequence;
    private volatile long offeredThrough;
    private double offeredWind = Double.NaN;
    private final java.util.ArrayDeque<SnapshotRequest> snapshotRequests = new java.util.ArrayDeque<>();
    private SnapshotRequest activeSnapshot;
    private volatile boolean checkpointControlReady;
    private final java.util.ArrayDeque<RestoreRequest> restoreRequests = new java.util.ArrayDeque<>();
    private final java.util.concurrent.ConcurrentHashMap<Long, Integer> restoringChunks = new java.util.concurrent.ConcurrentHashMap<>();
    private boolean preferRestore;
    private static final class RestoreRequest {
        final long chunk, tick, minimumPage;
        final double halfLife;
        final PendingHeatCheckpoint pending;
        final ThermalMemoryBudget.Reservation reservation;
        int cursor;
        RestoreRequest(long chunk, PendingHeatCheckpoint pending, long tick, double halfLife, long minimumPage,
                ThermalMemoryBudget.Reservation reservation) {
            this.chunk = chunk; this.pending = pending; this.tick = tick; this.halfLife = halfLife;
            this.minimumPage = minimumPage; this.reservation = reservation;
        }
    }
    public boolean offerRestoredHeat(long chunk, PendingHeatCheckpoint pending, long tick, double halfLife, long minimumPage) {
        if (pending.empty()) return true;
        var reservation = airBudget.reserveCompletion(32768L + 3 * pending.retainedBytes() + 2048L * pending.size());
        synchronized (intakeLock) {
            if (closed) { reservation.close(); return false; }
            restoreRequests.addLast(new RestoreRequest(chunk, pending, tick, halfLife, minimumPage, reservation));
            restoringChunks.merge(chunk, 1, Integer::sum);
        }
        return true;
    }
    private boolean restoreReady() {
        synchronized (intakeLock) {
            for (var restore : restoreRequests) if (activeSnapshot == null || !activeSnapshot.inputsFrozen || activeSnapshot.chunk != restore.chunk) return true;
            return false;
        }
    }
    private boolean receiveRestore() {
        RestoreRequest request = null;
        synchronized (intakeLock) {
            int remaining = restoreRequests.size();
            while (remaining-- > 0) {
                var candidate = restoreRequests.removeFirst(); restoreRequests.addLast(candidate);
                if (activeSnapshot == null || !activeSnapshot.inputsFrozen || activeSnapshot.chunk != candidate.chunk) { request = candidate; break; }
            }
        }
        if (request == null) return false;
        int end = Math.min(request.pending.size(), request.cursor + 128);
        var restored = request.pending.restore(request.cursor, end, request.tick, request.halfLife, request.minimumPage);
        regional.receiveRestoredHeat(restored, request.reservation); request.cursor = end;
        if (end == request.pending.size()) synchronized (intakeLock) {
            restoreRequests.remove(request); request.reservation.close();
            restoringChunks.computeIfPresent(request.chunk, (ignored, count) -> count == 1 ? null : count - 1);
        }
        return true;
    }
    private enum SnapshotStage { REQUESTED, QUIESCING, CAPTURING, SEALED }
    private static final class SnapshotRequest {
        final long chunk;
        final RegionalCheckpoint.Purpose purpose;
        final SpatialSourceTimeline.Capture producer;
        final PendingHeatCheckpoint cold;
        final double halfLife;
        final java.util.concurrent.CompletableFuture<RegionalCheckpoint> ready = new java.util.concurrent.CompletableFuture<>();
        java.util.concurrent.CompletableFuture<Void> finished;
        RegionalCheckpoint field;
        SpatialSourceTimeline.Pending sourceTail;
        ThermalMemoryBudget.Reservation reservation;
        boolean inputsFrozen;
        SnapshotStage stage = SnapshotStage.REQUESTED;
        boolean unloading;
        SnapshotRequest(long chunk, SpatialSourceTimeline.Capture producer, PendingHeatCheckpoint cold, double halfLife,
                RegionalCheckpoint.Purpose purpose) {
            this.chunk = chunk; this.producer = producer; this.cold = cold; this.halfLife = halfLife;
            this.purpose = purpose;
        }
    }
    public java.util.concurrent.CompletableFuture<RegionalCheckpoint> requestCheckpoint(long chunk, SpatialSourceTimeline.Capture producer,
            PendingHeatCheckpoint cold, double halfLife, RegionalCheckpoint.Purpose purpose) {
        Objects.requireNonNull(producer, "producer");
        synchronized (intakeLock) {
            if (closed) return java.util.concurrent.CompletableFuture.failedFuture(new IllegalStateException("Thermal owner has closed"));
            var request = new SnapshotRequest(chunk, producer, cold, halfLife, purpose); snapshotRequests.addLast(request);
            checkpointControlReady = true;
            return request.ready;
        }
    }
    public java.util.concurrent.CompletableFuture<Void> cancelCheckpoint(
            java.util.concurrent.CompletableFuture<RegionalCheckpoint> ready) {
        synchronized (intakeLock) {
            SnapshotRequest request = activeSnapshot != null && activeSnapshot.ready == ready ? activeSnapshot : null;
            if (request == null) for (var queued : snapshotRequests) if (queued.ready == ready) { request = queued; break; }
            if (request == null) return java.util.concurrent.CompletableFuture.completedFuture(null);
            if (request.finished != null) return request.finished;
            request.finished = new java.util.concurrent.CompletableFuture<>();
            checkpointControlReady = true;
            return request.finished;
        }
    }
    private boolean snapshotCommandsReady() {
        synchronized (intakeLock) {
            if (activeSnapshot == null) return !snapshotRequests.isEmpty();
            var request = activeSnapshot;
            if (request.finished != null || request.stage == SnapshotStage.REQUESTED) return true;
            if (request.stage == SnapshotStage.QUIESCING && regional.snapshotHasJobs(request)) return regional.hasReadyWork();
            if (request.stage == SnapshotStage.SEALED) return false;
            return true;
        }
    }
    private boolean snapshotTouches(RegionalContinuationBoundary boundary) {
        if (activeSnapshot == null || activeSnapshot.stage == SnapshotStage.REQUESTED) return false;
        if (regional.snapshotTouches(activeSnapshot, boundary)) return true;
        for (long section : boundary.sections()) if (chunkForSection(section) == activeSnapshot.chunk) return true;
        return false;
    }
    /** One control stage, never a physical catch-up cut. Helpers return through their ordinary cleanup. */
    private boolean processSnapshotCommand(long deadline) {
        SnapshotRequest request;
        synchronized (intakeLock) {
            if (activeSnapshot == null) activeSnapshot = snapshotRequests.pollFirst();
            request = activeSnapshot;
        }
        if (request == null) return false;
        try { return advanceSnapshot(request, deadline); }
        finally { checkpointControlReady = snapshotCommandsReady(); }
    }
    private boolean advanceSnapshot(SnapshotRequest request, long deadline) {
        if (request.stage == SnapshotStage.REQUESTED && request.finished == null) {
            regional.beginSnapshot(request, request.chunk, request.purpose); request.stage = SnapshotStage.QUIESCING;
        }
        if (request.finished != null) {
            if (request.field != null) request.field.releaseFrames();
            if (!request.ready.isDone()) request.ready.completeExceptionally(new java.util.concurrent.CancellationException("Native checkpoint was not consumed"));
            if (request.unloading) {
                sources.exportPendingEnergy(regional.exportChunkPending(request.chunk), request.sourceTail.export());
                sources.settleUnrecordedLoss(request.sourceTail.declaredLossJ(), request.sourceTail.degradedLossJ());
                synchronized (intakeLock) {
                    var queued = restoreRequests.iterator();
                    while (queued.hasNext()) {
                        var restore = queued.next();
                        if (restore.chunk == request.chunk) { queued.remove(); restore.reservation.close(); }
                    }
                    restoringChunks.remove(request.chunk);
                }
            }
            if (request.reservation != null) request.reservation.close();
            regional.resumeSnapshot(request);
            synchronized (intakeLock) { activeSnapshot = null; checkpointControlReady = !snapshotRequests.isEmpty(); }
            request.finished.complete(null);
            inputDependencyRevision++;
            sourceIngress.dependenciesChanged();
            return true;
        }
        if (request.stage == SnapshotStage.SEALED) return false;
        if (request.stage == SnapshotStage.QUIESCING) {
            if (!regional.snapshotReady(request)) {
                if (regional.hasReadyWork()) regional.runWorkRound(deadline);
                return true;
            }
            request.stage = SnapshotStage.CAPTURING;
        }
        if (!request.inputsFrozen) {
            request.inputsFrozen = true;
            regional.sealSnapshot(request);
        }
        if (request.reservation == null) {
            long records = request.cold.size() + request.producer.maximumPendingRecords(request.chunk, sourceBindings::checkpointTarget);
            synchronized (intakeLock) { for (var restore : restoreRequests) if (restore.chunk == request.chunk) records += restore.pending.size() - restore.cursor; }
            // One active checkpoint owns a chunk-sized workspace. It cannot wait
            // for the frozen region to advance or borrow another task's credit.
            request.reservation = airBudget.reserveCompletion(regional.checkpointBytes(request.chunk) + 1024L * records);
        }
        var tails = regional.collectCheckpointInputs(request.chunk);
        request.sourceTail = request.producer.pending(request.chunk, sourceBindings::checkpointTarget, tails::appendAggregate);
        request.cold.collectRemainingForSave(0, request.producer.tick, request.halfLife, tails::appendAggregate);
        synchronized (intakeLock) {
            for (var restore : restoreRequests) if (restore.chunk == request.chunk)
                restore.pending.collectRemainingForSave(restore.cursor, request.producer.tick, restore.halfLife, tails::appendAggregate);
        }
        request.field = regional.checkpoint(request.chunk, tails, request.reservation, request.cold);
        if (request.field == null) return false;
        request.field.production(request.producer, request.sourceTail);
        request.field.onFinish((unloading, finished) -> {
            synchronized (intakeLock) {
                if (request.finished == null) { request.unloading = unloading; request.finished = finished; }
                else request.finished.whenComplete((ignored, failure) -> {
                    if (failure == null) finished.complete(null); else finished.completeExceptionally(failure);
                });
                checkpointControlReady = true;
            }
        });
        request.stage = SnapshotStage.SEALED;
        var observer = com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer;
        if (observer != null) observer.checkpointSealed(request.chunk, regional.inputStore().unboundWindowsInChunk(request.chunk));
        request.ready.complete(request.field);
        return true;
    }

    /** Transfer Page controls and source windows to separate owners before returning the receipt. */
    public void offerInput(ThermalInputEnvelope envelope) {
        requireOpen();
        var diagnostics = com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer;
        if (envelope.dimensionGeneration() != dimensionGeneration) throw new IllegalArgumentException("Wrong input generation");
        var fragments = envelope.pageInputs();
        if (fragments == null) throw new IllegalArgumentException("Regional input must be sealed into Page fragments");
        synchronized (intakeLock) {
            if (envelope.sequence() <= offeredSequence) return;
            if (envelope.sequence() != offeredSequence + 1) throw new IllegalArgumentException("Transport sequence skipped");
            topologyIngress.prepare(fragments);
            for (var fragment : fragments) {
                topologyIngress.offer(fragment);
                pendingChunkInput.merge(chunkForSection(fragment.section()), fragment.boundaryTick(), Math::min);
            }
            var windows = envelope.sourceWindows();
            for (int index = 0; index < windows.length; index++) sourceIngress.enqueue(windows[index], envelope.sequence(), index);
            offeredSequence = envelope.sequence();
            offeredThrough = Math.max(offeredThrough, envelope.targetTick());
            if (envelope.hasFarFieldConductanceScale()) offeredWind = envelope.farFieldConductanceScale();
        }
        if (diagnostics != null) diagnostics.inputAccepted(envelope, !geometryWorks.isEmpty() || regional.hasPendingInputs());
    }

    private long inputDependencyRevision;
    private long lastWaitObservationTick = -1;
    private long inputRevision() { return inputDependencyRevision + sourceIngress.revision(); }

    private boolean sourceFrozen(SpatialSourceTimeline.Descriptor descriptor) {
        var snapshot = activeSnapshot;
        if (snapshot == null || !snapshot.inputsFrozen) return false;
        if (regional.sourceTouchesSnapshot(snapshot, descriptor.sourceId())) return true;
        for (int port = 0; port < descriptor.ports().length; port++) {
            var origin = sourceBindings.originalPort(descriptor, port);
            if (origin != null && chunkForPosition(origin.position()) == snapshot.chunk) return true;
            var destination = sourceBindings.checkpointTarget(descriptor, port);
            if (destination.target() != null && chunkForPosition(destination.target().position()) == snapshot.chunk) return true;
        }
        return false;
    }
    private static long chunkForPosition(long position) {
        return net.minecraft.world.level.ChunkPos.asLong(net.minecraft.core.BlockPos.getX(position) >> 4,
                net.minecraft.core.BlockPos.getZ(position) >> 4);
    }

    /** Effective source membership creates a structure command; it never creates a Page input waiter. */
    private void sourceControlChanged(SourceInputWindow window, LongSet mixing) {
        boolean queued = topologyInputs.receiveMixing(dimensionGeneration, window.sequence, window.through(), mixing, key -> {
            var page = pages.pageAt(key); return page == null ? null : page.handle;
        }, solver.windScale());
        if (queued) {
            oldestStructureTick = Math.min(oldestStructureTick, window.through());
        }
        // Binding sees the queued Page boundary immediately. The intake lane
        // refreshes regional holds once after this bounded group of controls,
        // before sealing or dispatching any numerical work.
        clearPendingBoundaries(); selectionIntake = -1;
        inputDependencyRevision++;
    }

    private long geometryBoundary(long section) {
        return Math.min(pendingInputBoundary(section), Math.min(topologyInputs.firstTick(section), activeGeometryTick(section)));
    }
    private long sourceBindingLimit(com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline.Descriptor descriptor) {
        return Math.min(sourceBindings.bindingLimit(descriptor,
                this::geometryBoundary, regional::sourceBindingLimitAt),
                regional.sourceBindingLimitFor(descriptor.sourceId()));
    }
    private long boundarySequence = -1;
    private long boundaryControlRevision = -1;
    private record ControlOrder(long tick, long sequence) { }

    /** Temporary union of inputs that install the same physical boundary. */
    private static final class BoundaryGroup {
        final LongOpenHashSet seeds = new LongOpenHashSet(), mixing = new LongOpenHashSet(), sections = new LongOpenHashSet();
        boolean structural, hasInput;
        void add(ThermalInputBatch input, LongSet changedMixing) {
            hasInput = true;
            TopologyUpdatePlanner.collectBoundarySeeds(input, seeds);
            structural |= RegionalContinuationBoundary.hasStructuralInput(input);
            mixing.addAll(changedMixing);
        }
    }

    private void refreshBoundaryHolds() {
        synchronized (intakeLock) { refreshBoundaryHoldsLocked(); }
    }

    private void refreshBoundaryHoldsLocked() {
        regional.resetBoundaryHolds();
        oldestStructureTick = Long.MAX_VALUE;
        var groups = new java.util.LinkedHashMap<ControlOrder, BoundaryGroup>();
        for (long section : topologyInputs.pendingSections()) {
            var next = topologyInputs.peek(section);
            groups.computeIfAbsent(new ControlOrder(next.batch().targetTick(), next.batch().sequence()), ignored -> new BoundaryGroup())
                    .add(next.batch(), next.mixing());
            oldestStructureTick = Math.min(oldestStructureTick, next.batch().targetTick());
        }
        for (var work : geometryWorks) if (work.phase != GeometryWork.Stage.DONE && (!work.recovering() || work.recoveryHandoff)) {
            regional.holdBoundary(work.boundary, work.input.targetTick(), work.input.sequence());
            oldestStructureTick = Math.min(oldestStructureTick, work.input.targetTick());
        }
        topologyIngress.forEachPending(fragment -> {
                var group = groups.computeIfAbsent(new ControlOrder(fragment.boundaryTick(), fragment.payload().sequence()),
                        ignored -> new BoundaryGroup());
                group.add(fragment.payload(), LongSets.EMPTY_SET);
                group.sections.add(fragment.section());
                oldestStructureTick = Math.min(oldestStructureTick, fragment.boundaryTick());
        });
        // A source control has the same physical closure before and after binding.
        // Group equal boundaries so a thousand registrations do not each walk all regions.
        sourceIngress.forEachPendingControl((source, tick, sequence) -> {
            var group = groups.computeIfAbsent(new ControlOrder(tick, sequence), ignored -> new BoundaryGroup());
            sourceBindings.collectMixingFootprint(source, group.mixing);
        });
        for (var entry : groups.entrySet()) {
            var order = entry.getKey();
            var group = entry.getValue();
            if (!group.hasInput && group.mixing.isEmpty()) continue;
            regional.holdBoundary(inputBoundary(group.seeds, group.structural, group.mixing).including(group.sections),
                    order.tick, order.sequence);
        }
        boundarySequence = offeredSequence;
        boundaryControlRevision = sourceIngress.controlRevision();
        regional.boundaryHoldsChanged();
        sourceIngress.dependenciesChanged();
    }

    private static long chunkForSection(long section) {
        return net.minecraft.world.level.ChunkPos.asLong(net.minecraft.core.SectionPos.x(section), net.minecraft.core.SectionPos.z(section));
    }

    private void refreshChunkInputHold(long chunk) {
        long pending = topologyInputs.firstTickInChunk(chunk);
        pending = Math.min(pending, topologyIngress.firstTickInChunk(chunk));
        for (var work : geometryWorks) if (!work.recovering() || work.recoveryHandoff) for (long section : work.boundary.sections())
            if (chunkForSection(section) == chunk) { pending = Math.min(pending, work.input.targetTick()); break; }
        if (pending == Long.MAX_VALUE) pendingChunkInput.remove(chunk); else pendingChunkInput.put(chunk, pending);
    }

    private void retireInputSlot(long section) {
        if (pages.pageAt(section) == null && topologyInputs.peek(section) == null) topologyIngress.retire(section);
    }

    private void receiveRegionalInput(long deadline) {
        long started = System.nanoTime();
        boolean restored = preferRestore && receiveRestore(); preferRestore = !preferRestore;
        var fragment = topologyIngress.poll();
        boolean received = fragment != null;
        if (received) {
            var chunks = new LongOpenHashSet();
            var diagnostics = com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer;
            // Use the first half of the existing intake slice for world controls.
            // One boundary refresh then leaves the source lane its own opportunity.
            long captureDeadline = Math.min(deadline, started + 1_000_000L);
            do {
                var current = fragment;
                var controls = current.payload();
                topologyInputs.receive(current, key -> {
                    var page = pages.pageAt(key); return page == null ? null : page.handle;
                });
                lastBatchSequence = Math.max(lastBatchSequence, controls.sequence());
                chunks.add(chunkForSection(current.section()));
                retireInputSlot(current.section());
                if (diagnostics != null) diagnostics.inputRetired(controls.sequence(), current.section());
                if (System.nanoTime() >= captureDeadline) break;
                fragment = topologyIngress.poll();
            } while (fragment != null);
            inputDependencyRevision++; clearPendingBoundaries(); selectionIntake = -1;
            refreshBoundaryHolds();
            synchronized (intakeLock) { for (long chunk : chunks) refreshChunkInputHold(chunk); }
        }
        if (!restored && !received && !sourceIngress.hasReadyWork()) receiveRestore();
        long controlsBefore = inputDependencyRevision;
        sourceIngress.advance(deadline);
        // Binding either installed a topology command or removed an announced source fence.
        // In both cases re-evaluate waiters before sealing numerical time.
        double wind;
        synchronized (intakeLock) {
            if (boundarySequence != offeredSequence || boundaryControlRevision != sourceIngress.controlRevision()
                    || controlsBefore != inputDependencyRevision) refreshBoundaryHoldsLocked();
            transportThrough = offeredThrough; wind = offeredWind;
        }
        regional.sealSpatialInput(transportThrough, wind);
        lastIntakeNanos = System.nanoTime() - started;
    }

    private long pendingInputBoundary(long section) { return topologyIngress.boundary(section); }

    private RegionalContinuationBoundary inputBoundary(ThermalInputBatch input, LongSet mixing) {
        var seeds = new LongOpenHashSet();
        TopologyUpdatePlanner.collectBoundarySeeds(input, seeds);
        return inputBoundary(seeds, RegionalContinuationBoundary.hasStructuralInput(input), mixing);
    }

    private RegionalContinuationBoundary inputBoundary(LongSet seeds, boolean structural, LongSet mixing) {
        var sections = new LongOpenHashSet(seeds);
        for (long position : mixing) {
            int x = net.minecraft.core.BlockPos.getX(position) >> 4;
            int y = net.minecraft.core.BlockPos.getY(position) >> 4;
            int z = net.minecraft.core.BlockPos.getZ(position) >> 4;
            sections.add(net.minecraft.core.SectionPos.asLong(x, y, z));
        }
        // Source membership changes discover passages too. Seed the same full
        // closure before freezing geometry, instead of appending a small halo
        // after a route closure that never saw the source change.
        boolean changesRoutes = structural || !mixing.isEmpty();
        var boundary = topologyPlan.continuationBoundary(sections, changesRoutes);
        for (var work : geometryWorks) if (overlaps(work.boundary, boundary))
            boundary = boundary.including(work.planner.continuationBoundary(sections, changesRoutes).sections());
        return regional.expandBoundary(sourceBindings.includeBoundOutlets(boundary));
    }

    private int nextWorkLane;
    private long lastSealedThrough = -1;
    private boolean intakeHasReadyWork() {
        return restoreReady() || topologyIngress.hasWork() || sourceIngress.hasReadyWork();
    }

    public void beginFailureRecovery() {
        if (!failureRecovery) { failureRecovery = true; regional.suspendAfterFailure(); }
    }
    public boolean checkpointRecovery() { return failureRecovery; }
    public boolean controlWorkPending() { return checkpointControlReady; }

    public void processWorkRound() {
        long started = System.nanoTime(), deadline = started + 2_000_000L;
        if (failureRecovery) {
            if (snapshotCommandsReady()) processSnapshotCommand(deadline); else regional.runWorkRound(deadline);
            return;
        }
        var diagnostics = com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer;
        if (diagnostics != null) diagnostics.engine(regional, arena, sources, queries, airReservation, topologyInputs, offeredThrough);
        if (boundarySequence != offeredSequence) refreshBoundaryHolds();
        if (snapshotCommandsReady() && processSnapshotCommand(deadline)) {
            reconcileGeometryBacking();
            return;
        }
        var observation = jdk.jfr.FlightRecorder.isInitialized() ? new RoundObservation() : null;
        if (observation != null) {
            observation.sequence = lastBatchSequence; observation.intakeTick = transportThrough;
            observation.geometryPhase = geometryWorks.isEmpty() ? -1 : geometryWorks.peekFirst().phase.ordinal(); observation.begin();
        }
        if (capacityContinuation != null) { airBudget.cancelCapacityWait(capacityContinuation); capacityContinuation = null; }
        // One owner class receives the shared coordinator slice. Later classes rotate to
        // the next round instead of each consuming an additional full deadline.
        int selectedLane = -1;
        for (int visited = 0; visited < 4; visited++) {
            int lane = (nextWorkLane + visited) & 3;
            boolean ready = switch (lane) {
                case 0 -> intakeHasReadyWork() || regional.hasSealWork() || lastSealedThrough != offeredThrough;
                case 1 -> regional.hasReadyWork();
                case 2 -> geometryCanStart() && !topologyInputs.isEmpty() && selectionChanged()
                        || geometryWorks.stream().anyMatch(GeometryWork::ready);
                default -> regional.hasProgressWork() || phases.hasReadyRequests() || pages.maintenanceReady(airBudget.capacityRevision());
            };
            if (ready) { selectedLane = lane; nextWorkLane = (lane + 1) & 3; break; }
        }
        long laneStarted = System.nanoTime();
        switch (selectedLane) {
            case 0 -> { receiveRegionalInput(deadline); lastSealedThrough = transportThrough; }
            case 1 -> regional.runWorkRound(deadline);
            case 2 -> {
                if (geometryCanStart() && selectionChanged()) startGeometryWork(deadline);
                int remaining = geometryWorks.size();
                while (remaining-- > 0 && System.nanoTime() < deadline) {
                    var work = geometryWorks.removeFirst(); geometryWorks.addLast(work);
                    if (!work.ready()) continue;
                    if (work.advance(deadline)) {
                        if (diagnostics != null) diagnostics.geometry(work, work.input, work.boundary, work.phase.name(), true);
                        geometryWorks.remove(work); structureCommits++; selectionTopology = -1;
                        synchronized (intakeLock) {
                            for (long section : work.boundary.sections()) refreshChunkInputHold(chunkForSection(section));
                        }
                    }
                    if (System.nanoTime() >= deadline) break;
                }
            }
            case 3 -> {
                regional.publishChunkProgress(8);
                phases.advanceMaterialRequests(32);
                pages.advanceMaintenance(arena, parameters.referenceTemperatureC(), deadline,
                        airBudget.capacityRevision(), update -> residencyEffects.put(update.sectionKey(), update));
            }
            default -> { }
        }
        reconcileGeometryBacking();
        long laneFinished = System.nanoTime();
        if ((selectedLane == 1 || selectedLane == 0)) {
            var request = regional.qualityControl().next();
            if (request != null) {
                var allowance = airBudget.tryReserve(4096L + 64L * request.sections().size() + sourceBindings.frozenPreparationBytes());
                if (allowance == null) regional.qualityControl().abandon(request);
                else {
                    // A quality candidate may intersect a parent's material fringe.
                    // Capture the entire physical owner before replacing that owner.
                    var boundary = regional.expandBoundary(RegionalContinuationBoundary.UNCHANGED.including(request.sections()));
                    if (!allowance.tryResize(4096L + 64L * boundary.sectionCount() + sourceBindings.frozenPreparationBytes())) {
                        allowance.close(); regional.qualityControl().abandon(request);
                    } else {
                        long tick = request.target() == RegionalQualityControl.Model.MINIMUM
                                ? regional.recoveryHandoffTick(boundary) : request.tick();
                        var input = new ThermalInputBatch(dimensionGeneration, Math.max(1, offeredSequence), tick,
                                ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RETIREMENTS, ThermalInputBatch.NO_RESIDENCY_UPDATES,
                                com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch.EMPTY,
                                ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN);
                        geometryWorks.addLast(new GeometryWork(java.util.List.of(new TopologyInbox.Input(request.page(), input,
                                LongSets.EMPTY_SET, false)), boundary, allowance, request));
                        if (request.target() == RegionalQualityControl.Model.MINIMUM)
                            regional.blockBoundary(boundary, tick, input.sequence());
                    }
                }
            }
        }
        long maintenanceFinished = System.nanoTime();
        updateRegionalProgress();
        if (diagnostics != null) diagnostics.roundFinished(regional, arena, sources, topologyInputs, offeredThrough, hasWork());
        long finished = System.nanoTime();
        lastRoundNanos = finished - started; maximumRoundNanos = Math.max(maximumRoundNanos, lastRoundNanos);
        if (observation != null) {
            long laneNanos = laneFinished - laneStarted;
            observation.inputNanos = selectedLane == 0 ? laneNanos : 0;
            observation.coordinatorRegionNanos = selectedLane == 1 ? laneNanos : 0;
            observation.geometryNanos = selectedLane == 2 ? laneNanos : 0;
            observation.maintenanceNanos = selectedLane == 3 ? laneNanos : 0;
            observation.qualityNanos = maintenanceFinished - laneFinished;
            observation.progressNanos = finished - maintenanceFinished;
            observation.end(); observation.commit();
        }
    }

    /** Bound temporary overlap to available workers while independent regions remain parallel. */
    private boolean geometryCanStart() {
        int active = 0;
        for (var work : geometryWorks) if (work.phase.controls() || work.scratch != null || work.resetting) active++;
        return active < com.teammoeg.frostedheart.content.climate.thermal.runtime.async.ThermalWorkerPool.shared().parallelism();
    }

    private void startGeometryWork(long deadline) {
        if (selectionRemaining == 0) {
            selectionIntake = lastBatchSequence; selectionCommit = regional.commitRevision(); selectionTopology = solver.structuralVersion();
            selectionRouting = regional.routingRevision(); selectionSources = inputRevision();
            selectionRemaining = topologyInputs.pendingSectionCount();
        }
        int closures = 0, visited = 0;
        while (selectionRemaining > 0 && !topologyInputs.isEmpty() && visited++ < 16
                && closures == 0 && System.nanoTime() < deadline) {
            selectionRemaining--;
            long section = topologyInputs.nextPendingSection();
            // Every selected boundary includes its own input Section. If that
            // Section is already occupied, computing its full route closure
            // cannot make it selectable; independent Sections still proceed.
            boolean sectionOccupied = false;
            for (var work : geometryWorks) if (work.boundary.affects(section)) { sectionOccupied = true; break; }
            if (sectionOccupied) continue;
            var input = topologyInputs.peek(section);
            if (input == null) continue;
            var cached = pendingBoundaries.get(section);
            long topology = solver.structuralVersion();
            if (cached == null || cached.input != input || cached.topology != topology) {
                if (cached != null) { pendingBoundaries.remove(section); cached.reservation.close(); }
                long estimated = 4096L + sourceBindings.frozenPreparationBytes() + topologyPlan.continuationBoundaryBytes(input.batch())
                        + 1024L * input.mixing().size();
                var allowance = airBudget.reserveCompletion(estimated);
                closures++;
                var boundary = inputBoundary(input.batch(), input.mixing()).including(new LongOpenHashSet(new long[] {section}));
                cached = new BoundaryCandidate(input, topology, boundary, allowance); pendingBoundaries.put(section, cached);
            }
            var boundary = cached.boundary;
            cached.reservation.completeResize(4096L + 64L * boundary.sectionCount() + sourceBindings.frozenPreparationBytes());
            boolean occupied = false;
            for (var work : geometryWorks) if (overlaps(work.boundary, boundary)) { occupied = true; break; }
            if (occupied) continue;
            if (!boundarySourcesReady(boundary, input.batch().targetTick(), input.batch().sequence()) || !regional.boundaryReady(boundary, input.batch().targetTick(), input.batch().sequence())) continue;
            var selected = topologyInputs.freeze(boundary.sections(), input.batch().targetTick(), input.batch().sequence());
            if (!selected.isEmpty()) {
                pendingBoundaries.remove(section);
                geometryWorks.addLast(new GeometryWork(selected, boundary, cached.reservation));
                selectionIntake = -1;
                return;
            }
        }
        if (topologyInputs.isEmpty()) selectionRemaining = 0;
    }

    private boolean boundarySourcesReady(RegionalContinuationBoundary boundary, long tick, long sequence) {
        return sourceIngress.boundThrough(boundary, tick, sequence);
    }

    private long activeGeometryTick(long section) {
        long tick = Long.MAX_VALUE;
        for (var work : geometryWorks) if (work.phase != GeometryWork.Stage.DONE && (!work.recovering() || work.recoveryHandoff) && work.boundary.affects(section)) tick = Math.min(tick, work.input.targetTick());
        return tick;
    }

    private long activeGeometryTickExcept(long section, GeometryWork except) {
        long tick = Long.MAX_VALUE;
        for (var work : geometryWorks) if (work != except && work.phase != GeometryWork.Stage.DONE && work.boundary.affects(section))
            tick = Math.min(tick, work.input.targetTick());
        return tick;
    }

    private static boolean overlaps(RegionalContinuationBoundary first, RegionalContinuationBoundary second) {
        var smaller = first.sectionCount() < second.sectionCount() ? first.sections() : second.sections();
        var larger = smaller == first.sections() ? second : first;
        for (long section : smaller) if (larger.affects(section)) return true;
        return false;
    }

    private void clearPendingBoundaries() {
        for (var cached : pendingBoundaries.values()) cached.reservation.close();
        pendingBoundaries.clear();
        selectionRemaining = 0;
    }

    private boolean selectionChanged() {
        return selectionRemaining > 0 || selectionIntake != lastBatchSequence || selectionCommit != regional.commitRevision()
                || selectionSources != inputRevision()
                || selectionRouting != regional.routingRevision()
                || selectionTopology != solver.structuralVersion();
    }

    private final class GeometryWork implements AutoCloseable {
        private RegionalAirRuntime.PageInputRetirement pageInputRetirement;
        final java.util.List<TopologyInbox.Input> inputs;
        ThermalInputBatch input;
        boolean installationCleaned;
        final RegionalContinuationBoundary boundary;
        enum Kind { STRUCTURE, QUALITY_DOWN, QUALITY_RECOVERY }
        enum Stage {
            MATERIAL_INPUT, MATERIAL_SETTLE, TOPOLOGY, REGIONS, PUBLICATION, ACCOUNTING, INSTALLATION, COMMIT, CLEANUP, DONE;
            boolean controls() { return this == MATERIAL_INPUT || this == MATERIAL_SETTLE; }
            boolean beforePublication() { return this != CLEANUP && this != DONE; }
        }
        final Kind kind;
        final RegionalQualityControl.Change qualityOwner;
        boolean recovering() { return kind == Kind.QUALITY_RECOVERY; }
        boolean recoveryHandoff, recoveryProjectionResumed, abandoning;
        long handoffWaitCommit = -1, handoffWaitInput = -1, handoffWaitRouting = -1;
        final WorkerPhysicalSourceBindings.FrozenSources emitters;
        final LongOpenHashSet mixing = new LongOpenHashSet();
        TopologyUpdatePlanner planner;
        PreparedTopologyChange prepared;
        FrozenAirGeometry captured;
        RegionalAirRuntime.Rebuild rebuild;
        RegionalAirRuntime.RebuildPreparation regionPreparation;
        ThermalMemoryBudget.Reservation scratch;
        final ThermalMemoryBudget.Reservation boundaryReservation;
        Stage phase = Stage.MATERIAL_INPUT;
        boolean refused;
        boolean numericalScratchAdmitted;
        boolean resetting, postCommitApplied, backingAdmitted;
        boolean topologyPreflight;
        String resourceWaitReason;
        com.teammoeg.frostedheart.content.climate.thermal.topology.RegionalTopologyInstall installation;
        long waitingCapacity = -1;
        long capacityAttempt;
        long validatedStructure = -1;
        GeometryWork(java.util.List<TopologyInbox.Input> inputs, RegionalContinuationBoundary boundary,
                ThermalMemoryBudget.Reservation boundaryReservation) {
            this(inputs, boundary, boundaryReservation, null);
        }
        GeometryWork(java.util.List<TopologyInbox.Input> inputs, RegionalContinuationBoundary boundary,
                ThermalMemoryBudget.Reservation boundaryReservation, RegionalQualityControl.Change qualityOwner) {
            this.inputs = inputs; this.boundary = boundary;
            this.qualityOwner = qualityOwner;
            kind = qualityOwner == null ? Kind.STRUCTURE : qualityOwner.target() == RegionalQualityControl.Model.FINE
                    ? Kind.QUALITY_RECOVERY : Kind.QUALITY_DOWN;
            this.boundaryReservation = boundaryReservation;
            var first = inputs.get(0); emitters = sourceBindings.freeze(boundary.sections(), recovering() ? offeredThrough : first.batch().targetTick());
            input = TopologyInbox.combine(inputs, first.batch().sequence(), first.batch().targetTick());
            for (var entry : inputs) mixing.addAll(entry.mixing());
            planner = topologyPlan.forInput(emitters, boundary.sections());
        }
        boolean advance(long deadline) {
            do {
                Stage before = phase;
                if (advanceStage(deadline)) return true;
                // Waiting stages and the common backing-admission point belong to the coordinator.
                if (phase == before || phase == Stage.ACCOUNTING || resetting || abandoning || resourceWaitReason != null) return false;
            } while (System.nanoTime() < deadline);
            return false;
        }
        private boolean advanceStage(long deadline) {
            var diagnostics = com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer;
            if (diagnostics != null) diagnostics.geometry(this, input, boundary, phase.name(), false);
            if (!ready()) return false;
            waitingCapacity = -1;
            capacityAttempt = airBudget.capacityRevision();
            try {
                if (kind != Kind.STRUCTURE && phase.beforePublication() && !regional.qualityControl().current(qualityOwner)) abandoning = true;
                if (recovering() && phase.beforePublication() && !recoveryInputsCurrent()) abandoning = true;
                if (abandoning) {
                    resetPreparation(deadline); if (resetting) return false;
                    regional.qualityControl().abandon(qualityOwner); phase = Stage.DONE;
                    refreshBoundaryHolds(); regional.resumeAfterBoundaries(); inputDependencyRevision++;
                    close(); return true;
                }
                if (resetting) { resetPreparation(deadline); return false; }
                if (kind == Kind.QUALITY_DOWN && phase == Stage.MATERIAL_INPUT && !boundaryReached()) return false;
                if (!phase.controls() && phase.beforePublication() && (!dependenciesCurrent()
                        || captured != null && rebuild != null && rebuild.ready() && !captured.dependenciesCurrent(pages))) {
                    if (recovering()) abandoning = true;
                    else resetPreparation(deadline);
                    return false;
                }
                if (phase == Stage.MATERIAL_INPUT) {
                    regional.acceptAdmissions(input);
                    for (var ack : input.phaseAcks()) phases.applyAck(ack.request(), ack.outcome());
                    for (var intent : input.phaseIntents()) {
                        int slot = pages.materialSlot(intent.page(), intent.blockIndex(), intent.sourceStateId());
                        if (slot >= 0) phases.forceMaterialTransition(slot, intent.branch());
                    }
                    pages.awaitChangedMaterials(input, arena, regional::materialControlChanged);
                    phase = Stage.MATERIAL_SETTLE;
                }
                if (phase == Stage.MATERIAL_SETTLE) {
                    if (!regional.commitMaterialControls(boundary)) return false;
                    planner.beginCut(); phase = Stage.TOPOLOGY;
                    return false;
                }
                if (phase == Stage.TOPOLOGY) {
                    long bytes = planner.preparationScratchBytes(input);
                    if (scratch == null) {
                        scratch = airBudget.reserveCompletion(bytes);
                    } else if (scratch.bytes() < bytes) scratch.completeResize(bytes);
                    prepared = planner.prepareRound(input, mixing, deadline, scratch);
                    if (prepared == null) return false;
                    observeGeometry("prepared");
                    long retainedBytes = planner.completedPreparationBytes(prepared);
                    scratch.completeResize(retainedBytes);
                    phase = Stage.REGIONS;
                    return false;
                }
                if (phase == Stage.REGIONS) {
                    if (kind == Kind.STRUCTURE && prepared.onlyNaturalTemperatureUpdates()) { phase = Stage.INSTALLATION; return false; }
                    if (captured == null) captured = planner.freezeAirGeometry(emitters, scratch);
                    if (!numericalScratchAdmitted) {
                        long bytes = Math.addExact(scratch.bytes(), regional.preparationMetadataBytes(
                                captured, prepared, boundary, emitters.activeAirPortCount()));
                        scratch.completeResize(bytes);
                        numericalScratchAdmitted = true;
                    }
                    if (rebuild == null) {
                        if (regionPreparation == null) regionPreparation = kind == Kind.STRUCTURE
                                ? regional.prepareLocalRebuild(emitters, prepared, captured, input.targetTick(), scratch, boundary)
                                : regional.prepareLocalQuality(emitters, prepared, captured, input.targetTick(), scratch, boundary, recovering());
                        if (!regionPreparation.run(deadline)) return false;
                        rebuild = regionPreparation.result(); regionPreparation.close(); regionPreparation = null;
                        return false;
                    }
                    if (recovering()) {
                        if (rebuild.recoveryAborted()) { abandoning = true; return false; }
                        if (!recoveryHandoff && rebuild.staticRecoveryReady()) {
                            long handoffTick = regional.recoveryHandoffTick(boundary);
                            for (long section : boundary.sections()) if (Math.min(pendingInputBoundary(section),
                                    Math.min(topologyInputs.firstTick(section), activeGeometryTickExcept(section, this))) < handoffTick) {
                                abandoning = true; return false;
                            }
                            recoveryHandoff = true;
                            input = new ThermalInputBatch(dimensionGeneration, Math.max(1, offeredSequence), handoffTick,
                                    ThermalInputBatch.NO_ADMISSIONS, ThermalInputBatch.NO_RETIREMENTS, ThermalInputBatch.NO_RESIDENCY_UPDATES,
                                    com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch.EMPTY,
                                    ThermalInputBatch.NO_ENVIRONMENT_UPDATES, ThermalInputBatch.NO_PHASE_ACKS, Double.NaN);
                            regional.blockBoundary(boundary, input.targetTick(), input.sequence());
                            return false;
                        }
                        if (!recoveryProjectionResumed && rebuild.staticRecoveryReady()) {
                            if (!boundaryReached()) return false;
                            if (!rebuild.resumeRecovery(input.targetTick())) { abandoning = true; return false; }
                            recoveryProjectionResumed = true;
                        }
                    }
                    if (!rebuild.advance()) return false;
                    phase = Stage.PUBLICATION;
                    return false;
                }
                if (phase == Stage.PUBLICATION) {
                    if (recovering()) {
                        if (!rebuild.validateRecovery(deadline)) return false;
                        if (!(rebuild.recoveryErrorC() < 1.0e-4)) {
                            lastWorkLimitedReason = "Fine recovery same-tick difference=" + rebuild.recoveryErrorC();
                            abandoning = true; return false;
                        }
                    }
                    rebuild.prepareInputHandoff();
                    if (!rebuild.prepareQueryInstallation(queries, deadline)) return false;
                    if (installation == null) installation = new com.teammoeg.frostedheart.content.climate.thermal.topology.RegionalTopologyInstall(
                            prepared, pages, arena, solver, phases);
                    if (!rebuild.prepareOwnership(installation.token, deadline)) return false;
                    if (!rebuild.prepareSupportChanges(deadline)) return false;
                    // The coordinator meters every ready handoff together before
                    // any of them can enter installation. There is no batch barrier.
                    phase = Stage.ACCOUNTING;
                    return false;
                }
                if (phase == Stage.INSTALLATION) {
                    if (!topologyPreflight) {
                        planner.prepareCommit(prepared);
                        if (installation == null) installation = new com.teammoeg.frostedheart.content.climate.thermal.topology.RegionalTopologyInstall(
                                prepared, pages, arena, solver, phases);
                        topologyPreflight = true;
                    }
                    if (!installation.prepare(deadline)) return false;
                    if (!installation.valid()) { resetPreparation(deadline); return false; }
                    phase = Stage.COMMIT; return false;
                }
                if (phase == Stage.COMMIT) {
                    if (rebuild != null) rebuild.prepareInputHandoff();
                    if (rebuild != null && !rebuild.prepareQueryInstallation(queries, deadline)) return false;
                    if (!regional.publishGeometry(rebuild, input, prepared.installedVersion(solver.structuralVersion()),
                            () -> { installation.commit(); planner.committed(); })) {
                        return false;
                    }
                    observeGeometry("published");
                    if (diagnostics != null) diagnostics.geometry(this, input, boundary, "PUBLISHED", false);
                    phase = Stage.CLEANUP; return false;
                }
                if (phase == Stage.CLEANUP) {
                    if (!cleanupInstalled(deadline)) return false;
                    return finishInput();
                }
                return phase == Stage.DONE;
            } catch (TopologyUpdatePlanner.WorkLimitedException | ContinuousAirSolver.PreparationLimitedException
                    | ThermalMemoryBudget.AllocationRefused limited) {
                lastWorkLimitedReason = limited.getMessage() + ", rootBeforeCleanup=" + airBudget.rootUsedBytes(); refused = true;
                // Observation only: ownership stays here; no recapture instruction
                // accompanies this notification to the main thread.
                limitedEffect = true;
                // Publication is irrevocable. A post-commit continuation keeps
                // its cleanup cursor and ownership instead of cancelling live state.
                if (!phase.beforePublication()) return waitForCapacity();
                if (recovering()) { abandoning = true; return false; }
                resourceWaitReason = limited instanceof TopologyUpdatePlanner.WorkLimitedException
                        ? "topology_capacity" : "resource_pressure";
                resetPreparation(deadline);
                return false;
            }
        }
        private boolean recoveryInputsCurrent() {
            for (long section : boundary.sections()) if (pendingInputBoundary(section) != Long.MAX_VALUE
                    || topologyInputs.firstTick(section) != Long.MAX_VALUE
                    || activeGeometryTickExcept(section, this) != Long.MAX_VALUE) return false;
            return !sourceBindings.mixingChangedAfter(emitters.mixingRevision(), boundary::affects);
        }
        boolean ready() {
            if (abandoning) return rebuild == null || rebuild.helpersReturned();
            if (resetting) return rebuild == null || rebuild.helpersReturned();
            if (phase.beforePublication() && snapshotTouches(boundary)) return false;
            if (phase == Stage.TOPOLOGY && scratch == null && !geometryCanStart()) return false;
            if (kind != Kind.STRUCTURE && !regional.qualityControl().current(qualityOwner) && phase.beforePublication()) return true;
            if (recovering() && phase.beforePublication() && !recoveryInputsCurrent()) return true;
            if (handoffWaitCommit >= 0 && !recoveryProjectionResumed && handoffWaitCommit == regional.commitRevision()
                    && handoffWaitInput == inputRevision() && handoffWaitRouting == regional.routingRevision()) return false;
            return (waitingCapacity < 0 || waitingCapacity != airBudget.capacityRevision())
                    && (phase != Stage.REGIONS || rebuild == null || rebuild.ready());
        }
        private boolean waitForCapacity() {
            if (recovering() && phase.beforePublication()) abandoning = true;
            else waitingCapacity = capacityAttempt;
            return false;
        }
        private boolean boundaryReached() {
            if (!boundarySourcesReady(boundary, input.targetTick(), input.sequence())
                    || !regional.boundaryReady(boundary, input.targetTick(), input.sequence())) {
                handoffWaitCommit = regional.commitRevision(); handoffWaitInput = inputRevision();
                handoffWaitRouting = regional.routingRevision(); return false;
            }
            handoffWaitCommit = handoffWaitInput = handoffWaitRouting = -1;
            return true;
        }
        private boolean dependenciesCurrent() {
            if (validatedStructure == structureCommits) return true;
            if (!planner.dependenciesCurrent() || rebuild != null && !rebuild.parentsCurrent()) return false;
            validatedStructure = structureCommits;
            return true;
        }
        @Override public void close() {
            if (regionPreparation != null) { regionPreparation.close(); regionPreparation = null; }
            if (rebuild != null && !rebuild.tryClose())
                throw new IllegalStateException("Construction readers still own their captured input");
            rebuild = null;
            if (captured != null) { captured.close(); captured = null; }
            releaseScratch(); boundaryReservation.close();
            // Even a private candidate may have pinned an older published layout.
            backingAdmitted = false; backingNeedsReclaim = true;
            prepared = null; planner = null;
            if (qualityOwner != null) regional.qualityControl().abandon(qualityOwner);
        }
        private boolean finishInput() {
            topologyInputs.complete(inputs);
            clearPendingBoundaries();
            phase = Stage.DONE; inputDependencyRevision++;
            refreshBoundaryHolds(); regional.resumeAfterBoundaries();
            for (long section : boundary.sections()) { refreshChunkInputHold(chunkForSection(section)); retireInputSlot(section); }
            limitedEffect |= refused;
            close();
            return true;
        }
        private boolean cleanupInstalled(long deadline) {
            if (installationCleaned) return true;
            if (!postCommitApplied) {
                if (pageInputRetirement == null) pageInputRetirement = regional.preparePageInputRetirement(input);
                if (!pageInputRetirement.run()) return false;
                sourceBindings.markCommittedSections(prepared.sourceDirtySections);
                sourceBindings.mixingCommitted(emitters.mixingRevision(), mixing);
                sourceBindings.rebindDirty(sources, id -> sourceBindings.geometryCurrent(id,
                        section -> activeGeometryTickExcept(section, this) == Long.MAX_VALUE));
                postCommitApplied = true;
            }
            if (!installation.cleanup(deadline)) return false;
            if (rebuild != null && !rebuild.applySupportChanges(deadline)) return false;
            if (rebuild != null && !rebuild.releaseSupportChanges(deadline)) return false;
            if (rebuild != null && !rebuild.cleanupOwnership(deadline)) return false;
            installation = null;
            queries.reclaimRegionalCapacityAfterAcknowledgement();
            for (var write : prepared.pageWrites) if (!write.retirement)
                write.page.handle.notePublishedGeometry(write.publication.geometryRevision(), input.targetTick());
            for (var token : prepared.committedResyncTokens) resyncEffects.put(token.sectionKey(), token);
            installationCleaned = true;
            return true;
        }
        private void resetPreparation(long deadline) {
            if (!resetting) observeGeometry("discarded");
            if (installation != null) {
                installation.cancel();
                if (!installation.cleanup(deadline)) { resetting = true; return; }
                if (rebuild != null && !rebuild.cleanupOwnership(deadline)) { resetting = true; return; }
                installation = null;
            }
            if (regionPreparation != null) { regionPreparation.close(); regionPreparation = null; }
            if (rebuild != null && !rebuild.releaseSupportChanges(deadline)) { resetting = true; return; }
            if (rebuild != null && !rebuild.tryClose()) { resetting = true; return; }
            resetting = false; rebuild = null;
            if (captured != null) { captured.close(); captured = null; }
            planner.discardPrepared();
            // The preparation charge is about to retire. A discarded task must
            // also drop its grown draft/halo/material tables, rather than keep
            // them live and require their full capacity before the next retry.
            planner = topologyPlan.forInput(emitters, boundary.sections());
            releaseScratch(); prepared = null; phase = Stage.TOPOLOGY; validatedStructure = -1;
            numericalScratchAdmitted = false;
            topologyPreflight = false;
            planner.beginCut();
            backingAdmitted = false; backingNeedsReclaim = true;
        }
        private void registerCapacityWait() {
            if (resetting) return;
            if (resourceWaitReason != null) {
                // Register after returning scratch and transferred backing, so a release
                // cannot immediately wake the very same failed attempt.
                waitingCapacity = airBudget.capacityRevision();
                if (!"topology_capacity".equals(resourceWaitReason)) regional.qualityControl().resourcePressure(boundary);
                resourceWaitReason = null;
            }
        }
        private void observeGeometry(String stage) {
            if (prepared == null || !jdk.jfr.FlightRecorder.isInitialized()) return;
            var event = new GeometryObservation();
            if (!event.isEnabled()) return;
            for (var write : prepared.pageWrites) {
                event = new GeometryObservation();
                event.stage = stage; event.kind = kind.name();
                event.section = write.page.handle.sectionKey();
                event.lifecycle = write.page.handle.lifecycleGeneration();
                event.targetTick = input.targetTick(); event.sequence = input.sequence();
                event.inputRevision = -1;
                for (var halo : input.geometry().halos()) if (halo.page() == write.page.handle)
                    event.inputRevision = Math.max(event.inputRevision, halo.geometryRevision());
                for (int i = 0; i < input.geometry().size(); i++) if (input.geometry().page(i) == write.page.handle)
                    event.inputRevision = Math.max(event.inputRevision, input.geometry().geometryRevision(i));
                event.proposedRevision = write.publication.geometryRevision();
                var visible = write.page.handle.lastPublication();
                event.visibleRevision = visible == null ? -1 : visible.geometryRevision();
                event.liveRevision = write.page.handle.liveGeometryRevision();
                event.retirement = write.retirement; event.commit();
            }
        }
        private void releaseScratch() {
            if (scratch != null) { scratch.close(); scratch = null; }
        }
    }

    @jdk.jfr.Name("frostedheart.ThermalGeometryPublication")
    @jdk.jfr.StackTrace(false)
    private static final class GeometryObservation extends jdk.jfr.Event {
        public String stage, kind;
        public long section, lifecycle, targetTick, sequence, inputRevision, proposedRevision, visibleRevision, liveRevision;
        public boolean retirement;
    }

    private void updateRegionalProgress() {
        var structural = geometryWorks.peekFirst();
        currentGeometryPhase = structural == null ? "none" : structural.phase.name();
        currentTopologyPhase = structural == null ? -1 : structural.planner.preparationPhase();
        currentRouteVisits = structural == null ? 0 : structural.planner.routeVisitsLastCut();
        regional.publishChunkProgress(8);
        minimumRegionTick = regional.minimumSampleTick(transportThrough);
        lastWorkerAirCoefficients = regional.coefficientCount();
        publishedBusyChunks = regional.busyChunkCount();
        lastWorkerContactMode = regional.progressContactSummary();
        publishedPendingJ = regional.pendingEnergyJ();
        if (PROFILE && offeredThrough - minimumRegionTick > 80 && offeredThrough - lastWaitObservationTick >= 200) {
            lastWaitObservationTick = offeredThrough;
            observeInputWaits();
        }
    }

    /** Bounded failure observations; normal play neither scans nor builds these descriptions. */
    private void observeInputWaits() {
        LOGGER.info("THERMAL_CONTROL_WAIT,receivedTick={},snapshotStage={},snapshotChunk={},snapshotFinished={},regionalReady={}",
                offeredThrough, activeSnapshot == null ? "none" : activeSnapshot.stage,
                activeSnapshot == null ? 0 : activeSnapshot.chunk,
                activeSnapshot != null && activeSnapshot.finished != null, regional.hasReadyWork());
        int remaining = 6;
        for (long section : topologyInputs.pendingSections()) {
            if (remaining-- == 0) break;
            var input = topologyInputs.peek(section);
            var cached = pendingBoundaries.get(section);
            var boundary = cached == null ? null : cached.boundary;
            LOGGER.info("THERMAL_TOPOLOGY_WAIT,receivedTick={},section={},tick={},sequence={},cached={},sourceReady={},regionWait={}",
                    offeredThrough, section, input.batch().targetTick(), input.batch().sequence(), boundary != null,
                    boundary == null ? "unknown" : boundarySourcesReady(boundary, input.batch().targetTick(), input.batch().sequence()),
                    boundary == null ? "unknown" : regional.boundaryWaitDescription(boundary, input.batch().targetTick(), input.batch().sequence()));
        }
        for (long section : topologyInputs.pendingSections()) {
            var input = topologyInputs.peek(section);
            var cached = pendingBoundaries.get(section);
            if (cached == null) continue;
            var blocker = sourceIngress.firstBlocker(cached.boundary(), input.batch().targetTick(), input.batch().sequence());
            if (blocker != null) {
                LOGGER.info("THERMAL_BOUNDARY_BLOCKER,section={},tick={},sequence={},{}", section,
                        input.batch().targetTick(), input.batch().sequence(), blocker);
                break;
            }
        }
    }

    public long minimumRegionTick() { return minimumRegionTick; }
    public boolean hasRetainedInputInChunk(long chunk) {
        return restoringChunks.containsKey(chunk) || pendingChunkInput.containsKey(chunk)
                || sourceIngress.hasOfferedInChunk(chunk) || regional.hasRetainedInputInChunk(chunk);
    }

    public boolean hasReadyWork() {
        if (failureRecovery) return snapshotCommandsReady() || regional.hasReadyWork();
        if (snapshotCommandsReady()) return true;
        if (intakeHasReadyWork() || regional.hasSealWork() || lastSealedThrough != offeredThrough) return true;
        if (regional.hasReadyWork() || regional.hasProgressWork() || phases.hasReadyRequests() || pages.maintenanceReady(airBudget.capacityRevision())
                || geometryCanStart() && !topologyInputs.isEmpty() && selectionChanged()) return true;
        for (var work : geometryWorks) if (work.ready()) return true;
        return false;
    }
    public boolean hasWork() {
        if (failureRecovery) return hasReadyWork() || regional.hasRunningWork() || activeSnapshot != null;
        synchronized (intakeLock) {
            if (topologyIngress.hasWork() || sourceIngress.hasWork() || !restoreRequests.isEmpty() || activeSnapshot != null || !snapshotRequests.isEmpty()) return true;
        }
        return hasReadyWork() || regional.hasRunningWork() || regional.hasPendingInputs() || !geometryWorks.isEmpty()
                || !resyncEffects.isEmpty() || !residencyEffects.isEmpty();
    }
    public ThermalCompletion pollWorkCompletion() {
        var requests = phases.drainRequests(parameters.maximumPhaseMutationsPerCompletion());
        if (requests.length == 0 && resyncEffects.isEmpty() && residencyEffects.isEmpty() && !limitedEffect) return null;
        var result = new ThermalCompletion(dimensionGeneration, ++effectSequence,
                limitedEffect ? ThermalCompletion.Status.WORK_LIMITED : ThermalCompletion.Status.COMPLETED, null, requests,
                resyncEffects.values().toArray(ThermalCompletion.NO_RESYNC_TOKENS),
                residencyEffects.values().toArray(ThermalCompletion.NO_RESIDENCY_UPDATES));
        resyncEffects.clear(); residencyEffects.clear(); limitedEffect = false;
        return result;
    }

    public void whenRoundReady(Runnable continuation) {
        if (closeContinuationReady || !closing && hasReadyWork()) { continuation.run(); return; }
        regional.whenRoundReady(continuation);
        long waiting = pages.maintenanceCapacityWait();
        for (var work : geometryWorks) waiting = Math.max(waiting, work.waitingCapacity);
        if (waiting >= 0) {
            if (capacityContinuation != null) airBudget.cancelCapacityWait(capacityContinuation);
            capacityContinuation = continuation;
            airBudget.whenCapacityChanges(waiting, continuation);
        }
    }

    private void reserveAirBacking() { reserveAirBacking(airBackingBytes()); }

    /** One union covers the ready candidates and this slice's completed releases. */
    private void reconcileGeometryBacking() {
        boolean admitting = false;
        for (var work : geometryWorks) if (work.phase == GeometryWork.Stage.ACCOUNTING) {
            work.backingAdmitted = true; admitting = true;
        }
        if (admitting || backingNeedsReclaim) {
            long bytes = airBackingBytes();
            if (admitting) reserveAirBacking(bytes);
            else if (airReservation != null) {
                long required = Math.addExact(bytes, completionCapacityBytes());
                if (required < airReservation.bytes()) airReservation.tryResize(required);
            }
            backingNeedsReclaim = false;
        }
        for (var work : geometryWorks) {
            if (work.phase == GeometryWork.Stage.ACCOUNTING) work.phase = GeometryWork.Stage.INSTALLATION;
            work.registerCapacityWait();
        }
    }

    private void reserveAirBacking(long required) {
        required = Math.addExact(required, completionCapacityBytes());
        if (airReservation != null) airReservation.completeResize(required);
        else airReservation = airBudget.reserveCompletion(required);
    }

    /** Reserve all outstanding one-shot notifications before any physical commit. */
    private long completionCapacityBytes() {
        return ThermalCompletion.MAXIMUM_PENDING * (1024L + 128L * parameters.maximumPhaseMutationsPerCompletion()
                + 128L * limits.maximumPages());
    }

    /** Installed state and every transferred candidate share one allocation inventory. */
    private long airBackingBytes() {
        var meter = backingMeter;
        try {
            var admitted = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<RegionalAirRuntime.Rebuild, Boolean>());
            for (var work : geometryWorks) if (work.backingAdmitted && work.rebuild != null)
                admitted.add(work.rebuild);
            regional.recordHandoffBacking(meter, admitted);
            sourceIngress.recordBacking(meter);
            queries.recordAirLayoutBacking(meter);
            sourceBindings.recordMixingBacking(meter);
            recordWorkerStorage(meter);
            for (var work : geometryWorks) if (work.backingAdmitted && work.prepared != null) {
                work.prepared.recordInstalledBacking(meter);
                work.planner.recordInstalledRouteBacking(meter);
            }
            return Math.max(1L, meter.bytes());
        } finally {
            meter.clear();
        }
    }

    private void recordWorkerStorage(AirBackingMeter meter) {
        arena.recordBacking(meter); solver.recordBacking(meter); sources.recordBacking(meter); pages.recordBacking(meter);
        sourceBindings.recordBacking(meter);
        phases.recordBacking(meter);
        // Every live GeometryWork prepays its private draft/merge backing in
        // scratch. Only the installing task transfers persistent payload above.
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            closeOwnedState();
        } finally {
            var meter = backingMeter;
            backingMeter = null;
            if (meter != null) meter.close();
        }
    }

    private void closeOwnedState() {
        var diagnostics = com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer;
        if (diagnostics != null) diagnostics.closed(arena);
        synchronized (intakeLock) {
            var failure = new IllegalStateException("Thermal owner closed before checkpoint completion");
            if (activeSnapshot != null) {
                if (activeSnapshot.field != null) activeSnapshot.field.releaseFrames();
                if (activeSnapshot.stage != SnapshotStage.REQUESTED) regional.resumeSnapshot(activeSnapshot);
                if (activeSnapshot.reservation != null) activeSnapshot.reservation.close();
                activeSnapshot.ready.completeExceptionally(failure);
                if (activeSnapshot.finished != null) activeSnapshot.finished.completeExceptionally(failure);
                activeSnapshot = null;
            }
            for (var request : snapshotRequests) request.ready.completeExceptionally(failure);
            snapshotRequests.clear();
            for (var request : restoreRequests) request.reservation.close();
            restoreRequests.clear(); restoringChunks.clear();
            sourceIngress.close(); topologyIngress.close(); pendingChunkInput.clear();
        }

        regional.clearRegionalInputs();
        if (AirBackingMeter.debugEnabled()) recordBackingInventory();
        for (var work : geometryWorks) work.close();
        geometryWorks.clear();
        topologyInputs.clear(); resyncEffects.clear(); residencyEffects.clear();
        clearPendingBoundaries();
        if (capacityContinuation != null) { airBudget.cancelCapacityWait(capacityContinuation); capacityContinuation = null; }
        if (geometryRetention != null) { geometryRetention.close(); geometryRetention = null; }
        try {
            try {
                regional.abortPreparedSteps();
            } finally { sources.close(); }
        } finally {
            try {
                pages.close();
            } finally {
                try {
                    queries.close();
                } finally {
                    if (airReservation != null) {
                        if (!queries.retainReaderAirBacking(airReservation)) airReservation.close();
                        airReservation = null;
                    }
                }
            }
        }
    }

    public boolean closeRound() {
        if (closed) return true;
        long deadline = System.nanoTime() + 2_000_000L;
        closing = true; closeContinuationReady = false;
        if (!regional.settleJobs()) { closeContinuationReady = regional.hasHelperCleanup(); return false; }
        if (!geometryWorks.isEmpty()) {
            var work = geometryWorks.peekFirst();
            if (work.phase == GeometryWork.Stage.CLEANUP) {
                if (!work.advance(deadline)) { closeContinuationReady = true; return false; }
            } else {
                work.resetPreparation(deadline);
                if (work.resetting) { closeContinuationReady = work.rebuild == null || work.rebuild.helpersReturned(); return false; }
                work.close();
            }
            geometryWorks.remove(work);
            closeContinuationReady = true; return false;
        }
        close();
        return true;
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("thermal dimension engine is closed");
        }
    }

    /** Opt-in full inventory; ordinary profiling uses the local capacity summaries. */
    private void recordBackingInventory() {
        try (var meter = new AirBackingMeter()) {
            regional.recordBacking(meter, null); sourceIngress.recordBacking(meter);
            sourceBindings.recordMixingBacking(meter); queries.recordAirLayoutBacking(meter);
            long represented = meter.bytes();
            arena.recordBacking(meter); long arenaBytes = meter.bytes() - represented;
            long before = meter.bytes(); solver.recordBacking(meter); long topologyBytes = meter.bytes() - before;
            before = meter.bytes(); sources.recordBacking(meter); long sourceBytes = meter.bytes() - before;
            before = meter.bytes(); pages.recordBacking(meter); long pagesBytes = meter.bytes() - before;
            LOGGER.info("THERMAL_RUNTIME_BACKING_INVENTORY,representedFieldBytes={},additionalArenaBytes={},additionalTopologyBytes={},additionalSourceBytes={},additionalPagesRoutesBytes={},unionBytes={},scanWorkspaceBytes={},currentAirReservationBytes={},rootBytes={},rootPeakBytes={}",
                    represented, arenaBytes, topologyBytes, sourceBytes, pagesBytes, meter.bytes(), meter.workspaceBytes(),
                    airReservation == null ? 0 : airReservation.bytes(), airBudget.rootUsedBytes(), airBudget.rootPeakUsedBytes());
        }
    }
}
