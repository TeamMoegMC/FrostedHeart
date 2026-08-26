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
    void eventTimeChangesPreserveSignedIntegralAcrossProductionDrains() {
        NodePowerAccumulatorArena arena = new NodePowerAccumulatorArena(0);
        int slot = arena.ensureNode(0L, 3, 2L);

        arena.changePowerAt(slot, 2L, 400.0D);
        ThermalCellArena first = cellArena(3);
        assertEquals(200.0D, arena.drainAllPendingEnergyTo(12L, first), EPSILON);
        assertEquals(200.0D, first.enthalpyJ(0), EPSILON);

        arena.changePowerAt(slot, 12L, -600.0D);
        ThermalCellArena second = cellArena(3);
        assertEquals(-100.0D, arena.drainAllPendingEnergyTo(22L, second), EPSILON);
        assertEquals(-100.0D, second.enthalpyJ(0), EPSILON);

        assertThrows(IllegalArgumentException.class, () -> arena.settleTo(slot, 21L));
    }

    @Test
    void nodeIdentityIncludesLifecycleGeneration() {
        NodePowerAccumulatorArena arena = new NodePowerAccumulatorArena(1);
        int oldGeneration = arena.ensureNode(0L, 4, 0L);
        int newGeneration = arena.ensureNode(1L, 5, 0L);

        arena.changePowerAt(oldGeneration, 0L, 100.0D);
        arena.changePowerAt(newGeneration, 0L, -20.0D);

        ThermalCellArena destination = cellArena(4, 5);
        assertEquals(40.0D,
                arena.drainAllPendingEnergyTo(10L, destination), EPSILON);
        assertEquals(50.0D, destination.enthalpyJ(0), EPSILON);
        assertEquals(-10.0D, destination.enthalpyJ(1), EPSILON);
    }

    @Test
    void staleArenaBindingRetainsEnergyUntilAValidGenerationCanReceiveIt() {
        NodePowerAccumulatorArena accumulators = new NodePowerAccumulatorArena(0);
        int accumulator = accumulators.ensureNode(0L, 1, 0L);
        accumulators.changePowerAt(accumulator, 0L, 100.0D);
        ThermalCellArena staleDestination = cellArena(2);

        assertThrows(IllegalStateException.class, () ->
                accumulators.drainAllPendingEnergyTo(10L, staleDestination));
        assertEquals(0.0D, staleDestination.enthalpyJ(0), EPSILON);

        ThermalCellArena currentDestination = cellArena(1);
        assertEquals(50.0D,
                accumulators.drainAllPendingEnergyTo(10L, currentDestination), EPSILON);
        assertEquals(50.0D, currentDestination.enthalpyJ(0), EPSILON);
        assertEquals(0.0D,
                accumulators.drainAllPendingEnergyTo(10L, currentDestination), EPSILON);
    }

    private static ThermalCellArena cellArena(int... generations) {
        ThermalCellArena destination = new ThermalCellArena(0);
        for (int slot = 0; slot < generations.length; slot++) {
            destination.allocatePageCells(
                    slot,
                    generations[slot],
                    new ThermalCellArena.CellSpec[]{new ThermalCellArena.CellSpec(
                            slot * 4, 0, 0, 0, 0, 100.0D)},
                    new ThermalCellArena.MixedBrickSpec[0],
                    new ThermalCellArena.MaterialPoleSpec[0],
                    new ThermalCellArena.PhaseReservoirSpec[0],
                    0.0D,
                    0.0D);
        }
        return destination;
    }
}
