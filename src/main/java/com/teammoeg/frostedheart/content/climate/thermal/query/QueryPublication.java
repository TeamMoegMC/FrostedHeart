/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.climate.thermal.query;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.MaterialSample;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.minecraft.AirFieldCheckpoint;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;

import java.util.Arrays;
import java.util.List;
import java.util.IdentityHashMap;
import com.teammoeg.frostedheart.content.climate.thermal.query.RegionalQueryIndex.Page;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import net.minecraft.core.BlockPos;

/**
 * 由协调者发布、主线程读取的温度与材料快照。
 *
 * <p>slot generation 与区域几何身份拒绝过期读。短发布锁保护目录交接；
 * 读者按需持有不可变区域帧和 Page，直到结束读取。</p>
 */
public final class QueryPublication implements AutoCloseable {
    private final ThermalMemoryBudget budget;
    private final int maximumPages;
    private int[] pageChangeEpochs;
    private int[] brickChangeEpochs;
    private long[] pendingInfraredBrickMasks;
    private int[] dirtyInfraredPages;
    private int dirtyInfraredCount;
    private ThermalMemoryBudget.Reservation pageReservation;

    private boolean acceptingPublications = true;
    private boolean valid;
    private long topologyGeneration = -1L;
    private long sampleTick = -1L;
    private int infraredEpoch;
    private long infraredActiveUntilTick = -1L;
    private ThermalMemoryBudget.Reservation regionalReservation;
    private RegionalQueryIndex regionalIndex;
    private final IdentityHashMap<RegionalReadLease, Boolean> readerLeases = new IdentityHashMap<>();
    private long readerPayloadRefusals, lastReaderPayloadBytes;
    private ThermalMemoryBudget.Reservation closedReaderAirReservation;
    private ThermalMemoryBudget.Reservation closedReaderPageReservation;
    private long regionalFamilyBytes, regionalPageBytes, readerMetadataBytes;
    private static final class Family {
        final long bytes;
        int references;
        Family(long bytes) { this.bytes = bytes; }
    }
    private final IdentityHashMap<Object, Family> payloadFamilies = new IdentityHashMap<>();
    private final IdentityHashMap<RegionalQuerySnapshot, Integer> frameReferences = new IdentityHashMap<>();

    public static final class FrameLease implements AutoCloseable {
        private QueryPublication owner;
        private final RegionalReadLease lease;
        public final RegionalQuerySnapshot field;
        private FrameLease(QueryPublication owner, RegionalQuerySnapshot field, RegionalReadLease lease) {
            this.owner = owner; this.field = field; this.lease = lease;
        }
        @Override public void close() {
            var publication = owner;
            if (publication == null) return;
            synchronized (publication) {
                owner = null; publication.releaseRegionalState(lease);
            }
        }
    }

    public synchronized FrameLease borrowFrame(RegionalQuerySnapshot field) {
        return borrowFrame(field, null);
    }
    public synchronized FrameLease borrowFrame(RegionalQuerySnapshot field, ThermalMemoryBudget.Reservation credit) {
        long extra = additionalFrameBytes(field, null) + (credit == null ? 256 : 0);
        if (!acceptingPublications || regionalReservation == null) return null;
        long required = regionalBytes() + extra;
        regionalReservation.completeResize(required);
        var lease = new RegionalReadLease(this);
        lease.externalMetadata = credit != null;
        if (lease.externalMetadata) lease.metadata = 0;
        lease.frames.put(field.template.handle, field); readerLeases.put(lease, Boolean.TRUE);
        retainFrame(field); readerMetadataBytes += lease.metadata;
        return new FrameLease(this, field, lease);
    }

    private static long arrayBytes(Object array) {
        int length = java.lang.reflect.Array.getLength(array);
        int width = array instanceof byte[] || array instanceof boolean[] ? 1 : array instanceof int[] ? 4 : 8;
        return AirBackingMeter.arrayPayloadBytes(length, width);
    }
    private long additionalFrameBytes(RegionalQuerySnapshot snapshot, IdentityHashMap<Object, Boolean> seen) {
        if (frameReferences.containsKey(snapshot)) return 0;
        long bytes = 192;
        for (Object array : snapshot.payloadArrays()) if (array != null && !payloadFamilies.containsKey(array)
                && (seen == null || seen.put(array, Boolean.TRUE) == null)) bytes += arrayBytes(array) + 64;
        return bytes;
    }
    private void retainFrame(RegionalQuerySnapshot snapshot) {
        Integer references = frameReferences.get(snapshot);
        if (references != null) { frameReferences.put(snapshot, references + 1); return; }
        frameReferences.put(snapshot, 1); regionalFamilyBytes += 192;
        for (Object array : snapshot.payloadArrays()) {
            if (array == null) continue;
            Family family = payloadFamilies.get(array);
            if (family == null) {
                family = new Family(arrayBytes(array) + 64); payloadFamilies.put(array, family);
                regionalFamilyBytes += family.bytes;
            }
            family.references++;
        }
    }
    private void releaseFrame(RegionalQuerySnapshot snapshot) {
        Integer references = frameReferences.get(snapshot);
        if (references == null) return;
        if (references > 1) { frameReferences.put(snapshot, references - 1); return; }
        frameReferences.remove(snapshot); regionalFamilyBytes -= 192;
        for (Object array : snapshot.payloadArrays()) {
            if (array == null) continue;
            Family family = payloadFamilies.get(array);
            if (--family.references == 0) { payloadFamilies.remove(array); regionalFamilyBytes -= family.bytes; }
        }
    }

    /** One read operation borrows only the Pages and physical owners it actually touches. */
    private static final class RegionalReadLease {
        final QueryPublication owner;
        final it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<Page> pages = new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>();
        final Long2ObjectOpenHashMap<Page> sections = new Long2ObjectOpenHashMap<>();
        final IdentityHashMap<RegionalQuerySnapshot.Handle, RegionalQuerySnapshot> frames = new IdentityHashMap<>();
        long metadata = 256;
        boolean refused;
        boolean externalMetadata;
        RegionalReadLease(QueryPublication owner) { this.owner = owner; }
        RegionalQuerySnapshot forSlot(int slot) { return owner.borrowSlot(this, slot); }
        RegionalQuerySnapshot at(double x, double y, double z) { return owner.borrowPoint(this, x, y, z); }
    }
    private final Long2BooleanOpenHashMap infraredRegionFreshness = new Long2BooleanOpenHashMap();

    /** Retained regional payloads and directory storage. */
    private long regionalBytes() {
        return Math.max(1L, 1024L + regionalFamilyBytes + regionalPageBytes + readerMetadataBytes
                + (regionalIndex == null ? 0 : regionalIndex.backingBytes()));
    }
    private void retainPage(Page page) {
        if (page != null && page.references++ == 0) regionalPageBytes += page.backingBytes;
    }
    private void releasePage(Page page) {
        if (page != null && --page.references == 0) regionalPageBytes -= page.backingBytes;
    }

    /** Local owners maintain their totals; an ACK never scans the dimension. */
    public synchronized void reclaimRegionalCapacityAfterAcknowledgement() {
        if (regionalReservation != null && acceptingPublications) regionalReservation.completeResize(regionalBytes());
    }

    public final class RegionInstallation implements AutoCloseable {
        private RegionalQueryIndex.Change change;
        private RegionalReadLease heldPages;
        private final ThermalMemoryBudget.Reservation preparation;
        private RegionInstallation(List<RegionalQuerySnapshot> removed, List<RegionalQuerySnapshot> added) {
            long pages = 0;
            for (var frame : removed) pages += frame.template.distinctPages.length;
            for (var frame : added) pages += frame.template.distinctPages.length;
            preparation = budget.reserveCompletion(1024L + 256L * pages);
            try {
                change = new RegionalQueryIndex.Change(regionalIndex, removed, added);
                preparation.completeResize(change.preparationBytes());
                heldPages = borrowRegionalState(preparation);
                for (var page : change.expectedPages) if (page != null) borrowPage(heldPages, page);
            } catch (RuntimeException | Error failure) {
                if (heldPages != null) releaseRegionalState(heldPages);
                preparation.close(); throw failure;
            }
        }
        public boolean run(long deadline) { return change != null && change.run(deadline); }
        public boolean dependenciesCurrent() {
            synchronized (QueryPublication.this) {
                return acceptingPublications && regionalIndex != null && change != null && change.current(regionalIndex);
            }
        }
        @Override public void close() {
            synchronized (QueryPublication.this) {
                change = null;
                if (heldPages != null) { releaseRegionalState(heldPages); heldPages = null; }
                preparation.close();
            }
        }
    }
    public synchronized RegionInstallation prepareRegionInstallation(List<RegionalQuerySnapshot> removed, List<RegionalQuerySnapshot> added) {
        if (!acceptingPublications) return null;
        if (regionalIndex == null) regionalIndex = new RegionalQueryIndex();
        return new RegionInstallation(removed, added);
    }
    /** Natural-environment controls retain the same geometry directory and owner handles. */
    public synchronized boolean publishEnvironmentBoundary(long topology, long tick, Runnable commit) {
        if (!acceptingPublications) return false;
        commit.run(); topologyGeneration = topology; sampleTick = Math.max(sampleTick, tick);
        return true;
    }

    public synchronized boolean installRegions(RegionInstallation preparation, long topologyGeneration, long targetTick,
            java.util.function.Consumer<RegionalQuerySnapshot[]> installTopology) {
        if (!preparation.dependenciesCurrent() || !preparation.change.ready || regionalReservation == null) return false;
        var change = preparation.change;
        var previous = new RegionalQuerySnapshot[change.removed.length];
        for (int index = 0; index < previous.length; index++) previous[index] = change.removed[index].handle.accepted().field();
        long extra = change.replacementBytes + regionalIndex.growthBytes(change);
        var seen = new IdentityHashMap<Object, Boolean>();
        for (int index = 0; index < change.added.length; index++) {
            var snapshot = change.added[index] = change.added[index].atTopology(topologyGeneration);
            extra += additionalFrameBytes(snapshot, seen);
        }
        regionalReservation.completeResize(regionalBytes() + extra);
        regionalIndex.reserve(change);
        int retained = 0;
        boolean installed = false;
        try {
            for (var snapshot : change.added) { retainFrame(snapshot); retained++; }
            for (var snapshot : change.added) {
                var old = regionalIndex.regions.get(snapshot.template.regionId);
                markRegionalInfrared(snapshot, old == null ? null : old.handle.accepted().field());
            }
            installTopology.accept(change.added);
            regionalIndex.install(change);
            for (var page : change.replacementPages) retainPage(page);
            for (var page : change.expectedPages) releasePage(page);
            for (var snapshot : previous) {
                releaseFrame(snapshot);
                var replacement = regionalIndex.regions.get(snapshot.template.regionId);
                if (replacement == null || replacement.handle != snapshot.template.handle) {
                    var accepted = snapshot.template.handle.accepted();
                    snapshot.template.handle.publish(null, accepted.sampleTick(), accepted.consumedSequence());
                }
                if (replacement == null) infraredRegionFreshness.remove(snapshot.template.regionId);
            }
            this.topologyGeneration = topologyGeneration;
            this.sampleTick = Math.max(this.sampleTick, targetTick);
            valid = true; installed = true;
            if (hasPendingInfraredChangesSafe()) commitInfraredChanges();
        } finally {
            if (!installed) for (int index = 0; index < retained; index++) releaseFrame(change.added[index]);
        }
        preparation.close();
        regionalReservation.completeResize(regionalBytes());
        return true;
    }

    /** All admission precedes the caller's non-allocating physical commit. */
    public synchronized boolean publishRegion(RegionalQuerySnapshot snapshot, Runnable accept) {
        if (!acceptingPublications || regionalIndex == null
                || regionalIndex.regions.get(snapshot.template.regionId) != snapshot.template) return false;
        var previous = snapshot.template.handle.accepted().field();
        if (previous == null || previous.sampleTick > snapshot.sampleTick) return false;
        if (regionalReservation == null) return false;
        regionalReservation.completeResize(regionalBytes() + additionalFrameBytes(snapshot, null));
        markRegionalInfrared(snapshot, previous);
        retainFrame(snapshot);
        try { accept.run(); }
        catch (RuntimeException | Error failure) { releaseFrame(snapshot); throw failure; }
        releaseFrame(previous);
        regionalReservation.completeResize(regionalBytes());
        if (hasPendingInfraredChangesSafe()) commitInfraredChanges();
        return true;
    }

    private boolean hasPendingInfraredChangesSafe() { return pendingInfraredBrickMasks != null && hasPendingInfraredChanges(); }

    private void markRegionalInfrared(RegionalQuerySnapshot next, RegionalQuerySnapshot previous) {
        if (pendingInfraredBrickMasks == null || next.sampleTick > infraredActiveUntilTick) return;
        if (next.minimumQuality() || previous != null && previous.minimumQuality()) {
            next.minimumInfraredEpoch = infraredEpoch = nextInfraredEpoch();
            return;
        }
        if (previous == null || previous.template != next.template) {
            for (int index = 0; index < next.template.distinctPages.length; index++)
                addInfraredChange(next.template.distinctPages[index], next.template.pageBrickMasks[index]);
            return;
        }
        if (previous.airC == next.airC && previous.enthalpyJ == next.enthalpyJ && previous.branches == next.branches) return;
        if (next.hasPreparedInfraredDiff(previous)) {
            for (int page = 0; page < next.changedPageCount(); page++)
                addInfraredChange(next.changedPage(page), next.changedBricks(page));
            return;
        }
        boolean sameTemplate = previous != null && previous.template == next.template;
        boolean airChanged = !sameTemplate || !Arrays.equals(previous.airC, next.airC);
        int body = 0, air = 0;
        for (int cell = 0; cell < next.template.cells.length; cell++) {
            int slot = next.template.cells[cell], page = next.template.pageSlots[cell];
            boolean material = body < next.template.materialSlots.length && next.template.materialSlots[body] == slot;
            double temperature = material ? next.materialTemperatureC(body) : next.componentMeanC(air);
            double previousTemperature = previous == null ? Double.NaN : sameTemplate
                    ? material ? previous.materialTemperatureC(body) : previous.componentMeanC(air)
                    : previous.temperatureC(slot);
            boolean changed = previous == null || quantizedInfrared(previousTemperature) != quantizedInfrared(temperature)
                    || !material && airChanged;
            if (changed) addInfraredChange(page, 1L << Byte.toUnsignedInt(next.template.bricks[cell]));
            if (material) body++; else air++;
        }
    }

    private QueryPublication(ThermalMemoryBudget budget, int maximumPages) {
        this.budget = budget;
        this.maximumPages = maximumPages;
        regionalIndex = new RegionalQueryIndex();
        regionalReservation = budget.reserveCompletion(regionalBytes());
        try { pageReservation = budget.reserveCompletion(projectedPagePayloadBytes(maximumPages)); }
        catch (RuntimeException | Error failure) { regionalReservation.close(); throw failure; }
    }

    public static QueryPublication createRegional(ThermalMemoryBudget budget, int maximumPages) {
        return new QueryPublication(budget, maximumPages);
    }
    /** Infrared epoch and dirty-mask storage for the Page directory. */
    private static long projectedPagePayloadBytes(int maximumPages) {
        return Math.multiplyExact(
                maximumPages, (long) Double.BYTES + 3L * Long.BYTES + 66L * Integer.BYTES);
    }

    /** Geometry remains charged while any directory entry or reader retains it. */
    public synchronized void recordAirLayoutBacking(AirBackingMeter meter) {
        recordCurrentRegionalGeometry(meter);
        for (var lease : readerLeases.keySet())
            for (var frame : lease.frames.values()) frame.recordGeometryBacking(meter);
    }

    private void recordCurrentRegionalGeometry(AirBackingMeter meter) {
        if (regionalIndex == null) return;
        for (var template : regionalIndex.regions.values()) {
            template.air.recordBacking(meter);
            var frame = template.handle.accepted().field();
            if (frame != null) frame.recordGeometryBacking(meter);
        }
    }

    private RegionalReadLease borrowRegionalState(ThermalMemoryBudget.Reservation credit) {
        long metadata = credit == null ? 256 : 0;
        if (regionalReservation == null) {
            readerPayloadRefusals++; return null;
        }
        regionalReservation.completeResize(regionalBytes() + metadata);
        var lease = new RegionalReadLease(this);
        lease.externalMetadata = credit != null; lease.metadata = metadata;
        readerLeases.put(lease, Boolean.TRUE); readerMetadataBytes += metadata;
        lastReaderPayloadBytes = regionalFamilyBytes + regionalPageBytes + readerMetadataBytes;
        return lease;
    }
    private boolean borrowPage(RegionalReadLease lease, Page page) {
        if (page == null || lease.refused) return false;
        if (lease.pages.containsKey(page.slot)) return lease.pages.get(page.slot) == page;
        long metadata = 192L + 96L * page.templates.length;
        long extra = 0;
        for (var template : page.templates) if (!lease.frames.containsKey(template.handle)) {
            var frame = template.handle.accepted().field();
            if (frame == null) { lease.refused = true; return false; }
            extra += additionalFrameBytes(frame, null);
        }
        if (!acceptingPublications || regionalReservation == null) {
            readerPayloadRefusals++; lease.refused = true; return false;
        }
        regionalReservation.completeResize(regionalBytes() + extra + (lease.externalMetadata ? 0 : metadata)
                + (page.references == 0 ? page.backingBytes : 0));
        lease.pages.put(page.slot, page); if (page.hasAir) lease.sections.put(page.section, page);
        retainPage(page);
        if (!lease.externalMetadata) { lease.metadata += metadata; readerMetadataBytes += metadata; }
        for (var template : page.templates) if (!lease.frames.containsKey(template.handle)) {
            var frame = template.handle.accepted().field(); lease.frames.put(template.handle, frame); retainFrame(frame);
        }
        return true;
    }
    private synchronized RegionalQuerySnapshot borrowSlot(RegionalReadLease lease, int slot) {
        for (var page : lease.pages.values()) {
            var template = page.cells.get(slot);
            if (template != null) return lease.frames.get(template.handle);
        }
        if (regionalIndex == null) return null;
        var template = regionalIndex.cells.get(slot);
        if (template == null) return null;
        int cell = template.cellIndex(slot);
        var page = regionalIndex.pages.get(template.pageSlots[cell]);
        return borrowPage(lease, page) ? lease.frames.get(template.handle) : null;
    }
    private synchronized RegionalQuerySnapshot borrowPoint(RegionalReadLease lease, double x, double y, double z) {
        int bx = (int) Math.floor(x), by = (int) Math.floor(y), bz = (int) Math.floor(z);
        long section = net.minecraft.core.SectionPos.asLong(bx >> 4, by >> 4, bz >> 4);
        var page = lease.sections.get(section);
        if (page == null) {
            page = regionalIndex == null ? null : regionalIndex.sections.get(section);
            if (!borrowPage(lease, page)) return null;
        }
        var candidates = page.bricks.get(BlockPos.asLong(bx & ~3, by & ~3, bz & ~3));
        if (candidates != null) for (var template : candidates)
            if (template.air.componentAt(bx, by, bz) != null) return lease.frames.get(template.handle);
        return null;
    }

    /** Minimum publications invalidate one owner. Visible Pages resolve that revision lazily. */
    private synchronized int minimumPageEpoch(RegionalReadLease lease, int pageSlot) {
        var page = lease.pages.get(pageSlot);
        if (page == null) {
            page = regionalIndex == null ? null : regionalIndex.pages.get(pageSlot);
            if (!borrowPage(lease, page)) return 0;
        }
        int epoch = 0;
        for (var template : page.templates) {
            var field = lease.frames.get(template.handle);
            if (field != null) epoch = Math.max(epoch, field.minimumInfraredEpoch);
        }
        return epoch;
    }
    private synchronized boolean currentReader(RegionalReadLease lease) {
        if (!acceptingPublications || lease.refused || regionalIndex == null) return false;
        for (var page : lease.pages.values()) if (regionalIndex.pages.get(page.slot) != page) return false;
        for (var entry : lease.frames.entrySet()) if (entry.getKey().accepted().field() != entry.getValue()) return false;
        return true;
    }
    private synchronized boolean anyFreshReader(RegionalReadLease lease, long gameTick, int maximumAgeTicks) {
        if (lease.refused) return false;
        if (!lease.frames.isEmpty()) {
            for (var frame : lease.frames.values()) if (frame.sampleTick >= 0 && gameTick >= frame.sampleTick && gameTick - frame.sampleTick <= maximumAgeTicks) return true;
            return false;
        }
        if (regionalIndex != null) for (var template : regionalIndex.regions.values()) {
            var frame = template.handle.accepted().field();
            if (frame != null && frame.sampleTick >= 0 && gameTick >= frame.sampleTick && gameTick - frame.sampleTick <= maximumAgeTicks) return true;
        }
        return false;
    }

    /** Diagnostic scan of explicitly borrowed payloads, outside ordinary publication. */
    private long readerPayloadBytes() {
        AirBackingMeter meter = new AirBackingMeter();
        for (var lease : readerLeases.keySet()) {
            for (var frame : lease.frames.values()) frame.recordPayloadBacking(meter);
            for (var page : lease.pages.values()) meter.objectOnce(page, page.backingBytes);
            meter.bytes(lease.metadata);
        }
        int leases = readerLeases.size();
        meter.container(readerLeases, leases, 16);
        meter.bytes(64L * leases);
        return Math.max(1L, meter.bytes());
    }

    private synchronized void releaseRegionalState(RegionalReadLease lease) {
        if (readerLeases.remove(lease) == null) return;
        readerMetadataBytes -= lease.metadata;
        for (var snapshot : lease.frames.values()) releaseFrame(snapshot);
        for (var page : lease.pages.values()) releasePage(page);
        lease.frames.clear(); lease.pages.clear(); lease.sections.clear();
        if (!acceptingPublications && regionalReservation != null) {
            if (readerLeases.isEmpty()) { regionalReservation.close(); regionalReservation = null; }
            else regionalReservation.tryResize(regionalBytes());
        } else if (regionalReservation != null) {
            // The last reader, not an unrelated future rebuild, releases the
            // retired frame's overlap and wakes waiting preparation.
            regionalReservation.completeResize(regionalBytes());
        }
        if (readerLeases.isEmpty() && closedReaderPageReservation != null) {
            closedReaderPageReservation.close();
            closedReaderPageReservation = null;
        }
        if (closedReaderAirReservation != null) {
            if (readerLeases.isEmpty()) {
                closedReaderAirReservation.close();
                closedReaderAirReservation = null;
            } else closedReaderAirReservation.tryResize(Math.max(1L, readerAirBackingBytes()));
        }
    }

    private long readerAirBackingBytes() {
        AirBackingMeter meter = new AirBackingMeter(true);
        for (var lease : readerLeases.keySet())
            for (var frame : lease.frames.values()) frame.recordGeometryBacking(meter);
        return meter.bytes();
    }

    /** Transfer an existing Air charge on shutdown; readers release it after their last use. */
    public synchronized boolean retainReaderAirBacking(ThermalMemoryBudget.Reservation reservation) {
        if (readerLeases.isEmpty()) return false;
        reservation.tryResize(Math.max(1L, readerAirBackingBytes()));
        closedReaderAirReservation = reservation;
        return true;
    }

    public synchronized int retainedReaderCount() {
        return readerLeases.size();
    }

    public synchronized long retainedReaderPayloadBytes() {
        return readerLeases.isEmpty() ? 0L : readerPayloadBytes();
    }

    /** Read-only production observation of snapshot borrowing capacity. */
    public synchronized String describeReaderMemory() {
        return "queryUsedBytes=" + budget.usedBytes() + ",queryLimitBytes=" + budget.limitBytes()
                + ",regionalReservedBytes=" + (regionalReservation == null ? 0 : regionalReservation.bytes())
                + ",lastReaderPayloadBytes=" + lastReaderPayloadBytes + ",readerPayloadRefusals=" + readerPayloadRefusals
                + ",retainedReaders=" + retainedReaderCount();
    }

    public synchronized long closedReaderAirBackingBytes() {
        return closedReaderAirReservation == null ? 0 : closedReaderAirReservation.bytes();
    }

    public synchronized long closedReaderPageBackingBytes() {
        return closedReaderPageReservation == null ? 0 : closedReaderPageReservation.bytes();
    }

    public synchronized long retiredReaderAirBytes() {
        AirBackingMeter meter = new AirBackingMeter(true);
        recordCurrentRegionalGeometry(meter);
        long current = meter.bytes();
        for (var lease : readerLeases.keySet())
            for (var frame : lease.frames.values()) frame.recordGeometryBacking(meter);
        return meter.bytes() - current;
    }

    private RegionalQuerySnapshot frameAt(double x, double y, double z) {
        var template = regionalIndex == null ? null : regionalIndex.at(x, y, z);
        return template == null ? null : template.handle.accepted().field();
    }
    private synchronized RegionalQuerySnapshot frameForSlot(int slot) {
        var template = regionalIndex == null ? null : regionalIndex.cells.get(slot);
        return template == null ? null : template.handle.accepted().field();
    }
    public synchronized long regionAt(double x, double y, double z) {
        var template = regionalIndex == null ? null : regionalIndex.at(x, y, z);
        return template == null ? -1 : template.regionId;
    }

    /** Exact-position temperature from one coherent accepted frame. */
    public boolean tryReadAir(double x, double y, double z, MutableSample out) {
        return readAir(x, y, z, -1L, 0, out, false);
    }

    public boolean tryReadCapturedAir(double x, double y, double z,
            long gameTick, int maximumAgeTicks, MutableSample out) {
        return readAir(x, y, z, gameTick, maximumAgeTicks, out, true);
    }

    private synchronized boolean readAir(double x, double y, double z,
            long gameTick, int maximumAgeTicks, MutableSample out, boolean captured) {
        out.clear();
        // A world mutation can change geometry tokens while this frame is sampled.
        for (int attempt = 0; attempt < 2; attempt++) {
            var snapshot = frameAt(x, y, z);
            var component = snapshot == null ? null : snapshot.template.air.componentAt((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
            if (component == null || !(captured ? component.capturedGeometry(gameTick, maximumAgeTicks) : component.currentGeometry())) {
                return false;
            }
            double temperature = snapshot.airTemperatureC(x, y, z);
            if (!Double.isFinite(temperature)) return false;
            if (!(captured ? component.capturedGeometry(gameTick, maximumAgeTicks) : component.currentGeometry())) continue;
            out.set(temperature, snapshot.sampleTick); out.airBasisTerms = component.pointBasisCount();
            out.minimumQuality = snapshot.minimumQuality(); return true;
        }
        return false;
    }
    /** Read-only diagnosis for production GameTest observations of Air fallback. */
    public synchronized String describeAirRead(double x, double y, double z) {
        var snapshot = frameAt(x, y, z);
        if (snapshot == null) return "component_missing,tick=-1";
        var component = snapshot.template.air.componentAt((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        return (component.currentGeometry() ? "geometry_current" : "geometry_mismatch," + component.describeGeometryMismatch())
                + ",tick=" + snapshot.sampleTick + ",basis=" + component.basisCount() + ",region=" + snapshot.template.regionId
                + ",minimum=" + snapshot.minimumQuality();
    }
    /**
     * Extends dimension-wide infrared tracking and returns whether it was
     * reactivated after the previous window expired.
     */
    public synchronized boolean noteInfraredRequest(long gameTick, int activeTicks) {
        requireNonNegative("gameTick", gameTick);
        if (activeTicks <= 0) {
            throw new IllegalArgumentException("activeTicks must be positive");
        }
        if (!acceptingPublications) {
            return false;
        }
        long deadline = Math.addExact(gameTick, activeTicks);
        boolean reactivated = gameTick > infraredActiveUntilTick;
        infraredActiveUntilTick = Math.max(infraredActiveUntilTick, deadline);
        int[] newPageEpochs = null;
        int[] newBrickEpochs = null;
        long[] newPendingMasks = null;
        if (reactivated && pageChangeEpochs == null) {
            newPageEpochs = new int[maximumPages];
            newBrickEpochs = new int[Math.multiplyExact(maximumPages, 64)];
            newPendingMasks = new long[maximumPages];
        }
        if (!reactivated) {
            updateRegionalInfraredFreshness(gameTick);
            return false;
        }
        if (newPageEpochs != null) {
            pageChangeEpochs = newPageEpochs;
            brickChangeEpochs = newBrickEpochs;
            pendingInfraredBrickMasks = newPendingMasks;
            dirtyInfraredPages = new int[maximumPages];
        }
        int next = nextInfraredEpoch();
        Arrays.fill(pageChangeEpochs, next);
        Arrays.fill(brickChangeEpochs, next);
        Arrays.fill(pendingInfraredBrickMasks, 0L); dirtyInfraredCount = 0;
        infraredEpoch = next;
        updateRegionalInfraredFreshness(gameTick);
        return true;
    }

    /** Freshness changes invalidate only the affected region, even with unchanged temperature. */
    private void updateRegionalInfraredFreshness(long gameTick) {
        if ((regionalIndex == null) || pendingInfraredBrickMasks == null) return;
        for (var template : regionalIndex.regions.values()) {
            var snapshot = template.handle.accepted().field();
            if (snapshot == null) continue;
            long age = gameTick - snapshot.sampleTick;
            boolean fresh = snapshot.sampleTick >= 0 && age >= 0 && age <= 40;
            long id = snapshot.template.regionId;
            boolean changed = !infraredRegionFreshness.containsKey(id) || infraredRegionFreshness.get(id) != fresh;
            infraredRegionFreshness.put(id, fresh);
            if (changed) {
                if (snapshot.minimumQuality()) snapshot.minimumInfraredEpoch = infraredEpoch = nextInfraredEpoch();
                else for (int page = 0; page < snapshot.template.distinctPages.length; page++)
                    addInfraredChange(snapshot.template.distinctPages[page], snapshot.template.pageBrickMasks[page]);
            }
        }
        if (hasPendingInfraredChangesSafe()) commitInfraredChanges();
    }

    /** Borrow an infrared/save reader; each touched Page and frame has its own lifetime. */
    public boolean beginRead(ReadCursor out) {
        if (out == null) throw new IllegalArgumentException("out is required");
        out.clear();
        synchronized (this) {
            if (!acceptingPublications || !valid || regionalIndex == null) return false;
            var lease = borrowRegionalState(null);
            if (lease == null) return false;
            out.owner = this; out.regionalLease = lease; out.valid = true;
            out.topologyGeneration = topologyGeneration; out.sampleTick = sampleTick;
            out.infraredEpoch = infraredEpoch; out.pageChangeEpochs = pageChangeEpochs;
            out.brickChangeEpochs = brickChangeEpochs;
            return true;
        }
    }
    /** Allocation-free slot lookup in one immutable Accepted frame. */
    public boolean tryRead(
            int arenaSlot,
            int expectedSlotGeneration,
            long minimumTopologyGeneration,
            MutableSample out) {
        if (arenaSlot < 0 || expectedSlotGeneration < 0 || minimumTopologyGeneration < 0L) {
            throw new IllegalArgumentException("query identity is invalid");
        }
        if (out == null) {
            throw new IllegalArgumentException("out is required");
        }
        out.clear();
        var snapshot = frameForSlot(arenaSlot);
        int cell = snapshot == null ? -1 : snapshot.template.cellIndex(arenaSlot);
        if (cell < 0 || snapshot.template.generations[cell] != expectedSlotGeneration
                || snapshot.topologyGeneration < minimumTopologyGeneration) return false;
        double temperature = snapshot.temperatureC(arenaSlot);
        if (!Double.isFinite(temperature)) return false;
        out.set(temperature, snapshot.sampleTick);
        return true;
    }
    /** Material identity follows its regional frame; unrelated topology does not invalidate it. */
    public synchronized boolean tryReadMaterial(
            int arenaSlot, int expectedGeneration, int expectedStateId, MaterialSample out) {
        out.clear();
        var snapshot = frameForSlot(arenaSlot);
        int cell = snapshot == null ? -1 : snapshot.template.cellIndex(arenaSlot);
        int body = snapshot == null ? -1 : snapshot.template.bodyIndex(arenaSlot);
        if (cell < 0 || body < 0 || snapshot.template.generations[cell] != expectedGeneration
                || snapshot.materialStateId(body) != expectedStateId) return false;
        out.setLive(snapshot.materialEnthalpyJ(body), snapshot.template.laws[body], snapshot.materialBranch(body),
                snapshot.sampleTick, snapshot.materialRequest(body));
        return true;
    }

    @Override
    public synchronized void close() {
        acceptingPublications = false;
        clearEnvelope();
        if (regionalReservation != null) {
            if (readerLeases.isEmpty()) { regionalReservation.close(); regionalReservation = null; }
            else regionalReservation.tryResize(regionalBytes());
        }
        if (pageReservation != null) {
            if (readerLeases.isEmpty()) pageReservation.close();
            else closedReaderPageReservation = pageReservation;
        }
        pageReservation = null;
        pageChangeEpochs = null;
        brickChangeEpochs = null;
        pendingInfraredBrickMasks = null;
        dirtyInfraredPages = null; dirtyInfraredCount = 0;
    }

    private void clearEnvelope() {
        if (regionalIndex != null) {
            for (var template : regionalIndex.regions.values()) {
                var accepted = template.handle.accepted();
                if (accepted.field() != null) releaseFrame(accepted.field());
                template.handle.publish(null, accepted.sampleTick(), accepted.consumedSequence());
            }
            for (var page : regionalIndex.pages.values()) releasePage(page);
        }
        valid = false;
        topologyGeneration = -1L;
        sampleTick = -1L;
        regionalIndex = null;
        infraredRegionFreshness.clear();
    }

    private void addInfraredChange(int page, long mask) {
        if (mask == 0) return;
        if (pendingInfraredBrickMasks[page] == 0) dirtyInfraredPages[dirtyInfraredCount++] = page;
        pendingInfraredBrickMasks[page] |= mask;
    }
    private boolean hasPendingInfraredChanges() { return dirtyInfraredCount != 0; }

    private void commitInfraredChanges() {
        int next = nextInfraredEpoch();
        for (int index = 0; index < dirtyInfraredCount; index++) {
            int pageSlot = dirtyInfraredPages[index];
            long remaining = pendingInfraredBrickMasks[pageSlot];
            pageChangeEpochs[pageSlot] = next;
            int firstBrick = pageSlot << 6;
            while (remaining != 0) {
                int brick = Long.numberOfTrailingZeros(remaining);
                brickChangeEpochs[firstBrick + brick] = next; remaining &= remaining - 1;
            }
            pendingInfraredBrickMasks[pageSlot] = 0;
        }
        dirtyInfraredCount = 0; infraredEpoch = next;
    }

    private int nextInfraredEpoch() {
        if (infraredEpoch == Integer.MAX_VALUE) {
            Arrays.fill(pageChangeEpochs, 0);
            Arrays.fill(brickChangeEpochs, 0);
            if (regionalIndex != null) for (var template : regionalIndex.regions.values()) {
                var field = template.handle.accepted().field();
                if (field != null) field.minimumInfraredEpoch = 0;
            }
            return 1;
        }
        return infraredEpoch + 1;
    }

    static short quantizedInfrared(double temperatureC) {
        long value = Math.round(temperatureC * 4.0D);
        return (short) Math.max(-32767L, Math.min(32767L, value));
    }

    private static void requireNonNegative(String name, long value) {
        if (value < 0L) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    public static final class MutableSample {
        private double temperatureC;
        private long sampleTick;
        private int airBasisTerms;
        private boolean minimumQuality;

        public int airBasisTerms() { return airBasisTerms; }
        public boolean minimumQuality() { return minimumQuality; }

        public double temperatureC() {
            return temperatureC;
        }

        public long sampleTick() {
            return sampleTick;
        }

        private void set(double temperatureC, long sampleTick) {
            this.temperatureC = temperatureC;
            this.sampleTick = sampleTick;
            airBasisTerms = 1;
            minimumQuality = false;
        }

        private void clear() {
            temperatureC = Double.NaN;
            sampleTick = -1L;
            airBasisTerms = 0;
            minimumQuality = false;
        }
    }

    /** Caller-owned coherent view of one published infrared/query cut. */
    public static final class ReadCursor {
        private QueryPublication owner;
        private boolean valid;
        private long topologyGeneration;
        private long sampleTick;
        private int infraredEpoch;
        private int[] pageChangeEpochs;
        private int[] brickChangeEpochs;
        private RegionalReadLease regionalLease;

        /** Read-only support diagnosis; a component's basis count alone cannot locate its local detail. */
        public int airAggregationSpanAt(double x, double y, double z) {
            if (!valid) return -1;
            var snapshot = (regionalLease.at(x, y, z));
            var layout = snapshot == null ? null : snapshot.template.air;
            var component = layout == null ? null : layout.componentAt((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
            return component == null || !component.currentGeometry() ? -1
                    : snapshot.airAggregationSpan();
        }

        /** Requires pageAirSampleTick(page) >= 0 on this lease; those immutable frames remain pinned. */
        public AirFieldCheckpoint captureValidatedAir(
                com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication page, double naturalC) {
            var capture = new AirFieldCheckpoint.Capture(naturalC);
            for (int brick = 0; brick < 64; brick++) {
                var payload = page.brick(brick);
                if (payload.firstSlot() < 0) continue;
                for (int node = 0; node < payload.airNodeCount(); node++) {
                    int slot = payload.firstSlot() + node;
                    var snapshot = regionalLease.forSlot(slot);
                    capture.atTick(snapshot.sampleTick);
                    if (snapshot.minimumQuality()) capture.add(snapshot.template.air,
                            (x, y, z) -> snapshot.componentTemperatureC(slot, x, y, z),
                            brick, slot, payload.arenaGeneration());
                    else capture.add(snapshot.template.air, snapshot.airC,
                            snapshot.referenceC, brick, slot, payload.arenaGeneration());
                }
            }
            return capture.finish();
        }
        public boolean valid() {
            return valid;
        }

        public boolean hasFreshSample(long gameTick, int maximumAgeTicks) {
            if (!valid) return false;
            return owner.anyFreshReader(regionalLease, gameTick, maximumAgeTicks);
        }

        public boolean freshSlot(int slot, int generation, long minimumTopology, long gameTick, int maximumAgeTicks) {
            if (!valid) return false;
            var snapshot = regionalLease.forSlot(slot);
            int cell = snapshot == null ? -1 : snapshot.template.cellIndex(slot);
            return cell >= 0 && snapshot.template.generations[cell] == generation
                    && snapshot.topologyGeneration >= minimumTopology && snapshot.sampleTick >= 0
                    && gameTick - snapshot.sampleTick >= 0 && gameTick - snapshot.sampleTick <= maximumAgeTicks;
        }

        /** Pins and validates all Air identities; material capture retains its independent times. */
        public long pageAirSampleTick(com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication page) {
            if (!valid) return -1;
            long tick = -1;
            for (int brick = 0; brick < 64; brick++) {
                var payload = page.brick(brick);
                if (payload.firstSlot() < 0) continue;
                for (int node = 0; node < payload.airNodeCount(); node++) {
                    int slot = payload.firstSlot() + node;
                    var snapshot = regionalLease.forSlot(slot);
                    int cell = snapshot == null ? -1 : snapshot.template.cellIndex(slot);
                    if (cell < 0 || snapshot.template.generations[cell] != payload.arenaGeneration()
                            || snapshot.topologyGeneration < page.topologyGeneration()) return -1;
                    var component = snapshot.template.air.componentForSlot(slot);
                    if (component == null || component.arenaGeneration != payload.arenaGeneration()) return -1;
                    tick = tick < 0 ? snapshot.sampleTick : Math.min(tick, snapshot.sampleTick);
                }
            }
            return tick < 0 ? sampleTick : tick;
        }

        public int infraredEpoch() {
            return infraredEpoch;
        }

        public long topologyGeneration() {
            return topologyGeneration;
        }

        public int pageChangeEpoch(int pageSlot) {
            if (pageSlot < 0 || pageChangeEpochs == null || pageSlot >= pageChangeEpochs.length) {
                throw new IllegalArgumentException("Page slot is outside the read cursor");
            }
            return Math.max(pageChangeEpochs[pageSlot], (owner.minimumPageEpoch(regionalLease, pageSlot)));
        }

        public int brickChangeEpoch(int pageSlot, int brickIndex) {
            if (pageSlot < 0
                    || pageChangeEpochs == null
                    || pageSlot >= pageChangeEpochs.length
                    || brickIndex < 0
                    || brickIndex >= 64) {
                throw new IllegalArgumentException("Brick is outside the read cursor");
            }
            return Math.max(brickChangeEpochs[(pageSlot << 6) + brickIndex],
                    (owner.minimumPageEpoch(regionalLease, pageSlot)));
        }

        public boolean tryRead(
                int arenaSlot,
                int expectedSlotGeneration,
                long minimumTopologyGeneration,
                MutableSample out) {
            if (arenaSlot < 0
                    || expectedSlotGeneration < 0
                    || minimumTopologyGeneration < 0L
                    || out == null) {
                throw new IllegalArgumentException("infrared query identity is invalid");
            }
            out.clear();
            if (!valid) return false;
            var snapshot = regionalLease.forSlot(arenaSlot);
            int cell = snapshot == null ? -1 : snapshot.template.cellIndex(arenaSlot);
            if (cell < 0 || snapshot.template.generations[cell] != expectedSlotGeneration
                    || snapshot.topologyGeneration < minimumTopologyGeneration) return false;
            double temperature = snapshot.temperatureC(arenaSlot);
            if (!Double.isFinite(temperature)) return false;
            out.set(temperature, snapshot.sampleTick);
            return true;
        }
        public boolean isCurrent() {
            return owner != null && ((owner.currentReader(regionalLease)));
        }

        /** Pinned material values use the same immutable envelope as checkpoint Air. */
        public boolean tryReadMaterial(int slot, int generation, int stateId, MaterialSample out) {
            out.clear();
            if (!valid) return false;
            var snapshot = regionalLease.forSlot(slot);
            int cell = snapshot == null ? -1 : snapshot.template.cellIndex(slot);
            int body = snapshot == null ? -1 : snapshot.template.bodyIndex(slot);
            if (cell < 0 || body < 0 || snapshot.template.generations[cell] != generation
                    || snapshot.materialStateId(body) != stateId) return false;
            out.setLive(snapshot.materialEnthalpyJ(body), snapshot.template.laws[body], snapshot.materialBranch(body),
                    snapshot.sampleTick, snapshot.materialRequest(body));
            return true;
        }

        /** Previous Page metadata is usable only while every participating region precedes its replacement. */
        public boolean pageBeforeTopology(com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication page,
                long maximumTopologyExclusive) {
            if (!valid) return false;
            for (int brick = 0; brick < 64; brick++) {
                var payload = page.brick(brick);
                if (payload.firstSlot() < 0) continue;
                int count = payload.airNodeCount() + (payload.blockLayout() == null ? 0
                        : Long.bitCount(payload.blockLayout().materialNodeMask()));
                for (int node = 0; node < count; node++) {
                    var snapshot = regionalLease.forSlot(payload.firstSlot() + node);
                    if (snapshot == null || snapshot.topologyGeneration >= maximumTopologyExclusive) return false;
                }
            }
            return true;
        }

        /** Release the borrowed publication and arrays after the read operation. */
        public void clear() {
            var borrowed = regionalLease;
            regionalLease = null;
            owner = null;
            valid = false;
            topologyGeneration = -1L;
            sampleTick = -1L;
            infraredEpoch = 0;
            pageChangeEpochs = null;
            brickChangeEpochs = null;
            if (borrowed != null) borrowed.owner.releaseRegionalState(borrowed);
        }
    }
}
