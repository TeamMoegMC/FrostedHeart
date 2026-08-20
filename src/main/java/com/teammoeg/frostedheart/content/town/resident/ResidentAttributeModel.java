/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.town.resident;

import java.util.Objects;
import java.util.function.DoubleSupplier;

/**
 * Pure numerical authority for resident attributes and profession proficiency.
 */
public final class ResidentAttributeModel {
    public static final double MIN_VALUE = 0.0;
    public static final double MAX_VALUE = 100.0;
    public static final double MAX_INITIAL_WORK_PROFICIENCY = 50.0;
    public static final double ELDER_PROFICIENCY_LOWER_BOUND = 50.0;
    /**
     * 非青壮年年龄组（幼儿/儿童/老人）属性生成的分布宽度：
     * 采样均值映射到 [center - 50*spread, center + 50*spread] 区间。
     */
    public static final double DEFAULT_ATTRIBUTE_SPREAD = 0.8;
    public static final int ADULT_ATTRIBUTE_SAMPLE_COUNT = 4;

    private ResidentAttributeModel() {
    }

    /**
     * Generates one center-biased attribute by averaging four independent
     * samples from the unit interval, then mapping the [0,1] mean onto a band
     * of width {@code spread} centered at {@code center}.
     */
    public static double generateAttribute(DoubleSupplier randomDouble, double center, double spread) {
        return generateAttribute(randomDouble, center, spread, ADULT_ATTRIBUTE_SAMPLE_COUNT);
    }

    /** Generates one center-biased attribute using the configured sample count. */
    public static double generateAttribute(
            DoubleSupplier randomDouble,
            double center,
            double spread,
            int sampleCount
    ) {
        Objects.requireNonNull(randomDouble);
        int safeSampleCount = Math.max(1, sampleCount);
        double sum = 0.0;
        for (int i = 0; i < safeSampleCount; i++) {
            sum += unitSample(randomDouble);
        }
        double mean = sum / safeSampleCount;
        return clampFinite(MAX_VALUE * (center / MAX_VALUE + (mean - 0.5) * spread), MIN_VALUE, MAX_VALUE, MIN_VALUE);
    }

    /**
     * Generates one center-biased adult attribute by averaging four independent
     * samples from the unit interval.
     */
    public static double generateAdultAttribute(DoubleSupplier randomDouble) {
        return generateAttribute(randomDouble, 50.0, 1.0);
    }

    /**
     * Generates prior profession experience in [0, max], biased toward low values.
     */
    public static double generateInitialWorkProficiency(DoubleSupplier randomDouble, double max) {
        Objects.requireNonNull(randomDouble);
        double sample = unitSample(randomDouble);
        return clampFinite(max * sample * sample, MIN_VALUE, MAX_VALUE, MIN_VALUE);
    }

    /**
     * Generates prior profession experience in [0, 50], biased toward low values.
     */
    public static double generateInitialWorkProficiency(DoubleSupplier randomDouble) {
        return generateInitialWorkProficiency(randomDouble, MAX_INITIAL_WORK_PROFICIENCY);
    }

    /**
     * Generates prior profession experience in [50, 100] for elders, who are
     * born with naturally higher work proficiency.
     */
    public static double generateElderInitialWorkProficiency(DoubleSupplier randomDouble) {
        return generateElderInitialWorkProficiency(
                randomDouble, ELDER_PROFICIENCY_LOWER_BOUND, MAX_VALUE);
    }

    public static double generateElderInitialWorkProficiency(
            DoubleSupplier randomDouble,
            double minimum,
            double maximum
    ) {
        Objects.requireNonNull(randomDouble);
        double sample = unitSample(randomDouble);
        double lower = clampFinite(minimum, MIN_VALUE, MAX_VALUE, MIN_VALUE);
        double upper = clampFinite(maximum, lower, MAX_VALUE, MAX_VALUE);
        return clampFinite(lower + (upper - lower) * sample, MIN_VALUE, MAX_VALUE, MIN_VALUE);
    }

    /**
     * Calculates one workday of diminishing proficiency growth.
     */
    public static double calculateDailyProficiencyGain(
            double proficiency,
            double growthAtZero,
            double minimumGrowth,
            double maximumProficiency
    ) {
        double safeMaximum = Math.max(1.0, finiteOr(maximumProficiency, MAX_VALUE));
        double normalizedProficiency = clampFinite(proficiency, MIN_VALUE, safeMaximum, MIN_VALUE);
        if (normalizedProficiency >= safeMaximum) {
            return 0.0;
        }

        double safeGrowthAtZero = clampFinite(growthAtZero, 0.0, safeMaximum, 0.0);
        double safeMinimumGrowth = clampFinite(minimumGrowth, 0.0, safeMaximum, 0.0);
        double diminishingGrowth = safeGrowthAtZero * (1.0 - normalizedProficiency / safeMaximum);
        double gain = Math.max(safeMinimumGrowth, diminishingGrowth);
        return Math.min(gain, safeMaximum - normalizedProficiency);
    }

    /**
     * Settles one day of strength or intelligence from current nutrition and activity.
     *
     * <p>Positive growth uses the age baseline plus the recorded activity's share of the remaining
     * day. Nutrition below the maintenance threshold activates a separate nonlinear decay term.
     * Elders may additionally receive a fixed age decay. The growth cap only stops positive growth;
     * it never clips an already acquired attribute.</p>
     *
     * @param currentValue stored attribute before settlement, in {@code 0..100}
     * @param recordedActivity completed physical or learning activity, in {@code 0..1}
     * @param baseActivity age-specific activity available without recorded work, in {@code 0..1}
     * @param nutritionSupport current strength or intelligence support, in {@code 0..1}
     * @param growthRate daily rate at full activity, full nutrition, and attribute zero
     * @param growthCap age-specific ceiling for positive growth
     * @param growthEfficiencyAtZeroSupport retained growth-efficiency fraction at zero support
     * @param maintenanceThreshold support below which nutrition decay activates
     * @param deficiencyExponent exponent applied to normalized distance below the threshold
     * @param decayAtZeroSupport maximum nutrition decay at support zero and attribute {@code 100}
     * @param ageDecay fixed age decay applied after nutrition decay, normally nonzero only for elders
     * @return clamped next value and a flat explanation of every transition component
     */
    public static ResidentAttributeChange settleDailyAttribute(
            double currentValue,
            double recordedActivity,
            double baseActivity,
            double nutritionSupport,
            double growthRate,
            double growthCap,
            double growthEfficiencyAtZeroSupport,
            double maintenanceThreshold,
            double deficiencyExponent,
            double decayAtZeroSupport,
            double ageDecay
    ) {
        double current = clampFinite(currentValue, MIN_VALUE, MAX_VALUE, MIN_VALUE);
        double activity = clampFinite(recordedActivity, 0.0, 1.0, 0.0);
        double baseline = clampFinite(baseActivity, 0.0, 1.0, 0.0);
        double effectiveActivity = baseline + (1.0 - baseline) * activity;
        double support = clampFinite(nutritionSupport, 0.0, 1.0, 0.0);
        double rate = Math.max(0.0, finiteOr(growthRate, 0.0));
        double cap = clampFinite(growthCap, 0.0, MAX_VALUE, 0.0);
        double zeroEfficiency = clampFinite(
                growthEfficiencyAtZeroSupport, 0.0, 1.0, 0.0);
        double growthEfficiency = zeroEfficiency + (1.0 - zeroEfficiency) * support;
        double remaining = cap <= 0.0 ? 0.0 : Math.max(0.0, 1.0 - current / cap);
        double growth = rate * effectiveActivity * growthEfficiency * remaining;
        growth = Math.min(Math.max(0.0, cap - current), Math.max(0.0, growth));

        double threshold = Math.max(0.0, finiteOr(maintenanceThreshold, 0.0));
        double deficiency = threshold <= 0.0 || support >= threshold
                ? 0.0 : (threshold - support) / threshold;
        double exponent = Math.max(0.0, finiteOr(deficiencyExponent, 1.0));
        double decay = Math.max(0.0, finiteOr(decayAtZeroSupport, 0.0))
                * Math.pow(deficiency, exponent) * current / MAX_VALUE;
        double safeAgeDecay = Math.max(0.0, finiteOr(ageDecay, 0.0));
        double next = clampFinite(
                current + growth - decay - safeAgeDecay,
                MIN_VALUE, MAX_VALUE, MIN_VALUE);
        return new ResidentAttributeChange(
                next, effectiveActivity, growth, decay, safeAgeDecay, next - current);
    }

    private static double unitSample(DoubleSupplier randomDouble) {
        return clampFinite(randomDouble.getAsDouble(), 0.0, 1.0, 0.0);
    }

    private static double finiteOr(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }

    private static double clampFinite(double value, double minimum, double maximum, double fallback) {
        if (!Double.isFinite(value)) {
            return fallback;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }
}
