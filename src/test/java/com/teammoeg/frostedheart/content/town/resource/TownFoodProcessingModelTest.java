/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 */

package com.teammoeg.frostedheart.content.town.resource;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TownFoodProcessingModelTest {
    @Test
    void processingConservesMeatAndUsesHighestFoodGainFirst() {
        TownFoodProcessingModel.MeatDefinition beef = new TownFoodProcessingModel.MeatDefinition(
                "beef", "cooked_beef", 4.8, 20.8, 3000, 6000, 1, 2);
        TownFoodProcessingModel.MeatDefinition chicken = new TownFoodProcessingModel.MeatDefinition(
                "chicken", "cooked_chicken", 3.2, 13.2, 1000, 6000, 0, 2);

        TownFoodProcessingModel.ProcessingResult result = TownFoodProcessingModel.process(
                Map.of("beef", 2, "chicken", 2), 2.5, List.of(chicken, beef));

        assertEquals(4.0, result.rawInputItems(), 1.0e-12);
        assertEquals(2.5, result.processedItems(), 1.0e-12);
        assertEquals(1.5, result.remainingRawItems(), 1.0e-12);
        assertEquals(2.0, result.cooked().get("cooked_beef"), 1.0e-12);
        assertEquals(0.5, result.cooked().get("cooked_chicken"), 1.0e-12);
    }
}
