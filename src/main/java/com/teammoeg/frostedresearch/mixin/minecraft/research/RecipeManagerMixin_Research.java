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

package com.teammoeg.frostedresearch.mixin.minecraft.research;

import java.util.Map;
import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.teammoeg.frostedresearch.ResearchListeners;

import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;
/**
 * Lock unresearched recipe
 * */
@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin_Research {
    /**
     * @author khjxiaogu
     * @reason Lock unresearched recipes
     */
    @Overwrite
    public <C extends Container, T extends Recipe<C>> Optional<T> getRecipeFor(RecipeType<T> recipeTypeIn, C pInventory, Level pLevel) {
        if (recipeTypeIn == RecipeType.CRAFTING && ForgeHooks.getCraftingPlayer() != null) {
        	
            return this.byType(recipeTypeIn).values().stream().filter((p_220266_) -> {
                return p_220266_.matches(pInventory, pLevel);
            }).filter(t -> ResearchListeners.canUseRecipe(ForgeHooks.getCraftingPlayer(), t)).findFirst();
        }
        return this.byType(recipeTypeIn).values().stream().filter((p_220266_) -> {
            return p_220266_.matches(pInventory, pLevel);
        }).findFirst();
    }

    @Shadow
    abstract <C extends Container, T extends Recipe<C>> Map<ResourceLocation, T> byType(RecipeType<T> pRecipeType);
}
