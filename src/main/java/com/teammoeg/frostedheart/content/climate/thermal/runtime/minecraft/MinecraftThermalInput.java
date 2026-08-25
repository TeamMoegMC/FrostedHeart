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

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.content.climate.data.StateTransitionData;
import com.teammoeg.frostedheart.content.climate.thermal.consumer.TownThermalProjection;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.GeometryDeltaRing;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPage;
import com.teammoeg.frostedheart.content.climate.thermal.phase0.mutation.Phase0aMutationProbe;
import com.teammoeg.frostedheart.content.climate.thermal.profile.DependencyOffsetMask;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolverBlockView;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalResolution;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureResolution;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.StateStaticThermalResolver;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.ThermalSignatureResolverDispatcher;
import com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication;
import com.teammoeg.frostedheart.content.climate.thermal.radiation.RadiationService;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.DimensionThermalRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalRuntimeCoordinator;
import com.teammoeg.frostedheart.content.climate.thermal.solver.BuoyancyConductance;
import com.teammoeg.frostedheart.content.climate.thermal.solver.InputWatermarks;
import com.teammoeg.frostedheart.content.climate.thermal.solver.LatestSolveEpochScheduler;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.solver.SealedInputFrame;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSweep;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalTimePolicy;
import com.teammoeg.frostedheart.content.climate.thermal.source.NodePowerAccumulatorArena;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceTimeline;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Main-thread Minecraft input owner for one dimension runtime.
 *
 * <pre>
 * section mutation Mixin
 *   owner lookup -> page invalidation -> loaded-only resolve -> primitive event
 * tick end
 *   seal page deltas -> seal five stream watermarks
 *   -> optional latest-only shadow executor -> topology -> coordinator
 * </pre>
 *
 * <p>Tests may construct this class explicitly. Normal gameplay starts it on
 * the first player temperature query in a dimension and admits only sections
 * that are queried or retained by a physical source.</p>
 */
public final class MinecraftThermalInput implements AutoCloseable {
    public static final int QUERY_STALE_GEOMETRY = 1;
    public static final int QUERY_DEGRADED_TOPOLOGY = 1 << 1;
    public static final int QUERY_PUBLICATION_MISS = 1 << 2;
    public static final int QUERY_PUBLICATION_STALE = 1 << 3;
    public static final int QUERY_NO_PAGE = 1 << 4;
    public static final int QUERY_NO_AIR_COMPONENT = 1 << 5;
    public static final int QUERY_RADIATION_UNAVAILABLE = 1 << 6;
    public static final int QUERY_RADIATION_BUDGET_LIMITED = 1 << 7;
    public static final int QUERY_RADIATION_UNRESOLVED = 1 << 8;
    public static final int QUERY_SURFACE_UNAVAILABLE = 1 << 9;
    public static final int QUERY_PARTIAL_REGION = 1 << 10;
    private static final int SHADOW_MAX_PUBLICATION_AGE_TICKS = 40;
    private static final int GAMEPLAY_PUBLICATION_CAPACITY = 65_536;
    private static final double GAMEPLAY_PHASE_FACE_CONDUCTANCE_W_PER_K = 20.0D;
    private static final double GAMEPLAY_PHASE_BASE_ENERGY_J = 38_000.0D;
    static final RadiationService.Parameters GAMEPLAY_RADIATION_PARAMETERS =
            new RadiationService.Parameters(
                    128, 1_024, 128,
                    64, 8, 24,
                    8, 256,
                    16.0D, 0.1D, 0.5D,
                    0.1D, 0.9D, 1.62D);
    private static final ThermalMemoryBudget GAMEPLAY_MEMORY_BUDGET =
            new ThermalMemoryBudget(128L * 1024L * 1024L, 4L * 1024L * 1024L);
    private static final Map<ServerLevel, MinecraftThermalInput> ACTIVE_BY_LEVEL =
            new IdentityHashMap<>();
    private static final AtomicLong NEXT_GAMEPLAY_RUNTIME_ID = new AtomicLong(1_000L);
    private static volatile boolean anyActive;
    private static ThermalSignatureRegistry gameplaySignatures;
    private static ThermalSignatureResolverDispatcher gameplayDispatcher;
    private static MaterialBoundaryRegistry gameplayMaterialBoundaries =
            MaterialBoundaryRegistry.empty();
    private static Map<BlockState, Integer> gameplayPhaseProfileIds = Map.of();
    private static long gameplayProfileWatermark;
    private static ThermalRuntimeCoordinator gameplayCoordinator;
    private static final int WITNESS_RADIUS_SECTIONS = 1;
    private static final int[][] PHASE_NEIGHBOR_OFFSETS = {
            {-1, 0, 0}, {1, 0, 0},
            {0, -1, 0}, {0, 1, 0},
            {0, 0, -1}, {0, 0, 1}
    };

    private final ServerLevel level;
    private final Thread mainThread;
    private final long dimensionGeneration;
    private final DimensionThermalRuntime runtime;
    private final ThermalSignatureResolverDispatcher resolverDispatcher;
    private final ThermalSignatureRegistry signatureRegistry;
    private final GeometryDeltaRing geometryDeltas;
    private final ResolvedGeometryInputRing resolvedInputs;
    private final Long2ObjectOpenHashMap<ThermalPage> pages =
            new Long2ObjectOpenHashMap<>();
    private final Map<Long, Integer> physicalSourcePageRefCounts = new HashMap<>();
    private final Set<Long> physicalSourceOwnedPages = new HashSet<>();
    private final Map<Long, Integer> witnessRefCounts = new HashMap<>();
    private final Set<Long> radiationTrackedSections = new HashSet<>();
    private final Map<Long, SectionOwner> ownersBySectionKey = new HashMap<>();
    private final IdentityHashMap<LevelChunkSection, SectionOwner> ownersByIdentity =
            new IdentityHashMap<>();
    private final IdentityHashMap<ThermalPage, Boolean> dirtyPages = new IdentityHashMap<>();
    private final AtomicBoolean offThreadResyncPending = new AtomicBoolean();
    private final AtomicLong nextSectionGeneration = new AtomicLong();
    private final ThermalPage.MutableCoverageQuery environmentCoverageScratch =
            new ThermalPage.MutableCoverageQuery();
    private final QueryPublication.MutableSample environmentPublicationScratch =
            new QueryPublication.MutableSample();
    private final RadiationService.MutableSample playerRadiationScratch =
            new RadiationService.MutableSample();
    private final MutableEnvironmentSample playerEnvironmentScratch =
            new MutableEnvironmentSample();
    private final MutableEnvironmentSample machineEnvironmentScratch =
            new MutableEnvironmentSample();
    private final MutableEnvironmentSample cropEnvironmentScratch =
            new MutableEnvironmentSample();
    private final MutableEnvironmentSample townEnvironmentScratch =
            new MutableEnvironmentSample();
    private final MutableEnvironmentSample townGroupEnvironmentScratch =
            new MutableEnvironmentSample();

    private long chunkWatermark;
    private final long profileWatermark;
    private long transitionAckWatermark;
    private long lastSealedTick;
    private volatile boolean closed;
    private MinecraftThermalTopologyApplier topologyApplier;
    private MinecraftPhaseTransitionHandler customPhaseTransitionHandler =
            MinecraftPhaseTransitionHandler.rejectCustomActions();
    private int maximumPhaseMutationsPerTick;
    private final PhaseTransitionRuntime.MutableRequest phaseRequest =
            new PhaseTransitionRuntime.MutableRequest();
    private MinecraftPhysicalSourceManager physicalSources;
    private MinecraftRadiationOcclusion radiationOcclusion;
    private RadiationService radiation;
    private ThermalRuntimeCoordinator shadowCoordinator;
    private Executor shadowExecutor;
    private final AtomicReference<SealedInputFrame> pendingShadowFrame =
            new AtomicReference<>();
    private final AtomicBoolean shadowWorkerScheduled = new AtomicBoolean();
    private volatile ShadowReport latestShadowReport;
    private long passivePlayerQueries;
    private long passivePlayerHits;
    private long passivePlayerMisses;
    private long playerShadowComparisons;
    private double playerShadowAbsoluteErrorSumC;
    private double playerShadowMaximumAbsoluteErrorC;
    private double latestLegacyAirTemperatureC = Double.NaN;
    private double latestShadowAirTemperatureC = Double.NaN;
    private long latestPlayerShadowSampleTick = -1L;
    private int latestPlayerShadowFlags;
    private long passiveMachineQueries;
    private long passiveMachineHits;
    private long passiveMachineMisses;
    private long machineShadowComparisons;
    private double machineShadowAbsoluteErrorSumC;
    private double machineShadowMaximumAbsoluteErrorC;
    private double latestLegacyMachineTemperatureC = Double.NaN;
    private double latestShadowMachineTemperatureC = Double.NaN;
    private long latestMachineShadowSampleTick = -1L;
    private int latestMachineShadowFlags;
    private long passiveCropQueries;
    private long passiveCropHits;
    private long passiveCropMisses;
    private long cropShadowComparisons;
    private double cropShadowAbsoluteErrorSumC;
    private double cropShadowMaximumAbsoluteErrorC;
    private double latestLegacyCropTemperatureC = Double.NaN;
    private double latestShadowCropTemperatureC = Double.NaN;
    private long latestCropShadowSampleTick = -1L;
    private int latestCropShadowFlags;
    private long passiveTownQueries;
    private long passiveTownGroupLookups;
    private long passiveTownHits;
    private long passiveTownMisses;
    private long partialTownQueries;
    private long townShadowComparisons;
    private double townShadowAbsoluteErrorSumC;
    private double townShadowMaximumAbsoluteErrorC;
    private double latestLegacyTownTemperatureC = Double.NaN;
    private double latestShadowTownTemperatureC = Double.NaN;
    private long latestTownShadowSampleTick = -1L;
    private int latestTownGroupCount;
    private int latestTownVoxelCount;
    private int latestTownShadowFlags;
    private long publishedAirLookups;
    private long publishedAirHits;
    private long publishedAirMisses;
    private long noPageLookups;
    private long noAirComponentLookups;
    private long staleGeometryLookups;
    private long publicationMissLookups;
    private long stalePublicationLookups;
    private long degradedTopologyLookups;
    private long publicationAgeSamples;
    private long publicationAgeTotalTicks;
    private long maximumObservedPublicationAgeTicks;
    private long shadowSealCalls;
    private long shadowSealTotalNanos;
    private long shadowSealMaximumNanos;
    private volatile long shadowWorkerFrames;
    private volatile long shadowWorkerTotalNanos;
    private volatile long shadowWorkerMaximumNanos;
    private volatile long shadowExecutorRejectedSubmissions;

    public MinecraftThermalInput(
            ServerLevel level,
            long dimensionGeneration,
            DimensionThermalRuntime runtime,
            ThermalSignatureResolverDispatcher resolverDispatcher,
            ThermalSignatureRegistry signatureRegistry,
            long profileWatermark,
            int geometryDeltaCapacity,
            int resolvedInputCapacity
    ) {
        this.level = Objects.requireNonNull(level, "level");
        this.mainThread = Thread.currentThread();
        this.dimensionGeneration = dimensionGeneration;
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.resolverDispatcher = Objects.requireNonNull(
                resolverDispatcher, "resolverDispatcher");
        this.signatureRegistry = Objects.requireNonNull(
                signatureRegistry, "signatureRegistry");
        if (dimensionGeneration < 0L || profileWatermark < 0L
                || runtime.dimensionGeneration() != dimensionGeneration) {
            throw new IllegalArgumentException(
                    "input/runtime generations and profile watermark are invalid");
        }
        this.profileWatermark = profileWatermark;
        this.geometryDeltas = new GeometryDeltaRing(geometryDeltaCapacity);
        this.resolvedInputs = new ResolvedGeometryInputRing(resolvedInputCapacity);
        requireMainThread();
        synchronized (ACTIVE_BY_LEVEL) {
            if (ACTIVE_BY_LEVEL.putIfAbsent(level, this) != null) {
                throw new IllegalStateException(
                        "one Minecraft thermal input is already active for this level");
            }
            anyActive = true;
        }
        lastSealedTick = runtime.lastCompletedTargetTick();
    }

    /**
     * Admits one section only when Minecraft already proves it contains air.
     * No cold scan or chunk load is performed.
     */
    public ThermalPage admitAllAirPage(
            LevelChunk chunk,
            int sectionIndex,
            int supportRef,
            int airMediumId
    ) {
        requireMainThread();
        requireOpen();
        Objects.requireNonNull(chunk, "chunk");
        if (chunk.getLevel() != level
                || sectionIndex < 0
                || sectionIndex >= chunk.getSections().length) {
            throw new IllegalArgumentException("chunk section does not belong to this level");
        }
        LevelChunkSection section = chunk.getSections()[sectionIndex];
        if (!section.hasOnlyAir()) {
            return null;
        }
        int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex);
        long sectionKey = SectionPos.asLong(chunk.getPos().x, sectionY, chunk.getPos().z);
        if (pages.containsKey(sectionKey)) {
            throw new IllegalStateException("thermal page is already admitted");
        }
        long lifecycleGeneration = nextSectionGeneration.incrementAndGet();
        ThermalPage page = ThermalPage.allAir(
                sectionKey, lifecycleGeneration, supportRef, airMediumId);
        pages.put(sectionKey, page);
        adjustWitnesses(chunk.getPos().x, sectionY, chunk.getPos().z, 1);
        refreshNearbyOwnerPageViews(chunk.getPos().x, sectionY, chunk.getPos().z);
        chunkWatermark = Math.incrementExact(chunkWatermark);
        if (topologyApplier != null) {
            topologyApplier.registerAllAirPage(page, chunkWatermark);
        }
        return page;
    }

    public boolean withdrawPage(long sectionKey) {
        requireMainThread();
        requireOpen();
        ThermalPage removed = pages.remove(sectionKey);
        if (removed == null) {
            return false;
        }
        physicalSourcePageRefCounts.remove(sectionKey);
        physicalSourceOwnedPages.remove(sectionKey);
        if (physicalSources != null) {
            physicalSources.onPageWithdrawn(sectionKey);
        }
        dirtyPages.remove(removed);
        int sectionX = SectionPos.x(sectionKey);
        int sectionY = SectionPos.y(sectionKey);
        int sectionZ = SectionPos.z(sectionKey);
        adjustWitnesses(sectionX, sectionY, sectionZ, -1);
        refreshNearbyOwnerPageViews(sectionX, sectionY, sectionZ);
        chunkWatermark = Math.incrementExact(chunkWatermark);
        if (topologyApplier != null) {
            topologyApplier.retirePage(removed, chunkWatermark);
        }
        return true;
    }

    /** Freezes the current main-thread cut and offers it to the PR7 runtime. */
    public SealReport sealTick(long effectiveTick) {
        requireMainThread();
        requireOpen();
        if (effectiveTick < lastSealedTick) {
            throw new IllegalArgumentException("thermal input ticks must be monotonic");
        }
        long startedNanos = System.nanoTime();
        processPhaseTransitions();
        if (physicalSources != null) {
            physicalSources.flush(effectiveTick);
        }
        if (offThreadResyncPending.getAndSet(false)) {
            for (ThermalPage page : pages.values()) {
                dirtyPages.put(page, Boolean.TRUE);
            }
        }

        int sealedDeltas = 0;
        int resyncPages = 0;
        for (ThermalPage page : List.copyOf(dirtyPages.keySet())) {
            var sealed = page.sealGeometryDeltas(geometryDeltas);
            sealedDeltas += sealed.offeredDeltas();
            if (page.fullGeometryResyncRequired()) {
                resyncPages++;
                ThermalPage.GeometryResyncToken token = page.beginFullGeometryResync();
                if (token != null) {
                    if (!resolvedInputs.canOfferFullResync()
                            || !resolvedInputs.offerFullResync(
                                    page.sectionKey(),
                                    page.lifecycleGeneration(),
                                    token.requiredRevision(),
                                    effectiveTick,
                                    token.reason(),
                                    captureFullPageSnapshot(page))) {
                        offThreadResyncPending.set(true);
                    }
                }
            }
        }
        dirtyPages.clear();

        InputWatermarks watermarks = new InputWatermarks(
                resolvedInputs.latestOfferedWatermark(),
                runtime.latestOfferedSourceWatermark(),
                chunkWatermark,
                profileWatermark,
                transitionAckWatermark
        );
        SealedInputFrame frame = new SealedInputFrame(
                effectiveTick, dimensionGeneration, watermarks);
        LatestSolveEpochScheduler.SealResult result = runtime.sealFrame(frame);
        if (result == LatestSolveEpochScheduler.SealResult.ACCEPTED
                || result == LatestSolveEpochScheduler.SealResult.DUPLICATE) {
            lastSealedTick = effectiveTick;
        }
        if (shadowCoordinator != null
                && (result == LatestSolveEpochScheduler.SealResult.ACCEPTED
                || result == LatestSolveEpochScheduler.SealResult.DUPLICATE)) {
            pendingShadowFrame.set(frame);
            scheduleShadowWorker();
        }
        long elapsedNanos = Math.max(0L, System.nanoTime() - startedNanos);
        shadowSealCalls = saturatingIncrement(shadowSealCalls);
        shadowSealTotalNanos = saturatingAdd(shadowSealTotalNanos, elapsedNanos);
        shadowSealMaximumNanos = Math.max(shadowSealMaximumNanos, elapsedNanos);
        return new SealReport(frame, result, sealedDeltas, resyncPages);
    }

    public GeometryDeltaRing geometryDeltas() {
        return geometryDeltas;
    }

    public ResolvedGeometryInputRing resolvedInputs() {
        return resolvedInputs;
    }

    /** Enables the concrete applier without changing tick-end or gameplay authority. */
    public void enableTopologyApplication(
            MinecraftThermalTopologyApplier.Parameters parameters
    ) {
        enableTopologyApplication(parameters, MaterialBoundaryRegistry.empty());
    }

    /** Enables topology plus one immutable worker-safe material profile cut. */
    public void enableTopologyApplication(
            MinecraftThermalTopologyApplier.Parameters parameters,
            MaterialBoundaryRegistry materialBoundaries
    ) {
        enableTopologyApplication(
                parameters,
                materialBoundaries,
                MinecraftPhaseTransitionHandler.rejectCustomActions());
    }

    /** Enables built-in snow/ice mutations plus one optional modded handler. */
    public void enableTopologyApplication(
            MinecraftThermalTopologyApplier.Parameters parameters,
            MaterialBoundaryRegistry materialBoundaries,
            MinecraftPhaseTransitionHandler customPhaseTransitionHandler
    ) {
        requireMainThread();
        requireOpen();
        if (topologyApplier != null) {
            throw new IllegalStateException("Minecraft thermal topology application is enabled");
        }
        topologyApplier = new MinecraftThermalTopologyApplier(
                runtime, signatureRegistry, geometryDeltas, resolvedInputs,
                parameters, materialBoundaries);
        this.customPhaseTransitionHandler = Objects.requireNonNull(
                customPhaseTransitionHandler, "customPhaseTransitionHandler");
        maximumPhaseMutationsPerTick = parameters.maximumPhaseMutationsPerTick();
        for (ThermalPage page : pages.values()) {
            topologyApplier.registerAllAirPage(page, chunkWatermark);
        }
    }

    /**
     * Connects tick sealing to the shared coordinator without granting gameplay
     * query authority. Coordinator workers remain externally owned.
     */
    public void enableShadowDispatch(
            ThermalRuntimeCoordinator coordinator,
            Executor boundedSharedExecutor
    ) {
        requireMainThread();
        requireOpen();
        Objects.requireNonNull(coordinator, "coordinator");
        Objects.requireNonNull(boundedSharedExecutor, "boundedSharedExecutor");
        if (topologyApplier == null) {
            throw new IllegalStateException(
                    "topology application must be enabled before shadow dispatch");
        }
        if (shadowCoordinator != null) {
            throw new IllegalStateException("Minecraft thermal shadow dispatch is enabled");
        }
        if (!coordinator.register(runtime)) {
            throw new IllegalStateException("dimension runtime could not be registered");
        }
        shadowCoordinator = coordinator;
        shadowExecutor = boundedSharedExecutor;
    }

    /** Enables the two frozen physical source producers without gameplay authority. */
    public MinecraftPhysicalSourceManager enablePhysicalSources(
            int maximumColdSourcePages
    ) {
        requireMainThread();
        requireOpen();
        if (topologyApplier == null) {
            throw new IllegalStateException(
                    "topology application must be enabled before physical sources");
        }
        if (physicalSources != null) {
            throw new IllegalStateException("Minecraft physical sources are enabled");
        }
        physicalSources = new MinecraftPhysicalSourceManager(
                this, runtime.sourceTimeline(), maximumColdSourcePages);
        return physicalSources;
    }

    /** Enables the optional, read-only Phase J receiver service. */
    public boolean tryEnableRadiation(
            RadiationService.Parameters parameters,
            ThermalMemoryBudget dimensionBudget
    ) {
        requireMainThread();
        requireOpen();
        if (radiation != null) {
            throw new IllegalStateException("Minecraft radiation is enabled");
        }
        MinecraftRadiationOcclusion nextOcclusion = new MinecraftRadiationOcclusion(
                this,
                level,
                parameters.maximumTrackedSections());
        RadiationService next = RadiationService.tryCreate(
                parameters, nextOcclusion, dimensionBudget);
        if (next == null) {
            return false;
        }
        radiationOcclusion = nextOcclusion;
        radiation = next;
        if (physicalSources != null) {
            physicalSources.replayRadiationSources();
        }
        return true;
    }

    public void sampleRadiation(
            long receiverKey,
            int receiverGeneration,
            double receiverX,
            double receiverFeetY,
            double receiverZ,
            RadiationService.MutableSample out
    ) {
        requireMainThread();
        requireOpen();
        if (radiation == null) {
            throw new IllegalStateException("Minecraft radiation is disabled");
        }
        radiation.samplePlayer(
                receiverKey,
                receiverGeneration,
                receiverX,
                receiverFeetY,
                receiverZ,
                out);
    }

    /**
     * Reads an already-admitted player environment without waiting for a
     * worker, loading a chunk, or creating Page interest.
     */
    public void samplePlayerEnvironment(
            long receiverKey,
            int receiverGeneration,
            double receiverX,
            double receiverFeetY,
            double receiverEyeY,
            double receiverZ,
            long currentTick,
            int maximumPublicationAgeTicks,
            MutableEnvironmentSample out
    ) {
        requireMainThread();
        requireOpen();
        Objects.requireNonNull(out, "out");
        requireFinite("receiverX", receiverX);
        requireFinite("receiverFeetY", receiverFeetY);
        requireFinite("receiverEyeY", receiverEyeY);
        requireFinite("receiverZ", receiverZ);
        if (receiverGeneration < 0 || currentTick < 0L
                || maximumPublicationAgeTicks < 0) {
            throw new IllegalArgumentException(
                    "receiver generation, tick, and maximum age must be non-negative");
        }

        passivePlayerQueries = saturatingIncrement(passivePlayerQueries);
        samplePublishedAir(
                receiverX,
                receiverEyeY,
                receiverZ,
                currentTick,
                maximumPublicationAgeTicks,
                out);

        if (radiation == null) {
            out.addFlag(QUERY_RADIATION_UNAVAILABLE);
        } else {
            radiation.samplePlayer(
                    receiverKey,
                    receiverGeneration,
                    receiverX,
                    receiverFeetY,
                    receiverZ,
                    playerRadiationScratch);
            out.setRadiation(
                    playerRadiationScratch.radiantFluxWPerM2(),
                    playerRadiationScratch.confidence());
            if ((playerRadiationScratch.flags()
                    & RadiationService.RADIATION_BUDGET_LIMITED) != 0) {
                out.addFlag(QUERY_RADIATION_BUDGET_LIMITED);
            }
            if ((playerRadiationScratch.flags()
                    & RadiationService.RADIATION_UNRESOLVED) != 0) {
                out.addFlag(QUERY_RADIATION_UNRESOLVED);
            }
        }

        if (out.airAvailable()) {
            passivePlayerHits = saturatingIncrement(passivePlayerHits);
        } else {
            passivePlayerMisses = saturatingIncrement(passivePlayerMisses);
        }
    }

    /**
     * Reads ambient air at one explicitly declared machine receiver point.
     * This passive path does not request radiation or create thermal interest.
     */
    public void sampleMachineEnvironment(
            double receiverX,
            double receiverY,
            double receiverZ,
            long currentTick,
            int maximumPublicationAgeTicks,
            MutableEnvironmentSample out
    ) {
        requireMainThread();
        requireOpen();
        Objects.requireNonNull(out, "out");
        requireFinite("receiverX", receiverX);
        requireFinite("receiverY", receiverY);
        requireFinite("receiverZ", receiverZ);
        if (currentTick < 0L || maximumPublicationAgeTicks < 0) {
            throw new IllegalArgumentException(
                    "tick and maximum age must be non-negative");
        }

        passiveMachineQueries = saturatingIncrement(passiveMachineQueries);
        samplePublishedAir(
                receiverX,
                receiverY,
                receiverZ,
                currentTick,
                maximumPublicationAgeTicks,
                out);
        if (out.airAvailable()) {
            passiveMachineHits = saturatingIncrement(passiveMachineHits);
        } else {
            passiveMachineMisses = saturatingIncrement(passiveMachineMisses);
        }
    }

    /** Reads ambient air for one passive crop/block tick. */
    public void sampleCropEnvironment(
            int receiverBlockX,
            int receiverBlockY,
            int receiverBlockZ,
            long currentTick,
            int maximumPublicationAgeTicks,
            MutableEnvironmentSample out
    ) {
        requireMainThread();
        requireOpen();
        Objects.requireNonNull(out, "out");
        if (currentTick < 0L || maximumPublicationAgeTicks < 0) {
            throw new IllegalArgumentException(
                    "tick and maximum age must be non-negative");
        }

        passiveCropQueries = saturatingIncrement(passiveCropQueries);
        samplePublishedAir(
                receiverBlockX + 0.5D,
                receiverBlockY + 0.5D,
                receiverBlockZ + 0.5D,
                currentTick,
                maximumPublicationAgeTicks,
                out);
        if (out.airAvailable()) {
            passiveCropHits = saturatingIncrement(passiveCropHits);
        } else {
            passiveCropMisses = saturatingIncrement(passiveCropMisses);
        }
    }

    /**
     * Reads one representative per 4-cubed group produced by an existing town
     * building scan. This passive aggregate never scans the world or admits a
     * Page; missing groups only reduce confidence.
     */
    public void sampleTownEnvironment(
            TownThermalProjection projection,
            long currentTick,
            int maximumPublicationAgeTicks,
            MutableEnvironmentSample out
    ) {
        requireMainThread();
        requireOpen();
        Objects.requireNonNull(projection, "projection");
        Objects.requireNonNull(out, "out");
        if (currentTick < 0L || maximumPublicationAgeTicks < 0) {
            throw new IllegalArgumentException(
                    "tick and maximum age must be non-negative");
        }

        passiveTownQueries = saturatingIncrement(passiveTownQueries);
        long[] groupKeys = projection.groupKeys();
        int hitWeight = 0;
        double weightedTemperatureC = 0.0D;
        int commonMediumId = -1;
        boolean mediumSet = false;
        int combinedCellFlags = 0;
        int combinedQueryFlags = 0;
        long oldestSampleTick = Long.MAX_VALUE;
        for (long groupKey : groupKeys) {
            int weight = projection.weight(groupKey);
            if (weight <= 0) {
                continue;
            }
            passiveTownGroupLookups = saturatingIncrement(
                    passiveTownGroupLookups);
            samplePublishedAir(
                    projection.representativeX(groupKey) + 0.5D,
                    projection.representativeY(groupKey) + 0.5D,
                    projection.representativeZ(groupKey) + 0.5D,
                    currentTick,
                    maximumPublicationAgeTicks,
                    townGroupEnvironmentScratch);
            combinedQueryFlags |= townGroupEnvironmentScratch.flags();
            if (!townGroupEnvironmentScratch.airAvailable()) {
                continue;
            }
            hitWeight += weight;
            weightedTemperatureC +=
                    townGroupEnvironmentScratch.airTemperatureC() * weight;
            combinedCellFlags |= townGroupEnvironmentScratch.cellFlags();
            oldestSampleTick = Math.min(oldestSampleTick,
                    townGroupEnvironmentScratch.sampleTick());
            if (!mediumSet) {
                commonMediumId = townGroupEnvironmentScratch.mediumId();
                mediumSet = true;
            } else if (commonMediumId
                    != townGroupEnvironmentScratch.mediumId()) {
                commonMediumId = -1;
            }
        }

        out.clear();
        int voxelCount = projection.voxelCount();
        if (hitWeight > 0) {
            if (hitWeight < voxelCount) {
                combinedQueryFlags |= QUERY_PARTIAL_REGION;
                partialTownQueries = saturatingIncrement(partialTownQueries);
            }
            out.setAggregateAir(
                    weightedTemperatureC / hitWeight,
                    commonMediumId,
                    combinedCellFlags,
                    oldestSampleTick,
                    voxelCount == 0 ? 0.0F : (float) hitWeight / voxelCount,
                    combinedQueryFlags);
            passiveTownHits = saturatingIncrement(passiveTownHits);
        } else {
            if (voxelCount == 0) {
                combinedQueryFlags |= QUERY_NO_AIR_COMPONENT;
            }
            out.addFlags(combinedQueryFlags);
            passiveTownMisses = saturatingIncrement(passiveTownMisses);
        }
    }

    private void samplePublishedAir(
            double receiverX,
            double receiverY,
            double receiverZ,
            long currentTick,
            int maximumPublicationAgeTicks,
            MutableEnvironmentSample out
    ) {
        out.clear();
        publishedAirLookups = saturatingIncrement(publishedAirLookups);
        int blockX = floor(receiverX);
        int blockY = floor(receiverY);
        int blockZ = floor(receiverZ);
        long sectionKey = SectionPos.asLong(
                SectionPos.blockToSectionCoord(blockX),
                SectionPos.blockToSectionCoord(blockY),
                SectionPos.blockToSectionCoord(blockZ));
        ThermalPage page = pages.get(sectionKey);
        int slot = MinecraftThermalTopologyApplier.POINT_TOPOLOGY_UNAVAILABLE;
        if (page == null) {
            out.addFlag(QUERY_NO_PAGE);
        } else {
            int localX = SectionPos.sectionRelative(blockX);
            int localY = SectionPos.sectionRelative(blockY);
            int localZ = SectionPos.sectionRelative(blockZ);
            if (!page.tryQueryPublishedCoverage(
                    localX, localY, localZ, environmentCoverageScratch)) {
                out.addFlag(QUERY_STALE_GEOMETRY);
            } else if (environmentCoverageScratch.coverageRef()
                    == ThermalPage.NO_COVERAGE) {
                out.addFlag(QUERY_NO_AIR_COMPONENT);
                slot = MinecraftThermalTopologyApplier.POINT_NO_AIR;
            } else if ((page.mixedBrickMask()
                    & (1L << environmentCoverageScratch.baseBrickIndex())) == 0L) {
                slot = environmentCoverageScratch.coverageRef();
            } else if (topologyApplier == null) {
                out.addFlag(QUERY_STALE_GEOMETRY);
            } else {
                slot = topologyApplier.resolvePublishedAirPoint(
                        page,
                        localX,
                        localY,
                        localZ,
                        microcellIndex(
                                receiverX, receiverY, receiverZ,
                                blockX, blockY, blockZ));
                if (slot == MinecraftThermalTopologyApplier.POINT_NO_AIR) {
                    out.addFlag(QUERY_NO_AIR_COMPONENT);
                } else if (slot
                        == MinecraftThermalTopologyApplier.POINT_TOPOLOGY_UNAVAILABLE) {
                    out.addFlag(QUERY_STALE_GEOMETRY);
                }
            }
        }

        if (slot >= 0) {
            if (!runtime.tryReadPublishedCell(slot, environmentPublicationScratch)) {
                out.addFlag(QUERY_PUBLICATION_MISS);
            } else if (!page.publishedGeometryIsCurrent()
                    || page.publishedGeometryRevision()
                    != environmentCoverageScratch.geometryRevision()
                    || page.publishedTopologyGeneration()
                    != environmentCoverageScratch.topologyGeneration()) {
                out.addFlag(QUERY_STALE_GEOMETRY);
            } else {
                long age = currentTick - environmentPublicationScratch.sampleTick();
                if (age >= 0L) {
                    publicationAgeSamples = saturatingIncrement(publicationAgeSamples);
                    publicationAgeTotalTicks = saturatingAdd(
                            publicationAgeTotalTicks, age);
                    maximumObservedPublicationAgeTicks = Math.max(
                            maximumObservedPublicationAgeTicks, age);
                }
                if (age < 0L || age > maximumPublicationAgeTicks) {
                    out.addFlag(QUERY_PUBLICATION_STALE);
                } else {
                    out.setAir(
                            environmentPublicationScratch.temperatureC(),
                            environmentPublicationScratch.mediumId(),
                            environmentPublicationScratch.flags(),
                            environmentPublicationScratch.sampleTick());
                    if (!runtime.topologyResolved()) {
                        out.addFlag(QUERY_DEGRADED_TOPOLOGY);
                    }
                }
            }
        }
        recordPublishedAirOutcome(out);
    }

    /** Observes the legacy player result; it never writes gameplay state. */
    public static void observePlayerEnvironment(
            ServerPlayer player,
            double legacyAirTemperatureC
    ) {
        Objects.requireNonNull(player, "player");
        MinecraftThermalInput input = active(player.serverLevel());
        if (input == null || !Double.isFinite(legacyAirTemperatureC)) {
            return;
        }
        input.samplePlayerEnvironment(
                playerReceiverKey(player),
                player.getId() & Integer.MAX_VALUE,
                player.getX(),
                player.getY(),
                player.getEyeY(),
                player.getZ(),
                player.serverLevel().getGameTime(),
                SHADOW_MAX_PUBLICATION_AGE_TICKS,
                input.playerEnvironmentScratch);
        input.recordPlayerEnvironment(
                legacyAirTemperatureC, input.playerEnvironmentScratch);
    }

    private void recordPlayerEnvironment(
            double legacyAirTemperatureC,
            MutableEnvironmentSample sample
    ) {
        if (sample.airAvailable()) {
            double error = Math.abs(
                    sample.airTemperatureC()
                            - legacyAirTemperatureC);
            playerShadowComparisons = saturatingIncrement(playerShadowComparisons);
            playerShadowAbsoluteErrorSumC = Math.min(
                    Double.MAX_VALUE,
                    playerShadowAbsoluteErrorSumC + error);
            playerShadowMaximumAbsoluteErrorC = Math.max(
                    playerShadowMaximumAbsoluteErrorC, error);
        }
        latestLegacyAirTemperatureC = legacyAirTemperatureC;
        latestShadowAirTemperatureC = sample.airTemperatureC();
        latestPlayerShadowSampleTick = sample.sampleTick();
        latestPlayerShadowFlags = sample.flags();
    }

    private static long playerReceiverKey(ServerPlayer player) {
        return player.getUUID().getMostSignificantBits()
                ^ Long.rotateLeft(player.getUUID().getLeastSignificantBits(), 17);
    }

    /**
     * Returns the published thermal air value used by the player temperature
     * path. The legacy value is retained only while the first Page publication
     * is being built or when the queried point has no resolved air component.
     */
    public static double gameplayPlayerEnvironment(
            ServerPlayer player,
            double legacyAirTemperatureC,
            MutableEnvironmentSample out
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(out, "out").clear();
        if (!Double.isFinite(legacyAirTemperatureC)) {
            return legacyAirTemperatureC;
        }
        MinecraftThermalInput input = active(player.serverLevel());
        if (input == null) {
            input = startGameplayInput(player.serverLevel(), legacyAirTemperatureC);
        }
        if (input == null || !input.ensureGameplayPage(
                BlockPos.containing(player.getX(), player.getEyeY(), player.getZ()))) {
            return legacyAirTemperatureC;
        }
        input.samplePlayerEnvironment(
                playerReceiverKey(player),
                player.getId() & Integer.MAX_VALUE,
                player.getX(),
                player.getY(),
                player.getEyeY(),
                player.getZ(),
                player.serverLevel().getGameTime(),
                SHADOW_MAX_PUBLICATION_AGE_TICKS,
                out);
        input.recordPlayerEnvironment(legacyAirTemperatureC, out);
        return out.airAvailable()
                ? out.airTemperatureC()
                : legacyAirTemperatureC;
    }

    public PlayerShadowSnapshot playerShadowSnapshot() {
        requireMainThread();
        double meanAbsoluteErrorC = playerShadowComparisons == 0L
                ? Double.NaN
                : playerShadowAbsoluteErrorSumC / playerShadowComparisons;
        double maximumAbsoluteErrorC = playerShadowComparisons == 0L
                ? Double.NaN : playerShadowMaximumAbsoluteErrorC;
        return new PlayerShadowSnapshot(
                passivePlayerQueries,
                passivePlayerHits,
                passivePlayerMisses,
                playerShadowComparisons,
                meanAbsoluteErrorC,
                maximumAbsoluteErrorC,
                latestLegacyAirTemperatureC,
                latestShadowAirTemperatureC,
                latestPlayerShadowSampleTick,
                latestPlayerShadowFlags);
    }

    /**
     * Records one explicitly registered QUERY_ONLY machine comparison. Ordinary
     * machines never call this method and therefore create no thermal state.
     */
    public static void observeRegisteredMachineEnvironment(
            ServerLevel level,
            BlockPos receiverPosition,
            double legacyAirTemperatureC
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(receiverPosition, "receiverPosition");
        MinecraftThermalInput input = active(level);
        if (input == null || !Double.isFinite(legacyAirTemperatureC)) {
            return;
        }
        input.sampleMachineEnvironment(
                receiverPosition.getX() + 0.5D,
                receiverPosition.getY() + 0.5D,
                receiverPosition.getZ() + 0.5D,
                level.getGameTime(),
                SHADOW_MAX_PUBLICATION_AGE_TICKS,
                input.machineEnvironmentScratch);
        if (input.machineEnvironmentScratch.airAvailable()) {
            double error = Math.abs(
                    input.machineEnvironmentScratch.airTemperatureC()
                            - legacyAirTemperatureC);
            input.machineShadowComparisons = saturatingIncrement(
                    input.machineShadowComparisons);
            input.machineShadowAbsoluteErrorSumC = Math.min(
                    Double.MAX_VALUE,
                    input.machineShadowAbsoluteErrorSumC + error);
            input.machineShadowMaximumAbsoluteErrorC = Math.max(
                    input.machineShadowMaximumAbsoluteErrorC, error);
        }
        input.latestLegacyMachineTemperatureC = legacyAirTemperatureC;
        input.latestShadowMachineTemperatureC =
                input.machineEnvironmentScratch.airTemperatureC();
        input.latestMachineShadowSampleTick =
                input.machineEnvironmentScratch.sampleTick();
        input.latestMachineShadowFlags = input.machineEnvironmentScratch.flags();
    }

    public MachineShadowSnapshot machineShadowSnapshot() {
        requireMainThread();
        double meanAbsoluteErrorC = machineShadowComparisons == 0L
                ? Double.NaN
                : machineShadowAbsoluteErrorSumC / machineShadowComparisons;
        double maximumAbsoluteErrorC = machineShadowComparisons == 0L
                ? Double.NaN : machineShadowMaximumAbsoluteErrorC;
        return new MachineShadowSnapshot(
                passiveMachineQueries,
                passiveMachineHits,
                passiveMachineMisses,
                machineShadowComparisons,
                meanAbsoluteErrorC,
                maximumAbsoluteErrorC,
                latestLegacyMachineTemperatureC,
                latestShadowMachineTemperatureC,
                latestMachineShadowSampleTick,
                latestMachineShadowFlags);
    }

    /** Returns published crop air when available, otherwise the legacy fallback. */
    public static double gameplayCropEnvironment(
            LevelAccessor level,
            BlockPos receiverPosition,
            double legacyBlockTemperatureC
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(receiverPosition, "receiverPosition");
        if (!(level instanceof ServerLevel serverLevel)
                || !Double.isFinite(legacyBlockTemperatureC)) {
            return legacyBlockTemperatureC;
        }
        MinecraftThermalInput input = active(serverLevel);
        if (input == null) {
            return legacyBlockTemperatureC;
        }
        input.sampleCropEnvironment(
                receiverPosition.getX(),
                receiverPosition.getY(),
                receiverPosition.getZ(),
                serverLevel.getGameTime(),
                SHADOW_MAX_PUBLICATION_AGE_TICKS,
                input.cropEnvironmentScratch);
        if (input.cropEnvironmentScratch.airAvailable()) {
            double error = Math.abs(
                    input.cropEnvironmentScratch.airTemperatureC()
                            - legacyBlockTemperatureC);
            input.cropShadowComparisons = saturatingIncrement(
                    input.cropShadowComparisons);
            input.cropShadowAbsoluteErrorSumC = Math.min(
                    Double.MAX_VALUE,
                    input.cropShadowAbsoluteErrorSumC + error);
            input.cropShadowMaximumAbsoluteErrorC = Math.max(
                    input.cropShadowMaximumAbsoluteErrorC, error);
        }
        input.latestLegacyCropTemperatureC = legacyBlockTemperatureC;
        input.latestShadowCropTemperatureC =
                input.cropEnvironmentScratch.airTemperatureC();
        input.latestCropShadowSampleTick =
                input.cropEnvironmentScratch.sampleTick();
        input.latestCropShadowFlags = input.cropEnvironmentScratch.flags();
        return input.cropEnvironmentScratch.airAvailable()
                ? input.cropEnvironmentScratch.airTemperatureC()
                : legacyBlockTemperatureC;
    }

    public CropShadowSnapshot cropShadowSnapshot() {
        requireMainThread();
        double meanAbsoluteErrorC = cropShadowComparisons == 0L
                ? Double.NaN
                : cropShadowAbsoluteErrorSumC / cropShadowComparisons;
        double maximumAbsoluteErrorC = cropShadowComparisons == 0L
                ? Double.NaN : cropShadowMaximumAbsoluteErrorC;
        return new CropShadowSnapshot(
                passiveCropQueries,
                passiveCropHits,
                passiveCropMisses,
                cropShadowComparisons,
                meanAbsoluteErrorC,
                maximumAbsoluteErrorC,
                latestLegacyCropTemperatureC,
                latestShadowCropTemperatureC,
                latestCropShadowSampleTick,
                latestCropShadowFlags);
    }

    /** Returns a complete published town average, otherwise the legacy fallback. */
    public static double gameplayTownEnvironment(
            LevelAccessor level,
            TownThermalProjection projection,
            double legacyAverageTemperatureC
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(projection, "projection");
        if (!(level instanceof ServerLevel serverLevel)
                || !Double.isFinite(legacyAverageTemperatureC)
                || projection.voxelCount() == 0) {
            return legacyAverageTemperatureC;
        }
        MinecraftThermalInput input = active(serverLevel);
        if (input == null) {
            return legacyAverageTemperatureC;
        }
        MutableEnvironmentSample sample = input.townEnvironmentScratch;
        input.sampleTownEnvironment(
                projection,
                serverLevel.getGameTime(),
                SHADOW_MAX_PUBLICATION_AGE_TICKS,
                sample);
        boolean complete = sample.airAvailable()
                && (sample.flags() & QUERY_PARTIAL_REGION) == 0;
        if (complete) {
            double error = Math.abs(
                    sample.airTemperatureC() - legacyAverageTemperatureC);
            input.townShadowComparisons = saturatingIncrement(
                    input.townShadowComparisons);
            input.townShadowAbsoluteErrorSumC = Math.min(
                    Double.MAX_VALUE,
                    input.townShadowAbsoluteErrorSumC + error);
            input.townShadowMaximumAbsoluteErrorC = Math.max(
                    input.townShadowMaximumAbsoluteErrorC, error);
        }
        input.latestLegacyTownTemperatureC = legacyAverageTemperatureC;
        input.latestShadowTownTemperatureC = sample.airTemperatureC();
        input.latestTownShadowSampleTick = sample.sampleTick();
        input.latestTownGroupCount = projection.groupCount();
        input.latestTownVoxelCount = projection.voxelCount();
        input.latestTownShadowFlags = sample.flags();
        return complete ? sample.airTemperatureC() : legacyAverageTemperatureC;
    }

    public TownShadowSnapshot townShadowSnapshot() {
        requireMainThread();
        double meanAbsoluteErrorC = townShadowComparisons == 0L
                ? Double.NaN
                : townShadowAbsoluteErrorSumC / townShadowComparisons;
        double maximumAbsoluteErrorC = townShadowComparisons == 0L
                ? Double.NaN : townShadowMaximumAbsoluteErrorC;
        return new TownShadowSnapshot(
                passiveTownQueries,
                passiveTownGroupLookups,
                passiveTownHits,
                passiveTownMisses,
                partialTownQueries,
                townShadowComparisons,
                meanAbsoluteErrorC,
                maximumAbsoluteErrorC,
                latestLegacyTownTemperatureC,
                latestShadowTownTemperatureC,
                latestTownShadowSampleTick,
                latestTownGroupCount,
                latestTownVoxelCount,
                latestTownShadowFlags);
    }

    public int radiationSourceCount() {
        requireMainThread();
        return radiation == null ? 0 : radiation.sourceCount();
    }

    public ShadowReport latestShadowReport() {
        return latestShadowReport;
    }

    /** Captures current shadow evidence without querying the world or gameplay state. */
    public ShadowRuntimeSnapshot shadowRuntimeSnapshot() {
        requireMainThread();
        requireOpen();
        long mixedBricks = 0L;
        for (ThermalPage page : pages.values()) {
            mixedBricks = saturatingAdd(
                    mixedBricks, Long.bitCount(page.mixedBrickMask()));
        }
        double meanPublicationAgeTicks = publicationAgeSamples == 0L
                ? Double.NaN
                : (double) publicationAgeTotalTicks / publicationAgeSamples;
        int readyCount = shadowCoordinator == null ? 0 : shadowCoordinator.readyCount();
        String mailboxState = shadowCoordinator == null
                ? "DISABLED"
                : shadowCoordinator.mailboxState(
                        runtime.runtimeId(), dimensionGeneration);
        return new ShadowRuntimeSnapshot(
                level.getGameTime(),
                pages.size(),
                mixedBricks,
                ownersBySectionKey.size(),
                physicalSources == null ? 0 : physicalSources.sourceCount(),
                radiation == null ? 0 : radiation.sourceCount(),
                publishedAirLookups,
                publishedAirHits,
                publishedAirMisses,
                noPageLookups,
                noAirComponentLookups,
                staleGeometryLookups,
                publicationMissLookups,
                stalePublicationLookups,
                degradedTopologyLookups,
                publicationAgeSamples,
                meanPublicationAgeTicks,
                maximumObservedPublicationAgeTicks,
                shadowSealCalls,
                shadowSealTotalNanos,
                shadowSealMaximumNanos,
                shadowWorkerFrames,
                shadowWorkerTotalNanos,
                shadowWorkerMaximumNanos,
                shadowExecutorRejectedSubmissions,
                readyCount,
                mailboxState,
                runtime.diagnostics(),
                playerShadowSnapshot(),
                machineShadowSnapshot(),
                cropShadowSnapshot(),
                townShadowSnapshot(),
                latestShadowReport);
    }

    /** Explicit manual path used when asynchronous shadow dispatch is disabled. */
    public MinecraftThermalTopologyApplier.ApplyReport applyTopology(
            SealedInputFrame frame
    ) {
        requireMainThread();
        requireOpen();
        if (topologyApplier == null) {
            throw new IllegalStateException("Minecraft thermal topology application is disabled");
        }
        return topologyApplier.apply(frame);
    }

    public int admittedPageCount() {
        requireMainThread();
        return pages.size();
    }

    public int witnessedSectionCount() {
        requireMainThread();
        return ownersBySectionKey.size();
    }

    long topologyGeneration() {
        return runtime.topologyGeneration();
    }

    void upsertRadiationSource(
            long sourceKey,
            int lifecycleGeneration,
            BlockPos sourcePosition,
            MinecraftPhysicalSourceProfile profile,
            double powerW,
            boolean enabled
    ) {
        if (radiation == null) {
            return;
        }
        radiation.upsertSource(
                sourceKey,
                lifecycleGeneration,
                sourcePosition.getX() + profile.radiationOffsetX(),
                sourcePosition.getY() + profile.radiationOffsetY(),
                sourcePosition.getZ() + profile.radiationOffsetZ(),
                enabled ? profile.radiativePowerW(powerW) : 0.0D,
                profile.radiationDirectionalUpperBound());
    }

    void removeRadiationSource(long sourceKey) {
        if (radiation != null) {
            radiation.removeSource(sourceKey);
        }
    }

    void retainRadiationSection(long packedRadiationSection) {
        long sectionKey = SectionPos.asLong(
                RadiationService.sectionX(packedRadiationSection),
                RadiationService.sectionY(packedRadiationSection),
                RadiationService.sectionZ(packedRadiationSection));
        if (radiationTrackedSections.add(sectionKey)) {
            attachLoadedWitness(sectionKey);
        }
    }

    MinecraftThermalTopologyApplier.PortResolution resolvePhysicalSourcePort(
            BlockPos target,
            com.teammoeg.frostedheart.content.climate.thermal.geometry
                    .ConservativeAirGeometry.Face targetFace
    ) {
        return topologyApplier.resolveAirFacePort(
                target.getX(), target.getY(), target.getZ(), targetFace);
    }

    private boolean ensureGameplayPage(BlockPos position) {
        requireMainThread();
        requireOpen();
        if (topologyApplier == null) {
            return false;
        }
        int sectionX = SectionPos.blockToSectionCoord(position.getX());
        int sectionY = SectionPos.blockToSectionCoord(position.getY());
        int sectionZ = SectionPos.blockToSectionCoord(position.getZ());
        long sectionKey = SectionPos.asLong(sectionX, sectionY, sectionZ);
        if (pages.containsKey(sectionKey)) {
            return true;
        }
        LevelChunk chunk = level.getChunkSource().getChunkNow(sectionX, sectionZ);
        if (chunk == null) {
            return false;
        }
        int sectionIndex = chunk.getSectionIndexFromSectionY(sectionY);
        if (sectionIndex < 0 || sectionIndex >= chunk.getSections().length) {
            return false;
        }

        long lifecycleGeneration = nextSectionGeneration.incrementAndGet();
        long admissionWatermark = Math.incrementExact(chunkWatermark);
        chunkWatermark = admissionWatermark;
        ThermalPage page = topologyApplier.registerCapturedPage(
                sectionKey,
                lifecycleGeneration,
                admissionWatermark,
                captureFullPageSnapshot(sectionKey));
        pages.put(sectionKey, page);
        adjustWitnesses(sectionX, sectionY, sectionZ, 1);
        refreshNearbyOwnerPageViews(sectionX, sectionY, sectionZ);
        if (physicalSources != null) {
            physicalSources.onChunkLoad(chunk);
        }
        return true;
    }

    boolean retainPhysicalSourcePage(BlockPos target, int maximumColdSourcePages) {
        requireMainThread();
        requireOpen();
        int sectionX = SectionPos.blockToSectionCoord(target.getX());
        int sectionY = SectionPos.blockToSectionCoord(target.getY());
        int sectionZ = SectionPos.blockToSectionCoord(target.getZ());
        long sectionKey = SectionPos.asLong(sectionX, sectionY, sectionZ);
        ThermalPage existing = pages.get(sectionKey);
        if (existing != null) {
            physicalSourcePageRefCounts.merge(sectionKey, 1, Math::addExact);
            return true;
        }
        if (physicalSourceOwnedPages.size() >= maximumColdSourcePages) {
            return false;
        }
        LevelChunk chunk = level.getChunkSource().getChunkNow(sectionX, sectionZ);
        if (chunk == null) {
            return false;
        }
        int sectionIndex = chunk.getSectionIndexFromSectionY(sectionY);
        if (sectionIndex < 0 || sectionIndex >= chunk.getSections().length) {
            return false;
        }

        long lifecycleGeneration = nextSectionGeneration.incrementAndGet();
        long admissionWatermark = Math.incrementExact(chunkWatermark);
        chunkWatermark = admissionWatermark;
        ThermalPage page = topologyApplier.registerCapturedPage(
                sectionKey,
                lifecycleGeneration,
                admissionWatermark,
                captureFullPageSnapshot(sectionKey));
        pages.put(sectionKey, page);
        physicalSourcePageRefCounts.put(sectionKey, 1);
        physicalSourceOwnedPages.add(sectionKey);
        adjustWitnesses(sectionX, sectionY, sectionZ, 1);
        refreshNearbyOwnerPageViews(sectionX, sectionY, sectionZ);
        return true;
    }

    void releasePhysicalSourcePage(long sectionKey) {
        requireMainThread();
        Integer references = physicalSourcePageRefCounts.get(sectionKey);
        if (references == null) {
            return;
        }
        if (references > 1) {
            physicalSourcePageRefCounts.put(sectionKey, references - 1);
            return;
        }
        physicalSourcePageRefCounts.remove(sectionKey);
        if (physicalSourceOwnedPages.remove(sectionKey)) {
            withdrawPage(sectionKey);
        }
    }

    public long chunkWatermark() {
        requireMainThread();
        return chunkWatermark;
    }

    public double committedPhaseTransitionEnergyJ() {
        requireMainThread();
        return topologyApplier == null ? 0.0D : topologyApplier.committedPhaseEnergyJ();
    }

    private void processPhaseTransitions() {
        if (topologyApplier == null || maximumPhaseMutationsPerTick == 0) {
            return;
        }
        topologyApplier.flushPendingPhaseAcks();
        int processed = 0;
        while (processed < maximumPhaseMutationsPerTick
                && topologyApplier.canAcceptPhaseAck()
                && topologyApplier.pollPhaseRequest(phaseRequest)) {
            MinecraftPhaseTransitionHandler.Outcome outcome =
                    applyPhaseTransition(phaseRequest);
            topologyApplier.submitPhaseAck(
                    phaseRequest,
                    switch (outcome) {
                        case APPLIED -> PhaseTransitionRuntime.AckOutcome.APPLIED;
                        case REJECTED -> PhaseTransitionRuntime.AckOutcome.REJECTED;
                        case RETRY -> PhaseTransitionRuntime.AckOutcome.RETRY;
                    });
            processed++;
        }
        topologyApplier.flushPendingPhaseAcks();
        transitionAckWatermark = Math.max(
                transitionAckWatermark,
                topologyApplier.latestPhaseAckWatermark());
    }

    private MinecraftPhaseTransitionHandler.Outcome applyPhaseTransition(
            PhaseTransitionRuntime.MutableRequest request
    ) {
        BlockPos position = new BlockPos(
                request.blockX(), request.blockY(), request.blockZ());
        long sectionKey = SectionPos.asLong(
                SectionPos.blockToSectionCoord(position.getX()),
                SectionPos.blockToSectionCoord(position.getY()),
                SectionPos.blockToSectionCoord(position.getZ()));
        ThermalPage page = pages.get(sectionKey);
        if (page == null
                || page.lifecycleGeneration() != request.lifecycleGeneration()) {
            return MinecraftPhaseTransitionHandler.Outcome.REJECTED;
        }
        LevelChunk chunk = level.getChunkSource().getChunkNow(
                SectionPos.blockToSectionCoord(position.getX()),
                SectionPos.blockToSectionCoord(position.getZ()));
        if (chunk == null || level.isOutsideBuildHeight(position)) {
            return MinecraftPhaseTransitionHandler.Outcome.REJECTED;
        }
        MaterialBoundaryRegistry.Profile profile =
                topologyApplier.materialProfile(request.profileId());
        if (profile == null
                || profile.model() != MaterialBoundaryRegistry.Model.PHASE_RESERVOIR) {
            return MinecraftPhaseTransitionHandler.Outcome.REJECTED;
        }
        BlockState currentState = chunk.getBlockState(position);
        LoadedCube cube = new LoadedCube(
                level, position.getX(), position.getY(), position.getZ());
        ResolvedThermalSignature currentSignature = resolveCurrentSignature(
                cube, 0, 0, 0);
        if (currentSignature == null
                || currentSignature.materialProfileId() != profile.id()
                || !hasExposedThermalAir(cube, currentSignature)) {
            return MinecraftPhaseTransitionHandler.Outcome.REJECTED;
        }
        int randomTickSpeed = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        if (!allowsAutomaticPhaseMutation(
                profile.transitionMutationPolicy(), randomTickSpeed)) {
            return profile.transitionMutationPolicy()
                    == MaterialBoundaryRegistry.TransitionMutationPolicy.NONE
                    ? MinecraftPhaseTransitionHandler.Outcome.REJECTED
                    : MinecraftPhaseTransitionHandler.Outcome.RETRY;
        }
        return switch (profile.transitionAction()) {
            case REMOVE_ONE_SNOW_LAYER -> removeOneSnowLayer(position, currentState);
            case MELT_ICE_TO_WATER -> meltIce(position, currentState);
            case APPLY_STATE_TRANSITION_RECIPE -> applyStateTransitionRecipe(
                    position, currentState, profile);
            case CUSTOM -> customPhaseTransitionHandler.apply(
                    level, position, currentState, profile);
            case NONE -> MinecraftPhaseTransitionHandler.Outcome.REJECTED;
        };
    }

    private MinecraftPhaseTransitionHandler.Outcome applyStateTransitionRecipe(
            BlockPos position,
            BlockState currentState,
            MaterialBoundaryRegistry.Profile profile
    ) {
        StateTransitionData data = StateTransitionData.getData(currentState);
        StateTransitionData.HeatingTransition transition = data == null
                ? null : data.heatingTransition(currentState);
        if (data == null
                || !data.willTransit()
                || data.heatCapacity() <= 0
                || transition == null
                || Double.compare(
                        transition.temperatureC(),
                        profile.transitionTemperatureC()) != 0) {
            return MinecraftPhaseTransitionHandler.Outcome.REJECTED;
        }
        if (currentState.is(BlockTags.ICE)
                && level.getBiome(position).is(FHTags.Biomes.ICE_DO_NOT_SMELT.tag)) {
            return MinecraftPhaseTransitionHandler.Outcome.RETRY;
        }
        return level.setBlockAndUpdate(position, transition.targetBlock())
                ? MinecraftPhaseTransitionHandler.Outcome.APPLIED
                : MinecraftPhaseTransitionHandler.Outcome.REJECTED;
    }

    /** True only while this exact hot-side candidate is installed in a live Page. */
    public static boolean ownsGameplayHeatingTransition(
            ServerLevel level,
            BlockPos position,
            BlockState currentState,
            StateTransitionData data
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(currentState, "currentState");
        Objects.requireNonNull(data, "data");
        if (!data.willTransit()
                || data.heatCapacity() <= 0
                || data.heatingTransition(currentState) == null) {
            return false;
        }
        MinecraftThermalInput input = active(level);
        if (input == null || input.topologyApplier == null) {
            return false;
        }
        input.requireMainThread();
        Integer profileId = gameplayPhaseProfileIds.get(currentState);
        if (profileId == null) {
            return false;
        }
        MaterialBoundaryRegistry.Profile profile =
                input.topologyApplier.materialProfile(profileId);
        if (profile == null
                || profile.transitionAction()
                        != MaterialBoundaryRegistry.TransitionAction
                                .APPLY_STATE_TRANSITION_RECIPE) {
            return false;
        }
        long sectionKey = SectionPos.asLong(
                SectionPos.blockToSectionCoord(position.getX()),
                SectionPos.blockToSectionCoord(position.getY()),
                SectionPos.blockToSectionCoord(position.getZ()));
        ThermalPage page = input.pages.get(sectionKey);
        return page != null && input.topologyApplier.hasAppliedPhaseCandidate(
                page,
                position.getX(),
                position.getY(),
                position.getZ(),
                profileId);
    }

    private ResolvedThermalSignature resolveCurrentSignature(
            LoadedCube cube,
            int offsetX,
            int offsetY,
            int offsetZ
    ) {
        Optional<ResolverBlockView.StateAndFluid<BlockState, FluidState>> cell =
                cube.cell(offsetX, offsetY, offsetZ).value();
        if (cell.isEmpty()) {
            return null;
        }
        ThermalSignatureResolverDispatcher.DispatchPlan plan =
                resolverDispatcher.plan(cell.orElseThrow().blockState());
        ThermalSignatureResolution resolution = resolveCenter(
                cube, offsetX, offsetY, offsetZ, plan);
        return resolution.status() == ThermalResolution.Status.RESOLVED
                ? signatureRegistry.signature(resolution.signatureId()).orElse(null)
                : null;
    }

    private boolean hasExposedThermalAir(
            LoadedCube cube,
            ResolvedThermalSignature currentSignature
    ) {
        MaterialBoundaryRegistry.ContactPattern pattern =
                topologyApplier.materialContactPattern(
                        currentSignature.materialContactPatternId());
        if (pattern == null) {
            return false;
        }
        long[] neighborAirMasks = new long[PHASE_NEIGHBOR_OFFSETS.length];
        for (int direction = 0; direction < PHASE_NEIGHBOR_OFFSETS.length; direction++) {
            int[] offset = PHASE_NEIGHBOR_OFFSETS[direction];
            ResolvedThermalSignature neighbor = resolveCurrentSignature(
                    cube, offset[0], offset[1], offset[2]);
            neighborAirMasks[direction] = provenAirMask(neighbor);
        }
        return hasExposedPhaseContact(
                pattern.materialMicrocellMask(),
                provenAirMask(currentSignature),
                neighborAirMasks);
    }

    static boolean hasExposedPhaseContact(
            long materialMask,
            long sameBlockAirMask,
            long[] neighborAirMasks
    ) {
        if (materialMask == 0L || neighborAirMasks == null
                || neighborAirMasks.length != PHASE_NEIGHBOR_OFFSETS.length) {
            return false;
        }
        for (long remaining = materialMask; remaining != 0L;
             remaining &= remaining - 1L) {
            int bit = Long.numberOfTrailingZeros(remaining);
            int microX = bit & 3;
            int microZ = (bit >>> 2) & 3;
            int microY = bit >>> 4;
            for (int direction = 0;
                 direction < PHASE_NEIGHBOR_OFFSETS.length; direction++) {
                int[] offset = PHASE_NEIGHBOR_OFFSETS[direction];
                int targetX = microX + offset[0];
                int targetY = microY + offset[1];
                int targetZ = microZ + offset[2];
                boolean sameBlock = targetX >= 0 && targetX < 4
                        && targetY >= 0 && targetY < 4
                        && targetZ >= 0 && targetZ < 4;
                int targetBit = (Math.floorMod(targetY, 4) << 4)
                        | (Math.floorMod(targetZ, 4) << 2)
                        | Math.floorMod(targetX, 4);
                long airMask = sameBlock
                        ? sameBlockAirMask : neighborAirMasks[direction];
                if ((airMask & (1L << targetBit)) != 0L) {
                    return true;
                }
            }
        }
        return false;
    }

    private static long provenAirMask(ResolvedThermalSignature signature) {
        if (signature == null) {
            return 0L;
        }
        long mask = 0L;
        for (var region : signature.airRegions()) {
            mask |= region.provenAirMicrocellMask();
        }
        return mask;
    }

    private MinecraftPhaseTransitionHandler.Outcome removeOneSnowLayer(
            BlockPos position,
            BlockState currentState
    ) {
        BlockState replacement;
        if (currentState.is(Blocks.SNOW)
                && currentState.hasProperty(SnowLayerBlock.LAYERS)) {
            int layers = currentState.getValue(SnowLayerBlock.LAYERS);
            replacement = layers > 1
                    ? currentState.setValue(SnowLayerBlock.LAYERS, layers - 1)
                    : Blocks.AIR.defaultBlockState();
        } else if (currentState.is(Blocks.SNOW_BLOCK)) {
            replacement = Blocks.AIR.defaultBlockState();
        } else {
            return MinecraftPhaseTransitionHandler.Outcome.REJECTED;
        }
        return level.setBlockAndUpdate(position, replacement)
                ? MinecraftPhaseTransitionHandler.Outcome.APPLIED
                : MinecraftPhaseTransitionHandler.Outcome.REJECTED;
    }

    private MinecraftPhaseTransitionHandler.Outcome meltIce(
            BlockPos position,
            BlockState currentState
    ) {
        if (!currentState.is(Blocks.ICE) && !currentState.is(Blocks.FROSTED_ICE)) {
            return MinecraftPhaseTransitionHandler.Outcome.REJECTED;
        }
        return level.setBlockAndUpdate(position, Blocks.WATER.defaultBlockState())
                ? MinecraftPhaseTransitionHandler.Outcome.APPLIED
                : MinecraftPhaseTransitionHandler.Outcome.REJECTED;
    }

    static boolean allowsAutomaticPhaseMutation(
            MaterialBoundaryRegistry.TransitionMutationPolicy policy,
            int randomTickSpeed
    ) {
        Objects.requireNonNull(policy, "policy");
        if (randomTickSpeed < 0) {
            throw new IllegalArgumentException("randomTickSpeed must be non-negative");
        }
        return switch (policy) {
            case IGNORE_RANDOM_TICK_SPEED -> true;
            case RESPECT_RANDOM_TICK_SPEED -> randomTickSpeed > 0;
            case NONE, SCRIPT_CONTROLLED -> false;
        };
    }

    /** Records a completed main-thread transition outcome without applying it. */
    public void advanceTransitionAckWatermark(long watermark) {
        requireMainThread();
        if (watermark < transitionAckWatermark) {
            throw new IllegalArgumentException("transition ACK watermark regressed");
        }
        transitionAckWatermark = watermark;
    }

    @Override
    public void close() {
        requireMainThread();
        if (closed) {
            return;
        }
        closed = true;
        if (physicalSources != null) {
            physicalSources.close();
        }
        if (radiation != null) {
            radiation.close();
        }
        synchronized (ACTIVE_BY_LEVEL) {
            ACTIVE_BY_LEVEL.remove(level, this);
            anyActive = !ACTIVE_BY_LEVEL.isEmpty();
        }
        if (shadowCoordinator != null) {
            pendingShadowFrame.set(null);
            shadowCoordinator.unload(runtime.runtimeId(), dimensionGeneration);
        }
        for (SectionOwner owner : new ArrayList<>(ownersByIdentity.values())) {
            detach(owner);
        }
        pages.clear();
        witnessRefCounts.clear();
        radiationTrackedSections.clear();
        dirtyPages.clear();
    }

    private static synchronized MinecraftThermalInput startGameplayInput(
            ServerLevel level,
            double initialAirTemperatureC
    ) {
        MinecraftThermalInput existing = active(level);
        if (existing != null) {
            return existing;
        }
        DimensionThermalRuntime runtime = null;
        MinecraftThermalInput input = null;
        try {
            prepareGameplayProfiles();
            if (gameplayCoordinator == null) {
                gameplayCoordinator = ThermalRuntimeCoordinator.tryCreate(
                        GAMEPLAY_MEMORY_BUDGET,
                        16,
                        16,
                        2,
                        20L,
                        4);
            }
            if (gameplayCoordinator == null) {
                return null;
            }

            long generation = NEXT_GAMEPLAY_RUNTIME_ID.getAndIncrement();
            long initialTick = level.getGameTime();
            ThermalCellArena arena = new ThermalCellArena(256);
            NodePowerAccumulatorArena accumulators =
                    new NodePowerAccumulatorArena(256);
            ThermalSourceTimeline sources = new ThermalSourceTimeline(
                    generation,
                    initialTick,
                    4_096,
                    new ThermalSourceRegistry(64, 3, 64, accumulators),
                    arena);
            BuoyancyConductance.Parameters buoyancy =
                    new BuoyancyConductance.Parameters(0.25D, 4.0D, 10.0D);
            ThermalSweep sweep = new ThermalSweep(
                    arena, List.of(), List.of(), buoyancy);
            QueryPublication publication = QueryPublication.tryCreate(
                    GAMEPLAY_MEMORY_BUDGET.createDimensionBudget(
                            16L * 1024L * 1024L,
                            1L * 1024L * 1024L),
                    GAMEPLAY_PUBLICATION_CAPACITY);
            if (publication == null) {
                return null;
            }
            runtime = new DimensionThermalRuntime(
                    generation,
                    generation,
                    initialTick,
                    InputWatermarks.ZERO,
                    0L,
                    0L,
                    false,
                    new ThermalTimePolicy(5L, 20L, 2),
                    arena,
                    sources,
                    sweep,
                    publication,
                    initialAirTemperatureC,
                    new DimensionThermalRuntime.Limits(
                            GAMEPLAY_PUBLICATION_CAPACITY,
                            262_144,
                            65_536,
                            20,
                            1.0e-6D));
            input = new MinecraftThermalInput(
                    level,
                    generation,
                    runtime,
                    gameplayDispatcher,
                    gameplaySignatures,
                    gameplayProfileWatermark,
                    16_384,
                    16_384);
            input.enableTopologyApplication(
                    new MinecraftThermalTopologyApplier.Parameters(
                            0,
                            0,
                            64,
                            1_200.0D,
                            initialAirTemperatureC,
                            initialAirTemperatureC,
                            1.0D,
                            0.25D,
                            true,
                            buoyancy,
                            1_024,
                            1_024,
                            8),
                    gameplayMaterialBoundaries);
            input.enablePhysicalSources(64);
            long radiationBytes = RadiationService.projectedMaximumBytes(
                    GAMEPLAY_RADIATION_PARAMETERS);
            if (!input.tryEnableRadiation(
                    GAMEPLAY_RADIATION_PARAMETERS,
                    GAMEPLAY_MEMORY_BUDGET.createDimensionBudget(
                            radiationBytes, 0L))) {
                FHMain.LOGGER.warn(
                        "Thermal radiation memory admission was refused for {}",
                        level.dimension().location());
            }
            input.enableShadowDispatch(gameplayCoordinator, Runnable::run);
            return input;
        } catch (RuntimeException exception) {
            if (input != null) {
                input.close();
            }
            if (runtime != null) {
                runtime.close();
            }
            FHMain.LOGGER.error(
                    "Could not start the thermal gameplay runtime for {}",
                    level.dimension().location(),
                    exception);
            return null;
        }
    }

    public static synchronized void prepareGameplayProfiles() {
        if (gameplaySignatures != null && gameplayDispatcher != null) {
            return;
        }
        List<Block> blocks = new ArrayList<>(ForgeRegistries.BLOCKS.getValues());
        blocks.sort(Comparator.comparing(block ->
                String.valueOf(ForgeRegistries.BLOCKS.getKey(block))));

        StateStaticThermalResolver geometryResolver =
                StateStaticThermalResolver.geometryOnly(64);
        Map<BlockState, StateStaticThermalResolver.SignatureMetadata> metadataByState =
                new IdentityHashMap<>();
        Map<BlockState, Integer> phaseProfileIdsByState = new IdentityHashMap<>();
        Map<GameplayPhaseProfileKey, Integer> profileIds = new LinkedHashMap<>();
        Map<Long, Integer> contactPatternIds = new LinkedHashMap<>();
        List<MaterialBoundaryRegistry.Profile> profiles = new ArrayList<>();
        List<MaterialBoundaryRegistry.ContactPattern> contactPatterns =
                new ArrayList<>();
        int transitionStateCount = 0;
        int skippedWithoutMaterialContact = 0;

        for (Block block : blocks) {
            if (block.hasDynamicShape()) {
                continue;
            }
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                StateTransitionData data = StateTransitionData.getData(state);
                if (data == null || !data.willTransit() || data.heatCapacity() <= 0) {
                    continue;
                }
                StateTransitionData.HeatingTransition transition =
                        data.heatingTransition(state);
                if (transition == null) {
                    continue;
                }
                transitionStateCount++;
                ThermalResolution<ResolvedThermalSignature> geometry =
                        geometryResolver.resolve(state, state.getFluidState());
                if (!geometry.isResolved()) {
                    skippedWithoutMaterialContact++;
                    continue;
                }
                long materialMask = materialMask(geometry.value().orElseThrow());
                if (materialMask == 0L) {
                    skippedWithoutMaterialContact++;
                    continue;
                }

                double transitionEnergyJ = GAMEPLAY_PHASE_BASE_ENERGY_J
                        * data.heatCapacity();
                GameplayPhaseProfileKey profileKey = new GameplayPhaseProfileKey(
                        transition.temperatureC(), transitionEnergyJ);
                Integer profileId = profileIds.get(profileKey);
                if (profileId == null) {
                    profileId = profiles.size() + 1;
                    profileIds.put(profileKey, profileId);
                    profiles.add(MaterialBoundaryRegistry.Profile.phaseReservoir(
                            profileId,
                            GAMEPLAY_PHASE_FACE_CONDUCTANCE_W_PER_K,
                            transition.temperatureC(),
                            transitionEnergyJ,
                            MaterialBoundaryRegistry.TransitionMutationPolicy
                                    .RESPECT_RANDOM_TICK_SPEED,
                            MaterialBoundaryRegistry.TransitionAction
                                    .APPLY_STATE_TRANSITION_RECIPE));
                }
                Integer contactPatternId = contactPatternIds.get(materialMask);
                if (contactPatternId == null) {
                    contactPatternId = contactPatterns.size() + 1;
                    contactPatternIds.put(materialMask, contactPatternId);
                    contactPatterns.add(new MaterialBoundaryRegistry.ContactPattern(
                            contactPatternId, materialMask));
                }
                metadataByState.put(
                        state,
                        new StateStaticThermalResolver.SignatureMetadata(
                                0, profileId, contactPatternId, 0, 0, 0, 0));
                phaseProfileIdsByState.put(state, profileId);
            }
        }

        StateStaticThermalResolver.SignatureMetadata neutral =
                new StateStaticThermalResolver.SignatureMetadata(
                        0, 0, 0, 0, 0, 0, 0);
        StateStaticThermalResolver resolver = new StateStaticThermalResolver(
                64,
                (state, fluid) -> metadataByState.getOrDefault(state, neutral));
        ThermalSignatureRegistry.Builder signatures =
                ThermalSignatureRegistry.builder();
        for (Block block : blocks) {
            if (block.hasDynamicShape()) {
                continue;
            }
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                ThermalResolution<ResolvedThermalSignature> resolution =
                        resolver.resolve(state, state.getFluidState());
                if (resolution.isResolved()) {
                    signatures.intern(resolution.value().orElseThrow());
                }
            }
        }
        gameplayMaterialBoundaries = new MaterialBoundaryRegistry(
                profiles, contactPatterns);
        gameplayPhaseProfileIds = phaseProfileIdsByState;
        gameplaySignatures = signatures.build();
        gameplayDispatcher = ThermalSignatureResolverDispatcher.builder(resolver).build();
        gameplayProfileWatermark = gameplayProfileWatermark == Long.MAX_VALUE
                ? 1L : gameplayProfileWatermark + 1L;
        FHMain.LOGGER.info(
                "Compiled {} hot-side state transition states into {} thermal profiles "
                        + "and {} contact patterns; {} states kept the legacy path "
                        + "because no conservative material contact was available",
                phaseProfileIdsByState.size(),
                profiles.size(),
                contactPatterns.size(),
                skippedWithoutMaterialContact);
        if (transitionStateCount == 0) {
            FHMain.LOGGER.warn(
                    "No enabled StateTransitionData entries were available for thermal profiles");
        }
    }

    /** Invalidates the immutable cut after the recipe cache has been replaced. */
    public static synchronized void invalidateGameplayProfilesForRecipeReload() {
        closeAll();
        gameplaySignatures = null;
        gameplayDispatcher = null;
        gameplayMaterialBoundaries = MaterialBoundaryRegistry.empty();
        gameplayPhaseProfileIds = Map.of();
    }

    private static long materialMask(ResolvedThermalSignature signature) {
        long provenAir = 0L;
        for (var region : signature.airRegions()) {
            provenAir |= region.provenAirMicrocellMask();
        }
        return ~provenAir;
    }

    private record GameplayPhaseProfileKey(
            double transitionTemperatureC,
            double transitionEnergyJPerUnit
    ) {
    }

    /** Single Mixin dispatch point preserving the gated Phase 0a evidence path. */
    public static void onSectionSetBlockState(
            LevelChunkSection section,
            int localX,
            int localY,
            int localZ,
            BlockState oldState,
            BlockState newState
    ) {
        Phase0aMutationProbe.onSectionSetBlockState(
                section, localX, localY, localZ, oldState, newState);
        if (oldState == newState) {
            return;
        }
        SectionOwner owner = attachment(section).frostedheart$getThermalInputOwner();
        if (owner != null) {
            owner.input.recordMutation(
                    owner, localX, localY, localZ, oldState, newState);
        }
    }

    public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        MinecraftThermalInput input = active(level);
        if (input != null) {
            if (input.radiationOcclusion != null) {
                input.radiationOcclusion.onChunkLoad(chunk);
            }
            if (input.physicalSources != null) {
                input.physicalSources.onChunkLoad(chunk);
            }
            input.attachWitnessesInChunk(chunk);
        }
    }

    public static void onChunkUnload(ServerLevel level, LevelChunk chunk) {
        MinecraftThermalInput input = active(level);
        if (input != null) {
            input.detachChunk(chunk);
        }
    }

    public static void sealActiveLevel(ServerLevel level) {
        MinecraftThermalInput input = active(level);
        if (input != null) {
            input.sealTick(level.getGameTime());
        }
    }

    public static void closeAll() {
        List<MinecraftThermalInput> active;
        ThermalRuntimeCoordinator coordinator;
        synchronized (ACTIVE_BY_LEVEL) {
            active = List.copyOf(ACTIVE_BY_LEVEL.values());
            coordinator = gameplayCoordinator;
            gameplayCoordinator = null;
        }
        for (MinecraftThermalInput input : active) {
            input.close();
        }
        if (coordinator != null) {
            coordinator.close();
        }
    }

    public static void onRawBlockContainerReplaced(LevelChunkSection section) {
        SectionOwner owner = attachment(section).frostedheart$getThermalInputOwner();
        if (owner != null) {
            if (owner.input.radiationOcclusion != null) {
                owner.input.radiationOcclusion.onSectionMutation(
                        owner.sectionX, owner.sectionY, owner.sectionZ);
            }
            owner.invalidateAffectedPages(
                    ThermalPage.GeometryResyncReason.EXPLICIT_INVALIDATION);
        }
    }

    public static void onGeneratorTick(
            ServerLevel level,
            BlockPos sourcePosition,
            BlockPos exhaustTarget,
            double thermalLevel,
            boolean active
    ) {
        MinecraftThermalInput input = active(level);
        if (input != null && input.physicalSources != null) {
            input.physicalSources.observeGenerator(
                    sourcePosition, exhaustTarget, thermalLevel, active);
        }
    }

    public static void onGeneratorRemoved(ServerLevel level, BlockPos sourcePosition) {
        MinecraftThermalInput input = active(level);
        if (input != null && input.physicalSources != null) {
            input.physicalSources.removeSource(sourcePosition);
        }
    }

    public static void onPotentialPhysicalSourcePlaced(
            ServerLevel level,
            BlockPos position,
            BlockState replacedState,
            BlockState placedState
    ) {
        MinecraftThermalInput input = active(level);
        if (input != null && input.physicalSources != null) {
            input.physicalSources.onBlockMutation(
                    position, replacedState, placedState);
        }
    }

    public static void onSectionIdentityReplaced(
            ServerLevel level,
            LevelChunk chunk,
            int sectionIndex,
            LevelChunkSection previousSection
    ) {
        MinecraftThermalInput input = active(level);
        if (input == null || sectionIndex < 0
                || sectionIndex >= chunk.getSections().length) {
            return;
        }
        input.requireMainThread();
        int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex);
        if (input.radiationOcclusion != null) {
            input.radiationOcclusion.onSectionIdentityReplaced(
                    chunk.getPos().x, sectionY, chunk.getPos().z);
        }
        SectionOwner previous = input.ownersByIdentity.get(previousSection);
        if (previous != null) {
            previous.invalidateAffectedPages(ThermalPage.GeometryResyncReason.SECTION_REPLACED);
            input.detach(previous);
        }
        input.attachWitnessSection(
                chunk,
                sectionIndex,
                sectionY);
    }

    private void recordMutation(
            SectionOwner owner,
            int localX,
            int localY,
            int localZ,
            BlockState oldState,
            BlockState newState
    ) {
        if (!owner.valid || oldState == newState) {
            return;
        }
        if (radiationOcclusion != null) {
            radiationOcclusion.onSectionMutation(
                    owner.sectionX, owner.sectionY, owner.sectionZ);
        }
        if (Thread.currentThread() != mainThread) {
            owner.invalidateAffectedPages(ThermalPage.GeometryResyncReason.OFF_THREAD_MUTATION);
            offThreadResyncPending.set(true);
            return;
        }
        if (ownersByIdentity.get(owner.section) != owner) {
            return;
        }

        int worldX = SectionPos.sectionToBlockCoord(owner.sectionX) + localX;
        int worldY = SectionPos.sectionToBlockCoord(owner.sectionY) + localY;
        int worldZ = SectionPos.sectionToBlockCoord(owner.sectionZ) + localZ;
        long effectiveTick = level.getGameTime();
        if (physicalSources != null) {
            physicalSources.onBlockMutation(
                    new BlockPos(worldX, worldY, worldZ), oldState, newState);
        }
        LoadedCube cube = new LoadedCube(level, worldX, worldY, worldZ);

        for (int dy = -1; dy <= 1; dy++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int centerX = worldX + dx;
                    int centerY = worldY + dy;
                    int centerZ = worldZ + dz;
                    long pageKey = SectionPos.asLong(
                            SectionPos.blockToSectionCoord(centerX),
                            SectionPos.blockToSectionCoord(centerY),
                            SectionPos.blockToSectionCoord(centerZ));
                    ThermalPage page = pages.get(pageKey);
                    if (page == null) {
                        continue;
                    }

                    ResolverBlockView.SnapshotCell<BlockState, FluidState> self =
                            cube.cell(dx, dy, dz);
                    ThermalSignatureResolution resolution;
                    ThermalSignatureResolverDispatcher.DispatchPlan plan;
                    Optional<ResolverBlockView.StateAndFluid<BlockState, FluidState>> value =
                            self.value();
                    if (value.isEmpty()) {
                        resolution = ThermalSignatureResolution.failure(
                                ThermalResolution.unresolved(self.status()
                                        == ResolverBlockView.LookupStatus.UNLOADED
                                        ? ThermalResolution.Reason.DEPENDENCY_UNLOADED
                                        : ThermalResolution.Reason.SNAPSHOT_DATA_MISSING));
                        plan = null;
                    } else {
                        plan = resolverDispatcher.plan(value.orElseThrow().blockState());
                        if (!plan.dependencyMask().contains(-dx, -dy, -dz)) {
                            continue;
                        }
                        resolution = resolveCenter(cube, dx, dy, dz, plan);
                    }

                    int pageLocalX = SectionPos.sectionRelative(centerX);
                    int pageLocalY = SectionPos.sectionRelative(centerY);
                    int pageLocalZ = SectionPos.sectionRelative(centerZ);
                    ThermalPage.MutationObservation mutation = page.recordGeometryMutation(
                            pageLocalX,
                            pageLocalY,
                            pageLocalZ,
                            effectiveTick,
                            geometryDeltas);
                    if (physicalSources != null) {
                        physicalSources.onPageInvalidated(page.sectionKey());
                    }
                    dirtyPages.put(page, Boolean.TRUE);
                    if (mutation.fullResyncRequired()) {
                        continue;
                    }
                    if (!resolvedInputs.offerResolvedCenter(
                            page.sectionKey(),
                            page.lifecycleGeneration(),
                            mutation.geometryRevision(),
                            effectiveTick,
                            blockIndex(pageLocalX, pageLocalY, pageLocalZ),
                            resolution)) {
                        page.requireFullGeometryResync(
                                ThermalPage.GeometryResyncReason.RING_OVERFLOW);
                        offThreadResyncPending.set(true);
                    }
                }
            }
        }
    }

    private ThermalSignatureResolution resolveCenter(
            LoadedCube cube,
            int centerX,
            int centerY,
            int centerZ,
            ThermalSignatureResolverDispatcher.DispatchPlan plan
    ) {
        ThermalResolution<ResolvedThermalSignature> resolved = resolveSignature(
                cube, centerX, centerY, centerZ, plan);
        if (!resolved.isResolved()) {
            return ThermalSignatureResolution.failure(resolved);
        }
        OptionalInt signatureId = signatureRegistry.idOf(resolved.value().orElseThrow());
        return signatureId.isPresent()
                ? ThermalSignatureResolution.resolved(signatureId.getAsInt())
                : ThermalSignatureResolution.failure(ThermalResolution.unsupported(
                        ThermalResolution.Reason.INVALID_RESOLVER_OUTPUT));
    }

    private static ThermalResolution<ResolvedThermalSignature> resolveSignature(
            LoadedCube cube,
            int centerX,
            int centerY,
            int centerZ,
            ThermalSignatureResolverDispatcher.DispatchPlan plan
    ) {
        Map<DependencyOffsetMask.Offset,
                ResolverBlockView.SnapshotCell<BlockState, FluidState>> cells =
                new LinkedHashMap<>();
        for (DependencyOffsetMask.Offset offset : plan.dependencyMask().offsets()) {
            cells.put(offset, cube.cell(
                    centerX + offset.x(),
                    centerY + offset.y(),
                    centerZ + offset.z()));
        }
        return plan.resolve(
                ResolverBlockView.snapshot(plan.dependencyMask(), cells));
    }

    /** Captures one complete loaded-only Page cut without retaining a World view. */
    private int[] captureFullPageSnapshot(ThermalPage page) {
        return captureFullPageSnapshot(page.sectionKey());
    }

    private int[] captureFullPageSnapshot(long sectionKey) {
        LoadedSectionSnapshot snapshot = new LoadedSectionSnapshot(
                level,
                SectionPos.sectionToBlockCoord(SectionPos.x(sectionKey)),
                SectionPos.sectionToBlockCoord(SectionPos.y(sectionKey)),
                SectionPos.sectionToBlockCoord(SectionPos.z(sectionKey)));
        int[] signatureIds = new int[ResolvedGeometryInputRing.BLOCKS_PER_PAGE];
        for (int localY = 0; localY < 16; localY++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                for (int localX = 0; localX < 16; localX++) {
                    ResolverBlockView.SnapshotCell<BlockState, FluidState> self =
                            snapshot.cell(localX, localY, localZ);
                    ThermalSignatureResolution resolution;
                    Optional<ResolverBlockView.StateAndFluid<BlockState, FluidState>> value =
                            self.value();
                    if (value.isEmpty()) {
                        resolution = ThermalSignatureResolution.failure(
                                ThermalResolution.unresolved(self.status()
                                        == ResolverBlockView.LookupStatus.UNLOADED
                                        ? ThermalResolution.Reason.DEPENDENCY_UNLOADED
                                        : ThermalResolution.Reason.SNAPSHOT_DATA_MISSING));
                    } else {
                        ThermalSignatureResolverDispatcher.DispatchPlan plan =
                                resolverDispatcher.plan(value.orElseThrow().blockState());
                        Map<DependencyOffsetMask.Offset,
                                ResolverBlockView.SnapshotCell<BlockState, FluidState>> cells =
                                new LinkedHashMap<>();
                        for (DependencyOffsetMask.Offset offset
                                : plan.dependencyMask().offsets()) {
                            cells.put(offset, snapshot.cell(
                                    localX + offset.x(),
                                    localY + offset.y(),
                                    localZ + offset.z()));
                        }
                        ThermalResolution<ResolvedThermalSignature> resolved = plan.resolve(
                                ResolverBlockView.snapshot(plan.dependencyMask(), cells));
                        if (!resolved.isResolved()) {
                            resolution = ThermalSignatureResolution.failure(resolved);
                        } else {
                            OptionalInt signatureId = signatureRegistry.idOf(
                                    resolved.value().orElseThrow());
                            resolution = signatureId.isPresent()
                                    ? ThermalSignatureResolution.resolved(
                                            signatureId.getAsInt())
                                    : ThermalSignatureResolution.failure(
                                            ThermalResolution.unsupported(
                                                    ThermalResolution.Reason
                                                            .INVALID_RESOLVER_OUTPUT));
                        }
                    }
                    signatureIds[blockIndex(localX, localY, localZ)] =
                            resolution.status() == ThermalResolution.Status.RESOLVED
                                    ? resolution.signatureId()
                                    : ThermalSignatureResolution.NO_SIGNATURE_ID;
                }
            }
        }
        return signatureIds;
    }

    private void adjustWitnesses(int centerX, int centerY, int centerZ, int delta) {
        for (int y = centerY - WITNESS_RADIUS_SECTIONS;
             y <= centerY + WITNESS_RADIUS_SECTIONS; y++) {
            for (int z = centerZ - WITNESS_RADIUS_SECTIONS;
                 z <= centerZ + WITNESS_RADIUS_SECTIONS; z++) {
                for (int x = centerX - WITNESS_RADIUS_SECTIONS;
                     x <= centerX + WITNESS_RADIUS_SECTIONS; x++) {
                    long key = SectionPos.asLong(x, y, z);
                    int next = witnessRefCounts.getOrDefault(key, 0) + delta;
                    if (next < 0) {
                        throw new IllegalStateException("thermal witness reference underflow");
                    }
                    if (next == 0) {
                        witnessRefCounts.remove(key);
                        SectionOwner owner = ownersBySectionKey.get(key);
                        if (owner != null && !radiationTrackedSections.contains(key)) {
                            detach(owner);
                        }
                    } else {
                        witnessRefCounts.put(key, next);
                        if (next == 1) {
                            attachLoadedWitness(key);
                        }
                    }
                }
            }
        }
    }

    private void attachLoadedWitness(long sectionKey) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(
                SectionPos.x(sectionKey), SectionPos.z(sectionKey));
        if (chunk == null) {
            return;
        }
        int sectionY = SectionPos.y(sectionKey);
        int sectionIndex = chunk.getSectionIndexFromSectionY(sectionY);
        if (sectionIndex < 0 || sectionIndex >= chunk.getSections().length) {
            return;
        }
        attachWitnessSection(chunk, sectionIndex, sectionY);
    }

    private void attachWitnessesInChunk(LevelChunk chunk) {
        requireMainThread();
        for (int sectionIndex = 0; sectionIndex < chunk.getSections().length; sectionIndex++) {
            int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex);
            long key = SectionPos.asLong(chunk.getPos().x, sectionY, chunk.getPos().z);
            if (witnessRefCounts.containsKey(key)
                    || radiationTrackedSections.contains(key)) {
                attachWitnessSection(chunk, sectionIndex, sectionY);
            }
        }
    }

    private void attachWitnessSection(
            LevelChunk chunk,
            int sectionIndex,
            int sectionY
    ) {
        long key = SectionPos.asLong(chunk.getPos().x, sectionY, chunk.getPos().z);
        if (!witnessRefCounts.containsKey(key)
                && !radiationTrackedSections.contains(key)) {
            return;
        }
        LevelChunkSection section = chunk.getSections()[sectionIndex];
        SectionOwner current = ownersBySectionKey.get(key);
        if (current != null && current.section == section && current.valid) {
            return;
        }
        if (current != null) {
            current.invalidateAffectedPages(ThermalPage.GeometryResyncReason.SECTION_REPLACED);
            detach(current);
        }
        SectionOwner owner = new SectionOwner(
                this,
                section,
                chunk.getPos().x,
                sectionY,
                chunk.getPos().z,
                nextSectionGeneration.incrementAndGet());
        ownersBySectionKey.put(key, owner);
        ownersByIdentity.put(section, owner);
        attachment(section).frostedheart$setThermalInputOwner(owner);
        refreshOwnerPageView(owner);
        chunkWatermark = Math.incrementExact(chunkWatermark);
    }

    private void detachChunk(LevelChunk chunk) {
        requireMainThread();
        if (radiationOcclusion != null) {
            radiationOcclusion.onChunkUnload(chunk);
            radiationTrackedSections.removeIf(sectionKey ->
                    SectionPos.x(sectionKey) == chunk.getPos().x
                            && SectionPos.z(sectionKey) == chunk.getPos().z);
        }
        if (physicalSources != null) {
            physicalSources.beforeChunkUnload(chunk, level.getGameTime());
        }
        List<SectionOwner> chunkOwners = new ArrayList<>();
        for (LevelChunkSection section : chunk.getSections()) {
            SectionOwner owner = ownersByIdentity.get(section);
            if (owner != null) {
                owner.invalidateAffectedPages(ThermalPage.GeometryResyncReason.SECTION_REPLACED);
                chunkOwners.add(owner);
            }
        }
        List<Long> unloadedPages = new ArrayList<>();
        for (long sectionKey : pages.keySet()) {
            if (SectionPos.x(sectionKey) == chunk.getPos().x
                    && SectionPos.z(sectionKey) == chunk.getPos().z) {
                unloadedPages.add(sectionKey);
            }
        }
        for (long sectionKey : unloadedPages) {
            withdrawPage(sectionKey);
        }
        for (SectionOwner owner : chunkOwners) {
            if (owner.valid) {
                detach(owner);
            }
        }
    }

    private void detach(SectionOwner owner) {
        owner.valid = false;
        ownersBySectionKey.remove(owner.sectionKey, owner);
        ownersByIdentity.remove(owner.section, owner);
        MinecraftThermalSectionAttachment attachment = attachment(owner.section);
        if (attachment.frostedheart$getThermalInputOwner() == owner) {
            attachment.frostedheart$setThermalInputOwner(null);
        }
        chunkWatermark = Math.incrementExact(chunkWatermark);
    }

    private void refreshNearbyOwnerPageViews(int centerX, int centerY, int centerZ) {
        for (int y = centerY - WITNESS_RADIUS_SECTIONS;
             y <= centerY + WITNESS_RADIUS_SECTIONS; y++) {
            for (int z = centerZ - WITNESS_RADIUS_SECTIONS;
                 z <= centerZ + WITNESS_RADIUS_SECTIONS; z++) {
                for (int x = centerX - WITNESS_RADIUS_SECTIONS;
                     x <= centerX + WITNESS_RADIUS_SECTIONS; x++) {
                    SectionOwner owner = ownersBySectionKey.get(SectionPos.asLong(x, y, z));
                    if (owner != null) {
                        refreshOwnerPageView(owner);
                    }
                }
            }
        }
    }

    private void refreshOwnerPageView(SectionOwner owner) {
        List<ThermalPage> affected = new ArrayList<>(27);
        for (int y = owner.sectionY - 1; y <= owner.sectionY + 1; y++) {
            for (int z = owner.sectionZ - 1; z <= owner.sectionZ + 1; z++) {
                for (int x = owner.sectionX - 1; x <= owner.sectionX + 1; x++) {
                    ThermalPage page = pages.get(SectionPos.asLong(x, y, z));
                    if (page != null) {
                        affected.add(page);
                    }
                }
            }
        }
        owner.affectedPages = affected.toArray(ThermalPage[]::new);
    }

    private static MinecraftThermalInput active(ServerLevel level) {
        if (!anyActive) {
            return null;
        }
        synchronized (ACTIVE_BY_LEVEL) {
            return ACTIVE_BY_LEVEL.get(level);
        }
    }

    private static MinecraftThermalSectionAttachment attachment(LevelChunkSection section) {
        return (MinecraftThermalSectionAttachment) section;
    }

    private static int blockIndex(int localX, int localY, int localZ) {
        return (localY << 8) | (localZ << 4) | localX;
    }

    private static int microcellIndex(
            double worldX,
            double worldY,
            double worldZ,
            int blockX,
            int blockY,
            int blockZ
    ) {
        int microX = Math.min(3, (int) ((worldX - blockX) * 4.0D));
        int microY = Math.min(3, (int) ((worldY - blockY) * 4.0D));
        int microZ = Math.min(3, (int) ((worldZ - blockZ) * 4.0D));
        return (microY << 4) | (microZ << 2) | microX;
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static void requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private void recordPublishedAirOutcome(MutableEnvironmentSample sample) {
        if (sample.airAvailable()) {
            publishedAirHits = saturatingIncrement(publishedAirHits);
        } else {
            publishedAirMisses = saturatingIncrement(publishedAirMisses);
        }
        int flags = sample.flags();
        if ((flags & QUERY_NO_PAGE) != 0) {
            noPageLookups = saturatingIncrement(noPageLookups);
        }
        if ((flags & QUERY_NO_AIR_COMPONENT) != 0) {
            noAirComponentLookups = saturatingIncrement(noAirComponentLookups);
        }
        if ((flags & QUERY_STALE_GEOMETRY) != 0) {
            staleGeometryLookups = saturatingIncrement(staleGeometryLookups);
        }
        if ((flags & QUERY_PUBLICATION_MISS) != 0) {
            publicationMissLookups = saturatingIncrement(publicationMissLookups);
        }
        if ((flags & QUERY_PUBLICATION_STALE) != 0) {
            stalePublicationLookups = saturatingIncrement(stalePublicationLookups);
        }
        if ((flags & QUERY_DEGRADED_TOPOLOGY) != 0) {
            degradedTopologyLookups = saturatingIncrement(degradedTopologyLookups);
        }
    }

    private static long saturatingIncrement(long value) {
        return value == Long.MAX_VALUE ? value : value + 1L;
    }

    private static long saturatingAdd(long value, long increment) {
        if (increment <= 0L) {
            return value;
        }
        return value > Long.MAX_VALUE - increment
                ? Long.MAX_VALUE
                : value + increment;
    }

    private void scheduleShadowWorker() {
        if (closed || !shadowWorkerScheduled.compareAndSet(false, true)) {
            return;
        }
        try {
            shadowExecutor.execute(this::drainShadowFrames);
        } catch (RuntimeException exception) {
            shadowWorkerScheduled.set(false);
            shadowExecutorRejectedSubmissions = saturatingIncrement(
                    shadowExecutorRejectedSubmissions);
            SealedInputFrame rejected = pendingShadowFrame.get();
            latestShadowReport = new ShadowReport(
                    rejected == null ? lastSealedTick : rejected.effectiveTick(),
                    null,
                    null,
                    null,
                    0L,
                    true);
        }
    }

    private void drainShadowFrames() {
        try {
            while (!closed) {
                SealedInputFrame frame = pendingShadowFrame.getAndSet(null);
                if (frame == null) {
                    return;
                }
                long startedNanos = System.nanoTime();
                MinecraftThermalTopologyApplier.ApplyReport topology =
                        topologyApplier.apply(frame);
                ThermalRuntimeCoordinator.RequestResult request = null;
                DimensionThermalRuntime.RunReport solve = null;
                if (topology.readyForSolve()) {
                    request = shadowCoordinator.request(
                            runtime.runtimeId(),
                            dimensionGeneration,
                            false,
                            frame.effectiveTick());
                    ThermalRuntimeCoordinator.DispatchResult dispatch;
                    while ((dispatch = shadowCoordinator.runNext(frame.effectiveTick())).status()
                            == ThermalRuntimeCoordinator.DispatchStatus.EXECUTED) {
                        // Drain the bounded coordinator on its shared worker.
                        if (dispatch.runtimeId() == runtime.runtimeId()
                                && dispatch.dimensionGeneration() == dimensionGeneration) {
                            solve = dispatch.runReport();
                        }
                    }
                }
                long elapsedNanos = Math.max(0L, System.nanoTime() - startedNanos);
                shadowWorkerTotalNanos = saturatingAdd(
                        shadowWorkerTotalNanos, elapsedNanos);
                shadowWorkerMaximumNanos = Math.max(
                        shadowWorkerMaximumNanos, elapsedNanos);
                // Publish the frame count last so readers that observe it also
                // observe the corresponding cumulative timing writes.
                shadowWorkerFrames = saturatingIncrement(shadowWorkerFrames);
                latestShadowReport = new ShadowReport(
                        frame.effectiveTick(), topology, request, solve,
                        elapsedNanos, false);
            }
        } finally {
            shadowWorkerScheduled.set(false);
            if (!closed && pendingShadowFrame.get() != null) {
                scheduleShadowWorker();
            }
        }
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Minecraft thermal input is closed");
        }
    }

    private void requireMainThread() {
        if (Thread.currentThread() != mainThread || !level.getServer().isSameThread()) {
            throw new IllegalStateException("Minecraft thermal input is main-thread confined");
        }
    }

    /** Caller-owned primitive result for Phase K environment query hot paths. */
    public static final class MutableEnvironmentSample {
        private boolean airAvailable;
        private double airTemperatureC = Double.NaN;
        private double radiantFluxWPerM2;
        private double surfaceFluxW;
        private int mediumId = -1;
        private float confidence;
        private long sampleTick = -1L;
        private int cellFlags;
        private int flags;

        public boolean valid() {
            return airAvailable;
        }

        public boolean airAvailable() {
            return airAvailable;
        }

        public double airTemperatureC() {
            return airTemperatureC;
        }

        public double radiantFluxWPerM2() {
            return radiantFluxWPerM2;
        }

        public double surfaceFluxW() {
            return surfaceFluxW;
        }

        public int mediumId() {
            return mediumId;
        }

        public float confidence() {
            return confidence;
        }

        public long sampleTick() {
            return sampleTick;
        }

        public int cellFlags() {
            return cellFlags;
        }

        public int flags() {
            return flags;
        }

        private void clear() {
            airAvailable = false;
            airTemperatureC = Double.NaN;
            radiantFluxWPerM2 = 0.0D;
            surfaceFluxW = 0.0D;
            mediumId = -1;
            confidence = 0.0F;
            sampleTick = -1L;
            cellFlags = 0;
            flags = QUERY_SURFACE_UNAVAILABLE;
        }

        private void setAir(
                double nextAirTemperatureC,
                int nextMediumId,
                int nextCellFlags,
                long nextSampleTick
        ) {
            airAvailable = true;
            airTemperatureC = nextAirTemperatureC;
            mediumId = nextMediumId;
            cellFlags = nextCellFlags;
            sampleTick = nextSampleTick;
            confidence = 1.0F;
        }

        private void setAggregateAir(
                double nextAirTemperatureC,
                int nextMediumId,
                int nextCellFlags,
                long nextSampleTick,
                float nextConfidence,
                int nextFlags
        ) {
            airAvailable = true;
            airTemperatureC = nextAirTemperatureC;
            mediumId = nextMediumId;
            cellFlags = nextCellFlags;
            sampleTick = nextSampleTick;
            confidence = nextConfidence;
            flags |= nextFlags;
        }

        private void setRadiation(double nextFluxWPerM2, float nextConfidence) {
            radiantFluxWPerM2 = nextFluxWPerM2;
            confidence = airAvailable
                    ? Math.min(confidence, nextConfidence)
                    : nextConfidence;
        }

        private void addFlag(int flag) {
            flags |= flag;
        }

        private void addFlags(int nextFlags) {
            flags |= nextFlags;
        }
    }

    public record PlayerShadowSnapshot(
            long queryCalls,
            long queryHits,
            long queryMisses,
            long comparisons,
            double meanAbsoluteErrorC,
            double maximumAbsoluteErrorC,
            double latestLegacyAirTemperatureC,
            double latestShadowAirTemperatureC,
            long latestSampleTick,
            int latestFlags
    ) {
    }

    public record MachineShadowSnapshot(
            long queryCalls,
            long queryHits,
            long queryMisses,
            long comparisons,
            double meanAbsoluteErrorC,
            double maximumAbsoluteErrorC,
            double latestLegacyAirTemperatureC,
            double latestShadowAirTemperatureC,
            long latestSampleTick,
            int latestFlags
    ) {
    }

    public record CropShadowSnapshot(
            long queryCalls,
            long queryHits,
            long queryMisses,
            long comparisons,
            double meanAbsoluteErrorC,
            double maximumAbsoluteErrorC,
            double latestLegacyBlockTemperatureC,
            double latestShadowAirTemperatureC,
            long latestSampleTick,
            int latestFlags
    ) {
    }

    public record TownShadowSnapshot(
            long queryCalls,
            long groupLookups,
            long queryHits,
            long queryMisses,
            long partialQueries,
            long comparisons,
            double meanAbsoluteErrorC,
            double maximumAbsoluteErrorC,
            double latestLegacyAverageTemperatureC,
            double latestShadowAverageTemperatureC,
            long latestSampleTick,
            int latestGroupCount,
            int latestVoxelCount,
            int latestFlags
    ) {
    }

    public record ShadowRuntimeSnapshot(
            long capturedTick,
            int admittedPageCount,
            long mixedBrickCount,
            int witnessedSectionCount,
            int physicalSourceCount,
            int radiationSourceCount,
            long publishedAirLookups,
            long publishedAirHits,
            long publishedAirMisses,
            long noPageLookups,
            long noAirComponentLookups,
            long staleGeometryLookups,
            long publicationMissLookups,
            long stalePublicationLookups,
            long degradedTopologyLookups,
            long publicationAgeSamples,
            double meanPublicationAgeTicks,
            long maximumPublicationAgeTicks,
            long sealCalls,
            long sealTotalNanos,
            long sealMaximumNanos,
            long workerFrames,
            long workerTotalNanos,
            long workerMaximumNanos,
            long executorRejectedSubmissions,
            int coordinatorReadyCount,
            String coordinatorMailboxState,
            DimensionThermalRuntime.Diagnostics runtime,
            PlayerShadowSnapshot player,
            MachineShadowSnapshot machine,
            CropShadowSnapshot crop,
            TownShadowSnapshot town,
            ShadowReport latestDispatch
    ) {
    }

    public record SealReport(
            SealedInputFrame frame,
            LatestSolveEpochScheduler.SealResult runtimeResult,
            int sealedGeometryDeltas,
            int fullResyncPages
    ) {
    }

    public record ShadowReport(
            long effectiveTick,
            MinecraftThermalTopologyApplier.ApplyReport topology,
            ThermalRuntimeCoordinator.RequestResult request,
            DimensionThermalRuntime.RunReport solve,
            long workerNanos,
            boolean executorRejected
    ) {
    }

    /** Attached owner is a concrete route, not a generic resolver callback. */
    public static final class SectionOwner {
        private final MinecraftThermalInput input;
        private final LevelChunkSection section;
        private final int sectionX;
        private final int sectionY;
        private final int sectionZ;
        private final long sectionKey;
        private final long lifecycleGeneration;
        private volatile ThermalPage[] affectedPages = new ThermalPage[0];
        private volatile boolean valid = true;

        private SectionOwner(
                MinecraftThermalInput input,
                LevelChunkSection section,
                int sectionX,
                int sectionY,
                int sectionZ,
                long lifecycleGeneration
        ) {
            this.input = input;
            this.section = section;
            this.sectionX = sectionX;
            this.sectionY = sectionY;
            this.sectionZ = sectionZ;
            this.sectionKey = SectionPos.asLong(sectionX, sectionY, sectionZ);
            this.lifecycleGeneration = lifecycleGeneration;
        }

        public long sectionKey() {
            return sectionKey;
        }

        public long lifecycleGeneration() {
            return lifecycleGeneration;
        }

        public boolean valid() {
            return valid;
        }

        private void invalidateAffectedPages(ThermalPage.GeometryResyncReason reason) {
            for (ThermalPage page : affectedPages) {
                page.requireFullGeometryResync(reason);
                if (Thread.currentThread() == input.mainThread) {
                    input.dirtyPages.put(page, Boolean.TRUE);
                }
            }
            if (Thread.currentThread() != input.mainThread) {
                input.offThreadResyncPending.set(true);
            }
        }
    }

    /** Lazy 5-cubed loaded-only union; each world position is read at most once. */
    private static final class LoadedCube {
        private static final int RADIUS = 2;
        private static final int WIDTH = 5;
        private final ServerLevel level;
        private final int originX;
        private final int originY;
        private final int originZ;
        private final ResolverBlockView.SnapshotCell<BlockState, FluidState>[] cells;

        @SuppressWarnings("unchecked")
        private LoadedCube(ServerLevel level, int originX, int originY, int originZ) {
            this.level = level;
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
            this.cells = new ResolverBlockView.SnapshotCell[WIDTH * WIDTH * WIDTH];
        }

        private ResolverBlockView.SnapshotCell<BlockState, FluidState> cell(
                int relativeX,
                int relativeY,
                int relativeZ
        ) {
            if (Math.abs(relativeX) > RADIUS
                    || Math.abs(relativeY) > RADIUS
                    || Math.abs(relativeZ) > RADIUS) {
                return ResolverBlockView.SnapshotCell.missing();
            }
            int index = ((relativeY + RADIUS) * WIDTH + relativeZ + RADIUS)
                    * WIDTH + relativeX + RADIUS;
            ResolverBlockView.SnapshotCell<BlockState, FluidState> cached = cells[index];
            if (cached != null) {
                return cached;
            }
            BlockPos position = new BlockPos(
                    originX + relativeX,
                    originY + relativeY,
                    originZ + relativeZ);
            ResolverBlockView.SnapshotCell<BlockState, FluidState> captured;
            if (level.isOutsideBuildHeight(position)) {
                captured = ResolverBlockView.SnapshotCell.missing();
            } else {
                LevelChunk chunk = level.getChunkSource().getChunkNow(
                        SectionPos.blockToSectionCoord(position.getX()),
                        SectionPos.blockToSectionCoord(position.getZ()));
                if (chunk == null) {
                    captured = ResolverBlockView.SnapshotCell.unloaded();
                } else {
                    BlockState state = chunk.getBlockState(position);
                    captured = ResolverBlockView.SnapshotCell.present(
                            state, state.getFluidState());
                }
            }
            cells[index] = captured;
            return captured;
        }
    }

    /** Lazy section plus one-block dependency halo used only during resnapshot. */
    private static final class LoadedSectionSnapshot {
        private static final int MIN_LOCAL = -1;
        private static final int MAX_LOCAL = 16;
        private static final int WIDTH = 18;
        private final ServerLevel level;
        private final int sectionMinX;
        private final int sectionMinY;
        private final int sectionMinZ;
        private final ResolverBlockView.SnapshotCell<BlockState, FluidState>[] cells;

        @SuppressWarnings("unchecked")
        private LoadedSectionSnapshot(
                ServerLevel level,
                int sectionMinX,
                int sectionMinY,
                int sectionMinZ
        ) {
            this.level = level;
            this.sectionMinX = sectionMinX;
            this.sectionMinY = sectionMinY;
            this.sectionMinZ = sectionMinZ;
            this.cells = new ResolverBlockView.SnapshotCell[WIDTH * WIDTH * WIDTH];
        }

        private ResolverBlockView.SnapshotCell<BlockState, FluidState> cell(
                int localX,
                int localY,
                int localZ
        ) {
            if (localX < MIN_LOCAL || localX > MAX_LOCAL
                    || localY < MIN_LOCAL || localY > MAX_LOCAL
                    || localZ < MIN_LOCAL || localZ > MAX_LOCAL) {
                return ResolverBlockView.SnapshotCell.missing();
            }
            int index = ((localY - MIN_LOCAL) * WIDTH + localZ - MIN_LOCAL)
                    * WIDTH + localX - MIN_LOCAL;
            ResolverBlockView.SnapshotCell<BlockState, FluidState> cached = cells[index];
            if (cached != null) {
                return cached;
            }
            BlockPos position = new BlockPos(
                    sectionMinX + localX,
                    sectionMinY + localY,
                    sectionMinZ + localZ);
            ResolverBlockView.SnapshotCell<BlockState, FluidState> captured;
            if (level.isOutsideBuildHeight(position)) {
                captured = ResolverBlockView.SnapshotCell.missing();
            } else {
                LevelChunk chunk = level.getChunkSource().getChunkNow(
                        SectionPos.blockToSectionCoord(position.getX()),
                        SectionPos.blockToSectionCoord(position.getZ()));
                if (chunk == null) {
                    captured = ResolverBlockView.SnapshotCell.unloaded();
                } else {
                    BlockState state = chunk.getBlockState(position);
                    captured = ResolverBlockView.SnapshotCell.present(
                            state, state.getFluidState());
                }
            }
            cells[index] = captured;
            return captured;
        }
    }
}
