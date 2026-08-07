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
 * Pure numerical model for fixed adult attributes and profession proficiency.
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
        Objects.requireNonNull(randomDouble);
        double sum = 0.0;
        for (int i = 0; i < ADULT_ATTRIBUTE_SAMPLE_COUNT; i++) {
            sum += unitSample(randomDouble);
        }
        double mean = sum / ADULT_ATTRIBUTE_SAMPLE_COUNT;
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
        Objects.requireNonNull(randomDouble);
        double sample = unitSample(randomDouble);
        return clampFinite(ELDER_PROFICIENCY_LOWER_BOUND + ELDER_PROFICIENCY_LOWER_BOUND * sample, MIN_VALUE, MAX_VALUE, MIN_VALUE);
    }

    /**
     * Calculates one workday of diminishing proficiency growth.
     */
    public static double calculateDailyProficiencyGain(
            double proficiency,
            double growthAtZero,
            double minimumGrowth
    ) {
        double normalizedProficiency = clampFinite(proficiency, MIN_VALUE, MAX_VALUE, MIN_VALUE);
        if (normalizedProficiency >= MAX_VALUE) {
            return 0.0;
        }

        double safeGrowthAtZero = clampFinite(growthAtZero, 0.0, MAX_VALUE, 0.0);
        double safeMinimumGrowth = clampFinite(minimumGrowth, 0.0, MAX_VALUE, 0.0);
        double diminishingGrowth = safeGrowthAtZero * (1.0 - normalizedProficiency / MAX_VALUE);
        double gain = Math.max(safeMinimumGrowth, diminishingGrowth);
        return Math.min(gain, MAX_VALUE - normalizedProficiency);
    }

    private static double unitSample(DoubleSupplier randomDouble) {
        return clampFinite(randomDouble.getAsDouble(), 0.0, 1.0, 0.0);
    }

    private static double clampFinite(double value, double minimum, double maximum, double fallback) {
        if (!Double.isFinite(value)) {
            return fallback;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }
}
