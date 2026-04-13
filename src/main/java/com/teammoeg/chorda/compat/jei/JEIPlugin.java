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

package com.teammoeg.chorda.compat.jei;

import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.client.cui.screenadapter.CUIOverlay;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import javax.annotation.Nullable;
import java.util.Arrays;

@JeiPlugin
public class JEIPlugin implements IModPlugin {
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
        return new ResourceLocation(Chorda.MODID, "jei_plugin");
    }
    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        man = jeiRuntime.getRecipeManager();
        jei = jeiRuntime;

    }
    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();

    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registry) {
        CUIOverlay.CUI_OVERLAYS.forEach(registry::addGlobalGuiHandler);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {

    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ClientLevel world = Minecraft.getInstance().level;
        checkNotNull(world, "minecraft world");
        RecipeManager recipeManager = world.getRecipeManager();

    }


    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
    }
}
