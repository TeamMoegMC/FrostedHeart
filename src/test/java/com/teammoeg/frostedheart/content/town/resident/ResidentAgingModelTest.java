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

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResidentAgingModelTest {
    private static final TownModelParameters.ResidentAgingParameters SOURCE =
            TownModelParameters.currentDefaults().residents().aging();
    private static final ResidentAgingModel.Parameters PARAMETERS =
            new ResidentAgingModel.Parameters(
                    SOURCE.infantToChildDays(), SOURCE.childToAdultDays(),
                    SOURCE.infantStrengthGainPerDay(), SOURCE.infantIntelligenceGainPerDay(),
                    SOURCE.infantAttributeCap(), SOURCE.childStrengthGainPerDay(),
                    SOURCE.childIntelligenceGainPerDay(), SOURCE.childStrengthCap(),
                    SOURCE.childIntelligenceCap(), SOURCE.adultStrengthGainPerDay(),
                    SOURCE.adultIntelligenceGainPerDay(), SOURCE.adultAttributeCap(),
                    SOURCE.elderStrengthDecayPerDay(), SOURCE.elderStrengthFloor());

    @Test
    void adultGrowthUsesDailyGainAndCap() {
        ResidentAgingModel.AgingResult normal = ResidentAgingModel.settleDay(
                Resident.AGE_ADULT, 30, 50.0, 59.98, PARAMETERS);
        ResidentAgingModel.AgingResult capped = ResidentAgingModel.settleDay(
                Resident.AGE_ADULT, 31, normal.strength(), normal.intelligence(), PARAMETERS);

        assertEquals(50.05, normal.strength(), 1.0e-12);
        assertEquals(60.0, normal.intelligence(), 1.0e-12);
        assertEquals(50.10, capped.strength(), 1.0e-12);
        assertEquals(60.0, capped.intelligence(), 1.0e-12);
    }

    @Test
    void transitionDayChangesAgeWithoutApplyingPreviousAgeGrowth() {
        ResidentAgingModel.AgingResult result = ResidentAgingModel.settleDay(
                Resident.AGE_INFANT, PARAMETERS.infantToChildDays() - 1,
                20.0, 30.0, PARAMETERS);

        assertEquals(Resident.AGE_CHILD, result.age());
        assertEquals(PARAMETERS.infantToChildDays(), result.ageDays());
        assertEquals(20.0, result.strength(), 1.0e-12);
        assertEquals(30.0, result.intelligence(), 1.0e-12);
    }

    @Test
    void proteinAndFatScaleOnlyTheirMappedGrowthChannels() {
        ResidentAgingModel.AgingResult result = ResidentAgingModel.settleDay(
                Resident.AGE_CHILD, 20, 30.0, 30.0,
                new ResidentNutrition(100, 70, 0, 70), PARAMETERS);

        assertEquals(30.0 + PARAMETERS.childStrengthGainPerDay() * 0.5,
                result.strength(), 1.0e-12);
        assertEquals(30.0 + PARAMETERS.childIntelligenceGainPerDay() * 1.25,
                result.intelligence(), 1.0e-12);
    }
}
