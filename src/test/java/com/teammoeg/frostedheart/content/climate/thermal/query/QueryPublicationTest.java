/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.query;

import com.teammoeg.frostedheart.content.climate.thermal.ThermalTestFixtures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.ThermalMemoryBudget;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryPublicationTest {
    private static final double EPSILON = 1.0e-12D;

    @Test
    void publicationWritesEveryLiveSlotAndValidatesSlotGeneration() {
        ThermalCellArena arena = arena(2, 10.0D);
        arena.setEnthalpyJ(0, 20.0D);
        arena.setEnthalpyJ(1, 40.0D);
        QueryPublication publication = publication(2, 2_000L);

        assertTrue(publication.publish(
                arena, 5.0D, 3L, 20L, null));
        QueryPublication.MutableSample out =
                new QueryPublication.MutableSample();
        assertTrue(publication.tryRead(1, 1, 3L, out));
        assertEquals(9.0D, out.temperatureC(), EPSILON);
        assertEquals(20L, out.sampleTick());
        assertFalse(publication.tryRead(1, 2, 3L, out));
        assertFalse(publication.tryRead(1, 1, 4L, out));
        publication.close();
    }

    @Test
    void sleepingRepublishAdvancesSampleTimeWithoutCopyingCells() {
        ThermalCellArena arena = arena(1, 10.0D);
        QueryPublication publication = publication(1, 2_000L);
        publication.publish(arena, 0.0D, 1L, 5L, null);
        arena.setEnthalpyJ(0, 100.0D);

        assertTrue(publication.republishUnchanged(1L, 10L));
        QueryPublication.MutableSample out =
                new QueryPublication.MutableSample();
        assertTrue(publication.tryRead(0, 1, 1L, out));
        assertEquals(0.0D, out.temperatureC(), EPSILON);
        assertEquals(10L, out.sampleTick());
        publication.close();
    }

    @Test
    void refusedGrowthPreservesThePreviousPublishedBuffer() {
        ThermalCellArena arena = arena(1, 1.0D);
        QueryPublication publication = publication(1, 1_220L);
        publication.publish(arena, 0.0D, 1L, 1L, null);

        assertFalse(publication.tryEnsureCapacity(2, 2));
        QueryPublication.MutableSample out =
                new QueryPublication.MutableSample();
        assertTrue(publication.tryRead(0, 1, 1L, out));
        publication.close();
    }

    @Test
    void fixedPageBackingIsIncludedInReservation() {
        long fixedPageBytes = 4L
                * (Double.BYTES + 3L * Long.BYTES
                        + 65L * Integer.BYTES);
        long initialCellBytes = 2L
                * (Double.BYTES + Integer.BYTES);
        long required = fixedPageBytes + initialCellBytes;

        ThermalMemoryBudget refused = new ThermalMemoryBudget(required - 1L);
        assertNull(QueryPublication.tryCreate(
                refused.createDimensionBudget(required - 1L), 1, 4));

        ThermalMemoryBudget admitted = new ThermalMemoryBudget(required);
        QueryPublication publication = QueryPublication.tryCreate(
                admitted.createDimensionBudget(required), 1, 4);
        assertNotNull(publication);
        publication.close();
    }

    @Test
    void successfulGrowthAlsoPreservesThePreviousPublishedBuffer() {
        ThermalCellArena arena = arena(1, 1.0D);
        QueryPublication publication = publication(1, 2_000L);
        publication.publish(arena, 0.0D, 2L, 4L, null);

        assertTrue(publication.tryEnsureCapacity(2, 2));
        QueryPublication.MutableSample out =
                new QueryPublication.MutableSample();
        assertTrue(publication.tryRead(0, 1, 2L, out));
        assertEquals(4L, out.sampleTick());
        publication.close();
    }

    @Test
    void concurrentReadersNeverMixBufferValuesAndSampleTime()
            throws InterruptedException {
        ThermalCellArena arena = arena(1, 1.0D);
        QueryPublication publication = publication(1, 2_000L);
        publication.publish(arena, 0.0D, 1L, 0L, null);
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread writer = new Thread(() -> {
            try {
                for (long sample = 1L; sample <= 10_000L; sample++) {
                    arena.setEnthalpyJ(0, sample);
                    publication.publish(arena, 0.0D, 1L, sample, null);
                }
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                running.set(false);
            }
        }, "thermal-publication-writer");
        writer.start();

        QueryPublication.MutableSample out =
                new QueryPublication.MutableSample();
        while (running.get()) {
            if (publication.tryRead(0, 1, 1L, out)
                    && out.temperatureC() != (double) out.sampleTick()) {
                failure.compareAndSet(null, new AssertionError(
                        "mixed publication: temperature=" + out.temperatureC()
                                + ", tick=" + out.sampleTick()));
                break;
            }
        }
        writer.join();
        if (failure.get() != null) {
            throw new AssertionError(
                    "concurrent publication failed", failure.get());
        }
        publication.close();
    }

    @Test
    void infraredTrackingMarksOnlyQuantizedBrickChangesAndExpires() {
        ThermalCellArena arena = arena(1, 1.0D);
        QueryPublication publication = publication(1, 2_000L);
        publication.publish(arena, 0.0D, 1L, 20L, null);

        assertTrue(publication.noteInfraredRequest(20L, 80));
        QueryPublication.InfraredReadCursor cursor =
                new QueryPublication.InfraredReadCursor();
        assertTrue(publication.beginInfraredRead(cursor));
        assertEquals(1, cursor.infraredEpoch());
        assertEquals(1, cursor.pageChangeEpoch(0));
        assertEquals(1, cursor.brickChangeEpoch(0, 0));

        arena.setEnthalpyJ(0, 0.1D);
        publication.publish(arena, 0.0D, 1L, 40L, null);
        assertTrue(publication.beginInfraredRead(cursor));
        assertEquals(1, cursor.infraredEpoch());

        arena.setEnthalpyJ(0, 0.3D);
        publication.publish(arena, 0.0D, 1L, 60L, null);
        assertTrue(publication.beginInfraredRead(cursor));
        assertEquals(2, cursor.infraredEpoch());
        assertEquals(2, cursor.pageChangeEpoch(0));
        assertEquals(2, cursor.brickChangeEpoch(0, 0));
        assertEquals(1, cursor.brickChangeEpoch(0, 1));

        arena.setEnthalpyJ(0, 1.0D);
        publication.publish(arena, 0.0D, 1L, 120L, null);
        assertTrue(publication.beginInfraredRead(cursor));
        assertEquals(2, cursor.infraredEpoch());

        assertTrue(publication.noteInfraredRequest(120L, 80));
        assertTrue(publication.beginInfraredRead(cursor));
        assertEquals(3, cursor.infraredEpoch());
        assertEquals(3, cursor.pageChangeEpoch(0));
        assertEquals(3, cursor.brickChangeEpoch(0, 63));
        publication.close();
    }

    @Test
    void topologyBrickMasksCommitAtomicallyAfterSuccessfulPublication() {
        ThermalCellArena arena = arena(2, 1.0D);
        QueryPublication publication = publication(1, 2_000L);
        publication.publish(arena(1, 1.0D), 0.0D, 1L, 0L, null);
        assertTrue(publication.noteInfraredRequest(0L, 80));
        publication.markInfraredBricksChanged(0, 1L << 5, 20L);
        publication.markInfraredBricksChanged(
                1, 1L << 7 | 1L << 31, 20L);

        QueryPublication.InfraredReadCursor cursor =
                new QueryPublication.InfraredReadCursor();
        assertTrue(publication.beginInfraredRead(cursor));
        assertEquals(1, cursor.infraredEpoch());
        assertFalse(publication.publish(arena, 0.0D, 2L, 20L, null));
        assertTrue(publication.beginInfraredRead(cursor));
        assertEquals(1, cursor.infraredEpoch());

        assertTrue(publication.tryEnsureCapacity(2, 2));
        assertTrue(publication.publish(arena, 0.0D, 2L, 20L, null));
        assertTrue(publication.beginInfraredRead(cursor));
        assertEquals(2, cursor.infraredEpoch());
        assertEquals(2, cursor.pageChangeEpoch(0));
        assertEquals(2, cursor.pageChangeEpoch(1));
        assertEquals(2, cursor.brickChangeEpoch(0, 5));
        assertEquals(2, cursor.brickChangeEpoch(1, 7));
        assertEquals(2, cursor.brickChangeEpoch(1, 31));
        assertEquals(1, cursor.brickChangeEpoch(0, 6));
        publication.close();
    }

    @Test
    void failedPublicationPreparationKeepsThePreviousCutReadable() {
        ThermalMemoryBudget server = new ThermalMemoryBudget(2_000L);
        QueryPublication publication = QueryPublication.tryCreate(
                server.createDimensionBudget(2_000L), 2, 1);
        assertNotNull(publication);
        ThermalCellArena previous = arena(1, 1.0D);
        assertTrue(publication.publish(previous, 0.0D, 1L, 20L, null));
        assertTrue(publication.noteInfraredRequest(20L, 80));

        ThermalCellArena invalid = arena(2, 1.0D);
        invalid.setEnthalpyJ(0, 1.0D);
        invalid.setEnthalpyJ(1, 1.0D);
        assertThrows(IllegalArgumentException.class, () ->
                publication.publish(invalid, 0.0D, 2L, 40L, null));

        QueryPublication.MutableSample out =
                new QueryPublication.MutableSample();
        assertTrue(publication.tryRead(0, 1, 1L, out));
        assertEquals(0.0D, out.temperatureC(), EPSILON);
        assertEquals(20L, out.sampleTick());
        publication.close();
    }

    private static QueryPublication publication(int capacity, long limit) {
        ThermalMemoryBudget server = new ThermalMemoryBudget(limit);
        QueryPublication publication = QueryPublication.tryCreate(
                server.createDimensionBudget(limit), capacity, 4);
        assertNotNull(publication);
        return publication;
    }

    private static ThermalCellArena arena(int cellCount, double capacity) {
        ThermalCellArena arena = new ThermalCellArena(cellCount);
        for (int slot = 0; slot < cellCount; slot++) {
            ThermalTestFixtures.regularBrick(
                    arena, slot, 1, slot * 4, 0, 0,
                    capacity, 0.0D, 0.0D);
        }
        return arena;
    }
}
