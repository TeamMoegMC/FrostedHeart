/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ArenaSpan;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalFragment;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSolver;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.SectionPos;

import java.util.ArrayList;
import java.util.Arrays;

/** Reusable fallible worker-local planning and sparse preparation stage. */
final class TopologyPlan {
    private static final int BLOCKS_PER_PAGE = 4096;
    private static final int BRICKS_PER_PAGE = 64;

    private final WorkerPageStore pages;
    private final ThermalCellArena arena;
    private final ThermalSolver solver;
    private final PhaseTransitionRuntime phases;
    private final ThermalSignatureCatalog signatures;
    private final BrickTopologyCompiler compiler;
    private final MaterialEdgeCompiler materialCompiler;
    private final BrickMigrationKernel migration;
    private final ThermalDimensionLimits limits;
    private final QueryPublication queries;
    private final PreparedTopologyChange.Builder changeBuilder =
            new PreparedTopologyChange.Builder();

    private final ArrayList<PageDraft> draftPool = new ArrayList<>();
    private final Long2ObjectOpenHashMap<PageDraft> draftsBySection =
            new Long2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<PageDraft> draftsBySlot =
            new Int2ObjectOpenHashMap<>();
    private final IntOpenHashSet affectedFragments = new IntOpenHashSet();
    private final LongOpenHashSet sourceDirtySections = new LongOpenHashSet();
    private final IntArrayList removedReservoirs = new IntArrayList();
    private final IntArrayList addedReservoirs = new IntArrayList();
    private final ArrayList<PreparedTopologyChange.OldSpan> oldSpans =
            new ArrayList<>();
    private final ArrayList<ThermalPageHandle.GeometryResyncToken> resyncTokens =
            new ArrayList<>();
    private int draftCount;
    private int stagedAdmissions;
    private int stagedCellCount;
    private final TopologyView view;

    TopologyPlan(
            WorkerPageStore pages,
            ThermalCellArena arena,
            ThermalSolver solver,
            PhaseTransitionRuntime phases,
            ThermalSignatureCatalog signatures,
            BrickTopologyCompiler compiler,
            ThermalTopologyParameters parameters,
            ThermalDimensionLimits limits,
            QueryPublication queries
    ) {
        this.pages = pages;
        this.arena = arena;
        this.solver = solver;
        this.phases = phases;
        this.signatures = signatures;
        this.compiler = compiler;
        this.materialCompiler = new MaterialEdgeCompiler(arena, solver);
        this.migration = new BrickMigrationKernel(
                arena, signatures, parameters);
        this.limits = limits;
        this.queries = queries;
        this.view = new TopologyView(
                pages, signatures, draftsBySection, draftsBySlot);
    }

    PreparedTopologyChange prepare(ThermalInputBatch batch) {
        reset();
        try {
            collectAdmissions(batch);
            collectRetirements(batch);
            collectGeometry(batch.geometry());
            collectEnvironment(batch.environmentUpdates());
            finalizeSignatureCuts();
            compileChangedCells();
            collectFragmentDependencies();
            FragmentChanges fragmentChanges = compileFragments();
            prepareMigrationsAndRetirements();
            MaterialEdgeCompiler.Result material = materialCompiler.compile(
                    fragmentChanges.indexes, fragmentChanges.fragments);
            preflightLimits(fragmentChanges, material);
            reserveBacking(fragmentChanges, material);
            PreparedTopologyChange.PageWrite[] pageWrites = pages.prepareWrites(
                    draftPool,
                    draftCount,
                    resyncTokens);
            long baseVersion = solver.structuralVersion();
            boolean structural = fragmentChanges.indexes.length != 0
                    || !oldSpans.isEmpty();
            long nextVersion = structural
                    ? Math.incrementExact(baseVersion)
                    : baseVersion;
            long[] dirtySections = sourceDirtySections.isEmpty()
                    ? PreparedTopologyChange.NO_LONGS
                    : sourceDirtySections.toLongArray();
            if (dirtySections.length > 1) {
                Arrays.sort(dirtySections);
            }
            return changeBuilder.identity(baseVersion, nextVersion)
                    .fragments(
                            fragmentChanges.indexes,
                            fragmentChanges.fragments)
                    .material(
                            material.keys(),
                            material.edges(),
                            material.executionFragments(),
                            material.executions())
                    .reservoirs(
                            removedReservoirs.isEmpty()
                                    ? PreparedTopologyChange.NO_INTS
                                    : removedReservoirs.toIntArray(),
                            addedReservoirs.isEmpty()
                                    ? PreparedTopologyChange.NO_INTS
                                    : addedReservoirs.toIntArray())
                    .pages(
                            pageWrites,
                            oldSpans.isEmpty()
                                    ? PreparedTopologyChange.NO_OLD_SPANS
                                    : oldSpans.toArray(
                                            PreparedTopologyChange.OldSpan[]::new))
                    .sourceSections(dirtySections)
                    .resyncTokens(
                            resyncTokens.isEmpty()
                                    ? ThermalCompletion.NO_RESYNC_TOKENS
                                    : resyncTokens.toArray(
                                            ThermalPageHandle.GeometryResyncToken[]::new))
                    .build();
        } catch (RuntimeException | Error failure) {
            try {
                discardStaging();
            } catch (Throwable cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
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
                if (staged != null && staged.span.count() != 0) {
                    arena.discardStagedCells(staged.span);
                }
                remaining &= remaining - 1L;
            }
            if (draft.admission) {
                pages.releaseStagedAdmission(draft.page);
            }
        }
    }

    private void reset() {
        draftsBySection.clear();
        draftsBySlot.clear();
        affectedFragments.clear();
        sourceDirtySections.clear();
        removedReservoirs.clear();
        addedReservoirs.clear();
        oldSpans.clear();
        resyncTokens.clear();
        draftCount = 0;
        stagedAdmissions = 0;
        stagedCellCount = 0;
    }

    private PageDraft acquireDraft(WorkerPageStore.PageState page) {
        PageDraft existing = draftsBySection.get(page.handle.sectionKey());
        if (existing != null) {
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
            PageDraft alreadyStaged =
                    draftsBySection.get(admission.page().sectionKey());
            if (alreadyStaged != null) {
                continue;
            }
            WorkerPageStore.PageState page = pages.find(admission.page());
            boolean staged = page == null;
            if (staged) {
                if (pages.activePageCount() + stagedAdmissions
                        >= limits.maximumPages()) {
                    throw new WorkLimitedException("thermal Page limit reached");
                }
                page = pages.stageAdmission(admission);
                stagedAdmissions++;
            }
            PageDraft draft = acquireDraft(page);
            if (staged) {
                draft.admission = true;
                draft.nextSignatures = admission.signatures();
                draft.geometryRevision = admission.geometryRevision();
                draft.signatureChangedMask = -1L;
                draft.topologyDirtyMask = -1L;
                draft.naturalTemperatureChanged = true;
                draft.naturalTemperatureC = admission.naturalTemperatureC();
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
            draft.topologyDirtyMask = -1L;
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
            if (draft.retirement
                    || geometry.geometryRevision(index) < draft.geometryRevision) {
                continue;
            }
            draft.geometryRevision = geometry.geometryRevision(index);
            if (geometry.kind(index)
                    == ResolvedGeometryBatch.Kind.FULL_RESYNC_REQUIRED) {
                draft.finishSignatures();
                draft.signatureChangedMask = 0L;
                draft.topologyDirtyMask = 0L;
                PageSignatures next = geometry.fullPageSignatures(index);
                compareFullSignatures(draft, next);
                draft.nextSignatures = next;
                ThermalPageHandle.GeometryResyncReason reason =
                        geometry.geometryResyncReason(index);
                draft.resyncToken = new ThermalPageHandle.GeometryResyncToken(
                        page.handle.sectionKey(),
                        page.handle.lifecycleGeneration(),
                        geometry.geometryRevision(index),
                        reason);
                continue;
            }
            int signatureId = signatures.valid(geometry.signatureId(index))
                    ? geometry.signatureId(index)
                    : ThermalSignatureCatalog.UNRESOLVED;
            draft.setBlock(
                    geometry.blockIndex(index),
                    signatureId,
                    signatures);
        }
    }

    private void collectEnvironment(
            ThermalInputBatch.PageEnvironmentUpdate[] updates
    ) {
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
                draft.setSkyColumn(
                        column, update.firstExposedLocalY()[index]);
                int brick = (column & 15) >>> 2
                        | (column >>> 4) >>> 2 << 2
                        | 3 << 4;
                draft.fragmentDirtyMask |= 1L << brick;
            }
        }
    }

    private void finalizeSignatureCuts() {
        for (int index = 0; index < draftCount; index++) {
            draftPool.get(index).finishSignatures();
        }
    }

    private void compileChangedCells() {
        for (int draftIndex = 0; draftIndex < draftCount; draftIndex++) {
            PageDraft draft = draftPool.get(draftIndex);
            if (draft.retirement) {
                continue;
            }
            long remaining = draft.topologyDirtyMask;
            while (remaining != 0L) {
                int brick = Long.numberOfTrailingZeros(remaining);
                WorkerBrickTopology next = compiler.compileCells(
                        draft.page,
                        draft.nextSignatures,
                        brick,
                        view);
                draft.replace(brick, next);
                draft.cellReplacementMask |= 1L << brick;
                stagedCellCount = Math.addExact(
                        stagedCellCount, next.span.count());
                sourceDirtySections.add(draft.page.handle.sectionKey());
                remaining &= remaining - 1L;
            }
        }
    }

    private void collectFragmentDependencies() {
        for (int draftIndex = 0; draftIndex < draftCount; draftIndex++) {
            PageDraft draft = draftPool.get(draftIndex);
            long remaining = draft.topologyDirtyMask
                    | draft.fragmentDirtyMask;
            if (draft.retirement) {
                remaining = -1L;
            }
            while (remaining != 0L) {
                int brick = Long.numberOfTrailingZeros(remaining);
                markFragmentNeighborhood(draft.page, brick);
                remaining &= remaining - 1L;
            }
        }
    }

    private void markFragmentNeighborhood(
            WorkerPageStore.PageState page,
            int brick
    ) {
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
        long sectionKey = SectionPos.asLong(
                SectionPos.blockToSectionCoord(minX),
                SectionPos.blockToSectionCoord(minY),
                SectionPos.blockToSectionCoord(minZ));
        WorkerPageStore.PageState page = view.page(sectionKey);
        if (page == null) {
            page = pages.find(sectionKey);
            PageDraft draft = page == null
                    ? null : draftsBySection.get(page.handle.sectionKey());
            if (draft == null || !draft.retirement) {
                return;
            }
        }
        int brick = Math.floorMod(minX, 16) >>> 2
                | (Math.floorMod(minZ, 16) >>> 2) << 2
                | (Math.floorMod(minY, 16) >>> 2) << 4;
        affectedFragments.add(page.fragmentIndex(brick));
    }

    private FragmentChanges compileFragments() {
        if (affectedFragments.isEmpty()) {
            return FragmentChanges.EMPTY;
        }
        int[] indexes = affectedFragments.toIntArray();
        Arrays.sort(indexes);
        ThermalFragment[] fragments = new ThermalFragment[indexes.length];
        for (int index = 0; index < indexes.length; index++) {
            int fragmentIndex = indexes[index];
            int pageSlot = fragmentIndex / BRICKS_PER_PAGE;
            int brick = fragmentIndex % BRICKS_PER_PAGE;
            WorkerPageStore.PageState page = view.pageSlot(pageSlot);
            if (page == null) {
                page = pages.findPageSlot(pageSlot);
            }
            PageDraft draft = acquireDraft(page);
            if (draft.retirement) {
                fragments[index] = ThermalFragment.EMPTY;
                continue;
            }
            BrickTopologyCompiler.CompiledFragment compiled =
                    compiler.compileFragment(page, brick, view);
            fragments[index] = compiled.fragment();
            WorkerBrickTopology base = view.brick(page, brick);
            WorkerBrickTopology next = base.withFragmentResult(
                    base.resolved && compiled.resolved(),
                    compiled.continuationFaceMask());
            draft.replace(brick, next);
            draft.fragmentChangedMask |= 1L << brick;
        }
        return new FragmentChanges(indexes, fragments);
    }

    private void prepareMigrationsAndRetirements() {
        for (int draftIndex = 0; draftIndex < draftCount; draftIndex++) {
            PageDraft draft = draftPool.get(draftIndex);
            if (draft.retirement) {
                for (int brick = 0; brick < BRICKS_PER_PAGE; brick++) {
                    WorkerBrickTopology old = draft.page.brick(brick);
                    collectOldSpan(draft.page, old);
                    for (int slot : old.phaseReservoirs.slot()) {
                        removedReservoirs.add(slot);
                    }
                }
                continue;
            }
            long remaining = draft.cellReplacementMask;
            while (remaining != 0L) {
                int brick = Long.numberOfTrailingZeros(remaining);
                WorkerBrickTopology old = draft.page.brick(brick);
                WorkerBrickTopology next = draft.replacements[brick];
                migration.migrate(
                        draft.page,
                        brick,
                        old,
                        next,
                        draft.nextSignatures);
                collectOldSpan(draft.page, old);
                collectReservoirChanges(old, next);
                remaining &= remaining - 1L;
            }
        }
    }

    private void collectReservoirChanges(
            WorkerBrickTopology old,
            WorkerBrickTopology next
    ) {
        for (int slot : old.phaseReservoirs.slot()) {
            removedReservoirs.add(slot);
        }
        for (int slot : next.phaseReservoirs.slot()) {
            addedReservoirs.add(slot);
        }
    }

    private void collectOldSpan(
            WorkerPageStore.PageState page,
            WorkerBrickTopology old
    ) {
        if (old.span.count() != 0) {
            oldSpans.add(new PreparedTopologyChange.OldSpan(
                    page.pageSlot,
                    page.lifecycleGeneration,
                    old.span));
        }
    }

    private void preflightLimits(
            FragmentChanges changes,
            MaterialEdgeCompiler.Result material
    ) {
        ArenaSpan[] retiringSpans = new ArenaSpan[oldSpans.size()];
        for (int index = 0; index < oldSpans.size(); index++) {
            retiringSpans[index] = oldSpans.get(index).span();
        }
        ThermalSolver.ProjectedWork work = solver.preflightReplacement(
                changes.indexes,
                changes.fragments,
                retiringSpans,
                material.expectedFinalSize());
        int finalLiveCells = Math.addExact(
                arena.liveCellCount(), stagedCellCount);
        for (PreparedTopologyChange.OldSpan old : oldSpans) {
            finalLiveCells -= old.span().count();
        }
        if (arena.requiredSlotCapacity() > limits.maximumArenaSlots()
                || finalLiveCells > limits.maximumLiveCells()
                || work.pairOperations() > limits.maximumPairOperations()
                || work.boundaryOperations()
                        > limits.maximumBoundaryOperations()
                || work.phaseOperations() > limits.maximumPhaseOperations()) {
            throw new WorkLimitedException("thermal topology work limit reached");
        }
    }

    private void reserveBacking(
            FragmentChanges fragments,
            MaterialEdgeCompiler.Result material
    ) {
        int fragmentCapacity = Math.multiplyExact(
                pages.pageSlotCapacity(), BRICKS_PER_PAGE);
        solver.reserveTopologyCapacity(
                fragmentCapacity,
                arena.requiredSlotCapacity(),
                pages.pageSlotCapacity());
        solver.reserveMaterialEdgeChanges(
                material.expectedFinalSize(),
                material.possibleInsertions());
        phases.reserveReservoirChanges(addedReservoirs.size());
        if (!queries.tryEnsureCapacity(
                arena.requiredSlotCapacity(), limits.maximumArenaSlots())) {
            throw new WorkLimitedException(
                    "thermal query publication memory was refused");
        }
    }

    private void compareFullSignatures(
            PageDraft draft,
            PageSignatures next
    ) {
        long signatureChanged = 0L;
        long topologyChanged = 0L;
        for (int block = 0; block < BLOCKS_PER_PAGE; block++) {
            int previous = draft.nextSignatures.get(block);
            int replacement = next.get(block);
            if (previous == replacement) {
                continue;
            }
            int brick = brickIndex(block);
            signatureChanged |= 1L << brick;
            if (!signatures.topologyEquivalent(previous, replacement)) {
                topologyChanged |= 1L << brick;
            }
        }
        draft.signatureChangedMask |= signatureChanged;
        draft.topologyDirtyMask |= topologyChanged;
    }

    private WorkerPageStore.PageState page(ThermalPageHandle handle) {
        PageDraft draft = draftsBySection.get(handle.sectionKey());
        if (draft != null && draft.page.handle == handle) {
            return draft.page;
        }
        return pages.find(handle);
    }

    private static int brickIndex(int blockIndex) {
        return (blockIndex & 15) >>> 2
                | (blockIndex >>> 4 & 15) >>> 2 << 2
                | (blockIndex >>> 8 & 15) >>> 2 << 4;
    }

    private static int indexWithinBrick(int blockIndex) {
        return blockIndex & 3
                | (blockIndex >>> 4 & 3) << 2
                | (blockIndex >>> 8 & 3) << 4;
    }

    private static int brickMinX(
            WorkerPageStore.PageState page,
            int brick
    ) {
        return SectionPos.sectionToBlockCoord(
                SectionPos.x(page.handle.sectionKey()))
                + ((brick & 3) << 2);
    }

    private static int brickMinY(
            WorkerPageStore.PageState page,
            int brick
    ) {
        return SectionPos.sectionToBlockCoord(
                SectionPos.y(page.handle.sectionKey()))
                + ((brick >>> 4 & 3) << 2);
    }

    private static int brickMinZ(
            WorkerPageStore.PageState page,
            int brick
    ) {
        return SectionPos.sectionToBlockCoord(
                SectionPos.z(page.handle.sectionKey()))
                + ((brick >>> 2 & 3) << 2);
    }

    static final class PageDraft {
        WorkerPageStore.PageState page;
        PageSignatures nextSignatures;
        private final int[][] brickSignatureValues = new int[64][];
        final WorkerBrickTopology[] replacements =
                new WorkerBrickTopology[BRICKS_PER_PAGE];
        short[] skyColumns = new short[8];
        byte[] skyValues = new byte[8];
        private byte[] nextSkyExposure;
        int skyCount;
        boolean admission;
        boolean retirement;
        boolean naturalTemperatureChanged;
        double naturalTemperatureC;
        long geometryRevision;
        private long signatureScratchMask;
        long signatureChangedMask;
        private long topologyDirtyMask;
        private long fragmentDirtyMask;
        long cellReplacementMask;
        long fragmentChangedMask;
        long replacementMask;
        ThermalPageHandle.GeometryResyncToken resyncToken;

        private void reset(WorkerPageStore.PageState page) {
            this.page = page;
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
            replacements[brick] = replacement;
            replacementMask |= 1L << brick;
        }

        private void setBlock(
                int block,
                int signatureId,
                ThermalSignatureCatalog catalog
        ) {
            int brick = brickIndex(block);
            int within = indexWithinBrick(block);
            int[] values = brickSignatureValues[brick];
            int previous = (signatureScratchMask & 1L << brick) == 0L
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
                    int localZ = ((brick >>> 2 & 3) << 2)
                            + (index >>> 2 & 3);
                    int localY = ((brick >>> 4 & 3) << 2)
                            + (index >>> 4 & 3);
                    values[index] = nextSignatures.get(
                            localX | localZ << 4 | localY << 8);
                }
                signatureScratchMask |= 1L << brick;
            }
            values[within] = signatureId;
            signatureChangedMask |= 1L << brick;
            if (!catalog.topologyEquivalent(previous, signatureId)) {
                topologyDirtyMask |= 1L << brick;
            }
        }

        private void finishSignatures() {
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
            nextSignatures = nextSignatures.withBricks(indexes, values);
            signatureScratchMask = 0L;
        }

        private void setSkyColumn(int column, byte value) {
            if (nextSkyExposure == null) {
                nextSkyExposure = page.firstExposedLocalY.clone();
            }
            nextSkyExposure[column] = value;
            for (int index = 0; index < skyCount; index++) {
                if (Short.toUnsignedInt(skyColumns[index]) == column) {
                    skyValues[index] = value;
                    return;
                }
            }
            if (skyCount == skyColumns.length) {
                int capacity = skyColumns.length
                        + Math.max(8, skyColumns.length >>> 1);
                skyColumns = Arrays.copyOf(skyColumns, capacity);
                skyValues = Arrays.copyOf(skyValues, capacity);
            }
            skyColumns[skyCount] = (short) column;
            skyValues[skyCount] = value;
            skyCount++;
        }

        byte[] nextSkyExposure() {
            return nextSkyExposure == null
                    ? page.firstExposedLocalY
                    : nextSkyExposure;
        }

    }

    private record FragmentChanges(
            int[] indexes,
            ThermalFragment[] fragments
    ) {
        private static final FragmentChanges EMPTY = new FragmentChanges(
                PreparedTopologyChange.NO_INTS,
                PreparedTopologyChange.NO_FRAGMENTS);
    }

    static final class WorkLimitedException extends RuntimeException {
        WorkLimitedException(String message) {
            super(message);
        }
    }
}
