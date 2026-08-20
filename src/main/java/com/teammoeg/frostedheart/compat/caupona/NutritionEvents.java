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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 */

package com.teammoeg.frostedheart.compat.caupona;

import com.teammoeg.caupona.CPConfig;
import com.teammoeg.caupona.api.CauponaHooks;
import com.teammoeg.caupona.data.recipes.FluidFoodValueRecipe;
import com.teammoeg.caupona.data.recipes.FoodValueRecipe;
import com.teammoeg.caupona.util.FloatemStack;
import com.teammoeg.caupona.util.IFoodInfo;
import com.teammoeg.caupona.util.StewInfo;
import com.teammoeg.frostedheart.content.health.event.GatherFoodNutritionEvent;
import com.teammoeg.frostedheart.content.health.nutrition.FoodNutritionProfile;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Caupona adapter for the internal dynamic-food nutrition event.
 *
 * <p>The adapter resolves each real ingredient or Caupona representative stack through the core
 * resolver, combines those already-converted percentage profiles using Caupona healing weights,
 * and replaces the event profile once. No legacy recipe-scale conversion occurs here.</p>
 */
public class NutritionEvents {
    /**
     * Builds the percentage profile of a Caupona soup from its ingredient and fluid contents.
     *
     * <p>Ingredient contribution is weighted by count and healing. The accumulated profile is
     * scaled by Caupona's beneficial-food modifier divided by the soup's total healing, matching
     * the existing soup formula.</p>
     *
     * @param event dynamic profile request for the actual soup stack
     */
    public static void gatherNutritionFromSoup(GatherFoodNutritionEvent event) {
        Optional<IFoodInfo> optionalInfo = CauponaHooks.getInfo(event.getStack());
        if (optionalInfo.isEmpty()) return;

        IFoodInfo info = optionalInfo.get();
        int totalHealing = info.getHealing();
        if (totalHealing <= 0) return;

        ProfileAccumulator accumulator = new ProfileAccumulator();
        for (FloatemStack ingredient : info.getStacks()) {
            FoodValueRecipe recipe = FoodValueRecipe.recipes == null
                    ? null : FoodValueRecipe.recipes.get(ingredient.getItem());
            ItemStack stack;
            int healing;
            if (recipe != null && recipe.getRepersent() != null) {
                stack = recipe.getRepersent();
                healing = recipe.heal;
            } else {
                stack = ingredient.getStack();
                FoodProperties food = stack.getFoodProperties(null);
                healing = food == null ? 0 : food.getNutrition();
            }
            accumulator.add(
                    event.resolveIngredient(stack), ingredient.getCount() * healing);
        }

        if (info instanceof StewInfo stew) {
            FluidFoodValueRecipe recipe = FluidFoodValueRecipe.recipes == null
                    ? null : FluidFoodValueRecipe.recipes.get(stew.base);
            if (recipe != null && recipe.getRepersent() != null) {
                accumulator.add(
                        event.resolveIngredient(recipe.getRepersent()), recipe.heal);
            }
        }

        double beneficialModifier = CPConfig.SERVER.benefitialMod.get();
        event.setProfile(accumulator.toProfile(beneficialModifier / totalHealing));
    }

    /** Local mutable arithmetic helper; accumulated values remain on the percentage scale. */
    static final class ProfileAccumulator {
        private double fat;
        private double carbohydrate;
        private double protein;
        private double vegetable;

        void add(FoodNutritionProfile profile, double weight) {
            if (profile == null || !Double.isFinite(weight) || weight <= 0.0) return;
            fat += profile.fat() * weight;
            carbohydrate += profile.carbohydrate() * weight;
            protein += profile.protein() * weight;
            vegetable += profile.vegetable() * weight;
        }

        FoodNutritionProfile toProfile(double scale) {
            double safeScale = Double.isFinite(scale) ? Math.max(0.0, scale) : 0.0;
            return new FoodNutritionProfile(
                    (float) (fat * safeScale),
                    (float) (carbohydrate * safeScale),
                    (float) (protein * safeScale),
                    (float) (vegetable * safeScale));
        }
    }
}
