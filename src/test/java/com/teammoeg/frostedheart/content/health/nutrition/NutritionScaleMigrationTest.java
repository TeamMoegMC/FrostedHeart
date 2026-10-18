/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
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
