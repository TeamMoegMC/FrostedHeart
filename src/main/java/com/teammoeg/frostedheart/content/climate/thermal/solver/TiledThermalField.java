/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirRouteValidity;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockBrickLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialBoundaryRegistry;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialThermalLaw;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.DormantThermalCooling;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.AirFieldCheckpoint;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.source.ThermalHeatStep;
import com.teammoeg.frostedheart.content.climate.thermal.source.AirMixingRegion;
import com.teammoeg.frostedheart.content.climate.thermal.topology.AirCornerTopology;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

/**
 * Fixed-space finite volumes: each Air/material cell owns one enthalpy.
 * Faces exchange equal and opposite energy; passage traces have no thermal mass.
 * Accepted stays readable while the sole worker advances a private frame.
 * Geometry edits touch one Section, without a connected-region field rebuild.
 * The original engine owns input, publication and native Chunk persistence.
 */
public final class TiledThermalField implements AutoCloseable {
    private static final int CELLS = 4096;
    private static final int QUERY_CACHE_CELLS = 128;
    private static final int QUERY_CACHE_TABLE = it.unimi.dsi.fastutil.HashCommon.arraySize(QUERY_CACHE_CELLS, .75f) + 1;
    private static final long DIRECTORY_BASE_BYTES = 2176 + AirBackingMeter.arrayPayloadBytes(81, 8)
            + AirBackingMeter.arrayPayloadBytes(QUERY_CACHE_CELLS, 8)
            + AirBackingMeter.arrayPayloadBytes(8 * QUERY_CACHE_CELLS, 8)
            + AirBackingMeter.arrayPayloadBytes(QUERY_CACHE_TABLE, 8)
            + AirBackingMeter.arrayPayloadBytes(QUERY_CACHE_TABLE, 4);
    private static final int IDLE = 0, PREPARE = 1, FIRST_INPUT = 2, FORWARD = 3,
            REVERSE = 4, SECOND_INPUT = 5, READY = 6, FORWARD_TRANSPORT = 7, REVERSE_TRANSPORT = 8, BUOYANCY = 9;
    private static final int[] CELL_STEPS = {1, 256, 16};
    private static final int[] CELL_SHIFTS = {0, 8, 4};

    /** Replacement resets matter; thermal conversion keeps H and gameplay conversion keeps temperature. */
    public enum Change { PHYSICAL, REPLACEMENT, PHASE, GAMEPLAY }

    public static final class Sample {
        private double temperatureC, enthalpyJ;
        private MaterialThermalLaw law;
        private long sampleTick;
        private long requestSequence;
        private int materialStateId;
        private int materialGeneration;
        private byte branch;
        public double temperatureC() { return temperatureC; }
        public double enthalpyJ() { return enthalpyJ; }
        public MaterialThermalLaw law() { return law; }
        public long sampleTick() { return sampleTick; }
        public long requestSequence() { return requestSequence; }
        public int materialStateId() { return materialStateId; }
        public int materialGeneration() { return materialGeneration; }
        public byte branch() { return branch; }
    }

    /** Caller-owned save buffer. No frame, builder, runtime, or world reference escapes. */
    public static final class SectionSnapshot {
        public long section, sampleTick, activeBricks;
        public long geometryRevision = -1;
        public double ambientC;
        public final int[] signatures = new int[CELLS];
        public final int[] materialGenerations = new int[CELLS];
        public final double[] enthalpyJ = new double[CELLS];
        public final byte[] branches = new byte[CELLS];
    }

    private static final class Frame {
        final int[] signatures, materialGenerations;
        final double[] h;
        final byte[] branches;
        final long[] waiting;
        long[] requests;
        Frame(int count) {
            signatures = new int[count]; materialGenerations = new int[count];
            h = new double[count]; branches = new byte[count]; waiting = new long[(count + 63) / 64];
        }
    }

    /** A passage samples cell means; its weights own no additional energy. */
    private static final class RouteSide {
        final long[] positions;
        final double[] weights;
        final MaterialThermalLaw effectiveAirLaw;
        RouteSide(long[] positions, double[] weights, MaterialThermalLaw effectiveAirLaw) {
            this.positions = positions; this.weights = weights; this.effectiveAirLaw = effectiveAirLaw;
        }
        boolean air() { return effectiveAirLaw != null; }
    }

    private RouteSide captureRouteSide(ThermalFragment.AirFaceTrace trace, int slot, ThermalCellArena arena) {
        if (trace == null) return new RouteSide(new long[] {BlockPos.asLong(arena.minimum(slot, 0), arena.minimum(slot, 1), arena.minimum(slot, 2))},
                new double[] {1}, null);
        long[] positions = new long[trace.size()];
        double[] weights = new double[trace.size()]; int size = 0;
        for (int entry = 0; entry < trace.size(); entry++) {
            long position = trace.block(entry); int found = -1;
            for (int prior = 0; prior < size; prior++) if (positions[prior] == position) { found = prior; break; }
            if (found < 0) { found = size++; positions[found] = position; }
            weights[found] += 1.0 / trace.size();
        }
        double inverse = 0;
        for (int index = 0; index < size; index++) inverse += weights[index] * weights[index] / airLaw.capacityJPerK();
        return new RouteSide(Arrays.copyOf(positions, size), Arrays.copyOf(weights, size), MaterialThermalLaw.sensible(1 / inverse));
    }

    private record Route(RouteSide first, RouteSide second, double conductance, AirRouteValidity validity) { }

    private record EnvironmentFace(long position, double conductance) { }

    private static final class Transport implements AutoCloseable {
        final Route[] pairs;
        final EnvironmentFace[] environment;
        final ThermalMemoryBudget.Reservation credit;
        Transport(Route[] pairs, EnvironmentFace[] environment, ThermalMemoryBudget.Reservation credit) {
            this.pairs = pairs; this.environment = environment; this.credit = credit;
        }
        @Override public void close() { credit.close(); }
    }

    private static final class Tile implements Comparable<Tile> {
        final long section;
        final Frame[] frames;
        final Tile[] neighbours = new Tile[6];
        final byte[] changes, brickSlots = new byte[64], brickIds;
        final long activeBricks;
        final int[] mixingSources;
        final double[] buoyancyFactors;
        final ThermalMemoryBudget.Reservation storage;
        double ambientC;
        long sampleTick, activeSince;
        long geometryRevision = -1, pendingGeometryRevision = -1;
        long preparedSequence, stepSequence;
        boolean geometryChanged, geometryPrepared, frozen, published;
        int current, directorySlot;
        long activateAt = Long.MIN_VALUE;
        double reentryAmbientC, reentryHalfLife;
        Frame current() { return frames[current]; }
        Frame working() { return frames[current ^ 1]; }
        Transport transport;
        void acceptGeometryRevision() {
            if (pendingGeometryRevision >= 0) { geometryRevision = pendingGeometryRevision; pendingGeometryRevision = -1; }
        }
        Tile(long section, long activeBricks, double ambientC, ThermalMemoryBudget.Reservation storage) {
            this.section = section; this.ambientC = ambientC; this.storage = storage;
            this.activeBricks = activeBricks;
            int count = Long.bitCount(activeBricks);
            frames = new Frame[] {new Frame(count * 64), new Frame(count * 64)};
            changes = new byte[count * 64]; brickIds = new byte[count]; Arrays.fill(brickSlots, (byte) -1);
            mixingSources = new int[count * 64];
            buoyancyFactors = new double[count * 64];
            long remaining = activeBricks; int ordinal = 0;
            while (remaining != 0) {
                int brick = Long.numberOfTrailingZeros(remaining); remaining &= remaining - 1;
                brickSlots[brick] = (byte) ordinal; brickIds[ordinal++] = (byte) brick;
            }
        }
        @Override public int compareTo(Tile other) { return Long.compare(section, other.section); }
        int index(int pageCell) {
            int brick = (pageCell & 15) >>> 2 | ((pageCell >>> 4 & 15) >>> 2) << 2 | ((pageCell >>> 8) >>> 2) << 4;
            int ordinal = brickSlots[brick];
            return ordinal < 0 ? -1 : ordinal * 64 + (pageCell & 3) + ((pageCell >>> 4 & 3) << 2) + ((pageCell >>> 8 & 3) << 4);
        }
        int pageCell(int index) { return BlockBrickLayout.pageBlock(Byte.toUnsignedInt(brickIds[index >>> 6]), index & 63); }
        boolean paused() { return frozen; }
    }

    private final ThermalMemoryBudget budget;
    private final ThermalSignatureTable signatures;
    private final MaterialBoundaryRegistry materials;
    private final MaterialThermalLaw airLaw;
    private final double airConductance, ambientConductance, inverseAirCapacity;
    private final BuoyancyConductance.Parameters buoyancy = new BuoyancyConductance.Parameters(.25, 4, 10);
    private double windScale = 1;
    private final int maximumTiles;
    private ThermalMemoryBudget.Reservation directory;
    private Long2ObjectOpenHashMap<Tile> bySection = new Long2ObjectOpenHashMap<>();
    private final ArrayList<Tile> ordered = new ArrayList<>();
    private final ArrayList<Tile> stepTiles = new ArrayList<>();
    private final ArrayList<Domain> domains = new ArrayList<>();
    private boolean domainsDirty = true;
    private long visibilityRevision;
    /** Changes only when a Tile first becomes readable; numeric steps already published do not invalidate geometry waits. */
    public long visibilityRevision() { return visibilityRevision; }

    /** Geometry membership only. Values, time and input remain owned by the original Tile/Store. */
    public static final class Domain {
        private final it.unimi.dsi.fastutil.longs.LongSet sections;
        private Domain(it.unimi.dsi.fastutil.longs.LongOpenHashSet sections) {
            this.sections = it.unimi.dsi.fastutil.longs.LongSets.unmodifiable(sections);
        }
        public it.unimi.dsi.fastutil.longs.LongSet sections() { return sections; }
        public boolean contains(long section) { return sections.contains(section); }
    }
    private final MaterialSample restoredMaterial = new MaterialSample();
    private final ThermalExchangeKernel.MutablePairResult airPair = new ThermalExchangeKernel.MutablePairResult();
    private final ThermalExchangeKernel.MutableMaterialPairResult materialPair = new ThermalExchangeKernel.MutableMaterialPairResult();
    private final ThermalExchangeKernel.MutableBoundaryResult airBoundary = new ThermalExchangeKernel.MutableBoundaryResult();
    // One reusable query stencil per field. Numeric scratch only; no retained cell/owner references.
    private final double[] queryStencil = new double[125];
    private final double[] queryGradients = new double[81];
    private int queryGradientMask;
    // Fixed FIFO of derived cell corners. World positions and doubles only;
    // every Accepted/visibility change clears it, without retaining a frame.
    private Long2IntOpenHashMap queryCells;
    private long[] queryKeys;
    private double[] queryValues;
    private int nextQueryCell;
    private long queryCacheHits, queryCacheMisses;
    private ThermalHeatStep input;
    private volatile int stage;
    private int tileCursor, inputCursor, cellCursor;
    private int transportTileCursor, transportCursor;
    private double routeEnthalpyJ;
    private MaterialThermalLaw routeLaw;
    private byte routeBranch;
    private boolean routeWaiting;
    private int nextMaterialGeneration, directoryCapacity;
    private final long initialTick;
    private long targetTick;
    private double halfSeconds, acceptedExternalJ, trialExternalJ;
    private double acceptedRejectedInputJ, trialRejectedInputJ;
    private int minimumSectionY = Integer.MAX_VALUE, maximumSectionY = Integer.MIN_VALUE;
    private long pairUpdates, geometryCells, activeNanos;
    private boolean closed;

    public TiledThermalField(ThermalMemoryBudget budget, ThermalSignatureTable signatures,
            MaterialBoundaryRegistry materials, double airCapacity, double airConductance,
            double ambientConductance, int maximumTiles, long initialTick) {
        if (!(airCapacity > 0) || !(airConductance >= 0) || !(ambientConductance >= 0)
                || maximumTiles <= 0 || initialTick < 0) throw new IllegalArgumentException("Invalid tiled field parameters");
        this.budget = budget; this.signatures = signatures; this.materials = materials;
        this.airLaw = MaterialThermalLaw.sensible(airCapacity); inverseAirCapacity = 1 / airCapacity;
        this.airConductance = airConductance; this.ambientConductance = ambientConductance;
        this.maximumTiles = maximumTiles; this.initialTick = initialTick;
        directory = budget.tryReserve(DIRECTORY_BASE_BYTES);
        if (directory == null) throw new ThermalMemoryBudget.AllocationRefused(DIRECTORY_BASE_BYTES);
        queryCells = new Long2IntOpenHashMap(QUERY_CACHE_CELLS, .75f); queryCells.defaultReturnValue(-1);
        queryKeys = new long[QUERY_CACHE_CELLS]; queryValues = new double[8 * QUERY_CACHE_CELLS];
    }

    public static long tileBytes() {
        return tileBytes(-1L);
    }

    public static long tileBytes(long activeBricks) {
        int count = Long.bitCount(activeBricks) * 64;
        return 2064 + 2 * (2 * AirBackingMeter.arrayPayloadBytes(count, 4)
                + AirBackingMeter.arrayPayloadBytes(count, 8) + AirBackingMeter.arrayPayloadBytes(count, 1)
                + AirBackingMeter.arrayPayloadBytes((count + 63) / 64, 8)
                + AirBackingMeter.arrayPayloadBytes(count, 8)) + AirBackingMeter.arrayPayloadBytes(count, 1)
                + AirBackingMeter.arrayPayloadBytes(64, 1) + AirBackingMeter.arrayPayloadBytes(count / 64, 1)
                + AirBackingMeter.arrayPayloadBytes(count, 4) + AirBackingMeter.arrayPayloadBytes(count, 8);
    }

    private int directoryCapacityFor(int required) {
        return Math.max(required, Math.min(maximumTiles, Math.max(16, directoryCapacity * 2)));
    }

    private boolean prepareDirectory(int required) { return prepareDirectory(required, null); }
    private boolean prepareDirectory(int required, ThermalMemoryBudget.Reservation prepaid) {
        if (required <= directoryCapacity) return true;
        int capacity = directoryCapacityFor(required);
        var next = storage(DIRECTORY_BASE_BYTES + 256L * capacity, prepaid);
        if (next == null) return false;
        try {
            var map = new Long2ObjectOpenHashMap<Tile>(capacity); map.putAll(bySection);
            ordered.ensureCapacity(capacity); bySection = map;
        } catch (RuntimeException | Error failure) { next.close(); throw failure; }
        directoryCapacity = capacity; directory.close(); directory = next;
        return true;
    }

    /** Full replacement overlap for one member of an atomic geometry batch. */
    public synchronized long geometryStorageBytes(long section, long bricks, boolean replacement) {
        var previous = bySection.get(section);
        if (previous == null || replacement) return tileBytes(bricks);
        long union = previous.activeBricks | bricks;
        return union == previous.activeBricks ? 0 : tileBytes(union);
    }

    /** Reserve the whole batch before any cell or numerical membership changes. */
    public synchronized ThermalMemoryBudget.Reservation reserveGeometryStorage(long bytes, int additions, int retirements, boolean completingExisting) {
        if (closed || stage != IDLE || ordered.size() + additions - retirements > maximumTiles) return null;
        int required = ordered.size() + additions;
        if (required > directoryCapacity) bytes += DIRECTORY_BASE_BYTES + 256L * directoryCapacityFor(required);
        var credit = completingExisting || bytes == 0 ? budget.reserveCompletion(Math.max(1, bytes)) : budget.tryReserve(bytes);
        if (credit == null) return null;
        try { prepareDirectory(required, credit); return credit; }
        catch (RuntimeException | Error failure) { credit.close(); throw failure; }
    }

    private ThermalMemoryBudget.Reservation storage(long bytes, ThermalMemoryBudget.Reservation prepaid) {
        return prepaid == null ? budget.tryReserve(bytes) : prepaid.split(bytes);
    }

    /** Geometry arrives as values captured on main; this never reads Minecraft world objects. */
    public synchronized boolean admit(long section, int[] captured, long activeBricks, double ambientC) {
        return admit(section, captured, activeBricks, ambientC, null);
    }
    public synchronized boolean admit(long section, int[] captured, long activeBricks, double ambientC, ThermalMemoryBudget.Reservation prepaid) {
        return admit(section, captured, activeBricks, ambientC, latestTick(), prepaid);
    }
    public synchronized boolean admit(long section, int[] captured, long activeBricks, double ambientC, long sampleTick, ThermalMemoryBudget.Reservation prepaid) {
        if (closed || stage != IDLE || bySection.containsKey(section) || prepaid == null && ordered.size() >= maximumTiles) return false;
        if (captured.length != CELLS || !Double.isFinite(ambientC)) throw new IllegalArgumentException("Invalid tile capture");
        if (!prepareDirectory(ordered.size() + 1, prepaid)) return false;
        var credit = storage(tileBytes(activeBricks), prepaid);
        if (credit == null) return false;
        Tile tile;
        try {
            tile = new Tile(section, activeBricks, ambientC, credit);
            tile.sampleTick = tile.activeSince = sampleTick;
            for (var frame : tile.frames) {
                for (int cell = 0; cell < frame.h.length; cell++) {
                    frame.signatures[cell] = captured[tile.pageCell(cell)];
                    var law = law(frame.signatures[cell]);
                    if (law != null) {
                        frame.h[cell] = law.enthalpyAtTemperature(ambientC); frame.branches[cell] = law.initialBranch(ambientC);
                        if (law != airLaw && frame == tile.frames[0])
                            frame.materialGenerations[cell] = nextMaterialGeneration = Math.incrementExact(nextMaterialGeneration);
                    }
                }
            }
            System.arraycopy(tile.frames[0].materialGenerations, 0, tile.frames[1].materialGenerations, 0, tile.frames[0].h.length);
            bySection.put(section, tile); ordered.add(-Collections.binarySearch(ordered, tile) - 1, tile); domainsDirty = true;
            invalidateQueries();
            minimumSectionY = Math.min(minimumSectionY, SectionPos.y(section));
            maximumSectionY = Math.max(maximumSectionY, SectionPos.y(section));
            for (int axis = 0; axis < 3; axis++) for (int side = 0; side < 2; side++) {
                long neighbourKey = neighbourSection(section, axis, side == 0 ? -1 : 1);
                var neighbour = bySection.get(neighbourKey);
                if (neighbour != null) { tile.neighbours[2 * axis + side] = neighbour; neighbour.neighbours[2 * axis + (side ^ 1)] = tile; }
            }
        } catch (RuntimeException | Error failure) { credit.close(); throw failure; }
        return true;
    }

    public synchronized boolean changeCell(long position, int signature, Change change) {
        if (closed || stage != IDLE) return false;
        var tile = bySection.get(section(position));
        if (tile == null) return false;
        int capturedCell = tile.index(local(position));
        if (capturedCell < 0) return false;
        if (change == Change.PHYSICAL && (tile.geometryChanged ? tile.working() : tile.current()).signatures[capturedCell] == signature) return true;
        if (tile.frozen) return changeFrozenCell(tile, capturedCell, signature, change);
        tile.geometryPrepared = false;
        if (!tile.geometryChanged) {
            System.arraycopy(tile.current().signatures, 0, tile.working().signatures, 0, tile.current().h.length);
            System.arraycopy(tile.current().materialGenerations, 0, tile.working().materialGenerations, 0, tile.current().h.length);
            tile.geometryChanged = true;
        }
        int cell = tile.index(local(position));
        if (cell < 0) return false;
        int previousSignature = tile.working().signatures[cell];
        int previousGeneration = tile.working().materialGenerations[cell];
        tile.working().signatures[cell] = signature;
        tile.changes[cell] |= (byte) (1 << change.ordinal());
        var before = tile.current(); var trial = tile.working();
        if (material(signature)) {
            if (conversionKeepsMatter(tile, before, trial, cell) || sameMatter(tile, before, trial, cell))
                trial.materialGenerations[cell] = before.materialGenerations[cell];
            else if (change != Change.REPLACEMENT && previousGeneration > 0
                    && signatures.sameMaterialType(previousSignature, signature)) trial.materialGenerations[cell] = previousGeneration;
            else trial.materialGenerations[cell] = nextMaterialGeneration = Math.incrementExact(nextMaterialGeneration);
        } else trial.materialGenerations[cell] = 0;
        return true;
    }

    /**
     * Adds captured Bricks to one Section. Existing cells keep both their
     * Accepted values and pending geometry incarnations; addresses never move
     * in the input bank. Reserve replacement overlap before touching the old
     * Tile, so refusal leaves its field and neighbours usable.
     */
    public synchronized boolean grow(long section, int[] captured, long addedBricks) {
        return grow(section, captured, addedBricks, null);
    }
    public synchronized boolean grow(long section, int[] captured, long addedBricks, ThermalMemoryBudget.Reservation prepaid) {
        if (closed || stage != IDLE) return false;
        var previous = bySection.get(section);
        if (previous == null || captured.length != CELLS) return false;
        long union = previous.activeBricks | addedBricks;
        if (union == previous.activeBricks) return true;
        return resizeTile(previous, captured, union, prepaid);
    }

    /** Finish a committed shrink even at the admission target; old/new overlap stays charged. */
    public synchronized boolean compact(long section) {
        var tile = bySection.get(section);
        if (closed || stage != IDLE || tile == null || tile.geometryChanged || tile.paused()) return false;
        long captured = capturedBricks(tile);
        if (captured == tile.activeBricks) return true;
        try (var credit = budget.reserveCompletion(tileBytes(captured))) { return resizeTile(tile, null, captured, credit); }
    }

    private boolean resizeTile(Tile previous, int[] captured, long bricks) {
        return resizeTile(previous, captured, bricks, null);
    }
    private boolean resizeTile(Tile previous, int[] captured, long bricks, ThermalMemoryBudget.Reservation prepaid) {
        long section = previous.section;
        var credit = storage(tileBytes(bricks), prepaid);
        if (credit == null) return false;
        Tile next;
        try {
            next = new Tile(section, bricks, previous.ambientC, credit);
            next.sampleTick = previous.sampleTick; next.activeSince = previous.activeSince; next.geometryChanged = previous.geometryChanged;
            next.geometryRevision = previous.geometryRevision; next.pendingGeometryRevision = previous.pendingGeometryRevision;
            next.frozen = previous.frozen; next.published = previous.published; next.current = previous.current;
            next.activateAt = previous.activateAt; next.reentryAmbientC = previous.reentryAmbientC;
            next.reentryHalfLife = previous.reentryHalfLife;
            if (previous.frames[0].requests != null || previous.frames[1].requests != null)
                for (var frame : next.frames) frame.requests = new long[frame.h.length];
            for (int cell = 0; cell < next.current().h.length; cell++) {
                int pageCell = next.pageCell(cell), oldCell = previous.index(pageCell);
                if (oldCell >= 0) {
                    for (int slot = 0; slot < 2; slot++) {
                        var from = previous.frames[slot == (previous.current ^ 1) && !previous.geometryChanged ? previous.current : slot]; var to = next.frames[slot];
                        to.signatures[cell] = from.signatures[oldCell]; to.materialGenerations[cell] = from.materialGenerations[oldCell];
                        to.h[cell] = from.h[oldCell]; to.branches[cell] = from.branches[oldCell];
                        setWaiting(to, cell, waiting(from, oldCell));
                        if (from.requests != null) to.requests[cell] = from.requests[oldCell];
                    }
                    next.changes[cell] = previous.changes[oldCell]; next.mixingSources[cell] = previous.mixingSources[oldCell];
                } else {
                    int signature = captured[pageCell]; var law = law(signature);
                    int generation = material(signature) ? nextMaterialGeneration = Math.incrementExact(nextMaterialGeneration) : 0;
                    next.current().signatures[cell] = ThermalSignatureTable.UNRESOLVED;
                    var frame = next.working();
                    frame.signatures[cell] = signature; frame.materialGenerations[cell] = generation;
                    if (law != null) { frame.h[cell] = law.enthalpyAtTemperature(next.ambientC); frame.branches[cell] = law.initialBranch(next.ambientC); }
                    next.changes[cell] = (byte) (1 << Change.PHYSICAL.ordinal()); next.geometryChanged = true;
                }
            }
        } catch (RuntimeException | Error failure) { credit.close(); throw failure; }
        System.arraycopy(previous.neighbours, 0, next.neighbours, 0, 6);
        for (int face = 0; face < 6; face++) {
            var neighbour = next.neighbours[face];
            if (neighbour != null) neighbour.neighbours[face ^ 1] = next;
        }
        bySection.put(section, next); ordered.set(Collections.binarySearch(ordered, previous), next); domainsDirty = true;
        next.transport = previous.transport; previous.transport = null;
        Arrays.fill(previous.neighbours, null); previous.storage.close();
        return true;
    }

    /** Replace one Section's passage and sky geometry; no old numerical frame or builder is retained. */
    public synchronized boolean replaceTransport(long section, ThermalFragment[] fragments, ThermalCellArena arena) {
        return replaceTransport(section, fragments, arena, null);
    }
    public synchronized boolean replaceTransport(long section, ThermalFragment[] fragments, ThermalCellArena arena, ThermalMemoryBudget.Reservation prepaid) {
        var tile = bySection.get(section);
        if (closed || stage != IDLE || tile == null) return false;
        int count = 0, environmentCount = 0;
        for (var fragment : fragments) {
            var contacts = fragment.routedContacts();
            for (int edge = 0; edge < contacts.size(); edge++) {
                if (!contacts.active(edge)) continue;
                count++;
            }
            environmentCount += fragment.farBoundaries().patchCount();
        }
        Transport next = null;
        if (count != 0 || environmentCount != 0) {
            var credit = storage(transportBytes(fragments), prepaid);
            if (credit == null) return false;
            try {
                Route[] pairs = new Route[count];
                EnvironmentFace[] environment = new EnvironmentFace[environmentCount];
                int index = 0, boundary = 0;
                for (var fragment : fragments) {
                    var contacts = fragment.routedContacts();
                    for (int edge = 0; edge < contacts.size(); edge++) {
                        if (!contacts.active(edge)) continue;
                        pairs[index++] = new Route(captureRouteSide(contacts.firstTrace(edge), contacts.first(edge), arena),
                                captureRouteSide(contacts.secondTrace(edge), contacts.second(edge), arena),
                                contacts.conductance(edge), contacts.routes()[edge]);
                    }
                    var far = fragment.farBoundaries();
                    for (int face = 0; face < far.patchCount(); face++)
                        environment[boundary++] = new EnvironmentFace(far.patchBlock(face), far.patchConductance(face));
                }
                next = new Transport(pairs, environment, credit);
            } catch (RuntimeException | Error failure) { credit.close(); throw failure; }
        }
        var previous = tile.transport;
        tile.transport = next;
        domainsDirty = true;
        if (previous != null) previous.close();
        return true;
    }

    /** Include not-yet-committed routes when funding the next geometry publication. */
    public static long transportBytes(ThermalFragment[] fragments) {
        long bytes = 0;
        for (var fragment : fragments) {
            var contacts = fragment.routedContacts();
            for (int edge = 0; edge < contacts.size(); edge++) if (!contacts.routes()[edge].retired()) {
                int first = contacts.firstTrace(edge) == null ? 1 : contacts.firstTrace(edge).size();
                int second = contacts.secondTrace(edge) == null ? 1 : contacts.secondTrace(edge).size();
                bytes += 192L + 32L * (first + second);
            }
            bytes += 64L * fragment.farBoundaries().patchCount();
        }
        return bytes == 0 ? 0 : bytes + 256;
    }

    /** Coefficients change locally when an emitter starts/stops; no mesh identity changes. */
    public synchronized boolean replaceMixing(long section, int brick, AirMixingRegion sources) {
        if (closed || stage != IDLE) return false;
        var tile = bySection.get(section);
        if (tile == null || (tile.activeBricks & 1L << brick) == 0) return false;
        int x0 = SectionPos.x(section) << 4, y0 = SectionPos.y(section) << 4, z0 = SectionPos.z(section) << 4;
        for (int block = 0; block < 64; block++) {
            int cell = BlockBrickLayout.pageBlock(brick, block);
            tile.mixingSources[tile.index(cell)] = sources.volumeScale(x0 + (cell & 15) + .5,
                    y0 + (cell >>> 8) + .5, z0 + (cell >>> 4 & 15) + .5) > 1 ? 1 : 0;
        }
        return true;
    }

    /** Coefficients change locally when an emitter starts/stops; no mesh identity changes. */
    public synchronized boolean updateMixing(long portPosition, int delta) {
        if (closed || stage != IDLE) return false;
        if (delta != 1 && delta != -1) throw new IllegalArgumentException("Emitter change must add or remove one source");
        int px = BlockPos.getX(portPosition), py = BlockPos.getY(portPosition), pz = BlockPos.getZ(portPosition);
        // Check removal before changing anything, so the caller can reconcile a stale control.
        for (int pass = delta < 0 ? 0 : 1; pass < 2; pass++)
            for (int x = -AirMixingRegion.RADIUS; x <= AirMixingRegion.RADIUS; x++)
                for (int y = -AirMixingRegion.RADIUS; y <= AirMixingRegion.RADIUS; y++)
                    for (int z = -AirMixingRegion.RADIUS; z <= AirMixingRegion.RADIUS; z++) {
                        if (x * x + y * y + z * z > AirMixingRegion.RADIUS * AirMixingRegion.RADIUS) continue;
                        long position = BlockPos.asLong(px + x, py + y, pz + z);
                        var tile = bySection.get(section(position));
                        int cell = tile == null ? -1 : tile.index(local(position));
                        if (cell < 0) continue;
                        if (pass == 0) { if (tile.mixingSources[cell] == 0) return false; }
                        else tile.mixingSources[cell] += delta;
                    }
        return true;
    }

    public synchronized boolean windScale(double scale) {
        if (closed || stage != IDLE) return false;
        if (!(scale > 0) || !Double.isFinite(scale)) throw new IllegalArgumentException("Invalid wind conductance scale");
        windScale = scale; return true;
    }

    /** Call only after its accepted state and pending input have been durably handed off. */
    public synchronized boolean retire(long section) {
        if (closed || stage != IDLE) return false;
        var tile = bySection.remove(section);
        if (tile == null) return false;
        acceptedExternalJ += ownedEnthalpyJ(tile.current());
        invalidateQueries();
        ordered.remove(tile); domainsDirty = true;
        for (int face = 0; face < 6; face++) {
            var neighbour = tile.neighbours[face];
            if (neighbour != null && neighbour.neighbours[face ^ 1] == tile) neighbour.neighbours[face ^ 1] = null;
        }
        Arrays.fill(tile.neighbours, null); tile.storage.close();
        if (tile.transport != null) { tile.transport.close(); tile.transport = null; }
        return true;
    }

    public synchronized boolean begin(ThermalHeatStep batch) {
        return begin(batch, null);
    }
    public synchronized boolean begin(ThermalHeatStep batch, it.unimi.dsi.fastutil.longs.LongSet sections) {
        if (closed || stage != IDLE || batch == null) return false;
        if (batch.toTick() < batch.fromTick()) throw new IllegalArgumentException("A numerical interval cannot move backwards");
        stepTiles.clear();
        for (var tile : ordered) if (!tile.frozen && (sections == null || sections.contains(tile.section))) {
            if (batch.toTick() != batch.fromTick() && tile.sampleTick != batch.fromTick()) {
                stepTiles.clear(); return false;
            }
            tile.stepSequence = batch.sequence(); stepTiles.add(tile);
        }
        input = batch; targetTick = batch.toTick(); halfSeconds = (targetTick - batch.fromTick()) / 40.0;
        trialExternalJ = acceptedExternalJ;
        trialRejectedInputJ = acceptedRejectedInputJ;
        tileCursor = inputCursor = cellCursor = 0;
        stage = PREPARE;
        return true;
    }

    private boolean selected(Tile tile) {
        return tile != null && input != null && tile.stepSequence == input.sequence() && !tile.frozen;
    }

    /** Rebuild only geometry membership. No component owns a second clock, input queue or numerical frame. */
    public synchronized boolean domainsDirty() { return domainsDirty; }
    public synchronized java.util.List<Domain> domains() {
        if (!domainsDirty || stage != IDLE) return domains;
        int[] parent = new int[ordered.size()];
        for (int index = 0; index < ordered.size(); index++) { parent[index] = index; ordered.get(index).directorySlot = index; }
        for (var tile : ordered) if (!tile.frozen) {
            for (var neighbour : tile.neighbours) if (neighbour != null && !neighbour.frozen)
                join(parent, tile.directorySlot, neighbour.directorySlot);
            if (tile.transport != null) for (var route : tile.transport.pairs) if (route.validity.activeForSolve()) {
                joinSide(parent, tile, route.first); joinSide(parent, tile, route.second);
            }
        }
        var members = new java.util.LinkedHashMap<Integer, it.unimi.dsi.fastutil.longs.LongOpenHashSet>();
        for (var tile : ordered) if (!tile.frozen)
            members.computeIfAbsent(root(parent, tile.directorySlot), ignored -> new it.unimi.dsi.fastutil.longs.LongOpenHashSet())
                    .add(tile.section);
        domains.clear();
        for (var sections : members.values()) domains.add(new Domain(sections));
        domainsDirty = false;
        return domains;
    }
    private void joinSide(int[] parent, Tile owner, RouteSide side) {
        for (long position : side.positions) {
            var tile = bySection.get(section(position));
            if (tile != null && !tile.frozen) join(parent, owner.directorySlot, tile.directorySlot);
        }
    }
    private static int root(int[] parent, int index) {
        while (parent[index] != index) { parent[index] = parent[parent[index]]; index = parent[index]; }
        return index;
    }
    private static void join(int[] parent, int first, int second) {
        int a = root(parent, first), b = root(parent, second);
        if (a != b) parent[Math.max(a, b)] = Math.min(a, b);
    }
    public synchronized long minimumTick(it.unimi.dsi.fastutil.longs.LongSet sections) {
        long tick = Long.MAX_VALUE;
        for (long section : sections) {
            var tile = bySection.get(section);
            if (tile != null && !tile.frozen) tick = Math.min(tick, tile.sampleTick);
        }
        return tick;
    }
    public synchronized long maximumTick(it.unimi.dsi.fastutil.longs.LongSet sections) {
        long tick = Long.MIN_VALUE;
        for (long section : sections) {
            var tile = bySection.get(section);
            if (tile != null && !tile.frozen) tick = Math.max(tick, tile.sampleTick);
        }
        return tick;
    }
    public synchronized boolean aligned(it.unimi.dsi.fastutil.longs.LongSet sections) {
        long minimum = minimumTick(sections), maximum = maximumTick(sections);
        return minimum == Long.MAX_VALUE || minimum == maximum;
    }
    public synchronized boolean atBoundary(it.unimi.dsi.fastutil.longs.LongSet sections, long tick) {
        for (long section : sections) {
            var tile = bySection.get(section);
            if (tile != null && !tile.frozen && tile.sampleTick != tick) return false;
        }
        return true;
    }

    /** Sole worker, resumable between bounded batches; no main-thread monitor surrounds numerical work. */
    public boolean advance(long deadline) {
        long started = System.nanoTime();
        try {
            do {
                if (stage == READY) return true;
                if (stage == IDLE || closed) return false;
                if (stage == PREPARE) prepareNextTile();
                else if (stage == FIRST_INPUT || stage == SECOND_INPUT) injectNextBatch();
                else if (stage == BUOYANCY) prepareBuoyancy();
                else if (stage == FORWARD_TRANSPORT || stage == REVERSE_TRANSPORT) advanceTransport(stage == REVERSE_TRANSPORT);
                else sweepNextCells(stage == REVERSE);
            } while (System.nanoTime() < deadline);
            return stage == READY;
        } finally { activeNanos += System.nanoTime() - started; }
    }

    private void prepareNextTile() {
        if (tileCursor == stepTiles.size()) {
            stage = FIRST_INPUT;
            tileCursor = cellCursor = inputCursor = 0; return;
        }
        var tile = stepTiles.get(tileCursor++);
        if (!tile.paused() && tile.preparedSequence != input.sequence()
                && (halfSeconds != 0 || tile.geometryChanged || !tile.published)) prepareTile(tile);
    }

    private void prepareTile(Tile tile) {
        tile.preparedSequence = input == null ? 0 : input.sequence();
        tile.geometryPrepared = halfSeconds == 0;
        var before = tile.current(); var trial = tile.working();
        int count = before.h.length;
        System.arraycopy(before.h, 0, trial.h, 0, count);
        System.arraycopy(before.branches, 0, trial.branches, 0, count);
        System.arraycopy(before.waiting, 0, trial.waiting, 0, before.waiting.length);
        if (before.requests != null) {
            if (trial.requests == null) trial.requests = new long[count];
            System.arraycopy(before.requests, 0, trial.requests, 0, count);
        } else if (trial.requests != null) Arrays.fill(trial.requests, 0);
        if (!tile.geometryChanged) {
            System.arraycopy(before.signatures, 0, trial.signatures, 0, count);
            System.arraycopy(before.materialGenerations, 0, trial.materialGenerations, 0, count);
            return;
        }
        for (int cell = 0; cell < count; cell++) {
            int old = before.signatures[cell], next = trial.signatures[cell];
            if (old == next && tile.changes[cell] == 0) continue;
            geometryCells++;
            var oldLaw = law(old); var nextLaw = law(next);
            boolean converted = conversionKeepsMatter(tile, before, trial, cell);
            boolean sameMatter = sameMatter(tile, before, trial, cell);
            double nextJ = projectedEnthalpy(tile, before, trial, cell);
            double previousJ = before.h[cell];
            if (oldLaw == airLaw && nextLaw == airLaw) nextJ = previousJ;
            trialExternalJ += previousJ - nextJ;
            trial.h[cell] = nextJ;
            trial.branches[cell] = nextLaw == null ? 0 : sameMatter && oldLaw == nextLaw && !converted ? before.branches[cell]
                    : nextLaw.initialBranch(nextLaw.temperatureC(nextJ, (byte) 0));
            setWaiting(trial, cell, sameMatter && oldLaw == nextLaw && !converted && waiting(before, cell));
            if (trial.requests != null && !(sameMatter && oldLaw == nextLaw && !converted)) trial.requests[cell] = 0;
        }
    }

    private Frame airFrame(Tile tile, boolean working) {
        return working && (input == null ? tile.geometryChanged : tile.preparedSequence == input.sequence()) ? tile.working() : tile.current();
    }

    /** Private geometry and frozen writeback values cannot change another owner's query. */
    private boolean airVisible(Tile tile, boolean working, Tile readOwner) {
        if (tile == null) return false;
        if (working) return selected(tile);
        if (readOwner.paused()) return tile == readOwner;
        return !tile.paused() && (tile == readOwner || tile.published && tile.sampleTick == readOwner.sampleTick);
    }

    private int cornerMask(int x, int y, int z, boolean working, Tile readOwner) {
        int mask = 0;
        for (int octant = 0; octant < 8; octant++) {
            long position = BlockPos.asLong(x - (octant & 1), y - (octant >>> 2), z - (octant >>> 1 & 1));
            var tile = bySection.get(section(position));
            if (!airVisible(tile, working, readOwner)) continue;
            int cell = tile.index(local(position));
            if (cell >= 0 && law(airFrame(tile, working).signatures[cell]) == airLaw) mask |= 1 << octant;
        }
        return mask;
    }

    private double airMean(int x, int y, int z, Tile readOwner) {
        long position = BlockPos.asLong(x, y, z);
        long ownerSection = section(position);
        var tile = ownerSection == readOwner.section ? readOwner : bySection.get(ownerSection);
        if (!airVisible(tile, false, readOwner)) return Double.NaN;
        int cell = tile.index(local(position));
        var frame = tile.current();
        return cell >= 0 && law(frame.signatures[cell]) == airLaw ? frame.h[cell] / airLaw.capacityJPerK() : Double.NaN;
    }

    /**
     * Reconstruct a shared vertex only when queried. It is a view of cell means,
     * never stored energy. Face-connected branches keep solid corners separate.
     */
    private double queryCorner(Tile owner, int x, int y, int z, int corner) {
        int vx = corner & 1, vy = corner >>> 2, vz = corner >>> 1 & 1, mask = 0;
        for (int octant = 0; octant < 8; octant++)
            if (Double.isFinite(queryMean(owner, x, y, z, vx - (octant & 1), vy - (octant >>> 2), vz - (octant >>> 1 & 1)))) mask |= 1 << octant;
        int members = AirCornerTopology.component(mask, AirCornerTopology.group(mask, corner)), remaining = members;
        double sum = 0, minimum = Double.POSITIVE_INFINITY, maximum = Double.NEGATIVE_INFINITY;
        while (remaining != 0) {
            int octant = Integer.numberOfTrailingZeros(remaining); remaining &= remaining - 1;
            int cx = vx - (octant & 1), cy = vy - (octant >>> 2), cz = vz - (octant >>> 1 & 1);
            double value = queryMean(owner, x, y, z, cx, cy, cz);
            minimum = Math.min(minimum, value); maximum = Math.max(maximum, value);
            int gradient = cx + 1 + 3 * (cz + 1) + 9 * (cy + 1), offset = 3 * gradient;
            // A cell can contribute to several shared vertices. Its slope is
            // independent of that vertex, so calculate it once per query.
            if ((queryGradientMask & 1 << gradient) == 0) {
                queryGradients[offset] = limitedSlope(value, queryMean(owner, x, y, z, cx - 1, cy, cz), queryMean(owner, x, y, z, cx + 1, cy, cz));
                queryGradients[offset + 1] = limitedSlope(value, queryMean(owner, x, y, z, cx, cy - 1, cz), queryMean(owner, x, y, z, cx, cy + 1, cz));
                queryGradients[offset + 2] = limitedSlope(value, queryMean(owner, x, y, z, cx, cy, cz - 1), queryMean(owner, x, y, z, cx, cy, cz + 1));
                queryGradientMask |= 1 << gradient;
            }
            sum += value + ((octant & 1) - .5) * queryGradients[offset] + ((octant >>> 2) - .5) * queryGradients[offset + 1]
                    + ((octant >>> 1 & 1) - .5) * queryGradients[offset + 2];
        }
        double temperature = sum / Integer.bitCount(members);
        // One-sided extrapolation may not invent cooling in a locally heat-only
        // field, or heating in a cold-only field. This changes the query, never H.
        if (minimum >= owner.ambientC) temperature = Math.max(owner.ambientC, temperature);
        if (maximum <= owner.ambientC) temperature = Math.min(owner.ambientC, temperature);
        return temperature;
    }

    /** Each actual cell is fetched at most once during this query, including missing cells. */
    private double queryMean(Tile owner, int x, int y, int z, int dx, int dy, int dz) {
        int index = dx + 2 + 5 * (dz + 2) + 25 * (dy + 2);
        double value = queryStencil[index];
        if (value == Double.NEGATIVE_INFINITY) queryStencil[index] = value = airMean(x + dx, y + dy, z + dz, owner);
        return value;
    }

    /** Monotonized central difference, with a one-sided slope at an Air boundary. */
    private static double limitedSlope(double value, double low, double high) {
        if (!Double.isFinite(low)) return Double.isFinite(high) ? high - value : 0;
        if (!Double.isFinite(high)) return value - low;
        double left = value - low, right = high - value;
        if (left * right <= 0) return 0;
        double central = (left + right) / 2;
        return Math.copySign(Math.min(Math.abs(central), 2 * Math.min(Math.abs(left), Math.abs(right))), central);
    }

    private double sampleAirCell(Tile tile, int cell, double fx, double fy, double fz) {
        int pageCell = tile.pageCell(cell);
        int x = (SectionPos.x(tile.section) << 4) + (pageCell & 15), y = (SectionPos.y(tile.section) << 4) + (pageCell >>> 8),
                z = (SectionPos.z(tile.section) << 4) + (pageCell >>> 4 & 15);
        long position = BlockPos.asLong(x, y, z);
        int cached = queryCells.get(position);
        if (cached < 0) {
            queryCacheMisses++;
            cached = nextQueryCell;
            if (queryCells.size() == QUERY_CACHE_CELLS) queryCells.remove(queryKeys[cached]);
            Arrays.fill(queryStencil, Double.NEGATIVE_INFINITY); queryGradientMask = 0;
            for (int corner = 0; corner < 8; corner++) queryValues[cached * 8 + corner] = queryCorner(tile, x, y, z, corner);
            queryKeys[cached] = position; queryCells.put(position, cached);
            nextQueryCell = (cached + 1) % QUERY_CACHE_CELLS;
        } else queryCacheHits++;
        double temperature = 0;
        for (int corner = 0; corner < 8; corner++) {
            double weight = ((corner & 1) == 0 ? 1 - fx : fx) * ((corner & 4) == 0 ? 1 - fy : fy) * ((corner & 2) == 0 ? 1 - fz : fz);
            temperature += weight * queryValues[cached * 8 + corner];
        }
        return temperature;
    }

    private void invalidateQueries() {
        queryCells.clear(); nextQueryCell = 0;
    }

    private boolean material(int signature) {
        return signatures.valid(signature) && !signatures.isAir(signature) && law(signature) != null;
    }

    private boolean conversionKeepsMatter(Tile tile, Frame before, Frame trial, int cell) {
        return (tile.changes[cell] & 1 << Change.REPLACEMENT.ordinal()) == 0
                && (tile.changes[cell] & (1 << Change.PHASE.ordinal() | 1 << Change.GAMEPLAY.ordinal())) != 0
                && material(before.signatures[cell]) && material(trial.signatures[cell]);
    }

    private boolean sameMatter(Tile tile, Frame before, Frame trial, int cell) {
        int old = before.signatures[cell], next = trial.signatures[cell];
        return (tile.changes[cell] & 1 << Change.REPLACEMENT.ordinal()) == 0 && law(old) != null && law(next) != null
                && signatures.isAir(old) == signatures.isAir(next) && signatures.sameMaterialType(old, next);
    }

    private double projectedEnthalpy(Tile tile, Frame before, Frame trial, int cell) {
        var nextLaw = law(trial.signatures[cell]);
        if (nextLaw == null) return 0;
        if (law(before.signatures[cell]) == airLaw && nextLaw == airLaw) return before.h[cell];
        if (conversionKeepsMatter(tile, before, trial, cell))
            return convertedEnthalpy(law(before.signatures[cell]), nextLaw, before.h[cell], before.branches[cell],
                    (tile.changes[cell] & 1 << Change.GAMEPLAY.ordinal()) != 0);
        return sameMatter(tile, before, trial, cell)
                ? law(before.signatures[cell]).afterMassChange(before.h[cell], nextLaw, tile.ambientC)
                : nextLaw.enthalpyAtTemperature(tile.ambientC);
    }

    private static double convertedEnthalpy(MaterialThermalLaw before, MaterialThermalLaw after,
            double energy, byte branch, boolean keepTemperature) {
        return keepTemperature ? after.enthalpyAtTemperature(before.temperatureC(energy, branch)) : energy;
    }

    private boolean changeFrozenCell(Tile tile, int cell, int signature, Change change) {
        var frame = tile.current();
        int previous = frame.signatures[cell];
        var oldLaw = law(previous); var nextLaw = law(signature);
        boolean converted = (change == Change.PHASE || change == Change.GAMEPLAY) && material(previous) && material(signature);
        boolean same = change != Change.REPLACEMENT && oldLaw != null && nextLaw != null
                && signatures.isAir(previous) == signatures.isAir(signature) && signatures.sameMaterialType(previous, signature);
        double next = 0;
        if (nextLaw != null) {
            if (converted) next = convertedEnthalpy(oldLaw, nextLaw, frame.h[cell], frame.branches[cell], change == Change.GAMEPLAY);
            else if (same) next = oldLaw.afterMassChange(frame.h[cell], nextLaw, tile.ambientC);
            else next = nextLaw.enthalpyAtTemperature(tile.ambientC);
        }
        acceptedExternalJ += frame.h[cell] - next;
        frame.h[cell] = next; frame.signatures[cell] = signature;
        boolean inherited = same && oldLaw == nextLaw && !converted;
        if (frame.requests != null && !inherited) frame.requests[cell] = 0;
        frame.branches[cell] = nextLaw == null ? 0 : inherited ? frame.branches[cell]
                : nextLaw.initialBranch(nextLaw.temperatureC(next, (byte) 0));
        var edge = nextLaw == null ? null : nextLaw.transition(frame.branches[cell]);
        setWaiting(frame, cell, edge != null && edge.complete(next));
        if (!material(signature)) frame.materialGenerations[cell] = 0;
        else if (!converted && !same) frame.materialGenerations[cell] = nextMaterialGeneration = Math.incrementExact(nextMaterialGeneration);
        geometryCells++;
        invalidateQueries();
        return true;
    }

    private void injectNextBatch() {
        int end = Math.min(input.size(), inputCursor + 128);
        for (; inputCursor < end; inputCursor++) {
            double energy = (input.joules(inputCursor) + input.pendingJoules(inputCursor)) * 0.5;
            if (energy == 0) continue;
            long position = input.position(inputCursor);
            int generation = input.materialGeneration(inputCursor);
            var tile = bySection.get(section(position));
            if (tile == null) { input.defer(inputCursor, energy); continue; }
            int cell = tile.index(local(position));
            if (cell < 0) { input.defer(inputCursor, energy); continue; }
            if (!selected(tile)) {
                var held = tile.current();
                boolean matches = generation < 0 ? law(held.signatures[cell]) == airLaw
                        : material(held.signatures[cell]) && held.materialGenerations[cell] == generation;
                if (matches || !signatures.valid(held.signatures[cell])) input.defer(inputCursor, energy);
                else { trialExternalJ += energy; trialRejectedInputJ += energy; }
                continue;
            }
            if (tile.preparedSequence != input.sequence()) prepareTile(tile);
            var frame = tile.working();
            int signature = frame.signatures[cell];
            if (!signatures.valid(signature)) { input.defer(inputCursor, energy); continue; }
            var law = law(signature);
            boolean matches = generation < 0 ? signatures.isAir(signature)
                    : frame.materialGenerations[cell] == generation;
            if (!matches || law == null) { trialExternalJ += energy; trialRejectedInputJ += energy; continue; }
            if (law == airLaw) {
                injectAirFace(position, input.face(inputCursor), energy);
                continue;
            }
            double remaining = energy;
            for (int part = 0; part < 6 && remaining != 0 && !waiting(frame, cell); part++) {
                double direction = Math.signum(remaining);
                byte branch = frame.branches[cell] = law.selectBranch(frame.h[cell], frame.branches[cell], direction);
                double amount = direction * Math.min(Math.abs(remaining), law.energyLimitJ(frame.h[cell], branch, direction));
                frame.h[cell] += amount; remaining -= amount;
                var transition = law.transition(branch);
                setWaiting(frame, cell, transition != null && transition.complete(frame.h[cell]));
                if (amount == 0) break;
            }
            if (remaining != 0) input.defer(inputCursor, remaining);
        }
        if (inputCursor == input.size()) {
            if (stage == SECOND_INPUT) stage = READY;
            else if (halfSeconds == 0) { stage = SECOND_INPUT; inputCursor = 0; }
            else { stage = BUOYANCY; tileCursor = cellCursor = 0; }
        }
    }

    /** The original volume probe: one factor per connected Air branch in a 4-cube, after half the source input. */
    private void prepareBuoyancy() {
        if (tileCursor == stepTiles.size()) { stage = FORWARD; resetSweep(false); return; }
        var tile = stepTiles.get(tileCursor);
        var frame = tile.working();
        if (!selected(tile) || cellCursor == frame.h.length) { tileCursor++; cellCursor = 0; return; }
        int base = cellCursor; cellCursor += 64;
        long remaining = 0;
        for (int cell = 0; cell < 64; cell++) if (law(frame.signatures[base + cell]) == airLaw) remaining |= 1L << cell;
        while (remaining != 0) {
            long group = Long.lowestOneBit(remaining), expanded;
            do {
                expanded = group;
                group |= (group & ~0x8888888888888888L) << 1 | (group & ~0x1111111111111111L) >>> 1
                        | (group & ~0xf000f000f000f000L) << 4 | (group & ~0x000f000f000f000fL) >>> 4
                        | group << 16 | group >>> 16;
                group &= remaining;
            } while (group != expanded);
            remaining &= ~group;
            double lower = 0, upper = 0, lowerHeight = 0, upperHeight = 0;
            int lowerCount = 0, upperCount = 0;
            long cells = group;
            while (cells != 0) {
                int cell = Long.numberOfTrailingZeros(cells); cells &= cells - 1;
                double temperature = frame.h[base + cell] / airLaw.capacityJPerK();
                if (cell < 32) { lower += temperature; lowerHeight += (cell >>> 4) + .5; lowerCount++; }
                else { upper += temperature; upperHeight += (cell >>> 4) + .5; upperCount++; }
            }
            double factor = 1;
            if (lowerCount != 0 && upperCount != 0) {
                double gradient = (lower / lowerCount - upper / upperCount) / (upperHeight / upperCount - lowerHeight / lowerCount);
                factor = Math.max(buoyancy.minimumFactor(), Math.min(buoyancy.maximumFactor(), 1 + gradient * 4 / buoyancy.temperatureScaleK()));
            }
            while (group != 0) {
                int cell = Long.numberOfTrailingZeros(group); group &= group - 1;
                tile.buoyancyFactors[base + cell] = factor;
            }
        }
    }

    /**
     * Preserve the original source footprint. Each face corner's energy is
     * projected equally to its connected incident Air cells (equal C/8 shares).
     */
    private void injectAirFace(long sourcePosition, int face, double energy) {
        int x = BlockPos.getX(sourcePosition), y = BlockPos.getY(sourcePosition), z = BlockPos.getZ(sourcePosition);
        int axisBit = face < 0 ? 0 : switch (face >>> 1) { case 0 -> 1; case 1 -> 4; default -> 2; };
        int selectedSide = face < 0 || (face & 1) == 0 ? 0 : axisBit;
        double perCorner = energy / (face < 0 ? 8 : 4);
        for (int corner = 0; corner < 8; corner++) {
            if (axisBit != 0 && (corner & axisBit) != selectedSide) continue;
            int vx = x + (corner & 1), vy = y + (corner >>> 2), vz = z + (corner >>> 1 & 1);
            int mask = cornerMask(vx, vy, vz, true, null);
            int members = AirCornerTopology.component(mask, AirCornerTopology.group(mask, corner));
            double share = perCorner / Integer.bitCount(members), remaining = perCorner;
            while (members != 0) {
                int octant = Integer.numberOfTrailingZeros(members); members &= members - 1;
                long position = BlockPos.asLong(vx - (octant & 1), vy - (octant >>> 2), vz - (octant >>> 1 & 1));
                var tile = bySection.get(section(position));
                if (tile.preparedSequence != input.sequence()) prepareTile(tile);
                double amount = members == 0 ? remaining : share;
                tile.working().h[tile.index(local(position))] += amount; remaining -= amount;
            }
        }
    }

    private void resetSweep(boolean reverse) {
        tileCursor = reverse ? stepTiles.size() - 1 : 0; cellCursor = 0;
    }

    private void sweepNextCells(boolean reverse) {
        int processed = 0;
        while (processed < 128) {
            if (tileCursor < 0 || tileCursor == stepTiles.size()) {
                if (!reverse) { stage = FORWARD_TRANSPORT; transportTileCursor = transportCursor = 0; }
                else { stage = SECOND_INPUT; inputCursor = 0; }
                return;
            }
            var tile = stepTiles.get(tileCursor);
            if (!selected(tile)) { tileCursor += reverse ? -1 : 1; cellCursor = 0; continue; }
            var frame = tile.working();
            if (cellCursor == frame.h.length) { tileCursor += reverse ? -1 : 1; cellCursor = 0; continue; }
            int cell = reverse ? frame.h.length - 1 - cellCursor : cellCursor;
            int pageCell = tile.pageCell(cell);
            if (law(frame.signatures[cell]) != null) for (int ordinal = 0; ordinal < 3; ordinal++) {
                int axis = reverse ? 2 - ordinal : ordinal;
                if (reverse) negativeBoundary(tile, frame, cell, pageCell, axis);
                positiveFace(tile, frame, cell, pageCell, axis);
                if (!reverse) negativeBoundary(tile, frame, cell, pageCell, axis);
            }
            processed++;
            cellCursor++;
        }
    }

    private void advanceTransport(boolean reverse) {
        int processed = 0;
        while (processed < 16) {
            if (transportTileCursor < 0 || transportTileCursor == stepTiles.size()) {
                if (!reverse) { stage = REVERSE_TRANSPORT; transportTileCursor = stepTiles.size() - 1; transportCursor = 0; }
                else { stage = REVERSE; resetSweep(true); }
                return;
            }
            var tile = stepTiles.get(transportTileCursor);
            var transport = tile.transport;
            int count = transport == null ? 0 : transport.pairs.length + transport.environment.length;
            if (!selected(tile) || transport == null || transportCursor == count) {
                transportTileCursor += reverse ? -1 : 1; transportCursor = 0; continue;
            }
            int index = reverse ? count - 1 - transportCursor : transportCursor;
            if (index < transport.pairs.length) exchangeRoute(transport.pairs[index]);
            else {
                var boundary = transport.environment[index - transport.pairs.length];
                int cell = tile.index(local(boundary.position));
                var frame = tile.working();
                if (cell >= 0 && law(frame.signatures[cell]) == airLaw)
                    exchangeEnvironment(tile, frame, cell, boundary.conductance);
            }
            transportCursor++; processed++;
        }
    }

    private boolean readRouteSide(RouteSide side) {
        if (!side.air()) {
            long position = side.positions[0];
            var tile = bySection.get(section(position));
            int cell = !selected(tile) ? -1 : tile.index(local(position));
            if (cell < 0) return false;
            var frame = tile.working();
            routeLaw = law(frame.signatures[cell]);
            if (routeLaw == null || routeLaw == airLaw) return false;
            routeEnthalpyJ = frame.h[cell]; routeBranch = frame.branches[cell]; routeWaiting = waiting(frame, cell);
            return true;
        }
        double temperature = 0;
        for (int index = 0; index < side.positions.length; index++) {
            long position = side.positions[index];
            var tile = bySection.get(section(position));
            int cell = !selected(tile) ? -1 : tile.index(local(position));
            if (cell < 0 || law(tile.working().signatures[cell]) != airLaw) return false;
            temperature += side.weights[index] * tile.working().h[cell] * inverseAirCapacity;
        }
        routeLaw = side.effectiveAirLaw; routeEnthalpyJ = temperature * routeLaw.capacityJPerK();
        routeBranch = 0; routeWaiting = false;
        return true;
    }

    private void exchangeRoute(Route route) {
        if (!route.validity.valid() || !readRouteSide(route.first)) return;
        var firstLaw = routeLaw;
        double firstJ = routeEnthalpyJ;
        byte firstBranch = routeBranch;
        boolean firstWaiting = routeWaiting;
        if (!readRouteSide(route.second)) return;
        double secondJ = routeEnthalpyJ;
        if (route.first.air() && route.second.air()) {
            double inverse = 1 / firstLaw.capacityJPerK() + 1 / routeLaw.capacityJPerK();
            for (int a = 0; a < route.first.positions.length; a++) for (int b = 0; b < route.second.positions.length; b++)
                if (route.first.positions[a] == route.second.positions[b])
                    inverse -= 2 * route.first.weights[a] * route.second.weights[b] * inverseAirCapacity;
            if (inverse <= 0) return;
            double delta = firstJ / firstLaw.capacityJPerK() - secondJ / routeLaw.capacityJPerK();
            double energy = delta * -Math.expm1(-route.conductance * inverse * halfSeconds) / inverse;
            distributeRouteEnergy(route.first, -energy); distributeRouteEnergy(route.second, energy); pairUpdates++;
            return;
        }
        ThermalExchangeKernel.exchangeMaterialsInto(firstLaw, firstJ, firstBranch, firstWaiting,
                routeLaw, secondJ, routeBranch, routeWaiting, route.conductance, halfSeconds, materialPair);
        writeRouteSide(route.first, firstJ, materialPair.firstJ(), materialPair.firstBranch(), materialPair.firstWaiting());
        writeRouteSide(route.second, secondJ, materialPair.secondJ(), materialPair.secondBranch(), materialPair.secondWaiting());
        pairUpdates += materialPair.updates();
    }

    private void writeRouteSide(RouteSide side, double previousJ, double nextJ, byte branch, boolean waiting) {
        if (side.air()) { distributeRouteEnergy(side, nextJ - previousJ); return; }
        long position = side.positions[0];
        var tile = bySection.get(section(position));
        int cell = tile.index(local(position));
        var frame = tile.working();
        frame.h[cell] = nextJ; frame.branches[cell] = branch; setWaiting(frame, cell, waiting);
    }

    private void distributeRouteEnergy(RouteSide side, double energy) {
        double remaining = energy;
        for (int index = 0; index < side.positions.length; index++) {
            long position = side.positions[index];
            var tile = bySection.get(section(position));
            double amount = index == side.positions.length - 1 ? remaining : energy * side.weights[index];
            tile.working().h[tile.index(local(position))] += amount; remaining -= amount;
        }
    }

    public synchronized int activeRouteCount() {
        int count = 0;
        for (var tile : ordered) if (tile.transport != null)
            for (var route : tile.transport.pairs) if (route.validity.valid()) count++;
        return count;
    }

    public synchronized int environmentBoundaryCount() {
        int count = 0;
        for (var tile : ordered) if (tile.transport != null) count += tile.transport.environment.length;
        return count;
    }

    private void positiveFace(Tile tile, Frame frame, int cell, int pageCell, int axis) {
        int coordinate = pageCell >>> CELL_SHIFTS[axis] & 15;
        var other = coordinate == 15 ? tile.neighbours[axis * 2 + 1] : tile;
        if (!selected(other)) other = null;
        int nextPageCell = pageCell + (coordinate == 15 ? -15 : 1) * CELL_STEPS[axis];
        int next = other == null ? -1 : other.index(nextPageCell);
        var target = other == null ? null : other.working();
        if (target == null || next < 0 || !signatures.valid(target.signatures[next])) {
            exchangeEnvironment(tile, frame, cell, ambientConductance); return;
        }
        var firstLaw = law(frame.signatures[cell]); var secondLaw = law(target.signatures[next]);
        if (secondLaw == null || waiting(frame, cell) || waiting(target, next)) return;
        if (firstLaw == airLaw && secondLaw == airLaw) {
            double firstScale = tile.mixingSources[cell] == 0 ? 1 : AirMixingRegion.MULTIPLIER;
            double secondScale = other.mixingSources[next] == 0 ? 1 : AirMixingRegion.MULTIPLIER;
            if (axis == 1) { firstScale *= tile.buoyancyFactors[cell]; secondScale *= other.buoyancyFactors[next]; }
            double g = airConductance * (firstScale + secondScale) / 2;
            if (g == 0) return;
            ThermalExchangeKernel.exchangePairWithInverseInto(frame.h[cell], airLaw.capacityJPerK(), inverseAirCapacity,
                    target.h[next], airLaw.capacityJPerK(), inverseAirCapacity, g, halfSeconds, airPair);
            frame.h[cell] = airPair.enthalpyAJ(); target.h[next] = airPair.enthalpyBJ(); pairUpdates++;
            return;
        }
        double g = conductance(tile, cell, other, next, axis * 2 + 1);
        if (g == 0) return;
        ThermalExchangeKernel.exchangeMaterialsInto(firstLaw, frame.h[cell], frame.branches[cell], false,
                secondLaw, target.h[next], target.branches[next], false, g, halfSeconds, materialPair);
        frame.h[cell] = materialPair.firstJ(); target.h[next] = materialPair.secondJ();
        frame.branches[cell] = materialPair.firstBranch(); target.branches[next] = materialPair.secondBranch();
        setWaiting(frame, cell, materialPair.firstWaiting()); setWaiting(target, next, materialPair.secondWaiting());
        pairUpdates += materialPair.updates();
    }

    private void negativeBoundary(Tile tile, Frame frame, int cell, int pageCell, int axis) {
        int coordinate = pageCell >>> CELL_SHIFTS[axis] & 15;
        var other = coordinate == 0 ? tile.neighbours[axis * 2] : tile;
        if (!selected(other)) other = null;
        int nextPageCell = pageCell + (coordinate == 0 ? 15 : -1) * CELL_STEPS[axis];
        int next = other == null ? -1 : other.index(nextPageCell);
        var target = other == null ? null : other.working();
        if (target == null || next < 0 || !signatures.valid(target.signatures[next]))
            exchangeEnvironment(tile, frame, cell, ambientConductance);
    }

    private double conductance(Tile tileA, int cellA, Tile tileB, int cellB, int face) {
        int a = tileA.working().signatures[cellA], b = tileB.working().signatures[cellB];
        boolean airA = signatures.isAir(a), airB = signatures.isAir(b);
        var first = materials.profileOrNull(signatures.materialProfileId(a));
        var second = materials.profileOrNull(signatures.materialProfileId(b));
        if (airA) return second == null ? 0 : second.faceConductanceWPerK();
        if (airB) return first == null ? 0 : first.faceConductanceWPerK();
        if ((signatures.fullContactFaces(a) & 1 << face) == 0 || (signatures.fullContactFaces(b) & 1 << (face ^ 1)) == 0) return 0;
        double ga = first.faceConductanceWPerK(), gb = second.faceConductanceWPerK();
        return 2 * ga * gb / (ga + gb);
    }

    private void exchangeEnvironment(Tile tile, Frame frame, int cell, double boundaryConductance) {
        double conductance = boundaryConductance * windScale;
        if (conductance == 0 || waiting(frame, cell)) return;
        var law = law(frame.signatures[cell]);
        double oldJ = frame.h[cell];
        if (law == airLaw) {
            ThermalExchangeKernel.exchangeFixedBoundaryInto(oldJ, airLaw.capacityJPerK(), 0, tile.ambientC,
                    conductance, halfSeconds, airBoundary);
            frame.h[cell] = airBoundary.enthalpyJ();
        } else {
            double remaining = halfSeconds;
            for (int part = 0; part < 6 && remaining > 0; part++) {
                double delta = tile.ambientC - law.temperatureC(frame.h[cell], frame.branches[cell]);
                if (delta == 0) break;
                double direction = Math.signum(delta);
                byte branch = frame.branches[cell] = law.selectBranch(frame.h[cell], frame.branches[cell], direction);
                double limit = law.energyLimitJ(frame.h[cell], branch, direction), slope = law.slopeKPerJ(frame.h[cell], branch);
                double requested = slope == 0 ? Math.abs(delta) * conductance * remaining
                        : Math.abs(delta) / slope * -Math.expm1(-conductance * slope * remaining);
                double amount = Math.min(requested, limit);
                if (amount == 0) break;
                frame.h[cell] += direction * amount;
                var transition = law.transition(branch);
                setWaiting(frame, cell, transition != null && transition.complete(frame.h[cell]));
                if (amount == requested || waiting(frame, cell)) break;
                double elapsed = slope == 0 ? amount / (conductance * Math.abs(delta))
                        : -Math.log1p(-amount * slope / Math.abs(delta)) / (conductance * slope);
                remaining = Math.max(0, remaining - elapsed);
            }
        }
        trialExternalJ += oldJ - frame.h[cell];
    }

    public synchronized boolean commit() {
        if (closed || stage != READY) return false;
        input.commit();
        invalidateQueries();
        acceptedExternalJ = trialExternalJ;
        acceptedRejectedInputJ = trialRejectedInputJ;
        for (var tile : stepTiles) if (!tile.paused()) {
            if (tile.preparedSequence != input.sequence()) continue;
            if (!tile.published) visibilityRevision++;
            tile.published = true;
            tile.acceptGeometryRevision();
            tile.current ^= 1;
            if (halfSeconds != 0) tile.sampleTick = targetTick;
            tile.geometryChanged = false; Arrays.fill(tile.changes, (byte) 0);
        }
        input = null; stepTiles.clear(); stage = IDLE;
        return true;
    }

    @FunctionalInterface
    public interface RestoredAirCells {
        double temperatureC(int brick, int cell);
    }

    /** Import the saved canonical cell temperatures; the sampler is not retained. */
    public synchronized void restorePreparedAir(long section, long bricks, RestoredAirCells temperatures) {
        if (stage != READY) throw new IllegalStateException("Restore values belong to the prepared geometry publication");
        var tile = bySection.get(section); var frame = tile.working();
        for (int cell = 0; cell < frame.h.length; cell++) {
            int brick = Byte.toUnsignedInt(tile.brickIds[cell >>> 6]);
            if ((bricks & 1L << brick) == 0 || law(frame.signatures[cell]) != airLaw) continue;
            double value = temperatures.temperatureC(brick, cell & 63);
            if (!Double.isFinite(value)) continue;
            double delta = (value - tile.ambientC) * airLaw.capacityJPerK();
            frame.h[cell] += delta; trialExternalJ -= delta;
        }
    }

    public synchronized void restorePreparedMaterial(long position, double energy, byte branch) {
        if (stage != READY) throw new IllegalStateException("Restore values belong to the prepared geometry publication");
        var tile = bySection.get(section(position)); int cell = tile.index(local(position));
        writePreparedMaterial(tile, cell, energy, branch);
        var frame = tile.working();
        if (frame.requests != null) frame.requests[cell] = 0;
    }

    /** Original geometry migration hands off before pending input is injected into the new body. */
    public synchronized void prepareMaterialValue(long position, double energy, byte branch) {
        if (stage != PREPARE) throw new IllegalStateException("Geometry values precede numerical input");
        var tile = bySection.get(section(position)); int cell = tile.index(local(position));
        if (tile.preparedSequence != input.sequence()) prepareTile(tile);
        writePreparedMaterial(tile, cell, energy, branch);
    }

    private void writePreparedMaterial(Tile tile, int cell, double energy, byte branch) {
        var frame = tile.working();
        trialExternalJ += frame.h[cell] - energy;
        frame.h[cell] = energy; frame.branches[cell] = branch;
        var edge = law(frame.signatures[cell]).transition(branch);
        setWaiting(frame, cell, edge != null && edge.complete(energy));
    }

    /** Caller first quiesces the sole writer. The accepted field and pending input remain intact. */
    public synchronized void cancel() {
        if (stage == IDLE) return;
        input.cancel(); input = null; stepTiles.clear(); stage = IDLE;
    }

    public synchronized boolean read(long position, boolean material, Sample out) {
        if (closed) return false;
        var tile = bySection.get(section(position));
        if (tile == null) return false;
        var frame = tile.current(); int cell = tile.index(local(position));
        if (cell < 0) return false;
        int signature = frame.signatures[cell];
        var law = law(signature);
        if (law == null || material == signatures.isAir(signature)) return false;
        out.enthalpyJ = frame.h[cell]; out.branch = frame.branches[cell];
        out.law = law;
        out.temperatureC = law.temperatureC(frame.h[cell], out.branch);
        out.sampleTick = tile.sampleTick;
        out.requestSequence = frame.requests == null ? 0 : frame.requests[cell];
        out.materialStateId = material ? signatures.materialStateId(signature) : -1;
        out.materialGeneration = material ? frame.materialGenerations[cell] : 0;
        return true;
    }

    public synchronized boolean sampleAir(double x, double y, double z, Sample out) {
        int bx = (int) Math.floor(x), by = (int) Math.floor(y), bz = (int) Math.floor(z);
        long position = BlockPos.asLong(bx, by, bz);
        var tile = bySection.get(section(position));
        int cell = tile == null ? -1 : tile.index(local(position));
        if (closed || cell < 0 || law(tile.current().signatures[cell]) != airLaw) return false;
        out.temperatureC = sampleAirCell(tile, cell, x - bx, y - by, z - bz);
        out.sampleTick = tile.sampleTick; out.enthalpyJ = tile.current().h[cell]; out.law = airLaw;
        out.branch = 0; out.materialStateId = -1; out.materialGeneration = 0; out.requestSequence = 0;
        return true;
    }

    /** Copies canonical cell means into v7 values; output ownership belongs to the checkpoint. */
    public synchronized AirFieldCheckpoint captureAir(long section, long brickMask, double naturalC) {
        return captureAir(section, brickMask, naturalC, null);
    }

    /** Optional caller-owned geometry/value projection is used by an already captured native save. */
    public synchronized AirFieldCheckpoint captureAir(long section, long brickMask, double naturalC, SectionSnapshot overlay) {
        var tile = bySection.get(section);
        if (closed || tile == null) return null;
        var frame = tile.current();
        var capture = new AirFieldCheckpoint.Capture(naturalC).atTick(tile.sampleTick);
        int x0 = SectionPos.x(section) << 4, y0 = SectionPos.y(section) << 4, z0 = SectionPos.z(section) << 4;
        for (int ordinal = 0; ordinal < tile.brickIds.length; ordinal++) {
            int brick = Byte.toUnsignedInt(tile.brickIds[ordinal]);
            if ((brickMask & 1L << brick) == 0) continue;
            long mask = 0;
            for (int cell = 0; cell < 64; cell++) {
                int signature = overlay == null ? frame.signatures[ordinal * 64 + cell] : overlay.signatures[BlockBrickLayout.pageBlock(brick, cell)];
                if (law(signature) == airLaw) mask |= 1L << cell;
            }
            if (mask == 0) continue;
            double[] values = new double[Long.bitCount(mask)];
            long cells = mask; int valueIndex = 0;
            while (cells != 0) {
                int cell = Long.numberOfTrailingZeros(cells); cells &= cells - 1;
                values[valueIndex] = (overlay == null ? frame.h[ordinal * 64 + cell] : overlay.enthalpyJ[BlockBrickLayout.pageBlock(brick, cell)]) / airLaw.capacityJPerK();
                valueIndex++;
            }
            capture.addCellBrick(x0, y0, z0, brick, mask, values);
        }
        return capture.finish();
    }

    /** Existing phase control supplies metadata at a quiescent boundary; it owns the request lifecycle. */
    public synchronized boolean applyMaterialControl(long position, int generation, double enthalpyJ, byte branch,
            boolean waiting, long requestSequence) {
        if (closed || stage != IDLE) return false;
        if (!Double.isFinite(enthalpyJ) || requestSequence < 0) throw new IllegalArgumentException("Invalid material control");
        var tile = bySection.get(section(position));
        int cell = tile == null ? -1 : tile.index(local(position));
        if (cell < 0) return false;
        var frame = tile.current();
        if (!material(frame.signatures[cell]) || frame.materialGenerations[cell] != generation) return false;
        if (requestSequence != 0 && frame.requests == null) {
            // Both optional arrays are covered by tileBytes before any material or request is admitted.
            frame.requests = new long[frame.h.length];
            var working = tile.working();
            if (working != frame) working.requests = new long[working.h.length];
        }
        acceptedExternalJ += frame.h[cell] - enthalpyJ;
        frame.h[cell] = enthalpyJ; frame.branches[cell] = branch; setWaiting(frame, cell, waiting);
        if (frame.requests != null) frame.requests[cell] = requestSequence;
        return true;
    }

    public synchronized boolean snapshot(long section, SectionSnapshot out) {
        return snapshot(section, out, true);
    }

    /** Query readers take only Accepted; world-save overlays remain a separate caller operation. */
    public synchronized boolean snapshotAccepted(long section, SectionSnapshot out) {
        return snapshot(section, out, false);
    }

    /** Geometry and its material values carry one revision through commit or native freeze. */
    public synchronized void prepareGeometryRevision(long section, long revision) {
        var tile = bySection.get(section);
        if (tile == null) return;
        tile.pendingGeometryRevision = revision;
        if (tile.frozen) tile.acceptGeometryRevision();
    }

    private boolean snapshot(long section, SectionSnapshot out, boolean projectGeometry) {
        var tile = bySection.get(section);
        if (closed || tile == null) return false;
        var frame = tile.current();
        out.section = section; out.sampleTick = tile.sampleTick; out.activeBricks = 0; out.ambientC = tile.ambientC;
        out.geometryRevision = projectGeometry && tile.geometryChanged && tile.pendingGeometryRevision >= 0
                ? tile.pendingGeometryRevision : tile.geometryRevision;
        Arrays.fill(out.signatures, ThermalSignatureTable.UNRESOLVED); Arrays.fill(out.materialGenerations, 0);
        Arrays.fill(out.enthalpyJ, 0); Arrays.fill(out.branches, (byte) 0);
        for (int cell = 0; cell < frame.h.length; cell++) {
            int pageCell = tile.pageCell(cell);
            out.signatures[pageCell] = frame.signatures[cell]; out.materialGenerations[pageCell] = frame.materialGenerations[cell];
            out.enthalpyJ[pageCell] = frame.h[cell];
            out.branches[pageCell] = frame.branches[cell];
            if (projectGeometry && tile.geometryChanged && tile.changes[cell] != 0) {
                var trial = tile.working();
                out.signatures[pageCell] = trial.signatures[cell];
                out.materialGenerations[pageCell] = trial.materialGenerations[cell];
                out.enthalpyJ[pageCell] = projectedEnthalpy(tile, frame, trial, cell);
                var nextLaw = law(trial.signatures[cell]);
                boolean retainBranch = sameMatter(tile, frame, trial, cell)
                        && law(frame.signatures[cell]) == nextLaw && !conversionKeepsMatter(tile, frame, trial, cell);
                out.branches[pageCell] = nextLaw == null ? 0 : retainBranch ? frame.branches[cell]
                        : nextLaw.initialBranch(nextLaw.temperatureC(out.enthalpyJ[pageCell], (byte) 0));
            }
            if (signatures.valid(out.signatures[pageCell])) out.activeBricks |= 1L << Byte.toUnsignedInt(tile.brickIds[cell >>> 6]);
        }
        return true;
    }

    /** Projects an uncaptured world edit into caller-owned save values, never Working. */
    public void projectSnapshotCell(SectionSnapshot out, int pageCell, int next, Change change) {
        int previous = out.signatures[pageCell];
        var before = law(previous); var after = law(next);
        boolean converted = (change == Change.PHASE || change == Change.GAMEPLAY) && material(previous) && material(next);
        boolean same = change != Change.REPLACEMENT && before != null && after != null
                && signatures.isAir(previous) == signatures.isAir(next) && signatures.sameMaterialType(previous, next);
        double nextJ = 0;
        if (after != null) {
            if (converted) nextJ = convertedEnthalpy(before, after, out.enthalpyJ[pageCell], out.branches[pageCell], change == Change.GAMEPLAY);
            else if (same) nextJ = before.afterMassChange(out.enthalpyJ[pageCell], after, out.ambientC);
            else nextJ = after.enthalpyAtTemperature(out.ambientC);
        }
        out.enthalpyJ[pageCell] = nextJ; out.signatures[pageCell] = next;
        if (!same && !converted) out.materialGenerations[pageCell] = 0;
        if (after == null) out.branches[pageCell] = 0;
        else if (!(same && before == after && !converted)) out.branches[pageCell] = after.initialBranch(after.temperatureC(nextJ, (byte) 0));
    }

    /** Source ownership follows already captured world edits, including same-state replacement. */
    public synchronized int currentMaterialStateId(long position, int generation) {
        var tile = bySection.get(section(position));
        int cell = tile == null ? -1 : tile.index(local(position));
        if (closed || cell < 0) return -1;
        var frame = (tile.geometryChanged ? tile.working() : tile.current());
        int signature = frame.signatures[cell];
        return material(signature) && frame.materialGenerations[cell] == generation ? signatures.materialStateId(signature) : -1;
    }

    public synchronized boolean airReceiverMayRemain(long position) {
        var tile = bySection.get(section(position));
        int cell = tile == null ? -1 : tile.index(local(position));
        if (closed) return false;
        if (cell < 0) return true; // A prepaid receiver may be awaiting its first geometry.
        int signature = (tile.geometryChanged ? tile.working() : tile.current()).signatures[cell];
        return !signatures.valid(signature) || law(signature) == airLaw;
    }

    /** Main-owned restore before stepping; the caller has checked saved BlockState identities. */
    private long tileTick(long section) { var tile = bySection.get(section); return tile == null ? initialTick : tile.sampleTick; }
    public synchronized boolean restoreValues(long section, double[] enthalpyJ, byte[] branches) {
        return restoreValues(section, enthalpyJ, branches, null, tileTick(section), tileTick(section), Double.POSITIVE_INFINITY);
    }

    public synchronized boolean restoreValues(long section, double[] enthalpyJ, byte[] branches, AirFieldCheckpoint savedAir,
            long savedTick, long restoreTick, double halfLifeSeconds) {
        var tile = bySection.get(section);
        if (closed || stage != IDLE || tile == null) return false;
        for (var frame : tile.frames) for (int cell = 0; cell < frame.h.length; cell++) {
            int pageCell = tile.pageCell(cell);
            var law = law(frame.signatures[cell]);
            if (law == null) continue;
            frame.h[cell] = enthalpyJ[pageCell]; frame.branches[cell] = branches[pageCell];
            var edge = law.transition(frame.branches[cell]);
            setWaiting(frame, cell, edge != null && edge.complete(frame.h[cell]));
        }
        if (savedAir != null) {
            double rate = DormantThermalCooling.rate(halfLifeSeconds);
            var frame = tile.current();
            for (int cell = 0; cell < frame.h.length; cell++) {
                if (law(frame.signatures[cell]) != airLaw) continue;
                int pageCell = tile.pageCell(cell), brick = Byte.toUnsignedInt(tile.brickIds[cell >>> 6]);
                long sampled = savedAir.sampleTickAt((SectionPos.x(section) << 4) + (pageCell & 15) + .5,
                        (SectionPos.y(section) << 4) + (pageCell >>> 8) + .5,
                        (SectionPos.z(section) << 4) + (pageCell >>> 4 & 15) + .5, savedTick);
                double factor = DormantThermalCooling.factor(sampled, restoreTick, rate);
                double value = savedAir.blockCenterC(brick, cell & 63);
                double temperature = Double.isFinite(value) ? DormantThermalCooling.temperature(value, tile.ambientC, factor)
                        : enthalpyJ[pageCell] * inverseAirCapacity;
                for (var destination : tile.frames) destination.h[cell] = temperature * airLaw.capacityJPerK();
            }
        }
        invalidateQueries();
        return true;
    }

    public synchronized boolean hasSection(long section) { return !closed && bySection.containsKey(section); }

    /** Accepted cell/latent state decides residency; point-query reconstruction owns no heat. */
    public synchronized boolean activeBrick(long section, int brick, double naturalC, double thresholdC) {
        var tile = bySection.get(section);
        int ordinal = tile == null ? -1 : tile.brickSlots[brick];
        if (closed || ordinal < 0 || !tile.published || tile.frozen) return false;
        var frame = tile.current();
        for (int cell = ordinal * 64, end = cell + 64; cell < end; cell++) {
            var law = law(frame.signatures[cell]);
            if (law == null) continue;
            var edge = law.transition(frame.branches[cell]);
            if (waiting(frame, cell) || edge != null && edge.progress(frame.h[cell]) > 0
                    || Math.abs(law.temperatureC(frame.h[cell], frame.branches[cell]) - naturalC) >= thresholdC) return true;
        }
        return false;
    }
    public synchronized boolean hasPublishedSection(long section) {
        var tile = bySection.get(section);
        return !closed && tile != null && tile.published;
    }

    /** Chunk-local lookups over the captured world's vertical range, without scanning other bases. */
    public synchronized void forEachSectionInChunk(long chunk, java.util.function.LongConsumer consumer) {
        if (closed) return;
        for (int y = minimumSectionY; y <= maximumSectionY; y++) {
            long section = SectionPos.asLong(net.minecraft.world.level.ChunkPos.getX(chunk), y, net.minecraft.world.level.ChunkPos.getZ(chunk));
            if (bySection.containsKey(section)) consumer.accept(section);
        }
    }
    public synchronized long activeBricks(long section) {
        var tile = bySection.get(section);
        return closed || tile == null ? 0 : capturedBricks(tile);
    }

    private long capturedBricks(Tile tile) {
        var frame = tile.current(); long captured = 0;
        for (int ordinal = 0; ordinal < tile.brickIds.length; ordinal++)
            for (int cell = ordinal * 64, end = cell + 64; cell < end; cell++) if (signatures.valid(frame.signatures[cell])) {
                captured |= 1L << Byte.toUnsignedInt(tile.brickIds[ordinal]); break;
            }
        return captured;
    }
    public boolean idle() { return stage == IDLE; }
    public synchronized int sectionCellCount(long section) {
        var tile = bySection.get(section);
        return closed || tile == null ? 0 : tile.current().h.length;
    }

    /**
     * The existing numerical arrays become the bounded writeback buffer. A frozen
     * Section contains no world reference, no longer exchanges with neighbours,
     * and keeps its actual sample clock while other Sections continue stepping.
     */
    public synchronized boolean freeze(long section) {
        if (closed || stage != IDLE) return false;
        var tile = bySection.get(section);
        if (tile == null) return false;
        if (tile.geometryChanged) {
            double before = ownedEnthalpyJ(tile.current()); trialExternalJ = acceptedExternalJ;
            if (!tile.geometryPrepared) prepareTile(tile);
            tile.current ^= 1; acceptedExternalJ += before - ownedEnthalpyJ(tile.current());
            tile.acceptGeometryRevision(); tile.geometryChanged = tile.geometryPrepared = false;
            Arrays.fill(tile.changes, (byte) 0);
        }
        tile.frozen = true; tile.activateAt = Long.MIN_VALUE; domainsDirty = true;
        invalidateQueries(); return true;
    }

    public synchronized boolean canAdvance(long section) {
        var tile = bySection.get(section);
        return tile != null && !tile.frozen;
    }

    /** Unload cancellation/re-entry is a physical activation event, separate from numerical input. */
    public synchronized boolean thaw(long section, double ambientC, double halfLifeSeconds) {
        return thaw(section, ambientC, halfLifeSeconds, latestTick(), null);
    }
    public synchronized boolean thaw(long section, double ambientC, double halfLifeSeconds, long tick,
            it.unimi.dsi.fastutil.longs.LongSet influence) {
        if (!stageThaw(section, ambientC, halfLifeSeconds, tick)) return false;
        activate(section, influence); return true;
    }
    /** Native receipts only stage this event. The coordinator discovers influence before activation. */
    public synchronized boolean stageThaw(long section, double ambientC, double halfLifeSeconds, long tick) {
        if (closed || stage != IDLE || !Double.isFinite(ambientC)) return false;
        var tile = bySection.get(section);
        if (tile == null) return false;
        if (!tile.frozen) { tile.ambientC = ambientC; invalidateQueries(); return true; }
        tile.activateAt = Math.max(tick, tile.sampleTick); tile.reentryAmbientC = ambientC;
        tile.reentryHalfLife = halfLifeSeconds; activationRevision++;
        return true;
    }
    @FunctionalInterface public interface ActivationConsumer { void accept(long section, long tick); }
    private long activationRevision;
    public long activationRevision() { return activationRevision; }
    public synchronized void forEachPendingActivation(ActivationConsumer consumer) {
        for (var tile : ordered) if (tile.frozen && tile.activateAt != Long.MIN_VALUE) consumer.accept(tile.section, tile.activateAt);
    }
    public synchronized long pendingActivationTick(long section) {
        var tile = bySection.get(section);
        return tile == null || !tile.frozen ? Long.MIN_VALUE : tile.activateAt;
    }
    public synchronized boolean activate(long section, it.unimi.dsi.fastutil.longs.LongSet influence) {
        if (closed || stage != IDLE) return false;
        var tile = bySection.get(section);
        if (tile == null || !tile.frozen || tile.activateAt == Long.MIN_VALUE) return false;
        if (influence != null && !atBoundary(influence, tile.activateAt)) return false;
        var frame = tile.current(); double rate = DormantThermalCooling.rate(tile.reentryHalfLife);
        for (int cell = 0; cell < frame.h.length; cell++) {
            var law = law(frame.signatures[cell]);
            if (law == null || waiting(frame, cell)) continue;
            double old = frame.h[cell];
            restoredMaterial.setStored(old, law, frame.branches[cell], tile.sampleTick);
            DormantThermalCooling.project(restoredMaterial, tile.activateAt, tile.reentryAmbientC, rate);
            frame.h[cell] = restoredMaterial.enthalpyJ(); frame.branches[cell] = restoredMaterial.branch();
            var edge = law.transition(frame.branches[cell]);
            setWaiting(frame, cell, edge != null && edge.complete(frame.h[cell]));
            acceptedExternalJ += old - frame.h[cell];
        }
        tile.ambientC = tile.reentryAmbientC; tile.sampleTick = tile.activeSince = tile.activateAt; tile.frozen = false;
        tile.activateAt = Long.MIN_VALUE; activationRevision++; domainsDirty = true; invalidateQueries();
        return true;
    }

    public synchronized boolean frozen(long section) {
        var tile = bySection.get(section);
        return tile != null && tile.frozen;
    }
    /** Lifetime boundary, not another advancing clock: no running interval exists before activation. */
    public synchronized long activeSince(long section) {
        var tile = bySection.get(section); return tile == null ? Long.MIN_VALUE : tile.activeSince;
    }

    /** Main updates the physical bath at a cut boundary; the energy reference never changes. */
    public synchronized boolean ambient(long section, double temperatureC) {
        if (closed || stage != IDLE || !Double.isFinite(temperatureC)) return false;
        var tile = bySection.get(section);
        if (tile == null) return false;
        tile.ambientC = temperatureC;
        invalidateQueries();
        return true;
    }

    public synchronized double totalEnthalpyJ() {
        double total = 0;
        for (var tile : ordered) total += ownedEnthalpyJ(tile.current());
        return total;
    }

    private double ownedEnthalpyJ(Frame frame) {
        double total = 0;
        for (double value : frame.h) total += value;
        return total;
    }

    /** Diagnostic watermark only; numerical intervals come from the selected Tiles. */
    public synchronized long acceptedTick() {
        long tick = Long.MAX_VALUE;
        for (var tile : ordered) if (!tile.frozen) tick = Math.min(tick, tile.sampleTick);
        return tick == Long.MAX_VALUE ? latestTick() : tick;
    }
    public synchronized long latestTick() {
        long tick = initialTick;
        for (var tile : ordered) tick = Math.max(tick, tile.sampleTick);
        return tick;
    }
    public double airCapacityJPerK() { return airLaw.capacityJPerK(); }
    public ThermalSignatureTable signatureTable() { return signatures; }
    public long pairUpdates() { return pairUpdates; }
    public long changedCells() { return geometryCells; }
    public long activeNanos() { return activeNanos; }
    public synchronized double externalEnergyJ() { return acceptedExternalJ; }
    public synchronized double rejectedInputJ() { return acceptedRejectedInputJ; }
    public synchronized int tileCount() { return ordered.size(); }

    public synchronized long sectionSampleTick(long section) {
        var tile = bySection.get(section);
        return closed || tile == null ? -1 : tile.sampleTick;
    }

    public synchronized boolean hasFreshSample(long gameTick, int maximumAgeTicks) {
        if (closed) return false;
        for (var tile : ordered) {
            long age = gameTick - tile.sampleTick;
            if (tile.activeBricks != 0 && tile.sampleTick >= 0 && age >= 0 && age <= maximumAgeTicks) return true;
        }
        return false;
    }

    private MaterialThermalLaw law(int signature) {
        if (!signatures.valid(signature)) return null;
        if (signatures.isAir(signature)) return signatures.ventilation(signature) == 0 ? null : airLaw;
        var profile = materials.profileOrNull(signatures.materialProfileId(signature));
        return profile == null ? null : profile.thermalLaw();
    }

    private static boolean waiting(Frame frame, int cell) { return (frame.waiting[cell >>> 6] & 1L << (cell & 63)) != 0; }
    private static void setWaiting(Frame frame, int cell, boolean waiting) {
        if (waiting) frame.waiting[cell >>> 6] |= 1L << (cell & 63);
        else frame.waiting[cell >>> 6] &= ~(1L << (cell & 63));
    }
    private static int local(long position) {
        return (BlockPos.getX(position) & 15) | (BlockPos.getZ(position) & 15) << 4 | (BlockPos.getY(position) & 15) << 8;
    }
    private static long section(long position) {
        return SectionPos.asLong(BlockPos.getX(position) >> 4, BlockPos.getY(position) >> 4, BlockPos.getZ(position) >> 4);
    }
    private static long neighbourSection(long section, int axis, int step) {
        return SectionPos.asLong(SectionPos.x(section) + (axis == 0 ? step : 0), SectionPos.y(section) + (axis == 1 ? step : 0),
                SectionPos.z(section) + (axis == 2 ? step : 0));
    }

    /** Close after quiescing the writer; neighbour links are cleared as well as directories. */
    @Override public synchronized void close() {
        if (closed) return;
        closed = true; input = null; stage = IDLE;
        for (var tile : ordered) {
            Arrays.fill(tile.neighbours, null); tile.storage.close();
            if (tile.transport != null) { tile.transport.close(); tile.transport = null; }
        }
        stepTiles.clear(); domains.clear(); ordered.clear(); ordered.trimToSize(); bySection.clear(); bySection.trim(0); directory.close();
        queryCells = null; queryKeys = null; queryValues = null;
    }
}
