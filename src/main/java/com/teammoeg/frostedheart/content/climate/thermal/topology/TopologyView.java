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

import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.AirBackingMeter;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockFace;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

/** Read-only planning projection over the plan's single draft authority. */
final class TopologyView implements AirGeometryView {
    static final int[] DX = {-1, 1, 0, 0, 0, 0};
    static final int[] DY = {0, 0, -1, 1, 0, 0};
    static final int[] DZ = {0, 0, 0, 0, -1, 1};
    private final WorkerPageStore pages;
    private final Long2ByteOpenHashMap materialSurfaceCache = new Long2ByteOpenHashMap();
    private final Long2ByteOpenHashMap materialOwnerCache = new Long2ByteOpenHashMap();
    private final Long2ObjectOpenHashMap<TopologyUpdatePlanner.PageDraft> draftsBySection;
    private final Int2ObjectOpenHashMap<TopologyUpdatePlanner.PageDraft> draftsBySlot;
    private AirRouteCompiler airRoutes;
    private record PageRead(WorkerPageStore.PageState page,
            com.teammoeg.frostedheart.content.climate.thermal.mesh.PagePublication publication) {}
    private final Long2ObjectOpenHashMap<PageRead> pageReads = new Long2ObjectOpenHashMap<>();
    private final it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap haloReads = new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap();
    private final java.util.LinkedHashMap<com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle,
            com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.GeometryHalo> haloWrites = new java.util.LinkedHashMap<>();
    private final java.util.IdentityHashMap<com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle,
            com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.GeometryHalo> previousHalos = new java.util.IdentityHashMap<>();
    private final it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap haloReferenceDelta = new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap();
    private final it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap haloValues = new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap();
    private boolean haloDirty;
    private it.unimi.dsi.fastutil.longs.LongSet captureSections;

    void recordBacking(AirBackingMeter meter) {
        meter.container(materialSurfaceCache, materialSurfaceCache.size(), 12);
        meter.container(materialOwnerCache, materialOwnerCache.size(), 12);
        meter.container(pageReads, pageReads.size(), 16); meter.bytes(32L * pageReads.size());
        meter.container(haloReads, haloReads.size(), 12); meter.container(haloWrites, haloWrites.size(), 16);
        meter.container(previousHalos, previousHalos.size(), 16);
        meter.container(haloReferenceDelta, haloReferenceDelta.size(), 12); meter.container(haloValues, haloValues.size(), 12);
    }

    long preparationBackingBytes() {
        return AirBackingMeter.containerBackingBytes(materialSurfaceCache, materialSurfaceCache.size(), 12)
                + AirBackingMeter.containerBackingBytes(materialOwnerCache, materialOwnerCache.size(), 12)
                + AirBackingMeter.containerBackingBytes(pageReads, pageReads.size(), 16) + 32L * pageReads.size()
                + AirBackingMeter.containerBackingBytes(haloReads, haloReads.size(), 12)
                + AirBackingMeter.containerBackingBytes(haloWrites, haloWrites.size(), 16)
                + AirBackingMeter.containerBackingBytes(previousHalos, previousHalos.size(), 16)
                + AirBackingMeter.containerBackingBytes(haloReferenceDelta, haloReferenceDelta.size(), 12)
                + AirBackingMeter.containerBackingBytes(haloValues, haloValues.size(), 12);
    }

    void captureSections(it.unimi.dsi.fastutil.longs.LongSet sections) { captureSections = sections; }
    boolean capturesSection(long section) { return captureSections == null || captureSections.contains(section); }
    void forEachCaptureSection(java.util.function.LongConsumer consumer) {
        if (captureSections != null) for (long section : captureSections) consumer.accept(section);
        else pages.forEachPage(page -> consumer.accept(page.handle.sectionKey()));
    }

    void readPage(long section) {
        if (pageReads.containsKey(section)) return;
        var page = pages.find(section);
        pageReads.put(section, new PageRead(page, page == null ? null : page.publication));
    }

    boolean dependenciesCurrent() {
        for (var entry : pageReads.long2ObjectEntrySet()) {
            var expected = entry.getValue(); var current = pages.find(entry.getLongKey());
            if (current != expected.page || current != null && current.publication != expected.publication) return false;
        }
        for (var entry : haloReads.long2IntEntrySet())
            if (pages.haloSignature(entry.getLongKey()) != entry.getIntValue()) return false;
        for (var entry : previousHalos.entrySet())
            if (pages.halo(entry.getKey()) != entry.getValue()) return false;
        return true;
    }

    void stageHalo(com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalPageHandle owner,
            com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.message.GeometryHalo halo) {
        if (!previousHalos.containsKey(owner)) previousHalos.put(owner, pages.halo(owner));
        haloWrites.put(owner, halo); haloDirty = true;
    }

    private void prepareHaloOverlay() {
        if (!haloDirty) return;
        haloReferenceDelta.clear(); haloValues.clear();
        for (var entry : haloWrites.entrySet()) {
            var previous = previousHalos.get(entry.getKey());
            if (previous != null) for (long position : previous.positions()) haloReferenceDelta.addTo(position, -1);
            var next = entry.getValue();
            if (next == null) continue;
            for (int index = 0; index < next.size(); index++) {
                long position = next.positions()[index];
                haloReferenceDelta.addTo(position, 1); haloValues.put(position, next.signatures()[index]);
            }
        }
        haloDirty = false;
    }

    private int haloSignature(long position) {
        prepareHaloOverlay();
        if (!haloReads.containsKey(position)) haloReads.put(position, pages.haloSignature(position));
        if (pages.haloReferences(position) + haloReferenceDelta.get(position) == 0) return ThermalSignatureTable.UNRESOLVED;
        return haloValues.containsKey(position) ? haloValues.get(position) : haloReads.get(position);
    }

    it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap copyHaloSignatures(int capacityBound) {
        prepareHaloOverlay();
        var copy = pages.copyHaloSignatures(captureSections, capacityBound);
        for (long position : haloReferenceDelta.keySet()) {
            if (!capturesSection(SectionPos.asLong(BlockPos.getX(position) >> 4,
                    BlockPos.getY(position) >> 4, BlockPos.getZ(position) >> 4))) continue;
            // The frozen reader records its own section dependencies. Copying
            // this overlay must not also grow the planner's per-position read cache.
            int value = haloValues.getOrDefault(position, copy.get(position));
            if (pages.haloReferences(position) + haloReferenceDelta.get(position) == 0
                    || value == ThermalSignatureTable.UNRESOLVED) copy.remove(position);
            else copy.put(position, value);
        }
        return copy;
    }

    /** Committed positions plus possible local additions, before the frozen copy allocates. */
    int capturedHaloSignatureBound() {
        prepareHaloOverlay();
        int count = pages.haloSignatureCount(captureSections);
        for (long position : haloValues.keySet())
            if (capturesSection(SectionPos.asLong(BlockPos.getX(position) >> 4,
                    BlockPos.getY(position) >> 4, BlockPos.getZ(position) >> 4))
                    && pages.haloReferences(position) == 0
                    && haloValues.get(position) != ThermalSignatureTable.UNRESOLVED)
                count = Math.incrementExact(count);
        return count;
    }

    int capturedSectionBound(int haloPositions) {
        return captureSections == null ? Math.addExact(candidatePageBound(), haloPositions) : captureSections.size();
    }

    void commitHalos() {
        for (var entry : haloWrites.entrySet()) {
            if (entry.getValue() == null) pages.removeHalo(entry.getKey());
            else pages.replaceHalo(entry.getKey(), entry.getValue());
        }
    }

    void resetPreparation() {
        resetMaterialContacts(); pageReads.clear(); haloReads.clear(); haloWrites.clear(); previousHalos.clear();
        haloReferenceDelta.clear(); haloValues.clear(); haloDirty = false;
    }

    void airRoutes(AirRouteCompiler compiler) {
        airRoutes = compiler;
    }

    AirRouteCompiler airRoutes() {
        return airRoutes;
    }

    void forEachCandidatePage(java.util.function.Consumer<WorkerPageStore.PageState> consumer) {
        if (captureSections != null) {
            for (long section : captureSections) {
                var page = page(section);
                if (page != null) consumer.accept(page);
            }
            return;
        }
        pages.forEachPage(page -> {
            if (!draftsBySection.containsKey(page.handle.sectionKey())) {
                readPage(page.handle.sectionKey()); consumer.accept(page);
            }
        });
        for (var draft : draftsBySection.values()) if (!draft.retirement) consumer.accept(draft.page);
    }

    int candidatePageBound() { return captureSections == null ? pages.activePageCount() + draftsBySection.size() : captureSections.size(); }
    WorkerPageStore committedPages() { return pages; }

    long residentMask(WorkerPageStore.PageState page) {
        var draft = draftsBySlot.get(page.pageSlot);
        return draft == null ? page.residentBrickMask : draft.nextResidentBrickMask;
    }

    @Override
    public long geometryRevision(WorkerPageStore.PageState page) {
        var draft = draftsBySlot.get(page.pageSlot);
        return draft == null ? page.geometryRevision : draft.geometryRevision;
    }

    TopologyView(
            WorkerPageStore pages,
            Long2ObjectOpenHashMap<TopologyUpdatePlanner.PageDraft> draftsBySection,
            Int2ObjectOpenHashMap<TopologyUpdatePlanner.PageDraft> draftsBySlot) {
        this.pages = pages;
        this.draftsBySection = draftsBySection;
        this.draftsBySlot = draftsBySlot;
    }

    WorkerPageStore.PageState page(long sectionKey) {
        if (!capturesSection(sectionKey)) return null;
        readPage(sectionKey);
        TopologyUpdatePlanner.PageDraft draft = draftsBySection.get(sectionKey);
        if (draft != null) {
            return draft.retirement ? null : draft.page;
        }
        return pages.find(sectionKey);
    }

    @Override
    public WorkerPageStore.PageState pageAt(long sectionKey) { return page(sectionKey); }

    @Override
    public WorkerPageStore.PageState pageAtSlot(int pageSlot) { return pageSlot(pageSlot); }

    @Override
    public boolean ownsCell(ThermalCellArena arena, int slot) {
        if (!arena.isLive(slot) && !arena.isStagedCell(slot)) return false;
        WorkerPageStore.PageState owner = pageSlot(arena.pageSlot(slot));
        if (owner == null) return false;
        int x = arena.minimum(slot, 0), y = arena.minimum(slot, 1), z = arena.minimum(slot, 2);
        int brickIndex = (x & 15) >>> 2 | ((z & 15) >>> 2) << 2 | ((y & 15) >>> 2) << 4;
        WorkerBrickTopology selected = brick(owner, brickIndex);
        return resident(owner, brickIndex) && slot >= selected.span.firstSlot()
                && slot < selected.span.endSlotExclusive();
    }

    WorkerPageStore.PageState pageSlot(int pageSlot) {
        TopologyUpdatePlanner.PageDraft draft = draftsBySlot.get(pageSlot);
        if (draft != null) {
            return draft.retirement || !capturesSection(draft.page.handle.sectionKey()) ? null : draft.page;
        }
        var page = pages.findPageSlot(pageSlot);
        if (page != null && !capturesSection(page.handle.sectionKey())) return null;
        if (page != null) readPage(page.handle.sectionKey());
        return page;
    }

    PageSignatures signatures(WorkerPageStore.PageState page) {
        TopologyUpdatePlanner.PageDraft draft = draftsBySlot.get(page.pageSlot);
        return draft == null ? page.signatures : draft.nextSignatures;
    }

    double naturalTemperature(WorkerPageStore.PageState page) {
        TopologyUpdatePlanner.PageDraft draft = draftsBySlot.get(page.pageSlot);
        return draft == null ? page.naturalTemperatureC : draft.naturalTemperatureC;
    }

    int firstExposedLocalY(WorkerPageStore.PageState page, int column) {
        TopologyUpdatePlanner.PageDraft draft = draftsBySlot.get(page.pageSlot);
        byte[] values = draft == null ? page.firstExposedLocalY : draft.nextSkyExposure();
        return Byte.toUnsignedInt(values[column]);
    }

    WorkerBrickTopology brick(WorkerPageStore.PageState page, int brickIndex) {
        TopologyUpdatePlanner.PageDraft draft = draftsBySlot.get(page.pageSlot);
        if (draft != null) {
            WorkerBrickTopology replacement = draft.replacements[brickIndex];
            if (replacement != null) {
                return replacement;
            }
        }
        return page.brick(brickIndex);
    }

    boolean resident(WorkerPageStore.PageState page, int brickIndex) {
        TopologyUpdatePlanner.PageDraft draft = draftsBySlot.get(page.pageSlot);
        long mask = draft == null ? page.residentBrickMask : draft.nextResidentBrickMask;
        return (mask & 1L << brickIndex) != 0L;
    }

    WorkerBrickTopology brickAtWorld(int brickMinX, int brickMinY, int brickMinZ) {
        WorkerPageStore.PageState page =
                page(
                        SectionPos.asLong(
                                SectionPos.blockToSectionCoord(brickMinX),
                                SectionPos.blockToSectionCoord(brickMinY),
                                SectionPos.blockToSectionCoord(brickMinZ)));
        if (page == null) {
            return null;
        }
        int index =
                Math.floorMod(brickMinX, 16) >>> 2
                        | (Math.floorMod(brickMinZ, 16) >>> 2) << 2
                        | (Math.floorMod(brickMinY, 16) >>> 2) << 4;
        return resident(page, index) ? brick(page, index) : null;
    }

    @Override
    public int signatureAtWorld(int x, int y, int z) {
        // A route may only use geometry that this task will also freeze for its solver.
        // Newly available outside Pages belong to a later, expanded structural input.
        if (!capturesSection(SectionPos.asLong(x >> 4, y >> 4, z >> 4))) return ThermalSignatureTable.UNRESOLVED;
        WorkerPageStore.PageState page =
                page(
                        SectionPos.asLong(
                                SectionPos.blockToSectionCoord(x),
                                SectionPos.blockToSectionCoord(y),
                                SectionPos.blockToSectionCoord(z)));
        if (page == null) return haloSignature(BlockPos.asLong(x, y, z));
        int local = (x & 15) | (z & 15) << 4 | (y & 15) << 8;
        int brick = (x & 15) >>> 2 | ((z & 15) >>> 2) << 2 | ((y & 15) >>> 2) << 4;
        return resident(page, brick) ? signatures(page).get(local) : haloSignature(BlockPos.asLong(x, y, z));
    }

    void resetMaterialContacts() {
        materialSurfaceCache.clear();
        materialOwnerCache.clear();
    }

    int airSlotAt(long position) {
        int x = BlockPos.getX(position), y = BlockPos.getY(position), z = BlockPos.getZ(position);
        var brick = brickAtWorld(x, y, z);
        return brick == null || !brick.cellsResolved
                ? -1
                : brick.airSlotAt((x & 3) | (z & 3) << 2 | (y & 3) << 4);
    }

    @Override
    public int resolveAirFaceTarget(int x, int y, int z, BlockFace face,
            ThermalSignatureTable signatures, WorkerPageStore.MutableAirTarget out) {
        long position = BlockPos.asLong(x, y, z);
        int slot = airSlotAt(position);
        if (slot >= 0) {
            var owner = page(SectionPos.asLong(x >> 4, y >> 4, z >> 4));
            out.set(owner.lifecycleGeneration, x, y, z, face);
            return slot;
        }
        var outlet = airRoutes.outletTrace(position);
        if (outlet == null) return WorkerPageStore.PORT_TOPOLOGY_UNAVAILABLE;
        long block = outlet.block(0);
        slot = airSlotAt(block);
        if (slot < 0) return WorkerPageStore.PORT_TOPOLOGY_UNAVAILABLE;
        int ox = BlockPos.getX(block), oy = BlockPos.getY(block), oz = BlockPos.getZ(block);
        var owner = page(SectionPos.asLong(ox >> 4, oy >> 4, oz >> 4));
        out.set(owner.lifecycleGeneration, ox, oy, oz, BlockFace.fromOrdinal(outlet.face(0)));
        return slot;
    }

    long airRegionAt(long position) {
        int x = BlockPos.getX(position), y = BlockPos.getY(position), z = BlockPos.getZ(position);
        var brick = brickAtWorld(x, y, z);
        int block = (x & 3) | (z & 3) << 2 | (y & 3) << 4;
        int member = Long.numberOfTrailingZeros(brick.nodeMask(brick.nodeAt(block)));
        return BlockPos.asLong(
                (x & ~3) + (member & 3), (y & ~3) + (member >>> 4), (z & ~3) + (member >>> 2 & 3));
    }

    boolean hasDirectAir(int x, int y, int z, ThermalSignatureTable signatures) {
        for (int face = 0; face < 6; face++) {
            int nx = x + DX[face], ny = y + DY[face], nz = z + DZ[face];
            if (signatures.isAir(signatureAtWorld(nx, ny, nz))
                    && airSlotAt(BlockPos.asLong(nx, ny, nz)) >= 0) return true;
        }
        return false;
    }

    boolean materialSurfaceAt(int x, int y, int z, ThermalSignatureTable signatures) {
        long position = BlockPos.asLong(x, y, z);
        byte cached = materialSurfaceCache.get(position);
        if (cached != 0) return cached == 2;
        boolean surface =
                hasDirectAir(x, y, z, signatures)
                        || airRoutes != null && airRoutes.hasRegion(position);
        materialSurfaceCache.put(position, (byte) (surface ? 2 : 1));
        return surface;
    }

    boolean materialContactAllowed(
            int ax, int ay, int az, int bx, int by, int bz, ThermalSignatureTable signatures) {
        boolean firstSurface = materialSurfaceAt(ax, ay, az, signatures);
        boolean secondSurface = materialSurfaceAt(bx, by, bz, signatures);
        if (firstSurface && secondSurface) return true;
        if (firstSurface) return ownsInnerLayer(ax, ay, az, bx, by, bz, signatures);
        if (secondSurface) return ownsInnerLayer(bx, by, bz, ax, ay, az, signatures);
        return false;
    }

    private boolean ownsInnerLayer(
            int surfaceX,
            int surfaceY,
            int surfaceZ,
            int x,
            int y,
            int z,
            ThermalSignatureTable signatures) {
        long position = BlockPos.asLong(x, y, z);
        byte owner = materialOwnerCache.get(position);
        if (owner == 0) {
            owner = 1; // Known to have no eligible surface owner.
            long selectedPosition = Long.MAX_VALUE;
            int contacts = signatures.fullContactFaces(signatureAtWorld(x, y, z));
            for (int face = 0; face < 6; face++) {
                if ((contacts & 1 << face) == 0) continue;
                int nx = x + DX[face], ny = y + DY[face], nz = z + DZ[face];
                int neighborSignature = signatureAtWorld(nx, ny, nz);
                if (signatures.materialProfileId(neighborSignature) == 0
                        || (signatures.fullContactFaces(neighborSignature) & 1 << (face ^ 1)) == 0
                        || !materialSurfaceAt(nx, ny, nz, signatures)) continue;
                long candidate = BlockPos.asLong(nx, ny, nz);
                if (candidate < selectedPosition) {
                    selectedPosition = candidate;
                    owner = (byte) (face + 2);
                }
            }
            materialOwnerCache.put(position, owner);
        }
        if (owner == 1) return false;
        int face = owner - 2;
        return surfaceX == x + DX[face] && surfaceY == y + DY[face] && surfaceZ == z + DZ[face];
    }
}
