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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.FHContent.FHItems;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.compat.jei.category.CampfireDefrostCategory;
import com.teammoeg.frostedheart.compat.jei.category.ChargerCategory;
import com.teammoeg.frostedheart.compat.jei.category.ChargerCookingCategory;
import com.teammoeg.frostedheart.compat.jei.category.ChargerDefrostCategory;
import com.teammoeg.frostedheart.compat.jei.category.CuttingCategory;
import com.teammoeg.frostedheart.compat.jei.category.GeneratorFuelCategory;
import com.teammoeg.frostedheart.compat.jei.category.GeneratorSteamCategory;
import com.teammoeg.frostedheart.compat.jei.category.SmokingDefrostCategory;
import com.teammoeg.frostedheart.compat.jei.extension.FuelingExtension;
import com.teammoeg.frostedheart.compat.jei.extension.InnerExtension;
import com.teammoeg.frostedheart.content.generator.GeneratorRecipe;
import com.teammoeg.frostedheart.content.generator.GeneratorSteamRecipe;
import com.teammoeg.frostedheart.content.generator.t1.T1GeneratorScreen;
import com.teammoeg.frostedheart.content.generator.t2.T2GeneratorScreen;
import com.teammoeg.frostedheart.content.recipes.CampfireDefrostRecipe;
import com.teammoeg.frostedheart.content.recipes.RecipeInner;
import com.teammoeg.frostedheart.content.recipes.SmokingDefrostRecipe;
import com.teammoeg.frostedheart.content.steamenergy.charger.ChargerRecipe;
import com.teammoeg.frostedheart.content.temperature.handstoves.RecipeFueling;
import com.teammoeg.frostedheart.data.FHDataManager;
import com.teammoeg.frostedheart.research.TeamResearchData;
import com.teammoeg.frostedheart.util.FHNBT;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaRecipeCategoryUid;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

@JeiPlugin
public class JEICompat implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(FHMain.MODID, "jei_plugin");
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(FHContent.FHMultiblocks.generator), GeneratorFuelCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(FHContent.FHMultiblocks.generator_t2), GeneratorFuelCategory.UID,GeneratorSteamCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(FHContent.FHBlocks.charger), ChargerCategory.UID,ChargerCookingCategory.UID,ChargerDefrostCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(Blocks.CAMPFIRE),CampfireDefrostCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(Blocks.SMOKER),SmokingDefrostCategory.UID);
    }
    @Override
    public void registerRecipes(IRecipeRegistration registration) {
    	 ClientWorld world = Minecraft.getInstance().world;
         checkNotNull(world, "minecraft world");
         RecipeManager recipeManager = world.getRecipeManager();
         CuttingCategory.matching=ForgeRegistries.ITEMS.getValues().stream().filter(e->e.getTags().contains(CuttingCategory.ktag)).collect(Collectors.toList());
         
        registration.addRecipes(new ArrayList<>(GeneratorRecipe.recipeList.values()), GeneratorFuelCategory.UID);
        registration.addRecipes(new ArrayList<>(GeneratorSteamRecipe.recipeList.values()),GeneratorSteamCategory.UID);
        registration.addRecipes(new ArrayList<>(ChargerRecipe.recipeList.values()),ChargerCategory.UID);
        registration.addRecipes(recipeManager.getRecipesForType(IRecipeType.SMOKING),ChargerCookingCategory.UID);
        registration.addRecipes(new ArrayList<>(CampfireDefrostRecipe.recipeList.values()),CampfireDefrostCategory.UID);
        registration.addRecipes(new ArrayList<>(SmokingDefrostRecipe.recipeList.values()),SmokingDefrostCategory.UID);
        registration.addRecipes(new ArrayList<>(CampfireDefrostRecipe.recipeList.values()),ChargerDefrostCategory.UID);
        registration.addRecipes(Arrays.asList(
        		new CuttingRecipe(FHNBT.Damage(new ItemStack(FHItems.red_mushroombed),0),new ItemStack(Items.RED_MUSHROOM,10)),
        		new CuttingRecipe(FHNBT.Damage(new ItemStack(FHItems.brown_mushroombed),0),new ItemStack(Items.BROWN_MUSHROOM,10))
        		),CuttingCategory.UID); 
    }
    public static IRecipeManager man;

	@Override
	public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
		man=jeiRuntime.getRecipeManager();
		syncJEI();
		man.hideRecipeCategory(VanillaRecipeCategoryUid.BLASTING);
		man.hideRecipeCategory(VanillaRecipeCategoryUid.SMOKING);
		man.hideRecipeCategory(VanillaRecipeCategoryUid.FURNACE);
	}
	public static void syncJEI() {
		if(man==null)return;
		ClientWorld world = Minecraft.getInstance().world;
        checkNotNull(world, "minecraft world");
        RecipeManager recipeManager = world.getRecipeManager();
		for(Entry<String, Set<ResourceLocation>> i:FHDataManager.researchRecipe.entrySet()) {
			for(ResourceLocation rl:i.getValue())
				if(!TeamResearchData.getClientInstance().getData(i.getKey()).isCompleted())
					recipeManager.getRecipe(rl).ifPresent(r->man.hideRecipe(r,VanillaRecipeCategoryUid.CRAFTING));
				else
					recipeManager.getRecipe(rl).ifPresent(r->man.unhideRecipe(r,VanillaRecipeCategoryUid.CRAFTING));
		}
	}
	public static void setRecipeStatus(String research,boolean show) {
		if(man==null)return;
		ClientWorld world = Minecraft.getInstance().world;
        checkNotNull(world, "minecraft world");
        RecipeManager recipeManager = world.getRecipeManager();
        Set<ResourceLocation> srl=FHDataManager.researchRecipe.get(research);
        if(srl!=null) {
        	if(!show)for(ResourceLocation rl:srl)
        			recipeManager.getRecipe(rl).ifPresent(r->man.hideRecipe(r,VanillaRecipeCategoryUid.CRAFTING));
				else for(ResourceLocation rl:srl)
					recipeManager.getRecipe(rl).ifPresent(r->man.unhideRecipe(r,VanillaRecipeCategoryUid.CRAFTING));
		}
	}
	@Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new GeneratorFuelCategory(guiHelper),
                new GeneratorSteamCategory(guiHelper),
                new ChargerCategory(guiHelper),
                new ChargerCookingCategory(guiHelper),
                new CuttingCategory(guiHelper),
                new CampfireDefrostCategory(guiHelper),
                new SmokingDefrostCategory(guiHelper),
                new ChargerDefrostCategory(guiHelper)
        );
    }

    @Override
	public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
    	registration.getCraftingCategory().addCategoryExtension(RecipeFueling.class,FuelingExtension::new);
    	registration.getCraftingCategory().addCategoryExtension(RecipeInner.class,InnerExtension::new);
	}

	@Override
    public void registerGuiHandlers(IGuiHandlerRegistration registry) {
        registry.addRecipeClickArea(T1GeneratorScreen.class,84, 35,9,12, GeneratorFuelCategory.UID);
        registry.addRecipeClickArea(T2GeneratorScreen.class,84, 35,9,12, GeneratorFuelCategory.UID,GeneratorSteamCategory.UID);
        //registry.addRecipeClickArea(CrucibleScreen.class, 80, 31, o2oarrowLoc[2], o2oarrowLoc[3], CrucibleCategory.UID);
    }

    public static <T> void checkNotNull(@Nullable T object, String name) {
        if (object == null) {
            throw new NullPointerException(name + " must not be null.");
        }
    }
}
