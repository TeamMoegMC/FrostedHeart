/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.data;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import blusunrize.immersiveengineering.api.IETags;
import com.teammoeg.caupona.CPFluids;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.content.trade.policy.TradeBuilder;
import com.teammoeg.frostedheart.util.RegistryUtils;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

public class FHRecipeProvider extends RecipeProvider {
	private final HashMap<String, Integer> PATH_COUNT = new HashMap<>();

	public FHRecipeProvider(DataGenerator generatorIn) {
		super(generatorIn.getPackOutput());
	}

	@Override
	protected void buildRecipes(@Nonnull Consumer<FinishedRecipe> out) {
		
		try (PrintStream ps=new PrintStream(FMLPaths.GAMEDIR.get()
				.resolve("../src/datagen/resources/data/frostedheart/data/food_healing.csv").toFile());Scanner sc = new Scanner(FMLPaths.GAMEDIR.get()
				.resolve("../src/datagen/resources/data/frostedheart/data/food_values.csv").toFile(), "UTF-8")) {
			if(sc.hasNextLine()) {
				sc.nextLine();
				while(sc.hasNextLine()) {
					String line=sc.nextLine();
					if(!line.isEmpty()) {
						String[] parts=line.split(",");
						if(parts.length==0)break;
						ResourceLocation id=new ResourceLocation(FHMain.MODID,"diet_value/"+parts[0].replaceAll(":","/"));
						ResourceLocation item=new ResourceLocation(parts[0]);
						Item it=RegistryUtils.getItem(item);
						if(it==null||it==Items.AIR) {
							FHMain.LOGGER.warn("TWR Recipe: " + item + " not exist");
							ps.println(item+","+parts[1]);
						}else {
							FoodProperties f=it.getFoodProperties();
							if(f==null)
								ps.println(item+","+parts[1]);
							else
								ps.println(item+","+f.getNutrition());
						}
						NutritionRecipeBuilder dvb=new NutritionRecipeBuilder().item(it);
						float grain=Float.parseFloat(parts[2])*10f;
						float veg=Float.parseFloat(parts[3])*10f;
						float oil=Float.parseFloat(parts[4])*10f;
						float protein=Float.parseFloat(parts[5])*10f;
						dvb.nutrition(grain,veg,oil,protein);
						dvb.save(out,id);
					}
				}
			}
		}
		// recipesGenerator(out);
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		CPFluids.getAll().filter(o->!Arrays.stream(ovride).anyMatch(RegistryUtils.getRegistryName(o).getPath()::equals)).forEach(f-> {

				out.accept(new WaterLevelFluidRecipe(new ResourceLocation(FHMain.MODID,"water_level/"+RegistryUtils.getRegistryName(f).getPath()+"_thermos"),Ingredient.of(ItemTags.create(new ResourceLocation(FHMain.MODID,"thermos"))),f,2,2));
		});
		CPFluids.getAll().filter(o->RegistryUtils.getRegistryName(o).getPath().equals("dilute_soup")).forEach(f-> {

				out.accept(new WaterLevelFluidRecipe(new ResourceLocation(FHMain.MODID,"water_level/"+RegistryUtils.getRegistryName(f).getPath()+"_thermos"),Ingredient.of(ItemTags.create(new ResourceLocation(FHMain.MODID,"thermos"))),f,3,2));
		});
		

		recipeTrade(out);
	}
	private void recipeTrade(@Nonnull Consumer<FinishedRecipe> out) {
		trade().group().buy(10,10,10,FHItems.rye_bread.get())
		.buy(1, 0.1f,20,FHItems.straw_lining.get())
		.buy(10,10,10,Items.RAW_COPPER).useAction().addFlag("copper", 1).finish()
		.sell(10, 1, 100,FHItems.energy_core.get())
		.sell(10, 1, 5,Items.COPPER_INGOT).restockAction().addFlag("copper", -1).finish().restocksBy().hasFlag("copper").finish()
		.basic()
		.finish()
		.weight(1).id("test").finish(out);;
		
	}
	private TradeBuilder trade() {
		return new TradeBuilder();
	}
	private void recipesGenerator(@Nonnull Consumer<FinishedRecipe> out) {
		GeneratorRecipeBuilder.builder(IETags.slag, 1).addInput(ItemTags.COALS).setTime(1000).build(out,
				toRL("generator/slag"));
		
	}
	private ResourceLocation toRL(String s) {
		if (!s.contains("/"))
			s = "crafting/" + s;
		if (PATH_COUNT.containsKey(s)) {
			int count = PATH_COUNT.get(s) + 1;
			PATH_COUNT.put(s, count);
			return new ResourceLocation(FHMain.MODID, s + count);
		}
		PATH_COUNT.put(s, 1);
		return new ResourceLocation(FHMain.MODID, s);
	}

	String[] ovride=new String[] {
			"dilute_soup",
			"nail_soup"
	};


}
