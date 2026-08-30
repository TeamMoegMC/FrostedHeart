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

import java.util.Objects;

/** Bounded main-thread direct-radiation query and receiver witness cache. */
public final class RadiationService implements AutoCloseable {
    public static final long NO_SECTION_REVISION = Long.MIN_VALUE;
    private static final double FOUR_PI = 4.0D * Math.PI;
    private static final TraceStatus[] TRACE_STATUSES = TraceStatus.values();

    private final Thread ownerThread = Thread.currentThread();
    private final Parameters parameters;
    private final SourceIndex sources;
    private final OcclusionTracer tracer;
    private final ThermalMemoryBudget.Reservation reservation;
    private final Long2ObjectOpenHashMap<ReceiverCache> receiverCaches =
            new Long2ObjectOpenHashMap<>();
    private final long[] candidateKeys;
    private final long[] candidateRevisions;
    private final double[] candidateX;
    private final double[] candidateY;
    private final double[] candidateZ;
    private final double[] candidatePower;
    private final double[] candidateDirectionalBound;
    private final double[] candidateUpperBounds;
    private final MutableTrace traceScratch;
    private final SourceVisitor sourceVisitor = this::visitSource;

    private double discoveryX;
    private double discoveryFeetY;
    private double discoveryZ;
    private int candidateCount;
    private int candidateVisits;
    private boolean candidateLimited;
    private long sampleSequence;
    private boolean closed;

    private RadiationService(
            Parameters parameters,
            SourceIndex sources,
            OcclusionTracer tracer,
            ThermalMemoryBudget.Reservation reservation
    ) {
        this.parameters = parameters;
        this.sources = sources;
        this.tracer = tracer;
        this.reservation = reservation;
        int candidates = parameters.maximumCandidatesPerReceiver();
        candidateKeys = new long[candidates];
        candidateRevisions = new long[candidates];
        candidateX = new double[candidates];
        candidateY = new double[candidates];
        candidateZ = new double[candidates];
        candidatePower = new double[candidates];
        candidateDirectionalBound = new double[candidates];
        candidateUpperBounds = new double[candidates];
        traceScratch = new MutableTrace(
                parameters.maximumWitnessSectionsPerRay());
    }

    public static RadiationService tryCreate(
            Parameters parameters,
            SourceIndex sources,
            OcclusionTracer tracer,
            ThermalMemoryBudget dimensionBudget
    ) {
        Objects.requireNonNull(parameters, "parameters");
        Objects.requireNonNull(sources, "sources");
        Objects.requireNonNull(tracer, "tracer");
        Objects.requireNonNull(dimensionBudget, "dimensionBudget");
        ThermalMemoryBudget.Reservation reservation = dimensionBudget.tryReserve(
                projectedMaximumBytes(parameters));
        return reservation == null ? null : new RadiationService(
                parameters, sources, tracer, reservation);
    }

    public static long projectedMaximumBytes(Parameters parameters) {
        Objects.requireNonNull(parameters, "parameters");
        long revisionBytes = Math.multiplyExact(
                parameters.maximumTrackedSections(), 48L);
        long witnessesPerReceiver = Math.multiplyExact(
                parameters.maximumCandidatesPerReceiver(), 3L);
        long witnessBytes = Math.addExact(
                80L,
                Math.multiplyExact(
                        parameters.maximumWitnessSectionsPerRay(),
                        2L * Long.BYTES));
        long receiverBytes = Math.multiplyExact(
                parameters.maximumReceivers(),
                Math.addExact(
                        96L,
                        Math.multiplyExact(
                                witnessesPerReceiver, witnessBytes)));
        long scratchBytes = Math.addExact(
                Math.multiplyExact(
                        parameters.maximumCandidatesPerReceiver(), 64L),
                Math.multiplyExact(
                        parameters.maximumWitnessSectionsPerRay(), 16L));
        return Math.addExact(
                revisionBytes,
                Math.addExact(receiverBytes, scratchBytes));
    }

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
            throw new IllegalArgumentException(
                    "receiver generation must be non-negative");
        }
        requireFinite("receiverX", receiverX);
        requireFinite("receiverFeetY", receiverFeetY);
        requireFinite("receiverZ", receiverZ);
        ReceiverCache cache = receiverCache(
                receiverKey, receiverGeneration);
        discover(receiverX, receiverFeetY, receiverZ);
        double totalFlux = 0.0D;
        int rays = 0;
        rayLoop:
        for (int candidate = 0;
             candidate < candidateCount;
             candidate++) {
            for (int ray = 0; ray < 3; ray++) {
                if (rays++ >= parameters.maximumRaysPerReceiver()) {
                    break rayLoop;
                }
                double targetY = receiverFeetY
                        + parameters.receiverOffset(ray);
                int quarterX = floorQuarter(receiverX);
                int quarterY = floorQuarter(targetY);
                int quarterZ = floorQuarter(receiverZ);
                int witness = cache.find(
                        candidateKeys[candidate],
                        candidateRevisions[candidate],
                        ray, quarterX, quarterY, quarterZ);
                TraceStatus status;
                if (witness >= 0
                        && cache.revisionsMatch(witness, tracer)) {
                    status = cache.status(witness);
                    cache.touch(witness, sampleSequence);
                } else {
                    traceScratch.clear();
                    tracer.trace(
                            candidateX[candidate],
                            candidateY[candidate],
                            candidateZ[candidate],
                            receiverX,
                            targetY,
                            receiverZ,
                            parameters.maximumDdaStepsPerRay(),
                            traceScratch);
                    status = traceScratch.status();
                    cache.store(
                            witness,
                            candidateKeys[candidate],
                            candidateRevisions[candidate],
                            ray, quarterX, quarterY, quarterZ,
                            traceScratch, sampleSequence);
                }
                if (status == TraceStatus.VISIBLE) {
                    double dx = receiverX - candidateX[candidate];
                    double dy = targetY - candidateY[candidate];
                    double dz = receiverZ - candidateZ[candidate];
                    double distanceSquared = Math.max(
                            dx * dx + dy * dy + dz * dz,
                            parameters.minimumDistanceBlocksSquared());
                    totalFlux = finiteSum(
                            totalFlux,
                            flux(
                                    candidatePower[candidate],
                                    candidateDirectionalBound[candidate],
                                    distanceSquared) / 3.0D);
                }
            }
        }
        out.finish(totalFlux);
        sampleSequence = Math.incrementExact(sampleSequence);
    }

    private void discover(double x, double feetY, double z) {
        discoveryX = x;
        discoveryFeetY = feetY;
        discoveryZ = z;
        candidateCount = 0;
        candidateVisits = 0;
        candidateLimited = false;
        double range = parameters.maximumRangeBlocks();
        int minX = floorSection(x - range);
        int maxX = floorSection(x + range);
        int minY = floorSection(
                feetY + parameters.feetOffsetBlocks() - range);
        int maxY = floorSection(
                feetY + parameters.headOffsetBlocks() + range);
        int minZ = floorSection(z - range);
        int maxZ = floorSection(z + range);
        discovery:
        for (int sectionY = minY; sectionY <= maxY; sectionY++) {
            for (int sectionZ = minZ; sectionZ <= maxZ; sectionZ++) {
                for (int sectionX = minX;
                     sectionX <= maxX;
                     sectionX++) {
                    sources.visitSection(
                            sectionX, sectionY, sectionZ, sourceVisitor);
                    if (candidateLimited
                            && candidateVisits
                                    >= parameters.maximumCandidateVisits()) {
                        break discovery;
                    }
                }
            }
        }
    }

    private boolean visitSource(
            long sourceKey,
            long sourceRevision,
            double sourceX,
            double sourceY,
            double sourceZ,
            double radiativePowerW,
            double directionalUpperBound
    ) {
        if (candidateVisits++ >= parameters.maximumCandidateVisits()) {
            candidateLimited = true;
            return false;
        }
        double minimumDistance = minimumRayDistanceSquared(
                sourceX, sourceY, sourceZ,
                discoveryX, discoveryFeetY, discoveryZ);
        if (minimumDistance > parameters.maximumRangeBlocksSquared()) {
            return true;
        }
        double upperBound = flux(
                radiativePowerW,
                directionalUpperBound,
                Math.max(
                        minimumDistance,
                        parameters.minimumDistanceBlocksSquared()));
        if (upperBound < parameters.minimumRadiantFluxWPerM2()) {
            return true;
        }
        int insertion = insertionIndex(
                sourceKey, upperBound, candidateCount);
        if (candidateCount < candidateKeys.length) {
            shiftCandidates(insertion, candidateCount);
            writeCandidate(
                    insertion,
                    sourceKey,
                    sourceRevision,
                    sourceX, sourceY, sourceZ,
                    radiativePowerW,
                    directionalUpperBound,
                    upperBound);
            candidateCount++;
        } else {
            candidateLimited = true;
            if (insertion < candidateCount) {
                shiftCandidates(insertion, candidateCount - 1);
                writeCandidate(
                        insertion,
                        sourceKey,
                        sourceRevision,
                        sourceX, sourceY, sourceZ,
                        radiativePowerW,
                        directionalUpperBound,
                        upperBound);
            }
        }
        return true;
    }

    private void writeCandidate(
            int index,
            long key,
            long revision,
            double x,
            double y,
            double z,
            double power,
            double directionalBound,
            double upperBound
    ) {
        candidateKeys[index] = key;
        candidateRevisions[index] = revision;
        candidateX[index] = x;
        candidateY[index] = y;
        candidateZ[index] = z;
        candidatePower[index] = power;
        candidateDirectionalBound[index] = directionalBound;
        candidateUpperBounds[index] = upperBound;
    }

    private int insertionIndex(long key, double upperBound, int count) {
        int index = 0;
        while (index < count) {
            int fluxOrder = Double.compare(
                    upperBound, candidateUpperBounds[index]);
            if (fluxOrder > 0 || fluxOrder == 0
                    && Long.compareUnsigned(key, candidateKeys[index]) < 0) {
                break;
            }
            index++;
        }
        return index;
    }

    private void shiftCandidates(int insertion, int lastDestination) {
        for (int index = lastDestination; index > insertion; index--) {
            candidateKeys[index] = candidateKeys[index - 1];
            candidateRevisions[index] = candidateRevisions[index - 1];
            candidateX[index] = candidateX[index - 1];
            candidateY[index] = candidateY[index - 1];
            candidateZ[index] = candidateZ[index - 1];
            candidatePower[index] = candidatePower[index - 1];
            candidateDirectionalBound[index] =
                    candidateDirectionalBound[index - 1];
            candidateUpperBounds[index] = candidateUpperBounds[index - 1];
        }
    }

    private ReceiverCache receiverCache(
            long receiverKey,
            int receiverGeneration
    ) {
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

    private double minimumRayDistanceSquared(
            double sourceX,
            double sourceY,
            double sourceZ,
            double receiverX,
            double receiverFeetY,
            double receiverZ
    ) {
        double dx = receiverX - sourceX;
        double dz = receiverZ - sourceZ;
        double horizontal = dx * dx + dz * dz;
        double minimum = Double.POSITIVE_INFINITY;
        for (int ray = 0; ray < 3; ray++) {
            double dy = receiverFeetY
                    + parameters.receiverOffset(ray) - sourceY;
            minimum = Math.min(minimum, horizontal + dy * dy);
        }
        return minimum;
    }

    private static double flux(
            double power,
            double directionalBound,
            double distanceSquared
    ) {
        double result = directionalBound * power
                / (FOUR_PI * distanceSquared);
        if (!Double.isFinite(result) || result < 0.0D) {
            throw new ArithmeticException(
                    "radiant flux exceeded the finite domain");
        }
        return result;
    }

    @Override
    public void close() {
        requireOwnerThread();
        if (closed) {
            return;
        }
        closed = true;
        receiverCaches.clear();
        reservation.close();
    }

    private void requireOwnerThread() {
        if (Thread.currentThread() != ownerThread) {
            throw new IllegalStateException(
                    "radiation service is main-thread owned");
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
            throw new IllegalArgumentException(
                    "radiation coordinate is out of section range");
        }
        return (int) section;
    }

    private static int floorQuarter(double blockCoordinate) {
        double quarter = Math.floor(blockCoordinate * 4.0D);
        if (quarter < Integer.MIN_VALUE || quarter > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "receiver coordinate is out of quarter range");
        }
        return (int) quarter;
    }

    public static long packSection(int x, int y, int z) {
        if (x < -2_097_152 || x > 2_097_151
                || z < -2_097_152 || z > 2_097_151
                || y < -524_288 || y > 524_287) {
            throw new IllegalArgumentException(
                    "radiation section coordinate is out of range");
        }
        return ((long) x & 0x3f_ffffL) << 42
                | ((long) z & 0x3f_ffffL) << 20
                | ((long) y & 0x0f_ffffL);
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
            throw new ArithmeticException(
                    "radiant flux sum exceeded the finite domain");
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
            if (maximumTrackedSections <= 0 || maximumReceivers <= 0
                    || maximumCandidateVisits <= 0
                    || maximumCandidatesPerReceiver <= 0
                    || maximumRaysPerReceiver <= 0
                    || maximumWitnessSectionsPerRay <= 0
                    || maximumDdaStepsPerRay <= 0) {
                throw new IllegalArgumentException(
                        "radiation caps must be positive");
            }
            requirePositiveFinite(
                    "maximumRangeBlocks", maximumRangeBlocks);
            requireNonNegativeFinite(
                    "minimumRadiantFluxWPerM2",
                    minimumRadiantFluxWPerM2);
            requirePositiveFinite(
                    "minimumDistanceBlocks", minimumDistanceBlocks);
            requireNonNegativeFinite("feetOffsetBlocks", feetOffsetBlocks);
            requireNonNegativeFinite("torsoOffsetBlocks", torsoOffsetBlocks);
            requireNonNegativeFinite("headOffsetBlocks", headOffsetBlocks);
            if (!(feetOffsetBlocks < torsoOffsetBlocks
                    && torsoOffsetBlocks < headOffsetBlocks)) {
                throw new IllegalArgumentException(
                        "receiver offsets must be ordered");
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
                default -> throw new IllegalArgumentException(
                        "ray index is out of range");
            };
        }
    }

    public interface SourceIndex {
        void visitSection(
                int sectionX,
                int sectionY,
                int sectionZ,
                SourceVisitor visitor);
    }

    public interface SourceVisitor {
        boolean visit(
                long sourceKey,
                long sourceRevision,
                double sourceX,
                double sourceY,
                double sourceZ,
                double radiativePowerW,
                double directionalUpperBound);
    }

    public enum TraceStatus {
        VISIBLE,
        BLOCKED,
        UNRESOLVED,
        BUDGET_LIMITED
    }

    public interface OcclusionTracer {
        void trace(
                double sourceX,
                double sourceY,
                double sourceZ,
                double targetX,
                double targetY,
                double targetZ,
                int maximumSteps,
                MutableTrace result);

        long currentSectionRevision(long packedSectionKey);
    }

    public static final class MutableTrace {
        private final long[] sectionKeys;
        private final long[] sectionRevisions;
        private TraceStatus status = TraceStatus.BUDGET_LIMITED;
        private int sectionCount;

        public MutableTrace(int maximumSections) {
            if (maximumSections <= 0) {
                throw new IllegalArgumentException(
                        "maximumSections must be positive");
            }
            sectionKeys = new long[maximumSections];
            sectionRevisions = new long[maximumSections];
        }

        public void clear() {
            status = TraceStatus.BUDGET_LIMITED;
            sectionCount = 0;
        }

        public boolean addSection(long key, long revision) {
            if (sectionCount > 0
                    && sectionKeys[sectionCount - 1] == key) {
                return true;
            }
            if (revision == NO_SECTION_REVISION
                    || sectionCount >= sectionKeys.length) {
                status = TraceStatus.BUDGET_LIMITED;
                return false;
            }
            sectionKeys[sectionCount] = key;
            sectionRevisions[sectionCount++] = revision;
            return true;
        }

        public void finish(TraceStatus status) {
            this.status = Objects.requireNonNull(status, "status");
        }

        public TraceStatus status() { return status; }
        public int sectionCount() { return sectionCount; }
        public long sectionKey(int index) { return sectionKeys[index]; }
        public long sectionRevision(int index) {
            return sectionRevisions[index];
        }
    }

    public static final class MutableSample {
        private double radiantFluxWPerM2;

        public double radiantFluxWPerM2() { return radiantFluxWPerM2; }

        private void clear() {
            radiantFluxWPerM2 = 0.0D;
        }

        private void finish(double flux) {
            radiantFluxWPerM2 = flux;
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
            sectionRevisions =
                    new long[maximumWitnesses * maximumSections];
        }

        private void clear(int generation) {
            receiverGeneration = generation;
            for (int index = 0; index < live.length; index++) {
                live[index] = 0;
                sectionCounts[index] = 0;
            }
        }

        private int find(
                long sourceKey,
                long sourceRevision,
                int ray,
                int x,
                int y,
                int z
        ) {
            for (int index = 0; index < live.length; index++) {
                if (live[index] != 0
                        && sourceKeys[index] == sourceKey
                        && sourceRevisions[index] == sourceRevision
                        && Byte.toUnsignedInt(rayIndices[index]) == ray
                        && quarterX[index] == x
                        && quarterY[index] == y
                        && quarterZ[index] == z) {
                    return index;
                }
            }
            return -1;
        }

        private boolean revisionsMatch(
                int witness,
                OcclusionTracer tracer
        ) {
            int offset = witness * maximumSections;
            for (int index = 0; index < sectionCounts[witness]; index++) {
                if (tracer.currentSectionRevision(
                        sectionKeys[offset + index])
                        != sectionRevisions[offset + index]) {
                    return false;
                }
            }
            return true;
        }

        private TraceStatus status(int witness) {
            return TRACE_STATUSES[
                    Byte.toUnsignedInt(statuses[witness])];
        }

        private void touch(int witness, long sequence) {
            lastUsed[witness] = sequence;
        }

        private void store(
                int preferred,
                long sourceKey,
                long sourceRevision,
                int ray,
                int x,
                int y,
                int z,
                MutableTrace trace,
                long sequence
        ) {
            int witness = preferred >= 0
                    ? preferred : replacementSlot();
            live[witness] = 1;
            sourceKeys[witness] = sourceKey;
            sourceRevisions[witness] = sourceRevision;
            rayIndices[witness] = (byte) ray;
            quarterX[witness] = x;
            quarterY[witness] = y;
            quarterZ[witness] = z;
            statuses[witness] = (byte) trace.status().ordinal();
            sectionCounts[witness] = trace.sectionCount();
            lastUsed[witness] = sequence;
            int offset = witness * maximumSections;
            for (int index = 0; index < trace.sectionCount(); index++) {
                sectionKeys[offset + index] = trace.sectionKey(index);
                sectionRevisions[offset + index] =
                        trace.sectionRevision(index);
            }
        }

        private int replacementSlot() {
            int oldest = 0;
            long sequence = Long.MAX_VALUE;
            for (int index = 0; index < live.length; index++) {
                if (live[index] == 0) {
                    return index;
                }
                if (lastUsed[index] < sequence) {
                    oldest = index;
                    sequence = lastUsed[index];
                }
            }
            return oldest;
        }
    }
}
