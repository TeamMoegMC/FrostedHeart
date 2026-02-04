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

import com.teammoeg.chorda.math.CMath;

import lombok.Getter;

public class MutableNutrition implements Nutrition{
	@Getter
	float fat, carbohydrate, protein, vegetable;
	
    public MutableNutrition(float fat, float carbohydrate, float protein, float vegetable) {
		super();
		this.fat = fat;
		this.carbohydrate = carbohydrate;
		this.protein = protein;
		this.vegetable = vegetable;
	}
    public Nutrition set(float fat, float carbohydrate, float protein, float vegetable) {
		this.fat = fat;
		this.carbohydrate = carbohydrate;
		this.protein = protein;
		this.vegetable = vegetable;
		return this;
	}
    public void set(Nutrition nutrition) {
		set(nutrition.getFat(),nutrition.getCarbohydrate(),nutrition.getProtein(),nutrition.getVegetable());
	}
	public Nutrition scale(float scale){
        fat*=scale;
        carbohydrate*=scale;
        protein*=scale;
        vegetable*=scale;
        return this;
    }
    public Nutrition add(Nutrition nutrition){
        fat+=nutrition.getFat();
        carbohydrate+=nutrition.getCarbohydrate();
        protein+=nutrition.getProtein();
        vegetable+=nutrition.getVegetable();
        return this;
    }
    @Override
    public Nutrition addScaled(Nutrition nutrition,float scale){
        fat+=nutrition.getFat()*scale;
        carbohydrate+=nutrition.getCarbohydrate()*scale;
        protein+=nutrition.getProtein()*scale;
        vegetable+=nutrition.getVegetable()*scale;
        return this;
    }
    public Nutrition ensureValid() {
    	fat=CMath.toValidClampedValue(fat, 0, 100000);
    	carbohydrate=CMath.toValidClampedValue(carbohydrate, 0, 100000);
    	protein=CMath.toValidClampedValue(protein, 0, 100000);
    	vegetable=CMath.toValidClampedValue(vegetable, 0, 100000);
    	return this;
    }
    public float getNutritionValue(){
        return fat + carbohydrate + protein + vegetable;
    }
    public boolean isZero(){
        return fat == 0 && carbohydrate == 0 && protein == 0 && vegetable == 0;
    }
    public ImmutableNutrition toImmutable(){
        return new ImmutableNutrition(fat,carbohydrate,protein,vegetable);
    }
	@Override
	public MutableNutrition mutableCopy() {
		return new MutableNutrition(fat,carbohydrate,protein,vegetable);
	}
}