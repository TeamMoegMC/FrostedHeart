/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.radiation;

import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.util.Objects;

/**
 * Main-thread, read-only receiver service for direct physical-source radiation.
 * Sources are indexed only in their origin section; bounded receiver queries
 * discover nearby buckets and never mutate a source ledger.
 */
public final class RadiationService implements AutoCloseable {
    public static final int RADIATION_BUDGET_LIMITED = 1;
    public static final int RADIATION_UNRESOLVED = 1 << 1;
    public static final long NO_SECTION_REVISION = Long.MIN_VALUE;
    private static final double FOUR_PI = 4.0D * Math.PI;
    private static final TraceStatus[] TRACE_STATUSES = TraceStatus.values();

    private final Thread ownerThread = Thread.currentThread();
    private final Parameters parameters;
    private final OcclusionTracer tracer;
    private final ThermalMemoryBudget.Reservation reservation;
    private final Long2ObjectOpenHashMap<Source> sources =
            new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<LongArrayList> sourcesBySection =
            new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<ReceiverCache> receiverCaches =
            new Long2ObjectOpenHashMap<>();
    private final Source[] candidateSources;
    private final double[] candidateUpperBounds;
    private final MutableTrace traceScratch;

    private long nextSourceRevision;
    private long sampleSequence;
    private long sourceAdmissionRefusals;
    private boolean closed;

    private RadiationService(
            Parameters parameters,
            OcclusionTracer tracer,
            ThermalMemoryBudget.Reservation reservation
    ) {
        this.parameters = parameters;
        this.tracer = tracer;
        this.reservation = reservation;
        candidateSources = new Source[parameters.maximumCandidatesPerReceiver()];
        candidateUpperBounds = new double[parameters.maximumCandidatesPerReceiver()];
        traceScratch = new MutableTrace(parameters.maximumWitnessSectionsPerRay());
    }

    /** Returns null when the dimension/server optional-memory cap refuses admission. */
    public static RadiationService tryCreate(
            Parameters parameters,
            OcclusionTracer tracer,
            ThermalMemoryBudget dimensionBudget
    ) {
        Objects.requireNonNull(parameters, "parameters");
        Objects.requireNonNull(tracer, "tracer");
        Objects.requireNonNull(dimensionBudget, "dimensionBudget");
        ThermalMemoryBudget.Reservation reservation = dimensionBudget.tryReserve(
                ThermalMemoryBudget.AllocationClass.OPTIONAL,
                projectedMaximumBytes(parameters));
        return reservation == null
                ? null : new RadiationService(parameters, tracer, reservation);
    }

    /** Conservative charged payload for all source, receiver, witness, and revision caps. */
    public static long projectedMaximumBytes(Parameters parameters) {
        Objects.requireNonNull(parameters, "parameters");
        long sourceBytes = Math.multiplyExact(parameters.maximumSources(), 160L);
        long bucketBytes = Math.multiplyExact(parameters.maximumSources(), 64L);
        long revisionBytes = Math.multiplyExact(
                parameters.maximumTrackedSections(), 48L);
        long witnessesPerReceiver = Math.multiplyExact(
                parameters.maximumCandidatesPerReceiver(), 3L);
        long witnessBytes = Math.addExact(
                80L,
                Math.multiplyExact(
                        parameters.maximumWitnessSectionsPerRay(), 2L * Long.BYTES));
        long receiverBytes = Math.multiplyExact(
                parameters.maximumReceivers(),
                Math.addExact(96L, Math.multiplyExact(witnessesPerReceiver, witnessBytes)));
        long scratchBytes = Math.addExact(
                Math.multiplyExact(parameters.maximumCandidatesPerReceiver(), 24L),
                Math.multiplyExact(parameters.maximumWitnessSectionsPerRay(), 16L));
        return Math.addExact(
                Math.addExact(sourceBytes, bucketBytes),
                Math.addExact(Math.addExact(revisionBytes, receiverBytes), scratchBytes));
    }

    /** Adds or replaces one radiative origin without touching emitted-energy state. */
    public boolean upsertSource(
            long sourceKey,
            int lifecycleGeneration,
            double originX,
            double originY,
            double originZ,
            double radiativePowerW,
            double directionalUpperBound
    ) {
        requireOwnerThread();
        requireOpen();
        if (lifecycleGeneration < 0) {
            throw new IllegalArgumentException("source generation must be non-negative");
        }
        requireFinite("originX", originX);
        requireFinite("originY", originY);
        requireFinite("originZ", originZ);
        requireNonNegativeFinite("radiativePowerW", radiativePowerW);
        requirePositiveFinite("directionalUpperBound", directionalUpperBound);
        if (radiativePowerW == 0.0D) {
            removeSource(sourceKey);
            return true;
        }

        long sectionKey = packSection(
                floorSection(originX), floorSection(originY), floorSection(originZ));
        Source source = sources.get(sourceKey);
        if (source == null) {
            if (sources.size() >= parameters.maximumSources()) {
                sourceAdmissionRefusals = Math.incrementExact(sourceAdmissionRefusals);
                return false;
            }
            source = new Source(sourceKey);
            sources.put(sourceKey, source);
        } else if (source.matches(
                lifecycleGeneration,
                originX, originY, originZ,
                radiativePowerW, directionalUpperBound)) {
            return true;
        } else {
            unindex(source);
        }
        source.lifecycleGeneration = lifecycleGeneration;
        nextSourceRevision = Math.incrementExact(nextSourceRevision);
        source.revision = nextSourceRevision;
        source.originX = originX;
        source.originY = originY;
        source.originZ = originZ;
        source.radiativePowerW = radiativePowerW;
        source.directionalUpperBound = directionalUpperBound;
        source.sectionKey = sectionKey;
        sourcesBySection.computeIfAbsent(
                sectionKey, ignored -> new LongArrayList()).add(sourceKey);
        return true;
    }

    public boolean removeSource(long sourceKey) {
        requireOwnerThread();
        requireOpen();
        Source removed = sources.remove(sourceKey);
        if (removed == null) {
            return false;
        }
        unindex(removed);
        return true;
    }

    /**
     * Samples deterministic feet/torso/head points. Repeating this call only
     * reads source state and may reuse LOS witnesses; it never consumes power.
     */
    public void samplePlayer(
            long receiverKey,
            int receiverGeneration,
            double receiverX,
            double receiverFeetY,
            double receiverZ,
            MutableSample out
    ) {
        requireOwnerThread();
        requireOpen();
        Objects.requireNonNull(out, "out").clear();
        if (receiverGeneration < 0) {
            throw new IllegalArgumentException("receiver generation must be non-negative");
        }
        requireFinite("receiverX", receiverX);
        requireFinite("receiverFeetY", receiverFeetY);
        requireFinite("receiverZ", receiverZ);
        if (sources.isEmpty()) {
            boolean sourceLimited = sourceAdmissionRefusals != 0L;
            out.finish(
                    0.0D,
                    sourceLimited ? 0.5F : 1.0F,
                    sourceLimited ? RADIATION_BUDGET_LIMITED : 0);
            return;
        }

        ReceiverCache cache = receiverCache(receiverKey, receiverGeneration);
        int candidateCount = 0;
        int candidateVisits = 0;
        boolean candidateLimited = false;
        double range = parameters.maximumRangeBlocks();
        int minimumSectionX = floorSection(receiverX - range);
        int maximumSectionX = floorSection(receiverX + range);
        int minimumSectionY = floorSection(
                receiverFeetY + parameters.feetOffsetBlocks() - range);
        int maximumSectionY = floorSection(
                receiverFeetY + parameters.headOffsetBlocks() + range);
        int minimumSectionZ = floorSection(receiverZ - range);
        int maximumSectionZ = floorSection(receiverZ + range);

        discovery:
        for (int y = minimumSectionY; y <= maximumSectionY; y++) {
            for (int z = minimumSectionZ; z <= maximumSectionZ; z++) {
                for (int x = minimumSectionX; x <= maximumSectionX; x++) {
                    LongArrayList bucket = sourcesBySection.get(packSection(x, y, z));
                    if (bucket == null) {
                        continue;
                    }
                    for (int index = 0; index < bucket.size(); index++) {
                        if (candidateVisits >= parameters.maximumCandidateVisits()) {
                            candidateLimited = true;
                            break discovery;
                        }
                        candidateVisits++;
                        Source source = sources.get(bucket.getLong(index));
                        if (source == null) {
                            continue;
                        }
                        double minimumDistanceSquared = minimumRayDistanceSquared(
                                source, receiverX, receiverFeetY, receiverZ);
                        if (minimumDistanceSquared
                                > parameters.maximumRangeBlocksSquared()) {
                            continue;
                        }
                        double upperBound = flux(
                                source,
                                Math.max(
                                        minimumDistanceSquared,
                                        parameters.minimumDistanceBlocksSquared()));
                        if (upperBound < parameters.minimumRadiantFluxWPerM2()) {
                            continue;
                        }
                        int insertion = insertionIndex(
                                source, upperBound, candidateCount);
                        if (candidateCount < candidateSources.length) {
                            shiftCandidates(insertion, candidateCount);
                            candidateSources[insertion] = source;
                            candidateUpperBounds[insertion] = upperBound;
                            candidateCount++;
                        } else {
                            candidateLimited = true;
                            if (insertion < candidateCount) {
                                shiftCandidates(insertion, candidateCount - 1);
                                candidateSources[insertion] = source;
                                candidateUpperBounds[insertion] = upperBound;
                            }
                        }
                    }
                }
            }
        }

        double totalFlux = 0.0D;
        int rays = 0;
        boolean rayLimited = false;
        boolean unresolved = false;
        rayLoop:
        for (int candidate = 0; candidate < candidateCount; candidate++) {
            Source source = candidateSources[candidate];
            for (int ray = 0; ray < 3; ray++) {
                if (rays >= parameters.maximumRaysPerReceiver()) {
                    rayLimited = true;
                    break rayLoop;
                }
                rays++;
                double targetY = receiverFeetY + parameters.receiverOffset(ray);
                int quarterX = floorQuarter(receiverX);
                int quarterY = floorQuarter(targetY);
                int quarterZ = floorQuarter(receiverZ);
                int witness = cache.find(
                        source.sourceKey,
                        source.revision,
                        ray,
                        quarterX,
                        quarterY,
                        quarterZ);
                TraceStatus status;
                if (witness >= 0 && cache.revisionsMatch(witness, tracer)) {
                    status = cache.status(witness);
                    cache.touch(witness, sampleSequence);
                } else {
                    traceScratch.clear();
                    tracer.trace(
                            source.originX, source.originY, source.originZ,
                            receiverX, targetY, receiverZ,
                            parameters.maximumDdaStepsPerRay(),
                            traceScratch);
                    status = traceScratch.status();
                    cache.store(
                            witness,
                            source.sourceKey,
                            source.revision,
                            ray,
                            quarterX,
                            quarterY,
                            quarterZ,
                            traceScratch,
                            sampleSequence);
                }
                if (status == TraceStatus.VISIBLE) {
                    double dx = receiverX - source.originX;
                    double dy = targetY - source.originY;
                    double dz = receiverZ - source.originZ;
                    double distanceSquared = Math.max(
                            dx * dx + dy * dy + dz * dz,
                            parameters.minimumDistanceBlocksSquared());
                    totalFlux = finiteSum(totalFlux, flux(source, distanceSquared) / 3.0D);
                } else if (status == TraceStatus.UNRESOLVED) {
                    unresolved = true;
                } else if (status == TraceStatus.BUDGET_LIMITED) {
                    rayLimited = true;
                }
            }
        }
        clearCandidateScratch(candidateCount);

        int flags = 0;
        float confidence = 1.0F;
        if (sourceAdmissionRefusals != 0L || candidateLimited || rayLimited) {
            flags |= RADIATION_BUDGET_LIMITED;
            confidence *= 0.5F;
        }
        if (unresolved) {
            flags |= RADIATION_UNRESOLVED;
            confidence *= 0.5F;
        }
        out.finish(
                totalFlux,
                confidence,
                flags);
        sampleSequence = Math.incrementExact(sampleSequence);
    }

    @Override
    public void close() {
        requireOwnerThread();
        if (closed) {
            return;
        }
        closed = true;
        sources.clear();
        sourcesBySection.clear();
        receiverCaches.clear();
        reservation.close();
    }

    private ReceiverCache receiverCache(long receiverKey, int receiverGeneration) {
        ReceiverCache cache = receiverCaches.get(receiverKey);
        if (cache != null) {
            if (cache.receiverGeneration != receiverGeneration) {
                cache.clear(receiverGeneration);
            }
            cache.lastSampleSequence = sampleSequence;
            return cache;
        }
        if (receiverCaches.size() >= parameters.maximumReceivers()) {
            long oldestKey = 0L;
            long oldestSequence = Long.MAX_VALUE;
            for (Long2ObjectMap.Entry<ReceiverCache> entry
                    : receiverCaches.long2ObjectEntrySet()) {
                if (entry.getValue().lastSampleSequence < oldestSequence) {
                    oldestKey = entry.getLongKey();
                    oldestSequence = entry.getValue().lastSampleSequence;
                }
            }
            receiverCaches.remove(oldestKey);
        }
        cache = new ReceiverCache(
                receiverGeneration,
                parameters.maximumCandidatesPerReceiver() * 3,
                parameters.maximumWitnessSectionsPerRay());
        cache.lastSampleSequence = sampleSequence;
        receiverCaches.put(receiverKey, cache);
        return cache;
    }

    private int insertionIndex(Source source, double upperBound, int count) {
        int index = 0;
        while (index < count) {
            int fluxOrder = Double.compare(upperBound, candidateUpperBounds[index]);
            if (fluxOrder > 0 || fluxOrder == 0
                    && Long.compareUnsigned(
                            source.sourceKey,
                            candidateSources[index].sourceKey) < 0) {
                break;
            }
            index++;
        }
        return index;
    }

    private void shiftCandidates(int insertion, int lastDestination) {
        for (int index = lastDestination; index > insertion; index--) {
            candidateSources[index] = candidateSources[index - 1];
            candidateUpperBounds[index] = candidateUpperBounds[index - 1];
        }
    }

    private void clearCandidateScratch(int count) {
        for (int index = 0; index < count; index++) {
            candidateSources[index] = null;
            candidateUpperBounds[index] = 0.0D;
        }
    }

    private double minimumRayDistanceSquared(
            Source source,
            double receiverX,
            double receiverFeetY,
            double receiverZ
    ) {
        double dx = receiverX - source.originX;
        double dz = receiverZ - source.originZ;
        double horizontal = dx * dx + dz * dz;
        double minimum = Double.POSITIVE_INFINITY;
        for (int ray = 0; ray < 3; ray++) {
            double dy = receiverFeetY + parameters.receiverOffset(ray) - source.originY;
            minimum = Math.min(minimum, horizontal + dy * dy);
        }
        return minimum;
    }

    private static double flux(Source source, double distanceSquared) {
        double result = source.directionalUpperBound * source.radiativePowerW
                / (FOUR_PI * distanceSquared);
        if (!Double.isFinite(result) || result < 0.0D) {
            throw new ArithmeticException("radiant flux exceeded the finite domain");
        }
        return result;
    }

    private void unindex(Source source) {
        LongArrayList bucket = sourcesBySection.get(source.sectionKey);
        if (bucket == null) {
            return;
        }
        bucket.rem(source.sourceKey);
        if (bucket.isEmpty()) {
            sourcesBySection.remove(source.sectionKey);
        }
    }

    private void requireOwnerThread() {
        if (Thread.currentThread() != ownerThread) {
            throw new IllegalStateException("radiation service is main-thread owned");
        }
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("radiation service is closed");
        }
    }

    private static int floorSection(double blockCoordinate) {
        double section = Math.floor(blockCoordinate / 16.0D);
        if (section < Integer.MIN_VALUE || section > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("radiation coordinate is out of section range");
        }
        return (int) section;
    }

    private static int floorQuarter(double blockCoordinate) {
        double quarter = Math.floor(blockCoordinate * 4.0D);
        if (quarter < Integer.MIN_VALUE || quarter > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("receiver coordinate is out of quarter range");
        }
        return (int) quarter;
    }

    public static long packSection(int sectionX, int sectionY, int sectionZ) {
        if (sectionX < -2_097_152 || sectionX > 2_097_151
                || sectionZ < -2_097_152 || sectionZ > 2_097_151
                || sectionY < -524_288 || sectionY > 524_287) {
            throw new IllegalArgumentException("radiation section coordinate is out of range");
        }
        return ((long) sectionX & 0x3f_ffffL) << 42
                | ((long) sectionZ & 0x3f_ffffL) << 20
                | ((long) sectionY & 0x0f_ffffL);
    }

    public static int sectionX(long packed) {
        return signExtend(packed >>> 42, 22);
    }

    public static int sectionY(long packed) {
        return signExtend(packed, 20);
    }

    public static int sectionZ(long packed) {
        return signExtend(packed >>> 20, 22);
    }

    private static int signExtend(long value, int bits) {
        int shift = Long.SIZE - bits;
        return (int) (value << shift >> shift);
    }

    private static double finiteSum(double first, double second) {
        double result = first + second;
        if (!Double.isFinite(result)) {
            throw new ArithmeticException("radiant flux sum exceeded the finite domain");
        }
        return result;
    }

    private static void requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }

    private static void requireNonNegativeFinite(String name, double value) {
        requireFinite(name, value);
        if (value < 0.0D) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static void requirePositiveFinite(String name, double value) {
        requireFinite(name, value);
        if (value <= 0.0D) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    public record Parameters(
            int maximumSources,
            int maximumTrackedSections,
            int maximumReceivers,
            int maximumCandidateVisits,
            int maximumCandidatesPerReceiver,
            int maximumRaysPerReceiver,
            int maximumWitnessSectionsPerRay,
            int maximumDdaStepsPerRay,
            double maximumRangeBlocks,
            double minimumRadiantFluxWPerM2,
            double minimumDistanceBlocks,
            double feetOffsetBlocks,
            double torsoOffsetBlocks,
            double headOffsetBlocks
    ) {
        public Parameters {
            if (maximumSources <= 0 || maximumTrackedSections <= 0
                    || maximumReceivers <= 0 || maximumCandidateVisits <= 0
                    || maximumCandidatesPerReceiver <= 0
                    || maximumRaysPerReceiver <= 0
                    || maximumWitnessSectionsPerRay <= 0
                    || maximumDdaStepsPerRay <= 0) {
                throw new IllegalArgumentException("radiation caps must be positive");
            }
            requirePositiveFinite("maximumRangeBlocks", maximumRangeBlocks);
            requireNonNegativeFinite(
                    "minimumRadiantFluxWPerM2", minimumRadiantFluxWPerM2);
            requirePositiveFinite("minimumDistanceBlocks", minimumDistanceBlocks);
            requireNonNegativeFinite("feetOffsetBlocks", feetOffsetBlocks);
            requireNonNegativeFinite("torsoOffsetBlocks", torsoOffsetBlocks);
            requireNonNegativeFinite("headOffsetBlocks", headOffsetBlocks);
            if (!(feetOffsetBlocks < torsoOffsetBlocks
                    && torsoOffsetBlocks < headOffsetBlocks)) {
                throw new IllegalArgumentException(
                        "receiver offsets must be ordered feet < torso < head");
            }
        }

        double maximumRangeBlocksSquared() {
            return maximumRangeBlocks * maximumRangeBlocks;
        }

        double minimumDistanceBlocksSquared() {
            return minimumDistanceBlocks * minimumDistanceBlocks;
        }

        double receiverOffset(int ray) {
            return switch (ray) {
                case 0 -> feetOffsetBlocks;
                case 1 -> torsoOffsetBlocks;
                case 2 -> headOffsetBlocks;
                default -> throw new IllegalArgumentException("ray index is out of range");
            };
        }
    }

    public enum TraceStatus {
        VISIBLE,
        BLOCKED,
        UNRESOLVED,
        BUDGET_LIMITED
    }

    /** Concrete adapters trace and expose revision-only cache validation here. */
    public interface OcclusionTracer {
        void trace(
                double sourceX,
                double sourceY,
                double sourceZ,
                double targetX,
                double targetY,
                double targetZ,
                int maximumSteps,
                MutableTrace result
        );

        long currentSectionRevision(long packedSectionKey);
    }

    /** Caller-owned trace result with a fixed witness-section cap. */
    public static final class MutableTrace {
        private final long[] sectionKeys;
        private final long[] sectionRevisions;
        private TraceStatus status = TraceStatus.BUDGET_LIMITED;
        private int sectionCount;

        public MutableTrace(int maximumSections) {
            if (maximumSections <= 0) {
                throw new IllegalArgumentException("maximumSections must be positive");
            }
            sectionKeys = new long[maximumSections];
            sectionRevisions = new long[maximumSections];
        }

        public void clear() {
            status = TraceStatus.BUDGET_LIMITED;
            sectionCount = 0;
        }

        public boolean addSection(long sectionKey, long revision) {
            if (sectionCount > 0 && sectionKeys[sectionCount - 1] == sectionKey) {
                return true;
            }
            if (revision == NO_SECTION_REVISION || sectionCount >= sectionKeys.length) {
                status = TraceStatus.BUDGET_LIMITED;
                return false;
            }
            sectionKeys[sectionCount] = sectionKey;
            sectionRevisions[sectionCount] = revision;
            sectionCount++;
            return true;
        }

        public void finish(TraceStatus nextStatus) {
            status = Objects.requireNonNull(nextStatus, "nextStatus");
        }

        public TraceStatus status() {
            return status;
        }

        public int sectionCount() {
            return sectionCount;
        }

        public long sectionKey(int index) {
            return sectionKeys[index];
        }

        public long sectionRevision(int index) {
            return sectionRevisions[index];
        }
    }

    public static final class MutableSample {
        private double radiantFluxWPerM2;
        private float confidence;
        private int flags;

        public double radiantFluxWPerM2() {
            return radiantFluxWPerM2;
        }

        public float confidence() {
            return confidence;
        }

        public int flags() {
            return flags;
        }

        private void clear() {
            radiantFluxWPerM2 = 0.0D;
            confidence = 0.0F;
            flags = 0;
        }

        private void finish(
                double nextFlux,
                float nextConfidence,
                int nextFlags
        ) {
            radiantFluxWPerM2 = nextFlux;
            confidence = nextConfidence;
            flags = nextFlags;
        }
    }

    private static final class Source {
        private final long sourceKey;
        private int lifecycleGeneration;
        private long revision;
        private double originX;
        private double originY;
        private double originZ;
        private double radiativePowerW;
        private double directionalUpperBound;
        private long sectionKey;

        private Source(long sourceKey) {
            this.sourceKey = sourceKey;
        }

        private boolean matches(
                int generation,
                double x,
                double y,
                double z,
                double power,
                double directionalBound
        ) {
            return lifecycleGeneration == generation
                    && Double.compare(originX, x) == 0
                    && Double.compare(originY, y) == 0
                    && Double.compare(originZ, z) == 0
                    && Double.compare(radiativePowerW, power) == 0
                    && Double.compare(directionalUpperBound, directionalBound) == 0;
        }
    }

    private static final class ReceiverCache {
        private int receiverGeneration;
        private long lastSampleSequence;
        private final byte[] live;
        private final long[] sourceKeys;
        private final long[] sourceRevisions;
        private final byte[] rayIndices;
        private final int[] quarterX;
        private final int[] quarterY;
        private final int[] quarterZ;
        private final byte[] statuses;
        private final int[] sectionCounts;
        private final long[] lastUsed;
        private final long[] sectionKeys;
        private final long[] sectionRevisions;
        private final int maximumSections;

        private ReceiverCache(
                int receiverGeneration,
                int maximumWitnesses,
                int maximumSections
        ) {
            this.receiverGeneration = receiverGeneration;
            this.maximumSections = maximumSections;
            live = new byte[maximumWitnesses];
            sourceKeys = new long[maximumWitnesses];
            sourceRevisions = new long[maximumWitnesses];
            rayIndices = new byte[maximumWitnesses];
            quarterX = new int[maximumWitnesses];
            quarterY = new int[maximumWitnesses];
            quarterZ = new int[maximumWitnesses];
            statuses = new byte[maximumWitnesses];
            sectionCounts = new int[maximumWitnesses];
            lastUsed = new long[maximumWitnesses];
            sectionKeys = new long[maximumWitnesses * maximumSections];
            sectionRevisions = new long[maximumWitnesses * maximumSections];
        }

        private void clear(int nextGeneration) {
            receiverGeneration = nextGeneration;
            for (int index = 0; index < live.length; index++) {
                live[index] = 0;
                sectionCounts[index] = 0;
            }
        }

        private int find(
                long sourceKey,
                long sourceRevision,
                int rayIndex,
                int nextQuarterX,
                int nextQuarterY,
                int nextQuarterZ
        ) {
            for (int index = 0; index < live.length; index++) {
                if (live[index] != 0
                        && sourceKeys[index] == sourceKey
                        && sourceRevisions[index] == sourceRevision
                        && Byte.toUnsignedInt(rayIndices[index]) == rayIndex
                        && quarterX[index] == nextQuarterX
                        && quarterY[index] == nextQuarterY
                        && quarterZ[index] == nextQuarterZ) {
                    return index;
                }
            }
            return -1;
        }

        private boolean revisionsMatch(int witness, OcclusionTracer tracer) {
            int offset = witness * maximumSections;
            for (int index = 0; index < sectionCounts[witness]; index++) {
                if (tracer.currentSectionRevision(sectionKeys[offset + index])
                        != sectionRevisions[offset + index]) {
                    return false;
                }
            }
            return true;
        }

        private TraceStatus status(int witness) {
            return TRACE_STATUSES[Byte.toUnsignedInt(statuses[witness])];
        }

        private void touch(int witness, long sequence) {
            lastUsed[witness] = sequence;
        }

        private void store(
                int preferred,
                long sourceKey,
                long sourceRevision,
                int rayIndex,
                int nextQuarterX,
                int nextQuarterY,
                int nextQuarterZ,
                MutableTrace trace,
                long sequence
        ) {
            int witness = preferred >= 0 ? preferred : replacementSlot();
            live[witness] = 1;
            sourceKeys[witness] = sourceKey;
            sourceRevisions[witness] = sourceRevision;
            rayIndices[witness] = (byte) rayIndex;
            quarterX[witness] = nextQuarterX;
            quarterY[witness] = nextQuarterY;
            quarterZ[witness] = nextQuarterZ;
            statuses[witness] = (byte) trace.status().ordinal();
            sectionCounts[witness] = trace.sectionCount();
            lastUsed[witness] = sequence;
            int offset = witness * maximumSections;
            for (int index = 0; index < trace.sectionCount(); index++) {
                sectionKeys[offset + index] = trace.sectionKey(index);
                sectionRevisions[offset + index] = trace.sectionRevision(index);
            }
        }

        private int replacementSlot() {
            int oldest = 0;
            long oldestSequence = Long.MAX_VALUE;
            for (int index = 0; index < live.length; index++) {
                if (live[index] == 0) {
                    return index;
                }
                if (lastUsed[index] < oldestSequence) {
                    oldest = index;
                    oldestSequence = lastUsed[index];
                }
            }
            return oldest;
        }
    }
}
