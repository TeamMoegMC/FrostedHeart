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
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryPublicationTest {
    private static final double EPSILON = 1.0e-12D;

    @Test
    void publicationWritesEveryLiveSlotAndValidatesSlotGeneration() {
        ThermalCellArena arena = arena(2, 10.0D);
        arena.setEnthalpyJ(0, 20.0D);
        arena.setEnthalpyJ(1, 40.0D);
        QueryPublication publication = publication(2, 1_000L);

        assertTrue(publication.publish(
                arena, 5.0D, 3L, true, 20L));
        QueryPublication.MutableSample out =
                new QueryPublication.MutableSample();
        assertTrue(publication.tryRead(1, 1, 3L, out));
        assertEquals(9.0D, out.temperatureC(), EPSILON);
        assertEquals(0, out.mediumId());
        assertTrue(out.topologyResolved());
        assertEquals(20L, out.sampleTick());
        assertFalse(publication.tryRead(1, 2, 3L, out));
        assertFalse(publication.tryRead(1, 1, 4L, out));
        publication.close();
    }

    @Test
    void sleepingRepublishAdvancesSampleTimeWithoutCopyingCells() {
        ThermalCellArena arena = arena(1, 10.0D);
        QueryPublication publication = publication(1, 1_000L);
        publication.publish(arena, 0.0D, 1L, true, 5L);
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
        QueryPublication publication = publication(1, 90L);
        publication.publish(arena, 0.0D, 1L, true, 1L);

        assertFalse(publication.tryEnsureCapacity(2, 2));
        QueryPublication.MutableSample out =
                new QueryPublication.MutableSample();
        assertTrue(publication.tryRead(0, 1, 1L, out));
        publication.close();
    }

    @Test
    void successfulGrowthAlsoPreservesThePreviousPublishedBuffer() {
        ThermalCellArena arena = arena(1, 1.0D);
        QueryPublication publication = publication(1, 1_000L);
        publication.publish(arena, 0.0D, 2L, false, 4L);

        assertTrue(publication.tryEnsureCapacity(2, 2));
        QueryPublication.MutableSample out =
                new QueryPublication.MutableSample();
        assertTrue(publication.tryRead(0, 1, 2L, out));
        assertEquals(4L, out.sampleTick());
        assertFalse(out.topologyResolved());
        publication.close();
    }

    @Test
    void concurrentReadersNeverMixBufferValuesAndSampleTime()
            throws InterruptedException {
        ThermalCellArena arena = arena(1, 1.0D);
        QueryPublication publication = publication(1, 1_000L);
        publication.publish(arena, 0.0D, 1L, true, 0L);
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread writer = new Thread(() -> {
            try {
                for (long sample = 1L; sample <= 10_000L; sample++) {
                    arena.setEnthalpyJ(0, sample);
                    publication.publish(arena, 0.0D, 1L, true, sample);
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

    private static QueryPublication publication(int capacity, long limit) {
        ThermalMemoryBudget server = new ThermalMemoryBudget(limit, 0L);
        QueryPublication publication = QueryPublication.tryCreate(
                server.createDimensionBudget(limit, 0L), capacity);
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
