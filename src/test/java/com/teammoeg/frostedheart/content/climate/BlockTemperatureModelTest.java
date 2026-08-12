/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockTemperatureModelTest {
    private static final double EPSILON = 1.0e-6;

    @Test
    void climateAffectionMatchesCurrentSeaAndStoneBoundaries() {
        assertEquals(0.0, BlockTemperatureModel.climateBlockAffection(0, 0, 63, 0.5F), EPSILON);
        assertEquals(0.25, BlockTemperatureModel.climateBlockAffection(31, 0, 62, 0.5F), EPSILON);
        assertEquals(0.5, BlockTemperatureModel.climateBlockAffection(63, 0, 63, 0.5F), EPSILON);
        assertEquals(0.5, BlockTemperatureModel.climateBlockAffection(64, 0, 63, 0.5F), EPSILON);
    }

    @Test
    void t1HeatUsesCurrentDoubleHeatAndCeilingRule() {
        assertEquals(10.0, BlockTemperatureModel.applyHeat(-10.0F, 10.0F, 2.0F, -273.0F), EPSILON);
        assertEquals(5.0, BlockTemperatureModel.applyHeat(-15.0F, 10.0F, 2.0F, -273.0F), EPSILON);
        assertEquals(0.0, BlockTemperatureModel.applyHeat(-20.0F, 10.0F, 2.0F, -273.0F), EPSILON);
        assertEquals(12.0, BlockTemperatureModel.applyHeat(12.0F, 10.0F, 2.0F, -273.0F), EPSILON);
    }
}
