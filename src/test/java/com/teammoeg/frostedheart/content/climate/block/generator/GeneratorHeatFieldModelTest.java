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
 */

package com.teammoeg.frostedheart.content.climate.block.generator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeneratorHeatFieldModelTest {
    @Test
    void currentT1LevelsRemainUnchanged() {
        assertEquals(16, GeneratorHeatFieldModel.radiusBlocks(1.0, 16, 8));
        assertEquals(23, GeneratorHeatFieldModel.radiusBlocks(1.99, 16, 8));
        assertEquals(24, GeneratorHeatFieldModel.radiusBlocks(2.0, 16, 8));
        assertEquals(10, GeneratorHeatFieldModel.temperatureCelsius(1.0, 10));
        assertEquals(20, GeneratorHeatFieldModel.temperatureCelsius(2.0, 10));
    }
}
