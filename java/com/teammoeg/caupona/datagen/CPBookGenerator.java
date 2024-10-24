/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Caupona.
 *
 * Caupona is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Caupona is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * Specially, we allow this software to be used alongside with closed source software Minecraft(R) and Forge or other modloader.
 * Any mods or plugins can also use apis provided by forge or com.teammoeg.caupona.api without using GPL or open source.
 *
 * You should have received a copy of the GNU General Public License
 * along with Caupona. If not, see <https://www.gnu.org/licenses/>.
 */

package com.teammoeg.caupona.datagen;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.teammoeg.caupona.CPBlocks;
import com.teammoeg.caupona.CPItems;
import com.teammoeg.caupona.CPMain;
import com.teammoeg.caupona.CPTags;
import com.teammoeg.caupona.data.TranslationProvider;
import com.teammoeg.caupona.data.recipes.SauteedRecipe;
import com.teammoeg.caupona.data.recipes.StewBaseCondition;
import com.teammoeg.caupona.data.recipes.StewCookingRecipe;
import com.teammoeg.caupona.data.recipes.baseconditions.FluidTag;
import com.teammoeg.caupona.data.recipes.baseconditions.FluidType;
import com.teammoeg.caupona.util.Utils;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class CPBookGenerator extends JsonGenerator {
	private Map<String, JsonObject> langs = new HashMap<>();
	private Map<String, Pair<ResourceLocation,StewCookingRecipe>> recipes;
	private Map<String, Pair<ResourceLocation,SauteedRecipe>> frecipes;

	class DatagenTranslationProvider implements TranslationProvider {
		String lang;

		public DatagenTranslationProvider(String lang) {
			super();
			this.lang = lang;
		}

		@Override
		public String getTranslation(String key, Object... objects) {
			if (langs.get(lang).has(key))
				return String.format(langs.get(lang).get(key).getAsString(), objects);
			return Utils.translate(key, objects).getString();
		}

		@Override
		public String getTranslationOrElse(String key, String candidate, Object... objects) {
			if (langs.get(lang).has(key))
				return String.format(langs.get(lang).get(key).getAsString(), objects);
			return candidate;
		}

	}



	public CPBookGenerator(PackOutput output, ExistingFileHelper helper) {
		super(PackType.CLIENT_RESOURCES,output, helper,"Caupona Patchouli");
	}

	String[] allangs = { "zh_cn", "en_us", "es_es", "ru_ru", "uk_ua" };

	private void loadLang(String locale) {
		try {
			Resource rc = helper.getResource(ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "lang/" + locale + ".json"),
					PackType.CLIENT_RESOURCES);
			JsonObject jo = JsonParser.parseReader(new InputStreamReader(rc.open(), "UTF-8")).getAsJsonObject();
			langs.put(locale, jo);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override
	protected void gather(JsonStorage reciver) {
		recipes = CPRecipeProvider.recipes.stream().filter(i -> i.getSecond() instanceof StewCookingRecipe)
				.map(e -> Pair.of(e.getFirst(),(StewCookingRecipe) e.getSecond()))
				.collect(Collectors.toMap(e -> Utils.getRegistryName(e.getSecond().output).getPath(), e -> e));
		frecipes= CPRecipeProvider.recipes.stream().filter(i -> i.getSecond() instanceof SauteedRecipe)
			.map(e -> Pair.of(e.getFirst(),(SauteedRecipe) e.getSecond()))
				.collect(Collectors.toMap(e -> Utils.getRegistryName(e.getSecond().output).getPath(), e -> e));
		for (String lang : allangs)
			loadLang(lang);

		for (String s : CPItems.soups)
			if (recipes.containsKey(s)&&helper.exists(PictureRL(recipes.get(s)),PackType.CLIENT_RESOURCES))
				defaultPage(reciver, s,recipes.get(s));
		for (String s : CPItems.dishes) {
			if (frecipes.containsKey(s)&&helper.exists(PictureRL(frecipes.get(s)),PackType.CLIENT_RESOURCES))
				defaultFryPage(reciver, s,frecipes.get(s));
		}
	}
	private void defaultPage(JsonStorage reciver, String name, Pair<ResourceLocation, StewCookingRecipe> pair) {
		for (String lang : allangs)
			saveEntry(name, lang, reciver, createRecipe(name, lang,pair));
	}

	private void defaultFryPage(JsonStorage reciver, String name, Pair<ResourceLocation, SauteedRecipe> pair) {
		for (String lang : allangs)
			saveFryEntry(name, lang, reciver, createFryingRecipe(name, lang, pair));
		
	}

	StewBaseCondition anyW = new FluidTag(CPTags.Fluids.ANY_WATER);
	StewBaseCondition stock = new FluidType(CPRecipeProvider.stock);
	StewBaseCondition milk = new FluidType(CPRecipeProvider.milk);
	private ResourceLocation PictureRL(Pair<ResourceLocation, ?> pair) {
		return ResourceLocation.fromNamespaceAndPath(pair.getFirst().getNamespace(), "textures/gui/recipes/" + pair.getFirst().getPath() + ".png");
	}
	private JsonObject createRecipe(String name, String locale, Pair<ResourceLocation, StewCookingRecipe> pair) {
		JsonObject page = new JsonObject();
		page.add("name", langs.get(locale).get("item.caupona." + name));
		page.addProperty("icon", ResourceLocation.fromNamespaceAndPath(CPMain.MODID, name).toString());
		page.addProperty("category", "caupona:cook_recipes");
		Item baseType = CPItems.any.get();
		if (pair.getSecond().getBase() != null && !pair.getSecond().getBase().isEmpty()) {
			StewBaseCondition sbc = pair.getSecond().getBase().get(0);
			if (sbc.equals(anyW))
				baseType = CPItems.anyWater.get();
			else if (sbc.equals(stock))
				baseType = CPItems.stock.get();
			else if (sbc.equals(milk))
				baseType = CPItems.milk.get();
		}
		JsonArray pages = new JsonArray();
		JsonObject imgpage = new JsonObject();
		imgpage.addProperty("type", "caupona:cookrecipe");
		imgpage.addProperty("img",PictureRL(pair).toString());
		imgpage.addProperty("result", ResourceLocation.fromNamespaceAndPath(CPMain.MODID, name).toString());
		imgpage.addProperty("recipe", pair.getFirst().toString());
		imgpage.addProperty("base", Utils.getRegistryName(baseType).toString());
		pages.add(imgpage);
		page.add("pages", pages);
		return page;
	}

	private JsonObject createFryingRecipe(String name, String locale, Pair<ResourceLocation, SauteedRecipe> pair) {
		JsonObject page = new JsonObject();
		page.add("name", langs.get(locale).get("item.caupona." + name));
		page.addProperty("icon", ResourceLocation.fromNamespaceAndPath(CPMain.MODID, name).toString());
		page.addProperty("category", "caupona:sautee_recipes");
		JsonArray pages = new JsonArray();
		JsonObject imgpage = new JsonObject();
		imgpage.addProperty("type", "caupona:fryrecipe");
		imgpage.addProperty("img",PictureRL(pair).toString());
		imgpage.addProperty("result",ResourceLocation.fromNamespaceAndPath(CPMain.MODID, name).toString());
		imgpage.addProperty("recipe", pair.getFirst().toString());
		imgpage.addProperty("base", Utils.getRegistryName(CPBlocks.GRAVY_BOAT).toString());
		pages.add(imgpage);
		page.add("pages", pages);
		return page;
	}

	private void saveEntry(String name, String locale,JsonStorage reciver, JsonObject entry) {
		reciver.accept(ResourceLocation.fromNamespaceAndPath(CPMain.MODID,"patchouli_books/book/"+locale + "/entries/recipes/" + name + ".json"),entry);
	}

	private void saveFryEntry(String name, String locale,JsonStorage reciver, JsonObject entry) {
		reciver.accept(ResourceLocation.fromNamespaceAndPath(CPMain.MODID,"patchouli_books/book/"+locale + "/entries/sautee_recipes/" + name + ".json"),entry);
	}



}
