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
import it.unimi.dsi.fastutil.longs.Long2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.core.SectionPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
        for (int faceOrdinal = 0;
             faceOrdinal < ConservativeAirGeometry.Face.COUNT;
             faceOrdinal++) {
            ConservativeAirGeometry.Face face =
                    ConservativeAirGeometry.Face.fromOrdinal(faceOrdinal);
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
                || arena.phaseProfileId(slot) != materialProfileId) {
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
        state.materialDirtyBrickMask = 0L;
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
        state.desiredBrickMask = -1L;
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
            boolean requiresTopologyCompilation = topologyDirty
                    || hasPendingRetirements(frame.watermarks().chunk());
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

            List<PageState> active = activePages(frame.watermarks().chunk());
            Long2ObjectMap<PageState> activeBySection = indexActivePages(active);
            propagateMaterialDependencyDirtiness(
                    frame.watermarks().chunk(), activeBySection);
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
                        rebuildPage(state, activeBySection);
                        rebuiltPages++;
                    }
                }
                rebuildDirtyMaterials(active, activeBySection);

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
                rebuildDirtyPairFragments(active, activeBySection);

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
                state.desiredBrickMask = -1L;
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
            state.desiredBrickMask |= 1L << baseIndexForBlockIndex(input.blockIndex());
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
                state.desiredBrickMask |= 1L << delta.baseBrickIndex();
                ensureDesiredSignatureIds(state);
                topologyDirty = true;
            }
        }
        return new DrainResult(resolvedCount, deltaCount);
    }

    private void rebuildPage(
            PageState state,
            Long2ObjectMap<PageState> activeBySection
    ) {
        boolean fullBuild = !state.fragmented
                || state.pendingResyncToken != null
                || state.page.coverageRepartitionRequired();
        long brickMask = fullBuild ? -1L : state.desiredBrickMask;
        if (brickMask == 0L) {
            state.dirty = false;
            return;
        }

        int[] oldFirst = state.fragmentFirst.clone();
        int[] oldCount = state.fragmentCount.clone();
        int[] nextFirst = fullBuild
                ? new int[ThermalPage.BASE_BRICK_COUNT]
                : oldFirst.clone();
        int[] nextCount = fullBuild
                ? new int[ThermalPage.BASE_BRICK_COUNT]
                : oldCount.clone();
        int[] nextCoverage = fullBuild
                ? new int[ThermalPage.BASE_BRICK_COUNT]
                : state.appliedCoverageRefs.clone();
        if (fullBuild) {
            Arrays.fill(nextCoverage, ThermalPage.NO_COVERAGE);
        }
        ComponentBrickCompiler.CompiledBrick[] nextMixed = fullBuild
                ? new ComponentBrickCompiler.CompiledBrick[ThermalPage.BASE_BRICK_COUNT]
                : state.appliedMixedGeometry.clone();
        GeometrySummary[] nextBaseSummaries = new GeometrySummary[
                ThermalPage.BASE_BRICK_COUNT];
        for (int baseIndex = 0; baseIndex < ThermalPage.BASE_BRICK_COUNT; baseIndex++) {
            nextBaseSummaries[baseIndex] = state.page.geometrySummary(baseIndex);
        }
        Map<MaterialPoleKey, Integer> nextMaterialSlots = fullBuild
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(state.appliedMaterialPoleSlots);
        Map<PhaseReservoirKey, Integer> nextPhaseSlots = fullBuild
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(state.appliedPhaseReservoirSlots);
        List<MaterialSurface> nextMaterialSurfaces = fullBuild
                ? new ArrayList<>()
                : new ArrayList<>(state.appliedMaterialSurfaces);
        List<PhaseSurface> nextPhaseSurfaces = fullBuild
                ? new ArrayList<>()
                : new ArrayList<>(state.appliedPhaseReservoirs);
        List<StatelessBridge> nextStatelessBridges = fullBuild
                ? new ArrayList<>()
                : new ArrayList<>(state.appliedStatelessBridges);
        PageBuild[] builds = new PageBuild[ThermalPage.BASE_BRICK_COUNT];
        List<ArenaSpan> allocated = new ArrayList<>(Long.bitCount(brickMask));
        GeometryMigrationLedger.MigrationResult migration;

        try {
            long remaining = brickMask;
            while (remaining != 0L) {
                int baseIndex = Long.numberOfTrailingZeros(remaining);
                PageBuild build = compileBrick(state, activeBySection, baseIndex);
                builds[baseIndex] = build;
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
                ArenaSpan span = allocation.cellSpan();
                if (span.count() != 0) {
                    allocated.add(span);
                    nextFirst[baseIndex] = span.firstSlot();
                    nextCount[baseIndex] = span.count();
                } else {
                    nextFirst[baseIndex] = 0;
                    nextCount[baseIndex] = 0;
                }
                int[] localCoverage = buildCoverage(build, allocation);
                nextCoverage[baseIndex] = localCoverage[baseIndex];
                nextMixed[baseIndex] = build.mixedGeometry[baseIndex];
                nextBaseSummaries[baseIndex] = build.baseSummaries[baseIndex];

                removeBrickMaterialState(
                        state, baseIndex, nextMaterialSlots, nextPhaseSlots,
                        nextMaterialSurfaces, nextPhaseSurfaces, nextStatelessBridges);
                nextMaterialSlots.putAll(buildMaterialPoleSlots(build, allocation));
                nextPhaseSlots.putAll(buildPhaseReservoirSlots(build, allocation));
                nextMaterialSurfaces.addAll(build.materialSurfaces);
                nextPhaseSurfaces.addAll(build.phaseSurfaces);
                nextStatelessBridges.addAll(build.statelessBridges);
                remaining &= remaining - 1L;
            }

            int[] oldSlots = collectFragmentSlots(oldFirst, oldCount, brickMask);
            int[] newSlots = collectFragmentSlots(nextFirst, nextCount, brickMask);
            migration = calculateFragmentMigration(
                    state, oldSlots, newSlots, nextCoverage, nextMixed,
                    nextMaterialSlots, nextPhaseSlots, brickMask);
            double[] enthalpies = migration.newEnthalpiesJ();
            for (int index = 0; index < newSlots.length; index++) {
                arena.setEnthalpyJ(newSlots[index], enthalpies[index]);
            }
            migratePhaseRequestState(state, nextPhaseSlots);

            boolean installed;
            if (fullBuild) {
                GeometrySummaryCache summaries = new GeometrySummaryCache();
                long mixedMask = 0L;
                byte[] widths = new byte[ThermalPage.BASE_BRICK_COUNT];
                Arrays.fill(widths, (byte) 4);
                for (int baseIndex = 0;
                     baseIndex < ThermalPage.BASE_BRICK_COUNT;
                     baseIndex++) {
                    summaries.setBaseSummary(baseIndex, nextBaseSummaries[baseIndex]);
                    if (nextMixed[baseIndex] != null) {
                        mixedMask |= 1L << baseIndex;
                    }
                }
                ThermalPage.FullGeometryState geometry = new ThermalPage.FullGeometryState(
                        nextCoverage, widths, summaries.snapshot(), mixedMask, ArenaSpan.EMPTY);
                installed = state.pendingResyncToken == null
                        ? state.page.tryInstallGeometryBuild(
                                state.desiredGeometryRevision, geometry)
                        : state.page.tryInstallFullGeometryResync(
                                state.pendingResyncToken, geometry);
            } else {
                installed = state.page.tryInstallBrickBuilds(
                        state.desiredGeometryRevision,
                        brickMask,
                        nextCoverage,
                        nextBaseSummaries);
            }
            if (!installed) {
                throw new LatestFrameException();
            }
        } catch (RuntimeException exception) {
            for (ArenaSpan span : allocated) {
                if (span.count() != 0 && arena.isLive(span.firstSlot())) {
                    arena.releasePageCells(
                            state.pageSlot, state.lifecycleGeneration, span);
                }
            }
            throw exception;
        }

        migrationLedger.record(migration);
        queueFragmentReleases(state, oldFirst, oldCount, brickMask);
        System.arraycopy(nextFirst, 0, state.fragmentFirst, 0, nextFirst.length);
        System.arraycopy(nextCount, 0, state.fragmentCount, 0, nextCount.length);
        state.fragmented = true;
        state.appliedSignatureIds = state.desiredSignatureIds;
        state.desiredSignatureIds = null;
        state.appliedCoverageRefs = nextCoverage;
        state.appliedMixedGeometry = nextMixed;
        state.appliedMaterialPoleSlots = Map.copyOf(nextMaterialSlots);
        state.appliedMaterialSurfaces = List.copyOf(nextMaterialSurfaces);
        state.appliedPhaseReservoirSlots = Map.copyOf(nextPhaseSlots);
        state.appliedPhaseReservoirs = List.copyOf(nextPhaseSurfaces);
        state.appliedStatelessBridges = List.copyOf(nextStatelessBridges);
        long remaining = brickMask;
        while (remaining != 0L) {
            int baseIndex = Long.numberOfTrailingZeros(remaining);
            PageBuild build = builds[baseIndex];
            state.unresolvedBricks[baseIndex] = build.unresolvedTopology;
            markPairDependencies(state, baseIndex, activeBySection);
            markMaterialNeighbors(state, baseIndex, activeBySection);
            remaining &= remaining - 1L;
        }
        state.materialDirtyBrickMask &= ~brickMask;
        state.unresolvedTopology = anyUnresolved(state.unresolvedBricks);
        state.desiredBrickMask = 0L;
        state.pendingResyncToken = null;
        state.dirty = false;
    }

    private PageBuild compileBrick(
            PageState state,
            Long2ObjectMap<PageState> activeBySection,
            int baseIndex
    ) {
        PageBuild build = new PageBuild();
        int sectionMinX = SectionPos.sectionToBlockCoord(SectionPos.x(state.page.sectionKey()));
        int sectionMinY = SectionPos.sectionToBlockCoord(SectionPos.y(state.page.sectionKey()));
        int sectionMinZ = SectionPos.sectionToBlockCoord(SectionPos.z(state.page.sectionKey()));
        int compilerGeneration = Math.toIntExact(state.page.topologyGeneration() + 1L);
        int[] signatureIds = buildSignatureIds(state);
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
                    int pageBlockIndex = blockIndex(localX, localY, localZ);
                    SignatureGeometry block = signatureGeometry(
                            signatureIds[pageBlockIndex]);
                    if (block.materialProfileId != 0
                            || block.materialContactPatternId != 0) {
                        build.materialCandidateBlocks[
                                build.materialCandidateCount++] = pageBlockIndex;
                    }
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
            build.regularOrdinal[baseIndex] = 0;
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
            } else {
                ComponentBrickCompiler.CompiledBrick brick = compiled.brick().orElseThrow();
                build.mixedOrdinal[baseIndex] = 0;
                build.mixedBricks.add(new ThermalCellArena.MixedBrickSpec(
                        minX, minY, minZ, brick, mediumId,
                        parameters.cellFlags(),
                        parameters.effectiveAirCapacityJPerBlockK()));
                build.mixedGeometry[baseIndex] = brick;
                build.baseSummaries[baseIndex] = GeometrySummary.mixed(
                        GeometrySummary.MATERIAL_INTERFACE);
                build.mixedBrickMask = 1L << baseIndex;
            }
        }
        compileMaterialBoundaries(state, activeBySection, build);
        return build;
    }

    private void compileMaterialBoundaries(
            PageState state,
            Long2ObjectMap<PageState> activeBySection,
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
        int[] signatureIds = buildSignatureIds(state);

        for (int candidate = 0;
             candidate < build.materialCandidateCount;
             candidate++) {
            int pageBlockIndex = build.materialCandidateBlocks[candidate];
            int localX = pageBlockIndex & 15;
            int localZ = (pageBlockIndex >>> 4) & 15;
            int localY = (pageBlockIndex >>> 8) & 15;
            SignatureGeometry signature = signatureGeometry(
                    signatureIds[pageBlockIndex]);
            ResolvedMaterial material = resolveMaterial(signature, build);
            if (material == null) {
                continue;
            }
            int blockX = sectionMinX + localX;
            int blockY = sectionMinY + localY;
            int blockZ = sectionMinZ + localZ;
            if (material.profile().model()
                    == MaterialBoundaryRegistry.Model.STATELESS_CONDUCTANCE) {
                if (material.pattern().materialMicrocellMask() != FULL_MICROCELL_MASK) {
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
                        for (int faceOrdinal = 0;
                             faceOrdinal < ConservativeAirGeometry.Face.COUNT;
                             faceOrdinal++) {
                            ConservativeAirGeometry.Face face =
                                    ConservativeAirGeometry.Face.fromOrdinal(faceOrdinal);
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
                    bridge.owner,
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
            Long2ObjectMap<PageState> activeBySection,
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
                                new MaterialSurfaceKey(blockX, blockY, blockZ),
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
            Long2ObjectMap<PageState> activeBySection,
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
            Long2ObjectMap<PageState> activeBySection,
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
        int[] signatureIds = desired && state.desiredSignatureIds != null
                ? state.desiredSignatureIds
                : state.appliedSignatureIds;
        int signatureId = signatureIds[pageBlock];
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
            long dirtyMask = state.dirty
                    ? (!state.fragmented || state.page.coverageRepartitionRequired()
                            ? -1L
                            : state.desiredBrickMask)
                    : 0L;
            if (dirtyMask != 0L && fragmentsReferenceSource(state, dirtyMask)) {
                return true;
            }
        }
        for (PageState state : pages.values()) {
            if (state.retirementChunkWatermark <= chunkWatermark
                    && fragmentsReferenceSource(state, -1L)) {
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

    private boolean fragmentsReferenceSource(PageState state, long brickMask) {
        long remaining = brickMask;
        while (remaining != 0L) {
            int baseIndex = Long.numberOfTrailingZeros(remaining);
            if (runtime.sourceTopologyReferences(new ArenaSpan(
                    state.fragmentFirst[baseIndex], state.fragmentCount[baseIndex]))) {
                return true;
            }
            remaining &= remaining - 1L;
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

    private void removeBrickMaterialState(
            PageState state,
            int baseIndex,
            Map<MaterialPoleKey, Integer> materialSlots,
            Map<PhaseReservoirKey, Integer> phaseSlots,
            List<MaterialSurface> materialSurfaces,
            List<PhaseSurface> phaseSurfaces,
            List<StatelessBridge> statelessBridges
    ) {
        materialSlots.keySet().removeIf(key -> belongsToBrick(
                state, baseIndex,
                key.surface().blockX(), key.surface().blockY(), key.surface().blockZ()));
        phaseSlots.keySet().removeIf(key -> belongsToBrick(
                state, baseIndex, key.brickMinX(), key.brickMinY(), key.brickMinZ()));
        materialSurfaces.removeIf(surface -> belongsToBrick(
                state, baseIndex,
                surface.key().blockX(), surface.key().blockY(), surface.key().blockZ()));
        phaseSurfaces.removeIf(surface -> belongsToBrick(
                state, baseIndex,
                surface.key().brickMinX(),
                surface.key().brickMinY(),
                surface.key().brickMinZ()));
        statelessBridges.removeIf(bridge -> belongsToBrick(
                state, baseIndex,
                bridge.owner().blockX(), bridge.owner().blockY(), bridge.owner().blockZ()));
    }

    private static boolean belongsToBrick(
            PageState state,
            int baseIndex,
            int blockX,
            int blockY,
            int blockZ
    ) {
        long sectionKey = SectionPos.asLong(
                SectionPos.blockToSectionCoord(blockX),
                SectionPos.blockToSectionCoord(blockY),
                SectionPos.blockToSectionCoord(blockZ));
        return sectionKey == state.page.sectionKey()
                && GeometrySummaryCache.baseIndex(
                        SectionPos.sectionRelative(blockX),
                        SectionPos.sectionRelative(blockY),
                        SectionPos.sectionRelative(blockZ)) == baseIndex;
    }

    private static int[] collectFragmentSlots(
            int[] first,
            int[] count,
            long brickMask
    ) {
        int length = 0;
        long remaining = brickMask;
        while (remaining != 0L) {
            int baseIndex = Long.numberOfTrailingZeros(remaining);
            length = Math.addExact(length, count[baseIndex]);
            remaining &= remaining - 1L;
        }
        int[] slots = new int[length];
        int write = 0;
        remaining = brickMask;
        while (remaining != 0L) {
            int baseIndex = Long.numberOfTrailingZeros(remaining);
            for (int offset = 0; offset < count[baseIndex]; offset++) {
                slots[write++] = first[baseIndex] + offset;
            }
            remaining &= remaining - 1L;
        }
        return slots;
    }

    private GeometryMigrationLedger.MigrationResult calculateFragmentMigration(
            PageState state,
            int[] oldSlots,
            int[] newSlots,
            int[] newCoverage,
            ComponentBrickCompiler.CompiledBrick[] newMixedGeometry,
            Map<MaterialPoleKey, Integer> newMaterialPoleSlots,
            Map<PhaseReservoirKey, Integer> newPhaseReservoirSlots,
            long brickMask
    ) {
        double[] oldEnthalpies = new double[oldSlots.length];
        double[] oldCapacities = new double[oldSlots.length];
        double[] oldTemperatureOffsets = new double[oldSlots.length];
        Int2IntOpenHashMap oldIndices = new Int2IntOpenHashMap(oldSlots.length);
        oldIndices.defaultReturnValue(-1);
        for (int index = 0; index < oldSlots.length; index++) {
            int slot = oldSlots[index];
            oldIndices.put(slot, index);
            oldEnthalpies[index] = arena.enthalpyJ(slot);
            oldCapacities[index] = arena.capacityJPerK(slot);
            oldTemperatureOffsets[index] = oldEnthalpies[index] / oldCapacities[index];
        }

        double[] newCapacities = new double[newSlots.length];
        double[] initialTemperatures = new double[newSlots.length];
        Int2IntOpenHashMap newIndices = new Int2IntOpenHashMap(newSlots.length);
        newIndices.defaultReturnValue(-1);
        for (int index = 0; index < newSlots.length; index++) {
            int slot = newSlots[index];
            newIndices.put(slot, index);
            newCapacities[index] = arena.capacityJPerK(slot);
            initialTemperatures[index] = arena.temperatureC(
                    slot, parameters.referenceTemperatureC());
        }

        double[] oldOverlapCapacities = new double[oldSlots.length];
        double[] newOverlapCapacities = new double[newSlots.length];
        double[] newOverlapEnthalpies = new double[newSlots.length];
        double microcellCapacity = parameters.effectiveAirCapacityJPerBlockK()
                / MICROCELLS_PER_BLOCK;
        long remaining = brickMask;
        while (remaining != 0L) {
            int baseIndex = Long.numberOfTrailingZeros(remaining);
            int minLocalX = (baseIndex & 3) << 2;
            int minLocalZ = ((baseIndex >>> 2) & 3) << 2;
            int minLocalY = ((baseIndex >>> 4) & 3) << 2;
            for (int localY = minLocalY; localY < minLocalY + 4; localY++) {
                for (int localZ = minLocalZ; localZ < minLocalZ + 4; localZ++) {
                    for (int localX = minLocalX; localX < minLocalX + 4; localX++) {
                        int block = blockIndex(localX, localY, localZ);
                        for (int microcell = 0;
                             microcell < MICROCELLS_PER_BLOCK;
                             microcell++) {
                            int oldSlot = cellForMicrocell(
                                    state.appliedSignatureIds,
                                    state.appliedCoverageRefs,
                                    state.appliedMixedGeometry,
                                    block,
                                    microcell);
                            int newSlot = cellForMicrocell(
                                    state.desiredSignatureIds,
                                    newCoverage,
                                    newMixedGeometry,
                                    block,
                                    microcell);
                            int oldIndex = oldIndices.get(oldSlot);
                            int newIndex = newIndices.get(newSlot);
                            if (oldIndex < 0 || newIndex < 0) {
                                continue;
                            }
                            accumulateMigrationOverlap(
                                    oldIndex, newIndex, microcellCapacity,
                                    oldTemperatureOffsets,
                                    oldOverlapCapacities,
                                    newOverlapCapacities,
                                    newOverlapEnthalpies);
                        }
                    }
                }
            }
            remaining &= remaining - 1L;
        }
        for (Map.Entry<MaterialPoleKey, Integer> entry :
                newMaterialPoleSlots.entrySet()) {
            Integer oldSlot = state.appliedMaterialPoleSlots.get(entry.getKey());
            if (oldSlot != null) {
                accumulateCellMigration(
                        oldIndices, newIndices, oldSlot, entry.getValue(),
                        oldTemperatureOffsets, oldOverlapCapacities,
                        newOverlapCapacities, newOverlapEnthalpies);
            }
        }
        for (Map.Entry<PhaseReservoirKey, Integer> entry :
                newPhaseReservoirSlots.entrySet()) {
            Integer oldSlot = state.appliedPhaseReservoirSlots.get(entry.getKey());
            if (oldSlot != null) {
                accumulateCellMigration(
                        oldIndices, newIndices, oldSlot, entry.getValue(),
                        oldTemperatureOffsets, oldOverlapCapacities,
                        newOverlapCapacities, newOverlapEnthalpies);
            }
        }
        return GeometryMigrationLedger.calculateAggregatedGeometryMigration(
                oldEnthalpies,
                oldCapacities,
                newCapacities,
                oldOverlapCapacities,
                newOverlapCapacities,
                newOverlapEnthalpies,
                initialTemperatures,
                parameters.referenceTemperatureC());
    }

    private void accumulateCellMigration(
            Int2IntOpenHashMap oldIndices,
            Int2IntOpenHashMap newIndices,
            int oldSlot,
            int newSlot,
            double[] oldTemperatureOffsets,
            double[] oldOverlapCapacities,
            double[] newOverlapCapacities,
            double[] newOverlapEnthalpies
    ) {
        int oldIndex = oldIndices.get(oldSlot);
        int newIndex = newIndices.get(newSlot);
        if (oldIndex < 0 || newIndex < 0) {
            return;
        }
        accumulateMigrationOverlap(
                oldIndex,
                newIndex,
                Math.min(arena.capacityJPerK(oldSlot), arena.capacityJPerK(newSlot)),
                oldTemperatureOffsets,
                oldOverlapCapacities,
                newOverlapCapacities,
                newOverlapEnthalpies);
    }

    private static void accumulateMigrationOverlap(
            int oldIndex,
            int newIndex,
            double overlapCapacity,
            double[] oldTemperatureOffsets,
            double[] oldOverlapCapacities,
            double[] newOverlapCapacities,
            double[] newOverlapEnthalpies
    ) {
        oldOverlapCapacities[oldIndex] += overlapCapacity;
        newOverlapCapacities[newIndex] += overlapCapacity;
        newOverlapEnthalpies[newIndex] += overlapCapacity
                * oldTemperatureOffsets[oldIndex];
    }

    private void queueFragmentReleases(
            PageState state,
            int[] first,
            int[] count,
            long brickMask
    ) {
        long remaining = brickMask;
        while (remaining != 0L) {
            int baseIndex = Long.numberOfTrailingZeros(remaining);
            queueRelease(
                    state.pageSlot,
                    state.lifecycleGeneration,
                    new ArenaSpan(first[baseIndex], count[baseIndex]));
            remaining &= remaining - 1L;
        }
    }

    private static boolean anyUnresolved(boolean[] unresolvedBricks) {
        for (boolean unresolved : unresolvedBricks) {
            if (unresolved) {
                return true;
            }
        }
        return false;
    }

    private void migratePhaseRequestState(
            PageState state,
            Map<PhaseReservoirKey, Integer> newPhaseReservoirSlots
    ) {
        for (Map.Entry<PhaseReservoirKey, Integer> entry :
                newPhaseReservoirSlots.entrySet()) {
            Integer oldSlot = state.appliedPhaseReservoirSlots.get(entry.getKey());
            if (oldSlot != null
                    && oldSlot.intValue() != entry.getValue().intValue()
                    && arena.phaseRequestOutstanding(oldSlot)) {
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

    private static int[] buildSignatureIds(PageState state) {
        return state.desiredSignatureIds != null
                ? state.desiredSignatureIds
                : state.appliedSignatureIds;
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
            Long2ObjectMap<PageState> activeBySection
    ) {
        List<ThermalSweep.PairOperation> pairs = compilePairs(active);
        Long2DoubleMap materialConductance = new Long2DoubleLinkedOpenHashMap();
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

        for (Long2DoubleMap.Entry entry : materialConductance.long2DoubleEntrySet()) {
            int first = (int) (entry.getLongKey() >>> 32);
            int second = (int) entry.getLongKey();
            pairs.add(ThermalSweep.PairOperation.fixed(
                    first, second, entry.getDoubleValue()));
        }
        return new CompiledTopology(
                pairs,
                boundaries,
                phaseOperations,
                farField.allOpenFrontiersResolved());
    }

    private int airCellForMicrocell(
            AirMicrocell air,
            Long2ObjectMap<PageState> activeBySection
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
            Long2DoubleMap conductances,
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
        double sum = conductances.get(key) + conductanceWPerK;
        if (!Double.isFinite(sum)) {
            throw new IllegalStateException("material conductance sum is not finite");
        }
        conductances.put(key, sum);
    }

    private List<ThermalSweep.PairOperation> compilePairs(List<PageState> active) {
        active.sort(Comparator
                .comparingInt((PageState state) -> SectionPos.x(state.page.sectionKey()))
                .thenComparingInt(state -> SectionPos.y(state.page.sectionKey()))
                .thenComparingInt(state -> SectionPos.z(state.page.sectionKey())));
        List<ThermalSweep.PairOperation> operations = new ArrayList<>();
        for (PageState state : active) {
            for (List<ThermalSweep.PairOperation> fragment : state.airPairFragments) {
                operations.addAll(fragment);
            }
        }
        return operations;
    }

    private FarFieldCompilation compileFarField(
            List<PageState> active,
            Long2ObjectMap<PageState> activeBySection,
            List<ThermalSweep.PairOperation> airPairs
    ) {
        int capacity = arena.highWaterMark();
        int[] parent = new int[capacity];
        int[] openPatchCount = new int[capacity];
        boolean[] skyExposed = new boolean[capacity];
        double[] naturalTemperatureC = new double[capacity];
        Arrays.fill(parent, ThermalCellArena.NO_SLOT);
        Arrays.fill(naturalTemperatureC, Double.NaN);

        boolean[] activePageSlots = new boolean[nextPageSlot];
        for (PageState state : active) {
            activePageSlots[state.pageSlot] = true;
        }
        for (int slot = 0; slot < capacity; slot++) {
            if (arena.isLive(slot)
                    && arena.pageSlot(slot) < activePageSlots.length
                    && activePageSlots[arena.pageSlot(slot)]
                    && !arena.isMaterialPole(slot)
                    && !arena.isPhaseReservoir(slot)) {
                parent[slot] = slot;
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
            for (int faceOrdinal = 0;
                 faceOrdinal < ConservativeAirGeometry.Face.COUNT;
                 faceOrdinal++) {
                ConservativeAirGeometry.Face face =
                        ConservativeAirGeometry.Face.fromOrdinal(faceOrdinal);
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
                queueFragmentReleases(
                        state, state.fragmentFirst, state.fragmentCount, -1L);
                state.retirementQueued = true;
                retired++;
            }
        }
        return retired;
    }

    private boolean hasPendingRetirements(long chunkWatermark) {
        for (PageState state : pages.values()) {
            if (state.retirementChunkWatermark <= chunkWatermark) {
                return true;
            }
        }
        return false;
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

    private void markPairDependencies(
            PageState state,
            int baseIndex,
            Long2ObjectMap<PageState> activeBySection
    ) {
        state.pairDirtyBrickMask |= 1L << baseIndex;
        markNeighborBrick(activeBySection, state, baseIndex, -1, 0, 0, true);
        markNeighborBrick(activeBySection, state, baseIndex, 0, -1, 0, true);
        markNeighborBrick(activeBySection, state, baseIndex, 0, 0, -1, true);
    }

    private void markMaterialNeighbors(
            PageState state,
            int baseIndex,
            Long2ObjectMap<PageState> activeBySection
    ) {
        markNeighborBrick(activeBySection, state, baseIndex, -1, 0, 0, false);
        markNeighborBrick(activeBySection, state, baseIndex, 1, 0, 0, false);
        markNeighborBrick(activeBySection, state, baseIndex, 0, -1, 0, false);
        markNeighborBrick(activeBySection, state, baseIndex, 0, 1, 0, false);
        markNeighborBrick(activeBySection, state, baseIndex, 0, 0, -1, false);
        markNeighborBrick(activeBySection, state, baseIndex, 0, 0, 1, false);
    }

    private static void markNeighborBrick(
            Long2ObjectMap<PageState> activeBySection,
            PageState owner,
            int baseIndex,
            int offsetX,
            int offsetY,
            int offsetZ,
            boolean pair
    ) {
        long sectionKey = owner.page.sectionKey();
        int worldX = SectionPos.sectionToBlockCoord(SectionPos.x(sectionKey))
                + ((baseIndex & 3) << 2) + offsetX * 4;
        int worldY = SectionPos.sectionToBlockCoord(SectionPos.y(sectionKey))
                + (((baseIndex >>> 4) & 3) << 2) + offsetY * 4;
        int worldZ = SectionPos.sectionToBlockCoord(SectionPos.z(sectionKey))
                + (((baseIndex >>> 2) & 3) << 2) + offsetZ * 4;
        PageState target = activeBySection.get(SectionPos.asLong(
                SectionPos.blockToSectionCoord(worldX),
                SectionPos.blockToSectionCoord(worldY),
                SectionPos.blockToSectionCoord(worldZ)));
        if (target == null) {
            return;
        }
        int targetBase = GeometrySummaryCache.baseIndex(
                SectionPos.sectionRelative(worldX),
                SectionPos.sectionRelative(worldY),
                SectionPos.sectionRelative(worldZ));
        if (pair) {
            target.pairDirtyBrickMask |= 1L << targetBase;
        } else {
            target.materialDirtyBrickMask |= 1L << targetBase;
        }
    }

    private void rebuildDirtyMaterials(
            List<PageState> active,
            Long2ObjectMap<PageState> activeBySection
    ) {
        for (PageState state : active) {
            long dirty = state.materialDirtyBrickMask;
            if (dirty == 0L) {
                continue;
            }
            List<MaterialSurface> materialSurfaces =
                    new ArrayList<>(state.appliedMaterialSurfaces);
            List<PhaseSurface> phaseSurfaces =
                    new ArrayList<>(state.appliedPhaseReservoirs);
            List<StatelessBridge> statelessBridges =
                    new ArrayList<>(state.appliedStatelessBridges);
            long remaining = dirty;
            while (remaining != 0L) {
                int baseIndex = Long.numberOfTrailingZeros(remaining);
                PageBuild build = new PageBuild();
                collectMaterialCandidates(state, baseIndex, build);
                compileMaterialBoundaries(state, activeBySection, build);
                removeBrickMaterialDescriptions(
                        state, baseIndex,
                        materialSurfaces, phaseSurfaces, statelessBridges);
                requireMaterialSlots(state, build);
                materialSurfaces.addAll(build.materialSurfaces);
                phaseSurfaces.addAll(build.phaseSurfaces);
                statelessBridges.addAll(build.statelessBridges);
                remaining &= remaining - 1L;
            }
            state.appliedMaterialSurfaces = List.copyOf(materialSurfaces);
            state.appliedPhaseReservoirs = List.copyOf(phaseSurfaces);
            state.appliedStatelessBridges = List.copyOf(statelessBridges);
            state.materialDirtyBrickMask = 0L;
        }
    }

    private void removeBrickMaterialDescriptions(
            PageState state,
            int baseIndex,
            List<MaterialSurface> materialSurfaces,
            List<PhaseSurface> phaseSurfaces,
            List<StatelessBridge> statelessBridges
    ) {
        materialSurfaces.removeIf(surface -> belongsToBrick(
                state, baseIndex,
                surface.key().blockX(), surface.key().blockY(), surface.key().blockZ()));
        phaseSurfaces.removeIf(surface -> belongsToBrick(
                state, baseIndex,
                surface.key().brickMinX(),
                surface.key().brickMinY(),
                surface.key().brickMinZ()));
        statelessBridges.removeIf(bridge -> belongsToBrick(
                state, baseIndex,
                bridge.owner().blockX(), bridge.owner().blockY(), bridge.owner().blockZ()));
    }

    private void collectMaterialCandidates(
            PageState state,
            int baseIndex,
            PageBuild build
    ) {
        int[] signatureIds = buildSignatureIds(state);
        int minLocalX = (baseIndex & 3) << 2;
        int minLocalZ = ((baseIndex >>> 2) & 3) << 2;
        int minLocalY = ((baseIndex >>> 4) & 3) << 2;
        for (int localY = minLocalY; localY < minLocalY + 4; localY++) {
            for (int localZ = minLocalZ; localZ < minLocalZ + 4; localZ++) {
                for (int localX = minLocalX; localX < minLocalX + 4; localX++) {
                    int pageBlockIndex = blockIndex(localX, localY, localZ);
                    SignatureGeometry signature = signatureGeometry(
                            signatureIds[pageBlockIndex]);
                    if (signature.materialProfileId != 0
                            || signature.materialContactPatternId != 0) {
                        build.materialCandidateBlocks[
                                build.materialCandidateCount++] = pageBlockIndex;
                    }
                }
            }
        }
    }

    private static void requireMaterialSlots(PageState state, PageBuild build) {
        for (MaterialPoleKey key : build.materialPoleKeys) {
            if (!state.appliedMaterialPoleSlots.containsKey(key)) {
                throw new LatestFrameException();
            }
        }
        for (PhaseReservoirKey key : build.phaseReservoirKeys) {
            if (!state.appliedPhaseReservoirSlots.containsKey(key)) {
                throw new LatestFrameException();
            }
        }
    }

    private void rebuildDirtyPairFragments(
            List<PageState> active,
            Long2ObjectMap<PageState> activeBySection
    ) {
        for (PageState state : active) {
            long remaining = state.pairDirtyBrickMask;
            if (remaining == 0L) {
                continue;
            }
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
            while (remaining != 0L) {
                int baseIndex = Long.numberOfTrailingZeros(remaining);
                ImplicitAirAdjacency.CompiledPairs compiled =
                        ImplicitAirAdjacency.compileOwnedBrickPairs(
                                owner,
                                neighbors,
                                arena,
                                baseIndex,
                                parameters.effectiveMixingWPerBlockK(),
                                parameters.minimumMixedFaceDistanceBlocks(),
                                parameters.applyBuoyancy());
                if (!compiled.ownerPublicationCurrent()
                        || compiled.unavailablePositivePages() != 0) {
                    throw new LatestFrameException();
                }
                state.airPairFragments[baseIndex] = compiled.operations();
                remaining &= remaining - 1L;
            }
            state.pairDirtyBrickMask = 0L;
        }
    }

    /** Rebuilds only immediate section neighbors whose material exposure may change. */
    private void propagateMaterialDependencyDirtiness(
            long chunkWatermark,
            Long2ObjectMap<PageState> activeBySection
    ) {
        for (PageState state : pages.values()) {
            if (!state.materialDependencyChanged
                    || state.admissionChunkWatermark > chunkWatermark) {
                continue;
            }
            for (int baseIndex = 0;
                 baseIndex < ThermalPage.BASE_BRICK_COUNT;
                 baseIndex++) {
                int brickX = baseIndex & 3;
                int brickZ = (baseIndex >>> 2) & 3;
                int brickY = (baseIndex >>> 4) & 3;
                if (brickX == 0) {
                    markNeighborBrick(
                            activeBySection, state, baseIndex, -1, 0, 0, false);
                    markNeighborBrick(
                            activeBySection, state, baseIndex, -1, 0, 0, true);
                }
                if (brickX == 3) {
                    markNeighborBrick(
                            activeBySection, state, baseIndex, 1, 0, 0, false);
                }
                if (brickY == 0) {
                    markNeighborBrick(
                            activeBySection, state, baseIndex, 0, -1, 0, false);
                    markNeighborBrick(
                            activeBySection, state, baseIndex, 0, -1, 0, true);
                }
                if (brickY == 3) {
                    markNeighborBrick(
                            activeBySection, state, baseIndex, 0, 1, 0, false);
                }
                if (brickZ == 0) {
                    markNeighborBrick(
                            activeBySection, state, baseIndex, 0, 0, -1, false);
                    markNeighborBrick(
                            activeBySection, state, baseIndex, 0, 0, -1, true);
                }
                if (brickZ == 3) {
                    markNeighborBrick(
                            activeBySection, state, baseIndex, 0, 0, 1, false);
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

    private static Long2ObjectMap<PageState> indexActivePages(List<PageState> active) {
        Long2ObjectMap<PageState> bySection = new Long2ObjectOpenHashMap<>(active.size());
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

    private static int baseIndexForBlockIndex(int blockIndex) {
        return ((blockIndex & 15) >>> 2)
                | ((((blockIndex >>> 4) & 15) >>> 2) << 2)
                | ((((blockIndex >>> 8) & 15) >>> 2) << 4);
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
            MaterialSurfaceKey owner,
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
            MaterialSurfaceKey owner,
            AirRegionKey negativeAir,
            AirRegionKey positiveAir
    ) {
    }

    private static final class MutableStatelessBridge {
        private final MaterialSurfaceKey owner;
        private final AirMicrocell negativeAir;
        private final AirMicrocell positiveAir;
        private double conductanceWPerK;

        private MutableStatelessBridge(
                MaterialSurfaceKey owner,
                AirMicrocell negativeAir,
                AirMicrocell positiveAir
        ) {
            this.owner = owner;
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
        private final int[] fragmentFirst = new int[ThermalPage.BASE_BRICK_COUNT];
        private final int[] fragmentCount = new int[ThermalPage.BASE_BRICK_COUNT];
        @SuppressWarnings("unchecked")
        private final List<ThermalSweep.PairOperation>[] airPairFragments =
                (List<ThermalSweep.PairOperation>[]) new List<?>[
                        ThermalPage.BASE_BRICK_COUNT];
        private final boolean[] unresolvedBricks =
                new boolean[ThermalPage.BASE_BRICK_COUNT];
        private long retirementChunkWatermark = Long.MAX_VALUE;
        private long desiredGeometryRevision;
        private long desiredBrickMask;
        private long materialDirtyBrickMask = -1L;
        private long pairDirtyBrickMask = -1L;
        private boolean dirty;
        private boolean fragmented;
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
            Arrays.fill(airPairFragments, List.of());
            ArenaSpan initialSpan = page.cellSpan();
            if (initialSpan.count() != 0) {
                fragmentFirst[0] = initialSpan.firstSlot();
                fragmentCount[0] = initialSpan.count();
            }
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
        private final GeometrySummary[] baseSummaries =
                new GeometrySummary[ThermalPage.BASE_BRICK_COUNT];
        private final ComponentBrickCompiler.CompiledBrick[] mixedGeometry =
                new ComponentBrickCompiler.CompiledBrick[ThermalPage.BASE_BRICK_COUNT];
        private final int[] materialCandidateBlocks = new int[64];
        private int materialCandidateCount;
        private long mixedBrickMask;
        private boolean unresolvedTopology;

        private PageBuild() {
            Arrays.fill(regularOrdinal, -1);
            Arrays.fill(mixedOrdinal, -1);
        }

        private void setNoAir(int baseIndex, boolean unresolved) {
            baseSummaries[baseIndex] = GeometrySummary.noAir(
                    unresolved ? GeometrySummary.UNRESOLVED_TOPOLOGY : 0);
            unresolvedTopology |= unresolved;
        }

        private void addStatelessBridge(
                MaterialSurfaceKey owner,
                AirMicrocell negative,
                AirMicrocell positive,
                double conductanceWPerK
        ) {
            StatelessBridgeKey key = new StatelessBridgeKey(
                    owner,
                    AirRegionKey.of(negative), AirRegionKey.of(positive));
            MutableStatelessBridge bridge = statelessBridgeBuilds.computeIfAbsent(
                    key, ignored -> new MutableStatelessBridge(owner, negative, positive));
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
