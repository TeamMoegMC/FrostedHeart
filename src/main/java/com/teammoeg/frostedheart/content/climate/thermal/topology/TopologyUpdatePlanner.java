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
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ArenaSpan;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialStateAccess;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.engine.ThermalDimensionLimits;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ResolvedGeometryBatch;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalCompletion;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalFragment;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSolver;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * 可失败、可复用的稀疏拓扑准备阶段。
 *
 * <p>它读取当前 {@link WorkerPageStore} 和一个输入 batch，为受影响 Brick
 * 生成完整 replacement 与 solver delta，但不修改任何已提交权威。成功结果
 * 由区域安装任务交接给 {@link RegionalTopologyInstall}。</p>
 */
public final class TopologyUpdatePlanner {
    private static final int BRICKS_PER_PAGE = 64;

    private final WorkerPageStore pages;
    private final ThermalCellArena arena;
    private final ThermalSolver solver;
    private final PhaseTransitionRuntime phases;
    private final ThermalSignatureTable signatures;
    private final BrickTopologyCompiler compiler;
    private final MaterialStateAccess materialStates;
    private final MaterialBoundaryRegistry materials;
    private final ThermalTopologyParameters parameters;
    private final BrickMigrationKernel migration;
    private final ThermalDimensionLimits limits;
    private final PreparedTopologyChange.Builder changeBuilder =
            new PreparedTopologyChange.Builder();

    private final ArrayList<PageDraft> draftPool = new ArrayList<>();
    private final Long2ObjectOpenHashMap<PageDraft> draftsBySection =
            new Long2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<PageDraft> draftsBySlot = new Int2ObjectOpenHashMap<>();
    private final IntOpenHashSet affectedFragments = new IntOpenHashSet();
    private final IntOpenHashSet layoutCandidates = new IntOpenHashSet();
    private final LongOpenHashSet sourceDirtySections = new LongOpenHashSet();
    private final IntArrayList removedPhaseSlots = new IntArrayList();
    private final IntArrayList addedPhaseSlots = new IntArrayList();
    private final ArrayList<PreparedTopologyChange.OldSpan> oldSpans = new ArrayList<>();
    private final ArrayList<ThermalPageHandle.GeometryResyncToken> resyncTokens = new ArrayList<>();
    private int draftCount;
    private int stagedAdmissions;
    private int stagedCellCount;
    private final TopologyView view;
    private final AirRouteCompiler airRoutes;
    private boolean routePreparationStarted;

    public TopologyUpdatePlanner(
            WorkerPageStore pages,
            ThermalCellArena arena,
            MaterialStateAccess materialStates,
            ThermalSolver solver,
            PhaseTransitionRuntime phases,
            ThermalSignatureTable signatures,
            MaterialBoundaryRegistry materials,
            BrickTopologyCompiler compiler,
            ThermalTopologyParameters parameters,
            ThermalDimensionLimits limits) {
        this(pages, arena, materialStates, solver, phases, signatures, materials, compiler, parameters, limits, null);
    }

    private TopologyUpdatePlanner(WorkerPageStore pages, ThermalCellArena arena, MaterialStateAccess materialStates,
            ThermalSolver solver, PhaseTransitionRuntime phases, ThermalSignatureTable signatures,
            MaterialBoundaryRegistry materials, BrickTopologyCompiler compiler, ThermalTopologyParameters parameters,
            ThermalDimensionLimits limits, AirRouteCompiler routes) {
        this.materialStates = materialStates; this.materials = materials; this.parameters = parameters;
        this.pages = pages;
        this.arena = arena;
        this.solver = solver;
        this.phases = phases;
        this.signatures = signatures;
        this.compiler = compiler;
        this.migration = new BrickMigrationKernel(arena, materialStates, signatures, parameters, materials);
        this.limits = limits;
        this.view = new TopologyView(pages, draftsBySection, draftsBySlot);
        airRoutes = routes == null ? new AirRouteCompiler(signatures, arena, limits.maximumLiveCells()) : routes.forPreparation();
        view.airRoutes(airRoutes);
        if (routes == null) pages.airRoutes(airRoutes);
        pages.signatures(signatures);
    }

    /** Geometry scratch belongs to a structural job; emitter membership is sealed with its input. */
    public TopologyUpdatePlanner forInput(
            com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings.FrozenSources sourceInput,
            LongSet sections) {
        var planner = new TopologyUpdatePlanner(pages, arena, materialStates, solver, phases, signatures, materials,
                compiler.forInput(sourceInput::collectMixingSources), parameters, limits, airRoutes);
        planner.view.captureSections(sections);
        return planner;
    }

    /** Separately admitted immutable inputs; callers transfer ownership to the numerical candidate. */
    public FrozenAirGeometry freezeAirGeometry(
            com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings.FrozenSources sources,
            com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget.Reservation scratch) {
        return FrozenAirGeometry.capture(view, arena, sources, signatures, scratch);
    }

    public void beginCut() {
        airRoutes.beginCut();
    }

    public long routeVisitsLastCut() {
        return airRoutes.lastVisitCount();
    }

    /** Includes set/queue growth and route traversal scratch before any closure is allocated. */
    public long continuationBoundaryBytes(ThermalInputBatch batch) {
        boolean pending = airRoutes.hasPendingWork();
        if (!RegionalContinuationBoundary.hasStructuralInput(batch) && !pending) return 0;
        long seeds = batch.admissions().length + (long) batch.retirements().length + batch.residencyUpdates().length
                + batch.geometry().size() + batch.geometry().materialChanges().size()
                + batch.phaseAcks().length + batch.phaseIntents().length + batch.environmentUpdates().length;
        for (var halo : batch.geometry().halos()) seeds += 1L + halo.halo().sectionCountBound();
        // This read-only walk borrows existing route components. Its allocations
        // are component references and Section sets/queues, never route positions.
        long sections = 27L * seeds + pages.capturedSectionCountBound() + airRoutes.dependencySectionCount();
        if (pending) sections += draftsBySection.size() + airRoutes.pendingSectionCountBound();
        // Includes old/new hash tables during growth and the primitive queue.
        return 2048L + 64L * airRoutes.componentCountBound() + 128L * sections;
    }

    /** Seed union is exact for inputs at the same physical boundary; no payload is copied. */
    public static void collectBoundarySeeds(ThermalInputBatch batch, LongSet seeds) {
        for (var admission : batch.admissions()) seeds.add(admission.page().sectionKey());
        for (var retirement : batch.retirements()) seeds.add(retirement.page().sectionKey());
        for (var update : batch.residencyUpdates()) seeds.add(update.page().sectionKey());
        for (int index = 0; index < batch.geometry().size(); index++) seeds.add(batch.geometry().page(index).sectionKey());
        var changes = batch.geometry().materialChanges();
        for (int index = 0; index < changes.size(); index++) seeds.add(changes.page(index).sectionKey());
        for (var halo : batch.geometry().halos()) {
            seeds.add(halo.page().sectionKey());
            for (long position : halo.halo().positions()) seeds.add(SectionPos.asLong(
                    BlockPos.getX(position) >> 4, BlockPos.getY(position) >> 4, BlockPos.getZ(position) >> 4));
        }
        for (var environment : batch.environmentUpdates())
            if (environment.skyColumns().length != 0) seeds.add(environment.page().sectionKey());
        for (var ack : batch.phaseAcks()) seeds.add(SectionPos.asLong(
                ack.request().blockX() >> 4, ack.request().blockY() >> 4, ack.request().blockZ() >> 4));
        for (var intent : batch.phaseIntents()) seeds.add(intent.page().sectionKey());
    }

    public RegionalContinuationBoundary continuationBoundary(LongSet seeds, boolean structuralInput) {
        boolean pending = airRoutes.hasPendingWork();
        boolean unresolved = airRoutes.discoveryIncomplete();
        if (!structuralInput && !pending) return RegionalContinuationBoundary.UNCHANGED;
        var affected = new LongOpenHashSet();
        var queue = new it.unimi.dsi.fastutil.longs.LongArrayList();
        for (long seed : seeds) for (int x = -1; x <= 1; x++)
            for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                long section = SectionPos.asLong(SectionPos.x(seed) + x, SectionPos.y(seed) + y, SectionPos.z(seed) + z);
                if (affected.add(section)) queue.add(section);
            }
        if (pending) airRoutes.includePendingSections(affected, queue);
        airRoutes.expandAffectedSections(affected, queue);
        {
            // A passage search only moves to a face-adjacent captured position.
            // Treat every captured section as fully connected: this deliberately
            // overestimates reach before discovering any new route as well.
            // Limiting this closure to already-pending searches omits outlets
            // reached by the very input whose boundary is being selected.
            // Include old/candidate/input geometry and actual halos so neither a
            // new Page nor an old dependency can create an unaccounted shortcut.
            for (int index = 0; index < queue.size(); index++) {
                long key = queue.getLong(index);
                for (int face = 0; face < 6; face++) {
                    long next = SectionPos.asLong(SectionPos.x(key) + TopologyView.DX[face],
                            SectionPos.y(key) + TopologyView.DY[face], SectionPos.z(key) + TopologyView.DZ[face]);
                    // Query the existing indexes instead of copying the whole dimension for this walk.
                    if (!affected.contains(next) && (pages.hasCapturedSection(next)
                            || draftsBySection.containsKey(next) || seeds.contains(next)
                            || airRoutes.hasDependencySection(next))) {
                        affected.add(next);
                        queue.add(next);
                    }
                }
            }
        }
        return new RegionalContinuationBoundary(affected, unresolved);
    }

    private double externalMaterialEnergyJ;

    public void committed() {
        view.commitHalos();
        airRoutes.committed();
    }

    public boolean dependenciesCurrent() { return view.dependenciesCurrent() && airRoutes.dependenciesCurrent(); }

    public void recordInstalledRouteBacking(AirBackingMeter meter) { airRoutes.recordInstalledBacking(meter); }

    public void recordBacking(AirBackingMeter meter) {
        meter.container(draftPool, draftPool.size(), 8); meter.container(draftsBySection, draftsBySection.size(), 16);
        meter.container(draftsBySlot, draftsBySlot.size(), 12); meter.container(affectedFragments, affectedFragments.size(), 4);
        meter.container(layoutCandidates, layoutCandidates.size(), 4); meter.container(sourceDirtySections, sourceDirtySections.size(), 8);
        meter.array(removedPhaseSlots.elements()); meter.array(addedPhaseSlots.elements());
        meter.container(oldSpans, oldSpans.size(), 8); meter.container(resyncTokens, resyncTokens.size(), 8);
        for (int index = 0; index < draftCount; index++) {
            var draft = draftPool.get(index);
            meter.objectOnce(draft, 256); meter.array(draft.brickSignatureValues); meter.array(draft.replacements);
            meter.array(draft.materialChangeIndexes); meter.array(draft.skyColumns); meter.array(draft.skyValues);
            meter.array(draft.nextSkyExposure); draft.nextSignatures.recordBacking(meter);
            for (var indexes : draft.materialChangeIndexes) if (indexes != null) meter.array(indexes.elements());
            for (var brick : draft.replacements) if (brick != null) brick.recordBacking(meter);
        }
        if (roundFragments != null) {
            meter.array(roundFragments.indexes); meter.array(roundFragments.fragments);
            for (var fragment : roundFragments.fragments) if (fragment != null) fragment.recordBacking(meter);
        }
        view.recordBacking(meter); airRoutes.recordBacking(meter);
    }

    /** Local owners report capacities; no yielded batch rescans a growing candidate object graph. */
    public long preparationScratchBytes(ThermalInputBatch input) {
        long growth = 1024L * 1024L;
        if (preparingBatch == null || preparePhase == 0)
            growth = Math.max(growth, 2 * input.queuedPayloadBytes());
        if (preparePhase == 3) growth = Math.max(growth, airRoutes.preparationGrowthBytes());
        return Math.addExact(preparationRetainedBytes(), growth);
    }

    private long preparationRetainedBytes() {
        long bytes = AirBackingMeter.containerBackingBytes(draftPool, draftPool.size(), 8)
                + AirBackingMeter.containerBackingBytes(draftsBySection, draftsBySection.size(), 16)
                + AirBackingMeter.containerBackingBytes(draftsBySlot, draftsBySlot.size(), 12)
                + AirBackingMeter.containerBackingBytes(affectedFragments, affectedFragments.size(), 4)
                + AirBackingMeter.containerBackingBytes(layoutCandidates, layoutCandidates.size(), 4)
                + AirBackingMeter.containerBackingBytes(sourceDirtySections, sourceDirtySections.size(), 8)
                + AirBackingMeter.shallowArrayBytes(removedPhaseSlots.elements())
                + AirBackingMeter.shallowArrayBytes(addedPhaseSlots.elements())
                + AirBackingMeter.containerBackingBytes(oldSpans, oldSpans.size(), 8)
                + AirBackingMeter.containerBackingBytes(resyncTokens, resyncTokens.size(), 8);
        for (int index = 0; index < draftCount; index++) bytes += draftPool.get(index).preparationBackingBytes();
        if (roundFragments != null) bytes += AirBackingMeter.shallowArrayBytes(roundFragments.indexes)
                + AirBackingMeter.shallowArrayBytes(roundFragments.fragments) + compiledFragmentBytes;
        bytes += view.preparationBackingBytes() + airRoutes.preparationBackingBytes();
        return 65536L + bytes;
    }

    /** Charge the completed fragment and Page replacement while readers still hold the old geometry. */
    public long completedPreparationBytes(PreparedTopologyChange change) {
        var meter = new AirBackingMeter(true);
        recordBacking(meter);
        change.recordBacking(meter);
        long retained = Math.addExact(65536L, meter.bytes());
        if (jdk.jfr.FlightRecorder.isInitialized()) {
            var event = new PreparationStorage();
            event.retainedBytes = retained;
            event.changedFragments = change.fragmentIndexes.length;
            event.changedPages = change.pageWrites.length;
            event.commit();
        }
        return retained;
    }

    @jdk.jfr.Name("frostedheart.ThermalTopologyPreparationStorage")
    @jdk.jfr.StackTrace(false)
    private static final class PreparationStorage extends jdk.jfr.Event {
        public long retainedBytes;
        public int changedFragments, changedPages;
    }

    /** Recheck current fragment references before the private replacement is installed. */
    public void prepareCommit(PreparedTopologyChange change) {
        preflightLimits(new FragmentChanges(change.fragmentIndexes, change.fragments));
        reserveBacking();
    }

    public void discardPrepared() {
        discardStaging();
        airRoutes.discardPreparation();
        reset();
    }

    private ThermalInputBatch preparingBatch;
    private int preparePhase, prepareCursor;
    private FragmentChanges roundFragments;
    private long compiledFragmentBytes;

    public int preparationPhase() { return preparingBatch == null ? -1 : preparePhase; }

    /**
     * One structural task owns these drafts, routes and read dependencies.
     * Independent planners may continue between rounds without changing them.
     * A null result publishes nothing and leaves every installed Page untouched.
     */
    public PreparedTopologyChange prepareRound(ThermalInputBatch batch, LongSet mixingChanges, long deadline,
            com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget.Reservation scratch) {
        do {
            long bytes = preparationScratchBytes(batch);
            if (scratch.bytes() < bytes) scratch.completeResize(bytes);
            PreparedTopologyChange result = advancePreparation(batch, mixingChanges);
            if (result != null) return result;
            // Route discovery can await geometry. Its own bounded traversal decides when to yield.
            if (preparePhase == 3) return null;
        } while (System.nanoTime() < deadline);
        return null;
    }

    private PreparedTopologyChange advancePreparation(ThermalInputBatch batch, LongSet mixingChanges) {
        if (preparingBatch == null) {
            reset();
            preparingBatch = batch;
            preparePhase = prepareCursor = 0;
        } else if (preparingBatch != batch) {
            throw new IllegalStateException("A different input still owns topology preparation");
        }
        try {
            switch (preparePhase) {
                case 0 -> {
                    airRoutes.invalidate(batch);
                    collectRetirements(batch);
                    collectAdmissions(batch);
                    collectResidency(batch.residencyUpdates());
                    collectGeometry(batch.geometry());
                    collectEnvironment(batch.environmentUpdates());
                    preparePhase++;
                }
                case 1 -> {
                    finalizeSignatureCuts();
                    collectLayoutDependencies();
                    boolean dirty = airRoutes.hasPendingWork() || !mixingChanges.isEmpty();
                    for (int index = 0; !dirty && index < draftCount; index++) {
                        PageDraft draft = draftPool.get(index);
                        dirty = draft.admission || draft.retirement || draft.replacedPage != null
                                || (draft.topologyDirtyMask | draft.fragmentDirtyMask | draft.materialDirtyMask) != 0L;
                    }
                    if (dirty) preparePhase++;
                    else { roundFragments = FragmentChanges.EMPTY; preparePhase = 6; }
                }
                case 2 -> {
                    // At most one Page (64 small Bricks), with the original order.
                    while (prepareCursor < draftCount && (draftPool.get(prepareCursor).retirement
                            || draftPool.get(prepareCursor).topologyDirtyMask == 0L)) prepareCursor++;
                    if (prepareCursor < draftCount) compileChangedCells(prepareCursor++);
                    else { prepareCursor = 0; preparePhase++; }
                }
                case 3 -> {
                    if (!routePreparationStarted) {
                        collectFragmentDependencies();
                        for (long position : mixingChanges)
                            markFragment(BlockPos.getX(position), BlockPos.getY(position), BlockPos.getZ(position));
                        airRoutes.beginPrepare(view, affectedFragments);
                        routePreparationStarted = true;
                    }
                    if (!airRoutes.advancePreparation(view, affectedFragments)) return null;
                    int[] indexes = affectedFragments.toIntArray();
                    Arrays.sort(indexes);
                    roundFragments = new FragmentChanges(indexes, new ThermalFragment[indexes.length]);
                    preparePhase++;
                }
                case 4 -> {
                    int end = Math.min(roundFragments.indexes.length, prepareCursor + 16);
                    compileFragments(roundFragments, prepareCursor, end);
                    prepareCursor = end;
                    if (end == roundFragments.indexes.length) { prepareCursor = 0; preparePhase++; }
                }
                case 5 -> {
                    while (prepareCursor < draftCount) {
                        PageDraft draft = draftPool.get(prepareCursor);
                        if (draft.retirement || draft.replacedPage != null || draft.cellReplacementMask != 0L
                                || (draft.page.residentBrickMask & ~draft.nextResidentBrickMask) != 0L) break;
                        prepareCursor++;
                    }
                    if (prepareCursor < draftCount) prepareMigrationsAndRetirements(prepareCursor++);
                    else { prepareCursor = 0; preparePhase++; }
                }
                case 6 -> {
                    preflightLimits(roundFragments);
                    reserveBacking();
                    PreparedTopologyChange.PageWrite[] pageWrites =
                            pages.prepareWrites(draftPool, draftCount, resyncTokens);
                    long baseVersion = solver.structuralVersion();
                    boolean structural = roundFragments.indexes.length != 0 || !oldSpans.isEmpty();
                    long nextVersion = structural ? Math.incrementExact(baseVersion) : baseVersion;
                    long[] dirtySections =
                            sourceDirtySections.isEmpty()
                                    ? PreparedTopologyChange.NO_LONGS
                                    : sourceDirtySections.toLongArray();
                    if (dirtySections.length > 1) {
                        Arrays.sort(dirtySections);
                    }
                    PreparedTopologyChange result = changeBuilder
                            .retiringRoutes(airRoutes.retiringRoutes())
                            .identity(baseVersion, nextVersion)
                            .externalMaterialEnergy(externalMaterialEnergyJ)
                            .fragments(roundFragments.indexes, roundFragments.fragments)
                            .phaseSlots(
                                    removedPhaseSlots.isEmpty()
                                            ? PreparedTopologyChange.NO_INTS
                                            : removedPhaseSlots.toIntArray(),
                                    addedPhaseSlots.isEmpty()
                                            ? PreparedTopologyChange.NO_INTS
                                            : addedPhaseSlots.toIntArray())
                            .pages(
                                    pageWrites,
                                    oldSpans.isEmpty()
                                            ? PreparedTopologyChange.NO_OLD_SPANS
                                            : oldSpans.toArray(PreparedTopologyChange.OldSpan[]::new))
                            .sourceSections(dirtySections)
                            .resyncTokens(
                                    resyncTokens.isEmpty()
                                            ? ThermalCompletion.NO_RESYNC_TOKENS
                                            : resyncTokens.toArray(
                                                    ThermalPageHandle.GeometryResyncToken[]::new))
                            .build();
                    preparingBatch = null;
                    roundFragments = null;
                    return result;
                }
                default -> throw new IllegalStateException("Unknown topology preparation phase");
            }
            return null;
        } catch (RuntimeException | Error failure) {
            try { discardStaging(); }
            catch (Throwable cleanupFailure) { failure.addSuppressed(cleanupFailure); }
            airRoutes.discardPreparation();
            reset();
            throw failure;
        }
    }

    private void discardStaging() {
        for (int draftIndex = 0; draftIndex < draftCount; draftIndex++) {
            PageDraft draft = draftPool.get(draftIndex);
            long remaining = draft.cellReplacementMask;
            while (remaining != 0L) {
                int brick = Long.numberOfTrailingZeros(remaining);
                WorkerBrickTopology staged = draft.replacements[brick];
                // Teardown may follow a failed authoritative commit. Only the
                // still-private spans belong to this cancellation operation.
                if (staged != null && staged.span.count() != 0 && arena.ownsStagedCells(staged.span)) {
                    arena.discardStagedCells(staged.span);
                }
                remaining &= remaining - 1L;
            }
            if (draft.admission && draft.replacedPage == null) {
                pages.releaseStagedAdmission(draft.page);
            }
        }
    }

    private void reset() {
        routePreparationStarted = false;
        preparingBatch = null;
        roundFragments = null;
        compiledFragmentBytes = 0;
        view.resetPreparation();
        draftsBySection.clear();
        draftsBySlot.clear();
        affectedFragments.clear();
        sourceDirtySections.clear();
        removedPhaseSlots.clear();
        addedPhaseSlots.clear();
        oldSpans.clear();
        resyncTokens.clear();
        draftCount = 0;
        stagedAdmissions = 0;
        stagedCellCount = 0;
        externalMaterialEnergyJ = 0;
    }

    private PageDraft acquireDraft(WorkerPageStore.PageState page) {
        view.readPage(page.handle.sectionKey());
        PageDraft existing = draftsBySection.get(page.handle.sectionKey());
        if (existing != null) {
            existing.backingDirty = true;
            return existing;
        }
        PageDraft draft;
        if (draftCount == draftPool.size()) {
            draft = new PageDraft();
            draftPool.add(draft);
        } else {
            draft = draftPool.get(draftCount);
        }
        draftCount++;
        draft.reset(page);
        draftsBySection.put(page.handle.sectionKey(), draft);
        draftsBySlot.put(page.pageSlot, draft);
        return draft;
    }

    private void collectAdmissions(ThermalInputBatch batch) {
        for (ThermalInputBatch.PageAdmission admission : batch.admissions()) {
            PageDraft draft = draftsBySection.get(admission.page().sectionKey());
            if (draft != null) {
                if (draft.retirement && draft.page.handle != admission.page()) {
                    WorkerPageStore.PageState previous = draft.page;
                    draft.page = pages.stageReplacement(previous, admission);
                    draft.replacedPage = previous;
                    draft.admission = true;
                    draft.retirement = false;
                    initializeAdmission(draft, admission);
                }
                continue;
            }
            WorkerPageStore.PageState page = pages.find(admission.page());
            boolean staged = page == null;
            if (staged) {
                if (pages.activePageCount() + stagedAdmissions >= limits.maximumPages()) {
                    throw new WorkLimitedException("thermal Page limit reached");
                }
                page = pages.stageAdmission(admission);
                stagedAdmissions++;
            }
            draft = acquireDraft(page);
            if (staged) {
                draft.admission = true;
                initializeAdmission(draft, admission);
            }
        }
    }

    private static void initializeAdmission(
            PageDraft draft, ThermalInputBatch.PageAdmission admission) {
        draft.nextSignatures = admission.signatures();
        draft.geometryRevision = admission.geometryRevision();
        draft.nextResidentBrickMask = admission.residentBrickMask();
        draft.nextSourceSeedMask = admission.sourceSeedMask();
        draft.topologyDirtyMask = admission.residentBrickMask();
        draft.naturalTemperatureChanged = true;
        draft.naturalTemperatureC = admission.naturalTemperatureC();
    }

    private void collectResidency(ThermalInputBatch.PageResidencyUpdate[] updates) {
        for (ThermalInputBatch.PageResidencyUpdate update : updates) {
            WorkerPageStore.PageState page = page(update.page());
            if (page == null) {
                continue;
            }
            PageDraft draft = acquireDraft(page);
            if (draft.retirement || update.geometryRevision() < draft.geometryRevision) {
                continue;
            }
            long added = update.residentBrickMask() & ~draft.nextResidentBrickMask;
            long removed = draft.nextResidentBrickMask & ~update.residentBrickMask();
            draft.nextResidentBrickMask = update.residentBrickMask();
            draft.nextSourceSeedMask = update.sourceSeedMask();
            compareFullSignatures(draft, update.signatures());
            draft.nextSignatures = update.signatures();
            draft.dormantMaterials = update.dormantMaterials();
            draft.geometryRevision = update.geometryRevision();
            draft.topologyDirtyMask |= added | removed;
            while (removed != 0L) {
                int brick = Long.numberOfTrailingZeros(removed);
                draft.replace(brick, WorkerBrickTopology.EMPTY);
                sourceDirtySections.add(page.handle.sectionKey());
                removed &= removed - 1L;
            }
        }
    }

    private void collectRetirements(ThermalInputBatch batch) {
        for (ThermalInputBatch.PageRetirement retirement : batch.retirements()) {
            WorkerPageStore.PageState page = page(retirement.page());
            if (page == null) {
                continue;
            }
            PageDraft draft = acquireDraft(page);
            draft.retirement = true;
            view.stageHalo(retirement.page(), null);
            draft.topologyDirtyMask = page.residentBrickMask;
            sourceDirtySections.add(page.handle.sectionKey());
        }
    }

    private void collectGeometry(ResolvedGeometryBatch geometry) {
        for (int index = 0; index < geometry.size(); index++) {
            WorkerPageStore.PageState page = page(geometry.page(index));
            if (page == null) {
                continue;
            }
            PageDraft draft = acquireDraft(page);
            if (draft.retirement || geometry.geometryRevision(index) < draft.geometryRevision) {
                continue;
            }
            draft.geometryRevision = geometry.geometryRevision(index);
            if (geometry.kind(index) == ResolvedGeometryBatch.Kind.FULL_RESYNC_REQUIRED) {
                draft.finishSignatures(signatures);
                draft.topologyDirtyMask = 0L;
                PageSignatures next = geometry.fullPageSignatures(index);
                compareFullSignatures(draft, next);
                draft.nextSignatures = next;
                ThermalPageHandle.GeometryResyncReason reason =
                        geometry.geometryResyncReason(index);
                if (reason == ThermalPageHandle.GeometryResyncReason.SECTION_REPLACED) {
                    draft.resetMaterials = true;
                    draft.topologyDirtyMask |= draft.nextResidentBrickMask;
                }
                draft.resyncToken =
                        new ThermalPageHandle.GeometryResyncToken(
                                page.handle.sectionKey(),
                                page.handle.lifecycleGeneration(),
                                geometry.geometryRevision(index),
                                reason);
                continue;
            }
            int signatureId =
                    signatures.valid(geometry.signatureId(index))
                            ? geometry.signatureId(index)
                            : ThermalSignatureTable.UNRESOLVED;
            draft.setBlock(geometry.blockIndex(index), signatureId);
        }
        for (var halo : geometry.halos()) {
            WorkerPageStore.PageState page = page(halo.page());
            if (page == null) continue;
            PageDraft draft = acquireDraft(page);
            if (draft.retirement || halo.geometryRevision() < draft.geometryRevision) continue;
            view.stageHalo(halo.page(), halo.halo());
            draft.geometryRevision = halo.geometryRevision();
            draft.fragmentDirtyMask |= draft.nextResidentBrickMask;
        }
        var changes = geometry.materialChanges();
        for (int index = 0; index < changes.size(); index++) {
            WorkerPageStore.PageState page = page(changes.page(index));
            if (page == null) continue;
            PageDraft draft = acquireDraft(page);
            if (draft.retirement) continue;
            int brick = brickIndex(changes.blockIndex(index));
            draft.materialChanges = changes;
            if (draft.materialChangeIndexes[brick] == null)
                draft.materialChangeIndexes[brick] = new IntArrayList();
            draft.materialChangeIndexes[brick].add(index);
            draft.materialDirtyMask |= 1L << brick;
            draft.topologyDirtyMask |= 1L << brick;
        }
    }

    private void collectEnvironment(ThermalInputBatch.PageEnvironmentUpdate[] updates) {
        for (ThermalInputBatch.PageEnvironmentUpdate update : updates) {
            WorkerPageStore.PageState page = page(update.page());
            if (page == null) {
                continue;
            }
            PageDraft draft = acquireDraft(page);
            if (draft.retirement) {
                continue;
            }
            if (update.naturalTemperatureChanged()) {
                draft.naturalTemperatureChanged = true;
                draft.naturalTemperatureC = update.naturalTemperatureC();
            }
            for (int index = 0; index < update.skyColumns().length; index++) {
                int column = Short.toUnsignedInt(update.skyColumns()[index]);
                draft.setSkyColumn(column, update.firstExposedLocalY()[index]);
                int brick = (column & 15) >>> 2 | (column >>> 4) >>> 2 << 2 | 3 << 4;
                draft.fragmentDirtyMask |= 1L << brick;
            }
        }
    }

    private void finalizeSignatureCuts() {
        for (int index = 0; index < draftCount; index++) {
            draftPool.get(index).finishSignatures(signatures);
        }
    }

    private void collectLayoutDependencies() {
        // First freeze candidates, then mark replacements: no recursive expansion.
        layoutCandidates.clear();
        int originalDrafts = draftCount;
        for (int i = 0; i < originalDrafts; i++) {
            PageDraft draft = draftPool.get(i);
            long mask = draft.retirement ? draft.page.residentBrickMask : draft.topologyDirtyMask;
            while (mask != 0) {
                int b = Long.numberOfTrailingZeros(mask);
                mask &= mask - 1;
                int x = brickMinX(draft.page, b),
                        y = brickMinY(draft.page, b),
                        z = brickMinZ(draft.page, b);
                for (int axis = 0; axis < 3; axis++)
                    for (int d = -4; d <= 4; d += 8) {
                        int nx = x + (axis == 0 ? d : 0),
                                ny = y + (axis == 1 ? d : 0),
                                nz = z + (axis == 2 ? d : 0);
                        long key =
                                SectionPos.asLong(
                                        SectionPos.blockToSectionCoord(nx),
                                        SectionPos.blockToSectionCoord(ny),
                                        SectionPos.blockToSectionCoord(nz));
                        var page = view.page(key);
                        int nb = (nx & 15) >>> 2 | ((nz & 15) >>> 2) << 2 | ((ny & 15) >>> 2) << 4;
                        if (page != null && view.resident(page, nb))
                            layoutCandidates.add(page.fragmentIndex(nb));
                    }
            }
        }
        for (int index : layoutCandidates) {
            var page = view.pageSlot(index / 64);
            int brick = index % 64;
            var draft = acquireDraft(page);
            // Surface ownership can change an adjacent Brick's outward material
            // edge even when fixed body capacity and node membership do not.
            draft.fragmentDirtyMask |= 1L << brick;
        }
    }

    private void compileChangedCells(int draftIndex) {
        PageDraft draft = draftPool.get(draftIndex);
        if (draft.retirement) return;
        long remaining = draft.topologyDirtyMask;
        while (remaining != 0L) {
            int brick = Long.numberOfTrailingZeros(remaining);
            if (!view.resident(draft.page, brick)) {
                remaining &= remaining - 1L;
                continue;
            }
            WorkerBrickTopology next =
                    compiler.compileCells(draft.page, draft.nextSignatures, brick, view);
            draft.replace(brick, next);
            draft.cellReplacementMask |= 1L << brick;
            stagedCellCount = Math.addExact(stagedCellCount, next.span.count());
            sourceDirtySections.add(draft.page.handle.sectionKey());
            remaining &= remaining - 1L;
        }
    }

    private void collectFragmentDependencies() {
        for (int draftIndex = 0; draftIndex < draftCount; draftIndex++) {
            PageDraft draft = draftPool.get(draftIndex);
            long remaining = draft.topologyDirtyMask | draft.fragmentDirtyMask;
            long removed = draft.page.residentBrickMask & ~draft.nextResidentBrickMask;
            while (removed != 0L) {
                int brick = Long.numberOfTrailingZeros(removed);
                affectedFragments.add(draft.page.fragmentIndex(brick));
                removed &= removed - 1L;
            }
            if (draft.retirement) {
                remaining = draft.page.residentBrickMask;
            } else if (draft.replacedPage != null) {
                remaining |= draft.replacedPage.residentBrickMask;
                // Replacement may admit fewer Bricks. Their old solver fragments still
                // occupy this Page slot and must be removed along with the old cells.
                long replacedRemoved = draft.replacedPage.residentBrickMask & ~draft.nextResidentBrickMask;
                while (replacedRemoved != 0L) {
                    int brick = Long.numberOfTrailingZeros(replacedRemoved);
                    affectedFragments.add(draft.page.fragmentIndex(brick));
                    replacedRemoved &= replacedRemoved - 1L;
                }
            }
            while (remaining != 0L) {
                int brick = Long.numberOfTrailingZeros(remaining);
                markFragmentNeighborhood(draft.page, brick);
                remaining &= remaining - 1L;
            }
        }
    }

    private void markFragmentNeighborhood(WorkerPageStore.PageState page, int brick) {
        int minX = brickMinX(page, brick);
        int minY = brickMinY(page, brick);
        int minZ = brickMinZ(page, brick);
        markFragment(minX, minY, minZ);
        markFragment(minX - 4, minY, minZ);
        markFragment(minX + 4, minY, minZ);
        markFragment(minX, minY - 4, minZ);
        markFragment(minX, minY + 4, minZ);
        markFragment(minX, minY, minZ - 4);
        markFragment(minX, minY, minZ + 4);
    }

    private void markFragment(int minX, int minY, int minZ) {
        long sectionKey =
                SectionPos.asLong(
                        SectionPos.blockToSectionCoord(minX),
                        SectionPos.blockToSectionCoord(minY),
                        SectionPos.blockToSectionCoord(minZ));
        WorkerPageStore.PageState page = view.page(sectionKey);
        if (page == null) {
            page = pages.find(sectionKey);
            PageDraft draft = page == null ? null : draftsBySection.get(page.handle.sectionKey());
            if (draft == null || !draft.retirement) {
                return;
            }
        }
        int brick =
                Math.floorMod(minX, 16) >>> 2
                        | (Math.floorMod(minZ, 16) >>> 2) << 2
                        | (Math.floorMod(minY, 16) >>> 2) << 4;
        if (!view.resident(page, brick)) {
            return;
        }
        affectedFragments.add(page.fragmentIndex(brick));
    }

    private void compileFragments(FragmentChanges change, int first, int end) {
        int[] indexes = change.indexes;
        ThermalFragment[] fragments = change.fragments;
        for (int index = first; index < end; index++) {
            int fragmentIndex = indexes[index];
            int pageSlot = fragmentIndex / BRICKS_PER_PAGE;
            int brick = fragmentIndex % BRICKS_PER_PAGE;
            WorkerPageStore.PageState page = view.pageSlot(pageSlot);
            if (page == null) {
                page = pages.findPageSlot(pageSlot);
            }
            PageDraft draft = acquireDraft(page);
            if (draft.retirement || !view.resident(page, brick)) {
                fragments[index] = ThermalFragment.EMPTY;
                continue;
            }
            BrickTopologyCompiler.CompiledFragment compiled =
                    compiler.compileFragment(page, brick, view);
            fragments[index] = compiled.fragment();
            compiledFragmentBytes += compiled.fragment().ownedBackingBytes();
            WorkerBrickTopology base = view.brick(page, brick);
            if (compiled.layout() != null) base = base.withLayout(compiled.layout());
            sourceDirtySections.add(page.handle.sectionKey());
            WorkerBrickTopology next =
                    base.withFragmentResult(base.cellsResolved && compiled.resolved());
            if (base.resolved != next.resolved) sourceDirtySections.add(page.handle.sectionKey());
            draft.replace(brick, next);
            draft.fragmentChangedMask |= 1L << brick;
        }
    }

    private void prepareMigrationsAndRetirements(int draftIndex) {
        PageDraft draft = draftPool.get(draftIndex);
        if (!draft.retirement && draft.replacedPage == null) {
            long removed = draft.page.residentBrickMask & ~draft.nextResidentBrickMask;
            while (removed != 0L) {
                int brick = Long.numberOfTrailingZeros(removed);
                WorkerBrickTopology old = draft.page.brick(brick);
                collectOldSpan(draft.page, old);
                for (int slot : old.phaseSlots) removedPhaseSlots.add(slot);
                removed &= removed - 1L;
            }
        }
        if (draft.retirement || draft.replacedPage != null) {
            WorkerPageStore.PageState retired = draft.retirement ? draft.page : draft.replacedPage;
            // Recompiled Bricks migrate below. Old-only Bricks retire directly, without
            // manufacturing new cells or treating residency loss as a world block edit.
            long remaining = retired.residentBrickMask & (draft.retirement ? -1L : ~draft.cellReplacementMask);
            while (remaining != 0L) {
                int brick = Long.numberOfTrailingZeros(remaining);
                WorkerBrickTopology old = retired.brick(brick);
                collectOldSpan(retired, old);
                for (int slot : old.phaseSlots) {
                    removedPhaseSlots.add(slot);
                }
                remaining &= remaining - 1L;
            }
            if (draft.retirement) return;
        }
        WorkerPageStore.PageState previous =
                draft.replacedPage == null ? draft.page : draft.replacedPage;
        long remaining = draft.cellReplacementMask;
        while (remaining != 0L) {
            int brick = Long.numberOfTrailingZeros(remaining);
            WorkerBrickTopology old = previous.brick(brick);
            WorkerBrickTopology next = draft.replacements[brick];
            migration.migrate(
                    previous,
                    brick,
                    old,
                    next,
                    draft.nextSignatures,
                    draft.replacedPage == null,
                    draft.materialChanges,
                    draft.materialChangeIndexes[brick],
                    draft.dormantMaterials,
                    draft.naturalTemperatureC,
                    draft.resetMaterials);
            externalMaterialEnergyJ += migration.externalMaterialEnergyJ;
            collectOldSpan(previous, old);
            collectPhaseSlotChanges(old, next);
            remaining &= remaining - 1L;
        }
    }

    private void collectPhaseSlotChanges(WorkerBrickTopology old, WorkerBrickTopology next) {
        for (int slot : old.phaseSlots) {
            removedPhaseSlots.add(slot);
        }
        for (int slot : next.phaseSlots) {
            addedPhaseSlots.add(slot);
        }
    }

    private void collectOldSpan(WorkerPageStore.PageState page, WorkerBrickTopology old) {
        if (old.span.count() != 0) {
            oldSpans.add(
                    new PreparedTopologyChange.OldSpan(
                            page.pageSlot, page.lifecycleGeneration, old.span));
        }
    }

    private void preflightLimits(FragmentChanges changes) {
        ArenaSpan[] retiringSpans = new ArenaSpan[oldSpans.size()];
        for (int index = 0; index < oldSpans.size(); index++) {
            retiringSpans[index] = oldSpans.get(index).span();
        }
        ThermalSolver.ProjectedWork work =
                solver.preflightReplacement(
                        changes.indexes,
                        changes.fragments,
                        retiringSpans);
        int finalLiveCells = Math.addExact(arena.liveCellCount(), stagedCellCount);
        for (PreparedTopologyChange.OldSpan old : oldSpans) {
            finalLiveCells -= old.span().count();
        }
        if (arena.requiredSlotCapacity() > limits.maximumArenaSlots()
                || finalLiveCells > limits.maximumLiveCells()
                || work.pairOperations() > limits.maximumPairOperations()
                || work.boundaryOperations() > limits.maximumBoundaryOperations()) {
            int visibleAir = 0, visibleMaterials = 0, retiringCells = 0;
            for (int slot = arena.nextLiveSlot(0); slot >= 0; slot = arena.nextLiveSlot(slot + 1)) {
                if (arena.isAirCell(slot)) visibleAir++; else visibleMaterials++;
            }
            for (var old : oldSpans) retiringCells += old.span().count();
            throw new WorkLimitedException("thermal topology work limit reached: slots="
                    + arena.requiredSlotCapacity() + "/" + limits.maximumArenaSlots()
                    + ", liveCells=" + finalLiveCells + "/" + limits.maximumLiveCells()
                    + ", liveBefore=" + arena.liveCellCount() + ", staged=" + stagedCellCount
                    + ", retiring=" + retiringCells + ", visibleAir=" + visibleAir
                    + ", visibleMaterials=" + visibleMaterials
                    + ", pairs=" + work.pairOperations() + "/" + limits.maximumPairOperations()
                    + ", boundaries=" + work.boundaryOperations() + "/" + limits.maximumBoundaryOperations());
        }
    }

    private void reserveBacking() {
        int fragmentCapacity = Math.multiplyExact(pages.pageSlotCapacity(), BRICKS_PER_PAGE);
        solver.reserveTopologyCapacity(
                fragmentCapacity, arena.requiredSlotCapacity(), pages.pageSlotCapacity());
        phases.reserveMaterialChanges(addedPhaseSlots.size());
    }

    private void compareFullSignatures(PageDraft draft, PageSignatures next) {
        long signatureChanged = 0L;
        long remaining = draft.nextResidentBrickMask;
        while (remaining != 0L) {
            int brick = Long.numberOfTrailingZeros(remaining);
            int minX = (brick & 3) << 2;
            int minZ = (brick >>> 2 & 3) << 2;
            int minY = (brick >>> 4 & 3) << 2;
            brickScan:
            for (int y = minY; y < minY + 4; y++) {
                for (int z = minZ; z < minZ + 4; z++) {
                    for (int x = minX; x < minX + 4; x++) {
                        int block = x | z << 4 | y << 8;
                        if (draft.nextSignatures.get(block) != next.get(block)) {
                            signatureChanged |= 1L << brick;
                            break brickScan;
                        }
                    }
                }
            }
            remaining &= remaining - 1L;
        }
        draft.topologyDirtyMask |= signatureChanged;
        draft.signatureChangedMask |= signatureChanged;
    }

    private WorkerPageStore.PageState page(ThermalPageHandle handle) {
        PageDraft draft = draftsBySection.get(handle.sectionKey());
        if (draft != null) {
            if (draft.page.handle == handle) {
                return draft.page;
            }
            if (draft.replacedPage != null && draft.replacedPage.handle == handle) {
                return null;
            }
        }
        return pages.find(handle);
    }

    private static int brickIndex(int blockIndex) {
        return (blockIndex & 15) >>> 2
                | (blockIndex >>> 4 & 15) >>> 2 << 2
                | (blockIndex >>> 8 & 15) >>> 2 << 4;
    }

    private static int indexWithinBrick(int blockIndex) {
        return blockIndex & 3 | (blockIndex >>> 4 & 3) << 2 | (blockIndex >>> 8 & 3) << 4;
    }

    private static int brickMinX(WorkerPageStore.PageState page, int brick) {
        return SectionPos.sectionToBlockCoord(SectionPos.x(page.handle.sectionKey()))
                + ((brick & 3) << 2);
    }

    private static int brickMinY(WorkerPageStore.PageState page, int brick) {
        return SectionPos.sectionToBlockCoord(SectionPos.y(page.handle.sectionKey()))
                + ((brick >>> 4 & 3) << 2);
    }

    private static int brickMinZ(WorkerPageStore.PageState page, int brick) {
        return SectionPos.sectionToBlockCoord(SectionPos.z(page.handle.sectionKey()))
                + ((brick >>> 2 & 3) << 2);
    }

    static final class PageDraft {
        WorkerPageStore.PageState page;
        WorkerPageStore.PageState replacedPage;
        PageSignatures nextSignatures;
        private final int[][] brickSignatureValues = new int[64][];
        final WorkerBrickTopology[] replacements = new WorkerBrickTopology[BRICKS_PER_PAGE];
        short[] skyColumns = new short[8];
        byte[] skyValues = new byte[8];
        private byte[] nextSkyExposure;
        int skyCount;
        boolean admission;
        boolean retirement;
        boolean naturalTemperatureChanged;
        double naturalTemperatureC;
        long geometryRevision;
        long nextResidentBrickMask;
        long nextSourceSeedMask;
        private long signatureScratchMask;
        long signatureChangedMask;
        private long topologyDirtyMask;
        private long fragmentDirtyMask;
        long cellReplacementMask;
        long fragmentChangedMask;
        long replacementMask;
        ThermalPageHandle.GeometryResyncToken resyncToken;
        ResolvedGeometryBatch.MaterialChanges materialChanges =
                ResolvedGeometryBatch.MaterialChanges.EMPTY;
        final IntArrayList[] materialChangeIndexes = new IntArrayList[64];
        long materialDirtyMask;
        boolean resetMaterials;
        ThermalInputBatch.DormantMaterialCut dormantMaterials;
        private boolean backingDirty = true;
        private long backingBytes;

        /** A Page is small and fixed at 64 Bricks; refresh only when its draft changes. */
        private long preparationBackingBytes() {
            if (!backingDirty) return backingBytes;
            long bytes = 256L + AirBackingMeter.shallowArrayBytes(brickSignatureValues)
                    + AirBackingMeter.shallowArrayBytes(replacements) + AirBackingMeter.shallowArrayBytes(materialChangeIndexes)
                    + AirBackingMeter.shallowArrayBytes(skyColumns) + AirBackingMeter.shallowArrayBytes(skyValues)
                    + AirBackingMeter.shallowArrayBytes(nextSkyExposure) + AirBackingMeter.arrayPayloadBytes(64, 8);
            for (int brick = 0; brick < 64; brick++) {
                bytes += AirBackingMeter.shallowArrayBytes(brickSignatureValues[brick]);
                Object signature = nextSignatures.brickPayload(brick);
                if (signature instanceof char[] || signature instanceof int[]) bytes += AirBackingMeter.shallowArrayBytes(signature);
                if (materialChangeIndexes[brick] != null) bytes += AirBackingMeter.shallowArrayBytes(materialChangeIndexes[brick].elements());
                if (replacements[brick] != null) bytes += replacements[brick].ownedBackingBytes();
            }
            backingDirty = false;
            return backingBytes = bytes;
        }

        private void reset(WorkerPageStore.PageState page) {
            backingDirty = true;
            while (materialDirtyMask != 0) {
                int brick = Long.numberOfTrailingZeros(materialDirtyMask);
                materialChangeIndexes[brick].clear();
                materialDirtyMask &= materialDirtyMask - 1;
            }
            materialChanges = ResolvedGeometryBatch.MaterialChanges.EMPTY;
            resetMaterials = false;
            dormantMaterials = page.dormantMaterials;
            this.page = page;
            replacedPage = null;
            nextSignatures = page.signatures;
            long staleReplacements = replacementMask;
            while (staleReplacements != 0L) {
                int brick = Long.numberOfTrailingZeros(staleReplacements);
                replacements[brick] = null;
                staleReplacements &= staleReplacements - 1L;
            }
            skyCount = 0;
            admission = false;
            retirement = false;
            naturalTemperatureChanged = false;
            naturalTemperatureC = page.naturalTemperatureC;
            geometryRevision = page.geometryRevision;
            nextResidentBrickMask = page.residentBrickMask;
            nextSourceSeedMask = page.sourceSeedMask;
            signatureScratchMask = 0L;
            signatureChangedMask = 0L;
            topologyDirtyMask = 0L;
            fragmentDirtyMask = 0L;
            cellReplacementMask = 0L;
            fragmentChangedMask = 0L;
            replacementMask = 0L;
            resyncToken = null;
            nextSkyExposure = null;
        }

        private void replace(int brick, WorkerBrickTopology replacement) {
            backingDirty = true;
            replacements[brick] = replacement;
            replacementMask |= 1L << brick;
        }

        private void setBlock(int block, int signatureId) {
            backingDirty = true;
            int brick = brickIndex(block);
            int within = indexWithinBrick(block);
            int[] values = brickSignatureValues[brick];
            int previous =
                    (signatureScratchMask & 1L << brick) == 0L
                            ? nextSignatures.get(block)
                            : values[within];
            if (previous == signatureId) {
                return;
            }
            if ((signatureScratchMask & 1L << brick) == 0L) {
                if (values == null) {
                    values = new int[64];
                    brickSignatureValues[brick] = values;
                }
                for (int index = 0; index < 64; index++) {
                    int localX = ((brick & 3) << 2) + (index & 3);
                    int localZ = ((brick >>> 2 & 3) << 2) + (index >>> 2 & 3);
                    int localY = ((brick >>> 4 & 3) << 2) + (index >>> 4 & 3);
                    values[index] = nextSignatures.get(localX | localZ << 4 | localY << 8);
                }
                signatureScratchMask |= 1L << brick;
            }
            values[within] = signatureId;
            signatureChangedMask |= 1L << brick;
            topologyDirtyMask |= 1L << brick;
        }

        private void finishSignatures(ThermalSignatureTable signatures) {
            backingDirty = true;
            int count = Long.bitCount(signatureScratchMask);
            if (count == 0) {
                return;
            }
            int[] indexes = new int[count];
            int[][] values = new int[count][];
            int write = 0;
            long remaining = signatureScratchMask;
            while (remaining != 0L) {
                int brick = Long.numberOfTrailingZeros(remaining);
                indexes[write] = brick;
                values[write] = brickSignatureValues[brick];
                write++;
                remaining &= remaining - 1L;
            }
            nextSignatures = nextSignatures.withBricks(signatures, indexes, values);
            signatureScratchMask = 0L;
        }

        private void setSkyColumn(int column, byte value) {
            backingDirty = true;
            if (nextSkyExposure == null) {
                nextSkyExposure = page.firstExposedLocalY.clone();
            }
            nextSkyExposure[column] = value;
            // Separate input batches can reach a lower column after a higher one.
            // Keep the prepared write ordered while the newest value wins per column.
            int index = Arrays.binarySearch(skyColumns, 0, skyCount, (short) column);
            if (index >= 0) {
                skyValues[index] = value;
                return;
            }
            index = -index - 1;
            if (skyCount == skyColumns.length) {
                int capacity = skyColumns.length + Math.max(8, skyColumns.length >>> 1);
                skyColumns = Arrays.copyOf(skyColumns, capacity);
                skyValues = Arrays.copyOf(skyValues, capacity);
            }
            System.arraycopy(skyColumns, index, skyColumns, index + 1, skyCount - index);
            System.arraycopy(skyValues, index, skyValues, index + 1, skyCount - index);
            skyColumns[index] = (short) column;
            skyValues[index] = value;
            skyCount++;
        }

        byte[] nextSkyExposure() {
            return nextSkyExposure == null ? page.firstExposedLocalY : nextSkyExposure;
        }
    }

    private record FragmentChanges(int[] indexes, ThermalFragment[] fragments) {
        private static final FragmentChanges EMPTY =
                new FragmentChanges(
                        PreparedTopologyChange.NO_INTS, PreparedTopologyChange.NO_FRAGMENTS);
    }

    public static final class WorkLimitedException extends RuntimeException {
        WorkLimitedException(String message) {
            super(message);
        }
    }
}
