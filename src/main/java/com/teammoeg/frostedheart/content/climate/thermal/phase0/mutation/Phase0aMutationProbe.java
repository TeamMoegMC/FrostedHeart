/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.phase0.mutation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Isolated Phase 0a mutation/lifecycle instrumentation. The owning Mixin is only
 * applied by the GameTest run and this class never feeds production temperature gameplay.
 */
public final class Phase0aMutationProbe {
    public static final String ENABLE_PROPERTY = "frostedheart.phase0aMutationProbe";

    private static final boolean ENABLED = Boolean.getBoolean(ENABLE_PROPERTY);
    private static final AtomicLong NEXT_GENERATION = new AtomicLong();
    private static final AtomicLong NEXT_DELTA_SEQUENCE = new AtomicLong();
    private static final AtomicLong HOOK_CALLS = new AtomicLong();
    private static final AtomicLong UNMAPPED_WRITES = new AtomicLong();
    private static final AtomicLong STALE_WRITES = new AtomicLong();
    private static final AtomicLong OFF_THREAD_WRITES = new AtomicLong();
    private static final AtomicLong RAW_BYPASS_DETECTIONS = new AtomicLong();
    private static final AtomicLong RAW_BLOCK_CONTAINER_REPLACEMENTS = new AtomicLong();
    private static final AtomicLong RAW_BIOME_CONTAINER_REPLACEMENTS = new AtomicLong();
    private static final AtomicLong SECTION_IDENTITY_REPLACEMENTS = new AtomicLong();
    private static final AtomicLong LIFECYCLE_THREAD_VIOLATIONS = new AtomicLong();

    // These collections are deliberately main-thread-owned. Off-thread hooks only touch the attached owner atomics.
    private static final IdentityHashMap<LevelChunkSection, LoadedSectionOwner> SECTION_OWNERS =
            new IdentityHashMap<>();
    private static final Map<ChunkKey, LoadedChunkOwner> CHUNK_OWNERS = new HashMap<>();
    private static final Map<ResourceKey<Level>, DimensionState> DIMENSIONS = new HashMap<>();
    private static Thread mainThread;

    private Phase0aMutationProbe() {
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    /** Called only by the five-argument LevelChunkSection hook. */
    public static void onSectionSetBlockState(
            LevelChunkSection section, int x, int y, int z, BlockState oldState, BlockState newState) {
        if (!ENABLED || oldState == newState) {
            return;
        }

        HOOK_CALLS.incrementAndGet();
        Phase0aSectionAttachment sectionAttachment = attachment(section);
        LoadedSectionOwner attached = sectionAttachment.frostedheart$getPhase0aOwner();
        if (attached == null) {
            UNMAPPED_WRITES.incrementAndGet();
            sectionAttachment.frostedheart$incrementPhase0aUnmappedWrites();
            return;
        }
        if (!attached.isValid()) {
            STALE_WRITES.incrementAndGet();
            return;
        }
        if (Thread.currentThread() != attached.mainThread()) {
            OFF_THREAD_WRITES.incrementAndGet();
            attached.observeOffThreadMutation(localIndex(x, y, z), oldState, newState);
            return;
        }

        LoadedSectionOwner mapped = SECTION_OWNERS.get(section);
        if (mapped != attached || !mapped.isValid()) {
            STALE_WRITES.incrementAndGet();
            return;
        }

        long revision = mapped.observeMainThreadMutation(localIndex(x, y, z), oldState, newState);
        DimensionState dimension = DIMENSIONS.get(mapped.dimension());
        if (dimension == null) {
            mapped.requireFullResync(ResyncReason.OWNER_STATE_MISSING);
            return;
        }
        BlockPos worldPos = new BlockPos(
                SectionPos.sectionToBlockCoord(mapped.sectionX()) + x,
                SectionPos.sectionToBlockCoord(mapped.sectionY()) + y,
                SectionPos.sectionToBlockCoord(mapped.sectionZ()) + z);
        dimension.record(section, mapped, worldPos, oldState, newState, revision, dimension.level.getGameTime());
    }

    /** Explicit adapter for writers that replace a mapped section's raw block container data. */
    public static void onRawBlockContainerReplaced(LevelChunkSection section) {
        if (!ENABLED) {
            return;
        }
        LoadedSectionOwner owner = attachedOwnerForAdapter(section);
        if (owner != null) {
            RAW_BLOCK_CONTAINER_REPLACEMENTS.incrementAndGet();
            owner.requireFullResync(ResyncReason.RAW_BLOCK_CONTAINER_REPLACED);
        }
    }

    /** Explicit adapter for writers that replace biome data used by natural-temperature queries. */
    public static void onRawBiomeContainerReplaced(LevelChunkSection section) {
        if (!ENABLED) {
            return;
        }
        LoadedSectionOwner owner = attachedOwnerForAdapter(section);
        if (owner != null) {
            RAW_BIOME_CONTAINER_REPLACEMENTS.incrementAndGet();
            owner.requireFullResync(ResyncReason.RAW_BIOME_CONTAINER_REPLACED);
        }
    }

    /**
     * Rebinds ownership after a loaded chunk replaces one entry in its section array.
     * The caller must invoke this on the server thread immediately after installing the replacement.
     */
    public static void onSectionIdentityReplaced(
            ServerLevel level,
            LevelChunk chunk,
            int sectionIndex,
            LevelChunkSection previousSection,
            LevelChunkSection reportedReplacement) {
        if (!ENABLED || previousSection == reportedReplacement) {
            return;
        }
        requireMainThread(level);
        if (sectionIndex < 0 || sectionIndex >= chunk.getSections().length) {
            return;
        }

        ChunkKey key = new ChunkKey(level.dimension(), chunk.getPos().toLong());
        LoadedChunkOwner chunkOwner = CHUNK_OWNERS.get(key);
        if (chunkOwner == null || chunkOwner.chunk != chunk) {
            return;
        }

        LevelChunkSection replacement = chunk.getSections()[sectionIndex];
        LevelChunkSection trackedSection = chunkOwner.sectionIdentities[sectionIndex];
        LoadedSectionOwner trackedOwner = chunkOwner.sections[sectionIndex];
        if (replacement == trackedSection) {
            return;
        }
        boolean fingerprintInterest = trackedOwner.fingerprintActive();

        // Revoke the old identity before exposing the replacement to publication checks.
        trackedOwner.invalidate();
        SECTION_OWNERS.remove(trackedSection, trackedOwner);

        LoadedSectionOwner conflictingOwner = SECTION_OWNERS.get(replacement);
        if (conflictingOwner != null) {
            conflictingOwner.invalidate();
            SECTION_OWNERS.remove(replacement, conflictingOwner);
        }

        LoadedSectionOwner replacementOwner = new LoadedSectionOwner(
                level.dimension(),
                chunk.getPos().x,
                chunk.getSectionYFromSectionIndex(sectionIndex),
                chunk.getPos().z,
                chunkOwner.generation,
                Thread.currentThread());
        if (fingerprintInterest) {
            replacementOwner.activateFingerprint(computeFingerprint(replacement));
        }
        replacementOwner.requireFullResync(ResyncReason.SECTION_IDENTITY_REPLACED);
        chunkOwner.replaceSection(sectionIndex, replacement, replacementOwner);
        SECTION_OWNERS.put(replacement, replacementOwner);
        attachment(replacement).frostedheart$setPhase0aOwner(replacementOwner);
        SECTION_IDENTITY_REPLACEMENTS.incrementAndGet();
    }

    static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        if (!level.getServer().isSameThread()) {
            LIFECYCLE_THREAD_VIOLATIONS.incrementAndGet();
            level.getServer().execute(() -> registerLoadedChunk(level, chunk));
            return;
        }
        registerLoadedChunk(level, chunk);
    }

    static void onChunkUnload(ServerLevel level, LevelChunk chunk) {
        if (!level.getServer().isSameThread()) {
            LIFECYCLE_THREAD_VIOLATIONS.incrementAndGet();
            invalidateAttachedOwners(chunk);
            level.getServer().execute(() -> unregisterLoadedChunk(level, chunk));
            return;
        }
        unregisterLoadedChunk(level, chunk);
    }

    static long registerLoadedChunk(ServerLevel level, LevelChunk chunk) {
        requireMainThread(level);
        ResourceKey<Level> dimension = level.dimension();
        ChunkKey key = new ChunkKey(dimension, chunk.getPos().toLong());
        LoadedChunkOwner existing = CHUNK_OWNERS.get(key);
        if (existing != null && existing.chunk == chunk) {
            return existing.generation;
        }
        if (existing != null) {
            invalidateAndRemove(existing);
        }

        long generation = NEXT_GENERATION.incrementAndGet();
        DimensionState dimensionState = DIMENSIONS.computeIfAbsent(dimension, ignored -> new DimensionState(level));
        if (dimensionState.level != level) {
            throw new IllegalStateException("Phase 0a dimension identity changed without server-stop cleanup");
        }

        LevelChunkSection[] sectionIdentities = chunk.getSections().clone();
        LoadedSectionOwner[] sections = new LoadedSectionOwner[sectionIdentities.length];
        for (int index = 0; index < chunk.getSections().length; index++) {
            LevelChunkSection section = sectionIdentities[index];
            LoadedSectionOwner previous = SECTION_OWNERS.get(section);
            if (previous != null && previous.isValid()) {
                previous.invalidate();
            }
            LoadedSectionOwner owner = new LoadedSectionOwner(
                    dimension,
                    chunk.getPos().x,
                    chunk.getSectionYFromSectionIndex(index),
                    chunk.getPos().z,
                    generation,
                    Thread.currentThread());
            SECTION_OWNERS.put(section, owner);
            attachment(section).frostedheart$setPhase0aOwner(owner);
            sections[index] = owner;
        }
        CHUNK_OWNERS.put(key, new LoadedChunkOwner(chunk, generation, sectionIdentities, sections));
        return generation;
    }

    static void unregisterLoadedChunk(ServerLevel level, LevelChunk chunk) {
        requireMainThread(level);
        ChunkKey key = new ChunkKey(level.dimension(), chunk.getPos().toLong());
        LoadedChunkOwner owner = CHUNK_OWNERS.get(key);
        if (owner == null || owner.chunk != chunk) {
            return;
        }

        // Generation/publication validity is revoked before the authoritative identity map is changed.
        owner.invalidate();
        CHUNK_OWNERS.remove(key);
        for (int index = 0; index < owner.sectionIdentities.length; index++) {
            SECTION_OWNERS.remove(owner.sectionIdentities[index], owner.sections[index]);
        }
    }

    static void sealLevel(ServerLevel level) {
        if (!level.getServer().isSameThread()) {
            LIFECYCLE_THREAD_VIOLATIONS.incrementAndGet();
            return;
        }
        DimensionState state = DIMENSIONS.get(level.dimension());
        if (state != null) {
            state.seal();
        }
    }

    static void clearAfterServerStop() {
        if (mainThread != null && Thread.currentThread() != mainThread) {
            LIFECYCLE_THREAD_VIOLATIONS.incrementAndGet();
        }
        for (LoadedChunkOwner owner : CHUNK_OWNERS.values()) {
            owner.invalidate();
        }
        SECTION_OWNERS.clear();
        CHUNK_OWNERS.clear();
        DIMENSIONS.clear();
        mainThread = null;
    }

    static LoadedSectionOwner ownerFor(LevelChunkSection section) {
        requireMainThread();
        return SECTION_OWNERS.get(section);
    }

    static PublicationToken capturePublication(LevelChunkSection section) {
        LoadedSectionOwner owner = Objects.requireNonNull(ownerFor(section), "section is not mapped");
        return new PublicationToken(section, owner.lifecycleGeneration(), owner.liveRevision());
    }

    static boolean acceptsPublication(PublicationToken token) {
        requireMainThread();
        LoadedSectionOwner owner = SECTION_OWNERS.get(token.section());
        return owner != null
                && owner.isValid()
                && !owner.fullGeometryResyncRequired()
                && owner.lifecycleGeneration() == token.lifecycleGeneration()
                && owner.liveRevision() == token.liveRevision();
    }

    static void setFingerprintInterest(LevelChunkSection section, boolean active) {
        LoadedSectionOwner owner = Objects.requireNonNull(ownerFor(section), "section is not mapped");
        if (active) {
            owner.activateFingerprint(computeFingerprint(section));
        } else {
            owner.deactivateFingerprint();
        }
    }

    static FingerprintScanResult scanFingerprint(LevelChunkSection section) {
        LoadedSectionOwner owner = Objects.requireNonNull(ownerFor(section), "section is not mapped");
        if (!owner.fingerprintActive()) {
            return new FingerprintScanResult(0, 0);
        }
        long actual = computeFingerprint(section);
        if (owner.acceptFingerprint(actual)) {
            return new FingerprintScanResult(1, 0);
        }
        RAW_BYPASS_DETECTIONS.incrementAndGet();
        owner.observeRawPaletteBypass(actual);
        return new FingerprintScanResult(1, 1);
    }

    static FingerprintScanResult scanActiveFingerprints(int maxSections) {
        requireMainThread();
        if (maxSections < 0) {
            throw new IllegalArgumentException("maxSections must be non-negative");
        }
        int scanned = 0;
        int mismatches = 0;
        for (Map.Entry<LevelChunkSection, LoadedSectionOwner> entry : SECTION_OWNERS.entrySet()) {
            if (scanned >= maxSections) {
                break;
            }
            LoadedSectionOwner owner = entry.getValue();
            if (!owner.isValid() || !owner.fingerprintActive()) {
                continue;
            }
            scanned++;
            long actual = computeFingerprint(entry.getKey());
            if (!owner.acceptFingerprint(actual)) {
                mismatches++;
                RAW_BYPASS_DETECTIONS.incrementAndGet();
                owner.observeRawPaletteBypass(actual);
            }
        }
        return new FingerprintScanResult(scanned, mismatches);
    }

    static ResyncToken beginFullGeometryResync(LevelChunkSection section) {
        LoadedSectionOwner owner = ownerFor(section);
        if (owner == null || !owner.isValid()) {
            return null;
        }
        ResyncRequirement requirement = owner.resyncRequirement();
        return requirement == null ? null : new ResyncToken(
                section,
                owner.lifecycleGeneration(),
                requirement.requiredRevision(),
                requirement.reason());
    }

    static boolean acknowledgeFullGeometryResync(ResyncToken token) {
        Objects.requireNonNull(token, "token");
        LoadedSectionOwner owner = ownerFor(token.section());
        if (owner == null
                || !owner.isValid()
                || owner.lifecycleGeneration() != token.lifecycleGeneration()) {
            return false;
        }
        ResyncRequirement requirement = owner.resyncRequirement();
        if (requirement == null
                || requirement.requiredRevision() != token.requiredRevision()
                || requirement.reason() != token.reason()
                || !owner.clearFullResync(requirement)) {
            return false;
        }
        if (owner.fingerprintActive()) {
            owner.replaceExpectedFingerprint(computeFingerprint(token.section()));
        }
        return true;
    }

    static long deltaCursor() {
        return NEXT_DELTA_SEQUENCE.get();
    }

    static List<MutationDelta> sealedDeltasAfter(long cursor) {
        requireMainThread();
        List<MutationDelta> result = new ArrayList<>();
        for (DimensionState state : DIMENSIONS.values()) {
            for (MutationDelta delta : state.sealedDeltas) {
                if (delta.sequence() > cursor) {
                    result.add(delta);
                }
            }
        }
        result.sort(Comparator.comparingLong(MutationDelta::sequence));
        return List.copyOf(result);
    }

    static void resetDiagnosticsForBatch(ServerLevel level) {
        requireMainThread(level);
        for (DimensionState state : DIMENSIONS.values()) {
            state.clearDiagnostics();
        }
        HOOK_CALLS.set(0);
        UNMAPPED_WRITES.set(0);
        STALE_WRITES.set(0);
        OFF_THREAD_WRITES.set(0);
        RAW_BYPASS_DETECTIONS.set(0);
        RAW_BLOCK_CONTAINER_REPLACEMENTS.set(0);
        RAW_BIOME_CONTAINER_REPLACEMENTS.set(0);
        SECTION_IDENTITY_REPLACEMENTS.set(0);
        LIFECYCLE_THREAD_VIOLATIONS.set(0);
    }

    static long hookCalls() {
        return HOOK_CALLS.get();
    }

    static long unmappedWrites() {
        return UNMAPPED_WRITES.get();
    }

    static long unmappedWrites(LevelChunkSection section) {
        return attachment(section).frostedheart$getPhase0aUnmappedWrites();
    }

    static long staleWrites() {
        return STALE_WRITES.get();
    }

    static long offThreadWrites() {
        return OFF_THREAD_WRITES.get();
    }

    static long rawBypassDetections() {
        return RAW_BYPASS_DETECTIONS.get();
    }

    static long rawBlockContainerReplacements() {
        return RAW_BLOCK_CONTAINER_REPLACEMENTS.get();
    }

    static long rawBiomeContainerReplacements() {
        return RAW_BIOME_CONTAINER_REPLACEMENTS.get();
    }

    static long sectionIdentityReplacements() {
        return SECTION_IDENTITY_REPLACEMENTS.get();
    }

    static long lifecycleThreadViolations() {
        return LIFECYCLE_THREAD_VIOLATIONS.get();
    }

    private static void invalidateAttachedOwners(LevelChunk chunk) {
        for (LevelChunkSection section : chunk.getSections()) {
            LoadedSectionOwner owner = attachment(section).frostedheart$getPhase0aOwner();
            if (owner != null) {
                owner.invalidate();
            }
        }
    }

    private static void invalidateAndRemove(LoadedChunkOwner owner) {
        owner.invalidate();
        for (int index = 0; index < owner.sectionIdentities.length; index++) {
            SECTION_OWNERS.remove(owner.sectionIdentities[index], owner.sections[index]);
        }
    }

    private static LoadedSectionOwner attachedOwnerForAdapter(LevelChunkSection section) {
        LoadedSectionOwner attached = attachment(section).frostedheart$getPhase0aOwner();
        if (attached == null) {
            return null;
        }
        if (!attached.isValid()) {
            STALE_WRITES.incrementAndGet();
            return null;
        }
        if (Thread.currentThread() == attached.mainThread()) {
            LoadedSectionOwner mapped = SECTION_OWNERS.get(section);
            if (mapped != attached || !mapped.isValid()) {
                STALE_WRITES.incrementAndGet();
                return null;
            }
        }
        return attached;
    }

    private static Phase0aSectionAttachment attachment(LevelChunkSection section) {
        if (!(section instanceof Phase0aSectionAttachment attachment)) {
            throw new IllegalStateException(
                    "Phase 0a section Mixin is absent; run with -D" + ENABLE_PROPERTY + "=true");
        }
        return attachment;
    }

    private static long computeFingerprint(LevelChunkSection section) {
        long fingerprint = 0L;
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int index = (y << 8) | (z << 4) | x;
                    fingerprint ^= fingerprintContribution(index, section.getBlockState(x, y, z));
                }
            }
        }
        return fingerprint;
    }

    private static long fingerprintContribution(int index, BlockState state) {
        long value = ((long) index << 32) ^ Integer.toUnsignedLong(Block.getId(state));
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdl;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53l;
        return value ^ (value >>> 33);
    }

    private static int localIndex(int x, int y, int z) {
        return (y << 8) | (z << 4) | x;
    }

    private static void requireMainThread(ServerLevel level) {
        if (!level.getServer().isSameThread()) {
            throw new IllegalStateException("Phase 0a owner map accessed off the Minecraft main thread");
        }
        if (mainThread == null) {
            mainThread = Thread.currentThread();
        } else if (mainThread != Thread.currentThread()) {
            throw new IllegalStateException("Phase 0a owner map main-thread identity changed");
        }
    }

    private static void requireMainThread() {
        if (mainThread == null || Thread.currentThread() != mainThread) {
            throw new IllegalStateException("Phase 0a owner map accessed off the Minecraft main thread");
        }
    }

    public enum ResyncReason {
        OFF_THREAD_WRITE,
        RAW_PALETTE_BYPASS,
        RAW_BLOCK_CONTAINER_REPLACED,
        RAW_BIOME_CONTAINER_REPLACED,
        SECTION_IDENTITY_REPLACED,
        OWNER_STATE_MISSING
    }

    private record ResyncRequirement(ResyncReason reason, long requiredRevision) {
    }

    public static final class LoadedSectionOwner {
        private final ResourceKey<Level> dimension;
        private final int sectionX;
        private final int sectionY;
        private final int sectionZ;
        private final long lifecycleGeneration;
        private final Thread mainThread;
        private final AtomicLong liveRevision = new AtomicLong();
        private final AtomicBoolean valid = new AtomicBoolean(true);
        private final AtomicReference<ResyncRequirement> resyncRequirement = new AtomicReference<>();
        private final AtomicLong mainThreadMutationCount = new AtomicLong();
        private final AtomicLong expectedFingerprint = new AtomicLong();
        private volatile boolean fingerprintActive;

        private LoadedSectionOwner(
                ResourceKey<Level> dimension, int sectionX, int sectionY, int sectionZ,
                long lifecycleGeneration, Thread mainThread) {
            this.dimension = dimension;
            this.sectionX = sectionX;
            this.sectionY = sectionY;
            this.sectionZ = sectionZ;
            this.lifecycleGeneration = lifecycleGeneration;
            this.mainThread = mainThread;
        }

        public ResourceKey<Level> dimension() {
            return dimension;
        }

        public int sectionX() {
            return sectionX;
        }

        public int sectionY() {
            return sectionY;
        }

        public int sectionZ() {
            return sectionZ;
        }

        public long lifecycleGeneration() {
            return lifecycleGeneration;
        }

        public long liveRevision() {
            return liveRevision.get();
        }

        public long mainThreadMutationCount() {
            return mainThreadMutationCount.get();
        }

        public boolean isValid() {
            return valid.get();
        }

        public boolean fullGeometryResyncRequired() {
            return resyncRequirement.get() != null;
        }

        public ResyncReason resyncReason() {
            ResyncRequirement requirement = resyncRequirement.get();
            return requirement == null ? null : requirement.reason();
        }

        private Thread mainThread() {
            return mainThread;
        }

        private long observeMainThreadMutation(int index, BlockState oldState, BlockState newState) {
            mainThreadMutationCount.incrementAndGet();
            updateExpectedFingerprint(index, oldState, newState);
            return liveRevision.incrementAndGet();
        }

        private void observeOffThreadMutation(int index, BlockState oldState, BlockState newState) {
            updateExpectedFingerprint(index, oldState, newState);
            requireFullResync(ResyncReason.OFF_THREAD_WRITE);
        }

        private void observeRawPaletteBypass(long actualFingerprint) {
            expectedFingerprint.set(actualFingerprint);
            requireFullResync(ResyncReason.RAW_PALETTE_BYPASS);
        }

        private void requireFullResync(ResyncReason reason) {
            long revision = liveRevision.incrementAndGet();
            resyncRequirement.getAndUpdate(current -> new ResyncRequirement(
                    current == null ? reason : current.reason(), revision));
        }

        private ResyncRequirement resyncRequirement() {
            return resyncRequirement.get();
        }

        private boolean clearFullResync(ResyncRequirement requirement) {
            return resyncRequirement.compareAndSet(requirement, null);
        }

        private void invalidate() {
            valid.set(false);
            liveRevision.incrementAndGet();
        }

        private void activateFingerprint(long fingerprint) {
            expectedFingerprint.set(fingerprint);
            fingerprintActive = true;
        }

        private void deactivateFingerprint() {
            fingerprintActive = false;
        }

        private boolean fingerprintActive() {
            return fingerprintActive;
        }

        private boolean acceptFingerprint(long actual) {
            return expectedFingerprint.get() == actual;
        }

        private void replaceExpectedFingerprint(long actual) {
            expectedFingerprint.set(actual);
        }

        private void updateExpectedFingerprint(int index, BlockState oldState, BlockState newState) {
            if (!fingerprintActive) {
                return;
            }
            long delta = fingerprintContribution(index, oldState) ^ fingerprintContribution(index, newState);
            expectedFingerprint.getAndUpdate(current -> current ^ delta);
        }
    }

    public record PublicationToken(
            LevelChunkSection section, long lifecycleGeneration, long liveRevision) {
    }

    public record ResyncToken(
            LevelChunkSection section,
            long lifecycleGeneration,
            long requiredRevision,
            ResyncReason reason) {
        public ResyncToken {
            Objects.requireNonNull(section, "section");
            Objects.requireNonNull(reason, "reason");
        }
    }

    public record FingerprintScanResult(int scannedSections, int mismatches) {
    }

    public record MutationDelta(
            long sequence,
            LevelChunkSection section,
            BlockPos worldPos,
            BlockState oldState,
            BlockState newState,
            FluidState oldFluidState,
            FluidState newFluidState,
            long lifecycleGeneration,
            long liveRevision,
            long effectiveTick,
            long watermark) {
    }

    private record ChunkKey(ResourceKey<Level> dimension, long chunkPos) {
    }

    private record MutationKey(BlockPos pos, long generation) {
    }

    private static final class PendingMutation {
        private final LevelChunkSection section;
        private final BlockPos worldPos;
        private final BlockState oldState;
        private BlockState newState;
        private final long generation;
        private long liveRevision;

        private PendingMutation(
                LevelChunkSection section, BlockPos worldPos, BlockState oldState, BlockState newState,
                long generation, long liveRevision) {
            this.section = section;
            this.worldPos = worldPos;
            this.oldState = oldState;
            this.newState = newState;
            this.generation = generation;
            this.liveRevision = liveRevision;
        }
    }

    private static final class DimensionState {
        private final ServerLevel level;
        private final LinkedHashMap<MutationKey, PendingMutation> pending = new LinkedHashMap<>();
        private final List<MutationDelta> sealedDeltas = new ArrayList<>();
        private long pendingTick = Long.MIN_VALUE;
        private long nextWatermark;

        private DimensionState(ServerLevel level) {
            this.level = level;
        }

        private void record(
                LevelChunkSection section, LoadedSectionOwner owner, BlockPos pos,
                BlockState oldState, BlockState newState, long revision, long tick) {
            if (pendingTick != Long.MIN_VALUE && pendingTick != tick) {
                seal();
            }
            pendingTick = tick;
            MutationKey key = new MutationKey(pos.immutable(), owner.lifecycleGeneration());
            PendingMutation mutation = pending.get(key);
            if (mutation == null) {
                pending.put(key, new PendingMutation(
                        section, key.pos(), oldState, newState, owner.lifecycleGeneration(), revision));
            } else {
                mutation.newState = newState;
                mutation.liveRevision = revision;
                if (mutation.oldState == newState) {
                    pending.remove(key);
                }
            }
        }

        private void seal() {
            if (pending.isEmpty()) {
                pendingTick = Long.MIN_VALUE;
                return;
            }
            long watermark = ++nextWatermark;
            for (PendingMutation mutation : pending.values()) {
                sealedDeltas.add(new MutationDelta(
                        NEXT_DELTA_SEQUENCE.incrementAndGet(),
                        mutation.section,
                        mutation.worldPos,
                        mutation.oldState,
                        mutation.newState,
                        mutation.oldState.getFluidState(),
                        mutation.newState.getFluidState(),
                        mutation.generation,
                        mutation.liveRevision,
                        pendingTick,
                        watermark));
            }
            pending.clear();
            pendingTick = Long.MIN_VALUE;
        }

        private void clearDiagnostics() {
            pending.clear();
            sealedDeltas.clear();
            pendingTick = Long.MIN_VALUE;
            nextWatermark = 0L;
        }
    }

    private static final class LoadedChunkOwner {
        private final LevelChunk chunk;
        private final long generation;
        private final LevelChunkSection[] sectionIdentities;
        private final LoadedSectionOwner[] sections;

        private LoadedChunkOwner(
                LevelChunk chunk,
                long generation,
                LevelChunkSection[] sectionIdentities,
                LoadedSectionOwner[] sections) {
            this.chunk = chunk;
            this.generation = generation;
            this.sectionIdentities = sectionIdentities;
            this.sections = sections;
        }

        private void replaceSection(
                int sectionIndex, LevelChunkSection section, LoadedSectionOwner owner) {
            sectionIdentities[sectionIndex] = section;
            sections[sectionIndex] = owner;
        }

        private void invalidate() {
            for (LoadedSectionOwner owner : sections) {
                owner.invalidate();
            }
        }
    }
}
