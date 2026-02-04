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

public record ImmutableNutrition(float fat , float carbohydrate, float protein , float vegetable) implements Nutrition{
    public ImmutableNutrition(){
        this(0);
    }
    public ImmutableNutrition(float v){
        this(v,v,v,v);
    }
    public ImmutableNutrition scale(float scale){
        return new ImmutableNutrition(fat*scale,carbohydrate*scale,protein*scale,vegetable*scale);
    }
    public ImmutableNutrition add(Nutrition nutrition){
        return new ImmutableNutrition(fat+nutrition.getFat(),carbohydrate+nutrition.getCarbohydrate(),protein+nutrition.getProtein(),vegetable+nutrition.getVegetable());
    }
    public float getNutritionValue(){
        return fat + carbohydrate + protein + vegetable;
    }
    public boolean isZero(){
        return fat == 0 && carbohydrate == 0 && protein == 0 && vegetable == 0;
    }
    public MutableNutrition mutableCopy() {
    	return new MutableNutrition(fat,carbohydrate,protein,vegetable);
    }
	@Override
	public float getFat() {
		return fat;
	}
	@Override
	public float getCarbohydrate() {
		return carbohydrate;
	}
	@Override
	public float getProtein() {
		return protein;
	}
	@Override
	public float getVegetable() {
		return vegetable;
	}
	@Override
	public Nutrition set(float fat, float carbon, float protein, float vegetable) {
		return new ImmutableNutrition(fat,carbon,protein,vegetable);
	}
	@Override
	public Nutrition toImmutable() {
		return this;
	}
	@Override
	public Nutrition addScaled(Nutrition nutrition, float scale) {
		return new ImmutableNutrition(fat+nutrition.getFat()*scale,carbohydrate+nutrition.getCarbohydrate()*scale,protein+nutrition.getProtein()*scale,vegetable+nutrition.getVegetable()*scale);
	}

}