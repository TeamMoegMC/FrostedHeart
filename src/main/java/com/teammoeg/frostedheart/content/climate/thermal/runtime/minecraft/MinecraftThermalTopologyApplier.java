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
import com.teammoeg.frostedheart.content.climate.thermal.mesh.FacePatchIterator;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.FarFieldProfileRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.GeometryMigrationLedger;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ImplicitAirAdjacency;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPage;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.TopologyGuard;
import com.teammoeg.frostedheart.content.climate.thermal.profile.LocalAirRegionPattern;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalResolution;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.DimensionThermalRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.solver.BuoyancyConductance;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.solver.SealedInputFrame;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSweep;
import com.teammoeg.frostedheart.content.climate.thermal.source.SourceBinding;
import net.minecraft.core.SectionPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Concrete logical-writer bridge from PR8 primitive geometry inputs to the
 * authoritative arena and one replacement sweep. It is explicitly enabled;
 * normal gameplay never constructs it.
 */
public final class MinecraftThermalTopologyApplier {
    static final int POINT_NO_AIR = -1;
    static final int POINT_TOPOLOGY_UNAVAILABLE = -2;
    private static final int BLOCKS_PER_PAGE = 16 * 16 * 16;
    private static final int MICROCELLS_PER_BLOCK = ConservativeAirGeometry.MICROCELL_COUNT;
    private static final int INITIAL_ALL_AIR = -2;
    private static final int UNRESOLVED_SIGNATURE = -1;
    private static final int SKY_EXPOSURE_COLUMNS = 16 * 16;
    private static final byte[] NO_SKY_EXPOSURE = noSkyExposure();
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
    private final MaterialBoundaryRegistry materialBoundaries;
    private final FarFieldSettings farFieldSettings;
    private final PhaseTransitionRuntime phaseTransitions;
    private final SignatureGeometry initialAllAirGeometry;
    private final SignatureGeometry[] signatureGeometryById;
    private final GeometryMigrationLedger migrationLedger = new GeometryMigrationLedger();
    private final Map<PageIdentity, PageState> pages = new LinkedHashMap<>();
    private final IdentityHashMap<ThermalPage, PageState> pagesByPage =
            new IdentityHashMap<>();
    private final List<RetiredSpan> spansAwaitingSweep = new ArrayList<>();

    private boolean topologyDirty;
    private double farFieldConductanceScale = 1.0D;
    private long publicationEpoch;
    private int nextPageSlot;

    public MinecraftThermalTopologyApplier(
            DimensionThermalRuntime runtime,
            ThermalSignatureRegistry signatures,
            GeometryDeltaRing geometryDeltas,
            ResolvedGeometryInputRing resolvedInputs,
            Parameters parameters
    ) {
        this(runtime, signatures, geometryDeltas, resolvedInputs, parameters,
                MaterialBoundaryRegistry.empty(), FarFieldSettings.disabled());
    }

    public MinecraftThermalTopologyApplier(
            DimensionThermalRuntime runtime,
            ThermalSignatureRegistry signatures,
            GeometryDeltaRing geometryDeltas,
            ResolvedGeometryInputRing resolvedInputs,
            Parameters parameters,
            MaterialBoundaryRegistry materialBoundaries
    ) {
        this(runtime, signatures, geometryDeltas, resolvedInputs, parameters,
                materialBoundaries, FarFieldSettings.disabled());
    }

    public MinecraftThermalTopologyApplier(
            DimensionThermalRuntime runtime,
            ThermalSignatureRegistry signatures,
            GeometryDeltaRing geometryDeltas,
            ResolvedGeometryInputRing resolvedInputs,
            Parameters parameters,
            MaterialBoundaryRegistry materialBoundaries,
            FarFieldSettings farFieldSettings
    ) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.arena = runtime.thermalCellArena();
        this.signatures = Objects.requireNonNull(signatures, "signatures");
        this.geometryDeltas = Objects.requireNonNull(geometryDeltas, "geometryDeltas");
        this.resolvedInputs = Objects.requireNonNull(resolvedInputs, "resolvedInputs");
        this.parameters = Objects.requireNonNull(parameters, "parameters");
        this.materialBoundaries = Objects.requireNonNull(
                materialBoundaries, "materialBoundaries");
        this.farFieldSettings = Objects.requireNonNull(
                farFieldSettings, "farFieldSettings");
        this.phaseTransitions = new PhaseTransitionRuntime(
                arena,
                parameters.phaseRequestCapacity(),
                parameters.phaseAckCapacity());
        this.initialAllAirGeometry = new SignatureGeometry(
                true, parameters.airMediumId(), 0, 0, FULL_AIR);
        this.signatureGeometryById = new SignatureGeometry[signatures.signatureCount()];
        for (int signatureId = 0; signatureId < signatureGeometryById.length; signatureId++) {
            signatureGeometryById[signatureId] = convertSignature(
                    signatures.signature(signatureId).orElseThrow());
        }
    }

    /** Immutable Minecraft-side policy for the already calibrated open-space profile. */
    public record FarFieldSettings(
            FarFieldProfileRegistry profiles,
            boolean naturalEnvironment,
            FarFieldProfileRegistry.EnvironmentClass environmentClass,
            FarFieldProfileRegistry.WindBucket windBucket,
            double calibrationSourcePowerW,
            double referenceOpeningAreaBlocksSquared,
            double continuationDistanceBlocks
    ) {
        public FarFieldSettings {
            Objects.requireNonNull(profiles, "profiles");
            Objects.requireNonNull(environmentClass, "environmentClass");
            Objects.requireNonNull(windBucket, "windBucket");
            requireNonNegativeFinite(
                    "calibrationSourcePowerW", calibrationSourcePowerW);
            requirePositiveFinite(
                    "referenceOpeningAreaBlocksSquared",
                    referenceOpeningAreaBlocksSquared);
            requirePositiveFinite(
                    "continuationDistanceBlocks", continuationDistanceBlocks);
        }

        public static FarFieldSettings disabled() {
            return new FarFieldSettings(
                    FarFieldProfileRegistry.empty(),
                    false,
                    FarFieldProfileRegistry.EnvironmentClass.CUSTOM_NATURAL,
                    FarFieldProfileRegistry.WindBucket.CALM,
                    0.0D,
                    1.0D,
                    16.0D);
        }

        private FarFieldProfileRegistry.Key openSpaceKey() {
            return new FarFieldProfileRegistry.Key(
                    0,
                    FarFieldProfileRegistry.OpeningClass.MULTI_FACE,
                    2,
                    FarFieldProfileRegistry.Orientation.HORIZONTAL,
                    windBucket,
                    environmentClass,
                    FarFieldProfileRegistry.TopologyClass.OPEN_SPACE);
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
            BuoyancyConductance.Parameters buoyancyParameters,
            int phaseRequestCapacity,
            int phaseAckCapacity,
            int maximumPhaseMutationsPerTick
    ) {
        public Parameters(
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
            this(
                    airMediumId, cellFlags, maximumRegionsPerBlock,
                    effectiveAirCapacityJPerBlockK,
                    initialAirTemperatureC, referenceTemperatureC,
                    effectiveMixingWPerBlockK, minimumMixedFaceDistanceBlocks,
                    applyBuoyancy, buoyancyParameters,
                    256, 256, 8);
        }

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
            if (phaseRequestCapacity <= 0 || phaseAckCapacity <= 0
                    || maximumPhaseMutationsPerTick <= 0) {
                throw new IllegalArgumentException("phase limits must be positive");
            }
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

    synchronized int stagedSignaturePageCount() {
        int count = 0;
        for (PageState state : pages.values()) {
            if (state.desiredSignatureIds != null) {
                count++;
            }
        }
        return count;
    }

    /** Updates only the fixed external temperature; geometry remains unchanged. */
    public synchronized boolean updateNaturalTemperature(
            ThermalPage page,
            double naturalTemperatureC,
            double minimumChangeC
    ) {
        Objects.requireNonNull(page, "page");
        requireFinite("naturalTemperatureC", naturalTemperatureC);
        requireNonNegativeFinite("minimumChangeC", minimumChangeC);
        PageState state = pagesByPage.get(page);
        if (state == null
                || Math.abs(state.naturalTemperatureC - naturalTemperatureC)
                < minimumChangeC) {
            return false;
        }
        state.naturalTemperatureC = naturalTemperatureC;
        topologyDirty = true;
        return true;
    }

    /** Replaces the main-thread heightmap cut used as cheap outdoor proof. */
    public synchronized boolean updateSkyExposure(
            ThermalPage page,
            byte[] firstExposedLocalY
    ) {
        Objects.requireNonNull(page, "page");
        byte[] normalized = normalizedSkyExposure(firstExposedLocalY);
        PageState state = pagesByPage.get(page);
        if (state == null || Arrays.equals(state.firstExposedLocalY, normalized)) {
            return false;
        }
        state.firstExposedLocalY = normalized;
        topologyDirty = true;
        return true;
    }

    /** Updates the global weather multiplier without changing geometry. */
    public synchronized boolean updateFarFieldConductanceScale(
            double conductanceScale,
            double minimumChange
    ) {
        requirePositiveFinite("conductanceScale", conductanceScale);
        requireNonNegativeFinite("minimumChange", minimumChange);
        if (Math.abs(farFieldConductanceScale - conductanceScale) < minimumChange) {
            return false;
        }
        farFieldConductanceScale = conductanceScale;
        topologyDirty = true;
        return true;
    }

    /** Returns non-sky open Page faces that need one loaded continuation Page. */
    synchronized int continuationFaceMask(ThermalPage page) {
        Objects.requireNonNull(page, "page");
        PageState state = pagesByPage.get(page);
        if (state == null || state.retirementChunkWatermark != Long.MAX_VALUE) {
            return 0;
        }
        int[] signatureIds = state.dirty
                ? state.desiredSignatureIds
                : state.appliedSignatureIds;
        int result = 0;
        for (ConservativeAirGeometry.Face face : ConservativeAirGeometry.Face.values()) {
            boolean continuation = false;
            for (int localY = 0; localY < 16 && !continuation; localY++) {
                for (int localZ = 0; localZ < 16 && !continuation; localZ++) {
                    for (int localX = 0; localX < 16; localX++) {
                        if (!onFace(localX, localY, localZ, face)
                                || localY >= Byte.toUnsignedInt(state.firstExposedLocalY[
                                        (localZ << 4) | localX])) {
                            continue;
                        }
                        SignatureGeometry geometry = signatureGeometry(
                                signatureIds[blockIndex(localX, localY, localZ)]);
                        if (geometry.resolved
                                && geometry.geometry.combinedFaceMask(face) != 0) {
                            continuation = true;
                            break;
                        }
                    }
                }
            }
            if (continuation) {
                result |= 1 << face.ordinal();
            }
        }
        return result;
    }

    MaterialBoundaryRegistry.Profile materialProfile(int profileId) {
        return materialBoundaries.profile(profileId).orElse(null);
    }

    MaterialBoundaryRegistry.ContactPattern materialContactPattern(int patternId) {
        return materialBoundaries.contactPattern(patternId).orElse(null);
    }

    boolean hasAppliedPhaseCandidate(
            ThermalPage page,
            int blockX,
            int blockY,
            int blockZ,
            int materialProfileId
    ) {
        PageState state = pagesByPage.get(page);
        if (state == null || state.dirty || state.unresolvedTopology) {
            return false;
        }
        PhaseReservoirKey key = new PhaseReservoirKey(
                Math.floorDiv(blockX, 4) * 4,
                Math.floorDiv(blockY, 4) * 4,
                Math.floorDiv(blockZ, 4) * 4,
                materialProfileId);
        Integer slot = state.appliedPhaseReservoirSlots.get(key);
        if (slot == null
                || !arena.isLive(slot)
                || !arena.isPhaseReservoir(slot)
                || arena.materialProfileId(slot) != materialProfileId) {
            return false;
        }
        int candidateBit = Math.floorMod(blockX, 4)
                | (Math.floorMod(blockZ, 4) << 2)
                | (Math.floorMod(blockY, 4) << 4);
        return (arena.phaseCandidateMask(slot) & (1L << candidateBit)) != 0L;
    }

    boolean pollPhaseRequest(PhaseTransitionRuntime.MutableRequest target) {
        return phaseTransitions.pollRequest(target);
    }

    boolean canAcceptPhaseAck() {
        return phaseTransitions.canAcceptAck();
    }

    void submitPhaseAck(
            PhaseTransitionRuntime.MutableRequest request,
            PhaseTransitionRuntime.AckOutcome outcome
    ) {
        phaseTransitions.submitAck(request, outcome);
    }

    int flushPendingPhaseAcks() {
        return phaseTransitions.flushPendingAcks();
    }

    long latestPhaseAckWatermark() {
        return phaseTransitions.latestOfferedAckWatermark();
    }

    double committedPhaseEnergyJ() {
        return phaseTransitions.committedTransitionEnergyJ();
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
        PageState state = new PageState(
                page,
                pageSlot,
                lifecycleGeneration,
                admissionChunkWatermark,
                page.coverageSnapshot(),
                parameters.initialAirTemperatureC());
        pages.put(identity, state);
        pagesByPage.put(page, state);
        topologyDirty = true;
    }

    /** Creates a provisional unpublished Page from one loaded main-thread snapshot. */
    public synchronized ThermalPage registerCapturedPage(
            long sectionKey,
            long lifecycleGeneration,
            long admissionChunkWatermark,
            int[] signatureIds
    ) {
        return registerCapturedPage(
                sectionKey,
                lifecycleGeneration,
                admissionChunkWatermark,
                signatureIds,
                parameters.initialAirTemperatureC());
    }

    /** Captures one Page with its local natural-air boundary temperature. */
    public synchronized ThermalPage registerCapturedPage(
            long sectionKey,
            long lifecycleGeneration,
            long admissionChunkWatermark,
            int[] signatureIds,
            double naturalTemperatureC
    ) {
        if (lifecycleGeneration < 0L || admissionChunkWatermark < 0L) {
            throw new IllegalArgumentException("captured Page generations are invalid");
        }
        requireFinite("naturalTemperatureC", naturalTemperatureC);
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
                naturalTemperatureC,
                parameters.referenceTemperatureC());
        ThermalPage page = ThermalPage.allAir(
                sectionKey, lifecycleGeneration, provisional.firstSlot(),
                parameters.airMediumId());
        PageState state = new PageState(
                page,
                pageSlot,
                generation,
                admissionChunkWatermark,
                page.coverageSnapshot(),
                naturalTemperatureC);
        state.desiredSignatureIds = normalizedSignatureCut(signatureIds);
        state.desiredGeometryRevision = page.liveGeometryRevision();
        state.dirty = true;
        pages.put(identity, state);
        pagesByPage.put(page, state);
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

    /** Resolves the exact quarter-block air component inside a mixed Brick. */
    synchronized int resolvePublishedAirPoint(
            ThermalPage page,
            int localX,
            int localY,
            int localZ,
            int microcellIndex
    ) {
        Objects.requireNonNull(page, "page");
        if ((localX | localY | localZ) < 0
                || localX >= 16 || localY >= 16 || localZ >= 16
                || microcellIndex < 0 || microcellIndex >= MICROCELLS_PER_BLOCK) {
            throw new IllegalArgumentException("local point is outside its Page");
        }
        PageState state = pagesByPage.get(page);
        if (state == null
                || state.page != page
                || state.retirementChunkWatermark != Long.MAX_VALUE
                || state.dirty
                || !page.publishedGeometryIsCurrent()) {
            return POINT_TOPOLOGY_UNAVAILABLE;
        }
        int slot = cellForMicrocell(
                state.appliedSignatureIds,
                state.appliedCoverageRefs,
                state.appliedMixedGeometry,
                blockIndex(localX, localY, localZ),
                microcellIndex);
        return slot == ThermalCellArena.NO_SLOT ? POINT_NO_AIR : slot;
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
        state.materialDependencyChanged = true;
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
            phaseTransitions.applyAcksThrough(
                    frame.watermarks().transitionAck());
            DrainResult drained = drain(frame);
            propagateMaterialDependencyDirtiness(frame.watermarks().chunk());
            List<PageState> active = activePages(frame.watermarks().chunk());
            Map<Long, PageState> activeBySection = indexActivePages(active);
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
            boolean requiresTopologyCompilation = topologyDirty || retiredPages != 0;
            if (!requiresTopologyCompilation) {
                DimensionThermalRuntime.AcknowledgeResult acknowledged;
                try {
                    acknowledged = runtime.finishTopologyUpdate(
                            frame.dimensionGeneration(),
                            frame.watermarks(),
                            Math.max(runtime.geometryRevision(), frame.watermarks().geometry()),
                            runtime.topologyGeneration(),
                            runtime.topologyResolved(),
                            null);
                } finally {
                    writerOwned = false;
                }
                return new ApplyReport(
                        acknowledged == DimensionThermalRuntime.AcknowledgeResult.APPLIED
                                ? ApplyStatus.APPLIED
                                : acknowledged == DimensionThermalRuntime.AcknowledgeResult.DUPLICATE
                                        ? ApplyStatus.DUPLICATE
                                        : ApplyStatus.ACK_REJECTED,
                        drained.resolvedInputs,
                        drained.geometryDeltas,
                        0,
                        0,
                        0,
                        runtime.topologyResolved(),
                        acknowledged);
            }
            int rebuiltPages = 0;
            try {
                for (PageState state : active) {
                    if (state.dirty) {
                        rebuildPage(state, activeBySection);
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

                CompiledTopology compiled = compileTopology(active, activeBySection);
                boolean topologyResolved = topologyResolved(active, compiled);
                ThermalSweep replacementSweep = new ThermalSweep(
                        arena,
                        compiled.pairs(),
                        compiled.boundaries(),
                        phaseTransitions,
                        compiled.phaseOperations(),
                        parameters.buoyancyParameters());

                long nextGeometryRevision = Math.max(
                        runtime.geometryRevision(), frame.watermarks().geometry());
                long nextTopologyGeneration = Math.incrementExact(
                        runtime.topologyGeneration());
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
                            compiled.pairs().size(),
                            topologyResolved,
                            acknowledged);
                }

                releaseCommittedSpans();
                removeRetiredPages(frame.watermarks().chunk());
                for (PageState state : pages.values()) {
                    state.materialDependencyChanged = false;
                }
                topologyDirty = false;
                return new ApplyReport(
                        acknowledged == DimensionThermalRuntime.AcknowledgeResult.APPLIED
                                ? ApplyStatus.APPLIED
                                : ApplyStatus.DUPLICATE,
                        drained.resolvedInputs,
                        drained.geometryDeltas,
                        rebuiltPages,
                        retiredPages,
                        compiled.pairs().size(),
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
            state.materialDependencyChanged = true;
            topologyDirty = true;
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
            ensureDesiredSignatureIds(state);
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
                state.materialDependencyChanged = true;
                ensureDesiredSignatureIds(state);
                topologyDirty = true;
            }
        }
        return new DrainResult(resolvedCount, deltaCount);
    }

    private void rebuildPage(
            PageState state,
            Map<Long, PageState> activeBySection
    ) {
        PageBuild build = compilePage(state, activeBySection);
        ArenaSpan oldSpan = state.page.cellSpan();
        ThermalCellArena.PageAllocation allocation = arena.allocatePageCells(
                state.pageSlot,
                state.lifecycleGeneration,
                build.regularCells.toArray(ThermalCellArena.CellSpec[]::new),
                build.mixedBricks.toArray(ThermalCellArena.MixedBrickSpec[]::new),
                build.materialPoles.toArray(ThermalCellArena.MaterialPoleSpec[]::new),
                build.phaseReservoirs.toArray(
                        ThermalCellArena.PhaseReservoirSpec[]::new),
                parameters.initialAirTemperatureC(),
                parameters.referenceTemperatureC());
        ArenaSpan newSpan = allocation.cellSpan();
        int[] coverage = buildCoverage(build, allocation);
        Map<MaterialPoleKey, Integer> materialPoleSlots =
                buildMaterialPoleSlots(build, allocation);
        Map<PhaseReservoirKey, Integer> phaseReservoirSlots =
                buildPhaseReservoirSlots(build, allocation);

        GeometryMigrationLedger.MigrationResult migration;
        try {
            migration = calculateMigration(
                    state,
                    build,
                    oldSpan,
                    newSpan,
                    coverage,
                    materialPoleSlots,
                    phaseReservoirSlots);
            double[] enthalpies = migration.newEnthalpiesJ();
            for (int offset = 0; offset < enthalpies.length; offset++) {
                arena.setEnthalpyJ(newSpan.firstSlot() + offset, enthalpies[offset]);
            }
            migratePhaseRequestState(state, phaseReservoirSlots);
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
        state.appliedSignatureIds = state.desiredSignatureIds;
        state.desiredSignatureIds = null;
        state.appliedCoverageRefs = coverage.clone();
        state.appliedMixedGeometry = build.mixedGeometry.clone();
        state.appliedMaterialPoleSlots = Map.copyOf(materialPoleSlots);
        state.appliedMaterialSurfaces = List.copyOf(build.materialSurfaces);
        state.appliedPhaseReservoirSlots = Map.copyOf(phaseReservoirSlots);
        state.appliedPhaseReservoirs = List.copyOf(build.phaseSurfaces);
        state.appliedStatelessBridges = List.copyOf(build.statelessBridges);
        state.unresolvedTopology = build.unresolvedTopology;
        state.pendingResyncToken = null;
        state.dirty = false;
    }

    private PageBuild compilePage(
            PageState state,
            Map<Long, PageState> activeBySection
    ) {
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
        compileMaterialBoundaries(state, activeBySection, build);
        return build;
    }

    private void compileMaterialBoundaries(
            PageState state,
            Map<Long, PageState> activeBySection,
            PageBuild build
    ) {
        if (materialBoundaries.profileCount() == 0
                && materialBoundaries.contactPatternCount() == 0) {
            return;
        }
        int sectionMinX = SectionPos.sectionToBlockCoord(SectionPos.x(state.page.sectionKey()));
        int sectionMinY = SectionPos.sectionToBlockCoord(SectionPos.y(state.page.sectionKey()));
        int sectionMinZ = SectionPos.sectionToBlockCoord(SectionPos.z(state.page.sectionKey()));
        Map<MaterialSurfaceKey, MutableMaterialSurface> surfaces = new LinkedHashMap<>();
        Map<PhaseReservoirKey, MutablePhaseReservoir> phases = new LinkedHashMap<>();

        for (int localY = 0; localY < 16; localY++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                for (int localX = 0; localX < 16; localX++) {
                    SignatureGeometry signature = signatureGeometry(state.desiredSignatureIds[
                            blockIndex(localX, localY, localZ)]);
                    ResolvedMaterial material = resolveMaterial(signature, build);
                    if (material == null) {
                        continue;
                    }
                    int blockX = sectionMinX + localX;
                    int blockY = sectionMinY + localY;
                    int blockZ = sectionMinZ + localZ;
                    if (material.profile().model()
                            == MaterialBoundaryRegistry.Model.STATELESS_CONDUCTANCE) {
                        if (material.pattern().materialMicrocellMask()
                                != FULL_MICROCELL_MASK) {
                            build.unresolvedTopology = true;
                            continue;
                        }
                        compileStatelessBridges(
                                blockX, blockY, blockZ,
                                material.profile(), activeBySection, build);
                        continue;
                    }

                    for (int microY = 0; microY < 4; microY++) {
                        for (int microZ = 0; microZ < 4; microZ++) {
                            for (int microX = 0; microX < 4; microX++) {
                                if (!material.pattern().contains(microX, microY, microZ)) {
                                    continue;
                                }
                                for (ConservativeAirGeometry.Face face :
                                        ConservativeAirGeometry.Face.values()) {
                                    AirMicrocell air = adjacentAirMicrocell(
                                            blockX, blockY, blockZ,
                                            microX, microY, microZ,
                                            face, activeBySection, true);
                                    if (air == null) {
                                        continue;
                                    }
                                    if (material.profile().model()
                                            == MaterialBoundaryRegistry.Model.PHASE_RESERVOIR) {
                                        PhaseReservoirKey key = new PhaseReservoirKey(
                                                Math.floorDiv(blockX, 4) * 4,
                                                Math.floorDiv(blockY, 4) * 4,
                                                Math.floorDiv(blockZ, 4) * 4,
                                                material.profile().id());
                                        int candidateBit = Math.floorMod(blockX, 4)
                                                | (Math.floorMod(blockZ, 4) << 2)
                                                | (Math.floorMod(blockY, 4) << 4);
                                        MutablePhaseReservoir phase = phases.computeIfAbsent(
                                                key,
                                                ignored -> new MutablePhaseReservoir(
                                                        key, material.profile()));
                                        phase.addContact(candidateBit, air);
                                        continue;
                                    }
                                    MaterialSurfaceKey key = new MaterialSurfaceKey(
                                            blockX, blockY, blockZ);
                                    MutableMaterialSurface surface = surfaces.computeIfAbsent(
                                            key,
                                            ignored -> new MutableMaterialSurface(
                                                    key, material.profile()));
                                    surface.addContact(air);
                                }
                            }
                        }
                    }
                }
            }
        }

        for (MutableMaterialSurface mutable : surfaces.values()) {
            double area = mutable.contactPatchCount / 16.0D;
            List<MaterialContact> contacts = new ArrayList<>(mutable.contacts.size());
            for (MutableMaterialContact contact : mutable.contacts.values()) {
                contacts.add(new MaterialContact(
                        contact.representative, contact.patchCount));
            }
            MaterialSurface surface = new MaterialSurface(
                    mutable.key,
                    mutable.profile,
                    area,
                    contacts);
            build.materialSurfaces.add(surface);
            addMaterialPole(
                    build,
                    new MaterialPoleKey(
                            mutable.key, ThermalCellArena.MaterialPoleDepth.SURFACE),
                    mutable.profile,
                    ThermalCellArena.MaterialPoleDepth.SURFACE,
                    mutable.profile.surfaceCapacityJPerK() * area,
                    state.naturalTemperatureC);
            if (mutable.profile.model() == MaterialBoundaryRegistry.Model.NATURAL_ROCK
                    && mutable.profile.deepCapacityJPerK() > 0.0D) {
                addMaterialPole(
                        build,
                        new MaterialPoleKey(
                                mutable.key, ThermalCellArena.MaterialPoleDepth.DEEP),
                        mutable.profile,
                        ThermalCellArena.MaterialPoleDepth.DEEP,
                        mutable.profile.deepCapacityJPerK() * area,
                        state.naturalTemperatureC);
            }
        }
        for (MutablePhaseReservoir mutable : phases.values()) {
            List<MaterialContact> contacts = new ArrayList<>(mutable.contacts.size());
            for (MutableMaterialContact contact : mutable.contacts.values()) {
                contacts.add(new MaterialContact(
                        contact.representative, contact.patchCount));
            }
            build.phaseReservoirKeys.add(mutable.key);
            build.phaseReservoirs.add(new ThermalCellArena.PhaseReservoirSpec(
                    mutable.key.brickMinX(),
                    mutable.key.brickMinY(),
                    mutable.key.brickMinZ(),
                    mutable.profile.id(),
                    mutable.candidateMask,
                    mutable.profile.transitionTemperatureC(),
                    mutable.profile.transitionEnergyJPerUnit()));
            build.phaseSurfaces.add(new PhaseSurface(
                    mutable.key, mutable.profile, contacts));
        }
        for (MutableStatelessBridge bridge : build.statelessBridgeBuilds.values()) {
            build.statelessBridges.add(new StatelessBridge(
                    bridge.negativeAir,
                    bridge.positiveAir,
                    bridge.conductanceWPerK));
        }
    }

    private ResolvedMaterial resolveMaterial(
            SignatureGeometry signature,
            PageBuild build
    ) {
        if (!signature.resolved) {
            return null;
        }
        int profileId = signature.materialProfileId;
        int patternId = signature.materialContactPatternId;
        if (profileId == 0 && patternId == 0) {
            return null;
        }
        if (profileId == 0 || patternId == 0) {
            build.unresolvedTopology = true;
            return null;
        }
        MaterialBoundaryRegistry.Profile profile =
                materialBoundaries.profile(profileId).orElse(null);
        MaterialBoundaryRegistry.ContactPattern pattern =
                materialBoundaries.contactPattern(patternId).orElse(null);
        if (profile == null || pattern == null
                || (pattern.materialMicrocellMask()
                & signature.geometry.provenAirMicrocellMask()) != 0L) {
            build.unresolvedTopology = true;
            return null;
        }
        return new ResolvedMaterial(profile, pattern);
    }

    private void compileStatelessBridges(
            int blockX,
            int blockY,
            int blockZ,
            MaterialBoundaryRegistry.Profile profile,
            Map<Long, PageState> activeBySection,
            PageBuild build
    ) {
        for (FacePatchIterator.Axis axis : FacePatchIterator.Axis.values()) {
            for (int v = 0; v < 4; v++) {
                for (int u = 0; u < 4; u++) {
                    AirMicrocell negative;
                    AirMicrocell positive;
                    if (axis == FacePatchIterator.Axis.X) {
                        negative = airMicrocellIfPresent(
                                blockX - 1, blockY, blockZ,
                                3, v, u, activeBySection, true);
                        positive = airMicrocellIfPresent(
                                blockX + 1, blockY, blockZ,
                                0, v, u, activeBySection, true);
                    } else if (axis == FacePatchIterator.Axis.Y) {
                        negative = airMicrocellIfPresent(
                                blockX, blockY - 1, blockZ,
                                u, 3, v, activeBySection, true);
                        positive = airMicrocellIfPresent(
                                blockX, blockY + 1, blockZ,
                                u, 0, v, activeBySection, true);
                    } else {
                        negative = airMicrocellIfPresent(
                                blockX, blockY, blockZ - 1,
                                u, v, 3, activeBySection, true);
                        positive = airMicrocellIfPresent(
                                blockX, blockY, blockZ + 1,
                                u, v, 0, activeBySection, true);
                    }
                    if (negative != null && positive != null) {
                        build.addStatelessBridge(
                                negative,
                                positive,
                                profile.faceConductanceWPerK() / 16.0D);
                    }
                }
            }
        }
    }

    private static void addMaterialPole(
            PageBuild build,
            MaterialPoleKey key,
            MaterialBoundaryRegistry.Profile profile,
            ThermalCellArena.MaterialPoleDepth depth,
            double capacityJPerK,
            double pageNaturalTemperatureC
    ) {
        if (!Double.isFinite(capacityJPerK) || capacityJPerK <= 0.0D) {
            throw new IllegalStateException("compiled material pole capacity is invalid");
        }
        build.materialPoleKeys.add(key);
        build.materialPoles.add(new ThermalCellArena.MaterialPoleSpec(
                key.surface().blockX(),
                key.surface().blockY(),
                key.surface().blockZ(),
                profile.id(),
                depth,
                capacityJPerK,
                profile.poleInitialTemperatureC(
                        key.surface().blockY(), pageNaturalTemperatureC)));
    }

    private AirMicrocell adjacentAirMicrocell(
            int blockX,
            int blockY,
            int blockZ,
            int microX,
            int microY,
            int microZ,
            ConservativeAirGeometry.Face face,
            Map<Long, PageState> activeBySection,
            boolean desired
    ) {
        int targetBlockX = blockX;
        int targetBlockY = blockY;
        int targetBlockZ = blockZ;
        int targetMicroX = microX;
        int targetMicroY = microY;
        int targetMicroZ = microZ;
        switch (face) {
            case NEGATIVE_X -> targetMicroX--;
            case POSITIVE_X -> targetMicroX++;
            case NEGATIVE_Y -> targetMicroY--;
            case POSITIVE_Y -> targetMicroY++;
            case NEGATIVE_Z -> targetMicroZ--;
            case POSITIVE_Z -> targetMicroZ++;
        }
        if (targetMicroX < 0) {
            targetBlockX--;
            targetMicroX = 3;
        } else if (targetMicroX >= 4) {
            targetBlockX++;
            targetMicroX = 0;
        }
        if (targetMicroY < 0) {
            targetBlockY--;
            targetMicroY = 3;
        } else if (targetMicroY >= 4) {
            targetBlockY++;
            targetMicroY = 0;
        }
        if (targetMicroZ < 0) {
            targetBlockZ--;
            targetMicroZ = 3;
        } else if (targetMicroZ >= 4) {
            targetBlockZ++;
            targetMicroZ = 0;
        }
        return airMicrocellIfPresent(
                targetBlockX, targetBlockY, targetBlockZ,
                targetMicroX, targetMicroY, targetMicroZ,
                activeBySection, desired);
    }

    private AirMicrocell airMicrocellIfPresent(
            int blockX,
            int blockY,
            int blockZ,
            int microX,
            int microY,
            int microZ,
            Map<Long, PageState> activeBySection,
            boolean desired
    ) {
        long sectionKey = SectionPos.asLong(
                SectionPos.blockToSectionCoord(blockX),
                SectionPos.blockToSectionCoord(blockY),
                SectionPos.blockToSectionCoord(blockZ));
        PageState state = activeBySection.get(sectionKey);
        if (state == null) {
            return null;
        }
        int localX = SectionPos.sectionRelative(blockX);
        int localY = SectionPos.sectionRelative(blockY);
        int localZ = SectionPos.sectionRelative(blockZ);
        int pageBlock = blockIndex(localX, localY, localZ);
        int signatureId = desired
                ? state.desiredSignatureIds[pageBlock]
                : state.appliedSignatureIds[pageBlock];
        SignatureGeometry signature = signatureGeometry(signatureId);
        int component = signature.resolved
                ? signature.geometry.componentAt(microX, microY, microZ)
                : -1;
        return component < 0 ? null : new AirMicrocell(
                blockX, blockY, blockZ,
                (microY << 4) | (microZ << 2) | microX,
                component);
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

    private static Map<MaterialPoleKey, Integer> buildMaterialPoleSlots(
            PageBuild build,
            ThermalCellArena.PageAllocation allocation
    ) {
        int[] slots = allocation.materialPoleSlots();
        if (slots.length != build.materialPoleKeys.size()) {
            throw new IllegalStateException("material pole allocation order changed");
        }
        Map<MaterialPoleKey, Integer> indexed = new LinkedHashMap<>();
        for (int index = 0; index < slots.length; index++) {
            if (indexed.put(build.materialPoleKeys.get(index), slots[index]) != null) {
                throw new IllegalStateException("duplicate material pole key");
            }
        }
        return indexed;
    }

    private static Map<PhaseReservoirKey, Integer> buildPhaseReservoirSlots(
            PageBuild build,
            ThermalCellArena.PageAllocation allocation
    ) {
        int[] slots = allocation.phaseReservoirSlots();
        if (slots.length != build.phaseReservoirKeys.size()) {
            throw new IllegalStateException("phase reservoir allocation order changed");
        }
        Map<PhaseReservoirKey, Integer> indexed = new LinkedHashMap<>();
        for (int index = 0; index < slots.length; index++) {
            if (indexed.put(build.phaseReservoirKeys.get(index), slots[index]) != null) {
                throw new IllegalStateException("duplicate phase reservoir key");
            }
        }
        return indexed;
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
            int[] newCoverage,
            Map<MaterialPoleKey, Integer> newMaterialPoleSlots,
            Map<PhaseReservoirKey, Integer> newPhaseReservoirSlots
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
        Arrays.fill(initialTemperatures, state.naturalTemperatureC);
        for (int offset = 0; offset < newSpan.count(); offset++) {
            newCapacities[offset] = arena.capacityJPerK(newSpan.firstSlot() + offset);
        }
        for (int index = 0; index < build.materialPoleKeys.size(); index++) {
            Integer slot = newMaterialPoleSlots.get(build.materialPoleKeys.get(index));
            if (slot == null) {
                throw new IllegalStateException("material pole has no allocated arena slot");
            }
            initialTemperatures[slot - newSpan.firstSlot()] =
                    build.materialPoles.get(index).initialTemperatureC();
        }
        for (Integer slot : newPhaseReservoirSlots.values()) {
            initialTemperatures[slot - newSpan.firstSlot()] =
                    parameters.referenceTemperatureC();
        }

        int maximumOverlaps = Math.addExact(
                BLOCKS_PER_PAGE * MICROCELLS_PER_BLOCK,
                Math.addExact(
                        newMaterialPoleSlots.size(),
                        newPhaseReservoirSlots.size()));
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
        for (Map.Entry<MaterialPoleKey, Integer> entry :
                newMaterialPoleSlots.entrySet()) {
            Integer oldSlot = state.appliedMaterialPoleSlots.get(entry.getKey());
            if (oldSlot == null) {
                continue;
            }
            int newSlot = entry.getValue();
            oldIndices[overlapCount] = oldSlot - oldSpan.firstSlot();
            newIndices[overlapCount] = newSlot - newSpan.firstSlot();
            overlapCapacities[overlapCount] = Math.min(
                    arena.capacityJPerK(oldSlot), arena.capacityJPerK(newSlot));
            overlapCount++;
        }
        for (Map.Entry<PhaseReservoirKey, Integer> entry :
                newPhaseReservoirSlots.entrySet()) {
            Integer oldSlot = state.appliedPhaseReservoirSlots.get(entry.getKey());
            if (oldSlot == null) {
                continue;
            }
            int newSlot = entry.getValue();
            oldIndices[overlapCount] = oldSlot - oldSpan.firstSlot();
            newIndices[overlapCount] = newSlot - newSpan.firstSlot();
            overlapCapacities[overlapCount] = Math.min(
                    arena.capacityJPerK(oldSlot), arena.capacityJPerK(newSlot));
            overlapCount++;
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

    private void migratePhaseRequestState(
            PageState state,
            Map<PhaseReservoirKey, Integer> newPhaseReservoirSlots
    ) {
        for (Map.Entry<PhaseReservoirKey, Integer> entry :
                newPhaseReservoirSlots.entrySet()) {
            Integer oldSlot = state.appliedPhaseReservoirSlots.get(entry.getKey());
            if (oldSlot != null && arena.phaseRequestOutstanding(oldSlot)) {
                arena.copyPhaseRequestState(oldSlot, entry.getValue());
            }
        }
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
                signature.materialProfileId(),
                signature.materialContactPatternId(),
                new ConservativeAirGeometry.Resolution(
                        ConservativeAirGeometry.Status.RESOLVED,
                        ConservativeAirGeometry.UnsupportedReason.NONE,
                        components,
                        ~airMask,
                components.size()));
    }

    private CompiledTopology compileTopology(
            List<PageState> active,
            Map<Long, PageState> activeBySection
    ) {
        List<ThermalSweep.PairOperation> pairs = compilePairs(active, activeBySection);
        Map<Long, Double> materialConductance = new LinkedHashMap<>();
        List<ThermalSweep.BoundaryOperation> boundaries = new ArrayList<>();
        List<ThermalSweep.PhaseOperation> phaseOperations = new ArrayList<>();
        FarFieldCompilation farField = compileFarField(
                active, activeBySection, pairs);
        boundaries.addAll(farField.boundaries());

        for (PageState state : active) {
            for (StatelessBridge bridge : state.appliedStatelessBridges) {
                int negative = airCellForMicrocell(bridge.negativeAir(), activeBySection);
                int positive = airCellForMicrocell(bridge.positiveAir(), activeBySection);
                addFixedConductance(
                        materialConductance,
                        negative,
                        positive,
                        bridge.conductanceWPerK());
            }
            for (MaterialSurface surface : state.appliedMaterialSurfaces) {
                Integer surfaceSlot = state.appliedMaterialPoleSlots.get(
                        new MaterialPoleKey(
                                surface.key(), ThermalCellArena.MaterialPoleDepth.SURFACE));
                if (surfaceSlot == null) {
                    throw new LatestFrameException();
                }
                double patchConductance =
                        surface.profile().faceConductanceWPerK() / 16.0D;
                for (MaterialContact contact : surface.airContacts()) {
                    addFixedConductance(
                            materialConductance,
                            airCellForMicrocell(contact.air(), activeBySection),
                            surfaceSlot,
                            patchConductance * contact.patchCount());
                }

                if (surface.profile().model()
                        != MaterialBoundaryRegistry.Model.NATURAL_ROCK) {
                    continue;
                }
                double naturalTemperature = surface.profile().naturalTemperatureC(
                        surface.key().blockY());
                Integer deepSlot = state.appliedMaterialPoleSlots.get(
                        new MaterialPoleKey(
                                surface.key(), ThermalCellArena.MaterialPoleDepth.DEEP));
                if (deepSlot == null) {
                    boundaries.add(new ThermalSweep.BoundaryOperation(
                            surfaceSlot,
                            naturalTemperature,
                            surface.profile().deepConductanceWPerK()
                                    * surface.areaBlocksSquared()));
                } else {
                    addFixedConductance(
                            materialConductance,
                            surfaceSlot,
                            deepSlot,
                            surface.profile().deepConductanceWPerK()
                                    * surface.areaBlocksSquared());
                    boundaries.add(new ThermalSweep.BoundaryOperation(
                            deepSlot,
                            naturalTemperature,
                            surface.profile().naturalConductanceWPerK()
                                    * surface.areaBlocksSquared()));
                }
            }
            for (PhaseSurface phase : state.appliedPhaseReservoirs) {
                Integer phaseSlot = state.appliedPhaseReservoirSlots.get(phase.key());
                if (phaseSlot == null) {
                    throw new LatestFrameException();
                }
                double patchConductance =
                        phase.profile().faceConductanceWPerK() / 16.0D;
                for (MaterialContact contact : phase.airContacts()) {
                    phaseOperations.add(new ThermalSweep.PhaseOperation(
                            airCellForMicrocell(contact.air(), activeBySection),
                            phaseSlot,
                            patchConductance * contact.patchCount()));
                }
            }
        }

        for (Map.Entry<Long, Double> entry : materialConductance.entrySet()) {
            int first = (int) (entry.getKey() >>> 32);
            int second = (int) (long) entry.getKey();
            pairs.add(ThermalSweep.PairOperation.fixed(
                    first, second, entry.getValue()));
        }
        return new CompiledTopology(
                pairs,
                boundaries,
                phaseOperations,
                farField.allOpenFrontiersResolved());
    }

    private int airCellForMicrocell(
            AirMicrocell air,
            Map<Long, PageState> activeBySection
    ) {
        long sectionKey = SectionPos.asLong(
                SectionPos.blockToSectionCoord(air.blockX()),
                SectionPos.blockToSectionCoord(air.blockY()),
                SectionPos.blockToSectionCoord(air.blockZ()));
        PageState state = activeBySection.get(sectionKey);
        if (state == null) {
            throw new LatestFrameException();
        }
        int pageBlock = blockIndex(
                SectionPos.sectionRelative(air.blockX()),
                SectionPos.sectionRelative(air.blockY()),
                SectionPos.sectionRelative(air.blockZ()));
        int slot = cellForMicrocell(
                state.appliedSignatureIds,
                state.appliedCoverageRefs,
                state.appliedMixedGeometry,
                pageBlock,
                air.microcellIndex());
        if (slot == ThermalCellArena.NO_SLOT || arena.isMaterialPole(slot)) {
            throw new LatestFrameException();
        }
        return slot;
    }

    private static void addFixedConductance(
            Map<Long, Double> conductances,
            int first,
            int second,
            double conductanceWPerK
    ) {
        if (first == second) {
            return;
        }
        if (!Double.isFinite(conductanceWPerK) || conductanceWPerK <= 0.0D) {
            throw new IllegalStateException("compiled material conductance is invalid");
        }
        int low = Math.min(first, second);
        int high = Math.max(first, second);
        long key = ((long) low << 32) | (high & 0xffff_ffffL);
        conductances.merge(key, conductanceWPerK, (left, right) -> {
            double sum = left + right;
            if (!Double.isFinite(sum)) {
                throw new IllegalStateException("material conductance sum is not finite");
            }
            return sum;
        });
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

    private FarFieldCompilation compileFarField(
            List<PageState> active,
            Map<Long, PageState> activeBySection,
            List<ThermalSweep.PairOperation> airPairs
    ) {
        int capacity = arena.highWaterMark();
        int[] parent = new int[capacity];
        int[] openPatchCount = new int[capacity];
        boolean[] skyExposed = new boolean[capacity];
        double[] naturalTemperatureC = new double[capacity];
        Arrays.fill(parent, ThermalCellArena.NO_SLOT);
        Arrays.fill(naturalTemperatureC, Double.NaN);

        for (PageState state : active) {
            ArenaSpan span = state.page.cellSpan();
            for (int slot = span.firstSlot(); slot < span.endSlotExclusive(); slot++) {
                if (arena.isLive(slot)
                        && !arena.isMaterialPole(slot)
                        && !arena.isPhaseReservoir(slot)) {
                    parent[slot] = slot;
                }
            }
        }
        for (ThermalSweep.PairOperation pair : airPairs) {
            union(parent, pair.cellA(), pair.cellB());
        }

        boolean allFrontierPatchesMapped = true;
        for (PageState state : active) {
            int sectionX = SectionPos.x(state.page.sectionKey());
            int sectionY = SectionPos.y(state.page.sectionKey());
            int sectionZ = SectionPos.z(state.page.sectionKey());
            for (ConservativeAirGeometry.Face face : ConservativeAirGeometry.Face.values()) {
                long neighborKey = neighborSectionKey(sectionX, sectionY, sectionZ, face);
                if (!activeBySection.containsKey(neighborKey)) {
                    allFrontierPatchesMapped &= collectOpenFrontierPatches(
                            state,
                            face,
                            openPatchCount,
                            skyExposed,
                            naturalTemperatureC);
                }
            }
        }

        int[] componentOpenPatchCount = new int[capacity];
        boolean[] componentSkyExposed = new boolean[capacity];
        double[] componentMaximumDeltaC = new double[capacity];
        for (int slot = 0; slot < capacity; slot++) {
            if (openPatchCount[slot] == 0) {
                continue;
            }
            int root = find(parent, slot);
            componentOpenPatchCount[root] += openPatchCount[slot];
            componentSkyExposed[root] |= skyExposed[slot];
            componentMaximumDeltaC[root] = Math.max(
                    componentMaximumDeltaC[root],
                    Math.abs(arena.temperatureC(
                            slot, parameters.referenceTemperatureC())
                            - naturalTemperatureC[slot]));
        }

        TopologyGuard.Decision[] decisions = new TopologyGuard.Decision[capacity];
        FarFieldProfileRegistry.Profile[] continuationProfiles =
                new FarFieldProfileRegistry.Profile[capacity];
        boolean allResolved = allFrontierPatchesMapped;
        FarFieldProfileRegistry.Key profileKey = farFieldSettings.openSpaceKey();
        for (int root = 0; root < capacity; root++) {
            if (componentOpenPatchCount[root] == 0) {
                continue;
            }
            TopologyGuard.OperatingPoint operatingPoint = new TopologyGuard.OperatingPoint(
                    farFieldSettings.calibrationSourcePowerW(),
                    componentMaximumDeltaC[root]);
            TopologyGuard.Decision decision = TopologyGuard.classify(
                    TopologyGuard.Evidence.open(
                            true,
                            true,
                            farFieldSettings.naturalEnvironment(),
                            componentSkyExposed[root],
                            profileKey),
                    operatingPoint,
                    farFieldSettings.profiles());
            decisions[root] = decision;
            if (decision.frontierClass()
                    == TopologyGuard.FrontierClass.OPEN_CONTINUATION) {
                continuationProfiles[root] = applicableFarFieldProfile(
                        profileKey, operatingPoint);
            }
            allResolved &= decision.frontierClass()
                    == TopologyGuard.FrontierClass.OPEN_AMBIENT;
        }

        List<ThermalSweep.BoundaryOperation> boundaries = new ArrayList<>();
        double patchesPerReferenceArea = 16.0D
                * farFieldSettings.referenceOpeningAreaBlocksSquared();
        for (int slot = 0; slot < capacity; slot++) {
            if (openPatchCount[slot] == 0) {
                continue;
            }
            int root = find(parent, slot);
            TopologyGuard.Decision decision = decisions[root];
            if (decision == null) {
                allResolved = false;
                continue;
            }
            double areaScale = farFieldConductanceScale
                    * openPatchCount[slot] / patchesPerReferenceArea;
            if (decision.frontierClass() == TopologyGuard.FrontierClass.OPEN_AMBIENT) {
                decision.boundaryOperation(slot, naturalTemperatureC[slot], areaScale)
                        .ifPresent(boundaries::add);
                continue;
            }
            if (decision.frontierClass()
                    != TopologyGuard.FrontierClass.OPEN_CONTINUATION) {
                continue;
            }
            FarFieldProfileRegistry.Profile profile = continuationProfiles[root];
            if (profile != null) {
                boundaries.add(new ThermalSweep.BoundaryOperation(
                        slot,
                        naturalTemperatureC[slot],
                        profile.conductanceWPerK() * areaScale
                                / (1.0D + farFieldSettings.continuationDistanceBlocks())));
            }
        }
        return new FarFieldCompilation(boundaries, allResolved);
    }

    private FarFieldProfileRegistry.Profile applicableFarFieldProfile(
            FarFieldProfileRegistry.Key key,
            TopologyGuard.OperatingPoint operatingPoint
    ) {
        FarFieldProfileRegistry.Profile profile =
                farFieldSettings.profiles().profile(key).orElse(null);
        return profile != null
                && profile.approval()
                == FarFieldProfileRegistry.Approval.APPROVED_STATIC_IMPEDANCE
                && profile.domain().contains(
                operatingPoint.absoluteSourcePowerW(),
                operatingPoint.absoluteLocalNaturalDeltaC())
                ? profile
                : null;
    }

    private boolean collectOpenFrontierPatches(
            PageState state,
            ConservativeAirGeometry.Face face,
            int[] openPatchCount,
            boolean[] skyExposed,
            double[] naturalTemperatureC
    ) {
        boolean allMapped = true;
        for (int localY = 0; localY < 16; localY++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                for (int localX = 0; localX < 16; localX++) {
                    if (!onFace(localX, localY, localZ, face)) {
                        continue;
                    }
                    int pageBlock = blockIndex(localX, localY, localZ);
                    SignatureGeometry geometry = signatureGeometry(
                            state.appliedSignatureIds[pageBlock]);
                    if (!geometry.resolved) {
                        continue;
                    }
                    int aperture = geometry.geometry.combinedFaceMask(face);
                    for (int patch = 0; patch < 16; patch++) {
                        if ((aperture & (1 << patch)) == 0) {
                            continue;
                        }
                        int slot = cellForMicrocell(
                                state.appliedSignatureIds,
                                state.appliedCoverageRefs,
                                state.appliedMixedGeometry,
                                pageBlock,
                                faceMicrocell(face, patch & 3, patch >>> 2));
                        if (slot == ThermalCellArena.NO_SLOT) {
                            allMapped = false;
                            continue;
                        }
                        openPatchCount[slot]++;
                        int skyCutoff = Byte.toUnsignedInt(state.firstExposedLocalY[
                                (localZ << 4) | localX]);
                        skyExposed[slot] |= localY >= skyCutoff;
                        naturalTemperatureC[slot] = state.naturalTemperatureC;
                    }
                }
            }
        }
        return allMapped;
    }

    private static long neighborSectionKey(
            int sectionX,
            int sectionY,
            int sectionZ,
            ConservativeAirGeometry.Face face
    ) {
        return SectionPos.asLong(
                sectionX + switch (face) {
                    case NEGATIVE_X -> -1;
                    case POSITIVE_X -> 1;
                    default -> 0;
                },
                sectionY + switch (face) {
                    case NEGATIVE_Y -> -1;
                    case POSITIVE_Y -> 1;
                    default -> 0;
                },
                sectionZ + switch (face) {
                    case NEGATIVE_Z -> -1;
                    case POSITIVE_Z -> 1;
                    default -> 0;
                });
    }

    private static void union(int[] parent, int first, int second) {
        int firstRoot = find(parent, first);
        int secondRoot = find(parent, second);
        if (firstRoot != secondRoot) {
            if (firstRoot < secondRoot) {
                parent[secondRoot] = firstRoot;
            } else {
                parent[firstRoot] = secondRoot;
            }
        }
    }

    private static int find(int[] parent, int slot) {
        if (slot < 0 || slot >= parent.length || parent[slot] == ThermalCellArena.NO_SLOT) {
            throw new LatestFrameException();
        }
        int root = slot;
        while (parent[root] != root) {
            root = parent[root];
        }
        while (parent[slot] != slot) {
            int next = parent[slot];
            parent[slot] = root;
            slot = next;
        }
        return root;
    }

    private boolean topologyResolved(
            List<PageState> active,
            CompiledTopology compiled
    ) {
        for (PageState state : active) {
            if (state.unresolvedTopology) {
                return false;
            }
        }
        return compiled.allOpenFrontiersResolved();
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
        pages.entrySet().removeIf(entry -> {
            PageState state = entry.getValue();
            if (state.retirementChunkWatermark > chunkWatermark) {
                return false;
            }
            pagesByPage.remove(state.page);
            return true;
        });
    }

    /** Rebuilds only immediate section neighbors whose material exposure may change. */
    private void propagateMaterialDependencyDirtiness(long chunkWatermark) {
        if (materialBoundaries.profileCount() == 0
                && materialBoundaries.contactPatternCount() == 0) {
            return;
        }
        List<Long> changedSections = new ArrayList<>();
        for (PageState state : pages.values()) {
            if (state.materialDependencyChanged
                    && state.admissionChunkWatermark <= chunkWatermark) {
                changedSections.add(state.page.sectionKey());
            }
        }
        if (changedSections.isEmpty()) {
            return;
        }
        for (PageState candidate : pages.values()) {
            if (candidate.admissionChunkWatermark > chunkWatermark
                    || candidate.retirementChunkWatermark <= chunkWatermark) {
                continue;
            }
            int candidateX = SectionPos.x(candidate.page.sectionKey());
            int candidateY = SectionPos.y(candidate.page.sectionKey());
            int candidateZ = SectionPos.z(candidate.page.sectionKey());
            for (long changed : changedSections) {
                int distance = Math.abs(candidateX - SectionPos.x(changed))
                        + Math.abs(candidateY - SectionPos.y(changed))
                        + Math.abs(candidateZ - SectionPos.z(changed));
                if (distance == 1) {
                    candidate.dirty = true;
                    ensureDesiredSignatureIds(candidate);
                    candidate.desiredGeometryRevision = Math.max(
                            candidate.desiredGeometryRevision,
                            candidate.page.liveGeometryRevision());
                    topologyDirty = true;
                    break;
                }
            }
        }
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

    private static void ensureDesiredSignatureIds(PageState state) {
        if (state.desiredSignatureIds == null) {
            state.desiredSignatureIds = state.appliedSignatureIds.clone();
        }
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

    private static void requireNonNegativeFinite(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(
                    name + " must be finite and non-negative");
        }
    }

    private static byte[] normalizedSkyExposure(byte[] firstExposedLocalY) {
        if (firstExposedLocalY == null
                || firstExposedLocalY.length != SKY_EXPOSURE_COLUMNS) {
            throw new IllegalArgumentException(
                    "sky exposure requires one cutoff for each Page column");
        }
        byte[] normalized = firstExposedLocalY.clone();
        for (byte cutoff : normalized) {
            int localY = Byte.toUnsignedInt(cutoff);
            if (localY > 16) {
                throw new IllegalArgumentException(
                        "sky exposure cutoffs must be in [0, 16]");
            }
        }
        return normalized;
    }

    private static byte[] noSkyExposure() {
        byte[] cutoffs = new byte[SKY_EXPOSURE_COLUMNS];
        Arrays.fill(cutoffs, (byte) 16);
        return cutoffs;
    }

    private record PageIdentity(long sectionKey, long lifecycleGeneration) {
    }

    private record DrainResult(int resolvedInputs, int geometryDeltas) {
    }

    private record RetiredSpan(int pageSlot, int lifecycleGeneration, ArenaSpan span) {
    }

    private record CompiledTopology(
            List<ThermalSweep.PairOperation> pairs,
            List<ThermalSweep.BoundaryOperation> boundaries,
            List<ThermalSweep.PhaseOperation> phaseOperations,
            boolean allOpenFrontiersResolved
    ) {
    }

    private record FarFieldCompilation(
            List<ThermalSweep.BoundaryOperation> boundaries,
            boolean allOpenFrontiersResolved
    ) {
    }

    private record ResolvedMaterial(
            MaterialBoundaryRegistry.Profile profile,
            MaterialBoundaryRegistry.ContactPattern pattern
    ) {
    }

    private record AirMicrocell(
            int blockX,
            int blockY,
            int blockZ,
            int microcellIndex,
            int localRegionId
    ) {
    }

    private record AirRegionKey(
            int blockX,
            int blockY,
            int blockZ,
            int localRegionId
    ) {
        private static AirRegionKey of(AirMicrocell air) {
            return new AirRegionKey(
                    air.blockX(), air.blockY(), air.blockZ(), air.localRegionId());
        }
    }

    private record MaterialSurfaceKey(
            int blockX,
            int blockY,
            int blockZ
    ) {
    }

    private record MaterialPoleKey(
            MaterialSurfaceKey surface,
            ThermalCellArena.MaterialPoleDepth depth
    ) {
        private MaterialPoleKey {
            Objects.requireNonNull(surface, "surface");
            Objects.requireNonNull(depth, "depth");
        }
    }

    private record MaterialSurface(
            MaterialSurfaceKey key,
            MaterialBoundaryRegistry.Profile profile,
            double areaBlocksSquared,
            List<MaterialContact> airContacts
    ) {
    }

    private record MaterialContact(AirMicrocell air, int patchCount) {
    }

    private record PhaseReservoirKey(
            int brickMinX,
            int brickMinY,
            int brickMinZ,
            int materialProfileId
    ) {
    }

    private record PhaseSurface(
            PhaseReservoirKey key,
            MaterialBoundaryRegistry.Profile profile,
            List<MaterialContact> airContacts
    ) {
    }

    private record StatelessBridge(
            AirMicrocell negativeAir,
            AirMicrocell positiveAir,
            double conductanceWPerK
    ) {
    }

    private static final class MutableMaterialSurface {
        private final MaterialSurfaceKey key;
        private final MaterialBoundaryRegistry.Profile profile;
        private final Map<AirRegionKey, MutableMaterialContact> contacts =
                new LinkedHashMap<>();
        private int contactPatchCount;

        private MutableMaterialSurface(
                MaterialSurfaceKey key,
                MaterialBoundaryRegistry.Profile profile
        ) {
            this.key = key;
            this.profile = profile;
        }

        private void addContact(AirMicrocell air) {
            contacts.computeIfAbsent(
                    AirRegionKey.of(air),
                    ignored -> new MutableMaterialContact(air)).patchCount++;
            contactPatchCount++;
        }
    }

    private static final class MutableMaterialContact {
        private final AirMicrocell representative;
        private int patchCount;

        private MutableMaterialContact(AirMicrocell representative) {
            this.representative = representative;
        }
    }

    private static final class MutablePhaseReservoir {
        private final PhaseReservoirKey key;
        private final MaterialBoundaryRegistry.Profile profile;
        private final Map<AirRegionKey, MutableMaterialContact> contacts =
                new LinkedHashMap<>();
        private long candidateMask;

        private MutablePhaseReservoir(
                PhaseReservoirKey key,
                MaterialBoundaryRegistry.Profile profile
        ) {
            this.key = key;
            this.profile = profile;
        }

        private void addContact(int candidateBit, AirMicrocell air) {
            candidateMask |= 1L << candidateBit;
            contacts.computeIfAbsent(
                    AirRegionKey.of(air),
                    ignored -> new MutableMaterialContact(air)).patchCount++;
        }
    }

    private record StatelessBridgeKey(
            AirRegionKey negativeAir,
            AirRegionKey positiveAir
    ) {
    }

    private static final class MutableStatelessBridge {
        private final AirMicrocell negativeAir;
        private final AirMicrocell positiveAir;
        private double conductanceWPerK;

        private MutableStatelessBridge(
                AirMicrocell negativeAir,
                AirMicrocell positiveAir
        ) {
            this.negativeAir = negativeAir;
            this.positiveAir = positiveAir;
        }
    }

    private record SignatureGeometry(
            boolean resolved,
            int mediumId,
            int materialProfileId,
            int materialContactPatternId,
            ConservativeAirGeometry.Resolution geometry
    ) {
        private static final SignatureGeometry UNRESOLVED =
                new SignatureGeometry(false, -1, -1, -1, null);
    }

    private static final class PageState {
        private final ThermalPage page;
        private final int pageSlot;
        private final int lifecycleGeneration;
        private final long admissionChunkWatermark;
        private double naturalTemperatureC;
        private byte[] firstExposedLocalY = NO_SKY_EXPOSURE;
        private int[] appliedSignatureIds = new int[BLOCKS_PER_PAGE];
        private int[] desiredSignatureIds;
        private int[] appliedCoverageRefs;
        private ComponentBrickCompiler.CompiledBrick[] appliedMixedGeometry =
                new ComponentBrickCompiler.CompiledBrick[ThermalPage.BASE_BRICK_COUNT];
        private Map<MaterialPoleKey, Integer> appliedMaterialPoleSlots = Map.of();
        private List<MaterialSurface> appliedMaterialSurfaces = List.of();
        private Map<PhaseReservoirKey, Integer> appliedPhaseReservoirSlots = Map.of();
        private List<PhaseSurface> appliedPhaseReservoirs = List.of();
        private List<StatelessBridge> appliedStatelessBridges = List.of();
        private long retirementChunkWatermark = Long.MAX_VALUE;
        private long desiredGeometryRevision;
        private boolean dirty;
        private ThermalPage.GeometryResyncToken pendingResyncToken;
        private boolean unresolvedTopology;
        private boolean retirementQueued;
        private boolean materialDependencyChanged = true;

        private PageState(
                ThermalPage page,
                int pageSlot,
                int lifecycleGeneration,
                long admissionChunkWatermark,
                int[] initialCoverage,
                double naturalTemperatureC
        ) {
            this.page = page;
            this.pageSlot = pageSlot;
            this.lifecycleGeneration = lifecycleGeneration;
            this.admissionChunkWatermark = admissionChunkWatermark;
            this.naturalTemperatureC = naturalTemperatureC;
            this.appliedCoverageRefs = initialCoverage;
            Arrays.fill(appliedSignatureIds, INITIAL_ALL_AIR);
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
        private final List<ThermalCellArena.MaterialPoleSpec> materialPoles =
                new ArrayList<>();
        private final List<MaterialPoleKey> materialPoleKeys = new ArrayList<>();
        private final List<MaterialSurface> materialSurfaces = new ArrayList<>();
        private final List<ThermalCellArena.PhaseReservoirSpec> phaseReservoirs =
                new ArrayList<>();
        private final List<PhaseReservoirKey> phaseReservoirKeys = new ArrayList<>();
        private final List<PhaseSurface> phaseSurfaces = new ArrayList<>();
        private final List<StatelessBridge> statelessBridges = new ArrayList<>();
        private final Map<StatelessBridgeKey, MutableStatelessBridge>
                statelessBridgeBuilds = new LinkedHashMap<>();
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

        private void addStatelessBridge(
                AirMicrocell negative,
                AirMicrocell positive,
                double conductanceWPerK
        ) {
            StatelessBridgeKey key = new StatelessBridgeKey(
                    AirRegionKey.of(negative), AirRegionKey.of(positive));
            MutableStatelessBridge bridge = statelessBridgeBuilds.computeIfAbsent(
                    key, ignored -> new MutableStatelessBridge(negative, positive));
            bridge.conductanceWPerK += conductanceWPerK;
            if (!Double.isFinite(bridge.conductanceWPerK)) {
                throw new IllegalStateException("stateless wall conductance is not finite");
            }
        }
    }

    private static final class LatestFrameException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
