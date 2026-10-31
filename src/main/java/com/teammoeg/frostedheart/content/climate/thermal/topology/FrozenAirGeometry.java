/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ArenaSpan;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockFace;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings.FrozenSources;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.BitSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

/** Immutable numerical geometry and captured emitter routing, without live planning state or temperature arrays. */
public final class FrozenAirGeometry implements AirGeometryView, AutoCloseable {
    @jdk.jfr.Name("frostedheart.FrozenAirGeometry")
    @jdk.jfr.StackTrace(false)
    private static final class CaptureObservation extends jdk.jfr.Event {
        public long reservedBytes, actualBackingBytes;
        public int haloBound, haloPositions, sectionBound, capturedPages, retainedCells;
        public boolean admitted;
        public String stage;
    }
    private final ThermalCellArena arena;
    private final ThermalMemoryBudget.Reservation reservation;
    private final Long2ObjectOpenHashMap<WorkerPageStore.PageState> pages;
    private final Int2ObjectOpenHashMap<WorkerPageStore.PageState> pagesBySlot;
    private final Long2ObjectOpenHashMap<BrickCells> bricks;
    private final Long2ObjectOpenHashMap<Outlet> routedPorts;
    private final Long2IntOpenHashMap halo;
    private final Long2ObjectOpenHashMap<HaloRead> haloReads;
    // Helpers only write true. Completion of every region task precedes validation.
    private static final class HaloRead {
        final long version;
        boolean read;
        HaloRead(long version) { this.version = version; }
    }
    private final BitSet cells;
    private final int pageCapacityBound, routeCapacityBound, cellCapacityBound;
    private final ArrayList<ThermalCellArena.GeometryReadLease> nodeLeases = new ArrayList<>();
    private int references = 1, retainedSpans;
    private boolean ownerClosed, cellsRetained;

    static FrozenAirGeometry capture(TopologyView view, ThermalCellArena arena, FrozenSources sources,
            ThermalSignatureTable signatures, ThermalMemoryBudget.Reservation scratch) {
        long[] counts = {0, 0};
        view.forEachCandidatePage(page -> {
            counts[0] += signatureBytes(view.signatures(page));
            long mask = view.residentMask(page);
            while (mask != 0) {
                int brick = Long.numberOfTrailingZeros(mask); mask &= mask - 1;
                if (view.brick(page, brick).span.count() != 0) counts[1]++;
            }
        });
        // Includes page shells/maps, shared signature payload retention, exact Air
        // membership, bitset growth and all possible span-pin/map allocations.
        int haloBound = view.capturedHaloSignatureBound();
        int sectionBound = view.capturedSectionBound(haloBound);
        long required = 4096L + 2176L * view.candidatePageBound() + counts[0] + 384L * counts[1]
                + haloMapBytes(haloBound) + 256L * sectionBound + 192L * sources.activeAirPortCount()
                + 2L * ((arena.requiredSlotCapacity() + 7L) / 8L);
        var observation = jdk.jfr.FlightRecorder.isInitialized() ? new CaptureObservation() : null;
        if (observation != null) {
            observation.stage = "capture";
            observation.reservedBytes = required; observation.haloBound = haloBound; observation.sectionBound = sectionBound;
        }
        var reservation = scratch.reserveCompletionSibling(required);
        try {
            var geometry = new FrozenAirGeometry(view, arena, sources, signatures, reservation, haloBound, sectionBound);
            if (observation != null && observation.isEnabled()) {
                observation.actualBackingBytes = -1;
                if (AirBackingMeter.debugEnabled()) {
                    try (var meter = new AirBackingMeter()) { geometry.recordBacking(meter); observation.actualBackingBytes = meter.bytes(); }
                }
                observation.admitted = true;
                observation.haloPositions = geometry.halo.size(); observation.capturedPages = geometry.pages.size();
                observation.commit();
            }
            return geometry;
        }
        catch (RuntimeException | Error failure) { reservation.close(); throw failure; }
    }

    /** The preallocated primitive map owns one long[] and one int[], with no entry objects. */
    private static long haloMapBytes(int entries) {
        int slots = Math.addExact(it.unimi.dsi.fastutil.HashCommon.arraySize(entries, .75f), 1);
        return 128L + AirBackingMeter.arrayPayloadBytes(slots, Long.BYTES)
                + AirBackingMeter.arrayPayloadBytes(slots, Integer.BYTES);
    }

    private FrozenAirGeometry(TopologyView view, ThermalCellArena arena, FrozenSources sources,
            ThermalSignatureTable signatures, ThermalMemoryBudget.Reservation reservation, int haloBound, int sectionBound) {
        this.arena = arena;
        this.reservation = reservation;
        int pageBound = view.candidatePageBound();
        pageCapacityBound = pageBound;
        routeCapacityBound = sources.activeAirPortCount();
        cellCapacityBound = arena.requiredSlotCapacity();
        pages = new Long2ObjectOpenHashMap<>(pageBound);
        pagesBySlot = new Int2ObjectOpenHashMap<>(pageBound);
        bricks = new Long2ObjectOpenHashMap<>();
        routedPorts = new Long2ObjectOpenHashMap<>(sources.activeAirPortCount());
        halo = view.copyHaloSignatures(haloBound);
        haloReads = new Long2ObjectOpenHashMap<>(sectionBound);
        for (long position : halo.keySet()) {
            long section = SectionPos.asLong(BlockPos.getX(position) >> 4, BlockPos.getY(position) >> 4, BlockPos.getZ(position) >> 4);
            haloReads.computeIfAbsent(section, key -> new HaloRead(view.committedPages().haloVersion(key)));
        }
        view.forEachCaptureSection(section -> haloReads.computeIfAbsent(section,
                key -> new HaloRead(view.committedPages().haloVersion(key))));
        cells = new BitSet(arena.requiredSlotCapacity());
        view.forEachCandidatePage(page -> {
            long resident = view.residentMask(page);
            var copy = WorkerPageStore.PageState.geometryMetadata(page, view.signatures(page),
                    view.geometryRevision(page), resident);
            pages.put(page.handle.sectionKey(), copy);
            pagesBySlot.put(page.pageSlot, copy);
            while (resident != 0) {
                int index = Long.numberOfTrailingZeros(resident); resident &= resident - 1;
                var brick = view.brick(page, index);
                if (brick.span.count() == 0) continue;
                byte[] airNodes = null;
                if (brick.airNodeCount != 0 && brick.blockLayout != null) {
                    airNodes = new byte[64];
                    for (int block = 0; block < 64; block++) {
                        int node = brick.nodeAt(block);
                        airNodes[block] = (byte) (node >= 0 && node < brick.airNodeCount ? node : 255);
                    }
                }
                bricks.put(AirRouteCompiler.brickPosition(page, index), new BrickCells(brick.span,
                        page.pageSlot, page.lifecycleGeneration, brick.airNodeCount, brick.cellsResolved, airNodes));
                cells.set(brick.span.firstSlot(), brick.span.endSlotExclusive());
            }
        });
        var target = new WorkerPageStore.MutableAirTarget();
        sources.collectOriginalAirPorts((x, y, z, face) -> {
            long position = BlockPos.asLong(x, y, z);
            if (!view.capturesSection(SectionPos.asLong(x >> 4, y >> 4, z >> 4))) {
                int existing = view.committedPages().resolveAirFaceTarget(x, y, z, face, signatures, target);
                if (existing < 0 || !view.capturesSection(SectionPos.asLong(
                        target.blockX() >> 4, target.blockY() >> 4, target.blockZ() >> 4))) return;
            }
            if (airSlotAt(x, y, z) >= 0 || routedPorts.containsKey(position)) return;
            int slot = view.resolveAirFaceTarget(x, y, z, face, signatures, target);
            if (slot >= 0) routedPorts.put(position, new Outlet(slot, target.generation(),
                    target.blockX(), target.blockY(), target.blockZ(), target.face()));
        });
    }

    /** Only changed numerical regions pin addresses; stable regions continue using their installed state. */
    public void retainCells(BitSet selected) {
        if (cellsRetained) throw new IllegalStateException("geometry cells were already retained");
        int cellCount = 0;
        for (BrickCells brick : bricks.values()) {
            if (selectedInSpan(selected, brick.span)) cellCount += brick.span.count();
        }
        // capture() already prepays every possible span pin, lease and map entry.
        // Pinning does not copy cells: their columns and mixed geometry remain
        // owned and charged by ThermalCellArena, even after a span is retired.
        long required = reservation.bytes();
        var observation = jdk.jfr.FlightRecorder.isInitialized() ? new CaptureObservation() : null;
        if (observation != null) {
            observation.stage = "retain"; observation.reservedBytes = required;
            observation.retainedCells = cellCount; observation.haloPositions = halo.size(); observation.capturedPages = pages.size();
        }
        cellsRetained = true;
        for (BrickCells brick : bricks.values()) {
            if (!selectedInSpan(selected, brick.span)) continue;
            nodeLeases.add(arena.retainGeometry(brick.pageSlot, brick.generation, brick.span));
            retainedSpans++;
        }
        if (observation != null && observation.isEnabled()) {
            observation.actualBackingBytes = -1;
            if (AirBackingMeter.debugEnabled()) {
                try (var meter = new AirBackingMeter()) { recordBacking(meter); observation.actualBackingBytes = meter.bytes(); }
            }
            observation.admitted = true; observation.commit();
        }
    }

    private static boolean selectedInSpan(BitSet selected, ArenaSpan span) {
        // A Brick has at most 64 cells. Never scan an unrelated base's address gap.
        for (int slot = span.firstSlot(); slot < span.endSlotExclusive(); slot++)
            if (selected.get(slot)) return true;
        return false;
    }

    public int retainedSpanCount() { return retainedSpans; }
    public long reservedBytes() { return reservation.bytes(); }
    public boolean retainsCell(int slot) {
        for (var lease : nodeLeases) if (lease.containsCell(slot)) return true;
        return false;
    }

    @Override public boolean ownsCell(ThermalCellArena arena, int slot) { return slot >= 0 && cells.get(slot); }
    @Override public CellCursor cellCursor(ThermalCellArena arena) {
        return new CellCursor() {
            int next = cells.nextSetBit(0);
            @Override public boolean hasNext() { return next >= 0; }
            @Override public int nextSlot() { int slot = next; next = cells.nextSetBit(slot + 1); return slot; }
        };
    }
    @Override public WorkerPageStore.PageState pageAt(long section) { return pages.get(section); }
    @Override public WorkerPageStore.PageState pageAtSlot(int slot) { return pagesBySlot.get(slot); }
    @Override public long geometryRevision(WorkerPageStore.PageState page) { return page.geometryRevision; }
    @Override public int signatureAtWorld(int x, int y, int z) {
        var page = pages.get(SectionPos.asLong(x >> 4, y >> 4, z >> 4));
        // Support discovery cannot traverse an unavailable Page (pageAt()==null).
        // Another base's halo is not an input to this frozen numerical closure.
        if (page == null) return ThermalSignatureTable.UNRESOLVED;
        int brick = (x & 15) >>> 2 | ((z & 15) >>> 2) << 2 | ((y & 15) >>> 2) << 4;
        if (page != null && (page.residentBrickMask & 1L << brick) != 0)
            return page.signatures.get((x & 15) | (z & 15) << 4 | (y & 15) << 8);
        var dependency = haloReads.get(SectionPos.asLong(x >> 4, y >> 4, z >> 4));
        if (dependency != null) dependency.read = true;
        return halo.get(BlockPos.asLong(x, y, z));
    }

    /** Coordinator-only, after all helpers using this frozen geometry have completed. */
    public boolean dependenciesCurrent(WorkerPageStore pages) {
        for (var entry : haloReads.long2ObjectEntrySet())
            if (entry.getValue().read && pages.haloVersion(entry.getLongKey()) != entry.getValue().version) return false;
        return true;
    }

    public int capturedCellCount() { return cells.cardinality(); }
    private int airSlotAt(int x, int y, int z) {
        var brick = bricks.get(BlockPos.asLong(x & ~3, y & ~3, z & ~3));
        if (brick == null || !brick.resolved || brick.airNodes == 0) return -1;
        int node = brick.blockAirNodes == null ? 0
                : Byte.toUnsignedInt(brick.blockAirNodes[(x & 3) | (z & 3) << 2 | (y & 3) << 4]);
        return node < brick.airNodes ? brick.span.firstSlot() + node : -1;
    }
    @Override public int resolveAirFaceTarget(int x, int y, int z, BlockFace face,
            ThermalSignatureTable signatures, WorkerPageStore.MutableAirTarget out) {
        int slot = airSlotAt(x, y, z);
        if (slot >= 0) {
            var page = pages.get(SectionPos.asLong(x >> 4, y >> 4, z >> 4));
            out.set(page.lifecycleGeneration, x, y, z, face);
            return slot;
        }
        var outlet = routedPorts.get(BlockPos.asLong(x, y, z));
        if (outlet == null) return WorkerPageStore.PORT_TOPOLOGY_UNAVAILABLE;
        out.set(outlet.generation, outlet.x, outlet.y, outlet.z, outlet.face);
        return outlet.slot;
    }

    /** Additional candidate readers release on the coordinator, after their helpers have settled. */
    public ReadLease retainRead() {
        if (references == 0) throw new IllegalStateException("geometry input was released");
        var readerBacking = reservation.reserveCompletionSibling(96);
        references++;
        return new ReadLease(readerBacking);
    }
    public final class ReadLease implements AutoCloseable {
        private final ThermalMemoryBudget.Reservation readerBacking;
        private boolean closed;
        private ReadLease(ThermalMemoryBudget.Reservation readerBacking) { this.readerBacking = readerBacking; }
        @Override public void close() {
            if (!closed) {
                closed = true;
                try { release(); } finally { readerBacking.close(); }
            }
        }
    }
    @Override public void close() { if (!ownerClosed) { ownerClosed = true; release(); } }
    private void release() {
        if (--references != 0) return;
        try { for (var lease : nodeLeases) lease.close(); }
        finally { nodeLeases.clear(); reservation.close(); }
    }

    /** Diagnostic upper bound, including shared signatures even while live Pages also own them. */
    private static long signatureBytes(PageSignatures signatures) {
        long bytes = 16 + 64 * 8;
        for (int brick = 0; brick < 64; brick++) {
            Object payload = signatures.brickPayload(brick);
            if (payload instanceof char[] values) bytes += 16L + values.length * 2L;
            else if (payload instanceof int[] values) bytes += 16L + values.length * 4L;
        }
        return bytes;
    }

    public void recordBacking(AirBackingMeter meter) {
        meter.container(pages, pageCapacityBound, 16);
        meter.container(pagesBySlot, pageCapacityBound, 12);
        meter.container(bricks, bricks.size(), 16);
        meter.container(routedPorts, routeCapacityBound, 16);
        meter.container(halo, halo.size(), 12);
        meter.container(haloReads, haloReads.size(), 16); meter.bytes(32L * haloReads.size());
        meter.container(nodeLeases, nodeLeases.size(), 8);
        meter.bytes(128L * pages.size() + 48L * (bricks.size() + routedPorts.size())
                + 128L * nodeLeases.size() + 16L + 8L * ((cellCapacityBound + 63L) / 64L));
        for (var page : pages.values()) {
            meter.array(page.bricks); meter.array(page.firstExposedLocalY); page.signatures.recordBacking(meter);
        }
        for (var brick : bricks.values()) meter.array(brick.blockAirNodes);
    }

    private record BrickCells(ArenaSpan span, int pageSlot, int generation, int airNodes,
            boolean resolved, byte[] blockAirNodes) {}
    private record Outlet(int slot, int generation, int x, int y, int z, BlockFace face) {}

}
