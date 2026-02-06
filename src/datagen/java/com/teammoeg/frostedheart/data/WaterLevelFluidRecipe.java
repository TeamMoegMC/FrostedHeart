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

package com.teammoeg.frostedheart.data;

import com.google.gson.JsonObject;
import com.teammoeg.chorda.util.CRegistryHelper;
import com.teammoeg.frostedheart.content.water.recipe.WaterLevelAndEffectRecipe;

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
		json.addProperty("fluid", CRegistryHelper.getRegistryName(f).toString());
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
