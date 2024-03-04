/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.mixin.minecraft;

import java.util.Map;
import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.teammoeg.frostedheart.content.research.ResearchListeners;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
/**
 * Lock unresearched recipe
 * */
@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {
    /**
     * @author khjxiaogu
     * @reason Lock unresearched recipes
     */
    @Overwrite
    public <C extends IInventory, T extends IRecipe<C>> Optional<T> getRecipe(IRecipeType<T> recipeTypeIn,
                                                                              C inventoryIn, World worldIn) {
        if (recipeTypeIn == IRecipeType.CRAFTING && ForgeHooks.getCraftingPlayer() != null) {
            return this.getRecipes(recipeTypeIn).values().stream().flatMap((recipe) -> {
                return Util.streamOptional(recipeTypeIn.matches(recipe, worldIn, inventoryIn));
            }).filter(t -> ResearchListeners.canUseRecipe(ForgeHooks.getCraftingPlayer(), t)).findFirst();
        }
        return this.getRecipes(recipeTypeIn).values().stream().flatMap((recipe) -> {
            return Util.streamOptional(recipeTypeIn.matches(recipe, worldIn, inventoryIn));
        }).findFirst();
    }

    @Shadow
    abstract <C extends IInventory, T extends IRecipe<C>> Map<ResourceLocation, IRecipe<C>> getRecipes(IRecipeType<T> recipeTypeIn);
}
