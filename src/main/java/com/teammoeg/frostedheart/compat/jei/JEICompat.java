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

package com.teammoeg.frostedheart.compat.jei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.compat.jei.category.*;
import com.teammoeg.frostedheart.content.town.buildings.mine.BiomeMineResourceRecipe;
import org.jetbrains.annotations.NotNull;

import com.teammoeg.chorda.math.Point;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.bootstrap.common.FHMultiblocks;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.compat.jei.extension.DamageModifierExtension;
import com.teammoeg.frostedheart.compat.jei.extension.FuelingExtension;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorContainer;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorRecipe;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorScreen;
import com.teammoeg.frostedheart.content.climate.recipe.CampfireDefrostRecipe;
import com.teammoeg.frostedheart.content.climate.recipe.SmokingDefrostRecipe;
import com.teammoeg.frostedheart.content.incubator.IncubateRecipe;
import com.teammoeg.frostedheart.content.incubator.IncubatorT1Screen;
import com.teammoeg.frostedheart.content.incubator.IncubatorT2Screen;
import com.teammoeg.frostedheart.content.steamenergy.charger.ChargerRecipe;
import com.teammoeg.frostedheart.content.steamenergy.sauna.SaunaRecipe;
import com.teammoeg.frostedheart.content.tips.client.gui.widget.TipWidget;
import com.teammoeg.frostedheart.content.utility.handstoves.FuelingRecipe;
import com.teammoeg.frostedheart.content.utility.recipe.ModifyDamageRecipe;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Blocks;

@JeiPlugin
public class JEICompat implements IModPlugin {
    public static IRecipeManager man;

    public static IJeiRuntime jei;


    public static <T> void checkNotNull(@Nullable T object, String name) {
        if (object == null) {
            throw new NullPointerException(name + " must not be null.");
        }
    }
    public static void resetRuntime() {
        man = null;
        jei = null;
    }



    public static void showJEICategory(ResourceLocation rl) {
    	man.getRecipeType(rl).ifPresent(o->jei.getRecipesGui().showTypes(Arrays.asList(o)));
    }

    public static void showJEIFor(ItemStack stack) {
        jei.getRecipesGui().show(jei.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT,VanillaTypes.ITEM_STACK,stack));
    }

    public static void showJEIUsageFor(ItemStack stack) {
        jei.getRecipesGui().show(jei.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.INPUT,VanillaTypes.ITEM_STACK,stack));
    }

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(FHMain.MODID, "jei_plugin");
    }
    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        man = jeiRuntime.getRecipeManager();
        jei = jeiRuntime;
        // man.hideRecipeCategory(RecipeTypes.BLASTING);
        // man.hideRecipeCategory(RecipeTypes.SMOKING);
        // man.hideRecipeCategory(RecipeTypes.SMELTING);

    }
    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new GeneratorFuelCategory(guiHelper), //new GeneratorSteamCategory(guiHelper),
                new ChargerCategory(guiHelper), new ChargerCookingCategory(guiHelper), new CuttingCategory(guiHelper),
                new CampfireDefrostCategory(guiHelper), new SmokingDefrostCategory(guiHelper),
                new ChargerDefrostCategory(guiHelper), new SaunaCategory(guiHelper), new IncubatorCategory(guiHelper),
                new BiomeMineResourceCategory(guiHelper));
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registry) {
        registry.addGenericGuiContainerHandler(GeneratorScreen.class, new IGuiContainerHandler<GeneratorScreen<?,?>>() {
			@Override
			public Collection<IGuiClickableArea> getGuiClickableAreas(GeneratorScreen<?,?> containerScreen, double mouseX, double mouseY) {
				List<IGuiClickableArea> col=new ArrayList<>(2);
				GeneratorContainer<?,?> container=containerScreen.getMenu();
				//if(container.getTank()!=null)
				//	col.add(IGuiClickableArea.createBasic(98, 84, 34, 4, GeneratorSteamCategory.UID));
				Point in=container.getSlotIn();
				Point out=container.getSlotOut();
				int ininvarry=in.getY()+6;
				int outinvarry=out.getY()+6;
				int ininvarrx=in.getX()+18;
				int outinvarrx=98;
				int inarryl=76-ininvarrx;
				int outarryl=out.getX()-2-outinvarrx;
				col.add(IGuiClickableArea.createBasic(ininvarrx,ininvarry, inarryl, 4, GeneratorFuelCategory.UID));
				col.add(IGuiClickableArea.createBasic(outinvarrx,outinvarry, outarryl, 4, GeneratorFuelCategory.UID));
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
        registration.addRecipeCatalyst(new ItemStack(FHMultiblocks.Registration.GENERATOR_T1.blockItem().get()), GeneratorFuelCategory.UID);
        //registration.addRecipeCatalyst(new ItemStack(FHMultiblocks.Registration.GENERATOR_T2.blockItem().get()), GeneratorFuelCategory.UID,GeneratorSteamCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(FHBlocks.CHARGER.get()), ChargerCategory.UID, ChargerCookingCategory.UID,
                ChargerDefrostCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(Blocks.CAMPFIRE), CampfireDefrostCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(Blocks.SMOKER), SmokingDefrostCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(FHBlocks.SAUNA_VENT.get()), SaunaCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(FHBlocks.INCUBATOR.get()), IncubatorCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(FHBlocks.HEAT_INCUBATOR.get()), IncubatorCategory.UID);
        registration.addRecipeCatalyst(new ItemStack(FHBlocks.MINE.get()), BiomeMineResourceCategory.UID);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ClientLevel world = Minecraft.getInstance().level;
        checkNotNull(world, "minecraft world");
        RecipeManager recipeManager = world.getRecipeManager();

        CuttingCategory.matching = FHTags.Items.KNIFE.getTagCollection();

        registration.addRecipes(GeneratorFuelCategory.UID, CUtils.filterRecipes(recipeManager,GeneratorRecipe.TYPE));
        //registration.addRecipes(GeneratorSteamCategory.UID,new ArrayList<>(GeneratorSteamRecipe.recipeList.values()));
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
        registration.addRecipes(BiomeMineResourceCategory.UID, CUtils.filterRecipes(recipeManager, BiomeMineResourceRecipe.TYPE));
        //todo: add JEI for ItemResourceAmountRecipe
    }


    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        registration.getCraftingCategory().addCategoryExtension(FuelingRecipe.class, FuelingExtension::new);
        //registration.getCraftingCategory().addCategoryExtension(InstallInnerRecipe.class, InnerExtension::new);
        registration.getCraftingCategory().addCategoryExtension(ModifyDamageRecipe.class, DamageModifierExtension::new);
        //registration.getCraftingCategory().addCategoryExtension(ShapelessCopyDataRecipe.class,ShapelessCopyDataExtension::new);
    }
}
