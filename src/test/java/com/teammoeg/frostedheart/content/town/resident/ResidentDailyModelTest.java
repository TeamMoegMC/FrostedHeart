/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.town.resident;

import com.teammoeg.frostedheart.content.town.model.TownModelParameters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ResidentDailyModelTest {
    @Test
    void currentDefaultsPreserveStrictWorkThresholdsAndHousingRequirement() {
        TownModelParameters.ResidentParameters parameters =
                TownModelParameters.currentDefaults().residents();

        assertFalse(canWork(parameters, 1, 10.0, 50.0, true));
        assertTrue(canWork(parameters, 1, Math.nextUp(10.0), 50.0, true));
        assertFalse(canWork(parameters, 1, 50.0, 5.0, true));
        assertFalse(canWork(parameters, 0, 50.0, 50.0, true));
        assertFalse(canWork(parameters, 1, 50.0, 50.0, false));
    }

    @Test
    void homelessPenaltyIsAppliedBeforeInclusiveRemovalThreshold() {
        TownModelParameters.ResidentParameters parameters =
                TownModelParameters.currentDefaults().residents();
        ResidentDailyModel.MorningResult result = ResidentDailyModel.settleMorning(
                15.0,
                50.0,
                false,
                parameters.homelessHealthLossPerDay(),
                parameters.removalHealthThreshold(),
                parameters.removalMentalThreshold());

        assertEquals(5.0, result.healthAfterHomelessPenalty());
        assertTrue(result.removedForHealth());
        assertTrue(result.removed());
    }

    @Test
    void proficiencyGrowthUsesTheConfiguredStorageMaximum() {
        TownModelParameters.ResidentParameters parameters =
                TownModelParameters.currentDefaults().residents();
        assertEquals(1.2, ResidentAttributeModel.calculateDailyProficiencyGain(
                50.0,
                parameters.proficiencyGrowthAtZeroPerWorkday(),
                parameters.minimumProficiencyGrowthPerWorkday(),
                parameters.maximumWorkProficiency()), 1.0e-12);
        assertEquals(1.8, ResidentAttributeModel.calculateDailyProficiencyGain(
                50.0,
                parameters.proficiencyGrowthAtZeroPerWorkday(),
                parameters.minimumProficiencyGrowthPerWorkday(),
                200.0), 1.0e-12);
    }

    private static boolean canWork(
            TownModelParameters.ResidentParameters parameters,
            int age,
            double health,
            double mental,
            boolean hasHousing
    ) {
        return ResidentDailyModel.canWork(
                age,
                health,
                mental,
                hasHousing,
                parameters.minimumWorkingAge(),
                parameters.minimumWorkingHealthExclusive(),
                parameters.minimumWorkingMentalExclusive(),
                parameters.workRequiresHousing());
    }
}
