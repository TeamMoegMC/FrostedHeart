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
import mezz.jei.api.constants.VanillaRecipeCategoryUid;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocus.Mode;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
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

@JeiPlugin
public class JEICompat implements IModPlugin {
    public static IRecipeManager man;

    public static IJeiRuntime jei;

    static Map<IRecipeType<?>, Set<ResourceLocation>> types = new HashMap<>();

    static {
        types.computeIfAbsent(IRecipeType.CRAFTING, i -> new HashSet<>()).add(VanillaRecipeCategoryUid.CRAFTING);


    }

    private static boolean cachedInfoAdd = false;

    public static Map<ResourceLocation, IRecipe<?>> overrides = new HashMap<>();

    private static Map<Item, List<IngredientInfoRecipe<ItemStack>>> infos = new HashMap<>();
    public static Map<Item, Map<String,ITextComponent>> research=new HashMap<>();
    public static void addInfo() {
        if (man == null) {
            cachedInfoAdd = true;
            return;
        }
        FHMain.LOGGER.info("added info");
        cachedInfoAdd = false;
        Set<Item> items = new HashSet<>();
        for (IRecipe<?> i : ResearchListeners.recipe) {
            ItemStack out = i.getRecipeOutput();
            if (out != null && !out.isEmpty()) {
                items.add(out.getItem());
            }
        }
        infos.clear();
        ITextComponent it = TranslateUtils.translate("gui.jei.info.require_research");
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
                    VanillaTypes.ITEM, it);
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
        Minecraft.getInstance().runImmediately(JEICompat::syncJEI);
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
            if (rg.getRecipeClass() == ICraftingRecipe.class)
                types.computeIfAbsent(IRecipeType.CRAFTING, i -> new HashSet<>()).add(rg.getUid());
            else
                cates.computeIfAbsent(rg.getRecipeClass(), i -> new HashSet<>()).add(rg.getUid());
        }
        Set<Item> locked = new HashSet<>();
        Set<Item> unlocked = new HashSet<>();
        for (IRecipe<?> i : ResearchListeners.recipe) {
            Set<ResourceLocation> hs = cates.remove(i.getClass());
            Set<ResourceLocation> all = types.computeIfAbsent(i.getType(), d -> new HashSet<>());
            if (i instanceof ICraftingRecipe && i.getType() != IRecipeType.CRAFTING)
                all.addAll(types.computeIfAbsent(IRecipeType.CRAFTING, d -> new HashSet<>()));
            if (hs != null) {
                all.addAll(hs);
            }
            //System.out.println(i.getType().toString()+":"+String.join(",",all.stream().map(Object::toString).collect(Collectors.toList())));
            ItemStack irs = i.getRecipeOutput();
            IRecipe<?> ovrd = overrides.get(i.getId());
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
                entry.getValue().forEach(r -> man.unhideRecipe(r, VanillaRecipeCategoryUid.INFORMATION));
            } else {
                entry.getValue().forEach(r -> man.hideRecipe(r, VanillaRecipeCategoryUid.INFORMATION));
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
        			crafting.getUnlocks().stream().map(IRecipe::getRecipeOutput).filter(t->t!=null&&!t.isEmpty()).map(ItemStack::getItem).forEach(item::add);
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
        man.hideRecipeCategory(VanillaRecipeCategoryUid.BLASTING);
        man.hideRecipeCategory(VanillaRecipeCategoryUid.SMOKING);
        man.hideRecipeCategory(VanillaRecipeCategoryUid.FURNACE);

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
				MasterGeneratorContainer<?> container=containerScreen.getContainer();
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
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(FHMultiblocks.generator), GeneratorFuelCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(FHMultiblocks.generator_t2), GeneratorFuelCategory.UID,
                GeneratorSteamCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(FHBlocks.charger.get()), ChargerCategory.UID, ChargerCookingCategory.UID,
                ChargerDefrostCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(Blocks.CAMPFIRE), CampfireDefrostCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(Blocks.SMOKER), SmokingDefrostCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(FHBlocks.sauna.get()), SaunaCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(FHBlocks.incubator1.get()), IncubatorCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(FHBlocks.incubator2.get()), IncubatorCategory.UID);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ClientWorld world = Minecraft.getInstance().world;
        checkNotNull(world, "minecraft world");
        RecipeManager recipeManager = world.getRecipeManager();
        CuttingCategory.matching = RegistryUtils.getItems().stream()
                .filter(e -> e.getTags().contains(CuttingCategory.ktag)).collect(Collectors.toList());

        registration.addRecipes(FHUtils.filterRecipes(recipeManager,GeneratorRecipe.TYPE), GeneratorFuelCategory.UID);
        registration.addRecipes(GeneratorSteamRecipe.recipeList.values(), GeneratorSteamCategory.UID);
        registration.addRecipes(FHUtils.filterRecipes(recipeManager,ChargerRecipe.TYPE), ChargerCategory.UID);
        registration.addRecipes(recipeManager.getRecipesForType(IRecipeType.SMOKING), ChargerCookingCategory.UID);
        registration.addRecipes(CampfireDefrostRecipe.recipeList.values(),
                CampfireDefrostCategory.UID);
 
        registration.addRecipes(FHUtils.filterRecipes(recipeManager,IRecipeType.SMOKING).stream()
            .filter(iRecipe -> iRecipe.getClass() == SmokingDefrostRecipe.class).collect(Collectors.toList()), SmokingDefrostCategory.UID);
        registration.addRecipes(CampfireDefrostRecipe.recipeList.values(), ChargerDefrostCategory.UID);
        registration.addRecipes(Arrays.asList(
                        new CuttingRecipe(FHUtils.Damage(new ItemStack(FHItems.red_mushroombed.get()), 0),
                                new ItemStack(Items.RED_MUSHROOM, 10)),
                        new CuttingRecipe(FHUtils.Damage(new ItemStack(FHItems.brown_mushroombed.get()), 0),
                                new ItemStack(Items.BROWN_MUSHROOM, 10))),
                CuttingCategory.UID);
        registration.addRecipes(FHUtils.filterRecipes(recipeManager,SaunaRecipe.TYPE), SaunaCategory.UID);
        List<IncubateRecipe> rcps = new ArrayList<>(FHUtils.filterRecipes(recipeManager,IncubateRecipe.TYPE));
        rcps.add(new IncubateRecipe());
        registration.addRecipes(rcps, IncubatorCategory.UID);
    }


    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        registration.getCraftingCategory().addCategoryExtension(FuelingRecipe.class, FuelingExtension::new);
        registration.getCraftingCategory().addCategoryExtension(InstallInnerRecipe.class, InnerExtension::new);
        registration.getCraftingCategory().addCategoryExtension(ModifyDamageRecipe.class, DamageModifierExtension::new);
        //registration.getCraftingCategory().addCategoryExtension(ShapelessCopyDataRecipe.class,ShapelessCopyDataExtension::new);
    }
}
