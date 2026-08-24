/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.phase0.reference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseZeroThermalRoutingTest {
    @Test
    void defaultRouteKeepsLegacyGameplayAuthoritative() {
        PhaseZeroThermalRouting.Decision decision = PhaseZeroThermalRouting.defaults();

        assertEquals(PhaseZeroThermalRouting.RequestedMode.LEGACY, decision.requestedMode());
        assertEquals(PhaseZeroThermalRouting.GameplayAuthority.LEGACY, decision.gameplayAuthority());
        assertFalse(decision.v1ShadowEnabled());
        assertFalse(decision.v1ProductionEnabled());
    }

    @Test
    void shadowCannotAffectGameplayOutputs() {
        PhaseZeroThermalRouting.Decision decision = PhaseZeroThermalRouting.select(
                PhaseZeroThermalRouting.RequestedMode.SHADOW
        );

        assertEquals(PhaseZeroThermalRouting.GameplayAuthority.LEGACY, decision.gameplayAuthority());
        assertTrue(decision.v1ShadowEnabled());
        assertFalse(decision.v1ProductionEnabled());
    }

    @Test
    void productionRequestRemainsBlockedUntilLaterGatesPass() {
        PhaseZeroThermalRouting.Decision decision = PhaseZeroThermalRouting.select(
                PhaseZeroThermalRouting.RequestedMode.V1_PRODUCTION
        );

        assertEquals(PhaseZeroThermalRouting.GameplayAuthority.LEGACY, decision.gameplayAuthority());
        assertTrue(decision.v1ShadowEnabled());
        assertFalse(decision.v1ProductionEnabled());
        assertEquals(
                PhaseZeroThermalRouting.Reason.V1_PRODUCTION_NOT_APPROVED,
                decision.reason()
        );
    }
}
