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

/** Pure one-day age and attribute transition used by gameplay and simulation. */
public final class ResidentAgingModel {
    private ResidentAgingModel() {
    }

    public static AgingResult settleDay(
            int age,
            int ageDays,
            double strength,
            double intelligence,
            Parameters parameters
    ) {
        int nextDays = Math.max(0, ageDays) + 1;
        int nextAge = age;
        double nextStrength = boundedAttribute(strength);
        double nextIntelligence = boundedAttribute(intelligence);
        switch (age) {
            case Resident.AGE_INFANT -> {
                if (nextDays >= parameters.infantToChildDays()) {
                    nextAge = Resident.AGE_CHILD;
                } else {
                    nextStrength = grow(nextStrength,
                            parameters.infantStrengthGainPerDay(), parameters.infantAttributeCap());
                    nextIntelligence = grow(nextIntelligence,
                            parameters.infantIntelligenceGainPerDay(), parameters.infantAttributeCap());
                }
            }
            case Resident.AGE_CHILD -> {
                if (nextDays >= parameters.childToAdultDays()) {
                    nextAge = Resident.AGE_ADULT;
                } else {
                    nextStrength = grow(nextStrength,
                            parameters.childStrengthGainPerDay(), parameters.childStrengthCap());
                    nextIntelligence = grow(nextIntelligence,
                            parameters.childIntelligenceGainPerDay(), parameters.childIntelligenceCap());
                }
            }
            case Resident.AGE_ADULT -> {
                nextStrength = grow(nextStrength,
                        parameters.adultStrengthGainPerDay(), parameters.adultAttributeCap());
                nextIntelligence = grow(nextIntelligence,
                        parameters.adultIntelligenceGainPerDay(), parameters.adultAttributeCap());
            }
            case Resident.AGE_ELDER -> nextStrength = decay(nextStrength,
                    parameters.elderStrengthDecayPerDay(), parameters.elderStrengthFloor());
            default -> throw new IllegalArgumentException("Unsupported resident age: " + age);
        }
        return new AgingResult(nextAge, nextDays, nextStrength, nextIntelligence);
    }

    private static double grow(double value, double gain, double cap) {
        double safeCap = boundedAttribute(cap);
        return value < safeCap ? Math.min(safeCap, value + nonNegative(gain)) : value;
    }

    private static double decay(double value, double decay, double floor) {
        double safeFloor = boundedAttribute(floor);
        return value > safeFloor ? Math.max(safeFloor, value - nonNegative(decay)) : value;
    }

    private static double boundedAttribute(double value) {
        if (!Double.isFinite(value)) return 0.0;
        return Math.max(0.0, Math.min(100.0, value));
    }

    private static double nonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }

    public record AgingResult(int age, int ageDays, double strength, double intelligence) {
    }

    public record Parameters(
            int infantToChildDays,
            int childToAdultDays,
            double infantStrengthGainPerDay,
            double infantIntelligenceGainPerDay,
            double infantAttributeCap,
            double childStrengthGainPerDay,
            double childIntelligenceGainPerDay,
            double childStrengthCap,
            double childIntelligenceCap,
            double adultStrengthGainPerDay,
            double adultIntelligenceGainPerDay,
            double adultAttributeCap,
            double elderStrengthDecayPerDay,
            double elderStrengthFloor
    ) {
    }
}
