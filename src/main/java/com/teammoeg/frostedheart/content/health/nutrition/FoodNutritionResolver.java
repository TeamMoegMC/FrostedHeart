/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.health.nutrition;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.health.event.GatherFoodNutritionEvent;
import com.teammoeg.frostedheart.content.health.recipe.NutritionRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authoritative lookup for static and dynamic food nutrition.
 *
 * <p>All player, resident, tooltip, simulation, and compatibility code must resolve food facts
 * through this class. Static recipes are converted from their raw scale to percentages here.
 * Dynamic integrations may then replace the result through {@link GatherFoodNutritionEvent}.
 * The resolver never returns {@code null}.</p>
 */
public final class FoodNutritionResolver {
    private static final Set<String> REPORTED_DUPLICATES = ConcurrentHashMap.newKeySet();

    private FoodNutritionResolver() {
    }

    /**
     * Resolves the canonical percentage profile for an actual item stack.
     *
     * <p>If multiple static recipes match, the recipe with the lexicographically smallest ID is
     * selected and the duplicate set is logged once. The selected static result, or
     * {@link FoodNutritionProfile#ZERO} when no recipe matches, becomes the original value of a
     * {@link GatherFoodNutritionEvent}. Event listeners operate on percentage profiles, so this
     * method performs no raw-scale conversion after the event.</p>
     *
     * @param level level whose recipe manager and event context should be used
     * @param stack actual stack to inspect, including dynamic contents such as soup ingredients
     * @return a non-null, clamped {@code 0..100} profile
     */
    public static FoodNutritionProfile resolve(Level level, ItemStack stack) {
        if (level == null || stack == null || stack.isEmpty()) {
            return FoodNutritionProfile.ZERO;
        }
        List<NutritionRecipe> matches = level.getRecipeManager()
                .getAllRecipesFor(NutritionRecipe.TYPE.get()).stream()
                .filter(recipe -> recipe.conform(stack))
                .sorted(Comparator.comparing(recipe -> recipe.getId().toString()))
                .toList();
        if (matches.size() > 1) {
            String key = matches.stream().map(recipe -> recipe.getId().toString())
                    .reduce((left, right) -> left + "," + right).orElse("");
            if (REPORTED_DUPLICATES.add(key)) {
                FHMain.LOGGER.error("Multiple nutrition recipes match {}: {}. Using {}.",
                        stack, key, matches.get(0).getId());
            }
        }
        FoodNutritionProfile base = matches.isEmpty()
                ? FoodNutritionProfile.ZERO
                : FoodNutritionProfile.fromRecipeValues(
                        matches.get(0).rawFat(),
                        matches.get(0).rawCarbohydrate(),
                        matches.get(0).rawProtein(),
                        matches.get(0).rawVegetable());
        GatherFoodNutritionEvent event = new GatherFoodNutritionEvent(base, level, stack);
        MinecraftForge.EVENT_BUS.post(event);
        return event.getProfile();
    }
}
