package com.teammoeg.frostedheart.data;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.content.water.recipe.WaterLevelAndEffectRecipe;
import com.teammoeg.frostedheart.util.RegistryUtils;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.resources.ResourceLocation;

public class WaterLevelFluidRecipe implements FinishedRecipe {
	ResourceLocation rid;
	Ingredient igd;
	int wl,ws;
	Fluid f;
	public WaterLevelFluidRecipe() {
	}

	public WaterLevelFluidRecipe(ResourceLocation rid, Ingredient igd,Fluid f, int wl, int ws) {
		super();
		this.rid = rid;
		this.igd = igd;
		this.wl = wl;
		this.ws = ws;
		this.f=f;
	}

	@Override
	public void serializeRecipeData(JsonObject json) {
		json.add("ingredient",igd.toJson());
		json.addProperty("waterLevel", wl);
		json.addProperty("waterSaturationLevel",ws);
		json.addProperty("fluid", RegistryUtils.getRegistryName(f).toString());
	}

	@Override
	public ResourceLocation getId() {
		return rid;
	}

	@Override
	public RecipeSerializer<?> getType() {
		return WaterLevelAndEffectRecipe.SERIALIZER.get();
	}

	@Override
	public JsonObject serializeAdvancement() {
		return null;
	}

	@Override
	public ResourceLocation getAdvancementId() {
		return null;
	}

}
