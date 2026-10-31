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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirFieldLayout;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.LocalAirShape;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.ThermalInputBatch;
import com.teammoeg.frostedheart.content.climate.thermal.source.minecraft.WorkerPhysicalSourceBindings.FrozenSources;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.IdentityHashMap;

/** Shared-grid source support and saved support lifetime, confined to the thermal worker. */
public final class AirShapeStore {
    private static final int WIDTH = LocalAirShape.WIDTH;
    private static final int CELLS = WIDTH * WIDTH * WIDTH;
    private static final int[] DX = {-1, 1, 0, 0, 0, 0};
    private static final int[] DY = {0, 0, -1, 1, 0, 0};
    private static final int[] DZ = {0, 0, 0, 0, -1, 1};
    private final ThermalSignatureTable signatures;
    private final Long2ObjectOpenHashMap<Footprint[]> footprints = new Long2ObjectOpenHashMap<>();
    private final Long2LongOpenHashMap supportRequests = new Long2LongOpenHashMap();
    private int footprintHighWater;
    private long supportFinds, supportCacheHits, supportVisitedCells, supportDiscoveryNanos, residualScanNanos;

    public String gridPreparationSummary() {
        return "supportFinds=" + supportFinds + ",supportCacheHits=" + supportCacheHits
                + ",supportVisitedCells=" + supportVisitedCells + ",supportDiscoveryUs=" + supportDiscoveryNanos / 1000
                + ",residualScanUs=" + residualScanNanos / 1000;
    }

    public AirShapeStore(ThermalSignatureTable signatures) { this.signatures = signatures; }

    /** Candidate preparation may replace footprint metadata without changing the installed cache. */
    public AirShapeStore copy() {
        AirShapeStore candidate = new AirShapeStore(signatures);
        for (var entry : footprints.long2ObjectEntrySet()) {
            Footprint[] source = entry.getValue();
            Footprint[] copies = new Footprint[source.length];
            for (int index = 0; index < source.length; index++) {
                Footprint footprint = source[index];
                if (footprint == null) continue;
                Footprint next = new Footprint(footprint.position, footprint.face);
                next.active = footprint.active;
                next.amplitudeC = footprint.amplitudeC;
                next.restoredAmplitudeC = footprint.restoredAmplitudeC;
                next.restorationComplete = footprint.restorationComplete;
                next.sourceResponsePending = footprint.sourceResponsePending;
                next.savedPieces.addAll(footprint.savedPieces);
                next.gridSupport = footprint.gridSupport;
                next.support.putAll(footprint.support);
                copies[index] = next;
            }
            candidate.footprints.put(entry.getLongKey(), copies);
        }
        candidate.footprintHighWater = candidate.footprints.size();
        return candidate;
    }

    public void recordBacking(AirBackingMeter meter) {
        meter.containerEntries(footprintHighWater, 16);
        meter.containerEntries(supportRequests.size(), 16);
        for (Footprint[] atBlock : footprints.values()) {
            meter.array(atBlock);
            for (Footprint footprint : atBlock) {
                if (footprint == null) continue;
                meter.bytes(80);
                if (footprint.gridSupport != null) {
                    var cache = footprint.gridSupport;
                    meter.container(cache, 0, 0);
                    meter.container(cache.geometry, 0, 1);
                    meter.array(cache.geometry.handles); meter.array(cache.geometry.revisions);
                    meter.array(cache.bricks); meter.array(cache.airMasks);
                }
                // R6 intersects at most eight sections; include the map's reserved zero-key slot.
                meter.containerEntries(Math.max(9, footprint.support.size()), 16);
                meter.containerEntries(footprint.savedPieces.size(), 8);
                for (LocalAirShape shape : footprint.savedPieces) shape.recordBacking(meter);
            }
        }
    }

    public void appendSupportRequests(Long2LongOpenHashMap requests) {
        for (var entry : supportRequests.long2LongEntrySet())
            requests.put(entry.getLongKey(), requests.get(entry.getLongKey()) | entry.getLongValue());
    }

    /** Copies footprint metadata only for shapes belonging to a new or merged region. */
    public void inherit(AirShapeStore previous, java.util.function.LongPredicate ownsSource,
            java.util.function.Predicate<LocalAirShape> ownsShape) {
        for (var entry : previous.footprints.long2ObjectEntrySet()) {
            Footprint[] source = entry.getValue();
            Footprint[] selected = null;
            for (int face = 0; face < source.length; face++) {
                Footprint footprint = source[face];
                if (footprint == null) continue;
                boolean included = ownsSource.test(footprint.position);
                if (!included) for (LocalAirShape shape : footprint.savedPieces)
                    if (ownsShape.test(shape)) { included = true; break; }
                if (!included) continue;
                if (selected == null) selected = footprints.computeIfAbsent(entry.getLongKey(), ignored -> new Footprint[6]);
                if (selected[face] != null) continue;
                Footprint next = new Footprint(footprint.position, footprint.face);
                next.active = footprint.active;
                next.amplitudeC = footprint.amplitudeC;
                next.restoredAmplitudeC = footprint.restoredAmplitudeC;
                next.restorationComplete = footprint.restorationComplete;
                next.sourceResponsePending = footprint.sourceResponsePending;
                next.savedPieces.addAll(footprint.savedPieces);
                next.gridSupport = footprint.gridSupport;
                next.support.putAll(footprint.support);
                selected[face] = next;
            }
        }
        footprintHighWater = Math.max(footprintHighWater, footprints.size());
    }

    public void restore(ThermalInputBatch.DormantAirCut cut, java.util.function.Predicate<LocalAirShape> ownsShape) {
        var checkpoint = cut.entry().spatialAir();
        if (checkpoint == null) return;
        Set<Footprint> replaced = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int index = 0; index < checkpoint.shapeCount(); index++) {
            LocalAirShape shape = checkpoint.shape(index).sectionSlice(checkpoint.minX(), checkpoint.minY(), checkpoint.minZ());
            if (shape == null || !ownsShape.test(shape)) continue;
            Footprint[] atBlock = footprints.computeIfAbsent(shape.sourceBlock, ignored -> new Footprint[6]);
            if (atBlock[shape.sourceFace] == null) atBlock[shape.sourceFace] = new Footprint(shape.sourceBlock, shape.sourceFace);
            Footprint footprint = atBlock[shape.sourceFace];
            if (replaced.add(footprint)) footprint.savedPieces.removeIf(old ->
                    old.minX >= checkpoint.minX() && old.minX < checkpoint.minX() + 16
                            && old.minY >= checkpoint.minY() && old.minY < checkpoint.minY() + 16
                            && old.minZ >= checkpoint.minZ() && old.minZ < checkpoint.minZ() + 16);
            footprint.savedPieces.add(shape);
            footprint.restoredAmplitudeC = Math.max(footprint.restoredAmplitudeC, Math.abs(checkpoint.amplitudeC(index) * cut.decayFactor()));
            footprint.restorationComplete = false;
        }
        footprintHighWater = Math.max(footprintHighWater, footprints.size());
    }

    @FunctionalInterface
    public interface ResidualSample { double maximumC(AirFieldLayout.Component component, double naturalC); }
    public GridPreparation beginSharedGrid(AirFieldLayout captured, AirFieldLayout previous, double[] previousC,
            AirGeometryView geometry, FrozenSources sources, double referenceC, ResidualSample residual) {
        return new GridPreparation(captured, previous, previousC, geometry, sources, referenceC, residual);
    }

    /** One source support is bounded by its existing radius; completed sources are never rescanned on yield. */
    public final class GridPreparation {
        private final AirFieldLayout captured, previous;
        private final double[] previousC;
        private final AirGeometryView geometry;
        private final double referenceC;
        private final long[] positions;
        private final ResidualSample residual;
        private int cursor;
        private GridPreparation(AirFieldLayout captured, AirFieldLayout previous, double[] previousC,
                AirGeometryView geometry, FrozenSources sources, double referenceC, ResidualSample residual) {
            this.captured = captured; this.previous = previous; this.previousC = previousC;
            this.geometry = geometry; this.referenceC = referenceC;
            this.residual = residual;
            supportFinds = supportCacheHits = supportVisitedCells = supportDiscoveryNanos = residualScanNanos = 0;
            for (Footprint[] atBlock : footprints.values()) for (Footprint footprint : atBlock)
                if (footprint != null) { footprint.active = false; footprint.amplitudeC = 0; }
            sources.collectAirPorts(geometry, (x, y, z, face) -> {
                long position = BlockPos.asLong(x, y, z);
                Footprint[] atBlock = footprints.computeIfAbsent(position, ignored -> new Footprint[6]);
                if (atBlock[face.ordinal()] == null) atBlock[face.ordinal()] = new Footprint(position, (byte) face.ordinal());
                atBlock[face.ordinal()].active = true;
            });
            footprintHighWater = Math.max(footprintHighWater, footprints.size());
            supportRequests.clear();
            positions = footprints.keySet().toLongArray(); Arrays.sort(positions);
            }
        private int faceCursor;
        private boolean retainedPosition;
        private SupportDiscovery discovery;

        public boolean run(long deadline) {
            while (cursor < positions.length) {
                if (!preparePosition(positions[cursor], deadline)) return false;
                cursor++;
                if (System.nanoTime() >= deadline) break;
            }
            return cursor == positions.length;
        }

        private boolean preparePosition(long position, long deadline) {
            Footprint[] atBlock = footprints.get(position);
            while (faceCursor < atBlock.length) {
                Footprint footprint = atBlock[faceCursor];
                if (footprint == null) { faceCursor++; continue; }
                int x = BlockPos.getX(position), y = BlockPos.getY(position), z = BlockPos.getZ(position);
                if (discovery == null && !footprint.active && previous != null) {
                    long residualStarted = System.nanoTime();
                    for (int bx = (x - 6) & ~3; bx <= x + 6; bx += 4)
                        for (int by = (y - 6) & ~3; by <= y + 6; by += 4)
                            for (int bz = (z - 6) & ~3; bz <= z + 6; bz += 4) {
                                var components = previous.borrowedComponentsAtBrick(BlockPos.asLong(bx, by, bz));
                                if (components == null) continue;
                                var page = geometry.pageAt(SectionPos.asLong(bx >> 4, by >> 4, bz >> 4));
                                double natural = page == null ? referenceC : page.naturalTemperatureC;
                                for (var component : components) footprint.amplitudeC = Math.max(footprint.amplitudeC, residual != null
                                        ? residual.maximumC(component, natural) : Math.max(
                                        Math.abs(referenceC + component.minimumOffsetC(previousC) - natural),
                                        Math.abs(referenceC + component.maximumOffsetC(previousC) - natural)));
                            }
                    residualScanNanos += System.nanoTime() - residualStarted;
                }
                if (!footprint.active && footprint.amplitudeC < .01 && footprint.savedPieces.isEmpty()) { faceCursor++; continue; }
                retainedPosition = true;
                long supportStarted = System.nanoTime();
                SupportGeometry support;
                if (discovery == null && footprint.gridSupport != null && footprint.savedPieces.isEmpty()
                        && !footprint.sourceResponsePending && footprint.gridSupport.current(captured, geometry)) {
                    support = footprint.gridSupport.geometry; supportCacheHits++;
                } else {
                    if (discovery == null) discovery = new SupportDiscovery(footprint, captured, geometry);
                    if (!discovery.run(deadline)) { supportDiscoveryNanos += System.nanoTime() - supportStarted; return false; }
                    support = discovery.result; discovery = null;
                    if (support.complete) footprint.gridSupport = cacheGridSupport(footprint, support, captured);
                }
                supportDiscoveryNanos += System.nanoTime() - supportStarted;
                footprint.sourceResponsePending = false; footprint.restorationComplete = true;
                footprint.savedPieces.clear();
                for (var request : footprint.support.long2LongEntrySet())
                    supportRequests.put(request.getLongKey(), supportRequests.get(request.getLongKey()) | request.getLongValue());
                faceCursor++;
                if (System.nanoTime() >= deadline && faceCursor < atBlock.length) return false;
            }
            if (!retainedPosition) footprints.remove(position);
            faceCursor = 0; retainedPosition = false;
            return true;
        }
    }

    /** Only called after the candidate field has been projected and committed. */
    public void projected() {
        for (Footprint[] atBlock : footprints.values()) for (Footprint footprint : atBlock) {
            if (footprint == null || footprint.savedPieces.isEmpty() || !footprint.restorationComplete) continue;
            footprint.savedPieces.clear();
            footprint.restoredAmplitudeC = 0;
            // The live source response belongs to current geometry, not to saved fragments.
            footprint.sourceResponsePending = footprint.active;
        }
    }

    /** Saved support is consumed only after its representation has been prepared. */
    public boolean canConsumeRestoration(long section) {
        int x = net.minecraft.core.SectionPos.x(section) << 4, y = net.minecraft.core.SectionPos.y(section) << 4,
                z = net.minecraft.core.SectionPos.z(section) << 4;
        for (var atBlock : footprints.values()) for (var footprint : atBlock) {
            if (footprint == null || footprint.restorationComplete) continue;
            for (var piece : footprint.savedPieces)
                if (piece.minX >= x && piece.minX < x + 16 && piece.minY >= y && piece.minY < y + 16
                        && piece.minZ >= z && piece.minZ < z + 16) return false;
        }
        return true;
    }

    public boolean hasPendingSourceResponse() {
        for (Footprint[] atBlock : footprints.values()) for (Footprint footprint : atBlock)
            if (footprint != null && footprint.sourceResponsePending) return true;
        return false;
    }

    /** Uniform fallback keeps captured support/history, without another fine support search. */
    public void holdUniformSupport() {
        for (Footprint[] atBlock : footprints.values()) for (Footprint footprint : atBlock)
            if (footprint != null) { footprint.sourceResponsePending = false; footprint.restorationComplete = true; }
    }

    /** Source-connected Air traversal. A yielded search keeps both its queue and dependency order. */
    private final class SupportDiscovery {
        final Footprint footprint;
        final AirFieldLayout coarse;
        final AirGeometryView geometry;
        final int minX, minY, minZ;
        final boolean[] air = new boolean[CELLS], visited = new boolean[CELLS];
        final int[] queue = new int[CELLS];
        final LinkedHashMap<ThermalPageHandle, Long> dependencies = new LinkedHashMap<>();
        final List<LocalAirShape> history;
        int head, tail, connectedCount, historyCursor;
        boolean complete = true;
        SupportGeometry result;

        SupportDiscovery(Footprint footprint, AirFieldLayout coarse, AirGeometryView geometry) {
            supportFinds++; footprint.gridSupport = null;
            this.footprint = footprint; this.coarse = coarse; this.geometry = geometry;
            minX = BlockPos.getX(footprint.position) - LocalAirShape.RADIUS;
            minY = BlockPos.getY(footprint.position) - LocalAirShape.RADIUS;
            minZ = BlockPos.getZ(footprint.position) - LocalAirShape.RADIUS;
            footprint.support.clear();
            int source = LocalAirShape.RADIUS * (1 + WIDTH + WIDTH * WIDTH);
            queue[tail++] = source; visited[source] = true;
            history = footprint.savedPieces;
            if (history.isEmpty()) historyCursor = CELLS;
        }

        boolean run(long deadline) {
            do {
                if (historyCursor < CELLS) {
                    int end = Math.min(CELLS, historyCursor + 128);
                    for (; historyCursor < end; historyCursor++) {
                        int cell = historyCursor;
                        if (visited[cell]) continue;
                        int x = minX + cell % WIDTH, y = minY + cell / (WIDTH * WIDTH), z = minZ + cell / WIDTH % WIDTH;
                        for (LocalAirShape saved : history) if (saved.containsCell(x, y, z)) {
                            visited[cell] = true; queue[tail++] = cell; break;
                        }
                    }
                } else if (head < tail) {
                    int remaining = 128;
                    while (remaining-- > 0 && head < tail) {
                        int cell = queue[head++];
                        int x = minX + cell % WIDTH, y = minY + cell / (WIDTH * WIDTH), z = minZ + cell / WIDTH % WIDTH;
                        long section = SectionPos.asLong(x >> 4, y >> 4, z >> 4);
                        int brick = (x & 15) >>> 2 | ((z & 15) >>> 2) << 2 | ((y & 15) >>> 2) << 4;
                        footprint.support.put(section, footprint.support.get(section) | 1L << brick);
                        int signature = geometry.signatureAtWorld(x, y, z);
                        var page = geometry.pageAt(section);
                        if (!signatures.valid(signature) || page == null) { complete = false; continue; }
                        dependencies.put(page.handle, geometry.geometryRevision(page));
                        if (!signatures.isAir(signature) || signatures.ventilation(signature) == 0) continue;
                        air[cell] = coarse.componentAt(x, y, z) != null;
                        if (air[cell]) connectedCount++; else complete = false;
                        for (int face = 0; face < 6; face++) {
                            int adjacent = neighbor(cell, face);
                            if (adjacent >= 0 && !visited[adjacent]) { visited[adjacent] = true; queue[tail++] = adjacent; }
                        }
                    }
                } else {
                    var handles = dependencies.keySet().toArray(ThermalPageHandle[]::new);
                    long[] revisions = new long[dependencies.size()]; int index = 0;
                    for (long revision : dependencies.values()) revisions[index++] = revision;
                    supportVisitedCells += tail;
                    result = new SupportGeometry(air, handles, revisions, connectedCount, complete);
                    return true;
                }
            } while (System.nanoTime() < deadline);
            return false;
        }
    }

    private record SupportGeometry(boolean[] air, ThermalPageHandle[] handles, long[] revisions,
            int connectedCount, boolean complete) {}

    private static GridSupportCache cacheGridSupport(Footprint footprint, SupportGeometry support, AirFieldLayout captured) {
        int size = 0;
        for (long mask : footprint.support.values()) size += Long.bitCount(mask);
        long[] bricks = new long[size], masks = new long[size];
        int index = 0;
        for (var entry : footprint.support.long2LongEntrySet()) {
            long section = entry.getLongKey(), remaining = entry.getLongValue();
            while (remaining != 0) {
                int brick = Long.numberOfTrailingZeros(remaining); remaining &= remaining - 1;
                long origin = BlockPos.asLong((SectionPos.x(section) << 4) + (brick & 3) * 4,
                        (SectionPos.y(section) << 4) + (brick >>> 4) * 4, (SectionPos.z(section) << 4) + (brick >>> 2 & 3) * 4);
                bricks[index] = origin; masks[index++] = captured.airBlocksAtBrick(origin);
            }
        }
        return new GridSupportCache(support, bricks, masks);
    }

    /** Only complete searches are cached; Page identity and Air membership cover capture/ownership changes. */
    private record GridSupportCache(SupportGeometry geometry, long[] bricks, long[] airMasks) {
        boolean current(AirFieldLayout captured, AirGeometryView view) {
            for (int index = 0; index < geometry.handles.length; index++) {
                var handle = geometry.handles[index];
                var page = view.pageAt(handle.sectionKey());
                if (page == null || page.handle != handle || view.geometryRevision(page) != geometry.revisions[index]) return false;
            }
            for (int index = 0; index < bricks.length; index++)
                if (captured.airBlocksAtBrick(bricks[index]) != airMasks[index]) return false;
            return true;
        }
    }

    /** Local address for one source-support neighbor. */
    private static int neighbor(int cell, int face) {
        int x = cell % WIDTH + DX[face], y = cell / (WIDTH * WIDTH) + DY[face], z = cell / WIDTH % WIDTH + DZ[face];
        return x < 0 || x >= WIDTH || y < 0 || y >= WIDTH || z < 0 || z >= WIDTH ? -1 : x + WIDTH * (z + WIDTH * y);
    }

    private static final class Footprint {
        final long position;
        final byte face;
        boolean active;
        double amplitudeC;
        double restoredAmplitudeC;
        boolean restorationComplete;
        boolean sourceResponsePending;
        final ArrayList<LocalAirShape> savedPieces = new ArrayList<>();
        GridSupportCache gridSupport;
        final Long2LongOpenHashMap support = new Long2LongOpenHashMap(8);
        Footprint(long position, byte face) { this.position = position; this.face = face; }
    }
}
