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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.FHItems;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHMultiblocks;
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
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.MasterGeneratorContainer;
import com.teammoeg.frostedheart.content.climate.heatdevice.generator.MasterGeneratorScreen;
import com.teammoeg.frostedheart.content.incubator.IncubateRecipe;
import com.teammoeg.frostedheart.content.incubator.IncubatorT1Screen;
import com.teammoeg.frostedheart.content.incubator.IncubatorT2Screen;
import com.teammoeg.frostedheart.recipes.CampfireDefrostRecipe;
import com.teammoeg.frostedheart.recipes.InstallInnerRecipe;
import com.teammoeg.frostedheart.recipes.ModifyDamageRecipe;
import com.teammoeg.frostedheart.recipes.SmokingDefrostRecipe;
import com.teammoeg.frostedheart.content.research.FHResearch;
import com.teammoeg.frostedheart.content.research.ResearchListeners;
import com.teammoeg.frostedheart.content.research.api.ClientResearchDataAPI;
import com.teammoeg.frostedheart.content.research.research.effects.Effect;
import com.teammoeg.frostedheart.content.research.research.effects.EffectCrafting;
import com.teammoeg.frostedheart.content.steamenergy.charger.ChargerRecipe;
import com.teammoeg.frostedheart.content.steamenergy.sauna.SaunaRecipe;
import com.teammoeg.frostedheart.content.utility.handstoves.FuelingRecipe;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.RegistryUtils;
import com.teammoeg.frostedheart.util.TranslateUtils;
import com.teammoeg.frostedheart.util.client.Point;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

@JeiPlugin
public class JEICompat implements IModPlugin {
    public static IRecipeManager man;

    public static IJeiRuntime jei;

    public static ResourceLocation BACK_GROUNG = new ResourceLocation("jei", "single_recipe_background");

    static Map<RecipeType<?>, Set<ResourceLocation>> types = new HashMap<>();

    static {
        types.computeIfAbsent(RecipeType.CRAFTING, i -> new HashSet<>()).add(RecipeTypes.CRAFTING.getUid());


    }

    private static boolean cachedInfoAdd = false;

    public static Map<ResourceLocation, Recipe<?>> overrides = new HashMap<>();

    public static mezz.jei.api.recipe.RecipeType<CampfireDefrostRecipe> CampfireDefrost = new mezz.jei.api.recipe.RecipeType<>(CampfireDefrostCategory.UID, CampfireDefrostRecipe.class);
    public static mezz.jei.api.recipe.RecipeType<ChargerRecipe> Charger = new mezz.jei.api.recipe.RecipeType<>(ChargerCategory.UID, ChargerRecipe.class);
    public static mezz.jei.api.recipe.RecipeType<SmokingRecipe> ChargerCooking = new mezz.jei.api.recipe.RecipeType<>(ChargerCookingCategory.UID, SmokingRecipe.class);
    public static mezz.jei.api.recipe.RecipeType<CampfireDefrostRecipe> ChargerDefrost = new mezz.jei.api.recipe.RecipeType<>(ChargerDefrostCategory.UID, CampfireDefrostRecipe.class);
    public static mezz.jei.api.recipe.RecipeType<CuttingRecipe> Cutting = new mezz.jei.api.recipe.RecipeType<>(CuttingCategory.UID, CuttingRecipe.class);
    public static mezz.jei.api.recipe.RecipeType<GeneratorRecipe> GeneratorFuel = new mezz.jei.api.recipe.RecipeType<>(GeneratorFuelCategory.UID, GeneratorRecipe.class);
    public static mezz.jei.api.recipe.RecipeType<GeneratorSteamRecipe> GeneratorSteam = new mezz.jei.api.recipe.RecipeType<>(GeneratorSteamCategory.UID, GeneratorSteamRecipe.class);
    public static mezz.jei.api.recipe.RecipeType<IncubateRecipe> Incubator = new mezz.jei.api.recipe.RecipeType<>(IncubatorCategory.UID, IncubateRecipe.class);
    public static mezz.jei.api.recipe.RecipeType<SaunaRecipe> Sauna = new mezz.jei.api.recipe.RecipeType<>(SaunaCategory.UID, SaunaRecipe.class);
    public static mezz.jei.api.recipe.RecipeType<SmokingDefrostRecipe> SmokingDefrost = new mezz.jei.api.recipe.RecipeType<>(SmokingDefrostCategory.UID, SmokingDefrostRecipe.class);









    private static Map<Item, List<IngredientInfoRecipe<ItemStack>>> infos = new HashMap<>();
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
            ItemStack out = i.getResultItem(null);
            if (out != null && !out.isEmpty()) {
                items.add(out.getItem());
            }
        }
        infos.clear();
        Component it = TranslateUtils.translate("gui.jei.info.require_research");
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
            List<IngredientInfoRecipe<ItemStack>> il = IngredientInfoRecipe.create(ImmutableList.of(new ItemStack(i)),
                    VanillaTypes.ITEM_STACK, it);
            il.forEach(r -> man.addRecipe(r, VanillaRecipeCategoryUid.INFORMATION));

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
        jei.getRecipesGui().showCategories(Collections.singletonList(rl));
    }

    public static void showJEIFor(ItemStack stack) {
        jei.getRecipesGui().show(man.createFocus(Mode.OUTPUT, stack));
    }

    public static void syncJEI() {
        if (man == null)
            return;
        if (cachedInfoAdd)
            addInfo();
        Map<Class<?>, Set<ResourceLocation>> cates = new HashMap<>();
        for (IRecipeCategory<?> rg : man.getRecipeCategories()) {
            //System.out.println(rg.getUid()+" : "+rg.getRecipeClass().getSimpleName());
            if (rg.getRecipeClass() == CraftingRecipe.class)
                types.computeIfAbsent(RecipeType.CRAFTING, i -> new HashSet<>()).add(rg.getUid());
            else
                cates.computeIfAbsent(rg.getRecipeClass(), i -> new HashSet<>()).add(rg.getUid());
        }
        Set<Item> locked = new HashSet<>();
        Set<Item> unlocked = new HashSet<>();
        for (Recipe<?> i : ResearchListeners.recipe) {
            Set<ResourceLocation> hs = cates.remove(i.getClass());
            Set<ResourceLocation> all = types.computeIfAbsent(i.getType(), d -> new HashSet<>());
            if (i instanceof CraftingRecipe && i.getType() != RecipeType.CRAFTING)
                all.addAll(types.computeIfAbsent(RecipeType.CRAFTING, d -> new HashSet<>()));
            if (hs != null) {
                all.addAll(hs);
            }
            //System.out.println(i.getType().toString()+":"+String.join(",",all.stream().map(Object::toString).collect(Collectors.toList())));
            ItemStack irs = i.getResultItem(null);
            Recipe<?> ovrd = overrides.get(i.getId());
            if (!ClientResearchDataAPI.getData().crafting.has(i)) {
                for (ResourceLocation rl : all) {
                	try {
                    man.hideRecipe(i, rl);
                	}catch(Exception ex) {
                        FHMain.LOGGER.error("Error hiding recipe",ex);
                    }//IDK How JEI And IE conflict, so just catch all.
                    if (ovrd != null)
                    	try {
                    		man.hideRecipe(ovrd, rl);
                    	}catch(Exception ex) {
                            FHMain.LOGGER.error("Error hiding recipe",ex);
                        }//IDK How JEI And IE conflict, so just catch all.
                    //System.out.println("hiding "+i.getId()+" for "+rl);
                }
                if (!irs.isEmpty())
                    locked.add(irs.getItem());
            } else {
                for (ResourceLocation rl : all) {
                	try {
                		man.unhideRecipe(i, rl);
                	}catch(Exception ex) {
                        FHMain.LOGGER.error("Error un-hiding recipe",ex);
                    }//IDK How JEI And IE conflict, so just catch all.
                    if (ovrd != null)
                    	try {
                    		man.unhideRecipe(ovrd, rl);
                    	}catch(Exception ex) {
                            FHMain.LOGGER.error("Error un-hiding recipe",ex);
                        }//IDK How JEI And IE conflict, so just catch all.
                }
                if (!irs.isEmpty())
                    unlocked.add(irs.getItem());
            }
        }
        for (Entry<Item, List<IngredientInfoRecipe<ItemStack>>> entry : infos.entrySet()) {
            if (locked.contains(entry.getKey()) || !unlocked.contains(entry.getKey())) {
                entry.getValue().forEach(r -> man.unhideRecipe(r, RecipeTypes.INFORMATION));
            } else {
                entry.getValue().forEach(r -> man.hideRecipe(r, RecipeTypes.INFORMATION));
            }
        }
        for (ResourceLocation rl : ResearchListeners.categories) {
            if (!ClientResearchDataAPI.getData().categories.has(rl))
                man.hideRecipeCategory(rl);
            else
                man.unhideRecipeCategory(rl);
        }
        research.clear();
        for(Effect effect:FHResearch.effects) {
        	if(effect.isGranted()&&effect instanceof EffectCrafting) {
        		Set<Item> item=new HashSet<>();
        		EffectCrafting crafting=(EffectCrafting) effect;
        		if(crafting.getItem()!=null)
        			item.add(crafting.getItem());
        		else if(crafting.getItemStack()!=null)
        			item.add(crafting.getItemStack().getItem());
        		else if(crafting.getUnlocks()!=null)
        			crafting.getUnlocks().stream().map(Recipe::getResultItem).filter(t->t!=null&&!t.isEmpty()).map(ItemStack::getItem).forEach(item::add);
        		for(Item ix:item) {
        			research.computeIfAbsent(ix, i->new LinkedHashMap<>()).put(effect.parent.get().getId(), TranslateUtils.translateTooltip("research_unlockable", effect.parent.get().getName()));
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
        registry.addGenericGuiContainerHandler(MasterGeneratorScreen.class, new IGuiContainerHandler<MasterGeneratorScreen<?>>() {
			@Override
			public Collection<IGuiClickableArea> getGuiClickableAreas(MasterGeneratorScreen<?> containerScreen, double mouseX, double mouseY) {
				List<IGuiClickableArea> col=new ArrayList<>(2);
				MasterGeneratorContainer<?> container=containerScreen.getMenu();
				if(container.getTank()!=null)
					col.add(IGuiClickableArea.createBasic(98, 84, 34, 4, GeneratorSteam));
				Point in=container.getSlotIn();
				Point out=container.getSlotOut();
				int ininvarry=in.getY()+6;
				int outinvarry=out.getY()+6;
				int ininvarrx=in.getX()+18;
				int outinvarrx=98;
				int inarryl=76-ininvarrx;
				int outarryl=out.getX()-2-outinvarrx;
				col.add(IGuiClickableArea.createBasic(ininvarrx,ininvarry, inarryl, 4, GeneratorSteam));
				col.add(IGuiClickableArea.createBasic(outinvarrx,outinvarry, outarryl, 4, GeneratorSteam));
				return col;
			}
		});
        registry.addRecipeClickArea(IncubatorT1Screen.class, 80, 28, 32, 29, Incubator);
        registry.addRecipeClickArea(IncubatorT2Screen.class, 107, 28, 14, 29, Incubator);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(FHMultiblocks.generator), CampfireDefrost);
        registration.addRecipeCatalyst(new ItemStack(FHMultiblocks.generator_t2), GeneratorFuel,
                GeneratorSteam);
        registration.addRecipeCatalyst(new ItemStack(FHBlocks.charger.get()), Charger, ChargerCooking,
                ChargerDefrost);
        registration.addRecipeCatalyst(new ItemStack(Blocks.CAMPFIRE), CampfireDefrost);
        registration.addRecipeCatalyst(new ItemStack(Blocks.SMOKER), SmokingDefrost);
        registration.addRecipeCatalyst(new ItemStack(FHBlocks.sauna.get()), Sauna);
        registration.addRecipeCatalyst(new ItemStack(FHBlocks.incubator1.get()), Incubator);
        registration.addRecipeCatalyst(new ItemStack(FHBlocks.incubator2.get()), Incubator);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ClientLevel world = Minecraft.getInstance().level;
        checkNotNull(world, "minecraft world");
        RecipeManager recipeManager = world.getRecipeManager();
//        CuttingCategory.matching = RegistryUtils.getItems().stream()
//                .filter(e -> e.getTag().contains(CuttingCategory.ktag)).collect(Collectors.toList());

        registration.addRecipes(GeneratorFuel,FHUtils.filterRecipes(recipeManager,GeneratorRecipe.TYPE.get()));
        registration.addRecipes(GeneratorSteam,GeneratorSteamRecipe.recipeList.values().stream().toList());
        registration.addRecipes(Charger,FHUtils.filterRecipes(recipeManager,ChargerRecipe.TYPE.get()));
        registration.addRecipes(ChargerCooking,recipeManager.getAllRecipesFor(RecipeType.SMOKING));
        registration.addRecipes(CampfireDefrost,CampfireDefrostRecipe.recipeList.values().stream().toList());
 
        registration.addRecipes(SmokingDefrost,FHUtils.filterRecipes(recipeManager,SmokingDefrostRecipe.TYPE.get()).stream()
            .filter(iRecipe -> iRecipe.getClass() == SmokingDefrostRecipe.class).collect(Collectors.toList()));
        registration.addRecipes(ChargerDefrost,CampfireDefrostRecipe.recipeList.values().stream().toList());
        registration.addRecipes(Cutting,Arrays.asList(
                        new CuttingRecipe(FHUtils.Damage(new ItemStack(FHItems.red_mushroombed.get()), 0),
                                new ItemStack(Items.RED_MUSHROOM, 10)),
                        new CuttingRecipe(FHUtils.Damage(new ItemStack(FHItems.brown_mushroombed.get()), 0),
                                new ItemStack(Items.BROWN_MUSHROOM, 10)))
                );
        registration.addRecipes(Sauna,FHUtils.filterRecipes(recipeManager,SaunaRecipe.TYPE.get()));
        List<IncubateRecipe> rcps = new ArrayList<>(FHUtils.filterRecipes(recipeManager,IncubateRecipe.TYPE.get()));
        rcps.add(new IncubateRecipe());
        registration.addRecipes(Incubator,rcps);
    }


    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        registration.getCraftingCategory().addCategoryExtension(FuelingRecipe.class, FuelingExtension::new);
        registration.getCraftingCategory().addCategoryExtension(InstallInnerRecipe.class, InnerExtension::new);
        registration.getCraftingCategory().addCategoryExtension(ModifyDamageRecipe.class, DamageModifierExtension::new);
        //registration.getCraftingCategory().addCategoryExtension(ShapelessCopyDataRecipe.class,ShapelessCopyDataExtension::new);
    }
}
