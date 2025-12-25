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