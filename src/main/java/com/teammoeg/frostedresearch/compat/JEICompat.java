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

package com.teammoeg.frostedresearch.compat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.teammoeg.frostedresearch.Lang;
import com.teammoeg.frostedresearch.FHResearch;
import com.teammoeg.frostedresearch.FRMain;
import com.teammoeg.frostedresearch.ResearchHooks;
import com.teammoeg.frostedresearch.UnlockList;
import com.teammoeg.frostedresearch.api.ClientResearchDataAPI;
import com.teammoeg.frostedresearch.research.Research;
import com.teammoeg.frostedresearch.research.effects.Effect;
import com.teammoeg.frostedresearch.research.effects.EffectCrafting;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.vanilla.IJeiIngredientInfoRecipe;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.library.ingredients.IngredientInfoRecipe;
import mezz.jei.library.util.RecipeUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
@JeiPlugin
public class JEICompat implements IModPlugin {

    public static IRecipeManager man;

    public static IJeiRuntime jei;

    static Map<Object,Set<RecipeType<?>>> types = new IdentityHashMap<>(3000);


    private static boolean cachedInfoAdd = false;

    public static Map<ResourceLocation, Recipe<?>> overrides = new HashMap<>();

    private static Map<Item, List<IJeiIngredientInfoRecipe>> infos = new HashMap<>();
    public static Map<Item, Map<String,Component>> research=new HashMap<>();
    public static void addInfo() {
        if (man == null) {
            cachedInfoAdd = true;
            return;
        }
        FRMain.LOGGER.info("added research jei info");
        cachedInfoAdd = false;
        Set<Item> items = new HashSet<>();
        for (Recipe<?> i : ResearchHooks.getLockList(ResearchHooks.RECIPE_UNLOCK_LIST)) {
            ItemStack out = RecipeUtil.getResultItem(i);
            if (out != null && !out.isEmpty()) {
                items.add(out.getItem());
            }
        }
        infos.clear();
        Component it = Lang.translateKey("gui.jei.info.require_research");
		/*List<IngredientInfoRecipe<ItemStack>> rinfos=(List<IngredientInfoRecipe<ItemStack>>) man.getRecipes(man.getRecipeCategory(VanillaRecipeCategoryUid.INFORMATION));
		for(IngredientInfoRecipe<ItemStack> info:rinfos) {
			List<ItemStack> iss=info.getIngredients();
			if(iss.size()==1) {
				if(items.remove(iss.get(0).getItem())) {
					infos.put(iss.get(0).getItem(),rinfos);
				}
			}
		}*/


        for (Item i : items) {
            List<IJeiIngredientInfoRecipe> il = IngredientInfoRecipe.create(jei.getIngredientManager(),ImmutableList.of(new ItemStack(i)),
                    VanillaTypes.ITEM_STACK, it);
            man.addRecipes(RecipeTypes.INFORMATION,il);

            infos.put(i, il);

        }
    }

    public static <T> void checkNotNull(@Nullable T object, String name) {
        if (object == null) {
            throw new NullPointerException(name + " must not be null.");
        }
    }
    public static void resetRuntime() {
        man = null;
        jei = null;
    }

    public static void scheduleSyncJEI() {
        //cachedInfoAdd=true;
        Minecraft.getInstance().executeBlocking(JEICompat::syncJEI);
    }

    public static void showJEICategory(ResourceLocation rl) {
    	man.getRecipeType(rl).ifPresent(o->jei.getRecipesGui().showTypes(Arrays.asList(o)));
    }

    public static void showJEIFor(ItemStack stack) {
        jei.getRecipesGui().show(jei.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT,VanillaTypes.ITEM_STACK,stack));
    }

    public static void syncJEI() {
        if (Minecraft.getInstance().level == null)
            return;
        if (man == null)
            return;
        if (cachedInfoAdd)
            addInfo();
        Set<Item> locked = new HashSet<>();
        Set<Item> unlocked = new HashSet<>();
        UnlockList<Recipe> unlockList=ClientResearchDataAPI.getData().get().getUnlockList(ResearchHooks.RECIPE_UNLOCK_LIST);
        for (Recipe<?> i : ResearchHooks.getLockList(ResearchHooks.RECIPE_UNLOCK_LIST)) {
            //System.out.println(i.getType().toString()+":"+String.join(",",all.stream().map(Object::toString).collect(Collectors.toList())));
            ItemStack irs = RecipeUtil.getResultItem(i);
           
            //Recipe<?> ovrd = overrides.get(i.getId());
            if (!unlockList.has(i)) {
            	Set<RecipeType<?>> type=types.get(i);
            	if(type!=null)
	                for (RecipeType<?> rl : type) {
	                	try {
	                    man.hideRecipes((RecipeType)rl, Collections.singletonList(i));
	                	}catch(Exception ex) {
	                        FRMain.LOGGER.error("Error hiding recipe",ex);
	                    }//IDK How JEI And IE conflict, so just catch all.
	                }
                if (!irs.isEmpty())
                    locked.add(irs.getItem());
            } else {
            	Set<RecipeType<?>> type=types.get(i);
            	if(type!=null)
	                for (RecipeType<?> rl : type) {
	                	try {
	                    man.unhideRecipes((RecipeType)rl, Collections.singletonList(i));
	                	}catch(Exception ex) {
	                        FRMain.LOGGER.error("Error hiding recipe",ex);
	                    }//IDK How JEI And IE conflict, so just catch all.
	                }
                if (!irs.isEmpty())
                    unlocked.add(irs.getItem());
            }
        }
        for (Entry<Item, List<IJeiIngredientInfoRecipe>> entry : infos.entrySet()) {
            if (locked.contains(entry.getKey()) || !unlocked.contains(entry.getKey())) {
                man.unhideRecipes(RecipeTypes.INFORMATION,entry.getValue());
            } else {
                man.hideRecipes(RecipeTypes.INFORMATION,entry.getValue());
            }
        }
        UnlockList<ResourceLocation> categoryUnlockList=ClientResearchDataAPI.getData().get().getUnlockList(ResearchHooks.CATEGORY_UNLOCK_LIST);
        for (ResourceLocation rl : ResearchHooks.getLockList(ResearchHooks.CATEGORY_UNLOCK_LIST)) {
        	RecipeType<?> type=man.getRecipeType(rl).orElse(null);
        	if(type!=null) {
	            if (!categoryUnlockList.has(rl)) {
	                man.hideRecipeCategory(type);
	            } else
	                man.unhideRecipeCategory(type);
        	}
        }
        research.clear();
        for(Research research:FHResearch.getAllResearch()) {
        	for(Effect effect:research.getEffects()) {
	        	if((!ClientResearchDataAPI.getData().get().isEffectGranted(research, effect))&&effect instanceof EffectCrafting) {
	        		Set<Item> item=new HashSet<>();
	        		EffectCrafting crafting=(EffectCrafting) effect;
	        		if(crafting.getIngredient()!=null)
	        			Stream.of(crafting.getIngredient().getItems()).map(t->t.getItem()).forEach(item::add);
	        		else if(crafting.getUnlocks()!=null)
	        			crafting.getUnlocks().stream().map(RecipeUtil::getResultItem).filter(t->t!=null&&!t.isEmpty()).map(ItemStack::getItem).forEach(item::add);
	        		for(Item ix:item) {
	        			JEICompat.research.computeIfAbsent(ix, i->new LinkedHashMap<>()).put(research.getId(), Lang.translateTooltip("research_unlockable", research.getName()));
	        		}
	        	}
        	}
        }
    }

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(FRMain.MODID, "jei_plugin");
    }
    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        man = jeiRuntime.getRecipeManager();
        jei = jeiRuntime;
        generateRecipeType();
        syncJEI();
        // man.hideRecipeCategory(RecipeTypes.BLASTING);
        // man.hideRecipeCategory(RecipeTypes.SMOKING);
        // man.hideRecipeCategory(RecipeTypes.SMELTING);


    }
    public void generateRecipeType() {
        Function<? super Object, ? extends Set<RecipeType<?>>> creator=o->new HashSet<>();
        Function<? super Object,Set<RecipeType<?>>> getter=o->types.computeIfAbsent((Object)o, creator);
        man.createRecipeCategoryLookup().includeHidden().get().forEach(t->{
        	man.createRecipeLookup(t.getRecipeType()).includeHidden().get().map(getter).forEach(o->o.add(t.getRecipeType()));
        	
        });;
    }
    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registry) {
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {

    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ClientLevel world = Minecraft.getInstance().level;
        checkNotNull(world, "minecraft world");
        RecipeManager recipeManager = world.getRecipeManager();
        JEICompat.scheduleSyncJEI();
    }


    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
    }
}
