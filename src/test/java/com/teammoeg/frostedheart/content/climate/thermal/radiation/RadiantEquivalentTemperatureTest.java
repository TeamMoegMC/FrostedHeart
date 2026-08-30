/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.thermal.radiation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadiantEquivalentTemperatureTest {
    @Test
    void preservesTheFrozenZeroAndOneHundredFluxContract() {
        assertEquals(0.0D, RadiantEquivalentTemperature.deltaC(0.0D));
        assertEquals(13.333333333333334D,
                RadiantEquivalentTemperature.deltaC(100.0D));
        assertEquals(33.333333333333336D,
                RadiantEquivalentTemperature.effectiveEnvironmentTemperatureC(20.0D, 100.0D));
    }

    @Test
    void invalidInputsRemainFiniteAndContributeNoRadiantHeat() {
        assertEquals(0.0D, RadiantEquivalentTemperature.deltaC(-1.0D));
        assertEquals(0.0D, RadiantEquivalentTemperature.deltaC(Double.NaN));
        assertEquals(0.0D, RadiantEquivalentTemperature.deltaC(Double.POSITIVE_INFINITY));
        assertTrue(Double.isFinite(
                RadiantEquivalentTemperature.effectiveEnvironmentTemperatureC(
                        Double.NaN, Double.POSITIVE_INFINITY)));
    }
}
