/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.health.capability;

public interface Nutrition{
	float getFat();
	float getCarbohydrate();
	float getProtein();
	float getVegetable();
	Nutrition add(Nutrition value);
	Nutrition set(float fat,float carbon,float protein,float vegetable);
	Nutrition scale(float scale);
	float getNutritionValue();
	Nutrition toImmutable();
	MutableNutrition mutableCopy();
	public boolean isZero();
	public static MutableNutrition createMutable() {
		return new MutableNutrition(0,0,0,0);
	}
	public static Nutrition createImmutable(float fat,float carbon,float protein,float vegetable) {
		return new ImmutableNutrition(fat,carbon,protein,vegetable);
	}
	public static final Nutrition ZERO=new ImmutableNutrition();
	Nutrition addScaled(Nutrition nutrition, float scale);
}