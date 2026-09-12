/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.source;

import com.teammoeg.frostedheart.content.climate.thermal.ThermalTestFixtures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThermalSourceLedgerTest {
    private static final double EPSILON = 1.0e-9D;
    private static final ThermalSourceLedger.EventObserver NO_OBSERVER =
            (events, index, ledger) -> { };

    @Test
    void eventTicksIntegrateOnlyThePowerActiveDuringEachInterval() {
        Fixture fixture = fixture(1);
        ThermalSourceBatch.Builder events = new ThermalSourceBatch.Builder(0L);
        events.addRegister(
                0L, 1, ThermalSourceMode.POWER_SOURCE,
                20.0D, true, 0L,
                0, 0, 0, 1,
                ports(fixture.firstNode, 1));
        events.addPowerChange(0L, 40.0D, 10L);

        fixture.ledger.acceptAndAdvance(
                events.buildAndReset(), 20L, NO_OBSERVER);

        assertEquals(30.0D,
                fixture.arena.enthalpyJ(fixture.firstNode), EPSILON);
        assertTrue(fixture.ledger.hasActivePowerOrPendingEnergy());
    }

    @Test
    void rebindAtCursorMovesFuturePowerWithoutReplayingPastEnergy() {
        Fixture fixture = fixture(2);
        ThermalSourceBatch.Builder events = new ThermalSourceBatch.Builder(0L);
        events.addRegister(
                7L, 3, ThermalSourceMode.POWER_SOURCE,
                20.0D, true, 0L,
                0, 0, 0, 1,
                ports(fixture.firstNode, 1));
        fixture.ledger.acceptAndAdvance(
                events.buildAndReset(), 10L, NO_OBSERVER);

        assertTrue(fixture.ledger.rebindAtCursor(
                7L, 3, 0,
                SourceBinding.thermalNode(fixture.secondNode, 1)));
        fixture.ledger.deliverThrough(20L);

        assertEquals(10.0D,
                fixture.arena.enthalpyJ(fixture.firstNode), EPSILON);
        assertEquals(10.0D,
                fixture.arena.enthalpyJ(fixture.secondNode), EPSILON);
        assertFalse(fixture.ledger.referencesThermalNode(
                fixture.firstNode, 1));
        assertTrue(fixture.ledger.referencesThermalNode(
                fixture.secondNode, 1));
    }

    @Test
    void unloadAndReregisterReuseIdentityWithoutHistoricalEnergy() {
        Fixture fixture = fixture(1);
        ThermalSourceBatch.Builder first = new ThermalSourceBatch.Builder(0L);
        first.addRegister(
                9L, 1, ThermalSourceMode.POWER_SOURCE,
                20.0D, true, 0L,
                0, 0, 0, 1,
                ports(fixture.firstNode, 1));
        fixture.ledger.acceptAndAdvance(
                first.buildAndReset(), 10L, NO_OBSERVER);

        ThermalSourceBatch.Builder replacement =
                new ThermalSourceBatch.Builder(10L);
        replacement.addUnload(9L, 1, 10L);
        replacement.addRegister(
                9L, 2, ThermalSourceMode.POWER_SOURCE,
                10.0D, true, 10L,
                0, 0, 0, 1,
                ports(fixture.firstNode, 1));
        fixture.ledger.acceptAndAdvance(
                replacement.buildAndReset(), 20L, NO_OBSERVER);

        assertEquals(15.0D,
                fixture.arena.enthalpyJ(fixture.firstNode), EPSILON);
        assertFalse(fixture.ledger.rebindAtCursor(
                9L, 1, 0, SourceBinding.declaredLoss(1L)));
        assertTrue(fixture.ledger.rebindAtCursor(
                9L, 2, 0, SourceBinding.declaredLoss(1L)));
    }

    private static EmissionPort[] ports(int node, int generation) {
        return new EmissionPort[]{EmissionPort.of(
                0, 1.0D, SourceBinding.thermalNode(node, generation))};
    }

    private static Fixture fixture(int cells) {
        ThermalCellArena arena = new ThermalCellArena(cells);
        int first = ThermalTestFixtures.regularBrick(
                arena, 0, 1, 0, 0, 0,
                100.0D, 0.0D, 0.0D).cellSpan().firstSlot();
        int second = first;
        if (cells > 1) {
            second = ThermalTestFixtures.regularBrick(
                    arena, 1, 1, 4, 0, 0,
                    100.0D, 0.0D, 0.0D).cellSpan().firstSlot();
        }
        ThermalSourceLedger ledger = new ThermalSourceLedger(
                0L, 2, 2, 16,
                new NodePowerAccumulatorArena(2, 32), arena);
        return new Fixture(arena, ledger, first, second);
    }

    private record Fixture(
            ThermalCellArena arena,
            ThermalSourceLedger ledger,
            int firstNode,
            int secondNode
    ) {
    }
}
