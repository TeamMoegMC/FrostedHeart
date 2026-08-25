/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.mesh;

import com.teammoeg.frostedheart.content.climate.thermal.solver.BuoyancyConductance;
import com.teammoeg.frostedheart.content.climate.thermal.solver.InputWatermarks;
import com.teammoeg.frostedheart.content.climate.thermal.solver.SolveEpoch;
import com.teammoeg.frostedheart.content.climate.thermal.solver.ThermalSweep;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopologyGuardTest {
    private static final TopologyGuard.OperatingPoint NOMINAL =
            new TopologyGuard.OperatingPoint(10_000.0D, 25.0D);

    @Test
    void materialAndUnresolvedFrontiersNeverCreateFarFieldTransport() {
        TopologyGuard.Decision material = TopologyGuard.classify(
                TopologyGuard.Evidence.material(),
                NOMINAL,
                FarFieldProfileRegistry.empty());
        TopologyGuard.Decision unresolved = TopologyGuard.classify(
                TopologyGuard.Evidence.open(
                        false, true, true, true, key()),
                NOMINAL,
                registry(approvedProfile()));

        assertEquals(TopologyGuard.FrontierClass.MATERIAL, material.frontierClass());
        assertFalse(material.requestsMoreGeometry());
        assertTrue(material.boundaryOperation(0, -20.0D).isEmpty());
        assertEquals(TopologyGuard.FrontierClass.UNRESOLVED, unresolved.frontierClass());
        assertEquals(TopologyGuard.Reason.TOPOLOGY_UNRESOLVED, unresolved.reason());
        assertTrue(unresolved.requestsMoreGeometry());
        assertTrue(unresolved.boundaryOperation(0, -20.0D).isEmpty());
    }

    @Test
    void nonSkyOpeningRemainsContinuationWithoutDirectionHeuristics() {
        TopologyGuard.Decision decision = TopologyGuard.classify(
                TopologyGuard.Evidence.open(
                        true, true, true, false, key()),
                NOMINAL,
                registry(approvedProfile()));

        assertEquals(
                TopologyGuard.FrontierClass.OPEN_CONTINUATION,
                decision.frontierClass());
        assertEquals(TopologyGuard.Reason.OUTDOOR_PROOF_MISSING, decision.reason());
        assertTrue(decision.requestsMoreGeometry());
    }

    @Test
    void missingCandidateAndOutOfDomainProfilesCannotBecomeAmbient() {
        TopologyGuard.Evidence evidence = outdoorEvidence();
        TopologyGuard.Decision missing = TopologyGuard.classify(
                evidence, NOMINAL, FarFieldProfileRegistry.empty());
        TopologyGuard.Decision candidate = TopologyGuard.classify(
                evidence, NOMINAL, registry(candidateProfile()));
        TopologyGuard.Decision outsideDomain = TopologyGuard.classify(
                evidence,
                new TopologyGuard.OperatingPoint(100_001.0D, 25.0D),
                registry(approvedProfile()));

        assertEquals(
                TopologyGuard.Reason.PROFILE_MISSING_OR_UNAPPROVED,
                missing.reason());
        assertEquals(
                TopologyGuard.Reason.PROFILE_MISSING_OR_UNAPPROVED,
                candidate.reason());
        assertEquals(
                TopologyGuard.Reason.PROFILE_OUTSIDE_CALIBRATION_DOMAIN,
                outsideDomain.reason());
        assertTrue(missing.boundaryOperation(0, -20.0D).isEmpty());
        assertTrue(candidate.boundaryOperation(0, -20.0D).isEmpty());
        assertTrue(outsideDomain.boundaryOperation(0, -20.0D).isEmpty());
    }

    @Test
    void approvedStaticImpedanceCompilesDirectlyIntoArenaBoundSweep() {
        TopologyGuard.Decision decision = TopologyGuard.classify(
                outdoorEvidence(), NOMINAL, registry(approvedProfile()));
        ThermalCellArena arena = new ThermalCellArena(1);
        arena.allocatePageCells(
                0,
                7,
                new ThermalCellArena.CellSpec[]{
                        ThermalCellArena.CellSpec.regularAir(
                                0, 0, 0, 4, 0, 0, 1_200.0D)
                },
                20.0D,
                0.0D);
        ThermalSweep.BoundaryOperation boundary = decision
                .boundaryOperation(0, -20.0D)
                .orElseThrow();
        ThermalSweep sweep = new ThermalSweep(
                arena,
                List.of(),
                List.of(boundary),
                new BuoyancyConductance.Parameters(1.0D, 1.0D, 1.0D));
        double initialEnthalpy = arena.enthalpyJ(0);

        ThermalSweep.Result result = sweep.apply(
                0.0D,
                new SolveEpoch(0L, 20L, 1L, 0L, InputWatermarks.ZERO));

        assertEquals(TopologyGuard.FrontierClass.OPEN_AMBIENT, decision.frontierClass());
        assertEquals(
                TopologyGuard.Reason.APPROVED_STATIC_IMPEDANCE,
                decision.reason());
        assertFalse(decision.requestsMoreGeometry());
        assertEquals(1, result.appliedBoundaries());
        assertTrue(arena.enthalpyJ(0) < initialEnthalpy);
        assertEquals(
                arena.enthalpyJ(0) - initialEnthalpy,
                result.boundaryEnergyJ(),
                1.0e-8D);
    }

    @Test
    void registryRejectsDuplicateKeysAndApprovedCrossingMismatch() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FarFieldProfileRegistry(
                        List.of(approvedProfile(), approvedProfile())));
        FarFieldProfileRegistry.ErrorEnvelope invalidEnvelope =
                new FarFieldProfileRegistry.ErrorEnvelope(
                        1.0D, 1.0D, true, -1.0D, 1.0D, 1.0D);

        assertThrows(
                IllegalArgumentException.class,
                () -> new FarFieldProfileRegistry.Profile(
                        key(),
                        5_000.0D,
                        new FarFieldProfileRegistry.ApplicabilityDomain(100_000.0D, 100.0D),
                        invalidEnvelope,
                        FarFieldProfileRegistry.Approval.APPROVED_STATIC_IMPEDANCE));
    }

    private static TopologyGuard.Evidence outdoorEvidence() {
        return TopologyGuard.Evidence.open(
                true, true, true, true, key());
    }

    private static FarFieldProfileRegistry registry(
            FarFieldProfileRegistry.Profile profile
    ) {
        return new FarFieldProfileRegistry(List.of(profile));
    }

    private static FarFieldProfileRegistry.Profile approvedProfile() {
        return profile(FarFieldProfileRegistry.Approval.APPROVED_STATIC_IMPEDANCE);
    }

    private static FarFieldProfileRegistry.Profile candidateProfile() {
        return profile(FarFieldProfileRegistry.Approval.CANDIDATE);
    }

    private static FarFieldProfileRegistry.Profile profile(
            FarFieldProfileRegistry.Approval approval
    ) {
        return new FarFieldProfileRegistry.Profile(
                key(),
                5_000.0D,
                new FarFieldProfileRegistry.ApplicabilityDomain(100_000.0D, 100.0D),
                new FarFieldProfileRegistry.ErrorEnvelope(
                        1.0D, 10.0D, false, -1_000.0D, 2_000.0D, 100.0D),
                approval);
    }

    private static FarFieldProfileRegistry.Key key() {
        return new FarFieldProfileRegistry.Key(
                0,
                FarFieldProfileRegistry.OpeningClass.MULTI_FACE,
                2,
                FarFieldProfileRegistry.Orientation.HORIZONTAL,
                FarFieldProfileRegistry.WindBucket.CALM,
                FarFieldProfileRegistry.EnvironmentClass.OVERWORLD_OUTDOOR,
                FarFieldProfileRegistry.TopologyClass.OPEN_SPACE);
    }
}
