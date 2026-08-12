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

import net.minecraft.util.RandomSource;

/** Pure generation and interpolation rules for the ordinary long-term climate events. */
public final class ClimateEventModel {
    private ClimateEventModel() {
    }

    public static EventDefinition generate(
            RandomSource random,
            long startTimeSeconds,
            Parameters parameters
    ) {
        int openingBonus = startTimeSeconds / parameters.secondsPerDay()
                <= parameters.openingBiasThroughDayInclusive()
                ? parameters.openingWarmRollBonus() : 0;
        int roll = random.nextInt(parameters.eventChoiceRollBound()) + openingBonus;
        return roll >= parameters.warmEventMinimumRollInclusive()
                ? generateWarm(random, startTimeSeconds, parameters)
                : generateCold(random, startTimeSeconds, parameters);
    }

    public static EventDefinition generateCold(
            RandomSource random,
            long startTimeSeconds,
            Parameters parameters
    ) {
        int weightTotal = parameters.coldBottomWeightExtreme()
                + parameters.coldBottomWeightSevere()
                + parameters.coldBottomWeightStrong()
                + parameters.coldBottomWeightNormal();
        int bottomRoll = random.nextInt(weightTotal);
        float bottom = chooseColdBottom(bottomRoll, parameters);
        long length = randomDurationSeconds(
                random, parameters.eventMinimumDays(), parameters.eventMaximumDaysExclusive(),
                parameters.secondsPerDay());
        long end = startTimeSeconds + length;
        long padding = randomDurationSeconds(
                random, parameters.paddingMinimumHours(), parameters.paddingMaximumHoursExclusive(),
                parameters.secondsPerHour());
        long peak = startTimeSeconds + padding;
        long bottomTime = startTimeSeconds + padding + (length - padding) / 4;
        float peakTemperature = parameters.coldPreludePeakCelsius()
                - (float) Math.abs(random.nextGaussian() * parameters.eventNoiseStandardDeviationCelsius());
        float bottomTemperature = bottom
                + (float) (random.nextGaussian() * parameters.eventNoiseStandardDeviationCelsius());
        long calm = randomDurationSeconds(
                random, parameters.calmMinimumDays(), parameters.calmMaximumDaysExclusive(),
                parameters.secondsPerDay());
        return new EventDefinition(
                startTimeSeconds, peak, peakTemperature, bottomTime, bottomTemperature,
                end, end + calm, true, false);
    }

    public static EventDefinition generateWarm(
            RandomSource random,
            long startTimeSeconds,
            Parameters parameters
    ) {
        long length = randomDurationSeconds(
                random, parameters.eventMinimumDays(), parameters.eventMaximumDaysExclusive(),
                parameters.secondsPerDay());
        long end = startTimeSeconds + length;
        long padding = randomDurationSeconds(
                random, parameters.paddingMinimumHours(), parameters.paddingMaximumHoursExclusive(),
                parameters.secondsPerHour());
        long peak = startTimeSeconds + padding + (length - padding) / 2;
        float peakTemperature = parameters.warmPeakCelsius()
                - (float) (Math.abs(random.nextGaussian())
                * parameters.warmNoiseScale()
                * parameters.eventNoiseStandardDeviationCelsius());
        long calm = randomDurationSeconds(
                random, parameters.calmMinimumDays(), parameters.calmMaximumDaysExclusive(),
                parameters.secondsPerDay());
        return new EventDefinition(
                startTimeSeconds, peak, peakTemperature, 0L, 0.0F,
                end, end + calm, false, false);
    }

    public static float temperatureAt(EventDefinition event, long timeSeconds) {
        if (event.cold()) {
            if (timeSeconds >= event.startTime() && timeSeconds < event.peakTime()) {
                return hermite(timeSeconds, event.startTime(), event.peakTime(),
                        0.0F, event.peakTemperatureCelsius());
            }
            if (timeSeconds >= event.peakTime() && timeSeconds < event.bottomTime()) {
                return hermite(timeSeconds, event.peakTime(), event.bottomTime(),
                        event.peakTemperatureCelsius(), event.bottomTemperatureCelsius());
            }
            if (timeSeconds >= event.bottomTime() && timeSeconds < event.endTime()) {
                return hermite(timeSeconds, event.bottomTime(), event.endTime(),
                        event.bottomTemperatureCelsius(), 0.0F);
            }
            return 0.0F;
        }
        if (timeSeconds >= event.startTime() && timeSeconds < event.peakTime()) {
            return hermite(timeSeconds, event.startTime(), event.peakTime(),
                    0.0F, event.peakTemperatureCelsius());
        }
        if (timeSeconds >= event.peakTime() && timeSeconds < event.endTime()) {
            return hermite(timeSeconds, event.peakTime(), event.endTime(),
                    event.peakTemperatureCelsius(), 0.0F);
        }
        return 0.0F;
    }

    /** Cubic Hermite interpolation with zero endpoint derivatives, as in current gameplay. */
    public static float hermite(long time, long start, long end, float startValue, float endValue) {
        if (end <= start) return endValue;
        float fraction = (float) (time - start) / (end - start);
        float oneMinus = 1.0F - fraction;
        return startValue * (1.0F + 2.0F * fraction) * oneMinus * oneMinus
                + endValue * (1.0F + 2.0F * oneMinus) * fraction * fraction;
    }

    private static float chooseColdBottom(int roll, Parameters parameters) {
        int boundary = parameters.coldBottomWeightExtreme();
        if (roll < boundary) return parameters.coldBottomExtremeCelsius();
        boundary += parameters.coldBottomWeightSevere();
        if (roll < boundary) return parameters.coldBottomSevereCelsius();
        boundary += parameters.coldBottomWeightStrong();
        if (roll < boundary) return parameters.coldBottomStrongCelsius();
        return parameters.coldBottomNormalCelsius();
    }

    private static long randomDurationSeconds(
            RandomSource random,
            int minimumUnits,
            int maximumUnitsExclusive,
            int secondsPerUnit
    ) {
        int spanSeconds = Math.multiplyExact(maximumUnitsExclusive - minimumUnits, secondsPerUnit);
        return (long) minimumUnits * secondsPerUnit + random.nextInt(spanSeconds);
    }

    public record EventDefinition(
            long startTime,
            long peakTime,
            float peakTemperatureCelsius,
            long bottomTime,
            float bottomTemperatureCelsius,
            long endTime,
            long calmEndTime,
            boolean cold,
            boolean blizzard
    ) {
    }

    public record Parameters(
            int secondsPerHour,
            int secondsPerDay,
            int eventChoiceRollBound,
            int warmEventMinimumRollInclusive,
            int openingWarmRollBonus,
            int openingBiasThroughDayInclusive,
            float coldBottomExtremeCelsius,
            float coldBottomSevereCelsius,
            float coldBottomStrongCelsius,
            float coldBottomNormalCelsius,
            int coldBottomWeightExtreme,
            int coldBottomWeightSevere,
            int coldBottomWeightStrong,
            int coldBottomWeightNormal,
            int eventMinimumDays,
            int eventMaximumDaysExclusive,
            int paddingMinimumHours,
            int paddingMaximumHoursExclusive,
            int calmMinimumDays,
            int calmMaximumDaysExclusive,
            float coldPreludePeakCelsius,
            float warmPeakCelsius,
            float eventNoiseStandardDeviationCelsius,
            float warmNoiseScale
    ) {
        public Parameters {
            if (secondsPerHour <= 0 || secondsPerDay <= 0
                    || eventChoiceRollBound <= 0
                    || warmEventMinimumRollInclusive < 0
                    || warmEventMinimumRollInclusive > eventChoiceRollBound
                    || eventMaximumDaysExclusive <= eventMinimumDays
                    || paddingMaximumHoursExclusive <= paddingMinimumHours
                    || calmMaximumDaysExclusive <= calmMinimumDays
                    || coldBottomWeightExtreme <= 0
                    || coldBottomWeightSevere <= 0
                    || coldBottomWeightStrong <= 0
                    || coldBottomWeightNormal <= 0) {
                throw new IllegalArgumentException("Invalid climate event parameters.");
            }
        }
    }
}
