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

import java.util.Objects;
import java.util.function.DoubleSupplier;
import java.util.function.IntUnaryOperator;

/**
 * Pure resident recruitment model shared by gameplay and the town simulator.
 * Random sources are supplied by the caller so the same equations can use
 * Minecraft's RNG in game and fixed seeds in numerical experiments.
 */
public final class ResidentGenerationModel {
    private ResidentGenerationModel() {
    }

    public static GeneratedResident generate(
            DoubleSupplier randomDouble,
            IntUnaryOperator randomInt,
            Parameters parameters
    ) {
        Objects.requireNonNull(randomDouble);
        Objects.requireNonNull(randomInt);
        Objects.requireNonNull(parameters);
        int age = pickAge(randomDouble, parameters.ageWeights(), parameters.fallbackAgeWeights());
        int ageDays = randomAgeDays(age, randomInt, parameters);
        return generateForAge(randomDouble, age, ageDays, parameters);
    }

    public static GeneratedResident generateForAge(
            DoubleSupplier randomDouble,
            int age,
            int ageDays,
            Parameters parameters
    ) {
        Objects.requireNonNull(randomDouble);
        Objects.requireNonNull(parameters);
        AttributeCenters centers = parameters.centers(age);
        double spread = age == Resident.AGE_ADULT
                ? parameters.adultAttributeSpread() : parameters.nonAdultAttributeSpread();
        double health = generateBoundedAverage(
                randomDouble, parameters.initialHealthMinimum(),
                parameters.initialHealthMaximum(), parameters.attributeSampleCount());
        double mental = generateBoundedAverage(
                randomDouble, parameters.initialMentalMinimum(),
                parameters.initialMentalMaximum(), parameters.attributeSampleCount());
        ResidentNutrition nutrition = new ResidentNutrition(
                generateBoundedAverage(randomDouble, parameters.initialNutritionMinimum(),
                        parameters.initialNutritionMaximum(), parameters.attributeSampleCount()),
                generateBoundedAverage(randomDouble, parameters.initialNutritionMinimum(),
                        parameters.initialNutritionMaximum(), parameters.attributeSampleCount()),
                generateBoundedAverage(randomDouble, parameters.initialNutritionMinimum(),
                        parameters.initialNutritionMaximum(), parameters.attributeSampleCount()),
                generateBoundedAverage(randomDouble, parameters.initialNutritionMinimum(),
                        parameters.initialNutritionMaximum(), parameters.attributeSampleCount()));
        double strength = ResidentAttributeModel.generateAttribute(
                randomDouble, centers.strength(), spread, parameters.attributeSampleCount());
        double intelligence = ResidentAttributeModel.generateAttribute(
                randomDouble, centers.intelligence(), spread, parameters.attributeSampleCount());
        int educationLevel = pickEducationLevel(randomDouble, parameters.educationWeights());
        // Gameplay initializes hunting, mining, then transport; retain that order for paired-seed parity.
        double hunting = generateProficiency(age, randomDouble, parameters);
        double mining = generateProficiency(age, randomDouble, parameters);
        double transport = generateProficiency(age, randomDouble, parameters);
        return new GeneratedResident(
                age, Math.max(0, ageDays), health, mental, nutrition,
                strength, intelligence, educationLevel, mining, hunting, transport);
    }

    public static int pickAge(
            DoubleSupplier randomDouble,
            AgeWeights configured,
            AgeWeights fallback
    ) {
        Objects.requireNonNull(randomDouble);
        AgeWeights weights = configured.nonNegative();
        if (weights.total() <= 0.0) weights = fallback.nonNegative();
        if (weights.total() <= 0.0) return Resident.AGE_ADULT;
        double roll = unitSample(randomDouble) * weights.total();
        if (roll < weights.infant()) return Resident.AGE_INFANT;
        roll -= weights.infant();
        if (roll < weights.child()) return Resident.AGE_CHILD;
        roll -= weights.child();
        if (roll < weights.adult()) return Resident.AGE_ADULT;
        return Resident.AGE_ELDER;
    }

    public static int randomAgeDays(
            int age,
            IntUnaryOperator randomInt,
            Parameters parameters
    ) {
        Objects.requireNonNull(randomInt);
        int infantDays = Math.max(1, parameters.infantToChildDays());
        int childDays = Math.max(1, parameters.childToAdultDays() - infantDays);
        int adultRange = Math.max(1, parameters.adultAgeRangeDaysExclusive());
        return switch (age) {
            case Resident.AGE_INFANT -> boundedRandomInt(randomInt, infantDays);
            case Resident.AGE_CHILD -> infantDays + boundedRandomInt(randomInt, childDays);
            case Resident.AGE_ADULT, Resident.AGE_ELDER ->
                    Math.max(0, parameters.childToAdultDays())
                            + boundedRandomInt(randomInt, adultRange);
            default -> throw new IllegalArgumentException("Unsupported resident age: " + age);
        };
    }

    public static double generateProficiency(
            int age,
            DoubleSupplier randomDouble,
            Parameters parameters
    ) {
        return switch (age) {
            case Resident.AGE_INFANT -> parameters.infantInitialProficiency();
            case Resident.AGE_CHILD -> ResidentAttributeModel.generateInitialWorkProficiency(
                    randomDouble, parameters.childMaximumInitialProficiency());
            case Resident.AGE_ELDER -> ResidentAttributeModel.generateElderInitialWorkProficiency(
                    randomDouble, parameters.elderMinimumInitialProficiency(),
                    parameters.elderMaximumInitialProficiency());
            case Resident.AGE_ADULT -> ResidentAttributeModel.generateInitialWorkProficiency(
                    randomDouble, parameters.adultMaximumInitialProficiency());
            default -> throw new IllegalArgumentException("Unsupported resident age: " + age);
        };
    }

    public static double generateBoundedAverage(
            DoubleSupplier randomDouble,
            double firstBound,
            double secondBound,
            int sampleCount
    ) {
        Objects.requireNonNull(randomDouble);
        double minimum = finiteOrZero(Math.min(firstBound, secondBound));
        double maximum = finiteOrZero(Math.max(firstBound, secondBound));
        int samples = Math.max(1, sampleCount);
        double sum = 0.0;
        for (int index = 0; index < samples; index++) {
            sum += unitSample(randomDouble);
        }
        return minimum + (maximum - minimum) * sum / samples;
    }

    public static int pickEducationLevel(
            DoubleSupplier randomDouble,
            EducationWeights configured
    ) {
        Objects.requireNonNull(randomDouble);
        EducationWeights weights = configured.nonNegative();
        double total = weights.total();
        if (total <= 0.0) return 1;
        double roll = unitSample(randomDouble) * total;
        for (int level = 0; level <= 5; level++) {
            double weight = weights.at(level);
            if (roll < weight) return level;
            roll -= weight;
        }
        return 5;
    }

    private static int boundedRandomInt(IntUnaryOperator randomInt, int bound) {
        int safeBound = Math.max(1, bound);
        int value = randomInt.applyAsInt(safeBound);
        return Math.max(0, Math.min(safeBound - 1, value));
    }

    private static double unitSample(DoubleSupplier randomDouble) {
        double value = randomDouble.getAsDouble();
        if (!Double.isFinite(value)) return 0.0;
        return Math.max(0.0, Math.min(Math.nextDown(1.0), value));
    }

    public record AgeWeights(double infant, double child, double adult, double elder) {
        public AgeWeights nonNegative() {
            return new AgeWeights(
                    finiteNonNegative(infant), finiteNonNegative(child),
                    finiteNonNegative(adult), finiteNonNegative(elder));
        }

        public double total() {
            return infant + child + adult + elder;
        }

    }

    public record AttributeCenters(double strength, double intelligence) {
    }

    public record EducationWeights(
            double level0,
            double level1,
            double level2,
            double level3,
            double level4,
            double level5
    ) {
        public EducationWeights nonNegative() {
            return new EducationWeights(
                    finiteNonNegative(level0), finiteNonNegative(level1),
                    finiteNonNegative(level2), finiteNonNegative(level3),
                    finiteNonNegative(level4), finiteNonNegative(level5));
        }

        public double total() {
            return level0 + level1 + level2 + level3 + level4 + level5;
        }

        public double at(int level) {
            return switch (level) {
                case 0 -> level0;
                case 1 -> level1;
                case 2 -> level2;
                case 3 -> level3;
                case 4 -> level4;
                case 5 -> level5;
                default -> 0.0;
            };
        }
    }

    public record Parameters(
            double initialHealthMinimum,
            double initialHealthMaximum,
            double initialMentalMinimum,
            double initialMentalMaximum,
            double initialNutritionMinimum,
            double initialNutritionMaximum,
            int attributeSampleCount,
            AttributeCenters infantCenters,
            AttributeCenters childCenters,
            AttributeCenters adultCenters,
            AttributeCenters elderCenters,
            double nonAdultAttributeSpread,
            double adultAttributeSpread,
            double infantInitialProficiency,
            double childMaximumInitialProficiency,
            double adultMaximumInitialProficiency,
            double elderMinimumInitialProficiency,
            double elderMaximumInitialProficiency,
            int infantToChildDays,
            int childToAdultDays,
            int adultAgeRangeDaysExclusive,
            AgeWeights ageWeights,
            AgeWeights fallbackAgeWeights,
            EducationWeights educationWeights
    ) {
        public AttributeCenters centers(int age) {
            return switch (age) {
                case Resident.AGE_INFANT -> infantCenters;
                case Resident.AGE_CHILD -> childCenters;
                case Resident.AGE_ELDER -> elderCenters;
                case Resident.AGE_ADULT -> adultCenters;
                default -> throw new IllegalArgumentException("Unsupported resident age: " + age);
            };
        }
    }

    public record GeneratedResident(
            int age,
            int ageDays,
            double health,
            double mental,
            ResidentNutrition nutrition,
            double strength,
            double intelligence,
            int educationLevel,
            double miningProficiency,
            double huntingProficiency,
            double transportProficiency
    ) {
    }


    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }

    private static double finiteNonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }
}
