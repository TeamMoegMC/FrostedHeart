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

import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResidentGenerationModelTest {
    @Test
    void fixedSeedReproducesAllGeneratedResidentFields() {
        ResidentGenerationModel.Parameters parameters = parameters();
        SplittableRandom first = new SplittableRandom(9182L);
        SplittableRandom second = new SplittableRandom(9182L);
        for (int index = 0; index < 100; index++) {
            assertEquals(
                    ResidentGenerationModel.generate(
                            first::nextDouble, first::nextInt, parameters),
                    ResidentGenerationModel.generate(
                            second::nextDouble, second::nextInt, parameters));
        }
    }

    @Test
    void configuredAgeWeightsProduceCurrentTenTwentySixtyTenMix() {
        ResidentGenerationModel.Parameters parameters = parameters();
        SplittableRandom random = new SplittableRandom(77L);
        int[] counts = new int[4];
        int samples = 100_000;
        for (int index = 0; index < samples; index++) {
            counts[ResidentGenerationModel.pickAge(
                    random::nextDouble, parameters.ageWeights(),
                    parameters.fallbackAgeWeights())]++;
        }
        double[] fractions = new double[4];
        for (int index = 0; index < counts.length; index++) {
            fractions[index] = (double) counts[index] / samples;
        }
        assertArrayEquals(new double[]{0.10, 0.20, 0.60, 0.10}, fractions, 0.005);
    }

    @Test
    void generatedAgeDaysAndProficienciesRespectGameplayBounds() {
        ResidentGenerationModel.Parameters parameters = parameters();
        SplittableRandom random = new SplittableRandom(190L);
        for (int index = 0; index < 10_000; index++) {
            ResidentGenerationModel.GeneratedResident resident =
                    ResidentGenerationModel.generate(
                            random::nextDouble, random::nextInt, parameters);
            switch (resident.age()) {
                case Resident.AGE_INFANT -> {
                    assertTrue(resident.ageDays() < parameters.infantToChildDays());
                    assertEquals(0.0, resident.miningProficiency(), 0.0);
                    assertEquals(0.0, resident.huntingProficiency(), 0.0);
                }
                case Resident.AGE_CHILD -> {
                    assertTrue(resident.ageDays() >= parameters.infantToChildDays());
                    assertTrue(resident.ageDays() < parameters.childToAdultDays());
                    assertTrue(resident.miningProficiency()
                            <= parameters.childMaximumInitialProficiency());
                }
                case Resident.AGE_ADULT -> assertTrue(resident.miningProficiency()
                        <= parameters.adultMaximumInitialProficiency());
                case Resident.AGE_ELDER -> assertTrue(resident.miningProficiency()
                        >= parameters.elderMinimumInitialProficiency());
                default -> throw new AssertionError("Unsupported generated age");
            }
        }
    }

    private static ResidentGenerationModel.Parameters parameters() {
        TownModelParameters.ResidentParameters residents =
                TownModelParameters.currentDefaults().residents();
        TownModelParameters.ResidentGenerationParameters generation = residents.generation();
        TownModelParameters.ResidentAgingParameters aging = residents.aging();
        return new ResidentGenerationModel.Parameters(
                generation.initialHealth(), generation.initialMental(),
                generation.attributeSampleCount(),
                new ResidentGenerationModel.AttributeCenters(
                        generation.infantStrengthCenter(), generation.infantIntelligenceCenter()),
                new ResidentGenerationModel.AttributeCenters(
                        generation.childStrengthCenter(), generation.childIntelligenceCenter()),
                new ResidentGenerationModel.AttributeCenters(
                        generation.adultStrengthCenter(), generation.adultIntelligenceCenter()),
                new ResidentGenerationModel.AttributeCenters(
                        generation.elderStrengthCenter(), generation.elderIntelligenceCenter()),
                generation.nonAdultAttributeSpread(), generation.adultAttributeSpread(),
                generation.infantInitialProficiency(),
                generation.childMaximumInitialProficiency(),
                generation.adultMaximumInitialProficiency(),
                generation.elderMinimumInitialProficiency(),
                generation.elderMaximumInitialProficiency(),
                aging.infantToChildDays(), aging.childToAdultDays(),
                generation.adultAgeRangeDaysExclusive(),
                weights(generation.ageWeights()), weights(generation.fallbackAgeWeights()));
    }

    private static ResidentGenerationModel.AgeWeights weights(
            TownModelParameters.ResidentAgeWeightParameters weights
    ) {
        return new ResidentGenerationModel.AgeWeights(
                weights.infant(), weights.child(), weights.adult(), weights.elder());
    }
}
