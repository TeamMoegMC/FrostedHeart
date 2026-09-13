/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;

import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos;

/** Read-only planning projection over the plan's single draft authority. */
final class TopologyView {
    static final int[] DX = {-1, 1, 0, 0, 0, 0};
    static final int[] DY = {0, 0, -1, 1, 0, 0};
    static final int[] DZ = {0, 0, 0, 0, -1, 1};
    private final WorkerPageStore pages;
    private final Long2ByteOpenHashMap materialSurfaceCache = new Long2ByteOpenHashMap();
    private final Long2ByteOpenHashMap materialOwnerCache = new Long2ByteOpenHashMap();
    private final Long2ObjectOpenHashMap<TopologyPlan.PageDraft> draftsBySection;
    private final Int2ObjectOpenHashMap<TopologyPlan.PageDraft> draftsBySlot;
    private AirRouteCompiler airRoutes;

    void airRoutes(AirRouteCompiler compiler) { airRoutes = compiler; }
    AirRouteCompiler airRoutes() { return airRoutes; }

    long geometryRevision(WorkerPageStore.PageState page) {
        var draft = draftsBySlot.get(page.pageSlot);
        return draft == null ? page.geometryRevision : draft.geometryRevision;
    }

    TopologyView(
            WorkerPageStore pages,
            Long2ObjectOpenHashMap<TopologyPlan.PageDraft> draftsBySection,
            Int2ObjectOpenHashMap<TopologyPlan.PageDraft> draftsBySlot
    ) {
        this.pages = pages;
        this.draftsBySection = draftsBySection;
        this.draftsBySlot = draftsBySlot;
    }

    WorkerPageStore.PageState page(long sectionKey) {
        TopologyPlan.PageDraft draft = draftsBySection.get(sectionKey);
        if (draft != null) {
            return draft.retirement ? null : draft.page;
        }
        return pages.find(sectionKey);
    }

    WorkerPageStore.PageState pageSlot(int pageSlot) {
        TopologyPlan.PageDraft draft = draftsBySlot.get(pageSlot);
        if (draft != null) {
            return draft.retirement ? null : draft.page;
        }
        return pages.findPageSlot(pageSlot);
    }

    PageSignatures signatures(WorkerPageStore.PageState page) {
        TopologyPlan.PageDraft draft = draftsBySlot.get(page.pageSlot);
        return draft == null ? page.signatures : draft.nextSignatures;
    }

    double naturalTemperature(WorkerPageStore.PageState page) {
        TopologyPlan.PageDraft draft = draftsBySlot.get(page.pageSlot);
        return draft == null
                ? page.naturalTemperatureC
                : draft.naturalTemperatureC;
    }

    int firstExposedLocalY(
            WorkerPageStore.PageState page,
            int column
    ) {
        TopologyPlan.PageDraft draft = draftsBySlot.get(page.pageSlot);
        byte[] values = draft == null
                ? page.firstExposedLocalY
                : draft.nextSkyExposure();
        return Byte.toUnsignedInt(values[column]);
    }

    WorkerBrickTopology brick(
            WorkerPageStore.PageState page,
            int brickIndex
    ) {
        TopologyPlan.PageDraft draft = draftsBySlot.get(page.pageSlot);
        if (draft != null) {
            WorkerBrickTopology replacement =
                    draft.replacements[brickIndex];
            if (replacement != null) {
                return replacement;
            }
        }
        return page.brick(brickIndex);
    }

    boolean resident(
            WorkerPageStore.PageState page,
            int brickIndex
    ) {
        TopologyPlan.PageDraft draft = draftsBySlot.get(page.pageSlot);
        long mask = draft == null
                ? page.residentBrickMask : draft.nextResidentBrickMask;
        return (mask & 1L << brickIndex) != 0L;
    }

    WorkerBrickTopology brickAtWorld(
            int brickMinX,
            int brickMinY,
            int brickMinZ
    ) {
        WorkerPageStore.PageState page = page(SectionPos.asLong(
                SectionPos.blockToSectionCoord(brickMinX),
                SectionPos.blockToSectionCoord(brickMinY),
                SectionPos.blockToSectionCoord(brickMinZ)));
        if (page == null) {
            return null;
        }
        int index = Math.floorMod(brickMinX, 16) >>> 2
                | (Math.floorMod(brickMinZ, 16) >>> 2) << 2
                | (Math.floorMod(brickMinY, 16) >>> 2) << 4;
        return resident(page, index) ? brick(page, index) : null;
    }

    int signatureAtWorld(int x, int y, int z) {
        WorkerPageStore.PageState page = page(SectionPos.asLong(
                SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(y), SectionPos.blockToSectionCoord(z)));
        if (page == null) return pages.haloSignatureAt(x, y, z);
        int local = (x & 15) | (z & 15) << 4 | (y & 15) << 8;
        int brick = (x & 15) >>> 2 | ((z & 15) >>> 2) << 2 | ((y & 15) >>> 2) << 4;
        return resident(page, brick) ? signatures(page).get(local) : pages.haloSignatureAt(x, y, z);
    }

    void resetMaterialContacts() {
        materialSurfaceCache.clear();
        materialOwnerCache.clear();
    }

    int airSlotAt(long position) {
        int x = BlockPos.getX(position), y = BlockPos.getY(position), z = BlockPos.getZ(position);
        var brick = brickAtWorld(x, y, z);
        return brick == null || !brick.cellsResolved ? -1 : brick.transportSlot((x & 3) | (z & 3) << 2 | (y & 3) << 4);
    }

    long airRegionAt(long position) {
        int x = BlockPos.getX(position), y = BlockPos.getY(position), z = BlockPos.getZ(position);
        var brick = brickAtWorld(x, y, z);
        int block = (x & 3) | (z & 3) << 2 | (y & 3) << 4;
        int member = Long.numberOfTrailingZeros(brick.nodeMask(brick.nodeAt(block)));
        return BlockPos.asLong((x & ~3) + (member & 3), (y & ~3) + (member >>> 4), (z & ~3) + (member >>> 2 & 3));
    }

    boolean hasDirectAir(int x, int y, int z, ThermalSignatureTable signatures) {
        for (int face = 0; face < 6; face++) {
            int nx = x + DX[face], ny = y + DY[face], nz = z + DZ[face];
            if (signatures.isAir(signatureAtWorld(nx, ny, nz)) && airSlotAt(BlockPos.asLong(nx, ny, nz)) >= 0) return true;
        }
        return false;
    }

    boolean materialSurfaceAt(int x, int y, int z, ThermalSignatureTable signatures) {
        long position = BlockPos.asLong(x, y, z);
        byte cached = materialSurfaceCache.get(position);
        if (cached != 0) return cached == 2;
        boolean surface = hasDirectAir(x, y, z, signatures)
                || airRoutes != null && airRoutes.hasRegion(position);
        materialSurfaceCache.put(position, (byte) (surface ? 2 : 1));
        return surface;
    }

    boolean materialContactAllowed(int ax, int ay, int az, int bx, int by, int bz,
            ThermalSignatureTable signatures) {
        boolean firstSurface = materialSurfaceAt(ax, ay, az, signatures);
        boolean secondSurface = materialSurfaceAt(bx, by, bz, signatures);
        if (firstSurface && secondSurface) return true;
        if (firstSurface) return ownsInnerLayer(ax, ay, az, bx, by, bz, signatures);
        if (secondSurface) return ownsInnerLayer(bx, by, bz, ax, ay, az, signatures);
        return false;
    }

    private boolean ownsInnerLayer(int surfaceX, int surfaceY, int surfaceZ,
            int x, int y, int z, ThermalSignatureTable signatures) {
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
