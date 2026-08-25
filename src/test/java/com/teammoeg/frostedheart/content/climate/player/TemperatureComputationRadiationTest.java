/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemperatureComputationRadiationTest {
    @Test
    void radiantFluxConvertsToAbsorbedEnergyOverTheUpdateInterval() {
        assertEquals(0.0112F,
                TemperatureComputation.radiantBodyTemperatureDelta(100.0D, 20),
                1.0e-7F);
        assertEquals(0.0224F,
                TemperatureComputation.radiantBodyTemperatureDelta(100.0D, 40),
                1.0e-7F);
        assertEquals(0.0F,
                TemperatureComputation.radiantBodyTemperatureDelta(0.0D, 20));
        assertEquals(13.333333F,
                TemperatureComputation.radiantFeelingTemperatureDelta(100.0D),
                1.0e-6F);
        assertEquals(0.0F,
                TemperatureComputation.radiantFeelingTemperatureDelta(0.0D));
    }
}
