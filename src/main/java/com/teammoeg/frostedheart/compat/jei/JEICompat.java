/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.compat.jei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.teammoeg.chorda.util.CRegistryHelper;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.bootstrap.common.FHMultiblocks;
import com.teammoeg.frostedheart.compat.jei.category.CampfireDefrostCategory;
import com.teammoeg.frostedheart.compat.jei.category.ChargerCategory;
import com.teammoeg.frostedheart.compat.jei.category.ChargerCookingCategory;
import com.teammoeg.frostedheart.compat.jei.category.ChargerDefrostCategory;
import com.teammoeg.frostedheart.compat.jei.category.CuttingCategory;
import com.teammoeg.frostedheart.compat.jei.category.GeneratorFuelCategory;
import com.teammoeg.frostedheart.compat.jei.category.GeneratorSteamCategory;
import com.teammoeg.frostedheart.compat.jei.category.IncubatorCategory;
import com.teammoeg.frostedheart.compat.jei.category.SaunaCategory;
import com.teammoeg.frostedheart.compat.jei.category.SmokingDefrostCategory;
import com.teammoeg.frostedheart.compat.jei.extension.DamageModifierExtension;
import com.teammoeg.frostedheart.compat.jei.extension.FuelingExtension;
import com.teammoeg.frostedheart.compat.jei.extension.InnerExtension;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.GeneratorRecipe;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.GeneratorSteamRecipe;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.GeneratorContainer;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.GeneratorScreen;
import com.teammoeg.frostedheart.content.incubator.IncubateRecipe;
import com.teammoeg.frostedheart.content.incubator.IncubatorT1Screen;
import com.teammoeg.frostedheart.content.incubator.IncubatorT2Screen;
import com.teammoeg.frostedheart.content.climate.recipe.CampfireDefrostRecipe;
import com.teammoeg.frostedheart.content.climate.recipe.InstallInnerRecipe;
import com.teammoeg.frostedheart.content.tips.client.gui.widget.TipWidget;
import com.teammoeg.frostedheart.content.utility.recipe.ModifyDamageRecipe;
import com.teammoeg.frostedheart.content.climate.recipe.SmokingDefrostRecipe;
import com.teammoeg.frostedheart.content.research.FHResearch;
import com.teammoeg.frostedheart.content.research.ResearchListeners;
import com.teammoeg.frostedheart.content.research.api.ClientResearchDataAPI;
import com.teammoeg.frostedheart.content.research.research.Research;
import com.teammoeg.frostedheart.content.research.research.effects.Effect;
import com.teammoeg.frostedheart.content.research.research.effects.EffectCrafting;
import com.teammoeg.frostedheart.content.steamenergy.charger.ChargerRecipe;
import com.teammoeg.frostedheart.content.steamenergy.sauna.SaunaRecipe;
import com.teammoeg.frostedheart.content.utility.handstoves.FuelingRecipe;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.util.client.Lang;
import com.teammoeg.chorda.util.client.Point;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
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
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

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
        FHMain.LOGGER.info("added info");
        cachedInfoAdd = false;
        Set<Item> items = new HashSet<>();
        for (Recipe<?> i : ResearchListeners.recipe) {
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
        for (Recipe<?> i : ResearchListeners.recipe) {
            //System.out.println(i.getType().toString()+":"+String.join(",",all.stream().map(Object::toString).collect(Collectors.toList())));
            ItemStack irs = RecipeUtil.getResultItem(i);
           
            //Recipe<?> ovrd = overrides.get(i.getId());
            if (!ClientResearchDataAPI.getData().get().crafting.has(i)) {
            	Set<RecipeType<?>> type=types.get(i);
            	if(type!=null)
	                for (RecipeType<?> rl : type) {
	                	try {
	                    man.hideRecipes((RecipeType)rl, Collections.singletonList(i));
	                	}catch(Exception ex) {
	                        FHMain.LOGGER.error("Error hiding recipe",ex);
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
	                        FHMain.LOGGER.error("Error hiding recipe",ex);
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
        for (ResourceLocation rl : ResearchListeners.categories) {
        	RecipeType<?> type=man.getRecipeType(rl).orElse(null);
        	if(type!=null) {
	            if (!ClientResearchDataAPI.getData().get().categories.has(rl)) {
	                man.hideRecipeCategory(type);
	            } else
	                man.unhideRecipeCategory(type);
        	}
        }
        research.clear();
        for(Research research:FHResearch.getAllResearch()) {
        	for(Effect effect:research.getEffects()) {
	        	if(ClientResearchDataAPI.getData().get().isEffectGranted(research, effect)&&effect instanceof EffectCrafting) {
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
        return new ResourceLocation(FHMain.MODID, "jei_plugin");
    }
    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        man = jeiRuntime.getRecipeManager();
        jei = jeiRuntime;
        syncJEI();
        man.hideRecipeCategory(RecipeTypes.BLASTING);
        man.hideRecipeCategory(RecipeTypes.SMOKING);
        man.hideRecipeCategory(RecipeTypes.SMELTING);
        Function<? super Object, ? extends Set<RecipeType<?>>> creator=o->new HashSet<>();
        Function<? super Object,Set<RecipeType<?>>> getter=o->types.computeIfAbsent((Object)o, creator);
        man.createRecipeCategoryLookup().includeHidden().get().forEach(t->{
        	man.createRecipeLookup(t.getRecipeType()).includeHidden().get().map(getter).forEach(o->o.add(t.getRecipeType()));
        	
        });;

    }
    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new GeneratorFuelCategory(guiHelper), new GeneratorSteamCategory(guiHelper),
                new ChargerCategory(guiHelper), new ChargerCookingCategory(guiHelper), new CuttingCategory(guiHelper),
                new CampfireDefrostCategory(guiHelper), new SmokingDefrostCategory(guiHelper),
                new ChargerDefrostCategory(guiHelper), new SaunaCategory(guiHelper), new IncubatorCategory(guiHelper));
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registry) {
        registry.addGenericGuiContainerHandler(GeneratorScreen.class, new IGuiContainerHandler<GeneratorScreen<?,?>>() {
			@Override
			public Collection<IGuiClickableArea> getGuiClickableAreas(GeneratorScreen<?,?> containerScreen, double mouseX, double mouseY) {
				List<IGuiClickableArea> col=new ArrayList<>(2);
				GeneratorContainer<?,?> container=containerScreen.getMenu();
				if(container.getTank()!=null)
					col.add(IGuiClickableArea.createBasic(98, 84, 34, 4, GeneratorSteamCategory.UID));
				Point in=container.getSlotIn();
				Point out=container.getSlotOut();
				int ininvarry=in.getY()+6;
				int outinvarry=out.getY()+6;
				int ininvarrx=in.getX()+18;
				int outinvarrx=98;
				int inarryl=76-ininvarrx;
				int outarryl=out.getX()-2-outinvarrx;
				col.add(IGuiClickableArea.createBasic(ininvarrx,ininvarry, inarryl, 4, GeneratorSteamCategory.UID));
				col.add(IGuiClickableArea.createBasic(outinvarrx,outinvarry, outarryl, 4, GeneratorSteamCategory.UID));
				return col;
			}
		});
        
        registry.addRecipeClickArea(IncubatorT1Screen.class, 80, 28, 32, 29, IncubatorCategory.UID);
        registry.addRecipeClickArea(IncubatorT2Screen.class, 107, 28, 14, 29, IncubatorCategory.UID);

        registry.addGlobalGuiHandler(new IGlobalGuiHandler() {
            @Override
            public @NotNull Collection<Rect2i> getGuiExtraAreas() {
                return Collections.singletonList(TipWidget.INSTANCE.getRect());
            }
        });
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(FHMultiblocks.Logic.GENERATOR_T1.blockItem().get()), GeneratorFuelCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(FHMultiblocks.Logic.GENERATOR_T2.blockItem().get()), GeneratorFuelCategory.UID,
                GeneratorSteamCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(FHBlocks.CHARGER.get()), ChargerCategory.UID, ChargerCookingCategory.UID,
                ChargerDefrostCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(Blocks.CAMPFIRE), CampfireDefrostCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(Blocks.SMOKER), SmokingDefrostCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(FHBlocks.SAUNA_VENT.get()), SaunaCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(FHBlocks.INCUBATOR.get()), IncubatorCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(FHBlocks.HEAT_INCUBATOR.get()), IncubatorCategory.UID);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ClientLevel world = Minecraft.getInstance().level;
        checkNotNull(world, "minecraft world");
        RecipeManager recipeManager = world.getRecipeManager();

        CuttingCategory.matching = CRegistryHelper.getItemHolders().filter(t->t.containsTag(CuttingCategory.ktag)).map(t->t.get()).collect(Collectors.toList());

        registration.addRecipes(GeneratorFuelCategory.UID, CUtils.filterRecipes(recipeManager,GeneratorRecipe.TYPE));
        registration.addRecipes(GeneratorSteamCategory.UID,new ArrayList<>(GeneratorSteamRecipe.recipeList.values()));
        registration.addRecipes(ChargerCategory.UID, CUtils.filterRecipes(recipeManager,ChargerRecipe.TYPE));
        registration.addRecipes(ChargerCookingCategory.UID, recipeManager.getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.SMOKING));
        registration.addRecipes(CampfireDefrostCategory.UID,new ArrayList<>(CampfireDefrostRecipe.recipeList.values()));
 
        registration.addRecipes(SmokingDefrostCategory.UID,recipeManager.getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.SMOKING).stream()
            .filter(iRecipe -> iRecipe.getClass() == SmokingDefrostRecipe.class).map(t->(SmokingDefrostRecipe)t).collect(Collectors.toList()));
        registration.addRecipes(ChargerDefrostCategory.UID ,new ArrayList<>(CampfireDefrostRecipe.recipeList.values()));
        registration.addRecipes(CuttingCategory.UID,Arrays.asList(
                        new CuttingRecipe(CUtils.Damage(new ItemStack(FHItems.red_mushroombed.get()), 0),
                                new ItemStack(Items.RED_MUSHROOM, 10)),
                        new CuttingRecipe(CUtils.Damage(new ItemStack(FHItems.brown_mushroombed.get()), 0),
                                new ItemStack(Items.BROWN_MUSHROOM, 10))));
        registration.addRecipes(SaunaCategory.UID , CUtils.filterRecipes(recipeManager,SaunaRecipe.TYPE));
        List<IncubateRecipe> rcps = new ArrayList<>(CUtils.filterRecipes(recipeManager,IncubateRecipe.TYPE));
        rcps.add(new IncubateRecipe());
        registration.addRecipes(IncubatorCategory.UID,rcps);
        //todo: add JEI for ItemResourceAmountRecipe
    }


    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        registration.getCraftingCategory().addCategoryExtension(FuelingRecipe.class, FuelingExtension::new);
        registration.getCraftingCategory().addCategoryExtension(InstallInnerRecipe.class, InnerExtension::new);
        registration.getCraftingCategory().addCategoryExtension(ModifyDamageRecipe.class, DamageModifierExtension::new);
        //registration.getCraftingCategory().addCategoryExtension(ShapelessCopyDataRecipe.class,ShapelessCopyDataExtension::new);
    }
}
