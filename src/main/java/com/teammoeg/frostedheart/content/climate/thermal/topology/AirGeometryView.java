/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.topology;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.BlockFace;
import com.teammoeg.frostedheart.content.climate.thermal.profile.ThermalSignatureTable;

/** The same Air geometry can be read from committed Pages or a prepared topology draft. */
public interface AirGeometryView {
    interface CellCursor {
        boolean hasNext();
        /** -1 is a visited address not owned by this view, rather than end-of-iteration. */
        int nextSlot();
    }
    boolean ownsCell(ThermalCellArena arena, int slot);
    default CellCursor cellCursor(ThermalCellArena arena) {
        return new CellCursor() {
            final int limit = arena.requiredSlotCapacity();
            int cursor;
            @Override public boolean hasNext() { return cursor < limit; }
            @Override public int nextSlot() {
                int slot = cursor++;
                return (arena.isLive(slot) || arena.isStagedCell(slot)) && ownsCell(arena, slot) ? slot : -1;
            }
        };
    }
    WorkerPageStore.PageState pageAt(long sectionKey);
    WorkerPageStore.PageState pageAtSlot(int pageSlot);
    default double naturalTemperatureC(int pageSlot) {
        var page = pageAtSlot(pageSlot);
        return page == null ? Double.NaN : page.naturalTemperatureC;
    }
    long geometryRevision(WorkerPageStore.PageState page);
    int signatureAtWorld(int x, int y, int z);
    int resolveAirFaceTarget(int x, int y, int z, BlockFace face,
            ThermalSignatureTable signatures, WorkerPageStore.MutableAirTarget out);
}
