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
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.data.StateTransitionData;
import com.teammoeg.frostedheart.content.climate.thermal.consumer.TownThermalProjection;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.ConservativeAirGeometry;
import com.teammoeg.frostedheart.content.climate.thermal.geometry.GeometryDeltaRing;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.FarFieldProfileRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPage;
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
import com.teammoeg.frostedheart.content.climate.thermal.solver.BuoyancyConductance;
import com.teammoeg.frostedheart.content.climate.thermal.solver.InputWatermarks;
import com.teammoeg.frostedheart.content.climate.thermal.solver.LatestSolveEpochScheduler;
import com.teammoeg.frostedheart.content.climate.thermal.solver.PhaseTransitionRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.solver.SealedInputFrame;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSweep;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSweepFragments;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalTimePolicy;
import com.teammoeg.frostedheart.content.climate.thermal.source.NodePowerAccumulatorArena;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalSourceTimeline;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.MinecraftPhysicalSourceProfile;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Main-thread Minecraft input owner for one dimension runtime.
 *
 * <pre>
 * section mutation Mixin
 *   owner lookup -> page invalidation -> loaded-only resolve -> primitive event
 * tick end
 *   seal page deltas -> seal five stream watermarks
 *   -> synchronous topology commit -> dimension runtime
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
    public static final int QUERY_PARTIAL_REGION = 1 << 10;
    public static final int QUERY_ANALYTIC_FIELD_APPLIED = 1 << 11;
    private static final int MAX_PUBLICATION_AGE_TICKS = 40;
    private static final long GAMEPLAY_SOLVE_INTERVAL_TICKS = 5L;
    private static final long GEOMETRY_REBUILD_DELAY_TICKS = 5L;
    private static final int DEFERRED_BLOCK_WORD_COUNT =
            ResolvedGeometryInputRing.BLOCKS_PER_PAGE / Long.SIZE;
    private static final int GAMEPLAY_INITIAL_PUBLICATION_CAPACITY = 256;
    private static final int GAMEPLAY_MAX_ACTIVE_CELLS = 65_536;
    private static final double GAMEPLAY_PHASE_FACE_CONDUCTANCE_W_PER_K = 20.0D;
    private static final double GAMEPLAY_PHASE_BASE_ENERGY_J = 38_000.0D;
    private static final double GAMEPLAY_FAR_FIELD_REFERENCE_AREA_BLOCKS_SQUARED = 32.0D;
    private static final double GAMEPLAY_FAR_FIELD_CALIBRATION_POWER_W = 1_000_000.0D;
    private static final double GAMEPLAY_CONTINUATION_DISTANCE_BLOCKS = 16.0D;
    private static final double GAMEPLAY_FAR_FIELD_WIND_GAIN = 0.8D;
    private static final int MAXIMUM_LOADED_CONTINUATION_PAGES = 64;
    private static final double FAR_FIELD_WIND_REBUILD_DELTA = 0.05D;
    private static final long NATURAL_TEMPERATURE_REFRESH_TICKS = 200L;
    private static final int MAXIMUM_NATURAL_TEMPERATURE_REFRESHES_PER_TICK = 16;
    private static final int MAXIMUM_SKY_COLUMN_REFRESHES_PER_TICK = 64;
    private static final double NATURAL_TEMPERATURE_REBUILD_DELTA_C = 0.25D;
    private static final FarFieldProfileRegistry GAMEPLAY_FAR_FIELDS =
            createGameplayFarFields();
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
    private static final int WITNESS_RADIUS_SECTIONS = 1;
    private static final int[][] PHASE_NEIGHBOR_OFFSETS = {
            {-1, 0, 0}, {1, 0, 0},
            {0, -1, 0}, {0, 1, 0},
            {0, 0, -1}, {0, 0, 1}
    };

    enum GameplayMaterial {
        INSULATING_FABRIC(0.12D, 120.0D),
        WOOD(0.45D, 450.0D),
        EARTH(1.0D, 1_100.0D),
        MASONRY(1.4D, 900.0D),
        GLASS(0.8D, 250.0D),
        METAL(6.0D, 700.0D),
        GENERIC_SOLID(1.0D, 650.0D);

        private final double faceConductanceWPerK;
        private final double surfaceCapacityJPerK;

        GameplayMaterial(
                double faceConductanceWPerK,
                double surfaceCapacityJPerK
        ) {
            this.faceConductanceWPerK = faceConductanceWPerK;
            this.surfaceCapacityJPerK = surfaceCapacityJPerK;
        }

        private int profileId() {
            return ordinal() + 1;
        }

        private MaterialBoundaryRegistry.Profile profile() {
            return MaterialBoundaryRegistry.Profile
                    .capacitiveSurfaceAtNaturalTemperature(
                            profileId(),
                            faceConductanceWPerK,
                            surfaceCapacityJPerK);
        }
    }

    public enum AnalyticCombineMode {
        OVERRIDE,
        MAX_HEAT,
        MIN_COOL,
        ADD_DELTA
    }

    public enum AnalyticShape {
        CUBE(0.0F),
        PILLAR(1.0F),
        SPHERE(2.0F);

        private final float infraredMode;

        AnalyticShape(float infraredMode) {
            this.infraredMode = infraredMode;
        }
    }

    /** One non-conservative control field definition; never copied per chunk. */
    public record AnalyticField(
            long fieldId,
            int priority,
            AnalyticCombineMode combineMode,
            AnalyticShape shape,
            double centerX,
            double centerY,
            double centerZ,
            double radius,
            double upperExtent,
            double lowerExtent,
            double temperatureC
    ) {
        public AnalyticField(
                long fieldId,
                int priority,
                AnalyticCombineMode combineMode,
                double centerX,
                double centerY,
                double centerZ,
                double radius,
                double temperatureC
        ) {
            this(fieldId, priority, combineMode, AnalyticShape.SPHERE,
                    centerX, centerY, centerZ, radius, radius, radius,
                    temperatureC);
        }

        public AnalyticField {
            Objects.requireNonNull(combineMode, "combineMode");
            Objects.requireNonNull(shape, "shape");
            requireFinite("centerX", centerX);
            requireFinite("centerY", centerY);
            requireFinite("centerZ", centerZ);
            requireFinite("temperatureC", temperatureC);
            if (!Double.isFinite(radius) || radius <= 0.0D) {
                throw new IllegalArgumentException("radius must be finite and positive");
            }
            if (!Double.isFinite(upperExtent) || upperExtent < 0.0D
                    || !Double.isFinite(lowerExtent) || lowerExtent < 0.0D) {
                throw new IllegalArgumentException(
                        "vertical extents must be finite and non-negative");
            }
        }

        private boolean contains(double x, double y, double z) {
            double dx = x - centerX;
            double dy = y - centerY;
            double dz = z - centerZ;
            return switch (shape) {
                case CUBE -> Math.abs(dx) <= radius
                        && Math.abs(dy) <= radius
                        && Math.abs(dz) <= radius;
                case PILLAR -> dy <= upperExtent && dy >= -lowerExtent
                        && dx * dx + dz * dz <= radius * radius;
                case SPHERE -> dx * dx + dy * dy + dz * dz <= radius * radius;
            };
        }

        private boolean intersectsHorizontalBounds(
                double minimumX,
                double maximumX,
                double minimumZ,
                double maximumZ
        ) {
            return centerX + radius >= minimumX
                    && centerX - radius <= maximumX
                    && centerZ + radius >= minimumZ
                    && centerZ - radius <= maximumZ;
        }

        private void writeInfrared(float[] output, int offset) {
            output[offset] = (float) centerX;
            output[offset + 1] = (float) centerY;
            output[offset + 2] = (float) centerZ;
            output[offset + 3] = shape.infraredMode;
            output[offset + 4] = (float) temperatureC;
            output[offset + 5] = (float) radius;
            output[offset + 6] = shape == AnalyticShape.PILLAR
                    ? (float) (centerY + upperExtent) : 0.0F;
            output[offset + 7] = shape == AnalyticShape.PILLAR
                    ? (float) (centerY - lowerExtent) : 0.0F;
        }
    }

    private static final Comparator<AnalyticField> ANALYTIC_FIELD_ORDER =
            Comparator.comparingInt((AnalyticField field) ->
                            field.combineMode().ordinal())
                    .thenComparingInt(AnalyticField::priority)
                    .thenComparingLong(AnalyticField::fieldId);
    static final Comparator<PageEnvironmentRefresh> PAGE_REFRESH_ORDER =
            Comparator.comparingLong((PageEnvironmentRefresh refresh) ->
                            refresh.dueTick)
                    .thenComparingLong(refresh -> refresh.sectionKey)
                    .thenComparingLong(refresh -> refresh.lifecycleGeneration);

    private static FarFieldProfileRegistry createGameplayFarFields() {
        List<FarFieldProfileRegistry.Profile> profiles = new ArrayList<>(
                FarFieldProfileRegistry.EnvironmentClass.values().length);
        for (FarFieldProfileRegistry.EnvironmentClass environment
                : FarFieldProfileRegistry.EnvironmentClass.values()) {
            profiles.add(new FarFieldProfileRegistry.Profile(
                    environment,
                    7_747.2298793470545D,
                    new FarFieldProfileRegistry.ApplicabilityDomain(
                            GAMEPLAY_FAR_FIELD_CALIBRATION_POWER_W,
                            129.16666666663735D)));
        }
        return new FarFieldProfileRegistry(profiles);
    }

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
    private final List<AnalyticField> analyticFields = new ArrayList<>();
    private final Map<Long, Integer> physicalSourcePageRefCounts = new HashMap<>();
    private final Set<Long> physicalSourceOwnedPages = new HashSet<>();
    private final Set<Long> continuationOwnedPages = new HashSet<>();
    private final Map<Long, Integer> witnessRefCounts = new HashMap<>();
    private final Set<Long> radiationTrackedSections = new HashSet<>();
    private final Map<Long, SectionOwner> ownersBySectionKey = new HashMap<>();
    private final IdentityHashMap<LevelChunkSection, SectionOwner> ownersByIdentity =
            new IdentityHashMap<>();
    private final Set<SectionOwner> deferredDirtyOwners =
            ConcurrentHashMap.newKeySet();
    private final Set<Long> deferredInvalidatedSourceSections =
            ConcurrentHashMap.newKeySet();
    private final IdentityHashMap<ThermalPage, Boolean> dirtyPages = new IdentityHashMap<>();
    private final LongOpenHashSet dirtySkyColumns = new LongOpenHashSet();
    private final long[] dirtySkyColumnRefreshScratch =
            new long[MAXIMUM_SKY_COLUMN_REFRESHES_PER_TICK];
    private final PriorityQueue<PageEnvironmentRefresh> pageEnvironmentRefreshes =
            new PriorityQueue<>(PAGE_REFRESH_ORDER);
    private final PageEnvironmentRefresh[] duePageEnvironmentRefreshScratch =
            new PageEnvironmentRefresh[MAXIMUM_NATURAL_TEMPERATURE_REFRESHES_PER_TICK];
    private final AtomicBoolean fullResyncRetryPending = new AtomicBoolean();
    private final AtomicBoolean urgentSolvePending = new AtomicBoolean(true);
    private final AtomicLong geometryRebuildDeadlineTick =
            new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong deferredBlockMutationRevision = new AtomicLong();
    private final AtomicLong nextSectionGeneration = new AtomicLong();
    private final ThermalPage.MutableCoverageQuery environmentCoverageScratch =
            new ThermalPage.MutableCoverageQuery();
    private final QueryPublication.MutableSample environmentPublicationScratch =
            new QueryPublication.MutableSample();
    private final RadiationService.MutableSample playerRadiationScratch =
            new RadiationService.MutableSample();
    private final MutableEnvironmentSample cropEnvironmentScratch =
            new MutableEnvironmentSample();
    private final MutableEnvironmentSample townEnvironmentScratch =
            new MutableEnvironmentSample();
    private final MutableEnvironmentSample townGroupEnvironmentScratch =
            new MutableEnvironmentSample();
    private final BlockPos.MutableBlockPos townNaturalPositionScratch =
            new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos naturalTemperaturePositionScratch =
            new BlockPos.MutableBlockPos();

    private long chunkWatermark;
    private final long profileWatermark;
    private long transitionAckWatermark;
    private long lastSealedTick;
    private long releasedGeometryWatermark;
    private long nextFarFieldWindRefreshTick;
    private long drainedDeferredBlockMutationRevision;
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
    private boolean dispatchEnabled;

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
        releasedGeometryWatermark = runtime.appliedWatermarks().geometry();
        nextFarFieldWindRefreshTick = lastSealedTick;
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
        continuationOwnedPages.remove(sectionKey);
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
        requestUrgentSolve();
        return true;
    }

    private LatestSolveEpochScheduler.SealResult sealTick(
            long effectiveTick,
            boolean urgent,
            boolean mainThreadInputsPrepared,
            boolean releaseGeometry
    ) {
        requireMainThread();
        requireOpen();
        if (effectiveTick < lastSealedTick) {
            throw new IllegalArgumentException("thermal input ticks must be monotonic");
        }
        if (!mainThreadInputsPrepared) {
            refreshNaturalTemperatures(effectiveTick);
            processPhaseTransitions();
        }
        if (releaseGeometry) {
            drainDeferredBlockMutations(effectiveTick);
        }
        if (physicalSources != null) {
            drainDeferredSourceInvalidations();
            drainCommittedSourceBindingSections();
            physicalSources.flush(effectiveTick);
        }
        if (releaseGeometry && fullResyncRetryPending.getAndSet(false)) {
            for (ThermalPage page : pages.values()) {
                if (page.fullGeometryResyncRequired()) {
                    dirtyPages.put(page, Boolean.TRUE);
                }
            }
        }

        if (releaseGeometry) {
            for (ThermalPage page : List.copyOf(dirtyPages.keySet())) {
                page.sealGeometryDeltas(geometryDeltas);
                if (page.fullGeometryResyncRequired()) {
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
                            fullResyncRetryPending.set(true);
                        }
                    }
                }
            }
            dirtyPages.clear();
        }

        InputWatermarks watermarks = new InputWatermarks(
                releaseGeometry
                        ? resolvedInputs.latestOfferedWatermark()
                        : releasedGeometryWatermark,
                runtime.latestOfferedSourceWatermark(),
                chunkWatermark,
                profileWatermark,
                transitionAckWatermark
        );
        SealedInputFrame frame = new SealedInputFrame(
                effectiveTick, dimensionGeneration, watermarks);
        LatestSolveEpochScheduler.SealResult result = runtime.sealFrame(frame, urgent);
        if (result == LatestSolveEpochScheduler.SealResult.ACCEPTED
                || result == LatestSolveEpochScheduler.SealResult.DUPLICATE) {
            lastSealedTick = effectiveTick;
            if (releaseGeometry) {
                releasedGeometryWatermark = watermarks.geometry();
            }
        }
        if (dispatchEnabled
                && (result == LatestSolveEpochScheduler.SealResult.ACCEPTED
                || result == LatestSolveEpochScheduler.SealResult.DUPLICATE)) {
            submitDispatch(frame);
        }
        return result;
    }

    private void sealGameplayTick(long effectiveTick) {
        requireMainThread();
        requireOpen();
        if (refreshDirtySkyExposureColumns()) {
            requestUrgentSolve();
        }
        if (refreshNaturalTemperatures(effectiveTick)) {
            requestUrgentSolve();
        }
        if (processPhaseTransitions()) {
            requestUrgentSolve();
        }
        if (drainCommittedSourceBindingSections()) {
            requestUrgentSolve();
        }
        if (drainDeferredSourceInvalidations()) {
            requestUrgentSolve();
        }

        long geometryDeadline = geometryRebuildDeadlineTick.get();
        boolean geometryDue = effectiveTick >= geometryDeadline;
        boolean urgent = urgentSolvePending.getAndSet(false) || geometryDue;
        if (!urgent && (runtime.sleeping()
                || effectiveTick - lastSealedTick < GAMEPLAY_SOLVE_INTERVAL_TICKS)) {
            return;
        }
        LatestSolveEpochScheduler.SealResult result =
                sealTick(effectiveTick, urgent, true, geometryDue);
        boolean accepted = result == LatestSolveEpochScheduler.SealResult.ACCEPTED
                || result == LatestSolveEpochScheduler.SealResult.DUPLICATE;
        if (geometryDue && accepted) {
            if (geometryRebuildDeadlineTick.compareAndSet(
                    geometryDeadline, Long.MAX_VALUE)) {
                reschedulePendingGeometry(effectiveTick);
            }
        }
        if (!accepted && urgent) {
            urgentSolvePending.set(true);
        }
    }

    private void scheduleGeometryRebuild(long effectiveTick) {
        long deadline = Math.addExact(effectiveTick, GEOMETRY_REBUILD_DELAY_TICKS);
        geometryRebuildDeadlineTick.accumulateAndGet(deadline, Math::min);
    }

    private void reschedulePendingGeometry(long effectiveTick) {
        if (fullResyncRetryPending.get()
                || deferredBlockMutationRevision.get()
                != drainedDeferredBlockMutationRevision) {
            scheduleGeometryRebuild(effectiveTick);
        }
    }

    void requestUrgentSolve() {
        urgentSolvePending.set(true);
    }

    /** Enables the concrete applier without changing tick-end or gameplay authority. */
    public void enableTopologyApplication(
            MinecraftThermalTopologyApplier.Parameters parameters
    ) {
        enableTopologyApplication(
                parameters,
                MaterialBoundaryRegistry.empty(),
                MinecraftThermalTopologyApplier.FarFieldSettings.disabled(),
                MinecraftPhaseTransitionHandler.rejectCustomActions());
    }

    /** Enables calibrated open-space FarField boundaries. */
    public void enableTopologyApplication(
            MinecraftThermalTopologyApplier.Parameters parameters,
            MaterialBoundaryRegistry materialBoundaries,
            MinecraftThermalTopologyApplier.FarFieldSettings farFieldSettings
    ) {
        enableTopologyApplication(
                parameters,
                materialBoundaries,
                farFieldSettings,
                MinecraftPhaseTransitionHandler.rejectCustomActions());
    }

    private void enableTopologyApplication(
            MinecraftThermalTopologyApplier.Parameters parameters,
            MaterialBoundaryRegistry materialBoundaries,
            MinecraftThermalTopologyApplier.FarFieldSettings farFieldSettings,
            MinecraftPhaseTransitionHandler customPhaseTransitionHandler
    ) {
        requireMainThread();
        requireOpen();
        if (topologyApplier != null) {
            throw new IllegalStateException("Minecraft thermal topology application is enabled");
        }
        topologyApplier = new MinecraftThermalTopologyApplier(
                runtime, signatureRegistry, geometryDeltas, resolvedInputs,
                parameters, materialBoundaries, farFieldSettings);
        this.customPhaseTransitionHandler = Objects.requireNonNull(
                customPhaseTransitionHandler, "customPhaseTransitionHandler");
        maximumPhaseMutationsPerTick = parameters.maximumPhaseMutationsPerTick();
    }

    /** Enables the main-thread topology and solve dispatch used by gameplay. */
    public void enableSynchronousDispatch() {
        requireMainThread();
        requireOpen();
        if (topologyApplier == null) {
            throw new IllegalStateException(
                    "topology application must be enabled before dispatch");
        }
        if (dispatchEnabled) {
            throw new IllegalStateException("Minecraft thermal dispatch is enabled");
        }
        dispatchEnabled = true;
    }

    /** Enables the two frozen physical source producers without gameplay authority. */
    void enablePhysicalSources(
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

    /**
     * Reads an already-admitted player environment without running a solve,
     * loading a chunk, or creating Page interest.
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

        samplePublishedAir(
                receiverX,
                receiverEyeY,
                receiverZ,
                currentTick,
                maximumPublicationAgeTicks,
                out);
        composeAnalyticFields(receiverX, receiverEyeY, receiverZ, out);

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

        samplePublishedAir(
                receiverBlockX + 0.5D,
                receiverBlockY + 0.5D,
                receiverBlockZ + 0.5D,
                currentTick,
                maximumPublicationAgeTicks,
                out);
        composeAnalyticFields(
                receiverBlockX + 0.5D,
                receiverBlockY + 0.5D,
                receiverBlockZ + 0.5D,
                out);
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
            samplePublishedAir(
                    projection.representativeX(groupKey) + 0.5D,
                    projection.representativeY(groupKey) + 0.5D,
                    projection.representativeZ(groupKey) + 0.5D,
                    currentTick,
                    maximumPublicationAgeTicks,
                    townGroupEnvironmentScratch);
            composeAnalyticFields(
                    projection.representativeX(groupKey) + 0.5D,
                    projection.representativeY(groupKey) + 0.5D,
                    projection.representativeZ(groupKey) + 0.5D,
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
            }
            out.setAggregateAir(
                    weightedTemperatureC / hitWeight,
                    commonMediumId,
                    combinedCellFlags,
                    oldestSampleTick,
                    voxelCount == 0 ? 0.0F : (float) hitWeight / voxelCount,
                    combinedQueryFlags);
        } else {
            if (voxelCount == 0) {
                combinedQueryFlags |= QUERY_NO_AIR_COMPONENT;
            }
            out.addFlags(combinedQueryFlags);
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
    }

    private void composeAnalyticFields(
            double x,
            double y,
            double z,
            MutableEnvironmentSample out
    ) {
        if (out.airAvailable()) {
            out.setComposedAirTemperature(composeAnalyticFields(
                    x, y, z, out.airTemperatureC(), out));
        }
    }

    private double composeAnalyticFields(
            double x,
            double y,
            double z,
            double baseTemperatureC,
            MutableEnvironmentSample out
    ) {
        double result = baseTemperatureC;
        boolean applied = false;
        for (int index = 0; index < analyticFields.size(); index++) {
            AnalyticField field = analyticFields.get(index);
            if (!field.contains(x, y, z)) {
                continue;
            }
            result = switch (field.combineMode()) {
                case OVERRIDE -> field.temperatureC();
                case MAX_HEAT -> Math.max(result, field.temperatureC());
                case MIN_COOL -> Math.min(result, field.temperatureC());
                case ADD_DELTA -> result + field.temperatureC();
            };
            applied = true;
        }
        if (applied && out != null) {
            out.addFlag(QUERY_ANALYTIC_FIELD_APPLIED);
        }
        return result;
    }

    private static long playerReceiverKey(ServerPlayer player) {
        return player.getUUID().getMostSignificantBits()
                ^ Long.rotateLeft(player.getUUID().getLeastSignificantBits(), 17);
    }

    /**
     * Composes the player's base air backend. The publication replaces only
     * the local air value; receiver-local weather, medium, surface, and body
     * effects are applied by {@code TemperatureComputation} afterward.
     */
    public static double gameplayPlayerEnvironment(
            ServerPlayer player,
            double naturalAirTemperatureC,
            MutableEnvironmentSample out
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(out, "out").clear();
        if (!Double.isFinite(naturalAirTemperatureC)) {
            return naturalAirTemperatureC;
        }
        MinecraftThermalInput input = active(player.serverLevel());
        if (input == null) {
            input = startGameplayInput(
                    player.serverLevel(),
                    WorldTemperature.naturalAir(
                            player.serverLevel(), player.blockPosition()));
        }
        if (input == null) {
            return naturalAirTemperatureC;
        }
        if (input.ensureGameplayPage(
                BlockPos.containing(player.getX(), player.getEyeY(), player.getZ()))) {
            input.samplePlayerEnvironment(
                    playerReceiverKey(player),
                    player.getId() & Integer.MAX_VALUE,
                    player.getX(),
                    player.getY(),
                    player.getEyeY(),
                    player.getZ(),
                    player.serverLevel().getGameTime(),
                    MAX_PUBLICATION_AGE_TICKS,
                    out);
        }
        return out.airAvailable()
                ? out.airTemperatureC()
                : input.composeAnalyticFields(
                        player.getX(), player.getEyeY(), player.getZ(),
                        naturalAirTemperatureC, out);
    }

    /** Returns the composed passive air value without creating Page interest. */
    public static double gameplayPassiveEnvironment(
            LevelReader level,
            BlockPos receiverPosition,
            double naturalTemperatureC
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(receiverPosition, "receiverPosition");
        if (!(level instanceof ServerLevel serverLevel)
                || !Double.isFinite(naturalTemperatureC)
                || !serverLevel.getServer().isSameThread()) {
            return naturalTemperatureC;
        }
        MinecraftThermalInput input = active(serverLevel);
        if (input == null) {
            return naturalTemperatureC;
        }
        MutableEnvironmentSample sample = input.cropEnvironmentScratch;
        input.sampleCropEnvironment(
                receiverPosition.getX(),
                receiverPosition.getY(),
                receiverPosition.getZ(),
                serverLevel.getGameTime(),
                MAX_PUBLICATION_AGE_TICKS,
                sample);
        return sample.airAvailable()
                ? sample.airTemperatureC()
                : input.composeAnalyticFields(
                        receiverPosition.getX() + 0.5D,
                        receiverPosition.getY() + 0.5D,
                        receiverPosition.getZ() + 0.5D,
                        naturalTemperatureC,
                        sample);
    }

    /** Returns composed crop air when available, otherwise natural temperature. */
    public static double gameplayCropEnvironment(
            LevelAccessor level,
            BlockPos receiverPosition,
            double naturalBlockTemperatureC
    ) {
        return gameplayPassiveEnvironment(
                level, receiverPosition, naturalBlockTemperatureC);
    }

    /** Returns a complete published town average, otherwise natural composition. */
    public static double gameplayTownEnvironment(
            LevelAccessor level,
            TownThermalProjection projection,
            double naturalAverageTemperatureC
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(projection, "projection");
        if (!(level instanceof ServerLevel serverLevel)
                || !Double.isFinite(naturalAverageTemperatureC)
                || projection.voxelCount() == 0) {
            return naturalAverageTemperatureC;
        }
        MinecraftThermalInput input = active(serverLevel);
        if (input == null) {
            return naturalAverageTemperatureC;
        }
        MutableEnvironmentSample sample = input.townEnvironmentScratch;
        input.sampleTownEnvironment(
                projection,
                serverLevel.getGameTime(),
                MAX_PUBLICATION_AGE_TICKS,
                sample);
        boolean complete = sample.airAvailable()
                && (sample.flags() & QUERY_PARTIAL_REGION) == 0;
        return complete
                ? sample.airTemperatureC()
                : input.composeTownAnalyticFallback(
                        projection, naturalAverageTemperatureC);
    }

    private double composeTownAnalyticFallback(
            TownThermalProjection projection,
            double naturalAverageTemperatureC
    ) {
        if (analyticFields.isEmpty()) {
            return naturalAverageTemperatureC;
        }
        int totalWeight = 0;
        double weightedTemperatureC = 0.0D;
        for (long groupKey : projection.groupKeys()) {
            int weight = projection.weight(groupKey);
            if (weight <= 0) {
                continue;
            }
            int x = projection.representativeX(groupKey);
            int y = projection.representativeY(groupKey);
            int z = projection.representativeZ(groupKey);
            townNaturalPositionScratch.set(x, y, z);
            double naturalTemperatureC = WorldTemperature.naturalBlock(
                    level, townNaturalPositionScratch);
            weightedTemperatureC += composeAnalyticFields(
                    x + 0.5D,
                    y + 0.5D,
                    z + 0.5D,
                    naturalTemperatureC,
                    null) * weight;
            totalWeight += weight;
        }
        return totalWeight == 0
                ? naturalAverageTemperatureC
                : weightedTemperatureC / totalWeight;
    }

    public static boolean upsertGameplayAnalyticField(
            ServerLevel level,
            AnalyticField field
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(field, "field");
        if (!level.getServer().isSameThread()) {
            return false;
        }
        MinecraftThermalInput input = active(level);
        if (input == null) {
            input = startGameplayInput(
                    level,
                    WorldTemperature.naturalAir(
                            level,
                            BlockPos.containing(
                                    field.centerX(), field.centerY(), field.centerZ())));
        }
        if (input == null) {
            return false;
        }
        input.upsertAnalyticField(field);
        return true;
    }

    public static boolean removeGameplayAnalyticField(
            ServerLevel level,
            long fieldId
    ) {
        Objects.requireNonNull(level, "level");
        MinecraftThermalInput input = active(level);
        return input != null
                && level.getServer().isSameThread()
                && input.removeAnalyticField(fieldId);
    }

    public static List<AnalyticField> gameplayAnalyticFieldsAt(
            ServerLevel level,
            BlockPos position
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(position, "position");
        MinecraftThermalInput input = active(level);
        if (input == null || !level.getServer().isSameThread()) {
            return List.of();
        }
        double x = position.getX() + 0.5D;
        double y = position.getY() + 0.5D;
        double z = position.getZ() + 0.5D;
        List<AnalyticField> result = new ArrayList<>();
        for (int index = 0; index < input.analyticFields.size(); index++) {
            AnalyticField field = input.analyticFields.get(index);
            if (field.contains(x, y, z)) {
                result.add(field);
            }
        }
        return result;
    }

    public static boolean hasGameplayAnalyticFieldAt(
            ServerLevel level,
            BlockPos position
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(position, "position");
        MinecraftThermalInput input = active(level);
        if (input == null || !level.getServer().isSameThread()) {
            return false;
        }
        double x = position.getX() + 0.5D;
        double y = position.getY() + 0.5D;
        double z = position.getZ() + 0.5D;
        for (int index = 0; index < input.analyticFields.size(); index++) {
            if (input.analyticFields.get(index).contains(x, y, z)) {
                return true;
            }
        }
        return false;
    }

    public void upsertAnalyticField(AnalyticField field) {
        requireMainThread();
        requireOpen();
        Objects.requireNonNull(field, "field");
        for (int index = 0; index < analyticFields.size(); index++) {
            if (analyticFields.get(index).fieldId() == field.fieldId()) {
                analyticFields.set(index, field);
                analyticFields.sort(ANALYTIC_FIELD_ORDER);
                return;
            }
        }
        analyticFields.add(field);
        analyticFields.sort(ANALYTIC_FIELD_ORDER);
    }

    public boolean removeAnalyticField(long fieldId) {
        requireMainThread();
        requireOpen();
        return analyticFields.removeIf(field -> field.fieldId() == fieldId);
    }

    /** Builds the existing infrared shader payload from the two live backends. */
    public static float[] gameplayInfraredFields(
            ServerLevel level,
            ChunkPos centerChunk,
            int chunkRadius,
            int maximumFields
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(centerChunk, "centerChunk");
        if (!level.getServer().isSameThread()
                || chunkRadius < 0
                || maximumFields <= 0) {
            return new float[0];
        }
        MinecraftThermalInput input = active(level);
        if (input == null) {
            return new float[0];
        }
        int minimumX = SectionPos.sectionToBlockCoord(centerChunk.x - chunkRadius);
        int maximumX = SectionPos.sectionToBlockCoord(centerChunk.x + chunkRadius) + 15;
        int minimumZ = SectionPos.sectionToBlockCoord(centerChunk.z - chunkRadius);
        int maximumZ = SectionPos.sectionToBlockCoord(centerChunk.z + chunkRadius) + 15;
        float[] fields = new float[Math.multiplyExact(maximumFields, 8)];
        int count = input.appendAnalyticInfraredFields(
                fields, 0, maximumFields,
                minimumX, maximumX, minimumZ, maximumZ);
        if (input.physicalSources != null && count < maximumFields) {
            count = input.physicalSources.appendInfraredFields(
                    fields, count, maximumFields,
                    minimumX, maximumX, minimumZ, maximumZ);
        }
        return count == maximumFields
                ? fields
                : Arrays.copyOf(fields, count * 8);
    }

    private int appendAnalyticInfraredFields(
            float[] output,
            int count,
            int maximumFields,
            double minimumX,
            double maximumX,
            double minimumZ,
            double maximumZ
    ) {
        for (int index = 0;
             index < analyticFields.size() && count < maximumFields;
             index++) {
            AnalyticField field = analyticFields.get(index);
            if (field.intersectsHorizontalBounds(
                    minimumX, maximumX, minimumZ, maximumZ)) {
                field.writeInfrared(output, count * 8);
                count++;
            }
        }
        return count;
    }

    public static BlockPos nearestGameplayGenerator(
            Level level,
            BlockPos receiverPosition,
            double maximumDistanceBlocks
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(receiverPosition, "receiverPosition");
        if (!(level instanceof ServerLevel serverLevel)
                || !serverLevel.getServer().isSameThread()
                || !Double.isFinite(maximumDistanceBlocks)
                || maximumDistanceBlocks <= 0.0D) {
            return null;
        }
        MinecraftThermalInput input = active(serverLevel);
        return input == null || input.physicalSources == null
                ? null
                : input.physicalSources.nearestEnabledGenerator(
                        receiverPosition,
                        maximumDistanceBlocks * maximumDistanceBlocks);
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
        ThermalPage existing = pages.get(sectionKey);
        if (existing != null) {
            boolean promoted = continuationOwnedPages.remove(sectionKey);
            promoted |= physicalSourceOwnedPages.remove(sectionKey);
            if (promoted) {
                admitLoadedContinuations(existing);
            }
            return true;
        }
        LevelChunk chunk = level.getChunkSource().getChunkNow(sectionX, sectionZ);
        if (chunk == null) {
            return false;
        }
        ThermalPage page = admitCapturedPage(sectionKey, chunk);
        if (page == null) {
            return false;
        }
        admitLoadedContinuations(page);
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
            boolean promoted = continuationOwnedPages.remove(sectionKey);
            if (promoted) {
                physicalSourceOwnedPages.add(sectionKey);
            }
            physicalSourcePageRefCounts.merge(sectionKey, 1, Math::addExact);
            if (promoted) {
                admitLoadedContinuations(existing);
            }
            return true;
        }
        if (physicalSourceOwnedPages.size() >= maximumColdSourcePages) {
            return false;
        }
        LevelChunk chunk = level.getChunkSource().getChunkNow(sectionX, sectionZ);
        if (chunk == null) {
            return false;
        }
        ThermalPage page = admitCapturedPage(sectionKey, chunk);
        if (page == null) {
            return false;
        }
        physicalSourcePageRefCounts.put(sectionKey, 1);
        physicalSourceOwnedPages.add(sectionKey);
        admitLoadedContinuations(page);
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

    private ThermalPage admitCapturedPage(long sectionKey, LevelChunk chunk) {
        int sectionX = SectionPos.x(sectionKey);
        int sectionY = SectionPos.y(sectionKey);
        int sectionZ = SectionPos.z(sectionKey);
        if (chunk.getPos().x != sectionX || chunk.getPos().z != sectionZ
                || pages.containsKey(sectionKey)) {
            return null;
        }
        int sectionIndex = chunk.getSectionIndexFromSectionY(sectionY);
        if (sectionIndex < 0 || sectionIndex >= chunk.getSections().length) {
            return null;
        }
        int[] signatureCut = captureFullPageSnapshot(
                sectionKey, chunk.getSections()[sectionIndex]);
        long lifecycleGeneration = nextSectionGeneration.incrementAndGet();
        long admissionWatermark = Math.incrementExact(chunkWatermark);
        chunkWatermark = admissionWatermark;
        ThermalPage page = topologyApplier.registerCapturedPage(
                sectionKey,
                lifecycleGeneration,
                admissionWatermark,
                signatureCut,
                naturalAirTemperature(sectionKey));
        topologyApplier.updateSkyExposure(page, captureSkyExposure(sectionKey, chunk));
        pages.put(sectionKey, page);
        scheduleNaturalTemperatureRefresh(page, level.getGameTime());
        adjustWitnesses(sectionX, sectionY, sectionZ, 1);
        refreshNearbyOwnerPageViews(sectionX, sectionY, sectionZ);
        requestUrgentSolve();
        return page;
    }

    private void admitLoadedContinuations(ThermalPage origin) {
        int faceMask = topologyApplier.continuationFaceMask(origin);
        if (faceMask == 0) {
            return;
        }
        int sectionX = SectionPos.x(origin.sectionKey());
        int sectionY = SectionPos.y(origin.sectionKey());
        int sectionZ = SectionPos.z(origin.sectionKey());
        for (int faceOrdinal = 0;
             faceOrdinal < ConservativeAirGeometry.Face.COUNT;
             faceOrdinal++) {
            ConservativeAirGeometry.Face face =
                    ConservativeAirGeometry.Face.fromOrdinal(faceOrdinal);
            if ((faceMask & (1 << face.ordinal())) == 0
                    || continuationOwnedPages.size()
                    >= MAXIMUM_LOADED_CONTINUATION_PAGES) {
                continue;
            }
            long neighborKey = continuationNeighborKey(
                    sectionX, sectionY, sectionZ, face);
            if (pages.containsKey(neighborKey)) {
                continue;
            }
            LevelChunk chunk = level.getChunkSource().getChunkNow(
                    SectionPos.x(neighborKey), SectionPos.z(neighborKey));
            if (chunk == null) {
                continue;
            }
            ThermalPage admitted = admitCapturedPage(neighborKey, chunk);
            if (admitted != null) {
                continuationOwnedPages.add(neighborKey);
            }
        }
    }

    private static long continuationNeighborKey(
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

    private boolean processPhaseTransitions() {
        if (topologyApplier == null || maximumPhaseMutationsPerTick == 0) {
            return false;
        }
        long previousAckWatermark = transitionAckWatermark;
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
        return transitionAckWatermark != previousAckWatermark;
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
        runtime.close();
        for (SectionOwner owner : new ArrayList<>(ownersByIdentity.values())) {
            detach(owner);
        }
        pages.clear();
        analyticFields.clear();
        witnessRefCounts.clear();
        radiationTrackedSections.clear();
        dirtyPages.clear();
        deferredDirtyOwners.clear();
        deferredInvalidatedSourceSections.clear();
        synchronized (dirtySkyColumns) {
            dirtySkyColumns.clear();
        }
        pageEnvironmentRefreshes.clear();
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
            long generation = NEXT_GAMEPLAY_RUNTIME_ID.getAndIncrement();
            long initialTick = level.getGameTime();
            ThermalCellArena arena = new ThermalCellArena(256);
            NodePowerAccumulatorArena accumulators =
                    new NodePowerAccumulatorArena(256);
            ThermalSourceTimeline sources = new ThermalSourceTimeline(
                    generation,
                    initialTick,
                    4_096,
                    new ThermalSourceRegistry(64, 3, accumulators),
                    arena);
            BuoyancyConductance.Parameters buoyancy =
                    new BuoyancyConductance.Parameters(0.25D, 4.0D, 10.0D);
            ThermalSweep sweep = ThermalSweepFragments.builder(
                    arena, null, buoyancy, 0).build();
            QueryPublication publication = QueryPublication.tryCreate(
                    GAMEPLAY_MEMORY_BUDGET.createDimensionBudget(
                            16L * 1024L * 1024L,
                            1L * 1024L * 1024L),
                    GAMEPLAY_INITIAL_PUBLICATION_CAPACITY);
            if (publication == null) {
                return null;
            }
            runtime = new DimensionThermalRuntime(
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
                            GAMEPLAY_MAX_ACTIVE_CELLS,
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
                    gameplayMaterialBoundaries,
                    new MinecraftThermalTopologyApplier.FarFieldSettings(
                            GAMEPLAY_FAR_FIELDS,
                            true,
                            farFieldEnvironment(level),
                            GAMEPLAY_FAR_FIELD_CALIBRATION_POWER_W,
                            GAMEPLAY_FAR_FIELD_REFERENCE_AREA_BLOCKS_SQUARED,
                            GAMEPLAY_CONTINUATION_DISTANCE_BLOCKS));
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
            input.enableSynchronousDispatch();
            return input;
        } catch (RuntimeException exception) {
            if (input != null) {
                input.close();
            }
            else if (runtime != null) {
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
        Map<BlockState, Integer> phaseProfileIdsByState = new IdentityHashMap<>();
        Map<GameplayPhaseProfileKey, Integer> profileIds = new LinkedHashMap<>();
        Map<Long, Integer> contactPatternIds = new LinkedHashMap<>();
        List<MaterialBoundaryRegistry.Profile> profiles = new ArrayList<>();
        for (GameplayMaterial material : GameplayMaterial.values()) {
            profiles.add(material.profile());
        }
        List<MaterialBoundaryRegistry.ContactPattern> contactPatterns =
                new ArrayList<>();
        int[] materialStateCounts = new int[GameplayMaterial.values().length];
        int transitionStateCount = 0;
        int skippedWithoutMaterialContact = 0;
        ThermalSignatureRegistry.Builder signatures =
                ThermalSignatureRegistry.builder();

        for (Block block : blocks) {
            if (block.hasDynamicShape()) {
                continue;
            }
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                StateTransitionData data = StateTransitionData.getData(state);
                StateTransitionData.HeatingTransition transition = null;
                if (data != null && data.willTransit() && data.heatCapacity() > 0) {
                    transition = data.heatingTransition(state);
                    if (transition != null) {
                        transitionStateCount++;
                    }
                }
                ThermalResolution<ResolvedThermalSignature> geometry =
                        geometryResolver.resolve(state, state.getFluidState());
                if (!geometry.isResolved()) {
                    if (transition != null) {
                        skippedWithoutMaterialContact++;
                    }
                    continue;
                }
                ResolvedThermalSignature geometrySignature =
                        geometry.value().orElseThrow();
                long materialMask = materialMask(geometrySignature);
                int profileId = 0;
                if (transition != null) {
                    if (materialMask == 0L) {
                        skippedWithoutMaterialContact++;
                    } else {
                        double transitionEnergyJ = GAMEPLAY_PHASE_BASE_ENERGY_J
                                * data.heatCapacity();
                        GameplayPhaseProfileKey profileKey = new GameplayPhaseProfileKey(
                                transition.temperatureC(), transitionEnergyJ);
                        Integer phaseProfileId = profileIds.get(profileKey);
                        if (phaseProfileId == null) {
                            phaseProfileId = profiles.size() + 1;
                            profileIds.put(profileKey, phaseProfileId);
                            profiles.add(MaterialBoundaryRegistry.Profile.phaseReservoir(
                                    phaseProfileId,
                                    GAMEPLAY_PHASE_FACE_CONDUCTANCE_W_PER_K,
                                    transition.temperatureC(),
                                    transitionEnergyJ,
                                    MaterialBoundaryRegistry.TransitionMutationPolicy
                                            .RESPECT_RANDOM_TICK_SPEED,
                                    MaterialBoundaryRegistry.TransitionAction
                                            .APPLY_STATE_TRANSITION_RECIPE));
                        }
                        profileId = phaseProfileId;
                        phaseProfileIdsByState.put(state, profileId);
                    }
                } else if (materialMask != 0L && state.getFluidState().isEmpty()) {
                    GameplayMaterial material = classifyGameplayMaterial(state);
                    if (material != null) {
                        profileId = material.profileId();
                        materialStateCounts[material.ordinal()]++;
                    }
                }

                int contactPatternId = profileId == 0
                        ? 0
                        : contactPatternId(
                                materialMask, contactPatternIds, contactPatterns);
                signatures.intern(withMaterialProfile(
                        geometrySignature, profileId, contactPatternId));
            }
        }

        StateStaticThermalResolver.SignatureMetadata neutral =
                new StateStaticThermalResolver.SignatureMetadata(
                        0, 0, 0, 0, 0, 0, 0);
        Map<Long, Integer> frozenContactPatternIds = Map.copyOf(contactPatternIds);
        StateStaticThermalResolver resolver = StateStaticThermalResolver.withMaterialMask(
                64,
                (state, fluid, materialMask) -> {
                    Integer profileId = phaseProfileIdsByState.get(state);
                    if (profileId == null && materialMask != 0L && fluid.isEmpty()) {
                        GameplayMaterial material = classifyGameplayMaterial(state);
                        if (material != null) {
                            profileId = material.profileId();
                        }
                    }
                    if (profileId == null || materialMask == 0L) {
                        return neutral;
                    }
                    Integer patternId = frozenContactPatternIds.get(materialMask);
                    if (patternId == null) {
                        return neutral;
                    }
                    return new StateStaticThermalResolver.SignatureMetadata(
                            0, profileId, patternId, 0, 0, 0, 0);
                }
        );
        gameplayMaterialBoundaries = new MaterialBoundaryRegistry(
                profiles, contactPatterns);
        gameplayPhaseProfileIds = phaseProfileIdsByState;
        gameplaySignatures = signatures.build();
        gameplayDispatcher = ThermalSignatureResolverDispatcher.builder(resolver).build();
        gameplayProfileWatermark = gameplayProfileWatermark == Long.MAX_VALUE
                ? 1L : gameplayProfileWatermark + 1L;
        int materialStateCount = 0;
        for (int count : materialStateCounts) {
            materialStateCount += count;
        }
        FHMain.LOGGER.info(
                "Compiled {} non-phase static states into {} shared material profiles, "
                        + "and {} hot-side state transition states into {} shared phase "
                        + "profiles; {} contact patterns, {} phase states retained legacy",
                materialStateCount,
                GameplayMaterial.values().length,
                phaseProfileIdsByState.size(),
                profileIds.size(),
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

    static boolean staticMutationSemanticsUnchanged(
            BlockState oldState,
            BlockState newState
    ) {
        boolean oldFluidEmpty = oldState.getFluidState().isEmpty();
        if (oldState.getBlock() != newState.getBlock()
                || oldState.is(Blocks.MOVING_PISTON)
                || oldState.getBlock().hasDynamicShape()
                || oldState.canOcclude() != newState.canOcclude()
                || oldFluidEmpty != newState.getFluidState().isEmpty()
                || !Objects.equals(
                        gameplayPhaseProfileIds.get(oldState),
                        gameplayPhaseProfileIds.get(newState))) {
            return false;
        }
        if (!oldFluidEmpty) {
            return true;
        }
        try {
            if (oldState.getCollisionShape(
                    net.minecraft.world.level.EmptyBlockGetter.INSTANCE,
                    BlockPos.ZERO)
                    != newState.getCollisionShape(
                            net.minecraft.world.level.EmptyBlockGetter.INSTANCE,
                            BlockPos.ZERO)) {
                return false;
            }
        } catch (RuntimeException ignored) {
            return false;
        }
        return classifyGameplayMaterial(oldState)
                == classifyGameplayMaterial(newState);
    }

    private static long materialMask(ResolvedThermalSignature signature) {
        long provenAir = 0L;
        for (var region : signature.airRegions()) {
            provenAir |= region.provenAirMicrocellMask();
        }
        return ~provenAir;
    }

    private static int contactPatternId(
            long materialMask,
            Map<Long, Integer> contactPatternIds,
            List<MaterialBoundaryRegistry.ContactPattern> contactPatterns
    ) {
        Integer existing = contactPatternIds.get(materialMask);
        if (existing != null) {
            return existing;
        }
        int patternId = contactPatterns.size() + 1;
        contactPatternIds.put(materialMask, patternId);
        contactPatterns.add(new MaterialBoundaryRegistry.ContactPattern(
                patternId, materialMask));
        return patternId;
    }

    private static ResolvedThermalSignature withMaterialProfile(
            ResolvedThermalSignature geometry,
            int materialProfileId,
            int contactPatternId
    ) {
        return new ResolvedThermalSignature(
                geometry.mediumId(),
                materialProfileId,
                geometry.airRegions(),
                contactPatternId,
                geometry.radiationOcclusionPatternId(),
                geometry.sourceProfileId(),
                geometry.gateKind(),
                geometry.flags());
    }

    static GameplayMaterial classifyGameplayMaterial(BlockState state) {
        Objects.requireNonNull(state, "state");
        if (state.getBlock() instanceof LeavesBlock
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.CROPS)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.REPLACEABLE)) {
            return null;
        }

        if (state.is(BlockTags.WOOL)
                || state.is(BlockTags.WOOL_CARPETS)) {
            return GameplayMaterial.INSULATING_FABRIC;
        }
        if (state.is(Tags.Blocks.GLASS)
                || state.is(Tags.Blocks.GLASS_PANES)) {
            return GameplayMaterial.GLASS;
        }
        if (isMetal(state)) {
            return GameplayMaterial.METAL;
        }
        if (isWood(state)) {
            return GameplayMaterial.WOOD;
        }
        if (isEarth(state)) {
            return GameplayMaterial.EARTH;
        }
        if (isMasonry(state)) {
            return GameplayMaterial.MASONRY;
        }
        return state.blocksMotion() ? GameplayMaterial.GENERIC_SOLID : null;
    }

    private static boolean isMetal(BlockState state) {
        return FHTags.Blocks.METAL_MACHINES.matches(state)
                || state.is(BlockTags.ANVIL)
                || state.is(Tags.Blocks.STORAGE_BLOCKS_IRON)
                || state.is(Tags.Blocks.STORAGE_BLOCKS_GOLD)
                || state.is(Tags.Blocks.STORAGE_BLOCKS_COPPER)
                || state.is(Tags.Blocks.STORAGE_BLOCKS_NETHERITE)
                || state.is(Tags.Blocks.STORAGE_BLOCKS_RAW_IRON)
                || state.is(Tags.Blocks.STORAGE_BLOCKS_RAW_GOLD)
                || state.is(Tags.Blocks.STORAGE_BLOCKS_RAW_COPPER);
    }

    private static boolean isWood(BlockState state) {
        return FHTags.Blocks.WOODEN_MACHINES.matches(state)
                || state.is(BlockTags.LOGS)
                || state.is(BlockTags.PLANKS)
                || state.is(BlockTags.WOODEN_DOORS)
                || state.is(BlockTags.WOODEN_STAIRS)
                || state.is(BlockTags.WOODEN_SLABS)
                || state.is(BlockTags.WOODEN_FENCES)
                || state.is(BlockTags.WOODEN_TRAPDOORS)
                || state.is(BlockTags.MINEABLE_WITH_AXE);
    }

    private static boolean isEarth(BlockState state) {
        return FHTags.Blocks.SOIL.matches(state)
                || state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(Tags.Blocks.GRAVEL)
                || state.is(BlockTags.MINEABLE_WITH_SHOVEL);
    }

    private static boolean isMasonry(BlockState state) {
        return FHTags.Blocks.STONE.matches(state)
                || state.is(BlockTags.STONE_BRICKS)
                || state.is(BlockTags.TERRACOTTA)
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.BASE_STONE_NETHER)
                || state.is(Tags.Blocks.STONE)
                || state.is(Tags.Blocks.COBBLESTONE)
                || state.is(Tags.Blocks.END_STONES)
                || state.is(Tags.Blocks.NETHERRACK)
                || state.is(Tags.Blocks.OBSIDIAN)
                || state.is(Tags.Blocks.SANDSTONE)
                || state.is(BlockTags.MINEABLE_WITH_PICKAXE);
    }

    private record GameplayPhaseProfileKey(
            double transitionTemperatureC,
            double transitionEnergyJPerUnit
    ) {
    }

    /** Single production Mixin dispatch point for section mutations. */
    public static void onSectionSetBlockState(
            LevelChunkSection section,
            int localX,
            int localY,
            int localZ,
            BlockState oldState,
            BlockState newState
    ) {
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
            input.requestUrgentSolve();
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
            input.sealGameplayTick(level.getGameTime());
        }
    }

    public static void closeAll() {
        List<MinecraftThermalInput> active;
        synchronized (ACTIVE_BY_LEVEL) {
            active = List.copyOf(ACTIVE_BY_LEVEL.values());
        }
        for (MinecraftThermalInput input : active) {
            input.close();
        }
    }

    public static void closeActiveLevel(ServerLevel level) {
        MinecraftThermalInput input = active(level);
        if (input != null) {
            input.close();
        }
    }

    public static void onRawBlockContainerReplaced(LevelChunkSection section) {
        SectionOwner owner = attachment(section).frostedheart$getThermalInputOwner();
        if (owner != null) {
            owner.input.queueSkyExposureChunkColumns(
                    owner.sectionX, owner.sectionZ);
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

    public static void onPhysicalSourceRemoved(ServerLevel level, BlockPos sourcePosition) {
        MinecraftThermalInput input = active(level);
        if (input != null && input.physicalSources != null) {
            input.physicalSources.removeSource(sourcePosition);
        }
    }

    public static void onFountainTick(
            ServerLevel level,
            BlockPos sourcePosition,
            BlockPos steamTarget,
            double thermalLevel,
            boolean active
    ) {
        MinecraftThermalInput input = active(level);
        if (input != null && input.physicalSources != null) {
            input.physicalSources.observeFountain(
                    sourcePosition, steamTarget, thermalLevel, active);
        }
    }

    public static void onRadiatorTick(
            ServerLevel level,
            BlockPos sourcePosition,
            BlockPos exhaustTarget,
            double thermalLevel,
            boolean active
    ) {
        MinecraftThermalInput input = active(level);
        if (input != null && input.physicalSources != null) {
            input.physicalSources.observeRadiator(
                    sourcePosition, exhaustTarget, thermalLevel, active);
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
        input.queueSkyExposureChunkColumns(
                chunk.getPos().x, chunk.getPos().z);
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
        if (Thread.currentThread() != mainThread) {
            requestUrgentSolve();
            scheduleGeometryRebuild(level.getGameTime());
            if (radiationOcclusion != null) {
                radiationOcclusion.onSectionMutation(
                        owner.sectionX, owner.sectionY, owner.sectionZ);
            }
            owner.recordDeferredBlockMutation(localX, localY, localZ);
            deferredDirtyOwners.add(owner);
            deferredBlockMutationRevision.incrementAndGet();
            return;
        }
        if (ownersByIdentity.get(owner.section) != owner) {
            return;
        }

        int worldX = SectionPos.sectionToBlockCoord(owner.sectionX) + localX;
        int worldY = SectionPos.sectionToBlockCoord(owner.sectionY) + localY;
        int worldZ = SectionPos.sectionToBlockCoord(owner.sectionZ) + localZ;
        long effectiveTick = level.getGameTime();
        queueSkyExposureColumn(worldX, worldZ);
        if (physicalSources != null) {
            physicalSources.onBlockMutation(
                    new BlockPos(worldX, worldY, worldZ), oldState, newState);
        }
        if (resolverDispatcher == gameplayDispatcher
                && signatureRegistry == gameplaySignatures
                && staticMutationSemanticsUnchanged(oldState, newState)) {
            return;
        }
        scheduleGeometryRebuild(effectiveTick);
        if (radiationOcclusion != null) {
            radiationOcclusion.onSectionMutation(
                    owner.sectionX, owner.sectionY, owner.sectionZ);
        }
        recordGeometryMutation(worldX, worldY, worldZ, effectiveTick);
    }

    private void drainDeferredBlockMutations(long effectiveTick) {
        long capturedRevision = deferredBlockMutationRevision.get();
        if (capturedRevision == drainedDeferredBlockMutationRevision) {
            return;
        }
        for (SectionOwner owner : deferredDirtyOwners) {
            if (!deferredDirtyOwners.remove(owner)) {
                continue;
            }
            long[] dirtyBlocks = owner.takeDeferredBlockMutations();
            if (dirtyBlocks == null || !owner.valid
                    || ownersByIdentity.get(owner.section) != owner) {
                continue;
            }
            int sectionMinX = SectionPos.sectionToBlockCoord(owner.sectionX);
            int sectionMinY = SectionPos.sectionToBlockCoord(owner.sectionY);
            int sectionMinZ = SectionPos.sectionToBlockCoord(owner.sectionZ);
            long dirtyWords = dirtyBlocks[DEFERRED_BLOCK_WORD_COUNT];
            while (dirtyWords != 0L) {
                int wordIndex = Long.numberOfTrailingZeros(dirtyWords);
                long remaining = dirtyBlocks[wordIndex];
                while (remaining != 0L) {
                    int packedIndex = (wordIndex << 6)
                            + Long.numberOfTrailingZeros(remaining);
                    int localX = packedIndex & 15;
                    int localZ = (packedIndex >>> 4) & 15;
                    int localY = (packedIndex >>> 8) & 15;
                    int worldX = sectionMinX + localX;
                    int worldY = sectionMinY + localY;
                    int worldZ = sectionMinZ + localZ;
                    if (physicalSources != null) {
                        physicalSources.resyncBlock(
                                new BlockPos(worldX, worldY, worldZ),
                                owner.section.getBlockState(localX, localY, localZ));
                    }
                    refreshSkyExposureColumn(worldX, worldZ);
                    recordGeometryMutation(worldX, worldY, worldZ, effectiveTick);
                    remaining &= remaining - 1L;
                }
                dirtyWords &= dirtyWords - 1L;
            }
        }
        drainedDeferredBlockMutationRevision = capturedRevision;
    }

    private void recordGeometryMutation(
            int worldX,
            int worldY,
            int worldZ,
            long effectiveTick
    ) {
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
                        fullResyncRetryPending.set(true);
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
        SectionOwner owner = ownersBySectionKey.get(page.sectionKey());
        return captureFullPageSnapshot(
                page.sectionKey(), owner == null ? null : owner.section);
    }

    private int[] captureFullPageSnapshot(
            long sectionKey,
            LevelChunkSection section
    ) {
        LoadedSectionSnapshot snapshot = new LoadedSectionSnapshot(
                level,
                SectionPos.sectionToBlockCoord(SectionPos.x(sectionKey)),
                SectionPos.sectionToBlockCoord(SectionPos.y(sectionKey)),
                SectionPos.sectionToBlockCoord(SectionPos.z(sectionKey)),
                section);
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

    private double naturalAirTemperature(long sectionKey) {
        naturalTemperaturePositionScratch.set(
                SectionPos.sectionToBlockCoord(SectionPos.x(sectionKey)) + 8,
                SectionPos.sectionToBlockCoord(SectionPos.y(sectionKey)) + 8,
                SectionPos.sectionToBlockCoord(SectionPos.z(sectionKey)) + 8);
        return WorldTemperature.naturalAir(level, naturalTemperaturePositionScratch);
    }

    private byte[] captureSkyExposure(long sectionKey, LevelChunk chunk) {
        int sectionX = SectionPos.x(sectionKey);
        int sectionY = SectionPos.y(sectionKey);
        int sectionZ = SectionPos.z(sectionKey);
        if (chunk.getPos().x != sectionX || chunk.getPos().z != sectionZ) {
            throw new IllegalArgumentException("sky exposure chunk does not own the Page");
        }
        int minX = SectionPos.sectionToBlockCoord(sectionX);
        int minY = SectionPos.sectionToBlockCoord(sectionY);
        int minZ = SectionPos.sectionToBlockCoord(sectionZ);
        byte[] firstExposedLocalY = new byte[16 * 16];
        for (int localZ = 0; localZ < 16; localZ++) {
            for (int localX = 0; localX < 16; localX++) {
                int firstExposedY = chunk.getHeight(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        minX + localX,
                        minZ + localZ);
                firstExposedLocalY[(localZ << 4) | localX] = (byte) Math.max(
                        0, Math.min(16, firstExposedY - minY));
            }
        }
        return firstExposedLocalY;
    }

    private boolean refreshSkyExposureColumn(int worldX, int worldZ) {
        int sectionX = SectionPos.blockToSectionCoord(worldX);
        int sectionZ = SectionPos.blockToSectionCoord(worldZ);
        LevelChunk chunk = level.getChunkSource().getChunkNow(sectionX, sectionZ);
        if (chunk == null) {
            return false;
        }
        int firstExposedY = chunk.getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, worldX, worldZ);
        int localX = SectionPos.sectionRelative(worldX);
        int localZ = SectionPos.sectionRelative(worldZ);
        boolean changed = false;
        for (int sectionIndex = 0;
             sectionIndex < chunk.getSections().length;
             sectionIndex++) {
            int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex);
            ThermalPage page = pages.get(SectionPos.asLong(
                    sectionX, sectionY, sectionZ));
            if (page == null) {
                continue;
            }
            int localFirstExposedY = Math.max(0, Math.min(
                    16, firstExposedY - SectionPos.sectionToBlockCoord(sectionY)));
            changed |= topologyApplier.updateSkyExposureColumn(
                    page, localX, localZ, localFirstExposedY);
        }
        return changed;
    }

    private void queueSkyExposureColumn(int worldX, int worldZ) {
        long column = ((long) worldX << 32)
                | Integer.toUnsignedLong(worldZ);
        synchronized (dirtySkyColumns) {
            dirtySkyColumns.add(column);
        }
    }

    private void queueSkyExposureChunkColumns(int sectionX, int sectionZ) {
        int minimumX = SectionPos.sectionToBlockCoord(sectionX);
        int minimumZ = SectionPos.sectionToBlockCoord(sectionZ);
        synchronized (dirtySkyColumns) {
            for (int localZ = 0; localZ < 16; localZ++) {
                for (int localX = 0; localX < 16; localX++) {
                    long column = ((long) (minimumX + localX) << 32)
                            | Integer.toUnsignedLong(minimumZ + localZ);
                    dirtySkyColumns.add(column);
                }
            }
        }
    }

    private boolean refreshDirtySkyExposureColumns() {
        int count = 0;
        synchronized (dirtySkyColumns) {
            if (dirtySkyColumns.isEmpty()) {
                return false;
            }
            LongIterator columns = dirtySkyColumns.iterator();
            while (count < MAXIMUM_SKY_COLUMN_REFRESHES_PER_TICK
                    && columns.hasNext()) {
                dirtySkyColumnRefreshScratch[count++] = columns.nextLong();
                columns.remove();
            }
        }
        boolean changed = false;
        for (int index = 0; index < count; index++) {
            long column = dirtySkyColumnRefreshScratch[index];
            changed |= refreshSkyExposureColumn(
                    (int) (column >> 32), (int) column);
        }
        return changed;
    }

    private boolean refreshNaturalTemperatures(long effectiveTick) {
        if (topologyApplier == null) {
            return false;
        }
        boolean changed = false;
        if (effectiveTick >= nextFarFieldWindRefreshTick) {
            nextFarFieldWindRefreshTick = effectiveTick
                    + NATURAL_TEMPERATURE_REFRESH_TICKS;
            double windScale = 1.0D + GAMEPLAY_FAR_FIELD_WIND_GAIN
                    * Math.max(0, Math.min(100, WorldTemperature.wind(level))) / 100.0D;
            changed = topologyApplier.updateFarFieldConductanceScale(
                    windScale, FAR_FIELD_WIND_REBUILD_DELTA);
        }
        int dueCount = pollDuePageEnvironmentRefreshes(
                pageEnvironmentRefreshes,
                effectiveTick,
                duePageEnvironmentRefreshScratch);
        for (int index = 0; index < dueCount; index++) {
            PageEnvironmentRefresh refresh = duePageEnvironmentRefreshScratch[index];
            duePageEnvironmentRefreshScratch[index] = null;
            ThermalPage page = pages.get(refresh.sectionKey);
            if (page == null
                    || page.lifecycleGeneration() != refresh.lifecycleGeneration) {
                continue;
            }
            changed |= topologyApplier.updateNaturalTemperature(
                    page,
                    naturalAirTemperature(page.sectionKey()),
                    NATURAL_TEMPERATURE_REBUILD_DELTA_C);
            refresh.dueTick = effectiveTick + NATURAL_TEMPERATURE_REFRESH_TICKS;
            pageEnvironmentRefreshes.add(refresh);
        }
        return changed;
    }

    static int pollDuePageEnvironmentRefreshes(
            PriorityQueue<PageEnvironmentRefresh> queue,
            long effectiveTick,
            PageEnvironmentRefresh[] target
    ) {
        Objects.requireNonNull(queue, "queue");
        Objects.requireNonNull(target, "target");
        int count = 0;
        while (count < target.length) {
            PageEnvironmentRefresh refresh = queue.peek();
            if (refresh == null || refresh.dueTick > effectiveTick) {
                break;
            }
            target[count++] = queue.remove();
        }
        return count;
    }

    private void scheduleNaturalTemperatureRefresh(
            ThermalPage page,
            long effectiveTick
    ) {
        long mixed = page.sectionKey();
        mixed ^= mixed >>> 33;
        mixed *= 0xff51afd7ed558ccdl;
        mixed ^= mixed >>> 33;
        mixed *= 0xc4ceb9fe1a85ec53l;
        mixed ^= mixed >>> 33;
        long offset = 1L + Math.floorMod(
                mixed, NATURAL_TEMPERATURE_REFRESH_TICKS);
        pageEnvironmentRefreshes.add(new PageEnvironmentRefresh(
                page.sectionKey(),
                page.lifecycleGeneration(),
                effectiveTick + offset));
    }

    private static FarFieldProfileRegistry.EnvironmentClass farFieldEnvironment(
            ServerLevel level
    ) {
        if (level.dimension() == Level.OVERWORLD) {
            return FarFieldProfileRegistry.EnvironmentClass.OVERWORLD_OUTDOOR;
        }
        if (level.dimension() == Level.NETHER) {
            return FarFieldProfileRegistry.EnvironmentClass.NETHER_OUTDOOR;
        }
        return FarFieldProfileRegistry.EnvironmentClass.CUSTOM_NATURAL;
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
        deferredDirtyOwners.remove(owner);
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

    private void submitDispatch(SealedInputFrame frame) {
        if (closed) {
            return;
        }
        dispatchSealedFrame(frame);
    }

    private void dispatchSealedFrame(SealedInputFrame frame) {
        if (closed) {
            return;
        }
        if (!recoverOlderInFlightEpoch(frame)) {
            urgentSolvePending.set(true);
            return;
        }
        MinecraftThermalTopologyApplier.ApplyStatus status;
        try {
            status = topologyApplier.apply(frame);
        } catch (RuntimeException failure) {
            FHMain.LOGGER.error(
                    "Thermal topology update failed for {}; scheduling a complete rebuild",
                    level.dimension().location(),
                    failure);
            topologyApplier.requestFullTopologyRecovery();
            urgentSolvePending.set(true);
            return;
        }
        if (status != MinecraftThermalTopologyApplier.ApplyStatus.APPLIED
                && status != MinecraftThermalTopologyApplier.ApplyStatus.TOPOLOGY_UNCHANGED) {
            urgentSolvePending.set(true);
            return;
        }
        if (drainCommittedSourceBindingSections()) {
            urgentSolvePending.set(true);
        }
        runtime.runOne();
        consumeSolveFailure();
    }

    /** Completes one old epoch before any newer input ring cut is drained. */
    private boolean recoverOlderInFlightEpoch(SealedInputFrame frame) {
        Optional<SealedInputFrame> inFlight = runtime.inFlightFrame();
        if (inFlight.isEmpty()) {
            return true;
        }
        boolean differentFrame = !inFlight.orElseThrow().equals(frame);
        if (!runtime.inFlightTopologyRecoveryRequired() && !differentFrame) {
            return true;
        }

        if (!runtime.inFlightTopologyRecoveryRequired()) {
            runtime.runOne();
            consumeSolveFailure();
            inFlight = runtime.inFlightFrame();
            if (inFlight.isEmpty()) {
                return true;
            }
        }

        MinecraftThermalTopologyApplier.ApplyStatus recovery;
        try {
            recovery = topologyApplier.recoverInFlightEpoch();
        } catch (RuntimeException failure) {
            FHMain.LOGGER.error(
                    "Thermal in-flight recovery failed for {}; retrying without draining the latest frame",
                    level.dimension().location(),
                    failure);
            topologyApplier.requestFullTopologyRecovery();
            return false;
        }
        if (recovery != MinecraftThermalTopologyApplier.ApplyStatus.APPLIED
                && recovery != MinecraftThermalTopologyApplier.ApplyStatus.TOPOLOGY_UNCHANGED) {
            return false;
        }
        boolean sourceBindingsInvalidated = drainCommittedSourceBindingSections();
        if (sourceBindingsInvalidated) {
            urgentSolvePending.set(true);
        }
        runtime.runOne();
        consumeSolveFailure();
        return runtime.inFlightFrame().isEmpty() && !sourceBindingsInvalidated;
    }

    private void consumeSolveFailure() {
        RuntimeException failure = runtime.consumeFailureForRecovery();
        if (failure == null) {
            return;
        }
        if (failure instanceof DimensionThermalRuntime.WorkLimitExceededException) {
            FHMain.LOGGER.warn(
                    "Thermal work limit reached for {}; suppressing mesh publication until topology input changes the work set: {}",
                    level.dimension().location(),
                    failure.getMessage());
        } else {
            FHMain.LOGGER.error(
                    "Thermal solve failed for {}; scheduling isolated in-flight topology recovery",
                    level.dimension().location(),
                    failure);
        }
        topologyApplier.requestFullTopologyRecovery();
        urgentSolvePending.set(true);
    }

    private void invalidatePhysicalSourcesForPage(long sectionKey) {
        if (physicalSources == null) {
            return;
        }
        if (Thread.currentThread() == mainThread) {
            physicalSources.onPageInvalidated(sectionKey);
            return;
        }
        deferredInvalidatedSourceSections.add(sectionKey);
    }

    private boolean drainDeferredSourceInvalidations() {
        if (physicalSources == null || deferredInvalidatedSourceSections.isEmpty()) {
            return false;
        }
        boolean invalidated = false;
        for (long sectionKey : deferredInvalidatedSourceSections) {
            if (deferredInvalidatedSourceSections.remove(sectionKey)) {
                physicalSources.onPageInvalidated(sectionKey);
                invalidated = true;
            }
        }
        return invalidated;
    }

    private boolean drainCommittedSourceBindingSections() {
        if (physicalSources == null || topologyApplier == null) {
            return false;
        }
        long[] sections = topologyApplier.drainCommittedSourceBindingSections();
        for (long sectionKey : sections) {
            physicalSources.onPageCommitted(sectionKey);
        }
        return sections.length != 0;
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
            mediumId = -1;
            confidence = 0.0F;
            sampleTick = -1L;
            cellFlags = 0;
            flags = 0;
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

        private void setComposedAirTemperature(double nextAirTemperatureC) {
            airTemperatureC = nextAirTemperatureC;
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
        private long[] deferredBlockMutations;

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

        private synchronized void recordDeferredBlockMutation(
                int localX,
                int localY,
                int localZ
        ) {
            if (!valid) {
                return;
            }
            if ((localX | localY | localZ) < 0
                    || localX >= 16 || localY >= 16 || localZ >= 16) {
                return;
            }
            if (deferredBlockMutations == null) {
                deferredBlockMutations = new long[DEFERRED_BLOCK_WORD_COUNT + 1];
            }
            int index = blockIndex(localX, localY, localZ);
            int wordIndex = index >>> 6;
            deferredBlockMutations[wordIndex] |= 1L << (index & 63);
            deferredBlockMutations[DEFERRED_BLOCK_WORD_COUNT] |= 1L << wordIndex;
        }

        private synchronized long[] takeDeferredBlockMutations() {
            long[] result = deferredBlockMutations;
            deferredBlockMutations = null;
            return result;
        }

        private void invalidateAffectedPages(ThermalPage.GeometryResyncReason reason) {
            input.requestUrgentSolve();
            input.geometryRebuildDeadlineTick.accumulateAndGet(
                    input.level.getGameTime(), Math::min);
            for (ThermalPage page : affectedPages) {
                page.requireFullGeometryResync(reason);
                input.invalidatePhysicalSourcesForPage(page.sectionKey());
                if (Thread.currentThread() == input.mainThread) {
                    input.dirtyPages.put(page, Boolean.TRUE);
                }
            }
            if (Thread.currentThread() != input.mainThread) {
                input.fullResyncRetryPending.set(true);
            }
        }
    }

    static final class PageEnvironmentRefresh {
        private final long sectionKey;
        private final long lifecycleGeneration;
        private long dueTick;

        PageEnvironmentRefresh(
                long sectionKey,
                long lifecycleGeneration,
                long dueTick
        ) {
            this.sectionKey = sectionKey;
            this.lifecycleGeneration = lifecycleGeneration;
            this.dueTick = dueTick;
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
        private final LevelChunkSection section;
        private final ResolverBlockView.SnapshotCell<BlockState, FluidState>[] cells;

        @SuppressWarnings("unchecked")
        private LoadedSectionSnapshot(
                ServerLevel level,
                int sectionMinX,
                int sectionMinY,
                int sectionMinZ,
                LevelChunkSection section
        ) {
            this.level = level;
            this.sectionMinX = sectionMinX;
            this.sectionMinY = sectionMinY;
            this.sectionMinZ = sectionMinZ;
            this.section = section;
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
            if (section != null
                    && localX >= 0 && localX < 16
                    && localY >= 0 && localY < 16
                    && localZ >= 0 && localZ < 16) {
                BlockState state = section.getBlockState(localX, localY, localZ);
                ResolverBlockView.SnapshotCell<BlockState, FluidState> captured =
                        ResolverBlockView.SnapshotCell.present(
                                state, state.getFluidState());
                cells[index] = captured;
                return captured;
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
