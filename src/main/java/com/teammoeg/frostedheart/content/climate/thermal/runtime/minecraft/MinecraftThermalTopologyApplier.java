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
import com.teammoeg.frostedheart.content.climate.thermal.mesh.FarFieldProfileRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.GeometryMigrationLedger;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ImplicitAirAdjacency;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPage;
import com.teammoeg.frostedheart.content.climate.thermal.profile.LocalAirRegionPattern;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalResolution;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.DimensionThermalRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.solver.BuoyancyConductance;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.solver.SealedInputFrame;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSweep;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSweepFragments;
import com.teammoeg.frostedheart.content.climate.thermal.source.SourceBinding;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import net.minecraft.core.SectionPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    private final Map<PageIdentity, PageState> pages = new LinkedHashMap<>();
    private final IdentityHashMap<ThermalPage, PageState> pagesByPage =
            new IdentityHashMap<>();
    private final List<RetiredSpan> spansAwaitingSweep = new ArrayList<>();
    private final IncrementalAirGraph incrementalAirGraph = new IncrementalAirGraph();
    private final IdentityHashMap<PageState, Boolean> dirtyPages =
            new IdentityHashMap<>();
    private final LongOpenHashSet committedSourceBindingSections =
            new LongOpenHashSet();
    private List<PageState> installedActivePages = List.of();
    private Long2ObjectMap<PageState> installedActiveBySection =
            new Long2ObjectOpenHashMap<>();

    private boolean fullTopologyCompilationRequired = true;
    private boolean farFieldConductanceChanged;
    private ThermalSweep installedFragmentSweep;
    private double farFieldConductanceScale = 1.0D;
    private long publicationEpoch;
    private int nextPageSlot;
    private final IntRBTreeSet freePageSlots = new IntRBTreeSet();
    private boolean pendingPageLifecycleChanges;

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
            double calibrationSourcePowerW,
            double referenceOpeningAreaBlocksSquared,
            double continuationDistanceBlocks
    ) {
        public FarFieldSettings {
            Objects.requireNonNull(profiles, "profiles");
            Objects.requireNonNull(environmentClass, "environmentClass");
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
                    0.0D,
                    1.0D,
                    16.0D);
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
        TOPOLOGY_UNCHANGED,
        WRITER_BUSY,
        SOURCE_INPUTS_PENDING,
        FULL_RESYNC_SNAPSHOT_REQUIRED,
        LATEST_FRAME_REQUIRED,
        GENERATION_MISMATCH,
        ACK_REJECTED
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
        state.naturalTemperatureChanged = true;
        dirtyPages.put(state, Boolean.TRUE);
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
        state.skyExposureDirtyBrickMask = -1L;
        dirtyPages.put(state, Boolean.TRUE);
        return true;
    }

    /** Updates one heightmap column without allocating a Page-sized cut. */
    public synchronized boolean updateSkyExposureColumn(
            ThermalPage page,
            int localX,
            int localZ,
            int firstExposedLocalY
    ) {
        Objects.requireNonNull(page, "page");
        if ((localX | localZ | firstExposedLocalY) < 0
                || localX >= 16 || localZ >= 16 || firstExposedLocalY > 16) {
            throw new IllegalArgumentException("sky exposure column is out of bounds");
        }
        PageState state = pagesByPage.get(page);
        int column = (localZ << 4) | localX;
        byte normalized = (byte) firstExposedLocalY;
        if (state == null || state.firstExposedLocalY[column] == normalized) {
            return false;
        }
        if (state.firstExposedLocalY == NO_SKY_EXPOSURE) {
            state.firstExposedLocalY = NO_SKY_EXPOSURE.clone();
        }
        state.firstExposedLocalY[column] = normalized;
        for (int brickY = 0; brickY < 4; brickY++) {
            int baseIndex = GeometrySummaryCache.baseIndex(
                    localX, brickY << 2, localZ);
            state.skyExposureDirtyBrickMask |= 1L << baseIndex;
        }
        dirtyPages.put(state, Boolean.TRUE);
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
        farFieldConductanceChanged = true;
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
            for (int v = 0; v < 16 && !continuation; v++) {
                for (int u = 0; u < 16; u++) {
                    int pageBlock = faceBlockIndex(face, u, v);
                    int localX = pageBlock & 15;
                    int localZ = (pageBlock >>> 4) & 15;
                    int localY = (pageBlock >>> 8) & 15;
                    if (localY >= Byte.toUnsignedInt(state.firstExposedLocalY[
                            (localZ << 4) | localX])) {
                        continue;
                    }
                    SignatureGeometry geometry = signatureGeometry(
                            signatureIds[pageBlock]);
                    if (geometry.resolved
                            && geometry.geometry.combinedFaceMask(face) != 0) {
                        continuation = true;
                        break;
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
        int baseIndex = GeometrySummaryCache.baseIndex(
                SectionPos.sectionRelative(blockX),
                SectionPos.sectionRelative(blockY),
                SectionPos.sectionRelative(blockZ));
        Integer slot = state.appliedPhaseReservoirSlotFragments[baseIndex].get(key);
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

    /** Creates an empty unpublished Page from one loaded main-thread snapshot. */
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
        if (pages.containsKey(identity)
                || freePageSlots.isEmpty() && nextPageSlot == Integer.MAX_VALUE) {
            throw new IllegalStateException("captured Page cannot allocate a unique owner slot");
        }
        int pageSlot;
        if (freePageSlots.isEmpty()) {
            pageSlot = nextPageSlot++;
        } else {
            pageSlot = freePageSlots.firstInt();
            freePageSlots.remove(pageSlot);
        }
        int generation = Math.toIntExact(lifecycleGeneration);
        ThermalPage page = new ThermalPage(sectionKey, lifecycleGeneration);
        PageState state = new PageState(
                page,
                pageSlot,
                generation,
                admissionChunkWatermark,
                naturalTemperatureC);
        state.desiredSignatureIds = normalizedSignatureCut(signatureIds);
        state.desiredGeometryRevision = page.liveGeometryRevision();
        state.desiredBrickMask = -1L;
        state.dirty = true;
        dirtyPages.put(state, Boolean.TRUE);
        pages.put(identity, state);
        pagesByPage.put(page, state);
        pendingPageLifecycleChanges = true;
        int requiredFragments = Math.multiplyExact(
                nextPageSlot, ThermalPage.BASE_BRICK_COUNT);
        if (installedFragmentSweep == null
                || installedFragmentSweep.fragmentCount() < requiredFragments) {
            fullTopologyCompilationRequired = true;
        }
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
        if (runtime.workLimitSuppressed()) {
            return PortResolution.topologyUnavailable();
        }
        long sectionKey = SectionPos.asLong(
                SectionPos.blockToSectionCoord(blockX),
                SectionPos.blockToSectionCoord(blockY),
                SectionPos.blockToSectionCoord(blockZ));
        PageState state = installedActiveBySection.get(sectionKey);
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

    synchronized long[] drainCommittedSourceBindingSections() {
        long[] sections = committedSourceBindingSections.toLongArray();
        committedSourceBindingSections.clear();
        return sections;
    }

    synchronized void requestFullTopologyRecovery() {
        fullTopologyCompilationRequired = true;
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
        dirtyPages.put(state, Boolean.TRUE);
        pendingPageLifecycleChanges = true;
    }

    public synchronized ApplyStatus apply(SealedInputFrame frame) {
        Objects.requireNonNull(frame, "frame");
        if (frame.dimensionGeneration() != runtime.dimensionGeneration()) {
            return ApplyStatus.GENERATION_MISMATCH;
        }
        if (!runtime.tryBeginTopologyUpdate()) {
            return ApplyStatus.WRITER_BUSY;
        }
        boolean writerOwned = true;
        boolean sourceCutPreApplied = false;
        try {
            phaseTransitions.applyAcksThrough(
                    frame.watermarks().transitionAck());
            drain(frame);
            boolean topologyInputsChanged = pendingPageLifecycleChanges
                    || !dirtyPages.isEmpty()
                    || farFieldConductanceChanged
                    || hasPendingRetirements(frame.watermarks().chunk());
            if (runtime.workLimitSuppressed() && topologyInputsChanged) {
                fullTopologyCompilationRequired = true;
            }
            boolean requiresTopologyCompilation = fullTopologyCompilationRequired
                    || topologyInputsChanged;
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
                return statusOf(acknowledged);
            }

            boolean fullCompilation = fullTopologyCompilationRequired
                    || installedFragmentSweep == null;
            boolean activeSetChanged = fullCompilation || pendingPageLifecycleChanges;
            List<PageState> active = activeSetChanged
                    ? activePages(frame.watermarks().chunk())
                    : installedActivePages;
            Long2ObjectMap<PageState> activeBySection = activeSetChanged
                    ? indexActivePages(active)
                    : installedActiveBySection;
            if (activeSetChanged) {
                propagateMaterialDependencyDirtiness(
                        frame.watermarks().chunk(), activeBySection);
            }
            Iterable<PageState> geometryWork = fullCompilation
                    ? active
                    : List.copyOf(dirtyPages.keySet());
            if (!cancelUnchangedBrickMutations(geometryWork)) {
                return ApplyStatus.LATEST_FRAME_REQUIRED;
            }
            for (PageState state : geometryWork) {
                if (state.page.fullGeometryResyncRequired()
                        && !state.hasCurrentResyncSnapshot()) {
                    return ApplyStatus.FULL_RESYNC_SNAPSHOT_REQUIRED;
                }
                if (state.dirty && state.desiredGeometryRevision
                        != state.page.liveGeometryRevision()) {
                    return ApplyStatus.LATEST_FRAME_REQUIRED;
                }
            }
            List<PageState> sourceWork = activeSetChanged
                    ? new ArrayList<>(active)
                    : List.copyOf(dirtyPages.keySet());
            if (pendingPageLifecycleChanges) {
                for (PageState state : dirtyPages.keySet()) {
                    if (state.retirementChunkWatermark <= frame.watermarks().chunk()) {
                        sourceWork.add(state);
                    }
                }
            }
            if (referencesAffectedSource(
                    sourceWork, frame.watermarks().chunk(), fullCompilation)) {
                DimensionThermalRuntime.SourceTopologyBarrierStatus barrier =
                        runtime.preApplySourcesForTopology(frame);
                if (barrier
                        == DimensionThermalRuntime.SourceTopologyBarrierStatus.INPUTS_PENDING
                        || barrier
                        == DimensionThermalRuntime.SourceTopologyBarrierStatus.FRAME_MISMATCH
                        || barrier
                        == DimensionThermalRuntime.SourceTopologyBarrierStatus.UNAVAILABLE) {
                    return ApplyStatus.SOURCE_INPUTS_PENDING;
                }
                sourceCutPreApplied = true;
                if (referencesAffectedSource(
                        sourceWork, frame.watermarks().chunk(), fullCompilation)) {
                    DimensionThermalRuntime.AcknowledgeResult acknowledged;
                    try {
                        acknowledged = finishDeferredTopologyFrame(frame);
                    } finally {
                        writerOwned = false;
                    }
                    if (acknowledged == DimensionThermalRuntime.AcknowledgeResult.APPLIED
                            || acknowledged
                            == DimensionThermalRuntime.AcknowledgeResult.TOPOLOGY_UNCHANGED) {
                        recordDeferredSourceBindingSections(sourceWork);
                    }
                    return statusOf(acknowledged);
                }
            }

            if (activeSetChanged) {
                queueRetirements(frame.watermarks().chunk());
                prepareIncrementalRetirements(
                        frame.watermarks().chunk(), activeBySection);
            }
            try {
                for (PageState state : geometryWork) {
                    if (state.dirty
                            && activeBySection.get(state.page.sectionKey()) == state) {
                        rebuildPage(state, activeBySection);
                    }
                }
                rebuildDirtyMaterials(
                        fullCompilation
                                ? active
                                : activeDirtyPages(activeBySection),
                        activeBySection);

                rebuildDirtyPairFragments(
                        fullCompilation
                                ? active
                                : activeDirtyPages(activeBySection),
                        activeBySection);

                if (!fullCompilation && !hasIncrementalTopologyChanges()) {
                    long nextGeometryRevision = Math.max(
                            runtime.geometryRevision(), frame.watermarks().geometry());
                    DimensionThermalRuntime.AcknowledgeResult acknowledged;
                    try {
                        acknowledged = runtime.finishTopologyUpdate(
                                frame.dimensionGeneration(),
                                frame.watermarks(),
                                nextGeometryRevision,
                                runtime.topologyGeneration(),
                                runtime.topologyResolved(),
                                null);
                    } finally {
                        writerOwned = false;
                    }
                    if (acknowledged == DimensionThermalRuntime.AcknowledgeResult.APPLIED
                            || acknowledged
                            == DimensionThermalRuntime.AcknowledgeResult.TOPOLOGY_UNCHANGED) {
                        recordDeferredSourceBindingSections(sourceWork);
                        publishCommittedGeometry(List.copyOf(dirtyPages.keySet()));
                    } else {
                        prepareFullTopologyRetry();
                    }
                    clearIncrementalUpdateState();
                    return statusOf(acknowledged);
                }

                CompiledTopology compiled = fullCompilation
                        ? compileTopology(active, activeBySection)
                        : compileIncrementalTopology(active, activeBySection);
                boolean topologyResolved = compiled.allOpenFrontiersResolved();

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
                            compiled.sweep());
                } finally {
                    writerOwned = false;
                }
                if (acknowledged != DimensionThermalRuntime.AcknowledgeResult.APPLIED
                        && acknowledged != DimensionThermalRuntime.AcknowledgeResult.TOPOLOGY_UNCHANGED) {
                    prepareFullTopologyRetry();
                    return ApplyStatus.ACK_REJECTED;
                }

                publishCommittedGeometry(fullCompilation
                        ? active : List.copyOf(dirtyPages.keySet()));
                installedFragmentSweep = compiled.sweep();
                fullTopologyCompilationRequired = false;
                recordCommittedSourceBindingSections(
                        activeSetChanged, activeBySection);
                if (activeSetChanged) {
                    installedActivePages = List.copyOf(active);
                    installedActiveBySection = activeBySection;
                }

                releaseCommittedSpans();
                if (activeSetChanged) {
                    removeRetiredPages(frame.watermarks().chunk());
                    for (PageState state : dirtyPages.keySet()) {
                        state.materialDependencyChanged = false;
                    }
                }
                pendingPageLifecycleChanges = false;
                clearIncrementalUpdateState();
                return statusOf(acknowledged);
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
                            == DimensionThermalRuntime.AcknowledgeResult.TOPOLOGY_UNCHANGED) {
                        recordDeferredSourceBindingSections(sourceWork);
                        publishCommittedGeometry(fullCompilation
                                ? active : List.copyOf(dirtyPages.keySet()));
                        releaseCommittedSpans();
                        if (activeSetChanged) {
                            removeRetiredPages(frame.watermarks().chunk());
                        }
                    }
                    return statusOf(acknowledged);
                }
                prepareFullTopologyRetry();
                return ApplyStatus.LATEST_FRAME_REQUIRED;
            } catch (RuntimeException exception) {
                prepareFullTopologyRetry();
                throw exception;
            }
        } finally {
            if (writerOwned) {
                runtime.cancelTopologyUpdate();
            }
        }
    }

    /**
     * Isolates an old in-flight epoch from all newer Page staging. No input
     * ring is drained here; the old source cut is settled against installed
     * spans, then an empty sweep lets that epoch complete without stale
     * transport before the latest frame is applied.
     */
    public synchronized ApplyStatus recoverInFlightEpoch() {
        if (!runtime.tryBeginTopologyUpdate()) {
            return ApplyStatus.WRITER_BUSY;
        }
        boolean writerOwned = true;
        try {
            DimensionThermalRuntime.RecoveryKind recoveryKind =
                    runtime.inFlightRecoveryKind();
            DimensionThermalRuntime.SourceTopologyBarrierStatus barrier =
                    runtime.preApplySourcesForInFlight();
            if (barrier
                    == DimensionThermalRuntime.SourceTopologyBarrierStatus.INPUTS_PENDING
                    || barrier
                    == DimensionThermalRuntime.SourceTopologyBarrierStatus.FRAME_MISMATCH
                    || barrier
                    == DimensionThermalRuntime.SourceTopologyBarrierStatus.UNAVAILABLE) {
                return ApplyStatus.SOURCE_INPUTS_PENDING;
            }
            ThermalSweep disabledSweep = ThermalSweepFragments.builder(
                    arena, null, parameters.buoyancyParameters(), 0).build();
            DimensionThermalRuntime.AcknowledgeResult acknowledged;
            try {
                acknowledged = runtime.finishTopologyUpdate(
                        runtime.dimensionGeneration(),
                        runtime.appliedWatermarks(),
                        runtime.geometryRevision(),
                        Math.incrementExact(runtime.topologyGeneration()),
                        false,
                        disabledSweep);
            } finally {
                writerOwned = false;
            }
            if (acknowledged != DimensionThermalRuntime.AcknowledgeResult.APPLIED
                    && acknowledged
                    != DimensionThermalRuntime.AcknowledgeResult.TOPOLOGY_UNCHANGED) {
                return statusOf(acknowledged);
            }
            boolean workLimitRecovery = recoveryKind
                    == DimensionThermalRuntime.RecoveryKind.WORK_LIMIT;
            installedFragmentSweep = workLimitRecovery ? disabledSweep : null;
            fullTopologyCompilationRequired = !workLimitRecovery;
            for (PageState state : installedActivePages) {
                committedSourceBindingSections.add(state.page.sectionKey());
            }
            runtime.completeInFlightTopologyRecovery();
            return statusOf(acknowledged);
        } finally {
            if (writerOwned) {
                runtime.cancelTopologyUpdate();
            }
        }
    }

    private DimensionThermalRuntime.AcknowledgeResult finishDeferredTopologyFrame(
            SealedInputFrame frame
    ) {
        ThermalSweep disabledSweep = ThermalSweepFragments.builder(
                arena, null, parameters.buoyancyParameters(), 0).build();
        DimensionThermalRuntime.AcknowledgeResult result = runtime.finishTopologyUpdate(
                frame.dimensionGeneration(),
                frame.watermarks(),
                Math.max(runtime.geometryRevision(), frame.watermarks().geometry()),
                Math.incrementExact(runtime.topologyGeneration()),
                false,
                disabledSweep);
        installedFragmentSweep = null;
        fullTopologyCompilationRequired = true;
        return result;
    }

    private void recordDeferredSourceBindingSections(Iterable<PageState> states) {
        for (PageState state : states) {
            committedSourceBindingSections.add(state.page.sectionKey());
        }
    }

    private void publishCommittedGeometry(Iterable<PageState> states) {
        long nextPublicationEpoch = Math.incrementExact(publicationEpoch);
        boolean publishedAny = false;
        for (PageState state : states) {
            if (state.retirementChunkWatermark != Long.MAX_VALUE
                    || state.page.publishedGeometryRevision()
                    == state.page.liveGeometryRevision()
                    && state.page.publishedTopologyGeneration()
                    == state.page.topologyGeneration()) {
                continue;
            }
            publishedAny |= state.page.tryPublishGeometry(
                    state.page.liveGeometryRevision(),
                    state.page.topologyGeneration(),
                    nextPublicationEpoch);
        }
        if (publishedAny) {
            publicationEpoch = nextPublicationEpoch;
        }
    }

    private void prepareFullTopologyRetry() {
        fullTopologyCompilationRequired = true;
    }

    private static ApplyStatus statusOf(
            DimensionThermalRuntime.AcknowledgeResult result
    ) {
        return switch (result) {
            case APPLIED -> ApplyStatus.APPLIED;
            case TOPOLOGY_UNCHANGED -> ApplyStatus.TOPOLOGY_UNCHANGED;
            default -> ApplyStatus.ACK_REJECTED;
        };
    }

    private void drain(SealedInputFrame frame) {
        ResolvedGeometryInputRing.MutableInput input =
                new ResolvedGeometryInputRing.MutableInput();
        while (resolvedInputs.pollThroughWatermark(
                frame.watermarks().geometry(), input)) {
            PageState state = pages.get(new PageIdentity(
                    input.sectionKey(), input.lifecycleGeneration()));
            if (state == null) {
                continue;
            }
            state.desiredGeometryRevision = Math.max(
                    state.desiredGeometryRevision, input.geometryRevision());
            if (input.kind() == ResolvedGeometryInputRing.Kind.FULL_RESYNC_REQUIRED) {
                int[] snapshot = input.fullPageSignatureIds();
                if (snapshot == null
                        || snapshot.length != ResolvedGeometryInputRing.BLOCKS_PER_PAGE) {
                    throw new IllegalStateException(
                            "full resync input is missing its Page snapshot");
                }
                long changedBrickMask = 0L;
                for (int index = 0; index < snapshot.length; index++) {
                    int signatureId = snapshot[index];
                    if (signatureId < 0 || signatures.signature(signatureId).isEmpty()) {
                        snapshot[index] = UNRESOLVED_SIGNATURE;
                    }
                    if (!sameTopologySignature(
                            state.appliedSignatureIds[index], snapshot[index])) {
                        changedBrickMask |= 1L << baseIndexForBlockIndex(index);
                    }
                }
                ThermalPage.GeometryResyncToken token =
                        new ThermalPage.GeometryResyncToken(
                                input.sectionKey(),
                                input.lifecycleGeneration(),
                                input.geometryRevision(),
                                input.geometryResyncReason());
                if (changedBrickMask == 0L
                        && state.fragmented
                        && state.page.tryAcknowledgeUnchangedFullGeometryResync(token)) {
                    state.appliedSignatureIds = snapshot;
                    state.desiredSignatureIds = null;
                    state.desiredBrickMask = 0L;
                    state.pendingResyncToken = null;
                    state.dirty = false;
                    dirtyPages.put(state, Boolean.TRUE);
                    continue;
                }
                state.dirty = true;
                dirtyPages.put(state, Boolean.TRUE);
                state.desiredSignatureIds = snapshot;
                state.desiredBrickMask = changedBrickMask;
                state.pendingResyncToken = token;
                continue;
            }
            state.dirty = true;
            dirtyPages.put(state, Boolean.TRUE);
            ensureDesiredSignatureIds(state);
            state.desiredSignatureIds[input.blockIndex()] =
                    input.status() == ThermalResolution.Status.RESOLVED
                            && signatures.signature(input.signatureId()).isPresent()
                            ? input.signatureId()
                            : UNRESOLVED_SIGNATURE;
            state.desiredBrickMask |= 1L << baseIndexForBlockIndex(input.blockIndex());
        }

        GeometryDeltaRing.MutableGeometryDelta delta =
                new GeometryDeltaRing.MutableGeometryDelta();
        while (geometryDeltas.pollThroughTick(frame.effectiveTick(), delta)) {
            PageState state = pages.get(new PageIdentity(
                    delta.sectionKey(), delta.lifecycleGeneration()));
            if (state != null) {
                state.desiredGeometryRevision = Math.max(
                        state.desiredGeometryRevision, delta.geometryRevision());
                state.dirty = true;
                dirtyPages.put(state, Boolean.TRUE);
                state.desiredBrickMask |= 1L << delta.baseBrickIndex();
                ensureDesiredSignatureIds(state);
            }
        }
    }

    private void rebuildPage(
            PageState state,
            Long2ObjectMap<PageState> activeBySection
    ) {
        state.lastUnresolvedTopology = state.unresolvedTopology;
        boolean fullBuild = !state.fragmented;
        long brickMask = fullBuild ? -1L : state.desiredBrickMask;
        if (brickMask == 0L) {
            if (state.pendingResyncToken != null) {
                if (!state.page.tryAcknowledgeUnchangedFullGeometryResync(
                        state.pendingResyncToken)) {
                    throw new LatestFrameException();
                }
                state.appliedSignatureIds = state.desiredSignatureIds;
                state.desiredSignatureIds = null;
                state.desiredBrickMask = 0L;
                state.pendingResyncToken = null;
            }
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
        Map<MaterialPoleKey, Integer>[] nextMaterialSlots = fullBuild
                ? emptyMapFragmentArray()
                : state.appliedMaterialPoleSlotFragments.clone();
        Map<PhaseReservoirKey, Integer>[] nextPhaseSlots = fullBuild
                ? emptyMapFragmentArray()
                : state.appliedPhaseReservoirSlotFragments.clone();
        List<MaterialSurface>[] nextMaterialSurfaces = fullBuild
                ? emptyFragmentArray()
                : state.appliedMaterialSurfaceFragments.clone();
        List<PhaseSurface>[] nextPhaseSurfaces = fullBuild
                ? emptyFragmentArray()
                : state.appliedPhaseReservoirFragments.clone();
        List<StatelessBridge>[] nextStatelessBridges = fullBuild
                ? emptyFragmentArray()
                : state.appliedStatelessBridgeFragments.clone();
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

                nextMaterialSlots[baseIndex] = immutableMapPreservingOrder(
                        buildMaterialPoleSlots(build, allocation));
                nextPhaseSlots[baseIndex] = immutableMapPreservingOrder(
                        buildPhaseReservoirSlots(build, allocation));
                nextMaterialSurfaces[baseIndex] = List.copyOf(build.materialSurfaces);
                nextPhaseSurfaces[baseIndex] = List.copyOf(build.phaseSurfaces);
                nextStatelessBridges[baseIndex] = List.copyOf(build.statelessBridges);
                remaining &= remaining - 1L;
            }

            int[] oldSlots = collectFragmentSlots(oldFirst, oldCount, brickMask);
            int[] newSlots = collectFragmentSlots(nextFirst, nextCount, brickMask);
            state.lastRemovedAirSlots = filterAirSlots(oldSlots);
            state.lastAddedAirSlots = filterAirSlots(newSlots);
            migration = calculateFragmentMigration(
                    state, oldSlots, newSlots, nextCoverage, nextMixed,
                    nextMaterialSlots, nextPhaseSlots,
                    brickMask);
            double[] enthalpies = migration.newEnthalpiesJ();
            for (int index = 0; index < newSlots.length; index++) {
                arena.setEnthalpyJ(newSlots[index], enthalpies[index]);
            }
            migratePhaseRequestState(
                    state, nextPhaseSlots, brickMask);

            boolean installed;
            if (fullBuild) {
                long mixedMask = 0L;
                for (int baseIndex = 0;
                     baseIndex < ThermalPage.BASE_BRICK_COUNT;
                     baseIndex++) {
                    if (nextMixed[baseIndex] != null) {
                        mixedMask |= 1L << baseIndex;
                    }
                }
                ThermalPage.FullGeometryState geometry = new ThermalPage.FullGeometryState(
                        nextCoverage, nextBaseSummaries, mixedMask);
                installed = state.pendingResyncToken == null
                        ? state.page.tryInstallGeometryBuild(
                                state.desiredGeometryRevision, geometry)
                        : state.page.tryInstallFullGeometryResync(
                                state.pendingResyncToken, geometry);
            } else if (state.pendingResyncToken != null) {
                installed = state.page.tryInstallBrickFullGeometryResync(
                        state.pendingResyncToken,
                        brickMask,
                        nextCoverage,
                        nextBaseSummaries);
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

        queueFragmentReleases(state, oldFirst, oldCount, brickMask);
        System.arraycopy(nextFirst, 0, state.fragmentFirst, 0, nextFirst.length);
        System.arraycopy(nextCount, 0, state.fragmentCount, 0, nextCount.length);
        state.fragmented = true;
        state.appliedSignatureIds = state.desiredSignatureIds;
        state.desiredSignatureIds = null;
        state.appliedCoverageRefs = nextCoverage;
        state.appliedMixedGeometry = nextMixed;
        state.appliedBaseSummaries = nextBaseSummaries;
        state.appliedMaterialPoleSlotFragments = nextMaterialSlots;
        state.appliedMaterialSurfaceFragments = nextMaterialSurfaces;
        state.appliedPhaseReservoirSlotFragments = nextPhaseSlots;
        state.appliedPhaseReservoirFragments = nextPhaseSurfaces;
        state.appliedStatelessBridgeFragments = nextStatelessBridges;
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
        state.lastRebuiltGeometryBrickMask |= brickMask;
        state.lastRebuiltMaterialBrickMask |= brickMask;
        state.unresolvedTopology = anyUnresolved(state.unresolvedBricks);
        state.desiredBrickMask = 0L;
        state.pendingResyncToken = null;
        state.dirty = false;
    }

    private boolean cancelUnchangedBrickMutations(Iterable<PageState> active) {
        for (PageState state : active) {
            if (!state.dirty
                    || !state.fragmented
                    || state.pendingResyncToken != null) {
                continue;
            }
            long unchanged = 0L;
            long remaining = state.desiredBrickMask;
            while (remaining != 0L) {
                int baseIndex = Long.numberOfTrailingZeros(remaining);
                if (brickTopologyUnchanged(state, baseIndex)) {
                    unchanged |= 1L << baseIndex;
                }
                remaining &= remaining - 1L;
            }
            if (unchanged == 0L) {
                continue;
            }
            if (!state.page.tryAcknowledgeUnchangedBricks(
                    state.desiredGeometryRevision,
                    unchanged,
                    state.appliedCoverageRefs,
                    state.appliedBaseSummaries)) {
                return false;
            }
            copyBrickSignatures(
                    state.desiredSignatureIds,
                    state.appliedSignatureIds,
                    unchanged);
            state.desiredBrickMask &= ~unchanged;
            if (state.desiredBrickMask == 0L) {
                state.desiredSignatureIds = null;
                state.pendingResyncToken = null;
                state.dirty = false;
            }
        }
        return true;
    }

    private boolean brickTopologyUnchanged(PageState state, int baseIndex) {
        int minX = (baseIndex & 3) << 2;
        int minZ = ((baseIndex >>> 2) & 3) << 2;
        int minY = ((baseIndex >>> 4) & 3) << 2;
        for (int localY = minY; localY < minY + 4; localY++) {
            for (int localZ = minZ; localZ < minZ + 4; localZ++) {
                for (int localX = minX; localX < minX + 4; localX++) {
                    int block = blockIndex(localX, localY, localZ);
                    if (!sameTopologySignature(
                            state.appliedSignatureIds[block],
                            state.desiredSignatureIds[block])) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static void copyBrickSignatures(
            int[] source,
            int[] target,
            long brickMask
    ) {
        long remaining = brickMask;
        while (remaining != 0L) {
            int baseIndex = Long.numberOfTrailingZeros(remaining);
            int minX = (baseIndex & 3) << 2;
            int minZ = ((baseIndex >>> 2) & 3) << 2;
            int minY = ((baseIndex >>> 4) & 3) << 2;
            for (int localY = minY; localY < minY + 4; localY++) {
                for (int localZ = minZ; localZ < minZ + 4; localZ++) {
                    int first = blockIndex(minX, localY, localZ);
                    System.arraycopy(source, first, target, first, 4);
                }
            }
            remaining &= remaining - 1L;
        }
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
                    minX, minY, minZ, mediumId, parameters.cellFlags(),
                    parameters.effectiveAirCapacityJPerBlockK()));
            build.baseSummaries[baseIndex] = GeometrySummary.singleAir(mediumId);
        } else {
            ComponentBrickCompiler.Compilation compiled = ComponentBrickCompiler.compile(
                    geometry, parameters.maximumRegionsPerBlock());
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
        for (ImplicitAirAdjacency.Axis axis : ImplicitAirAdjacency.Axis.values()) {
            for (int v = 0; v < 4; v++) {
                for (int u = 0; u < 4; u++) {
                    AirMicrocell negative;
                    AirMicrocell positive;
                    if (axis == ImplicitAirAdjacency.Axis.X) {
                        negative = airMicrocellIfPresent(
                                blockX - 1, blockY, blockZ,
                                3, v, u, activeBySection, true);
                        positive = airMicrocellIfPresent(
                                blockX + 1, blockY, blockZ,
                                0, v, u, activeBySection, true);
                    } else if (axis == ImplicitAirAdjacency.Axis.Y) {
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
            long chunkWatermark,
            boolean includeRetirements
    ) {
        for (PageState state : active) {
            long dirtyMask = state.dirty
                    ? (!state.fragmented ? -1L : state.desiredBrickMask)
                    : 0L;
            if (dirtyMask != 0L && fragmentsReferenceSource(state, dirtyMask)) {
                return true;
            }
        }
        if (includeRetirements) {
            for (PageState state : pages.values()) {
                if (state.retirementChunkWatermark <= chunkWatermark
                        && fragmentsReferenceSource(state, -1L)) {
                    return true;
                }
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

    private int[] filterAirSlots(int[] slots) {
        int count = 0;
        for (int slot : slots) {
            if (arena.isLive(slot)
                    && !arena.isMaterialPole(slot)
                    && !arena.isPhaseReservoir(slot)) {
                count++;
            }
        }
        int[] air = new int[count];
        int write = 0;
        for (int slot : slots) {
            if (arena.isLive(slot)
                    && !arena.isMaterialPole(slot)
                    && !arena.isPhaseReservoir(slot)) {
                air[write++] = slot;
            }
        }
        return air;
    }

    private GeometryMigrationLedger.MigrationResult calculateFragmentMigration(
            PageState state,
            int[] oldSlots,
            int[] newSlots,
            int[] newCoverage,
            ComponentBrickCompiler.CompiledBrick[] newMixedGeometry,
            Map<MaterialPoleKey, Integer>[] newMaterialPoleSlots,
            Map<PhaseReservoirKey, Integer>[] newPhaseReservoirSlots,
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
        remaining = brickMask;
        while (remaining != 0L) {
            int baseIndex = Long.numberOfTrailingZeros(remaining);
            Map<MaterialPoleKey, Integer> oldFragment =
                    state.appliedMaterialPoleSlotFragments[baseIndex];
            for (Map.Entry<MaterialPoleKey, Integer> entry :
                    newMaterialPoleSlots[baseIndex].entrySet()) {
                Integer oldSlot = oldFragment.get(entry.getKey());
                if (oldSlot != null) {
                    accumulateCellMigration(
                            oldIndices, newIndices, oldSlot, entry.getValue(),
                            oldTemperatureOffsets, oldOverlapCapacities,
                            newOverlapCapacities, newOverlapEnthalpies);
                }
            }
            remaining &= remaining - 1L;
        }
        remaining = brickMask;
        while (remaining != 0L) {
            int baseIndex = Long.numberOfTrailingZeros(remaining);
            Map<PhaseReservoirKey, Integer> oldFragment =
                    state.appliedPhaseReservoirSlotFragments[baseIndex];
            for (Map.Entry<PhaseReservoirKey, Integer> entry :
                    newPhaseReservoirSlots[baseIndex].entrySet()) {
                Integer oldSlot = oldFragment.get(entry.getKey());
                if (oldSlot != null) {
                    accumulateCellMigration(
                            oldIndices, newIndices, oldSlot, entry.getValue(),
                            oldTemperatureOffsets, oldOverlapCapacities,
                            newOverlapCapacities, newOverlapEnthalpies);
                }
            }
            remaining &= remaining - 1L;
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
            Map<PhaseReservoirKey, Integer>[] newPhaseReservoirSlots,
            long brickMask
    ) {
        long remaining = brickMask;
        while (remaining != 0L) {
            int baseIndex = Long.numberOfTrailingZeros(remaining);
            Map<PhaseReservoirKey, Integer> oldFragment =
                    state.appliedPhaseReservoirSlotFragments[baseIndex];
            for (Map.Entry<PhaseReservoirKey, Integer> entry :
                    newPhaseReservoirSlots[baseIndex].entrySet()) {
                Integer oldSlot = oldFragment.get(entry.getKey());
                if (oldSlot != null
                        && oldSlot.intValue() != entry.getValue().intValue()
                        && arena.phaseRequestOutstanding(oldSlot)) {
                    arena.copyPhaseRequestState(oldSlot, entry.getValue());
                }
            }
            remaining &= remaining - 1L;
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

    private boolean sameTopologySignature(int firstSignatureId, int secondSignatureId) {
        return firstSignatureId == secondSignatureId
                || signatureGeometry(firstSignatureId).equals(
                        signatureGeometry(secondSignatureId));
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
        sortActivePages(active);
        int fragmentCount = Math.multiplyExact(
                nextPageSlot, ThermalPage.BASE_BRICK_COUNT);
        ThermalSweepFragments.Builder sweep = ThermalSweepFragments.builder(
                arena,
                phaseTransitions,
                parameters.buoyancyParameters(),
                fragmentCount);
        sweep.setFragmentOrder(fragmentOrder(active));
        for (PageState state : active) {
            state.sweepFragmentOffset = Math.multiplyExact(
                    state.pageSlot, ThermalPage.BASE_BRICK_COUNT);
            for (int baseIndex = 0;
                 baseIndex < ThermalPage.BASE_BRICK_COUNT;
                 baseIndex++) {
                int fragment = state.sweepFragmentOffset + baseIndex;
                sweep.setAirPairs(fragment, state.airPairFragments[baseIndex]);
                CompiledMaterialFragment material = compileMaterialFragment(
                        state, baseIndex, activeBySection);
                sweep.setMaterial(
                        fragment,
                        material.pairs,
                        material.boundaries,
                        material.phases);
            }
        }
        boolean allOpenFrontiersResolved = incrementalAirGraph.rebuildFull(
                active, activeBySection, sweep);
        return new CompiledTopology(
                sweep.build(),
                allOpenFrontiersResolved);
    }

    private CompiledTopology compileIncrementalTopology(
            List<PageState> active,
            Long2ObjectMap<PageState> activeBySection
    ) {
        ThermalSweepFragments.Patch patch =
                installedFragmentSweep.beginFragmentPatch();
        if (pendingPageLifecycleChanges) {
            sortActivePages(active);
            patch.replaceFragmentOrder(fragmentOrder(active));
        }
        for (PageState state : dirtyPages.keySet()) {
            long remaining = state.lastRebuiltPairBrickMask;
            while (remaining != 0L) {
                int baseIndex = Long.numberOfTrailingZeros(remaining);
                patch.replaceAirPairs(
                        state.sweepFragmentOffset + baseIndex,
                        state.airPairFragments[baseIndex]);
                remaining &= remaining - 1L;
            }
        }

        for (PageState state : dirtyPages.keySet()) {
            long remaining = state.lastRebuiltMaterialBrickMask;
            if (remaining == 0L) {
                continue;
            }
            while (remaining != 0L) {
                int baseIndex = Long.numberOfTrailingZeros(remaining);
                CompiledMaterialFragment material = isRetiring(
                        state, activeBySection)
                        ? CompiledMaterialFragment.EMPTY
                        : compileMaterialFragment(
                                state, baseIndex, activeBySection);
                patch.replaceMaterial(
                        state.sweepFragmentOffset + baseIndex,
                        material.pairs,
                        material.boundaries,
                        material.phases);
                remaining &= remaining - 1L;
            }
        }
        boolean allResolved = incrementalAirGraph.patch(activeBySection, patch);
        return new CompiledTopology(
                installedFragmentSweep.withFragmentPatch(patch), allResolved);
    }

    private static int[] fragmentOrder(List<PageState> active) {
        int[] order = new int[Math.multiplyExact(
                active.size(), ThermalPage.BASE_BRICK_COUNT)];
        int write = 0;
        for (PageState state : active) {
            for (int baseIndex = 0;
                 baseIndex < ThermalPage.BASE_BRICK_COUNT;
                 baseIndex++) {
                order[write++] = state.sweepFragmentOffset + baseIndex;
            }
        }
        return order;
    }

    private void clearIncrementalUpdateState() {
        for (PageState state : dirtyPages.keySet()) {
            state.lastRebuiltGeometryBrickMask = 0L;
            state.lastRebuiltPairBrickMask = 0L;
            state.lastRebuiltMaterialBrickMask = 0L;
            state.lastRemovedAirSlots = new int[0];
            state.lastAddedAirSlots = new int[0];
            state.lastUnresolvedTopology = state.unresolvedTopology;
            state.naturalTemperatureChanged = false;
            state.skyExposureDirtyBrickMask = 0L;
        }
        dirtyPages.clear();
        farFieldConductanceChanged = false;
    }

    private List<PageState> activeDirtyPages(
            Long2ObjectMap<PageState> activeBySection
    ) {
        List<PageState> active = new ArrayList<>();
        for (PageState state : dirtyPages.keySet()) {
            if (!isRetiring(state, activeBySection)) {
                active.add(state);
            }
        }
        return active;
    }

    private static boolean isRetiring(
            PageState state,
            Long2ObjectMap<PageState> activeBySection
    ) {
        return activeBySection.get(state.page.sectionKey()) != state;
    }

    private void recordCommittedSourceBindingSections(
            boolean fullCompilation,
            Long2ObjectMap<PageState> activeBySection
    ) {
        for (PageState state : dirtyPages.keySet()) {
            if (state.lastRebuiltGeometryBrickMask != 0L) {
                committedSourceBindingSections.add(state.page.sectionKey());
            }
        }
        if (!fullCompilation) {
            return;
        }
        for (long sectionKey : installedActiveBySection.keySet()) {
            if (!activeBySection.containsKey(sectionKey)) {
                committedSourceBindingSections.add(sectionKey);
            }
        }
        for (long sectionKey : activeBySection.keySet()) {
            if (!installedActiveBySection.containsKey(sectionKey)) {
                committedSourceBindingSections.add(sectionKey);
            }
        }
    }

    private boolean hasIncrementalTopologyChanges() {
        if (farFieldConductanceChanged) {
            return true;
        }
        for (PageState state : dirtyPages.keySet()) {
            if (state.lastRebuiltGeometryBrickMask != 0L
                    || state.lastRebuiltPairBrickMask != 0L
                    || state.lastRebuiltMaterialBrickMask != 0L
                    || state.naturalTemperatureChanged
                    || state.skyExposureDirtyBrickMask != 0L) {
                return true;
            }
        }
        return false;
    }

    private CompiledMaterialFragment compileMaterialFragment(
            PageState state,
            int baseIndex,
            Long2ObjectMap<PageState> activeBySection
    ) {
        List<ThermalSweep.PairOperation> pairs = new ArrayList<>();
        List<ThermalSweep.BoundaryOperation> boundaries = new ArrayList<>();
        List<ThermalSweep.PhaseOperation> phases = new ArrayList<>();
        for (StatelessBridge bridge
                : state.appliedStatelessBridgeFragments[baseIndex]) {
            int negative = airCellForMicrocell(
                    bridge.negativeAir(), activeBySection);
            int positive = airCellForMicrocell(
                    bridge.positiveAir(), activeBySection);
            if (negative != positive) {
                pairs.add(ThermalSweep.PairOperation.fixed(
                        negative, positive, bridge.conductanceWPerK()));
            }
        }
        Map<MaterialPoleKey, Integer> materialSlots =
                state.appliedMaterialPoleSlotFragments[baseIndex];
        for (MaterialSurface surface
                : state.appliedMaterialSurfaceFragments[baseIndex]) {
            Integer surfaceSlot = materialSlots.get(new MaterialPoleKey(
                    surface.key(), ThermalCellArena.MaterialPoleDepth.SURFACE));
            if (surfaceSlot == null) {
                throw new LatestFrameException();
            }
            double patchConductance =
                    surface.profile().faceConductanceWPerK() / 16.0D;
            for (MaterialContact contact : surface.airContacts()) {
                int air = airCellForMicrocell(contact.air(), activeBySection);
                if (air != surfaceSlot) {
                    pairs.add(ThermalSweep.PairOperation.fixed(
                            air,
                            surfaceSlot,
                            patchConductance * contact.patchCount()));
                }
            }
            if (surface.profile().model()
                    != MaterialBoundaryRegistry.Model.NATURAL_ROCK) {
                continue;
            }
            double naturalTemperature = surface.profile().naturalTemperatureC(
                    surface.key().blockY());
            Integer deepSlot = materialSlots.get(new MaterialPoleKey(
                    surface.key(), ThermalCellArena.MaterialPoleDepth.DEEP));
            if (deepSlot == null) {
                boundaries.add(new ThermalSweep.BoundaryOperation(
                        surfaceSlot,
                        naturalTemperature,
                        surface.profile().deepConductanceWPerK()
                                * surface.areaBlocksSquared()));
            } else {
                pairs.add(ThermalSweep.PairOperation.fixed(
                        surfaceSlot,
                        deepSlot,
                        surface.profile().deepConductanceWPerK()
                                * surface.areaBlocksSquared()));
                boundaries.add(new ThermalSweep.BoundaryOperation(
                        deepSlot,
                        naturalTemperature,
                        surface.profile().naturalConductanceWPerK()
                                * surface.areaBlocksSquared()));
            }
        }
        Map<PhaseReservoirKey, Integer> phaseSlots =
                state.appliedPhaseReservoirSlotFragments[baseIndex];
        for (PhaseSurface phase
                : state.appliedPhaseReservoirFragments[baseIndex]) {
            Integer phaseSlot = phaseSlots.get(phase.key());
            if (phaseSlot == null) {
                throw new LatestFrameException();
            }
            double patchConductance =
                    phase.profile().faceConductanceWPerK() / 16.0D;
            for (MaterialContact contact : phase.airContacts()) {
                phases.add(new ThermalSweep.PhaseOperation(
                        airCellForMicrocell(contact.air(), activeBySection),
                        phaseSlot,
                        patchConductance * contact.patchCount()));
            }
        }
        return new CompiledMaterialFragment(pairs, boundaries, phases);
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

    private static void sortActivePages(List<PageState> active) {
        active.sort(Comparator
                .comparingInt((PageState state) -> SectionPos.x(state.page.sectionKey()))
                .thenComparingInt(state -> SectionPos.y(state.page.sectionKey()))
                .thenComparingInt(state -> SectionPos.z(state.page.sectionKey())));
    }

    private FarFieldProfileRegistry.Profile applicableFarFieldProfile(
            double absoluteSourcePowerW,
            double absoluteLocalNaturalDeltaC
    ) {
        FarFieldProfileRegistry.Profile profile =
                farFieldSettings.profiles().profile(
                        farFieldSettings.environmentClass()).orElse(null);
        return profile != null
                && profile.domain().contains(
                        absoluteSourcePowerW,
                        absoluteLocalNaturalDeltaC)
                ? profile
                : null;
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

    private OpenPatchFragment compileOpenPatchFragment(
            PageState state,
            int baseIndex,
            Long2ObjectMap<PageState> activeBySection
    ) {
        int brickX = baseIndex & 3;
        int brickZ = (baseIndex >>> 2) & 3;
        int brickY = (baseIndex >>> 4) & 3;
        int sectionX = SectionPos.x(state.page.sectionKey());
        int sectionY = SectionPos.y(state.page.sectionKey());
        int sectionZ = SectionPos.z(state.page.sectionKey());
        Int2IntOpenHashMap counts = new Int2IntOpenHashMap();
        IntOpenHashSet sky = new IntOpenHashSet();
        int unmapped = 0;
        for (int faceOrdinal = 0;
             faceOrdinal < ConservativeAirGeometry.Face.COUNT;
             faceOrdinal++) {
            ConservativeAirGeometry.Face face =
                    ConservativeAirGeometry.Face.fromOrdinal(faceOrdinal);
            boolean ownsPageFace = switch (face) {
                case NEGATIVE_X -> brickX == 0;
                case POSITIVE_X -> brickX == 3;
                case NEGATIVE_Y -> brickY == 0;
                case POSITIVE_Y -> brickY == 3;
                case NEGATIVE_Z -> brickZ == 0;
                case POSITIVE_Z -> brickZ == 3;
            };
            if (!ownsPageFace || activeBySection.containsKey(
                    neighborSectionKey(sectionX, sectionY, sectionZ, face))) {
                continue;
            }
            int minX = brickX << 2;
            int minY = brickY << 2;
            int minZ = brickZ << 2;
            for (int localY = minY; localY < minY + 4; localY++) {
                for (int localZ = minZ; localZ < minZ + 4; localZ++) {
                    for (int localX = minX; localX < minX + 4; localX++) {
                        if (!blockTouchesFace(localX, localY, localZ, face)) {
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
                                unmapped++;
                                continue;
                            }
                            counts.addTo(slot, 1);
                            int skyCutoff = Byte.toUnsignedInt(
                                    state.firstExposedLocalY[(localZ << 4) | localX]);
                            if (localY >= skyCutoff) {
                                sky.add(slot);
                            }
                        }
                    }
                }
            }
        }
        int[] slots = counts.keySet().toIntArray();
        Arrays.sort(slots);
        int[] patchCounts = new int[slots.length];
        boolean[] skyExposed = new boolean[slots.length];
        for (int index = 0; index < slots.length; index++) {
            patchCounts[index] = counts.get(slots[index]);
            skyExposed[index] = sky.contains(slots[index]);
        }
        return new OpenPatchFragment(
                slots, patchCounts, skyExposed, unmapped);
    }

    private static boolean blockTouchesFace(
            int localX,
            int localY,
            int localZ,
            ConservativeAirGeometry.Face face
    ) {
        return switch (face) {
            case NEGATIVE_X -> localX == 0;
            case POSITIVE_X -> localX == 15;
            case NEGATIVE_Y -> localY == 0;
            case POSITIVE_Y -> localY == 15;
            case NEGATIVE_Z -> localZ == 0;
            case POSITIVE_Z -> localZ == 15;
        };
    }

    private int queueRetirements(long chunkWatermark) {
        int retired = 0;
        for (PageState state : List.copyOf(dirtyPages.keySet())) {
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

    private void prepareIncrementalRetirements(
            long chunkWatermark,
            Long2ObjectMap<PageState> activeBySection
    ) {
        for (PageState state : List.copyOf(dirtyPages.keySet())) {
            if (state.retirementChunkWatermark > chunkWatermark
                    || !isRetiring(state, activeBySection)) {
                continue;
            }
            state.lastUnresolvedTopology = state.unresolvedTopology;
            state.unresolvedTopology = false;
            state.lastRemovedAirSlots = filterAirSlots(collectFragmentSlots(
                    state.fragmentFirst, state.fragmentCount, -1L));
            state.lastAddedAirSlots = new int[0];
            state.lastRebuiltGeometryBrickMask = -1L;
            state.lastRebuiltPairBrickMask = -1L;
            state.lastRebuiltMaterialBrickMask = -1L;
            state.pairDirtyBrickMask = 0L;
            state.materialDirtyBrickMask = 0L;
            Arrays.fill(state.airPairFragments, List.of());
        }
    }

    private boolean hasPendingRetirements(long chunkWatermark) {
        for (PageState state : List.copyOf(dirtyPages.keySet())) {
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
            releasePageSlot(state.pageSlot);
            return true;
        });
    }

    private void releasePageSlot(int pageSlot) {
        if (pageSlot < 0 || pageSlot >= nextPageSlot || !freePageSlots.add(pageSlot)) {
            throw new IllegalStateException("Page owner slot was released twice");
        }
        while (nextPageSlot > 0 && freePageSlots.remove(nextPageSlot - 1)) {
            nextPageSlot--;
        }
        incrementalAirGraph.releasePageSlot(pageSlot, nextPageSlot);
    }

    private void markPairDependencies(
            PageState state,
            int baseIndex,
            Long2ObjectMap<PageState> activeBySection
    ) {
        state.pairDirtyBrickMask |= 1L << baseIndex;
        dirtyPages.put(state, Boolean.TRUE);
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

    private void markNeighborBrick(
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
            int supportRef = target.appliedCoverageRefs[targetBase];
            if (supportRef != ThermalPage.NO_COVERAGE && arena.isLive(supportRef)) {
                targetBase = GeometrySummaryCache.baseIndex(
                        SectionPos.sectionRelative(arena.minimumX(supportRef)),
                        SectionPos.sectionRelative(arena.minimumY(supportRef)),
                        SectionPos.sectionRelative(arena.minimumZ(supportRef)));
            }
            target.pairDirtyBrickMask |= 1L << targetBase;
            dirtyPages.put(target, Boolean.TRUE);
        } else {
            target.materialDirtyBrickMask |= 1L << targetBase;
            dirtyPages.put(target, Boolean.TRUE);
        }
    }

    private void rebuildDirtyMaterials(
            Iterable<PageState> active,
            Long2ObjectMap<PageState> activeBySection
    ) {
        for (PageState state : active) {
            long dirty = state.materialDirtyBrickMask;
            if (dirty == 0L) {
                continue;
            }
            List<MaterialSurface>[] materialSurfaces =
                    state.appliedMaterialSurfaceFragments.clone();
            List<PhaseSurface>[] phaseSurfaces =
                    state.appliedPhaseReservoirFragments.clone();
            List<StatelessBridge>[] statelessBridges =
                    state.appliedStatelessBridgeFragments.clone();
            long remaining = dirty;
            while (remaining != 0L) {
                int baseIndex = Long.numberOfTrailingZeros(remaining);
                PageBuild build = new PageBuild();
                collectMaterialCandidates(state, baseIndex, build);
                compileMaterialBoundaries(state, activeBySection, build);
                requireMaterialSlots(state, baseIndex, build);
                materialSurfaces[baseIndex] = List.copyOf(build.materialSurfaces);
                phaseSurfaces[baseIndex] = List.copyOf(build.phaseSurfaces);
                statelessBridges[baseIndex] = List.copyOf(build.statelessBridges);
                remaining &= remaining - 1L;
            }
            state.appliedMaterialSurfaceFragments = materialSurfaces;
            state.appliedPhaseReservoirFragments = phaseSurfaces;
            state.appliedStatelessBridgeFragments = statelessBridges;
            state.materialDirtyBrickMask = 0L;
            state.lastRebuiltMaterialBrickMask |= dirty;
        }
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

    private static void requireMaterialSlots(
            PageState state,
            int baseIndex,
            PageBuild build
    ) {
        Map<MaterialPoleKey, Integer> materialSlots =
                state.appliedMaterialPoleSlotFragments[baseIndex];
        for (MaterialPoleKey key : build.materialPoleKeys) {
            if (!materialSlots.containsKey(key)) {
                throw new LatestFrameException();
            }
        }
        Map<PhaseReservoirKey, Integer> phaseSlots =
                state.appliedPhaseReservoirSlotFragments[baseIndex];
        for (PhaseReservoirKey key : build.phaseReservoirKeys) {
            if (!phaseSlots.containsKey(key)) {
                throw new LatestFrameException();
            }
        }
    }

    private void rebuildDirtyPairFragments(
            Iterable<PageState> active,
            Long2ObjectMap<PageState> activeBySection
    ) {
        for (PageState state : active) {
            long remaining = state.pairDirtyBrickMask;
            if (remaining == 0L) {
                continue;
            }
            long rebuiltMask = remaining;
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
                        ImplicitAirAdjacency.compileOwnedBrickPairsInstalled(
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
            state.lastRebuiltPairBrickMask |= rebuiltMask;
        }
    }

    /** Rebuilds only immediate section neighbors whose material exposure may change. */
    private void propagateMaterialDependencyDirtiness(
            long chunkWatermark,
            Long2ObjectMap<PageState> activeBySection
    ) {
        for (PageState state : List.copyOf(dirtyPages.keySet())) {
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

    private static int faceBlockIndex(
            ConservativeAirGeometry.Face face,
            int u,
            int v
    ) {
        return switch (face) {
            case NEGATIVE_X -> blockIndex(0, v, u);
            case POSITIVE_X -> blockIndex(15, v, u);
            case NEGATIVE_Y -> blockIndex(u, 0, v);
            case POSITIVE_Y -> blockIndex(u, 15, v);
            case NEGATIVE_Z -> blockIndex(u, v, 0);
            case POSITIVE_Z -> blockIndex(u, v, 15);
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

    @SuppressWarnings("unchecked")
    private static <T> List<T>[] emptyFragmentArray() {
        List<T>[] fragments = (List<T>[]) new List<?>[ThermalPage.BASE_BRICK_COUNT];
        Arrays.fill(fragments, List.of());
        return fragments;
    }

    @SuppressWarnings("unchecked")
    private static <K> Map<K, Integer>[] emptyMapFragmentArray() {
        Map<K, Integer>[] fragments =
                (Map<K, Integer>[]) new Map<?, ?>[ThermalPage.BASE_BRICK_COUNT];
        Arrays.fill(fragments, Map.of());
        return fragments;
    }

    private static <K> Map<K, Integer> immutableMapPreservingOrder(
            Map<K, Integer> source
    ) {
        return source.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    private record PageIdentity(long sectionKey, long lifecycleGeneration) {
    }

    private record RetiredSpan(int pageSlot, int lifecycleGeneration, ArenaSpan span) {
    }

    private record CompiledTopology(
            ThermalSweep sweep,
            boolean allOpenFrontiersResolved
    ) {
    }

    private record CompiledMaterialFragment(
            List<ThermalSweep.PairOperation> pairs,
            List<ThermalSweep.BoundaryOperation> boundaries,
            List<ThermalSweep.PhaseOperation> phases
    ) {
        private static final CompiledMaterialFragment EMPTY =
                new CompiledMaterialFragment(List.of(), List.of(), List.of());
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

    private final class IncrementalAirGraph {
        private boolean[] active = new boolean[0];
        private int[] adjacencyHead = new int[0];
        private int[] componentBySlot = new int[0];
        private int[] openPatchCount = new int[0];
        private int[] skyExposedPatchCount = new int[0];
        private double[] naturalTemperatureByPage = new double[0];
        private int[] edgeA = new int[16];
        private int[] edgeB = new int[16];
        private int[] edgeNextA = new int[16];
        private int[] edgeNextB = new int[16];
        private boolean[] edgeLive = new boolean[16];
        private int edgeHighWater;
        private int freeEdge = -1;
        private int[][] fragmentEdges = new int[0][];
        private OpenPatchFragment[] openFragments = new OpenPatchFragment[0];
        private final Int2ObjectOpenHashMap<AirComponent> components =
                new Int2ObjectOpenHashMap<>();
        private int unresolvedComponents;
        private int unresolvedPages;
        private int unmappedPatches;

        private boolean rebuildFull(
                List<PageState> activePages,
                Long2ObjectMap<PageState> activeBySection,
                ThermalSweepFragments.Builder sweep
        ) {
            int capacity = arena.highWaterMark();
            ensureNodeCapacity(capacity);
            ensurePageCapacity(nextPageSlot);
            Arrays.fill(active, 0, capacity, false);
            Arrays.fill(adjacencyHead, 0, capacity, -1);
            Arrays.fill(componentBySlot, 0, capacity, -1);
            Arrays.fill(openPatchCount, 0, capacity, 0);
            Arrays.fill(skyExposedPatchCount, 0, capacity, 0);
            Arrays.fill(naturalTemperatureByPage, 0, nextPageSlot, Double.NaN);
            Arrays.fill(edgeLive, 0, edgeHighWater, false);
            edgeHighWater = 0;
            freeEdge = -1;
            components.clear();
            unresolvedComponents = 0;
            unresolvedPages = 0;
            unmappedPatches = 0;
            fragmentEdges = new int[Math.multiplyExact(
                    nextPageSlot, ThermalPage.BASE_BRICK_COUNT)][];
            openFragments = new OpenPatchFragment[fragmentEdges.length];

            for (PageState state : activePages) {
                naturalTemperatureByPage[state.pageSlot] = state.naturalTemperatureC;
                if (state.unresolvedTopology) {
                    unresolvedPages++;
                }
                for (int baseIndex = 0;
                     baseIndex < ThermalPage.BASE_BRICK_COUNT;
                     baseIndex++) {
                    int first = state.fragmentFirst[baseIndex];
                    int end = first + state.fragmentCount[baseIndex];
                    for (int slot = first; slot < end; slot++) {
                        if (arena.isLive(slot)
                                && !arena.isMaterialPole(slot)
                                && !arena.isPhaseReservoir(slot)) {
                            active[slot] = true;
                        }
                    }
                }
            }
            for (PageState state : activePages) {
                for (int baseIndex = 0;
                     baseIndex < ThermalPage.BASE_BRICK_COUNT;
                     baseIndex++) {
                    installPairFragment(
                            state.sweepFragmentOffset + baseIndex,
                            state.airPairFragments[baseIndex]);
                    OpenPatchFragment fragment = compileOpenPatchFragment(
                            state, baseIndex, activeBySection);
                    state.openUnmappedPatches[baseIndex] =
                            fragment.unmappedPatches();
                    addOpenFragment(
                            state.sweepFragmentOffset + baseIndex, fragment);
                }
            }
            for (int slot = 0; slot < capacity; slot++) {
                if (active[slot] && componentBySlot[slot] == -1) {
                    AirComponent component = buildComponent(slot);
                    components.put(component.id, component);
                    if (component.unresolved) {
                        unresolvedComponents++;
                    }
                    emitComponent(component, sweep);
                }
            }
            return allResolved();
        }

        private boolean patch(
                Long2ObjectMap<PageState> activeBySection,
                ThermalSweepFragments.Patch sweep
        ) {
            ensureNodeCapacity(arena.highWaterMark());
            IntOpenHashSet boundaryComponentIds = new IntOpenHashSet();
            IntOpenHashSet connectivityComponentIds = new IntOpenHashSet();
            IntArrayList seeds = new IntArrayList();

            if (farFieldConductanceChanged) {
                boundaryComponentIds.addAll(components.keySet());
            }
            for (PageState state : dirtyPages.keySet()) {
                if (state.naturalTemperatureChanged) {
                    ensurePageCapacity(state.pageSlot + 1);
                    naturalTemperatureByPage[state.pageSlot] =
                            state.naturalTemperatureC;
                    collectPageComponents(state, boundaryComponentIds);
                }
                long remaining = state.skyExposureDirtyBrickMask
                        & ~state.lastRebuiltGeometryBrickMask;
                while (remaining != 0L) {
                    int baseIndex = Long.numberOfTrailingZeros(remaining);
                    OpenPatchFragment fragment = openFragments[
                            state.sweepFragmentOffset + baseIndex];
                    if (fragment != null) {
                        for (int slot : fragment.slots) {
                            collectComponent(slot, boundaryComponentIds);
                        }
                    }
                    remaining &= remaining - 1L;
                }
            }

            for (PageState state : dirtyPages.keySet()) {
                if (state.lastRebuiltGeometryBrickMask == 0L) {
                    continue;
                }
                if (state.lastUnresolvedTopology != state.unresolvedTopology) {
                    unresolvedPages += state.unresolvedTopology ? 1 : -1;
                }
                for (int slot : state.lastRemovedAirSlots) {
                    collectComponent(slot, connectivityComponentIds);
                }
            }
            for (PageState state : dirtyPages.keySet()) {
                long remaining = state.lastRebuiltPairBrickMask;
                while (remaining != 0L) {
                    int baseIndex = Long.numberOfTrailingZeros(remaining);
                    int fragment = state.sweepFragmentOffset + baseIndex;
                    int[] oldEdges = fragmentEdges[fragment];
                    if (oldEdges != null) {
                        for (int edge : oldEdges) {
                            collectComponent(edgeA[edge], connectivityComponentIds);
                            collectComponent(edgeB[edge], connectivityComponentIds);
                        }
                    }
                    for (ThermalSweep.PairOperation operation
                            : state.airPairFragments[baseIndex]) {
                        collectComponent(operation.cellA(), connectivityComponentIds);
                        collectComponent(operation.cellB(), connectivityComponentIds);
                    }
                    remaining &= remaining - 1L;
                }
            }
            for (int componentId : connectivityComponentIds) {
                AirComponent component = components.remove(componentId);
                if (component == null) {
                    continue;
                }
                boundaryComponentIds.remove(componentId);
                if (component.unresolved) {
                    unresolvedComponents--;
                }
                for (int slot : component.openMembers) {
                    sweep.clearFarBoundary(slot);
                }
                for (int slot : component.members) {
                    if (active[slot]) {
                        componentBySlot[slot] = -1;
                        seeds.add(slot);
                    }
                }
            }

            for (PageState state : dirtyPages.keySet()) {
                long remaining = state.lastRebuiltPairBrickMask;
                while (remaining != 0L) {
                    int baseIndex = Long.numberOfTrailingZeros(remaining);
                    removePairFragment(state.sweepFragmentOffset + baseIndex);
                    remaining &= remaining - 1L;
                }
            }
            for (PageState state : dirtyPages.keySet()) {
                long remaining = state.lastRebuiltGeometryBrickMask;
                while (remaining != 0L) {
                    int baseIndex = Long.numberOfTrailingZeros(remaining);
                    removeOpenFragment(state.sweepFragmentOffset + baseIndex);
                    state.openUnmappedPatches[baseIndex] = 0;
                    remaining &= remaining - 1L;
                }
                for (int slot : state.lastRemovedAirSlots) {
                    if (slot < active.length) {
                        if (adjacencyHead[slot] != -1) {
                            throw new IllegalStateException(
                                    "removed air cell still owns adjacency: " + slot);
                        }
                        active[slot] = false;
                        componentBySlot[slot] = -1;
                        openPatchCount[slot] = 0;
                        skyExposedPatchCount[slot] = 0;
                    }
                }
                if (isRetiring(state, activeBySection)) {
                    continue;
                }
                for (int slot : state.lastAddedAirSlots) {
                    active[slot] = true;
                    adjacencyHead[slot] = -1;
                    componentBySlot[slot] = -1;
                    openPatchCount[slot] = 0;
                    skyExposedPatchCount[slot] = 0;
                    seeds.add(slot);
                }
                remaining = state.lastRebuiltGeometryBrickMask;
                while (remaining != 0L) {
                    int baseIndex = Long.numberOfTrailingZeros(remaining);
                    OpenPatchFragment next = compileOpenPatchFragment(
                            state, baseIndex, activeBySection);
                    state.openUnmappedPatches[baseIndex] =
                            next.unmappedPatches();
                    addOpenFragment(
                            state.sweepFragmentOffset + baseIndex, next);
                    remaining &= remaining - 1L;
                }
            }
            for (PageState state : dirtyPages.keySet()) {
                long remaining = state.skyExposureDirtyBrickMask
                        & ~state.lastRebuiltGeometryBrickMask;
                while (remaining != 0L) {
                    int baseIndex = Long.numberOfTrailingZeros(remaining);
                    int fragment = state.sweepFragmentOffset + baseIndex;
                    removeOpenFragment(fragment);
                    OpenPatchFragment next = compileOpenPatchFragment(
                            state, baseIndex, activeBySection);
                    state.openUnmappedPatches[baseIndex] =
                            next.unmappedPatches();
                    addOpenFragment(fragment, next);
                    remaining &= remaining - 1L;
                }
            }
            for (PageState state : dirtyPages.keySet()) {
                long remaining = state.lastRebuiltPairBrickMask;
                while (remaining != 0L) {
                    int baseIndex = Long.numberOfTrailingZeros(remaining);
                    installPairFragment(
                            state.sweepFragmentOffset + baseIndex,
                            state.airPairFragments[baseIndex]);
                    remaining &= remaining - 1L;
                }
            }

            for (int index = 0; index < seeds.size(); index++) {
                int slot = seeds.getInt(index);
                if (!active[slot] || componentBySlot[slot] != -1) {
                    continue;
                }
                AirComponent component = buildComponent(slot);
                components.put(component.id, component);
                if (component.unresolved) {
                    unresolvedComponents++;
                }
                emitComponent(component, sweep);
            }
            for (int componentId : boundaryComponentIds) {
                refreshComponentBoundary(componentId, sweep);
            }
            return allResolved();
        }

        private void collectComponent(int slot, IntOpenHashSet target) {
            if (slot >= 0 && slot < componentBySlot.length
                    && active[slot] && componentBySlot[slot] >= 0) {
                target.add(componentBySlot[slot]);
            }
        }

        private void collectPageComponents(
                PageState state,
                IntOpenHashSet target
        ) {
            for (int baseIndex = 0;
                 baseIndex < ThermalPage.BASE_BRICK_COUNT;
                 baseIndex++) {
                int first = state.fragmentFirst[baseIndex];
                int end = first + state.fragmentCount[baseIndex];
                for (int slot = first; slot < end; slot++) {
                    collectComponent(slot, target);
                }
            }
        }

        private void addOpenFragment(
                int fragmentIndex,
                OpenPatchFragment fragment
        ) {
            openFragments[fragmentIndex] = fragment;
            unmappedPatches = Math.addExact(
                    unmappedPatches, fragment.unmappedPatches);
            for (int index = 0; index < fragment.slots.length; index++) {
                int slot = fragment.slots[index];
                if (!active[slot]) {
                    throw new IllegalStateException(
                            "open patch references an inactive air cell: " + slot);
                }
                openPatchCount[slot] = Math.addExact(
                        openPatchCount[slot], fragment.patchCounts[index]);
                if (fragment.skyExposed[index]) {
                    skyExposedPatchCount[slot] = Math.addExact(
                            skyExposedPatchCount[slot], 1);
                }
            }
        }

        private void removeOpenFragment(int fragmentIndex) {
            OpenPatchFragment fragment = openFragments[fragmentIndex];
            if (fragment == null) {
                return;
            }
            unmappedPatches -= fragment.unmappedPatches;
            for (int index = 0; index < fragment.slots.length; index++) {
                int slot = fragment.slots[index];
                openPatchCount[slot] -= fragment.patchCounts[index];
                if (fragment.skyExposed[index]) {
                    skyExposedPatchCount[slot]--;
                }
                if (openPatchCount[slot] < 0 || skyExposedPatchCount[slot] < 0) {
                    throw new IllegalStateException(
                            "open patch fragment reference underflow");
                }
            }
            openFragments[fragmentIndex] = null;
        }

        private AirComponent buildComponent(int start) {
            IntArrayList queue = new IntArrayList();
            IntArrayList members = new IntArrayList();
            queue.add(start);
            componentBySlot[start] = -2;
            int minimumSlot = start;
            for (int read = 0; read < queue.size(); read++) {
                int slot = queue.getInt(read);
                members.add(slot);
                minimumSlot = Math.min(minimumSlot, slot);
                for (int edge = adjacencyHead[slot]; edge != -1;
                     edge = nextEdge(edge, slot)) {
                    if (!edgeLive[edge]) {
                        continue;
                    }
                    int neighbor = edgeA[edge] == slot ? edgeB[edge] : edgeA[edge];
                    if (active[neighbor] && componentBySlot[neighbor] == -1) {
                        componentBySlot[neighbor] = -2;
                        queue.add(neighbor);
                    }
                }
            }
            int[] componentMembers = members.toIntArray();
            for (int slot : componentMembers) {
                componentBySlot[slot] = minimumSlot;
            }
            return describeComponent(minimumSlot, componentMembers);
        }

        private AirComponent describeComponent(int componentId, int[] members) {
            IntArrayList openMembers = new IntArrayList();
            int totalOpenPatches = 0;
            boolean componentSkyExposed = false;
            double maximumDeltaC = 0.0D;
            for (int slot : members) {
                if (openPatchCount[slot] == 0) {
                    continue;
                }
                openMembers.add(slot);
                totalOpenPatches = Math.addExact(
                        totalOpenPatches, openPatchCount[slot]);
                componentSkyExposed |= skyExposedPatchCount[slot] != 0;
                maximumDeltaC = Math.max(
                        maximumDeltaC,
                        Math.abs(arena.temperatureC(
                                slot, parameters.referenceTemperatureC())
                                - naturalTemperature(slot)));
            }
            int[] componentOpenMembers = openMembers.toIntArray();
            if (totalOpenPatches == 0) {
                return new AirComponent(
                        componentId,
                        members,
                        componentOpenMembers,
                        null,
                        false,
                        false);
            }
            FarFieldProfileRegistry.Profile profile = applicableFarFieldProfile(
                    farFieldSettings.calibrationSourcePowerW(), maximumDeltaC);
            boolean ambient = farFieldSettings.naturalEnvironment()
                    && componentSkyExposed
                    && profile != null;
            return new AirComponent(
                    componentId,
                    members,
                    componentOpenMembers,
                    profile,
                    ambient,
                    !ambient);
        }

        private void refreshComponentBoundary(
                int componentId,
                ThermalSweepFragments.Patch sweep
        ) {
            AirComponent previous = components.get(componentId);
            if (previous == null) {
                return;
            }
            for (int slot : previous.openMembers) {
                sweep.clearFarBoundary(slot);
            }
            AirComponent refreshed = describeComponent(
                    componentId, previous.members);
            if (previous.unresolved != refreshed.unresolved) {
                unresolvedComponents += refreshed.unresolved ? 1 : -1;
            }
            components.put(componentId, refreshed);
            emitComponent(refreshed, sweep);
        }

        private void emitComponent(
                AirComponent component,
                ThermalSweepFragments.Builder sweep
        ) {
            for (int slot : component.openMembers) {
                BoundaryValue boundary = boundaryValue(component, slot);
                if (boundary != null) {
                    sweep.setFarBoundary(
                            slot, naturalTemperature(slot), boundary.conductance);
                }
            }
        }

        private void emitComponent(
                AirComponent component,
                ThermalSweepFragments.Patch sweep
        ) {
            for (int slot : component.openMembers) {
                BoundaryValue boundary = boundaryValue(component, slot);
                if (boundary != null) {
                    sweep.setFarBoundary(
                            slot, naturalTemperature(slot), boundary.conductance);
                }
            }
        }

        private BoundaryValue boundaryValue(AirComponent component, int slot) {
            if (openPatchCount[slot] == 0 || component.profile == null) {
                return null;
            }
            double areaScale = farFieldConductanceScale
                    * openPatchCount[slot]
                    / (16.0D
                    * farFieldSettings.referenceOpeningAreaBlocksSquared());
            double continuationScale = component.ambient
                    ? 1.0D
                    : 1.0D / (1.0D
                    + farFieldSettings.continuationDistanceBlocks());
            return new BoundaryValue(
                    component.profile.conductanceWPerK()
                            * areaScale
                            * continuationScale);
        }

        private void installPairFragment(
                int fragment,
                List<ThermalSweep.PairOperation> operations
        ) {
            int[] edges = new int[operations.size()];
            for (int index = 0; index < operations.size(); index++) {
                ThermalSweep.PairOperation operation = operations.get(index);
                edges[index] = addEdge(operation.cellA(), operation.cellB());
            }
            fragmentEdges[fragment] = edges;
        }

        private void removePairFragment(int fragment) {
            int[] edges = fragmentEdges[fragment];
            if (edges != null) {
                for (int edge : edges) {
                    removeEdge(edge);
                }
            }
            fragmentEdges[fragment] = null;
        }

        private int addEdge(int first, int second) {
            if (first < 0 || second < 0
                    || first >= active.length || second >= active.length
                    || !active[first] || !active[second] || first == second) {
                throw new IllegalStateException("air adjacency references invalid cells");
            }
            int edge;
            if (freeEdge != -1) {
                edge = freeEdge;
                freeEdge = edgeNextA[edge];
            } else {
                edge = edgeHighWater++;
                ensureEdgeCapacity(edgeHighWater);
            }
            edgeA[edge] = first;
            edgeB[edge] = second;
            edgeNextA[edge] = adjacencyHead[first];
            edgeNextB[edge] = adjacencyHead[second];
            adjacencyHead[first] = edge;
            adjacencyHead[second] = edge;
            edgeLive[edge] = true;
            return edge;
        }

        private void removeEdge(int edge) {
            if (edge < 0 || edge >= edgeHighWater || !edgeLive[edge]) {
                throw new IllegalStateException("air adjacency edge is stale");
            }
            unlinkEdge(edgeA[edge], edge);
            unlinkEdge(edgeB[edge], edge);
            edgeLive[edge] = false;
            edgeA[edge] = -1;
            edgeB[edge] = -1;
            edgeNextA[edge] = freeEdge;
            edgeNextB[edge] = -1;
            freeEdge = edge;
        }

        private void unlinkEdge(int slot, int target) {
            int previous = -1;
            for (int edge = adjacencyHead[slot]; edge != -1;
                 edge = nextEdge(edge, slot)) {
                if (edge == target) {
                    int next = nextEdge(edge, slot);
                    if (previous == -1) {
                        adjacencyHead[slot] = next;
                    } else {
                        setNextEdge(previous, slot, next);
                    }
                    return;
                }
                previous = edge;
            }
            throw new IllegalStateException("air adjacency edge owner is missing");
        }

        private int nextEdge(int edge, int slot) {
            return edgeA[edge] == slot ? edgeNextA[edge] : edgeNextB[edge];
        }

        private void setNextEdge(int edge, int slot, int next) {
            if (edgeA[edge] == slot) {
                edgeNextA[edge] = next;
            } else if (edgeB[edge] == slot) {
                edgeNextB[edge] = next;
            } else {
                throw new IllegalStateException("air adjacency owner is invalid");
            }
        }

        private boolean allResolved() {
            return unresolvedPages == 0
                    && unresolvedComponents == 0
                    && unmappedPatches == 0;
        }

        private void ensureNodeCapacity(int required) {
            if (required <= active.length) {
                return;
            }
            int old = active.length;
            int capacity = graphCapacity(old, required);
            active = Arrays.copyOf(active, capacity);
            adjacencyHead = Arrays.copyOf(adjacencyHead, capacity);
            componentBySlot = Arrays.copyOf(componentBySlot, capacity);
            openPatchCount = Arrays.copyOf(openPatchCount, capacity);
            skyExposedPatchCount = Arrays.copyOf(
                    skyExposedPatchCount, capacity);
            Arrays.fill(adjacencyHead, old, capacity, -1);
            Arrays.fill(componentBySlot, old, capacity, -1);
        }

        private void ensurePageCapacity(int required) {
            if (required <= naturalTemperatureByPage.length) {
                return;
            }
            int old = naturalTemperatureByPage.length;
            naturalTemperatureByPage = Arrays.copyOf(
                    naturalTemperatureByPage, graphCapacity(old, required));
            Arrays.fill(
                    naturalTemperatureByPage,
                    old,
                    naturalTemperatureByPage.length,
                    Double.NaN);
        }

        private void releasePageSlot(int pageSlot, int pageHighWater) {
            if (pageSlot >= 0 && pageSlot < naturalTemperatureByPage.length) {
                naturalTemperatureByPage[pageSlot] = Double.NaN;
            }
            if (pageHighWater < naturalTemperatureByPage.length / 4) {
                int capacity = graphCapacity(0, pageHighWater);
                naturalTemperatureByPage = Arrays.copyOf(
                        naturalTemperatureByPage, capacity);
                Arrays.fill(
                        naturalTemperatureByPage,
                        pageHighWater,
                        naturalTemperatureByPage.length,
                        Double.NaN);
            }
        }

        private double naturalTemperature(int slot) {
            int pageSlot = arena.pageSlot(slot);
            if (pageSlot < 0 || pageSlot >= naturalTemperatureByPage.length) {
                throw new IllegalStateException(
                        "air cell does not own a natural-temperature Page");
            }
            return naturalTemperatureByPage[pageSlot];
        }

        private void ensureEdgeCapacity(int required) {
            if (required <= edgeA.length) {
                return;
            }
            int capacity = graphCapacity(edgeA.length, required);
            edgeA = Arrays.copyOf(edgeA, capacity);
            edgeB = Arrays.copyOf(edgeB, capacity);
            edgeNextA = Arrays.copyOf(edgeNextA, capacity);
            edgeNextB = Arrays.copyOf(edgeNextB, capacity);
            edgeLive = Arrays.copyOf(edgeLive, capacity);
        }

        private int graphCapacity(int current, int required) {
            int capacity = Math.max(16, current);
            while (capacity < required) {
                capacity = Math.max(required, capacity + (capacity >>> 1));
            }
            return capacity;
        }
    }

    private record AirComponent(
            int id,
            int[] members,
            int[] openMembers,
            FarFieldProfileRegistry.Profile profile,
            boolean ambient,
            boolean unresolved
    ) {
    }

    private record BoundaryValue(double conductance) {
    }

    private record OpenPatchFragment(
            int[] slots,
            int[] patchCounts,
            boolean[] skyExposed,
            int unmappedPatches
    ) {
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
        private GeometrySummary[] appliedBaseSummaries =
                new GeometrySummary[ThermalPage.BASE_BRICK_COUNT];
        private ComponentBrickCompiler.CompiledBrick[] appliedMixedGeometry =
                new ComponentBrickCompiler.CompiledBrick[ThermalPage.BASE_BRICK_COUNT];
        private Map<MaterialPoleKey, Integer>[] appliedMaterialPoleSlotFragments =
                emptyMapFragmentArray();
        private List<MaterialSurface>[] appliedMaterialSurfaceFragments =
                emptyFragmentArray();
        private Map<PhaseReservoirKey, Integer>[] appliedPhaseReservoirSlotFragments =
                emptyMapFragmentArray();
        private List<PhaseSurface>[] appliedPhaseReservoirFragments =
                emptyFragmentArray();
        private List<StatelessBridge>[] appliedStatelessBridgeFragments =
                emptyFragmentArray();
        private final int[] fragmentFirst = new int[ThermalPage.BASE_BRICK_COUNT];
        private final int[] fragmentCount = new int[ThermalPage.BASE_BRICK_COUNT];
        @SuppressWarnings("unchecked")
        private final List<ThermalSweep.PairOperation>[] airPairFragments =
                (List<ThermalSweep.PairOperation>[]) new List<?>[
                        ThermalPage.BASE_BRICK_COUNT];
        private final boolean[] unresolvedBricks =
                new boolean[ThermalPage.BASE_BRICK_COUNT];
        private final int[] openUnmappedPatches =
                new int[ThermalPage.BASE_BRICK_COUNT];
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
        private int sweepFragmentOffset = -1;
        private boolean lastUnresolvedTopology;
        private long lastRebuiltGeometryBrickMask;
        private long lastRebuiltPairBrickMask;
        private long lastRebuiltMaterialBrickMask;
        private long skyExposureDirtyBrickMask;
        private boolean naturalTemperatureChanged;
        private int[] lastRemovedAirSlots = new int[0];
        private int[] lastAddedAirSlots = new int[0];

        private PageState(
                ThermalPage page,
                int pageSlot,
                int lifecycleGeneration,
                long admissionChunkWatermark,
                double naturalTemperatureC
        ) {
            this.page = page;
            this.pageSlot = pageSlot;
            this.lifecycleGeneration = lifecycleGeneration;
            this.admissionChunkWatermark = admissionChunkWatermark;
            this.naturalTemperatureC = naturalTemperatureC;
            this.sweepFragmentOffset = Math.multiplyExact(
                    pageSlot, ThermalPage.BASE_BRICK_COUNT);
            this.appliedCoverageRefs = page.coverageSnapshot();
            for (int baseIndex = 0;
                 baseIndex < ThermalPage.BASE_BRICK_COUNT;
                 baseIndex++) {
                appliedBaseSummaries[baseIndex] = page.geometrySummary(baseIndex);
            }
            Arrays.fill(appliedSignatureIds, INITIAL_ALL_AIR);
            Arrays.fill(airPairFragments, List.of());
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
