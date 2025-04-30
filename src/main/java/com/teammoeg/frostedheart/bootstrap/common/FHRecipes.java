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

package com.teammoeg.frostedheart.bootstrap.common;

import com.mojang.serialization.Codec;
import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.recipe.CodecRecipeSerializer;
import com.teammoeg.chorda.recipe.DataContainerRecipe;
import com.teammoeg.chorda.recipe.ToolActionIngredient;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorRecipe;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorSteamRecipe;
import com.teammoeg.frostedheart.content.climate.data.*;
import com.teammoeg.frostedheart.content.climate.recipe.CampfireDefrostRecipe;
import com.teammoeg.frostedheart.content.climate.recipe.DismantleInnerRecipe;
import com.teammoeg.frostedheart.content.climate.recipe.SmokingDefrostRecipe;
import com.teammoeg.frostedheart.content.health.recipe.NutritionRecipe;
import com.teammoeg.frostedheart.content.incubator.IncubateRecipe;
import com.teammoeg.frostedheart.content.steamenergy.charger.ChargerRecipe;
import com.teammoeg.frostedheart.content.steamenergy.sauna.SaunaRecipe;
import com.teammoeg.frostedheart.content.town.resource.ItemResourceAmountRecipe;
import com.teammoeg.frostedheart.content.trade.policy.TradePolicy;
import com.teammoeg.frostedheart.content.utility.handstoves.FuelingRecipe;
import com.teammoeg.frostedheart.content.utility.recipe.ModifyDamageRecipe;
import com.teammoeg.frostedheart.content.utility.recipe.ShapelessCopyDataRecipe;
import com.teammoeg.frostedheart.content.water.recipe.WaterLevelAndEffectRecipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class FHRecipes {
	public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(
		ForgeRegistries.RECIPE_SERIALIZERS, FHMain.MODID);
	public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(
		ForgeRegistries.RECIPE_TYPES, FHMain.MODID);

	public static final DeferredRegister<RecipeSerializer<?>> CRECIPE_SERIALIZERS = DeferredRegister.create(
		ForgeRegistries.RECIPE_SERIALIZERS, Chorda.MODID);
	public static final DeferredRegister<RecipeType<?>> CRECIPE_TYPES = DeferredRegister.create(
		ForgeRegistries.RECIPE_TYPES, Chorda.MODID);
	static {
		 
		GeneratorRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("generator", GeneratorRecipe.Serializer::new);
		GeneratorSteamRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("steam_generator", GeneratorSteamRecipe.Serializer::new);
		// InstallInnerRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("recipe_inner",
		// InstallInnerRecipe.Serializer::new);
		ChargerRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("charger", ChargerRecipe.Serializer::new);
		DismantleInnerRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("recipe_inner_dismantle", DismantleInnerRecipe.Serializer::new);
		CampfireDefrostRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("defrost_campfire", CampfireDefrostRecipe.Serializer::new);
		SmokingDefrostRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("defrost_smoking", SmokingDefrostRecipe.Serializer::new);
		FuelingRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("fuel_stove", FuelingRecipe.Serializer::new);
		// DietValueRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("diet_override",
		// DietValueRecipe.Serializer::new);
		IncubateRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("incubate", IncubateRecipe.Serializer::new);

		SaunaRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("sauna", SaunaRecipe.Serializer::new);
		ModifyDamageRecipe.SERIALIZER = CRECIPE_SERIALIZERS.register("modify_damage", ModifyDamageRecipe.Serializer::new);

		ShapelessCopyDataRecipe.SERIALIZER = CRECIPE_SERIALIZERS.register("shapless_copy_data", ShapelessCopyDataRecipe.Serializer::new);
		TradePolicy.SERIALIZER = RECIPE_SERIALIZERS.register("trade", TradePolicy.Serializer::new);
		WaterLevelAndEffectRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("water_level_and_effect", WaterLevelAndEffectRecipe.Serializer::new);
		NutritionRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("diet_override", NutritionRecipe.Serializer::new);
		ItemResourceAmountRecipe.SERIALIZER = RECIPE_SERIALIZERS.register("item_resource_amount", ItemResourceAmountRecipe.Serializer::new);
		GeneratorRecipe.TYPE = createRecipeType("generator");
		GeneratorSteamRecipe.TYPE = createRecipeType("steam_generator");
		ChargerRecipe.TYPE = createRecipeType("charger");
		// DietValueRecipe.TYPE = createRecipeType("diet_override");
		IncubateRecipe.TYPE = createRecipeType("incubate");
		SaunaRecipe.TYPE = createRecipeType("sauna");

		TradePolicy.TYPE = createRecipeType("trade");
		WaterLevelAndEffectRecipe.TYPE = createRecipeType("water_level_and_effect");
		NutritionRecipe.TYPE = createRecipeType("diet_override");
		ItemResourceAmountRecipe.TYPE = createRecipeType("item_resource_amount");
		ArmorTempData.TYPE = createCodecRecipeType("armor_temp", ArmorTempData.CODEC);
		BiomeTempData.TYPE = createCodecRecipeType("biome_temp", BiomeTempData.CODEC);
		BlockTempData.TYPE = createCodecRecipeType("block_temp", BlockTempData.CODEC);
		StateTransitionData.TYPE = createCodecRecipeType("state_transition", StateTransitionData.CODEC);
		CupData.TYPE = createCodecRecipeType("cup_temp", CupData.CODEC);
		DrinkTempData.TYPE = createCodecRecipeType("drink_temp", DrinkTempData.CODEC);
		FoodTempData.TYPE = createCodecRecipeType("food_temp", FoodTempData.CODEC);
		PlantTempData.TYPE = createCodecRecipeType("plant_temp", PlantTempData.CODEC);
		WorldTempData.TYPE = createCodecRecipeType("world_temp", WorldTempData.CODEC);

	}

	public static <T extends Recipe<?>> RegistryObject<RecipeType<T>> createRecipeType(String name) {
		return RECIPE_TYPES.register(name, () -> RecipeType.simple(new ResourceLocation(FHMain.MODID, name)));
	}
	public static <T extends Recipe<?>> RegistryObject<RecipeType<T>> createCRecipeType(String name) {
		return CRECIPE_TYPES.register(name, () -> RecipeType.simple(new ResourceLocation(Chorda.MODID, name)));
	}
	public static <T> RegistryObject<CodecRecipeSerializer<T>> createCodecRecipeType(String name, Codec<T> codec) {
		RegistryObject<RecipeType<DataContainerRecipe<T>>> rct = RECIPE_TYPES.register(name, () -> RecipeType.simple(new ResourceLocation(FHMain.MODID, name)));
		return RECIPE_SERIALIZERS.register(name, () -> new CodecRecipeSerializer<T>(codec, rct.get()));

	}
}