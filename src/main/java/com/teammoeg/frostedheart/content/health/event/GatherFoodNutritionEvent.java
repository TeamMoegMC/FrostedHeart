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

package com.teammoeg.frostedheart.content.health.event;

import com.teammoeg.frostedheart.content.health.nutrition.FoodNutritionProfile;
import com.teammoeg.frostedheart.content.health.nutrition.FoodNutritionResolver;

import lombok.Getter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Event;

/**
 * Internal, strongly typed extension point for dynamic food nutrition.
 *
 * <p>The event begins with the resolver's static percentage profile. A compatibility listener
 * may replace that profile as a whole, but it must keep using {@link FoodNutritionProfile}'s
 * {@code 0..100} channel semantics. This event is currently an internal bridge for optional
 * integrations such as Caupona, not a compatibility promise for external consumers.</p>
 */
public class GatherFoodNutritionEvent extends Event {
	@Getter
	private final FoodNutritionProfile originalValue;
	@Getter
	private final Level level;
	@Getter
	private final ItemStack stack;
	@Getter
	private FoodNutritionProfile profile;
	public GatherFoodNutritionEvent(FoodNutritionProfile originalValue, Level level, ItemStack stack) {
		super();
		this.originalValue = originalValue == null ? FoodNutritionProfile.ZERO : originalValue;
		this.profile = this.originalValue;
		this.level = level;
		this.stack = stack;
	}

	/**
	 * Replaces the resolved percentage profile for this stack.
	 *
	 * @param profile complete replacement profile; {@code null} becomes the zero profile
	 */
	public void setProfile(FoodNutritionProfile profile) {
		this.profile = profile == null ? FoodNutritionProfile.ZERO : profile;
	}

	/**
	 * Resolves an ingredient through the same authoritative pipeline as the containing food.
	 *
	 * @param stack actual ingredient or representative stack
	 * @return the ingredient's non-null percentage profile
	 */
	public FoodNutritionProfile resolveIngredient(ItemStack stack) {
		return FoodNutritionResolver.resolve(level, stack);
	}
}
