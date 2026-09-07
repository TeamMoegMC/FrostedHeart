/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.content.climate.thermal.geometry.ComponentBrickCompiler;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.core.SectionPos;

/** Read-only planning projection over the plan's single draft authority. */
final class TopologyView {
    private final WorkerPageStore pages;
    private final ThermalSignatureTable signatures;
    private final Long2ObjectOpenHashMap<TopologyPlan.PageDraft> draftsBySection;
    private final Int2ObjectOpenHashMap<TopologyPlan.PageDraft> draftsBySlot;

    TopologyView(
            WorkerPageStore pages,
            ThermalSignatureTable signatures,
            Long2ObjectOpenHashMap<TopologyPlan.PageDraft> draftsBySection,
            Int2ObjectOpenHashMap<TopologyPlan.PageDraft> draftsBySlot
    ) {
        this.pages = pages;
        this.signatures = signatures;
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

    long airReference(
            WorkerPageStore.PageState localPage,
            PageSignatures localSignatures,
            int blockX,
            int blockY,
            int blockZ,
            int microX,
            int microY,
            int microZ
    ) {
        long sectionKey = SectionPos.asLong(
                SectionPos.blockToSectionCoord(blockX),
                SectionPos.blockToSectionCoord(blockY),
                SectionPos.blockToSectionCoord(blockZ));
        WorkerPageStore.PageState page;
        PageSignatures pageSignatures;
        if (localPage.handle.sectionKey() == sectionKey) {
            page = localPage;
            pageSignatures = localSignatures;
        } else {
            page = page(sectionKey);
            if (page == null) {
                return PackedAirReference.NONE;
            }
            pageSignatures = signatures(page);
        }
        int localX = SectionPos.sectionRelative(blockX);
        int localY = SectionPos.sectionRelative(blockY);
        int localZ = SectionPos.sectionRelative(blockZ);
        int pageBlock = localX | localZ << 4 | localY << 8;
        int brickIndex = localX >>> 2
                | (localZ >>> 2) << 2
                | (localY >>> 2) << 4;
        if (!resident(page, brickIndex)) {
            return PackedAirReference.NONE;
        }
        int signatureId = pageSignatures.get(pageBlock);
        int microcell = microX | microZ << 2 | microY << 4;
        int region = signatures.componentOrdinal(signatureId, microcell);
        return region == 0xff
                ? PackedAirReference.NONE
                : PackedAirReference.pack(
                        page.pageSlot, pageBlock, microcell, region);
    }

    int resolveAirSlot(long reference) {
        if (reference == PackedAirReference.NONE) {
            return -1;
        }
        WorkerPageStore.PageState page =
                pageSlot(PackedAirReference.pageSlot(reference));
        if (page == null) {
            return -1;
        }
        int pageBlock = PackedAirReference.pageBlock(reference);
        int brickIndex = (pageBlock & 15) >>> 2
                | (pageBlock >>> 4 & 15) >>> 2 << 2
                | (pageBlock >>> 8 & 15) >>> 2 << 4;
        if (!resident(page, brickIndex)) {
            return -1;
        }
        WorkerBrickTopology brick = brick(page, brickIndex);
        if (brick.coverageSlot < 0 || !brick.cellsResolved) {
            return -1;
        }
        ComponentBrickCompiler.CompiledBrick mixed = brick.mixedGeometry;
        if (mixed == null) {
            return brick.coverageSlot;
        }
        int blockInBrick = pageBlock & 3
                | (pageBlock >>> 4 & 3) << 2
                | (pageBlock >>> 8 & 3) << 4;
        int component = mixed.compiledComponentAt(
                blockInBrick,
                PackedAirReference.localRegion(reference));
        return component < 0 ? -1 : brick.coverageSlot + component;
    }
}
