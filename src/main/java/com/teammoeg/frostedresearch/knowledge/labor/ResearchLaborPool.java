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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.labor;

import com.teammoeg.frostedresearch.knowledge.definition.ProjectRequirement;

import java.util.ArrayList;
import java.util.List;

/**
 * 一天的研究人力，按居民教育等级 0–5 分级，以标准研究员计。高等级人力可以做低等级的需求；取用时先用满足条件的最低等级。
 * <p>
 * One day of research labor in standard researchers, split by resident education level 0–5. Labor of a higher level
 * may do work that asks for a lower one; taking labor uses the lowest qualifying level first, leaving the higher levels
 * for harder work.
 */
public final class ResearchLaborPool {
    public static final int LEVELS = ProjectRequirement.MAX_EDUCATION + 1;
    private static final double EPSILON = 1e-9;
    private final double[] labor = new double[LEVELS];

    public ResearchLaborPool() {
    }

    public ResearchLaborPool(List<Double> byEducation) {
        for (int level = 0; level < Math.min(LEVELS, byEducation.size()); level++) add(level, byEducation.get(level));
    }

    /**
     * Adds labor at one education level; levels outside 0–5 are clamped and non-finite or negative amounts ignored.
     */
    public void add(int education, double amount) {
        if (!Double.isFinite(amount) || amount <= 0) return;
        labor[clamp(education)] += amount;
    }

    public double get(int education) {
        return education < 0 || education >= LEVELS ? 0 : labor[education];
    }

    /**
     * Labor able to do work that asks for at least this education level.
     */
    public double available(int minimumEducation) {
        double sum = 0;
        for (int level = Math.max(0, minimumEducation); level < LEVELS; level++) sum += labor[level];
        return sum;
    }

    public double total() {
        return available(0);
    }

    /**
     * Takes up to {@code amount} from levels at or above the minimum, lowest level first.
     *
     * @return the labor actually taken
     */
    public double take(int minimumEducation, double amount) {
        if (!Double.isFinite(amount) || amount <= 0) return 0;
        double remaining = amount;
        for (int level = Math.max(0, minimumEducation); level < LEVELS && remaining > EPSILON; level++) {
            double used = Math.min(labor[level], remaining);
            labor[level] -= used;
            if (labor[level] < EPSILON) labor[level] = 0;
            remaining -= used;
        }
        return amount - Math.max(0, remaining);
    }

    public List<Double> byEducation() {
        List<Double> result = new ArrayList<>(LEVELS);
        for (double value : labor) result.add(value);
        return List.copyOf(result);
    }

    public ResearchLaborPool copy() {
        return new ResearchLaborPool(byEducation());
    }

    private static int clamp(int education) {
        return Math.max(0, Math.min(LEVELS - 1, education));
    }
}
