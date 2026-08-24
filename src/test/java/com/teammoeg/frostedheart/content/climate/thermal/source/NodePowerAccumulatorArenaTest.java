/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.source;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NodePowerAccumulatorArenaTest {
    private static final double EPSILON = 1.0e-9D;

    @Test
    void eventTimeChangesPreserveSignedIntegralAcrossArbitraryDrainCadence() {
        NodePowerAccumulatorArena arena = new NodePowerAccumulatorArena(0);
        int slot = arena.ensureNode(41L, 3, 2L);

        arena.changePowerAt(slot, 2L, 400.0D);
        assertEquals(200.0D, arena.drainPendingEnergyTo(slot, 12L), EPSILON);

        arena.changePowerAt(slot, 12L, -600.0D);
        assertEquals(-200.0D, arena.currentPowerW(slot), EPSILON);
        arena.addImpulseAt(slot, 17L, -30.0D);
        assertEquals(-130.0D, arena.drainPendingEnergyTo(slot, 22L), EPSILON);
        assertEquals(22L, arena.lastIntegralTick(slot));

        assertThrows(IllegalArgumentException.class, () -> arena.settleTo(slot, 21L));
    }

    @Test
    void nodeIdentityIncludesLifecycleGeneration() {
        NodePowerAccumulatorArena arena = new NodePowerAccumulatorArena(1);
        int oldGeneration = arena.ensureNode(7L, 4, 0L);
        int newGeneration = arena.ensureNode(7L, 5, 0L);

        arena.changePowerAt(oldGeneration, 0L, 100.0D);
        arena.changePowerAt(newGeneration, 0L, -20.0D);

        assertEquals(2, arena.accumulatorCount());
        assertEquals(50.0D, arena.drainPendingEnergyTo(oldGeneration, 10L), EPSILON);
        assertEquals(-10.0D, arena.drainPendingEnergyTo(newGeneration, 10L), EPSILON);
    }

    @Test
    void failedNodeDeliveryRetainsTheUndeliveredEnergyForRetry() {
        NodePowerAccumulatorArena arena = new NodePowerAccumulatorArena(0);
        int slot = arena.ensureNode(9L, 2, 0L);
        arena.changePowerAt(slot, 0L, 100.0D);

        assertThrows(IllegalStateException.class, () ->
                arena.drainAllPendingEnergyTo(10L, (ignoredSlot, nodeId,
                        generation, energyJ) -> {
                    throw new IllegalStateException("destination unavailable");
                }));

        assertEquals(50.0D, arena.pendingEnergyJ(slot), EPSILON);
        double[] delivered = {0.0D};
        assertEquals(50.0D, arena.drainAllPendingEnergyTo(
                10L,
                (ignoredSlot, nodeId, generation, energyJ) ->
                        delivered[0] += energyJ
        ), EPSILON);
        assertEquals(50.0D, delivered[0], EPSILON);
        assertEquals(0.0D, arena.pendingEnergyJ(slot), EPSILON);
    }

    @Test
    void staleArenaBindingRetainsEnergyUntilAValidGenerationCanReceiveIt() {
        NodePowerAccumulatorArena accumulators = new NodePowerAccumulatorArena(0);
        int accumulator = accumulators.ensureNode(0L, 1, 0L);
        accumulators.changePowerAt(accumulator, 0L, 100.0D);
        ThermalCellArena staleDestination = cellArena(2);

        assertThrows(IllegalStateException.class, () ->
                accumulators.drainAllPendingEnergyTo(10L, staleDestination));
        assertEquals(50.0D, accumulators.pendingEnergyJ(accumulator), EPSILON);
        assertEquals(0.0D, staleDestination.enthalpyJ(0), EPSILON);

        ThermalCellArena currentDestination = cellArena(1);
        assertEquals(50.0D,
                accumulators.drainAllPendingEnergyTo(10L, currentDestination), EPSILON);
        assertEquals(0.0D, accumulators.pendingEnergyJ(accumulator), EPSILON);
        assertEquals(50.0D, currentDestination.enthalpyJ(0), EPSILON);
    }

    private static ThermalCellArena cellArena(int generation) {
        ThermalCellArena destination = new ThermalCellArena(0);
        destination.allocatePageCells(
                0,
                generation,
                new ThermalCellArena.CellSpec[]{new ThermalCellArena.CellSpec(
                        0, 0, 0, 4, 0, 0, 100.0D)},
                new double[]{0.0D});
        return destination;
    }
}
