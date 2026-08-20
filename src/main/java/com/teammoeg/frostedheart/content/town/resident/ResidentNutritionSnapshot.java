/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.resident;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Last resident nutrition and attribute settlement persisted for explanatory UI.
 *
 * <p>The snapshot is deliberately flat: it records only the current meal's support, the day's
 * completed activity, actual recovery, and the additive components of strength/intelligence
 * change. It is not an input to future settlement.</p>
 */
public record ResidentNutritionSnapshot(
        boolean hasData,
        ResidentNutritionSupportModel.Satisfaction satisfaction,
        ResidentNutritionSupportModel.Supports supports,
        ResidentActivity activity,
        Recovery recovery,
        ResidentAttributeChange strength,
        ResidentAttributeChange intelligence
) {
    private static final ResidentNutritionSupportModel.Supports FULL_SUPPORT =
            new ResidentNutritionSupportModel.Supports(1.0, 1.0, 1.0, 1.0);
    public static final ResidentNutritionSnapshot EMPTY = new ResidentNutritionSnapshot(
            false,
            ResidentNutritionSupportModel.Satisfaction.FULL,
            FULL_SUPPORT,
            ResidentActivity.NONE,
            Recovery.EMPTY,
            ResidentAttributeChange.EMPTY,
            ResidentAttributeChange.EMPTY);

    public static final Codec<ResidentNutritionSnapshot> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.optionalFieldOf("hasData", false)
                            .forGetter(ResidentNutritionSnapshot::hasData),
                    ResidentNutritionSupportModel.Satisfaction.CODEC
                            .optionalFieldOf("satisfaction",
                                    ResidentNutritionSupportModel.Satisfaction.FULL)
                            .forGetter(ResidentNutritionSnapshot::satisfaction),
                    ResidentNutritionSupportModel.Supports.CODEC
                            .optionalFieldOf("supports", FULL_SUPPORT)
                            .forGetter(ResidentNutritionSnapshot::supports),
                    ResidentActivity.CODEC.optionalFieldOf("activity", ResidentActivity.NONE)
                            .forGetter(ResidentNutritionSnapshot::activity),
                    Recovery.CODEC.optionalFieldOf("recovery", Recovery.EMPTY)
                            .forGetter(ResidentNutritionSnapshot::recovery),
                    ResidentAttributeChange.CODEC
                            .optionalFieldOf("strength", ResidentAttributeChange.EMPTY)
                            .forGetter(ResidentNutritionSnapshot::strength),
                    ResidentAttributeChange.CODEC
                            .optionalFieldOf("intelligence", ResidentAttributeChange.EMPTY)
                            .forGetter(ResidentNutritionSnapshot::intelligence)
            ).apply(instance, ResidentNutritionSnapshot::new));

    public ResidentNutritionSnapshot {
        satisfaction = satisfaction == null
                ? ResidentNutritionSupportModel.Satisfaction.FULL : satisfaction;
        supports = supports == null ? FULL_SUPPORT : supports;
        activity = activity == null ? ResidentActivity.NONE : activity;
        recovery = recovery == null ? Recovery.EMPTY : recovery;
        strength = strength == null ? ResidentAttributeChange.EMPTY : strength;
        intelligence = intelligence == null ? ResidentAttributeChange.EMPTY : intelligence;
    }

    /** Starts a new daily explanation from the post-meal nutrition state. */
    public static ResidentNutritionSnapshot afterMeal(
            ResidentNutrition nutrition,
            double healthyReserve,
            ResidentNutritionSupportModel.Weights weights
    ) {
        ResidentNutritionSupportModel.Satisfaction satisfaction =
                ResidentNutritionSupportModel.satisfaction(nutrition, healthyReserve);
        return new ResidentNutritionSnapshot(
                true,
                satisfaction,
                ResidentNutritionSupportModel.supports(satisfaction, weights),
                ResidentActivity.NONE,
                Recovery.EMPTY,
                ResidentAttributeChange.EMPTY,
                ResidentAttributeChange.EMPTY);
    }

    /** Adds the health and mental result produced by housing settlement. */
    public ResidentNutritionSnapshot withRecovery(
            double healthRecovery,
            double healthNet,
            double mentalRecovery,
            double mentalNet
    ) {
        return new ResidentNutritionSnapshot(
                true, satisfaction, supports, activity,
                new Recovery(healthRecovery, healthNet, mentalRecovery, mentalNet),
                strength, intelligence);
    }

    /** Completes the snapshot with the day's activity and both attribute transitions. */
    public ResidentNutritionSnapshot withAttributes(
            ResidentActivity activity,
            ResidentAttributeChange strength,
            ResidentAttributeChange intelligence
    ) {
        return new ResidentNutritionSnapshot(
                true, satisfaction, supports, activity, recovery, strength, intelligence);
    }

    /** Actual health and mental recovery/net change from the same daily settlement. */
    public record Recovery(
            double healthRecovery,
            double healthNet,
            double mentalRecovery,
            double mentalNet
    ) {
        public static final Recovery EMPTY = new Recovery(0.0, 0.0, 0.0, 0.0);
        public static final Codec<Recovery> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.DOUBLE.optionalFieldOf("healthRecovery", 0.0)
                                .forGetter(Recovery::healthRecovery),
                        Codec.DOUBLE.optionalFieldOf("healthNet", 0.0)
                                .forGetter(Recovery::healthNet),
                        Codec.DOUBLE.optionalFieldOf("mentalRecovery", 0.0)
                                .forGetter(Recovery::mentalRecovery),
                        Codec.DOUBLE.optionalFieldOf("mentalNet", 0.0)
                                .forGetter(Recovery::mentalNet)
                ).apply(instance, Recovery::new));

        public Recovery {
            healthRecovery = nonNegative(healthRecovery);
            healthNet = finite(healthNet);
            mentalRecovery = nonNegative(mentalRecovery);
            mentalNet = finite(mentalNet);
        }
    }

    private static double nonNegative(double value) {
        return Math.max(0.0, finite(value));
    }

    private static double finite(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }
}
