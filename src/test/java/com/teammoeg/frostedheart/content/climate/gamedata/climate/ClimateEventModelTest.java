/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.climate.gamedata.climate;

import com.teammoeg.frostedheart.content.town.model.TownModelParameters;
import com.teammoeg.frostedheart.content.town.model.TownStageFourModel;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClimateEventModelTest {
    private static final double EPSILON = 1.0e-6;

    @Test
    void eventGenerationAndInterpolationAreFixedSeedReproducible() {
        ClimateEventModel.Parameters parameters = TownStageFourModel.eventParameters(
                TownModelParameters.currentDefaults().climate());
        ClimateEventModel.EventDefinition first = ClimateEventModel.generate(
                RandomSource.create(12345L), 365L * WorldClockSource.secondsPerDay, parameters);
        ClimateEventModel.EventDefinition second = ClimateEventModel.generate(
                RandomSource.create(12345L), 365L * WorldClockSource.secondsPerDay, parameters);
        assertEquals(first, second);
        assertEquals(0.0, ClimateEventModel.temperatureAt(first, first.startTime()), EPSILON);
        assertEquals(first.peakTemperatureCelsius(),
                ClimateEventModel.temperatureAt(first, first.peakTime()), EPSILON);
        assertEquals(0.0, ClimateEventModel.temperatureAt(first, first.endTime()), EPSILON);
    }

    @Test
    void zeroDerivativeHermiteHitsEndpointsAndMidpoint() {
        assertEquals(2.0, ClimateEventModel.hermite(10, 10, 20, 2.0F, 6.0F), EPSILON);
        assertEquals(4.0, ClimateEventModel.hermite(15, 10, 20, 2.0F, 6.0F), EPSILON);
        assertEquals(6.0, ClimateEventModel.hermite(20, 10, 20, 2.0F, 6.0F), EPSILON);
    }
}
