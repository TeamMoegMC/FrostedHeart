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

package com.teammoeg.frostedheart.compat.jei;

import com.google.common.collect.ImmutableList;
import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.FHItems;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHMultiblocks;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.compat.jei.category.*;
import com.teammoeg.frostedheart.compat.jei.extension.DamageModifierExtension;
import com.teammoeg.frostedheart.compat.jei.extension.FuelingExtension;
import com.teammoeg.frostedheart.compat.jei.extension.InnerExtension;
import com.teammoeg.frostedheart.content.generator.GeneratorRecipe;
import com.teammoeg.frostedheart.content.generator.GeneratorSteamRecipe;
import com.teammoeg.frostedheart.content.generator.t1.T1GeneratorScreen;
import com.teammoeg.frostedheart.content.generator.t2.T2GeneratorScreen;
import com.teammoeg.frostedheart.content.recipes.CampfireDefrostRecipe;
import com.teammoeg.frostedheart.content.recipes.RecipeInner;
import com.teammoeg.frostedheart.content.recipes.RecipeModifyDamage;
import com.teammoeg.frostedheart.content.recipes.SmokingDefrostRecipe;
import com.teammoeg.frostedheart.content.steamenergy.charger.ChargerRecipe;
import com.teammoeg.frostedheart.content.temperature.handstoves.RecipeFueling;
import com.teammoeg.frostedheart.research.ResearchListeners;
import com.teammoeg.frostedheart.research.data.TeamResearchData;
import com.teammoeg.frostedheart.util.FHNBT;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaRecipeCategoryUid;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.plugins.jei.info.IngredientInfoRecipe;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@JeiPlugin
public class JEICompat implements IModPlugin {
	@Override
	public ResourceLocation getPluginUid() {
		return new ResourceLocation(FHMain.MODID, "jei_plugin");
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addRecipeCatalyst(new ItemStack(FHMultiblocks.generator), GeneratorFuelCategory.UID);
		registration.addRecipeCatalyst(new ItemStack(FHMultiblocks.generator_t2), GeneratorFuelCategory.UID,
				GeneratorSteamCategory.UID);
		registration.addRecipeCatalyst(new ItemStack(FHBlocks.charger), ChargerCategory.UID, ChargerCookingCategory.UID,
				ChargerDefrostCategory.UID);
		registration.addRecipeCatalyst(new ItemStack(Blocks.CAMPFIRE), CampfireDefrostCategory.UID);
		registration.addRecipeCatalyst(new ItemStack(Blocks.SMOKER), SmokingDefrostCategory.UID);
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		ClientWorld world = Minecraft.getInstance().world;
		checkNotNull(world, "minecraft world");
		RecipeManager recipeManager = world.getRecipeManager();
		CuttingCategory.matching = ForgeRegistries.ITEMS.getValues().stream()
				.filter(e -> e.getTags().contains(CuttingCategory.ktag)).collect(Collectors.toList());

		registration.addRecipes(new ArrayList<>(GeneratorRecipe.recipeList.values()), GeneratorFuelCategory.UID);
		registration.addRecipes(new ArrayList<>(GeneratorSteamRecipe.recipeList.values()), GeneratorSteamCategory.UID);
		registration.addRecipes(new ArrayList<>(ChargerRecipe.recipeList.values()), ChargerCategory.UID);
		registration.addRecipes(recipeManager.getRecipesForType(IRecipeType.SMOKING), ChargerCookingCategory.UID);
		registration.addRecipes(new ArrayList<>(CampfireDefrostRecipe.recipeList.values()),
				CampfireDefrostCategory.UID);
		registration.addRecipes(new ArrayList<>(SmokingDefrostRecipe.recipeList.values()), SmokingDefrostCategory.UID);
		registration.addRecipes(new ArrayList<>(CampfireDefrostRecipe.recipeList.values()), ChargerDefrostCategory.UID);
		registration.addRecipes(Arrays.asList(
				new CuttingRecipe(FHNBT.Damage(new ItemStack(FHItems.red_mushroombed), 0),
						new ItemStack(Items.RED_MUSHROOM, 10)),
				new CuttingRecipe(FHNBT.Damage(new ItemStack(FHItems.brown_mushroombed), 0),
						new ItemStack(Items.BROWN_MUSHROOM, 10))),
				CuttingCategory.UID);

	}

	public static IRecipeManager man;

	@Override
	public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
		man = jeiRuntime.getRecipeManager();

		syncJEI();
		man.hideRecipeCategory(VanillaRecipeCategoryUid.BLASTING);
		man.hideRecipeCategory(VanillaRecipeCategoryUid.SMOKING);
		man.hideRecipeCategory(VanillaRecipeCategoryUid.FURNACE);

	}

	static Map<IRecipeType<?>, Set<ResourceLocation>> types = new HashMap<>();

	static {
		types.computeIfAbsent(IRecipeType.CRAFTING, i -> new HashSet<>()).add(VanillaRecipeCategoryUid.CRAFTING);
	}
	private static boolean cachedInfoAdd = false;
	private static Map<Item, List<IngredientInfoRecipe<ItemStack>>> infos = new HashMap<>();

	public static void addInfo() {
		if (man == null) {
			cachedInfoAdd = true;
			return;
		}
		cachedInfoAdd = false;
		Set<Item> items = new HashSet<>();
		for (IRecipe<?> i : ResearchListeners.recipe) {
			ItemStack out = i.getRecipeOutput();
			if (out != null && !out.isEmpty()) {
				items.add(out.getItem());
			}
		}
		ITextComponent it = GuiUtils.translate("gui.jei.info.require_research");
		for (Item i : items) {

			List<IngredientInfoRecipe<ItemStack>> il = IngredientInfoRecipe.create(ImmutableList.of(new ItemStack(i)),
					VanillaTypes.ITEM, it);
			il.forEach(r -> man.addRecipe(r, VanillaRecipeCategoryUid.INFORMATION));
			infos.put(i, il);

		}
	}

	public static void syncJEI() {
		if (man == null)
			return;
		if (cachedInfoAdd)
			addInfo();
		Map<Class<?>, Set<ResourceLocation>> cates = new HashMap<>();
		for (IRecipeCategory<?> rg : man.getRecipeCategories()) {
			if (rg.getRecipeClass() == ICraftingRecipe.class)
				types.computeIfAbsent(IRecipeType.CRAFTING, i -> new HashSet<>()).add(rg.getUid());
			else
				cates.computeIfAbsent(rg.getRecipeClass(), i -> new HashSet<>()).add(rg.getUid());
		}
		Set<Item> locked=new HashSet<>();
		Set<Item> unlocked=new HashSet<>();
		for (IRecipe<?> i : ResearchListeners.recipe) {
			Set<ResourceLocation> hs = cates.remove(i.getClass());
			Set<ResourceLocation> all = types.computeIfAbsent(i.getType(), d -> new HashSet<>());
			if (hs != null) {
				all.addAll(hs);
			}
			ItemStack irs=i.getRecipeOutput();
			if (!TeamResearchData.getClientInstance().crafting.has(i)) {
				for (ResourceLocation rl : all) 
					man.hideRecipe(i, rl);
				if(!irs.isEmpty())
					locked.add(irs.getItem());
			} else {
				for (ResourceLocation rl : all)
					man.unhideRecipe(i, rl);
				if(!irs.isEmpty()) 
					unlocked.add(irs.getItem());
			}
		}
		for(Entry<Item, List<IngredientInfoRecipe<ItemStack>>> entry:infos.entrySet()) {
			if(locked.contains(entry.getKey())||!unlocked.contains(entry.getKey()))
				entry.getValue().forEach(r->man.unhideRecipe(r,VanillaRecipeCategoryUid.INFORMATION));
			else 
				entry.getValue().forEach(r->man.hideRecipe(r,VanillaRecipeCategoryUid.INFORMATION));
				
		}
		for (ResourceLocation rl : ResearchListeners.categories) {
			if (!TeamResearchData.getClientInstance().categories.has(rl))
				man.hideRecipeCategory(rl);
			else
				man.unhideRecipeCategory(rl);
		}
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
		registration.addRecipeCategories(new GeneratorFuelCategory(guiHelper), new GeneratorSteamCategory(guiHelper),
				new ChargerCategory(guiHelper), new ChargerCookingCategory(guiHelper), new CuttingCategory(guiHelper),
				new CampfireDefrostCategory(guiHelper), new SmokingDefrostCategory(guiHelper),
				new ChargerDefrostCategory(guiHelper));
	}

	@Override
	public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
		registration.getCraftingCategory().addCategoryExtension(RecipeFueling.class, FuelingExtension::new);
		registration.getCraftingCategory().addCategoryExtension(RecipeInner.class, InnerExtension::new);
		registration.getCraftingCategory().addCategoryExtension(RecipeModifyDamage.class, DamageModifierExtension::new);
	}

	@Override
	public void registerGuiHandlers(IGuiHandlerRegistration registry) {
		registry.addRecipeClickArea(T1GeneratorScreen.class, 84, 35, 9, 12, GeneratorFuelCategory.UID);
		registry.addRecipeClickArea(T2GeneratorScreen.class, 84, 35, 9, 12, GeneratorFuelCategory.UID,
				GeneratorSteamCategory.UID);
	}

	public static <T> void checkNotNull(@Nullable T object, String name) {
		if (object == null) {
			throw new NullPointerException(name + " must not be null.");
		}
	}
}
