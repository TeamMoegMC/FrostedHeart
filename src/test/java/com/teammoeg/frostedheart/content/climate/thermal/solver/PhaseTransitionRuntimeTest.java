/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.ThermalTestFixtures;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ArenaSpan;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseTransitionRuntimeTest {
    private static final double EPSILON = 1.0e-9D;

    @Test
    void requestAckConsumesExactlyOneReservedUnit() {
        Fixture fixture = fixture(1, 50.0D);
        fixture.arena.setEnthalpyJ(fixture.airSlot, 10_000.0D);

        assertTrue(fixture.runtime.applyContact(
                fixture.airSlot, fixture.phaseSlot,
                100.0D, 0.0D, 1.0D));
        PhaseTransitionRuntime.Request[] requests =
                fixture.runtime.drainRequests(8);
        assertEquals(1, requests.length);
        assertEquals(0, requests[0].blockX());
        assertEquals(50.0D,
                fixture.arena.enthalpyJ(fixture.phaseSlot), EPSILON);

        assertTrue(fixture.runtime.applyAck(
                requests[0], PhaseTransitionRuntime.AckOutcome.APPLIED));
        assertEquals(0.0D,
                fixture.arena.enthalpyJ(fixture.phaseSlot), EPSILON);
        assertFalse(fixture.arena.phaseRequestOutstanding(fixture.phaseSlot));
    }

    @Test
    void rejectedAckKeepsEnergyAndRetryOffersTheSameReservationAgain() {
        Fixture fixture = fixture(1, 25.0D);
        fixture.arena.setEnthalpyJ(fixture.airSlot, 10_000.0D);
        fixture.runtime.applyContact(
                fixture.airSlot, fixture.phaseSlot,
                100.0D, 0.0D, 1.0D);
        PhaseTransitionRuntime.Request first =
                fixture.runtime.drainRequests(1)[0];

        assertTrue(fixture.runtime.applyAck(
                first, PhaseTransitionRuntime.AckOutcome.RETRY));
        fixture.runtime.applyContact(
                fixture.airSlot, fixture.phaseSlot,
                0.0D, 0.0D, 0.0D);
        PhaseTransitionRuntime.Request retried =
                fixture.runtime.drainRequests(1)[0];
        assertEquals(first.requestSequence(), retried.requestSequence());

        assertTrue(fixture.runtime.applyAck(
                retried, PhaseTransitionRuntime.AckOutcome.REJECTED));
        assertEquals(25.0D,
                fixture.arena.enthalpyJ(fixture.phaseSlot), EPSILON);
        assertFalse(fixture.arena.phaseRequestOutstanding(fixture.phaseSlot));
    }

    @Test
    void staleGenerationAckCannotReachAReplacementReservoir() {
        Fixture fixture = fixture(1, 10.0D);
        fixture.arena.setEnthalpyJ(fixture.airSlot, 10_000.0D);
        fixture.runtime.applyContact(
                fixture.airSlot, fixture.phaseSlot,
                100.0D, 0.0D, 1.0D);
        PhaseTransitionRuntime.Request stale =
                fixture.runtime.drainRequests(1)[0];

        fixture.runtime.unregisterReservoir(fixture.phaseSlot);
        fixture.arena.releasePageCells(0, 1, fixture.span);
        ThermalCellArena.BrickAllocation replacement =
                ThermalTestFixtures.phaseBrick(
                        fixture.arena,
                        0, 2, 0, 0, 0,
                        100.0D, 1, 1L, 0.0D, 10.0D, 0.0D);
        int replacementPhase = replacement.phaseReservoirSlots()[0];
        fixture.runtime.registerReservoir(replacementPhase);

        assertFalse(fixture.runtime.applyAck(
                stale, PhaseTransitionRuntime.AckOutcome.APPLIED));
        assertEquals(0.0D,
                fixture.arena.enthalpyJ(replacementPhase), EPSILON);
    }

    private static Fixture fixture(long candidateMask, double unitEnergy) {
        ThermalCellArena arena = new ThermalCellArena(2);
        ThermalCellArena.BrickAllocation allocation =
                ThermalTestFixtures.phaseBrick(
                        arena,
                        0, 1, 0, 0, 0,
                        100.0D, 1, candidateMask,
                        0.0D, unitEnergy, 0.0D);
        int air = allocation.cellSpan().firstSlot();
        int phase = allocation.phaseReservoirSlots()[0];
        PhaseTransitionRuntime runtime = new PhaseTransitionRuntime(arena, 4);
        runtime.registerReservoir(phase);
        return new Fixture(
                arena, runtime, allocation.cellSpan(), air, phase);
    }

    private record Fixture(
            ThermalCellArena arena,
            PhaseTransitionRuntime runtime,
            ArenaSpan span,
            int airSlot,
            int phaseSlot
    ) {
    }
}
