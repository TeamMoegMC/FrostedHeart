/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.health.nutrition;

/**
 * Immutable persistent nutrition state for a player.
 *
 * <p>Every channel is a percentage in {@code 0..100}. This is deliberately separate from
 * {@link FoodNutritionProfile}: a profile describes one food, while this type describes the
 * player's current reserves and owns the state-transition formulas. Construction replaces
 * non-finite values with zero and clamps all writes to the valid range.</p>
 */
public record PlayerNutritionState(
        float fat,
        float carbohydrate,
        float protein,
        float vegetable
) {
    public static final float MAXIMUM = 100.0f;

    /** Initial state used when a save contains no value for a channel. */
    public static final PlayerNutritionState DEFAULT = uniform(70.0f);

    public PlayerNutritionState {
        fat = bounded(fat);
        carbohydrate = bounded(carbohydrate);
        protein = bounded(protein);
        vegetable = bounded(vegetable);
    }

    /**
     * Creates a state with the same value in all four channels.
     *
     * @param value value before constructor clamping
     * @return a uniformly initialized state
     */
    public static PlayerNutritionState uniform(float value) {
        return new PlayerNutritionState(value, value, value, value);
    }

    public PlayerNutritionState withFat(float value) {
        return new PlayerNutritionState(value, carbohydrate, protein, vegetable);
    }

    public PlayerNutritionState withCarbohydrate(float value) {
        return new PlayerNutritionState(fat, value, protein, vegetable);
    }

    public PlayerNutritionState withProtein(float value) {
        return new PlayerNutritionState(fat, carbohydrate, value, vegetable);
    }

    public PlayerNutritionState withVegetable(float value) {
        return new PlayerNutritionState(fat, carbohydrate, protein, value);
    }

    public PlayerNutritionState addFat(float amount) {
        return withFat(sum(fat, amount));
    }

    public PlayerNutritionState addCarbohydrate(float amount) {
        return withCarbohydrate(sum(carbohydrate, amount));
    }

    public PlayerNutritionState addProtein(float amount) {
        return withProtein(sum(protein, amount));
    }

    public PlayerNutritionState addVegetable(float amount) {
        return withVegetable(sum(vegetable, amount));
    }

    /**
     * Applies nutrition from eating one item, limited by hunger that the item can actually fill.
     *
     * <p>For each channel, the gain is:</p>
     * <pre>{@code
     * effectiveHunger * profilePercent / 100 * gainRate
     * }</pre>
     * <p>{@code effectiveHunger} is the lesser of the item's vanilla hunger and the player's
     * missing hunger before eating. Saturation and food overflow do not produce nutrition.</p>
     *
     * @param profile canonical nutrition profile for the consumed stack; {@code null} is zero
     * @param itemHunger vanilla hunger restored by the item
     * @param foodLevelBeforeEating player food level before vanilla applies the item
     * @param gainRate state points gained per effective hunger at a {@code 100%} profile
     * @return the clamped state after eating
     */
    public PlayerNutritionState afterEating(
            FoodNutritionProfile profile,
            int itemHunger,
            int foodLevelBeforeEating,
            double gainRate
    ) {
        FoodNutritionProfile safeProfile = profile == null ? FoodNutritionProfile.ZERO : profile;
        int effectiveHunger = Math.min(Math.max(0, itemHunger),
                Math.max(0, 20 - foodLevelBeforeEating));
        double safeRate = finiteNonNegative(gainRate);
        double factor = effectiveHunger / 100.0 * safeRate;
        return new PlayerNutritionState(
                sum(fat, safeProfile.fat() * factor),
                sum(carbohydrate, safeProfile.carbohydrate() * factor),
                sum(protein, safeProfile.protein() * factor),
                sum(vegetable, safeProfile.vegetable() * factor));
    }

    /**
     * Applies deterministic nutrition consumption caused by lost vanilla hunger.
     *
     * <p>Every channel loses {@code max(0, hungerLost) * consumptionRate}; current channel
     * proportions do not affect the loss.</p>
     *
     * @param hungerLost whole vanilla hunger points lost since the previous settlement
     * @param consumptionRate state points lost per hunger point
     * @return the clamped state after consumption
     */
    public PlayerNutritionState afterHungerLoss(int hungerLost, double consumptionRate) {
        double loss = Math.max(0, hungerLost) * finiteNonNegative(consumptionRate);
        return new PlayerNutritionState(
                sum(fat, -loss),
                sum(carbohydrate, -loss),
                sum(protein, -loss),
                sum(vegetable, -loss));
    }

    private static float sum(float value, double delta) {
        if (!Double.isFinite(delta)) return value;
        return bounded(value + delta);
    }

    private static double finiteNonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }

    private static float bounded(double value) {
        if (!Double.isFinite(value)) return 0.0f;
        return (float) Math.max(0.0, Math.min(MAXIMUM, value));
    }
}
