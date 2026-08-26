/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.query;

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
    void publicationCarriesOneRevisionGenerationAndEpochEnvelope() {
        ThermalCellArena arena = arena(2, 10.0D);
        arena.setEnthalpyJ(0, 20.0D);
        arena.setEnthalpyJ(1, 40.0D);
        QueryPublication publication = publication(2, 1_000L);

        assertTrue(publication.publish(
                arena, 5.0D, 7L, 11L, 13L, 17L, 20L, 0, 2));
        QueryPublication.MutableSample out = new QueryPublication.MutableSample();
        assertTrue(publication.tryRead(1, 7L, 11L, out));
        assertTrue(out.valid());
        assertEquals(9.0D, out.temperatureC(), EPSILON);
        assertEquals(0, out.mediumId());
        assertEquals(7L, out.lifecycleGeneration());
        assertEquals(11L, out.geometryRevision());
        assertEquals(13L, out.topologyGeneration());
        assertEquals(17L, out.solveEpoch());
        assertEquals(20L, out.sampleTick());
        assertFalse(publication.tryRead(1, 7L, 12L, out));
        assertFalse(out.valid());
        publication.close();
    }

    @Test
    void sleepingRepublishUpdatesOnlyTheStableEnvelope() {
        ThermalCellArena arena = arena(1, 10.0D);
        QueryPublication publication = publication(1, 1_000L);
        publication.publish(arena, 0.0D, 1L, 2L, 3L, 4L, 5L, 0, 1);
        assertTrue(publication.republishUnchanged(1L, 2L, 3L, 5L, 10L));
        QueryPublication.MutableSample out = new QueryPublication.MutableSample();
        assertTrue(publication.tryRead(0, 1L, 2L, out));
        assertEquals(5L, out.solveEpoch());
        assertEquals(10L, out.sampleTick());
        publication.close();
    }

    @Test
    void resizeChargesPeakDoubleBackingAndKeepsOldPublicationOnRefusal() {
        ThermalMemoryBudget server = new ThermalMemoryBudget(90L, 0L);
        ThermalMemoryBudget dimension = server.createDimensionBudget(90L, 0L);
        QueryPublication publication = QueryPublication.tryCreate(dimension, 1);
        assertNotNull(publication);
        ThermalCellArena arena = arena(1, 1.0D);
        publication.publish(arena, 0.0D, 1L, 1L, 1L, 1L, 1L, 0, 1);

        assertFalse(publication.tryEnsureCapacity(2));
        assertEquals(1, publication.capacity());
        QueryPublication.MutableSample out = new QueryPublication.MutableSample();
        assertTrue(publication.tryRead(0, 1L, 1L, out));
        publication.close();
        QueryPublication replacement = QueryPublication.tryCreate(dimension, 1);
        assertNotNull(replacement);
        replacement.close();
    }

    @Test
    void retiredLifecycleRejectsStaleWorkerPublication() {
        ThermalCellArena arena = arena(1, 1.0D);
        QueryPublication publication = publication(1, 1_000L);
        publication.publish(arena, 0.0D, 3L, 4L, 5L, 6L, 7L, 0, 1);

        publication.retire();

        assertFalse(publication.publish(
                arena, 0.0D, 3L, 4L, 5L, 7L, 8L, 0, 1));
        QueryPublication.MutableSample out = new QueryPublication.MutableSample();
        assertFalse(publication.tryRead(0, 3L, 4L, out));
        publication.close();
    }

    @Test
    void acceptedConcurrentReadsNeverMixBufferAndEnvelopeGenerations()
            throws InterruptedException {
        ThermalCellArena arena = arena(1, 1.0D);
        QueryPublication publication = publication(1, 1_000L);
        publication.publish(arena, 0.0D, 1L, 1L, 1L, 0L, 0L, 0, 1);
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread writer = new Thread(() -> {
            try {
                for (long epoch = 1L; epoch <= 20_000L; epoch++) {
                    arena.setEnthalpyJ(0, epoch);
                    publication.publish(
                            arena, 0.0D, 1L, 1L, 1L,
                            epoch, epoch, 0, 1);
                }
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                running.set(false);
            }
        }, "thermal-publication-writer");
        writer.start();

        QueryPublication.MutableSample out = new QueryPublication.MutableSample();
        while (running.get()) {
            if (publication.tryRead(0, 1L, 1L, out)
                    && out.temperatureC() != (double) out.solveEpoch()) {
                failure.compareAndSet(null, new AssertionError(
                        "mixed publication: temperature=" + out.temperatureC()
                                + ", epoch=" + out.solveEpoch()));
                break;
            }
        }
        writer.join();
        if (failure.get() != null) {
            throw new AssertionError("concurrent publication failed", failure.get());
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
        ThermalCellArena.CellSpec[] cells = new ThermalCellArena.CellSpec[cellCount];
        for (int slot = 0; slot < cellCount; slot++) {
            cells[slot] = new ThermalCellArena.CellSpec(
                    slot * 4, 0, 0, 0, 0, capacity);
        }
        ThermalCellArena arena = new ThermalCellArena(cellCount);
        arena.allocatePageCells(
                0,
                1,
                cells,
                new ThermalCellArena.MixedBrickSpec[0],
                new ThermalCellArena.MaterialPoleSpec[0],
                new ThermalCellArena.PhaseReservoirSpec[0],
                0.0D,
                0.0D);
        return arena;
    }
}
