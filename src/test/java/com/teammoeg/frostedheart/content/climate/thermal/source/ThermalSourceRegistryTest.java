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

class ThermalSourceRegistryTest {
    private static final double EPSILON = 1.0e-8D;

    @Test
    void oneSourceAndHundredFractionalSourcesProduceTheSameNodeIntegral() {
        NodePowerAccumulatorArena singleNodes = new NodePowerAccumulatorArena(0);
        ThermalSourceRegistry single = registry(singleNodes, 1);
        single.registerSource(
                1L, 1, ThermalSourceMode.POWER_SOURCE,
                1_000.0D, true, 0L, ports(nodePort(0, 0L, 1)));

        NodePowerAccumulatorArena manyNodes = new NodePowerAccumulatorArena(0);
        ThermalSourceRegistry many = registry(manyNodes, 1);
        for (int index = 0; index < 100; index++) {
            many.registerSource(
                    100L + index, 1, ThermalSourceMode.POWER_SOURCE,
                    10.0D, true, 0L, ports(nodePort(0, 0L, 1)));
        }

        ThermalCellArena singleDestination = destination(1);
        ThermalCellArena manyDestination = destination(1);
        assertEquals(2_000.0D,
                singleNodes.drainAllPendingEnergyTo(40L, singleDestination), EPSILON);
        assertEquals(2_000.0D,
                manyNodes.drainAllPendingEnergyTo(40L, manyDestination), EPSILON);
        assertEquals(2_000.0D, singleDestination.enthalpyJ(0), EPSILON);
        assertEquals(2_000.0D, manyDestination.enthalpyJ(0), EPSILON);
        assertEquals(100, many.sourceCount());
    }

    @Test
    void midCadenceOnOffAndCoolingPowerUseTheExactTickIntegral() {
        NodePowerAccumulatorArena nodes = new NodePowerAccumulatorArena(0);
        ThermalSourceRegistry registry = registry(nodes, 2);
        registry.registerSource(
                2L, 1, ThermalSourceMode.POWER_SOURCE,
                100.0D, true, 0L, ports(nodePort(0, 0L, 1)));

        registry.setEnabled(2L, false, 5L);
        registry.setEnabled(2L, true, 15L);
        registry.setPower(2L, -40.0D, 25L);

        assertEquals(55.0D, registry.routedEnergyJAt(
                2L, SourceBinding.Kind.THERMAL_NODE, 35L), EPSILON);
        ThermalCellArena destination = destination(1);
        assertEquals(55.0D,
                nodes.drainAllPendingEnergyTo(35L, destination), EPSILON);
        assertEquals(55.0D, destination.enthalpyJ(0), EPSILON);
    }

    @Test
    void multipleEmissionPortsPartitionPowerAcrossNodeAndExplicitSink() {
        NodePowerAccumulatorArena nodes = new NodePowerAccumulatorArena(0);
        ThermalSourceRegistry registry = registry(nodes, 2);
        registry.registerSource(
                21L,
                1,
                ThermalSourceMode.POWER_SOURCE,
                100.0D,
                true,
                0L,
                ports(
                        new EmissionPort(
                                0,
                                0.7D,
                                SourceBinding.thermalNode(0L, 1)),
                        new EmissionPort(
                                1,
                                0.3D,
                                SourceBinding.internalReservoir(900L))));

        assertEquals(70.0D, registry.routedEnergyJAt(
                21L, SourceBinding.Kind.THERMAL_NODE, 20L), EPSILON);
        ThermalCellArena destination = destination(1);
        assertEquals(70.0D,
                nodes.drainAllPendingEnergyTo(20L, destination), EPSILON);
        assertEquals(70.0D, destination.enthalpyJ(0), EPSILON);
        assertEquals(30.0D, registry.routedEnergyJ(
                21L, SourceBinding.Kind.INTERNAL_RESERVOIR), EPSILON);
    }

    @Test
    void rebindSettlesOldAndNewNodesBeforeMovingPower() {
        NodePowerAccumulatorArena nodes = new NodePowerAccumulatorArena(0);
        ThermalSourceRegistry registry = registry(nodes, 1);
        registry.registerSource(
                3L, 1, ThermalSourceMode.POWER_SOURCE,
                200.0D, true, 0L, ports(nodePort(4, 0L, 1)));

        registry.rebindPort(3L, 4, SourceBinding.thermalNode(1L, 2), 10L);
        registry.routedEnergyJAt(3L, SourceBinding.Kind.THERMAL_NODE, 30L);

        ThermalCellArena destination = destination(1, 2);
        assertEquals(300.0D,
                nodes.drainAllPendingEnergyTo(30L, destination), EPSILON);
        assertEquals(100.0D, destination.enthalpyJ(0), EPSILON);
        assertEquals(200.0D, destination.enthalpyJ(1), EPSILON);
        assertEquals(300.0D, registry.routedEnergyJ(
                3L, SourceBinding.Kind.THERMAL_NODE), EPSILON);
    }

    @Test
    void unloadSettlesBeforeRemovingNodePower() {
        NodePowerAccumulatorArena unloadNodes = new NodePowerAccumulatorArena(0);
        ThermalSourceRegistry unloading = registry(unloadNodes, 1);
        unloading.registerSource(
                8L, 3, ThermalSourceMode.POWER_SOURCE,
                100.0D, true, 0L, ports(nodePort(0, 0L, 3)));
        assertEquals(ThermalSourceRegistry.UnloadStatus.STALE_GENERATION,
                unloading.unloadSource(8L, 2, 5L));
        assertEquals(ThermalSourceRegistry.UnloadStatus.APPLIED,
                unloading.unloadSource(8L, 3, 10L));

        ThermalCellArena unloadDestination = destination(3);
        assertEquals(50.0D,
                unloadNodes.drainAllPendingEnergyTo(30L, unloadDestination), EPSILON);
        assertEquals(50.0D, unloadDestination.enthalpyJ(0), EPSILON);
        assertEquals(0.0D,
                unloadNodes.drainAllPendingEnergyTo(40L, unloadDestination), EPSILON);
        assertEquals(50.0D, unloading.routedEnergyJ(
                8L, SourceBinding.Kind.THERMAL_NODE), EPSILON);
    }

    @Test
    void unloadedSourceIdRevivesInPlaceAndOnlyPowersTheNewGeneration() {
        NodePowerAccumulatorArena nodes = new NodePowerAccumulatorArena(0);
        ThermalSourceRegistry registry = registry(nodes, 3);
        registry.registerSource(
                9L, 1, ThermalSourceMode.POWER_SOURCE,
                100.0D, true, 0L, ports(nodePort(0, 0L, 1)));
        assertEquals(ThermalSourceRegistry.UnloadStatus.APPLIED,
                registry.unloadSource(9L, 1, 10L));

        registry.registerSource(
                9L,
                2,
                ThermalSourceMode.POWER_SOURCE,
                1_000.0D,
                true,
                10L,
                ports(
                        new EmissionPort(
                                0, 0.7D,
                                SourceBinding.thermalNode(1L, 2)),
                        new EmissionPort(
                                1, 0.1D,
                                SourceBinding.internalReservoir(901L)),
                        new EmissionPort(
                                2, 0.2D,
                                SourceBinding.declaredLoss(902L))));

        assertEquals(1, registry.sourceCount());
        assertEquals(700.0D, registry.routedEnergyJAt(
                9L, SourceBinding.Kind.THERMAL_NODE, 30L), EPSILON);
        ThermalCellArena reviveDestination = destination(1, 2);
        assertEquals(750.0D,
                nodes.drainAllPendingEnergyTo(30L, reviveDestination), EPSILON);
        assertEquals(50.0D, reviveDestination.enthalpyJ(0), EPSILON);
        assertEquals(700.0D, reviveDestination.enthalpyJ(1), EPSILON);
        assertEquals(100.0D, registry.routedEnergyJ(
                9L, SourceBinding.Kind.INTERNAL_RESERVOIR), EPSILON);
        assertEquals(200.0D, registry.routedEnergyJ(
                9L, SourceBinding.Kind.DECLARED_LOSS), EPSILON);
    }

    private static ThermalSourceRegistry registry(
            NodePowerAccumulatorArena nodes,
            int maxPorts
    ) {
        return new ThermalSourceRegistry(0, maxPorts, nodes);
    }

    private static ThermalCellArena destination(int... generations) {
        ThermalCellArena arena = new ThermalCellArena(generations.length);
        for (int slot = 0; slot < generations.length; slot++) {
            arena.allocatePageCells(
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
        return arena;
    }

    private static EmissionPort[] ports(EmissionPort... ports) {
        return ports;
    }

    private static EmissionPort nodePort(int portId, long nodeId, int generation) {
        return EmissionPort.of(
                portId,
                1.0D,
                SourceBinding.thermalNode(nodeId, generation));
    }
}
