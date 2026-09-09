/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.PageSignatures;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.core.SectionPos;

/** Read-only planning projection over the plan's single draft authority. */
final class TopologyView {
    private final WorkerPageStore pages;
    private final Long2ObjectOpenHashMap<TopologyPlan.PageDraft> draftsBySection;
    private final Int2ObjectOpenHashMap<TopologyPlan.PageDraft> draftsBySlot;

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
        if (page == null) return ThermalSignatureTable.UNRESOLVED;
        int local = (x & 15) | (z & 15) << 4 | (y & 15) << 8;
        int brick = (x & 15) >>> 2 | ((z & 15) >>> 2) << 2 | ((y & 15) >>> 2) << 4;
        return resident(page, brick) ? signatures(page).get(local) : ThermalSignatureTable.UNRESOLVED;
    }

}
