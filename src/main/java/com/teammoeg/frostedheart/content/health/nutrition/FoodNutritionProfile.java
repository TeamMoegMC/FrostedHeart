/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.health.nutrition;

/**
 * Canonical nutrition facts for one food item.
 *
 * <p>Each channel is an independent percentage in {@code 0..100}; the four channels do not
 * need to add up to {@code 100}. Construction replaces non-finite values with zero and clamps
 * finite values to the public percentage range. Generated nutrition recipes use a legacy raw
 * scale and must enter this type through {@link #fromRecipeValues(float, float, float, float)}.</p>
 */
public record FoodNutritionProfile(
        float fat,
        float carbohydrate,
        float protein,
        float vegetable
) {
    /** Converts generated recipe values to public percentages. */
    public static final double RAW_TO_PERCENT = 1.0 / 400.0;

    /** Profile returned when no nutrition source matches an item. */
    public static final FoodNutritionProfile ZERO = new FoodNutritionProfile(0, 0, 0, 0);

    public FoodNutritionProfile {
        fat = bounded(fat);
        carbohydrate = bounded(carbohydrate);
        protein = bounded(protein);
        vegetable = bounded(vegetable);
    }

    /**
     * Converts four generated-recipe values to the canonical percentage scale.
     *
     * <p>For example, a raw value of {@code 24000} becomes {@code 60}. The record constructor
     * performs the final finite check and {@code 0..100} clamp.</p>
     *
     * @param rawFat legacy recipe fat value
     * @param rawCarbohydrate legacy recipe carbohydrate value
     * @param rawProtein legacy recipe protein value
     * @param rawVegetable legacy recipe vegetable value
     * @return an immutable percentage profile
     */
    public static FoodNutritionProfile fromRecipeValues(
            float rawFat,
            float rawCarbohydrate,
            float rawProtein,
            float rawVegetable
    ) {
        return new FoodNutritionProfile(
                (float) (rawFat * RAW_TO_PERCENT),
                (float) (rawCarbohydrate * RAW_TO_PERCENT),
                (float) (rawProtein * RAW_TO_PERCENT),
                (float) (rawVegetable * RAW_TO_PERCENT));
    }

    /**
     * @return the sum of all four independent channels, in the range {@code 0..400}
     */
    public float total() {
        return fat + carbohydrate + protein + vegetable;
    }

    /**
     * @return whether all four channels are zero
     */
    public boolean isZero() {
        return fat == 0 && carbohydrate == 0 && protein == 0 && vegetable == 0;
    }

    private static float bounded(float value) {
        if (!Float.isFinite(value)) return 0.0f;
        return Math.max(0.0f, Math.min(100.0f, value));
    }
}
