/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.geometry.ComponentBrickCompiler;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.ConservativeAirGeometry;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.GeometryDeltaRing;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.GeometrySummary;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.GeometrySummaryCache;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ArenaSpan;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.GeometryMigrationLedger;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ImplicitAirAdjacency;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPage;
import com.teammoeg.frostedheart.content.climate.thermal.profile.LocalAirRegionPattern;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalResolution;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.DimensionThermalRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.solver.BuoyancyConductance;
import com.teammoeg.frostedheart.content.climate.thermal.solver.SealedInputFrame;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSweep;
import com.teammoeg.frostedheart.content.climate.thermal.source.SourceBinding;
import net.minecraft.core.SectionPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Concrete logical-writer bridge from PR8 primitive geometry inputs to the
 * authoritative arena and one replacement sweep. It is opt-in shadow code;
 * normal gameplay never constructs it.
 */
public final class MinecraftThermalTopologyApplier {
    private static final int BLOCKS_PER_PAGE = 16 * 16 * 16;
    private static final int MICROCELLS_PER_BLOCK = ConservativeAirGeometry.MICROCELL_COUNT;
    private static final int INITIAL_ALL_AIR = -2;
    private static final int UNRESOLVED_SIGNATURE = -1;
    private static final long FULL_MICROCELL_MASK = -1L;
    private static final ConservativeAirGeometry.Resolution FULL_AIR =
            new ConservativeAirGeometry.Resolution(
                    ConservativeAirGeometry.Status.RESOLVED,
                    ConservativeAirGeometry.UnsupportedReason.NONE,
                    List.of(new ConservativeAirGeometry.AirComponent(
                            0,
                            FULL_MICROCELL_MASK,
                            MICROCELLS_PER_BLOCK,
                            ConservativeAirGeometry.FULL_FACE_MASK,
                            ConservativeAirGeometry.FULL_FACE_MASK,
                            ConservativeAirGeometry.FULL_FACE_MASK,
                            ConservativeAirGeometry.FULL_FACE_MASK,
                            ConservativeAirGeometry.FULL_FACE_MASK,
                            ConservativeAirGeometry.FULL_FACE_MASK)),
                    0L,
                    1);

    private final DimensionThermalRuntime runtime;
    private final ThermalCellArena arena;
    private final ThermalSignatureRegistry signatures;
    private final GeometryDeltaRing geometryDeltas;
    private final ResolvedGeometryInputRing resolvedInputs;
    private final Parameters parameters;
    private final SignatureGeometry initialAllAirGeometry;
    private final SignatureGeometry[] signatureGeometryById;
    private final GeometryMigrationLedger migrationLedger = new GeometryMigrationLedger();
    private final Map<PageIdentity, PageState> pages = new LinkedHashMap<>();
    private final List<RetiredSpan> spansAwaitingSweep = new ArrayList<>();

    private boolean topologyDirty;
    private long publicationEpoch;
    private int nextPageSlot;

    public MinecraftThermalTopologyApplier(
            DimensionThermalRuntime runtime,
            ThermalSignatureRegistry signatures,
            GeometryDeltaRing geometryDeltas,
            ResolvedGeometryInputRing resolvedInputs,
            Parameters parameters
    ) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.arena = runtime.thermalCellArena();
        this.signatures = Objects.requireNonNull(signatures, "signatures");
        this.geometryDeltas = Objects.requireNonNull(geometryDeltas, "geometryDeltas");
        this.resolvedInputs = Objects.requireNonNull(resolvedInputs, "resolvedInputs");
        this.parameters = Objects.requireNonNull(parameters, "parameters");
        this.initialAllAirGeometry = new SignatureGeometry(
                true, parameters.airMediumId(), FULL_AIR);
        this.signatureGeometryById = new SignatureGeometry[signatures.signatureCount()];
        for (int signatureId = 0; signatureId < signatureGeometryById.length; signatureId++) {
            signatureGeometryById[signatureId] = convertSignature(
                    signatures.signature(signatureId).orElseThrow());
        }
    }

    public record Parameters(
            int airMediumId,
            int cellFlags,
            int maximumRegionsPerBlock,
            double effectiveAirCapacityJPerBlockK,
            double initialAirTemperatureC,
            double referenceTemperatureC,
            double effectiveMixingWPerBlockK,
            double minimumMixedFaceDistanceBlocks,
            boolean applyBuoyancy,
            BuoyancyConductance.Parameters buoyancyParameters
    ) {
        public Parameters {
            if (airMediumId < 0 || cellFlags < 0 || cellFlags > 0xff
                    || maximumRegionsPerBlock <= 0) {
                throw new IllegalArgumentException("topology IDs and limits are invalid");
            }
            requirePositiveFinite(
                    "effectiveAirCapacityJPerBlockK", effectiveAirCapacityJPerBlockK);
            requireFinite("initialAirTemperatureC", initialAirTemperatureC);
            requireFinite("referenceTemperatureC", referenceTemperatureC);
            requirePositiveFinite(
                    "effectiveMixingWPerBlockK", effectiveMixingWPerBlockK);
            requirePositiveFinite(
                    "minimumMixedFaceDistanceBlocks", minimumMixedFaceDistanceBlocks);
            Objects.requireNonNull(buoyancyParameters, "buoyancyParameters");
        }
    }

    public enum ApplyStatus {
        APPLIED,
        DUPLICATE,
        WRITER_BUSY,
        SOURCE_REBIND_REQUIRED,
        SOURCE_INPUTS_PENDING,
        FULL_RESYNC_SNAPSHOT_REQUIRED,
        LATEST_FRAME_REQUIRED,
        GENERATION_MISMATCH,
        ACK_REJECTED
    }

    public record ApplyReport(
            ApplyStatus status,
            int consumedResolvedInputs,
            int consumedGeometryDeltas,
            int rebuiltPages,
            int retiredPages,
            int pairOperations,
            boolean topologyResolved,
            DimensionThermalRuntime.AcknowledgeResult acknowledgeResult
    ) {
        private static ApplyReport pending(ApplyStatus status) {
            return new ApplyReport(status, 0, 0, 0, 0, 0, false, null);
        }

        public boolean readyForSolve() {
            return acknowledgeResult == DimensionThermalRuntime.AcknowledgeResult.APPLIED
                    || acknowledgeResult
                    == DimensionThermalRuntime.AcknowledgeResult.DUPLICATE;
        }
    }

    public synchronized GeometryMigrationLedger.Snapshot migrationSnapshot() {
        return migrationLedger.snapshot();
    }

    public synchronized void registerAllAirPage(
            ThermalPage page,
            long admissionChunkWatermark
    ) {
        Objects.requireNonNull(page, "page");
        if (admissionChunkWatermark < 0L) {
            throw new IllegalArgumentException("admission chunk watermark must be non-negative");
        }
        int lifecycleGeneration = Math.toIntExact(page.lifecycleGeneration());
        ArenaSpan span = page.cellSpan();
        if (span.count() == 0) {
            throw new IllegalArgumentException("all-air admission must own an arena cell");
        }
        int pageSlot = arena.pageSlot(span.firstSlot());
        if (pageSlot == Integer.MAX_VALUE) {
            nextPageSlot = Integer.MAX_VALUE;
        } else {
            nextPageSlot = Math.max(nextPageSlot, pageSlot + 1);
        }
        for (int slot = span.firstSlot(); slot < span.endSlotExclusive(); slot++) {
            if (arena.pageSlot(slot) != pageSlot
                    || arena.lifecycleGeneration(slot) != lifecycleGeneration) {
                throw new IllegalArgumentException("Page and arena ownership disagree");
            }
        }
        PageIdentity identity = new PageIdentity(page.sectionKey(), page.lifecycleGeneration());
        if (pages.containsKey(identity)) {
            throw new IllegalStateException("thermal Page is already registered with the applier");
        }
        pages.put(identity, new PageState(
                page,
                pageSlot,
                lifecycleGeneration,
                admissionChunkWatermark,
                page.coverageSnapshot()));
        topologyDirty = true;
    }

    /** Creates a provisional unpublished Page from one loaded main-thread snapshot. */
    public synchronized ThermalPage registerCapturedPage(
            long sectionKey,
            long lifecycleGeneration,
            long admissionChunkWatermark,
            int[] signatureIds
    ) {
        if (lifecycleGeneration < 0L || admissionChunkWatermark < 0L) {
            throw new IllegalArgumentException("captured Page generations are invalid");
        }
        if (signatureIds == null || signatureIds.length != BLOCKS_PER_PAGE) {
            throw new IllegalArgumentException("captured Page requires 4096 signature IDs");
        }
        PageIdentity identity = new PageIdentity(sectionKey, lifecycleGeneration);
        if (pages.containsKey(identity) || nextPageSlot == Integer.MAX_VALUE) {
            throw new IllegalStateException("captured Page cannot allocate a unique owner slot");
        }
        int pageSlot = nextPageSlot++;
        int generation = Math.toIntExact(lifecycleGeneration);
        int minX = SectionPos.sectionToBlockCoord(SectionPos.x(sectionKey));
        int minY = SectionPos.sectionToBlockCoord(SectionPos.y(sectionKey));
        int minZ = SectionPos.sectionToBlockCoord(SectionPos.z(sectionKey));
        ArenaSpan provisional = arena.allocatePageCells(
                pageSlot,
                generation,
                new ThermalCellArena.CellSpec[]{ThermalCellArena.CellSpec.regularAir(
                        minX,
                        minY,
                        minZ,
                        16,
                        parameters.airMediumId(),
                        parameters.cellFlags(),
                        parameters.effectiveAirCapacityJPerBlockK())},
                parameters.initialAirTemperatureC(),
                parameters.referenceTemperatureC());
        ThermalPage page = ThermalPage.allAir(
                sectionKey, lifecycleGeneration, provisional.firstSlot(),
                parameters.airMediumId());
        PageState state = new PageState(
                page,
                pageSlot,
                generation,
                admissionChunkWatermark,
                page.coverageSnapshot());
        state.desiredSignatureIds = normalizedSignatureCut(signatureIds);
        state.desiredGeometryRevision = page.liveGeometryRevision();
        state.dirty = true;
        pages.put(identity, state);
        topologyDirty = true;
        return page;
    }

    /** Resolves the exact air component touching one declared block face. */
    public synchronized PortResolution resolveAirFacePort(
            int blockX,
            int blockY,
            int blockZ,
            ConservativeAirGeometry.Face targetFace
    ) {
        Objects.requireNonNull(targetFace, "targetFace");
        long sectionKey = SectionPos.asLong(
                SectionPos.blockToSectionCoord(blockX),
                SectionPos.blockToSectionCoord(blockY),
                SectionPos.blockToSectionCoord(blockZ));
        PageState state = null;
        for (PageState candidate : pages.values()) {
            if (candidate.page.sectionKey() == sectionKey
                    && candidate.retirementChunkWatermark == Long.MAX_VALUE) {
                state = candidate;
                break;
            }
        }
        if (state == null
                || state.dirty
                || state.page.fullGeometryResyncRequired()
                || state.page.publishedGeometryRevision()
                        != state.page.liveGeometryRevision()) {
            return PortResolution.topologyUnavailable();
        }

        int localX = SectionPos.sectionRelative(blockX);
        int localY = SectionPos.sectionRelative(blockY);
        int localZ = SectionPos.sectionRelative(blockZ);
        GeometrySummary targetSummary = state.page.geometrySummary(
                GeometrySummaryCache.baseIndex(localX, localY, localZ));
        if ((targetSummary.topologyFlags()
                & GeometrySummary.UNRESOLVED_TOPOLOGY) != 0) {
            return PortResolution.topologyUnavailable();
        }
        int pageBlock = blockIndex(localX, localY, localZ);
        int[] slots = new int[16];
        int distinct = 0;
        for (int vertical = 0; vertical < 4; vertical++) {
            for (int horizontal = 0; horizontal < 4; horizontal++) {
                int microcell = faceMicrocell(targetFace, horizontal, vertical);
                int slot = cellForMicrocell(
                        state.appliedSignatureIds,
                        state.appliedCoverageRefs,
                        state.appliedMixedGeometry,
                        pageBlock,
                        microcell);
                if (slot == ThermalCellArena.NO_SLOT) {
                    continue;
                }
                int found = -1;
                for (int index = 0; index < distinct; index++) {
                    if (slots[index] == slot) {
                        found = index;
                        break;
                    }
                }
                if (found < 0) {
                    slots[distinct] = slot;
                    distinct++;
                }
            }
        }
        if (distinct == 0) {
            return PortResolution.blocked();
        }
        if (distinct != 1) {
            return PortResolution.topologyUnavailable();
        }
        return PortResolution.resolved(SourceBinding.thermalNode(
                slots[0], state.lifecycleGeneration));
    }

    public enum PortResolutionStatus {
        RESOLVED,
        BLOCKED,
        TOPOLOGY_UNAVAILABLE
    }

    public record PortResolution(
            PortResolutionStatus status,
            SourceBinding binding
    ) {
        private static PortResolution resolved(SourceBinding binding) {
            return new PortResolution(PortResolutionStatus.RESOLVED, binding);
        }

        private static PortResolution blocked() {
            return new PortResolution(PortResolutionStatus.BLOCKED, null);
        }

        private static PortResolution topologyUnavailable() {
            return new PortResolution(PortResolutionStatus.TOPOLOGY_UNAVAILABLE, null);
        }
    }

    public synchronized void retirePage(
            ThermalPage page,
            long retirementChunkWatermark
    ) {
        Objects.requireNonNull(page, "page");
        if (retirementChunkWatermark < 0L) {
            throw new IllegalArgumentException("retirement chunk watermark must be non-negative");
        }
        PageState state = pages.get(new PageIdentity(
                page.sectionKey(), page.lifecycleGeneration()));
        if (state == null) {
            return;
        }
        if (retirementChunkWatermark < state.admissionChunkWatermark) {
            throw new IllegalArgumentException("Page retirement precedes admission");
        }
        state.retirementChunkWatermark = Math.min(
                state.retirementChunkWatermark, retirementChunkWatermark);
        topologyDirty = true;
    }

    public synchronized ApplyReport apply(SealedInputFrame frame) {
        Objects.requireNonNull(frame, "frame");
        if (frame.dimensionGeneration() != runtime.dimensionGeneration()) {
            return ApplyReport.pending(ApplyStatus.GENERATION_MISMATCH);
        }
        if (!runtime.tryBeginTopologyUpdate()) {
            return ApplyReport.pending(ApplyStatus.WRITER_BUSY);
        }
        boolean writerOwned = true;
        boolean sourceCutPreApplied = false;
        try {
            DrainResult drained = drain(frame);
            List<PageState> active = activePages(frame.watermarks().chunk());
            for (PageState state : active) {
                if (state.page.fullGeometryResyncRequired()
                        && !state.hasCurrentResyncSnapshot()) {
                    return new ApplyReport(
                            ApplyStatus.FULL_RESYNC_SNAPSHOT_REQUIRED,
                            drained.resolvedInputs,
                            drained.geometryDeltas,
                            0, 0, 0, false, null);
                }
                if (state.dirty && state.desiredGeometryRevision
                        != state.page.liveGeometryRevision()) {
                    return new ApplyReport(
                            ApplyStatus.LATEST_FRAME_REQUIRED,
                            drained.resolvedInputs,
                        drained.geometryDeltas,
                        0, 0, 0, false, null);
                }
            }
            if (referencesAffectedSource(active, frame.watermarks().chunk())) {
                DimensionThermalRuntime.SourceTopologyBarrierReport barrier =
                        runtime.preApplySourcesForTopology(frame);
                if (barrier.status()
                        == DimensionThermalRuntime.SourceTopologyBarrierStatus.INPUTS_PENDING
                        || barrier.status()
                        == DimensionThermalRuntime.SourceTopologyBarrierStatus.FRAME_MISMATCH
                        || barrier.status()
                        == DimensionThermalRuntime.SourceTopologyBarrierStatus.UNAVAILABLE) {
                    return new ApplyReport(
                            ApplyStatus.SOURCE_INPUTS_PENDING,
                            drained.resolvedInputs,
                            drained.geometryDeltas,
                            0, 0, 0, false, null);
                }
                sourceCutPreApplied = true;
                if (referencesAffectedSource(active, frame.watermarks().chunk())) {
                    DimensionThermalRuntime.AcknowledgeResult acknowledged;
                    try {
                        acknowledged = finishDeferredTopologyFrame(frame);
                    } finally {
                        writerOwned = false;
                    }
                    return deferredReport(
                            ApplyStatus.SOURCE_REBIND_REQUIRED,
                            drained,
                            0,
                            0,
                            acknowledged);
                }
            }

            int retiredPages = queueRetirements(frame.watermarks().chunk());
            int rebuiltPages = 0;
            try {
                for (PageState state : active) {
                    if (state.dirty) {
                        rebuildPage(state);
                        rebuiltPages++;
                    }
                }

                long nextPublicationEpoch = Math.incrementExact(publicationEpoch);
                for (PageState state : active) {
                    if (!state.page.tryPublishGeometry(
                            state.page.liveGeometryRevision(),
                            state.page.topologyGeneration(),
                            nextPublicationEpoch)) {
                        throw new LatestFrameException();
                    }
                }
                publicationEpoch = nextPublicationEpoch;

                Map<Long, PageState> activeBySection = indexActivePages(active);
                List<ThermalSweep.PairOperation> pairs = compilePairs(active, activeBySection);
                boolean topologyResolved = topologyResolved(active, activeBySection);
                ThermalSweep replacementSweep = new ThermalSweep(
                        arena, pairs, List.of(), parameters.buoyancyParameters());

                boolean topologyChanged = topologyDirty || rebuiltPages != 0 || retiredPages != 0;
                long nextGeometryRevision = Math.max(
                        runtime.geometryRevision(), frame.watermarks().geometry());
                long nextTopologyGeneration = runtime.topologyGeneration()
                        + (topologyChanged ? 1L : 0L);
                DimensionThermalRuntime.AcknowledgeResult acknowledged;
                try {
                    acknowledged = runtime.finishTopologyUpdate(
                            frame.dimensionGeneration(),
                            frame.watermarks(),
                            nextGeometryRevision,
                            nextTopologyGeneration,
                            topologyResolved,
                            replacementSweep);
                } finally {
                    writerOwned = false;
                }
                if (acknowledged != DimensionThermalRuntime.AcknowledgeResult.APPLIED
                        && acknowledged != DimensionThermalRuntime.AcknowledgeResult.DUPLICATE) {
                    return new ApplyReport(
                            ApplyStatus.ACK_REJECTED,
                            drained.resolvedInputs,
                            drained.geometryDeltas,
                            rebuiltPages,
                            retiredPages,
                            pairs.size(),
                            topologyResolved,
                            acknowledged);
                }

                releaseCommittedSpans();
                removeRetiredPages(frame.watermarks().chunk());
                topologyDirty = false;
                return new ApplyReport(
                        acknowledged == DimensionThermalRuntime.AcknowledgeResult.APPLIED
                                ? ApplyStatus.APPLIED
                                : ApplyStatus.DUPLICATE,
                        drained.resolvedInputs,
                        drained.geometryDeltas,
                        rebuiltPages,
                        retiredPages,
                        pairs.size(),
                        topologyResolved,
                        acknowledged);
            } catch (LatestFrameException exception) {
                if (sourceCutPreApplied) {
                    DimensionThermalRuntime.AcknowledgeResult acknowledged;
                    try {
                        acknowledged = finishDeferredTopologyFrame(frame);
                    } finally {
                        writerOwned = false;
                    }
                    if (acknowledged == DimensionThermalRuntime.AcknowledgeResult.APPLIED
                            || acknowledged
                            == DimensionThermalRuntime.AcknowledgeResult.DUPLICATE) {
                        releaseCommittedSpans();
                        removeRetiredPages(frame.watermarks().chunk());
                    }
                    return deferredReport(
                            ApplyStatus.LATEST_FRAME_REQUIRED,
                            drained,
                            rebuiltPages,
                            retiredPages,
                            acknowledged);
                }
                return new ApplyReport(
                        ApplyStatus.LATEST_FRAME_REQUIRED,
                        drained.resolvedInputs,
                        drained.geometryDeltas,
                        rebuiltPages,
                        retiredPages,
                        0,
                        false,
                        null);
            }
        } finally {
            if (writerOwned) {
                runtime.cancelTopologyUpdate();
            }
        }
    }

    private DimensionThermalRuntime.AcknowledgeResult finishDeferredTopologyFrame(
            SealedInputFrame frame
    ) {
        ThermalSweep disabledSweep = new ThermalSweep(
                arena,
                List.of(),
                List.of(),
                parameters.buoyancyParameters());
        return runtime.finishTopologyUpdate(
                frame.dimensionGeneration(),
                frame.watermarks(),
                Math.max(runtime.geometryRevision(), frame.watermarks().geometry()),
                Math.incrementExact(runtime.topologyGeneration()),
                false,
                disabledSweep);
    }

    private static ApplyReport deferredReport(
            ApplyStatus status,
            DrainResult drained,
            int rebuiltPages,
            int retiredPages,
            DimensionThermalRuntime.AcknowledgeResult acknowledged
    ) {
        return new ApplyReport(
                status,
                drained.resolvedInputs,
                drained.geometryDeltas,
                rebuiltPages,
                retiredPages,
                0,
                false,
                acknowledged);
    }

    private DrainResult drain(SealedInputFrame frame) {
        int resolvedCount = 0;
        ResolvedGeometryInputRing.MutableInput input =
                new ResolvedGeometryInputRing.MutableInput();
        while (resolvedInputs.pollThroughWatermark(
                frame.watermarks().geometry(), input)) {
            resolvedCount++;
            PageState state = pages.get(new PageIdentity(
                    input.sectionKey(), input.lifecycleGeneration()));
            if (state == null) {
                continue;
            }
            state.desiredGeometryRevision = Math.max(
                    state.desiredGeometryRevision, input.geometryRevision());
            state.dirty = true;
            if (input.kind() == ResolvedGeometryInputRing.Kind.FULL_RESYNC_REQUIRED) {
                int[] snapshot = input.fullPageSignatureIds();
                if (snapshot == null
                        || snapshot.length != ResolvedGeometryInputRing.BLOCKS_PER_PAGE) {
                    throw new IllegalStateException(
                            "full resync input is missing its Page snapshot");
                }
                for (int index = 0; index < snapshot.length; index++) {
                    int signatureId = snapshot[index];
                    if (signatureId < 0 || signatures.signature(signatureId).isEmpty()) {
                        snapshot[index] = UNRESOLVED_SIGNATURE;
                    }
                }
                state.desiredSignatureIds = snapshot;
                state.pendingResyncToken = new ThermalPage.GeometryResyncToken(
                        input.sectionKey(),
                        input.lifecycleGeneration(),
                        input.geometryRevision(),
                        input.geometryResyncReason());
                continue;
            }
            state.desiredSignatureIds[input.blockIndex()] =
                    input.status() == ThermalResolution.Status.RESOLVED
                            && signatures.signature(input.signatureId()).isPresent()
                            ? input.signatureId()
                            : UNRESOLVED_SIGNATURE;
        }

        int deltaCount = 0;
        GeometryDeltaRing.MutableGeometryDelta delta =
                new GeometryDeltaRing.MutableGeometryDelta();
        while (geometryDeltas.pollThroughTick(frame.effectiveTick(), delta)) {
            deltaCount++;
            PageState state = pages.get(new PageIdentity(
                    delta.sectionKey(), delta.lifecycleGeneration()));
            if (state != null) {
                state.desiredGeometryRevision = Math.max(
                        state.desiredGeometryRevision, delta.geometryRevision());
                state.dirty = true;
            }
        }
        return new DrainResult(resolvedCount, deltaCount);
    }

    private void rebuildPage(PageState state) {
        PageBuild build = compilePage(state);
        ArenaSpan oldSpan = state.page.cellSpan();
        ThermalCellArena.PageAllocation allocation = arena.allocatePageCells(
                state.pageSlot,
                state.lifecycleGeneration,
                build.regularCells.toArray(ThermalCellArena.CellSpec[]::new),
                build.mixedBricks.toArray(ThermalCellArena.MixedBrickSpec[]::new),
                parameters.initialAirTemperatureC(),
                parameters.referenceTemperatureC());
        ArenaSpan newSpan = allocation.cellSpan();
        int[] coverage = buildCoverage(build, allocation);

        GeometryMigrationLedger.MigrationResult migration;
        try {
            migration = calculateMigration(
                    state,
                    build,
                    oldSpan,
                    newSpan,
                    coverage);
            double[] enthalpies = migration.newEnthalpiesJ();
            for (int offset = 0; offset < enthalpies.length; offset++) {
                arena.setEnthalpyJ(newSpan.firstSlot() + offset, enthalpies[offset]);
            }
            ThermalPage.FullGeometryState geometry = new ThermalPage.FullGeometryState(
                    coverage,
                    build.coverageWidths,
                    build.summaries,
                    build.mixedBrickMask,
                    newSpan);
            boolean installed = state.pendingResyncToken == null
                    ? state.page.tryInstallGeometryBuild(
                            state.desiredGeometryRevision, geometry)
                    : state.page.tryInstallFullGeometryResync(
                            state.pendingResyncToken, geometry);
            if (!installed) {
                arena.releasePageCells(
                        state.pageSlot, state.lifecycleGeneration, newSpan);
                throw new LatestFrameException();
            }
        } catch (RuntimeException exception) {
            if (newSpan.count() != 0 && !newSpan.equals(state.page.cellSpan())
                    && arena.isLive(newSpan.firstSlot())) {
                arena.releasePageCells(
                        state.pageSlot, state.lifecycleGeneration, newSpan);
            }
            throw exception;
        }

        migrationLedger.record(migration);
        queueRelease(state.pageSlot, state.lifecycleGeneration, oldSpan);
        state.appliedSignatureIds = state.desiredSignatureIds.clone();
        state.appliedCoverageRefs = coverage.clone();
        state.appliedMixedGeometry = build.mixedGeometry.clone();
        state.unresolvedTopology = build.unresolvedTopology;
        state.pendingResyncToken = null;
        state.dirty = false;
    }

    private PageBuild compilePage(PageState state) {
        PageBuild build = new PageBuild();
        int sectionMinX = SectionPos.sectionToBlockCoord(SectionPos.x(state.page.sectionKey()));
        int sectionMinY = SectionPos.sectionToBlockCoord(SectionPos.y(state.page.sectionKey()));
        int sectionMinZ = SectionPos.sectionToBlockCoord(SectionPos.z(state.page.sectionKey()));
        int compilerGeneration = Math.toIntExact(state.page.topologyGeneration() + 1L);

        for (int baseIndex = 0; baseIndex < ThermalPage.BASE_BRICK_COUNT; baseIndex++) {
            int brickX = baseIndex & 3;
            int brickZ = (baseIndex >>> 2) & 3;
            int brickY = (baseIndex >>> 4) & 3;
            List<ConservativeAirGeometry.Resolution> geometry = new ArrayList<>(64);
            int mediumId = -1;
            int provenAirMicrocells = 0;
            boolean fullAir = true;
            boolean unsupported = false;

            for (int blockY = 0; blockY < 4; blockY++) {
                for (int blockZ = 0; blockZ < 4; blockZ++) {
                    for (int blockX = 0; blockX < 4; blockX++) {
                        int localX = (brickX << 2) + blockX;
                        int localY = (brickY << 2) + blockY;
                        int localZ = (brickZ << 2) + blockZ;
                        int blockIndex = blockIndex(localX, localY, localZ);
                        SignatureGeometry block = signatureGeometry(
                                state.desiredSignatureIds[blockIndex]);
                        geometry.add(block.geometry);
                        if (!block.resolved) {
                            unsupported = true;
                            fullAir = false;
                            continue;
                        }
                        long airMask = block.geometry.provenAirMicrocellMask();
                        provenAirMicrocells += Long.bitCount(airMask);
                        fullAir &= airMask == FULL_MICROCELL_MASK
                                && block.geometry.components().size() == 1;
                        if (airMask != 0L) {
                            if (mediumId == -1) {
                                mediumId = block.mediumId;
                            } else if (mediumId != block.mediumId) {
                                unsupported = true;
                            }
                        }
                    }
                }
            }

            int minX = sectionMinX + (brickX << 2);
            int minY = sectionMinY + (brickY << 2);
            int minZ = sectionMinZ + (brickZ << 2);
            if (unsupported) {
                build.setNoAir(baseIndex, true);
            } else if (provenAirMicrocells == 0) {
                build.setNoAir(baseIndex, false);
            } else if (fullAir && provenAirMicrocells == 64 * MICROCELLS_PER_BLOCK) {
                build.regularOrdinal[baseIndex] = build.regularCells.size();
                build.regularCells.add(ThermalCellArena.CellSpec.regularAir(
                        minX, minY, minZ, 4, mediumId, parameters.cellFlags(),
                        parameters.effectiveAirCapacityJPerBlockK()));
                build.baseSummaries[baseIndex] = GeometrySummary.singleAir(mediumId);
            } else {
                ComponentBrickCompiler.Compilation compiled = ComponentBrickCompiler.compile(
                        geometry, parameters.maximumRegionsPerBlock(), compilerGeneration);
                if (compiled.status() != ComponentBrickCompiler.Status.RESOLVED
                        || compiled.brick().orElseThrow().componentCount() == 0) {
                    build.setNoAir(baseIndex, true);
                    continue;
                }
                ComponentBrickCompiler.CompiledBrick brick = compiled.brick().orElseThrow();
                build.mixedOrdinal[baseIndex] = build.mixedBricks.size();
                build.mixedBricks.add(new ThermalCellArena.MixedBrickSpec(
                        minX, minY, minZ, brick, mediumId,
                        parameters.cellFlags(),
                        parameters.effectiveAirCapacityJPerBlockK()));
                build.mixedGeometry[baseIndex] = brick;
                build.baseSummaries[baseIndex] = GeometrySummary.mixed(
                        GeometrySummary.MATERIAL_INTERFACE);
                build.mixedBrickMask |= 1L << baseIndex;
            }
        }

        GeometrySummaryCache summaries = new GeometrySummaryCache();
        for (int baseIndex = 0; baseIndex < ThermalPage.BASE_BRICK_COUNT; baseIndex++) {
            summaries.setBaseSummary(baseIndex, build.baseSummaries[baseIndex]);
        }
        build.summaries = summaries.snapshot();
        return build;
    }

    private int[] buildCoverage(
            PageBuild build,
            ThermalCellArena.PageAllocation allocation
    ) {
        int[] coverage = new int[ThermalPage.BASE_BRICK_COUNT];
        Arrays.fill(coverage, ThermalPage.NO_COVERAGE);
        int[] mixedSupports = allocation.mixedSupportRefs();
        for (int baseIndex = 0; baseIndex < ThermalPage.BASE_BRICK_COUNT; baseIndex++) {
            int regularOrdinal = build.regularOrdinal[baseIndex];
            if (regularOrdinal >= 0) {
                coverage[baseIndex] = allocation.cellSpan().firstSlot() + regularOrdinal;
            } else if (build.mixedOrdinal[baseIndex] >= 0) {
                coverage[baseIndex] = mixedSupports[build.mixedOrdinal[baseIndex]];
            }
        }
        return coverage;
    }

    private boolean referencesAffectedSource(
            List<PageState> active,
            long chunkWatermark
    ) {
        for (PageState state : active) {
            if (state.dirty && runtime.sourceTopologyReferences(state.page.cellSpan())) {
                return true;
            }
        }
        for (PageState state : pages.values()) {
            if (state.retirementChunkWatermark <= chunkWatermark
                    && runtime.sourceTopologyReferences(state.page.cellSpan())) {
                return true;
            }
        }
        for (RetiredSpan retired : spansAwaitingSweep) {
            if (runtime.sourceTopologyReferences(retired.span)) {
                return true;
            }
        }
        return false;
    }

    private int[] normalizedSignatureCut(int[] signatureIds) {
        int[] normalized = signatureIds.clone();
        for (int index = 0; index < normalized.length; index++) {
            int signatureId = normalized[index];
            if (signatureId < 0 || signatures.signature(signatureId).isEmpty()) {
                normalized[index] = UNRESOLVED_SIGNATURE;
            }
        }
        return normalized;
    }

    private GeometryMigrationLedger.MigrationResult calculateMigration(
            PageState state,
            PageBuild build,
            ArenaSpan oldSpan,
            ArenaSpan newSpan,
            int[] newCoverage
    ) {
        double[] oldEnthalpies = new double[oldSpan.count()];
        double[] oldCapacities = new double[oldSpan.count()];
        for (int offset = 0; offset < oldSpan.count(); offset++) {
            int slot = oldSpan.firstSlot() + offset;
            oldEnthalpies[offset] = arena.enthalpyJ(slot);
            oldCapacities[offset] = arena.capacityJPerK(slot);
        }
        double[] newCapacities = new double[newSpan.count()];
        double[] initialTemperatures = new double[newSpan.count()];
        Arrays.fill(initialTemperatures, parameters.initialAirTemperatureC());
        for (int offset = 0; offset < newSpan.count(); offset++) {
            newCapacities[offset] = arena.capacityJPerK(newSpan.firstSlot() + offset);
        }

        int maximumOverlaps = BLOCKS_PER_PAGE * MICROCELLS_PER_BLOCK;
        int[] oldIndices = new int[maximumOverlaps];
        int[] newIndices = new int[maximumOverlaps];
        double[] overlapCapacities = new double[maximumOverlaps];
        double microcellCapacity = parameters.effectiveAirCapacityJPerBlockK()
                / MICROCELLS_PER_BLOCK;
        int overlapCount = 0;
        for (int block = 0; block < BLOCKS_PER_PAGE; block++) {
            for (int microcell = 0; microcell < MICROCELLS_PER_BLOCK; microcell++) {
                int oldSlot = cellForMicrocell(
                        state.appliedSignatureIds,
                        state.appliedCoverageRefs,
                        state.appliedMixedGeometry,
                        block,
                        microcell);
                int newSlot = cellForMicrocell(
                        state.desiredSignatureIds,
                        newCoverage,
                        build.mixedGeometry,
                        block,
                        microcell);
                if (oldSlot == ThermalCellArena.NO_SLOT
                        || newSlot == ThermalCellArena.NO_SLOT) {
                    continue;
                }
                oldIndices[overlapCount] = oldSlot - oldSpan.firstSlot();
                newIndices[overlapCount] = newSlot - newSpan.firstSlot();
                overlapCapacities[overlapCount] = microcellCapacity;
                overlapCount++;
            }
        }
        return GeometryMigrationLedger.calculateSparseGeometryMigration(
                oldEnthalpies,
                oldCapacities,
                newCapacities,
                oldIndices,
                newIndices,
                overlapCapacities,
                overlapCount,
                initialTemperatures,
                parameters.referenceTemperatureC());
    }

    private int cellForMicrocell(
            int[] signatureIds,
            int[] coverage,
            ComponentBrickCompiler.CompiledBrick[] mixedGeometry,
            int pageBlockIndex,
            int microcellIndex
    ) {
        SignatureGeometry signature = signatureGeometry(signatureIds[pageBlockIndex]);
        if (!signature.resolved) {
            return ThermalCellArena.NO_SLOT;
        }
        int microX = microcellIndex & 3;
        int microZ = (microcellIndex >>> 2) & 3;
        int microY = (microcellIndex >>> 4) & 3;
        int localRegion = signature.geometry.componentAt(microX, microY, microZ);
        if (localRegion < 0) {
            return ThermalCellArena.NO_SLOT;
        }
        int localX = pageBlockIndex & 15;
        int localZ = (pageBlockIndex >>> 4) & 15;
        int localY = (pageBlockIndex >>> 8) & 15;
        int baseIndex = GeometrySummaryCache.baseIndex(localX, localY, localZ);
        int support = coverage[baseIndex];
        if (support == ThermalPage.NO_COVERAGE) {
            return ThermalCellArena.NO_SLOT;
        }
        ComponentBrickCompiler.CompiledBrick brick = mixedGeometry[baseIndex];
        if (brick == null) {
            return support;
        }
        int blockInBrick = (localX & 3) | ((localZ & 3) << 2) | ((localY & 3) << 4);
        int component = brick.compiledComponentAt(blockInBrick, localRegion);
        return component < 0
                ? ThermalCellArena.NO_SLOT
                : arena.mixedComponentSlot(support, component);
    }

    private SignatureGeometry signatureGeometry(int signatureId) {
        if (signatureId == INITIAL_ALL_AIR) {
            return initialAllAirGeometry;
        }
        if (signatureId < 0 || signatureId >= signatureGeometryById.length) {
            return SignatureGeometry.UNRESOLVED;
        }
        return signatureGeometryById[signatureId];
    }

    private static SignatureGeometry convertSignature(ResolvedThermalSignature signature) {
        List<ConservativeAirGeometry.AirComponent> components =
                new ArrayList<>(signature.airRegions().size());
        long airMask = 0L;
        for (LocalAirRegionPattern region : signature.airRegions()) {
            components.add(new ConservativeAirGeometry.AirComponent(
                    region.localRegionId(),
                    region.provenAirMicrocellMask(),
                    region.microcellCount(),
                    region.negativeXMask(),
                    region.positiveXMask(),
                    region.negativeYMask(),
                    region.positiveYMask(),
                    region.negativeZMask(),
                    region.positiveZMask()));
            airMask |= region.provenAirMicrocellMask();
        }
        return new SignatureGeometry(
                true,
                signature.mediumId(),
                new ConservativeAirGeometry.Resolution(
                        ConservativeAirGeometry.Status.RESOLVED,
                        ConservativeAirGeometry.UnsupportedReason.NONE,
                        components,
                        ~airMask,
                        components.size()));
    }

    private List<ThermalSweep.PairOperation> compilePairs(
            List<PageState> active,
            Map<Long, PageState> activeBySection
    ) {
        active.sort(Comparator
                .comparingInt((PageState state) -> SectionPos.x(state.page.sectionKey()))
                .thenComparingInt(state -> SectionPos.y(state.page.sectionKey()))
                .thenComparingInt(state -> SectionPos.z(state.page.sectionKey())));
        List<ThermalSweep.PairOperation> operations = new ArrayList<>();
        for (PageState state : active) {
            int sectionX = SectionPos.x(state.page.sectionKey());
            int sectionY = SectionPos.y(state.page.sectionKey());
            int sectionZ = SectionPos.z(state.page.sectionKey());
            ImplicitAirAdjacency.PageView owner = pageView(state);
            ImplicitAirAdjacency.PositiveNeighbors neighbors =
                    new ImplicitAirAdjacency.PositiveNeighbors(
                            pageView(activeBySection.get(SectionPos.asLong(
                                    sectionX + 1, sectionY, sectionZ))),
                            pageView(activeBySection.get(SectionPos.asLong(
                                    sectionX, sectionY + 1, sectionZ))),
                            pageView(activeBySection.get(SectionPos.asLong(
                                    sectionX, sectionY, sectionZ + 1))));
            ImplicitAirAdjacency.CompiledPairs compiled =
                    ImplicitAirAdjacency.compileOwnedPairs(
                            owner,
                            neighbors,
                            arena,
                            parameters.effectiveMixingWPerBlockK(),
                            parameters.minimumMixedFaceDistanceBlocks(),
                            parameters.applyBuoyancy());
            if (!compiled.ownerPublicationCurrent()
                    || compiled.unavailablePositivePages() != 0) {
                throw new LatestFrameException();
            }
            operations.addAll(compiled.operations());
        }
        return operations;
    }

    private boolean topologyResolved(
            List<PageState> active,
            Map<Long, PageState> activeBySection
    ) {
        for (PageState state : active) {
            if (state.unresolvedTopology) {
                return false;
            }
            int sectionX = SectionPos.x(state.page.sectionKey());
            int sectionY = SectionPos.y(state.page.sectionKey());
            int sectionZ = SectionPos.z(state.page.sectionKey());
            for (ConservativeAirGeometry.Face face : ConservativeAirGeometry.Face.values()) {
                int neighborX = sectionX + (face == ConservativeAirGeometry.Face.NEGATIVE_X ? -1
                        : face == ConservativeAirGeometry.Face.POSITIVE_X ? 1 : 0);
                int neighborY = sectionY + (face == ConservativeAirGeometry.Face.NEGATIVE_Y ? -1
                        : face == ConservativeAirGeometry.Face.POSITIVE_Y ? 1 : 0);
                int neighborZ = sectionZ + (face == ConservativeAirGeometry.Face.NEGATIVE_Z ? -1
                        : face == ConservativeAirGeometry.Face.POSITIVE_Z ? 1 : 0);
                if (!activeBySection.containsKey(SectionPos.asLong(
                        neighborX, neighborY, neighborZ))
                        && pageHasOpenFace(state, face)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean pageHasOpenFace(
            PageState state,
            ConservativeAirGeometry.Face face
    ) {
        for (int localY = 0; localY < 16; localY++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                for (int localX = 0; localX < 16; localX++) {
                    if (!onFace(localX, localY, localZ, face)) {
                        continue;
                    }
                    SignatureGeometry geometry = signatureGeometry(
                            state.appliedSignatureIds[blockIndex(localX, localY, localZ)]);
                    if (geometry.resolved
                            && geometry.geometry.combinedFaceMask(face) != 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private int queueRetirements(long chunkWatermark) {
        int retired = 0;
        for (PageState state : pages.values()) {
            if (state.retirementChunkWatermark <= chunkWatermark
                    && !state.retirementQueued) {
                queueRelease(
                        state.pageSlot,
                        state.lifecycleGeneration,
                        state.page.cellSpan());
                state.retirementQueued = true;
                retired++;
            }
        }
        return retired;
    }

    private void queueRelease(int pageSlot, int lifecycleGeneration, ArenaSpan span) {
        if (span.count() == 0) {
            return;
        }
        RetiredSpan retired = new RetiredSpan(pageSlot, lifecycleGeneration, span);
        if (!spansAwaitingSweep.contains(retired)) {
            spansAwaitingSweep.add(retired);
        }
    }

    private void releaseCommittedSpans() {
        for (RetiredSpan retired : spansAwaitingSweep) {
            arena.releasePageCells(
                    retired.pageSlot, retired.lifecycleGeneration, retired.span);
        }
        spansAwaitingSweep.clear();
    }

    private void removeRetiredPages(long chunkWatermark) {
        pages.values().removeIf(state -> state.retirementChunkWatermark <= chunkWatermark);
    }

    private List<PageState> activePages(long chunkWatermark) {
        List<PageState> active = new ArrayList<>();
        for (PageState state : pages.values()) {
            if (state.admissionChunkWatermark <= chunkWatermark
                    && state.retirementChunkWatermark > chunkWatermark) {
                active.add(state);
            }
        }
        return active;
    }

    private static Map<Long, PageState> indexActivePages(List<PageState> active) {
        Map<Long, PageState> bySection = new HashMap<>();
        for (PageState state : active) {
            if (bySection.put(state.page.sectionKey(), state) != null) {
                throw new IllegalStateException("two active Page generations share one section");
            }
        }
        return bySection;
    }

    private static ImplicitAirAdjacency.PageView pageView(PageState state) {
        if (state == null) {
            return null;
        }
        long sectionKey = state.page.sectionKey();
        return new ImplicitAirAdjacency.PageView(
                state.page,
                state.pageSlot,
                SectionPos.sectionToBlockCoord(SectionPos.x(sectionKey)),
                SectionPos.sectionToBlockCoord(SectionPos.y(sectionKey)),
                SectionPos.sectionToBlockCoord(SectionPos.z(sectionKey)));
    }

    private static boolean onFace(
            int x,
            int y,
            int z,
            ConservativeAirGeometry.Face face
    ) {
        return switch (face) {
            case NEGATIVE_X -> x == 0;
            case POSITIVE_X -> x == 15;
            case NEGATIVE_Y -> y == 0;
            case POSITIVE_Y -> y == 15;
            case NEGATIVE_Z -> z == 0;
            case POSITIVE_Z -> z == 15;
        };
    }

    private static int blockIndex(int localX, int localY, int localZ) {
        return (localY << 8) | (localZ << 4) | localX;
    }

    private static int faceMicrocell(
            ConservativeAirGeometry.Face face,
            int horizontal,
            int vertical
    ) {
        int x;
        int y;
        int z;
        switch (face) {
            case NEGATIVE_X, POSITIVE_X -> {
                x = face == ConservativeAirGeometry.Face.NEGATIVE_X ? 0 : 3;
                y = vertical;
                z = horizontal;
            }
            case NEGATIVE_Y, POSITIVE_Y -> {
                x = horizontal;
                y = face == ConservativeAirGeometry.Face.NEGATIVE_Y ? 0 : 3;
                z = vertical;
            }
            case NEGATIVE_Z, POSITIVE_Z -> {
                x = horizontal;
                y = vertical;
                z = face == ConservativeAirGeometry.Face.NEGATIVE_Z ? 0 : 3;
            }
            default -> throw new IllegalStateException("unknown thermal face");
        }
        return (y << 4) | (z << 2) | x;
    }

    private static void requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private static void requirePositiveFinite(String name, double value) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalArgumentException(name + " must be finite and positive");
        }
    }

    private record PageIdentity(long sectionKey, long lifecycleGeneration) {
    }

    private record DrainResult(int resolvedInputs, int geometryDeltas) {
    }

    private record RetiredSpan(int pageSlot, int lifecycleGeneration, ArenaSpan span) {
    }

    private record SignatureGeometry(
            boolean resolved,
            int mediumId,
            ConservativeAirGeometry.Resolution geometry
    ) {
        private static final SignatureGeometry UNRESOLVED =
                new SignatureGeometry(false, -1, null);
    }

    private static final class PageState {
        private final ThermalPage page;
        private final int pageSlot;
        private final int lifecycleGeneration;
        private final long admissionChunkWatermark;
        private int[] appliedSignatureIds = new int[BLOCKS_PER_PAGE];
        private int[] desiredSignatureIds = new int[BLOCKS_PER_PAGE];
        private int[] appliedCoverageRefs;
        private ComponentBrickCompiler.CompiledBrick[] appliedMixedGeometry =
                new ComponentBrickCompiler.CompiledBrick[ThermalPage.BASE_BRICK_COUNT];
        private long retirementChunkWatermark = Long.MAX_VALUE;
        private long desiredGeometryRevision;
        private boolean dirty;
        private ThermalPage.GeometryResyncToken pendingResyncToken;
        private boolean unresolvedTopology;
        private boolean retirementQueued;

        private PageState(
                ThermalPage page,
                int pageSlot,
                int lifecycleGeneration,
                long admissionChunkWatermark,
                int[] initialCoverage
        ) {
            this.page = page;
            this.pageSlot = pageSlot;
            this.lifecycleGeneration = lifecycleGeneration;
            this.admissionChunkWatermark = admissionChunkWatermark;
            this.appliedCoverageRefs = initialCoverage;
            Arrays.fill(appliedSignatureIds, INITIAL_ALL_AIR);
            Arrays.fill(desiredSignatureIds, INITIAL_ALL_AIR);
        }

        private boolean hasCurrentResyncSnapshot() {
            return pendingResyncToken != null
                    && pendingResyncToken.requiredRevision()
                            == page.liveGeometryRevision()
                    && desiredGeometryRevision == pendingResyncToken.requiredRevision();
        }
    }

    private static final class PageBuild {
        private final List<ThermalCellArena.CellSpec> regularCells = new ArrayList<>();
        private final List<ThermalCellArena.MixedBrickSpec> mixedBricks = new ArrayList<>();
        private final int[] regularOrdinal = new int[ThermalPage.BASE_BRICK_COUNT];
        private final int[] mixedOrdinal = new int[ThermalPage.BASE_BRICK_COUNT];
        private final byte[] coverageWidths = new byte[ThermalPage.BASE_BRICK_COUNT];
        private final GeometrySummary[] baseSummaries =
                new GeometrySummary[ThermalPage.BASE_BRICK_COUNT];
        private final ComponentBrickCompiler.CompiledBrick[] mixedGeometry =
                new ComponentBrickCompiler.CompiledBrick[ThermalPage.BASE_BRICK_COUNT];
        private GeometrySummary[] summaries;
        private long mixedBrickMask;
        private boolean unresolvedTopology;

        private PageBuild() {
            Arrays.fill(regularOrdinal, -1);
            Arrays.fill(mixedOrdinal, -1);
            Arrays.fill(coverageWidths, (byte) 4);
        }

        private void setNoAir(int baseIndex, boolean unresolved) {
            baseSummaries[baseIndex] = GeometrySummary.noAir(
                    unresolved ? GeometrySummary.UNRESOLVED_TOPOLOGY : 0);
            unresolvedTopology |= unresolved;
        }
    }

    private static final class LatestFrameException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
