/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.town.resident;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResidentNutritionSupportModelTest {
    private static final double EPSILON = 1.0e-9;

    @Test
    void healthyLineAndSurplusBothMeanFullSatisfaction() {
        assertEquals(ResidentNutritionSupportModel.Satisfaction.FULL,
                ResidentNutritionSupportModel.satisfaction(
                        new ResidentNutrition(70, 70, 70, 70), 70));
        assertEquals(ResidentNutritionSupportModel.Satisfaction.FULL,
                ResidentNutritionSupportModel.satisfaction(
                        new ResidentNutrition(100, 100, 100, 100), 70));
    }

    @Test
    void matrixRowsNormalizeAndStrengthUsesProteinHeavyDefaults() {
        ResidentNutritionSupportModel.Weights weights = new ResidentNutritionSupportModel.Weights(
                new ResidentNutritionSupportModel.WeightRow(5, 1, 3, 1),
                new ResidentNutritionSupportModel.WeightRow(-2, -1, -3, -4),
                ResidentNutritionSupportModel.DEFAULT_STRENGTH,
                ResidentNutritionSupportModel.DEFAULT_INTELLIGENCE);
        ResidentNutritionSupportModel.Satisfaction onlyProtein =
                new ResidentNutritionSupportModel.Satisfaction(1, 0, 0, 0);
        ResidentNutritionSupportModel.Supports supports =
                ResidentNutritionSupportModel.supports(onlyProtein, weights);

        assertEquals(0.5, supports.health(), EPSILON);
        assertEquals(0.1, supports.mental(), EPSILON);
        assertEquals(0.75, supports.strength(), EPSILON);
        assertEquals(0.05, supports.intelligence(), EPSILON);
        assertEquals(0.25, ResidentNutritionSupportModel.DEFAULT_STRENGTH.apply(
                new ResidentNutritionSupportModel.Satisfaction(0, 1, 1, 1)), EPSILON);
        assertEquals(0.4, ResidentNutritionSupportModel.DEFAULT_STRENGTH.apply(
                new ResidentNutritionSupportModel.Satisfaction(0.2, 1, 1, 1)), EPSILON);
    }

    @Test
    void recoveryFloorsAndLimitingNutrientsRemainCurrentOnly() {
        assertEquals(0.25,
                ResidentNutritionSupportModel.healthRecoveryMultiplier(0), EPSILON);
        assertEquals(0.35,
                ResidentNutritionSupportModel.mentalRecoveryMultiplier(0), EPSILON);
        assertEquals(1.0,
                ResidentNutritionSupportModel.healthRecoveryMultiplier(1), EPSILON);

        var limits = ResidentNutritionSupportModel.limitingNutrients(
                new ResidentNutritionSupportModel.Satisfaction(0, 1, 0.5, 1),
                ResidentNutritionSupportModel.DEFAULT_STRENGTH, 2);
        assertEquals(ResidentNutritionSupportModel.Nutrient.PROTEIN, limits.get(0));
        assertEquals(ResidentNutritionSupportModel.Nutrient.VEGETABLE, limits.get(1));
    }

    @Test
    void activityInterpolationAndChannelwiseMaximumAreBounded() {
        assertEquals(new ResidentActivity(1.0, 0.7),
                new ResidentActivity(1.0, 0.2).max(new ResidentActivity(0.4, 0.7)));
        ResidentAttributeChange idleChild = settle(0, 0, 0.7, 1,
                1, 100, 0.2, 0.4, 0.7, 0);
        ResidentAttributeChange activeChild = settle(0, 1, 0.7, 1,
                1, 100, 0.2, 0.4, 0.7, 0);

        assertEquals(0.7, idleChild.effectiveActivity(), EPSILON);
        assertEquals(0.7, idleChild.growth(), EPSILON);
        assertEquals(1.0, activeChild.effectiveActivity(), EPSILON);
        assertEquals(1.0, activeChild.growth(), EPSILON);
    }

    @Test
    void deficiencyUsesThresholdAndPowerOnePointFive() {
        ResidentAttributeChange atThreshold = settle(50, 0, 0, 0.4,
                0, 100, 0.2, 0.4, 0.7, 0);
        ResidentAttributeChange halfDeficient = settle(50, 0, 0, 0.2,
                0, 100, 0.2, 0.4, 0.7, 0);
        ResidentAttributeChange zeroSupport = settle(50, 0, 0, 0,
                0, 100, 0.2, 0.4, 0.7, 0);

        assertEquals(0.0, atThreshold.nutritionDecay(), EPSILON);
        assertEquals(0.7 * Math.pow(0.5, 1.5) * 0.5,
                halfDeficient.nutritionDecay(), EPSILON);
        assertEquals(0.35, zeroSupport.nutritionDecay(), EPSILON);
    }

    @Test
    void adultFifteenDayDeficiencyExamplesMatchBalanceTargets() {
        double proteinMissingStrength = repeat(50, 15, 1, 0.3, 0.25,
                0.05, 100, 0.2, 0.4, 0.7, 0);
        double zeroStrength = repeat(50, 15, 1, 0.3, 0,
                0.05, 100, 0.2, 0.4, 0.7, 0);
        double zeroIntelligence = repeat(50, 15, 0.25, 0.3, 0,
                0.05, 100, 0.4, 0.3, 0.17, 0);

        assertEquals(48.96, proteinMissingStrength, 0.02);
        assertEquals(45.07, zeroStrength, 0.02);
        assertEquals(48.81, zeroIntelligence, 0.02);
    }

    @Test
    void infancyAndChildhoodReachExpectedAdultAttributes() {
        double strength = repeat(20, 30, 1, 1, 1,
                1.8, 40, 0.2, 0.4, 0.7, 0);
        strength = repeat(strength, 30, 1, 0.7, 1,
                3.9, 80, 0.2, 0.4, 0.7, 0);
        double intelligence = repeat(30, 30, 0.25, 1, 1,
                1.6, 40, 0.4, 0.3, 0.17, 0);
        intelligence = repeat(intelligence, 30, 0.25, 0.7, 1,
                4.2, 85, 0.4, 0.3, 0.17, 0);

        assertEquals(69.95, strength, 0.02);
        assertEquals(70.14, intelligence, 0.02);
    }

    @Test
    void elderBalanceAllowsMaintenanceAndHasNoStrengthFloor() {
        double idleStrength = repeat(35, 120, 0, 0.1, 1,
                0.06, 100, 0.2, 0.4, 0.7, 0.0048);
        double activeStrength = repeat(35, 120, 1, 0.1, 1,
                0.06, 100, 0.2, 0.4, 0.7, 0.0048);
        double idleIntelligence = repeat(65, 120, 0, 0.1, 1,
                0.05, 100, 0.4, 0.3, 0.17, 0.002);
        double malnourishedLowStrength = repeat(1, 20, 0, 0.1, 0,
                0.06, 100, 0.2, 0.4, 0.7, 0.0048);

        assertEquals(34.89, idleStrength, 0.02);
        assertEquals(38.96, activeStrength, 0.02);
        assertEquals(64.97, idleIntelligence, 0.02);
        assertTrue(malnourishedLowStrength < 1.0);
    }

    @Test
    void adultFullActivityApproachesOneHundredExponentially() {
        assertEquals(70.0, repeat(50, 1022, 1, 0.3, 1,
                0.05, 100, 0.2, 0.4, 0.7, 0), 0.02);
        assertEquals(90.0, repeat(80, 1386, 1, 0.3, 1,
                0.05, 100, 0.2, 0.4, 0.7, 0), 0.02);
        double nearMaximum = repeat(95, 3219, 1, 0.3, 1,
                0.05, 100, 0.2, 0.4, 0.7, 0);
        assertEquals(99.0, nearMaximum, 0.02);
        assertTrue(nearMaximum < 100.0);
    }

    private static ResidentAttributeChange settle(
            double current,
            double activity,
            double baseActivity,
            double support,
            double growthRate,
            double cap,
            double zeroEfficiency,
            double threshold,
            double decayAtZero,
            double ageDecay
    ) {
        return ResidentAttributeModel.settleDailyAttribute(
                current, activity, baseActivity, support, growthRate, cap,
                zeroEfficiency, threshold, 1.5, decayAtZero, ageDecay);
    }

    private static double repeat(
            double current,
            int days,
            double activity,
            double baseActivity,
            double support,
            double growthRate,
            double cap,
            double zeroEfficiency,
            double threshold,
            double decayAtZero,
            double ageDecay
    ) {
        double value = current;
        for (int day = 0; day < days; day++) {
            value = settle(value, activity, baseActivity, support, growthRate, cap,
                    zeroEfficiency, threshold, decayAtZero, ageDecay).nextValue();
        }
        return value;
    }
}
