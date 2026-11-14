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

package com.teammoeg.frostedheart.content.town.buildings.institute;

import com.teammoeg.frostedheart.content.town.TownMathFunctions;

/**
 * 研究所的纯算术：岗位数、单名研究员的研究人力、装饰加成、知识容量与联想概率。不依赖 Forge。
 * <p>
 * Forge-independent arithmetic of a research institute: researcher slots, one researcher's labor, the decoration
 * multiplier, the knowledge capacity it grants and the chance of association.
 */
public final class ResearchInstituteDailyModel {
    /** Highest resident education level; research labor is kept per level 0 to this. */
    public static final int MAX_EDUCATION = 5;

    private ResearchInstituteDailyModel() {
    }

    /** Resident attributes and research proficiency captured before work. */
    public record ResearcherInput(double health, double mental, double strength, double intelligence, double proficiency) {
    }

    /** Runtime or simulator parameters for one researcher's labor. */
    public record Parameters(
            double healthWeight,
            double mentalWeight,
            double strengthWeight,
            double intelligenceWeight,
            double productivityAtAttributeZero,
            double productivityAtAttributeHundred,
            double maximumProficiency,
            double bonusAtMaximumProficiency,
            double minimumProductivity,
            double maximumProductivity,
            double decorationBonusAtFullRating
    ) {
    }

    /**
     * Researcher slots: one per desk, and no more than the space-rated floor area allows.
     */
    public static int researcherSlots(double spaceRating, int floorArea, double floorBlocksPerResearcher, int desks) {
        if (desks <= 0 || floorArea <= 0 || !(floorBlocksPerResearcher > 0)) return 0;
        double rating = Double.isFinite(spaceRating) ? Math.max(0, Math.min(1, spaceRating)) : 0;
        int bySpace = (int) Math.floor(rating * floorArea / floorBlocksPerResearcher);
        return Math.max(0, Math.min(desks, bySpace));
    }

    /**
     * Labor multiplier of the room's decoration: {@code 1 + bonus × rating}, with the rating clamped to 0–1.
     */
    public static double decorationMultiplier(double decorationRating, double bonusAtFullRating) {
        double rating = Double.isFinite(decorationRating) ? Math.max(0, Math.min(1, decorationRating)) : 0;
        double bonus = Double.isFinite(bonusAtFullRating) ? Math.max(0, bonusAtFullRating) : 0;
        return 1 + bonus * rating;
    }

    /**
     * One researcher's labor in standard researchers: attribute and proficiency productivity, mostly from
     * intelligence, times the decoration multiplier. A standard researcher (all attributes 50, proficiency 0) in an
     * undecorated room gives 1.0.
     */
    public static double researcherLabor(ResearcherInput researcher, double decorationRating, Parameters parameters) {
        if (researcher == null || parameters == null) return 0;
        double productivity = TownMathFunctions.linearResidentProductivity(
                new double[]{finite(researcher.health()), finite(researcher.mental()), finite(researcher.strength()), finite(researcher.intelligence())},
                new double[]{nonNegative(parameters.healthWeight()), nonNegative(parameters.mentalWeight()),
                        nonNegative(parameters.strengthWeight()), nonNegative(parameters.intelligenceWeight())},
                finite(researcher.proficiency()),
                nonNegative(parameters.productivityAtAttributeZero()),
                nonNegative(parameters.productivityAtAttributeHundred()),
                nonNegative(parameters.maximumProficiency()),
                nonNegative(parameters.bonusAtMaximumProficiency()),
                nonNegative(parameters.minimumProductivity()),
                nonNegative(parameters.maximumProductivity()));
        double labor = nonNegative(productivity) * decorationMultiplier(decorationRating, parameters.decorationBonusAtFullRating());
        return Double.isFinite(labor) ? labor : 0;
    }

    /**
     * Knowledge capacity a working institute grants: {@code bookshelves × perBookshelf}, saturating at
     * {@link Integer#MAX_VALUE}.
     */
    public static int capacity(int bookshelves, int perBookshelf) {
        if (bookshelves <= 0 || perBookshelf <= 0) return 0;
        return (int) Math.min(Integer.MAX_VALUE, (long) bookshelves * perBookshelf);
    }

    /**
     * Chance that a day's idle research labor associates: {@code 1 − e^(−rate × labor)}.
     */
    public static double associationChance(double labor, double ratePerStandardResearcherDay) {
        double safeLabor = nonNegative(labor);
        double rate = nonNegative(ratePerStandardResearcherDay);
        double chance = 1 - Math.exp(-rate * safeLabor);
        return Double.isFinite(chance) ? Math.max(0, Math.min(1, chance)) : 0;
    }

    /** Education level as a research labor tier, clamped to 0–5. */
    public static int tier(int educationLevel) {
        return Math.max(0, Math.min(MAX_EDUCATION, educationLevel));
    }

    /** Labor as players see it: whole standard researchers, rounded down. */
    public static long shown(double labor) {
        return Double.isFinite(labor) && labor > 0 ? (long) Math.floor(labor + 1e-9) : 0;
    }

    /**
     * 人·天进度，向下取整到两位小数，与研究札记一致。
     * Worker-days of progress, floored to two decimals as in the research journal, so unfinished work never reads as done.
     */
    public static String workerDays(double value) {
        double safe = Double.isFinite(value) && value > 0 ? value : 0;
        return String.format(java.util.Locale.ROOT, "%.2f", Math.floor(safe * 100 + 1e-6) / 100);
    }

    private static double finite(double value) {
        return Double.isFinite(value) ? value : 0;
    }

    private static double nonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0, value) : 0;
    }
}
