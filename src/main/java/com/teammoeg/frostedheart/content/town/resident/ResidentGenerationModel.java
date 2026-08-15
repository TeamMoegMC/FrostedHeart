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
        AttributeCenters centers = parameters.centers(age);
        double spread = age == Resident.AGE_ADULT
                ? parameters.adultAttributeSpread() : parameters.nonAdultAttributeSpread();
        double strength = ResidentAttributeModel.generateAttribute(
                randomDouble, centers.strength(), spread, parameters.attributeSampleCount());
        double intelligence = ResidentAttributeModel.generateAttribute(
                randomDouble, centers.intelligence(), spread, parameters.attributeSampleCount());
        // Gameplay initializes hunting before mining; retain that order for paired-seed parity.
        double hunting = generateProficiency(age, randomDouble, parameters);
        double mining = generateProficiency(age, randomDouble, parameters);
        return new GeneratedResident(
                age, ageDays, parameters.initialHealth(), parameters.initialMental(),
                strength, intelligence, mining, hunting);
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

        private static double finiteNonNegative(double value) {
            return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
        }
    }

    public record AttributeCenters(double strength, double intelligence) {
    }

    public record Parameters(
            double initialHealth,
            double initialMental,
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
            AgeWeights fallbackAgeWeights
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
            double strength,
            double intelligence,
            double miningProficiency,
            double huntingProficiency
    ) {
    }
}
