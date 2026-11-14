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
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialStateAccess;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalCompletion;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputEnvelope;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch.MaterialChanges;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.source.AirLoadTable;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSolver;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceLedger;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceTimeline;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialSourceIngress;
import com.teammoeg.frostedheart.content.climate.thermal.source.SourceInputWindow;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatInput;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatStore;
import com.teammoeg.frostedheart.content.climate.thermal.source.SpatialHeatBuffer;
import com.teammoeg.frostedheart.content.climate.thermal.source.AirMixingRegion;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockBrickLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.TopologyIngress;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.RegionalCheckpoint;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.PendingHeatCheckpoint;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.MaterialSectionState;
import com.teammoeg.frostedheart.content.climate.thermal.solver.TiledThermalField;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalFragment;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.TopologyInbox;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import com.teammoeg.frostedheart.content.climate.thermal.topology.BrickTopologyCompiler;
import com.teammoeg.frostedheart.content.climate.thermal.topology.FarFieldSettings;
import com.teammoeg.frostedheart.content.climate.thermal.topology.RegionalContinuationBoundary;
import com.teammoeg.frostedheart.content.climate.thermal.topology.PreparedTopologyChange;
import com.teammoeg.frostedheart.content.climate.thermal.topology.ThermalTopologyParameters;
import com.teammoeg.frostedheart.content.climate.thermal.topology.TopologyUpdatePlanner;
import com.teammoeg.frostedheart.content.climate.thermal.topology.WorkerPageStore;

import java.util.Objects;

/** Dimension coordinator for the committed fixed field, source input and native save lifecycle. */
public final class ThermalDimensionEngine implements AutoCloseable, MaterialStateAccess, SpatialSourceIngress.InputOwner {

    private final long dimensionGeneration;
    private final ThermalTopologyParameters parameters;
    private final MaterialBoundaryRegistry materialProfiles;
    private final ThermalDimensionLimits limits;
    private final ThermalCellArena arena;
    private final QueryPublication queries;
    private final WorkerPageStore pages;
    private final PhaseTransitionRuntime phases;
    private final ThermalSolver solver;
    private final ThermalSourceLedger sources;
    private final WorkerPhysicalSourceBindings sourceBindings;
    private final TopologyUpdatePlanner topologyPlan;
    private final TiledThermalField fixedField;
    private final SpatialHeatBuffer fixedHeat;
    private final SpatialHeatStore fixedInputs;
    private ThermalMemoryBudget.Reservation fixedInputCredit;
    private final TiledThermalField.Sample fixedMaterial = new TiledThermalField.Sample();
    private SpatialHeatStore.Claim fixedClaim;
    private long fixedFromTick, fixedSolveTick, fixedCommits;
    private LongSet fixedScope = LongSets.EMPTY_SET;
    private FixedGeometry fixedGeometry;
    private final java.util.IdentityHashMap<TopologyInbox.Input, RegionalContinuationBoundary> fixedBoundaries = new java.util.IdentityHashMap<>();
    private final it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<RegionalContinuationBoundary> fixedActivationBoundaries = new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>();
    private it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap fixedPhysicalCuts = new it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap();
    private it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap fixedNextPhysicalCuts = new it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap();
    private long fixedBoundaryRevision = -1, fixedBoundaryStructure = -1, fixedControlRevision = -1, fixedBoundarySequence = -1, fixedActivationRevision = -1;
    private long sourceFreezeRevision;
    private long fixedCapacityWait = -1;
    private double fixedIncomingJ, fixedDiscardedJ, fixedPendingJ, fixedRejectedJ;
    private int[] fixedSignatures;
    private AirMixingRegion fixedMixing;
    private ThermalMemoryBudget.Reservation fixedSnapshotScratchCredit;
    private TiledThermalField.SectionSnapshot fixedSnapshotValues;
    private MaterialSectionState.CaptureScratch fixedMaterialCapture;
    private final AirLoadTable airLoads;
    private final SpatialSourceIngress sourceIngress;
    private final TopologyIngress topologyIngress;
    private final ThermalMemoryBudget airBudget;
    private ThermalMemoryBudget.Reservation airReservation;
    private AirBackingMeter backingMeter;
    private long backingInventoryRuns;

    private static final long BACKING_RECONCILE_BYTES = 1L << 20;
    private long fixedServiceBackingBytes, fixedUnreconciledBackingBytes, fixedBackingTick;

    private long lastBatchSequence;
    private boolean closed;
    private boolean closing, closeContinuationReady;
    private volatile boolean failureRecovery;

    private long transportThrough;

    public ThermalDimensionEngine(long dimensionGeneration, long initialTick, ThermalCellArena arena,
            ThermalSignatureTable signatures, MaterialBoundaryRegistry materials, ThermalTopologyParameters parameters,
            FarFieldSettings farField, MinecraftPhysicalSourceProfile campfireProfile,
            ThermalDimensionLimits limits, QueryPublication queries, ThermalMemoryBudget airBudget) {
        if (dimensionGeneration < 0L || initialTick < 0L) {
            throw new IllegalArgumentException("engine identity is invalid");
        }
        this.dimensionGeneration = dimensionGeneration;
        this.parameters = Objects.requireNonNull(parameters, "parameters");
        this.materialProfiles = Objects.requireNonNull(materials, "materials");
        this.limits = Objects.requireNonNull(limits, "limits");
        this.arena = Objects.requireNonNull(arena, "arena");
        this.queries = Objects.requireNonNull(queries, "queries");
        this.airBudget = Objects.requireNonNull(airBudget, "airBudget");
        fixedField = queries.fixedState();
        ThermalSignatureTable catalog = Objects.requireNonNull(
                signatures, "signatures");
        fixedInputs = new SpatialHeatStore();
        pages = new WorkerPageStore(limits.maximumPages(), fixedInputs);
        pages.setFixedField(fixedField);
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
        fixedHeat = new SpatialHeatBuffer(airBudget,
                Math.min(32, 3 * limits.maximumSources()), 3 * limits.maximumSources(), initialTick);
        {
            fixedInputCredit = airBudget.reserveCompletion(32768);
            fixedSignatures = new int[4096]; fixedMixing = new AirMixingRegion();
        }
        phases = new PhaseTransitionRuntime(arena, this, parameters.phaseRequestCapacity());
        pages.phaseMaintenance(phases::markMaterialDirty);
        phases.onMaterialControlChanged(this::materialControlChanged);
        topologyIngress = new TopologyIngress(airBudget);
        sourceIngress = new SpatialSourceIngress(sources, sourceBindings, this, arena, airLoads,
                this::sourceBindingLimit, this::sourceFrozen,
                () -> sourceFreezeRevision + structureCommits, this::sourceControlChanged);
        BrickTopologyCompiler compiler = new BrickTopologyCompiler(
                arena, catalog,
                Objects.requireNonNull(materials, "materials"),
                parameters,
                Objects.requireNonNull(farField, "farField"),
                limits.maximumArenaSlots(), sourceBindings::collectMixingSources);
        topologyPlan = new TopologyUpdatePlanner(
                pages,
                arena,
                this,
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

            sourceBindings.bindingGeometryCurrent(id -> sourceBindings.geometryCurrent(id,
                    section -> sourceGeometryTick(section) == Long.MAX_VALUE));
            // Prepaid coordinator metadata and a small startup checkpoint slot.
            geometryRetention = airBudget.reserveCompletion(4096 + 258L * 256);
        } catch (RuntimeException | Error failure) {
            try {

                fixedHeat.close();
                if (fixedInputCredit != null) fixedInputCredit.close();
                if (geometryRetention != null) geometryRetention.close();
                sources.close(); pages.close();
                if (airReservation != null) airReservation.close();
            } finally {
                if (backingMeter != null) backingMeter.close();
            }
            throw failure;
        }

    }

    @Override public SpatialHeatStore inputStore() { return fixedInputs; }

    @Override public void prepareSourceWindow(SpatialSourceTimeline.Window window, SpatialHeatInput.Target[] origins) {

        int routes = 0;
        for (var origin : origins) if (origin != null) routes++;
        long bytes = SourceInputWindow.storageBytes(window.size, origins.length, routes);
        fixedInputCredit.completeResize(Math.max(fixedInputCredit.bytes(), fixedInputs.retainedBytes() + bytes));
    }

    @Override public void prepareSpatialInput(int maximumRecords) {

        fixedInputCredit.completeResize(Math.max(fixedInputCredit.bytes(), fixedInputs.retainedBytes() + 4096L + 2304L * maximumRecords));
    }

    public void receiveSpatialHeat(SpatialHeatInput input) {
        for (int index = 0; index < input.size(); index++)
            receiveSpatialHeat(input.target(index), input.fromTick(index), input.toTick(index), input.energyJ(index));
    }

    @Override public void receiveSpatialHeat(SpatialHeatInput.Target target, long from, long to, double joules) {

        if ((!fixedField.idle() || fixedClaim != null) && inFixedScope(target)
                && (from < fixedSolveTick || from == to && from == fixedSolveTick)) cancelFixedStep();
        // Accepted J follows the world receiver, independently of the emitter or a numerical slot.
        var world = new SpatialHeatInput.Target(target.position(), target.face(), -1, 0,
                target.materialProfile(), target.incarnation(), 0, 0, target.materialStateId(),
                Math.max(target.minimumPageGeneration(), target.face() >= 0 ? target.generation() : 0));
        fixedInputs.receive(target.face() >= 6 ? 0 : 1, world, from, to, joules);
    }

    @Override public void sourceBound(long sourceId, LongSet sections) {
        inputDependencyRevision++;
    }

    private void materialControlChanged(int slot) {
        cancelFixedStep();
        if (arena.isLive(slot) && fixedField.read(materialPosition(slot), true, fixedMaterial))
            publishFixedMaterialControl(slot, fixedMaterial.enthalpyJ(), fixedMaterial.branch());
    }

    /** The claim still owns all J; these values are a private, cancellable numerical copy. */
    private void prepareFixedRecord(SpatialHeatInput.Target target, long from, long to, double joules) {
        if (joules == 0 || fixedCapacityWait >= 0) return;
        long position = target.position();
        int incarnation = -1;
        if (target.face() == SpatialHeatInput.MATERIAL) {
            if (!fixedField.read(position, true, fixedMaterial)) { fixedDiscardedJ += joules; return; }
            int profile = fixedField.signatureTable().materialProfileId(pages.signatureAtWorld(BlockPos.getX(position), BlockPos.getY(position), BlockPos.getZ(position)));
            int current = pages.materialSlot(position, profile);
            if (current < 0 || target.incarnation() != 0 && target.incarnation() != arena.materialIncarnation(current)
                    || target.materialStateId() >= 0 && target.materialStateId() != fixedMaterial.materialStateId()) {
                fixedDiscardedJ += joules; return;
            }
            incarnation = fixedMaterial.materialGeneration();
        }
        if (!fixedHeat.add(new SpatialHeatBuffer.Address(position, incarnation, target.face()), joules)) {
            fixedCapacityWait = airBudget.capacityRevision(); return;
        }
        fixedIncomingJ += joules;
    }

    private boolean sourceFrozen(SpatialSourceTimeline.Descriptor descriptor) {
        var snapshot = activeSnapshot;
        return snapshot != null && snapshot.inputsFrozen && sourceBindings.airPortsTouchChunk(descriptor, snapshot.chunk);
    }

    private void sourceControlChanged(SourceInputWindow window, LongSet mixing) {
        var cuts = new it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap<LongOpenHashSet>();
        for (long position : mixing) {
            long tick = Math.max(window.through(), fixedField.activeSince(sectionAt(position)));
            cuts.computeIfAbsent(tick, ignored -> new LongOpenHashSet()).add(position);
        }
        for (var cut : cuts.long2ObjectEntrySet()) topologyInputs.receiveMixing(dimensionGeneration, window.sequence,
                cut.getLongKey(), cut.getValue(), key -> { var page = pages.pageAt(key); return page == null ? null : page.handle; }, solver.windScale());
        inputDependencyRevision++;
    }

    private long sourceBindingLimit(SpatialSourceTimeline.Descriptor descriptor) {
        return sourceBindings.bindingLimit(descriptor, this::geometryBoundary, ignored -> Long.MAX_VALUE);
    }

    private long materialPosition(int slot) {
        return BlockPos.asLong(arena.minimum(slot, 0), arena.minimum(slot, 1), arena.minimum(slot, 2));
    }

    @Override public void readMaterial(int slot, MaterialSample target) {

        if (arena.isLive(slot) && fixedField.read(materialPosition(slot), true, fixedMaterial))
            target.setLive(fixedMaterial.enthalpyJ(), fixedMaterial.law(), fixedMaterial.branch(), fixedMaterial.sampleTick(), fixedMaterial.requestSequence());
        else arena.readMaterial(slot, target);
    }

    @Override public void applyMaterialControl(int slot, double enthalpyJ, byte branch) {

        cancelFixedStep();
        if (!publishFixedMaterialControl(slot, enthalpyJ, branch)) arena.applyMaterialControl(slot, enthalpyJ, branch);
    }

    private boolean publishFixedMaterialControl(int slot, double enthalpyJ, byte branch) {
        long position = materialPosition(slot);
        var page = pages.pageAt(SectionPos.asLong(BlockPos.getX(position) >> 4, BlockPos.getY(position) >> 4, BlockPos.getZ(position) >> 4));
        var geometry = page == null ? null : page.handle.lastPublication();
        if (geometry == null || !fixedField.read(position, true, fixedMaterial)) return false;
        int block = (BlockPos.getX(position) & 15) | (BlockPos.getZ(position) & 15) << 4 | (BlockPos.getY(position) & 15) << 8;
        return queries.publishFixedMaterialControl(geometry, block, fixedMaterial.materialGeneration(), enthalpyJ, branch,
                arena.materialTransitionWaiting(slot), arena.hasMaterialTransition(slot) ? arena.phaseRequestSequence(slot) : 0);
    }

    private void cancelFixedStep() {
        synchronized (inputStore()) {
            if (fixedField.idle()) fixedHeat.discardPreparation();
            else fixedField.cancel();
            if (fixedClaim != null) { fixedClaim.close(); fixedClaim = null; }
            fixedInputs.advanceCleanup(128);
            fixedIncomingJ = fixedDiscardedJ = 0;
            fixedScope = LongSets.EMPTY_SET;
        }
    }

    private boolean fixedHasReadyWork() {
        if (failureRecovery) return snapshotCommandsReady();
        if (snapshotCommandsReady() || fixedBackingReconciliationDue() || restoreReady()) return true;
        if (boundarySequence != offeredSequence || !fixedField.idle() || fixedClaim != null || fixedInputs.hasCleanup() || fixedInputs.hasReadyRouting()
                || phases.hasReadyRequests() || topologyIngress.hasWork()) return true;
        if (fixedBoundaryWorkPending()) return true;
        if (fixedActivationReady()) return true;
        if (fixedGeometry != null && fixedGeometry.ready()) return true;
        if (fixedGeometry != null && !fixedGeometry.ready() && nextFixedMaintenanceInput() != null) return true;
        if (sourceIngress.hasReadyWork()) return true;
        if (fixedGeometry == null && (pages.maintenanceReady(airBudget.capacityRevision()) || nextFixedGeometryInput() != null)) return true;
        return fixedCapacityWait != airBudget.capacityRevision() && nextFixedDomain() != null;
    }
    private boolean fixedBoundaryWorkPending() {
        return fixedBoundaryRevision != topologyInputs.revision() || fixedBoundaryStructure != structureCommits
                || fixedControlRevision != sourceIngress.controlRevision() || fixedBoundarySequence != boundarySequence
                || fixedActivationRevision != fixedField.activationRevision() || fixedField.idle() && fixedField.domainsDirty();
    }

    private static long sectionAt(long position) {
        return SectionPos.asLong(BlockPos.getX(position) >> 4, BlockPos.getY(position) >> 4, BlockPos.getZ(position) >> 4);
    }
    private boolean inFixedScope(SpatialHeatInput.Target target) { return fixedScope.contains(sectionAt(target.position())); }
    private boolean fixedPending(LongSet sections, long tick) {
        return fixedInputs.pendingThrough(1, tick, target -> sections.contains(sectionAt(target.position())));
    }
    /** Choose real committed values. A disconnected receiver with missing input cannot stop another base. */
    private TiledThermalField.Domain nextFixedDomain() {
        TiledThermalField.Domain selected = null;
        long oldest = Long.MAX_VALUE;
        for (var domain : fixedField.domains()) {
            long from = fixedField.minimumTick(domain.sections());
            if (from >= oldest || !fixedField.aligned(domain.sections())) continue;
            long through = fixedNumericalThrough(domain.sections(), from);
            if (through > from || fixedPending(domain.sections(), from)) { selected = domain; oldest = from; }
        }
        return selected;
    }
    private boolean fixedHasPendingNumerics() {
        for (var domain : fixedField.domains()) if (fixedField.minimumTick(domain.sections()) < offeredThrough) return true;
        return false;
    }
    /** Disconnected domains with the same real interval share one original Store transaction. */
    private LongSet fixedNumericalScope(TiledThermalField.Domain first) {
        long from = fixedField.minimumTick(first.sections()), through = fixedNumericalThrough(first.sections(), from);
        var matching = new java.util.ArrayList<TiledThermalField.Domain>();
        var bounds = new it.unimi.dsi.fastutil.ints.IntArrayList();
        int firstBound = fixedInputBound(first.sections());
        long wanted = fixedHeat.targetCount() + (long) firstBound;
        for (var domain : fixedField.domains()) if (domain != first && fixedField.atBoundary(domain.sections(), from)
                && fixedNumericalThrough(domain.sections(), from) == through) {
            int bound = fixedInputBound(domain.sections()); matching.add(domain); bounds.add(bound); wanted += bound;
        }
        if (matching.isEmpty()) return first.sections();
        int capacity = fixedHeat.prepareTargets((int) Math.min(Integer.MAX_VALUE, wanted));
        LongOpenHashSet combined = null;
        long used = fixedHeat.targetCount() + (long) firstBound;
        for (int index = 0; index < matching.size(); index++) if (used + bounds.getInt(index) <= capacity) {
            if (combined == null) combined = new LongOpenHashSet(first.sections());
            combined.addAll(matching.get(index).sections()); used += bounds.getInt(index);
        }
        return combined == null ? first.sections() : combined;
    }
    private int fixedInputBound(LongSet sections) {
        var chunks = new LongOpenHashSet();
        int targets = 0;
        for (long section : sections) if (chunks.add(chunkForSection(section))) targets += fixedInputs.targetsInChunk(chunkForSection(section));
        return targets;
    }
    /** Readiness and execution share every physical/input fence of the selected geometry domain. */
    private long fixedNumericalThrough(LongSet sections, long from) {
        long through = Math.min(transportThrough, from + ThermalInputBatch.CUT_INTERVAL_TICKS);
        for (long section : sections) through = Math.min(through, fixedPhysicalCuts.getOrDefault(section, Long.MAX_VALUE));
        through = Math.min(through, sourceIngress.firstPendingTick(sections, section -> pages.capturedBrickMask(section) != 0));
        return Math.max(from, through);
    }

    private void processFixedRound() {
        long started = System.nanoTime(), deadline = started + 2_000_000L;
        if (capacityContinuation != null) { airBudget.cancelCapacityWait(capacityContinuation); capacityContinuation = null; }
        if (processSnapshotCommand(deadline) || failureRecovery) return;
        if (fixedBackingReconciliationDue()) {
            if (fixedGeometry == null) { reserveAirBacking(); return; }
            // Reconcile installed owners after returning a capacity-blocked
            // private candidate. Its arrays remain paid by the task meanwhile.
            if (fixedGeometry.capacityWait >= 0 && fixedField.idle()) {
                backingNeedsReclaim = true;
                fixedGeometry.returningInputs = true;
                if (fixedGeometry.advance(deadline)) { fixedGeometry.close(); fixedGeometry = null; }
                return;
            }
        }
        // One bounded restore fragment shares this round with ordinary work.
        // A busy trial keeps its input cutoff; archive replay starts at the next cut.
        if (receiveRestore() && System.nanoTime() >= deadline) return;
        if (fixedInputs.hasCleanup()) {
            fixedInputs.advanceCleanup(128);
            if (!fixedInputs.hasCleanup()) fixedInputCredit.tryResize(32768L + fixedInputs.retainedBytes());
            return;
        }
        if (fixedField.idle() && fixedGeometry == null && pages.maintenanceReady(airBudget.capacityRevision())) {
            pages.advanceMaintenance(arena, parameters.referenceTemperatureC(), deadline, airBudget.capacityRevision(),
                    update -> residencyEffects.put(update.sectionKey(), update));
            if (System.nanoTime() >= deadline) return;
        }
        if (fixedGeometry != null && (fixedField.idle() || fixedGeometry.stage == 2) && fixedGeometry.ready()) {
            if (fixedGeometry.advance(deadline)) { fixedGeometry.close(); fixedGeometry = null; }
            return;
        }
        var fragment = topologyIngress.poll();
        while (fragment != null) {
            topologyInputs.receive(fragment, key -> { var page = pages.pageAt(key); return page == null ? null : page.handle; });
            lastBatchSequence = Math.max(lastBatchSequence, fragment.payload().sequence());
            if (System.nanoTime() >= deadline) return;
            fragment = topologyIngress.poll();
        }
        // An offered envelope becomes a usable prefix only after its geometry
        // references have crossed the same handoff. Main may offer again after
        // this short snapshot; that later envelope belongs to the next round.
        synchronized (intakeLock) {
            if (!topologyIngress.hasWork()) { transportThrough = offeredThrough; boundarySequence = offeredSequence; }
        }
        refreshFixedBoundaries();
        if (fixedField.idle() && activateFixedSections()) return;
        sourceIngress.advance(deadline);
        if (System.nanoTime() >= deadline) return;
        if (fixedField.idle()) {
            topologyInputs.collapseUninstalledRetirements(fixedField::hasSection, transportThrough);
            refreshFixedBoundaries();
        }
        if (fixedGeometry != null && fixedField.idle() && !fixedGeometry.ready() && nextFixedMaintenanceInput() != null) {
            fixedGeometry.returningInputs = true;
            if (fixedGeometry.advance(deadline)) { fixedGeometry.close(); fixedGeometry = null; }
            return;
        }
        fixedInputs.route(fixedRouting, 32);
        if (fixedInputs.hasReadyRouting() || System.nanoTime() >= deadline) return;
        if (!fixedField.idle() || fixedClaim != null) for (long section : fixedScope)
            if (fixedPhysicalCuts.getOrDefault(section, Long.MAX_VALUE) < fixedSolveTick) { cancelFixedStep(); break; }
        var selected = fixedGeometry == null && fixedField.idle() ? nextFixedGeometryInput() : null;
        if (selected != null) {
            if (fixedInputObsolete(selected)) { completeObsoleteFixedInput(selected); return; }
            if (fixedClaim != null) cancelFixedStep();
            if (applyFixedMixing(selected)) return;
            fixedGeometry = new FixedGeometry(selected);
            if (fixedGeometry.advance(deadline)) { fixedGeometry.close(); fixedGeometry = null; }
            return;
        }
        if (!fixedField.idle()) {
            if (fixedField.advance(deadline)) {
                synchronized (fixedInputs) {
                    if (!queries.publishFixed(new QueryPublication.SectionPublication[0], this::acceptFixedHeat))
                        throw new IllegalStateException("Completed fixed state was not published");
                }
                fixedCommits++; phases.collectMaterialRequests();
            }
            return;
        }
        phases.advanceMaterialRequests(32);
        if (fixedCapacityWait == airBudget.capacityRevision()) return;
        if (fixedClaim == null) {
            var domain = nextFixedDomain();
            if (domain == null) return;
            fixedScope = fixedNumericalScope(domain);
            fixedFromTick = fixedField.minimumTick(fixedScope);
            fixedSolveTick = fixedNumericalThrough(fixedScope, fixedFromTick);
        }
        fixedCapacityWait = -1;
        if (!Double.isNaN(offeredWind)) fixedField.windScale(offeredWind);
        if (!prepareFixedHeat(fixedScope, fixedFromTick, fixedSolveTick, deadline)) return;
        if (!fixedField.begin(fixedHeat.seal(fixedFromTick, fixedSolveTick, position -> fixedScope.contains(sectionAt(position))), fixedScope))
            throw new IllegalStateException("Fixed input did not match its recipient's committed interval");
        lastIntakeNanos = System.nanoTime() - started;
    }

    private boolean fixedMaintenanceInput(TopologyInbox.Input input) {
        if (fixedInputObsolete(input)) return true;
        long section = input.page().sectionKey();
        boolean installed = fixedField.hasSection(section);
        if (installed && input.batch().admissions().length == 0 && !fixedGrowthOnly(input)
                && (RegionalContinuationBoundary.hasStructuralInput(input.batch()) || input.batch().environmentUpdates().length != 0
                    || !input.mixing().isEmpty())) return true;
        long current = fixedField.activeBricks(section);
        for (var admission : input.batch().admissions()) if (!installed || (admission.residentBrickMask() & ~current) != 0) return false;
        for (var update : input.batch().residencyUpdates()) if (!installed || (update.residentBrickMask() & ~current) != 0) return false;
        return installed || input.batch().retirements().length != 0;
    }
    private TopologyInbox.Input nextFixedMaintenanceInput() {
        return topologyInputs.earliestSchedulableInput(input -> fixedMaintenanceInput(input) && fixedGeometryReady(input), this::fixedGrowthOnly);
    }
    private TopologyInbox.Input nextFixedGeometryInput() {
        var maintenance = nextFixedMaintenanceInput();
        if (maintenance != null) return maintenance;
        return topologyInputs.earliestSchedulableInput(input -> {
            if (!fixedGeometryReady(input)) return false;
            int additions = 0;
            for (var admission : input.batch().admissions()) if (pages.pageAt(admission.page().sectionKey()) == null) additions++;
            return pages.activePageCount() + additions <= limits.maximumPages();
        }, this::fixedGrowthOnly);
    }

    private boolean fixedGeometryReady(TopologyInbox.Input input) {
        if (input.batch().targetTick() > transportThrough) return false;
        if (fixedInputObsolete(input)) return true;
        var boundary = fixedBoundaries.get(input);
        if (boundary == null) return false;
        if (!fixedGrowthOnly(input)) return fixedField.atBoundary(boundary.sections(), fixedApplicationTick(input));
        if (fixedField.frozen(input.page().sectionKey())) return false;
        long tick = fixedField.minimumTick(boundary.sections());
        return fixedField.aligned(boundary.sections()) && (tick == Long.MAX_VALUE || tick >= input.batch().targetTick());
    }

    private boolean fixedInputObsolete(TopologyInbox.Input input) {
        var page = pages.pageAt(input.page().sectionKey());
        if (page != null && page.handle == input.page()) return false;
        for (var admission : input.batch().admissions()) if (admission.page() == input.page()
                && (page == null || admission.page().lifecycleGeneration() > page.handle.lifecycleGeneration())) return false;
        return true;
    }
    /** Controls before an active lifetime initialize it; ordinary running history keeps the original cut. */
    private long fixedApplicationTick(TopologyInbox.Input input) {
        long tick = input.batch().targetTick();
        var boundary = fixedBoundaries.get(input);
        if (boundary == null) return tick;
        if (fixedGrowthOnly(input)) {
            long current = fixedField.minimumTick(boundary.sections());
            return current == Long.MAX_VALUE ? tick : Math.max(tick, current);
        }
        for (long section : boundary.sections()) tick = Math.max(tick, fixedField.activeSince(section));
        return tick;
    }
    private void completeObsoleteFixedInput(TopologyInbox.Input input) {
        var completed = topologyInputs.freeze(input.batch().targetTick(), input.batch().sequence(),
                candidate -> candidate == input, this::fixedGrowthOnly);
        topologyInputs.complete(completed);
        long section = input.page().sectionKey(); retireInputSlot(section); refreshChunkInputHold(chunkForSection(section));
        sourceBindings.rebindDirty(sources, id -> sourceBindings.geometryCurrent(id,
                key -> sourceGeometryTick(key) == Long.MAX_VALUE));
        sourceIngress.dependenciesChanged(); fixedInputs.geometryChanged();
    }

    /** Cache geometry influence by input identity; controls only add cheap Section cutoffs to that closure. */
    private void refreshFixedBoundaries() {
        long revision = topologyInputs.revision(), sequence = boundarySequence;
        boolean membershipChanged = fixedField.idle() && fixedField.domainsDirty();
        if (membershipChanged) fixedField.domains();
        if (fixedBoundaryStructure != structureCommits || membershipChanged) {
            fixedBoundaries.clear(); fixedActivationBoundaries.clear(); fixedBoundaryRevision = -1;
            fixedBoundaryStructure = structureCommits;
        }
        if (fixedBoundaryRevision != revision) {
            fixedActivationBoundaries.clear();
            var retained = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<TopologyInbox.Input, Boolean>());
            topologyInputs.forEachPendingInput(input -> {
                retained.add(input);
                if (fixedBoundaries.containsKey(input)) return;
                var seeds = new LongOpenHashSet();
                TopologyUpdatePlanner.collectBoundarySeeds(input.batch(), seeds); seeds.add(input.page().sectionKey());
                var boundary = RegionalContinuationBoundary.hasStructuralInput(input.batch()) || topologyPlan.hasPendingRoutes()
                        ? topologyPlan.continuationBoundary(seeds, true)
                        : RegionalContinuationBoundary.UNCHANGED.including(seeds);
                fixedBoundaries.put(input, includeFixedOutlets(boundary));
            });
            fixedBoundaries.keySet().retainAll(retained);
            fixedBoundaryRevision = revision; fixedControlRevision = -1;
        }
        long controls = sourceIngress.controlRevision();
        if (fixedControlRevision == controls && fixedBoundarySequence == sequence
                && fixedActivationRevision == fixedField.activationRevision()) return;
        fixedNextPhysicalCuts.clear();
        topologyInputs.forEachPendingInput(input -> {
            if (!fixedInputObsolete(input) && !fixedGrowthOnly(input))
                addFixedCut(fixedBoundaries.get(input).sections(), fixedApplicationTick(input));
        });
        var footprint = new LongOpenHashSet();
        sourceIngress.forEachPendingControl((source, tick, sourceSequence) -> {
            footprint.clear(); sourceBindings.collectMixingFootprint(source, footprint);
            for (long position : footprint) addFixedCut(sectionAt(position), tick);
            addFixedCut(source.section(), tick);
            for (int port = 0; port < source.ports().length; port++) {
                var origin = sourceBindings.originalPort(source, port);
                if (origin != null) addFixedCut(sectionAt(origin.position()), tick);
            }
        });
        var activating = new LongOpenHashSet();
        fixedField.forEachPendingActivation((section, tick) -> {
            activating.add(section); addFixedCut(fixedActivationBoundary(section).sections(), tick);
        });
        fixedActivationBoundaries.keySet().retainAll(activating);
        boolean changed = !sameFixedCuts(fixedNextPhysicalCuts, fixedPhysicalCuts);
        var previous = fixedPhysicalCuts; fixedPhysicalCuts = fixedNextPhysicalCuts; fixedNextPhysicalCuts = previous;
        fixedNextPhysicalCuts.clear();
        if (changed) { sourceIngress.dependenciesChanged(); fixedInputs.geometryChanged(); }
        fixedControlRevision = sourceIngress.controlRevision(); fixedBoundarySequence = sequence;
        fixedActivationRevision = fixedField.activationRevision();
    }
    private void addFixedCut(LongSet sections, long tick) { for (long section : sections) addFixedCut(section, tick); }
    private void addFixedCut(long section, long tick) {
        fixedNextPhysicalCuts.put(section, Math.min(tick, fixedNextPhysicalCuts.getOrDefault(section, Long.MAX_VALUE)));
    }
    private static boolean sameFixedCuts(it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap first,
            it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap second) {
        if (first.size() != second.size()) return false;
        var entries = it.unimi.dsi.fastutil.longs.Long2LongMaps.fastIterator(first);
        while (entries.hasNext()) {
            var entry = entries.next();
            if (second.getOrDefault(entry.getLongKey(), Long.MAX_VALUE) != entry.getLongValue()) return false;
        }
        return true;
    }
    private RegionalContinuationBoundary includeFixedOutlets(RegionalContinuationBoundary boundary) {
        var outlets = new LongOpenHashSet();
        sourceIngress.forEachIndexedSource(boundary.sections(), id -> sourceBindings.collectBoundOutlets(id, boundary, outlets));
        return boundary.including(outlets);
    }
    private RegionalContinuationBoundary fixedActivationBoundary(long section) {
        return fixedActivationBoundaries.computeIfAbsent(section, key -> {
            var pending = new LongOpenHashSet();
            topologyInputs.forEachPendingInput(input -> {
                if (!fixedInputObsolete(input) && !fixedGrowthOnly(input)) TopologyUpdatePlanner.collectBoundarySeeds(input.batch(), pending);
            });
            if (fixedGeometry != null) pending.addAll(fixedGeometry.boundary.sections());
            return includeFixedOutlets(topologyPlan.continuationBoundary(LongSets.singleton(key), true, pending));
        });
    }
    private void stageFixedReentry(long section, long requestedTick, double ambientC, double halfLife) {
        fixedField.stageThaw(section, ambientC, halfLife, Math.max(requestedTick, fixedField.latestTick()));
    }
    private boolean fixedActivationReady() {
        if (fixedActivationBoundaries.isEmpty()) return false;
        for (var entry : fixedActivationBoundaries.long2ObjectEntrySet()) {
            long tick = fixedField.pendingActivationTick(entry.getLongKey());
            if (tick != Long.MIN_VALUE && fixedField.atBoundary(entry.getValue().sections(), tick)) return true;
        }
        return false;
    }
    private boolean activateFixedSections() {
        if (fixedActivationBoundaries.isEmpty()) return false;
        boolean changed = false;
        for (var entry : fixedActivationBoundaries.long2ObjectEntrySet())
            changed |= fixedField.activate(entry.getLongKey(), entry.getValue().sections());
        if (changed) { structureCommits++; sourceIngress.dependenciesChanged(); fixedInputs.geometryChanged(); }
        return changed;
    }

    /** Numerical expansion is optional storage, not a change to already represented world physics. */
    private boolean fixedGrowthOnly(TopologyInbox.Input input) {
        var batch = input.batch();
        if (!input.mixing().isEmpty() || batch.residencyUpdates().length == 0 || batch.admissions().length != 0
                || batch.retirements().length != 0 || !batch.geometry().isEmpty() || batch.environmentUpdates().length != 0
                || batch.phaseAcks().length != 0 || batch.phaseIntents().length != 0) return false;
        for (var update : batch.residencyUpdates()) if (!pages.onlyAddsResidentBricks(update)) return false;
        return true;
    }

    private long sourceGeometryTick(long section) {
        return topologyInputs.firstTick(section, input -> !fixedGrowthOnly(input));
    }

    /** One Page owns at most 64 mixing Bricks; retain its original control boundary and queue order. */
    private boolean applyFixedMixing(TopologyInbox.Input selected) {
        var input = selected.batch();
        long tick = fixedApplicationTick(selected);
        long section = selected.page().sectionKey();
        if (RegionalContinuationBoundary.hasStructuralInput(input) || input.environmentUpdates().length != 0
                || topologyPlan.hasPendingRoutes() || !fixedField.hasSection(section)) return false;
        var inputs = topologyInputs.freeze(input.targetTick(), input.sequence(),
                value -> value.page().sectionKey() == section, this::fixedGrowthOnly);
        long revision = sourceBindings.mixingRevision(), active = fixedField.activeBricks(section);
        int updated = 0;
        // Route discovery depends on captured geometry, not source power. Updating
        // these coefficients neither changes a route nor advances physical time.
        for (long position : selected.mixing()) {
            int x = BlockPos.getX(position), y = BlockPos.getY(position), z = BlockPos.getZ(position);
            int brick = (x & 15) >>> 2 | ((z & 15) >>> 2) << 2 | ((y & 15) >>> 2) << 4;
            if ((active & 1L << brick) == 0) continue;
            fixedMixing.reset(x, y, z); sourceBindings.collectMixingSources(fixedMixing, tick);
            fixedField.replaceMixing(section, brick, fixedMixing); updated++;
        }
        sourceBindings.mixingCommitted(revision, selected.mixing());
        topologyInputs.complete(inputs);
        retireInputSlot(section); refreshChunkInputHold(chunkForSection(section));
        sourceBindings.rebindDirty(sources, id -> sourceBindings.geometryCurrent(id,
                key -> sourceGeometryTick(key) == Long.MAX_VALUE));
        sourceIngress.dependenciesChanged(); fixedInputs.geometryChanged();
        var observer = com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer;
        if (observer != null) observer.mixingUpdated(tick == input.targetTick() ? input
                : TopologyInbox.combine(inputs, input.sequence(), tick), updated);
        return true;
    }

    private boolean prepareFixedHeat(LongSet scope, long from, long through, long deadline) {
        if (fixedClaim == null) {
            fixedScope = scope; fixedFromTick = from;
            fixedHeat.prepareInterval(from);
            fixedClaim = fixedInputs.claim(1, fixedFromTick, through, this::inFixedScope, this::prepareFixedRecord);
            fixedSolveTick = through;
        }
        boolean prepared = fixedClaim.prepare(deadline);
        if (fixedCapacityWait >= 0) {
            cancelFixedStep();
            fixedCapacityWait = airBudget.capacityRevision();
            return false;
        }
        return prepared;
    }

    /** The claim still owns all J; these values are a private, cancellable numerical copy. */

    private final WorkerPageStore.MutableAirTarget fixedAirTarget = new WorkerPageStore.MutableAirTarget();
    private int resolveFixedRoute(SpatialHeatInput.Target target) {
        long position = target.position();
        var page = pages.pageAt(SectionPos.asLong(BlockPos.getX(position) >> 4, BlockPos.getY(position) >> 4, BlockPos.getZ(position) >> 4));
        if (page == null || page.handle.lifecycleGeneration() < target.minimumPageGeneration()) return WorkerPageStore.PORT_TOPOLOGY_UNAVAILABLE;
        int slot = pages.resolveAirFaceTarget(BlockPos.getX(position), BlockPos.getY(position), BlockPos.getZ(position),
                com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockFace.fromOrdinal(target.face() - 6), fixedField.signatureTable(), fixedAirTarget);
        if (slot >= 0) {
            long section = SectionPos.asLong(fixedAirTarget.blockX() >> 4, fixedAirTarget.blockY() >> 4, fixedAirTarget.blockZ() >> 4);
            if (!fixedField.canAdvance(section)) return WorkerPageStore.PORT_TOPOLOGY_UNAVAILABLE;
        }
        return slot;
    }
    private final SpatialHeatStore.Receiver fixedRouting = new SpatialHeatStore.Receiver() {
        @Override public long availableThrough(SpatialHeatInput.Target target) {
            return resolveFixedRoute(target) == WorkerPageStore.PORT_TOPOLOGY_UNAVAILABLE ? -1
                    : Math.min(transportThrough, geometryBoundary(SectionPos.asLong(BlockPos.getX(target.position()) >> 4,
                            BlockPos.getY(target.position()) >> 4, BlockPos.getZ(target.position()) >> 4)));
        }
        @Override public boolean accept(SpatialHeatInput.Target target, long from, long to, double joules) {
            int slot = resolveFixedRoute(target);
            if (slot == WorkerPageStore.PORT_TOPOLOGY_UNAVAILABLE) return false;
            if (slot < 0) { sources.confirmRecordedLoss(joules, false); return true; }
            var world = new SpatialHeatInput.Target(BlockPos.asLong(fixedAirTarget.blockX(), fixedAirTarget.blockY(), fixedAirTarget.blockZ()),
                    (byte) fixedAirTarget.face().ordinal(), -1, 0, 0, 0, 0, 0, -1, fixedAirTarget.generation());
            if ((!fixedField.idle() || fixedClaim != null) && inFixedScope(world)
                    && (from < fixedSolveTick || from == to && from == fixedSolveTick)) cancelFixedStep();
            prepareSpatialInput(1); fixedInputs.receive(1, world, from, to, joules);
            return true;
        }
    };

    /** Runs inside the value/input publication monitor. The Store claim and deferred heat change owners together. */
    private void acceptFixedHeat() {
        if (fixedClaim != null) { fixedClaim.accepted(); fixedClaim = null; }
        double pending = fixedHeat.committedPendingJoules(), rejected = fixedField.rejectedInputJ();
        sources.confirmRecordedDelivery(fixedIncomingJ + fixedPendingJ - pending - (rejected - fixedRejectedJ),
                fixedDiscardedJ + rejected - fixedRejectedJ);
        fixedPendingJ = pending; fixedRejectedJ = rejected; fixedIncomingJ = fixedDiscardedJ = 0;
        minimumRegionTick = fixedField.acceptedTick();
        pages.markAcceptedMaintenance();
        publishedPendingJ = fixedInputs.pendingEnergyJ() + pending;
        fixedScope = LongSets.EMPTY_SET;
        sourceIngress.dependenciesChanged();
    }

    /** One existing-engine geometry task; it prepares local topology and never creates regional numerical state. */
    private final class FixedGeometry implements AutoCloseable {
        final java.util.List<TopologyInbox.Input> inputs;
        final ThermalInputBatch input;
        final RegionalContinuationBoundary boundary;
        final TopologyUpdatePlanner planner;
        final ThermalMemoryBudget.Reservation credit;
        ThermalMemoryBudget.Reservation fieldStorage;
        final LongOpenHashSet mixing = new LongOpenHashSet();
        PreparedTopologyChange prepared;
        com.teammoeg.frostedheart.content.climate.thermal.topology.RegionalTopologyInstall installation;
        QueryPublication.SectionPublication[] headers;
        long[] restoringBricks;
        long publicationTick;
        long capacityWait = -1;
        int stage;
        boolean returningInputs;
        boolean ready() {
            if (returningInputs || stage >= 2) return true;
            if (capacityWait == airBudget.capacityRevision()) return false;
            for (var selected : inputs) if (!fixedGeometryReady(selected)) return false;
            return true;
        }
        FixedGeometry(TopologyInbox.Input first) {
            int[] available = {limits.maximumPages() - pages.activePageCount()};
            boolean maintenance = fixedMaintenanceInput(first);
            long applicationTick = fixedApplicationTick(first);
            publicationTick = applicationTick;
            // Preserve a producer cut, but only include recipients at its real boundary.
            inputs = topologyInputs.freeze(first.batch().targetTick(), first.batch().sequence(), selected -> {
                if (!fixedGeometryReady(selected)) return false;
                if (fixedInputObsolete(selected) || fixedApplicationTick(selected) != applicationTick) return false;
                // A full-capacity growth must not hold unrelated retirement or
                // edits hostage; maintenance retains its existing priority.
                if (maintenance && !fixedMaintenanceInput(selected)) return false;
                int additions = 0;
                for (var admission : selected.batch().admissions()) if (pages.pageAt(admission.page().sectionKey()) == null) additions++;
                if (additions > available[0]) return false;
                available[0] -= additions; return true;
            }, ThermalDimensionEngine.this::fixedGrowthOnly);
            topologyInputs.separateFrozenGrowth(inputs, fixedField::activeBricks, ThermalDimensionEngine.this::fixedGrowthOnly);
            var seeds = new LongOpenHashSet();
            for (var selected : inputs) {
                TopologyUpdatePlanner.collectBoundarySeeds(selected.batch(), seeds);
                seeds.add(selected.page().sectionKey());
            }
            boundary = topologyPlan.continuationBoundary(seeds, true);
            input = TopologyInbox.combine(inputs, first.batch().sequence(), applicationTick);
            for (var selected : inputs) mixing.addAll(selected.mixing());
            credit = airBudget.reserveCompletion(32768L + topologyPlan.preparationScratchBytes(input));
            planner = topologyPlan.forFixedInput(boundary.sections()); planner.beginCut();
            for (var ack : input.phaseAcks()) phases.applyAck(ack.request(), ack.outcome());
            for (var intent : input.phaseIntents()) {
                int slot = pages.materialSlot(intent.page(), intent.blockIndex(), intent.sourceStateId());
                if (slot >= 0) phases.forceMaterialTransition(slot, intent.branch());
            }
            pages.awaitChangedMaterials(input, arena, ThermalDimensionEngine.this::materialControlChanged);
        }
        boolean advance(long deadline) {
            if (stage == 5) return true;
            if (!ready()) return false;
            if (returningInputs) {
                if (installation != null) {
                    installation.cancel();
                    if (!installation.cleanup(deadline)) return false;
                }
                planner.discardPrepared(); topologyInputs.returnFrozen(inputs, ThermalDimensionEngine.this::fixedGrowthOnly); stage = 5;
                return true;
            }
            if (stage == 0) {
                observe("TOPOLOGY", false);
                prepared = planner.prepareRound(input, mixing, deadline, credit);
                if (prepared == null) return false;
                // prepareRound just preflighted this change. The sole fixed geometry writer has no intervening topology commit.
                restoringBricks = new long[prepared.pageWrites.length];
                for (int index = 0; index < restoringBricks.length; index++) {
                    var write = prepared.pageWrites[index];
                    long previous = write.replacesLifecycle() ? 0 : fixedField.activeBricks(write.page.handle.sectionKey());
                    restoringBricks[index] = write.retirement ? 0 : write.capturedBricks() & ~previous;
                }
                installation = new com.teammoeg.frostedheart.content.climate.thermal.topology.RegionalTopologyInstall(prepared, pages, arena, solver, phases);
                stage = 1;
            }
            if (stage == 1) {
                if (!installation.prepare(deadline)) return false;
                if (!installation.valid()) throw new IllegalStateException("Fixed topology changed while its sole coordinator prepared it");
                publicationTick = fixedApplicationTick(inputs.get(0));
                if (!prepareField()) { capacityWait = airBudget.capacityRevision(); return false; }
                if (!beginGeometryValues()) throw new IllegalStateException("Geometry did not own its recipients' numerical boundary");
                prepareGameplayValues();
                stage = 2;
            }
            if (stage == 2) {
                // Native UNLOAD can cancel the private zero-time step while copying Accepted.
                // The staged topology still owns its publication and resumes at the same clock.
                if (fixedField.idle()) {
                    if (!beginGeometryValues())
                        throw new IllegalStateException("Cancelled fixed geometry could not resume its numerical boundary");
                    prepareGameplayValues();
                }
                if (!fixedField.advance(deadline)) return false;
                restoreNativeValues();
                observe("BEFORE_PUBLICATION", false);
                synchronized (fixedInputs) {
                    if (!queries.publishFixed(headers, () -> {
                        installation.commit(); planner.committed(); structureCommits++;
                        for (var write : prepared.pageWrites) if (write.retirement) fixedField.retire(write.page.handle.sectionKey());
                        acceptFixedHeat();
                    })) throw new IllegalStateException("Prepared fixed geometry was not published");
                }
                fixedCommits++; stage = 3;
                observe("PUBLISHED", false);
            }
            if (stage == 3) {
                for (var write : prepared.pageWrites) if (!write.retirement) {
                    if (!copyTransport(write.page.handle.sectionKey(), fixedField, fieldStorage))
                        throw new IllegalStateException("Prepaid fixed transport lost its Section");
                    refreshFixedMixing(write.page.handle.sectionKey());
                }
                sourceBindings.markCommittedSections(prepared.sourceDirtySections);
                sourceBindings.mixingCommitted(sourceBindings.mixingRevision(), mixing);
                stage = 4;
            }
            if (!installation.cleanup(deadline)) return false;
            topologyInputs.complete(inputs);
            for (var selected : inputs) {
                long section = selected.page().sectionKey(); retireInputSlot(section); refreshChunkInputHold(chunkForSection(section));
            }
            sourceBindings.rebindDirty(sources, id -> sourceBindings.geometryCurrent(id,
                    section -> sourceGeometryTick(section) == Long.MAX_VALUE));
            sourceIngress.dependenciesChanged();
            fixedInputs.geometryChanged();
            for (var write : prepared.pageWrites) if (!write.retirement)
                write.page.handle.notePublishedGeometry(write.publication.geometryRevision(), publicationTick);
            for (var token : prepared.committedResyncTokens) resyncEffects.put(token.sectionKey(), token);
            handoffFixedGeometryBacking(this);
            fixedInputs.advanceCleanup(128); phases.collectMaterialRequests();
            stage = 5;
            observe("CLEANUP", true);
            return true;
        }
        private void observe(String phase, boolean complete) {
            var observer = com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer;
            if (observer != null) observer.geometry(this, input, boundary, phase, complete);
        }
        private boolean prepareField() {
            long bytes = 0;
            int additions = 0, retirements = 0;
            boolean completingExisting = true;
            for (var write : prepared.pageWrites) {
                long section = write.page.handle.sectionKey();
                if (write.retirement) { if (fixedField.hasSection(section)) retirements++; continue; }
                if (!fixedField.hasSection(section)) additions++;
                if (!fixedField.hasSection(section) || (write.capturedBricks() & ~fixedField.activeBricks(section)) != 0) completingExisting = false;
                bytes += fixedField.geometryStorageBytes(section, write.capturedBricks(), write.replacesLifecycle());
                var fragments = new ThermalFragment[64];
                for (int brick = 0; brick < 64; brick++) {
                    int slot = write.publication.workerPageSlot() * 64 + brick;
                    int changed = java.util.Arrays.binarySearch(prepared.fragmentIndexes(), slot);
                    fragments[brick] = changed >= 0 ? prepared.fragments()[changed] : solver.fragment(slot);
                }
                bytes += TiledThermalField.transportBytes(fragments);
            }
            fieldStorage = fixedField.reserveGeometryStorage(bytes, additions, retirements, completingExisting);
            if (fieldStorage == null) return false;
            var changed = new java.util.ArrayList<QueryPublication.SectionPublication>();
            for (var write : prepared.pageWrites) {
                if (write.retirement) continue;
                long section = write.page.handle.sectionKey(), mask = write.capturedBricks();
                for (int block = 0; block < 4096; block++) fixedSignatures[block] = write.signatures().get(block);
                if (write.replacesLifecycle() && fixedField.hasSection(section)) fixedField.retire(section);
                boolean existing = fixedField.hasSection(section);
                if (!existing && !fixedField.admit(section, fixedSignatures, mask, write.naturalTemperatureC(), publicationTick, fieldStorage)
                        || existing && !fixedField.grow(section, fixedSignatures, mask, fieldStorage))
                    throw new IllegalStateException("Prepaid fixed geometry lost its numerical boundary");
                if (existing) {
                    long bricks = write.changedBricks() | (fixedField.activeBricks(section) & ~mask);
                    while (bricks != 0) {
                        int brick = Long.numberOfTrailingZeros(bricks); bricks &= bricks - 1;
                        for (int cell = 0; cell < 64; cell++) {
                            int block = BlockBrickLayout.pageBlock(brick, cell);
                            long position = BlockPos.asLong((SectionPos.x(section) << 4) + (block & 15),
                                    (SectionPos.y(section) << 4) + (block >>> 8), (SectionPos.z(section) << 4) + (block >>> 4 & 15));
                            fixedField.changeCell(position, (mask & 1L << brick) == 0 ? ThermalSignatureTable.UNRESOLVED : fixedSignatures[block],
                                    changeAt(write.page.handle, block));
                        }
                    }
                    fixedField.ambient(section, write.naturalTemperatureC());
                }
                fixedField.prepareGeometryRevision(section, write.publication.geometryRevision());
                changed.add(new QueryPublication.SectionPublication(write.page.handle, write.publication));
            }
            headers = changed.toArray(QueryPublication.SectionPublication[]::new);
            return true;
        }
        private boolean beginGeometryValues() {
            return fixedField.begin(fixedHeat.seal(publicationTick, publicationTick,
                    position -> boundary.affects(sectionAt(position))), boundary.sections());
        }
        private void prepareGameplayValues() {
            var changes = input.geometry().materialChanges();
            if (changes.size() == 0) return;
            var migrated = new MaterialSample();
            for (int event = 0; event < changes.size(); event++) {
                if (changes.cause(event) != MaterialChanges.GAMEPLAY_TRANSITION) continue;
                for (var write : prepared.pageWrites) if (!write.retirement && write.page.handle == changes.page(event)) {
                    int cell = changes.blockIndex(event);
                    int brick = (cell & 15) >>> 2 | ((cell >>> 4 & 15) >>> 2) << 2 | ((cell >>> 8) >>> 2) << 4;
                    var payload = write.publication.brick(brick); var layout = payload.blockLayout();
                    if (layout == null) break;
                    int node = layout.nodeAt((cell & 3) | (cell >>> 4 & 3) << 2 | (cell >>> 8 & 3) << 4);
                    if (node < layout.airNodeCount()) break;
                    arena.readMaterial(payload.firstSlot() + node, migrated);
                    long section = write.page.handle.sectionKey();
                    long position = BlockPos.asLong((SectionPos.x(section) << 4) + (cell & 15),
                            (SectionPos.y(section) << 4) + (cell >>> 8), (SectionPos.z(section) << 4) + (cell >>> 4 & 15));
                    fixedField.prepareMaterialValue(position, migrated.enthalpyJ(), migrated.branch());
                    break;
                }
            }
        }
        private void restoreNativeValues() {
            var restored = new MaterialSample();
            for (int index = 0; index < prepared.pageWrites.length; index++) {
                long bricks = restoringBricks[index]; if (bricks == 0) continue;
                var write = prepared.pageWrites[index];
                ThermalInputBatch.DormantAirCut air = null;
                ThermalInputBatch.DormantMaterialCut material = null;
                for (var admission : input.admissions()) if (admission.page() == write.page.handle) {
                    air = admission.dormantAir(); material = admission.dormantMaterials();
                }
                for (var update : input.residencyUpdates()) if (update.page() == write.page.handle) {
                    if (update.dormantAir() != null) air = update.dormantAir();
                    if (update.dormantMaterials() != null) material = update.dormantMaterials();
                }
                long section = write.page.handle.sectionKey();
                if (air != null) {
                    long tick = Math.max(publicationTick, fixedField.sectionSampleTick(section));
                    if (air.targetTick() >= 0 && tick > air.targetTick()) air = new ThermalInputBatch.DormantAirCut(air.entry(),
                            air.naturalTemperatureC(), com.teammoeg.frostedheart.content.climate.thermal.persistence.DormantThermalCooling.factor(
                                    air.entry().savedGameTick(), tick, air.coolingRate()), tick, air.coolingRate());
                    fixedField.restorePreparedAir(section, bricks, air::cellTemperatureC);
                }
                if (material != null && material.state() != null) {
                    var state = material.state(); var table = fixedField.signatureTable();
                    for (int body = 0; body < state.size(); body++) {
                        int cell = state.position(body);
                        int brick = (cell & 15) >>> 2 | (cell >>> 4 & 15) >>> 2 << 2 | (cell >>> 8) >>> 2 << 4;
                        if ((bricks & 1L << brick) == 0 || changeAt(write.page.handle, cell) == TiledThermalField.Change.REPLACEMENT) continue;
                        int signature = write.signatures().get(cell);
                        var profile = table.valid(signature) && !table.isAir(signature) ? materialProfiles.profileOrNull(table.materialProfileId(signature)) : null;
                        if (profile == null || !state.readMatchingState(body, table.materialStateId(signature), profile.thermalLaw(),
                                Math.max(material.tick(), Math.max(publicationTick, fixedField.sectionSampleTick(section))),
                                material.naturalC(), material.coolingRate(), restored)) continue;
                        long position = BlockPos.asLong((SectionPos.x(section) << 4) + (cell & 15),
                                (SectionPos.y(section) << 4) + (cell >>> 8), (SectionPos.z(section) << 4) + (cell >>> 4 & 15));
                        fixedField.restorePreparedMaterial(position, restored.enthalpyJ(), restored.branch());
                    }
                }
            }
        }
        private TiledThermalField.Change changeAt(com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle page, int block) {
            var changes = input.geometry().materialChanges();
            TiledThermalField.Change result = TiledThermalField.Change.PHYSICAL;
            for (int index = 0; index < changes.size(); index++) if (changes.page(index) == page && changes.blockIndex(index) == block) {
                byte cause = changes.cause(index);
                if (cause == MaterialChanges.REPLACE)
                    return TiledThermalField.Change.REPLACEMENT;
                if (cause == MaterialChanges.THERMAL_TRANSITION)
                    result = TiledThermalField.Change.PHASE;
                if (cause == MaterialChanges.GAMEPLAY_TRANSITION)
                    result = TiledThermalField.Change.GAMEPLAY;
            }
            for (int index = 0; index < input.geometry().size(); index++)
                if (input.geometry().page(index) == page && input.geometry().fullPageSignatures(index) != null) return TiledThermalField.Change.REPLACEMENT;
            return result;
        }
        @Override public void close() {
            if (installation != null && stage < 3) installation.cancel();
            if (installation != null) while (!installation.cleanup(System.nanoTime() + 2_000_000L)) { }
            if (stage < 3) planner.discardPrepared();
            if (fieldStorage != null) { fieldStorage.close(); fieldStorage = null; }
            credit.close();
            if (stage == 5 && !returningInputs) for (var write : prepared.pageWrites) if (!write.retirement)
                fixedField.compact(write.page.handle.sectionKey());
        }
    }

    private void refreshFixedMixing(long section) {
        long bricks = fixedField.activeBricks(section);
        while (bricks != 0) {
            int brick = Long.numberOfTrailingZeros(bricks); bricks &= bricks - 1;
            fixedMixing.reset((SectionPos.x(section) << 4) + (brick & 3) * 4,
                    (SectionPos.y(section) << 4) + (brick >>> 4) * 4, (SectionPos.z(section) << 4) + (brick >>> 2 & 3) * 4);
            sourceBindings.collectMixingSources(fixedMixing, fixedField.sectionSampleTick(section));
            fixedField.replaceMixing(section, brick, fixedMixing);
        }
    }

    private void closeFixedState() {
        synchronized (inputStore()) {
            cancelFixedStep();
            closeSnapshotState();
            if (fixedGeometry != null) { fixedGeometry.close(); fixedGeometry = null; }
            sourceIngress.close(); topologyIngress.close(); topologyInputs.clear(); fixedInputs.clear();
            fixedBoundaries.clear(); fixedActivationBoundaries.clear(); fixedPhysicalCuts.clear(); fixedNextPhysicalCuts.clear(); fixedScope = LongSets.EMPTY_SET;
            if (capacityContinuation != null) { airBudget.cancelCapacityWait(capacityContinuation); capacityContinuation = null; }
            sources.close(); pages.close(); queries.close(); fixedField.close(); fixedHeat.close();
            if (fixedInputCredit != null) { fixedInputCredit.close(); fixedInputCredit = null; }
            if (geometryRetention != null) { geometryRetention.close(); geometryRetention = null; }
            if (airReservation != null) { airReservation.close(); airReservation = null; }
            fixedSnapshotValues = null; fixedMaterialCapture = null;
            if (fixedSnapshotScratchCredit != null) { fixedSnapshotScratchCredit.close(); fixedSnapshotScratchCredit = null; }
            fixedSignatures = null; fixedMixing = null;
        }
    }

    private long lastIntakeNanos;
    private volatile double publishedPendingJ;

    public String regionalProgressDescription() {
        return "runtime=fixed_sections,intakeTick=" + offeredThrough + ",minimumSampleTick=" + fixedField.acceptedTick()
                + ",latestSampleTick=" + fixedField.latestTick()
                + ",sections=" + fixedField.tileCount() + ",pendingJ=" + fixedInputs.pendingEnergyJ()
                + ",commits=" + fixedCommits + ",geometry=" + (fixedGeometry == null ? "none" : fixedGeometry.stage)
                + ",pendingGeometry=" + describeFixedPendingBoundary()
                + ",rootBytes=" + airBudget.rootUsedBytes();

    }

    private String describeFixedPendingBoundary() {
        var input = topologyInputs.earliestPendingInput();
        if (input == null) return "none";
        var boundary = fixedBoundaries.get(input);
        return "tick:" + input.batch().targetTick() + ":section:" + input.page().sectionKey()
                + ":activeSince:" + fixedField.activeSince(input.page().sectionKey()) + ":mixing:" + input.mixing().size()
                + ":growth:" + fixedGrowthOnly(input) + (boundary == null ? "" : ":range:"
                + fixedField.minimumTick(boundary.sections()) + ".." + fixedField.maximumTick(boundary.sections()));
    }

    /** Coordinator-boundary export of existing passage/sky compilation into the fixed-space numerical core. */
    public boolean copyTransport(long section, TiledThermalField field) {
        return copyTransport(section, field, null);
    }
    private boolean copyTransport(long section, TiledThermalField field, ThermalMemoryBudget.Reservation prepaid) {
        requireOpen();
        var page = pages.pageAt(section);
        var fragments = new ThermalFragment[64];
        for (int brick = 0; brick < fragments.length; brick++) {
            fragments[brick] = page == null
                    ? ThermalFragment.EMPTY
                    : solver.fragment(page.workerPageSlot() * 64 + brick);
        }
        return field.replaceTransport(section, fragments, arena, prepaid);
    }

    /** Use the existing source outlet resolver, including passage routing and declared/degraded sinks. */
    public SpatialSourceTimeline.PortDestination sourcePortDestination(SpatialSourceTimeline.Descriptor source, int port) {
        requireOpen();
        return sourceBindings.checkpointTarget(source, port);
    }

    private final TopologyInbox topologyInputs = new TopologyInbox();
    private ThermalMemoryBudget.Reservation geometryRetention;
    private boolean backingNeedsReclaim;
    private final it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap<com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle.GeometryResyncToken> resyncEffects = new it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap<>();
    private final it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap<ThermalCompletion.BrickResidency> residencyEffects = new it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap<>();
    private boolean limitedEffect;
    private long effectSequence;
    private volatile long minimumRegionTick;
    private long structureCommits;

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

    private static final class RestoreRequest {
        final long chunk, tick, minimumPage;
        final double halfLife;
        final PendingHeatCheckpoint pending;
        final ThermalMemoryBudget.Reservation reservation;
        int cursor;
        long geometryWait = Long.MIN_VALUE;
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
            {
                if (fixedGeometry != null) return false;
                long revision = structureCommits + fixedField.visibilityRevision();
                for (var restore : restoreRequests) if (restore.geometryWait != revision
                        && !(activeSnapshot != null && activeSnapshot.inputsFrozen && activeSnapshot.chunk == restore.chunk)) return true;
                return false;
            }

        }
    }
    private boolean restorationAvailable(RestoreRequest request) {
        if (activeSnapshot != null && activeSnapshot.inputsFrozen && activeSnapshot.chunk == request.chunk) return false;

        if (fixedGeometry != null) return false;
        long revision = structureCommits + fixedField.visibilityRevision();
        if (request.geometryWait == revision) return false;
        int end = Math.min(request.pending.size(), request.cursor + 128);
        for (int index = request.cursor; index < end; index++) {
            long position = request.pending.position(index), section = SectionPos.asLong(BlockPos.getX(position) >> 4,
                    BlockPos.getY(position) >> 4, BlockPos.getZ(position) >> 4);
            var page = pages.pageAt(section);
            if (page == null || page.handle.lifecycleGeneration() < request.minimumPage || !fixedField.hasPublishedSection(section)
                    || fixedField.frozen(section) || pages.geometryRevision(page) != page.handle.liveGeometryRevision()
                    || !fixedField.signatureTable().valid(pages.signatureAtWorld(BlockPos.getX(position), BlockPos.getY(position), BlockPos.getZ(position)))) {
                request.geometryWait = revision; return false;
            }
        }
        return true;
    }
    private boolean receiveRestore() {
        synchronized (inputStore()) {
            RestoreRequest request = null;
            synchronized (intakeLock) {
                int remaining = restoreRequests.size();
                while (remaining-- > 0) {
                    var candidate = restoreRequests.removeFirst(); restoreRequests.addLast(candidate);
                    if (restorationAvailable(candidate)) { request = candidate; break; }
                }
            }
            if (request == null) return false;
            int end = Math.min(request.pending.size(), request.cursor + 128);
            long requestedTick = request.tick;
            var restored = request.pending.restore(request.cursor, end, position -> Math.max(requestedTick, fixedField.sectionSampleTick(sectionAt(position))), request.halfLife, request.minimumPage);
            receiveFixedRestore(restored, request.reservation);
            request.cursor = end;
            if (end == request.pending.size()) synchronized (intakeLock) {
                restoreRequests.remove(request); request.reservation.close();
                restoringChunks.computeIfPresent(request.chunk, (ignored, count) -> count == 1 ? null : count - 1);
            }
            return true;
        }
    }

    private void receiveFixedRestore(PendingHeatCheckpoint.Restored restored, ThermalMemoryBudget.Reservation allowance) {
        var input = restored.heat(); var bound = new SpatialHeatInput.Builder();
        bound.reserve(input.size()); double rejected = 0;
        for (int index = 0; index < input.size(); index++) {
            var target = input.target(index);
            if (!input.isAir(index)) {
                long position = target.position();
                int signature = pages.signatureAtWorld(BlockPos.getX(position), BlockPos.getY(position), BlockPos.getZ(position));
                var table = fixedField.signatureTable();
                int slot = table.isAir(signature) ? -1 : pages.materialSlot(position, table.materialProfileId(signature));
                if (slot < 0 || table.materialStateId(signature) != target.materialStateId()) {
                    rejected += input.energyJ(index); continue;
                }
                target = new SpatialHeatInput.Target(position, target.face(), -1, 0, table.materialProfileId(signature),
                        arena.materialIncarnation(slot), 0, 0, target.materialStateId(), target.minimumPageGeneration());
            }
            bound.appendAggregate(target, input.fromTick(index), input.toTick(index), input.energyJ(index));
        }
        long required = Math.max(fixedInputCredit.bytes(), fixedInputs.retainedBytes() + 8192L + 2048L * input.size());
        long growth = required - fixedInputCredit.bytes();
        if (growth > 0) allowance.transferTo(fixedInputCredit, growth);
        sources.restorePendingEnergy(restored.originalJ());
        sources.confirmRecordedLoss(restored.coolingLossJ(), false);
        sources.confirmRecordedDelivery(0, restored.unacceptedJ() + rejected);
        receiveSpatialHeat(bound.freeze());
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
        it.unimi.dsi.fastutil.longs.LongArrayList frozenSections;
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

            if (request.stage == SnapshotStage.SEALED) return false;
            return true;
        }
    }

    /** One control stage, never a physical catch-up cut. Helpers return through their ordinary cleanup. */
    private boolean processSnapshotCommand(long deadline) {
        return processSnapshotCommand(deadline, true);
    }
    /** Called only under mailbox ownership while no coordinator turn is executing. */
    public boolean processCheckpointCommandOnMain() {
        return snapshotCommandsReady() && processSnapshotCommand(System.nanoTime() + 2_000_000L, false);
    }
    private boolean processSnapshotCommand(long deadline, boolean allowNumericalWork) {
        SnapshotRequest request;
        synchronized (intakeLock) {
            if (activeSnapshot == null) activeSnapshot = snapshotRequests.pollFirst();
            request = activeSnapshot;
        }
        if (request == null) return false;
        try {

            synchronized (fixedInputs) {
                // The coordinator already excludes numerical publication. Store
                // excludes another capture and input transfer; point queries can
                // continue reading Accepted while this private copy is assembled.
                return advanceSnapshot(request, deadline, allowNumericalWork);
            }
        } finally { checkpointControlReady = snapshotCommandsReady(); }
    }
    private boolean advanceSnapshot(SnapshotRequest request, long deadline, boolean allowNumericalWork) {
        if (request.stage == SnapshotStage.REQUESTED && request.finished == null) {
            {
                prepareFixedSnapshotScratch();
                if (request.purpose == RegionalCheckpoint.Purpose.UNLOAD) {
                    request.frozenSections = new it.unimi.dsi.fastutil.longs.LongArrayList();
                    fixedField.forEachSectionInChunk(request.chunk, section -> {
                        if (!fixedField.frozen(section)) request.frozenSections.add(section);
                    });
                    if (!request.frozenSections.isEmpty()) {
                        cancelFixedStep();
                        for (long section : request.frozenSections) fixedField.freeze(section);
                        structureCommits++;
                    }
                }
            }
            request.stage = SnapshotStage.QUIESCING;
        }
        if (request.finished != null) {
            if (request.field != null) { request.field.onFinish(null); request.field.releaseFrames(); }
            if (!request.ready.isDone()) request.ready.completeExceptionally(new java.util.concurrent.CancellationException("Native checkpoint was not consumed"));
            if (request.unloading) {
                sources.exportPendingEnergy(exportFixedChunkPending(request.chunk), request.sourceTail.export());
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
            if (!request.unloading && request.frozenSections != null && !request.frozenSections.isEmpty()) {
                cancelFixedStep();
                for (long section : request.frozenSections) if (fixedField.snapshotAccepted(section, fixedSnapshotValues))
                    stageFixedReentry(section, Math.max(request.producer.tick, offeredThrough), fixedSnapshotValues.ambientC, request.halfLife);
            }
            synchronized (intakeLock) { activeSnapshot = null; sourceFreezeRevision++; checkpointControlReady = !snapshotRequests.isEmpty(); }
            request.finished.complete(null);
            inputDependencyRevision++;
            sourceIngress.dependenciesChanged();
            return true;
        }
        if (request.stage == SnapshotStage.SEALED) return false;
        if (request.stage == SnapshotStage.QUIESCING) {

            request.stage = SnapshotStage.CAPTURING;
        }
        if (!request.inputsFrozen) {
            request.inputsFrozen = true;
            sourceFreezeRevision++;

        }
        if (!captureSnapshot(request)) return false;
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
        observeCheckpoint(request);
        request.ready.complete(request.field);
        return true;
    }

    /** Original Store -> publication is the only save-visible transition; private work keeps running. */
    public RegionalCheckpoint captureCommittedCheckpoint(long chunk, long tick, SpatialSourceTimeline producer,
            PendingHeatCheckpoint cold, double halfLife) {

        SnapshotRequest request = null;
        SpatialSourceTimeline.Capture captured = null;
        try {
            synchronized (fixedInputs) {
                synchronized (queries) {
                    if (closed) throw new IllegalStateException("Thermal owner has closed");
                    prepareFixedSnapshotScratch();
                    captured = producer.captureChunk(tick, chunk, sourceBindings::checkpointTarget);
                    request = new SnapshotRequest(chunk, captured, cold, halfLife, RegionalCheckpoint.Purpose.SAVE);
                    if (!captureSnapshot(request)) throw new IllegalStateException("Committed fixed checkpoint was unavailable");
                    observeCheckpoint(request);
                    return request.field;
                }
            }
        } catch (RuntimeException | Error failure) {
            // Releasing credit may wake the mailbox; do it after leaving the read transaction.
            if (request != null && request.reservation != null) request.reservation.close();
            if (captured != null) captured.close();
            throw failure;
        }
    }

    private boolean captureSnapshot(SnapshotRequest request) {
        if (request.reservation == null) {
            long records = request.cold.size() + request.producer.maximumPendingRecords(request.chunk, sourceBindings::checkpointTarget);
            synchronized (intakeLock) { for (var restore : restoreRequests) if (restore.chunk == request.chunk) records += restore.pending.size() - restore.cursor; }
            // One active checkpoint owns a chunk-sized workspace. It cannot wait
            // for the frozen region to advance or borrow another task's credit.
            request.reservation = airBudget.reserveCompletion((fixedCheckpointBytes(request.chunk)) + 1024L * records);
        }
        var tails = collectFixedCheckpointInputs(request.chunk);
        request.sourceTail = request.producer.pending(request.chunk, sourceBindings::checkpointTarget, tails::appendAggregate);
        PendingHeatCheckpoint inputCold = request.cold;
        synchronized (intakeLock) {
            // Main may already have written a native cold copy while its export
            // still awaits a control turn. The Store/producer still owns those J;
            // add only the cold input that preceded that native copy. The result
            // keeps request.cold as the Chunk's eventual post-export cache.
            if (activeSnapshot != null && activeSnapshot != request
                    && activeSnapshot.unloading && activeSnapshot.chunk == request.chunk) inputCold = activeSnapshot.cold;
            for (var restore : restoreRequests) if (restore.chunk == request.chunk)
                restore.pending.collectRemainingForSave(restore.cursor, request.producer.tick, restore.halfLife, tails::appendAggregate);
        }
        inputCold.collectRemainingForSave(0, request.producer.tick, request.halfLife, tails::appendAggregate);
        request.field = captureFixedCheckpoint(request, tails);
        if (request.field == null) return false;
        request.field.production(request.producer, request.sourceTail);
        return true;
    }

    private void observeCheckpoint(SnapshotRequest request) {
        var observer = com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer;
        if (observer != null) observer.checkpointSealed(request.chunk, inputStore().unboundWindowsInChunk(request.chunk),
                request.field, request.producer, request.cold, request.halfLife);
    }

    private long fixedCheckpointBytes(long chunk) {
        long[] bytes = {32768L + 4096L * fixedInputs.targetsInChunk(chunk) + 6144L * fixedHeat.targetCountInChunk(chunk)};
        // Output plus capture/builder overlap, including worst-case disconnected
        // Q1 corners and material palettes. The retained scratch is separate.
        fixedField.forEachSectionInChunk(chunk, section -> bytes[0] += 16384L + 512L * fixedField.sectionCellCount(section));
        return bytes[0];
    }

    private SpatialHeatInput.Builder collectFixedCheckpointInputs(long chunk) {
        var pending = new SpatialHeatInput.Builder();
        fixedInputs.collectChunk(chunk, (target, from, to, joules) -> {
            if (target.face() == SpatialHeatInput.MATERIAL && target.materialStateId() < 0) {
                int signature = pages.signatureAtWorld(BlockPos.getX(target.position()), BlockPos.getY(target.position()), BlockPos.getZ(target.position()));
                var table = fixedField.signatureTable();
                int slot = table.valid(signature) ? pages.materialSlot(target.position(), table.materialProfileId(signature)) : -1;
                int state = slot >= 0 && (target.incarnation() == 0 || arena.materialIncarnation(slot) == target.incarnation())
                        ? table.materialStateId(signature) : -1;
                target = new SpatialHeatInput.Target(target.position(), target.face(), -1, 0, target.materialProfile(),
                        0, 0, 0, state, target.minimumPageGeneration());
            }
            pending.appendAggregate(target, from, to, joules);
        });
        fixedHeat.committedPendingInChunk(chunk, (address, joules, since) -> {
            int state = address.materialGeneration() < 0 ? -1
                    : fixedField.currentMaterialStateId(address.position(), address.materialGeneration());
            int count = address.materialGeneration() < 0 && address.face() < 0 ? 6 : 1;
            double remaining = joules;
            for (int face = 0; face < count; face++) {
                double amount = face == count - 1 ? remaining : joules / count; remaining -= amount;
                byte side = address.materialGeneration() >= 0 ? SpatialHeatInput.MATERIAL : (byte) (count == 1 ? address.face() : face);
                pending.appendAggregate(new SpatialHeatInput.Target(address.position(), side, -1, 0, 0, 0, 0, 0, state),
                        since, Math.max(since, fixedField.sectionSampleTick(sectionAt(address.position()))), amount);
            }
        });
        return pending;
    }

    private RegionalCheckpoint captureFixedCheckpoint(SnapshotRequest request, SpatialHeatInput.Builder pending) {
        var sections = new java.util.ArrayList<RegionalCheckpoint.Section>();
        fixedField.forEachSectionInChunk(request.chunk, section -> {
            if (!fixedField.hasPublishedSection(section) || !fixedField.snapshotAccepted(section, fixedSnapshotValues)) return;
            var values = fixedSnapshotValues;
            var air = fixedField.captureAir(section, values.activeBricks, Double.NaN);
            var materials = MaterialSectionState.captureCells(values.signatures, values.enthalpyJ, values.branches,
                    values.sampleTick, fixedField.signatureTable(), materialProfiles, fixedMaterialCapture);
            sections.add(new RegionalCheckpoint.Section(section, values.geometryRevision,
                    values.activeBricks, values.sampleTick, air, materials));
        });
        return new RegionalCheckpoint(sections, pending.freeze(), request.cold, request.reservation);
    }

    private void prepareFixedSnapshotScratch() {
        if (fixedSnapshotValues == null) {
            fixedSnapshotScratchCredit = airBudget.reserveCompletion(512 * 1024);
            fixedSnapshotValues = new TiledThermalField.SectionSnapshot();
            fixedMaterialCapture = new MaterialSectionState.CaptureScratch();
        }
    }

    private double exportFixedChunkPending(long chunk) {
        if (!fixedInputs.hasRetainedInputInChunk(chunk) && !fixedHeat.hasRetainedInputInChunk(chunk)) return 0;
        cancelFixedStep();
        double[] total = {fixedHeat.exportCommittedPendingInChunk(chunk)};
        fixedInputs.retireChunk(chunk, (target, joules) -> total[0] += joules);
        fixedPendingJ = fixedHeat.committedPendingJoules();
        publishedPendingJ = fixedInputs.pendingEnergyJ() + fixedPendingJ;
        return total[0];
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
        if (diagnostics != null) diagnostics.inputAccepted(envelope, fixedInputs.targetCount() != 0);
    }

    private long inputDependencyRevision;

    @Override public boolean sourceControlReady(SpatialSourceTimeline.Descriptor source, long tick) {

        long through = sourceBindings.bindingLimit(source, this::fixedControlTime,
                position -> fixedControlTime(sectionAt(position)));
        var footprint = new LongOpenHashSet();
        sourceBindings.collectMixingFootprint(source, footprint);
        for (long position : footprint) through = Math.min(through, fixedControlTime(sectionAt(position)));
        return through >= tick;
    }
    private long fixedControlTime(long section) {
        // Frozen writeback owns no running interval. Its controls may feed the
        // active neighbours that must reach the explicit reentry cut first.
        if (fixedField.hasSection(section)) return fixedField.frozen(section) ? Long.MAX_VALUE : fixedField.sectionSampleTick(section);
        var input = topologyInputs.peek(section);
        // First admission supplies a virtual initial cut until its Tile exists.
        return input == null ? Long.MAX_VALUE : input.batch().targetTick();
    }

    /** Source coefficients share Page input order; they never create a Page input waiter. */

    private long geometryBoundary(long section) {
        return fixedPhysicalCuts.getOrDefault(section, Long.MAX_VALUE);

    }

    private long boundarySequence = -1;

    /** Temporary union of inputs that install the same physical boundary. */

    private static long chunkForSection(long section) {
        return net.minecraft.world.level.ChunkPos.asLong(net.minecraft.core.SectionPos.x(section), net.minecraft.core.SectionPos.z(section));
    }

    private void refreshChunkInputHold(long chunk) {
        long pending = topologyInputs.firstTickInChunk(chunk);
        pending = Math.min(pending, topologyIngress.firstTickInChunk(chunk));
        if (pending == Long.MAX_VALUE) pendingChunkInput.remove(chunk); else pendingChunkInput.put(chunk, pending);
    }

    private void retireInputSlot(long section) {
        if (pages.pageAt(section) == null && topologyInputs.peek(section) == null) topologyIngress.retire(section);
    }

    public void beginFailureRecovery() {
        if (!failureRecovery) {
            failureRecovery = true;
            cancelFixedStep();
        }
    }
    public boolean checkpointRecovery() { return failureRecovery; }
    public boolean controlWorkPending() { return checkpointControlReady; }

    public void processWorkRound() {
        {
            processFixedRound();
            var observer = com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalDiagnostics.observer;
            if (observer != null) observer.fixedRoundFinished(this);
            return;
        }

        // One owner class receives the shared coordinator slice. Later classes rotate to
        // the next round instead of each consuming an additional full deadline.

        // Boundary discovery/readiness may consume the original deadline. The
        // selected owner still needs a usable slice; the pool charges the entire
        // round, including this prelude, to the shared service allowance.

    }

    /** Bound temporary overlap to available workers while independent regions remain parallel. */

    /** Bounded failure observations; normal play neither scans nor builds these descriptions. */

    public long minimumRegionTick() { return minimumRegionTick; }
    public boolean hasRetainedInputInChunk(long chunk) {
        return restoringChunks.containsKey(chunk) || pendingChunkInput.containsKey(chunk)
                || sourceIngress.hasOfferedInChunk(chunk) || (fixedInputs.hasRetainedInputInChunk(chunk) || fixedHeat.hasRetainedInputInChunk(chunk));
    }

    public boolean hasReadyWork() {
        return fixedHasReadyWork();

    }
    public boolean hasWork() {
        synchronized (intakeLock) {
            if (failureRecovery) return activeSnapshot != null || !snapshotRequests.isEmpty();
            if (topologyIngress.hasWork() || sourceIngress.hasWork() || !restoreRequests.isEmpty() || activeSnapshot != null || !snapshotRequests.isEmpty()) return true;
        }
        return fixedHasReadyWork() || fixedHasPendingNumerics()
                || fixedGeometry != null || fixedClaim != null || !topologyInputs.isEmpty()
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
        long capacity = airBudget.capacityRevision();
        boolean waitingForCapacity = (fixedCapacityWait == capacity
                || fixedGeometry != null && fixedGeometry.capacityWait == capacity || pages.maintenanceCapacityWait() == capacity);
        if (closeContinuationReady || !closing && hasReadyWork()) { continuation.run(); return; }
        {
            if (capacityContinuation != null) airBudget.cancelCapacityWait(capacityContinuation);
            capacityContinuation = null;
            // Input and native-control arrivals already wake the original mailbox.
            // Only actual admission pressure waits on the memory budget revision.
            if (waitingForCapacity) {
                capacityContinuation = continuation;
                airBudget.whenCapacityChanges(capacity, continuation);
            }
            return;
        }

    }

    private void reserveAirBacking() {
        // Capture services first: main may offer more source metadata while the
        // complete union is scanned, and that later growth must remain payable.
        long services = fixedServiceBackingBytes();
        reserveAirBacking(airBackingBytes());
        {
            fixedServiceBackingBytes = services;
            fixedUnreconciledBackingBytes = 0;
            fixedBackingTick = fixedField.acceptedTick();
            backingNeedsReclaim = false;
        }
    }

    /** One retained union for the fixed field's original worker services and installed geometry. */
    private long airBackingBytes() {
        backingInventoryRuns++;
        var meter = backingMeter;
        try {
            sourceIngress.recordBacking(meter); queries.recordAirLayoutBacking(meter);
            sourceBindings.recordMixingBacking(meter);
            arena.recordBacking(meter); solver.recordBacking(meter); sources.recordBacking(meter); pages.recordBacking(meter);
            sourceBindings.recordBacking(meter); phases.recordBacking(meter);
            return Math.max(1L, meter.bytes());
        } finally { meter.clear(); }
    }

    /** New installed payload keeps the task's prepaid credit until a retained-union reconciliation. */
    private void handoffFixedGeometryBacking(FixedGeometry geometry) {
        var meter = backingMeter;
        long services, retained;
        try {
            recordFixedServiceBacking(meter);
            services = meter.bytes();
            geometry.installation.recordInstalledBacking(meter);
            geometry.planner.recordInstalledRouteBacking(meter);
            retained = meter.bytes() - services + Math.max(0, services - fixedServiceBackingBytes);
        } finally { meter.clear(); }
        geometry.credit.completeResize(Math.max(geometry.credit.bytes(), retained));
        geometry.credit.transferTo(airReservation, retained);
        fixedServiceBackingBytes = services;
        // A retirement can release data without adding a new allocation.
        fixedUnreconciledBackingBytes += Math.max(1, retained);
    }

    /** Reclaim at an ordinary input interval, one MiB of conservative handoffs, or actual capacity pressure. */
    private boolean fixedBackingReconciliationDue() {
        if (backingNeedsReclaim) return true;
        if (fixedUnreconciledBackingBytes == 0) return false;
        if (fixedUnreconciledBackingBytes >= BACKING_RECONCILE_BYTES
                || fixedField.acceptedTick() - fixedBackingTick >= ThermalInputBatch.CUT_INTERVAL_TICKS) return true;
        long revision = airBudget.capacityRevision();
        return fixedCapacityWait == revision || fixedGeometry != null && fixedGeometry.capacityWait == revision;
    }

    private long fixedServiceBackingBytes() {
        var meter = backingMeter;
        try { recordFixedServiceBacking(meter); return meter.bytes(); }
        finally { meter.clear(); }
    }

    /** Mutable service capacities; unchanged Page geometry and route payloads are not traversed here. */
    private void recordFixedServiceBacking(AirBackingMeter meter) {
        arena.recordStorageBacking(meter); solver.recordBacking(meter); sources.recordBacking(meter);
        pages.recordDirectoryBacking(meter); sourceBindings.recordBacking(meter);
        sourceIngress.recordBacking(meter); phases.recordBacking(meter);
        meter.container(fixedPhysicalCuts, fixedPhysicalCuts.size(), 16);
        meter.container(fixedNextPhysicalCuts, fixedNextPhysicalCuts.size(), 16);
        meter.container(fixedBoundaries, fixedBoundaries.size(), 32);
        meter.container(fixedActivationBoundaries, fixedActivationBoundaries.size(), 16);
        for (var boundary : fixedBoundaries.values()) if (meter.objectOnce(boundary, 64))
            meter.container(boundary.sections(), boundary.sections().size(), 8);
    }

    /** One union covers the ready candidates and this slice's completed releases. */

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
        { closeFixedState(); return; }

    }

    private void closeSnapshotState() {
        synchronized (intakeLock) {
            var failure = new IllegalStateException("Thermal owner closed before checkpoint completion");
            if (activeSnapshot != null) {
                if (activeSnapshot.field != null) {
                    activeSnapshot.field.onFinish(null);
                    activeSnapshot.field.releaseFrames();
                }

                if (activeSnapshot.reservation != null) activeSnapshot.reservation.close();
                activeSnapshot.ready.completeExceptionally(failure);
                if (activeSnapshot.finished != null) activeSnapshot.finished.completeExceptionally(failure);
                activeSnapshot = null;
            }
            for (var request : snapshotRequests) request.ready.completeExceptionally(failure);
            snapshotRequests.clear();
            for (var request : restoreRequests) request.reservation.close();
            restoreRequests.clear(); restoringChunks.clear();
            checkpointControlReady = false;
        }
    }

    public boolean closeRound() {
        if (closed) return true;
        { close(); return true; }

    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("thermal dimension engine is closed");
        }
    }

    /** Opt-in full inventory; ordinary profiling uses the local capacity summaries. */

}
