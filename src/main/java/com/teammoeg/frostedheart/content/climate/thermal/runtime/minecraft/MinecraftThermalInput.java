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

import com.teammoeg.frostedheart.content.climate.thermal.geometry.GeometryDeltaRing;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPage;
import com.teammoeg.frostedheart.content.climate.thermal.phase0.mutation.Phase0aMutationProbe;
import com.teammoeg.frostedheart.content.climate.thermal.profile.DependencyOffsetMask;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolvedThermalSignature;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ResolverBlockView;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalResolution;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureResolution;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.ThermalSignatureResolverDispatcher;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.DimensionThermalRuntime;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalRuntimeCoordinator;
import com.teammoeg.frostedheart.content.climate.thermal.solver.InputWatermarks;
import com.teammoeg.frostedheart.content.climate.thermal.solver.LatestSolveEpochScheduler;
import com.teammoeg.frostedheart.content.climate.thermal.solver.SealedInputFrame;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;

import java.util.ArrayList;
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
 * Main-thread Minecraft input owner for one dormant/shadow dimension runtime.
 *
 * <pre>
 * section mutation Mixin
 *   owner lookup -> page invalidation -> loaded-only resolve -> primitive event
 * tick end
 *   seal page deltas -> seal five stream watermarks
 *   -> optional latest-only shadow executor -> topology -> coordinator
 * </pre>
 *
 * <p>Construction is explicit: normal gameplay creates no instance and an
 * unowned section pays only the Mixin's null-owner branch. Topology application
 * is a second explicit opt-in and is never run automatically from tick sealing.</p>
 */
public final class MinecraftThermalInput implements AutoCloseable {
    private static final Map<ServerLevel, MinecraftThermalInput> ACTIVE_BY_LEVEL =
            new IdentityHashMap<>();
    private static volatile boolean anyActive;
    private static final int WITNESS_RADIUS_SECTIONS = 1;

    private final ServerLevel level;
    private final Thread mainThread;
    private final long dimensionGeneration;
    private final DimensionThermalRuntime runtime;
    private final ThermalSignatureResolverDispatcher resolverDispatcher;
    private final ThermalSignatureRegistry signatureRegistry;
    private final GeometryDeltaRing geometryDeltas;
    private final ResolvedGeometryInputRing resolvedInputs;
    private final Map<Long, ThermalPage> pages = new HashMap<>();
    private final Map<Long, Integer> physicalSourcePageRefCounts = new HashMap<>();
    private final Set<Long> physicalSourceOwnedPages = new HashSet<>();
    private final Map<Long, Integer> witnessRefCounts = new HashMap<>();
    private final Map<Long, SectionOwner> ownersBySectionKey = new HashMap<>();
    private final IdentityHashMap<LevelChunkSection, SectionOwner> ownersByIdentity =
            new IdentityHashMap<>();
    private final IdentityHashMap<ThermalPage, Boolean> dirtyPages = new IdentityHashMap<>();
    private final AtomicBoolean offThreadResyncPending = new AtomicBoolean();
    private final AtomicLong nextSectionGeneration = new AtomicLong();

    private long chunkWatermark;
    private final long profileWatermark;
    private long transitionAckWatermark;
    private long lastSealedTick;
    private volatile boolean closed;
    private MinecraftThermalTopologyApplier topologyApplier;
    private MinecraftPhysicalSourceManager physicalSources;
    private ThermalRuntimeCoordinator shadowCoordinator;
    private Executor shadowExecutor;
    private final AtomicReference<SealedInputFrame> pendingShadowFrame =
            new AtomicReference<>();
    private final AtomicBoolean shadowWorkerScheduled = new AtomicBoolean();
    private volatile ShadowReport latestShadowReport;

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
        requireMainThread();
        requireOpen();
        if (topologyApplier != null) {
            throw new IllegalStateException("Minecraft thermal topology application is enabled");
        }
        topologyApplier = new MinecraftThermalTopologyApplier(
                runtime, signatureRegistry, geometryDeltas, resolvedInputs, parameters);
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

    public ShadowReport latestShadowReport() {
        return latestShadowReport;
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

    MinecraftThermalTopologyApplier.PortResolution resolvePhysicalSourcePort(
            BlockPos target,
            com.teammoeg.frostedheart.content.climate.thermal.geometry
                    .ConservativeAirGeometry.Face targetFace
    ) {
        return topologyApplier.resolveAirFacePort(
                target.getX(), target.getY(), target.getZ(), targetFace);
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
        dirtyPages.clear();
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
        synchronized (ACTIVE_BY_LEVEL) {
            active = List.copyOf(ACTIVE_BY_LEVEL.values());
        }
        for (MinecraftThermalInput input : active) {
            input.close();
        }
    }

    public static void onRawBlockContainerReplaced(LevelChunkSection section) {
        SectionOwner owner = attachment(section).frostedheart$getThermalInputOwner();
        if (owner != null) {
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
        SectionOwner previous = input.ownersByIdentity.get(previousSection);
        if (previous != null) {
            previous.invalidateAffectedPages(ThermalPage.GeometryResyncReason.SECTION_REPLACED);
            input.detach(previous);
        }
        input.attachWitnessSection(
                chunk,
                sectionIndex,
                chunk.getSectionYFromSectionIndex(sectionIndex));
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
        Map<DependencyOffsetMask.Offset,
                ResolverBlockView.SnapshotCell<BlockState, FluidState>> cells =
                new LinkedHashMap<>();
        for (DependencyOffsetMask.Offset offset : plan.dependencyMask().offsets()) {
            cells.put(offset, cube.cell(
                    centerX + offset.x(),
                    centerY + offset.y(),
                    centerZ + offset.z()));
        }
        ThermalResolution<ResolvedThermalSignature> resolved = plan.resolve(
                ResolverBlockView.snapshot(plan.dependencyMask(), cells));
        if (!resolved.isResolved()) {
            return ThermalSignatureResolution.failure(resolved);
        }
        OptionalInt signatureId = signatureRegistry.idOf(resolved.value().orElseThrow());
        return signatureId.isPresent()
                ? ThermalSignatureResolution.resolved(signatureId.getAsInt())
                : ThermalSignatureResolution.failure(ThermalResolution.unsupported(
                        ThermalResolution.Reason.INVALID_RESOLVER_OUTPUT));
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
                        if (owner != null) {
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
            if (witnessRefCounts.containsKey(key)) {
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
        if (!witnessRefCounts.containsKey(key)) {
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

    private void scheduleShadowWorker() {
        if (closed || !shadowWorkerScheduled.compareAndSet(false, true)) {
            return;
        }
        try {
            shadowExecutor.execute(this::drainShadowFrames);
        } catch (RuntimeException exception) {
            shadowWorkerScheduled.set(false);
            SealedInputFrame rejected = pendingShadowFrame.get();
            latestShadowReport = new ShadowReport(
                    rejected == null ? lastSealedTick : rejected.effectiveTick(),
                    null,
                    null,
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
                MinecraftThermalTopologyApplier.ApplyReport topology =
                        topologyApplier.apply(frame);
                ThermalRuntimeCoordinator.RequestResult request = null;
                if (topology.readyForSolve()) {
                    request = shadowCoordinator.request(
                            runtime.runtimeId(),
                            dimensionGeneration,
                            false,
                            frame.effectiveTick());
                    while (shadowCoordinator.runNext(frame.effectiveTick()).status()
                            == ThermalRuntimeCoordinator.DispatchStatus.EXECUTED) {
                        // Drain the bounded coordinator on its shared worker.
                    }
                }
                latestShadowReport = new ShadowReport(
                        frame.effectiveTick(), topology, request, false);
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
