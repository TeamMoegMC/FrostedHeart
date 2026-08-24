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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThermalSourceRegistryTest {
    private static final double EPSILON = 1.0e-8D;

    @Test
    void oneSourceAndHundredFractionalSourcesProduceTheSameNodeIntegral() {
        NodePowerAccumulatorArena singleNodes = new NodePowerAccumulatorArena(0);
        ThermalSourceRegistry single = registry(singleNodes, 1, 4);
        single.registerSource(
                1L, 0L, 1, 1, ThermalSourceMode.POWER_SOURCE,
                1_000.0D, true, 0L, ports(nodePort(0, 9L, 1)));

        NodePowerAccumulatorArena manyNodes = new NodePowerAccumulatorArena(0);
        ThermalSourceRegistry many = registry(manyNodes, 1, 4);
        for (int index = 0; index < 100; index++) {
            many.registerSource(
                    100L + index, index, 1, 1, ThermalSourceMode.POWER_SOURCE,
                    10.0D, true, 0L, ports(nodePort(0, 9L, 1)));
        }

        int singleNode = singleNodes.findNode(9L, 1);
        int manyNode = manyNodes.findNode(9L, 1);
        assertEquals(2_000.0D,
                singleNodes.drainPendingEnergyTo(singleNode, 40L), EPSILON);
        assertEquals(2_000.0D,
                manyNodes.drainPendingEnergyTo(manyNode, 40L), EPSILON);
        assertEquals(100, many.sourceCount());
    }

    @Test
    void midCadenceOnOffAndCoolingPowerUseTheExactTickIntegral() {
        NodePowerAccumulatorArena nodes = new NodePowerAccumulatorArena(0);
        ThermalSourceRegistry registry = registry(nodes, 2, 16);
        registry.registerSource(
                2L, 0L, 1, 1, ThermalSourceMode.POWER_SOURCE,
                100.0D, true, 0L, ports(nodePort(0, 20L, 1)));

        registry.setEnabled(2L, false, 5L);
        registry.setEnabled(2L, true, 15L);
        registry.setPower(2L, -40.0D, 25L);
        SourceResyncSnapshot snapshot = registry.snapshotAt(2L, 35L);

        int node = nodes.findNode(20L, 1);
        assertEquals(55.0D, nodes.drainPendingEnergyTo(node, 35L), EPSILON);
        assertEquals(55.0D, snapshot.cumulativeEmittedEnergyJ(), EPSILON);
        assertEquals(55.0D, snapshot.reconstructedCumulativeEnergyJ(), EPSILON);
        assertEquals(55.0D,
                registry.routedEnergyJ(2L, SourceBinding.Kind.THERMAL_NODE), EPSILON);
    }

    @Test
    void multipleEmissionPortsPartitionPowerAcrossNodeAndExplicitSink() {
        NodePowerAccumulatorArena nodes = new NodePowerAccumulatorArena(0);
        ThermalSourceRegistry registry = registry(nodes, 2, 8);
        registry.registerSource(
                21L,
                0L,
                1,
                1,
                ThermalSourceMode.POWER_SOURCE,
                100.0D,
                true,
                0L,
                ports(
                        new EmissionPort(
                                0,
                                1,
                                SourceChannel.CONVECTION,
                                0.7D,
                                SourceBinding.thermalNode(210L, 1)
                        ),
                        new EmissionPort(
                                1,
                                1,
                                SourceChannel.CONTACT,
                                0.3D,
                                SourceBinding.internalReservoir(900L)
                        )
                )
        );

        SourceResyncSnapshot snapshot = registry.snapshotAt(21L, 20L);

        assertEquals(70.0D, nodes.drainPendingEnergyTo(
                nodes.findNode(210L, 1), 20L), EPSILON);
        assertEquals(70.0D,
                registry.routedEnergyJ(21L, SourceBinding.Kind.THERMAL_NODE), EPSILON);
        assertEquals(30.0D,
                registry.routedEnergyJ(21L, SourceBinding.Kind.INTERNAL_RESERVOIR), EPSILON);
        assertEquals(100.0D, snapshot.reconstructedCumulativeEnergyJ(), EPSILON);
    }

    @Test
    void rebindSettlesOldAndNewNodesBeforeMovingPower() {
        NodePowerAccumulatorArena nodes = new NodePowerAccumulatorArena(0);
        ThermalSourceRegistry registry = registry(nodes, 1, 16);
        registry.registerSource(
                3L, 0L, 1, 1, ThermalSourceMode.POWER_SOURCE,
                200.0D, true, 0L, ports(nodePort(4, 30L, 1)));

        registry.rebindPort(3L, 4, SourceBinding.thermalNode(31L, 2), 10L);
        SourceResyncSnapshot snapshot = registry.snapshotAt(3L, 30L);

        assertEquals(100.0D, nodes.drainPendingEnergyTo(
                nodes.findNode(30L, 1), 30L), EPSILON);
        assertEquals(200.0D, nodes.drainPendingEnergyTo(
                nodes.findNode(31L, 2), 30L), EPSILON);
        assertEquals(300.0D, snapshot.cumulativeEmittedEnergyJ(), EPSILON);
        assertEquals(2, registry.port(3L, 4).portRevision());
    }

    @Test
    void signedImpulseUsesItsEventBindingAndNeverCreatesContinuousPower() {
        NodePowerAccumulatorArena nodes = new NodePowerAccumulatorArena(0);
        ThermalSourceRegistry registry = registry(nodes, 1, 8);
        registry.registerSource(
                4L, 0L, 1, 2, ThermalSourceMode.IMPULSE,
                0.0D, true, 0L, ports(nodePort(0, 40L, 2)));

        registry.applyImpulse(4L, 0, -250.0D, 7L);
        SourceResyncSnapshot snapshot = registry.snapshotAt(4L, 20L);

        int node = nodes.findNode(40L, 2);
        assertEquals(-250.0D, nodes.drainPendingEnergyTo(node, 20L), EPSILON);
        assertEquals(0.0D, nodes.currentPowerW(node), EPSILON);
        assertEquals(-250.0D, snapshot.cumulativeEmittedEnergyJ(), EPSILON);
        assertEquals(7L, snapshot.retainedSegments()[0].startTick());
        assertEquals(7L, snapshot.retainedSegments()[0].endTick());
    }

    @Test
    void retainedSegmentsReplayInOrderAndRepeatedSnapshotIsANoOp() {
        NodePowerAccumulatorArena nodes = new NodePowerAccumulatorArena(0);
        ThermalSourceRegistry registry = registry(nodes, 1, 16);
        registry.registerSource(
                5L, 0L, 1, 1, ThermalSourceMode.POWER_SOURCE,
                100.0D, true, 0L, ports(nodePort(0, 50L, 1)));
        registry.rebindPort(5L, 0, SourceBinding.thermalNode(51L, 1), 10L);
        registry.setPower(5L, 200.0D, 20L);
        SourceResyncSnapshot first = registry.snapshotAt(5L, 30L);

        CollectingReplayTarget target = new CollectingReplayTarget(binding -> true);
        SourceResyncReplayer replayer = new SourceResyncReplayer();
        SourceResyncReplayer.ReplayResult applied = replayer.replay(first, target);
        SourceResyncReplayer.ReplayResult duplicate = replayer.replay(first, target);

        assertEquals(SourceResyncReplayer.ReplayStatus.APPLIED, applied.status());
        assertEquals(SourceResyncReplayer.ReplayStatus.ALREADY_APPLIED, duplicate.status());
        assertEquals(50.0D, target.energyAt(SourceBinding.thermalNode(50L, 1)), EPSILON);
        assertEquals(150.0D, target.energyAt(SourceBinding.thermalNode(51L, 1)), EPSILON);
        assertEquals(200.0D, target.totalAppliedEnergy(), EPSILON);
        assertTrue(target.losses.isEmpty());

        assertTrue(registry.acknowledge(first));
        registry.setPower(5L, 100.0D, 40L);
        SourceResyncSnapshot second = registry.snapshotAt(5L, 50L);
        assertEquals(first.eventWatermark(), second.baseAckWatermark());
        assertEquals(SourceResyncReplayer.ReplayStatus.APPLIED,
                replayer.replay(second, target).status());
        assertEquals(350.0D, target.totalAppliedEnergy(), EPSILON);
        assertEquals(350.0D, second.cumulativeEmittedEnergyJ(), EPSILON);
    }

    @Test
    void exhaustedHistoryBecomesExplicitLossInsteadOfFinalBindingInjection() {
        NodePowerAccumulatorArena nodes = new NodePowerAccumulatorArena(0);
        ThermalSourceRegistry registry = registry(nodes, 1, 2);
        registry.registerSource(
                6L, 0L, 1, 1, ThermalSourceMode.POWER_SOURCE,
                100.0D, true, 0L, ports(nodePort(0, 60L, 1)));
        registry.setPower(6L, 200.0D, 10L);
        registry.setPower(6L, -100.0D, 20L);
        registry.rebindPort(6L, 0, SourceBinding.thermalNode(61L, 1), 30L);
        SourceResyncSnapshot snapshot = registry.snapshotAt(6L, 40L);

        assertEquals(2, snapshot.retainedSegments().length);
        assertEquals(1, snapshot.losses().length);
        assertEquals(150.0D, snapshot.losses()[0].signedEnergyJ(), EPSILON);
        assertEquals(50.0D, snapshot.cumulativeEmittedEnergyJ(), EPSILON);
        assertEquals(50.0D, snapshot.reconstructedCumulativeEnergyJ(), EPSILON);

        CollectingReplayTarget target = new CollectingReplayTarget(binding -> true);
        SourceResyncReplayer.ReplayResult result =
                new SourceResyncReplayer().replay(snapshot, target);
        assertEquals(SourceResyncReplayer.ReplayStatus.APPLIED, result.status());
        assertEquals(-50.0D,
                target.energyAt(SourceBinding.thermalNode(61L, 1)), EPSILON);
        assertEquals(-100.0D, target.totalAppliedEnergy(), EPSILON);
        assertEquals(150.0D, target.totalLossEnergy(), EPSILON);
        assertNotEquals(snapshot.cumulativeEmittedEnergyJ(),
                target.energyAt(SourceBinding.thermalNode(61L, 1)));
    }

    @Test
    void coldRouteAndUnloadSettleBeforeRemovingNodePower() {
        NodePowerAccumulatorArena coldNodes = new NodePowerAccumulatorArena(0);
        ThermalSourceRegistry cold = registry(coldNodes, 1, 8);
        cold.registerSource(
                7L, 0L, 1, 3, ThermalSourceMode.POWER_SOURCE,
                100.0D, true, 0L, ports(nodePort(0, 70L, 3)));
        cold.routeColdSourceTo(7L, SourceBinding.degradedLoss(700L), 10L);
        SourceResyncSnapshot coldSnapshot = cold.snapshotAt(7L, 20L);

        assertEquals(50.0D, coldNodes.drainPendingEnergyTo(
                coldNodes.findNode(70L, 3), 20L), EPSILON);
        assertEquals(50.0D,
                cold.routedEnergyJ(7L, SourceBinding.Kind.THERMAL_NODE), EPSILON);
        assertEquals(50.0D,
                cold.routedEnergyJ(7L, SourceBinding.Kind.DEGRADED_LOSS), EPSILON);
        assertEquals(100.0D, coldSnapshot.cumulativeEmittedEnergyJ(), EPSILON);

        NodePowerAccumulatorArena unloadNodes = new NodePowerAccumulatorArena(0);
        ThermalSourceRegistry unloading = registry(unloadNodes, 1, 8);
        unloading.registerSource(
                8L, 0L, 1, 3, ThermalSourceMode.POWER_SOURCE,
                100.0D, true, 0L, ports(nodePort(0, 80L, 3)));
        assertEquals(ThermalSourceRegistry.UnloadStatus.STALE_GENERATION,
                unloading.unloadSource(8L, 2, 5L));
        assertEquals(ThermalSourceRegistry.UnloadStatus.APPLIED,
                unloading.unloadSource(8L, 3, 10L));
        SourceResyncSnapshot unloadSnapshot = unloading.snapshotAt(8L, 10L);

        int oldNode = unloadNodes.findNode(80L, 3);
        assertEquals(50.0D, unloadNodes.drainPendingEnergyTo(oldNode, 30L), EPSILON);
        assertEquals(0.0D, unloadNodes.currentPowerW(oldNode), EPSILON);
        assertFalse(unloadSnapshot.enabled());
        assertEquals(SourceBinding.Kind.UNBOUND,
                unloadSnapshot.currentPorts()[0].binding().kind());

        CollectingReplayTarget newGenerationOnly =
                new CollectingReplayTarget(binding ->
                        !binding.isThermalNode() || binding.lifecycleGeneration() == 4);
        SourceResyncReplayer.ReplayResult replay =
                new SourceResyncReplayer().replay(unloadSnapshot, newGenerationOnly);
        assertEquals(0.0D, newGenerationOnly.totalAppliedEnergy(), EPSILON);
        assertEquals(50.0D, newGenerationOnly.totalLossEnergy(), EPSILON);
        assertEquals(1, replay.lossCount());
        assertEquals(
                SourceResyncSnapshot.SourceResyncLoss.Reason
                        .STALE_LIFECYCLE_GENERATION,
                newGenerationOnly.losses.get(0).reason()
        );
    }

    @Test
    void unloadedSourceIdRevivesInPlaceWithoutGrowingRegistryStorage() {
        NodePowerAccumulatorArena nodes = new NodePowerAccumulatorArena(0);
        ThermalSourceRegistry registry = registry(nodes, 3, 8);
        registry.registerSource(
                9L, 90L, 1, 1, ThermalSourceMode.POWER_SOURCE,
                100.0D, true, 0L, ports(nodePort(0, 90L, 1)));
        assertEquals(ThermalSourceRegistry.UnloadStatus.APPLIED,
                registry.unloadSource(9L, 1, 10L));

        registry.registerSource(
                9L,
                91L,
                2,
                2,
                ThermalSourceMode.POWER_SOURCE,
                1_000.0D,
                true,
                10L,
                ports(
                        new EmissionPort(
                                0, 1, SourceChannel.CONVECTION, 0.7D,
                                SourceBinding.thermalNode(91L, 2)),
                        new EmissionPort(
                                1, 1, SourceChannel.CONTACT, 0.1D,
                                SourceBinding.internalReservoir(901L)),
                        new EmissionPort(
                                2, 1, SourceChannel.RADIATION, 0.2D,
                                SourceBinding.declaredLoss(902L))));

        ThermalSourceRegistry.ThermalSourceEntry revived = registry.entry(9L);
        assertEquals(1, registry.sourceCount());
        assertEquals(91L, revived.packedPosition());
        assertEquals(2, revived.profileId());
        assertEquals(2, revived.lifecycleGeneration());
        assertEquals(3, revived.portCount());
        assertFalse(revived.unloaded());
    }

    private static ThermalSourceRegistry registry(
            NodePowerAccumulatorArena nodes,
            int maxPorts,
            int historyCapacity
    ) {
        return new ThermalSourceRegistry(0, maxPorts, historyCapacity, nodes);
    }

    private static EmissionPort[] ports(EmissionPort... ports) {
        return ports;
    }

    private static EmissionPort nodePort(int portId, long nodeId, int generation) {
        return EmissionPort.of(
                portId,
                SourceChannel.CONVECTION,
                1.0D,
                SourceBinding.thermalNode(nodeId, generation)
        );
    }

    private static final class CollectingReplayTarget
            implements SourceResyncReplayer.ReplayTarget {
        private final Predicate<SourceBinding> acceptance;
        private final Map<SourceBinding, Double> applied = new HashMap<>();
        private final List<SourceResyncSnapshot.SourceResyncLoss> losses =
                new ArrayList<>();

        private CollectingReplayTarget(Predicate<SourceBinding> acceptance) {
            this.acceptance = acceptance;
        }

        @Override
        public boolean accepts(SourceBinding binding) {
            return acceptance.test(binding);
        }

        @Override
        public void applyEnergy(
                long sourceId,
                SourceResyncSnapshot.BindingEnergySegment segment
        ) {
            applied.merge(segment.binding(), segment.signedEnergyJ(), Double::sum);
        }

        @Override
        public void recordLoss(
                long sourceId,
                SourceResyncSnapshot.SourceResyncLoss loss
        ) {
            losses.add(loss);
        }

        private double energyAt(SourceBinding binding) {
            return applied.getOrDefault(binding, 0.0D);
        }

        private double totalAppliedEnergy() {
            return applied.values().stream().mapToDouble(Double::doubleValue).sum();
        }

        private double totalLossEnergy() {
            return losses.stream()
                    .mapToDouble(SourceResyncSnapshot.SourceResyncLoss::signedEnergyJ)
                    .sum();
        }
    }
}
