/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.town;

import com.teammoeg.frostedheart.content.town.model.TownModelParameters;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TownMathFunctionsParameterTest {
    private static final double EPSILON = 1.0e-12;

    @Test
    void parameterizedBuildingRatingsPreserveLegacyDefaultFormulas() {
        TownModelParameters parameters = TownModelParameters.currentDefaults();
        TownModelParameters.TemperatureRatingParameters temperature =
                parameters.buildingScoring().temperature();
        TownModelParameters.SpaceRatingParameters space = parameters.buildingScoring().space();
        TownModelParameters.DecorationRatingParameters decoration =
                parameters.housing().decorationRating();

        double actualTemperature = TownMathFunctions.calculateTemperatureRating(
                14.0,
                temperature.comfortableTemperatureCelsius(),
                temperature.minimumRating(),
                temperature.sigmoidSlopePerCelsius(),
                temperature.halfPointTemperatureDifferenceCelsius());
        assertEquals(0.017 + 1.0 / 2.0, actualTemperature, EPSILON);

        double actualSpace = TownMathFunctions.calculateSpaceRating(
                24,
                12,
                space.areaCoefficient(),
                space.heightLogCoefficient(),
                space.heightLogOffset(),
                space.responseScale(),
                space.responseExponent());
        double legacySpaceScore = 12.0 * (1.55 + Math.log(2.0 - 1.6) * 0.6);
        assertEquals(1.0 - Math.exp(-0.024 * Math.pow(legacySpaceScore, 1.11)),
                actualSpace, EPSILON);

        double actualDecoration = TownMathFunctions.calculateDecorationRating(
                Map.of("chair", 2, "painting", 1),
                16,
                decoration.countLogOffset(),
                decoration.countLogMultiplier(),
                decoration.typeBaseScore(),
                decoration.baseDemand(),
                decoration.floorBlocksPerDemand());
        double legacyDecorationScore = (Math.log(2.32) * 1.75 + 0.9)
                + (Math.log(1.32) * 1.75 + 0.9);
        assertEquals(Math.min(1.0, legacyDecorationScore / 7.0), actualDecoration, EPSILON);
    }
}
