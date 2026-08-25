/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.solver;

import com.teammoeg.frostedheart.content.climate.thermal.mesh.ArenaSpan;
import com.teammoeg.frostedheart.content.climate.thermal.mesh.ThermalCellArena;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseTransitionRuntimeTest {
    private static final double EPSILON = 1.0e-9D;

    @Test
    void reservesOneVisibleUnitAndCommitsItExactlyOnce() {
        Fixture fixture = fixture(4, 4, 0b11L, 50.0D);
        fixture.arena.setEnthalpyJ(fixture.airSlots[0], 1_000.0D);

        double initial = totalEnergy(fixture);
        assertTrue(fixture.runtime.applyContact(
                fixture.airSlots[0], fixture.phaseSlots[0],
                100.0D, 0.0D, 1.0D));
        assertEquals(initial, totalEnergy(fixture), EPSILON);
        assertEquals(100.0D, fixture.arena.enthalpyJ(fixture.phaseSlots[0]), EPSILON);
        assertEquals(50.0D,
                fixture.arena.phaseReservedEnergyJ(fixture.phaseSlots[0]), EPSILON);

        PhaseTransitionRuntime.MutableRequest request =
                new PhaseTransitionRuntime.MutableRequest();
        assertTrue(fixture.runtime.pollRequest(request));
        assertEquals(0, request.candidateBit());
        assertFalse(fixture.runtime.pollRequest(
                new PhaseTransitionRuntime.MutableRequest()));

        fixture.runtime.submitAck(request, PhaseTransitionRuntime.AckOutcome.APPLIED);
        assertEquals(1L, fixture.runtime.latestOfferedAckWatermark());
        assertEquals(1, fixture.runtime.applyAcksThrough(1L));
        assertEquals(50.0D, fixture.arena.enthalpyJ(fixture.phaseSlots[0]), EPSILON);
        assertEquals(50.0D, fixture.runtime.committedTransitionEnergyJ(), EPSILON);
        assertFalse(fixture.arena.phaseRequestOutstanding(fixture.phaseSlots[0]));
        assertEquals(initial,
                totalEnergy(fixture) + fixture.runtime.committedTransitionEnergyJ(),
                EPSILON);
    }

    @Test
    void multipleContactsAddWhileWeakSingleContactCannotRequest() {
        Fixture fixture = fixture(4, 4, 1L, 50.0D);
        fixture.arena.setEnthalpyJ(fixture.airSlots[0], 100.0D);
        fixture.arena.setEnthalpyJ(fixture.airSlots[1], 100.0D);

        assertTrue(fixture.runtime.applyContact(
                fixture.airSlots[0], fixture.phaseSlots[0],
                5.0D, 0.0D, 1.0D));
        assertFalse(fixture.runtime.pollRequest(
                new PhaseTransitionRuntime.MutableRequest()),
                "one weak contact must remain below the visible-unit threshold");

        assertTrue(fixture.runtime.applyContact(
                fixture.airSlots[1], fixture.phaseSlots[0],
                5.0D, 0.0D, 1.0D));
        assertTrue(fixture.runtime.pollRequest(
                new PhaseTransitionRuntime.MutableRequest()),
                "independent contacts must add into the same Brick-local reservoir");
    }

    @Test
    void requestAndAckOverflowRetryPerReservoirWithoutDuplicateMutation() {
        Fixture fixture = fixture(1, 1, 1L, 10.0D, 1L, 10.0D);
        fixture.arena.setEnthalpyJ(fixture.airSlots[0], 1_000.0D);
        fixture.arena.setEnthalpyJ(fixture.airSlots[1], 1_000.0D);
        assertTrue(fixture.runtime.applyContact(
                fixture.airSlots[0], fixture.phaseSlots[0],
                100.0D, 0.0D, 1.0D));
        assertTrue(fixture.runtime.applyContact(
                fixture.airSlots[1], fixture.phaseSlots[1],
                100.0D, 0.0D, 1.0D));
        assertTrue(fixture.runtime.requestRetryCount() > 0L);

        PhaseTransitionRuntime.MutableRequest first =
                new PhaseTransitionRuntime.MutableRequest();
        PhaseTransitionRuntime.MutableRequest second =
                new PhaseTransitionRuntime.MutableRequest();
        assertTrue(fixture.runtime.pollRequest(first));
        assertTrue(fixture.runtime.applyContact(
                fixture.airSlots[1], fixture.phaseSlots[1],
                0.0D, 0.0D, 0.0D));
        assertTrue(fixture.runtime.pollRequest(second));

        fixture.runtime.submitAck(first, PhaseTransitionRuntime.AckOutcome.APPLIED);
        fixture.runtime.submitAck(second, PhaseTransitionRuntime.AckOutcome.APPLIED);
        assertTrue(fixture.runtime.ackRetryCount() > 0L);
        assertEquals(1, fixture.runtime.applyAcksThrough(1L));
        assertEquals(1, fixture.runtime.flushPendingAcks());
        assertEquals(2L, fixture.runtime.latestOfferedAckWatermark());
        assertEquals(1, fixture.runtime.applyAcksThrough(2L));
        assertEquals(20.0D, fixture.runtime.committedTransitionEnergyJ(), EPSILON);
    }

    @Test
    void staleGenerationAckCannotCommitReplacementReservoir() {
        Fixture fixture = fixture(2, 2, 1L, 10.0D);
        fixture.arena.setEnthalpyJ(fixture.airSlots[0], 1_000.0D);
        assertTrue(fixture.runtime.applyContact(
                fixture.airSlots[0], fixture.phaseSlots[0],
                100.0D, 0.0D, 1.0D));
        PhaseTransitionRuntime.MutableRequest stale =
                new PhaseTransitionRuntime.MutableRequest();
        assertTrue(fixture.runtime.pollRequest(stale));

        fixture.arena.releasePageCells(0, 1, fixture.span);
        ThermalCellArena.PageAllocation replacement = allocate(
                fixture.arena, 0, 2, 1L, 10.0D);
        int replacementPhase = replacement.phaseReservoirSlots()[0];
        fixture.runtime.submitAck(stale, PhaseTransitionRuntime.AckOutcome.APPLIED);
        assertEquals(1, fixture.runtime.applyAcksThrough(1L));

        assertEquals(0.0D, fixture.arena.enthalpyJ(replacementPhase), EPSILON);
        assertEquals(0.0D, fixture.runtime.committedTransitionEnergyJ(), EPSILON);
    }

    private static Fixture fixture(
            int requestCapacity,
            int ackCapacity,
            long candidateMask,
            double unitEnergy
    ) {
        return fixture(
                requestCapacity, ackCapacity, candidateMask, unitEnergy,
                0L, unitEnergy);
    }

    private static Fixture fixture(
            int requestCapacity,
            int ackCapacity,
            long firstMask,
            double firstUnitEnergy,
            long secondMask,
            double secondUnitEnergy
    ) {
        ThermalCellArena arena = new ThermalCellArena(0);
        ThermalCellArena.PageAllocation allocation = secondMask == 0L
                ? allocate(arena, 0, 1, firstMask, firstUnitEnergy)
                : allocate(
                        arena,
                        0,
                        1,
                        new ThermalCellArena.PhaseReservoirSpec[] {
                                phase(0, 1, firstMask, firstUnitEnergy),
                                phase(4, 2, secondMask, secondUnitEnergy)
                        });
        int[] airSlots = new int[] {
                allocation.cellSpan().firstSlot(),
                allocation.cellSpan().firstSlot() + 1
        };
        return new Fixture(
                arena,
                new PhaseTransitionRuntime(arena, requestCapacity, ackCapacity),
                allocation.cellSpan(),
                airSlots,
                allocation.phaseReservoirSlots());
    }

    private static ThermalCellArena.PageAllocation allocate(
            ThermalCellArena arena,
            int pageSlot,
            int generation,
            long candidateMask,
            double unitEnergy
    ) {
        return allocate(
                arena,
                pageSlot,
                generation,
                new ThermalCellArena.PhaseReservoirSpec[] {
                        phase(0, 1, candidateMask, unitEnergy)
                });
    }

    private static ThermalCellArena.PageAllocation allocate(
            ThermalCellArena arena,
            int pageSlot,
            int generation,
            ThermalCellArena.PhaseReservoirSpec[] phases
    ) {
        return arena.allocatePageCells(
                pageSlot,
                generation,
                new ThermalCellArena.CellSpec[] {
                        new ThermalCellArena.CellSpec(
                                8, 0, 0, 4, 0, 0, 10.0D),
                        new ThermalCellArena.CellSpec(
                                12, 0, 0, 4, 0, 0, 10.0D)
                },
                new ThermalCellArena.MixedBrickSpec[0],
                new ThermalCellArena.MaterialPoleSpec[0],
                phases,
                0.0D,
                0.0D);
    }

    private static ThermalCellArena.PhaseReservoirSpec phase(
            int brickMinX,
            int profileId,
            long candidateMask,
            double unitEnergy
    ) {
        return new ThermalCellArena.PhaseReservoirSpec(
                brickMinX, 0, 0, profileId, candidateMask, 0.0D, unitEnergy);
    }

    private static double totalEnergy(Fixture fixture) {
        double total = 0.0D;
        for (int slot = fixture.span.firstSlot();
             slot < fixture.span.endSlotExclusive(); slot++) {
            total += fixture.arena.enthalpyJ(slot);
        }
        return total;
    }

    private record Fixture(
            ThermalCellArena arena,
            PhaseTransitionRuntime runtime,
            ArenaSpan span,
            int[] airSlots,
            int[] phaseSlots
    ) {
    }
}
