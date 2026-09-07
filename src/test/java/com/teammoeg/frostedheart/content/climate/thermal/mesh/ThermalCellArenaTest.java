/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.mesh;

import com.teammoeg.frostedheart.content.climate.thermal.ThermalTestFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ThermalCellArenaTest {
    private static final double EPSILON = 1.0e-9D;

    @Test
    void regularBrickStoresHCapacityAndInverseCapacity() {
        ThermalCellArena arena = new ThermalCellArena(1);
        ThermalCellArena.BrickAllocation allocation =
                ThermalTestFixtures.regularBrick(
                        arena, 7, 3, 0, 0, 0,
                        640.0D, -10.0D, 0.0D);
        int slot = allocation.cellSpan().firstSlot();

        assertEquals(1, allocation.cellSpan().count());
        assertEquals(1, arena.liveCellCount());
        assertEquals(3, arena.lifecycleGeneration(slot));
        assertEquals(640.0D, arena.capacityJPerK(slot), EPSILON);
        assertEquals(1.0D / 640.0D,
                arena.inverseCapacityKPerJ(slot), EPSILON);
        assertEquals(-10.0D, arena.temperatureC(slot, 0.0D), EPSILON);

        arena.addEnthalpyJ(slot, 1_280.0D);
        assertEquals(-8.0D, arena.temperatureC(slot, 0.0D), EPSILON);
        assertThrows(IllegalArgumentException.class,
                () -> arena.setEnthalpyJ(slot, Double.NaN));
    }

    @Test
    void nodeWritesRejectAStaleGenerationWithoutMutation() {
        ThermalCellArena arena = new ThermalCellArena(1);
        int slot = ThermalTestFixtures.regularBrick(
                arena, 0, 2, 0, 0, 0,
                100.0D, 0.0D, 0.0D).cellSpan().firstSlot();
        arena.setEnthalpyJ(slot, 10.0D);

        assertThrows(IllegalStateException.class,
                () -> arena.addNodeEnthalpyJ(slot, 1, 5.0D));
        assertEquals(10.0D, arena.enthalpyJ(slot), EPSILON);
        arena.addNodeEnthalpyJ(slot, 2, 5.0D);
        assertEquals(15.0D, arena.enthalpyJ(slot), EPSILON);
    }

    @Test
    void bestFitSpansReuseHolesAndTailReleaseTrimsHighWater() {
        ThermalCellArena arena = new ThermalCellArena(1);
        ArenaSpan small = allocateSpan(arena, 0, 1, 3);
        allocateSpan(arena, 1, 2, 1);
        ArenaSpan large = allocateSpan(arena, 2, 3, 5);
        ArenaSpan tail = allocateSpan(arena, 3, 4, 1);
        assertEquals(10, arena.highWaterMark());

        arena.releasePageCells(0, 1, small);
        arena.releasePageCells(2, 3, large);
        ArenaSpan four = allocateSpan(arena, 4, 5, 4);
        ArenaSpan three = allocateSpan(arena, 5, 6, 3);
        assertEquals(4, four.firstSlot());
        assertEquals(0, three.firstSlot());
        assertEquals(10, arena.highWaterMark());

        arena.releasePageCells(3, 4, tail);
        assertEquals(8, arena.highWaterMark());
    }

    @Test
    void slotLimitRefusesGrowthBeforeWritingArenaState() {
        ThermalCellArena arena = new ThermalCellArena(1);
        allocateSpan(arena, 0, 1, 1);
        ThermalBrickCellLayout layout = regularLayout(4, 0, 0, 1);

        assertNull(arena.stageBrickCells(
                1, 2, layout, 0.0D, 0.0D, 1));
        assertEquals(1, arena.highWaterMark());
        assertEquals(1, arena.liveCellCount());
    }

    @Test
    void bestFitIndexSurvivesRepeatedSplitMergeAndReuse() {
        ThermalCellArena arena = new ThermalCellArena(1);
        ArenaSpan[] spans = new ArenaSpan[48];
        boolean[] occupied = new boolean[256];
        for (int index = 0; index < spans.length; index++) {
            int count = index % 5 + 1;
            spans[index] = allocateSpan(arena, index, index + 1, count);
            mark(occupied, spans[index], true);
        }
        for (int index = 0; index < spans.length; index += 2) {
            arena.releasePageCells(index, index + 1, spans[index]);
            mark(occupied, spans[index], false);
        }
        for (int index = 0; index < spans.length / 2; index++) {
            ArenaSpan replacement = allocateSpan(
                    arena, 100 + index, 100 + index, index % 4 + 1);
            mark(occupied, replacement, true);
        }

        int expected = 0;
        for (boolean live : occupied) {
            if (live) expected++;
        }
        int actual = 0;
        for (int slot = arena.nextLiveSlot(0);
             slot >= 0;
             slot = arena.nextLiveSlot(slot + 1)) {
            actual++;
        }
        assertEquals(expected, arena.liveCellCount());
        assertEquals(expected, actual);
    }

    private static ArenaSpan allocateSpan(
            ThermalCellArena arena,
            int pageSlot,
            int generation,
            int count
    ) {
        ThermalCellArena.BrickAllocation allocation = arena.stageBrickCells(
                pageSlot,
                generation,
                regularLayout(pageSlot * 4, 0, 0, count),
                0.0D,
                0.0D,
                1_000);
        arena.commitStagedCells(allocation.cellSpan());
        return allocation.cellSpan();
    }

    private static ThermalBrickCellLayout regularLayout(
            int minX,
            int minY,
            int minZ,
            int count
    ) {
        ThermalBrickCellLayout layout = new ThermalBrickCellLayout();
        layout.reset(minX, minY, minZ);
        layout.setRegularAir(1.0D);
        for (int index = 1; index < count; index++) {
            layout.addMaterialPole(
                    minX + index,
                    minY,
                    minZ,
                    1.0D,
                    0.0D);
        }
        return layout;
    }

    private static void mark(boolean[] occupied, ArenaSpan span, boolean value) {
        for (int slot = span.firstSlot(); slot < span.endSlotExclusive(); slot++) {
            if (occupied[slot] == value) {
                throw new AssertionError("arena span overlap or duplicate release");
            }
            occupied[slot] = value;
        }
    }
}
