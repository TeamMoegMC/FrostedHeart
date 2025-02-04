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