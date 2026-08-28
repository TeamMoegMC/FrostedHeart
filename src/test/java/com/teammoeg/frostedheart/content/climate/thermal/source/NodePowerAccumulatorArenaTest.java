/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.source;

import com.teammoeg.frostedheart.content.climate.thermal.ThermalTestFixtures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodePowerAccumulatorArenaTest {
    private static final double EPSILON = 1.0e-9D;

    @Test
    void activeNodeIntegratesPowerOnceAndDeliversPendingEnergy() {
        ThermalCellArena cells = new ThermalCellArena(1);
        int node = ThermalTestFixtures.regularBrick(
                cells, 0, 3, 0, 0, 0,
                100.0D, 0.0D, 0.0D).cellSpan().firstSlot();
        NodePowerAccumulatorArena accumulators =
                new NodePowerAccumulatorArena(1);
        accumulators.retainBinding(node, 3, 0L);
        int slot = accumulators.ensureNode(node, 3, 0L);
        accumulators.changePowerAt(slot, 0L, 20.0D);

        assertTrue(accumulators.hasActivePowerOrPendingEnergy());
        accumulators.drainAllPendingEnergyTo(20L, cells);
        assertEquals(20.0D, cells.enthalpyJ(node), EPSILON);
        assertTrue(accumulators.referencesNode(node, 3));

        accumulators.changePowerAt(slot, 20L, -20.0D);
        accumulators.releaseBinding(node, 3, 20L);
        assertFalse(accumulators.referencesNode(node, 3));
        assertFalse(accumulators.hasActivePowerOrPendingEnergy());
    }

    @Test
    void idleSlotsAreRecycledAcrossNodeGenerations() {
        NodePowerAccumulatorArena accumulators =
                new NodePowerAccumulatorArena(1);
        int first = accumulators.ensureNode(0L, 1, 0L);
        accumulators.retainBinding(0L, 1, 0L);
        accumulators.releaseBinding(0L, 1, 0L);

        int replacement = accumulators.ensureNode(1L, 2, 0L);
        assertEquals(first, replacement);
        assertFalse(accumulators.referencesNode(0L, 1));
    }

    @Test
    void impulsesRemainActiveUntilTheyAreDelivered() {
        ThermalCellArena cells = new ThermalCellArena(1);
        int node = ThermalTestFixtures.regularBrick(
                cells, 0, 1, 0, 0, 0,
                100.0D, 0.0D, 0.0D).cellSpan().firstSlot();
        NodePowerAccumulatorArena accumulators =
                new NodePowerAccumulatorArena(1);
        int slot = accumulators.ensureNode(node, 1, 0L);
        accumulators.addImpulseAt(slot, 5L, 12.5D);

        assertTrue(accumulators.hasActivePowerOrPendingEnergy());
        accumulators.drainAllPendingEnergyTo(5L, cells);
        assertEquals(12.5D, cells.enthalpyJ(node), EPSILON);
        assertFalse(accumulators.hasActivePowerOrPendingEnergy());
    }
}
