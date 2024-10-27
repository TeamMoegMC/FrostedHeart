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

import static com.teammoeg.caupona.CPTags.Fluids.*;
import static com.teammoeg.caupona.CPTags.Items.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.teammoeg.caupona.CPBlocks;
import com.teammoeg.caupona.CPFluids;
import com.teammoeg.caupona.CPItems;
import com.teammoeg.caupona.CPMain;
import com.teammoeg.caupona.CPMobEffects;
import com.teammoeg.caupona.data.IDataRecipe;
import com.teammoeg.caupona.data.recipes.AspicMeltingRecipe;
import com.teammoeg.caupona.data.recipes.BoilingRecipe;
import com.teammoeg.caupona.data.recipes.BowlContainingRecipe;
import com.teammoeg.caupona.data.recipes.DoliumRecipe;
import com.teammoeg.caupona.data.recipes.FluidFoodValueRecipe;
import com.teammoeg.caupona.data.recipes.FoodValueRecipe;
import com.teammoeg.caupona.data.recipes.SpiceRecipe;
import com.teammoeg.caupona.util.SizedOrCatalystIngredient;
import com.teammoeg.caupona.util.Utils;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForgeMod;

public class CPRecipeProvider extends RecipeProvider {
	private final HashMap<String, Integer> PATH_COUNT = new HashMap<>();

	static final Fluid water = fluid(mrl("nail_soup")), milk = fluid(mrl("scalded_milk")), stock = fluid(mrl("stock"));
	public static List<Pair<ResourceLocation,IDataRecipe>> recipes = new ArrayList<>();

	public CPRecipeProvider(DataGenerator generatorIn,CompletableFuture<HolderLookup.Provider> provider) {
		super(generatorIn.getPackOutput(), provider);
	}

	@Override
	protected void buildRecipes(RecipeOutput outx) {
		BiConsumer<ResourceLocation,IDataRecipe> out = (r1,r2) -> {
			outx.accept(r1,r2,null);
		};
		for (String s : CPFluids.getSoupfluids()) {
			ResourceLocation fs = mrl(s);
			out.accept(rl("bowl/" + s),new BowlContainingRecipe( item(fs), fluid(fs),Ingredient.of(Items.BOWL)));
			
		}
		for (String s : CPItems.bread_bowls) {
			ResourceLocation fs = mrl(s);
			out.accept(rl("bowl/" + s+"_loaf"),new BowlContainingRecipe( item(fs), fluid(fs),Ingredient.of(CPBlocks.LOAF_BOWL.get())));
			
		}
		
		// out.accept(dissolve(RankineItems.CORN_EAR.get()));
		//out.accept(rl("bowl_type/bowl"), new BowlTypeRecipe(Ingredient.of(Items.BOWL),Ingredient.of(Items.BOWL)));
		//out.accept(rl("bowl_type/loaf_bowl"), new BowlTypeRecipe(Ingredient.of(Items.BOWL),Ingredient.of(CPBlocks.LOAF_BOWL.get().asItem())));
		
		out.accept(rl("bowl/water"),new BowlContainingRecipe( cpitem("water"), Fluids.WATER,Ingredient.of(Items.BOWL)));
		out.accept(rl("bowl/milk"),new BowlContainingRecipe( cpitem("milk"), NeoForgeMod.MILK.get(),Ingredient.of(Items.BOWL)));
		out.accept(rl("bowl/water"),new BowlContainingRecipe( cpitem("water"), Fluids.WATER,Ingredient.of(CPBlocks.LOAF_BOWL.get())));
		out.accept(rl("bowl/milk"),new BowlContainingRecipe( cpitem("milk"), NeoForgeMod.MILK.get(),Ingredient.of(CPBlocks.LOAF_BOWL.get())));
		out.accept(rl("boil/water"),new BoilingRecipe( fluid(mcrl("water")), fluid(mrl("nail_soup")), 200));
		out.accept(rl("boil/milk"),new BoilingRecipe( fluid(mcrl("milk")), fluid(mrl("scalded_milk")), 200));
		out.accept(rl("food/mushroom"),new FoodValueRecipe( 3, 0.6f, new ItemStack(Items.RED_MUSHROOM),
				Items.RED_MUSHROOM, Items.BROWN_MUSHROOM));
		out.accept(rl("food/pumpkin"),new FoodValueRecipe( 3, 0.6f, new ItemStack(Items.PUMPKIN), Items.PUMPKIN,
				Items.CARVED_PUMPKIN));
		out.accept(rl("food/wheat"), new FoodValueRecipe(3, 0.6f, new ItemStack(Items.WHEAT), Items.WHEAT,
				Items.WHEAT_SEEDS));
		out.accept(rl("food/fern"), 
				new FoodValueRecipe(2, 0.3f, new ItemStack(Items.FERN), Items.FERN, Items.LARGE_FERN));
		out.accept(rl("food/allium"), new FoodValueRecipe(1, 0.2f, new ItemStack(Items.ALLIUM), Items.ALLIUM));
		// System.out.println(CPBlocks.stove1.asItem());
		// System.out.println(CPBlocks.stove1.asItem().getItemCategory());
		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS,cpitem("mud_kitchen_stove")).define('D', Items.DIRT).define('S', Items.COBBLESTONE)
				.pattern("DDD").pattern("SSS").pattern("S S").unlockedBy("has_cobblestone", has(Blocks.COBBLESTONE))
				.save(outx);
		// ShapedRecipeBuilder.shaped(CPBlocks.stove2).define('T',Items.BRICK_SLAB).define('B',Items.BRICKS).define('C',Items.CLAY).pattern("TTT").pattern("BCB").pattern("B
		// B").unlockedBy("has_bricks", has(Blocks.BRICKS)).save(outx);
		ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS,CPItems.clay_pot.get()).define('C', Items.CLAY_BALL).define('S', Items.STICK)
				.pattern("CCC").pattern("CSC").pattern("CCC").unlockedBy("has_clay", has(Items.CLAY_BALL)).save(outx);
		//ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC,cpitem("lead_ingot"), 1).requires(Ingredient.of(rk(ftag("nuggets/lead"))), 9).unlockedBy("has_lead_nugget", has(cpitem("lead_nugget"))).save(outx,rl("lead_ingot_from_nugget"));
		//ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC,cpitem("lead_nugget"), 9).requires(Ingredient.of(rk(ftag("ingots/lead"))), 1).unlockedBy("has_lead_ingot", has(cpitem("lead_ingot"))).save(outx);
		//ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC,cpitem("lead_block"), 1).requires(Ingredient.of(rk(ftag("ingots/lead"))), 9).unlockedBy("has_lead_ingot", has(cpitem("lead_ingot"))).save(outx);
		//ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC,cpitem("lead_ingot"), 9).requires(Ingredient.of(rk(ftag("storage_blocks/lead"))), 9).unlockedBy("has_lead_ingot", has(cpitem("lead_block"))).save(outx,rl("lead_ingot_from_block"));
		SimpleCookingRecipeBuilder.smelting(Ingredient.of(CPItems.clay_pot.get()),RecipeCategory.DECORATIONS, CPBlocks.STEW_POT.get(), 0.35f, 200)
				.unlockedBy("has_claypot", has(CPItems.clay_pot.get())).save(outx);
		// ShapedRecipeBuilder.shapedRecipe(THPBlocks.stew_pot).key('B',Items.BRICK).key('C',Items.CLAY_BALL).patternLine("BCB").patternLine("B
		// B").patternLine("BBB").unlockedBy("has_brick",
		// hasItem(Items.BRICK)).build(out);
		// ShapelessRecipeBuilder.shapelessRecipe(THPItems.BOOK).addIngredient(Items.BOOK).addIngredient(Items.BOWL).unlockedBy("has_bowl",
		// hasItem(Items.BOWL)).build(out);
		out.accept(rl("fluid_food/milk"), new FluidFoodValueRecipe(0, 1f, new ItemStack(Items.MILK_BUCKET), 4,
				cpfluid("scalded_milk")));
		out.accept(rl("fluid_food/stock"), new FluidFoodValueRecipe(2, 1f, null, 4,
			cpfluid("stock")));
		simpleFood(out, 2, 0.4f, Items.HONEYCOMB);
		/*
		 * simpleFood(out,3,5f,ItemRegistry.amaranthitem);
		 * simpleFood(out,3,5f,ItemRegistry.barleyitem);
		 * simpleFood(out,3,5f,ItemRegistry.beanitem);
		 * simpleFood(out,3,5f,ItemRegistry.chickpeaitem);
		 * simpleFood(out,3,5f,ItemRegistry.cornitem);
		 * simpleFood(out,3,5f,ItemRegistry.lentilitem);
		 * simpleFood(out,3,5f,ItemRegistry.milletitem);
		 * simpleFood(out,3,5f,ItemRegistry.oatsitem);
		 * simpleFood(out,3,5f,ItemRegistry.quinoaitem);
		 * simpleFood(out,3,5f,ItemRegistry.riceitem);
		 * simpleFood(out,3,5f,ItemRegistry.ryeitem);
		 * simpleFood(out,3,5f,ItemRegistry.soybeanitem);
		 */
		simpleFood(out, 0, .5f, Items.BONE_MEAL);
		simpleFood(out, 1, .5f, Items.BONE);
		simpleFood(out, 3, .5f, Items.EGG);
		simpleFood(out, 3, .5f, cpitem("snail_block"));
		simpleFood(out, 3, .6f, cpitem("snail"));
		simpleFood(out, 5, .8f, cpitem("plump_snail"));
		simpleFood(out, 2, .4f, cpitem("fresh_wolfberry_leaves"));
		for (String s : ImmutableSet.of("bisque", "borscht", "dilute_soup", "egg_drop_soup", "fish_soup", "goulash",
				"hodgepodge", "meat_soup", "mushroom_soup", "nettle_soup", "poultry_soup", "pumpkin_soup",
				"seaweed_soup", "stracciatella", "vegetable_soup")) {
			aspic(s, out);
		}
		aspicNoBase("stock",out);
		spice(cpitem("garum_spice_jar"), MobEffects.JUMP, out);
		spice(cpitem("sugar_spice_jar"), MobEffects.MOVEMENT_SPEED, out);
		spice(cpitem("chives_spice_jar"), MobEffects.SLOW_FALLING, out);
		spiceLead(cpitem("vinegar_spice_jar"), MobEffects.NIGHT_VISION, out);
		spice(cpitem("asafoetida_spice_jar"), MobEffects.DAMAGE_RESISTANCE, out);
		spice(cpitem("sapa_spice_jar"), CPMobEffects.HYPERACTIVE, out);
		stewCooking(out);
		frying(out);
		out.accept(ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "dolium/garum_spice_jar"), new DoliumRecipe(null, Fluids.EMPTY, 0,
				0f, false, new ItemStack(cpitem("garum_spice_jar")),
				Arrays.asList(SizedOrCatalystIngredient.of(ItemTags.create(ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "garum_fish")), 4)),
				Ingredient.of(Items.FLOWER_POT),6000));
		out.accept(ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "dolium/vinegar_spice_jar_from_fruits"), new DoliumRecipe(null,
				Fluids.EMPTY, 0, 0f, false, new ItemStack(cpitem("vinegar_spice_jar")),
				Arrays.asList(
					SizedOrCatalystIngredient.of(ItemTags.create(ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "vinegar_fruits")), 4)),
				Ingredient.of(Items.FLOWER_POT),6000));
		out.accept(ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "dolium/vinegar_spice_jar_from_berries"), new DoliumRecipe(null,
				Fluids.EMPTY, 0, 0f, false, new ItemStack(cpitem("vinegar_spice_jar")),
				Arrays.asList(SizedOrCatalystIngredient.of(ItemTags.create(ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "vinegar_fruits_small")), 16)),
				Ingredient.of(Items.FLOWER_POT),6000));
		out.accept(ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "dolium/gravy_boat"), new DoliumRecipe(null, Fluids.EMPTY, 0, 0f,
				false, new ItemStack(CPItems.gravy_boat.get()),
				Arrays.asList(SizedOrCatalystIngredient.of(ItemTags.create(ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "walnut")), 8),
					SizedOrCatalystIngredient.of(ItemTags.ANVIL, 0)),
				Ingredient.of(CPItems.gravy_boat.get()),6000));
		out.accept(ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "dolium/gravy_boat_glass_bottle"), new DoliumRecipe(null,
				Fluids.EMPTY, 0, 0f, false, new ItemStack(CPItems.gravy_boat.get()),
				Arrays.asList(SizedOrCatalystIngredient.of(ItemTags.create(ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "walnut")), 8),
					SizedOrCatalystIngredient.of(ItemTags.ANVIL, 0)),
				Ingredient.of(Items.GLASS_BOTTLE),6000));
		out.accept(ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "dolium/vivid_charcoal"), new DoliumRecipe(null, Fluids.LAVA, 250,
				0f, false, new ItemStack(cpitem("vivid_charcoal"), 8),
				Arrays.asList(SizedOrCatalystIngredient.of(ItemTags.COALS, 3),SizedOrCatalystIngredient.of(Items.SLIME_BALL, 1)),
				null,6000));
		out.accept(ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "dolium/asafoetida"), new DoliumRecipe(null, Fluids.EMPTY, 0, 0f,
				false, new ItemStack(cpitem("asafoetida")),
				Arrays.asList(SizedOrCatalystIngredient.of(cpitem("silphium"), 1),
					SizedOrCatalystIngredient.of(ItemTags.ANVIL, 0)),
				null,6000));
		out.accept(ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "dolium/litharge_cake"), new DoliumRecipe(null, Fluids.EMPTY, 0, 0f,
				false, new ItemStack(cpitem("litharge_cake")),
				Arrays.asList(SizedOrCatalystIngredient.of(cpitem("leaden_walnut"), 1),
				SizedOrCatalystIngredient.of(ItemTags.ANVIL, 0)),
				null,6000));
		//SimpleCookingRecipeBuilder.blasting(Ingredient.of(cpitem("litharge_cake")),RecipeCategory.MISC,cpitem("lead_nugget"), 0.7f, 100).unlockedBy("has_litharge_cake", has(cpitem("litharge_cake"))).save(outx,ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "blasting/lead_nugget"));
		//SimpleCookingRecipeBuilder.smelting(Ingredient.of(cpitem("litharge_cake")),RecipeCategory.MISC,cpitem("lead_nugget"), 0.7f, 200).unlockedBy("has_litharge_cake", has(cpitem("litharge_cake"))).save(outx,ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "smelting/lead_nugget"));
	}

	private void frying(BiConsumer<ResourceLocation, IDataRecipe> out) {
		out = out.andThen((r1,r2)->recipes.add(Pair.of(r1, r2)));
		Ingredient BOWL=Ingredient.of(Items.BOWL);
		fry("huevos_pericos").high().require().mainly().of(EGGS).and().then().finish(out,BOWL);
		fry("sauteed_beef").high().require().mainly().of(ftag("raw_beef")).and().then().finish(out,BOWL);
		fry("sauteed_greens").high().require().mainly().of(GREENS).and().then().finish(out,BOWL);
		fry("sauteed_meat").med().require().mainly().of(MEAT).and().then().finish(out,BOWL);
		fry("sauteed_mushrooms").high().require().mainly().of(MUSHROOMS).and().then().finish(out,BOWL);
		fry("sauteed_roots").high().require().mainly().of(ROOTS).and().then().finish(out,BOWL);
		fry("sauteed_seafood").med().require().mainly().of(SEAFOOD).and().then().finish(out,BOWL);
		fry("sauteed_vegetables").med().require().mainly().of(VEGETABLES).and().then().finish(out,BOWL);
		fry("seared_fillet").med().require().mainly().of(FISH).and().then().finish(out,BOWL);
		fry("seared_poultry").high().require().mainly().of(POULTRY).and().then().finish(out,BOWL);
		fry("sauteed_hodgepodge").low().finish(out,BOWL);
		BOWL=Ingredient.of(CPBlocks.LOAF_BOWL.get());
		
		fry("huevos_pericos","_loaf").high().require().mainly().of(EGGS).and().then().finish(out,BOWL);
		fry("sauteed_beef","_loaf").high().require().mainly().of(ftag("raw_beef")).and().then().finish(out,BOWL);
		fry("sauteed_greens","_loaf").high().require().mainly().of(GREENS).and().then().finish(out,BOWL);
		fry("sauteed_meat","_loaf").med().require().mainly().of(MEAT).and().then().finish(out,BOWL);
		fry("sauteed_mushrooms","_loaf").high().require().mainly().of(MUSHROOMS).and().then().finish(out,BOWL);
		fry("sauteed_roots","_loaf").high().require().mainly().of(ROOTS).and().then().finish(out,BOWL);
		fry("sauteed_seafood","_loaf").med().require().mainly().of(SEAFOOD).and().then().finish(out,BOWL);
		fry("sauteed_vegetables","_loaf").med().require().mainly().of(VEGETABLES).and().then().finish(out,BOWL);
		fry("seared_fillet","_loaf").med().require().mainly().of(FISH).and().then().finish(out,BOWL);
		fry("seared_poultry","_loaf").high().require().mainly().of(POULTRY).and().then().finish(out,BOWL);
		fry("sauteed_hodgepodge","_loaf").low().finish(out,BOWL);
	}

	private void stewCooking(BiConsumer<ResourceLocation, IDataRecipe> out) {
		out = out.andThen((r1,r2)->recipes.add(Pair.of(r1, r2)));
		cook("acquacotta").high().base().tag(ANY_WATER).and().require().mainly().of(BAKED).and().then().finish(out);
		cook("congee").med().base().tag(ANY_WATER).and().require().half().of(RICE).and().then().dense(0.25).finish(out);
		cook("rice_pudding").med().base().type(milk).and().require().half().of(RICE).and().then().dense(0.25)
				.finish(out);
		cook("gruel").base().tag(ANY_WATER).and().require().half().of(CEREALS).and().then().dense(0.25).finish(out);
		cook("porridge").base().type(milk).and().require().half().of(CEREALS).and().then().dense(0.25).finish(out);
		cook("egg_drop_soup").base().tag(ANY_WATER).and().require().mainly().of(EGGS).and().not().any().of(VEGETABLES)
				.and().then().dense(0.5).finish(out);
		cook("stracciatella").base().tag(ANY_WATER).and().require().mainly().of(EGGS).and().any().of(VEGETABLES).and()
				.then().dense(0.5).finish(out);
		cook("custard").base().type(milk).and().require().mainly().of(EGGS).and().then().dense(0.5).finish(out);
		cook("vegetable_soup").base().tag(ANY_WATER).and().require().mainly().of(VEGETABLES).and().then().finish(out);
		cook("vegetable_chowder").base().type(milk).and().require().mainly().of(VEGETABLES).and().then().finish(out);
		cook("borscht").high().base().tag(ANY_WATER).and().require().mainly().of(VEGETABLES).and().typeMainly(VEGETABLES)
				.of(Items.BEETROOT).and().then().finish(out);
		cook("borscht_cream").high().base().type(milk).and().require().mainly().of(VEGETABLES).and()
				.typeMainly(VEGETABLES).of(Items.BEETROOT).and().then().finish(out);
		cook("pumpkin_soup").high().base().tag(ANY_WATER).and().require().mainly().of(VEGETABLES).and()
				.typeMainly(VEGETABLES).of(PUMPKIN).and().then().finish(out);
		cook("pumpkin_soup_cream").high().base().type(milk).and().require().mainly().of(VEGETABLES).and()
				.typeMainly(VEGETABLES).of(PUMPKIN).and().then().finish(out);
		cook("mushroom_soup").high().base().tag(ANY_WATER).and().require().mainly().of(VEGETABLES).and()
				.typeMainly(VEGETABLES).of(MUSHROOMS).and().then().finish(out);
		cook("cream_of_mushroom_soup").high().base().type(milk).and().require().mainly().of(VEGETABLES).and()
				.typeMainly(VEGETABLES).of(MUSHROOMS).and().then().finish(out);
		cook("seaweed_soup").med().base().tag(ANY_WATER).and().require().mainly().of(Items.KELP).and().then()
				.finish(out);
		cook("bisque").base().tag(ANY_WATER).and().require().mainly().of(CRUSTACEANS).and().then().finish(out);
		cook("fish_soup").base().tag(ANY_WATER).and().require().mainly().of(FISH).and().then().finish(out);
		cook("fish_chowder").base().type(milk).and().require().mainly().of(SEAFOOD).and().then().finish(out);
		cook("poultry_soup").base().tag(ANY_WATER).and().require().mainly().of(POULTRY).and().then().finish(out);
		cook("fricassee").base().type(milk).and().require().mainly().of(POULTRY).and().then().finish(out);
		cook("meat_soup").base().tag(ANY_WATER).and().require().mainly().of(MEAT).and().then().finish(out);
		cook("cream_of_meat_soup").base().type(milk).and().require().mainly().of(MEAT).and().then().finish(out);
		cook("hodgepodge").prio(-1).finish(out);
		cook("dilute_soup").prio(-2).dense(0).finish(out);

		cook("stock").special().base().type(water).and().require().mainly().of(mrl("bone")).plus(POULTRY).and().any()
				.of(mrl("bone")).of(POULTRY).and().then().finish(out);
		// cook("bone_gelatin").special().high().base().type(water).and().require().half().of(Items.BONE_MEAL).and().then()
		// .dense(3).finish(out);
		cook("egg_tongsui").special().med().base().type(water).and().require().half().of(EGGS).and().any().of(SUGAR)
				.and().not().any().of(MEATS).of(SEAFOOD).of(VEGETABLES).of(mrl("wolfberries")).and().then().finish(out);
		cook("walnut_soup").special().med().base().type(water).and().require().half().of(WALNUT).and().any().of(SUGAR)
				.and().not().any().of(MEATS).of(SEAFOOD).of(VEGETABLES).of(mrl("wolfberries")).and().then().finish(out);
		cook("goji_tongsui").special().med().base().type(water).and().require().mainly().of(SUGAR).and().any()
				.of(mrl("wolfberries")).and().not().any().of(MEATS).of(SEAFOOD).of(VEGETABLES).and().then().finish(out);
		cook("ukha").special().med().base().type(water).and().require().half().of(FISH).plus(ROOTS).and().any()
				.of(FISH).of(ROOTS).and().not().any().of(MEATS).of(CEREALS).and().then().finish(out);
		cook("goulash").special().high().base().type(stock).and().require().mainly().of(ftag("raw_beef")).and().any()
				.of(VEGETABLES).and().not().any().of(SEAFOOD).of(CEREALS).and().then().finish(out);
		cook("okroshka").special().high().require().half().of(VEGETABLES).plus(MEATS).and().any().of(VEGETABLES)
				.of(MEATS).of(mrl("ice")).and().then().finish(out);
		cook("nettle_soup").special().med().require().half().of(mrl("fern")).and().not().any().of(SEAFOOD).of(MEATS)
				.of(CEREALS).and().then().finish(out);
		cook("bone_gelatin").special().med().require().only().of(Items.BONE).and().then().dense(1).finish(out);
		//cook("scalded_milk").require().any().of(Ingredient.of(Items.ACACIA_BOAT),"Test").and().any().of(Ingredient.of(Items.ACACIA_BOAT),"item.caupona.any_based").and().then().dense(3).finish(out);
	}

	private void spice(Item spice, Holder<MobEffect> eff, BiConsumer<ResourceLocation, IDataRecipe> out) {
		out.accept(ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "spice/" + Utils.getRegistryName(spice).getPath()),new SpiceRecipe(
				Ingredient.of(spice), new MobEffectInstance(eff, 200)));

	}
	private void spiceLead(Item spice, Holder<MobEffect> eff, BiConsumer<ResourceLocation, IDataRecipe> out) {
		out.accept(ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "spice/" + Utils.getRegistryName(spice).getPath()),new SpiceRecipe(
				Ingredient.of(spice), new MobEffectInstance(eff, 200),true));

	}
	private void aspic(String soup, BiConsumer<ResourceLocation, IDataRecipe> out) {
		out.accept(ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "dolium/" + soup + "_aspic"), 
				new DoliumRecipe(stock,
						cpfluid(soup), 250, 0.25F, true, new ItemStack(cpitem(soup + "_aspic")), null,12000));
		out.accept(ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "melt/" + soup + "_aspic"),
			new AspicMeltingRecipe(Ingredient.of(cpitem(soup + "_aspic")), cpfluid(soup)));
	}
	private void aspicNoBase(String soup, BiConsumer<ResourceLocation, IDataRecipe> out) {
		out.accept(ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "dolium/" + soup + "_aspic"), new DoliumRecipe(null,cpfluid(soup), 250, 0.25F, true, new ItemStack(cpitem(soup + "_aspic")), null,12000));
		out.accept(ResourceLocation.fromNamespaceAndPath(CPMain.MODID, "melt/" + soup + "_aspic"),new AspicMeltingRecipe(Ingredient.of(cpitem(soup + "_aspic")), cpfluid(soup)));
	}

	private Fluid cpfluid(String name) {
		return BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath(CPMain.MODID, name));
	}

	private Item cpitem(String name) {
		return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CPMain.MODID, name));
	}

	private Item mitem(String name) {
		return BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(name));
	}

	private void simpleFood(BiConsumer<ResourceLocation, IDataRecipe> out, int h, float s, Item i) {
		out.accept(rl("food/" + Utils.getRegistryName(i).getPath()), new FoodValueRecipe(h, s*h*2, new ItemStack(i), i));
	}

	private StewRecipeBuilder cook(String s) {
		return StewRecipeBuilder.start(fluid(mrl(s)));
	}
	
	private SauteedRecipeBuilder fry(String s) {
		return fry(s,"");
	}

	private SauteedRecipeBuilder fry(String s, String addition) {
		return SauteedRecipeBuilder.start(mrl("block/dynamic/"+s), item(mrl(s+addition)));
	}

	private Item item(ResourceLocation rl) {
		return BuiltInRegistries.ITEM.get(rl);
	}

	private static Fluid fluid(ResourceLocation rl) {
		return BuiltInRegistries.FLUID.get(rl);
	}

	private static ResourceLocation mrl(String s) {
		return ResourceLocation.fromNamespaceAndPath(CPMain.MODID, s);
	}

	private ResourceLocation ftag(String s) {
		return ResourceLocation.fromNamespaceAndPath("c", s);
	}

	private ResourceLocation mcrl(String s) {
		return ResourceLocation.withDefaultNamespace(s);
	}
	private TagKey<Item> rk(ResourceLocation rl){
		return TagKey.create(Registries.ITEM, rl);
	}

	private ResourceLocation rl(String s) {
		if (!s.contains("/"))
			s = "crafting/" + s;
		if (PATH_COUNT.containsKey(s)) {
			int count = PATH_COUNT.get(s) + 1;
			PATH_COUNT.put(s, count);
			return ResourceLocation.fromNamespaceAndPath(CPMain.MODID, s + count);
		}
		PATH_COUNT.put(s, 1);
		return ResourceLocation.fromNamespaceAndPath(CPMain.MODID, s);
	}


}
