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
import java.util.HashMap;
import java.util.HashSet;
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
import com.teammoeg.frostedheart.client.util.GuiUtils;
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
import com.teammoeg.frostedheart.content.generator.GeneratorRecipe;
import com.teammoeg.frostedheart.content.generator.GeneratorSteamRecipe;
import com.teammoeg.frostedheart.content.generator.t1.T1GeneratorScreen;
import com.teammoeg.frostedheart.content.generator.t2.T2GeneratorScreen;
import com.teammoeg.frostedheart.content.incubator.IncubateRecipe;
import com.teammoeg.frostedheart.content.incubator.IncubatorT1Screen;
import com.teammoeg.frostedheart.content.incubator.IncubatorT2Screen;
import com.teammoeg.frostedheart.content.recipes.CampfireDefrostRecipe;
import com.teammoeg.frostedheart.content.recipes.InstallInnerRecipe;
import com.teammoeg.frostedheart.content.recipes.ModifyDamageRecipe;
import com.teammoeg.frostedheart.content.recipes.SmokingDefrostRecipe;
import com.teammoeg.frostedheart.content.steamenergy.charger.ChargerRecipe;
import com.teammoeg.frostedheart.content.steamenergy.sauna.SaunaRecipe;
import com.teammoeg.frostedheart.content.temperature.handstoves.FuelingRecipe;
import com.teammoeg.frostedheart.research.ResearchListeners;
import com.teammoeg.frostedheart.research.data.TeamResearchData;
import com.teammoeg.frostedheart.util.FHUtils;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaRecipeCategoryUid;
import mezz.jei.api.constants.VanillaTypes;
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
import net.minecraftforge.registries.ForgeRegistries;

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
        ITextComponent it = GuiUtils.translate("gui.jei.info.require_research");
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
        Minecraft.getInstance().runImmediately(() -> syncJEI());
    }

    public static void showJEICategory(ResourceLocation rl) {
        jei.getRecipesGui().showCategories(Arrays.asList(rl));
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
            if (!TeamResearchData.getClientInstance().crafting.has(i)) {
                for (ResourceLocation rl : all) {
                	try {
                    man.hideRecipe(i, rl);
                	}catch(Exception ex) {}//IDK How JEI And IE conflict, so just catch all.
                    if (ovrd != null)
                    	try {
                    		man.hideRecipe(ovrd, rl);
                    	}catch(Exception ex) {}//IDK How JEI And IE conflict, so just catch all.
                    //System.out.println("hiding "+i.getId()+" for "+rl);
                }
                if (!irs.isEmpty())
                    locked.add(irs.getItem());
            } else {
                for (ResourceLocation rl : all) {
                    man.unhideRecipe(i, rl);
                    if (ovrd != null)
                        man.unhideRecipe(ovrd, rl);
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
            if (!TeamResearchData.getClientInstance().categories.has(rl))
                man.hideRecipeCategory(rl);
            else
                man.unhideRecipeCategory(rl);
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
        registry.addRecipeClickArea(T1GeneratorScreen.class, 84, 35, 9, 12, GeneratorFuelCategory.UID);
        registry.addRecipeClickArea(T2GeneratorScreen.class, 84, 35, 9, 12, GeneratorFuelCategory.UID,
                GeneratorSteamCategory.UID);
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
                        new CuttingRecipe(FHUtils.Damage(new ItemStack(FHItems.red_mushroombed.get()), 0),
                                new ItemStack(Items.RED_MUSHROOM, 10)),
                        new CuttingRecipe(FHUtils.Damage(new ItemStack(FHItems.brown_mushroombed.get()), 0),
                                new ItemStack(Items.BROWN_MUSHROOM, 10))),
                CuttingCategory.UID);
        registration.addRecipes(new ArrayList<>(SaunaRecipe.recipeList.values()), SaunaCategory.UID);
        List<IncubateRecipe> rcps = new ArrayList<>(IncubateRecipe.recipeList.values());
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
