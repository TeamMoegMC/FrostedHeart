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

package com.teammoeg.frostedheart.data;

import java.util.function.Consumer;

import com.google.gson.JsonObject;

import com.teammoeg.frostedheart.content.health.recipe.NutritionRecipe;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

public class  NutritionRecipeBuilder  implements RecipeBuilder {
	// In CSV
	// Gr=谷物
	// Va=蔬菜
	// Oi=油脂
	// Pt=蛋白
	private float fat,carbohydrate,protein,vegetable;
	protected Item item;
	protected ResourceLocation itemID;
	private final Advancement.Builder advancement = Advancement.Builder.recipeAdvancement();

	public  NutritionRecipeBuilder () {
	}

	public  NutritionRecipeBuilder item(Item item) {
		this.item = item;
		return this;
	}

	public  NutritionRecipeBuilder ItemID(ResourceLocation resourceLocation) {
		this.itemID = resourceLocation;
		return this;
	}

	public  NutritionRecipeBuilder  nutrition(float carbohydrate, float vegetable, float fat, float protein) {
		this.fat = fat;
		this.carbohydrate = carbohydrate;
		this.protein = protein;
		this.vegetable = vegetable;
		return this;
	}

	public  NutritionRecipeBuilder  fat(float fat) {
		this.fat = fat;
		return this;
	}

	public  NutritionRecipeBuilder  carbohydrate(float carbohydrate) {
		this.carbohydrate = carbohydrate;
		return this;
	}

	public  NutritionRecipeBuilder  protein(float protein) {
		this.protein = protein;
		return this;
	}

	public  NutritionRecipeBuilder  vegetable(float vegetable) {
		this.vegetable = vegetable;
		return this;
	}

	@Override
	public  NutritionRecipeBuilder  unlockedBy(String s, CriterionTriggerInstance criterionTriggerInstance) {
		this.advancement.addCriterion(s, criterionTriggerInstance);
		return this;
	}

	@Override
	public  NutritionRecipeBuilder  group(@Nullable String s) {
		return null;
	}

	@Override
	public Item getResult() {
		return item;
	}

	@Override
	public void save(Consumer<FinishedRecipe> pFinishedRecipeConsumer, ResourceLocation pRecipeId) {
		this.advancement.parent(ROOT_RECIPE_ADVANCEMENT).addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(pRecipeId)).rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(pRecipeId)).requirements(RequirementsStrategy.OR);
		pFinishedRecipeConsumer.accept(new Result(pRecipeId,fat,carbohydrate,protein,vegetable,item,itemID, this.advancement, pRecipeId.withPrefix("recipes/diet_value/")));
	}
	public static class Result implements FinishedRecipe {

		private final float fat,carbohydrate,protein,vegetable;
		protected Item item;
		protected ResourceLocation itemID;
		private final ResourceLocation id;
		private final Advancement.Builder advancement;
		private final ResourceLocation advancementId;

		public Result(ResourceLocation id,float fat,float carbohydrate,float protein,float vegetable, Item item,ResourceLocation itemID,Advancement.Builder advancement, ResourceLocation advancementId) {
			this.id = id;
			this.advancement = advancement;
			this.advancementId = advancementId;
			this.fat = fat;
			this.carbohydrate = carbohydrate;
			this.protein = protein;
			this.vegetable = vegetable;
			this.item = item;
			this.itemID = itemID;
		}

		@Override
		public void serializeRecipeData(JsonObject json) {
			JsonObject group = new JsonObject();
			group.addProperty("fat", this.fat);
			group.addProperty("carbohydrate", this.carbohydrate);
			group.addProperty("protein", this.protein);
			group.addProperty("vegetable", this.vegetable);
			json.add("group", group);
			if (this.item!=null)
				json.addProperty("item", ForgeRegistries.ITEMS.getKey(this.item).toString());
			else json.addProperty("item", this.itemID.toString());
		}

		@Override
		public ResourceLocation getId() {
			return id;
		}

		@Override
		public RecipeSerializer<?> getType() {
			return NutritionRecipe.SERIALIZER.get();
		}

		@Nullable
		@Override
		public JsonObject serializeAdvancement() {
			return this.advancement.serializeToJson();
		}

		@Nullable
		@Override
		public ResourceLocation getAdvancementId() {
			return this.advancementId;
		}
	}
}
