/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.health.nutrition;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NutritionScaleMigrationTest {
    @Test
    void versionOneDefaultsConvertToPercentageScaleDefaults() {
        NutritionScaleMigration.Rates rates =
                NutritionScaleMigration.fromVersionOne(0.0025, 0.0025);

        assertEquals(1.0, rates.gainRate(), 1.0e-12);
        assertEquals(0.25, rates.consumptionRate(), 1.0e-12);
    }
}
